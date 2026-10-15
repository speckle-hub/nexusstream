import Link from "next/link";
import { LogoMark } from "@/components/sections/LogoMark";

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-void px-6 text-center">
      <div
        aria-hidden
        className="absolute left-1/2 top-[-200px] h-[400px] w-[640px] -translate-x-1/2 rounded-full bg-accent/20 blur-[120px]"
      />
      <div className="relative">
        <LogoMark className="mx-auto mb-8 h-16 w-16" />
        <p className="eyebrow text-accent-soft">Error 404</p>
        <p className="display mt-4 font-sans font-bold text-paper" style={{ fontSize: "clamp(3rem, 10vw, 6rem)" }}>
          Lost pages.
        </p>
        <p className="mx-auto mt-5 max-w-sm text-[16px] leading-relaxed text-muted">
          This stream ended — or never existed. Head back to the home feed.
        </p>
        <Link
          href="/"
          className="press mt-8 inline-block rounded-full bg-paper px-8 py-3.5 text-[15px] font-semibold tracking-tight text-void hover:bg-white"
        >
          Back to Moovio
        </Link>
      </div>
    </main>
  );
}
