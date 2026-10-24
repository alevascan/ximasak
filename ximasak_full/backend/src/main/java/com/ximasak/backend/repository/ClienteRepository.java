package com.ximasak.backend.repository;


import org.springframework.data.jpa.repository.JpaRepository;

import com.ximasak.backend.entity.Cliente;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByDni(String dni);

    Optional<Cliente> findByRuc(String ruc);

    Optional<Cliente> findByDniOrRuc(String dni, String ruc);
}