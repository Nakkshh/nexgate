-- ============================================================
-- NexGate — Initial Schema Migration
-- Run against NeonDB: psql $DATABASE_URL -f db/migrations/001_init.sql
-- Also auto-applied to local postgres via docker-entrypoint-initdb.d
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ── Tenants ──────────────────────────────────────────────────────
-- Each tenant is a company/partner consuming our gateway
CREATE TABLE IF NOT EXISTS tenants (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    name        VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    plan        VARCHAR(20)  NOT NULL DEFAULT 'free'
                    CHECK (plan IN ('free', 'pro', 'enterprise')),
    rate_limit  INTEGER      NOT NULL DEFAULT 100,   -- requests per minute
    is_active   BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- ── API Keys ─────────────────────────────────────────────────────
-- Raw key is shown once on creation and NEVER stored.
-- key_hash (SHA-256) is what we store and match on.
-- key_prefix (first 8 chars) is shown in dashboard for identification.
CREATE TABLE IF NOT EXISTS api_keys (
    id          UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    key_hash    VARCHAR(64) NOT NULL UNIQUE,   -- SHA-256 hex
    key_prefix  VARCHAR(8)  NOT NULL,          -- e.g. "nxg_a3f8"
    name        VARCHAR(100),                  -- human label e.g. "Production key"
    is_active   BOOLEAN     NOT NULL DEFAULT true,
    last_used_at TIMESTAMPTZ,
    expires_at  TIMESTAMPTZ,                   -- null = never expires
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Request Logs ─────────────────────────────────────────────────
-- Written by Kafka consumer, not the gateway directly.
-- Gateway publishes events to Kafka; consumer persists here async.
CREATE TABLE IF NOT EXISTS request_logs (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    api_key_id      UUID        REFERENCES api_keys(id) ON DELETE SET NULL,
    method          VARCHAR(10) NOT NULL,
    endpoint        VARCHAR(500) NOT NULL,
    status_code     SMALLINT    NOT NULL,
    latency_ms      INTEGER     NOT NULL,
    request_size    INTEGER,
    response_size   INTEGER,
    ip_address      INET,
    user_agent      TEXT,
    error_message   TEXT,
    logged_at       TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ── Tenant Metrics ───────────────────────────────────────────────
-- Pre-aggregated by Kafka consumer — one row per tenant per minute window.
-- Dashboard reads from here, NOT from request_logs directly.
-- This is why dashboards are fast even with millions of log rows.
CREATE TABLE IF NOT EXISTS tenant_metrics (
    id              UUID        PRIMARY KEY DEFAULT uuid_generate_v4(),
    tenant_id       UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    window_start    TIMESTAMPTZ NOT NULL,
    window_end      TIMESTAMPTZ NOT NULL,
    req_count       INTEGER     NOT NULL DEFAULT 0,
    error_count     INTEGER     NOT NULL DEFAULT 0,
    avg_latency_ms  NUMERIC(8,2),
    p99_latency_ms  INTEGER,
    rate_limit_hits INTEGER     NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    -- One row per tenant per minute window
    UNIQUE (tenant_id, window_start)
);

-- ── Indexes ──────────────────────────────────────────────────────

-- Fast key lookup on every gateway request (hot path)
CREATE INDEX IF NOT EXISTS idx_api_keys_key_hash
    ON api_keys(key_hash);

CREATE INDEX IF NOT EXISTS idx_api_keys_tenant_id
    ON api_keys(tenant_id);

-- Per-tenant time-range queries for the dashboard
-- Composite index: filters by tenant first, then sorts by time
CREATE INDEX IF NOT EXISTS idx_request_logs_tenant_time
    ON request_logs(tenant_id, logged_at DESC);

CREATE INDEX IF NOT EXISTS idx_request_logs_logged_at
    ON request_logs(logged_at DESC);

-- Dashboard metric queries
CREATE INDEX IF NOT EXISTS idx_tenant_metrics_tenant_window
    ON tenant_metrics(tenant_id, window_start DESC);

-- ── Auto-update updated_at on tenants ────────────────────────────
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tenants_updated_at ON tenants;
CREATE TRIGGER tenants_updated_at
    BEFORE UPDATE ON tenants
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ── Seed data for local dev ───────────────────────────────────────
-- Three tenants on different plans to test rate limiting
-- API keys are shown here for dev convenience only.
-- In prod, raw keys are never stored — only their SHA-256 hashes.
--
-- Key format: nxg_<random>
-- Dev keys (raw → SHA-256 already pre-computed):
--   Acme Corp    → nxg_free_test_key_001  (plan: free,  100/min)
--   StartupX     → nxg_pro_test_key_0001  (plan: pro,  1000/min)
--   BigCorp Ltd  → nxg_ent_test_key_0001  (plan: enterprise, 5000/min)

INSERT INTO tenants (id, name, email, plan, rate_limit) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Acme Corp',   'acme@example.com',    'free',       100),
    ('22222222-2222-2222-2222-222222222222', 'StartupX',    'startup@example.com', 'pro',       1000),
    ('33333333-3333-3333-3333-333333333333', 'BigCorp Ltd', 'bigcorp@example.com', 'enterprise', 5000)
ON CONFLICT DO NOTHING;

-- SHA-256 of "nxg_free_test_key_001":
--   echo -n "nxg_free_test_key_001" | sha256sum
INSERT INTO api_keys (tenant_id, key_hash, key_prefix, name) VALUES
    ('11111111-1111-1111-1111-111111111111',
     '9d83d0753204ba13e0fffa9ac7a9ee160bc7b4239a56d56b7d8e5ac3efdd3a81',
     'nxg_free', 'Acme Dev Key'),
    ('22222222-2222-2222-2222-222222222222',
     'e17d4e2c70192b8c1c9dbe739fcd2ba55ed24959cc4b19c01cf2a436c0778736',
     'nxg_pro_', 'StartupX Prod Key'),
    ('33333333-3333-3333-3333-333333333333',
     'c755f6f0842d3e8374dfa1f710fa5d1c704e721c5d6dc220fb1bd6d6d39b0839',
     'nxg_ent_', 'BigCorp Prod Key')
ON CONFLICT DO NOTHING;
