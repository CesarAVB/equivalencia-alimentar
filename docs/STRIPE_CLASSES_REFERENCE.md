# 📦 Referência Rápida - Classes do Stripe

## 📍 Localização das Classes

```
src/main/java/br/com/sistema/alimentos/
├── service/StripeService.java ..................... 🧠 Lógica de pagamento
├── controller/PagamentoController.java ........... 🚪 Endpoints REST
├── dtos/
│   ├── request/CheckoutRequest.java ............. 📥 Dados de entrada
│   └── response/CheckoutResponse.java ........... 📤 Dados de saída
├── entity/Usuario.java .......................... 💾 Modelo do BD
└── enums/PlanoTipo.java ......................... 🏷️ Tipos de plano

src/main/resources/
├── application.properties ........................ ⚙️ Config padrão
├── application-local.properties ................. 🔧 Config local
├── application-prod.properties .................. 🔧 Config produção
└── db/migration/V1__init.sql ................... 📊 Estrutura BD
```

---

## 🎯 Responsabilidades de Cada Classe

### 🧠 StripeService
**Arquivo:** `service/StripeService.java`

**O que faz:**
- Gerencia toda comunicação com API Stripe
- Cria clientes (Customer) no Stripe
- Cria sessões de checkout e portal
- Processa webhooks de eventos

**Métodos:**
```java
// Inicializa com chave de API do Stripe
@PostConstruct
public void init()

// Cria uma sessão de pagamento
@Transactional
public CheckoutResponse criarCheckoutSession(UUID usuarioId, CheckoutRequest request)

// Cria portal para gerenciar assinatura
public CheckoutResponse criarPortalSession(UUID usuarioId, String returnUrl)

// Processa eventos do Stripe
@Transactional
public void processarWebhook(String payload, String sigHeader)

// Obtém customer existente ou cria novo
private String obterOuCriarCustomer(Usuario usuario)

// Processa pagamento confirmado
private void processarPagamentoConcluido(com.stripe.model.Event event)

// Processa cancelamento de assinatura
private void processarAssinaturaCancelada(com.stripe.model.Event event)
```

**Dependências injetadas:**
```java
private final UsuarioRepository usuarioRepository;
```

**Variáveis de configuração que lê:**
```properties
stripe.api.key              // Chave secreta do Stripe
stripe.webhook.secret       // Secret para validar webhooks
stripe.price.basic          // Price ID do plano BASIC
stripe.price.pro            // Price ID do plano PRO
```

---

### 🚪 PagamentoController
**Arquivo:** `controller/PagamentoController.java`

**O que faz:**
- Expõe endpoints REST para pagamentos
- Valida requisições HTTP
- Redireciona para StripeService
- Retorna respostas ao frontend

**Endpoints:**

#### 1. POST /api/v1/pagamentos/checkout
```
Requisição:
{
  "plano": "BASIC",
  "successUrl": "http://localhost:4200/sucesso",
  "cancelUrl": "http://localhost:4200/cancelado"
}

Resposta (200):
{
  "url": "https://checkout.stripe.com/pay/cs_test_..."
}

Erros:
- 400: plano inválido ou URLs vazias
- 401: usuário não autenticado
- 404: usuário não encontrado
```

#### 2. GET /api/v1/pagamentos/portal?returnUrl=...
```
Parâmetros:
- returnUrl: URL para voltar após gerenciar assinatura

Resposta (200):
{
  "url": "https://billing.stripe.com/b/abc123..."
}

Erros:
- 400: returnUrl não fornecida
- 401: usuário não autenticado
- 404: usuário sem assinatura
```

#### 3. POST /api/v1/pagamentos/webhook
```
Requisição:
- Body: payload Stripe (JSON)
- Header: Stripe-Signature

Resposta: 200 (sem conteúdo)

Processa automaticamente:
- checkout.session.completed (pagamento aprovado)
- customer.subscription.deleted (assinatura cancelada)
```

**Métodos:**
```java
@PostMapping("/checkout")
public ResponseEntity<CheckoutResponse> checkout(
    @AuthenticationPrincipal Usuario usuario,
    @RequestBody @Valid CheckoutRequest request)

@GetMapping("/portal")
public ResponseEntity<CheckoutResponse> portal(
    @AuthenticationPrincipal Usuario usuario,
    @RequestParam String returnUrl)

@PostMapping("/webhook")
public ResponseEntity<Void> webhook(
    @RequestBody String payload,
    @RequestHeader("Stripe-Signature") String sigHeader)
```

---

### 📥 CheckoutRequest (DTO de Entrada)
**Arquivo:** `dtos/request/CheckoutRequest.java`

**O que é:** Record Java que define dados enviados pelo frontend

**Campos:**
```java
@NotNull(message = "O plano é obrigatório")
PlanoTipo plano  // BASIC ou PRO (FREE não é permitido)

@NotBlank(message = "A URL de sucesso é obrigatória")
String successUrl  // URL para redirecionar após sucesso

@NotBlank(message = "A URL de cancelamento é obrigatória")
String cancelUrl  // URL para redirecionar se usuário cancelar
```

**Exemplo de JSON:**
```json
{
  "plano": "BASIC",
  "successUrl": "http://localhost:4200/pagamento-sucesso",
  "cancelUrl": "http://localhost:4200/pagamento-cancelado"
}
```

---

### 📤 CheckoutResponse (DTO de Saída)
**Arquivo:** `dtos/response/CheckoutResponse.java`

**O que é:** Record Java que retorna URL do Stripe ao frontend

**Campos:**
```java
String url  // URL para abrir Stripe Checkout
```

**Exemplo de JSON:**
```json
{
  "url": "https://checkout.stripe.com/pay/cs_test_abc123xyz..."
}
```

---

### 🏷️ PlanoTipo (Enum)
**Arquivo:** `enums/PlanoTipo.java`

**O que é:** Define os 3 planos disponíveis

**Valores:**
```java
FREE    // Sem custos, sem pagamento
BASIC   // Plano básico, requer pagamento
PRO     // Plano premium, requer pagamento
```

---

### 💾 Usuario (Entity)
**Arquivo:** `entity/Usuario.java`

**O que é:** Entidade JPA mapeada para tabela `usuarios` do BD

**Campos relacionados a Stripe:**

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `stripeCustomerId` | String | ID do cliente no Stripe (gerado automaticamente) |
| `plano` | PlanoTipo | Plano atual do usuário (FREE, BASIC, PRO) |
| `planoExpiraEm` | LocalDateTime | Data de expiração da assinatura |

**Exemplo:**
```java
Usuario usuario = Usuario.builder()
    .id(UUID.randomUUID())
    .nome("João Silva")
    .email("joao@example.com")
    .senha("...")
    .tipo(UsuarioTipo.USUARIO)
    .stripeCustomerId("cus_123456789")  // ← Preenchido após checkout
    .plano(PlanoTipo.BASIC)              // ← Atualizado após pagamento
    .planoExpiraEm(LocalDateTime.now().plusMonths(1))  // ← Data de vencimento
    .ativo(true)
    .build();
```

---

## 🔄 Fluxo de Dados Entre Classes

```
1. Frontend envia CheckoutRequest
   ↓
2. PagamentoController recebe e valida
   ↓
3. PagamentoController → StripeService.criarCheckoutSession()
   ↓
4. StripeService → UsuarioRepository.findById()
   ↓
5. StripeService → obterOuCriarCustomer()
   ↓
6. StripeService → Stripe.Customer.create() [API Stripe]
   ↓
7. StripeService → usuario.setStripeCustomerId()
   ↓
8. StripeService → UsuarioRepository.save()
   ↓
9. StripeService → Stripe.Session.create() [API Stripe]
   ↓
10. StripeService retorna CheckoutResponse
    ↓
11. PagamentoController retorna CheckoutResponse ao Frontend
    ↓
12. Frontend → window.location.href = response.url
    ↓
13. Browser abre Stripe Checkout
```

---

## ⚙️ Variáveis de Configuração

### Em application.properties (base)
```properties
stripe.api.key=YOUR_STRIPE_SECRET_KEY
stripe.webhook.secret=YOUR_STRIPE_WEBHOOK_SECRET
stripe.price.basic=price_BASIC_CONFIGURE_NO_STRIPE
stripe.price.pro=price_PRO_CONFIGURE_NO_STRIPE
```

### Em application-local.properties (desenvolvimento)
```properties
stripe.api.key=sk_test_...
stripe.webhook.secret=whsec_...
stripe.price.basic=price_...
stripe.price.pro=price_...
```

### Em application-prod.properties (produção)
```properties
stripe.api.key=${STRIPE_API_KEY}
stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET}
stripe.price.basic=${STRIPE_PRICE_BASIC}
stripe.price.pro=${STRIPE_PRICE_PRO}
```

---

## 🗄️ Estrutura do Banco de Dados

### Tabela: usuarios
```sql
CREATE TABLE usuarios (
    id                 VARCHAR(36)  PRIMARY KEY,
    nome               VARCHAR(255) NOT NULL,
    email              VARCHAR(255) NOT NULL UNIQUE,
    cpf                VARCHAR(14)  UNIQUE,
    senha              VARCHAR(255) NOT NULL,
    tipo               VARCHAR(50)  NOT NULL,
    ativo              TINYINT(1)   NOT NULL DEFAULT 1,
    stripe_customer_id VARCHAR(255),  ← ID do cliente no Stripe
    plano              VARCHAR(50)  NOT NULL DEFAULT 'FREE',  ← Plano atual
    plano_expira_em    DATETIME,      ← Quando expira a assinatura
    created_at         DATETIME     NOT NULL,
    updated_at         DATETIME
);
```

---

## 🔐 Fluxo de Segurança

### Webhook Signature Verification
```java
// Stripe envia: X-Stripe-Signature = HMAC(webhook_secret, payload)
// Backend valida: Webhook.constructEvent(payload, sig, secret)
// Se não corresponder: SignatureVerificationException
```

### API Key Management
```properties
# LOCAL - Chave de teste (segura, pois é só teste)
stripe.api.key=sk_test_...

# PRODUÇÃO - Chave secreta (NUNCA hardcoded)
stripe.api.key=${STRIPE_API_KEY}  # Variável de ambiente
```

### Autenticação de Endpoints
```java
@AuthenticationPrincipal Usuario usuario  // Spring Security injeta
// Só usuário autenticado pode chamar
```

---

## 📞 Métodos da API Stripe Usados

| Método Stripe | Usado em | Propósito |
|---------------|----------|-----------|
| `Customer.create()` | StripeService | Criar cliente no Stripe |
| `Session.create()` (Checkout) | StripeService | Criar sessão de pagamento |
| `Session.create()` (BillingPortal) | StripeService | Criar portal do cliente |
| `Webhook.constructEvent()` | StripeService | Validar e parsear webhook |

---

## 🎯 Fluxo de Implementação Futura

Para completar o sistema, ainda faltam:

- [ ] Implementar `processarPagamentoConcluido()` (em StripeService:140)
  - Atualizar `usuario.plano` para BASIC ou PRO
  - Atualizar `usuario.planoExpiraEm`
  - Salvar no BD

- [ ] Implementar `processarAssinaturaCancelada()` (em StripeService:144)
  - Atualizar `usuario.plano` para FREE
  - Limpar `usuario.planoExpiraEm`
  - Salvar no BD

- [ ] Criar migration SQL se necessário adicionar novos campos
  - Ex: `stripe_subscription_id`, `ultima_renovacao`

- [ ] Adicionar validação de plano expirado
  - Middleware ou Aspect que verifica `planoExpiraEm`
  - Se expirado, reverter para FREE

