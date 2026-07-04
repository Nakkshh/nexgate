// components/TerminalLogs.tsx
"use client";

import { useEffect, useRef } from "react";

export interface LogEntry {
  time: string;
  level: "INFO" | "WARN" | "ERROR" | "RECV";
  message: string;
}

interface TerminalLogsProps {
  logs: LogEntry[];
  maxVisible?: number;
}

const LEVEL_COLOR: Record<LogEntry["level"], string> = {
  INFO:  "text-primary",
  RECV:  "text-primary",
  WARN:  "text-error",
  ERROR: "text-error",
};

export default function TerminalLogs({ logs, maxVisible = 20 }: TerminalLogsProps) {
  const containerRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to top (newest logs prepended)
  useEffect(() => {
    if (containerRef.current) containerRef.current.scrollTop = 0;
  }, [logs]);

  const visible = logs.slice(0, maxVisible);

  return (
    <div className="bg-[#1c1b1c] border border-[#3a393a] hover:border-primary transition-all overflow-hidden">
      {/* Terminal chrome */}
      <div className="bg-surface-container px-4 py-2 flex items-center justify-between border-b border-outline-variant">
        <div className="flex items-center gap-2">
          <span className="material-symbols-outlined text-xs text-on-surface-variant">terminal</span>
          <span className="text-[10px] font-bold uppercase tracking-[0.2em] text-on-surface-variant">
            Real-time Ingress Logs
          </span>
        </div>
        <div className="flex gap-2">
          <div className="w-1.5 h-1.5 rounded-full bg-outline-variant" />
          <div className="w-1.5 h-1.5 rounded-full bg-outline-variant" />
          <div className="w-1.5 h-1.5 rounded-full bg-outline-variant" />
        </div>
      </div>

      {/* Log lines */}
      <div
        ref={containerRef}
        className="p-4 font-mono text-[11px] h-48 overflow-y-auto space-y-1 bg-surface-container-lowest"
      >
        {visible.map((entry, i) => (
          <div key={i} className="flex gap-4">
            <span className="text-on-surface-variant opacity-20 shrink-0">{entry.time}</span>
            <span className={`${LEVEL_COLOR[entry.level]} shrink-0`}>[{entry.level}]</span>
            <span className="text-on-surface-variant">{entry.message}</span>
          </div>
        ))}
      </div>
    </div>
  );
}