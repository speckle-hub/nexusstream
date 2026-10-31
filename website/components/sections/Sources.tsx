import { ArrowRight } from "lucide-react";
import { SFW_SOURCE_COUNT } from "@/lib/utils";

const sources = [
  {
    name: "Stremio add-ons",
    note: "Any manifest URL. Torrentio and OpenSubtitles ship by default; one failing add-on never takes the list down.",
  },
  {
    name: "CloudStream extensions",
    note: ".cs3 providers download, install and run in-process, so the sites you already know become playable sources.",
  },
  {
    name: "Built-in scrapers",
    note: `${SFW_SOURCE_COUNT} movie, TV and anime sources compiled into the app, so Play works with nothing installed.`,
  },
];

const outcomes = [
  ["One search", "Every enabled source answered in one list"],
  ["One library", "Favourites and resume, stored on the device"],
  ["One player", "ExoPlayer with the subtitles and tracks you picked"],
];

export function Sources() {
  return (
    <section id="sources" className="border-b border-rule">
      <div className="shell py-24 md:py-28">
        <p className="mono text-[12px] text-amber">00:04:12</p>
        <h2 className="mt-5 max-w-[24ch] text-[clamp(1.9rem,3.6vw,2.9rem)] font-medium leading-[1.1] tracking-[-0.02em] text-paper">
          The sources you already have. One place to play them.
        </h2>
        <p className="mt-5 max-w-[58ch] text-[16px] leading-[1.65] text-muted">
          Moovio does not host a catalogue. It connects the ones you point it at, then gets out
          of the way: one search across all of them, one library, one player.
        </p>

        <div className="mt-16 grid items-center gap-10 lg:grid-cols-[1fr_64px_0.9fr] lg:gap-0">
          <ul className="flex flex-col">
            {sources.map((s) => (
              <li
                key={s.name}
                className="border-t border-rule py-6 first:border-t-0 first:pt-0 last:pb-0"
              >
                <p className="text-[15px] font-medium text-paper">{s.name}</p>
                <p className="mt-2 max-w-[44ch] text-[14px] leading-[1.6] text-muted">
                  {s.note}
                </p>
              </li>
            ))}
          </ul>

          <div aria-hidden className="relative hidden h-full min-h-[260px] lg:block">
            <span className="absolute left-1/2 top-0 h-full w-px bg-rule" />
            {[12, 50, 88].map((top) => (
              <span
                key={top}
                className="absolute right-0 h-px w-1/2 bg-amber/40"
                style={{ top: `${top}%` }}
              />
            ))}
            <ArrowRight
              className="absolute -right-1 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-amber"
              strokeWidth={1.5}
            />
          </div>

          <div className="mx-auto h-8 w-px bg-amber/40 lg:hidden" />

          <div className="rounded-[14px] border border-rule bg-ink-raised p-6 md:p-7">
            <div className="flex items-baseline justify-between">
              <span className="text-[15px] font-medium text-paper">Moovio</span>
              <span className="mono text-[11px] text-faint">1 player</span>
            </div>
            <ul className="mt-6">
              {outcomes.map(([label, note]) => (
                <li key={label} className="border-t border-rule-soft py-4 first:border-t-0 first:pt-0 last:pb-0">
                  <p className="text-[14px] text-paper">{label}</p>
                  <p className="mt-1.5 text-[13px] leading-[1.55] text-muted">{note}</p>
                </li>
              ))}
            </ul>
          </div>
        </div>

        <p className="mt-10 max-w-[66ch] border-t border-rule-soft pt-5 text-[13px] leading-[1.6] text-faint">
          Aniyomi and Mihon/Keiyoushi repositories can be added and their extensions installed
          and updated today. Running them is still in progress, and the app says so on the
          screen instead of pretending otherwise.
        </p>
      </div>
    </section>
  );
}
