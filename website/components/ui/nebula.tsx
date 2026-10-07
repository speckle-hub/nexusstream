"use client";

import { useEffect, useState } from "react";
import { NebulaBackground } from "@designcodeio/threeui/components/NebulaBackground";
import { cn } from "@/lib/utils";

/** ThreeUI nebula shader, hue-shifted toward NexusStream violet. Static gradient under reduced motion. */
export function Nebula({ className }: { className?: string }) {
  const [reduced, setReduced] = useState<boolean | null>(null);

  useEffect(() => {
    setReduced(window.matchMedia("(prefers-reduced-motion: reduce)").matches);
  }, []);

  return (
    <div className={cn("pointer-events-none absolute inset-0 overflow-hidden", className)} aria-hidden>
      {reduced ? (
        <div className="h-full w-full bg-[radial-gradient(ellipse_80%_60%_at_50%_20%,rgba(124,92,255,0.28),transparent_70%)]" />
      ) : (
        <NebulaBackground
          hue={-30}
          saturation={1.25}
          brightness={0.85}
          className="h-full w-full opacity-70"
        />
      )}
    </div>
  );
}
