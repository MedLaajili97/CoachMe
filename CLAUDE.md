# CoachApp — CLAUDE.md

> This file is the source of truth for all development decisions.
> Read it fully before writing any code. Never deviate from it without
> updating it first.

> **Progress tracking:** All phases and issues are tracked in
> `C:\Users\mli\.claude\projects\C---source-CoachMe\memory\project_issues.md`.
> Always read that file at the start of each session to know where we left off,
> and mark issues `[x]` as soon as they are completed.

> **Git policy:** Never run git commands (commit, push, branch, close issues).
> The user manages all git operations themselves.

---

## 1. Project Overview

A multi-tenant private coaching SaaS.
- Coaches manage clients, create plans, and track progress
- Clients self-register via invite links and log their progress
- Each coach operates in an isolated workspace (tenant)
- Core value: structured planning + accountability, not chat

---

## 2. Tech Stack

| Layer          | Technology                        |
|----------------|-----------------------------------|
| Backend        | Spring Boot 3.x, Java 21          |
| Frontend       | Angular 18, TailwindCSS           |
| Database       | PostgreSQL 16                     |
| Auth           | JWT (access + refresh tokens)     |
| Reverse Proxy  | NGINX                             |
| Infra          | Single VPS, Docker Compose        |
| Migrations     | Flyway                            |
| API Docs       | SpringDoc OpenAPI (Swagger UI)    |

---

## 3. Repository Structure

```
/
├── backend/                  # Spring Boot app
│   ├── src/main/java/com/coachapp/
│   │   ├── config/           # Security, tenant, CORS, Flyway config
│   │   ├── controller/       # REST controllers (public + tenant-scoped)
│   │   ├── service/          # Business logic
│   │   ├── repository/       # JPA repositories
│   │   ├── model/            # JPA entities
│   │   ├── dto/              # Request/Response DTOs only (no entities in API)
│   │   ├── tenant/           # Tenant context: resolver, interceptor, provider
│   │   ├── security/         # JWT filter, token service, user details
│   │   └── exception/        # Global exception handler
│   ├── src/main/resources/
│   │   ├── db/migration/public/     # Flyway migrations: public schema
│   │   ├── db/migration/tenant/     # Flyway migrations: per-tenant schema
│   │   └── application.yml
│   └── Dockerfile
│
├── frontend/                 # Angular app
│   ├── src/app/
│   │   ├── core/             # Auth, interceptors, guards, tenant service
│   │   ├── shared/           # Reusable components, pipes, directives
│   │   ├── features/
│   │   │   ├── auth/         # Login, register, invite flow
│   │   │   ├── coach/        # Coach dashboard, client management, plans
│   │   │   └── client/       # Client dashboard, plan view, progress logging
│   │   └── layout/           # Shell, sidebar, navbar, responsive wrappers
│   ├── tailwind.config.js
│   └── Dockerfile
│
├── nginx/
│   ├── nginx.conf            # Subdomain routing, proxy rules, SSL
│   └── certs/                # Let's Encrypt wildcard cert
│
├── docker-compose.yml        # Full local + production stack
├── docker-compose.dev.yml    # Dev overrides (hot reload, no SSL)
└── CLAUDE.md                 # This file
```

---

## 4. Multi-Tenancy Architecture

### Strategy
- **Shared database, separate schema per tenant**
- Tenant = one coach workspace
- Schema naming: `tenant_{tenant_id}`

### How It Works
```
Request hits NGINX
→ subdomain extracted (coachA.app.com → coachA)
→ forwarded to Spring Boot with header: X-Tenant-ID: coachA
→ TenantInterceptor reads header → sets TenantContext (ThreadLocal)
→ HibernateMultiTenantConnectionProvider switches schema
→ All queries run inside tenant_coachA schema
→ ThreadLocal cleared after request (finally block)
```

### Public Schema (shared tables)
```sql
users          -- all users (coaches + clients), role, email, password_hash
tenants        -- id, subdomain, coach_id, name, created_at, status
invitations    -- id, tenant_id, email, token_hash, expires_at, status, role
refresh_tokens -- id, user_id, token_hash, expires_at, revoked
```

### Tenant Schema (per coach workspace)
```sql
clients          -- id, user_id, joined_at, status
plans            -- id, client_id, title, description, status, created_at
phases           -- id, plan_id, title, order_index, start_date, end_date
tasks            -- id, phase_id, title, description, recurring,
                 --   recurrence_type (DAILY/WEEKLY/NONE), due_date
task_completions -- id, task_id, client_id, completed_at, note
progress_logs    -- id, client_id, date, metric_key, value (numeric), note
coach_notes      -- id, client_id, authored_by, content, is_private, created_at
metrics          -- id, name, unit, created_by (coach-defined custom metrics)
```

### Schema Creation
- When a coach registers → Spring service creates their schema immediately
- Flyway applies the tenant migration baseline to the new schema
- Never create schemas manually

---

## 5. Authentication & Security

### Flow
- Login → returns `accessToken` (15min) + `refreshToken` (7d, HttpOnly cookie)
- Access token in `Authorization: Bearer <token>` header
- Refresh via `POST /api/auth/refresh`
- Logout revokes refresh token in DB

### JWT Claims
```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "COACH | CLIENT",
  "tenantId": "coachA",
  "iat": "",
  "exp": ""
}
```

### Rules
- All `/api/auth/**` endpoints are public
- All other endpoints require valid JWT
- Coach endpoints under `/api/coach/**` require role COACH
- Client endpoints under `/api/client/**` require role CLIENT
- TenantInterceptor runs on all `/api/**` except `/api/auth/**`
- Never expose internal IDs in URLs — use UUIDs everywhere

### Invite Flow
1. Coach creates invite → generates UUID token → stored hashed (SHA-256) in DB
2. Email sent with link: `coachA.app.com/join?token=<raw_token>`
3. Client opens link → frontend sends token to `POST /api/auth/verify-invite`
4. Backend hashes token → looks up invitation → validates expiry + status
5. Client completes registration form (email pre-filled, read-only)
6. On submit → `POST /api/auth/register/client` with token + password
7. Token marked as ACCEPTED, tenant schema record created

---

## 6. API Design Rules

- Base path: `/api/v1/`
- Always return consistent response envelope:
```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2025-01-01T00:00:00Z"
}
```
- Error response:
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "INVITATION_EXPIRED",
    "message": "Human readable message"
  }
}
```
- Use DTOs for all inputs and outputs — never expose JPA entities directly
- Validate all inputs with Bean Validation (`@Valid`, `@NotBlank`, etc.)
- Global exception handler in `GlobalExceptionHandler.java` for all error mapping
- Use UUIDs as all public-facing IDs
- Swagger available at `/api/swagger-ui.html` (dev only)

### Key Endpoints (reference)
```
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout
POST   /api/v1/auth/register/coach
POST   /api/v1/auth/verify-invite
POST   /api/v1/auth/register/client

GET    /api/v1/coach/clients
POST   /api/v1/coach/clients/invite
GET    /api/v1/coach/clients/{id}
GET    /api/v1/coach/clients/{id}/plans
POST   /api/v1/coach/plans
PUT    /api/v1/coach/plans/{id}
POST   /api/v1/coach/plans/{id}/phases
POST   /api/v1/coach/phases/{id}/tasks
GET    /api/v1/coach/clients/{id}/progress

GET    /api/v1/client/me
GET    /api/v1/client/plans
GET    /api/v1/client/plans/{id}
POST   /api/v1/client/tasks/{id}/complete
POST   /api/v1/client/progress
GET    /api/v1/client/progress
```

---

## 7. Frontend Architecture Rules

### Routing Structure
```
/login                      → AuthModule (public)
/register                   → AuthModule (public)
/join?token=xxx             → AuthModule (public)

/coach                      → CoachModule (guard: isCoach)
/coach/dashboard            → client overview
/coach/clients/:id          → client detail
/coach/clients/:id/plans    → plan management
/coach/clients/:id/progress → progress view

/client                     → ClientModule (guard: isClient)
/client/dashboard           → today's tasks + streak
/client/plans               → assigned plans
/client/plans/:id           → plan detail
/client/progress            → log + history
```

### Rules
- Use **standalone components** (Angular 18 default)
- Use **signals** for local state, **RxJS** for HTTP streams
- One service per feature domain (AuthService, PlanService, ProgressService…)
- HTTP interceptor adds JWT to all requests automatically
- HTTP interceptor handles 401 → refresh token → retry once → logout
- TenantService reads subdomain on app init → stores in app state
- All forms use **Reactive Forms**, never Template-driven
- Never call HTTP from components — always go through a service
- Mobile-first CSS: design for 375px width, then scale up

### Responsive Breakpoints (Tailwind)
```
sm:  640px   → tablet portrait
md:  768px   → tablet landscape
lg:  1024px  → desktop
xl:  1280px  → wide desktop
```

### Component Conventions
- Smart components in `features/` — handle data, call services
- Dumb/presentational components in `shared/` — inputs/outputs only
- File naming: `kebab-case.component.ts`, `kebab-case.service.ts`

---

## 8. Database Rules

- Always use Flyway for schema changes — never alter schema manually
- Migration naming:
    - Public: `V1__init_public.sql`, `V2__add_refresh_tokens.sql`
    - Tenant: `V1__init_tenant.sql`, `V2__add_metrics.sql`
- All tables have `created_at TIMESTAMP DEFAULT NOW()` and `updated_at`
- All primary keys are UUIDs (`gen_random_uuid()`)
- Never delete records — use `status` or `deleted_at` soft deletes
- Foreign keys always defined explicitly
- All tenant migrations applied to new schemas on tenant creation

---

## 9. Docker & Infrastructure

### Services in docker-compose.yml
```yaml
services:
  postgres:    # PostgreSQL 16
  backend:     # Spring Boot JAR
  frontend:    # NGINX serving Angular build
  nginx:       # Reverse proxy + SSL termination
```

### NGINX Routing
```
*.app.com  → extract subdomain → proxy to backend:8080
           → pass header X-Tenant-ID: {subdomain}
app.com    → proxy to frontend:80
```

### Environment Variables (never hardcode)
```
POSTGRES_URL, POSTGRES_USER, POSTGRES_PASSWORD
JWT_SECRET, JWT_EXPIRY_MS
MAIL_HOST, MAIL_USER, MAIL_PASS
APP_DOMAIN
```

---

## 10. Code Style & Conventions

### Java / Spring Boot
- Package by feature, not by layer (inside each feature, sub-package by layer)
- Constructor injection only — never `@Autowired` on fields
- Services are `@Transactional` at method level, not class level
- No business logic in controllers — controllers are thin
- No JPA entities in DTOs and vice versa — explicit mapping only (MapStruct)
- Exceptions: create domain-specific exceptions (`InvitationExpiredException`, etc.)

### Angular / TypeScript
- Strict mode enabled (`strict: true` in tsconfig)
- No `any` type — ever
- Interfaces for all API response shapes in `core/models/`
- Environment-specific config in `environment.ts` only
- Lazy load all feature modules

---

## 11. What NOT to Do

- ❌ Don't use `@Autowired` field injection
- ❌ Don't expose JPA entities via REST
- ❌ Don't put tenant logic in every service — the interceptor handles it
- ❌ Don't store JWT in localStorage — use memory + HttpOnly cookie for refresh
- ❌ Don't write raw SQL unless Flyway migration
- ❌ Don't create new schemas or tables manually
- ❌ Don't add Kubernetes, Redis, or message queues at this stage
- ❌ Don't build notifications until core flow is complete
- ❌ Don't use template-driven forms in Angular

---

## 12. Current Status & Next Steps

### ✅ Done
- [ ] Nothing yet — project is in planning phase

### 🔨 In Progress
- [ ] CLAUDE.md finalized

### 📋 Build Order
1. Docker Compose + PostgreSQL + NGINX skeleton
2. Flyway public schema migrations
3. Coach registration + JWT auth
4. Tenant creation + schema provisioning
5. Invite system (generate, send, verify)
6. Client registration via invite
7. Plan CRUD (coach side)
8. Phase + Task CRUD
9. Client plan view
10. Progress logging (client side)
11. Progress dashboard (coach side)
12. Responsive polish pass
```

---

## How to Use It

- **Keep it updated** — every time a decision changes, update CLAUDE.md first, then code
- **Reference it explicitly** in prompts: *"Following CLAUDE.md conventions, implement..."*
- **Put the build order at the bottom** and check items off as you go — Claude Code uses it to avoid building things out of sequence
- **Add gotchas as you find them** — e.g. *"Flyway tenant migrations must be applied in TenantProvisioningService.java, not on startup"*

---
