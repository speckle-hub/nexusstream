"use client";

import { ArrowDownToLine, ExternalLink } from "lucide-react";
import { APK_URL, RELEASES_URL } from "@/lib/utils";
import { Stagger } from "../ui/reveal";

const spec = [
  ["File", "app-release.apk"],
  ["Size", "≈44 MB"],
  ["Requires", "Android 7.0 · SDK 24"],
  ["Licence", "GPL-3.0"],
];

export function DownloadCta() {
  return (
    <section id="download" className="relative overflow-hidden border-t border-rule">
      <div
        aria-hidden
        className="absolute left-1/2 top-1/2 h-[420px] w-[720px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent/20 blur-[140px]"
      />
      <div className="relative mx-auto max-w-editorial px-5 py-24 sm:px-8 md:py-36">
        <Stagger
          items={[
            <p key="eyebrow" className="eyebrow text-accent-soft">
              Ready when you are
            </p>,
            <h2
              key="h2"
              className="display mt-4 font-sans font-bold text-paper"
              style={{ fontSize: "clamp(2.5rem, 7vw, 5.5rem)" }}
            >
              Take it with you.
              <span className="text-gradient block">Nothing owed.</span>
            </h2>,
            <p key="deck" className="mt-7 max-w-xl text-[17px] leading-relaxed text-muted md:text-[19px]">
              One APK, four ecosystems, no account. Install it, point it at the
              sources you already trust, and keep it for as long as it serves you.
            </p>,
            <div key="ctas" className="mt-10 flex flex-wrap items-center gap-3">
              <a
                href={APK_URL}
                className="press inline-flex items-center gap-2.5 rounded-full bg-paper px-8 py-4 text-[15px] font-semibold tracking-tight text-void shadow-glow-accent hover:bg-white"
              >
                <ArrowDownToLine className="h-4 w-4" />
                Download APK
              </a>
              <a
                href={RELEASES_URL}
                target="_blank"
                rel="noreferrer"
                className="press inline-flex items-center gap-2.5 rounded-full border border-white/20 px-8 py-4 text-[15px] font-semibold tracking-tight text-paper hover:border-white/50 hover:bg-white/[0.05]"
              >
                All releases
                <ExternalLink className="h-3.5 w-3.5" />
              </a>
            </div>,
            <div key="spec" className="mt-14 block border-t border-rule pt-6">
              <dl className="grid grid-cols-2 gap-x-6 gap-y-5 sm:grid-cols-4">
                {spec.map(([k, v]) => (
                  <div key={k}>
                    <dt className="eyebrow text-[10px] text-faint">{k}</dt>
                    <dd className="tabular mt-1.5 font-display text-[15px] font-semibold tracking-tight text-paper">
                      {v}
                    </dd>
                  </div>
                ))}
              </dl>
            </div>,
          ]}
        />
      </div>
    </section>
  );
}
