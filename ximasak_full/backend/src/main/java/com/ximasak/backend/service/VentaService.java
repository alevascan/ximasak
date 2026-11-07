package com.ximasak.backend.service;

import com.ximasak.backend.dto.ItemVentaRequest;
import com.ximasak.backend.dto.VentaRequest;
import com.ximasak.backend.entity.*;
import com.ximasak.backend.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Service
public class VentaService {

    private static final BigDecimal FACTOR_IGV = new BigDecimal("1.18");

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final MedioPagoRepository medioPagoRepository;
    private final MovimientoStockRepository movimientoStockRepository;

    public VentaService(
            VentaRepository ventaRepository,
            ProductoRepository productoRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            MedioPagoRepository medioPagoRepository,
            MovimientoStockRepository movimientoStockRepository) {

        this.ventaRepository = ventaRepository;
        this.productoRepository = productoRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.medioPagoRepository = medioPagoRepository;
        this.movimientoStockRepository = movimientoStockRepository;
    }

    @Transactional
    public Venta procesarVenta(VentaRequest request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new RuntimeException("El carrito no puede estar vacío");
        }

        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        MedioPago medioPago = medioPagoRepository.findById(request.getIdMedioPago())
                .orElseThrow(() -> new RuntimeException("Medio de pago no encontrado"));

        Venta venta = new Venta();
        venta.setCliente(cliente);
        venta.setUsuario(usuario);
        venta.setMedioPago(medioPago);
        venta.setFecha(LocalDateTime.now());
        venta.setEstado("PAGADA");

        BigDecimal total = BigDecimal.ZERO;

        for (ItemVentaRequest item : request.getItems()) {

            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new RuntimeException("Cantidad inválida para un producto del carrito");
            }

            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException(
                            "Producto no encontrado: ID " + item.getIdProducto()));

            if (!Boolean.TRUE.equals(producto.getEstado())) {
                throw new RuntimeException("El producto '" + producto.getNombre() + "' no está disponible");
            }

            if (producto.getStock() < item.getCantidad()) {
                throw new RuntimeException(
                        "Stock insuficiente para '" + producto.getNombre() +
                        "'. Disponible: " + producto.getStock());
            }

            // Precio autoritativo: siempre del servidor, nunca del frontend
            BigDecimal precioUnitario = producto.getPrecioUnitario();
            BigDecimal subtotalItem = precioUnitario.multiply(BigDecimal.valueOf(item.getCantidad()));

            DetalleVenta detalle = new DetalleVenta();
            detalle.setVenta(venta);
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotal(subtotalItem);

            venta.getDetalles().add(detalle);

            total = total.add(subtotalItem);

            // Descontar stock
            producto.setStock(producto.getStock() - item.getCantidad());
            productoRepository.save(producto);

            // Registrar movimiento de stock
            MovimientoStock movimiento = new MovimientoStock();
            movimiento.setProducto(producto);
            movimiento.setUsuario(usuario);
            movimiento.setTipo("venta");
            movimiento.setCantidad(-item.getCantidad());
            movimiento.setMotivo("Venta mostrador");
            movimiento.setFecha(LocalDateTime.now());
            movimientoStockRepository.save(movimiento);
        }

        // precio_unitario ya incluye IGV -> total = suma de items; subtotal = total / 1.18
        BigDecimal subtotalSinIgv = total.divide(FACTOR_IGV, 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotalSinIgv);

        venta.setSubtotal(subtotalSinIgv);
        venta.setIgv(igv);
        venta.setTotal(total);

        return ventaRepository.save(venta);
    }

    public java.util.List<Venta> listarVentas() {
        return ventaRepository.findAll();
    }

    public java.util.Optional<Venta> obtenerPorId(Integer id) {
        return ventaRepository.findById(id);
    }
}