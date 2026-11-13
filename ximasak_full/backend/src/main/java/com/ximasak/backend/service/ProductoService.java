package com.ximasak.backend.service;

import com.ximasak.backend.entity.Producto;
import com.ximasak.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;

    public ProductoService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public List<Producto> listarActivos() {
        return productoRepository.findByEstadoTrue();
    }

    public List<Producto> buscarPorNombre(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return listarActivos();
        }
        return productoRepository.findByNombreContainingIgnoreCaseAndEstadoTrue(nombre.trim());
    }

    public Producto guardar(Producto p) {
        validarDatosProducto(p);
        if (p.getEstado() == null) {
            p.setEstado(true);
        }
        return productoRepository.save(p);
    }

    public Producto actualizar(Integer id, Producto p) {
        return productoRepository.findById(id).map(existente -> {
            if (p.getNombre() != null) {
                if (p.getNombre().trim().isEmpty()) {
                    throw new IllegalArgumentException("El nombre del producto no puede estar vacío");
                }
                existente.setNombre(p.getNombre().trim());
            }
            if (p.getPrecioUnitario() != null) {
                if (p.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException("El precio unitario debe ser mayor a cero");
                }
                existente.setPrecioUnitario(p.getPrecioUnitario());
            }
            if (p.getStock() != null) {
                if (p.getStock() < 0) {
                    throw new IllegalArgumentException("El stock no puede ser negativo");
                }
                existente.setStock(p.getStock());
            }
            if (p.getStockMinimo() != null) existente.setStockMinimo(p.getStockMinimo());
            if (p.getEstado() != null) existente.setEstado(p.getEstado());
            if (p.getDescripcion() != null) existente.setDescripcion(p.getDescripcion());
            return productoRepository.save(existente);
        }).orElseThrow(() -> new RuntimeException("Producto no encontrado con ID " + id));
    }

    /** Baja lógica para preservar integridad referencial con ventas y movimientos históricos. */
    public void eliminar(Integer id) {
        Producto existente = productoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado con ID " + id));
        existente.setEstado(false);
        productoRepository.save(existente);
    }

    private void validarDatosProducto(Producto p) {
        if (p == null) {
            throw new IllegalArgumentException("El producto no puede ser nulo");
        }
        if (p.getNombre() == null || p.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        if (p.getPrecioUnitario() == null || p.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio unitario debe ser mayor a cero");
        }
        if (p.getStock() == null || p.getStock() < 0) {
            throw new IllegalArgumentException("El stock inicial no puede ser negativo");
        }
        p.setNombre(p.getNombre().trim());
    }
}
