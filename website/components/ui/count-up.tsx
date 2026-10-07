"use client";

import { useEffect, useRef } from "react";
import { m, useInView, useSpring, useTransform } from "framer-motion";

/** Animated counter that springs to its target when scrolled into view. */
export function CountUp({ to, suffix = "", className }: { to: number; suffix?: string; className?: string }) {
  const ref = useRef<HTMLSpanElement>(null);
  const inView = useInView(ref, { once: true, margin: "-60px" });
  const spring = useSpring(0, { stiffness: 60, damping: 18 });
  const text = useTransform(spring, (v) => `${Math.round(v).toLocaleString()}${suffix}`);

  useEffect(() => {
    if (inView) spring.set(to);
  }, [inView, spring, to]);

  return (
    <span ref={ref} className={className}>
      <m.span>{text}</m.span>
    </span>
  );
}
