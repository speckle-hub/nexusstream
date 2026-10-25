const items = [
  "Stremio add-ons",
  "Cinemeta metadata",
  "AniList sync",
  "libmpv playback",
  "PIN-gated NSFW",
  "Local library",
  "Hardware decode",
  "Windows native",
  "Keyboard first",
];

function Row({ ariaHidden = false }: { ariaHidden?: boolean }) {
  return (
    <div aria-hidden={ariaHidden} className="flex shrink-0 items-center gap-3 pr-3">
      {items.map((x) => (
        <span
          key={x}
          className="eyebrow whitespace-nowrap rounded-xl border border-rule bg-white/[0.03] px-5 py-2.5 text-muted"
        >
          {x}
        </span>
      ))}
    </div>
  );
}

/** Protocol marquee: everything the desktop app speaks, drifting forever. */
export function EcosystemMarquee() {
  return (
    <section id="ecosystem" aria-label="Supported ecosystems" className="border-y border-rule bg-void-deep/40 py-7">
      <div className="relative">
        <div
          aria-hidden
          className="absolute inset-y-0 left-0 w-24 bg-gradient-to-r from-void to-transparent"
        />
        <div
          aria-hidden
          className="absolute inset-y-0 right-0 w-24 bg-gradient-to-l from-void to-transparent"
        />
        <div className="marquee mask-fade-x overflow-hidden">
          <div className="marquee-track flex w-max gap-3">
            <Row />
            <Row ariaHidden />
          </div>
        </div>
      </div>
    </section>
  );
}
