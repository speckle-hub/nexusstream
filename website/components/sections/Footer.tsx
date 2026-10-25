"use client";

import { Github, Scale } from "lucide-react";
import Link from "next/link";
import {
  DESKTOP_ZIP_URL,
  DESKTOP_MSI_URL,
  APK_URL,
  GITHUB_URL,
  RELEASES_URL,
} from "@/lib/utils";
import { LogoMark } from "./LogoMark";

const colophon = [
  {
    head: "Desktop (Windows)",
    links: [
      { label: "MSI Installer", href: DESKTOP_MSI_URL },
      { label: "Portable ZIP", href: DESKTOP_ZIP_URL },
      { label: "GitHub Releases", href: RELEASES_URL },
    ],
  },
  {
    head: "Android",
    links: [
      { label: "APK Download", href: APK_URL },
      { label: "Android App", href: "#ecosystem" },
    ],
  },
  {
    head: "Project",
    links: [
      { label: "Repository", href: GITHUB_URL },
      { label: "Issues", href: `${GITHUB_URL}/issues` },
      { label: "Build logs", href: `${GITHUB_URL}/actions` },
    ],
  },
  {
    head: "On this page",
    links: [
      { label: "Features", href: "#features" },
      { label: "Showcase", href: "#showcase" },
      { label: "FAQ", href: "#faq" },
      { label: "Download", href: "#download" },
    ],
  },
];

export function Footer() {
  return (
    <footer className="border-t border-rule bg-void-deep/60">
      <div className="mx-auto max-w-editorial px-5 py-16 sm:px-8 md:py-20">
        <div className="grid gap-12 md:grid-cols-[1.3fr_2fr] md:gap-16">
          <div>
            <div className="flex items-center gap-2.5">
              <LogoMark className="h-8 w-8" />
              <span className="font-display text-lg font-semibold tracking-tight text-paper">
                Moovio
              </span>
            </div>
            <p className="mt-5 max-w-sm text-[14.5px] leading-relaxed text-muted">
              Premium native Windows streaming client. Compose Multiplatform,
              libmpv, Stremio add-ons, dark cinematic UI. Free and open source.
            </p>
            <div className="mt-6 flex items-center gap-3">
              <Link
                href={GITHUB_URL}
                target="_blank"
                rel="noreferrer"
                aria-label="GitHub repository"
                className="press inline-flex h-10 w-10 items-center justify-center rounded-full border border-rule text-muted hover:border-paper/40 hover:text-paper"
              >
                <Github className="h-4 w-4" />
              </Link>
              <span className="eyebrow flex items-center gap-2 text-[10px] text-faint">
                <span className="h-1.5 w-1.5 rounded-full bg-accent animate-pulse-soft" />
                v0.1.0
              </span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-x-8 gap-y-10 sm:grid-cols-3">
            {colophon.map((c) => (
              <nav key={c.head} aria-label={c.head}>
                <h3 className="eyebrow border-b border-rule pb-3 text-[10px] text-faint">
                  {c.head}
                </h3>
                <ul className="mt-4 space-y-2.5">
                  {c.links.map((l) => (
                    <li key={l.label}>
                      <Link
                        href={l.href}
                        target={l.href.startsWith("http") ? "_blank" : undefined}
                        rel={l.href.startsWith("http") ? "noreferrer" : undefined}
                        className="press text-[14px] text-muted transition-colors hover:text-paper"
                      >
                        {l.label}
                      </Link>
                    </li>
                  ))}
                </ul>
              </nav>
            ))}
          </div>
        </div>

        <div className="mt-14 flex flex-col gap-4 border-t border-rule pt-6 text-[12.5px] text-faint md:flex-row md:items-center md:justify-between">
          <p>© 2026 Moovio contributors. Desktop: MIT · libmpv: GPLv2+.</p>
          <p className="flex items-center gap-2">
            <Scale className="h-3.5 w-3.5" />
            Not affiliated with Stremio, CloudStream, Aniyomi or Mihon.
          </p>
        </div>
      </div>
    </footer>
  );
}
