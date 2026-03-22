package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.response.EquivalenciaResponse;
import br.com.sistema.alimentos.service.EquivalenciaService;
import br.com.sistema.alimentos.service.JwtService;
import br.com.sistema.alimentos.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EquivalenciaController.class)
@AutoConfigureMockMvc(addFilters = false)
class EquivalenciaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EquivalenciaService equivalenciaService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Deve retornar 200 quando listar equivalências")
    void deveRetornarOkQuandoListarEquivalencias() throws Exception {
        EquivalenciaResponse response = new EquivalenciaResponse(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                1,
                "Banana",
                2,
                "Maçã",
                new BigDecimal("1.20"),
                "Troca comum",
                LocalDateTime.now()
        );

        when(equivalenciaService.listar(any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/equivalencias"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].alimentoOrigemDescricao").value("Banana"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    @DisplayName("Deve retornar 422 quando criar equivalência com payload inválido")
    void deveRetornar422QuandoCriarEquivalenciaComPayloadInvalido() throws Exception {
        String payload = """
                {
                  "alimentoOrigemId": null,
                  "alimentoDestinoId": null,
                  "fatorEquivalencia": 0
                }
                """;

        mockMvc.perform(post("/api/v1/equivalencias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("fator")));
    }
}
