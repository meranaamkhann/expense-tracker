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
docker-compose.yml       Postgres + backend + frontend, for local/prod-like runs
.github/workflows/ci.yml Build + test on every push
```

## Run it locally (no Docker)

**Backend** — needs Java 17 + Maven. Uses an embedded H2 file database in `dev` mode (Postgres-compatible
mode, so the same Flyway migrations run against both), so there's nothing else to install.

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
six starter categories already created.

## Run the tests

```bash
cd backend
mvn test
```

Covers: JWT generation/validation, category business rules (duplicate names, delete guards),
auth service logic, and full integration flows through real HTTP calls — register → login →
access a protected route, and a dedicated test that proves **user A can never see or delete
user B's expenses**.

## Run it with Docker

```bash
cp .env.example .env
# edit .env: set JWT_SECRET and DATABASE_PASSWORD to real values
docker compose up --build
```

This starts Postgres, the backend (`prod` profile), and the frontend together.

## Environment variables (backend)

| Variable | Required in prod | Notes |
|---|---|---|
| `JWT_SECRET` | Yes | Random string, 32+ chars. Generate with `openssl rand -base64 48`. |
| `JWT_ACCESS_TTL_MINUTES` | No (default 15) | Access token lifetime. |
| `JWT_REFRESH_TTL_DAYS` | No (default 7) | Refresh token lifetime. |
| `DATABASE_URL` | Yes | e.g. `jdbc:postgresql://host:5432/expensetracker` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | Yes | Postgres credentials. |
| `CORS_ALLOWED_ORIGINS` | Yes | Comma-separated list, e.g. your Vercel URL. |
| `SPRING_PROFILES_ACTIVE` | Yes | Set to `prod`. |
| `SMTP_HOST` / `SMTP_PORT` / `SMTP_USERNAME` / `SMTP_PASSWORD` | No | For real password-reset emails. If `SMTP_HOST` is blank, reset links are logged instead of emailed — fine for demos/dev. |
| `MAIL_FROM` | No | From address for reset emails. |
| `FRONTEND_RESET_URL` | No | Where reset links point, e.g. `https://yourapp.com/reset-password`. |

Copy `backend/.env.example` for the full list.

## What's implemented

**Backend**
- JPA persistence (User, Category, Expense, PasswordResetToken) replacing the old in-memory ArrayList
- **Flyway migrations** (`db/migration/V1__init.sql`) — schema is version-controlled, not auto-generated
- JWT auth: register, login, refresh-token rotation (hashed server-side, single-use), logout
- **Forgot / reset password** flow (time-limited single-use tokens; emails via SMTP if configured, otherwise logged for local dev)
- Per-user data isolation on every query (verified by an integration test, not just assumed)
- Categories: CRUD, duplicate-name prevention, 6 defaults seeded on signup, can't delete a category with expenses attached or your last remaining one
- Expenses: CRUD, pagination support, income/expense kind
- Analytics endpoint: income/expense/balance totals, top categories, monthly trend
- DTOs everywhere — entities never serialize directly over the API
- Centralized exception handling with consistent JSON error shape
- **Rate limiting** on `/api/auth/login`, `/register`, `/refresh` (20 req/min per IP, in-memory)
- **Swagger / OpenAPI** at `/swagger-ui.html`, with bearer-auth wired in so you can try endpoints directly
- **Tests**: unit tests (JWT service, category rules, auth service) + integration tests (full auth flow, cross-user isolation, unauthorized access) via MockMvc
- CORS configured via env var, stateless sessions, BCrypt password hashing (strength 12)
- Docker for both services, docker-compose for local/prod-like orchestration
- **CI** (`.github/workflows/ci.yml`): backend build+test, frontend lint+build, Docker image builds — runs on every push/PR
- Currency is INR end-to-end (backend defaults, frontend formatting)

**Frontend**
- Real backend integration (no localStorage fallback) with automatic access-token refresh
- Auth-guarded dashboard routes
- Forgot-password / reset-password pages
- ₹ formatting throughout

## What's still not done

Being upfront about the remaining gaps rather than pretending they're covered:

- **Email verification on signup** (accounts are usable immediately; only password-reset emails exist)
- **Multi-instance rate limiting** — the current limiter is in-memory per instance; put it behind a shared store (Redis) if you run more than one backend instance
- **Load/perf testing**, structured request logging/correlation IDs, and full observability (metrics/tracing) beyond the basic `/actuator/health`
- **Role-based endpoints for ADMIN** — the `Role` enum and `ROLE_ADMIN` authority exist, but there's no admin-only functionality wired up yet (e.g. user management)

## Deploying

- **Backend → Render/Railway/Fly.io:** point at `backend/`, it has a Dockerfile. Set the env vars above.
  Attach a managed Postgres and point `DATABASE_URL` at it. Flyway will create the schema automatically
  on first boot.
- **Frontend → Vercel:** point at `frontend/`, set `NEXT_PUBLIC_API_URL` to your deployed backend URL.
- Remember to set `CORS_ALLOWED_ORIGINS` and `FRONTEND_RESET_URL` on the backend to your actual frontend
  domain once deployed, and real `SMTP_*` credentials if you want password-reset emails to actually send.
