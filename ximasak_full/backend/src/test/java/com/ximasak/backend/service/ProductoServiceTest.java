package com.ximasak.backend.service;

import com.ximasak.backend.entity.Producto;
import com.ximasak.backend.repository.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoRepository productoRepository;

    @InjectMocks
    private ProductoService productoService;

    private Producto productoValido;

    @BeforeEach
    void setUp() {
        productoValido = new Producto();
        productoValido.setIdProducto(1);
        productoValido.setNombre("Memoria RAM 16GB DDR4");
        productoValido.setPrecioUnitario(new BigDecimal("118.00"));
        productoValido.setStock(10);
        productoValido.setStockMinimo(3);
        productoValido.setEstado(true);
    }

    @Test
    @DisplayName("CP-UT-15: Guardar producto válido asigna estado activo por defecto")
    void testGuardarProductoValido_Exitoso() {
        productoValido.setEstado(null);
        productoValido.setNombre("  Memoria RAM 16GB DDR4  ");
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto guardado = productoService.guardar(productoValido);

        assertNotNull(guardado);
        assertTrue(guardado.getEstado());
        assertEquals("Memoria RAM 16GB DDR4", guardado.getNombre());
        verify(productoRepository, times(1)).save(productoValido);
    }

    @Test
    @DisplayName("CP-UT-16: Rechazar producto con nombre vacío o precio <= 0 (caso borde)")
    void testGuardarProducto_DatosInvalidos_LanzaExcepcion() {
        productoValido.setNombre("   ");
        assertThrows(IllegalArgumentException.class, () -> productoService.guardar(productoValido));

        productoValido.setNombre("SSD 500GB");
        productoValido.setPrecioUnitario(BigDecimal.ZERO);
        assertThrows(IllegalArgumentException.class, () -> productoService.guardar(productoValido));

        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-UT-17: Rechazar producto con stock inicial negativo (caso borde)")
    void testGuardarProducto_StockNegativo_LanzaExcepcion() {
        productoValido.setStock(-5);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> productoService.guardar(productoValido));

        assertTrue(ex.getMessage().contains("negativo"));
        verify(productoRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-UT-18: Actualizar producto existente modifica precio y stock")
    void testActualizarProducto_Exitoso() {
        Producto cambios = new Producto();
        cambios.setPrecioUnitario(new BigDecimal("135.00"));
        cambios.setStock(15);

        when(productoRepository.findById(1)).thenReturn(Optional.of(productoValido));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Producto actualizado = productoService.actualizar(1, cambios);

        assertEquals(new BigDecimal("135.00"), actualizado.getPrecioUnitario());
        assertEquals(15, actualizado.getStock());
    }

    @Test
    @DisplayName("CP-UT-19: Actualizar producto inexistente lanza RuntimeException")
    void testActualizarProducto_NoEncontrado_LanzaExcepcion() {
        when(productoRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> productoService.actualizar(99, productoValido));
    }

    @Test
    @DisplayName("CP-UT-20: Eliminar producto aplica baja lógica (estado=false) sin borrado físico")
    void testEliminarProducto_BajaLogica() {
        when(productoRepository.findById(1)).thenReturn(Optional.of(productoValido));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        productoService.eliminar(1);

        assertFalse(productoValido.getEstado());
        verify(productoRepository, times(1)).save(productoValido);
        verify(productoRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("CP-UT-21: Buscar producto con filtro vacío retorna todos los activos")
    void testBuscarPorNombre_VacioRetornaActivos() {
        when(productoRepository.findByEstadoTrue()).thenReturn(List.of(productoValido));

        List<Producto> resultado = productoService.buscarPorNombre("  ");

        assertEquals(1, resultado.size());
        verify(productoRepository, times(1)).findByEstadoTrue();
    }
}
