// components/TopBar.tsx
"use client";

export default function TopBar() {
  return (
    <header className="flex justify-between items-center h-16 px-6 w-full bg-surface-dim border-b border-outline-variant sticky top-0 z-30">
      {/* Left: wordmark (visible on mobile when sidebar is hidden) */}
      <div className="flex items-center gap-4">
        <div className="flex flex-col">
          <span className="text-[10px] font-bold tracking-[0.2em] text-on-surface-variant opacity-40 leading-none uppercase">
            API Gateway
          </span>
          <span className="text-lg font-bold tracking-tight text-on-surface">NexGate</span>
        </div>
      </div>

      {/* Search */}
      <div className="hidden lg:flex flex-1 max-w-xl mx-8">
        <div className="relative w-full">
          <span className="material-symbols-outlined absolute left-3 top-1/2 -translate-y-1/2 text-on-surface-variant text-sm">
            search
          </span>
          <input
            type="text"
            placeholder="Search gateways, proxies, or logs..."
            className="w-full bg-surface-container-low border border-outline-variant py-1.5 pl-10 pr-4 text-sm focus:outline-none focus:border-primary transition-all placeholder:opacity-30 rounded-none font-sans text-on-surface"
          />
        </div>
      </div>

      {/* Right: live indicator + icons */}
      <div className="flex items-center gap-4">
        {/* Live badge */}
        <div className="flex items-center gap-2 bg-surface-container-high px-3 py-1 border border-outline-variant">
          <div className="relative flex h-2 w-2">
            <span className="absolute inline-flex h-full w-full rounded-full bg-primary opacity-50 animate-ping-dot" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-primary" />
          </div>
          <span className="font-mono text-[10px] text-primary uppercase font-bold tracking-widest">Live</span>
        </div>

        {/* Icon row */}
        <div className="hidden sm:flex items-center gap-3 text-on-surface-variant">
          <span className="material-symbols-outlined cursor-pointer hover:text-primary transition-colors text-xl">
            terminal
          </span>
          <span className="material-symbols-outlined cursor-pointer hover:text-primary transition-colors text-xl">
            settings
          </span>
          {/* Avatar */}
          <div className="h-7 w-7 bg-surface-container-highest border border-outline-variant flex items-center justify-center text-on-surface font-bold text-[10px] cursor-pointer">
            AC
          </div>
        </div>
      </div>
    </header>
  );
}