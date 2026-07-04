"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import { Client } from "@stomp/stompjs";
import SockJS from "sockjs-client";
import MetricCard from "@/components/MetricCard";
import TrafficChart from "@/components/TrafficChart";
import LatencyHeatmap from "@/components/LatencyHeatmap";
import TerminalLogs, { LogEntry } from "@/components/TerminalLogs";

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

const TENANT_ID    = "11111111-1111-1111-1111-111111111111";
const CONSUMER_URL = process.env.NEXT_PUBLIC_CONSUMER_URL || "http://localhost:8082";
const GATEWAY_URL  = process.env.NEXT_PUBLIC_GATEWAY_URL  || "https://nexgate-gateway-1aws.onrender.com";

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

const SEED_LOGS: LogEntry[] = [
  { time: "14:55:01", level: "INFO", message: "Proxy request accepted: GET /api/v1/users (200 OK)" },
  { time: "14:55:04", level: "INFO", message: "Proxy request accepted: POST /api/v1/auth/login (201 Created)" },
  { time: "14:55:08", level: "WARN", message: "Rate limit approaching for IP 192.168.1.1" },
  { time: "14:55:12", level: "INFO", message: "Proxy request accepted: GET /api/v1/assets/logo.png (200 OK)" },
];

export default function DashboardPage() {
  const [chartData, setChartData] = useState<ChartPoint[]>([]);
  const [connected, setConnected] = useState(false);
  const [logs, setLogs]           = useState<LogEntry[]>(SEED_LOGS);

  const [requests,   setRequests]   = useState(4829);
  const [errorRate,  setErrorRate]  = useState(0.42);
  const [latency,    setLatency]    = useState(142);
  const [rateLimits, setRateLimits] = useState(12);

  // Demo control state
  const [firing,        setFiring]        = useState(false);
  const [firedCount,    setFiredCount]    = useState(0);
  const [totalToFire,   setTotalToFire]   = useState(0);
  const [waking,        setWaking]        = useState(false);
  const [gatewayStatus, setGatewayStatus] = useState<"unknown" | "awake" | "sleeping">("unknown");

  const clientRef = useRef<Client | null>(null);

  const pushLog = useCallback((entry: LogEntry) => {
    setLogs((prev) => [entry, ...prev].slice(0, 40));
  }, []);

  const wakeGateway = async () => {
    setWaking(true);
    setGatewayStatus("sleeping");
    try {
      const res = await fetch(`${GATEWAY_URL}/actuator/health`);
      if (res.ok) {
        setGatewayStatus("awake");
        pushLog({ time: nowTime(), level: "INFO", message: "Gateway is awake and healthy" });
      } else {
        setGatewayStatus("sleeping");
      }
    } catch {
      setGatewayStatus("sleeping");
      pushLog({ time: nowTime(), level: "WARN", message: "Gateway cold start in progress — try again in 30s" });
    }
    setWaking(false);
  };

  const fireRequests = async (count: number) => {
    if (gatewayStatus !== "awake") return;
    setFiring(true);
    setFiredCount(0);
    setTotalToFire(count);
    pushLog({ time: nowTime(), level: "INFO", message: `Firing ${count} requests through gateway pipeline...` });
    for (let i = 0; i < count; i++) {
      try {
        await fetch(`${GATEWAY_URL}/api/todos`, {
          headers: { "X-API-Key": "nxg_free_test_key_001" },
        });
      } catch {}
      setFiredCount(i + 1);
      await new Promise(r => setTimeout(r, 150));
    }
    pushLog({ time: nowTime(), level: "INFO", message: `Done — ${count} requests fired. Watch metrics update.` });
    setFiring(false);
  };

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
      .catch(() => {});
  }, []);

  useEffect(() => {
    const client = new Client({
      webSocketFactory: () => new SockJS(`${CONSUMER_URL}/ws`),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        client.subscribe(`/topic/metrics/${TENANT_ID}`, (msg) => {
          const metric: MetricPoint = JSON.parse(msg.body);
          const point = toChartPoint(metric);
          const rate = metric.reqCount > 0 ? (metric.errorCount / metric.reqCount) * 100 : 0;

          setRequests(metric.reqCount);
          setLatency(Math.round(metric.avgLatencyMs));
          setRateLimits(metric.rateLimitHits);
          setErrorRate(parseFloat(rate.toFixed(2)));

          setChartData((prev) => {
            const idx = prev.findIndex((p) => p.time === point.time);
            if (idx >= 0) {
              const updated = [...prev];
              updated[idx] = point;
              return updated;
            }
            return [...prev.slice(-29), point];
          });

          pushLog({ time: nowTime(), level: "RECV", message: `Metric heartbeat — ${metric.reqCount} req, ${metric.errorCount} errors` });
        });
      },
      onDisconnect: () => setConnected(false),
    });

    client.activate();
    clientRef.current = client;
    return () => { client.deactivate(); };
  }, [pushLog]);

  useEffect(() => {
    if (connected) return;
    const id = setInterval(() => {
      setRequests((v) => v + Math.floor(Math.random() * 5));
      setLatency(140 + Math.floor(Math.random() * 8));
      setRateLimits((v) => v + (Math.random() > 0.95 ? 1 : 0));
      const now = nowTime();
      setChartData((prev) => {
        const point: ChartPoint = {
          time: now,
          requests: 4800 + Math.floor(Math.random() * 200),
          errors: Math.floor(Math.random() * 10),
          latency: 140 + Math.floor(Math.random() * 40),
          rateLimitHits: Math.floor(Math.random() * 3),
        };
        return [...prev.slice(-29), point];
      });
      pushLog({ time: nowTime(), level: "RECV", message: "Metric heartbeat received from worker-node-04" });
    }, 3000);
    return () => clearInterval(id);
  }, [connected, pushLog]);

  const errorOk = errorRate < 5.0;

  const btnBase: React.CSSProperties = {
    borderRadius: 8,
    padding: "10px 18px",
    fontSize: 12,
    fontWeight: 600,
    fontFamily: "'Space Grotesk', sans-serif",
    letterSpacing: "0.05em",
    whiteSpace: "nowrap",
    transition: "all 0.2s",
    cursor: "pointer",
    border: "none",
  };

  return (
    <div className="space-y-8">

      {/* Demo Control Panel */}
      <div className="bg-[#1c1b1c] border border-[#3a393a] hover:border-primary transition-all p-5">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h3 className="text-[11px] font-bold uppercase tracking-widest text-on-surface">
              Demo Controls
            </h3>
            <p className="text-[11px] text-on-surface-variant mt-1 font-mono">
              {firing
                ? `[ firing ${firedCount}/${totalToFire} requests... ]`
                : gatewayStatus === "awake"
                ? "[ gateway online — ready to fire ]"
                : "[ wake gateway before firing requests ]"}
            </p>
          </div>
          {/* Status indicator */}
          <div className="flex items-center gap-2">
            <div className={`w-1.5 h-1.5 rounded-full ${
              gatewayStatus === "awake" ? "bg-primary" :
              gatewayStatus === "sleeping" ? "bg-error" : "bg-outline"
            }`} />
            <span className="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant">
              {gatewayStatus === "awake" ? "Online" : gatewayStatus === "sleeping" ? "Offline" : "Unknown"}
            </span>
          </div>
        </div>

        <div className="flex flex-wrap gap-3">
          {/* Wake button */}
          <button
            onClick={wakeGateway}
            disabled={waking || firing}
            className={`px-4 py-2 text-[11px] font-bold uppercase tracking-wider border transition-all ${
              gatewayStatus === "awake"
                ? "border-primary text-primary bg-primary-container"
                : "border-outline text-on-surface-variant hover:border-primary hover:text-primary"
            } ${waking || firing ? "opacity-40 cursor-not-allowed" : "cursor-pointer"}`}
          >
            {waking ? "CHECKING..." : gatewayStatus === "awake" ? "✓ GATEWAY AWAKE" : "⟳ WAKE GATEWAY"}
          </button>

          {/* Fire 20 requests */}
          <button
            onClick={() => fireRequests(20)}
            disabled={firing || gatewayStatus !== "awake"}
            className={`px-4 py-2 text-[11px] font-bold uppercase tracking-wider border transition-all ${
              firing || gatewayStatus !== "awake"
                ? "border-outline-variant text-on-surface-variant opacity-40 cursor-not-allowed"
                : "border-primary bg-primary text-on-primary hover:bg-secondary cursor-pointer"
            }`}
          >
            {firing && totalToFire === 20 ? `FIRING ${firedCount}/20...` : "⚡ FIRE 20 REQUESTS"}
          </button>

          {/* Trigger rate limit */}
          <button
            onClick={() => fireRequests(105)}
            disabled={firing || gatewayStatus !== "awake"}
            className={`px-4 py-2 text-[11px] font-bold uppercase tracking-wider border transition-all ${
              firing || gatewayStatus !== "awake"
                ? "border-outline-variant text-on-surface-variant opacity-40 cursor-not-allowed"
                : "border-error text-error hover:bg-error-container cursor-pointer"
            }`}
          >
            {firing && totalToFire === 105 ? `RATE LIMITING ${firedCount}/105...` : "⚠ TRIGGER RATE LIMIT"}
          </button>
        </div>
      </div>

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