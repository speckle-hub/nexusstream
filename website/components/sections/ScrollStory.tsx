"use client";

import { useEffect, useRef, useState } from "react";
import { Reveal } from "@/components/motion/Reveal";

const steps = [
  {
    time: "00:18:05",
    title: "Browse one grid",
    body: "Posters from every enabled source, deduplicated and ranked. Filters are local, so a slow provider cannot hold the grid.",
    points: ["Movies · Series · Anime · Manga", "Continue watching on top", "One poster, many sources"],
    surface: "home",
  },
  {
    time: "00:22:41",
    title: "Play the link that answers",
    body: "Sources are asked in parallel and the first playable stream wins. Quality, subtitles and audio tracks are yours to keep.",
    points: ["ExoPlayer with HLS and DASH", "Failover keeps your position", "Subtitle styling that sticks"],
    surface: "player",
  },
  {
    time: "00:26:09",
    title: "Read without leaving",
    body: "Manga chapters open in the same library: right-to-left paging, double-page spreads and a warm, low-contrast reader for long sessions.",
    points: ["RTL and double-page", "Panels scale with the phone", "Progress saved per chapter"],
    surface: "reader",
  },
];

function HomeSurface() {
  return (
    <div className="absolute inset-0 flex flex-col">
      <div className="flex items-center justify-between border-b border-rule-soft px-4 py-2.5">
        <span className="text-[11px] text-paper">Movies</span>
        <span className="mono text-[9px] text-faint">142 titles</span>
      </div>
      <div className="grid flex-1 grid-cols-4 gap-1.5 p-3">
        {[
          "#241F18",
          "#1D211E",
          "#231A1B",
          "#1B1E22",
          "#22201A",
          "#1A1D1C",
          "#211C20",
          "#1E211A",
        ].map((tone, i) => (
          <div
            key={i}
            className="relative overflow-hidden rounded-[4px] border border-rule-soft"
            style={{ background: `linear-gradient(180deg, ${tone} 0%, #0B0B0A 100%)` }}
          >
            {/* A title and a meta line, so a poster tile reads as a title, not a swatch. */}
            <div className="absolute inset-x-1.5 bottom-2 space-y-1">
              <div className="h-[2px] w-3/4 rounded-full bg-paper/30" />
              <div className="h-[2px] w-1/2 rounded-full bg-paper/15" />
            </div>
            {i === 0 ? (
              <div className="absolute inset-x-0 top-0 h-[2px] bg-amber/80" />
            ) : null}
          </div>
        ))}
      </div>
      <div className="border-t border-rule-soft px-4 py-2.5">
        <div className="h-px w-full bg-rule">
          <div className="h-px w-1/3 bg-amber/70" />
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
      <div className="absolute left-3 top-3 rounded-full bg-amber px-2 py-[3px] text-[9px] font-medium text-ink">
        Play
      </div>
      <div className="absolute inset-x-0 bottom-0 p-4">
        <div className="flex items-center gap-3">
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-paper text-[10px] font-medium text-ink">
            II
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
        {[0, 1].map((page) => (
          <div
            key={page}
            className="flex-1 rounded-[3px] border border-rule-soft bg-[#141412] p-3"
          >
            <div className="space-y-1.5">
              {[92, 88, 94, 72, 90, 86, 78, 92, 84, 90, 66].map((width, line) => (
                <div key={line} className="h-px bg-paper/15" style={{ width: `${width}%` }} />
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

const surfaces = {
  home: <HomeSurface />,
  player: <PlayerSurface />,
  reader: <ReaderSurface />,
} as const;

/**
 * The sticky story.
 *
 * The phone stays in place while three explanations scroll past; an IntersectionObserver with
 * a centre band decides which surface is shown. On narrow screens the sticky column is
 * disabled and the surfaces still swap as the steps pass.
 */
export function ScrollStory() {
  const [active, setActive] = useState(0);
  const stepRefs = useRef<Array<HTMLLIElement | null>>([]);

  useEffect(() => {
    const observers = stepRefs.current.map((element, index) => {
      if (!element) return null;
      const observer = new IntersectionObserver(
        (entries) => {
          if (entries.some((entry) => entry.isIntersecting)) setActive(index);
        },
        { rootMargin: "-42% 0px -42% 0px", threshold: 0 },
      );
      observer.observe(element);
      return observer;
    });
    return () => observers.forEach((observer) => observer?.disconnect());
  }, []);

  return (
    <section id="screens" className="border-b border-rule">
      <div className="shell py-24 md:py-28">
        <Reveal>
          <p className="marker mono max-w-[240px] text-[11.5px] text-amber">00:18:05</p>
        </Reveal>

        <Reveal delay={40}>
          <h2 className="mt-6 max-w-[24ch] text-[clamp(1.9rem,3.8vw,3rem)] font-medium leading-[1.08] tracking-[-0.025em] text-paper">
            Three screens you will actually spend time in.
          </h2>
        </Reveal>

        <div className="mt-14 grid gap-12 lg:grid-cols-[0.92fr_1.08fr] lg:gap-16">
          <div className="lg:sticky lg:top-24 lg:self-start">
            <div className="mx-auto w-full max-w-[308px]">
              <div className="rounded-[34px] border border-rule bg-ink-raised p-2 shadow-[0_40px_80px_-56px_rgba(0,0,0,0.95)]">
                <div className="relative aspect-[9/16.4] overflow-hidden rounded-[26px] border border-rule-soft bg-ink">
                  {Object.entries(surfaces).map(([key, node]) => (
                    <div
                      key={key}
                      className="surface"
                      data-active={steps[active]?.surface === key}
                      aria-hidden={steps[active]?.surface !== key}
                    >
                      {node}
                    </div>
                  ))}
                </div>
              </div>

              <div className="mt-6 flex items-center justify-center gap-2" aria-hidden>
                {steps.map((step, index) => (
                  <span
                    key={step.time}
                    className="step-dot h-[2px] w-6 rounded-full bg-rule"
                    data-active={index === active}
                  />
                ))}
              </div>
            </div>
          </div>

          <ol className="space-y-14 lg:space-y-[38vh] lg:pb-[26vh]">
            {steps.map((step, index) => (
              <li
                key={step.time}
                ref={(element) => {
                  stepRefs.current[index] = element;
                }}
                className="border-t border-rule pt-6 lg:min-h-[30vh]"
              >
                <p className="mono text-[11.5px] text-amber">{step.time}</p>
                <h3 className="mt-4 text-[clamp(1.3rem,2.4vw,1.75rem)] font-medium leading-[1.2] tracking-[-0.02em] text-paper">
                  {step.title}
                </h3>
                <p className="mt-3 max-w-[46ch] text-[14.5px] leading-[1.65] text-muted">
                  {step.body}
                </p>
                <ul className="mt-6 space-y-2.5">
                  {step.points.map((point) => (
                    <li key={point} className="flex items-baseline gap-3 text-[13.5px] text-paper/80">
                      <span aria-hidden className="h-px w-4 shrink-0 translate-y-[-4px] bg-amber/70" />
                      {point}
                    </li>
                  ))}
                </ul>
              </li>
            ))}
          </ol>
        </div>
      </div>
    </section>
  );
}
