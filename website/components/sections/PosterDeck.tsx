"use client";

import { useRef, useState } from "react";

const posters = [
  {
    title: "Nine Rivers",
    tag: "Playing",
    year: "2026",
    runtime: "S1 E4 · 47 min",
    progress: 62,
    detail: "1080p",
    plate:
      "linear-gradient(168deg, #4B3A21 0%, #2A2117 46%, #14110D 100%)",
  },
  {
    title: "Paper Lanterns",
    tag: "Downloaded",
    year: "2026",
    runtime: "Film · 1 h 58",
    progress: 18,
    detail: "offline",
    plate:
      "linear-gradient(168deg, #2E3D34 0%, #1B241E 48%, #0E1210 100%)",
  },
  {
    title: "Salt & Static",
    tag: "Library",
    year: "2026",
    runtime: "S2 E1 · 42 min",
    progress: 84,
    detail: "in progress",
    plate:
      "linear-gradient(168deg, #45292B 0%, #281818 46%, #130E0E 100%)",
  },
];

/**
 * Three posters on a light table: each fully legible, tilted a couple of degrees,
 * straightening when the group is hovered. The artwork is set in type, not faked
 * with empty image boxes — there is no screenshot to show, so the card is the design.
 */
export function PosterDeck() {
  const ref = useRef<HTMLDivElement>(null);
  const [tilt, setTilt] = useState({ x: 0, y: 0 });
  const [lifted, setLifted] = useState(false);

  return (
    <div
      ref={ref}
      className="deck mx-auto w-full max-w-[520px]"
      onPointerMove={(event) => {
        const rect = ref.current?.getBoundingClientRect();
        if (!rect) return;
        setTilt({
          x: (event.clientX - rect.left) / rect.width - 0.5,
          y: (event.clientY - rect.top) / rect.height - 0.5,
        });
      }}
      onPointerEnter={() => setLifted(true)}
      onPointerLeave={() => {
        setLifted(false);
        setTilt({ x: 0, y: 0 });
      }}
      role="img"
      aria-label="Three posters from the library: Nine Rivers, Paper Lanterns, Salt and Static."
    >
      <div
        className="grid grid-cols-3 gap-2.5 transition-transform duration-500 [transform-style:preserve-3d] sm:gap-3"
        style={{
          transform: `rotateX(${-tilt.y * 6}deg) rotateY(${tilt.x * 9}deg)`,
          transitionTimingFunction: "cubic-bezier(0.16, 1, 0.3, 1)",
        }}
      >
        {posters.map((poster, index) => (
          <article
            key={poster.title}
            className="deck-card flex aspect-[2/3] flex-col rounded-[10px] border border-paper/12 bg-ink p-2 sm:p-2.5"
            style={{
              transform: lifted
                ? "translateY(-6px) rotate(0deg)"
                : `translateY(${index === 1 ? 0 : 10}px) rotate(${[-2.2, 0, 2.2][index]}deg)`,
              zIndex: index === 1 ? 2 : 1,
            }}
          >
            <div className="flex items-center justify-between">
              <span className="mono text-[8.5px] uppercase tracking-[0.16em] text-paper/45 sm:text-[9px]">
                {poster.tag}
              </span>
              <span className="mono text-[8.5px] text-paper/30 sm:text-[9px]">
                {String(index + 1).padStart(2, "0")}
              </span>
            </div>

            <div
              className="relative mt-2 flex flex-1 flex-col justify-between overflow-hidden rounded-[6px] border border-paper/[0.07] p-2.5"
              style={{ backgroundImage: poster.plate }}
            >
              <span className="mono text-[8px] uppercase tracking-[0.18em] text-paper/45 sm:text-[9px]">
                Moovio · {poster.year}
              </span>
              <span className="text-[clamp(0.9rem,1.8vw,1.25rem)] font-medium leading-[0.98] tracking-[-0.03em] text-paper">
                {poster.title}
              </span>
              <span className="mono text-[8px] text-paper/50 sm:text-[9px]">
                {poster.runtime}
              </span>
            </div>

            <div className="mt-2 flex items-center justify-between">
              <span className="mono text-[8.5px] text-paper/45 sm:text-[9px]">
                {poster.progress}%
              </span>
              <span className="mono text-[8.5px] text-paper/30 sm:text-[9px]">
                {poster.detail}
              </span>
            </div>
            <div className="mt-1.5 h-px w-full bg-paper/12">
              <div className="h-px bg-amber/80" style={{ width: `${poster.progress}%` }} />
            </div>
          </article>
        ))}
      </div>
    </div>
  );
}
