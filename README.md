# iHub

An idea-to-investment platform. Creators publish business ideas, administrators run
timed auctions against them, and investors compete with live bids.

- **Backend** — Java 17 · Spring Boot 3.2 · MySQL (JDBC / `NamedParameterJdbcTemplate`) · Elasticsearch · STOMP over WebSocket · JWT
- **Frontend** — Next.js 16 (App Router) · TypeScript · Tailwind CSS v4 · TanStack Query · Zustand

---

## Contents

- [Architecture](#architecture)
- [Running locally](#running-locally)
- [Configuration](#configuration)
- [Database migrations](#database-migrations)
- [Domain model](#domain-model)
- [API reference](#api-reference)
- [Real-time channels](#real-time-channels)
- [Security model](#security-model)
- [Testing](#testing)
- [Deployment](#deployment)

---

## Architecture

```
                       ┌──────────────────────────┐
 Browser ──────────────│  Next.js (Vercel)        │
   │                   │  · pages + route proxy   │
   │  /api/* rewrite   │  · /api/* → backend      │
   │                   └────────────┬─────────────┘
   │                                │
   │  STOMP /ws (direct)            │ REST
   │                                ▼
   │                   ┌──────────────────────────┐
   └───────────────────│  Spring Boot             │
                       │  Controller → Service    │
                       │       → DAO → MySQL      │
                       │            ↘ Elasticsearch│
                       └──────────────────────────┘
```

REST traffic is proxied through Next.js rewrites so the browser stays same-origin.
The WebSocket connects to the backend directly, which is why the backend's allowed
origins must include the frontend URL.

**Layering.** Controllers handle HTTP only. Services own business rules and
transactions. DAOs own SQL via `NamedParameterJdbcTemplate` — every value is a bound
parameter; only fixed SQL fragments are ever concatenated.

**MySQL is the system of record.** Elasticsearch is a derived read model for
discovery. Index writes are best-effort and never abort a database transaction, and
the index is created lazily rather than during repository bootstrap — so an
unreachable cluster degrades search to a 503 without failing application startup or
taking the platform down. (With Spring Data's default `createIndex`, a cold search
cluster aborts startup and crash-loops the service.) Drift is repaired from
**Admin → Settings → Rebuild index**.

---

## Running locally

**Prerequisites:** Java 17+, Maven 3.9+, Node 20.9+, MySQL 8, Elasticsearch 8+.

### 1. Database

```sql
CREATE DATABASE ihub CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ihub'@'%' IDENTIFIED BY 'your-password';
GRANT ALL PRIVILEGES ON ihub.* TO 'ihub'@'%';
```

Tables are created by Flyway on first boot — no manual SQL needed.

### 2. Backend

```bash
cd server
cp .env.example .env          # then fill in DB_URL, DB_USERNAME, DB_PASSWORD, JWT_SECRET
mvn spring-boot:run
```

`.env` is not read automatically — export the variables, use your IDE's env-file
support, or run through `docker compose`. The app refuses to start without
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD` and `JWT_SECRET`; that is deliberate, so a
deployment can never silently fall back to development credentials.

Health check: <http://localhost:8081/actuator/health>

### 3. Frontend

```bash
cd client
npm install
cp .env.local.example .env.local
npm run dev
```

Open <http://localhost:3000>.

### 4. Everything in containers

```bash
cd server
cp .env.example .env          # MYSQL_ROOT_PASSWORD, ELASTIC_PASSWORD and JWT_SECRET are required
docker compose up --build
```

Compose provisions MySQL and Elasticsearch, waits for both to report healthy, then
starts the backend.

---

## Configuration

Every setting is an environment variable. Nothing sensitive is committed.

### Backend

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `DB_URL` | ✅ | — | JDBC URL |
| `DB_USERNAME` | ✅ | — | Database user |
| `DB_PASSWORD` | ✅ | — | Database password |
| `JWT_SECRET` | ✅ | — | HMAC signing key, **minimum 32 characters** (validated at startup) |
| `SPRING_PROFILES_ACTIVE` | | `dev` | `dev` logs SQL; `prod` does not |
| `PORT` | | `8081` | HTTP port |
| `FRONTEND_URL` | | `http://localhost:3000` | Comma-separated CORS/WebSocket origins |
| `ES_URL` | | `http://localhost:9200` | Elasticsearch endpoint |
| `ES_USERNAME` / `ES_PASSWORD` | | empty | Elasticsearch credentials |
| `JWT_EXPIRATION` | | `3600000` | Access token lifetime (ms) |
| `JWT_REFRESH_EXPIRATION` | | `604800000` | Refresh token lifetime (ms) |
| `EMAIL_ENABLED` | | `false` | `false` logs emails instead of sending |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | | empty | SMTP |
| `DB_POOL_MAX` / `DB_POOL_MIN` | | `10` / `2` | HikariCP sizing |

> **Time zones.** Run the JVM and MySQL in the same zone. Auction windows are
> evaluated by the database so bidding and the scheduler can never disagree, but
> timestamps displayed to users still come from the JVM.

### Frontend

| Variable | Default | Purpose |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | `http://localhost:8081` | Rewrite target for `/api/*` |
| `NEXT_PUBLIC_WS_URL` | `http://localhost:8081/ws` | STOMP endpoint (connected to directly) |

### Runtime settings

Some behaviour is configured at runtime in **Admin → Settings**, stored in
`platform_settings`: default bid increment, auto-start/auto-end of auctions, email
delivery, and per-type notification toggles. These take effect immediately.

---

## Database migrations

Flyway owns the schema. Migrations live in
`server/src/main/resources/db/migration` and run automatically at startup.

| Version | Contents |
|---|---|
| `V1__baseline_schema.sql` | All 14 core tables |
| `V2__platform_settings.sql` | Runtime settings store |
| `V3__auction_constraints_and_indexes.sql` | One-live-auction-per-idea constraint, scheduler indexes |
| `V4__seed_categories.sql` | Reference categories |

`spring.sql.init` is disabled — Flyway is the single source of truth. Databases that
predate Flyway are baselined at V1 automatically (`baseline-on-migrate`), so an
existing install upgrades in place without re-applying the baseline.

To add a change, create `V5__description.sql`. Never edit an applied migration:
Flyway validates checksums and will refuse to start.

---

## Domain model

### Auction lifecycle

```
SCHEDULED ──(start time reached, or admin/creator starts)──▶ ACTIVE
                                                              │
                                    (end time reached, or manually ended)
                                                              ▼
                                                           CLOSED
     └──────────────── CANCELLED ◀───────────────┘
              (from SCHEDULED or ACTIVE only)
```

`AuctionLifecycleService` owns every transition. The scheduler, the creator-facing
API and the admin console all route through it, so winner selection, event history,
search synchronisation and notifications happen exactly once per transition
regardless of what triggered it.

`UPCOMING` is accepted as an inbound alias for `SCHEDULED` but is never persisted.

### Bidding rules

A bid is accepted only when all of the following hold:

1. The bidder is an **investor**.
2. The auction status is `ACTIVE` **and** the current time is inside its window.
3. The bidder is not the idea's creator.
4. The amount is at least the base price (first bid) or `current highest + increment`.

Concurrency is handled with a pessimistic row lock:

```sql
SELECT ... FROM auctions a ... WHERE a.id = :id FOR UPDATE OF a
```

Simultaneous bids on one auction are serialised for the duration of the transaction,
so two investors cannot both clear the same "current highest". Closing an auction
takes the same lock, which is why no bid can land between winner selection and
closure.

### Winner selection

Highest `bid_amount` wins; ties break on the earliest `created_at`. The result is
written to `auction_winners`, which is unique per auction.

---

## API reference

Base path `/api`. All errors share one envelope:

```json
{
  "timestamp": "2026-08-15T13:47:33",
  "status": 409,
  "error": "Conflict",
  "message": "This auction has already closed",
  "path": "/api/bids",
  "fieldErrors": null
}
```

| Status | Meaning |
|---|---|
| `400` | Malformed body or failed field validation (see `fieldErrors`) |
| `401` | Missing, invalid or expired credentials |
| `403` | Authenticated but not permitted |
| `404` | No such resource, or not visible to you |
| `409` | Conflicts with current state (auction already closed, duplicate email) |
| `422` | Valid request, violates a business rule (bid below minimum) |
| `503` | Search unavailable — the rest of the platform still works |

### Auth
| Method | Path | Access |
|---|---|---|
| `POST` | `/auth/login` | Public |
| `POST` | `/auth/refresh` | Public — rotates; the presented token is single-use |
| `POST` | `/auth/logout` | Public |

### Users
| Method | Path | Access |
|---|---|---|
| `POST` | `/users` | Public — `CREATOR`/`INVESTOR` only; `ADMIN` requires an admin |
| `GET` | `/users/me` | Authenticated |
| `PUT` | `/users/me` | Authenticated — display name |
| `PUT` | `/users/me/password` | Authenticated |
| `GET` | `/users/{id}` | Authenticated — email withheld unless self or admin |
| `GET` | `/users` | **Admin** |

### Ideas
| Method | Path | Access |
|---|---|---|
| `GET` | `/ideas` | Public — paged; totals in `X-Total-Count` |
| `GET` | `/ideas/{id}` | Public for published; owner/admin otherwise |
| `POST` | `/ideas` | Creator |
| `PUT` | `/ideas/{id}` | Creator (owner, drafts only) |
| `POST` | `/ideas/{id}/publish` | Creator (owner) |
| `DELETE` | `/ideas/{id}` | Creator (owner) — archives |

### Auctions
| Method | Path | Access |
|---|---|---|
| `GET` | `/auctions` | Public — paged |
| `GET` | `/auctions/{id}` | Public |
| `GET` | `/auctions/{id}/winner` | Public |
| `GET` | `/auctions/{id}/history` | Public |
| `POST` | `/auctions` | Creator (own idea) |
| `POST` | `/auctions/{id}/start` | Creator (owner) or Admin |
| `POST` | `/auctions/{id}/close` | Creator (owner) or Admin |

### Bids
| Method | Path | Access |
|---|---|---|
| `POST` | `/bids` | Investor |
| `GET` | `/bids/auction/{id}/history` | Public — paged |
| `GET` | `/bids/auction/{id}/highest` | Public |
| `GET` | `/bids/auction/{id}/leaderboard` | Public |
| `GET` | `/bids/auction/{id}/bidders/count` | Public |
| `GET` | `/bids/my` | Investor |

### Search
| Method | Path | Access |
|---|---|---|
| `GET` | `/search` | Public — `q`, `category`, `tags`, `minBudget`, `maxBudget`, `auctionStatus`, `sort`, `page`, `size` |
| `GET` | `/search/category` · `/search/live` · `/search/facets/categories` | Public |

### Notifications
`GET /notifications`, `GET /notifications/unread-count`,
`PATCH /notifications/{id}/read`, `PATCH /notifications/read-all` — authenticated,
always scoped to the caller.

### Admin (all require `ADMIN`)
`/admin/dashboard` · `/admin/metrics` · `/admin/dashboard/charts` · `/admin/users` ·
`/admin/ideas` · `/admin/auctions` (+ `/{id}`, `/{id}/start|end|cancel`) ·
`/admin/winners` · `/admin/bids` · `/admin/audit-logs` · `/admin/settings` ·
`/admin/search/health` · `/admin/search/reindex`

---

## Real-time channels

STOMP over SockJS at `/ws`. The access token is sent on the `CONNECT` frame —
SockJS cannot attach headers to its transport requests.

| Topic | Access |
|---|---|
| `/topic/auction/{id}/bids` | Public |
| `/topic/auction/{id}/leaderboard` | Public |
| `/topic/user/{id}/notifications` | **That user only** |
| `/topic/user/{id}/notifications/count` | **That user only** |

Auction topics are public because the same data is served by public REST endpoints.
Per-user topics are authorised per subscription against the connected identity.

---

## Security model

- **Passwords** — BCrypt.
- **Access tokens** — HS256 JWT, 1 hour, carrying subject and role. Tokens typed as
  anything other than `access` are rejected.
- **Refresh tokens** — opaque random strings stored server-side, rotated on every
  use, revoked on logout and on account suspension. Expired rows are purged nightly.
- **Authorization** — URL rules in `SecurityConfig` for coarse role gating; ownership
  checks in services for anything addressed by ID. A creator cannot reach another
  creator's idea by changing the path variable.
- **Registration** — the requested role is validated server-side; `ADMIN` cannot be
  self-assigned.
- **PII** — listing all users is admin-only; another user's email is never returned.
- **Errors** — SQL, driver messages and stack traces are logged, never returned.
- **Transport** — HSTS and `X-Frame-Options: DENY`; CORS restricted to configured
  origins with no wildcard.

---

## Testing

```bash
# Backend — unit + context load, no MySQL or Elasticsearch needed
cd server && mvn test

# Frontend — type check and production build
cd client && npm run build && npm run lint
```

The backend suite covers bidding rules and the auction window, the lifecycle state
machine and its concurrency guards, registration/PII rules, JWT handling, and the
exception-to-status mapping. It runs on in-memory H2 with the schedulers disabled.

---

## Deployment

📘 **[DEPLOYMENT.md](DEPLOYMENT.md) is the step-by-step runbook** — a free
always-on host (Oracle Cloud Always Free) running the whole backend stack, with
the frontend on Vercel. It covers firewall setup, TLS, the first-administrator
seed, verification and day-2 operations. The notes below are the summary.

> **Why not a scale-to-zero host?** The auction scheduler must run continuously —
> if the process sleeps, auctions never open or close on their own.

### First administrator

Registration refuses to self-assign `ADMIN`, so a fresh database has none. Set
`IHUB_ADMIN_EMAIL` and `IHUB_ADMIN_PASSWORD` (min 12 chars) for one boot; the seed
is ignored once an administrator exists and can never reset an existing account.
Blank the variables afterwards.

### Frontend → Vercel

Set `NEXT_PUBLIC_API_URL` and `NEXT_PUBLIC_WS_URL` to your deployed backend. The
`/api/*` rewrite means the browser never issues a cross-origin API request.

### Backend → any container host

```bash
cd server
docker build -t ihub-backend .
docker run -p 8081:8081 --env-file .env ihub-backend
```

Or the full single-VM stack (backend + MySQL + Elasticsearch + TLS):

```bash
cd server && cp .env.example .env   # fill it in, then
docker compose -f docker-compose.prod.yml up -d --build
```

`docker-compose.prod.yml` is standalone rather than an overlay on
`docker-compose.yml`: Compose merges port lists rather than replacing them, so an
overlay could not withdraw the dev file's published MySQL and Elasticsearch ports —
which must not be internet-facing.

The image is multi-stage (build inside, JRE only at runtime), runs as a non-root
user, respects container memory limits, and exposes a healthcheck.

**Production checklist**

- [ ] `SPRING_PROFILES_ACTIVE=prod` — keeps SQL and bound parameters out of logs
- [ ] `JWT_SECRET` is 32+ random characters and unique to the environment
- [ ] `FRONTEND_URL` lists exactly the deployed frontend origin(s)
- [ ] Database user has no more than the privileges it needs
- [ ] TLS terminates in front of the app (`forward-headers-strategy` is already set)
- [ ] Probes point at `/actuator/health/liveness` and `/actuator/health/readiness`

> Elasticsearch and mail are intentionally excluded from the health aggregate.
> Neither should cause an orchestrator to restart an otherwise healthy instance —
> search degrades, and email queues in `email_outbox` until SMTP returns.

⚠️ **Credentials in git history.** Early commits contained a hardcoded JWT secret and
development database passwords. They were parameterised later, but remain recoverable
from history. Treat them as compromised: never reuse those values.
