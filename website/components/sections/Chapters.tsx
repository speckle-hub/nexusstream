import { Play, Volume2 } from "lucide-react";
import { Stagger } from "../ui/reveal";
import { SectionHead } from "./SectionHead";

/* ── Plates ──────────────────────────────────────────────────
   Every mock is a dark plate: the product is the only thing on
   the page allowed to carry colour.                        */

function Plate({
  fig,
  caption,
  children,
}: {
  fig: string;
  caption: string;
  children: React.ReactNode;
}) {
  return (
    <figure className="w-full">
      <div className="overflow-hidden rounded-[4px] border border-ink/10 bg-ink shadow-lift">
        {children}
      </div>
      <figcaption className="mt-3 flex items-baseline gap-3 border-t border-rule pt-2.5">
        <span className="eyebrow shrink-0 text-[9px] text-vermilion">{fig}</span>
        <span className="text-[12.5px] leading-relaxed text-ash">{caption}</span>
      </figcaption>
    </figure>
  );
}

function PlayerPlate() {
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
        <span className="eyebrow tabular text-[9px] text-white/70">75</span>
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

function ReaderPlate() {
  return (
    <div className="flex aspect-[16/10] w-full items-stretch gap-[3px] p-4">
      {[0, 1, 2].map((i) => (
        <div
          key={i}
          className="relative flex-1 overflow-hidden rounded-[2px] bg-gradient-to-b from-zinc-700/60 via-zinc-800/50 to-zinc-900/70"
        >
          <div className="absolute inset-2.5 space-y-1.5">
            {[0, 1, 2, 3, 4].map((r) => (
              <div
                key={r}
                className="h-[5px] rounded-sm bg-white/12"
                style={{ width: `${86 - r * 12 - i * 7}%` }}
              />
            ))}
          </div>
          {i === 1 && (
            <span className="eyebrow absolute bottom-2 right-2 rounded-full bg-accent px-2 py-[3px] text-[7px] text-white">
              RTL
            </span>
          )}
        </div>
      ))}
    </div>
  );
}

function DownloadsPlate() {
  const rows = [
    { name: "Aurora Protocol · E4", pct: "100%", w: "w-full", tone: "bg-signal" },
    { name: "Tide Signal · E2", pct: "62%", w: "w-[62%]", tone: "bg-accent" },
    { name: "Ash Runner · E7", pct: "28%", w: "w-[28%]", tone: "bg-accent/70" },
  ];
  return (
    <div className="aspect-[16/10] w-full space-y-5 p-6">
      {rows.map((r) => (
        <div key={r.name}>
          <div className="mb-2 flex justify-between">
            <span className="text-[12px] text-white/80">{r.name}</span>
            <span className="eyebrow tabular text-[10px] text-white/45">
              {r.pct}
            </span>
          </div>
          <div className="h-[3px] overflow-hidden rounded-full bg-white/12">
            <div className={`h-full rounded-full ${r.tone} ${r.w}`} />
          </div>
        </div>
      ))}
    </div>
  );
}

function SourcesPlate() {
  const rows = [
    { name: "Cinemeta", on: true },
    { name: "OpenSubtitles", on: true },
    { name: "My own repo", on: true },
    { name: "Unvetted add-on", on: false },
  ];
  return (
    <div className="aspect-[16/10] w-full space-y-2.5 p-6">
      {rows.map((r) => (
        <div
          key={r.name}
          className="flex items-center justify-between rounded-[3px] border border-white/[0.07] bg-white/[0.04] px-4 py-3"
        >
          <span className="text-[12.5px] text-white/85">{r.name}</span>
          <span
            className={`flex h-[18px] w-[34px] items-center rounded-full px-[2px] ${
              r.on ? "justify-end bg-accent" : "justify-start bg-white/15"
            }`}
          >
            <span className="h-[14px] w-[14px] rounded-full bg-white" />
          </span>
        </div>
      ))}
    </div>
  );
}

function ThemingPlate() {
  const swatches = ["#7C5CFF", "#3DDC97", "#F97316", "#3B82F6", "#EC4899"];
  return (
    <div className="aspect-[16/10] w-full p-6">
      <div className="flex gap-3">
        {swatches.map((c, i) => (
          <span
            key={c}
            className={`h-9 w-9 rounded-full ${
              i === 0
                ? "ring-2 ring-white/70 ring-offset-2 ring-offset-ink"
                : ""
            }`}
            style={{ background: c }}
          />
        ))}
      </div>
      <div className="mt-5 rounded-[3px] bg-gradient-to-r from-accent/40 via-accent/15 to-transparent p-4">
        <div className="h-2 w-3/4 rounded-full bg-white/30" />
        <div className="mt-2 h-2 w-1/2 rounded-full bg-white/15" />
        <span className="eyebrow mt-4 inline-block rounded-full bg-accent px-3 py-1 text-[8px] text-white">
          Material You
        </span>
      </div>
    </div>
  );
}

function PrivacyPlate() {
  return (
    <div className="flex aspect-[16/10] w-full items-center justify-center gap-10">
      <div className="flex h-20 w-20 items-center justify-center rounded-[4px] border border-white/10 bg-white/[0.05]">
        <svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="#BFAEFF" strokeWidth="1.6">
          <rect x="4" y="10.5" width="16" height="10.5" rx="2" />
          <path d="M8 10.5V7a4 4 0 0 1 8 0v3.5" />
          <circle cx="12" cy="15.6" r="1.4" fill="#BFAEFF" stroke="none" />
        </svg>
      </div>
      <div className="space-y-3">
        <div className="flex gap-2.5">
          {[0, 1, 2, 3].map((i) => (
            <span
              key={i}
              className="h-3 w-3 rounded-full bg-accent"
              style={{ opacity: 1 - i * 0.18 }}
            />
          ))}
        </div>
        <div className="h-[5px] w-32 rounded-full bg-white/12" />
        <div className="h-[5px] w-20 rounded-full bg-white/[0.07]" />
      </div>
    </div>
  );
}

const chapters = [
  {
    n: "01",
    title: "A player that fights above its weight",
    body: "Gesture brightness, volume and seek with haptic HUDs. Hold for 2X speed, subtitle styling and sync offset, stats for nerds, PiP, mirror failover and hand-off to any external player.",
    fig: "Fig. 02",
    caption: "Playback surface. Every control is reachable by gesture alone.",
    plate: <PlayerPlate />,
  },
  {
    n: "02",
    title: "A real manga reader",
    body: "Webtoon, RTL, LTR and landscape spreads with zoom lock, night filters, volume-key turns and per-chapter resume. It reads like a reader, not a web page in a frame.",
    fig: "Fig. 03",
    caption: "Three-up spread with right-to-left paging enabled.",
    plate: <ReaderPlate />,
  },
  {
    n: "03",
    title: "Offline, properly",
    body: "Media3-powered downloads including encrypted HLS playlists, plus a manga chapter downloader — retries, mirror failover, pause and resume, progress notifications and a storage meter.",
    fig: "Fig. 04",
    caption: "Download queue. Completed work stays visible, not hidden.",
    plate: <DownloadsPlate />,
  },
  {
    n: "04",
    title: "Your sources, your call",
    body: "Ships source-neutral. Paste a Stremio manifest URL or a provider repository, browse what it offers, and install only what you want. Nothing is enabled without your say-so.",
    fig: "Fig. 05",
    caption: "Add-on manager. Opt-in is the default state, not a setting.",
    plate: <SourcesPlate />,
  },
  {
    n: "05",
    title: "Theming that follows you",
    body: "Material You dynamic colour, accent presets, true AMOLED black and poster-palette tinting that reads the artwork on screen and pulls the interface toward it.",
    fig: "Fig. 06",
    caption: "Accent presets. The active swatch drives every surface.",
    plate: <ThemingPlate />,
  },
  {
    n: "06",
    title: "Private by default",
    body: "Biometric app lock, a separately gated NSFW area with its own PIN, incognito mode that stops watch history, FLAG_SECURE screen protection — and JSON backup and restore of everything that matters.",
    fig: "Fig. 07",
    caption: "App lock. The gated area is a separate surface, not a filter.",
    plate: <PrivacyPlate />,
  },
];

export function Chapters() {
  return (
    <section id="features" className="border-t border-rule">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <SectionHead
          index="03"
          rubric="Features"
          title={
            <>
              Six chapters,{" "}
              <span className="font-serif italic">read front to back.</span>
            </>
          }
          intro="No grid of equal boxes. Each capability gets a full spread, in the
            order you meet it."
        />

        <div className="mt-4">
          {chapters.map((c, i) => (
            <article
              key={c.n}
              className="grid gap-8 border-b border-rule py-12 md:grid-cols-[92px_1fr] md:gap-10 lg:grid-cols-[92px_1fr_1.05fr] lg:gap-12 lg:py-16"
            >
              <div className="md:pt-1">
                <span
                  className="block font-serif italic leading-none text-vermilion"
                  style={{ fontSize: "clamp(2.25rem, 4vw, 3.25rem)" }}
                >
                  {c.n}
                </span>
              </div>

              <Stagger
                className={i % 2 === 1 ? "lg:order-3" : undefined}
                items={[
                  <h3
                    key="h3"
                    className="display font-sans font-bold text-ink"
                    style={{ fontSize: "clamp(1.5rem, 2.4vw, 2.1rem)" }}
                  >
                    {c.title}
                  </h3>,
                  <p key="p" className="measure mt-5 text-[16px] text-graphite">
                    {c.body}
                  </p>,
                ]}
              />

              <div className={i % 2 === 1 ? "lg:order-2" : undefined}>
                <Plate fig={c.fig} caption={c.caption}>
                  {c.plate}
                </Plate>
              </div>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
