package com.ximasak.backend.service;

import com.ximasak.backend.entity.PasswordResetToken;
import com.ximasak.backend.entity.Usuario;
import com.ximasak.backend.repository.PasswordResetTokenRepository;
import com.ximasak.backend.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.frontend.reset-password-url}")
    private String frontendResetUrl;

    private static final int EXPIRACION_MINUTOS = 30;

    public void solicitarRecuperacion(String correo) {
        usuarioRepository.findByCorreo(correo).ifPresent(usuario -> {
            String token = UUID.randomUUID().toString();

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setToken(token);
            resetToken.setUsuario(usuario);
            resetToken.setFechaExpiracion(LocalDateTime.now().plusMinutes(EXPIRACION_MINUTOS));
            resetToken.setUsado(false);
            tokenRepository.save(resetToken);

            String enlace = frontendResetUrl + "?token=" + token;
            emailService.enviarCorreoRecuperacion(usuario.getCorreo(), usuario.getNombre(), enlace);
        });
        // Nota: no lanzamos error si el correo no existe, para no revelar
        // qué correos están registrados (buena práctica de seguridad).
    }

    public void resetearPassword(String token, String nuevaPassword) {
        PasswordResetToken resetToken = tokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Token inválido"));

        if (Boolean.TRUE.equals(resetToken.getUsado())) {
            throw new RuntimeException("Este enlace ya fue utilizado");
        }
        if (resetToken.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("El enlace ha expirado, solicita uno nuevo");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setPassword(passwordEncoder.encode(nuevaPassword));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        tokenRepository.save(resetToken);
    }
}