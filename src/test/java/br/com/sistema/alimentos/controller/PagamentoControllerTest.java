package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.service.JwtService;
import br.com.sistema.alimentos.service.StripeService;
import br.com.sistema.alimentos.service.UsuarioService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PagamentoController.class)
@AutoConfigureMockMvc(addFilters = false)
class PagamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StripeService stripeService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    @DisplayName("Deve retornar 200 quando processar webhook com assinatura")
    void deveRetornarOkQuandoProcessarWebhookComAssinatura() throws Exception {
        doNothing().when(stripeService).processarWebhook("{}", "sig-123");

        mockMvc.perform(post("/api/v1/pagamentos/webhook")
                        .contentType("application/json")
                        .content("{}")
                        .header("Stripe-Signature", "sig-123"))
                .andExpect(status().isOk());
    }
}
