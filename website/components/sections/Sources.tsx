import { ArrowDown } from "lucide-react";
import { SFW_SOURCE_COUNT } from "@/lib/utils";
import { Reveal } from "@/components/motion/Reveal";
import { Spotlight } from "@/components/motion/Spotlight";

const sources = [
  {
    index: "01",
    name: "Stremio add-ons",
    note: "Any manifest URL. Cinemeta and OpenSubtitles ship by default; one failing add-on never takes the list down.",
    tag: "manifest.json",
  },
  {
    index: "02",
    name: "CloudStream extensions",
    note: ".cs3 providers download, install and run in-process, so the sites you already know become playable sources.",
    tag: ".cs3",
  },
  {
    index: "03",
    name: "Built-in scrapers",
    note: `${SFW_SOURCE_COUNT} movie, TV and anime sources compiled into the app, so Play works with nothing installed.`,
    tag: `${SFW_SOURCE_COUNT} sources`,
  },
];

export function Sources() {
  return (
    <section id="sources" className="border-b border-rule">
      <div className="shell py-24 md:py-28">
        <Reveal>
          <p className="marker mono max-w-[240px] text-[11.5px] text-amber">00:04:12</p>
        </Reveal>

        <div className="mt-6 grid gap-6 lg:grid-cols-[1.1fr_0.9fr] lg:items-end">
          <Reveal delay={40}>
            <h2 className="max-w-[26ch] text-[clamp(1.9rem,3.8vw,3rem)] font-medium leading-[1.08] tracking-[-0.025em] text-paper">
              The sources you already have. One place to play them.
            </h2>
          </Reveal>
          <Reveal delay={100}>
            <p className="max-w-[52ch] text-[15px] leading-[1.65] text-muted">
              Moovio does not host a catalogue. It connects the ones you point it at, then gets
              out of the way: one search across all of them, one library, one player.
            </p>
          </Reveal>
        </div>

        <div className="mt-14 grid gap-4 md:grid-cols-3">
          {sources.map((source, i) => (
            <Reveal key={source.name} delay={i * 90}>
              <Spotlight className="h-full rounded-[14px] border border-rule bg-ink-raised p-6">
                <div className="flex items-baseline justify-between">
                  <span className="mono text-[11px] text-faint">{source.index}</span>
                  <span className="mono rounded-full border border-rule px-2 py-[3px] text-[10px] text-muted">
                    {source.tag}
                  </span>
                </div>
                <h3 className="mt-10 text-[16px] font-medium leading-snug text-paper">
                  {source.name}
                </h3>
                <p className="mt-3 text-[14px] leading-[1.6] text-muted">{source.note}</p>
              </Spotlight>
            </Reveal>
          ))}
        </div>

        {/* The convergence: three inputs, one queue. */}
        <Reveal delay={120}>
          <div className="mt-6 flex items-center justify-center gap-3 py-2 text-faint" aria-hidden>
            <span className="mono text-[10px] uppercase tracking-[0.18em]">merge</span>
            <ArrowDown className="h-3.5 w-3.5 text-amber" strokeWidth={1.5} />
          </div>
        </Reveal>

        <Reveal delay={160}>
          <div className="grid gap-6 rounded-[14px] border border-rule bg-ink-raised p-6 md:grid-cols-[0.8fr_1.2fr] md:items-center md:p-8">
            <div>
              <p className="text-[15px] font-medium text-paper">What reaches the screen</p>
              <p className="mt-2 max-w-[34ch] text-[13.5px] leading-[1.6] text-muted">
                Ranked on quality, then reliability. Dead links are dropped before you see them.
              </p>
            </div>
            <ul className="grid gap-3 sm:grid-cols-3">
              {[
                ["One search", "Every enabled source, one list"],
                ["One library", "Favourites and resume, on device"],
                ["One player", "ExoPlayer, your tracks and subs"],
              ].map(([label, note]) => (
                <li key={label} className="rounded-[10px] border border-rule-soft p-4">
                  <p className="text-[13.5px] text-paper">{label}</p>
                  <p className="mt-1.5 text-[12.5px] leading-[1.55] text-muted">{note}</p>
                </li>
              ))}
            </ul>
          </div>
        </Reveal>

        <Reveal delay={120}>
          <p className="mt-8 max-w-[68ch] border-t border-rule-soft pt-5 text-[13px] leading-[1.6] text-faint">
            Aniyomi and Mihon/Keiyoushi repositories can be added and their extensions installed
            and updated today. Running them is still in progress, and the app says so on the
            screen instead of pretending otherwise.
          </p>
        </Reveal>
      </div>
    </section>
  );
}
