package com.ximasak.backend.controller;

import com.ximasak.backend.entity.Cliente;
import com.ximasak.backend.entity.Reserva;
import com.ximasak.backend.repository.ClienteRepository;
import com.ximasak.backend.repository.ReservaRepository;
import com.ximasak.backend.service.EmailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@RestController
@RequestMapping("/api/reservas")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservaController {

    private final ReservaRepository reservaRepository;
    private final ClienteRepository clienteRepository;
    private final EmailService emailService;

    public ReservaController(
            ReservaRepository reservaRepository,
            ClienteRepository clienteRepository,
            EmailService emailService) {
        this.reservaRepository = reservaRepository;
        this.clienteRepository = clienteRepository;
        this.emailService = emailService;
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listarReservas() {
        List<Reserva> lista = reservaRepository.findAllByOrderByIdReservaDesc();
        List<Map<String, Object>> respuesta = new ArrayList<>();

        for (Reserva r : lista) {
            Map<String, Object> item = new HashMap<>();
            item.put("idReserva", r.getIdReserva());
            item.put("id", r.getIdReserva());
            item.put("nombre", r.getCliente() != null ? r.getCliente().getNombre() : "Cliente");
            item.put("dni", r.getCliente() != null ? r.getCliente().getDni() : "");
            item.put("telefono", r.getCliente() != null ? r.getCliente().getTelefono() : "");
            item.put("correo", r.getCliente() != null ? r.getCliente().getCorreo() : "");
            item.put("fecha", r.getFecha().toString());
            item.put("hora", r.getHora().toString());
            item.put("tipo", "Servicio Técnico");
            item.put("detalle", r.getDescripcion());
            item.put("descripcion", r.getDescripcion());
            item.put("estado", r.getEstado());
            item.put("cliente", r.getCliente());
            respuesta.add(item);
        }

        return ResponseEntity.ok(respuesta);
    }

    @PostMapping
    public ResponseEntity<?> crearReserva(@RequestBody Map<String, String> datos) {
        try {
            String dni = datos.getOrDefault("dni", "").trim();
            String nombre = datos.getOrDefault("nombre", "").trim();
            String correo = datos.getOrDefault("correo", "").trim();
            String telefono = datos.getOrDefault("telefono", "").trim();
            String ruc = datos.getOrDefault("ruc", "").trim();
            String fechaStr = datos.get("fecha");
            String horaStr = datos.get("hora");
            String tipo = datos.getOrDefault("tipo", "Mantenimiento");
            String detalle = datos.getOrDefault("detalle", "").trim();

            if (dni.isEmpty() || nombre.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "DNI y nombre son requeridos"));
            }

            // Buscar o crear cliente
            Cliente cliente = clienteRepository.findByDniOrRuc(dni, dni)
                    .orElseGet(() -> {
                        Cliente nuevo = new Cliente();
                        nuevo.setDni(dni);
                        nuevo.setNombre(nombre);
                        nuevo.setCorreo(correo);
                        nuevo.setTelefono(telefono);
                        nuevo.setRuc(ruc.isEmpty() ? null : ruc);
                        nuevo.setEstado(true);
                        return clienteRepository.save(nuevo);
                    });

            Reserva reserva = new Reserva();
            reserva.setCliente(cliente);
            reserva.setFecha(LocalDate.parse(fechaStr));
            
            // Normalizar hora (si viene "10:00" o "10:00:00")
            LocalTime hora = horaStr != null && horaStr.length() == 5 
                    ? LocalTime.parse(horaStr + ":00") 
                    : (horaStr != null ? LocalTime.parse(horaStr) : LocalTime.of(9, 0));
            reserva.setHora(hora);
            reserva.setDescripcion(tipo + ": " + detalle);
            reserva.setEstado("PENDIENTE");

            Reserva guardada = reservaRepository.save(reserva);

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Reserva guardada exitosamente",
                    "idReserva", guardada.getIdReserva()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(
            @PathVariable Integer id,
            @RequestBody Map<String, String> body) {
        
        return reservaRepository.findById(id).map(reserva -> {
            String nuevoEstado = body.getOrDefault("estado", "ATENDIDA").toUpperCase();
            reserva.setEstado(nuevoEstado);
            reservaRepository.save(reserva);

            // HU-11: Enviar notificación automática por correo si pasa a ATENDIDA
            if ("ATENDIDA".equalsIgnoreCase(nuevoEstado) && reserva.getCliente() != null) {
                String emailCliente = reserva.getCliente().getCorreo();
                String nombreCliente = reserva.getCliente().getNombre();
                emailService.enviarNotificacionOrdenAtendida(
                        emailCliente,
                        nombreCliente,
                        "Servicio Técnico",
                        reserva.getDescripcion()
                );
            }

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Estado actualizado exitosamente a " + nuevoEstado,
                    "id", id,
                    "estado", nuevoEstado
            ));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarReserva(@PathVariable Integer id) {
        if (reservaRepository.existsById(id)) {
            reservaRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("mensaje", "Reserva eliminada"));
        }
        return ResponseEntity.notFound().build();
    }
}
