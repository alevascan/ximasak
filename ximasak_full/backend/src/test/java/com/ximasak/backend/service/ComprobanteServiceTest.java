package com.ximasak.backend.service;

import com.ximasak.backend.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ComprobanteServiceTest {

    private ComprobanteService comprobanteService;
    private Venta venta;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        comprobanteService = new ComprobanteService();

        cliente = new Cliente();
        cliente.setIdCliente(1);
        cliente.setNombre("Juan Diego Tasilla");
        cliente.setDni("72345678");

        Producto producto = new Producto();
        producto.setNombre("Memoria RAM 16GB DDR4");

        DetalleVenta detalle = new DetalleVenta();
        detalle.setProducto(producto);
        detalle.setCantidad(2);
        detalle.setPrecioUnitario(new BigDecimal("118.00"));
        detalle.setSubtotal(new BigDecimal("236.00"));

        MedioPago medio = new MedioPago();
        medio.setNombre("Efectivo");

        Usuario usuario = new Usuario();
        usuario.setNombre("Matias Admin");

        venta = new Venta();
        venta.setIdVenta(10);
        venta.setFecha(LocalDateTime.of(2026, 11, 13, 13, 30));
        venta.setCliente(cliente);
        venta.setMedioPago(medio);
        venta.setUsuario(usuario);
        venta.setDetalles(List.of(detalle));
        venta.setSubtotal(new BigDecimal("200.00"));
        venta.setIgv(new BigDecimal("36.00"));
        venta.setTotal(new BigDecimal("236.00"));
    }

    @Test
    @DisplayName("CP-UT-22: Generar PDF de BOLETA retorna archivo PDF válido")
    void testGenerarPdf_BoletaValida() {
        byte[] pdf = comprobanteService.generarPdf(venta, "BOLETA");

        assertNotNull(pdf);
        assertTrue(pdf.length > 500);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    @Test
    @DisplayName("CP-UT-23: Generar PDF de FACTURA válido cuando cliente posee RUC")
    void testGenerarPdf_FacturaConRucValido() {
        cliente.setRuc("20601234567");

        byte[] pdf = comprobanteService.generarPdf(venta, "FACTURA");

        assertNotNull(pdf);
        assertEquals("%PDF", new String(pdf, 0, 4));
    }

    @Test
    @DisplayName("CP-UT-24: Rechazar emisión de FACTURA cuando el cliente no tiene RUC")
    void testGenerarPdf_FacturaSinRuc_LanzaExcepcion() {
        cliente.setRuc("");

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> comprobanteService.generarPdf(venta, "FACTURA"));

        assertTrue(ex.getMessage().contains("RUC"));
    }
}
