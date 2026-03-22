# 🚀 Guia de Setup do Stripe - Passo a Passo

## 1️⃣ Criar Conta Stripe

1. Acesse [stripe.com](https://stripe.com)
2. Clique em "Sign up"
3. Preencha seu email e senha
4. Confirme email
5. Preencha dados da empresa

---

## 2️⃣ Obter Chave de API (stripe.api.key)

### Passos:
1. Acesse [Stripe Dashboard](https://dashboard.stripe.com)
2. No menu esquerdo, clique em **"Developers"** → **"API keys"**
3. Na aba **"Secret keys"**, você verá duas chaves:
   - **Publishable key** (começa com `pk_test_`) - NÃO USE AQUI
   - **Secret key** (começa com `sk_test_`) - USE ESTA
4. Clique no ícone de cópia ao lado da Secret key
5. Cole em `application-local.properties`:

```properties
stripe.api.key=sk_test_seu_valor_aqui
```

---

## 3️⃣ Criar Webhook (stripe.webhook.secret)

### Passos:
1. Acesse [Stripe Dashboard](https://dashboard.stripe.com)
2. No menu esquerdo, clique em **"Developers"** → **"Webhooks"**
3. Clique em **"Add endpoint"**
4. Preencha:
   - **Endpoint URL:** `http://localhost:8080/api/v1/pagamentos/webhook` (local)
   - Ou: `https://seu-dominio.com/api/v1/pagamentos/webhook` (produção)
5. Em **"Events to send"**, selecione:
   - ✅ `checkout.session.completed`
   - ✅ `customer.subscription.deleted`
   - (Deixe desabilitado: `source.chargeable`, `invoice.*`, etc.)
6. Clique em **"Add endpoint"**
7. Na página do webhook, clique em **"Reveal"** ao lado de "Signing secret"
8. Copie o valor que começa com `whsec_`
9. Cole em `application-local.properties`:

```properties
stripe.webhook.secret=whsec_seu_valor_aqui
```

---

## 4️⃣ Criar Produtos e Preços

### O que é um Produto?
Um Produto é algo que você vende (ex: "Plano BASIC").

### O que é um Preço?
Um Preço é quanto custa e com qual frequência (ex: "R$ 29,90 por mês").

### Passos para criar Produto BASIC:

1. Acesse [Stripe Dashboard](https://dashboard.stripe.com)
2. No menu esquerdo, clique em **"Products"**
3. Clique em **"Add product"**
4. Preencha:
   - **Name:** `Plano BASIC`
   - **Description:** (opcional) `Acesso básico ao sistema`
   - **Image:** (opcional) Foto do plano
   - **Price (Optional):** Deixe em branco (vamos adicionar via Pricing)
5. Clique em **"Save product"**
6. Na página do produto, vá para aba **"Pricing"**
7. Clique em **"Add a price"**
8. Preencha:
   - **Billing period:** `Monthly` (mensal)
   - **Price:** `29.90` (em reais)
   - **Currency:** `BRL` (Real brasileiro)
   - **Recurring:** ✅ Habilitado (Monthly)
9. Clique em **"Save price"**
10. Copie o **Price ID** (começa com `price_`)
11. Cole em `application-local.properties`:

```properties
stripe.price.basic=price_seu_valor_aqui
```

### Passos para criar Produto PRO:

Repita o processo acima, mas com:
- **Name:** `Plano PRO`
- **Description:** `Acesso premium ao sistema`
- **Price:** `79.90` (em reais)

E cole em:
```properties
stripe.price.pro=price_seu_valor_aqui
```

---

## 5️⃣ Testar Webhooks Localmente (Opcional mas Recomendado)

### Problema:
Se você está testando localmente (`http://localhost:8080`), Stripe não consegue enviar webhooks porque seu PC não é acessível da internet.

### Solução: Usar Stripe CLI

#### 1. Instalar Stripe CLI
- **Windows:** Baixe em [stripe.com/docs/stripe-cli](https://stripe.com/docs/stripe-cli)
- **Mac:** `brew install stripe/stripe-cli/stripe`
- **Linux:** `curl -fsSL https://get.stripe.com/stripe-cli | bash`

#### 2. Fazer Login
```bash
stripe login
```
Vai abrir navegador para fazer login.

#### 3. Encaminhar Webhooks para Local
```bash
stripe listen --forward-to localhost:8080/api/v1/pagamentos/webhook
```

Isso vai gerar um secret (começa com `whsec_`). Use este em `application-local.properties`:

```properties
stripe.webhook.secret=whsec_xxxx_xxxx_xxxx
```

---

## 6️⃣ Testar a Integração

### Usar Cartão de Teste Stripe

Stripe fornece números de cartão para testes:

| Cartão | Número | Exp | CVC |
|--------|--------|-----|-----|
| Sucesso | `4242 4242 4242 4242` | Qualquer futuro | Qualquer 3 dígitos |
| Falha | `4000 0000 0000 0002` | Qualquer futuro | Qualquer 3 dígitos |

### Testar Checkout Completo:

1. **Inicie o backend:**
```bash
mvn spring-boot:run
```

2. **Faça uma requisição para criar checkout:**
```bash
curl -X POST http://localhost:8080/api/v1/pagamentos/checkout \
  -H "Authorization: Bearer seu_token_jwt" \
  -H "Content-Type: application/json" \
  -d '{
    "plano": "BASIC",
    "successUrl": "http://localhost:4200/sucesso",
    "cancelUrl": "http://localhost:4200/cancelado"
  }'
```

3. **Copie a URL do response e abra no navegador**

4. **Preencha o formulário com:**
   - Email: qualquer email
   - Cartão: `4242 4242 4242 4242`
   - Validade: mês/ano futuro
   - CVC: qualquer 3 dígitos

5. **Clique em "Pay"**

6. **Webhook será processado automaticamente**

---

## 7️⃣ Variáveis para Produção

Quando for fazer deploy, você precisa configurar estas variáveis de ambiente:

```bash
STRIPE_API_KEY=sk_live_sua_chave_secreta_live
STRIPE_WEBHOOK_SECRET=whsec_sua_webhook_secret_live
STRIPE_PRICE_BASIC=price_sua_price_id_live_basic
STRIPE_PRICE_PRO=price_sua_price_id_live_pro
```

### Onde configurar:
- **Heroku:** Settings → Config Vars
- **AWS:** Environment Variables
- **Google Cloud:** Environment Variables
- **DigitalOcean:** App Platform → Environment
- **Docker:** `docker run -e STRIPE_API_KEY=...`

---

## ✅ Checklist Final

- [ ] Conta Stripe criada
- [ ] API Key copiada para `application-local.properties`
- [ ] Webhook criado em Stripe
- [ ] Webhook Secret copiado para `application-local.properties`
- [ ] Produto BASIC criado
- [ ] Price BASIC copiado para `application-local.properties`
- [ ] Produto PRO criado
- [ ] Price PRO copiado para `application-local.properties`
- [ ] Stripe CLI instalado e rodando (opcional)
- [ ] Backend testado com cartão de teste
- [ ] Webhook testado com `stripe trigger` (opcional)

---

## 🐛 Troubleshooting

| Erro | Solução |
|------|---------|
| "Invalid API Key" | Copie novamente do Dashboard (Secret key, não Publishable) |
| "Webhook signature verification failed" | Copie novamente o webhook secret |
| "Price not found" | O Price ID pode estar incorreto ou ser de outro produto |
| "Checkout failed" | Verifique se `successUrl` e `cancelUrl` estão preenchidas |
| "Customer not found" | Faça checkout antes de abrir o portal |

---

## 📚 Links Úteis

- [Stripe Dashboard](https://dashboard.stripe.com)
- [Stripe API Documentation](https://stripe.com/docs/api)
- [Stripe Testing Cards](https://stripe.com/docs/testing)
- [Stripe CLI Reference](https://stripe.com/docs/stripe-cli)

