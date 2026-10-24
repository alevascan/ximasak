package com.ximasak.backend.service;

import com.ximasak.backend.entity.Producto;
import com.ximasak.backend.repository.ProductoRepository;
import org.springframework.stereotype.Service;

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
        if (p.getEstado() == null) {
            p.setEstado(true);
        }
        return productoRepository.save(p);
    }

    public Producto actualizar(Integer id, Producto p) {
        return productoRepository.findById(id).map(existente -> {
            if (p.getNombre() != null) existente.setNombre(p.getNombre());
            if (p.getPrecioUnitario() != null) existente.setPrecioUnitario(p.getPrecioUnitario());
            if (p.getStock() != null) existente.setStock(p.getStock());
            if (p.getStockMinimo() != null) existente.setStockMinimo(p.getStockMinimo());
            if (p.getEstado() != null) existente.setEstado(p.getEstado());
            if (p.getDescripcion() != null) existente.setDescripcion(p.getDescripcion());
            return productoRepository.save(existente);
        }).orElseThrow(() -> new RuntimeException("Producto no encontrado con ID " + id));
    }

    public void eliminar(Integer id) {
        productoRepository.deleteById(id);
    }
}
