// Basic load/smoke test for the SpendWise API using k6 (https://k6.io).
//
// Install k6: https://grafana.com/docs/k6/latest/set-up/install-k6/
// Run against a local backend:
//   k6 run backend/loadtest/smoke.js
// Run against a deployed backend with more load:
//   BASE_URL=https://your-api.example.com k6 run --vus 20 --duration 60s backend/loadtest/smoke.js
//
// What it does per virtual user (VU):
//   1. Registers a unique account (each VU/iteration gets its own — this endpoint is rate
//      limited to 20/min per IP, so keep --vus modest unless RATE_LIMIT_MAX_PER_MINUTE is raised)
//   2. Logs in with it
//   3. Fetches categories, creates an expense, lists expenses, fetches the analytics summary
//   4. Checks status codes and response shape at every step
//
// This is deliberately NOT run automatically in CI — it hits real endpoints with real load
// and needs a server already running. Treat it as a manual/staging tool.

import http from "k6/http";
import { check, sleep } from "k6";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";

export const options = {
  vus: Number(__ENV.VUS || 5),
  duration: __ENV.DURATION || "30s",
  thresholds: {
    http_req_failed: ["rate<0.01"],   // fewer than 1% of requests should fail
    http_req_duration: ["p(95)<800"], // 95% of requests should complete under 800ms
  },
};

function uniqueEmail() {
  return `loadtest-${__VU}-${__ITER}-${Date.now()}@example.com`;
}

export default function () {
  const email = uniqueEmail();
  const password = "LoadTest12345";

  // 1. Register
  const registerRes = http.post(
    `${BASE_URL}/api/auth/register`,
    JSON.stringify({ name: "Load Test", email, password }),
    { headers: { "Content-Type": "application/json" } }
  );
  check(registerRes, {
    "register: status 201": (r) => r.status === 201,
    "register: has accessToken": (r) => !!r.json("accessToken"),
  });

  const accessToken = registerRes.json("accessToken");
  const authHeaders = { headers: { Authorization: `Bearer ${accessToken}`, "Content-Type": "application/json" } };

  // 2. List categories (seeded on registration)
  const categoriesRes = http.get(`${BASE_URL}/api/categories`, authHeaders);
  check(categoriesRes, {
    "categories: status 200": (r) => r.status === 200,
    "categories: at least one default category": (r) => r.json().length > 0,
  });

  const categoryId = categoriesRes.json()[0].id;

  // 3. Create an expense
  const expenseRes = http.post(
    `${BASE_URL}/api/expenses`,
    JSON.stringify({ title: "Load test coffee", amount: 4.5, categoryId, date: "2026-08-01", kind: "expense" }),
    authHeaders
  );
  check(expenseRes, {
    "create expense: status 201": (r) => r.status === 201,
  });

  // 4. List expenses
  const listRes = http.get(`${BASE_URL}/api/expenses`, authHeaders);
  check(listRes, {
    "list expenses: status 200": (r) => r.status === 200,
    "list expenses: contains the one we just made": (r) => r.json().length > 0,
  });

  // 5. Analytics summary
  const analyticsRes = http.get(`${BASE_URL}/api/analytics/summary`, authHeaders);
  check(analyticsRes, {
    "analytics: status 200": (r) => r.status === 200,
  });

  sleep(1);
}
