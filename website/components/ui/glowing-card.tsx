"use client";

import { useRef, useState } from "react";
import { motion, useMotionTemplate, useMotionValue } from "framer-motion";
import { cn } from "@/lib/utils";

/**
 * Cursor-style glowing border (Aceternity GlowingEffect, simplified): a radial accent glow
 * follows the pointer around the card's border region.
 */
export function GlowingCard({
  className,
  children,
  glowColor = "124, 92, 255",
}: {
  className?: string;
  children: React.ReactNode;
  glowColor?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);
  const x = useMotionValue(0);
  const y = useMotionValue(0);
  const [active, setActive] = useState(false);

  const background = useMotionTemplate`radial-gradient(320px circle at ${x}px ${y}px, rgba(${glowColor}, 0.22), transparent 70%)`;
  const border = useMotionTemplate`radial-gradient(180px circle at ${x}px ${y}px, rgba(${glowColor}, 0.85), transparent 70%)`;

  return (
    <div
      ref={ref}
      onPointerMove={(e) => {
        const rect = ref.current?.getBoundingClientRect();
        if (!rect) return;
        x.set(e.clientX - rect.left);
        y.set(e.clientY - rect.top);
      }}
      onPointerEnter={() => setActive(true)}
      onPointerLeave={() => setActive(false)}
      className={cn("relative rounded-2xl", className)}
    >
      <motion.div
        aria-hidden
        className="pointer-events-none absolute -inset-px rounded-2xl"
        style={{ background: border, opacity: active ? 1 : 0, transition: "opacity 0.4s" }}
      />
      <motion.div
        aria-hidden
        className="pointer-events-none absolute inset-0 rounded-2xl"
        style={{ background, opacity: active ? 1 : 0, transition: "opacity 0.4s" }}
      />
      <div className="relative h-full rounded-2xl">{children}</div>
    </div>
  );
}
