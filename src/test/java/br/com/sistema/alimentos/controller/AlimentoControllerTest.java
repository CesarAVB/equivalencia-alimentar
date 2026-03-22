package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.response.AlimentoResponse;
import br.com.sistema.alimentos.enums.GrupoAlimentar;
import br.com.sistema.alimentos.service.AlimentoService;
import br.com.sistema.alimentos.service.JwtService;
import br.com.sistema.alimentos.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlimentoController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlimentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlimentoService alimentoService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Deve retornar 200 quando listar alimentos")
    void deveRetornarOkQuandoListarAlimentos() throws Exception {
        AlimentoResponse response = new AlimentoResponse(
                1,
                "A001",
                GrupoAlimentar.FRUTAS,
                "Banana",
                new BigDecimal("89.5"),
                LocalDateTime.now()
        );

        when(alimentoService.listar(eq(null), eq(null), any())).thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/v1/alimentos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].descricao").value("Banana"));
    }

    @Test
    @DisplayName("Deve retornar 422 quando criar alimento com payload inválido")
    void deveRetornar422QuandoCriarAlimentoComPayloadInvalido() throws Exception {
        String payload = """
                {
                  "codigoSubstituicao": "",
                  "descricao": "",
                  "energiaKcal": -1
                }
                """;

        mockMvc.perform(post("/api/v1/alimentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("descrição")));
    }
}
