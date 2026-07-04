// components/MetricCard.tsx
"use client";

import { useEffect, useRef, useState } from "react";

interface MetricCardProps {
  label: string;
  icon: string;
  value: string | number;
  subtext?: React.ReactNode;
}

export default function MetricCard({ label, icon, value, subtext }: MetricCardProps) {
  const [flicker, setFlicker] = useState(false);
  const prevValue = useRef(value);

  useEffect(() => {
    if (prevValue.current !== value) {
      setFlicker(true);
      const t = setTimeout(() => setFlicker(false), 300);
      prevValue.current = value;
      return () => clearTimeout(t);
    }
  }, [value]);

  return (
    <div className="bg-[#1c1b1c] border border-[#3a393a] hover:border-primary transition-all p-5 relative overflow-hidden">
      {/* Header row */}
      <div className="flex justify-between items-start mb-4">
        <span className="text-[11px] font-bold uppercase tracking-widest text-on-surface-variant">
          {label}
        </span>
        <span className="material-symbols-outlined text-primary text-sm">{icon}</span>
      </div>

      {/* Value */}
      <div
        className={`text-3xl font-bold tracking-tight text-on-surface tabular-nums ${
          flicker ? "animate-flicker" : ""
        }`}
      >
        {value}
      </div>

      {/* Sub-text slot */}
      {subtext && <div className="mt-2">{subtext}</div>}
    </div>
  );
}