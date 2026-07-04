# NexGate — Multi-Tenant API Gateway with Real-Time Traffic Analytics

A production-grade API gateway handling auth, per-tenant rate limiting, async request logging, and live traffic analytics.

> **Load tested:** 980+ req/min across 3 concurrent tenants · 62ms median latency · 0% error rate (k6, 90 VUs)

## Architecture

```
Client → [Gateway :8080] ──────────────────────────→ [Mock Backend :8081]
              │  - SHA-256 API key auth
              │  - Redis sliding window rate limit
              │  - Request proxy (WebClient)
              │  - Micrometer → Prometheus metrics
              │
              ↓ Kafka (async, fire-and-forget)
         [Consumer :8082]
              │  - Aggregates per-tenant/per-min windows
              │  - Writes → PostgreSQL tenant_metrics
              │  - DLT with 3-retry backoff
              │
              ↓ STOMP WebSocket push
         [Next.js Dashboard :3000]
              - Live Recharts area charts
              - REST history endpoint

[Prometheus :9090] ← scrapes Gateway /actuator/prometheus
[Grafana :3001]    ← visualises Prometheus metrics
```

## Services

| Service | Port | Description |
|---|---|---|
| gateway | 8080 | Core gateway — auth, rate limit, proxy |
| consumer | 8082 | Kafka consumer + WebSocket server |
| mock-backend | 8081 | Dummy API for testing |
| frontend | 3000 | Next.js dashboard |
| kafka | 9092 | Message broker |
| redis | 6379 | Rate limiting + tenant cache |
| postgres | 5432 | Local dev only (prod uses NeonDB) |
| prometheus | 9090 | Metrics scraper |
| grafana | 3001 | Metrics visualisation (admin/admin) |

## Quick Start

```bash
# 1. Clone the repo
git clone https://github.com/yourusername/nexgate.git
cd nexgate

# 2. Start all services
docker compose up -d

# 3. Wait ~2 minutes for gateway to boot, then verify
curl http://localhost:8080/actuator/health
curl -H "X-API-Key: nxg_free_test_key_001" http://localhost:8080/api/products

# 4. Start the frontend
cd frontend && npm install && npm run dev
```

Open `http://localhost:3000` — dashboard
Open `http://localhost:3001` — Grafana (admin/admin)
Open `http://localhost:8090` — Kafka UI

## Seed API Keys

| Tenant | Plan | Rate Limit | API Key |
|---|---|---|---|
| Acme Corp | Free | 100/min | `nxg_free_test_key_001` |
| StartupX | Pro | 1000/min | `nxg_pro_test_key_0001` |
| BigCorp Ltd | Enterprise | 5000/min | `nxg_ent_test_key_0001` |

## Test Rate Limiting

```bash
# Hit the free tier limit (100/min) — requests 1-100 → 200, 101+ → 429
for /L %i in (1,1,105) do curl -s -o nul -w "%i: %{http_code}\n" -H "X-API-Key: nxg_free_test_key_001" http://localhost:8080/api/products
```

## Run Load Test

```bash
k6 run load-tests/load-test.js
```

### Results (90 VUs, 3 minutes sustained)

| Metric | Value |
|---|---|
| Throughput | 980+ req/min |
| Median latency | 62ms |
| p90 latency | 366ms |
| p99 latency | ~2.1s |
| Error rate | 0% |
| Total requests | 3,285 |

## Tech Stack

| Layer | Technology |
|---|---|
| Gateway | Java 17, Spring Boot 3, Spring Security |
| Rate Limiting | Redis 7 + atomic Lua sliding window |
| Auth | SHA-256 API key hashing, Redis tenant cache |
| Async Logging | Apache Kafka 7.6, Dead Letter Topic |
| Database | PostgreSQL 16 (NeonDB in prod) |
| Observability | Prometheus, Grafana, Micrometer |
| Frontend | Next.js 15, TypeScript, Recharts, STOMP.js |
| Infrastructure | Docker, Docker Compose |
| Load Testing | k6 |
| CI/CD | GitHub Actions, Render, Vercel |

## API Reference

### Gateway (port 8080)
All proxied requests require `X-API-Key` header.
```
GET  /api/products
GET  /api/orders
GET  /api/users
GET  /api/payments
POST /api/products
POST /api/orders
```

### Consumer (port 8082)
```
GET /metrics/all
GET /metrics/history/{tenantId}?minutes=60
WS  /ws  (STOMP → subscribe to /topic/metrics/{tenantId})
```

### Admin (port 8080)
```
POST /admin/tenants
POST /admin/api-keys
```

## Key Design Decisions

**Redis sliding window rate limiter** — Atomic Lua script ensures no race conditions under concurrent load. Fails open on Redis errors to avoid blocking legitimate traffic.

**Kafka fire-and-forget logging** — Gateway publishes events asynchronously after response is sent, adding zero latency to the request path.

**Dead Letter Topic** — Failed consumer events retry 3 times with 2s backoff before landing in `request-logs-DLT` for manual inspection.

**Tenant cache** — Redis caches tenant config for 5 minutes to avoid a DB lookup on every request. Fails open on Redis errors.

## Project Structure

```
nexgate/
├── gateway/           # Spring Boot API Gateway
├── consumer/          # Kafka consumer + WebSocket server
├── mock-backend/      # Mock API for testing
├── frontend/          # Next.js dashboard
├── db/migrations/     # PostgreSQL schema
├── infra/             # Prometheus + Grafana config
├── load-tests/        # k6 load test scripts
└── docker-compose.yml
```