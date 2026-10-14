/**
 * Wordmark: a single flat block, no gradient and no glow. The page stays
 * monochrome so the product carries all the colour.
 *
 * `variant="onInk"` flips the plate for dark surfaces.
 */
export function LogoMark({
  className,
  variant = "onPaper",
}: {
  className?: string;
  variant?: "onPaper" | "onInk";
}) {
  const plate = variant === "onPaper" ? "#0C0C0E" : "#F2F1EC";
  const glyph = variant === "onPaper" ? "#F2F1EC" : "#0C0C0E";

  return (
    <svg viewBox="0 0 48 48" fill="none" className={className} aria-hidden>
      <rect x="0" y="0" width="48" height="48" rx="12" fill={plate} />
      <path
        d="M15 34V14l9.5 11.5L34 14v20"
        stroke={glyph}
        strokeWidth="3.6"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="34" cy="34" r="2.8" fill="#D8452A" />
    </svg>
  );
}
