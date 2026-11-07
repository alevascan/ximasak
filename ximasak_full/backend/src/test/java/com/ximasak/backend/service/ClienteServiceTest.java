package com.ximasak.backend.service;

import com.ximasak.backend.entity.Cliente;
import com.ximasak.backend.repository.ClienteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteService clienteService;

    @Test
    @DisplayName("CP-UT-13: Buscar cliente por DNI válido retorna datos")
    void testBuscarPorDocumento_DniValido() {
        Cliente cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Cliente Prueba");
        cliente.setDni("87654321");

        when(clienteRepository.findByDniOrRuc("87654321", "87654321")).thenReturn(Optional.of(cliente));

        Optional<Cliente> resultado = clienteService.buscarPorDocumento("87654321");

        assertTrue(resultado.isPresent());
        assertEquals("Cliente Prueba", resultado.get().getNombre());
    }

    @Test
    @DisplayName("CP-UT-14: Buscar cliente con documento nulo o vacío retorna Optional vacío")
    void testBuscarPorDocumento_Vacio() {
        Optional<Cliente> resNull = clienteService.buscarPorDocumento(null);
        Optional<Cliente> resVacio = clienteService.buscarPorDocumento("   ");

        assertTrue(resNull.isEmpty());
        assertTrue(resVacio.isEmpty());
        verifyNoInteractions(clienteRepository);
    }
}
