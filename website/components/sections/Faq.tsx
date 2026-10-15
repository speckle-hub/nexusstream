"use client";

import { useState } from "react";
import { cn } from "@/lib/utils";
import { Stagger } from "../ui/reveal";

const faqs = [
  {
    q: "Is Moovio free?",
    a: "Yes — free and open source. No accounts, no ads, no tracking, no paywalls. Download the APK from GitHub Releases and you're set.",
  },
  {
    q: "Where does the content come from?",
    a: "Moovio is a player and organiser, not a content source. Metadata comes from free public APIs (TMDB, AniList, MangaDex, Jikan, Cinemeta) and subtitles from OpenSubtitles. Built-in resolvers cover popular movies, TV and anime out of the box, and you can add third-party add-ons of your own — nothing extra is enabled without your say-so.",
  },
  {
    q: "How do I install it?",
    a: "Download app-release.apk from the latest GitHub release, tap it, and allow “Install unknown apps” for your file manager. Android 7.0 (SDK 24) or newer is required. Future updates apply in place — and the app can check for new releases itself.",
  },
  {
    q: "What are Stremio, CloudStream, Aniyomi and Mihon add-ons?",
    a: "Four popular open extension ecosystems. Moovio speaks all four: paste a Stremio manifest URL or a provider repository URL in the Add-on Manager, browse its extensions, and install what you want. Nothing is enabled without your say-so.",
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

export function Faq() {
  const [open, setOpen] = useState<number | null>(0);

  return (
    <section id="faq" className="border-t border-rule bg-void-deep/40">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <Stagger
          items={[
            <p key="rubric" className="eyebrow text-accent-soft">
              04 — FAQ
            </p>,
            <h2
              key="h2"
              className="display mt-4 max-w-3xl font-sans font-bold text-paper"
              style={{ fontSize: "clamp(1.9rem, 3.6vw, 3.25rem)" }}
            >
              The honest answers, <span className="text-muted">including the legal one.</span>
            </h2>,
          ]}
        />

        <div className="mt-10 grid md:grid-cols-[220px_1fr] md:gap-12">
          <p className="hidden text-[13.5px] leading-relaxed text-faint md:block">
            <span className="sticky top-28 block">
              Still stuck? Everything is written down in the repository — the
              README, the build logs and the test suite are all public.
            </span>
          </p>

          <div>
            {faqs.map((f, i) => {
              const isOpen = open === i;
              return (
                <div
                  key={f.q}
                  data-open={isOpen ? "true" : "false"}
                  className={cn("t-acc border-b border-rule", i === 0 && "border-t")}
                >
                  <button
                    type="button"
                    onClick={() => setOpen(isOpen ? null : i)}
                    aria-expanded={isOpen}
                    className="press group flex w-full items-start gap-5 py-6 text-left"
                  >
                    <span className="eyebrow tabular mt-1.5 shrink-0 text-[10px] text-faint transition-colors group-hover:text-accent-soft">
                      {String(i + 1).padStart(2, "0")}
                    </span>
                    <span className="flex-1 font-display text-[17px] font-semibold leading-snug tracking-tight text-paper md:text-[19px]">
                      {f.q}
                    </span>
                    <span className="t-acc-chevron shrink-0 text-muted">
                      <svg width="18" height="18" viewBox="0 0 16 16" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M4 6.5L8 10.5L12 6.5" />
                      </svg>
                    </span>
                  </button>
                  <div className="t-acc-panel">
                    <div className="t-acc-panel-inner">
                      <p className="max-w-[62ch] pb-7 pl-[3.1rem] pr-8 text-[15.5px] leading-[1.65] text-muted">
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
