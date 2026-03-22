package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.response.UsuarioResponse;
import br.com.sistema.alimentos.enums.PlanoTipo;
import br.com.sistema.alimentos.enums.UsuarioTipo;
import br.com.sistema.alimentos.service.JwtService;
import br.com.sistema.alimentos.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsuarioController.class)
@AutoConfigureMockMvc(addFilters = false)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar 200 quando listar usuários")
    void deveRetornarOkQuandoListarUsuarios() throws Exception {
        UsuarioResponse response = new UsuarioResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "Maria",
                "maria@email.com",
                "123.456.789-00",
                UsuarioTipo.ADMIN,
                true,
                PlanoTipo.PADRAO,
                null,
                LocalDateTime.now()
        );

        when(usuarioService.listar()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("maria@email.com"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar 422 quando criar usuário com payload inválido")
    void deveRetornar422QuandoCriarUsuarioComPayloadInvalido() throws Exception {
        String payload = """
                {
                  "nome": "",
                  "email": "invalido",
                  "cpf": "123",
                  "senha": "123",
                  "tipo": null
                }
                """;

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("senha")));
    }
}
