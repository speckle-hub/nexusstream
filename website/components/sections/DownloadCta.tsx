"use client";

import { m } from "framer-motion";
import { FileDown, Smartphone } from "lucide-react";
import { APK_URL, RELEASES_URL } from "@/lib/utils";
import { BackgroundBeams } from "../ui/background-beams";
import { MovingBorderButton } from "../ui/moving-border";
import { Sparkles } from "../ui/sparkles";
import { Stagger } from "../ui/reveal";

/** Critically damped house spring (apple-design): graceful, no overshoot. */
const SPRING = { type: "spring", bounce: 0, duration: 0.4 } as const;

function LearnMoreChevron() {
  return (
    <span className="t-learn-chevron">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path className="t-learn-arm t-learn-arm-top" d="M6 4L10 8" />
        <path className="t-learn-arm t-learn-arm-bot" d="M10 8L6 12" />
      </svg>
    </span>
  );
}

export function DownloadCta() {
  return (
    <section id="download" className="relative overflow-hidden py-28 md:py-40">
      <BackgroundBeams />
      <Sparkles density={50} color="#8F74FF" />
      <div className="absolute left-1/2 top-1/2 h-[380px] w-[680px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent/15 blur-[130px]" />

      <div className="relative z-10 mx-auto max-w-3xl px-6 text-center">
        <Stagger
          items={[
            <span key="h2" className="display-tight block font-display text-4xl font-bold text-mist md:text-6xl">
              Ready when you are.
              <br />
              <span className="text-gradient">Free forever.</span>
            </span>,
            <span key="sub" className="mx-auto mt-5 block max-w-xl text-sm text-muted md:text-base">
              Grab the APK from GitHub Releases and install it like any other app.
              Android 7.0+ · ≈42 MB · updates install in place.
            </span>,
          ]}
        />

        <m.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true, margin: "-100px" }}
          transition={{ ...SPRING, delay: 0.15 }}
          className="mt-10 flex flex-col items-center justify-center gap-4 sm:flex-row"
        >
          <MovingBorderButton href={APK_URL} containerClassName="shadow-glow-accent rounded-full">
            <FileDown className="h-4 w-4" />
            Download v1.7.0 APK
          </MovingBorderButton>
          <a
            href={RELEASES_URL}
            target="_blank"
            rel="noreferrer"
            className="t-learn flex items-center gap-2 rounded-full border border-white/15 px-8 py-3.5 font-semibold text-mist transition hover:border-white/35 hover:bg-white/5"
            style={{ transitionDuration: "var(--duration-fast)", transitionTimingFunction: "var(--ease-smooth-out)" }}
          >
            All releases
            <LearnMoreChevron />
          </a>
        </m.div>

        <m.div
          initial={{ opacity: 0 }}
          whileInView={{ opacity: 1 }}
          viewport={{ once: true }}
          transition={{ ...SPRING, delay: 0.3 }}
          className="mt-12 inline-flex items-center gap-2.5 rounded-full border border-white/10 bg-white/[0.03] px-5 py-2.5 text-xs text-muted"
        >
          <Smartphone className="h-4 w-4 text-accent-soft" />
          <span className="t-shimmer" data-text="No account. No ads. No tracking. Just your library.">
            No account. No ads. No tracking. Just your library.
          </span>
        </m.div>
      </div>
    </section>
  );
}
