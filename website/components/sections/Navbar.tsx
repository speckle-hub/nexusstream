"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { Github, Menu, X } from "lucide-react";
import { ANDROID_VERSION, APK_URL, GITHUB_URL } from "@/lib/utils";
import { LogoMark } from "./LogoMark";

const links = [
  { label: "Sources", href: "#sources" },
  { label: "Features", href: "#features" },
  { label: "Showcase", href: "#showcase" },
  { label: "FAQ", href: "#faq" },
];

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 8);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={`fixed inset-x-0 top-0 z-50 border-b transition-colors duration-200 ${
        scrolled || open
          ? "border-rule bg-ink"
          : "border-transparent bg-transparent"
      }`}
    >
      <div className="shell flex h-16 items-center justify-between gap-8">
        <Link href="#top" className="flex items-center gap-2.5" aria-label="Moovio home">
          <LogoMark className="h-7 w-7" />
          <span className="text-[16px] font-medium tracking-[-0.01em] text-paper">Moovio</span>
          <span className="mono hidden text-[11px] text-faint sm:inline">
            {ANDROID_VERSION}
          </span>
        </Link>

        <nav className="hidden items-center gap-7 md:flex" aria-label="Primary">
          {links.map((l) => (
            <Link
              key={l.href}
              href={l.href}
              className="link-quiet text-[14px] transition-colors"
            >
              {l.label}
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
          <Link href={APK_URL} className="btn btn-primary hidden sm:inline-flex">
            Download
          </Link>
          <button
            type="button"
            onClick={() => setOpen((v) => !v)}
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

      {open && (
        <div className="border-t border-rule bg-ink md:hidden">
          <nav className="shell flex flex-col py-2" aria-label="Mobile">
            {links.map((l) => (
              <Link
                key={l.href}
                href={l.href}
                onClick={() => setOpen(false)}
                className="border-b border-rule-soft py-3 text-[15px] text-muted last:border-0"
              >
                {l.label}
              </Link>
            ))}
            <Link
              href={APK_URL}
              onClick={() => setOpen(false)}
              className="btn btn-primary mt-3 mb-3"
            >
              Download for Android
            </Link>
          </nav>
        </div>
      )}
    </header>
  );
}
