"use client";

import { useEffect, useRef } from "react";
import { cn } from "@/lib/utils";

type SparklesProps = {
  className?: string;
  density?: number;
  color?: string;
  maxSize?: number;
  speed?: number;
};

/** Lightweight canvas sparkles (Aceternity-style, no particle-engine dependency). */
export function Sparkles({
  className,
  density = 90,
  color = "#B9A8FF",
  maxSize = 1.8,
  speed = 0.6,
}: SparklesProps) {
  const ref = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = ref.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let raf = 0;
    let width = 0;
    let height = 0;
    let visible = false;

    type P = { x: number; y: number; r: number; phase: number; drift: number };
    let particles: P[] = [];

    const resize = () => {
      const rect = canvas.getBoundingClientRect();
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      width = rect.width;
      height = rect.height;
      canvas.width = width * dpr;
      canvas.height = height * dpr;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const count = Math.floor((width * height) / (12000 / (density / 60)));
      particles = Array.from({ length: count }, () => ({
        x: Math.random() * width,
        y: Math.random() * height,
        r: 0.4 + Math.random() * maxSize,
        phase: Math.random() * Math.PI * 2,
        drift: (Math.random() - 0.5) * speed,
      }));
    };

    const drawStatic = () => {
      // One calm frame for reduced-motion users / offscreen pause.
      ctx.clearRect(0, 0, width, height);
      for (const p of particles) {
        ctx.globalAlpha = 0.45;
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fill();
      }
    };

    const tick = (t: number) => {
      ctx.clearRect(0, 0, width, height);
      for (const p of particles) {
        p.x += p.drift * 0.12;
        p.y += Math.sin(t / 2600 + p.phase) * 0.06;
        if (p.x < -4) p.x = width + 4;
        if (p.x > width + 4) p.x = -4;
        const tw = 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(t / 900 + p.phase * 3));
        ctx.globalAlpha = tw * 0.8;
        ctx.fillStyle = color;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.r, 0, Math.PI * 2);
        ctx.fill();
      }
      raf = requestAnimationFrame(tick);
    };

    const start = () => {
      if (raf === 0) raf = requestAnimationFrame(tick);
    };
    const stop = () => {
      cancelAnimationFrame(raf);
      raf = 0;
    };

    resize();
    const ro = new ResizeObserver(resize);
    ro.observe(canvas);

    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    let io: IntersectionObserver | null = null;
    if (reduced) {
      drawStatic();
    } else {
      // Only burn rAF while the canvas is actually on screen.
      io = new IntersectionObserver(([entry]) => {
        visible = entry.isIntersecting;
        if (visible) start();
        else stop();
      });
      io.observe(canvas);
    }

    return () => {
      stop();
      ro.disconnect();
      io?.disconnect();
    };
  }, [density, color, maxSize, speed]);

  return <canvas ref={ref} className={cn("pointer-events-none absolute inset-0 h-full w-full", className)} />;
}
