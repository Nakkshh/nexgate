// app/page.tsx  (or components/Dashboard.tsx if you prefer — just drop the default export)
"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import MetricCard from "@/components/MetricCard";
import TrafficChart from "@/components/TrafficChart";
import LatencyHeatmap from "@/components/LatencyHeatmap";
import TerminalLogs, { LogEntry } from "@/components/TerminalLogs";

// ─── Types ────────────────────────────────────────────────────────────────────
interface MetricPoint {
  windowStart: string;
  reqCount: number;
  errorCount: number;
  avgLatencyMs: number;
  rateLimitHits: number;
  tenantId: string;
}

interface ChartPoint {
  time: string;
  requests: number;
  errors: number;
  latency: number;
  rateLimitHits: number;
}

// ─── Config ───────────────────────────────────────────────────────────────────
const TENANT_ID    = "11111111-1111-1111-1111-111111111111";
const CONSUMER_URL = "http://localhost:8082";

// ─── Helpers ──────────────────────────────────────────────────────────────────
function toChartPoint(m: MetricPoint): ChartPoint {
  return {
    time:          new Date(m.windowStart).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }),
    requests:      m.reqCount,
    errors:        m.errorCount,
    latency:       Math.round(m.avgLatencyMs ?? 0),
    rateLimitHits: m.rateLimitHits,
  };
}

function nowTime() {
  return new Date().toLocaleTimeString([], { hour12: false });
}

// Seed logs shown before WS connects
const SEED_LOGS: LogEntry[] = [
  { time: "14:55:01", level: "INFO",  message: "Proxy request accepted: GET /api/v1/users (200 OK)" },
  { time: "14:55:04", level: "INFO",  message: "Proxy request accepted: POST /api/v1/auth/login (201 Created)" },
  { time: "14:55:08", level: "WARN",  message: "Rate limit approaching for IP 192.168.1.1" },
  { time: "14:55:12", level: "INFO",  message: "Proxy request accepted: GET /api/v1/assets/logo.png (200 OK)" },
];

// ─── Component ────────────────────────────────────────────────────────────────
export default function DashboardPage() {
  const [chartData, setChartData] = useState<ChartPoint[]>([]);
  const [connected, setConnected] = useState(false);
  const [logs, setLogs]           = useState<LogEntry[]>(SEED_LOGS);

  // Metric card state
  const [requests,    setRequests]    = useState(4829);
  const [errorRate,   setErrorRate]   = useState(0.42);
  const [latency,     setLatency]     = useState(142);
  const [rateLimits,  setRateLimits]  = useState(12);

  const clientRef = useRef<Client | null>(null);

  // Push a new log line (prepend so newest is first)
  const pushLog = useCallback((entry: LogEntry) => {
    setLogs((prev) => [entry, ...prev].slice(0, 40));
  }, []);

  // ── History fetch ──────────────────────────────────────────────────────────
  useEffect(() => {
    fetch(`${CONSUMER_URL}/metrics/history/${TENANT_ID}?minutes=60`)
      .then((r) => r.json())
      .then((data: MetricPoint[]) => {
        const points = [...data].reverse().map(toChartPoint);
        setChartData(points);
        if (points.length) {
          const last = points[points.length - 1];
          setRequests(last.requests);
          setLatency(last.latency);
          setRateLimits(last.rateLimitHits);
        }
      })
      .catch(() => {
        // Backend not running yet — simulation takes over below
      });
  }, []);

  // ── WebSocket ──────────────────────────────────────────────────────────────
  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${CONSUMER_URL}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/metrics/${TENANT_ID}`, (msg) => {
          const metric: MetricPoint = JSON.parse(msg.body);
          const point = toChartPoint(metric);

          setRequests(metric.reqCount);
          setLatency(Math.round(metric.avgLatencyMs));
          setRateLimits(metric.rateLimitHits);

          setChartData((prev) => {
            const idx = prev.findIndex((p) => p.time === point.time);
            if (idx >= 0) {
              const updated = [...prev];
              updated[idx] = point;
              return updated;
            }
            return [...prev.slice(-29), point];
          });

          pushLog({ time: nowTime(), level: "RECV", message: `Metric heartbeat received from worker-node-04` });
        });
      },
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, [pushLog]);

  // ── Client-side simulation (runs when WS is not connected) ─────────────────
  useEffect(() => {
    if (connected) return;

    const id = setInterval(() => {
      setRequests((v) => v + Math.floor(Math.random() * 5));
      setLatency(140 + Math.floor(Math.random() * 8));
      setRateLimits((v) => v + (Math.random() > 0.95 ? 1 : 0));

      // Add a simulated chart point
      const now = nowTime();
      setChartData((prev) => {
        const point: ChartPoint = {
          time:          now,
          requests:      4800 + Math.floor(Math.random() * 200),
          errors:        Math.floor(Math.random() * 10),
          latency:       140 + Math.floor(Math.random() * 40),
          rateLimitHits: Math.floor(Math.random() * 3),
        };
        return [...prev.slice(-29), point];
      });

      pushLog({ time: nowTime(), level: "RECV", message: "Metric heartbeat received from worker-node-04" });
    }, 3000);

    return () => clearInterval(id);
  }, [connected, pushLog]);

  // ─── Error rate icon logic ─────────────────────────────────────────────────
  const errorOk = errorRate < 5.0;

  return (
    <div className="space-y-8">
      {/* Connection / tenant bar */}
      <div className="flex flex-wrap items-center justify-between gap-4 bg-surface-container-lowest border border-outline-variant p-4">
        <div className="flex items-center gap-8">
          <div className="flex flex-col">
            <span className="text-[10px] font-bold text-on-surface-variant opacity-40 uppercase tracking-widest">
              Tenant ID
            </span>
            <span className="font-mono text-xs text-on-surface">{TENANT_ID}</span>
          </div>
          <div className="h-6 w-px bg-outline-variant" />
          <div className="flex flex-col">
            <span className="text-[10px] font-bold text-on-surface-variant opacity-40 uppercase tracking-widest">
              License
            </span>
            <span className="text-[11px] font-bold text-primary uppercase tracking-wide">
              Acme Corp · Free Tier
            </span>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <span className="text-[10px] font-bold text-on-surface-variant opacity-40 uppercase tracking-widest">
            Uptime:
          </span>
          <span className="font-mono text-xs text-on-surface">99.998%</span>
        </div>
      </div>

      {/* Metric cards */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <MetricCard
          label="Requests/min"
          icon="speed"
          value={requests.toLocaleString()}
          subtext={
            <div className="flex items-center gap-2 text-primary">
              <span className="material-symbols-outlined text-[10px]">trending_up</span>
              <span className="font-mono text-[10px]">+12.4% vs last hour</span>
            </div>
          }
        />
        <MetricCard
          label="Error Rate"
          icon={errorOk ? "check_circle" : "error"}
          value={`${errorRate.toFixed(2)}%`}
          subtext={
            <div className="flex items-center gap-2 text-on-surface-variant opacity-40">
              <span className="font-mono text-[10px]">
                Threshold: <span className="text-error opacity-100">5.0%</span>
              </span>
            </div>
          }
        />
        <MetricCard
          label="Avg Latency"
          icon="timer"
          value={
            <span>
              {latency}
              <span className="text-[10px] font-bold text-on-surface-variant opacity-40 uppercase ml-1">ms</span>
            </span>
          }
          subtext={
            <div className="text-on-surface-variant opacity-40">
              <span className="font-mono text-[10px]">P99: 310ms</span>
            </div>
          }
        />
        <MetricCard
          label="Rate Limit Hits"
          icon="block"
          value={rateLimits}
          subtext={
            <div className="flex items-center gap-2 text-error">
              <span className="material-symbols-outlined text-[10px]">warning</span>
              <span className="font-mono text-[10px]">3 IPs blacklisted</span>
            </div>
          }
        />
      </div>

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <TrafficChart data={chartData} />
        <LatencyHeatmap />
      </div>

      {/* Terminal logs */}
      <TerminalLogs logs={logs} />
    </div>
  );
}