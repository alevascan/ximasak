package com.ximasak.backend.repository;

import com.ximasak.backend.entity.Rol;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RolRepository extends JpaRepository<Rol, Integer> { 
    
    
    Optional<Rol> findByNombre(String nombre);

}