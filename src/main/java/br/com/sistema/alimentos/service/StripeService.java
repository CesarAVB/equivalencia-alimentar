package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.CheckoutRequest;
import br.com.sistema.alimentos.dtos.response.CheckoutResponse;
import br.com.sistema.alimentos.entity.Usuario;
import br.com.sistema.alimentos.enums.PlanoTipo;
import br.com.sistema.alimentos.repository.UsuarioRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.billing.Session;
import com.stripe.model.checkout.SessionCreateParams;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.billing.SessionCreateParams.FlowData;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.api.key}")
    private String stripeApiKey;

    @Value("${stripe.webhook.secret}")
    private String webhookSecret;

    private final UsuarioRepository usuarioRepository;

    // Mapeamento de plano → Price ID do Stripe (configurar no Dashboard Stripe)
    private static final Map<PlanoTipo, String> PRICE_IDS = Map.of(
            PlanoTipo.BASIC, "price_BASIC_CONFIGURE_NO_STRIPE",
            PlanoTipo.PRO,   "price_PRO_CONFIGURE_NO_STRIPE"
    );

    @PostConstruct
    public void init() {
        Stripe.apiKey = stripeApiKey;
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

            String priceId = PRICE_IDS.get(request.plano());
            if (priceId == null) {
                throw new IllegalArgumentException("Plano FREE não requer pagamento");
            }

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
        // Implementar lógica de reversão para plano FREE após cancelamento
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
