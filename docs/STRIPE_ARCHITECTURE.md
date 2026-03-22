# 🏗️ Arquitetura do Sistema Stripe

## Diagrama de Classes

```
┌─────────────────────────────────────────────────────────────────┐
│                         FRONTEND (Angular)                       │
│                    Componente de Pagamento                       │
└──────────────────────────────────┬──────────────────────────────┘
                                   │
                    POST /api/v1/pagamentos/checkout
                    GET  /api/v1/pagamentos/portal
                    POST /api/v1/pagamentos/webhook
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                   PagamentoController                            │
│                                                                   │
│  + checkout(usuario, request): ResponseEntity<CheckoutResponse> │
│  + portal(usuario, returnUrl): ResponseEntity<CheckoutResponse> │
│  + webhook(payload, sigHeader): ResponseEntity<Void>            │
│                                                                   │
│  @AuthenticationPrincipal usuario: Usuario                       │
│  @Inject stripeService: StripeService                           │
└──────────────────────────────────┬──────────────────────────────┘
                                   │
                                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                       StripeService                              │
│                   (Lógica de Negócio)                           │
│                                                                   │
│  + init(): void                          [@PostConstruct]       │
│  + criarCheckoutSession(usuarioId, req) → CheckoutResponse    │
│  + criarPortalSession(usuarioId, url)  → CheckoutResponse    │
│  + processarWebhook(payload, sig)      → void                │
│  - obterOuCriarCustomer(usuario)       → String (customerId)  │
│  - processarPagamentoConcluido(event)  → void                │
│  - processarAssinaturaCancelada(event) → void                │
│                                                                   │
│  @Inject usuarioRepository: UsuarioRepository                   │
│  @Value stripe.api.key: String                                  │
│  @Value stripe.webhook.secret: String                           │
│  @Value stripe.price.basic: String                              │
│  @Value stripe.price.pro: String                                │
│  priceIds: Map<PlanoTipo, String>                              │
└──────────┬───────────────────────────────┬──────────────────────┘
           │                               │
           ▼ (salva/busca usuario)         ▼ (chama API)
     ┌──────────────┐              ┌──────────────────┐
     │ UsuarioRepo  │              │ Stripe API Java  │
     │  - findById  │              │                  │
     │  - save      │              │ Customer.create  │
     └──────────────┘              │ Session.create   │
                                   │ Event.construct  │
                                   └──────────────────┘
                                          │
                                          ▼
                                   ┌──────────────────┐
                                   │  Stripe Cloud    │
                                   │  (Pagamentos)    │
                                   └──────────────────┘
           │
           ▼
     ┌──────────────────┐
     │ Usuario (Entity) │
     │                  │
     │ id: UUID         │
     │ nome: String     │
     │ email: String    │
     │ cpf: String      │
     │ senha: String    │
     │ tipo: UsuarioTipo│
     │ ativo: boolean   │
     │ ────────────────│
     │ stripeCustId*   │  ← Preenchido por StripeService
     │ plano*          │  ← Atualizado por webhook
     │ planoExpiraEm*  │  ← Atualizado por webhook
     │ ────────────────│
     │ createdAt       │
     │ updatedAt       │
     └──────────────────┘
```

---

## Diagrama de Fluxo de Checkout

```
┌─────────────────────────────────────────────────────────────────┐
│ FASE 1: Frontend solicita Checkout                               │
└─────────────────────────────────────────────────────────────────┘

   Frontend (Angular)
        │
        │ POST /pagamentos/checkout
        │ {plano: BASIC, successUrl, cancelUrl}
        │
        ▼
   PagamentoController.checkout()
        │
        ├─ @AuthenticationPrincipal injeta usuario
        │
        ├─ @RequestBody injeta CheckoutRequest
        │
        └─► stripeService.criarCheckoutSession(usuarioId, request)
                │
                │
                ├─ usuarioRepository.findById(usuarioId)
                │
                ├─ obterOuCriarCustomer(usuario)
                │  │
                │  ├─ if (usuario.stripeCustId != null)
                │  │   return usuario.stripeCustId
                │  │
                │  └─ else
                │      ├─ Customer.create(email, name)  [API Stripe]
                │      │
                │      ├─ usuario.setStripeCustId(customer.getId())
                │      │
                │      └─ usuarioRepository.save(usuario)
                │
                ├─ priceIds.get(request.plano())
                │
                └─ Session.create(customerId, priceId)  [API Stripe]
                    │
                    │ Retorna: session.getUrl()
                    │
                    ▼
              CheckoutResponse(url)
                    │
                    ▼
              Frontend recebe URL
                    │
                    ▼
              window.location.href = url
                    │
                    ▼
              Browser abre Stripe Checkout


┌─────────────────────────────────────────────────────────────────┐
│ FASE 2: Usuário paga no Stripe                                   │
└─────────────────────────────────────────────────────────────────┘

   Browser → https://checkout.stripe.com/pay/cs_test_...
        │
        ├─ Usuário insere email
        │
        ├─ Usuário insere dados do cartão
        │
        ├─ Usuário clica "Pay"
        │
        ▼
   Stripe processa pagamento
        │
        ├─ Se aprovado: cria Subscription
        │
        ├─ Se rejeitado: volta para formulário
        │
        ▼ (sucesso)
   Stripe envia webhook "checkout.session.completed"


┌─────────────────────────────────────────────────────────────────┐
│ FASE 3: Backend processa Webhook                                 │
└─────────────────────────────────────────────────────────────────┘

   Stripe Cloud
        │
        │ POST /api/v1/pagamentos/webhook
        │ Headers: Stripe-Signature
        │ Body: payload (JSON do evento)
        │
        ▼
   PagamentoController.webhook(payload, sigHeader)
        │
        └─► stripeService.processarWebhook(payload, sigHeader)
                │
                ├─ Webhook.constructEvent(payload, sig, secret)
                │  │
                │  └─ Valida assinatura (segurança)
                │
                ├─ switch(event.getType())
                │
                ├─ case "checkout.session.completed"
                │  │
                │  └─► processarPagamentoConcluido(event)
                │      │
                │      ├─ event.getDataObjectDeserializer()
                │      │   .getObject() [Desserializa sessão]
                │      │
                │      ├─ Busca usuário pelo customerId
                │      │
                │      ├─ usuario.setPlano(BASIC)
                │      │
                │      ├─ usuario.setPlanoExpiraEm(agora + 1 mês)
                │      │
                │      └─ usuarioRepository.save()
                │
                └─ return 200 OK


┌─────────────────────────────────────────────────────────────────┐
│ FASE 4: Frontend é redirecionado                                 │
└─────────────────────────────────────────────────────────────────┘

   Stripe → window.location.href = successUrl + "?session_id=..."
        │
        ▼
   Browser volta para: http://localhost:4200/sucesso?session_id=...
        │
        ▼
   Frontend mostra "Pagamento aprovado!"
        │
        ▼
   Usuario.plano = BASIC ✅
```

---

## Diagrama de Fluxo de Portal

```
Frontend → GET /api/v1/pagamentos/portal?returnUrl=...
        │
        ▼
Controller → StripeService.criarPortalSession()
        │
        ├─ usuarioRepository.findById()
        │
        ├─ if (usuario.stripeCustId == null)
        │  └─ throw IllegalStateException
        │
        └─ BillingPortal.Session.create(customerId)
            │
            │ Retorna: session.getUrl()
            │
            ▼
        CheckoutResponse(url)
            │
            ▼
        Frontend recebe URL
            │
            ▼
        window.location.href = url
            │
            ▼
        Abre https://billing.stripe.com/b/abc123...
            │
            ▼
        Usuário pode:
        ├─ Mudar plano (BASIC → PRO ou PRO → FREE)
        │  └─ Stripe envia webhook (subscription.updated)
        │
        ├─ Cancelar assinatura
        │  └─ Stripe envia webhook (subscription.deleted)
        │
        └─ Ver fatura histórica
            │
            ▼
        Clica "Return to [app name]"
            │
            ▼
        Browser volta para: returnUrl
```

---

## Diagrama de Entidades do BD

```
┌─────────────────────────────────────────────────────┐
│                    usuarios                          │
├─────────────────────────────────────────────────────┤
│ id (PK)              │ VARCHAR(36)   │ UUID          │
│ nome                 │ VARCHAR(255)  │ João Silva    │
│ email (UNIQUE)       │ VARCHAR(255)  │ joao@ex.com   │
│ cpf (UNIQUE)         │ VARCHAR(14)   │ 123.456.789-0 │
│ senha                │ VARCHAR(255)  │ (hash)        │
│ tipo                 │ VARCHAR(50)   │ ADMIN/USUARIO │
│ ativo                │ TINYINT(1)    │ 1             │
├─────────────────────────────────────────────────────┤
│ stripe_customer_id   │ VARCHAR(255)  │ cus_123... ◄─ STRIPE |
│ plano                │ VARCHAR(50)   │ BASIC ◄────── STRIPE |
│ plano_expira_em      │ DATETIME      │ 2024-04-22 ◄ STRIPE |
├─────────────────────────────────────────────────────┤
│ created_at           │ DATETIME      │ 2024-03-22    │
│ updated_at           │ DATETIME      │ 2024-03-22    │
└─────────────────────────────────────────────────────┘

Campos Stripe (* = preenchidos/atualizados por StripeService):

1. stripe_customer_id
   ├─ Preenchido quando: obterOuCriarCustomer()
   ├─ ID do cliente no Stripe
   ├─ Exemplo: "cus_5hv9P8qy5ZhP9e"
   └─ Nunca muda (é ID imutável)

2. plano
   ├─ Atualizado quando: processarWebhook() detecta pagamento
   ├─ Valores: FREE, BASIC, PRO
   ├─ Muda quando: usuário faz upgrade ou downgrade
   └─ Revertido para FREE quando: subscription.deleted

3. plano_expira_em
   ├─ Atualizado quando: processarWebhook() detecta pagamento
   ├─ Data de vencimento da assinatura
   ├─ Incrementado: +1 mês a cada renovação
   └─ Limpo quando: subscription.deleted (NULL)
```

---

## Integração com Stripe Cloud

```
┌──────────────────────┐
│   Backend Java       │
│                      │
│  Tem: stripe.api.key │
│        stripe.*      │
│                      │
└──────┬───────────────┘
       │
       │ HTTPS
       │ (encriptado)
       │
       ▼
┌──────────────────────────────────────────┐
│         Stripe Cloud API                  │
│                                            │
│  POST /v1/customers                       │
│  POST /v1/checkout/sessions               │
│  POST /v1/billing_portal/sessions         │
│  GET /v1/events                           │
│                                            │
│  Processa:                                 │
│  ├─ Pagamentos com cartão                 │
│  ├─ Assinaturas recorrentes               │
│  ├─ Webhooks para seu servidor            │
│  └─ Segurança e conformidade (PCI-DSS)    │
└──────────────────────────────────────────┘
       │
       │ Webhook (POST)
       │ Headers: Stripe-Signature
       │
       ▼
┌──────────────────────┐
│   Backend Java       │
│                      │
│  /pagamentos/webhook │
│  (verifica signature)│
│  (processa evento)   │
│  (atualiza usuario)  │
│                      │
└──────────────────────┘
```

---

## Ciclo de Vida de um Cliente Stripe

```
1. Novo usuário cria conta
   └─ No BD: stripe_customer_id = NULL, plano = FREE

2. Usuário clica "Upgrade para BASIC"
   └─ Frontend: POST /pagamentos/checkout {plano: BASIC, ...}

3. Backend cria Customer no Stripe
   └─ Stripe retorna: customer.id = "cus_..."
   └─ Backend salva: usuario.stripe_customer_id = "cus_..."

4. Backend cria Session de Checkout
   └─ Stripe retorna: session.url = "https://checkout.stripe.com/..."

5. Frontend redireciona usuário para Stripe
   └─ Browser abre: https://checkout.stripe.com/...

6. Usuário paga
   └─ Stripe processa pagamento
   └─ Cria Subscription (recorrência mensal)

7. Stripe envia webhook "checkout.session.completed"
   └─ Backend recebe: event.type = "checkout.session.completed"
   └─ Backend extrai: customer_id, subscription_id
   └─ Backend atualiza: usuario.plano = BASIC
   └─ Backend atualiza: usuario.plano_expira_em = agora + 1 mês

8. Usuário volta para o app
   └─ plano = BASIC ✅

9. Todo mês, Stripe renova automaticamente
   └─ Processa novo pagamento
   └─ (opcional) Poderia enviar webhook para atualizar data

10. Usuário cancela
    └─ Abre Portal Stripe
    └─ Clica "Cancel"
    └─ Stripe envia webhook "subscription.deleted"
    └─ Backend atualiza: usuario.plano = FREE
    └─ Backend limpa: usuario.plano_expira_em = NULL

11. Ciclo recomeça
    └─ Usuario.plano = FREE
```

---

## Segurança: Webhook Signature Verification

```
Backend → Stripe: "Quero receber webhooks em /pagamentos/webhook"
Stripe → Backend: "OK, aqui está seu webhook secret: whsec_abc123..."

Quando pagamento ocorre:

Stripe calcula: HMAC(webhook_secret, payload)
Stripe envia: headers "Stripe-Signature": HMAC_VALUE

Backend recebe requisição:
├─ Calcula: HMAC(webhook_secret, payload)
│
├─ Compara: HMAC_VALUE == HMAC calculado?
│
├─ Se SIM: ✅ É realmente do Stripe
│   └─ Processa event
│
└─ Se NÃO: ❌ Pode ser ataque/falsificação
    └─ Rejeita (SignatureVerificationException)
```

---

## Variáveis de Configuração

```
application.properties (padrão)
  stripe.api.key=YOUR_STRIPE_SECRET_KEY
  stripe.webhook.secret=YOUR_STRIPE_WEBHOOK_SECRET
  stripe.price.basic=price_BASIC_CONFIGURE_NO_STRIPE
  stripe.price.pro=price_PRO_CONFIGURE_NO_STRIPE

application-local.properties (desenvolvimento)
  stripe.api.key=sk_test_abc123...
  stripe.webhook.secret=whsec_def456...
  stripe.price.basic=price_local_basic_xyz...
  stripe.price.pro=price_local_pro_xyz...

application-prod.properties (produção)
  stripe.api.key=${STRIPE_API_KEY}
  stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET}
  stripe.price.basic=${STRIPE_PRICE_BASIC}
  stripe.price.pro=${STRIPE_PRICE_PRO}
```

