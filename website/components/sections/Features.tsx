import { Gauge, Palette, Play, ShieldCheck, Volume2 } from "lucide-react";
import { BentoGrid, BentoGridItem } from "../ui/bento-grid";
import { GlowingCard } from "../ui/glowing-card";
import { TextGenerateEffect } from "../ui/text-generate-effect";

function PlayerVisual() {
  return (
    <div className="relative mx-4 mt-4 h-44 overflow-hidden rounded-xl border border-white/10 bg-gradient-to-br from-[#181828] via-[#101018] to-[#1c1030] md:mx-5 md:mt-5 md:h-52">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_30%_20%,rgba(124,92,255,0.25),transparent_60%)]" />
      <div className="absolute left-3 top-3 rounded-md bg-black/50 px-2 py-1 text-[9px] font-bold tracking-wide text-white/85 backdrop-blur-sm">1080p · HLS</div>
      <div className="absolute right-3 top-1/2 flex -translate-y-1/2 flex-col items-center gap-1.5">
        <Volume2 className="h-3 w-3 text-white/70" />
        <div className="h-16 w-1 overflow-hidden rounded-full bg-white/15">
          <div className="h-3/4 w-full rounded-full bg-accent" />
        </div>
        <span className="text-[8px] font-semibold text-white/70">75%</span>
      </div>
      <div className="absolute left-1/2 top-1/2 flex h-11 w-11 -translate-x-1/2 -translate-y-1/2 items-center justify-center rounded-full bg-white/10 backdrop-blur-md">
        <Play className="ml-0.5 h-4 w-4 fill-white text-white" />
      </div>
      <div className="absolute inset-x-3 bottom-3">
        <div className="mb-1 flex justify-between text-[8px] font-medium text-white/60"><span>24:16</span><span>47:32</span></div>
        <div className="h-1 overflow-hidden rounded-full bg-white/15">
          <div className="h-full w-1/2 rounded-full bg-gradient-to-r from-accent to-accent-soft" />
        </div>
      </div>
    </div>
  );
}

function ReaderVisual() {
  return (
    <div className="mx-4 mt-4 flex h-44 items-stretch justify-center gap-2 overflow-hidden rounded-xl border border-white/10 bg-[#0d0d14] p-3 md:mx-5 md:mt-5 md:h-52">
      {[0, 1, 2].map((i) => (
        <div key={i} className="relative flex-1 overflow-hidden rounded-md border border-white/10 bg-gradient-to-b from-zinc-700/50 via-zinc-800/40 to-zinc-900/60">
          <div className="absolute inset-2 space-y-1.5">
            {[...Array(5)].map((_, r) => (
              <div key={r} className="h-2 rounded-sm bg-white/10" style={{ width: `${88 - r * 13 - i * 6}%` }} />
            ))}
          </div>
          {i === 1 && <div className="absolute bottom-1.5 right-1.5 rounded-full bg-accent px-1.5 py-0.5 text-[7px] font-bold text-white">RTL</div>}
        </div>
      ))}
    </div>
  );
}

function DownloadsVisual() {
  const rows = [
    { name: "Aurora Protocol · E4", pct: "w-full", label: "Done", tone: "bg-emerald-400" },
    { name: "Tide Signal · E2", pct: "w-[62%]", label: "62%", tone: "bg-accent" },
    { name: "Ash Runner · E7", pct: "w-[28%]", label: "28%", tone: "bg-accent" },
  ];
  return (
    <div className="mx-4 mt-4 space-y-2.5 rounded-xl border border-white/10 bg-[#0d0d14] p-4 md:mx-5 md:mt-5">
      {rows.map((r) => (
        <div key={r.name}>
          <div className="mb-1 flex justify-between text-[9px]"><span className="text-white/75">{r.name}</span><span className="text-muted">{r.label}</span></div>
          <div className="h-1.5 overflow-hidden rounded-full bg-white/10"><div className={`h-full rounded-full ${r.tone} ${r.pct}`} /></div>
        </div>
      ))}
    </div>
  );
}

function SourcesVisual() {
  const chips = [
    { name: "Cinemeta", on: true }, { name: "OpenSubtitles", on: true },
    { name: "My Repo", on: true }, { name: "Custom add-on", on: false },
  ];
  return (
    <div className="mx-4 mt-4 space-y-2 rounded-xl border border-white/10 bg-[#0d0d14] p-4 md:mx-5 md:mt-5">
      {chips.map((c) => (
        <div key={c.name} className="flex items-center justify-between rounded-lg bg-white/[0.04] px-3 py-2">
          <span className="text-[10px] font-medium text-white/80">{c.name}</span>
          <div className={`flex h-3.5 w-7 items-center rounded-full px-0.5 ${c.on ? "justify-end bg-accent" : "justify-start bg-white/15"}`}>
            <div className="h-2.5 w-2.5 rounded-full bg-white" />
          </div>
        </div>
      ))}
    </div>
  );
}

function ThemingVisual() {
  return (
    <div className="mx-4 mt-4 rounded-xl border border-white/10 bg-[#0d0d14] p-4 md:mx-5 md:mt-5">
      <div className="mb-3 flex gap-2">
        {["#7C5CFF", "#10B981", "#EF4444", "#F97316", "#3B82F6"].map((c, i) => (
          <div key={c} className={`h-7 w-7 rounded-full ${i === 0 ? "ring-2 ring-white/70 ring-offset-2 ring-offset-[#0d0d14]" : ""}`} style={{ background: c }} />
        ))}
      </div>
      <div className="rounded-lg bg-gradient-to-r from-accent/40 via-accent/15 to-transparent p-3">
        <div className="h-1.5 w-3/4 rounded-full bg-white/25" />
        <div className="mt-1.5 h-1.5 w-1/2 rounded-full bg-white/15" />
        <div className="mt-3 inline-flex rounded-full bg-accent px-3 py-1 text-[8px] font-bold text-white">Material You</div>
      </div>
    </div>
  );
}

function PrivacyVisual() {
  return (
    <div className="mx-4 mt-4 flex items-center justify-center gap-4 rounded-xl border border-white/10 bg-[#0d0d14] p-4 md:mx-5 md:mt-5">
      <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-accent/15">
        <ShieldCheck className="h-7 w-7 text-accent-soft" />
      </div>
      <div className="space-y-2">
        <div className="flex gap-1.5">{[...Array(4)].map((_, i) => <div key={i} className="h-2.5 w-2.5 rounded-full bg-accent" />)}</div>
        <div className="h-1.5 w-24 rounded-full bg-white/10" />
        <div className="h-1.5 w-16 rounded-full bg-white/5" />
      </div>
    </div>
  );
}

const features = [
  {
    className: "md:col-span-4",
    title: "A player that fights above its weight",
    description: "Gesture brightness, volume and seek with haptic HUDs. Subtitle styling and sync offset, speed control, skip intro/outro, stats for nerds, PiP, mirror failover and external-player hand-off.",
    header: <PlayerVisual />,
  },
  {
    className: "md:col-span-2",
    title: "A real manga reader",
    description: "Webtoon, RTL, LTR and landscape spreads with zoom lock, night filters, volume-key turns and per-chapter resume.",
    header: <ReaderVisual />,
  },
  {
    className: "md:col-span-2",
    title: "Offline, properly",
    description: "Media3-powered video downloads (HLS included) plus a manga chapter downloader — with retries, mirror failover and progress notifications.",
    header: <DownloadsVisual />,
  },
  {
    className: "md:col-span-2",
    title: "Your sources, your call",
    description: "Ships source-neutral: add your own Stremio add-ons and CloudStream / Aniyomi / Mihon repositories in the manager.",
    header: <SourcesVisual />,
  },
  {
    className: "md:col-span-2",
    title: "Theming that follows you",
    description: "Material You dynamic color, accent presets, true AMOLED black and poster-palette tinting across every screen.",
    header: <ThemingVisual />,
  },
  {
    className: "md:col-span-6",
    title: "Private by default",
    description: "Biometric app lock, a separately gated NSFW area with PIN, incognito mode that stops watch history, FLAG_SECURE screen protection — and JSON backup/restore of everything that matters.",
    header: <PrivacyVisual />,
  },
];

export function Features() {
  return (
    <section id="features" className="relative mx-auto max-w-7xl px-6 py-24 md:py-32">
      <div className="mb-14 text-center">
        <p className="mb-3 flex items-center justify-center gap-2 text-xs font-semibold uppercase tracking-[0.28em] text-accent-soft">
          <Gauge className="h-4 w-4" /> Features
        </p>
        <h2 className="font-display text-3xl font-bold tracking-tight text-mist md:text-5xl">
          Built like a flagship, <span className="text-gradient">not a wrapper</span>
        </h2>
        <TextGenerateEffect
          className="mx-auto mt-4 max-w-2xl text-sm text-muted md:text-base"
          words="Every screen, sheet and gesture was designed and re-designed over twenty-one documented phases — the polish shows."
        />
      </div>

      <BentoGrid>
        {features.map((f) => (
          <GlowingCard key={f.title} className={f.className}>
            <BentoGridItem className="h-full border-0 bg-transparent" title={f.title} description={f.description} header={f.header} />
          </GlowingCard>
        ))}
      </BentoGrid>
    </section>
  );
}
