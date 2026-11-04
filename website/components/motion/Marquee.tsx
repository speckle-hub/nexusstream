/**
 * Marquee — one continuous track, duplicated once, translated by exactly half its width.
 *
 * Pure CSS (see `.marquee` in globals.css): no layout reads, no JS on scroll, paused on
 * hover and stopped entirely under reduced motion. Marked aria-hidden because the same names
 * appear as real text in the section below it.
 */
import type { CSSProperties } from "react";

export function Marquee({
  items,
  durationSeconds = 48,
  className = "",
}: {
  items: string[];
  durationSeconds?: number;
  className?: string;
}) {
  return (
    <div className={`marquee ${className}`} aria-hidden="true">
      <div
        className="marquee__track"
        style={{ "--marquee-duration": `${durationSeconds}s` } as CSSProperties}
      >
        {[0, 1].map((copy) => (
          <div key={copy} className="flex shrink-0 items-center">
            {items.map((item) => (
              <span
                key={`${copy}-${item}`}
                className="mono flex items-center gap-6 whitespace-nowrap px-3 text-[11.5px] uppercase tracking-[0.18em] text-faint"
              >
                {item}
                <span className="h-1 w-1 rounded-full bg-amber/50" />
              </span>
            ))}
          </div>
        ))}
      </div>
    </div>
  );
}
