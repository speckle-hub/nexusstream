/**
 * Moovio mark, flat.
 *
 * The M is the product's real launcher glyph. The plate and the wordmark carry the brand, so
 * the artwork keeps one solid ink field and one hairline instead of the old violet bloom and
 * gradient dot — a gradient tile is the first thing that reads as generated.
 */
export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 48 48" fill="none" className={className} aria-hidden>
      <rect width="48" height="48" rx="10" fill="#141412" />
      <rect
        x="0.5"
        y="0.5"
        width="47"
        height="47"
        rx="9.5"
        stroke="#EFEDE8"
        strokeOpacity="0.14"
      />
      <path
        fill="#EFEDE8"
        d="M30.23,12.73 34.57,12.75 34.57,31.59 30.21,31.59 30.19,17.65 29.60,19.73 25.57,31.41 22.17,31.48 17.98,19.73 17.16,16.52 17.23,31.45 13.55,31.48 13.52,13.12 19.62,13.17 23.46,24.33 24.05,26.65 24.63,24.42 28.52,13.17 30.19,13.10Z"
      />
    </svg>
  );
}
