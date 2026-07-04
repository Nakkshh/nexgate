// components/Sidebar.tsx
"use client";

import { usePathname } from "next/navigation";
import Link from "next/link";

const navItems = [
  { icon: "dashboard",  label: "Dashboard", href: "/" },
  { icon: "analytics",  label: "Traffic",   href: "/traffic" },
  { icon: "security",   label: "Security",  href: "/security" },
  { icon: "hub",        label: "Gateways",  href: "/gateways" },
  { icon: "terminal",   label: "Logs",      href: "/logs" },
];

const bottomItems = [
  { icon: "menu_book",       label: "Docs",    href: "/docs" },
  { icon: "contact_support", label: "Support", href: "/support" },
];

export default function Sidebar() {
  const pathname = usePathname();

  return (
    <aside className="hidden md:flex flex-col fixed left-0 top-0 h-full z-40 w-64 border-r border-outline-variant bg-surface-container">
      <div className="p-6 flex flex-col gap-6 h-full">
        {/* Wordmark */}
        <div className="flex flex-col">
          <span className="text-2xl font-bold tracking-tight text-on-surface">NexGate</span>
          <span className="text-[10px] uppercase font-bold text-on-surface-variant opacity-40 tracking-[0.2em] mt-1">
            Terminal v2.4.0
          </span>
        </div>

        {/* New Proxy CTA */}
        <button className="bg-primary text-on-primary px-4 py-2 flex items-center justify-center gap-2 font-bold uppercase text-[12px] tracking-wider active:scale-95 transition-transform">
          <span className="material-symbols-outlined text-sm">add</span>
          New Proxy
        </button>

        {/* Nav */}
        <nav className="flex flex-col gap-1 mt-4">
          {navItems.map(({ icon, label, href }) => {
            const active = pathname === href;
            return (
              <Link
                key={href}
                href={href}
                className={`px-4 py-2.5 flex items-center gap-3 border-l-2 transition-all text-[11px] font-bold uppercase tracking-wider ${
                  active
                    ? "text-primary bg-surface-container-highest border-primary"
                    : "text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high border-transparent hover:border-outline"
                }`}
              >
                <span className="material-symbols-outlined text-sm">{icon}</span>
                {label}
              </Link>
            );
          })}
        </nav>

        {/* Bottom links */}
        <div className="mt-auto border-t border-outline-variant pt-6 flex flex-col gap-1">
          {bottomItems.map(({ icon, label, href }) => (
            <Link
              key={href}
              href={href}
              className="text-on-surface-variant hover:text-on-surface px-4 py-2 flex items-center gap-3 transition-all text-[11px] font-bold uppercase tracking-wider"
            >
              <span className="material-symbols-outlined text-sm">{icon}</span>
              {label}
            </Link>
          ))}
        </div>
      </div>
    </aside>
  );
}