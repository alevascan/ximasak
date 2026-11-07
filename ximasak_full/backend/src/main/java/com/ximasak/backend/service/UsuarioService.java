package com.ximasak.backend.service;

import com.ximasak.backend.entity.Rol;
import com.ximasak.backend.entity.Usuario;
import com.ximasak.backend.repository.RolRepository;
import com.ximasak.backend.repository.UsuarioRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UsuarioService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository) {

        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    public Usuario crearUsuario(
            String nombre,
            String dni,
            String correo,
            String password,
            Integer idRol) {

        if (usuarioRepository.existsByDni(dni)) {
            throw new RuntimeException("El DNI ya está registrado");
        }

        if (usuarioRepository.existsByCorreo(correo)) {
            throw new RuntimeException("El correo ya está registrado");
        }

        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() ->
                        new RuntimeException("El rol no existe"));

        Usuario usuario = new Usuario();

        usuario.setNombre(nombre);
        usuario.setDni(dni);
        usuario.setCorreo(correo);

        // La contraseña se guarda cifrada
        usuario.setPassword(
                passwordEncoder.encode(password)
        );

        usuario.setRol(rol);
        usuario.setEstado(true);

        return usuarioRepository.save(usuario);
    }

    public List<Usuario> listarUsuarios() {
        return usuarioRepository.findAll();
    }
}