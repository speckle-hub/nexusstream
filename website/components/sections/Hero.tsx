"use client";

import { m } from "framer-motion";
import { Download, Github, Sparkle } from "lucide-react";
import { Chip } from "@heroui/react";
import { APK_URL, GITHUB_URL } from "@/lib/utils";
import { Spotlight } from "../ui/spotlight";
import { Sparkles } from "../ui/sparkles";
import { Nebula } from "../ui/nebula";
import { MovingBorderButton } from "../ui/moving-border";
import { TextGenerateEffect } from "../ui/text-generate-effect";
import { PhoneMockup } from "../PhoneMockup";

const heroStats = ["4 add-on ecosystems", "113 unit tests", "Android 7.0+", "100% free"];

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
        <div className="text-center lg:text-left">
          <m.div
            initial={{ opacity: 0, y: 14 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.55 }}
            className="mb-6 inline-flex"
          >
            <Chip
              variant="bordered"
              className="border-accent/40 bg-accent/10 px-3 py-1 text-accent-soft"
              startContent={<Sparkle className="h-3.5 w-3.5" />}
            >
              v1.1.0 · Free & open source
            </Chip>
          </m.div>

          <m.h1
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.65, delay: 0.08 }}
            className="font-display text-5xl font-bold leading-[1.04] tracking-tight text-mist md:text-7xl"
          >
            Every stream.
            <br />
            <span className="text-gradient">One app.</span>
          </m.h1>

          <TextGenerateEffect
            className="mx-auto mt-6 max-w-xl text-base text-muted md:text-lg lg:mx-0"
            delay={0.35}
            words="NexusStream unifies Stremio add-ons, CloudStream extensions, Aniyomi and Mihon repos into one beautiful Android app — movies, TV, anime and manga, with a player and reader that feel purpose-built."
          />

          <m.div
            initial={{ opacity: 0, y: 18 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6, delay: 0.55 }}
            className="mt-9 flex flex-col items-center gap-4 sm:flex-row lg:justify-start sm:justify-center"
          >
            <MovingBorderButton href={APK_URL}>
              <Download className="h-4 w-4" />
              Download APK
            </MovingBorderButton>
            <a
              href={GITHUB_URL}
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-2 rounded-full border border-white/15 px-8 py-3.5 font-semibold text-mist transition hover:border-white/35 hover:bg-white/5"
            >
              <Github className="h-4 w-4" />
              View on GitHub
            </a>
          </m.div>

          <m.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ duration: 0.8, delay: 0.8 }}
            className="mt-9 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 lg:justify-start"
          >
            {heroStats.map((s) => (
              <span key={s} className="flex items-center gap-2 text-xs text-muted">
                <span className="h-1 w-1 rounded-full bg-accent" />
                {s}
              </span>
            ))}
          </m.div>
        </div>

        <m.div
          initial={{ opacity: 0, y: 40, rotate: 4 }}
          animate={{ opacity: 1, y: 0, rotate: 0 }}
          transition={{ duration: 0.9, delay: 0.35, ease: [0.22, 1, 0.36, 1] }}
          className="perspective-1200 relative"
        >
          <div className="absolute left-1/2 top-1/2 h-[420px] w-[420px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent/20 blur-[110px]" />
          <div className="animate-float-slow [transform:rotateX(4deg)_rotateY(-8deg)]">
            <PhoneMockup className="relative" />
          </div>
        </m.div>
      </div>
    </section>
  );
}
