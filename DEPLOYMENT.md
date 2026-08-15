# Deploying iHub for free

A complete, self-contained runbook. Follow it top to bottom and you'll have the
platform running on a public HTTPS domain at no cost.

**Target:** backend + MySQL + Elasticsearch on one Oracle Cloud Always Free VM,
frontend on Vercel.

---

## Contents

- [Why this shape](#why-this-shape)
- [What you need before starting](#what-you-need-before-starting)
- [Part 1 — Create the free VM](#part-1--create-the-free-vm)
- [Part 2 — Open the firewall](#part-2--open-the-firewall)
- [Part 3 — Install Docker](#part-3--install-docker)
- [Part 4 — Prepare the host for Elasticsearch](#part-4--prepare-the-host-for-elasticsearch)
- [Part 5 — Point a domain at the VM](#part-5--point-a-domain-at-the-vm)
- [Part 6 — Deploy the backend](#part-6--deploy-the-backend)
- [Part 7 — Create your administrator](#part-7--create-your-administrator)
- [Part 8 — Deploy the frontend to Vercel](#part-8--deploy-the-frontend-to-vercel)
- [Part 9 — Verify the deployment](#part-9--verify-the-deployment)
- [Day-2 operations](#day-2-operations)
- [Troubleshooting](#troubleshooting)
- [Alternative: managed free services](#alternative-managed-free-services)

---

## Why this shape

Most "free tier" hosting **sleeps your container when idle**. That is fine for a
CRUD app and wrong for this one:

- `AuctionScheduler` runs every minute to open auctions, send "ending soon"
  notifications, and close auctions with winner selection.
- `EmailOutboxScheduler` drains queued mail every two minutes.
- `/ws` holds long-lived STOMP connections for live bidding.

If the process sleeps, **auctions never start or close on their own** and every
one has to be driven by hand from the admin console. So the requirement is an
*always-on* host, which rules out scale-to-zero platforms as the primary target.

Oracle Cloud's Always Free tier gives roughly 4 ARM cores / 24 GB RAM / 200 GB
storage with **no time limit** — enough to run all three services comfortably,
with real MySQL (your bid-locking correctness depends on genuine
`SELECT … FOR UPDATE` semantics) and a matching Elasticsearch 8.x.

> Cloud providers revise free tiers often. Confirm current terms before relying on
> any of this long-term.

---

## What you need before starting

| | |
|---|---|
| Oracle Cloud account | Free signup; a credit card is required for identity verification but is not charged on Always Free resources |
| A domain name | Needed for HTTPS. A cheap `.com`, or a free subdomain from a provider like DuckDNS |
| GitHub repo | This project pushed somewhere the VM can clone from |
| Vercel account | Free; sign in with GitHub |
| SSH key pair | Generated during VM creation |

---

## Part 1 — Create the free VM

1. Sign in to Oracle Cloud → **Compute → Instances → Create instance**.
2. **Image:** Ubuntu 22.04 (or 24.04).
3. **Shape:** click *Change shape* → **Ampere** → `VM.Standard.A1.Flex`.
   Set **4 OCPUs** and **24 GB memory** — this is the Always Free allowance.
4. **Networking:** keep the default VCN, and ensure *Assign a public IPv4 address*
   is checked.
5. **SSH keys:** choose *Generate a key pair* and **download the private key**.
   You cannot retrieve it later.
6. Create, and note the **public IP address**.

> **If you get "Out of host capacity":** ARM capacity in popular regions is often
> exhausted. Either retry periodically, or pick a less busy region — the region is
> fixed at account creation, so choose carefully if you're signing up now. An
> AMD `VM.Standard.E2.1.Micro` instance is also Always Free but has only 1 GB RAM,
> which is not enough for MySQL + Elasticsearch + a JVM together.

Connect:

```bash
chmod 600 ~/Downloads/ssh-key-*.key
ssh -i ~/Downloads/ssh-key-*.key ubuntu@<PUBLIC_IP>
```

---

## Part 2 — Open the firewall

**This is the single most common reason a working deployment appears dead.**
Oracle blocks traffic in *two* independent places, and both must be opened.

### 2a. Security list (Oracle's cloud firewall)

Console → **Networking → Virtual Cloud Networks →** your VCN **→ Security Lists →
Default Security List → Add Ingress Rules**:

| Source CIDR | IP Protocol | Destination Port | Purpose |
|---|---|---|---|
| `0.0.0.0/0` | TCP | 80 | HTTP (Let's Encrypt validation) |
| `0.0.0.0/0` | TCP | 443 | HTTPS |

Do **not** open 3306 or 9200. The production compose file keeps MySQL and
Elasticsearch off the host network entirely.

### 2b. The VM's own iptables

Oracle's Ubuntu images ship with restrictive `iptables` rules that survive
everything else you do:

```bash
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 80 -j ACCEPT
sudo iptables -I INPUT 6 -m state --state NEW -p tcp --dport 443 -j ACCEPT
sudo netfilter-persistent save
```

---

## Part 3 — Install Docker

```bash
sudo apt-get update
sudo apt-get install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
https://download.docker.com/linux/ubuntu $(. /etc/os-release && echo $VERSION_CODENAME) stable" \
  | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo usermod -aG docker $USER
newgrp docker            # or log out and back in
docker --version
```

---

## Part 4 — Prepare the host for Elasticsearch

Elasticsearch refuses to start without a raised `mmap` limit. Set it permanently:

```bash
echo 'vm.max_map_count=262144' | sudo tee /etc/sysctl.d/99-elasticsearch.conf
sudo sysctl --system
sysctl vm.max_map_count      # expect 262144
```

Set the system timezone. Auction *windows* are evaluated in SQL so bidding stays
correct regardless, but timestamps shown to users come from the JVM — keep the
host, MySQL and the JVM on one zone:

```bash
sudo timedatectl set-timezone Asia/Kolkata     # or your zone
```

---

## Part 5 — Point a domain at the VM

Create a DNS **A record** for the hostname you'll use (e.g. `api.example.com`)
pointing at the VM's public IP. Verify before continuing — Let's Encrypt will fail
if DNS hasn't propagated:

```bash
dig +short api.example.com     # must print your VM's public IP
```

---

## Part 6 — Deploy the backend

```bash
git clone <YOUR_REPO_URL> ihub
cd ihub/server
cp .env.example .env
nano .env
```

Fill in `.env`. Generate real secrets — do not invent them by hand:

```bash
openssl rand -base64 48     # run twice: once for JWT_SECRET, once for passwords
```

```ini
# --- required ---
MYSQL_ROOT_PASSWORD=<generated>
ELASTIC_PASSWORD=<generated>
JWT_SECRET=<generated, at least 32 characters>
DOMAIN=api.example.com
FRONTEND_URL=https://<your-project>.vercel.app

# --- first boot only; blank these afterwards ---
IHUB_ADMIN_EMAIL=you@example.com
IHUB_ADMIN_PASSWORD=<at least 12 characters>
IHUB_ADMIN_NAME=Your Name

# --- optional ---
SPRING_PROFILES_ACTIVE=prod
EMAIL_ENABLED=false
TZ=Asia/Kolkata
```

`FRONTEND_URL` must be the **exact** public origin of your frontend — no trailing
slash. It drives both CORS and the WebSocket allow-list; a mismatch means live
bidding silently fails to connect while everything else looks fine. You'll have
the real Vercel URL after Part 8, so put a placeholder now and correct it then.

Start the stack:

```bash
docker compose -f docker-compose.prod.yml up -d --build
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f backend
```

The first build takes several minutes (Maven downloads dependencies). Watch for:

```
Successfully applied 4 migrations to schema `ihub`
Seeded the initial administrator (id=1, email=…)
Started IhubApplication in N seconds
```

Flyway creates the entire schema on an empty database — there is no SQL to run by
hand.

---

## Part 7 — Create your administrator

Self-service registration deliberately refuses the `ADMIN` role, so nobody can sign
up as an administrator. The first one is seeded from `IHUB_ADMIN_EMAIL` /
`IHUB_ADMIN_PASSWORD` on a database that has none — that's what the log line above
confirms.

The seed is **inert once an administrator exists**: it can never reset or re-grant
an existing account, even if the variables are left set. Still, clear them so the
password isn't sitting in a file on disk:

```bash
nano .env          # blank IHUB_ADMIN_EMAIL and IHUB_ADMIN_PASSWORD
docker compose -f docker-compose.prod.yml up -d backend
```

Then sign in and change the password from **Profile → Password**.

---

## Part 8 — Deploy the frontend to Vercel

1. Vercel → **Add New → Project** → import your repository.
2. **Root Directory:** `client` ← easy to miss, and nothing works without it.
3. Framework preset: Next.js (auto-detected). Leave build settings alone.
4. **Environment Variables:**

   | Name | Value |
   |---|---|
   | `NEXT_PUBLIC_API_URL` | `https://api.example.com` |
   | `NEXT_PUBLIC_WS_URL` | `https://api.example.com/ws` |

5. Deploy, then copy the assigned URL (e.g. `https://ihub-xyz.vercel.app`).
6. **Back on the VM**, set `FRONTEND_URL` in `.env` to that exact URL and restart:

   ```bash
   nano .env
   docker compose -f docker-compose.prod.yml up -d backend
   ```

REST traffic goes to `/api/*` on Vercel and is rewritten server-side to your
backend, so the browser stays same-origin and never issues a CORS preflight. The
**WebSocket connects directly** to `NEXT_PUBLIC_WS_URL` — which is exactly why
`FRONTEND_URL` on the backend has to match.

---

## Part 9 — Verify the deployment

From your laptop:

```bash
# 1. TLS and health
curl https://api.example.com/actuator/health
# {"status":"UP","groups":["liveness","readiness"]}

# 2. Public read path
curl https://api.example.com/api/categories

# 3. Search (Elasticsearch reachable)
curl "https://api.example.com/api/search?q=test"

# 4. Admin sign-in
curl -X POST https://api.example.com/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"you@example.com","password":"<your password>"}'
```

Then in the browser:

- Open the Vercel URL, sign in as your administrator → redirected to `/admin/dashboard`.
- Register a creator and an investor account.
- Creator: submit an idea → publish it.
- Admin: **Auctions → Create auction**, pick the idea, set a start time a minute
  or two out.
- Wait for the scheduler to open it (up to 60s), then bid as the investor.
- Open the auction in two browsers: a bid in one should appear in the other within
  a second. **If it doesn't, `FRONTEND_URL` doesn't match your Vercel origin.**

---

## Day-2 operations

```bash
cd ~/ihub/server

# logs
docker compose -f docker-compose.prod.yml logs -f backend
docker compose -f docker-compose.prod.yml logs --tail=100 caddy

# deploy a new version
git pull
docker compose -f docker-compose.prod.yml up -d --build backend

# restart / stop
docker compose -f docker-compose.prod.yml restart backend
docker compose -f docker-compose.prod.yml down          # keeps data volumes

# database backup  (do this before any upgrade)
docker exec ihub-mysql mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" ihub \
  > ~/ihub-backup-$(date +%F).sql

# restore
cat ~/ihub-backup-2026-08-15.sql \
  | docker exec -i ihub-mysql mysql -uroot -p"$MYSQL_ROOT_PASSWORD" ihub
```

Schedule the backup so it actually happens:

```bash
crontab -e
# 0 3 * * * cd ~/ihub/server && docker exec ihub-mysql mysqldump -uroot -p"$(grep MYSQL_ROOT_PASSWORD .env | cut -d= -f2)" ihub > ~/backups/ihub-$(date +\%F).sql
```

**Search drift.** Index writes are best-effort by design — an Elasticsearch outage
never rolls back a database write. After any outage, rebuild from the admin console:
**Settings → Rebuild index**. MySQL is always the system of record.

**Adding a database change.** Create `server/src/main/resources/db/migration/V5__description.sql`.
Never edit an applied migration — Flyway validates checksums and will refuse to start.

---

## Troubleshooting

| Symptom | Cause and fix |
|---|---|
| `curl` to the domain hangs | Firewall. Both Part 2a *and* 2b are required; check `sudo iptables -L INPUT -n --line-numbers` |
| Caddy logs a certificate failure | DNS not propagated, or port 80 unreachable. `dig +short <domain>` must return the VM IP |
| `elasticsearch` container exits immediately | `vm.max_map_count` too low — redo Part 4, then `docker compose -f docker-compose.prod.yml up -d` |
| Backend restarts repeatedly | `docker compose -f docker-compose.prod.yml logs backend`. Usually a missing required variable — `DB_URL`, `DB_PASSWORD` and `JWT_SECRET` have no defaults on purpose |
| `JWT_SECRET must be at least 32 characters` | Exactly what it says; regenerate with `openssl rand -base64 48` |
| Login works, live bidding doesn't | `FRONTEND_URL` ≠ your Vercel origin. Must match exactly, no trailing slash |
| Search returns 503 | Elasticsearch is down. The rest of the platform keeps working — that's intended. Check `docker compose -f docker-compose.prod.yml logs elasticsearch` |
| Auctions never start or close | The backend isn't running continuously, or both auto-start/auto-end were switched off in **Admin → Settings** |
| No administrator exists | Set `IHUB_ADMIN_EMAIL` / `IHUB_ADMIN_PASSWORD` in `.env` and restart the backend once |
| Out of disk | `docker system prune -a` removes old images and build cache |

---

## Alternative: managed free services

Less server administration, more moving parts — and the scheduler caveat from the
top of this document applies.

| Layer | Service | Watch out for |
|---|---|---|
| Frontend | Vercel | Same as Part 8 |
| Backend | Render (free web service) | Sleeps after ~15 min idle. Ping `/actuator/health` every ~10 min from a free cron service, or auctions stall |
| MySQL | Aiven free tier | ~1 GB. Must be **real MySQL** |
| Search | Bonsai free sandbox | Small document limits, and **confirm it runs Elasticsearch 8.x** — the client here is 8.10.4 and will not talk to a 7.x cluster |

Two compatibility traps worth stating plainly:

1. **Avoid MySQL-*compatible* engines** (TiDB, Vitess-based offerings, etc.).
   `V3__auction_constraints_and_indexes.sql` adds a **STORED generated column** via
   `ALTER TABLE`, which several of them restrict, and bid correctness depends on
   precise `FOR UPDATE` + `READ COMMITTED` behaviour. Use genuine MySQL 8.
2. **Check the Elasticsearch major version** before committing to a provider.

Set the same environment variables from Part 6 in each provider's dashboard. The
application reads everything from the environment — no code changes are needed for
any hosting target.
