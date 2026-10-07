"use client";

import { useEffect, useId, useRef, useState } from "react";
import { m, useAnimationFrame, useMotionValue, useTransform } from "framer-motion";
import { cn } from "@/lib/utils";

/**
 * Aceternity Moving Border: a comet of accent light travels the button's rounded-rect path.
 * The beam is SVG-native and only renders after mount (decorative; avoids SSR/CSR drift).
 */
export function MovingBorderButton({
  children,
  className,
  containerClassName,
  duration = 2600,
  href,
}: {
  children: React.ReactNode;
  className?: string;
  containerClassName?: string;
  duration?: number;
  href?: string;
}) {
  return (
    <a
      href={href}
      target="_blank"
      rel="noreferrer"
      className={cn(
        "relative inline-block overflow-hidden rounded-full p-[1.5px]",
        containerClassName,
      )}
    >
      <div className="absolute inset-0">
        <RectBeam duration={duration} />
      </div>
      <div
        className={cn(
          "relative flex items-center justify-center gap-2 rounded-full bg-accent px-8 py-3.5 font-semibold text-white shadow-glow-accent transition hover:bg-accent-deep",
          className,
        )}
      >
        {children}
      </div>
    </a>
  );
}

function RectBeam({ duration }: { duration: number }) {
  const pathRef = useRef<SVGRectElement>(null);
  const progress = useMotionValue<number>(0);
  const [mounted, setMounted] = useState(false);
  const gradientId = `beam-glow-${useId().replace(/:/g, "")}`;

  useEffect(() => setMounted(true), []);

  useAnimationFrame((time) => {
    const length = pathRef.current?.getTotalLength();
    if (length) progress.set(((time % duration) / duration) * length);
  });

  const cx = useTransform(progress, (v) => pathRef.current?.getPointAtLength(v).x ?? -100);
  const cy = useTransform(progress, (v) => pathRef.current?.getPointAtLength(v).y ?? -100);

  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      preserveAspectRatio="none"
      className="absolute h-full w-full"
      width="100%"
      height="100%"
      aria-hidden
    >
      <rect ref={pathRef} fill="none" width="100%" height="100%" rx="999" />
      {mounted && <m.circle r="16" fill={`url(#${gradientId})`} style={{ cx, cy }} />}
      <defs>
        <radialGradient id={gradientId}>
          <stop offset="0%" stopColor="#CFC2FF" stopOpacity="0.95" />
          <stop offset="45%" stopColor="#7C5CFF" stopOpacity="0.7" />
          <stop offset="100%" stopColor="#7C5CFF" stopOpacity="0" />
        </radialGradient>
      </defs>
    </svg>
  );
}
