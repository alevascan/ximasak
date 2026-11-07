package com.ximasak.backend.service;

import com.ximasak.backend.entity.Rol;
import com.ximasak.backend.entity.Usuario;
import com.ximasak.backend.repository.RolRepository;
import com.ximasak.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RolRepository rolRepository;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioAdmin;
    private Rol rolAdmin;

    @BeforeEach
    void setUp() {
        rolAdmin = new Rol();
        rolAdmin.setIdRol(1);
        rolAdmin.setNombre("ADMIN");

        usuarioAdmin = new Usuario();
        usuarioAdmin.setIdUsuario(1);
        usuarioAdmin.setNombre("Admin Test");
        usuarioAdmin.setCorreo("admin@ximasak.com");
        usuarioAdmin.setPassword("$2a$10$hashedPassword");
        usuarioAdmin.setEstado(true);
        usuarioAdmin.setRol(rolAdmin);
    }

    @Test
    @DisplayName("CP-UT-05: Login exitoso con credenciales válidas genera token JWT")
    void testLogin_Exitoso() {
        when(usuarioRepository.findByCorreo("admin@ximasak.com")).thenReturn(Optional.of(usuarioAdmin));
        when(passwordEncoder.matches("123456", "$2a$10$hashedPassword")).thenReturn(true);
        when(jwtService.generarToken(usuarioAdmin)).thenReturn("mocked.jwt.token");

        String token = authService.login("admin@ximasak.com", "123456");

        assertNotNull(token);
        assertEquals("mocked.jwt.token", token);
        verify(jwtService, times(1)).generarToken(usuarioAdmin);
    }

    @Test
    @DisplayName("CP-UT-06: Login fallido con contraseña incorrecta lanza excepción")
    void testLogin_PasswordIncorrecta_LanzaExcepcion() {
        when(usuarioRepository.findByCorreo("admin@ximasak.com")).thenReturn(Optional.of(usuarioAdmin));
        when(passwordEncoder.matches("password_erroneo", "$2a$10$hashedPassword")).thenReturn(false);

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.login("admin@ximasak.com", "password_erroneo"));

        assertTrue(exception.getMessage().contains("incorrectos"));
        verifyNoInteractions(jwtService);
    }

    @Test
    @DisplayName("CP-UT-07: Login con usuario inactivo es rechazado")
    void testLogin_UsuarioInactivo_LanzaExcepcion() {
        usuarioAdmin.setEstado(false);
        when(usuarioRepository.findByCorreo("admin@ximasak.com")).thenReturn(Optional.of(usuarioAdmin));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.login("admin@ximasak.com", "123456"));

        assertTrue(exception.getMessage().contains("inactivo"));
    }

    @Test
    @DisplayName("CP-UT-08: Registrar cliente con correo ya existente es rechazado")
    void testRegistrarNuevoCliente_CorreoExistente_LanzaExcepcion() {
        when(usuarioRepository.findByCorreo("admin@ximasak.com")).thenReturn(Optional.of(usuarioAdmin));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                authService.registrarNuevoCliente("77778888", "Carlos", "admin@ximasak.com", "clave123"));

        assertTrue(exception.getMessage().contains("ya se encuentra registrado"));
        verify(usuarioRepository, never()).save(any());
    }
}
