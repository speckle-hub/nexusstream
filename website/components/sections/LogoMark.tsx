export function LogoMark({ className }: { className?: string }) {
  return (
    <svg viewBox="0 0 48 48" fill="none" className={className} aria-hidden>
      <rect x="2" y="2" width="44" height="44" rx="13" fill="url(#lg-bg)" />
      <path
        d="M16 33V15l9 10.5L34 15v18"
        stroke="white"
        strokeWidth="3.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="34" cy="33" r="2.6" fill="white" opacity="0.9" />
      <defs>
        <linearGradient id="lg-bg" x1="2" y1="2" x2="46" y2="46">
          <stop stopColor="#8F74FF" />
          <stop offset="0.55" stopColor="#7C5CFF" />
          <stop offset="1" stopColor="#4F8DFF" />
        </linearGradient>
      </defs>
    </svg>
  );
}
