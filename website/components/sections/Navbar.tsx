"use client";

import { useEffect, useState } from "react";
import { Github } from "lucide-react";
import { Button } from "@heroui/react";
import { cn, GITHUB_URL } from "@/lib/utils";
import { LogoMark } from "./LogoMark";

const links = [
  { label: "Features", href: "#features" },
  { label: "Ecosystem", href: "#ecosystem" },
  { label: "FAQ", href: "#faq" },
  { label: "Download", href: "#download" },
];

export function Navbar() {
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const onScroll = () => setScrolled(window.scrollY > 24);
    onScroll();
    window.addEventListener("scroll", onScroll, { passive: true });
    return () => window.removeEventListener("scroll", onScroll);
  }, []);

  return (
    <header
      className={cn(
        "fixed inset-x-0 top-0 z-50 transition-all duration-500",
        scrolled ? "py-2" : "py-4",
      )}
    >
      <div
        className={cn(
          "mx-auto flex max-w-6xl items-center justify-between rounded-2xl px-4 py-2.5 transition-all duration-500 md:px-6",
          scrolled ? "glass mx-4 shadow-card md:mx-auto" : "bg-transparent",
        )}
      >
        <a href="#top" className="flex items-center gap-2.5">
          <LogoMark className="h-8 w-8" />
          <span className="font-display text-lg font-bold tracking-tight text-mist">
            Nexus<span className="text-gradient">Stream</span>
          </span>
        </a>

        <nav className="hidden items-center gap-1 md:flex">
          {links.map((l) => (
            <a
              key={l.href}
              href={l.href}
              className="rounded-full px-3.5 py-1.5 text-sm text-muted transition hover:bg-white/5 hover:text-mist"
            >
              {l.label}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-2">
          <Button
            as="a"
            href={GITHUB_URL}
            target="_blank"
            variant="bordered"
            size="sm"
            className="border-white/15 text-mist hover:border-white/30"
            startContent={<Github className="h-4 w-4" />}
          >
            GitHub
          </Button>
        </div>
      </div>
    </header>
  );
}
