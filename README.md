# Fila de Atendimento API

API REST para gestão de fila de atendimento com senhas comuns e prioritárias, feita
com **Java 21 + Spring Boot 3.5**, **PostgreSQL** (docker-compose) e autenticação
**JWT Bearer** com autorização por papéis.

É uma recriação melhorada de um MVP existente: mesma proposta de negócio
(geração e acompanhamento de senhas de espera), com arquitetura e segurança refeitas.

## 🧩 Funcionalidades

- **Autenticação Bearer (JWT)**: login por credenciais, token com 1h de validade
  (configurável), claims de identidade e papel, segredo validado no startup.
- **Papéis** (`USER`, `ATTENDANT`, `ADMIN`):

  | Ação                                     | USER | ATTENDANT | ADMIN |
  |------------------------------------------|:----:|:---------:|:-----:|
  | Emitir / consultar senhas                | ✅   | ✅        | ✅    |
  | Listar chamadas recentes                 | ✅   | ✅        | ✅    |
  | Chamar próxima senha                     | ❌   | ✅        | ✅    |
  | Cancelar senha individual                | ❌   | ✅        | ✅    |
  | Excluir senha permanentemente            | ❌   | ❌        | ✅    |
  | Limpar a fila                            | ❌   | ❌        | ✅    |
  | Listar usuários                          | ❌   | ❌        | ✅    |

- **Emissão de senhas** com número sequencial global ("001", "002", ...) reservado
  atomicamente (linhas com lock pessimista).
- **Política de justiça prioritária**: o atendente chama prioritárias em sequência até
  um teto configurável (padrão: 3); depois uma senha comum entra automaticamente,
  evitando inanição da fila comum. Config: `attendance.max-consecutive-priority`.
- **Consulta por posição** (1-based) nas duas filas e estado da fila com totais.
- **Cancelamento mantendo o registro** (auditável) e exclusão definitiva (ADMIN).
- **Histórico de chamadas** com timestamp preciso (não é string formatada).
- **OpenAPI/Swagger UI** com esquema `bearerAuth` e respostas documentadas.
- **Erros padronizados em JSON** (`status`, `message`, `path`, `errors`) para
  400/401/403/404/409/500 — inclusive rotas inexistentes e corpo malformados.

## 🚀 Como rodar

### Docker Compose (recomendado)

```bash
docker compose up -d --build
```

Sobe dois serviços: `postgres` (17-alpine, com healthcheck + volume persistente) e
`api` (porta 8080). Usuários iniciais de desenvolvimento (criados no primeiro boot):

- ADMIN: `admin@fila.dev` / `SenhaDeAdmin@Dev`
- ATENDENTE: `atendente@fila.dev` / `SenhaDeAtendente@Dev`

> ⚠️ Em qualquer ambiente real, troque essas senhas e defina `JWT_SECRET`
> (mínimo de 32 caracteres) via variável de ambiente.

### Local

```bash
mvn test                  # bateria completa de testes (H2, sem Docker)
mvn spring-boot:run       # conecta no PostgreSQL local (suba o compose)
```

## 📚 Documentação interativa

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## 🔌 Principais endpoints (`/api/v1`)

| Método | Rota                      | Descrição                            | Papel                  |
|--------|---------------------------|--------------------------------------|------------------------|
| POST   | `/auth/login`             | Login → token Bearer                 | público                |
| POST   | `/users`                  | Cadastro público (sempre papel USER) | público                |
| GET    | `/users`                  | Listar usuários                      | ADMIN                  |
| GET    | `/queue`                  | Estado da fila (aguardando + totais) | qualquer autenticado   |
| POST   | `/queue/calls`            | Chamar próxima senha                 | ATTENDANT ou ADMIN     |
| GET    | `/queue/calls?limit=10`   | Histórico de chamadas                | qualquer autenticado   |
| POST   | `/queue/clear`            | Cancelar tudo que está aguardando    | ADMIN                  |
| POST   | `/tickets`                | Emitir senha                         | qualquer autenticado   |
| GET    | `/tickets/{id}`           | Consultar senha por id               | qualquer autenticado   |
| GET    | `/tickets/position/{pos}` | Consultar por posição da fila        | qualquer autenticado   |
| POST   | `/tickets/{id}/cancel`    | Cancelar senha (mantém registro)     | ATTENDANT ou ADMIN     |
| DELETE | `/tickets/{id}`           | Excluir registro                     | ADMIN                  |

Respostas de erro seguem sempre o formato:

```json
{
  "occurredAt": "2026-11-10T15:32:33Z",
  "status": 409,
  "message": "Não é possível cancelar: a senha 007 está atualmente no estado CANCELLED",
  "path": "/api/v1/tickets/42/cancel",
  "errors": null
}
```

## 🏗️ Arquitetura

Organização "package by feature", com DDD pragmático na entidade principal:

```
com.filaatendimento
├── attendance/       # núcleo da fila: Ticket, TicketStatus, PriorityPolicy...
│   ├── domain/       # Ticket (aggregate root: valida transições de estado)
│   ├── application/  # QueueService, MaxConsecutivePriorityPolicy, propriedades
│   ├── persistence/  # TicketRepository (locks pessimistas), TicketNumberSequence
│   └── api/          # controllers + DTOs (records) de entrada/saída
├── access/           # usuários: User, UserRole, cadastro e login
│   ├── domain/       # User (hash BCrypt dentro do agregado), UserRole
│   ├── application/  # AuthenticationService, UserRegistrationService, bootstrap
│   ├── persistence/  # UserRepository, adaptador BCrypt
│   └── api/          # endpoints de auth/registro/listagem
├── security/         # JwtProperties, JwtTokenService, JwtAuthenticationFilter,
│                     # SecurityConfiguration (rotas + papéis), CORS por props, erros 401/403
└── common/           # NotFoundException/ConflictException + GlobalExceptionHandler
```

Decisões de arquitetura (o que melhorou em relação ao MVP base):

1. **Estado da fila vive só no banco** — o MVP base mantinha `LinkedHashSet`s em
   memória sincronizados com o banco (quebra com más instâncias da API e perde
   consistência). Aqui todo estado deriva do PostgreSQL, com lock pessimista para
   chamada concorrente.
2. **Ticket como agregado rico** — as transições (`WAITING → CALLED`,
   `WAITING/CALLED → CANCELLED`) são métodos do agregado que validam as regras e
   guardam `calledAt`/`cancelledAt` reais, em vez de uma string `dd/MM/yyyy` solta.
3. **Política de prioridade testável** — `MaxConsecutivePriorityPolicy` implementa
   `PriorityPolicy`, substituindo a escolha aleatória (`ThreadLocalRandom`) do MVP base.
4. **Erros padronizados** — `GlobalExceptionHandler` centraliza tudo em JSON.
5. **Sem estados redundantes** — saiu o par `CANCELADA`/`REMOVIDA` do MVP base
   (semântica confusa); apenas `WAITING`, `CALLED` e `CANCELLED`.
6. **Papéis protegidos** — registro público cria sempre `USER`; `ATTENDANT` e `ADMIN`
   só existem via bootstrap de configuração, sem escalada de privilégio.
7. **Números de senha atômicos** — sequência própria com lock por linha, em vez de
   `parseInt(último número) + 1` (condição de corrida do MVP base).

## 🔐 Segurança

- **JWT HS256**: segredo de no mínimo 32 caracteres validado no startup (a aplicação
  não sobe com segredo fraco), validação de assinatura, issuer e expiração, relógio
  injetado (testável).
- **Senhas BCrypt (força 12)**, nunca armazenadas ou logadas em texto puro.
- **Stateless**: sem sessão HTTP; CSRF desligado por ser API exclusivamente Bearer.
- **401/403 em JSON** para requisições sem token ou com papel insuficiente.
- **CORS configurável** por propriedade (`security.cors.allowed-origins`).
- **Login raso** ("Login ou senha inválidos"), sem informar qual parte falhou, para
  não permitir sondar logins existentes.
- **Papéis aplicados nas rotas** e claims de identidade no token.

## ✅ Testes (53)

- **Domínio**: `Ticket` (transições e regras de estado), `MaxConsecutivePriorityPolicy`
  (teto de 3, teto 0 desativa).
- **Aplicação (Mockito)**: `QueueService` (política de prioridade, fallback entre filas,
  contagem de prioritárias consecutivas, cancelamento e limpeza), `AuthenticationService`
  (erro raso; senha errada nunca emite token), `UserRegistrationService` (hash + papel
  USER, conflito de login).
- **Segurança**: `JwtTokenService` com relógio fixo (round-trip de claims, token
  expirado, assinatura estrangeira, issuer estrangeiro, segredo curto proibido).
- **Integração (HTTP real + H2)**:
  - `AttendanceFlowIntegrationTest` — fluxo completo: cadastro, login, emissão,
    chamadas na ordem justa (prioritária, prioritária, comum, prioritária),
    estado da fila, consulta por posição, histórico, limpeza da fila e exclusão ADMIN.
  - `RolesAuthorizationIntegrationTest` — 401/403 por papel, token adulterado,
    cancelamento único e 409 ao cancelar duas vezes, listagem de usuários ADMIN.
  - `ValidationIntegrationTest` — 400 com detalhes por campo, sem vazar 500.

```bash
mvn test
```

## ⚠️ Limitações conhecidas

- Sem refresh/revogação de tokens (MVP): logout significa esperar a expiração de 1h;
  rotação do segredo revoga todas as sessões.
- Sem atualização em tempo real (SSE/WebSocket): a tela de exibição precisa consultar
  `GET /queue` periodicamente.
- A política de prioridade é global, não por guichê.
