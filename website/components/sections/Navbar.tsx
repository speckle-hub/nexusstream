"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Github, Menu, X } from "lucide-react";
import { ANDROID_VERSION, APK_URL, GITHUB_URL } from "@/lib/utils";
import { LogoMark } from "./LogoMark";

const links = [
  { label: "Sources", href: "#sources" },
  { label: "Inside", href: "#features" },
  { label: "Screens", href: "#screens" },
  { label: "FAQ", href: "#faq" },
];

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const [progress, setProgress] = useState(0);

  useEffect(() => {
    let frame = 0;
    const read = () => {
      frame = 0;
      const y = window.scrollY;
      setScrolled(y > 8);
      const height = document.documentElement.scrollHeight - window.innerHeight;
      setProgress(height > 0 ? Math.min(1, y / height) : 0);
    };
    const onScroll = () => {
      // One read per frame: scroll fires far more often than the layout changes.
      if (!frame) frame = requestAnimationFrame(read);
    };
    read();
    window.addEventListener("scroll", onScroll, { passive: true });
    window.addEventListener("resize", onScroll, { passive: true });
    return () => {
      window.removeEventListener("scroll", onScroll);
      window.removeEventListener("resize", onScroll);
      cancelAnimationFrame(frame);
    };
  }, []);

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 border-b transition-colors duration-200 ${
        scrolled || open ? "border-rule bg-ink/95 backdrop-blur" : "border-transparent bg-transparent"
      }`}
    >
      <div className="shell nav-enter flex h-16 items-center justify-between gap-8">
        <Link href="#top" className="flex items-center gap-2.5" aria-label="Moovio home">
          <LogoMark className="h-7 w-7" />
          <span className="text-[16px] font-medium tracking-[-0.01em] text-paper">Moovio</span>
          <span className="mono hidden text-[11px] text-faint sm:inline">{ANDROID_VERSION}</span>
        </Link>

        <nav className="hidden items-center gap-7 md:flex" aria-label="Primary">
          {links.map((link) => (
            <Link key={link.href} href={link.href} className="link-quiet text-[14px]">
              {link.label}
            </Link>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          <Link
            href={GITHUB_URL}
            target="_blank"
            rel="noreferrer"
            aria-label="GitHub repository"
            className="hidden h-9 w-9 items-center justify-center rounded-[8px] border border-rule text-muted transition-colors hover:border-paper/30 hover:text-paper sm:flex"
          >
            <Github className="h-4 w-4" strokeWidth={1.5} />
          </Link>
          <Link href={APK_URL} className="btn btn-primary hidden h-9 !px-4 text-[13px] sm:inline-flex">
            Download
          </Link>
          <button
            type="button"
            onClick={() => setOpen((value) => !value)}
            aria-expanded={open}
            aria-label={open ? "Close menu" : "Open menu"}
            className="flex h-9 w-9 items-center justify-center rounded-[8px] border border-rule text-paper md:hidden"
          >
            {open ? (
              <X className="h-4 w-4" strokeWidth={1.5} />
            ) : (
              <Menu className="h-4 w-4" strokeWidth={1.5} />
            )}
          </button>
        </div>
      </div>

      {/* Reading progress: the page's own timecode. */}
      <div aria-hidden className="h-px w-full bg-transparent">
        <div
          className="h-px bg-amber/80"
          style={{ width: `${progress * 100}%`, transition: "width 80ms linear" }}
        />
      </div>

      {open && (
        <div className="border-t border-rule bg-ink md:hidden">
          <nav className="shell flex flex-col py-2" aria-label="Mobile">
            {links.map((link) => (
              <Link
                key={link.href}
                href={link.href}
                onClick={() => setOpen(false)}
                className="border-b border-rule-soft py-3 text-[15px] text-muted last:border-0"
              >
                {link.label}
              </Link>
            ))}
            <Link href={APK_URL} onClick={() => setOpen(false)} className="btn btn-primary mb-3 mt-3">
              Download for Android
            </Link>
          </nav>
        </div>
      )}
    </header>
  );
}
