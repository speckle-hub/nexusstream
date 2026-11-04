import Link from "next/link";
import type { CSSProperties } from "react";
import {
  ANDROID_APK_SIZE,
  ANDROID_MIN_SDK,
  ANDROID_VERSION,
  APK_URL,
  DESKTOP_MSI_URL,
  DESKTOP_VERSION,
  TEST_COUNT,
} from "@/lib/utils";
import { Marquee } from "@/components/motion/Marquee";
import { Reveal } from "@/components/motion/Reveal";
import { PosterDeck } from "./PosterDeck";

const facts = [
  ["Release", ANDROID_VERSION],
  ["Tests", `${TEST_COUNT} passing`],
  ["Requires", ANDROID_MIN_SDK],
  ["Account", "Not required"],
];

const ecosystem = [
  "Stremio",
  "CloudStream",
  "Aniyomi",
  "Mihon",
  "TMDB",
  "AniList",
  "MangaDex",
  "Trakt",
  "OpenSubtitles",
  "Chromecast",
];

export function Hero() {
  return (
    <section id="top" className="relative overflow-hidden border-b border-rule">
      {/* One warm wash, from the top right, like light through a booth window. */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0"
        style={{
          background:
            "radial-gradient(900px 520px at 78% -14%, rgba(223,164,92,0.10) 0%, rgba(223,164,92,0.03) 38%, transparent 70%)",
        }}
      />

      <div className="shell relative">
        {/*
          Mobile order is copy → CTA → deck → facts, so the signature still lands in the
          first scroll. Desktop places the same blocks in two columns with the deck centred.
        */}
        <div className="grid gap-10 pb-16 pt-28 lg:grid-cols-[1.04fr_0.96fr] lg:items-center lg:gap-x-10 lg:gap-y-8 lg:pb-24 lg:pt-36">
          <div className="lg:col-start-1 lg:row-start-1">
            <Reveal>
              <p className="mono flex flex-wrap items-center gap-x-3 gap-y-1 text-[11.5px] text-muted">
                <span className="text-amber">Android {ANDROID_VERSION}</span>
                <span className="h-px w-6 bg-rule" aria-hidden />
                <span>Free · Open source · No account</span>
              </p>
            </Reveal>

            <Reveal delay={40}>
              <span aria-hidden className="beam beam-draw mt-7 w-16" />
            </Reveal>

            {/* The signature: two lines rise out of their slots, once, 100 ms apart. */}
            <h1 className="mt-7 max-w-[15ch] text-[clamp(2.7rem,6.4vw,5rem)] font-medium leading-[1.02] tracking-[-0.035em] text-paper">
              <span className="slot" style={{ "--slot-delay": "120ms" } as CSSProperties}>
                <span>Every source.</span>
              </span>
              <span className="slot" style={{ "--slot-delay": "220ms" } as CSSProperties}>
                <span>
                  <span className="text-amber-soft">One</span> player.
                </span>
              </span>
            </h1>

            <Reveal delay={140}>
              <p className="mt-7 max-w-[54ch] text-[16.5px] leading-[1.65] text-muted">
                Moovio plays movies, TV, anime and manga from the sources you already use:
                Stremio add-ons, CloudStream extensions and fourteen built-in scrapers. One
                search, one library, one player.
              </p>
            </Reveal>
          </div>

          <Reveal
            delay={180}
            className="order-3 flex justify-center lg:order-none lg:col-start-2 lg:row-span-2 lg:row-start-1 lg:self-center lg:justify-end"
          >
            <PosterDeck />
          </Reveal>

          <Reveal delay={200} className="order-2 lg:order-none lg:col-start-1 lg:row-start-2">
            <div className="flex flex-wrap items-center gap-x-7 gap-y-4">
              <Link href={APK_URL} className="btn btn-primary">
                Download for Android
                <span className="mono text-[11px] opacity-70">{ANDROID_APK_SIZE}</span>
              </Link>
              <Link href={DESKTOP_MSI_URL} className="link-quiet text-[14px]">
                Windows build <span className="mono text-[12px]">v{DESKTOP_VERSION}</span>
              </Link>
            </div>
          </Reveal>

          <Reveal delay={260} className="order-4 lg:order-none lg:col-start-1 lg:row-start-3">
            <dl className="grid max-w-lg grid-cols-2 gap-x-8 gap-y-5 border-t border-rule pt-6 sm:grid-cols-4">
              {facts.map(([label, value]) => (
                <div key={label}>
                  <dt className="mono text-[10.5px] uppercase tracking-[0.14em] text-faint">
                    {label}
                  </dt>
                  <dd className="mt-2 text-[14.5px] text-paper">{value}</dd>
                </div>
              ))}
            </dl>
          </Reveal>
        </div>
      </div>

      <div className="border-t border-rule-soft py-4">
        <Marquee items={ecosystem} durationSeconds={34} />
      </div>
    </section>
  );
}
