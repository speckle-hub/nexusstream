"use client";

import { m } from "framer-motion";
import { cn } from "@/lib/utils";

/** Compact Aceternity-style Background Beams: gradient pulses travel along SVG paths. */
export function BackgroundBeams({ className }: { className?: string }) {
  const paths = [
    "M-380 -120C-380 44 -280 260 -120 380C40 500 240 560 480 560C720 560 920 500 1080 380C1240 260 1340 44 1340 -120",
    "M-340 -180C-300 20 -180 300 20 420C220 540 460 600 700 580C940 560 1120 440 1240 260C1330 120 1380 -40 1380 -180",
    "M-420 -60C-420 120 -320 340 -140 460C40 580 300 640 560 620C820 600 1020 480 1160 320C1280 180 1320 20 1320 -60",
    "M-360 -240C-320 -20 -200 240 0 380C200 520 480 580 740 540C1000 500 1180 360 1280 180C1350 60 1390 -100 1390 -240",
  ];

  return (
    <div className={cn("absolute inset-0 overflow-hidden", className)} aria-hidden>
      <svg className="absolute h-full w-full" viewBox="-400 -250 1800 900" fill="none" preserveAspectRatio="xMidYMid slice">
        {paths.map((d, i) => (
          <m.path
            key={i}
            d={d}
            stroke={`url(#beam-${i})`}
            strokeWidth="1.5"
            strokeLinecap="round"
            initial={{ pathLength: 0, opacity: 0 }}
            animate={{ pathLength: [0, 1], opacity: [0, 1, 1, 0] }}
            transition={{
              duration: 7 + i * 1.6,
              repeat: Infinity,
              repeatDelay: 2.5 - i * 0.4,
              ease: "easeInOut",
              delay: i * 1.3,
            }}
          />
        ))}
        <defs>
          {paths.map((_, i) => (
            <linearGradient key={i} id={`beam-${i}`} x1="0%" y1="0%" x2="100%" y2="100%">
              <stop offset="0%" stopColor="#7C5CFF" stopOpacity="0" />
              <stop offset="45%" stopColor="#7C5CFF" />
              <stop offset="60%" stopColor="#4F8DFF" />
              <stop offset="100%" stopColor="#4F8DFF" stopOpacity="0" />
            </linearGradient>
          ))}
        </defs>
      </svg>
    </div>
  );
}
