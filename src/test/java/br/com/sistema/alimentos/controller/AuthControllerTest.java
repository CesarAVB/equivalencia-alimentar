package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.request.LoginRequest;
import br.com.sistema.alimentos.dtos.response.LoginResponse;
import br.com.sistema.alimentos.enums.PlanoTipo;
import br.com.sistema.alimentos.enums.UsuarioTipo;
import br.com.sistema.alimentos.service.AuthService;
import br.com.sistema.alimentos.service.JwtService;
import br.com.sistema.alimentos.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private UsuarioService usuarioService;

    @Test
    @DisplayName("Deve retornar 200 quando credenciais forem válidas")
    void deveRetornarOkQuandoCredenciaisValidas() throws Exception {
        LoginResponse response = new LoginResponse(
                "token-jwt",
                "Bearer",
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Maria",
                "maria@email.com",
                UsuarioTipo.ADMIN,
                "padrao",
                null
        );

        when(authService.autenticar(any(LoginRequest.class))).thenReturn(response);

        String payload = """
                {
                  "email": "maria@email.com",
                  "senha": "123456"
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("token-jwt"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.email").value("maria@email.com"));
    }

    @Test
    @DisplayName("Deve retornar 422 quando payload de login for inválido")
    void deveRetornar422QuandoPayloadLoginInvalido() throws Exception {
        String payload = """
                {
                  "email": "",
                  "senha": ""
                }
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.message").value(containsString("senha")));
    }
}
