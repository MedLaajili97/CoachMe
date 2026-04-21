# CoachMe

A multi-tenant private coaching SaaS platform. Coaches manage clients, create structured plans, and track progress. Clients self-register via invite links and log their own progress. Each coach operates in a fully isolated workspace.

---

## Tech Stack

| Layer         | Technology                     |
|---------------|--------------------------------|
| Backend       | Spring Boot 3.x, Java 21       |
| Frontend      | Angular 18, TailwindCSS        |
| Database      | PostgreSQL 16                  |
| Auth          | JWT (access + refresh tokens)  |
| Reverse Proxy | NGINX                          |
| Infra         | Single VPS, Docker Compose     |
| Migrations    | Flyway                         |
| API Docs      | SpringDoc OpenAPI (Swagger UI) |

---

## Project Structure

```
/
├── backend/                  # Spring Boot app
│   ├── src/main/java/com/coachapp/
│   │   ├── config/           # Security, tenant, CORS, Flyway config
│   │   ├── controller/       # REST controllers (public + tenant-scoped)
│   │   ├── service/          # Business logic
│   │   ├── repository/       # JPA repositories
│   │   ├── model/            # JPA entities
│   │   ├── dto/              # Request/Response DTOs
│   │   ├── tenant/           # Tenant context: resolver, interceptor, provider
│   │   ├── security/         # JWT filter, token service, user details
│   │   └── exception/        # Global exception handler
│   └── src/main/resources/
│       ├── db/migration/public/   # Flyway migrations: public schema
│       ├── db/migration/tenant/   # Flyway migrations: per-tenant schema
│       └── application.yml
│
├── frontend/                 # Angular 18 app
│   └── src/app/
│       ├── core/             # Auth, interceptors, guards, tenant service
│       ├── shared/           # Reusable components, pipes, directives
│       ├── features/
│       │   ├── auth/         # Login, register, invite flow
│       │   ├── coach/        # Coach dashboard, client management, plans
│       │   └── client/       # Client dashboard, plan view, progress logging
│       └── layout/           # Shell, sidebar, navbar
│
├── nginx/
│   ├── nginx.conf            # Subdomain routing, proxy rules, SSL
│   └── certs/                # Let's Encrypt wildcard cert
│
├── docker-compose.yml        # Full stack (production)
└── docker-compose.dev.yml    # Dev overrides (hot reload, no SSL)
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 21 (for local backend development)
- Node.js 20+ (for local frontend development)

### Run with Docker (recommended)

```bash
# Development (hot reload, no SSL)
docker compose -f docker-compose.yml -f docker-compose.dev.yml up

# Production
docker compose up -d
```

### Environment Variables

Copy and configure before running:

```bash
cp .env.example .env
```

| Variable            | Description                      |
|---------------------|----------------------------------|
| `POSTGRES_URL`      | JDBC connection URL              |
| `POSTGRES_USER`     | Database user                    |
| `POSTGRES_PASSWORD` | Database password                |
| `JWT_SECRET`        | Secret key for signing JWTs      |
| `JWT_EXPIRY_MS`     | Access token TTL in milliseconds |
| `MAIL_HOST`         | SMTP host                        |
| `MAIL_USER`         | SMTP user                        |
| `MAIL_PASS`         | SMTP password                    |
| `APP_DOMAIN`        | Base domain (e.g. `app.com`)     |

### Local Backend (without Docker)

```bash
cd backend
./mvnw spring-boot:run
```

### Local Frontend (without Docker)

```bash
cd frontend
npm install
ng serve
```

API docs: `http://localhost:8080/api/swagger-ui.html` (dev mode only)

---

## Multi-Tenancy

The app uses a **shared database, separate PostgreSQL schema per tenant** strategy. Each coach workspace is a tenant identified by subdomain.

```
Request → NGINX
  → extracts subdomain (coachA.app.com → "coachA")
  → forwards with header X-Tenant-ID: coachA
  → TenantInterceptor sets ThreadLocal context
  → Hibernate switches to tenant_coachA schema
  → ThreadLocal cleared after request
```

Tenant schemas are provisioned automatically when a coach registers. Never create or modify schemas manually — Flyway handles all migrations.

---

## Authentication

- **Login** returns an `accessToken` (15 min, in response body) and a `refreshToken` (7 days, HttpOnly cookie).
- All requests to `/api/**` except `/api/auth/**` require `Authorization: Bearer <accessToken>`.
- Token refresh: `POST /api/v1/auth/refresh`
- Logout revokes the refresh token in the database.

### Client Invite Flow

1. Coach creates an invite → a UUID token is generated and stored hashed (SHA-256).
2. Client receives a link: `coachA.app.com/join?token=<raw_token>`
3. Client visits the link → token verified via `POST /api/v1/auth/verify-invite`
4. Client sets a password → registered via `POST /api/v1/auth/register/client`
5. Invitation marked ACCEPTED, client record created in the tenant schema.

---

## API Reference

Base path: `/api/v1/`

All responses use a consistent envelope:

```json
{
  "success": true,
  "data": {},
  "error": null,
  "timestamp": "2025-01-01T00:00:00Z"
}
```

### Auth Endpoints

| Method | Endpoint                         | Description                    |
|--------|----------------------------------|--------------------------------|
| POST   | `/api/v1/auth/login`             | Login, returns tokens          |
| POST   | `/api/v1/auth/refresh`           | Refresh access token           |
| POST   | `/api/v1/auth/logout`            | Revoke refresh token           |
| POST   | `/api/v1/auth/register/coach`    | Coach self-registration        |
| POST   | `/api/v1/auth/verify-invite`     | Validate invite token          |
| POST   | `/api/v1/auth/register/client`   | Client registration via invite |

### Coach Endpoints

| Method | Endpoint                               | Description            |
|--------|----------------------------------------|------------------------|
| GET    | `/api/v1/coach/clients`                | List all clients       |
| POST   | `/api/v1/coach/clients/invite`         | Send client invite     |
| GET    | `/api/v1/coach/clients/{id}`           | Client detail          |
| GET    | `/api/v1/coach/clients/{id}/plans`     | Client's plans         |
| POST   | `/api/v1/coach/plans`                  | Create plan            |
| PUT    | `/api/v1/coach/plans/{id}`             | Update plan            |
| POST   | `/api/v1/coach/plans/{id}/phases`      | Add phase to plan      |
| POST   | `/api/v1/coach/phases/{id}/tasks`      | Add task to phase      |
| GET    | `/api/v1/coach/clients/{id}/progress`  | View client progress   |

### Client Endpoints

| Method | Endpoint                               | Description              |
|--------|----------------------------------------|--------------------------|
| GET    | `/api/v1/client/me`                    | Current client profile   |
| GET    | `/api/v1/client/plans`                 | Assigned plans           |
| GET    | `/api/v1/client/plans/{id}`            | Plan detail              |
| POST   | `/api/v1/client/tasks/{id}/complete`   | Mark task complete       |
| POST   | `/api/v1/client/progress`              | Log a progress entry     |
| GET    | `/api/v1/client/progress`              | Progress history         |

---

## Database Schema

### Public Schema (shared across all tenants)

| Table            | Purpose                                               |
|------------------|-------------------------------------------------------|
| `users`          | All users (coaches + clients), role, email, password  |
| `tenants`        | Coach workspaces: subdomain, coach_id, status         |
| `invitations`    | Invite tokens (stored hashed), expiry, status         |
| `refresh_tokens` | Refresh token hashes, revocation state                |

### Tenant Schema (`tenant_{tenant_id}`)

| Table              | Purpose                                          |
|--------------------|--------------------------------------------------|
| `clients`          | Coach's clients, linked to `users`               |
| `plans`            | Coaching plans assigned to clients               |
| `phases`           | Ordered phases within a plan                     |
| `tasks`            | Tasks within phases (supports daily/weekly recurrence) |
| `task_completions` | Client task completion records                   |
| `progress_logs`    | Numeric metric entries per client                |
| `coach_notes`      | Private/shared notes on clients                  |
| `metrics`          | Coach-defined custom progress metrics            |

All primary keys are UUIDs. All tables include `created_at` and `updated_at`. Records are soft-deleted via `status` or `deleted_at` — never hard-deleted.

---

## Frontend Routes

```
/login                        → public
/register                     → public
/join?token=xxx               → public (invite flow)

/coach/dashboard              → coach: client overview
/coach/clients/:id            → coach: client detail
/coach/clients/:id/plans      → coach: plan management
/coach/clients/:id/progress   → coach: progress view

/client/dashboard             → client: today's tasks + streak
/client/plans                 → client: assigned plans
/client/plans/:id             → client: plan detail
/client/progress              → client: log + history
```

---

## Infrastructure

NGINX handles SSL termination and subdomain-based routing:

```
*.app.com  →  backend:8080   (with X-Tenant-ID header injected)
app.com    →  frontend:80
```

Docker Compose services: `postgres`, `backend`, `frontend`, `nginx`.