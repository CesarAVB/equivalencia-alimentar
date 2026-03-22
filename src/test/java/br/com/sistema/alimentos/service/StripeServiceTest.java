package br.com.sistema.alimentos.service;

import br.com.sistema.alimentos.dtos.request.CheckoutRequest;
import br.com.sistema.alimentos.dtos.response.CheckoutResponse;
import br.com.sistema.alimentos.entity.Usuario;
import br.com.sistema.alimentos.enums.PlanoTipo;
import br.com.sistema.alimentos.enums.UsuarioTipo;
import br.com.sistema.alimentos.repository.UsuarioRepository;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StripeServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(stripeService, "stripePricePadrao", "price_test_123");
    }

    @Test
    @DisplayName("Deve inicializar Stripe API key")
    void deveInicializarStripeApiKey() {
        ReflectionTestUtils.setField(stripeService, "stripeApiKey", "sk_test_123");
        ReflectionTestUtils.setField(stripeService, "stripePricePadrao", "price_test_123");

        stripeService.init();

        assertEquals("sk_test_123", Stripe.apiKey);
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar checkout quando usuário não existir")
    void deveLancarExcecaoAoCriarCheckoutQuandoUsuarioNaoExistir() {
        UUID usuarioId = UUID.randomUUID();
        CheckoutRequest request = new CheckoutRequest("https://ok", "https://cancel");
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> stripeService.criarCheckoutSession(usuarioId, request));
    }

    

    @Test
    @DisplayName("Deve criar checkout session quando dados forem válidos")
    void deveCriarCheckoutSessionQuandoDadosForemValidos() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, "cus_123");
        CheckoutRequest request = new CheckoutRequest("https://ok", "https://cancel");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));

        com.stripe.model.checkout.Session session = new com.stripe.model.checkout.Session();
        session.setUrl("https://checkout.stripe.com/session");

        try (MockedStatic<com.stripe.model.checkout.Session> sessionMock = mockStatic(com.stripe.model.checkout.Session.class)) {
            sessionMock.when(() -> com.stripe.model.checkout.Session.create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenReturn(session);

            CheckoutResponse response = stripeService.criarCheckoutSession(usuarioId, request);

            assertEquals("https://checkout.stripe.com/session", response.url());
        }
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar portal quando usuário não existir")
    void deveLancarExcecaoAoCriarPortalQuandoUsuarioNaoExistir() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> stripeService.criarPortalSession(usuarioId, "https://retorno"));
    }

    @Test
    @DisplayName("Deve lançar exceção ao criar portal quando usuário não tiver stripeCustomerId")
    void deveLancarExcecaoAoCriarPortalQuandoUsuarioSemStripeCustomerId() {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario(usuarioId, null)));

        assertThrows(IllegalStateException.class, () -> stripeService.criarPortalSession(usuarioId, "https://retorno"));
    }

    @Test
    @DisplayName("Deve criar portal session quando usuário tiver customer id")
    void deveCriarPortalSessionQuandoUsuarioTiverCustomerId() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario(usuarioId, "cus_999")));

        com.stripe.model.billingportal.Session session = new com.stripe.model.billingportal.Session();
        session.setUrl("https://billing.stripe.com/session");

        try (MockedStatic<com.stripe.model.billingportal.Session> sessionMock = mockStatic(com.stripe.model.billingportal.Session.class)) {
            sessionMock.when(() -> com.stripe.model.billingportal.Session.create(any(com.stripe.param.billingportal.SessionCreateParams.class)))
                    .thenReturn(session);

            CheckoutResponse response = stripeService.criarPortalSession(usuarioId, "https://retorno");

            assertEquals("https://billing.stripe.com/session", response.url());
        }
    }

    @Test
    @DisplayName("Deve lançar exceção de assinatura inválida no webhook")
    void deveLancarExcecaoDeAssinaturaInvalidaNoWebhook() {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "whsec_123");

        try (MockedStatic<com.stripe.net.Webhook> webhookMock = mockStatic(com.stripe.net.Webhook.class)) {
            webhookMock.when(() -> com.stripe.net.Webhook.constructEvent("{}", "sig", "whsec_123"))
                    .thenThrow(new SignatureVerificationException("assinatura inválida", "sig"));

            assertThrows(IllegalArgumentException.class, () -> stripeService.processarWebhook("{}", "sig"));
        }
    }

    @Test
    @DisplayName("Deve ignorar evento não tratado no webhook")
    void deveIgnorarEventoNaoTratadoNoWebhook() throws Exception {
        ReflectionTestUtils.setField(stripeService, "webhookSecret", "whsec_123");
        Event event = new Event();
        event.setType("invoice.created");

        try (MockedStatic<com.stripe.net.Webhook> webhookMock = mockStatic(com.stripe.net.Webhook.class)) {
            webhookMock.when(() -> com.stripe.net.Webhook.constructEvent("{}", "sig", "whsec_123"))
                    .thenReturn(event);

            stripeService.processarWebhook("{}", "sig");

            verify(usuarioRepository, never()).save(any(Usuario.class));
        }
    }

    @Test
    @DisplayName("Deve criar customer quando usuário não tiver stripeCustomerId")
    void deveCriarCustomerQuandoUsuarioNaoTiverStripeCustomerId() throws Exception {
        UUID usuarioId = UUID.randomUUID();
        Usuario usuario = usuario(usuarioId, null);
        CheckoutRequest request = new CheckoutRequest("https://ok", "https://cancel");

        when(usuarioRepository.findById(usuarioId)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Customer customer = mock(Customer.class);
        when(customer.getId()).thenReturn("cus_novo");

        com.stripe.model.checkout.Session session = new com.stripe.model.checkout.Session();
        session.setUrl("https://checkout.stripe.com/session2");

        try (MockedStatic<Customer> customerMock = mockStatic(Customer.class);
             MockedStatic<com.stripe.model.checkout.Session> sessionMock = mockStatic(com.stripe.model.checkout.Session.class)) {

            customerMock.when(() -> Customer.create(any(com.stripe.param.CustomerCreateParams.class))).thenReturn(customer);
            sessionMock.when(() -> com.stripe.model.checkout.Session.create(any(com.stripe.param.checkout.SessionCreateParams.class)))
                    .thenReturn(session);

            CheckoutResponse response = stripeService.criarCheckoutSession(usuarioId, request);

            assertEquals("https://checkout.stripe.com/session2", response.url());
            assertEquals("cus_novo", usuario.getStripeCustomerId());
            verify(usuarioRepository).save(usuario);
        }
    }

    private static Usuario usuario(UUID id, String stripeCustomerId) {
        return Usuario.builder()
                .id(id)
                .nome("Usuario")
                .email("usuario@email.com")
                .senha("senha")
                .tipo(UsuarioTipo.ADMIN)
                .plano(PlanoTipo.PADRAO)
                .ativo(true)
                .stripeCustomerId(stripeCustomerId)
                .build();
    }
}
