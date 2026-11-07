package com.ximasak.backend.controller;

import com.ximasak.backend.dto.VentaRequest;
import com.ximasak.backend.entity.Venta;
import com.ximasak.backend.service.ComprobanteService;
import com.ximasak.backend.service.VentaService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "http://localhost:4200")
public class VentaController {

    private final VentaService ventaService;
    private final ComprobanteService comprobanteService;

    public VentaController(VentaService ventaService, ComprobanteService comprobanteService) {
        this.ventaService = ventaService;
        this.comprobanteService = comprobanteService;
    }

    @PostMapping
    public ResponseEntity<?> procesarVenta(@RequestBody VentaRequest request) {
        try {
            Venta venta = ventaService.procesarVenta(request);
            return ResponseEntity.ok(venta);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> listarVentas() {
        return ResponseEntity.ok(ventaService.listarVentas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerVenta(@PathVariable Integer id) {
        return ventaService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/comprobante")
    public ResponseEntity<?> comprobante(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "BOLETA") String tipo) {

        if (!"BOLETA".equalsIgnoreCase(tipo) && !"FACTURA".equalsIgnoreCase(tipo)) {
            return ResponseEntity.badRequest().body(Map.of("error", "Tipo de comprobante inválido"));
        }

        return ventaService.obtenerPorId(id).map(venta -> {
            try {
                byte[] pdf = comprobanteService.generarPdf(venta, tipo);
                String prefijo = "FACTURA".equalsIgnoreCase(tipo) ? "F001" : "B001";
                String nombre = String.format("%s-%06d.pdf", prefijo, id);
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                        .contentType(MediaType.APPLICATION_PDF)
                        .body((Object) pdf);
            } catch (RuntimeException e) {
                return ResponseEntity.badRequest().body((Object) Map.of("error", e.getMessage()));
            }
        }).orElse(ResponseEntity.notFound().build());
    }
}
