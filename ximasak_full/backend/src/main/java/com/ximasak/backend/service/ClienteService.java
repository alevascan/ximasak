package com.ximasak.backend.service;

import com.ximasak.backend.entity.Cliente;
import com.ximasak.backend.repository.ClienteRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;

    public ClienteService(ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
    }

    public Optional<Cliente> buscarPorDocumento(String documento) {

        if (documento == null || documento.trim().isEmpty()) {
            return Optional.empty();
        }

        documento = documento.trim();

        return clienteRepository.findByDniOrRuc(documento, documento);
    }

    public java.util.List<Cliente> listarTodos() {
        return clienteRepository.findAll();
    }
}