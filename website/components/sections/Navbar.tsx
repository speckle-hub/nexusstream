"use client";

import { useEffect, useRef, useState } from "react";
import { Download, Github, Menu, X } from "lucide-react";
import { APK_URL, GITHUB_URL, cn } from "@/lib/utils";
import { LogoMark } from "./LogoMark";

const links = [
  { label: "Showcase", href: "#features" },
  { label: "Ecosystem", href: "#ecosystem" },
  { label: "Numbers", href: "#numbers" },
  { label: "FAQ", href: "#faq" },
  { label: "Download", href: "#download" },
];

const CLOSE_MS = 150;

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);
  const [open, setOpen] = useState(false);
  const [closing, setClosing] = useState(false);
  const closeTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  useEffect(() => () => {
    if (closeTimer.current) clearTimeout(closeTimer.current);
  }, []);

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
          "mx-auto flex max-w-6xl items-center justify-between px-4 py-2.5 transition-all md:px-6",
          scrolled
            ? "glass mx-3 mt-2 rounded-2xl shadow-card md:mx-auto"
            : "mt-2 bg-transparent",
        )}
        style={{ transitionDuration: "var(--duration-medium)", transitionTimingFunction: "var(--ease-smooth-out)" }}
      >
        <a href="#top" className="flex items-center gap-2.5" aria-label="NexusStream home">
          <LogoMark className="h-8 w-8" />
          <span className="font-display text-lg font-bold tracking-tight text-mist">
            Nexus<span className="text-gradient">Stream</span>
          </span>
        </a>

        <nav className="hidden items-center gap-1 md:flex" aria-label="Primary">
          {links.map((l) => (
            <a
              key={l.href}
              href={l.href}
              className="rounded-full px-3.5 py-1.5 text-sm text-muted transition hover:bg-white/5 hover:text-mist"
              style={{ transitionDuration: "var(--duration-quick)", transitionTimingFunction: "var(--ease-smooth-out)" }}
            >
              {l.label}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          <a
            href={APK_URL}
            className="hidden items-center gap-1.5 rounded-full bg-accent px-4 py-1.5 text-sm font-semibold text-white transition hover:brightness-110 sm:inline-flex"
            style={{ transitionDuration: "var(--duration-quick)" }}
          >
            <Download className="h-3.5 w-3.5" />
            Get the app
          </a>
          <a
            href={GITHUB_URL}
            target="_blank"
            rel="noreferrer"
            aria-label="GitHub repository"
            className="hidden rounded-full border border-white/15 p-2 text-mist transition hover:border-white/30 sm:block"
            style={{ transitionDuration: "var(--duration-quick)" }}
          >
            <Github className="h-4 w-4" />
          </a>
          <div className="relative md:hidden">
            <button
              onClick={toggleMenu}
              aria-expanded={open}
              aria-label={open ? "Close menu" : "Open menu"}
              className="rounded-full border border-white/15 p-2 text-mist"
            >
              {open ? <X className="h-4 w-4" /> : <Menu className="h-4 w-4" />}
            </button>
            {(open || closing) && (
              <div
                data-origin="top-right"
                className={cn(
                  "t-dropdown absolute right-0 top-full mt-2 w-52 rounded-2xl border border-white/10 bg-elevated/95 p-2 shadow-card backdrop-blur-xl",
                  open && !closing && "is-open",
                  closing && "is-closing",
                )}
              >
                {links.map((l) => (
                  <a
                    key={l.href}
                    href={l.href}
                    onClick={closeMenu}
                    className="block rounded-xl px-4 py-2.5 text-sm text-muted transition hover:bg-white/5 hover:text-mist"
                  >
                    {l.label}
                  </a>
                ))}
                <a
                  href={GITHUB_URL}
                  target="_blank"
                  rel="noreferrer"
                  className="mt-1 flex items-center gap-2 rounded-xl px-4 py-2.5 text-sm text-muted transition hover:bg-white/5 hover:text-mist"
                >
                  <Github className="h-4 w-4" /> GitHub
                </a>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}
