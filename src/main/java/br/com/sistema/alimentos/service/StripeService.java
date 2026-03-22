package br.com.sistema.alimentos.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import br.com.sistema.alimentos.dtos.request.CheckoutRequest;
import br.com.sistema.alimentos.dtos.response.CheckoutResponse;
import br.com.sistema.alimentos.entity.Usuario;
// PlanoTipo removido: sistema usa apenas o plano PADRAO
import br.com.sistema.alimentos.repository.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final UsuarioRepository usuarioRepository;

        @Value("${stripe.price.padrao}")
        private String stripePricePadrao;

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
        validarPriceIdPadrao(stripePricePadrao);
    }

    // ====================================================
    // criarCheckoutSession - Cria uma sessão de pagamento Stripe Checkout
    // ====================================================
    @Transactional
    public CheckoutResponse criarCheckoutSession(UUID usuarioId, CheckoutRequest request) {
        try {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

            String customerId = obterOuCriarCustomer(usuario);

            String priceId = obterPriceIdPadraoValidado();

            com.stripe.model.checkout.Session session = com.stripe.model.checkout.Session.create(
                    SessionCreateParams.builder()
                            .setCustomer(customerId)
                            .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                            .setSuccessUrl(request.successUrl() + "?session_id={CHECKOUT_SESSION_ID}")
                            .setCancelUrl(request.cancelUrl())
                            .addLineItem(SessionCreateParams.LineItem.builder()
                                    .setPrice(priceId)
                                    .setQuantity(1L)
                                    .build())
                            .build()
            );

            return new CheckoutResponse(session.getUrl());

        } catch (StripeException e) {
            throw new RuntimeException("Erro ao criar sessão de pagamento: " + e.getMessage(), e);
        }
    }

    // ====================================================
    // criarPortalSession - Cria sessão do Portal do Cliente Stripe para gerenciar assinatura
    // ====================================================
    public CheckoutResponse criarPortalSession(UUID usuarioId, String returnUrl) {
        try {
            Usuario usuario = usuarioRepository.findById(usuarioId)
                    .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));

            if (usuario.getStripeCustomerId() == null) {
                throw new IllegalStateException("Usuário não possui assinatura ativa");
            }

            com.stripe.model.billingportal.Session session =
                    com.stripe.model.billingportal.Session.create(
                            com.stripe.param.billingportal.SessionCreateParams.builder()
                                    .setCustomer(usuario.getStripeCustomerId())
                                    .setReturnUrl(returnUrl)
                                    .build()
                    );

            return new CheckoutResponse(session.getUrl());

        } catch (StripeException e) {
            throw new RuntimeException("Erro ao criar portal do cliente: " + e.getMessage(), e);
        }
    }

    // ====================================================
    // processarWebhook - Processa eventos enviados pelo Stripe via webhook
    // ====================================================
    @Transactional
    public void processarWebhook(String payload, String sigHeader) {
        try {
            com.stripe.model.Event event =
                    com.stripe.net.Webhook.constructEvent(payload, sigHeader, webhookSecret);

            switch (event.getType()) {
                case "checkout.session.completed" -> processarPagamentoConcluido(event);
                case "customer.subscription.deleted" -> processarAssinaturaCancelada(event);
                default -> { /* evento não tratado */ }
            }

        } catch (com.stripe.exception.SignatureVerificationException e) {
            throw new IllegalArgumentException("Assinatura do webhook inválida", e);
        } catch (StripeException e) {
            throw new RuntimeException("Erro ao processar webhook: " + e.getMessage(), e);
        }
    }

    private void processarPagamentoConcluido(com.stripe.model.Event event) {
        // Implementar lógica de ativação do plano após pagamento confirmado
    }

    private void processarAssinaturaCancelada(com.stripe.model.Event event) {
        // Implementar lógica de reversão para o estado após cancelamento
    }

    private String obterPriceIdPadraoValidado() {
        validarPriceIdPadrao(stripePricePadrao);
        return stripePricePadrao;
    }

    private void validarPriceIdPadrao(String priceId) {
        if (priceId == null || priceId.isBlank()) {
            throw new IllegalStateException("Configuração inválida: 'stripe.price.padrao' não foi definida");
        }

        if (!priceId.startsWith("price_")) {
            throw new IllegalStateException("Configuração inválida: 'stripe.price.padrao' deve ser um Price ID do Stripe (ex.: price_...). Valor atual parece ser um Product ID");
        }
    }

    private String obterOuCriarCustomer(Usuario usuario) throws StripeException {
        if (usuario.getStripeCustomerId() != null) {
            return usuario.getStripeCustomerId();
        }

        Customer customer = Customer.create(
                CustomerCreateParams.builder()
                        .setEmail(usuario.getEmail())
                        .setName(usuario.getNome())
                        .build()
        );

        usuario.setStripeCustomerId(customer.getId());
        usuarioRepository.save(usuario);

        return customer.getId();
    }
}
