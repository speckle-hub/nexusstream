import { Pause, Play } from "lucide-react";

function Frame({
  label,
  note,
  children,
}: {
  label: string;
  note: string;
  children: React.ReactNode;
}) {
  return (
    <figure className="min-w-0">
      <div className="overflow-hidden rounded-[12px] border border-rule bg-ink-raised">
        <div className="relative aspect-[16/10] w-full">{children}</div>
      </div>
      <figcaption className="mt-4 flex flex-wrap items-baseline gap-x-3 gap-y-1">
        <span className="text-[13.5px] text-paper">{label}</span>
        <span className="mono text-[11px] text-faint">{note}</span>
      </figcaption>
    </figure>
  );
}

function HomeSurface() {
  return (
    <div className="absolute inset-0 flex flex-col">
      <div className="flex items-center justify-between border-b border-rule-soft px-4 py-2.5">
        <span className="text-[11px] text-paper">Movies</span>
        <span className="mono text-[9px] text-faint">142 titles</span>
      </div>
      <div className="grid flex-1 grid-cols-4 gap-1.5 p-3">
        {["#1B1C1F", "#201C17", "#181A18", "#1D1A1A", "#191B1D", "#1F1B16", "#1A191B", "#1C1E1B"].map(
          (tone, i) => (
            <div
              key={i}
              className="rounded-[4px] border border-rule-soft"
              style={{ background: `linear-gradient(180deg, ${tone} 0%, #0B0B0A 100%)` }}
            />
          ),
        )}
      </div>
      <div className="border-t border-rule-soft px-4 py-2">
        <div className="h-px w-full bg-rule">
          <div className="h-px w-1/3 bg-amber/60" />
        </div>
      </div>
    </div>
  );
}

function PlayerSurface() {
  return (
    <div className="absolute inset-0">
      <div
        className="absolute inset-0"
        style={{
          background:
            "radial-gradient(110% 90% at 70% 10%, #3A2A19 0%, #171310 50%, #0A0A09 100%)",
        }}
      />
      <div className="absolute inset-x-0 bottom-0 p-4">
        <div className="flex items-center gap-3">
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-paper text-ink">
            <Pause className="h-3.5 w-3.5" strokeWidth={2} />
          </span>
          <div className="flex-1">
            <div className="h-px w-full bg-paper/20">
              <div className="h-px w-[42%] bg-amber" />
            </div>
          </div>
          <span className="mono text-[9.5px] text-paper/80">24:16 / 47:32</span>
        </div>
        <div className="mt-3 flex items-center justify-between">
          <span className="text-[10px] text-paper/80">Aurora Protocol · S1 E4</span>
          <span className="mono rounded-[4px] border border-paper/15 px-1.5 py-0.5 text-[9px] text-paper/70">
            1080p
          </span>
        </div>
      </div>
    </div>
  );
}

function ReaderSurface() {
  return (
    <div className="absolute inset-0 flex flex-col">
      <div className="flex items-center justify-between border-b border-rule-soft px-4 py-2.5">
        <span className="text-[11px] text-paper">Chapter 12</span>
        <span className="mono text-[9px] text-faint">RTL</span>
      </div>
      <div className="flex flex-1 items-stretch gap-1.5 p-3">
        {[0, 1].map((i) => (
          <div
            key={i}
            className="flex-1 rounded-[3px] border border-rule-soft bg-[#141412] p-3"
          >
            <div className="space-y-1.5">
              {[92, 88, 94, 72, 90, 86, 78, 92, 84, 90, 66].map((w, j) => (
                <div
                  key={j}
                  className="h-px bg-paper/15"
                  style={{ width: `${w}%` }}
                />
              ))}
            </div>
          </div>
        ))}
      </div>
      <div className="border-t border-rule-soft px-4 py-2">
        <span className="mono text-[9px] text-faint">Page 12 / 40</span>
      </div>
    </div>
  );
}

export function Showcase() {
  return (
    <section id="showcase" className="border-b border-rule">
      <div className="shell py-24 md:py-28">
        <div className="flex flex-col gap-5 md:flex-row md:items-end md:justify-between">
          <div>
            <p className="mono text-[12px] text-amber">00:18:05</p>
            <h2 className="mt-5 max-w-[22ch] text-[clamp(1.9rem,3.6vw,2.9rem)] font-medium leading-[1.1] tracking-[-0.02em] text-paper">
              Three screens you will actually spend time in.
            </h2>
          </div>
          <p className="max-w-[32ch] text-[14px] leading-[1.6] text-muted">
            Drawn from the app itself: the grid, the player chrome and the manga reader.
          </p>
        </div>

        <div className="mt-14 grid gap-8 md:grid-cols-3">
          <Frame label="Home feed" note="Movies · 142 titles">
            <HomeSurface />
          </Frame>
          <Frame label="Player" note="ExoPlayer · 1080p">
            <PlayerSurface />
          </Frame>
          <Frame label="Manga reader" note="Right to left · page 12 of 40">
            <ReaderSurface />
          </Frame>
        </div>
      </div>
    </section>
  );
}
