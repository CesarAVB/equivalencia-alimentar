package br.com.sistema.alimentos.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import io.jsonwebtoken.ExpiredJwtException;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    @Test
    @DisplayName("Deve gerar token e extrair e-mail corretamente")
    void deveGerarTokenEExtrairEmailCorretamente() {
        JwtService jwtService = novoJwtService(3_600_000L);
        UserDetails user = User.withUsername("user@email.com").password("x").authorities("ROLE_ADMIN").build();

        String token = jwtService.gerarToken(user);
        String email = jwtService.extrairEmail(token);

        assertEquals("user@email.com", email);
    }

    @Test
    @DisplayName("Deve validar token quando usuário for o mesmo e token estiver válido")
    void deveValidarTokenQuandoUsuarioForMesmoETokenValido() {
        JwtService jwtService = novoJwtService(3_600_000L);
        UserDetails user = User.withUsername("user@email.com").password("x").authorities("ROLE_ADMIN").build();

        String token = jwtService.gerarToken(user);

        assertTrue(jwtService.validarToken(token, user));
    }

    @Test
    @DisplayName("Deve retornar falso na validação quando usuário for diferente")
    void deveRetornarFalsoNaValidacaoQuandoUsuarioForDiferente() {
        JwtService jwtService = novoJwtService(3_600_000L);
        UserDetails userGerador = User.withUsername("user@email.com").password("x").authorities("ROLE_ADMIN").build();
        UserDetails outroUsuario = User.withUsername("outro@email.com").password("x").authorities("ROLE_ADMIN").build();

        String token = jwtService.gerarToken(userGerador);

        assertFalse(jwtService.validarToken(token, outroUsuario));
    }

    @Test
    @DisplayName("Deve lançar exceção na validação quando token estiver expirado")
    void deveLancarExcecaoNaValidacaoQuandoTokenEstiverExpirado() {
        JwtService jwtService = novoJwtService(-1L);
        UserDetails user = User.withUsername("user@email.com").password("x").authorities("ROLE_ADMIN").build();

        String token = jwtService.gerarToken(user);

        assertThrows(ExpiredJwtException.class, () -> jwtService.validarToken(token, user));
    }

    @Test
    @DisplayName("Deve gerar token com secret em texto puro")
    void deveGerarTokenComSecretTextoPuro() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "12345678901234567890123456789012");
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L);

        UserDetails user = User.withUsername("plain@email.com").password("x").authorities("ROLE_ADMIN").build();

        String token = jwtService.gerarToken(user);

        assertEquals("plain@email.com", jwtService.extrairEmail(token));
    }

    @Test
    @DisplayName("Deve falhar quando secret tiver menos de 32 bytes")
    void deveFalharQuandoSecretMuitoCurto() {
        JwtService jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", "1234567890123456789012345678901");
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L);

        UserDetails user = User.withUsername("short@email.com").password("x").authorities("ROLE_ADMIN").build();

        assertThrows(IllegalStateException.class, () -> jwtService.gerarToken(user));
    }

    private static JwtService novoJwtService(long expiration) {
        JwtService jwtService = new JwtService();
        String secret = Base64.getEncoder().encodeToString("12345678901234567890123456789012".getBytes());
        ReflectionTestUtils.setField(jwtService, "secret", secret);
        ReflectionTestUtils.setField(jwtService, "expiration", expiration);
        return jwtService;
    }
}
