// components/TrafficChart.tsx
"use client";

import {
  ResponsiveContainer,
  ComposedChart,
  Bar,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from "recharts";

interface ChartPoint {
  time: string;
  requests: number;
  errors: number;
}

interface TrafficChartProps {
  data: ChartPoint[];
}

export default function TrafficChart({ data }: TrafficChartProps) {
  return (
    <div className="bg-[#1c1b1c] border border-[#3a393a] hover:border-primary transition-all p-6 lg:col-span-2 flex flex-col gap-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div className="flex flex-col">
          <h3 className="text-sm font-bold uppercase tracking-widest text-on-surface">
            Traffic Overview
          </h3>
          <p className="text-[11px] text-on-surface-variant opacity-40">
            Request volume vs Error responses (last 60m)
          </p>
        </div>

        <div className="flex gap-2">
          <span className="flex items-center gap-2 px-2 py-1 bg-surface-container border border-outline-variant text-[9px] font-bold uppercase tracking-wider">
            <span className="w-1.5 h-1.5 bg-primary inline-block" /> Requests
          </span>
          <span className="flex items-center gap-2 px-2 py-1 bg-surface-container border border-outline-variant text-[9px] font-bold uppercase tracking-wider">
            <span className="w-1.5 h-1.5 bg-error inline-block" /> Errors
          </span>
        </div>
      </div>

      {/* Chart */}
      <div className="h-64 w-full">
        <ResponsiveContainer width="100%" height="100%">
          <ComposedChart data={data} margin={{ top: 4, right: 4, bottom: 0, left: -20 }}>
            <CartesianGrid strokeDasharray="2 6" stroke="#3a393a" vertical={false} />
            <XAxis
              dataKey="time"
              tick={{ fontSize: 9, fill: "#a1a1aa", fontFamily: "JetBrains Mono, monospace" }}
              axisLine={false}
              tickLine={false}
            />
            <YAxis
              tick={{ fontSize: 9, fill: "#a1a1aa", fontFamily: "JetBrains Mono, monospace" }}
              axisLine={false}
              tickLine={false}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: "#1c1b1c",
                border: "1px solid #3a393a",
                borderRadius: 0,
                fontFamily: "JetBrains Mono, monospace",
                fontSize: 11,
                color: "#e2e2e2",
              }}
              cursor={{ fill: "rgba(16,185,129,0.05)" }}
            />
            <Bar dataKey="requests" fill="rgba(16,185,129,0.15)" stroke="#10b981" strokeWidth={1} name="Requests" />
            <Line
              type="monotone"
              dataKey="errors"
              stroke="#ef4444"
              strokeWidth={1.5}
              dot={false}
              name="Errors"
            />
          </ComposedChart>
        </ResponsiveContainer>
      </div>
    </div>
  );
}