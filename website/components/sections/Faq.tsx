"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { SectionHead } from "./SectionHead";

const faqs = [
  {
    q: "Is NexusStream free?",
    a: "Yes — free and open source. No accounts, no ads, no tracking, no paywalls. Download the APK from GitHub Releases and you're set.",
  },
  {
    q: "Where does the content come from?",
    a: "NexusStream is a player and organiser, not a content source. Metadata comes from free public APIs (TMDB, AniList, MangaDex, Jikan, Cinemeta) and subtitles from OpenSubtitles. Any streaming capability comes from third-party add-ons that you choose to install — the app ships with no stream providers preinstalled.",
  },
  {
    q: "How do I install it?",
    a: "Download app-release.apk from the latest GitHub release, tap it, and allow “Install unknown apps” for your file manager. Android 7.0 (SDK 24) or newer is required. Future updates apply in place — and the app can check for new releases itself.",
  },
  {
    q: "What are Stremio, CloudStream, Aniyomi and Mihon add-ons?",
    a: "Four popular open extension ecosystems. NexusStream speaks all four: paste a Stremio manifest URL or a provider repository URL in the Add-on Manager, browse its extensions, and install what you want. Nothing is enabled without your say-so.",
  },
  {
    q: "Can it play and read offline?",
    a: "Yes. Videos download through Media3 (progressive and HLS, including encrypted playlists) and manga chapters download page-by-page. Both render offline-first from the Library — with pause and resume, mirror failover and a storage meter.",
  },
  {
    q: "Is there NSFW content handling?",
    a: "The NSFW area is fully separate from normal content and locked by default. You can require a PIN and/or biometrics, it auto-locks when the app is backgrounded, and incognito mode keeps it out of your watch history.",
  },
];

function Chevron() {
  return (
    <span className="t-acc-chevron shrink-0 text-ash">
      <svg
        width="18"
        height="18"
        viewBox="0 0 16 16"
        fill="none"
        stroke="currentColor"
        strokeWidth="1.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        <path d="M4 6.5L8 10.5L12 6.5" />
      </svg>
    </span>
  );
}

export function Faq() {
  const [open, setOpen] = useState<number | null>(0);

  return (
    <section id="faq" className="border-t border-rule">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <SectionHead
          index="05"
          rubric="FAQ"
          title={
            <>
              The honest answers,{" "}
              <span className="font-serif italic">including the legal one.</span>
            </>
          }
        />

        <div className="mt-4 grid md:grid-cols-[190px_1fr] md:gap-12 lg:grid-cols-[220px_1fr]">
          <div className="hidden md:block">
            <p className="sticky top-28 text-[13.5px] leading-relaxed text-ash">
              Still stuck? Everything is written down in the repository — the
              README, the build logs and the test suite are all public.
            </p>
          </div>

          <div>
            {faqs.map((f, i) => {
              const isOpen = open === i;
              return (
                <div
                  key={f.q}
                  data-open={isOpen ? "true" : "false"}
                  className={cn(
                    "t-acc border-b",
                    isOpen ? "border-rule" : "border-rule-soft",
                    i === 0 && "border-t",
                  )}
                >
                  <button
                    type="button"
                    onClick={() => setOpen(isOpen ? null : i)}
                    aria-expanded={isOpen}
                    className="press t-acc-head group flex w-full items-start gap-5 py-6 text-left"
                  >
                    <span className="eyebrow tabular mt-1.5 shrink-0 text-[10px] text-ash transition-colors group-hover:text-vermilion">
                      {String(i + 1).padStart(2, "0")}
                    </span>
                    <span className="flex-1 font-display text-[17px] font-semibold leading-snug tracking-tight text-ink md:text-[19px]">
                      {f.q}
                    </span>
                    <Chevron />
                  </button>
                  <div className="t-acc-panel">
                    <div className="t-acc-panel-inner">
                      <p className="max-w-[62ch] pb-7 pl-[3.1rem] pr-8 text-[15.5px] leading-[1.65] text-graphite">
                        {f.a}
                      </p>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </section>
  );
}
