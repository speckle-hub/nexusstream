import Link from "next/link";
import { LogoMark } from "@/components/sections/LogoMark";

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-void px-6 text-center">
      <div className="absolute inset-0 bg-[radial-gradient(ellipse_60%_50%_at_50%_35%,rgba(124,92,255,0.22),transparent_70%)]" />
      <div className="relative">
        <LogoMark className="mx-auto mb-8 h-16 w-16" />
        <p className="font-display text-7xl font-bold text-gradient md:text-8xl">404</p>
        <p className="mt-4 max-w-sm text-sm text-muted md:text-base">
          This stream ended — or never existed. Head back to the home feed.
        </p>
        <Link
          href="/"
          className="mt-8 inline-block rounded-full bg-accent px-8 py-3.5 font-semibold text-white shadow-glow-accent transition hover:bg-accent-deep"
        >
          Back to NexusStream
        </Link>
      </div>
    </main>
  );
}
