"use client";

import { m } from "framer-motion";
import {
  ArrowDownToLine,
  Github,
  Monitor,
  FileText,
  Download,
  ShieldCheck,
  Star,
} from "lucide-react";
import Link from "next/link";
import {
  DESKTOP_ZIP_URL,
  DESKTOP_MSI_URL,
  APK_URL,
  GITHUB_URL,
  cn,
} from "@/lib/utils";
import { Stagger } from "../ui/reveal";
import { LogoMark } from "./LogoMark";

const SPRING = { type: "spring", bounce: 0, duration: 0.5 } as const;

const proof = [
  ["OS", "Windows 10/11 x64"],
  ["Install", "≈180 MB ZIP / MSI"],
  ["JDK", "Bundled (JDK 21)"],
  ["Account", "None"],
];

export function Hero() {
  return (
    <section id="top" className="relative overflow-hidden pt-16">
      {/* Backdrop: grid + violet glows */}
      <div aria-hidden className="hero-grid absolute inset-0" />
      <div
        aria-hidden
        className="absolute left-1/2 top-[-300px] h-[560px] w-[900px] -translate-x-1/2 rounded-full bg-accent/20 blur-[140px]"
      />
      <div
        aria-hidden
        className="absolute right-[-200px] top-[35%] h-[420px] w-[420px] rounded-full bg-signal/10 blur-[120px]"
      />
      <div
        aria-hidden
        className="absolute inset-x-0 bottom-0 h-52 bg-gradient-to-t from-void to-transparent"
      />

      <div className="relative mx-auto max-w-editorial px-5 sm:px-8">
        <div className="grid items-center gap-14 pb-20 pt-14 md:pt-24 lg:grid-cols-[1.05fr_0.95fr] lg:gap-8 lg:pb-32">
          <Stagger
            className="max-w-2xl"
            items={[
              <span
                key="badge"
                className="eyebrow mb-10 inline-flex items-center gap-2.5 rounded-full border border-rule bg-white/[0.04] py-2 pl-3 pr-4 text-muted"
              >
                <span className="h-1.5 w-1.5 rounded-full bg-accent animate-pulse-soft" />
                Moovio Desktop v0.1.0 — native Windows client
              </span>,

              <h1 key="h1" className="display text-paper" style={{ fontSize: "clamp(3rem, 8vw, 6rem)" }}>
                <span className="block font-display font-bold">Every stream.</span>
                <span className="text-gradient block font-display font-bold">One app.</span>
              </h1>,

              <p key="deck" className="mt-8 max-w-xl text-[17px] leading-relaxed text-muted md:text-[20px]">
                A premium native Windows streaming client built with
                <span className="text-paper">Kotlin + Compose Multiplatform</span>
                and <span className="text-paper">libmpv</span>. Dark cinematic
                UI, glass surfaces, animated navigation, hardware-accelerated
                playback, and Stremio add-on support — not Electron, not a
                stretched phone UI.
              </p>,

              <div key="ctas" className="mt-12 flex flex-wrap items-center gap-4">
                <ButtonDesktop
                  href={DESKTOP_MSI_URL}
                  icon={<Monitor className="h-4 w-4" />}
                  label="Windows Installer (MSI)"
                  variant="default"
                />
                <ButtonDesktop
                  href={DESKTOP_ZIP_URL}
                  icon={<FileText className="h-4 w-4" />}
                  label="Portable ZIP"
                  variant="outline"
                />
                <ButtonDesktop
                  href={APK_URL}
                  icon={<Download className="h-4 w-4" />}
                  label="Android APK"
                  variant="ghost"
                />
              </div>,

              <div key="assure" className="mt-10 flex flex-wrap items-center gap-x-6 gap-y-3 text-[13px] text-faint">
                <span className="inline-flex items-center gap-1.5">
                  <ShieldCheck className="h-3.5 w-3.5 text-accent" />
                  Hardware decode (D3D11)
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Star className="h-3.5 w-3.5 text-accent-soft" />
                  24+ unit tests, all green
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <span className="h-1.5 w-1.5 rounded-full bg-signal animate-pulse-soft" />
                  No account, no ads
                </span>
              </div>,

              <div key="proof" className="mt-12 block border-t border-rule pt-6">
                <dl className="grid grid-cols-2 gap-x-8 gap-y-5 sm:grid-cols-4">
                  {proof.map(([k, v]) => (
                    <div key={k}>
                      <dt className="eyebrow text-[10px] text-faint">{k}</dt>
                      <dd className="tabular mt-2 font-display text-[16px] font-semibold tracking-tight text-paper">
                        {v}
                      </dd>
                    </div>
                  ))}
                </dl>
              </div>,
            ]}
          />

          <m.div
            initial={{ opacity: 0, y: 32 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ ...SPRING, delay: 0.3 }}
            className="relative flex justify-center"
          >
            <div
              aria-hidden
              className="absolute bottom-4 left-1/2 h-28 w-[80%] -translate-x-1/2 rounded-full bg-accent/30 blur-3xl"
            />
            <DesktopMockup />
          </m.div>
        </div>
      </div>
    </section>
  );
}

function ButtonDesktop({
  href,
  icon,
  label,
  variant,
}: {
  href: string;
  icon: React.ReactNode;
  label: string;
  variant: "default" | "outline" | "ghost";
}) {
  return (
    <Link
      href={href}
      className={cn(
        "press inline-flex items-center justify-center gap-2.5 rounded-xl px-8 py-4 text-[15px] font-semibold tracking-tight transition-colors",
        variant === "default"
          ? "bg-primary text-primary-foreground shadow-glow-accent hover:bg-primary/90"
          : variant === "outline"
            ? "border border-input bg-background/30 text-paper hover:border-white/50 hover:bg-white/[0.06]"
            : "bg-white/[0.04] text-paper hover:bg-white/[0.08]",
      )}
    >
      {icon}
      {label}
    </Link>
  );
}

/** Desktop app mockup: native window frame with sidebar + home feed. */
function DesktopMockup() {
  return (
    <div className="relative flex flex-col w-full max-w-[720px] aspect-video rounded-[28px] border border-white/10 bg-[#0a0a11] shadow-card shadow-glow-soft overflow-hidden">
      {/* Title bar */}
      <div className="flex items-center gap-2 px-5 py-3 bg-[#07070b]/60 backdrop-blur border-b border-white/5">
        {[1, 2, 3].map((i) => (
          <div
            key={i}
            className="h-3 w-3 rounded-full bg-white/10"
            style={{
              backgroundColor:
                i === 1 ? "#ff5f56" : i === 2 ? "#ffbd2e" : "#27c93f",
            }}
          />
        ))}
        <div className="ml-4 flex-1" />
        <div className="flex items-center gap-1 text-[10px] text-muted">
          Moovio · v0.1.0
        </div>
      </div>

      {/* App content */}
      <div className="flex flex-col h-full">
        {/* Sidebar */}
        <div className="flex h-full border-r border-white/5 bg-[#07070b]/40">
          <div className="flex h-full flex-col py-4">
            <div className="flex items-center gap-2 px-4 py-3">
              <div className="h-8 w-8 rounded-xl bg-gradient-to-br from-accent via-accent/80 to-accent/60 flex items-center justify-center">
                <svg className="h-5 w-5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M2 12s3-4 10-4 10 4 10 4" />
                  <circle cx="6" cy="12" r="3" />
                </svg>
              </div>
              <span className="font-display text-xs font-bold tracking-wider text-paper">MOOVIO</span>
            </div>
            <nav className="flex-1 px-3 space-y-1">
              {[
                { label: "Home", icon: "◍", active: true },
                { label: "Movies", icon: "▶" },
                { label: "TV", icon: "▱" },
                { label: "Anime", icon: "✦" },
                { label: "Library", icon: "▩" },
              ].map((item) => (
                <div
                  key={item.label}
                  className={cn(
                    "flex items-center gap-3 px-3 py-2.5 rounded-lg transition-colors text-[13px]",
                    item.active
                      ? "bg-accent/10 text-paper border border-accent/20"
                      : "text-muted hover:bg-white/05 hover:text-paper",
                  )}
                >
                  <span className="text-xs">{item.icon}</span>
                  {item.label}
                </div>
              ))}
            </nav>
            <div className="mt-auto px-3 py-4">
              <div className="flex items-center gap-2 px-2 py-2 rounded-xl border border-white/5 bg-white/[0.02]">
                <Monitor className="h-4 w-4 text-accent-soft" />
                <div className="flex-1">
                  <div className="text-[11px] font-medium text-paper">Windows Desktop</div>
                  <div className="text-[10px] text-muted">v0.1.0 · Stremio ready</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Main content */}
        <div className="flex-1 overflow-hidden bg-[#07070b]/20 relative">
          {/* Hero artwork area */}
          <div className="absolute inset-0 bg-gradient-to-br from-indigo-600/20 via-violet-700/10 to-fuchsia-700/5" />
          <div
            className="absolute inset-0 bg-[radial-gradient(ellipse_at_80%_20%,rgba(124,92,255,0.25),transparent_50%)]"
          />
          <div className="absolute inset-0 bg-gradient-to-t from-[#07070b]/90 via-transparent to-transparent" />

          {/* Overlay content */}
          <div className="absolute inset-0 flex flex-col justify-between p-8 pointer-events-none">
            <div className="flex justify-between items-start">
              <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 bg-black/30 backdrop-blur-sm">
                <span className="h-1.5 w-1.5 rounded-full bg-accent" />
                <span className="text-[10px] font-medium text-muted">Featured</span>
              </div>
              <div className="flex items-center gap-2">
                <div className="h-9 w-9 rounded-lg border border-white/10 bg-white/5 flex items-center justify-center">
                  <svg className="h-4 w-4 text-white" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24"><path d="M2 12s3-4 10-4 10 4 10 4"/><circle cx="6" cy="12" r="3"/></svg>
                </div>
              </div>
            </div>

            <div className="mt-auto mb-4">
              <h2 className="text-[2.5rem] font-display font-bold text-paper leading-[1.1]">Aurora Protocol</h2>
              <div className="mt-2 flex items-center gap-2 text-xs text-muted">
                <span>Series · 2026 · 9.2</span>
                <span className="h-1 w-1 rounded-full bg-accent" />
                <span>S1 · E4</span>
              </div>
              <div className="mt-4 flex items-center gap-3">
                <button className="flex items-center gap-2 rounded-xl bg-accent px-5 py-2.5 text-[13px] font-semibold text-white hover:bg-primary/90">
                  <svg className="h-4 w-4 text-[#0a0a11]" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                  Play
                </button>
                <button className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-5 py-2.5 text-[13px] font-semibold text-muted hover:bg-white/[0.1]">
                  Sources
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
