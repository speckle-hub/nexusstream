"use client";

import { useEffect, useRef } from "react";
import { cn } from "@/lib/utils";

/**
 * Number pop-in (transitions.dev `02`). Each character re-enters with a
 * blurred slide; the trailing pair rides in 1x / 2x behind the rest.
 * Replay contract from the reference file: remove `.is-animating`,
 * re-render the digits, force a reflow, then re-add the class.
 */
export function PopNumber({
  value,
  className,
  replay = false,
}: {
  value: string;
  className?: string;
  /** Re-run the animation every time the figure re-enters view. */
  replay?: boolean;
}) {
  const ref = useRef<HTMLSpanElement>(null);
  const seen = useRef(0);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;

    const chars = [...value];
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)");

    const build = (stagger: boolean) =>
      chars.map((ch, i) => {
        const span = document.createElement("span");
        span.className = "t-digit";
        span.textContent = ch;
        if (stagger) {
          if (i === chars.length - 2) span.dataset.stagger = "1";
          else if (i === chars.length - 1) span.dataset.stagger = "2";
        }
        return span;
      });

    /* Resting state first, so nothing is ever invisible if JS stalls. */
    el.replaceChildren(...build(false));

    const play = () => {
      if (reduce.matches) return;
      el.classList.remove("is-animating");
      el.replaceChildren(...build(true));
      void el.offsetHeight; /* reflow — this is what guarantees the replay */
      el.classList.add("is-animating");
    };

    const io = new IntersectionObserver(
      (entries) => {
        for (const e of entries) {
          if (!e.isIntersecting) continue;
          if (!replay && seen.current > 0) continue;
          seen.current += 1;
          play();
        }
      },
      { threshold: 0.5 },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [value, replay]);

  return (
    <span className={cn("t-digit-group", className)}>
      <span className="sr-only">{value}</span>
      <span ref={ref} aria-hidden />
    </span>
  );
}
