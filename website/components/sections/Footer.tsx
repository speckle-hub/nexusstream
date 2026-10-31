import Link from "next/link";
import { Github } from "lucide-react";
import {
  ANDROID_VERSION,
  APK_URL,
  DESKTOP_MSI_URL,
  DESKTOP_ZIP_URL,
  GITHUB_URL,
  RELEASES_URL,
} from "@/lib/utils";
import { LogoMark } from "./LogoMark";

const columns = [
  {
    head: "Download",
    links: [
      { label: "Android APK", href: APK_URL },
      { label: "Windows MSI", href: DESKTOP_MSI_URL },
      { label: "Portable ZIP", href: DESKTOP_ZIP_URL },
      { label: "All releases", href: RELEASES_URL },
    ],
  },
  {
    head: "Project",
    links: [
      { label: "Repository", href: GITHUB_URL },
      { label: "Issues", href: `${GITHUB_URL}/issues` },
      { label: "Releases", href: RELEASES_URL },
    ],
  },
  {
    head: "On this page",
    links: [
      { label: "Sources", href: "#sources" },
      { label: "Features", href: "#features" },
      { label: "Showcase", href: "#showcase" },
      { label: "FAQ", href: "#faq" },
    ],
  },
];

export function Footer() {
  return (
    <footer>
      <div className="shell py-16">
        <div className="grid gap-12 md:grid-cols-[1.2fr_2fr] md:gap-16">
          <div>
            <div className="flex items-center gap-2.5">
              <LogoMark className="h-8 w-8" />
              <span className="text-[16px] font-medium tracking-[-0.01em] text-paper">
                Moovio
              </span>
            </div>
            <p className="mt-5 max-w-[34ch] text-[14px] leading-[1.6] text-muted">
              One player for the sources you already use. Free, open source, no account.
            </p>
            <div className="mt-6 flex items-center gap-4">
              <Link
                href={GITHUB_URL}
                target="_blank"
                rel="noreferrer"
                aria-label="GitHub repository"
                className="flex h-9 w-9 items-center justify-center rounded-[8px] border border-rule text-muted transition-colors hover:border-paper/30 hover:text-paper"
              >
                <Github className="h-4 w-4" strokeWidth={1.5} />
              </Link>
              <span className="mono text-[11px] text-faint">Android {ANDROID_VERSION}</span>
            </div>
          </div>

          <div className="grid grid-cols-2 gap-x-8 gap-y-10 sm:grid-cols-3">
            {columns.map((c) => (
              <nav key={c.head} aria-label={c.head}>
                <h3 className="border-b border-rule pb-3 text-[13px] text-paper">{c.head}</h3>
                <ul className="mt-4 space-y-2.5">
                  {c.links.map((l) => (
                    <li key={l.label}>
                      <Link
                        href={l.href}
                        target={l.href.startsWith("http") ? "_blank" : undefined}
                        rel={l.href.startsWith("http") ? "noreferrer" : undefined}
                        className="link-quiet text-[13.5px]"
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

        <div className="mt-14 flex flex-col gap-3 border-t border-rule pt-6 text-[12.5px] leading-[1.6] text-faint md:flex-row md:items-start md:justify-between">
          <p>© 2026 Moovio contributors. App MIT · libmpv GPLv2+.</p>
          <p className="max-w-[56ch]">
            No media is hosted or bundled. Moovio plays what your sources return. Not
            affiliated with Stremio, CloudStream, Aniyomi or Mihon.
          </p>
        </div>
      </div>
    </footer>
  );
}
