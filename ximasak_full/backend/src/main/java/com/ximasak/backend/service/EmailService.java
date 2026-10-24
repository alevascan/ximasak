package com.ximasak.backend.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void enviarCorreoRecuperacion(String destinatario, String nombre, String enlaceReset) {
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("Recuperación de contraseña - Ximasak");
            mensaje.setText(
                    "Hola " + nombre + ",\n\n" +
                    "Recibimos una solicitud para restablecer tu contraseña.\n" +
                    "Haz clic en el siguiente enlace (válido por 30 minutos):\n\n" +
                    enlaceReset + "\n\n" +
                    "Si no solicitaste este cambio, ignora este correo.\n\n" +
                    "Equipo Ximasak"
            );
            mailSender.send(mensaje);
        } catch (Exception e) {
            System.err.println("Aviso: No se pudo enviar el correo de recuperación: " + e.getMessage());
        }
    }

    public void enviarNotificacionOrdenAtendida(String destinatario, String nombreCliente, String servicio, String detalle) {
        if (destinatario == null || destinatario.trim().isEmpty() || !destinatario.contains("@")) {
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setTo(destinatario);
            mensaje.setSubject("¡Tu equipo está listo para entrega! - Ximasak EIRL");
            mensaje.setText(
                    "Estimado(a) " + (nombreCliente != null ? nombreCliente : "Cliente") + ",\n\n" +
                    "Le informamos que el servicio técnico para su equipo ha sido completado con éxito:\n\n" +
                    "• Servicio: " + (servicio != null ? servicio : "Mantenimiento / Reparación") + "\n" +
                    "• Detalle de atención: " + (detalle != null ? detalle : "Revisión técnica finalizada") + "\n" +
                    "• Estado: ATENDIDA / LISTO PARA RECOJO\n\n" +
                    "Puede acercarse a nuestra tienda en Ximasak EIRL para realizar el retiro de su equipo.\n\n" +
                    "Atentamente,\n" +
                    "Área de Soporte Técnico - Ximasak EIRL"
            );
            mailSender.send(mensaje);
            System.out.println("Correo de orden atendida enviado exitosamente a: " + destinatario);
        } catch (Exception e) {
            System.err.println("Aviso: No se pudo enviar el correo de orden atendida: " + e.getMessage());
        }
    }
}
