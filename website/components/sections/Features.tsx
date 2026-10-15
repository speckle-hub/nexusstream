"use client";

import {
  Clapperboard,
  BookOpenText,
  DownloadCloud,
  Blocks,
  Palette,
  LockKeyhole,
} from "lucide-react";
import { Stagger } from "../ui/reveal";
import { Tilt } from "../ui/tilt";

const cards = [
  {
    icon: Clapperboard,
    title: "A player that fights above its weight",
    body: "Gesture brightness, volume and seek with haptic HUDs. Hold for 2X speed, subtitle styling with sync offset, stats for nerds, PiP, mirror failover and hand-off to VLC or MPV.",
    tag: "Playback",
  },
  {
    icon: BookOpenText,
    title: "A real manga reader",
    body: "Webtoon, RTL, LTR and landscape spreads with zoom lock, night filters, volume-key turns and per-chapter resume. It reads like a reader, not a web page in a frame.",
    tag: "Reading",
  },
  {
    icon: DownloadCloud,
    title: "Offline, properly",
    body: "Media3-powered video downloads including encrypted HLS playlists, plus a page-by-page manga downloader — retries, mirror failover, pause and resume, and a live storage meter.",
    tag: "Offline",
  },
  {
    icon: Blocks,
    title: "Your sources, your call",
    body: "Ships source-neutral. Paste a Stremio manifest URL or a provider repository, browse what it offers, and install only what you want. Nothing is enabled without your say-so.",
    tag: "Extensions",
  },
  {
    icon: Palette,
    title: "Theming that follows you",
    body: "Material You dynamic colour, accent presets, true AMOLED black, and poster-palette tinting that reads the artwork on screen and pulls the interface toward it.",
    tag: "Design",
  },
  {
    icon: LockKeyhole,
    title: "Private by default",
    body: "Biometric app lock, a separately gated area with its own PIN, incognito mode that stops watch history, FLAG_SECURE screen protection — plus JSON backup and restore.",
    tag: "Privacy",
  },
];

export function Features() {
  return (
    <section id="features" className="relative">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <Stagger
          items={[
            <p key="rubric" className="eyebrow text-accent-soft">
              01 — Features
            </p>,
            <h2
              key="h2"
              className="display mt-4 max-w-3xl font-sans font-bold text-paper"
              style={{ fontSize: "clamp(1.9rem, 3.6vw, 3.25rem)" }}
            >
              Everything a flagship does.{" "}
              <span className="text-muted">Nothing it charges for.</span>
            </h2>,
            <p key="intro" className="mt-5 max-w-xl text-[16px] leading-relaxed text-muted md:text-[17px]">
              Six capabilities, each one finished — player, reader, offline,
              extensions, theming and privacy. No placeholders, no wrappers.
            </p>,
          ]}
        />

        <div className="mt-12 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {cards.map((c) => (
            <Tilt key={c.title} max={6} allowScroll cardClassName="rounded-2xl">
              <article className="group flex h-full flex-col rounded-2xl border border-rule bg-white/[0.025] p-7 transition-colors duration-300 hover:border-white/20 hover:bg-white/[0.05]">
                <div className="flex items-center justify-between">
                  <span className="flex h-11 w-11 items-center justify-center rounded-xl border border-rule bg-white/[0.04] text-accent-soft">
                    <c.icon className="h-5 w-5" />
                  </span>
                  <span className="eyebrow text-[9px] text-faint">{c.tag}</span>
                </div>
                <h3 className="mt-6 font-display text-[19px] font-semibold leading-snug tracking-tight text-paper">
                  {c.title}
                </h3>
                <p className="mt-3 flex-1 text-[14.5px] leading-relaxed text-muted">
                  {c.body}
                </p>
              </article>
            </Tilt>
          ))}
        </div>
      </div>
    </section>
  );
}
