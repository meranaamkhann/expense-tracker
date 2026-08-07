# SpendWise — Expense Tracker
![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3-green)
![Next.js](https://img.shields.io/badge/Next.js-15-black)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue)
![License](https://img.shields.io/badge/License-MIT-lightgrey)
 
 🌐 **Live Demo:** https://spendwise-sibbus.vercel.app/

📖 **API Documentation:** https://spendwise-backend-2y44.onrender.com/swagger-ui.html

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
`/api/admin/users` (list, paginated), `/api/admin/users/{id}/status` (enable/disable), and
`/api/admin/stats` (platform totals) are gated by `hasRole("ADMIN")`. An admin who logs in sees an
"Admin" link in the sidebar leading to a page with these — regular users never see the link and
are redirected away from `/dashboard/admin` if they navigate there directly.

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
- JPA persistence (User, Category, Expense, PasswordResetToken, EmailVerificationToken, RefreshToken, Budget)
- **Flyway migrations** — schema is version-controlled (`V1` through `V4`)
- JWT auth: register, login, refresh-token rotation, logout
- **Multi-session support**: each device/browser gets its own refresh-token row. Logging in on
  your phone no longer silently signs your laptop out — logout revokes just that session
  (or every session, if no token is sent)
- **Account lockout**: 5 failed login attempts locks the account for 15 minutes (on top of the
  per-IP rate limiter, which only throttles, not tracks per-account)
- **Scheduled cleanup** (`TokenCleanupService`, daily): expired/used reset, verification, and
  refresh tokens no longer accumulate forever
- **Delete-my-account**: password-confirmed, irreversible, cascades everywhere
- Email verification (soft) and forgot/reset password, both with a dev-safe "log instead of email" fallback
- Per-user data isolation on every query, verified by an integration test
- Categories: CRUD, duplicate-name prevention, 6 defaults seeded on signup, delete guards
- Expenses: CRUD, pagination (service-level; not yet wired to a controller route or the UI — see below), **CSV export**
- **Budgets**: monthly limit per category, spend/remaining/percent-used computed live against this month's expenses
- Analytics: income/expense/balance totals, top categories, monthly trend
- Admin: user list/enable/disable, platform stats, bootstrap-via-env-var promotion
- DTOs everywhere, centralized exception handling, consistent JSON error shape
- Rate limiting, swappable between in-memory and Redis-backed
- **Security headers**: CSP, HSTS, frame-deny, no-referrer, permissions-policy
- Swagger/OpenAPI at `/swagger-ui.html` with bearer-auth wired in
- Observability: correlation IDs, structured JSON logs in prod, Prometheus metrics
- Tests: unit (JWT, category rules, auth service incl. lockout, budget calculations, rate limiter)
  + integration (auth flow, cross-user isolation, email verification lifecycle, admin access control)
- CORS via env var, stateless sessions, BCrypt (strength 12)
- Docker for both services + Redis, docker-compose orchestration
- CI: backend build+test, frontend lint+build, Docker image builds, on every push/PR
- Currency is INR end-to-end
- k6 load-test script for manual/staging load testing

**Frontend**
- Real backend integration, no localStorage fallback, automatic access-token refresh
- Auth-guarded dashboard routes
- Forgot-password / reset-password / verify-email pages, resend-verification banner
- **Budgets page**: set a monthly limit per category, live progress bars
- **Admin page**: platform stats, user list with enable/disable (admin-only nav link)
- **CSV export button** on the Entries page
- **Delete-my-account** danger zone on the profile page, password-confirmed
- ₹ formatting throughout — same visual design as before, no theme changes

## What's still not done

- **Recurring transactions** (rent, subscriptions) — not implemented
- **PDF export** — CSV only for now
- **Multi-currency selector** — the `currency` field exists per-expense (defaults INR), but there's
  no UI to pick a different one; everything displays as ₹
- **Frontend pagination in the UI** — the backend has a paginated expenses endpoint at the service
  layer, but no controller route exposes it yet and the UI still fetches the full list. Fine at
  personal-use scale; would need work before it scales to thousands of entries
- **Frontend automated tests** (Playwright/Jest) — none yet
- **Sentry / error tracking** — not wired up on either side
- Distributed tracing beyond correlation-id log lines (no OpenTelemetry/Jaeger)
- Automated load-test runs in CI (the k6 script is manual/staging-only, by design)
- API versioning strategy

## Deploying

- **Backend → Render/Railway/Fly.io:** point at `backend/`, it has a Dockerfile. Set the env vars above.
  Attach a managed Postgres and point `DATABASE_URL` at it. Flyway creates the schema on first boot.
  Add a managed Redis if you're running more than one instance and want shared rate limiting.
- **Frontend → Vercel:** point at `frontend/`, set `NEXT_PUBLIC_API_URL` to your deployed backend URL.
- Set `CORS_ALLOWED_ORIGINS`, `FRONTEND_RESET_URL`, and `FRONTEND_VERIFY_URL` on the backend to your
  actual frontend domain once deployed, and real `SMTP_*` credentials if you want emails to actually send.