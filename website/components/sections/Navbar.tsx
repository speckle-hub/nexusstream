"use client";

import { useEffect, useRef, useState } from "react";
import { Download, Github, Menu, X } from "lucide-react";
import { APK_URL, GITHUB_URL, cn } from "@/lib/utils";
import { LogoMark } from "./LogoMark";

const links = [
  { label: "Ecosystem", href: "#ecosystem" },
  { label: "Features", href: "#features" },
  { label: "Showcase", href: "#showcase" },
  { label: "FAQ", href: "#faq" },
];

const CLOSE_MS = 150;

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const [closing, setClosing] = useState(false);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 16);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(
    () => () => {
      if (closeTimer.current) clearTimeout(closeTimer.current);
    },
    [],
  );

  const toggleMenu = () => {
    if (open) {
      setOpen(false);
      setClosing(true);
      closeTimer.current = setTimeout(() => setClosing(false), CLOSE_MS);
    } else {
      setClosing(false);
      setOpen(true);
    }
  };

  const closeMenu = () => {
    if (!open) return;
    setOpen(false);
    setClosing(true);
    closeTimer.current = setTimeout(() => setClosing(false), CLOSE_MS);
  };

  return (
    <header className="fixed inset-x-0 top-0 z-50">
      <div
        className={cn(
          "transition-[background-color,border-color] duration-300 ease-[var(--ease-smooth-out)]",
          scrolled
            ? "material-void border-b border-rule-soft"
            : "border-b border-transparent bg-transparent",
        )}
      >
        <div className="mx-auto flex h-16 max-w-editorial items-center justify-between gap-6 px-5 sm:px-8">
          <a
            href="#top"
            className="press flex shrink-0 items-center gap-2.5"
            aria-label="Moovio home"
          >
            <LogoMark className="h-7 w-7" />
            <span className="font-display text-[17px] font-semibold tracking-tight text-paper">
              Moovio
            </span>
            <span className="eyebrow hidden rounded-full border border-rule bg-white/[0.04] px-2.5 py-1 text-[9px] text-muted sm:block">
              v1.10.0
            </span>
          </a>

          <nav className="hidden items-center gap-7 lg:flex" aria-label="Primary">
            {links.map((l) => (
              <a
                key={l.href}
                href={l.href}
                className="press eyebrow group relative py-1 text-muted transition-colors hover:text-paper"
              >
                {l.label}
                <span
                  aria-hidden
                  className="absolute -bottom-0.5 left-0 h-px w-0 bg-accent-soft transition-[width] duration-300 ease-[var(--ease-smooth-out)] group-hover:w-full"
                />
              </a>
            ))}
          </nav>

          <div className="flex items-center gap-3">
            <a
              href={GITHUB_URL}
              target="_blank"
              rel="noreferrer"
              aria-label="GitHub repository"
              className="press hidden h-9 w-9 items-center justify-center rounded-full border border-rule text-muted hover:border-paper/40 hover:text-paper sm:flex"
            >
              <Github className="h-4 w-4" />
            </a>

            <a
              href={APK_URL}
              className="press hidden items-center gap-2 rounded-full bg-paper px-5 py-2.5 text-[13px] font-semibold tracking-tight text-void hover:bg-white sm:inline-flex"
            >
              <Download className="h-3.5 w-3.5" />
              Download APK
            </a>

            <div className="relative lg:hidden">
              <button
                onClick={toggleMenu}
                aria-expanded={open}
                aria-label={open ? "Close menu" : "Open menu"}
                className="press flex h-9 w-9 items-center justify-center rounded-full border border-rule text-paper"
              >
                {open ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
              </button>

              {(open || closing) && (
                <div
                  className={cn(
                    "t-dropdown absolute right-0 top-full mt-3 w-60 origin-top-right overflow-hidden rounded-2xl border border-rule bg-void-raised p-1.5 shadow-card",
                    open && !closing && "is-open",
                    closing && "is-closing",
                  )}
                >
                  {links.map((l) => (
                    <a
                      key={l.href}
                      href={l.href}
                      onClick={closeMenu}
                      className="press block rounded-xl px-4 py-3 text-sm font-medium text-muted hover:bg-white/[0.05] hover:text-paper"
                    >
                      {l.label}
                    </a>
                  ))}
                  <div className="my-1.5 h-px bg-rule-soft" />
                  <a
                    href={APK_URL}
                    onClick={closeMenu}
                    className="press flex items-center gap-2 rounded-xl bg-paper px-4 py-3 text-sm font-semibold text-void"
                  >
                    <Download className="h-4 w-4" />
                    Download APK
                  </a>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}
