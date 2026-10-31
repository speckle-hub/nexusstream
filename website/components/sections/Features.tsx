const features = [
  {
    title: "Playback that tolerates a bad source",
    body: "Every provider runs under a five-second ceiling and mirrors are raced, so the first playable link wins. If a stream dies mid-episode, failover keeps your position.",
  },
  {
    title: "One library, kept on the device",
    body: "Favourites, continue watching, continue reading and watch progress live in local storage. Nothing is uploaded, and there is no account to lose.",
  },
  {
    title: "Downloads that work offline",
    body: "Video and manga chapters download in the background to a folder you choose. A free-space floor stops a full disk from breaking a download mid-file.",
  },
  {
    title: "Search that reads the words",
    body: "Type anime 2024 high rating or scifi movies and the query is compiled on the device into type, year, rating and genre before any source is asked.",
  },
  {
    title: "A private area that stays private",
    body: "The adult section sits behind a PIN or biometric lock, unlocks for the session only, and its results are excluded from Home, Search and Library by default.",
  },
  {
    title: "The small things, finished",
    body: "Trackers for Trakt, AniList and MyAnimeList, encrypted backups, sleep timer, subtitle styling, Chromecast handoff and a reader with page-turn physics.",
  },
];

export function Features() {
  return (
    <section id="features" className="border-b border-rule">
      <div className="shell py-24 md:py-28">
        <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="mono text-[12px] text-amber">00:11:38</p>
            <h2 className="mt-5 max-w-[22ch] text-[clamp(1.9rem,3.6vw,2.9rem)] font-medium leading-[1.1] tracking-[-0.02em] text-paper">
              What the player does when the network does not.
            </h2>
          </div>
          <p className="max-w-[34ch] text-[14px] leading-[1.6] text-muted">
            Six things that decide whether a streaming app is usable on a phone, not just
            pleasant in a screenshot.
          </p>
        </div>

        <div className="mt-14 grid gap-x-10 gap-y-0 sm:grid-cols-2 lg:grid-cols-3">
          {features.map((f, i) => (
            <article key={f.title} className="border-t border-rule py-7">
              <p className="mono text-[11px] text-faint">
                {String(i + 1).padStart(2, "0")}
              </p>
              <h3 className="mt-3 text-[15.5px] font-medium leading-[1.35] text-paper">
                {f.title}
              </h3>
              <p className="mt-2.5 text-[14px] leading-[1.6] text-muted">{f.body}</p>
            </article>
          ))}
        </div>
      </div>
    </section>
  );
}
