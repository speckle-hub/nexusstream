"use client";

import { Play, Volume2 } from "lucide-react";
import { DragRail } from "../ui/drag-rail";
import { Tilt } from "../ui/tilt";
import { Stagger } from "../ui/reveal";

function Frame({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <figure style={{ width: 320 }} className="shrink-0 select-none">
      <Tilt max={7} allowScroll cardClassName="rounded-2xl">
        <div className="overflow-hidden rounded-2xl border border-rule bg-void-raised shadow-card">
          {children}
        </div>
      </Tilt>
      <figcaption className="eyebrow mt-3 text-center text-[9px] text-faint">{label}</figcaption>
    </figure>
  );
}

function PlayerShot() {
  return (
    <div className="relative aspect-[16/10] w-full">
      <div className="absolute inset-0 bg-[linear-gradient(135deg,#1b1730_0%,#0d0d16_55%,#171026_100%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_30%_25%,rgba(124,92,255,0.35),transparent_60%)]" />
      <div className="absolute left-4 top-4 flex items-center gap-2 rounded-full border border-white/10 bg-black/40 px-3 py-1.5 backdrop-blur-sm">
        <span className="h-1.5 w-1.5 rounded-full bg-signal" />
        <span className="eyebrow text-[9px] text-white/80">1080p · HLS</span>
      </div>
      <div className="absolute right-4 top-1/2 flex -translate-y-1/2 flex-col items-center gap-2">
        <Volume2 className="h-3.5 w-3.5 text-white/70" />
        <div className="h-20 w-[3px] overflow-hidden rounded-full bg-white/15">
          <div className="h-3/4 w-full rounded-full bg-accent" />
        </div>
      </div>
      <div className="absolute left-1/2 top-1/2 flex h-14 w-14 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full border border-white/15 bg-white/10 backdrop-blur-md">
        <Play className="ml-0.5 h-5 w-5 fill-white text-white" />
      </div>
      <div className="absolute inset-x-4 bottom-4">
        <div className="mb-1.5 flex justify-between">
          <span className="eyebrow tabular text-[9px] text-white/60">24:16</span>
          <span className="eyebrow tabular text-[9px] text-white/60">47:32</span>
        </div>
        <div className="h-[3px] overflow-hidden rounded-full bg-white/15">
          <div className="h-full w-1/2 rounded-full bg-gradient-to-r from-accent to-accent-soft" />
        </div>
      </div>
    </div>
  );
}

function ReaderShot() {
  return (
    <div className="flex aspect-[16/10] w-full items-stretch gap-[3px] p-4">
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          className="relative flex-1 overflow-hidden rounded-md bg-gradient-to-b from-zinc-700/60 via-zinc-800/50 to-zinc-900/70"
        >
          <div className="absolute inset-2.5 space-y-1.5">
            {[0, 1, 2, 3, 4].map((r) => (
              <div
                key={r}
                className="h-[5px] rounded-sm bg-white/15"
                style={{ width: `${86 - r * 12 - i * 7}%` }}
              />
            ))}
          </div>
        </div>
      ))}
    </div>
  );
}

function DownloadsShot() {
  const rows = [
    { name: "Aurora Protocol · E4", pct: "100%", w: "w-full" },
    { name: "Tide Signal · E2", pct: "62%", w: "w-[62%]" },
    { name: "Ash Runner · E7", pct: "28%", w: "w-[28%]" },
  ];
  return (
    <div className="aspect-[16/10] w-full space-y-5 p-6">
      {rows.map((r) => (
        <div key={r.name}>
          <div className="mb-2 flex justify-between">
            <span className="text-[12px] text-white/80">{r.name}</span>
            <span className="eyebrow tabular text-[10px] text-white/45">{r.pct}</span>
          </div>
          <div className="h-[3px] overflow-hidden rounded-full bg-white/15">
            <div className={`h-full rounded-full bg-gradient-to-r from-accent to-signal ${r.w}`} />
          </div>
        </div>
      ))}
    </div>
  );
}

const shots = [
  { label: "Playback surface", node: <PlayerShot /> },
  { label: "Manga reader", node: <ReaderShot /> },
  { label: "Download queue", node: <DownloadsShot /> },
];

const STEP = 320 + 24;

export function Showcase() {
  return (
    <section id="showcase" className="border-t border-rule bg-void-deep/40">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <Stagger
          items={[
            <p key="rubric" className="eyebrow text-accent-soft">
              02 — Showcase
            </p>,
            <h2
              key="h2"
              className="display mt-4 max-w-3xl font-sans font-bold text-paper"
              style={{ fontSize: "clamp(1.9rem, 3.6vw, 3.25rem)" }}
            >
              The product, <span className="text-muted">up close.</span>
            </h2>,
            <p key="intro" className="mt-5 max-w-xl text-[16px] leading-relaxed text-muted md:text-[17px]">
              Real surfaces from the app — playback, reading and downloads.
              Grab the rail and throw it.
            </p>,
          ]}
        />

        <div className="mt-12">
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
