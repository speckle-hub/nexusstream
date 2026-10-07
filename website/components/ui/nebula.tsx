"use client";

import { NebulaBackground } from "@designcodeio/threeui/components/NebulaBackground";
import { cn } from "@/lib/utils";

/** ThreeUI nebula shader, hue-shifted toward NexusStream violet. */
export function Nebula({ className }: { className?: string }) {
  return (
    <div className={cn("pointer-events-none absolute inset-0 overflow-hidden", className)} aria-hidden>
      <NebulaBackground
        hue={-30}
        saturation={1.25}
        brightness={0.85}
        className="h-full w-full opacity-70"
      />
    </div>
  );
}
