"use client";

import { m } from "framer-motion";
import { Download, Github } from "lucide-react";
import { APK_URL, GITHUB_URL } from "@/lib/utils";
import { Spotlight } from "../ui/spotlight";
import { Sparkles } from "../ui/sparkles";
import { Nebula } from "../ui/nebula";
import { MovingBorderButton } from "../ui/moving-border";
import { PhoneMockup } from "../PhoneMockup";
import { Stagger } from "../ui/reveal";
import { Tilt } from "../ui/tilt";

/** Critically damped house spring (apple-design): graceful, no overshoot. */
const SPRING = { type: "spring", bounce: 0, duration: 0.4 } as const;

const heroStats = ["4 add-on ecosystems", "209 unit tests", "Android 7.0+", "100% free"];

export function Hero() {
  return (
    <section id="top" className="relative overflow-hidden pb-16 pt-36 md:pb-24 md:pt-44">
      {/* layered background */}
      <Nebula />
      <div className="absolute inset-0 bg-grid-white [mask-image:radial-gradient(ellipse_75%_65%_at_50%_35%,black,transparent)]" />
      <Spotlight className="-top-40 left-0 md:-top-20 md:left-56" fill="#8F74FF" />
      <Sparkles density={70} />
      <div className="absolute inset-x-0 bottom-0 h-48 bg-gradient-to-t from-void to-transparent" />

      <div className="relative z-10 mx-auto grid max-w-7xl items-center gap-14 px-6 lg:grid-cols-[1.15fr_0.85fr] lg:gap-6">
        <Stagger
          className="text-center lg:text-left"
          items={[
            <span
              key="badge"
              className="mb-6 inline-flex items-center gap-2 rounded-full border border-accent/40 bg-accent/10 px-3 py-1 text-sm text-accent-soft"
            >
              <span className="h-1.5 w-1.5 rounded-full bg-signal" />
              <span className="t-shimmer" data-text="v1.7.0 · Free & open source">
                v1.7.0 · Free & open source
              </span>
            </span>,
            <span key="h1" className="display-tight font-display text-5xl font-bold text-mist md:text-7xl">
              Every stream.
              <br />
              <span className="text-gradient">One app.</span>
            </span>,
            <span key="sub" className="mx-auto mt-6 block max-w-xl text-base leading-relaxed text-muted md:text-lg lg:mx-0">
              NexusStream unifies Stremio add-ons, CloudStream extensions, Aniyomi and Mihon repos
              into one beautiful Android app — movies, TV, anime and manga, with a player and
              reader that feel purpose-built.
            </span>,
            <span key="ctas" className="mt-9 flex flex-col items-center gap-4 sm:flex-row lg:justify-start sm:justify-center">
              <MovingBorderButton href={APK_URL}>
                <Download className="h-4 w-4" />
                Download APK
              </MovingBorderButton>
              <a
                href={GITHUB_URL}
                target="_blank"
                rel="noreferrer"
                className="flex items-center gap-2 rounded-full border border-white/15 px-8 py-3.5 font-semibold text-mist transition hover:border-white/35 hover:bg-white/5"
                style={{ transitionDuration: "var(--duration-fast)", transitionTimingFunction: "var(--ease-smooth-out)" }}
              >
                <Github className="h-4 w-4" />
                View on GitHub
              </a>
            </span>,
            <span key="stats" className="mt-9 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 lg:justify-start">
              {heroStats.map((s) => (
                <span key={s} className="flex items-center gap-2 text-xs text-muted">
                  <span className="h-1 w-1 rounded-full bg-signal" />
                  {s}
                </span>
              ))}
            </span>,
          ]}
        />

        <m.div
          initial={{ opacity: 0, y: 40, rotate: 4 }}
          animate={{ opacity: 1, y: 0, rotate: 0 }}
          transition={{ ...SPRING, delay: 0.35 }}
          className="perspective-1200 relative"
        >
          <div className="absolute left-1/2 top-1/2 h-[420px] w-[420px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent/20 blur-[110px]" />
          <div className="animate-float-slow [transform:rotateX(4deg)_rotateY(-8deg)]" data-t-generate="skeleton">
            <Tilt max={10} allowScroll>
              <PhoneMockup className="relative" />
            </Tilt>
          </div>
        </m.div>
      </div>
    </section>
  );
}
