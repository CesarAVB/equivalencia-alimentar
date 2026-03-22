package br.com.sistema.alimentos.controller;

import br.com.sistema.alimentos.dtos.request.CheckoutRequest;
import br.com.sistema.alimentos.dtos.response.CheckoutResponse;
import br.com.sistema.alimentos.entity.Usuario;
import br.com.sistema.alimentos.service.StripeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pagamentos")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Integração com Stripe para assinaturas e pagamentos")
public class PagamentoController {

    private final StripeService stripeService;

    // ====================================================
    // checkout - Cria sessão de pagamento Stripe Checkout para o usuário autenticado
    // ====================================================
    @PostMapping("/checkout")
    @Operation(summary = "Criar sessão de checkout no Stripe")
    public ResponseEntity<CheckoutResponse> checkout(@AuthenticationPrincipal Usuario usuario, @RequestBody @Valid CheckoutRequest request) {
        return ResponseEntity.ok(stripeService.criarCheckoutSession(usuario.getId(), request));
    }

    // ====================================================
    // portal - Cria sessão do Portal do Cliente para gerenciar assinatura
    // ====================================================
    @GetMapping("/portal")
    @Operation(summary = "Criar sessão do portal do cliente Stripe")
    public ResponseEntity<CheckoutResponse> portal(@AuthenticationPrincipal Usuario usuario, @RequestParam String returnUrl) {
        return ResponseEntity.ok(stripeService.criarPortalSession(usuario.getId(), returnUrl));
    }

    // ====================================================
    // webhook - Recebe e processa eventos enviados pelo Stripe (endpoint público)
    // ====================================================
    @PostMapping("/webhook")
    @Operation(summary = "Webhook do Stripe para processar eventos de pagamento", hidden = true)
    public ResponseEntity<Void> webhook(@RequestBody String payload, @RequestHeader("Stripe-Signature") String sigHeader) {
        stripeService.processarWebhook(payload, sigHeader);
        return ResponseEntity.ok().build();
    }
}
