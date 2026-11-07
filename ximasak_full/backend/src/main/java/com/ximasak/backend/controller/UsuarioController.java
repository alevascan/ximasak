package com.ximasak.backend.controller;

import com.ximasak.backend.entity.Usuario;
import com.ximasak.backend.service.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")

public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<?> crearUsuario(
            @RequestBody Map<String, Object> datos) {

        try {

            String nombre = (String) datos.get("nombre");
            String dni = (String) datos.get("dni");
            String correo = (String) datos.get("correo");
            String password = (String) datos.get("password");

            Integer idRol = Integer.valueOf(
                    datos.get("idRol").toString()
            );

            Usuario usuario = usuarioService.crearUsuario(
                    nombre,
                    dni,
                    correo,
                    password,
                    idRol
            );

            Map<String, Object> respuesta = new HashMap<>();

            respuesta.put("mensaje", "Usuario creado correctamente");
            respuesta.put("idUsuario", usuario.getIdUsuario());
            respuesta.put("nombre", usuario.getNombre());
            respuesta.put("dni", usuario.getDni());
            respuesta.put("correo", usuario.getCorreo());
            respuesta.put("rol", usuario.getRol().getNombre());

            return ResponseEntity.ok(respuesta);

        } catch (RuntimeException e) {

            Map<String, String> error = new HashMap<>();

            error.put("error", e.getMessage());

            return ResponseEntity.badRequest().body(error);
        }
    }

    @GetMapping
    public ResponseEntity<List<Usuario>> listarUsuarios() {

        return ResponseEntity.ok(
                usuarioService.listarUsuarios()
        );
    }
}
