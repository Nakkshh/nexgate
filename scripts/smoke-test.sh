#!/bin/sh
# Day 1 smoke test — run after docker-compose up
# Usage: chmod +x scripts/smoke-test.sh && ./scripts/smoke-test.sh

set -e
GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

pass() { printf "${GREEN}[PASS]${NC} $1\n"; }
fail() { printf "${RED}[FAIL]${NC} $1\n"; exit 1; }

echo "===== NexGate Day 1 Smoke Tests ====="

# 1. Mock backend health
echo "\n1. Mock backend..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8081/health)
[ "$STATUS" = "200" ] && pass "Mock backend healthy (200)" || fail "Mock backend returned $STATUS"

# 2. Mock backend endpoints
echo "\n2. Mock backend endpoints..."
curl -sf http://localhost:8081/products > /dev/null && pass "GET /products" || fail "GET /products failed"
curl -sf http://localhost:8081/orders > /dev/null && pass "GET /orders" || fail "GET /orders failed"
curl -sf http://localhost:8081/users > /dev/null && pass "GET /users" || fail "GET /users failed"
curl -sf -X POST http://localhost:8081/orders \
  -H "Content-Type: application/json" \
  -d '{"items":["item1"],"total":99.99}' > /dev/null && pass "POST /orders" || fail "POST /orders failed"
curl -sf -X POST http://localhost:8081/payments \
  -H "Content-Type: application/json" \
  -d '{"amount":99.99,"currency":"INR","method":"card"}' > /dev/null && pass "POST /payments" || fail "POST /payments failed"

# 3. Gateway health
echo "\n3. Gateway..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
[ "$STATUS" = "200" ] && pass "Gateway actuator healthy (200)" || fail "Gateway returned $STATUS"

curl -sf http://localhost:8080/gateway/info > /dev/null && pass "GET /gateway/info" || fail "GET /gateway/info failed"

# 4. Prometheus metrics endpoint
echo "\n4. Prometheus metrics..."
curl -sf http://localhost:8080/actuator/prometheus | grep -q "jvm_memory" && pass "Prometheus metrics available" || fail "Prometheus metrics missing"

# 5. Redis
echo "\n5. Redis..."
PONG=$(redis-cli -h localhost -p 6379 PING 2>/dev/null || echo "FAILED")
[ "$PONG" = "PONG" ] && pass "Redis responding" || fail "Redis not responding (is redis-cli installed?)"

# 6. Kafka UI
echo "\n6. Kafka UI..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8090)
[ "$STATUS" = "200" ] && pass "Kafka UI accessible" || fail "Kafka UI returned $STATUS"

# 7. Grafana
echo "\n7. Grafana..."
STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:3001/api/health)
[ "$STATUS" = "200" ] && pass "Grafana healthy" || fail "Grafana returned $STATUS"

echo "\n===== All Day 1 checks passed! ====="
echo "Next: Day 2 — API key auth filter + Redis tenant cache + request proxying"
