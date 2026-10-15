const items = [
  "Stremio add-ons",
  "CloudStream providers",
  "Aniyomi extensions",
  "Mihon extensions",
  "TMDB metadata",
  "AniList sync",
  "MangaDex chapters",
  "OpenSubtitles",
  "Media3 ExoPlayer",
  "Offline downloads",
];

function Row({ ariaHidden = false }: { ariaHidden?: boolean }) {
  return (
    <div aria-hidden={ariaHidden} className="flex shrink-0 items-center gap-3 pr-3">
      {items.map((x) => (
        <span
          key={x}
          className="eyebrow whitespace-nowrap rounded-full border border-rule bg-white/[0.03] px-5 py-2.5 text-muted"
        >
          {x}
        </span>
      ))}
    </div>
  );
}

/** Protocol marquee: everything the app speaks, drifting forever. */
export function EcosystemMarquee() {
  return (
    <section id="ecosystem" aria-label="Supported ecosystems" className="border-y border-rule bg-void-deep/60 py-7">
      <div className="marquee mask-fade-x overflow-hidden">
        <div className="marquee-track flex w-max gap-3">
          <Row />
          <Row ariaHidden />
        </div>
      </div>
    </section>
  );
}
