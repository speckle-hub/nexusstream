import Link from "next/link";
import { LogoMark } from "@/components/sections/LogoMark";

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-paper px-6 text-center">
      <div className="relative">
        <LogoMark className="mx-auto mb-8 h-16 w-16" />
        <p className="eyebrow text-vermilion">Error 404</p>
        <p className="display mt-4 font-sans font-bold text-ink" style={{ fontSize: "clamp(3rem, 10vw, 6rem)" }}>
          Lost pages.
        </p>
        <p className="measure mx-auto mt-5 text-[16px] text-graphite">
          This stream ended — or never existed. Head back to the home feed.
        </p>
        <Link
          href="/"
          className="press mt-8 inline-block rounded-full bg-ink px-8 py-3.5 text-[15px] font-semibold tracking-tight text-paper hover:bg-graphite"
        >
          Back to NexusStream
        </Link>
      </div>
    </main>
  );
}
