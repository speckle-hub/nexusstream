"use client";

import { useEffect, useRef } from "react";
import { cn } from "@/lib/utils";

/**
 * Number pop-in (transitions-dev): each character re-enters with a blurred
 * slide when scrolled into view; the last two ride in staggered behind.
 */
export function StatNumber({ value, className }: { value: string; className?: string }) {
  const ref = useRef<HTMLSpanElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    const play = () => {
      el.classList.remove("is-animating");
      void el.offsetHeight; // reflow so the animation replays
      el.classList.add("is-animating");
    };
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) return;
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            play();
            io.disconnect();
          }
        });
      },
      { threshold: 0.4 },
    );
    io.observe(el);
    return () => io.disconnect();
  }, [value]);

  const chars = value.split("");
  return (
    <span ref={ref} className={cn("t-digit-group", className)}>
      {chars.map((ch, i) => (
        <span
          key={i}
          className="t-digit"
          data-stagger={i === chars.length - 2 ? "1" : i === chars.length - 1 ? "2" : undefined}
        >
          {ch}
        </span>
      ))}
    </span>
  );
}
