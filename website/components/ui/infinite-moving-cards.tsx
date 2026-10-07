"use client";

import { useEffect, useState } from "react";
import { cn } from "@/lib/utils";

/** Aceternity-style infinite horizontal marquee. */
export function InfiniteMovingCards({
  items,
  direction = "forwards",
  speed = "slow",
  className,
}: {
  items: { icon: React.ReactNode; name: string; detail: string }[];
  direction?: "forwards" | "reverse";
  speed?: "fast" | "normal" | "slow";
  className?: string;
}) {
  const containerRef = { current: null as HTMLDivElement | null };
  const [start, setStart] = useState(false);

  useEffect(() => setStart(true), []);

  const duration = speed === "fast" ? "20s" : speed === "normal" ? "40s" : "80s";

  return (
    <div
      ref={(el) => { containerRef.current = el; }}
      className={cn("mask-fade-x relative w-full overflow-hidden", className)}
      style={{ ["--animation-duration" as string]: duration, ["--animation-direction" as string]: direction === "forwards" ? "forwards" : "reverse" }}
    >
      <div className={cn("flex w-max min-w-full shrink-0 items-stretch gap-4 py-2", start && "animate-scroll-x")}>
        {[...items, ...items].map((item, i) => (
          <div
            key={`${item.name}-${i}`}
            className="glass flex w-[260px] shrink-0 items-center gap-3.5 rounded-2xl px-5 py-4"
          >
            <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-accent/15 text-accent-soft">
              {item.icon}
            </div>
            <div className="min-w-0">
              <div className="truncate font-display text-sm font-semibold text-mist">{item.name}</div>
              <div className="truncate text-xs text-muted">{item.detail}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
