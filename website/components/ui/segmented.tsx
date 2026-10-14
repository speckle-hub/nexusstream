"use client";

import {
  useEffect,
  useLayoutEffect,
  useRef,
  useState,
  type ReactNode,
} from "react";
import { m, AnimatePresence } from "framer-motion";
import { cn } from "@/lib/utils";

/**
 * Segmented control with a sliding pill (transitions.dev `16`).
 * JS only ever writes the active tab's offsetLeft / offsetWidth; CSS owns
 * the tween. First paint and resize are written with the transition
 * suspended, so the pill never animates in from zero.
 */
export function Segmented<T extends string>({
  tabs,
  value,
  onChange,
  panel,
}: {
  tabs: readonly T[];
  value: T;
  onChange: (v: T) => void;
  panel: ReactNode;
}) {
  const barRef = useRef<HTMLDivElement>(null);
  const pillRef = useRef<HTMLSpanElement>(null);
  const tabRefs = useRef<(HTMLButtonElement | null)[]>([]);
  const first = useRef(true);

  const place = (animate: boolean) => {
    const index = tabs.indexOf(value);
    const tab = tabRefs.current[index];
    const pill = pillRef.current;
    if (!tab || !pill) return;

    if (!animate) {
      const prev = pill.style.transition;
      pill.style.transition = "none";
      pill.style.transform = `translateX(${tab.offsetLeft}px)`;
      pill.style.width = `${tab.offsetWidth}px`;
      void pill.offsetWidth; /* reflow — snap before any tween can run */
      pill.style.transition = prev;
      return;
    }
    pill.style.transform = `translateX(${tab.offsetLeft}px)`;
    pill.style.width = `${tab.offsetWidth}px`;
  };

  useLayoutEffect(() => {
    place(!first.current);
    first.current = false;
    /* eslint-disable-next-line react-hooks/exhaustive-deps */
  }, [value]);

  useLayoutEffect(() => {
    place(false);
    /* eslint-disable-next-line react-hooks/exhaustive-deps */
  }, []);

  useEffect(() => {
    const onResize = () => place(false);
    window.addEventListener("resize", onResize);
    return () => window.removeEventListener("resize", onResize);
    /* eslint-disable-next-line react-hooks/exhaustive-deps */
  }, [value]);

  return (
    <div>
      <div
        ref={barRef}
        className="t-tabs"
        role="tablist"
        aria-label="Ecosystem categories"
      >
        <span ref={pillRef} className="t-tabs-pill" aria-hidden />
        {tabs.map((t, i) => (
          <button
            key={t}
            ref={(el) => {
              tabRefs.current[i] = el;
            }}
            type="button"
            role="tab"
            aria-selected={value === t}
            onClick={() => onChange(t)}
            className="t-tab"
          >
            {t}
          </button>
        ))}
      </div>

      <div className="relative mt-9 min-h-[268px]">
        <AnimatePresence mode="wait">
          <m.div
            key={value}
            initial={{ opacity: 0, y: 10, filter: "blur(3px)" }}
            animate={{ opacity: 1, y: 0, filter: "blur(0px)" }}
            exit={{ opacity: 0, y: -6, filter: "blur(3px)" }}
            transition={{ type: "spring", bounce: 0, duration: 0.35 }}
          >
            {panel}
          </m.div>
        </AnimatePresence>
      </div>
    </div>
  );
}

/* ── The panel: a specification table, not a grid of logos ─── */

type Entry = { name: string; detail: string };

export function SpecTable({
  entries,
  className,
}: {
  entries: Entry[];
  className?: string;
}) {
  return (
    <dl className={cn("border-t border-rule", className)}>
      {entries.map((e, i) => (
        <div
          key={e.name}
          className="group grid grid-cols-[1.1fr_1.6fr] items-baseline gap-4 border-b border-rule-soft py-4 md:grid-cols-[240px_1fr] md:gap-8"
        >
          <dt className="flex items-baseline gap-3">
            <span className="eyebrow tabular text-[10px] text-ash">
              {String(i + 1).padStart(2, "0")}
            </span>
            <span className="font-display text-[15px] font-semibold tracking-tight text-ink transition-colors group-hover:text-vermilion">
              {e.name}
            </span>
          </dt>
          <dd className="text-[14.5px] leading-relaxed text-ash">{e.detail}</dd>
        </div>
      ))}
    </dl>
  );
}
