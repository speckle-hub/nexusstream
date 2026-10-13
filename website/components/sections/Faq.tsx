"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { Stagger } from "../ui/reveal";

const faqs = [
  {
    q: "Is NexusStream free?",
    a: "Yes — free and open source. No accounts, no ads, no tracking, no paywalls. Download the APK from GitHub Releases and you're set.",
  },
  {
    q: "Where does the content come from?",
    a: "NexusStream is a player and organizer, not a content source. Metadata comes from free public APIs (TMDB, AniList, MangaDex, Jikan, Cinemeta) and subtitles from OpenSubtitles. Any streaming capability comes from third-party add-ons that you choose to install — the app ships with no stream providers preinstalled.",
  },
  {
    q: "How do I install it?",
    a: "Download app-release.apk from the latest GitHub release, tap it, and allow “Install unknown apps” for your file manager. Android 7.0 (SDK 24) or newer is required. Future updates can be applied in place — and the app can check for new releases itself.",
  },
  {
    q: "What are Stremio / CloudStream / Aniyomi / Mihon add-ons?",
    a: "They are four popular open extension ecosystems. NexusStream speaks all four: paste a Stremio manifest URL or a provider repository URL in the Add-on Manager, browse its extensions, and install what you want. Nothing is enabled without your say-so.",
  },
  {
    q: "Can it play and read offline?",
    a: "Yes. Videos download through Media3 (progressive and HLS, including encrypted playlists) and manga chapters download page-by-page. Both render offline-first from the Library — with pause/resume, mirror failover and a storage meter.",
  },
  {
    q: "Is there NSFW content handling?",
    a: "The NSFW area is fully separate from normal content and locked by default. You can require a PIN and/or biometrics, it auto-locks when the app is backgrounded, and incognito mode keeps it out of your watch history.",
  },
];

function Chevron() {
  return (
    <span className="t-acc-chevron shrink-0 text-muted">
      <svg width="16" height="16" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round">
        <path d="M4 6.5L8 10.5L12 6.5" />
      </svg>
    </span>
  );
}

export function Faq() {
  const [open, setOpen] = useState<number | null>(0);

  return (
    <section id="faq" className="mx-auto max-w-3xl px-6 py-24 md:py-32">
      <Stagger
        className="mb-12 text-center"
        items={[
          <span key="eyebrow" className="mb-3 block text-xs font-semibold uppercase tracking-[0.28em] text-accent-soft">
            FAQ
          </span>,
          <span key="h2" className="display-tight block font-display text-3xl font-bold text-mist md:text-5xl">
            Good questions
          </span>,
          <span key="sub" className="mx-auto mt-4 block max-w-xl text-sm text-muted md:text-base">
            The honest answers, including the legal ones.
          </span>,
        ]}
      />

      <div className="space-y-3">
        {faqs.map((f, i) => {
          const isOpen = open === i;
          return (
            <div
              key={f.q}
              data-open={isOpen ? "true" : "false"}
              className={cn(
                "t-acc glass !rounded-2xl",
                isOpen && "border-accent/25",
              )}
            >
              <button
                onClick={() => setOpen(isOpen ? null : i)}
                aria-expanded={isOpen}
                className="t-acc-head flex w-full items-center justify-between gap-4 px-5 py-4 text-left"
              >
                <span className="font-display text-[15px] font-semibold text-mist">{f.q}</span>
                <Chevron />
              </button>
              <div className="t-acc-panel">
                <div className="t-acc-panel-inner">
                  <p className="px-5 pb-5 text-sm leading-relaxed text-muted">{f.a}</p>
                </div>
              </div>
            </div>
          );
        })}
      </div>
    </section>
  );
}
