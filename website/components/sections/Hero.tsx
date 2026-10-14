"use client";

import { m } from "framer-motion";
import { ArrowDownToLine, Github } from "lucide-react";
import { APK_URL, GITHUB_URL } from "@/lib/utils";
import { PhoneMockup } from "../PhoneMockup";
import { Stagger } from "../ui/reveal";

/** Critically damped house spring (apple-design §4): no overshoot. */
const SPRING = { type: "spring", bounce: 0, duration: 0.4 } as const;

const proof = [
  ["Android", "7.0+"],
  ["Install", "≈42 MB"],
  ["Account", "None"],
  ["Ads / tracking", "None"],
];

export function Hero() {
  return (
    <section id="top" className="relative pt-16">
      {/* ── Masthead ─────────────────────────────────────────── */}
      <div className="mx-auto max-w-editorial px-5 sm:px-8">
        <div className="eyebrow flex items-center justify-between gap-4 border-b border-rule py-3.5 text-ash">
          <span className="text-ink">NexusStream</span>
          <span className="hidden md:block">Movies · TV · Anime · Manga</span>
          <span className="flex items-center gap-2">
            <span className="h-1.5 w-1.5 rounded-full bg-vermilion animate-pulse-soft" />
            v1.7.0 — free &amp; open source
          </span>
        </div>
      </div>

      <div className="mx-auto max-w-editorial px-5 sm:px-8">
        <div className="grid items-start gap-12 pb-20 pt-14 md:pt-20 lg:grid-cols-[1.05fr_0.95fr] lg:gap-0 lg:pb-28">
          {/* ── Column: copy ───────────────────────────────── */}
          <Stagger
            className="lg:pr-14"
            items={[
              <span
                key="eyebrow"
                className="eyebrow mb-8 flex items-center gap-3 text-ash"
              >
                <span className="h-px w-8 bg-ink" />
                One app, four ecosystems
              </span>,

              <h1
                key="h1"
                className="display text-ink"
                style={{ fontSize: "clamp(2.75rem, 7.6vw, 6.5rem)" }}
              >
                <span className="block font-sans font-bold">Every stream.</span>
                <span className="block font-serif italic text-vermilion">
                  One app.
                </span>
              </h1>,

              <p key="deck" className="measure mt-8 text-[17px] text-graphite md:text-[19px]">
                NexusStream unifies Stremio add-ons, CloudStream extensions, Aniyomi
                and Mihon repos into a single Android app — with a player and a
                reader built for the thing you are actually watching.
              </p>,

              <div key="ctas" className="mt-10 flex flex-wrap items-center gap-3">
                <a
                  href={APK_URL}
                  className="press inline-flex items-center gap-2.5 rounded-full bg-ink px-7 py-4 text-[15px] font-semibold tracking-tight text-paper hover:bg-graphite"
                >
                  <ArrowDownToLine className="h-4 w-4" />
                  Download the APK
                </a>
                <a
                  href={GITHUB_URL}
                  target="_blank"
                  rel="noreferrer"
                  className="press t-learn inline-flex items-center gap-2.5 rounded-full border border-ink/25 px-7 py-4 text-[15px] font-semibold tracking-tight text-ink hover:border-ink hover:bg-ink/[0.04]"
                >
                  <Github className="h-4 w-4" />
                  Source
                  <span className="t-learn-chevron">
                    <svg
                      width="14"
                      height="14"
                      viewBox="0 0 16 16"
                      fill="none"
                      stroke="currentColor"
                      strokeWidth="1.8"
                      strokeLinecap="round"
                      strokeLinejoin="round"
                    >
                      <path className="t-learn-arm t-learn-arm-top" d="M6 4L10 8" />
                      <path className="t-learn-arm t-learn-arm-bot" d="M10 8L6 12" />
                    </svg>
                  </span>
                </a>
              </div>,

              <div key="proof" className="mt-12 block border-t border-rule pt-5">
                <dl className="grid grid-cols-2 gap-x-6 gap-y-4 sm:grid-cols-4">
                  {proof.map(([k, v]) => (
                    <div key={k}>
                      <dt className="eyebrow text-[10px] text-ash">{k}</dt>
                      <dd className="mt-1 font-display text-[15px] font-semibold tracking-tight text-ink tabular">
                        {v}
                      </dd>
                    </div>
                  ))}
                </dl>
              </div>,
            ]}
          />

          {/* ── Column: the product, presented as an object ──── */}
          <div className="relative lg:border-l lg:border-rule lg:pl-14">
            <m.div
              initial={{ opacity: 0, y: 32 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ ...SPRING, delay: 0.3 }}
              className="perspective-1200 relative flex justify-center"
            >
              {/* ground shadow — the device sits on the page, it doesn't float */}
              <div
                aria-hidden
                className="absolute bottom-2 left-1/2 h-24 w-[78%] -translate-x-1/2 rounded-[50%] bg-ink/25 blur-3xl"
              />

              <div className="[transform:rotateY(-6deg)_rotateX(2deg)]">
                <PhoneMockup className="relative" />
              </div>
            </m.div>

            {/* Museum label */}
            <m.figcaption
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ ...SPRING, delay: 0.6 }}
              className="mx-auto mt-10 max-w-[300px] border-t border-rule pt-3 text-center lg:max-w-none"
            >
              <span className="eyebrow block text-[10px] text-vermilion">
                Fig. 01
              </span>
              <span className="mt-1.5 block text-[13px] leading-relaxed text-ash">
                Home feed, unmodified. Poster palettes are extracted from artwork
                at runtime — no two libraries tint alike.
              </span>
            </m.figcaption>
          </div>
        </div>
      </div>
    </section>
  );
}
