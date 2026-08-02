# SpendWise — Expense Tracker

A full-stack expense tracker: Next.js/React frontend + Spring Boot backend with real persistence
(PostgreSQL/H2), JWT authentication, and per-user data isolation. All amounts are in **INR (₹)**.

## Stack

- **Backend:** Spring Boot 3.3, Spring Data JPA (Hibernate), Spring Security (stateless JWT), PostgreSQL (prod) / H2 (dev)
- **Frontend:** Next.js 15, React, Tailwind CSS, axios

## Project layout

```
backend/    Spring Boot API (port 8080)
frontend/   Next.js app (port 3000)
docker-compose.yml   Postgres + backend + frontend, for local/prod-like runs
```

## Run it locally (no Docker)

**Backend** — needs Java 17 + Maven. Uses an embedded H2 file database in `dev` mode, so there's
nothing else to install.

```bash
cd backend
mvn spring-boot:run
```

The API comes up on `http://localhost:8080`. A dev-only JWT secret is baked in for convenience —
**never use it in production** (the `prod` profile refuses to start without `JWT_SECRET` set).

**Frontend** — needs Node 20+.

```bash
cd frontend
npm install
npm run dev
```

Visit `http://localhost:3000`. Register a new account — you'll land straight in the dashboard with
six starter categories already created.

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

Copy `backend/.env.example` for the full list.

## What's implemented

- JPA persistence (User, Category, Expense) replacing the old in-memory ArrayList
- JWT auth: register, login, refresh-token rotation (hashed server-side, single-use), logout
- Per-user data isolation on every query (no cross-user leaks)
- Categories: CRUD, duplicate-name prevention, 6 defaults seeded on signup, can't delete a category with expenses attached or your last remaining one
- Expenses: CRUD, pagination support, income/expense kind
- Analytics endpoint: income/expense/balance totals, top categories, monthly trend
- DTOs everywhere — entities never serialize directly over the API
- Centralized exception handling with consistent JSON error shape
- CORS configured via env var, stateless sessions, BCrypt password hashing
- Docker for both services, docker-compose for local/prod-like orchestration
- Currency is INR end-to-end (backend defaults, frontend formatting)

## What's intentionally not done yet

This was a large ask and these are real, separate efforts — flagging them honestly rather than
pretending they're covered:

- **Automated tests** (unit/integration/controller/security) — only the default Spring Boot smoke test exists
- **Swagger/OpenAPI docs**
- **CI/CD** (GitHub Actions) — not set up
- **Rate limiting**
- **Database migrations** (Flyway/Liquibase) — currently Hibernate `ddl-auto: update` manages the schema; fine for getting started, but swap this in before a real production launch
- Email verification / "forgot password" flow

## Deploying

- **Backend → Render/Railway/Fly.io:** point at `backend/`, it has a Dockerfile. Set the env vars above.
  Attach a managed Postgres and point `DATABASE_URL` at it.
- **Frontend → Vercel:** point at `frontend/`, set `NEXT_PUBLIC_API_URL` to your deployed backend URL.
- Remember to set `CORS_ALLOWED_ORIGINS` on the backend to your actual frontend domain once deployed.
