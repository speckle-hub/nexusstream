"use client";

import {
  useCallback,
  useEffect,
  useRef,
  useState,
  type PointerEvent as ReactPointerEvent,
  type ReactNode,
} from "react";
import { animate, m, useMotionValue } from "framer-motion";
import { ChevronLeft, ChevronRight } from "lucide-react";
import { cn } from "@/lib/utils";

/* ────────────────────────────────────────────────────────────────
   A rail that behaves like a physical object.

   Every rule below is apple-design translated to the pointer:
   §2  1:1 tracking — the track is glued to the pointer.
   §3  interruptibility — pointer-down reads the live on-screen
       value and any in-flight spring is stopped, so a rail grabbed
       mid-flight continues from where it actually is.
   §6  momentum projection — release velocity chooses the resting
       point, not the release point.
   §9  rubber-banding — the ends resist progressively instead of
       stopping hard.
   §5  velocity handoff — the settle spring starts at the finger's
       release velocity, so there is no seam between drag and coast.
   ──────────────────────────────────────────────────────────────── */

const DECELERATION = 0.998;

/** Apple's exponential-decay projection (not the physics-textbook form). */
function project(velocityPxPerSec: number) {
  return (velocityPxPerSec / 1000) * (DECELERATION / (1 - DECELERATION));
}

function rubberband(overshoot: number, dimension: number, constant = 0.55) {
  return (
    (overshoot * dimension * constant) /
    (dimension + constant * Math.abs(overshoot))
  );
}

const clamp = (v: number, min: number, max: number) =>
  Math.min(max, Math.max(min, v));

/** Distance the pointer must travel before we call it a drag, not a tap. */
const HYSTERESIS = 8;

export function DragRail({
  children,
  step,
  className,
  label,
}: {
  children: ReactNode;
  /** Card pitch used by the arrow buttons (card width + gap). */
  step: number;
  className?: string;
  label: string;
}) {
  const viewportRef = useRef<HTMLDivElement>(null);
  const trackRef = useRef<HTMLDivElement>(null);

  const x = useMotionValue(0);
  const spring = useRef<ReturnType<typeof animate> | null>(null);

  const bounds = useRef({ min: 0, max: 0 });
  const drag = useRef({
    active: false,
    pointerId: -1,
    originX: 0,
    startX: 0,
    samples: [] as { t: number; x: number }[],
    moved: 0,
  });
  const dragged = useRef(false);

  const [index, setIndex] = useState(0);
  const [cards, setCards] = useState(0);
  const [canPrev, setCanPrev] = useState(false);
  const [canNext, setCanNext] = useState(true);

  const measure = useCallback(() => {
    const viewport = viewportRef.current;
    const track = trackRef.current;
    if (!viewport || !track) return;

    const min = Math.min(0, viewport.clientWidth - track.scrollWidth);
    bounds.current = { min, max: 0 };

    const current = clamp(x.get(), min, 0);
    if (current !== x.get()) x.set(current);

    const width = track.children.length ? track.scrollWidth : 0;
    const perView = Math.max(1, Math.round(viewport.clientWidth / step));
    setCards(Math.max(0, track.children.length - perView));
    setCanPrev(current < -1);
    setCanNext(current > min + 1);
    setIndex(
      width ? Math.round(Math.abs(current) / step) : 0,
    );
  }, [step, x]);

  useEffect(() => {
    measure();
    const viewport = viewportRef.current;
    const track = trackRef.current;
    if (!viewport || !track) return;

    const ro = new ResizeObserver(measure);
    ro.observe(viewport);
    ro.observe(track);
    window.addEventListener("resize", measure);
    return () => {
      ro.disconnect();
      window.removeEventListener("resize", measure);
      spring.current?.stop();
    };
  }, [measure]);

  /* Subscribe to the motion value so the arrows stay in sync while a
     spring is still coasting. */
  useEffect(
    () =>
      x.on("change", (v) => {
        const { min } = bounds.current;
        setCanPrev(v < min + 1 || v < -1);
        setCanNext(v > min + 1);
      }),
    [x],
  );

  const onPointerDown = (e: ReactPointerEvent<HTMLDivElement>) => {
    if (e.pointerType === "mouse" && e.button !== 0) return;

    /* §3 — read the presentation value: stop the spring where it is. */
    spring.current?.stop();
    spring.current = null;

    const viewport = viewportRef.current;
    viewport?.setPointerCapture(e.pointerId);

    drag.current = {
      active: true,
      pointerId: e.pointerId,
      originX: e.clientX,
      startX: x.get(),
      samples: [{ t: performance.now(), x: x.get() }],
      moved: 0,
    };
  };

  const onPointerMove = (e: ReactPointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d.active || d.pointerId !== e.pointerId) return;

    const raw = d.startX + (e.clientX - d.originX);
    d.moved = Math.max(d.moved, Math.abs(e.clientX - d.originX));

    const { min, max } = bounds.current;
    const width = viewportRef.current?.clientWidth ?? 0;

    /* §9 — past the end, follow less and less instead of stopping. */
    let next = raw;
    if (raw > max) next = max + rubberband(raw - max, width);
    else if (raw < min) next = min + rubberband(raw - min, width);

    x.set(next);

    /* Keep a short history so release has real velocity (§5). */
    const now = performance.now();
    d.samples.push({ t: now, x: next });
    if (d.samples.length > 6) d.samples.shift();

    if (d.moved > HYSTERESIS) dragged.current = true;
  };

  const settle = (velocity: number) => {
    const { min, max } = bounds.current;
    /* §6 — aim at where the gesture was going, not where it stopped. */
    const target = clamp(x.get() + project(velocity), min, max);

    spring.current = animate(x, target, {
      type: "spring",
      bounce: 0,
      duration: 0.5,
      velocity,
      onComplete: () => {
        spring.current = null;
        const v = x.get();
        setCanPrev(v < min + 1 || v < -1);
        setCanNext(v > min + 1);
      },
    });
  };

  const onPointerUp = (e: ReactPointerEvent<HTMLDivElement>) => {
    const d = drag.current;
    if (!d.active || d.pointerId !== e.pointerId) return;
    d.active = false;

    try {
      viewportRef.current?.releasePointerCapture(e.pointerId);
    } catch {
      /* pointer may already be gone */
    }

    const s = d.samples;
    let velocity = 0;
    if (s.length >= 2) {
      const first = s[0];
      const last = s[s.length - 1];
      const dt = (last.t - first.t) / 1000;
      if (dt > 0.008) velocity = (last.x - first.x) / dt;
    }

    settle(velocity);
    d.samples = [];
  };

  /* A drag must not also fire the click it started on (§10). */
  const onClickCapture = (e: React.MouseEvent) => {
    if (dragged.current) {
      e.preventDefault();
      e.stopPropagation();
      dragged.current = false;
    }
  };

  const nudge = (direction: 1 | -1) => {
    spring.current?.stop();
    const { min, max } = bounds.current;
    const target = clamp(x.get() + direction * step, min, max);
    spring.current = animate(x, target, {
      type: "spring",
      bounce: 0,
      duration: 0.4,
      onComplete: () => {
        spring.current = null;
      },
    });
  };

  const atStart = !canPrev;
  const atEnd = !canNext;

  return (
    <div className={cn("relative", className)}>
      <div
        ref={viewportRef}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={onPointerUp}
        onPointerCancel={onPointerUp}
        onClickCapture={onClickCapture}
        className="cursor-grab overflow-hidden active:cursor-grabbing"
        style={{ touchAction: "pan-y" }}
        role="group"
        aria-label={label}
      >
        <m.div
          ref={trackRef}
          className="flex will-change-transform"
          style={{ x, gap: "1.5rem" }}
        >
          {children}
        </m.div>
      </div>

      {/* Controls — the rail stays operable without a pointer. */}
      <div className="mt-8 flex items-center gap-3">
        <button
          type="button"
          onClick={() => nudge(-1)}
          disabled={atStart}
          aria-label={`Scroll ${label} backwards`}
          className="press flex h-11 w-11 items-center justify-center rounded-full border border-white/10 text-paper transition-colors hover:border-accent hover:bg-accent hover:text-paper disabled:pointer-events-none disabled:opacity-25"
        >
          <ChevronLeft className="h-5 w-5" />
        </button>
        <button
          type="button"
          onClick={() => nudge(1)}
          disabled={atEnd}
          aria-label={`Scroll ${label} forwards`}
          className="press flex h-11 w-11 items-center justify-center rounded-full border border-white/10 text-paper transition-colors hover:border-accent hover:bg-accent hover:text-paper disabled:pointer-events-none disabled:opacity-25"
        >
          <ChevronRight className="h-5 w-5" />
        </button>

        <div className="ml-3 flex-1">
          <div className="relative h-px w-full max-w-[220px] bg-white/10">
            <div
              className="absolute inset-y-0 left-0 bg-accent transition-[width] duration-300 ease-out"
              style={{
                width: `${
                  cards <= 0
                    ? 100
                    : Math.min(100, ((index + 1) / (cards + 1)) * 100)
                }%`,
              }}
            />
          </div>
        </div>

        <span className="eyebrow hidden text-[10px] text-muted sm:block">
          Drag
        </span>
      </div>
    </div>
  );
}
