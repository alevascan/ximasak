package com.ximasak.backend.repository;

import com.ximasak.backend.entity.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, Integer> {
    List<Producto> findByEstadoTrue();
    List<Producto> findByNombreContainingIgnoreCaseAndEstadoTrue(String nombre);
}