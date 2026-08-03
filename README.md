# SpendWise — Expense Tracker

A full-stack expense tracker: Next.js/React frontend + Spring Boot backend with real persistence
(PostgreSQL/H2), JWT authentication, and per-user data isolation. All amounts are in **INR (₹)**.

## Stack

- **Backend:** Spring Boot 3.3, Spring Data JPA (Hibernate), Flyway, Spring Security (stateless JWT), PostgreSQL (prod) / H2 (dev), springdoc-openapi
- **Frontend:** Next.js 15, React, Tailwind CSS, axios

## Project layout

```
backend/    Spring Boot API (port 8080)
frontend/   Next.js app (port 3000)
docker-compose.yml       Postgres + Redis + backend + frontend
.github/workflows/ci.yml Build + test on every push
```

## Run it locally (no Docker)

**Backend** — needs Java 17 + Maven. Uses an embedded H2 file database in `dev` mode (Postgres-compatible
mode, so the same Flyway migrations run against both), so there's nothing else to install — no Redis,
no SMTP, no Postgres needed to get started.

```bash
cd backend
mvn spring-boot:run
```

The API comes up on `http://localhost:8080`. A dev-only JWT secret is baked in for convenience —
**never use it in production** (the `prod` profile refuses to start without `JWT_SECRET` set).

API docs: `http://localhost:8080/swagger-ui.html`

**Frontend** — needs Node 20+.

```bash
cd frontend
npm install
npm run dev
```

Visit `http://localhost:3000`. Register a new account — you'll land straight in the dashboard with
six starter categories already created. A verification email link gets logged to the backend
console (no SMTP needed for local dev — see "Email" below).

## Run the tests

```bash
cd backend
mvn test
```

Covers: JWT generation/validation, category business rules, rate-limiter behavior, auth service
logic, and full integration flows through real HTTP calls — register → login → access a protected
route, a test proving **user A can never see or delete user B's expenses**, email-verification
token lifecycle (valid/expired/replayed), and admin-endpoint access control (anonymous → 401,
regular user → 403).

## Run it with Docker

```bash
cp .env.example .env
# edit .env: set JWT_SECRET and DATABASE_PASSWORD to real values
docker compose up --build
```

This starts Postgres, Redis, the backend (`prod` profile, Redis-backed rate limiting), and the
frontend together.

## Environment variables (backend)

| Variable | Required in prod | Notes |
|---|---|---|
| `JWT_SECRET` | Yes | Random string, 32+ chars. Generate with `openssl rand -base64 48`. |
| `JWT_ACCESS_TTL_MINUTES` / `JWT_REFRESH_TTL_DAYS` | No | Defaults: 15 min / 7 days. |
| `DATABASE_URL` / `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Yes | Postgres connection. |
| `CORS_ALLOWED_ORIGINS` | Yes | Comma-separated list, e.g. your Vercel URL. |
| `SPRING_PROFILES_ACTIVE` | Yes | Set to `prod`. |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | No | If `SMTP_HOST` is blank, reset/verification links are logged instead of emailed. |
| `MAIL_FROM` | No | From address for emails. |
| `FRONTEND_RESET_URL` / `FRONTEND_VERIFY_URL` | No | Where those email links point. |
| `ADMIN_EMAILS` | No | Comma-separated. Promotes existing accounts to admin **on next restart**. |
| `RATE_LIMIT_BACKEND` | No | `memory` (default) or `redis`. See "Rate limiting" below. |
| `RATE_LIMIT_MAX_PER_MINUTE` | No | Default 20, per client IP, on login/register/refresh only. |
| `REDIS_HOST` / `REDIS_PORT` / `REDIS_PASSWORD` | Only if `RATE_LIMIT_BACKEND=redis` | Ignored otherwise — Redis isn't even connected to unless you opt in. |

Copy `backend/.env.example` for the full list.

## Rate limiting

Default is an in-memory fixed-window limiter (20 req/min/IP on `/login`, `/register`, `/refresh`) —
fine for one instance, but each process counts independently. If you run more than one backend
instance behind a load balancer, set `RATE_LIMIT_BACKEND=redis` so all instances share the same
counter. **Redis is never touched unless you explicitly opt in** — its Spring Boot auto-config is
excluded by default specifically so a missing Redis instance can't break normal startup.

## Email

Password reset and "confirm your email" both work without any SMTP setup: if `SMTP_HOST` is blank,
the backend logs the link instead of emailing it (check the console/logs). Email verification does
**not** block login — new accounts work immediately; unverified users just see a dismissible banner
in the dashboard with a resend button. This was a deliberate choice for a personal finance tool
where login friction has a real cost; flip it to a hard gate in `UserPrincipal.isEnabled()` /
`AuthService` if your use case needs it.

## Admin

There's no self-service "become admin" button — that would be a privilege-escalation hole. To make
an account an admin: register normally, add its email to `ADMIN_EMAILS`, and restart the backend.
`/api/admin/users` (list, paginated) and `/api/admin/users/{id}/status` (enable/disable) and
`/api/admin/stats` (platform totals) are then available, gated by `hasRole("ADMIN")`. There's no
frontend admin UI yet — these are API-only for now.

## Load testing

A k6 smoke/load script lives at `backend/loadtest/smoke.js` (register → login → create expense →
list → analytics, with pass/fail checks and latency thresholds). It's not run in CI — it needs a
live server and is meant for manual/staging use:

```bash
k6 run backend/loadtest/smoke.js
BASE_URL=https://your-api.example.com k6 run --vus 20 --duration 60s backend/loadtest/smoke.js
```

## Observability

- `/actuator/health`, `/actuator/metrics`, `/actuator/prometheus` (behind auth by default — open
  them up or put a scraper with credentials in front, depending on your setup)
- Every request gets a correlation id (`X-Request-Id` request/response header + logging MDC), and
  one summary log line (method, path, status, duration)
- JSON-structured logs in `prod` (Logstash encoder), human-readable in `dev`

## What's implemented

**Backend**
- JPA persistence (User, Category, Expense, PasswordResetToken, EmailVerificationToken)
- **Flyway migrations** — schema is version-controlled (`V1__init.sql`, `V2__email_verification.sql`)
- JWT auth: register, login, refresh-token rotation (hashed, single-use), logout
- **Email verification** (soft — doesn't block login) and **forgot/reset password**, both with
  time-limited single-use tokens and a dev-safe "log instead of email" fallback
- Per-user data isolation on every query, verified by an integration test
- Categories: CRUD, duplicate-name prevention, 6 defaults seeded on signup, delete guards
- Expenses: CRUD, pagination, income/expense kind
- Analytics: income/expense/balance totals, top categories, monthly trend
- **Admin**: user list/enable/disable, platform stats, bootstrap-via-env-var promotion
- DTOs everywhere, centralized exception handling, consistent JSON error shape
- **Rate limiting**, swappable between in-memory and Redis-backed
- **Swagger/OpenAPI** at `/swagger-ui.html` with bearer-auth wired in
- **Observability**: correlation IDs, structured JSON logs in prod, Prometheus metrics
- **Tests**: unit (JWT, category rules, auth service, rate limiter) + integration (auth flow,
  cross-user isolation, email verification lifecycle, admin access control) via MockMvc
- CORS via env var, stateless sessions, BCrypt (strength 12)
- Docker for both services + Redis, docker-compose orchestration
- **CI**: backend build+test, frontend lint+build, Docker image builds, on every push/PR
- Currency is INR end-to-end
- **k6 load-test script** for manual/staging load testing

**Frontend**
- Real backend integration, no localStorage fallback, automatic access-token refresh
- Auth-guarded dashboard routes
- Forgot-password / reset-password / verify-email pages, resend-verification banner
- ₹ formatting throughout

## What's still not done

- **Distributed tracing** beyond correlation-id log lines (no OpenTelemetry/Jaeger wiring)
- **Frontend admin UI** — the admin API exists; there's no dashboard screen for it yet
- **Automated load-test runs in CI** — the k6 script is there but intentionally manual
- Multi-region/multi-tenant concerns, WebSocket/real-time updates, CSV/PDF export

## Deploying

- **Backend → Render/Railway/Fly.io:** point at `backend/`, it has a Dockerfile. Set the env vars above.
  Attach a managed Postgres and point `DATABASE_URL` at it. Flyway creates the schema on first boot.
  Add a managed Redis if you're running more than one instance and want shared rate limiting.
- **Frontend → Vercel:** point at `frontend/`, set `NEXT_PUBLIC_API_URL` to your deployed backend URL.
- Set `CORS_ALLOWED_ORIGINS`, `FRONTEND_RESET_URL`, and `FRONTEND_VERIFY_URL` on the backend to your
  actual frontend domain once deployed, and real `SMTP_*` credentials if you want emails to actually send.
