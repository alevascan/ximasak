package com.ximasak.backend.controller;

import com.ximasak.backend.dto.ResetearPasswordRequest;
import com.ximasak.backend.dto.SolicitarRecuperacionRequest;
import com.ximasak.backend.entity.Usuario;
import com.ximasak.backend.service.AuthService;
import com.ximasak.backend.service.PasswordResetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    public AuthController(AuthService authService, PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
    }

    @GetMapping("/usuarios")
    public ResponseEntity<?> listarUsuarios() {
        return ResponseEntity.ok(authService.listarTodosLosUsuarios());
    }

    @PostMapping("/usuarios")
    public ResponseEntity<?> crearUsuarioAdmin(@RequestBody Map<String, String> datos) {
        try {
            String dni = datos.get("dni");
            String nombre = datos.get("nombre");
            String correo = datos.get("correo");
            String password = datos.get("password");
            String rol = datos.getOrDefault("rol", "CLIENTE");

            Usuario creado = authService.crearUsuarioDesdeAdmin(dni, nombre, correo, password, rol);
            return ResponseEntity.ok(creado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> editarUsuario(@PathVariable Integer id, @RequestBody Map<String, String> datos) {
        try {
            String dni = datos.get("dni");
            String nombre = datos.get("nombre");
            String correo = datos.get("correo");
            String password = datos.get("password");

            Usuario actualizado = authService.actualizarUsuario(id, dni, nombre, correo, password);
            return ResponseEntity.ok(actualizado);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> eliminarUsuario(@PathVariable Integer id) {
        authService.eliminarUsuario(id);
        return ResponseEntity.ok(Map.of("mensaje", "Eliminado correctamente"));
    }
    @PostMapping("/registro")
    public ResponseEntity<?> registrarCliente(@RequestBody Map<String, String> datos) {
        try {
            String dni = datos.get("dni");
            String nombre = datos.get("nombre");
            String correo = datos.get("correo"); 
            String password = datos.get("password");
            
            // AHORA SÍ GUARDAMOS EN LA BD
            authService.registrarNuevoCliente(dni, nombre, correo, password);
            
            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Cuenta creada con éxito");
            return ResponseEntity.ok(respuesta);
            
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error); 
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> datos) {
        try {
            String correo = datos.get("correo");
            String password = datos.get("password");

            String token = authService.login(correo, password);
            Usuario usuario = authService.buscarUsuarioPorCorreo(correo);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("mensaje", "Login exitoso");
            respuesta.put("token", token);
            respuesta.put("idUsuario", usuario.getIdUsuario());
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("correo", usuario.getCorreo());
            respuesta.put("rol", usuario.getRol().getNombre());

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(401).body(error);
        }
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<?> recuperarPassword(@RequestBody SolicitarRecuperacionRequest request) {
        passwordResetService.solicitarRecuperacion(request.getCorreo());
        return ResponseEntity.ok(Map.of(
                "mensaje", "Si el correo está registrado, recibirás un enlace de recuperación"
        ));
    }

    @PostMapping("/resetear-password")
    public ResponseEntity<?> resetearPassword(@RequestBody ResetearPasswordRequest request) {
        try {
            passwordResetService.resetearPassword(request.getToken(), request.getNuevaPassword());
            return ResponseEntity.ok(Map.of("mensaje", "Contraseña actualizada exitosamente"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}