"use client";

import { useEffect, useRef, type ReactNode } from "react";
import { cn } from "@/lib/utils";

/**
 * 3D pointer tilt with cursor glare (transitions-dev `card-tilt`).
 * Pointer is tracked on the flat outer wrapper, never the tilting card.
 */
export function Tilt({
  children,
  className,
  cardClassName,
  max = 10,
  glare = true,
  allowScroll = false,
}: {
  children: ReactNode;
  className?: string;
  cardClassName?: string;
  max?: number;
  glare?: boolean;
  /** Keep vertical page scroll on touch (tilt still follows tap-hold-drag). */
  allowScroll?: boolean;
}) {
  const tiltRef = useRef<HTMLDivElement>(null);
  const cardRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const tilt = tiltRef.current;
    const card = cardRef.current;
    if (!tilt || !card) return;
    const reduce = window.matchMedia("(prefers-reduced-motion: reduce)");
    if (reduce.matches) return;

    const reset = () => {
      tilt.classList.remove("is-hover");
      card.classList.remove("is-tilting");
      card.style.setProperty("--tilt-rx", "0deg");
      card.style.setProperty("--tilt-ry", "0deg");
    };
    const track = (e: PointerEvent) => {
      const r = tilt.getBoundingClientRect();
      const px = Math.min(1, Math.max(0, (e.clientX - r.left) / r.width));
      const py = Math.min(1, Math.max(0, (e.clientY - r.top) / r.height));
      tilt.classList.add("is-hover");
      card.classList.add("is-tilting");
      card.style.setProperty("--tilt-ry", ((px - 0.5) * max).toFixed(2) + "deg");
      card.style.setProperty("--tilt-rx", ((0.5 - py) * max).toFixed(2) + "deg");
      card.style.setProperty("--tilt-gx", (px * 100).toFixed(1) + "%");
      card.style.setProperty("--tilt-gy", (py * 100).toFixed(1) + "%");
    };
    const onDown = (e: PointerEvent) => {
      if (e.pointerType !== "mouse") {
        try {
          tilt.setPointerCapture(e.pointerId);
        } catch {
          /* noop */
        }
      }
    };
    const onLeave = (e: PointerEvent) => {
      if (e.pointerType === "mouse") reset();
    };

    tilt.addEventListener("pointerdown", onDown);
    tilt.addEventListener("pointermove", track);
    tilt.addEventListener("pointerup", reset);
    tilt.addEventListener("pointercancel", reset);
    tilt.addEventListener("pointerleave", onLeave);
    return () => {
      tilt.removeEventListener("pointerdown", onDown);
      tilt.removeEventListener("pointermove", track);
      tilt.removeEventListener("pointerup", reset);
      tilt.removeEventListener("pointercancel", reset);
      tilt.removeEventListener("pointerleave", onLeave);
    };
  }, [max]);

  return (
    <div
      ref={tiltRef}
      className={cn("t-tilt", className)}
      style={allowScroll ? { touchAction: "pan-y" } : undefined}
    >
      <div ref={cardRef} className={cn("t-tilt-card", cardClassName)}>
        {children}
        {glare && <div className="t-tilt-glare" aria-hidden />}
      </div>
    </div>
  );
}
