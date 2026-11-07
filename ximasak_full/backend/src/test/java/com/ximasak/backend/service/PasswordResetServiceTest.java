package com.ximasak.backend.service;

import com.ximasak.backend.entity.PasswordResetToken;
import com.ximasak.backend.entity.Usuario;
import com.ximasak.backend.repository.PasswordResetTokenRepository;
import com.ximasak.backend.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordResetTokenRepository tokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService passwordResetService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Test User");
        usuario.setCorreo("test@ximasak.com");
        usuario.setPassword("$2a$10$oldHash");

        ReflectionTestUtils.setField(passwordResetService, "frontendResetUrl", "http://localhost:4200/resetear-password");
    }

    @Test
    @DisplayName("CP-UT-09: Solicitar recuperación de contraseña genera token y envía correo")
    void testSolicitarRecuperacion_Exitoso() {
        when(usuarioRepository.findByCorreo("test@ximasak.com")).thenReturn(Optional.of(usuario));

        passwordResetService.solicitarRecuperacion("test@ximasak.com");

        verify(tokenRepository, times(1)).save(any(PasswordResetToken.class));
        verify(emailService, times(1)).enviarCorreoRecuperacion(eq("test@ximasak.com"), eq("Test User"), contains("token="));
    }

    @Test
    @DisplayName("CP-UT-10: Resetear contraseña con token expirado lanza excepción")
    void testResetearPassword_TokenExpirado_LanzaExcepcion() {
        PasswordResetToken tokenExpirado = new PasswordResetToken();
        tokenExpirado.setToken("exp-token");
        tokenExpirado.setUsuario(usuario);
        tokenExpirado.setUsado(false);
        tokenExpirado.setFechaExpiracion(LocalDateTime.now().minusMinutes(5));

        when(tokenRepository.findByToken("exp-token")).thenReturn(Optional.of(tokenExpirado));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                passwordResetService.resetearPassword("exp-token", "nuevaClave123"));

        assertTrue(exception.getMessage().contains("expirado"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-UT-11: Resetear contraseña con token ya usado lanza excepción")
    void testResetearPassword_TokenYaUsado_LanzaExcepcion() {
        PasswordResetToken tokenUsado = new PasswordResetToken();
        tokenUsado.setToken("used-token");
        tokenUsado.setUsuario(usuario);
        tokenUsado.setUsado(true);
        tokenUsado.setFechaExpiracion(LocalDateTime.now().plusMinutes(20));

        when(tokenRepository.findByToken("used-token")).thenReturn(Optional.of(tokenUsado));

        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                passwordResetService.resetearPassword("used-token", "nuevaClave123"));

        assertTrue(exception.getMessage().contains("ya fue utilizado"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("CP-UT-12: Resetear contraseña exitoso actualiza hash y marca token como usado")
    void testResetearPassword_Exitoso() {
        PasswordResetToken tokenValido = new PasswordResetToken();
        tokenValido.setToken("valid-token");
        tokenValido.setUsuario(usuario);
        tokenValido.setUsado(false);
        tokenValido.setFechaExpiracion(LocalDateTime.now().plusMinutes(25));

        when(tokenRepository.findByToken("valid-token")).thenReturn(Optional.of(tokenValido));
        when(passwordEncoder.encode("nuevaClaveSegura")).thenReturn("$2a$10$newHashedClave");

        passwordResetService.resetearPassword("valid-token", "nuevaClaveSegura");

        assertTrue(tokenValido.getUsado());
        assertEquals("$2a$10$newHashedClave", usuario.getPassword());
        verify(usuarioRepository, times(1)).save(usuario);
        verify(tokenRepository, times(1)).save(tokenValido);
    }
}
