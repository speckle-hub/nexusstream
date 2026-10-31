import Link from "next/link";
import { LogoMark } from "@/components/sections/LogoMark";

export default function NotFound() {
  return (
    <main className="flex min-h-screen flex-col items-center justify-center bg-ink px-6 text-center">
      <LogoMark className="h-14 w-14" />
      <p className="mono mt-8 text-[12px] text-amber">404</p>
      <h1 className="mt-4 text-[clamp(2.2rem,7vw,3.6rem)] font-medium leading-[1.05] tracking-[-0.03em] text-paper">
        This reel is missing.
      </h1>
      <p className="mt-5 max-w-[36ch] text-[15px] leading-[1.6] text-muted">
        The page you asked for is not here. The home feed is.
      </p>
      <Link href="/" className="btn btn-primary mt-8">
        Back to Moovio
      </Link>
    </main>
  );
}
