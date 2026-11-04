"use client";

import { useRef, type ReactNode } from "react";

/**
 * Spotlight — a pointer-tracked highlight, written to the element as `--mx` / `--my`.
 *
 * The gradient itself lives in CSS (`.spotlight::before`), so hover costs no React render:
 * the handler only writes two custom properties.
 */
export function Spotlight({
  children,
  className = "",
}: {
  children: ReactNode;
  className?: string;
}) {
  const ref = useRef<HTMLDivElement>(null);

  return (
    <div
      ref={ref}
      className={`spotlight ${className}`}
      onPointerMove={(event) => {
        const el = ref.current;
        if (!el) return;
        const rect = el.getBoundingClientRect();
        el.style.setProperty("--mx", `${event.clientX - rect.left}px`);
        el.style.setProperty("--my", `${event.clientY - rect.top}px`);
      }}
    >
      {children}
    </div>
  );
}
