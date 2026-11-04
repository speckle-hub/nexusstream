"use client";

import { useEffect, useRef, useState } from "react";

/**
 * SlidingNumber — counts up once, when it first becomes visible.
 *
 * The final value is rendered immediately for reduced-motion users and for anyone whose
 * observer never fires, so the number is never missing from the page.
 */
export function SlidingNumber({
  value,
  decimals = 0,
  durationMs = 1600,
  prefix = "",
  suffix = "",
}: {
  value: number;
  decimals?: number;
  durationMs?: number;
  prefix?: string;
  suffix?: string;
}) {
  const ref = useRef<HTMLSpanElement>(null);
  const [display, setDisplay] = useState(value);
  const done = useRef(false);

  useEffect(() => {
    const el = ref.current;
    if (!el || done.current) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      done.current = true;
      return;
    }

    let frame = 0;
    const observer = new IntersectionObserver(
      (entries) => {
        if (!entries.some((entry) => entry.isIntersecting)) return;
        observer.disconnect();
        done.current = true;
        setDisplay(0);
        const start = performance.now();
        const tick = (now: number) => {
          const progress = Math.min(1, (now - start) / durationMs);
          const eased = 1 - Math.pow(1 - progress, 3);
          setDisplay(value * eased);
          if (progress < 1) frame = requestAnimationFrame(tick);
        };
        frame = requestAnimationFrame(tick);
      },
      { threshold: 0.4 },
    );
    observer.observe(el);
    return () => {
      observer.disconnect();
      cancelAnimationFrame(frame);
    };
  }, [value, durationMs]);

  return (
    <span ref={ref} className="num">
      {prefix}
      {display.toFixed(decimals)}
      {suffix}
    </span>
  );
}
