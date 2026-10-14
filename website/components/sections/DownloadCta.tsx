"use client";

import { ArrowDownToLine, ExternalLink } from "lucide-react";
import { APK_URL, RELEASES_URL } from "@/lib/utils";
import { Stagger } from "../ui/reveal";

const spec = [
  ["File", "app-release.apk"],
  ["Size", "≈42 MB"],
  ["Requires", "Android 7.0 · SDK 24"],
  ["Licence", "GPL-3.0"],
];

/**
 * Closing plate. Inverted type on ink — the page ends the way a magazine
 * ends, with a colophon block rather than another button on white.
 */
export function DownloadCta() {
  return (
    <section id="download" className="relative bg-ink text-paper">
      <div className="mx-auto max-w-editorial px-5 py-24 sm:px-8 md:py-36">
        <Stagger
          items={[
            <span
              key="eyebrow"
              className="eyebrow mb-8 flex items-center gap-3 text-white/45"
            >
              <span className="h-px w-8 bg-vermilion" />
              Ready when you are
            </span>,

            <h2
              key="h2"
              className="display font-sans font-bold text-paper"
              style={{ fontSize: "clamp(2.5rem, 7vw, 6rem)" }}
            >
              <span className="block">Take it with you.</span>
              <span className="block font-serif italic text-white/45">
                Nothing owed.
              </span>
            </h2>,

            <p key="deck" className="measure mt-8 text-[17px] text-white/60 md:text-[19px]">
              One APK, four ecosystems, no account. Install it, point it at the
              sources you already trust, and keep it for as long as it serves
              you.
            </p>,

            <div key="ctas" className="mt-11 flex flex-wrap items-center gap-3">
              <a
                href={APK_URL}
                className="press t-learn inline-flex items-center gap-2.5 rounded-full bg-paper px-8 py-4 text-[15px] font-semibold tracking-tight text-ink hover:bg-white"
              >
                <ArrowDownToLine className="h-4 w-4" />
                Download APK
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
              <a
                href={RELEASES_URL}
                target="_blank"
                rel="noreferrer"
                className="press inline-flex items-center gap-2.5 rounded-full border border-white/25 px-8 py-4 text-[15px] font-semibold tracking-tight text-paper hover:border-white/60 hover:bg-white/[0.06]"
              >
                All releases
                <ExternalLink className="h-3.5 w-3.5" />
              </a>
            </div>,

            <div key="spec" className="mt-14 block border-t border-white/15 pt-6">
              <dl className="grid grid-cols-2 gap-x-6 gap-y-5 sm:grid-cols-4">
                {spec.map(([k, v]) => (
                  <div key={k}>
                    <dt className="eyebrow text-[10px] text-white/40">{k}</dt>
                    <dd className="mt-1.5 font-display text-[15px] font-semibold tracking-tight tabular text-paper">
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
