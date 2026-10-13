"use client";

import { useEffect, useRef, type ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * Staggered blurred-rise entrance (transitions-dev `texts-reveal`).
 * Adds `.is-shown` when scrolled into view, once.
 */
export function Stagger({ items, className }: { items: ReactNode[]; className?: string }) {
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (window.matchMedia("(prefers-reduced-motion: reduce)").matches) {
      el.classList.add("is-shown");
      return;
    }
    const io = new IntersectionObserver(
      (entries) => {
        entries.forEach((e) => {
          if (e.isIntersecting) {
            el.classList.add("is-shown");
            io.disconnect();
          }
        });
      },
      { threshold: 0.2 },
    );
    io.observe(el);
    return () => io.disconnect();
  }, []);

  return (
    <div ref={ref} className={cn("t-stagger", className)}>
      {items.map((item, i) => (
        <span key={i} className={cn("t-stagger-line", i > 0 && `t-stagger-line--${Math.min(i + 1, 5)}`)}>
          {item}
        </span>
      ))}
    </div>
  );
}
