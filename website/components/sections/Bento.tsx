import { Reveal } from "@/components/motion/Reveal";
import { Spotlight } from "@/components/motion/Spotlight";

/** A four-step mirror race, drawn as rails: the fastest wins, the rest fade. */
function RaceDiagram() {
  return (
    <div aria-hidden className="mt-8 space-y-2.5">
      {[
        { label: "mirror A", width: "92%", win: true },
        { label: "mirror B", width: "54%", win: false },
        { label: "mirror C", width: "36%", win: false },
      ].map((rail) => (
        <div key={rail.label} className="flex items-center gap-3">
          <span className="mono w-[62px] shrink-0 text-[9.5px] uppercase tracking-[0.12em] text-faint">
            {rail.label}
          </span>
          <span className="h-[3px] flex-1 rounded-full bg-rule">
            <span
              className={`block h-[3px] rounded-full ${rail.win ? "bg-amber" : "bg-paper/20"}`}
              style={{ width: rail.width }}
            />
          </span>
          <span
            className={`mono w-[34px] shrink-0 text-right text-[9.5px] ${rail.win ? "text-amber" : "text-faint"}`}
          >
            {rail.win ? "0.8s" : "—"}
          </span>
        </div>
      ))}
    </div>
  );
}

/** A query compiled into constraints before a single request leaves the device. */
function QueryDiagram() {
  return (
    <div aria-hidden className="mt-8 flex flex-wrap gap-2">
      {[
        ["anime", "type"],
        ["2024", "year"],
        ["high rating", "score ≥ 7"],
        ["scifi", "genre"],
      ].map(([input, mapped]) => (
        <span
          key={input}
          className="flex items-center gap-2 rounded-full border border-rule px-3 py-1.5 text-[11px] text-paper/85"
        >
          {input}
          <span className="mono text-[9px] uppercase tracking-[0.1em] text-amber">{mapped}</span>
        </span>
      ))}
    </div>
  );
}

/** Favourite / resume / completed, as a tiny stacked ledger. */
function LibraryDiagram() {
  return (
    <div aria-hidden className="mt-8 space-y-2">
      {[76, 48, 92].map((width, i) => (
        <div key={i} className="flex items-center gap-3">
          <span className="h-6 w-4 rounded-[3px] border border-rule-soft bg-ink" />
          <span className="h-px flex-1 bg-rule">
            <span className="block h-px bg-amber/60" style={{ width: `${width}%` }} />
          </span>
        </div>
      ))}
    </div>
  );
}

/** A download queue with a storage floor. */
function DownloadDiagram() {
  return (
    <div aria-hidden className="mt-8">
      <div className="flex items-center justify-between">
        <span className="mono text-[9.5px] uppercase tracking-[0.14em] text-faint">offline</span>
        <span className="mono text-[9.5px] text-amber">62%</span>
      </div>
      <div className="mt-2 h-[3px] rounded-full bg-rule">
        <div className="h-[3px] w-[62%] rounded-full bg-amber" />
      </div>
      <div className="mt-2 flex items-center justify-between">
        <span className="mono text-[9.5px] text-faint">free space floor</span>
        <span className="mono text-[9.5px] text-faint">1 GB</span>
      </div>
    </div>
  );
}

const cells = [
  {
    title: "Playback that tolerates a bad source",
    body: "Every provider runs under a five-second ceiling and mirrors are raced, so the first playable link wins. If a stream dies mid-episode, failover keeps your position.",
    span: "md:col-span-4",
    diagram: <RaceDiagram />,
  },
  {
    title: "One library, kept on the device",
    body: "Favourites, continue watching, continue reading and watch progress live in local storage. Nothing is uploaded.",
    span: "md:col-span-2",
    diagram: <LibraryDiagram />,
  },
  {
    title: "Downloads that work offline",
    body: "Video and manga chapters download in the background to a folder you choose. A free-space floor stops a full disk from breaking a file.",
    span: "md:col-span-2",
    diagram: <DownloadDiagram />,
  },
  {
    title: "Search that reads the words",
    body: "Type anime 2024 high rating or scifi movies and the query is compiled on the device into type, year, rating and genre before any source is asked.",
    span: "md:col-span-4",
    diagram: <QueryDiagram />,
  },
  {
    title: "A private area that stays private",
    body: "The adult section sits behind a PIN or biometric lock, unlocks for the session only, and stays out of Home, Search and Library by default.",
    span: "md:col-span-3",
    diagram: null,
  },
  {
    title: "The small things, finished",
    body: "Trackers for Trakt, AniList and MyAnimeList, encrypted backups, sleep timer, subtitle styling, Chromecast handoff and a reader with page-turn physics.",
    span: "md:col-span-3",
    diagram: null,
  },
];

export function Bento() {
  return (
    <section id="features" className="border-b border-rule">
      <div className="shell py-24 md:py-28">
        <Reveal>
          <p className="marker mono max-w-[240px] text-[11.5px] text-amber">00:11:38</p>
        </Reveal>

        <div className="mt-6 grid gap-6 lg:grid-cols-[1.15fr_0.85fr] lg:items-end">
          <Reveal delay={40}>
            <h2 className="max-w-[24ch] text-[clamp(1.9rem,3.8vw,3rem)] font-medium leading-[1.08] tracking-[-0.025em] text-paper">
              What the player does when the network does not.
            </h2>
          </Reveal>
          <Reveal delay={100}>
            <p className="max-w-[36ch] text-[14.5px] leading-[1.6] text-muted">
              Six decisions that make a streaming app usable on a phone, not just pleasant in a
              screenshot.
            </p>
          </Reveal>
        </div>

        <div className="mt-12 grid gap-4 md:grid-cols-6">
          {cells.map((cell, i) => (
            <Reveal
              key={cell.title}
              delay={(i % 3) * 70}
              className={cell.span}
            >
              <Spotlight className="flex h-full flex-col rounded-[14px] border border-rule bg-ink-raised p-6">
                <span className="mono text-[11px] text-faint">
                  {String(i + 1).padStart(2, "0")}
                </span>
                <h3 className="mt-4 max-w-[26ch] text-[15.5px] font-medium leading-[1.35] text-paper">
                  {cell.title}
                </h3>
                <p className="mt-2.5 text-[14px] leading-[1.6] text-muted">{cell.body}</p>
                {cell.diagram}
              </Spotlight>
            </Reveal>
          ))}
        </div>
      </div>
    </section>
  );
}
