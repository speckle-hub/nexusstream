"use client";

import { useRef } from "react";
import { motion, useAnimationFrame, useMotionTemplate, useMotionValue, useTransform } from "framer-motion";
import { cn } from "@/lib/utils";

/**
 * Aceternity Moving Border: a comet of accent light travels the button's rounded-rect path.
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

  useAnimationFrame((time) => {
    const length = pathRef.current?.getTotalLength();
    if (length) progress.set((time % duration) / duration * length);
  });

  const x = useTransform(progress, (v) => pathRef.current?.getPointAtLength(v).x ?? 0);
  const y = useTransform(progress, (v) => pathRef.current?.getPointAtLength(v).y ?? 0);
  const transform = useMotionTemplate`translateX(${x}px) translateY(${y}px) translateX(-50%) translateY(-50%)`;

  return (
    <svg xmlns="http://www.w3.org/2000/svg" preserveAspectRatio="none" className="absolute h-full w-full" width="100%" height="100%">
      <rect ref={pathRef} fill="none" width="100%" height="100%" rx="999" />
      <motion.div style={{ position: "absolute", top: 0, left: 0, transform }}>
        <div className="h-20 w-20 rounded-full bg-[radial-gradient(circle,#CFC2FF_0%,#7C5CFF_40%,transparent_70%)] opacity-90" />
      </motion.div>
    </svg>
  );
}
