"use client";

import {
  Play,
  BookOpen,
  Download,
  Shield,
  Palette,
  Lock,
  Monitor,
} from "lucide-react";
import { Stagger } from "../ui/reveal";
import { Tilt } from "../ui/tilt";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";

const cards = [
  {
    icon: Monitor,
    title: "Native Windows client",
    tag: "Desktop",
    body: "Built with Kotlin + Compose Multiplatform — not Electron, not a stretched phone UI. Wide grids, large hero art, hover states, and keyboard-first navigation on a real desktop window.",
  },
  {
    icon: Play,
    title: "libmpv playback",
    tag: "Player",
    body: "Hardware-accelerated video (D3D11 auto-safe), fullscreen, Space/arrows/F/Esc shortcuts, quality/audio/subtitle tracks, resume position, and software-rendered frames so Compose controls draw on top.",
  },
  {
    icon: Shield,
    title: "PIN-gated NSFW area",
    tag: "Privacy",
    body: "Fully separate from home, search and library. PBKDF2-HMAC-SHA256 PIN (4–8 digits), session-only unlock, and adult add-ons that never leak into normal feeds.",
  },
  {
    icon: Download,
    title: "Stremio add-on client",
    tag: "Extensions",
    body: "Install any Stremio add-on by manifest URL. Pre-seeded defaults (Torrentio, OpenSubtitles v3, Kitsu). One failing add-on never breaks the app — per-add-on timeouts and error isolation.",
  },
  {
    icon: BookOpen,
    title: "Local library & resume",
    tag: "Library",
    body: "Favorites and Continue Watching persist as atomic JSON under %APPDATA%\\Moovio. Remove with confirmation, auto-resume at your saved position, and 5-second progress persistence during playback.",
  },
  {
    icon: Palette,
    title: "Cinematic dark theme",
    tag: "Design",
    body: "Deep void surfaces, violet accent, glassmorphism, gradient glows, and smooth enter/exit animations on navigation, cards and the hero carousel.",
  },
];

export function Features() {
  return (
    <section id="features" className="relative border-t border-rule">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <Stagger
          items={[
            <p key="rubric" className="eyebrow text-accent-soft">
              01 — Features
            </p>,
            <h2
              key="h2"
              className="display mt-4 max-w-3xl font-display font-bold text-paper"
              style={{ fontSize: "clamp(2rem, 4vw, 3.5rem)" }}
            >
              Everything a flagship does.{" "}
              <span className="text-muted">Nothing it charges for.</span>
            </h2>,
            <p key="intro" className="mt-6 max-w-xl text-[16px] leading-relaxed text-muted md:text-[18px]">
              Desktop-native playback, a PIN-gated private area, Stremio
              add-ons, and a library that actually persists. Finished surfaces
              — no placeholders.
            </p>,
          ]}
        />

        <div className="mt-14 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {cards.map((c) => (
            <Tilt key={c.title} max={6} allowScroll cardClassName="rounded-2xl">
              <Card variant="elevated" className="group hover:border-accent/30 transition-colors duration-300">
                <CardHeader className="pb-3">
                  <div className="flex items-center justify-between">
                    <div className="flex h-10 w-10 items-center justify-center rounded-xl border border-rule bg-white/[0.04] text-accent-soft">
                      <c.icon className="h-5 w-5" />
                    </div>
                    <span className="eyebrow text-[9px] text-faint">{c.tag}</span>
                  </div>
                  <h3 className="mt-5 font-display text-[18px] font-semibold leading-snug tracking-tight text-paper">
                    {c.title}
                  </h3>
                </CardHeader>
                <CardContent>
                  <p className="text-[14.5px] leading-relaxed text-muted">
                    {c.body}
                  </p>
                </CardContent>
              </Card>
            </Tilt>
          ))}
        </div>
      </div>
    </section>
  );
}
