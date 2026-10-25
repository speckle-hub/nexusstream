"use client";

import { useState } from "react";
import Link from "next/link";
import { GITHUB_URL } from "@/lib/utils";
import { Stagger } from "../ui/reveal";

const faqs = [
  {
    q: "What is Moovio Desktop?",
    a: "A premium native Windows streaming client built with Kotlin + Compose Multiplatform and libmpv. It plays streams from Stremio add-ons, shows metadata from Cinemeta and AniList, and keeps a local library — all in a dark cinematic desktop window, not Electron.",
  },
  {
    q: "Which download should I get?",
    a: "Choose the MSI installer if you want a proper installed app with Start Menu entry and uninstaller. Choose the portable ZIP if you want to unzip and run Moovio.exe from anywhere without installing. Both include libmpv.",
  },
  {
    q: "Do I need Java installed?",
    a: "No. The app bundles a JDK 21 runtime via jpackage, so it runs on any Windows 10/11 x64 machine without requiring a separate Java installation.",
  },
  {
    q: "Is Moovio free and open source?",
    a: "Yes — free and open source (GPL-3.0 for the libmpv redistribution; the app itself is MIT). No accounts, no ads, no tracking. Metadata comes from free public APIs and playback capability comes from add-ons you install yourself.",
  },
  {
    q: "How does the NSFW area work?",
    a: "The NSFW section is fully separate from home, search and library. It's locked by a 4–8 digit PIN salted and hashed with PBKDF2-HMAC-SHA256, unlocks per-session only, and adult add-ons never surface outside the gated area.",
  },
  {
    q: "Is there still an Android app?",
    a: "Yes — Moovio for Android is still maintained and available as an APK from GitHub Releases. The desktop app is a separate project built specifically for Windows.",
  },
];

export function Faq() {
  const [open, setOpen] = useState<number | null>(0);

  return (
    <section id="faq" className="border-t border-rule bg-void-deep/30">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <Stagger
          items={[
            <p key="rubric" className="eyebrow text-accent-soft">
              04 — FAQ
            </p>,
            <h2
              key="h2"
              className="display mt-4 max-w-3xl font-display font-bold text-paper"
              style={{ fontSize: "clamp(2rem, 4vw, 3.5rem)" }}
            >
              The honest answers, <span className="text-muted">including the legal one.</span>
            </h2>,
          ]}
        />

        <div className="mt-12 space-y-2">
          {faqs.map((f, i) => {
            const isOpen = open === i;
            return (
              <div
                key={f.q}
                data-open={isOpen ? "true" : "false"}
                className="t-acc border-b border-rule group"
              >
                <button
                  type="button"
                  onClick={() => setOpen(isOpen ? null : i)}
                  aria-expanded={isOpen}
                  className="press flex w-full items-start gap-4 py-5 text-left"
                >
                  <span className="eyebrow tabular mt-1 shrink-0 text-[10px] text-faint transition-colors group-hover:text-accent-soft">
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
                    <p className="pb-6 pr-8 text-[15px] leading-[1.65] text-muted">
                      {f.a}
                    </p>
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      </div>
    </section>
  );
}
