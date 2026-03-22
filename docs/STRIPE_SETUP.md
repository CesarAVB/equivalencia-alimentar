# 🎯 Guia Completo do Stripe - Para Iniciantes

## O que é Stripe?

Stripe é uma plataforma de pagamentos que permite suas aplicações processarem pagamentos de forma segura e confiável. Em vez de você armazenar dados de cartão de crédito (o que é muito inseguro), você envia o usuário para o Stripe fazer o pagamento e Stripe cuida de tudo.

---

## 📋 Arquitetura do Sistema de Pagamentos

```
Frontend (Angular)
       ↓
PagamentoController (API REST)
       ↓
StripeService (Lógica de Pagamento)
       ↓
Stripe Dashboard (Gateway de Pagamento)
       ↓
Banco de Dados (Armazena stripe_customer_id)
```

---

## 🔑 Classes Principais

### 1. **StripeService** (`service/StripeService.java`)
É o "coração" do sistema de pagamentos. Faz toda a comunicação com o Stripe.

**Responsabilidades:**
- 🔐 Configurar a chave de API do Stripe
- 💳 Criar clientes no Stripe (Customer)
- 🛒 Criar sessões de checkout para pagamentos
- 🎭 Criar portal do cliente para gerenciar assinatura
- 📨 Processar webhooks (eventos enviados pelo Stripe)

**Métodos principais:**

| Método | O que faz | Quando usar |
|--------|-----------|-----------|
| `criarCheckoutSession()` | Gera um link de pagamento | Quando usuário clica em "Upgrade para Plano" |
| `criarPortalSession()` | Abre portal para gerenciar assinatura | Quando usuário quer mudar plano ou cancelar |
| `processarWebhook()` | Processa eventos do Stripe | Automaticamente quando Stripe envia eventos |

---

### 2. **PagamentoController** (`controller/PagamentoController.java`)
É a "porta de entrada" para pagamentos. Expõe os endpoints REST que o frontend chama.

**Endpoints:**

```
POST /api/v1/pagamentos/checkout
├─ O que faz: Cria uma sessão de pagamento
├─ Recebe: { plano, successUrl, cancelUrl }
├─ Retorna: { url } (link para ir ao Stripe)
└─ Exemplo: https://checkout.stripe.com/pay/cs_test_123...

GET /api/v1/pagamentos/portal?returnUrl=...
├─ O que faz: Cria portal para gerenciar assinatura
├─ Retorna: { url } (link do portal do Stripe)
└─ Exemplo: https://billing.stripe.com/b/abc123...

POST /api/v1/pagamentos/webhook
├─ O que faz: Recebe eventos do Stripe
├─ Recebe: payload + assinatura
└─ Processa automaticamente (sem resposta ao cliente)
```

---

### 3. **DTOs (Data Transfer Objects)**

#### `CheckoutRequest` (Request)
```java
public record CheckoutRequest(
    PlanoTipo plano,        // BASIC ou PRO (não FREE)
    String successUrl,      // URL para redirecionar após sucesso
    String cancelUrl        // URL para redirecionar se cancelar
) {}
```

**Exemplo:**
```json
{
  "plano": "BASIC",
  "successUrl": "http://localhost:4200/sucesso",
  "cancelUrl": "http://localhost:4200/cancelado"
}
```

#### `CheckoutResponse` (Response)
```java
public record CheckoutResponse(String url) {}
```

**Exemplo:**
```json
{
  "url": "https://checkout.stripe.com/pay/cs_test_abc123..."
}
```

---

### 4. **Enum PlanoTipo**
Define os 3 planos disponíveis:

```java
public enum PlanoTipo {
    FREE,   // Sem custos (não precisa de pagamento)
    BASIC,  // Plano básico (R$ X por mês)
    PRO     // Plano profissional (R$ Y por mês)
}
```

---

### 5. **Entity Usuario**
A entidade Usuario armazena dados relacionados ao Stripe:

```java
@Column(name = "stripe_customer_id")
private String stripeCustomerId;  // ID único do cliente no Stripe

@Column(name = "plano")
private PlanoTipo plano;  // Qual plano o usuário tem

@Column(name = "plano_expira_em")
private LocalDateTime planoExpiraEm;  // Quando a assinatura expira
```

---

## 🗄️ Banco de Dados

### Tabela `usuarios`

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `id` | VARCHAR(36) | ID único (UUID) |
| `nome` | VARCHAR(255) | Nome do usuário |
| `email` | VARCHAR(255) | Email único |
| `cpf` | VARCHAR(14) | CPF do usuário |
| `senha` | VARCHAR(255) | Senha hasheada |
| `tipo` | VARCHAR(50) | ADMIN, USUARIO |
| `ativo` | TINYINT(1) | 1 = ativo, 0 = inativo |
| **`stripe_customer_id`** | VARCHAR(255) | **ID do cliente no Stripe** |
| **`plano`** | VARCHAR(50) | **Plano atual (FREE, BASIC, PRO)** |
| **`plano_expira_em`** | DATETIME | **Quando a assinatura vence** |
| `created_at` | DATETIME | Data de criação |
| `updated_at` | DATETIME | Data de atualização |

---

## 🔄 Fluxo de Funcionamento

### 1️⃣ Usuário deseja fazer upgrade

```
[Frontend] → POST /api/v1/pagamentos/checkout
   ↓
[Controller] → Valida requisição
   ↓
[Service] → Obtém ou cria Customer no Stripe
   ↓
[Service] → Cria Checkout Session
   ↓
[Frontend] ← URL do Stripe
   ↓
[Browser] → Redireciona para Stripe
```

### 2️⃣ Stripe processa o pagamento

```
[Stripe] → Usuário insere dados do cartão
   ↓
[Stripe] → Processa pagamento
   ↓
[Stripe] → Cria Subscription (assinatura)
```

### 3️⃣ Stripe envia confirmação via Webhook

```
[Stripe] → Envia evento: "checkout.session.completed"
   ↓
[Backend] → POST /api/v1/pagamentos/webhook
   ↓
[Service] → Verifica assinatura (segurança)
   ↓
[Service] → Processa "checkout.session.completed"
   ↓
[Service] → Atualiza usuario.plano no BD
   ↓
[Service] → Atualiza usuario.plano_expira_em
```

### 4️⃣ Usuário gerencia sua assinatura

```
[Frontend] → GET /api/v1/pagamentos/portal?returnUrl=...
   ↓
[Service] → Cria Portal Session
   ↓
[Frontend] ← URL do portal
   ↓
[Browser] → Abre portal do Stripe
   ↓
[Stripe] → Usuário pode mudar plano ou cancelar
```

---

## 🔐 Configuração - Chaves do Stripe

Você precisa de 3 informações do Stripe:

### 1. **API Key (Chave Secreta)**
- Onde obter: [Stripe Dashboard → Developers → API Keys](https://dashboard.stripe.com/apikeys)
- Começa com: `sk_test_` (teste) ou `sk_live_` (produção)
- ⚠️ **NUNCA compartilhe com ninguém!** Use variáveis de ambiente.
- **Variável:** `stripe.api.key`

### 2. **Webhook Secret**
- Onde obter: [Stripe Dashboard → Developers → Webhooks](https://dashboard.stripe.com/webhooks)
- Começa com: `whsec_`
- Usado para verificar se o webhook veio realmente do Stripe
- **Variável:** `stripe.webhook.secret`

### 3. **Price IDs**
- Onde obter: [Stripe Dashboard → Products → Prices](https://dashboard.stripe.com/products)
- Começa com: `price_`
- Um Price ID por plano (BASIC e PRO)
- **Variáveis:** `stripe.price.basic`, `stripe.price.pro`

---

## ⚙️ Configuração em application-local.properties

```properties
# ============================================
# Stripe Configuration
# ============================================
# Obtenha em: https://dashboard.stripe.com/apikeys
stripe.api.key=sk_test_51234567890abcdef...

# Obtenha em: https://dashboard.stripe.com/webhooks
stripe.webhook.secret=whsec_1234567890abcdef...

# Obtenha em: https://dashboard.stripe.com/products
# Estes são PREÇOS, não produtos!
stripe.price.basic=price_1234567890abcdef...
stripe.price.pro=price_0987654321fedcba...
```

---

## ⚙️ Configuração em application-prod.properties

```properties
# ============================================
# Stripe Configuration
# ============================================
# Use variáveis de ambiente em produção!
stripe.api.key=${STRIPE_API_KEY}
stripe.webhook.secret=${STRIPE_WEBHOOK_SECRET}
stripe.price.basic=${STRIPE_PRICE_BASIC}
stripe.price.pro=${STRIPE_PRICE_PRO}
```

---

## 🎬 Fluxo Completo no Frontend (Angular)

### 1. Usuário clica em "Upgrade para BASIC"

```typescript
checkout() {
  const request: CheckoutRequest = {
    plano: 'BASIC',
    successUrl: 'http://localhost:4200/sucesso',
    cancelUrl: 'http://localhost:4200/cancelado'
  };

  this.pagamentoService.checkout(request).subscribe(response => {
    // response.url = URL do Stripe
    window.location.href = response.url;
  });
}
```

### 2. Backend cria a sessão

```
POST /api/v1/pagamentos/checkout
Body: {
  "plano": "BASIC",
  "successUrl": "...",
  "cancelUrl": "..."
}

Response: {
  "url": "https://checkout.stripe.com/..."
}
```

### 3. Usuário é redirecionado para Stripe

```
Abre: https://checkout.stripe.com/pay/cs_test_...
↓
Insere dados do cartão
↓
Clica "Pagar"
```

### 4. Stripe processa e envia webhook

```
Stripe → POST /api/v1/pagamentos/webhook
Body: { event, signature, ... }
```

### 5. Backend atualiza usuário

```java
usuario.setPlano(PlanoTipo.BASIC);
usuario.setPlanoExpiraEm(LocalDateTime.now().plusMonths(1));
usuarioRepository.save(usuario);
```

### 6. Usuário é redirecionado de volta

```
Stripe → Redireciona para successUrl
http://localhost:4200/sucesso?session_id=cs_test_...
```

---

## 🐛 Troubleshooting

| Problema | Causa | Solução |
|----------|-------|--------|
| "Plano FREE não requer pagamento" | Tentou fazer checkout com FREE | Apenas BASIC e PRO permitem checkout |
| "Usuário não possui assinatura ativa" | stripe_customer_id é NULL | Fez checkout antes? Se não, chame checkout primeiro |
| "Assinatura do webhook inválida" | webhook.secret incorreto | Copie do dashboard novamente |
| "Erro ao criar sessão de pagamento" | API key inválida | Copie do dashboard novamente |

---

## 📚 Mais Informações

- [Stripe Documentation](https://stripe.com/docs)
- [Stripe API Reference](https://stripe.com/docs/api)
- [Stripe Java SDK](https://github.com/stripe/stripe-java)
- [Webhook Events](https://stripe.com/docs/api/events)

