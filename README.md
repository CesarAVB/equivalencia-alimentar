# Equivalência Alimentar - Backend

API REST para gestão de equivalências nutricionais entre alimentos, com autenticação JWT, controle de acesso por papel, assinaturas via Stripe e banco de dados pré-populado com 95 alimentos.

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.0-brightgreen)
![MySQL](https://img.shields.io/badge/MySQL-8.x-orange)
![Flyway](https://img.shields.io/badge/Flyway-migrations-red)

---

## Sumário

- [Tecnologias](#tecnologias)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar localmente](#como-rodar-localmente)
- [Estrutura de pacotes](#estrutura-de-pacotes)
- [Modelo de dados](#modelo-de-dados)
- [Autenticação](#autenticação)
- [Controle de acesso](#controle-de-acesso)
- [Endpoints](#endpoints)
- [Integração Stripe](#integração-stripe)
- [Migrations Flyway](#migrations-flyway)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Testes](#testes)
- [Exemplos de requisições](#exemplos-de-requisições)
- [Troubleshooting (Prod)](#troubleshooting-prod)

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.5.0 |
| Spring Security | gerenciado pelo Spring Boot |
| Spring Data JPA | gerenciado pelo Spring Boot |
| MySQL | 8.x |
| Flyway | gerenciado pelo Spring Boot |
| JJWT | 0.12.6 |
| Stripe Java SDK | 26.4.0 |
| Lombok | gerenciado pelo Spring Boot |
| SpringDoc OpenAPI | 2.7.0 |

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- MySQL 8.x rodando localmente na porta 3306

---

## Como rodar localmente

1. Clone o repositório:
   ```bash
   git clone <url-do-repositorio>
   cd equivalencia-alimentar-backend
   ```

2. Configure o arquivo `src/main/resources/application-local.properties` com suas credenciais:
   ```properties
  spring.datasource.url=jdbc:mysql://localhost:3306/equivalencia_alimentar?createDatabaseIfNotExist=true&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
   spring.datasource.username=root
   spring.datasource.password=sua_senha
  spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

  # Aceita Base64 forte OU string textual com no minimo 32 caracteres
  jwt.secret=gere_um_secret_com_32_ou_mais_caracteres
   jwt.expiration=86400000

  stripe.api.key=sk_test_...
  stripe.webhook.secret=whsec_...
  stripe.price.padrao=price_...

  app.cors.allowed-origins=http://localhost:4200,http://localhost:5173
   ```
  > Dica: para gerar secret forte em Base64 use `openssl rand -base64 64`.

3. Execute:
   ```bash
   mvn spring-boot:run
   ```

4. Acesse a documentação Swagger:
   ```
   http://localhost:8080/api/v1/swagger-ui.html
   ```

> O Flyway criará o banco automaticamente e executará as migrations em ordem: `V1` (tabelas), `V2` (dados iniciais de alimentos) e `V3` em diante (evoluções).

---

## Estrutura de pacotes

```
br.com.sistema.alimentos
  ├── config/         → AppConfig, CorsConfig, GrupoAlimentarConverter, OpenApiConfig, SecurityConfig
  ├── controller/     → AuthController, UsuarioController, AlimentoController, EquivalenciaController, PagamentoController
  ├── dtos/
  │   ├── request/    → Records de entrada com Bean Validation
  │   └── response/   → Records de saída
  ├── entity/         → Alimento, Usuario, Equivalencia
  ├── enums/          → UsuarioTipo, PlanoTipo, GrupoAlimentar
  ├── exception/      → GlobalExceptionHandler
  ├── filter/         → JwtAuthFilter
  ├── repository/     → Interfaces JpaRepository
  └── service/        → AuthService, UsuarioService, AlimentoService, EquivalenciaService, JwtService, StripeService
```

---

## Modelo de dados

### `alimentos`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | INT AUTO_INCREMENT | Chave primária |
| `codigo_substituicao` | VARCHAR(20) UNIQUE | Código ex.: `FRUT.001` |
| `grupo` | ENUM | `Frutas`, `Carboidratos`, `Proteína`, `Laticíneos`, `Gordura Vegetal` |
| `descricao` | VARCHAR(255) | Nome do alimento |
| `energia_kcal` | DECIMAL(10,2) | Calorias por porção de referência |
| `created_at` / `updated_at` | TIMESTAMP | Gerenciados pelo banco |

A tabela é pré-populada com **95 alimentos** via migration `V2__seed_alimentos.sql`.

### `usuarios`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | VARCHAR(36) | UUID gerado pela aplicação |
| `nome` | VARCHAR(255) | Nome completo |
| `email` | VARCHAR(255) UNIQUE | Login |
| `cpf` | VARCHAR(14) UNIQUE | Formato `000.000.000-00` (opcional) |
| `senha` | VARCHAR(255) | Hash BCrypt |
| `tipo` | VARCHAR(50) | `ADMIN`, `NUTRICIONISTA` ou `PACIENTE` |
| `ativo` | TINYINT(1) | `1` = ativo, `0` = desativado |
| `stripe_customer_id` | VARCHAR(255) | ID do cliente no Stripe |
| `plano` | VARCHAR(50) | `PADRAO` |
| `plano_expira_em` | DATETIME | Data de vencimento do plano |

### `equivalencias`

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | VARCHAR(36) | UUID |
| `alimento_origem_id` | INT (FK) | Referência a `alimentos.id` |
| `alimento_destino_id` | INT (FK) | Referência a `alimentos.id` |
| `fator_equivalencia` | DECIMAL(10,4) | Quantidade do destino equivalente a 1 unidade da origem |
| `observacao` | TEXT | Observação livre (opcional) |

---

## Autenticação

Todas as rotas — exceto `POST /auth/login` e `POST /pagamentos/webhook` — exigem o header:

```
Authorization: Bearer <token>
```

O token é obtido via `POST /api/v1/auth/login` e expira conforme `jwt.expiration`.

Resposta de login:

- O campo `plano` é uma **string de exibição**.
- Valores atuais: `trial` (janela de 30 dias desde a criação/expiração configurada) ou `padrão`.

---

## Controle de acesso

| Papel | Permissões |
|---|---|
| `ADMIN` | Acesso total: usuários, alimentos e equivalências |
| `NUTRICIONISTA` | Criar e editar alimentos e equivalências |
| `PACIENTE` | Apenas leitura de alimentos e equivalências |

---

## Endpoints

Todos os paths abaixo são prefixados com `/api/v1`.

| Método | Path | Papel mínimo | Descrição |
|---|---|---|---|
| POST | `/auth/login` | — | Autenticar e obter JWT |
| GET | `/usuarios` | ADMIN | Listar todos os usuários |
| POST | `/usuarios` | ADMIN | Criar usuário |
| GET | `/usuarios/{id}` | ADMIN | Buscar usuário por ID |
| PUT | `/usuarios/{id}` | ADMIN | Atualizar usuário |
| PATCH | `/usuarios/{id}/ativar` | ADMIN | Ativar usuário |
| PATCH | `/usuarios/{id}/desativar` | ADMIN | Desativar usuário |
| DELETE | `/usuarios/{id}` | ADMIN | Remover usuário |
| GET | `/alimentos` | Autenticado | Listar alimentos (paginado; filtros: `descricao`, `grupo`) |
| GET | `/alimentos/{id}` | Autenticado | Buscar alimento por ID |
| POST | `/alimentos` | ADMIN / NUTRICIONISTA | Cadastrar alimento |
| PUT | `/alimentos/{id}` | ADMIN / NUTRICIONISTA | Atualizar alimento |
| DELETE | `/alimentos/{id}` | ADMIN | Remover alimento |
| GET | `/equivalencias` | Autenticado | Listar equivalências (paginado) |
| GET | `/equivalencias/{id}` | Autenticado | Buscar equivalência por ID |
| GET | `/equivalencias/alimento/{id}` | Autenticado | Equivalências de um alimento |
| POST | `/equivalencias` | ADMIN / NUTRICIONISTA | Cadastrar equivalência |
| PUT | `/equivalencias/{id}` | ADMIN / NUTRICIONISTA | Atualizar equivalência |
| DELETE | `/equivalencias/{id}` | ADMIN / NUTRICIONISTA | Remover equivalência |
| POST | `/pagamentos/checkout` | Autenticado | Criar sessão Stripe Checkout |
| GET | `/pagamentos/portal` | Autenticado | Abrir portal de assinatura Stripe |
| POST | `/pagamentos/webhook` | — | Webhook Stripe (público) |

A documentação completa com schemas de request/response está disponível no Swagger:
`http://localhost:8080/api/v1/swagger-ui.html`

---

## Integração Stripe

Antes de usar os endpoints de pagamento:

1. Configure `stripe.price.padrao` (em local/prod) com o Price ID do plano único no Stripe.
2. Configure o webhook no [Stripe Dashboard](https://dashboard.stripe.com/webhooks) apontando para `POST /api/v1/pagamentos/webhook`.
3. Copie o `whsec_...` para `stripe.webhook.secret` no `application-local.properties` ou variável `STRIPE_WEBHOOK_SECRET` em produção.

---

## Migrations Flyway

| Arquivo | Descrição |
|---|---|
| `V1__init.sql` | Criação das tabelas `alimentos`, `usuarios` e `equivalencias` |
| `V2__seed_alimentos.sql` | Inserção dos 95 alimentos iniciais (5 grupos alimentares) |
| `V3__add_cpf_usuario.sql` | Adição da coluna `cpf` na tabela `usuarios` |
| `V4__migrate_plano_padrao.sql` | Conversão de planos legados (`FREE/BASIC/PRO`) para `PADRAO` |

---

## Variáveis de ambiente

Usadas em produção via `application-prod.properties`:

| Variável | Descrição |
|---|---|
| `SERVER_PORT` | Porta do servidor (padrão: `8080`) |
| `DATASOURCE_URL` | URL JDBC do banco de dados |
| `DATASOURCE_USERNAME` | Usuário do banco |
| `DATASOURCE_PASSWORD` | Senha do banco |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas (separadas por vírgula) |
| `JWT_SECRET` | Secret para assinar JWT (Base64 forte ou texto >= 32 caracteres) |
| `JWT_EXPIRATION` | Expiração do token em ms (padrão em prod: `3600000` = 1 h) |
| `STRIPE_API_KEY` | Chave secreta do Stripe (`sk_live_...`) |
| `STRIPE_WEBHOOK_SECRET` | Secret do webhook Stripe (`whsec_...`) |
| `STRIPE_PRICE_PADRAO` | Price ID do plano único no Stripe (`price_...`) |

---

## Testes

O projeto possui **17 classes de teste** cobrindo controllers, services, entities, enums e filtros.

```bash
# Executar todos os testes
mvn test

# Executar com relatório de cobertura
mvn verify
```

Estrutura dos testes:

```
src/test/java/br/com/sistema/alimentos/
  ├── config/      → AppConfigTest, GrupoAlimentarConverterTest, OpenApiConfigTest
  ├── controller/  → AuthControllerTest, UsuarioControllerTest, AlimentoControllerTest,
  │                  EquivalenciaControllerTest, PagamentoControllerTest
  ├── entity/      → UsuarioTest, EquivalenciaTest
  ├── enums/       → GrupoAlimentarTest
  ├── filter/      → JwtAuthFilterTest
  └── service/     → AuthServiceTest, UsuarioServiceTest, AlimentoServiceTest,
                     EquivalenciaServiceTest, JwtServiceTest, StripeServiceTest
```

---

## Exemplos de requisições

```bash
# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@exemplo.com", "senha": "minhasenha"}'

# Criar usuário
curl -X POST http://localhost:8080/api/v1/usuarios \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"nome": "João Silva", "email": "joao@exemplo.com", "cpf": "123.456.789-00", "senha": "senha123", "tipo": "NUTRICIONISTA"}'

# Listar alimentos filtrados por grupo (paginado)
curl "http://localhost:8080/api/v1/alimentos?grupo=Frutas&page=0&size=10" \
  -H "Authorization: Bearer <token>"

# Buscar equivalências de um alimento
curl http://localhost:8080/api/v1/equivalencias/alimento/5 \
  -H "Authorization: Bearer <token>"

# Criar equivalência
curl -X POST http://localhost:8080/api/v1/equivalencias \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"alimentoOrigemId": 1, "alimentoDestinoId": 5, "fatorEquivalencia": 1.2, "observacao": "Substituição calórica equivalente"}'

# Desativar usuário
curl -X PATCH http://localhost:8080/api/v1/usuarios/<uuid>/desativar \
  -H "Authorization: Bearer <token>"

# Criar sessão de checkout Stripe
curl -X POST http://localhost:8080/api/v1/pagamentos/checkout \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"successUrl": "http://localhost:4200/sucesso", "cancelUrl": "http://localhost:4200/planos"}'
```

---

## Troubleshooting (Prod)

Erros comuns de inicialização no profile `prod`:

- `Could not resolve placeholder 'JWT_SECRET'`: variável `JWT_SECRET` não definida no ambiente.
- `Driver ... does not accept jdbcUrl, ${DATASOURCE_URL}`: variável `DATASOURCE_URL` ausente ou com valor literal `${DATASOURCE_URL}`.

Exemplo de `DATASOURCE_URL` válido:

```text
jdbc:mysql://SEU_HOST:3306/db_equivalencia_alimentar?createDatabaseIfNotExist=true&serverTimezone=UTC&useSSL=false&allowPublicKeyRetrieval=true
```
