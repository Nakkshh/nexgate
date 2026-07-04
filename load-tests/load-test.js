import http from "k6/http";
import { check, sleep } from "k6";
import { Rate, Trend } from "k6/metrics";

const errorRate    = new Rate("error_rate");
const latencyTrend = new Trend("request_latency", true);

export const options = {
  scenarios: {
    free_tenant: {
      executor:        "constant-arrival-rate",
      rate:            80,
      timeUnit:        "1m",
      duration:        "3m",
      preAllocatedVUs: 5,
      maxVUs:          15,
      exec:            "freeTenant",
      // Delay start by 20s to let warmup finish
      startTime:       "20s",
    },
    pro_tenant: {
      executor:        "constant-arrival-rate",
      rate:            300,
      timeUnit:        "1m",
      duration:        "3m",
      preAllocatedVUs: 15,
      maxVUs:          40,
      exec:            "proTenant",
      startTime:       "20s",
    },
    enterprise_tenant: {
      executor:        "constant-arrival-rate",
      rate:            500,
      timeUnit:        "1m",
      duration:        "3m",
      preAllocatedVUs: 25,
      maxVUs:          60,
      exec:            "enterpriseTenant",
      startTime:       "20s",
    },

    // ── Warmup: ramps up all VUs and fires one request each ──
    warmup: {
      executor:        "ramping-vus",
      startVUs:        0,
      stages: [
        { duration: "10s", target: 45 },  // ramp up all VUs
        { duration: "10s", target: 45 },  // hold — each VU fires once, warms connection
      ],
      exec:            "warmupFunc",
      startTime:       "0s",
    },
  },

  thresholds: {
    // Only apply thresholds to non-warmup requests
    "http_req_duration{scenario:free_tenant}":       ["p(99)<500"],
    "http_req_duration{scenario:pro_tenant}":        ["p(99)<500"],
    "http_req_duration{scenario:enterprise_tenant}": ["p(99)<500"],
    error_rate: ["rate<0.01"],
  },
};

const BASE = "http://localhost:8080";

const HEADERS = {
  free:       { "X-API-Key": "nxg_free_test_key_001"  },
  pro:        { "X-API-Key": "nxg_pro_test_key_0001"  },
  enterprise: { "X-API-Key": "nxg_ent_test_key_0001"  },
};

const ENDPOINTS = ["/api/products", "/api/orders", "/api/users"];

function pickEndpoint() {
  return ENDPOINTS[Math.floor(Math.random() * ENDPOINTS.length)];
}

function doRequest(headers, tenantLabel) {
  const url = BASE + pickEndpoint();

  const res = http.get(url, {
    headers,
    timeout: "5s",   // back to 5s — cold starts are now handled by warmup
    tags:    { tenant: tenantLabel },
  });

  const ok = res.status === 200 || res.status === 429;

  check(res, {
    [`${tenantLabel} status ok`]: () => ok,
    "not a server error":         () => res.status < 500,
  });

  errorRate.add(res.status >= 500);
  latencyTrend.add(res.timings.duration);

  sleep(0.1);
}

// Warmup: just fires one request per VU to open the connection, no checks recorded
export function warmupFunc() {
  http.get(`${BASE}/api/products`, {
    headers: HEADERS.free,
    timeout: "10s",   // generous — cold start is expected here
  });
  sleep(1);
}

export function freeTenant()       { doRequest(HEADERS.free,       "free");       }
export function proTenant()        { doRequest(HEADERS.pro,        "pro");        }
export function enterpriseTenant() { doRequest(HEADERS.enterprise, "enterprise"); }