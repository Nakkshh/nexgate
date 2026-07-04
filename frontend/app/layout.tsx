// app/layout.tsx
import type { Metadata } from "next";
import { Inter, JetBrains_Mono } from "next/font/google";
import "./globals.css";
import Sidebar from "@/components/Sidebar";
import TopBar from "@/components/TopBar";

const inter = Inter({
  subsets: ["latin"],
  variable: "--font-inter",
});

const jetbrainsMono = JetBrains_Mono({
  subsets: ["latin"],
  variable: "--font-mono",
});

export const metadata: Metadata = {
  title: "NexGate | Real-time API Gateway Analytics",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en" className="dark">
      <head>
        <link
          rel="stylesheet"
          href="https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined"
        />
      </head>
      <body
        className={`
          ${inter.variable}
          ${jetbrainsMono.variable}
          font-sans
          bg-[#131314]
          text-[#e2e2e2]
          overflow-x-hidden
        `}
      >
        <Sidebar />

        <div className="md:ml-64 min-h-screen flex flex-col">
          <TopBar />
          <main className="flex-1 p-6 lg:p-10">{children}</main>
        </div>
      </body>
    </html>
  );
}