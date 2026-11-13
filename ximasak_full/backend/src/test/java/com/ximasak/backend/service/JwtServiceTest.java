package com.ximasak.backend.service;

import com.ximasak.backend.entity.Rol;
import com.ximasak.backend.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @Test
    @DisplayName("CP-UT-25: Generar token JWT con formato estándar de 3 segmentos")
    void testGenerarToken_FormatoJwtValido() {
        Rol rol = new Rol();
        rol.setNombre("ADMIN");

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(1);
        usuario.setNombre("Matias Admin");
        usuario.setCorreo("tasillamatias@gmail.com");
        usuario.setRol(rol);

        String token = jwtService.generarToken(usuario);

        assertNotNull(token);
        String[] partes = token.split("\\.");
        assertEquals(3, partes.length, "El JWT debe contener Header, Payload y Signature");
    }
}
