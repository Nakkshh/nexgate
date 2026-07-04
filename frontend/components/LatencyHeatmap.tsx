// components/LatencyHeatmap.tsx
"use client";

interface Region {
  name: string;
  latencyMs: number;
  thresholdMs?: number; // above this → error color
}

interface LatencyHeatmapProps {
  regions?: Region[];
  peakLoad?: string;
  optimization?: string;
}

const DEFAULT_REGIONS: Region[] = [
  { name: "US-EAST-1",    latencyMs: 42,  thresholdMs: 200 },
  { name: "EU-CENTRAL-1", latencyMs: 112, thresholdMs: 200 },
  { name: "AP-SOUTH-1",   latencyMs: 285, thresholdMs: 200 },
];

const MAX_MS = 320;

export default function LatencyHeatmap({
  regions = DEFAULT_REGIONS,
  peakLoad = "14:22 UTC",
  optimization = "ACTIVE",
}: LatencyHeatmapProps) {
  return (
    <div className="bg-[#1c1b1c] border border-[#3a393a] hover:border-primary transition-all p-6 flex flex-col gap-6">
      <div className="flex flex-col">
        <h3 className="text-sm font-bold uppercase tracking-widest text-on-surface">
          Latency Heatmap
        </h3>
        <p className="text-[11px] text-on-surface-variant opacity-40">Regional performance metrics</p>
      </div>

      <div className="space-y-5">
        {regions.map((r) => {
          const isHigh = r.latencyMs >= (r.thresholdMs ?? 200);
          const pct = Math.min((r.latencyMs / MAX_MS) * 100, 100);
          return (
            <div key={r.name} className="flex flex-col gap-1.5">
              <div className="flex justify-between text-[9px] font-bold uppercase tracking-wider text-on-surface-variant">
                <span>{r.name}</span>
                <span className={isHigh ? "text-error" : "text-primary"}>{r.latencyMs}ms</span>
              </div>
              <div className="w-full bg-surface-container-highest h-1">
                <div
                  className={`h-full transition-all duration-700 ${isHigh ? "bg-error" : "bg-primary"}`}
                  style={{ width: `${pct}%` }}
                />
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-auto pt-6 border-t border-outline-variant">
        <div className="grid grid-cols-2 gap-4">
          <div>
            <div className="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant opacity-40">
              Peak Load
            </div>
            <div className="font-mono text-xs text-on-surface">{peakLoad}</div>
          </div>
          <div>
            <div className="text-[9px] font-bold uppercase tracking-widest text-on-surface-variant opacity-40">
              Optimization
            </div>
            <div className="font-mono text-xs text-primary">{optimization}</div>
          </div>
        </div>
      </div>
    </div>
  );
}