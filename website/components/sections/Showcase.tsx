"use client";

import { DragRail } from "../ui/drag-rail";
import { Tilt } from "../ui/tilt";
import { SectionHead } from "./SectionHead";

/** Artwork palettes are pulled straight from the product's poster rails. */
const titles = [
  { n: "01", title: "Neon Divide", meta: "Series · 2026", score: "9.1", g: "linear-gradient(150deg,#4F46E5 0%,#7C3AED 55%,#A21CAF 100%)" },
  { n: "02", title: "Tide Signal", meta: "Series · 2025", score: "8.7", g: "linear-gradient(150deg,#0EA5E9 0%,#06B6D4 55%,#14B8A6 100%)" },
  { n: "03", title: "Ash Runner", meta: "Film · 2026", score: "8.9", g: "linear-gradient(150deg,#F97316 0%,#EF4444 55%,#B91C1C 100%)" },
  { n: "04", title: "Low Orbit", meta: "Series · 2026", score: "9.3", g: "linear-gradient(150deg,#7C5CFF 0%,#4F8DFF 55%,#1E3A8A 100%)" },
  { n: "05", title: "Sunfall Saga", meta: "Manga · Vol. 4", score: "—", g: "linear-gradient(150deg,#FBBF24 0%,#F59E0B 55%,#C2410C 100%)" },
  { n: "06", title: "Blue Hour", meta: "Film · 2025", score: "8.4", g: "linear-gradient(150deg,#38BDF8 0%,#3B82F6 55%,#1D4ED8 100%)" },
  { n: "07", title: "Petal Code", meta: "Anime · S2", score: "9.0", g: "linear-gradient(150deg,#F472B6 0%,#EC4899 55%,#BE185D 100%)" },
  { n: "08", title: "Verdant", meta: "Series · 2024", score: "8.4", g: "linear-gradient(150deg,#4ADE80 0%,#10B981 55%,#047857 100%)" },
];

const CARD_W = 248;
const GAP = 24;
const STEP = CARD_W + GAP;

export function Showcase() {
  return (
    <section id="showcase" className="border-t border-rule">
      <div className="mx-auto max-w-editorial px-5 py-20 sm:px-8 md:py-28">
        <SectionHead
          index="01"
          rubric="Showcase"
          title={
            <>
              The library, <span className="font-serif italic">as a catalogue.</span>
            </>
          }
          intro={
            <>
              Posters are the interface. Grab the rail and throw it — it carries
              your momentum, resists the ends, and can be caught again mid-flight.
            </>
          }
          aside={
            <dl className="flex gap-8">
              <div>
                <dt className="eyebrow text-[10px] text-ash">Titles shown</dt>
                <dd className="mt-1 font-display text-lg font-semibold tabular text-ink">
                  08
                </dd>
              </div>
              <div>
                <dt className="eyebrow text-[10px] text-ash">Sources</dt>
                <dd className="mt-1 font-display text-lg font-semibold tabular text-ink">
                  Yours
                </dd>
              </div>
            </dl>
          }
        />

        <div className="mt-12">
          <DragRail step={STEP} label="showcase titles">
            {titles.map((t) => (
              <article
                key={t.n}
                style={{ width: CARD_W }}
                className="shrink-0 select-none"
              >
                <Tilt max={8} allowScroll cardClassName="rounded-[3px]">
                  <div
                    className="relative aspect-[2/3] w-full overflow-hidden rounded-[3px] shadow-lift"
                    style={{ backgroundImage: t.g }}
                  >
                    {/* light catching the surface */}
                    <div
                      aria-hidden
                      className="absolute inset-0 bg-[radial-gradient(ellipse_at_25%_15%,rgba(255,255,255,0.32),transparent_58%)]"
                    />
                    <div
                      aria-hidden
                      className="absolute inset-x-0 bottom-0 h-1/3 bg-gradient-to-t from-black/45 to-transparent"
                    />
                    <div className="absolute inset-x-0 top-0 flex items-start justify-between p-3.5">
                      <span className="eyebrow text-[9px] text-white/75">
                        {t.n}
                      </span>
                      {t.score !== "—" && (
                        <span className="eyebrow tabular rounded-full bg-black/35 px-2 py-1 text-[9px] text-white backdrop-blur-sm">
                          {t.score}
                        </span>
                      )}
                    </div>
                  </div>
                </Tilt>

                <div className="mt-3 border-t border-rule pt-2.5">
                  <h3 className="font-display text-[15px] font-semibold tracking-tight text-ink">
                    {t.title}
                  </h3>
                  <p className="mt-0.5 font-serif text-[13px] italic text-ash">
                    {t.meta}
                  </p>
                </div>
              </article>
            ))}
          </DragRail>
        </div>
      </div>
    </section>
  );
}
