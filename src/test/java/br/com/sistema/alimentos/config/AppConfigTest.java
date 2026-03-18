package br.com.sistema.alimentos.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AppConfigTest {

    @Test
    @DisplayName("Deve criar password encoder bcrypt")
    void deveCriarPasswordEncoderBcrypt() {
        AppConfig config = new AppConfig();

        PasswordEncoder encoder = config.passwordEncoder();

        assertNotNull(encoder);
        assertTrue(encoder.matches("123456", encoder.encode("123456")));
    }
}
