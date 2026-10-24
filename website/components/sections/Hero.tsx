"use client";

import { m } from "framer-motion";
import { ArrowDownToLine, Github, ShieldCheck, Star } from "lucide-react";
import { APK_URL, GITHUB_URL } from "@/lib/utils";
import { PhoneMockup } from "../PhoneMockup";
import { Stagger } from "../ui/reveal";

/** Critically damped house spring: no overshoot. */
const SPRING = { type: "spring", bounce: 0, duration: 0.5 } as const;

const proof = [
  ["Android", "7.0+"],
  ["Install", "≈44 MB"],
  ["Account", "None"],
  ["Ads / tracking", "None"],
];

export function Hero() {
  return (
    <section id="top" className="relative overflow-hidden pt-16">
      {/* Backdrop: grid + violet/mint glows + bottom fade */}
      <div aria-hidden className="hero-grid absolute inset-0" />
      <div
        aria-hidden
        className="absolute left-1/2 top-[-320px] h-[560px] w-[900px] -translate-x-1/2 rounded-full bg-accent/25 blur-[140px]"
      />
      <div
        aria-hidden
        className="absolute right-[-180px] top-[30%] h-[380px] w-[380px] rounded-full bg-signal/10 blur-[120px]"
      />
      <div
        aria-hidden
        className="absolute inset-x-0 bottom-0 h-48 bg-gradient-to-t from-void to-transparent"
      />

      <div className="relative mx-auto max-w-editorial px-5 sm:px-8">
        <div className="grid items-center gap-14 pb-20 pt-14 md:pt-20 lg:grid-cols-[1.05fr_0.95fr] lg:gap-6 lg:pb-28">
          <Stagger
            className="max-w-2xl"
            items={[
              <span
                key="badge"
                className="eyebrow mb-8 inline-flex items-center gap-2.5 rounded-full border border-rule bg-white/[0.04] py-2 pl-3 pr-4 text-muted"
              >
                <span className="h-1.5 w-1.5 rounded-full bg-signal animate-pulse-soft" />
                v1.10.4 — free &amp; open source
              </span>,

              <h1 key="h1" className="display text-paper" style={{ fontSize: "clamp(2.75rem, 7vw, 5.75rem)" }}>
                <span className="block font-sans font-bold">Every stream.</span>
                <span className="text-gradient block font-sans font-bold">One app.</span>
              </h1>,

              <p key="deck" className="mt-7 max-w-xl text-[17px] leading-relaxed text-muted md:text-[19px]">
                Moovio unifies Stremio add-ons, CloudStream extensions,
                Aniyomi and Mihon repos into a single Android app — movies, TV,
                anime and manga, with a player and a reader built for the thing
                you are actually watching.
              </p>,

              <div key="ctas" className="mt-10 flex flex-wrap items-center gap-3">
                <a
                  href={APK_URL}
                  className="press inline-flex items-center gap-2.5 rounded-full bg-paper px-7 py-4 text-[15px] font-semibold tracking-tight text-void shadow-glow-accent hover:bg-white"
                >
                  <ArrowDownToLine className="h-4 w-4" />
                  Download the APK
                </a>
                <a
                  href={GITHUB_URL}
                  target="_blank"
                  rel="noreferrer"
                  className="press inline-flex items-center gap-2.5 rounded-full border border-white/20 px-7 py-4 text-[15px] font-semibold tracking-tight text-paper hover:border-white/50 hover:bg-white/[0.05]"
                >
                  <Github className="h-4 w-4" />
                  Source
                </a>
              </div>,

              <div key="assure" className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-2 text-[13px] text-faint">
                <span className="inline-flex items-center gap-1.5">
                  <ShieldCheck className="h-3.5 w-3.5 text-signal" />
                  No accounts, no ads, no tracking
                </span>
                <span className="inline-flex items-center gap-1.5">
                  <Star className="h-3.5 w-3.5 text-accent-soft" />
                  309 unit tests, all green
                </span>
              </div>,

              <div key="proof" className="mt-10 block border-t border-rule pt-5">
                <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-4">
                  {proof.map(([k, v]) => (
                    <div key={k}>
                      <dt className="eyebrow text-[10px] text-faint">{k}</dt>
                      <dd className="tabular mt-1 font-display text-[15px] font-semibold tracking-tight text-paper">
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
            transition={{ ...SPRING, delay: 0.25 }}
            className="perspective-1200 relative flex justify-center"
          >
            <div
              aria-hidden
              className="absolute bottom-2 left-1/2 h-24 w-[78%] -translate-x-1/2 rounded-[50%] bg-accent/30 blur-3xl"
            />
            <div className="[transform:rotateY(-6deg)_rotateX(2deg)]">
              <PhoneMockup className="relative" />
            </div>
          </m.div>
        </div>
      </div>
    </section>
  );
}
