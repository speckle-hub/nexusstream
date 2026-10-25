"use client";

import { Play, Monitor, Layers } from "lucide-react";
import { DragRail } from "../ui/drag-rail";
import { Tilt } from "../ui/tilt";
import { Stagger } from "../ui/reveal";

function Frame({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <figure style={{ width: 420 }} className="shrink-0 select-none">
      <Tilt max={7} allowScroll cardClassName="rounded-2xl">
        <div className="overflow-hidden rounded-2xl border border-rule bg-[#07070b] shadow-card">
          {children}
        </div>
      </Tilt>
      <figcaption className="eyebrow mt-3 text-center text-[9px] text-faint">{label}</figcaption>
    </figure>
  );
}

function DesktopHomeShot() {
  return (
    <div className="relative aspect-video w-full">
      {/* Background glow */}
      <div className="absolute inset-0 bg-[linear-gradient(135deg,#1b1730_0%,#0d0d16_55%,#171026_100%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_30%_25%,rgba(124,92,255,0.3),transparent_60%)]" />

      {/* Window chrome */}
      <div className="absolute inset-x-0 top-0 h-12 flex items-center justify-between px-6 z-10 pointer-events-none">
        <div className="flex items-center gap-1.5">
          {[1, 2, 3].map((i) => (
            <div
              key={i}
              className="h-2.5 w-2.5 rounded-full bg-white/10"
              style={{
                backgroundColor:
                  i === 1 ? "#ff5f56" : i === 2 ? "#ffbd2e" : "#27c93f",
              }}
            />
          ))}
        </div>
        <div className="text-[10px] text-muted">Moovio · v0.1.0</div>
      </div>

      {/* Sidebar */}
      <div className="absolute inset-y-0 left-0 w-60 border-r border-white/5 bg-[#0a0a11]/40 backdrop-blur-sm flex flex-col">
        <div className="flex items-center gap-2.5 px-5 py-4">
          <div className="h-8 w-8 rounded-lg bg-gradient-to-br from-accent via-accent/80 to-accent/60 flex items-center justify-center">
            <svg className="h-4.5 w-4.5 text-white" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <path d="M2 12s3-4 10-4 10 4 10 4" />
              <circle cx="6" cy="12" r="3" />
            </svg>
          </div>
          <span className="font-display text-xs font-bold tracking-wider text-paper">MOOVIO</span>
        </div>
        <nav className="flex-1 px-3 space-y-1">
          {[
            { label: "Home", icon: "⌂", active: true },
            { label: "Movies", icon: "▶" },
            { label: "TV", icon: "▱" },
            { label: "Anime", icon: "✦" },
            { label: "Library", icon: "▩" },
            { label: "Settings", icon: "⚙" },
          ].map((item) => (
            <div
              key={item.label}
              className="flex items-center gap-2.5 px-2.5 py-2 rounded-lg transition-colors text-[12px]"
              style={{
                backgroundColor: item.active ? "rgba(124,92,255,0.12)" : "transparent",
                color: item.active ? "#EDEDF2" : "#9A9AB0",
                borderColor: item.active ? "rgba(124,92,255,0.3)" : "transparent",
                borderWidth: item.active ? 1 : 0,
                borderStyle: "solid",
                borderRadius: 8,
              }}
            >
              <span className="text-xs">{item.icon}</span>
              {item.label}
            </div>
          ))}
        </nav>
        <div className="mt-auto px-3 py-4">
          <div className="flex items-center gap-2 px-3 py-2.5 rounded-lg border border-white/5 bg-black/20 backdrop-blur-sm">
            <Monitor className="h-3.5 w-3.5 text-accent-soft" />
            <div className="flex-1">
              <div className="text-[11px] font-medium text-paper">Windows Desktop</div>
              <div className="text-[9.5px] text-muted">v0.1.0 · Stremio ready</div>
            </div>
          </div>
        </div>
      </div>

      {/* Main content */}
      <div className="absolute inset-y-0 left-60 right-0 bg-[#07070b]/20 relative overflow-hidden">
        {/* Hero art */}
        <div className="absolute inset-0 bg-gradient-to-br from-indigo-600/15 via-violet-700/5 to-transparent" />
        <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_80%_20%,rgba(124,92,255,0.2),transparent_50%)]" />

        {/* Overlay */}
        <div className="absolute inset-0 flex flex-col justify-between p-10 pointer-events-none">
          <div className="flex justify-between items-start">
            <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 bg-black/30 backdrop-blur-sm">
              <span className="h-1.5 w-1.5 rounded-full bg-accent" />
              <span className="text-[10px] font-medium text-muted">Featured · Series</span>
            </div>
          </div>

          <div className="mt-auto mb-6">
            <h2 className="text-[3rem] font-display font-bold text-paper leading-[1.05]">Aurora Protocol</h2>
            <div className="mt-3 flex items-center gap-2 text-xs text-muted">
              <span>2026 · 9.2</span>
              <span className="h-1 w-1 rounded-full bg-accent" />
              <span>S1 · E4</span>
            </div>
            <div className="mt-6 flex items-center gap-3">
              <button className="flex items-center gap-2 rounded-xl bg-accent px-6 py-3 text-[14px] font-semibold text-[#0a0a11] hover:bg-primary/90 transition-colors">
                <svg className="h-4 w-4 text-[#0a0a11]" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg>
                Play
              </button>
              <button className="flex items-center gap-2 rounded-xl border border-white/10 bg-white/5 px-6 py-3 text-[14px] font-semibold text-muted hover:bg-white/[0.1] transition-colors">
                Sources
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function PlayerShot() {
  return (
    <div className="relative aspect-video w-full">
      <div className="absolute inset-0 bg-[#07070b]" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_50%_50%,rgba(124,92,255,0.25),transparent_70%)]" />
      <div className="absolute inset-x-0 bottom-0 h-1/2 bg-gradient-to-t from-[#07070b] to-transparent" />

      {/* Controls overlay */}
      <div className="absolute inset-0 flex flex-col justify-between p-6 pointer-events-none">
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-lg border border-white/10 bg-black/40 backdrop-blur-md">
            <svg className="h-3.5 w-3.5 text-paper" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path d="M11 2H4v20h16V2h-7"/></svg>
            <span className="text-[11px] font-medium text-paper">Aurora Protocol · S1E4</span>
          </div>
          <div className="flex items-center gap-2">
            <div className="h-8 w-8 rounded-lg border border-white/10 bg-white/5 flex items-center justify-center text-[11px]">1080p</div>
            <div className="h-8 w-8 rounded-lg border border-white/10 bg-white/5 flex items-center justify-center text-[11px]">D3D11</div>
          </div>
        </div>

        <div className="mt-auto mb-8 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <button className="h-14 w-14 rounded-full border border-white/20 bg-white/10 flex items-center justify-center text-paper hover:bg-white/20 transition-colors">
              <svg className="h-6 w-6" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" viewBox="0 0 24 24"><polygon points="5 3 19 12 5 21 5 3"/></svg>
            </button>
            <div className="flex items-center gap-2">
              <div className="h-32 w-1 rounded-full bg-white/10 overflow-hidden">
                <div className="h-full w-full bg-gradient-to-r from-accent to-accent-soft" />
              </div>
              <div className="text-[10px] text-muted">24:16 / 47:32</div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button className="h-10 w-10 rounded-full border border-white/10 bg-white/5 flex items-center justify-center text-paper hover:bg-white/10 transition-colors">
              <svg className="h-4 w-4" fill="none" stroke="currentColor" strokeWidth="2" viewBox="0 0 24 24"><path d="M12 5v14l11-7z"/></svg>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

function LibraryShot() {
  return (
    <div className="relative aspect-video w-full">
      <div className="absolute inset-0 bg-[#07070b]" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_20%_30%,rgba(61,220,151,0.2),transparent_60%)]" />

      <div className="absolute inset-0 p-8 pointer-events-none">
        <div className="max-w-2xl mx-auto">
          <h2 className="text-[1.5rem] font-display font-bold text-paper mb-6">Continue Watching</h2>
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {[1, 2, 3, 4].map((i) => (
              <div key={i} className="aspect-[2/3] rounded-xl border border-white/5 bg-white/[0.02] overflow-hidden relative group">
                <div className="absolute inset-0 bg-gradient-to-b from-transparent to-black/60" />
                <div className="absolute inset-0 bg-gradient-to-tr from-indigo-500/20 via-transparent to-transparent" />
                <div className="absolute bottom-0 left-0 right-0 p-4">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="h-1 w-1 rounded-full bg-signal" />
                    <span className="text-[10px] font-medium text-paper">S1 · E{i}</span>
                  </div>
                  <h3 className="text-[13px] font-semibold text-paper line-clamp-1">Aurora Protocol Episode {i}</h3>
                  <div className="mt-1 h-[3px] w-full rounded-full bg-white/10 overflow-hidden">
                    <div className="h-full bg-gradient-to-r from-accent to-accent-soft" style={{ width: `${60 + i * 10}%` }} />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

const shots = [
  { label: "Desktop home", node: <DesktopHomeShot /> },
  { label: "libmpv player", node: <PlayerShot /> },
  { label: "Library & resume", node: <LibraryShot /> },
];

const STEP = 420 + 24;

export function Showcase() {
  return (
    <section id="showcase" className="border-t border-rule bg-void-deep/30">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <Stagger
          items={[
            <p key="rubric" className="eyebrow text-accent-soft">
              02 — Showcase
            </p>,
            <h2
              key="h2"
              className="display mt-4 max-w-3xl font-display font-bold text-paper"
              style={{ fontSize: "clamp(2rem, 4vw, 3.5rem)" }}
            >
              The product, <span className="text-muted">up close.</span>
            </h2>,
            <p key="intro" className="mt-6 max-w-xl text-[16px] leading-relaxed text-muted md:text-[18px]">
              Real surfaces from the desktop app — home feed, libmpv player and
              library resume. Native window chrome, not a browser tab.
            </p>,
          ]}
        />

        <div className="mt-14">
          <DragRail step={STEP} label="app surfaces">
            {shots.map((s) => (
              <Frame key={s.label} label={s.label}>
                {s.node}
              </Frame>
            ))}
          </DragRail>
        </div>
      </div>
    </section>
  );
}
