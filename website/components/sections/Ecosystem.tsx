"use client";

import { useState } from "react";
import { Segmented, SpecTable } from "../ui/segmented";
import { SectionHead } from "./SectionHead";

const TABS = ["Extensions", "Metadata", "Playback"] as const;
type Tab = (typeof TABS)[number];

const panels: Record<Tab, { name: string; detail: string }[]> = {
  Extensions: [
    { name: "Stremio", detail: "Full add-on protocol — paste a manifest URL, browse, install." },
    { name: "CloudStream", detail: "Provider repository support, including custom repos." },
    { name: "Aniyomi", detail: "Anime extension repositories, installed per source." },
    { name: "Mihon / Keiyoushi", detail: "Manga extension repositories with the same opt-in model." },
  ],
  Metadata: [
    { name: "TMDB", detail: "Movies and television: artwork, cast, seasons, watch providers." },
    { name: "AniList", detail: "Anime metadata, seasonal charts and synced lists." },
    { name: "MangaDex", detail: "Manga catalogues, chapter trees and multilingual groups." },
    { name: "Jikan", detail: "Unofficial MyAnimeList bridge for ratings and rankings." },
  ],
  Playback: [
    { name: "Media3 ExoPlayer", detail: "HLS, DASH and progressive, with encrypted playlist support." },
    { name: "OpenSubtitles", detail: "Subtitle lookup with styling, sync offset and language pinning." },
    { name: "Hand-off", detail: "Send any stream to an external player without losing position." },
    { name: "Downloads", detail: "The same pipeline, written to disk with retries and failover." },
  ],
};

export function Ecosystem() {
  const [tab, setTab] = useState<Tab>("Extensions");

  return (
    <section id="ecosystem" className="border-t border-rule bg-paper-deep/60">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <SectionHead
          index="04"
          rubric="Ecosystem"
          title={
            <>
              It speaks the protocols{" "}
              <span className="font-serif italic">you already use.</span>
            </>
          }
          intro="NexusStream is an organiser, not a catalogue. It connects to four
            extension ecosystems and five metadata sources — and ships with none
            of them switched on."
        />

        <div className="mt-10 grid gap-10 lg:grid-cols-[220px_1fr] lg:gap-14">
          <div className="lg:pt-2">
            <Segmented
              tabs={TABS}
              value={tab}
              onChange={setTab}
              panel={<SpecTable entries={panels[tab]} />}
            />
          </div>

          <aside className="self-start border-t border-rule pt-6 lg:border-l lg:border-t-0 lg:pl-10 lg:pt-0">
            <p className="font-serif text-[19px] italic leading-relaxed text-ink md:text-[22px]">
              “Ships with no stream providers, no provider repositories and no
              catalogs of its own.”
            </p>
            <p className="mt-5 text-[13.5px] leading-relaxed text-ash">
              Content neutrality is a design constraint, not a disclaimer. The
              app is a player and an organiser — everything else is installed by
              you, and can be removed by you.
            </p>
            <div className="mt-6 flex flex-wrap gap-x-6 gap-y-2 border-t border-rule pt-4">
              {["No accounts", "No ads", "No tracking", "No paywalls"].map((x) => (
                <span key={x} className="eyebrow text-[10px] text-ash">
                  {x}
                </span>
              ))}
            </div>
          </aside>
        </div>
      </div>
    </section>
  );
}
