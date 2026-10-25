"use client";

import { useId } from "react";

/**
 * Moovio mark: the same artwork as the Windows app icon and the Android
 * launcher — an ink plate with a violet bloom, the white "M" and the mint
 * stream dot. Ids are scoped per instance because the mark appears in the
 * navbar, the footer and the 404 page at once.
 */
export function LogoMark({ className }: { className?: string }) {
  const uid = useId().replace(/:/g, "");
  const plate = `plate-${uid}`;
  const bloom = `bloom-${uid}`;
  const dot = `dot-${uid}`;

  return (
    <svg viewBox="0 0 48 48" fill="none" className={className} aria-hidden>
      <defs>
        <linearGradient
          id={plate}
          x1="6"
          y1="2"
          x2="44"
          y2="47"
          gradientUnits="userSpaceOnUse"
        >
          <stop stopColor="#3A2F70" />
          <stop offset="0.45" stopColor="#1E1A36" />
          <stop offset="1" stopColor="#07070D" />
        </linearGradient>
        <radialGradient
          id={bloom}
          cx="0"
          cy="0"
          r="1"
          gradientUnits="userSpaceOnUse"
          gradientTransform="translate(9 7) scale(36)"
        >
          <stop stopColor="#7C5CFF" stopOpacity="0.28" />
          <stop offset="1" stopColor="#7C5CFF" stopOpacity="0" />
        </radialGradient>
        <radialGradient
          id={dot}
          cx="0"
          cy="0"
          r="1"
          gradientUnits="userSpaceOnUse"
          gradientTransform="translate(35.8 35.8) scale(7)"
        >
          <stop stopColor="#23CFEE" />
          <stop offset="0.55" stopColor="#4FA8F0" />
          <stop offset="1" stopColor="#875FF6" />
        </radialGradient>
      </defs>
      <rect width="48" height="48" rx="9.8" fill={`url(#${plate})`} />
      <rect width="48" height="48" rx="9.8" fill={`url(#${bloom})`} />
      <rect
        x="2.7"
        y="2.7"
        width="42.6"
        height="42.6"
        rx="7.4"
        stroke="#FFFFFF"
        strokeOpacity="0.10"
        strokeWidth="0.5"
      />
      <path
        fill="#F5F5FA"
        d="M30.23,12.73 34.57,12.75 34.57,31.59 30.21,31.59 30.19,17.65 29.60,19.73 25.57,31.41 22.17,31.48 17.98,19.73 17.16,16.52 17.23,31.45 13.55,31.48 13.52,13.12 19.62,13.17 23.46,24.33 24.05,26.65 24.63,24.42 28.52,13.17 30.19,13.10Z"
      />
      <circle cx="35.8" cy="35.8" r="7" fill={`url(#${dot})`} />
    </svg>
  );
}
