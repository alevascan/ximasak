package com.ximasak.backend.service;

import com.ximasak.backend.dto.ItemVentaRequest;
import com.ximasak.backend.dto.VentaRequest;
import com.ximasak.backend.entity.*;
import com.ximasak.backend.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock
    private VentaRepository ventaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private MedioPagoRepository medioPagoRepository;

    @Mock
    private MovimientoStockRepository movimientoStockRepository;

    @InjectMocks
    private VentaService ventaService;

    private Cliente cliente;
    private Usuario usuario;
    private MedioPago medioPago;
    private Producto producto;

    @BeforeEach
    void setUp() {
        cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Juan Pérez");
        cliente.setDni("12345678");

        usuario = new Usuario();
        usuario.setIdUsuario(2);
        usuario.setNombre("Luis Vendedor");

        medioPago = new MedioPago();
        medioPago.setIdMedioPago(1);
        medioPago.setNombre("Efectivo");

        producto = new Producto();
        producto.setIdProducto(10);
        producto.setNombre("Memoria RAM 16GB");
        producto.setPrecioUnitario(new BigDecimal("118.00")); // Precio con IGV
        producto.setStock(10);
        producto.setEstado(true);
    }

    @Test
    @DisplayName("CP-UT-01: Procesar venta exitosa con cálculo de IGV y descuento de stock")
    void testProcesarVentaExitosa_CalculoIgvYDescuentoStock() {
        // Arrange
        VentaRequest request = new VentaRequest();
        request.setIdCliente(1);
        request.setIdUsuario(2);
        request.setIdMedioPago(1);

        ItemVentaRequest item = new ItemVentaRequest();
        item.setIdProducto(10);
        item.setCantidad(2);
        request.setItems(List.of(item));

        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuario));
        when(medioPagoRepository.findById(1)).thenReturn(Optional.of(medioPago));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));
        when(ventaRepository.save(any(Venta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Venta resultado = ventaService.procesarVenta(request);

        // Assert
        assertNotNull(resultado);
        assertEquals("PAGADA", resultado.getEstado());
        // 2 unidades a 118.00 = 236.00 Total
        assertEquals(new BigDecimal("236.00"), resultado.getTotal());
        // Subtotal = 236 / 1.18 = 200.00
        assertEquals(new BigDecimal("200.00"), resultado.getSubtotal());
        // IGV = 236 - 200 = 36.00
        assertEquals(new BigDecimal("36.00"), resultado.getIgv());

        // Stock original 10 - 2 = 8
        assertEquals(8, producto.getStock());
        verify(productoRepository, times(1)).save(producto);
        verify(movimientoStockRepository, times(1)).save(any(MovimientoStock.class));
        verify(ventaRepository, times(1)).save(any(Venta.class));
    }

    @Test
    @DisplayName("CP-UT-02: Rechazar venta cuando el carrito está vacío")
    void testProcesarVenta_CarritoVacio_LanzaExcepcion() {
        VentaRequest request = new VentaRequest();
        request.setItems(Collections.emptyList());

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ventaService.procesarVenta(request));

        assertTrue(exception.getMessage().contains("El carrito no puede estar"));
        verifyNoInteractions(ventaRepository);
    }

    @Test
    @DisplayName("CP-UT-03: Rechazar venta cuando no hay stock suficiente")
    void testProcesarVenta_StockInsuficiente_LanzaExcepcion() {
        VentaRequest request = new VentaRequest();
        request.setIdCliente(1);
        request.setIdUsuario(2);
        request.setIdMedioPago(1);

        ItemVentaRequest item = new ItemVentaRequest();
        item.setIdProducto(10);
        item.setCantidad(20); // Solicita 20 cuando solo hay 10
        request.setItems(List.of(item));

        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuario));
        when(medioPagoRepository.findById(1)).thenReturn(Optional.of(medioPago));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ventaService.procesarVenta(request));

        assertTrue(exception.getMessage().contains("Stock insuficiente"));
        assertEquals(10, producto.getStock()); // El stock no debe modificarse
        verify(ventaRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-UT-04: Rechazar venta cuando el producto está inactivo")
    void testProcesarVenta_ProductoInactivo_LanzaExcepcion() {
        producto.setEstado(false);

        VentaRequest request = new VentaRequest();
        request.setIdCliente(1);
        request.setIdUsuario(2);
        request.setIdMedioPago(1);

        ItemVentaRequest item = new ItemVentaRequest();
        item.setIdProducto(10);
        item.setCantidad(1);
        request.setItems(List.of(item));

        when(clienteRepository.findById(1)).thenReturn(Optional.of(cliente));
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuario));
        when(medioPagoRepository.findById(1)).thenReturn(Optional.of(medioPago));
        when(productoRepository.findById(10)).thenReturn(Optional.of(producto));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                ventaService.procesarVenta(request));

        assertTrue(exception.getMessage().contains("no est"));
        verify(ventaRepository, never()).save(any());
    }
}
