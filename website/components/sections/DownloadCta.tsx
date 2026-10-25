"use client";

import { ArrowDownToLine, ExternalLink, Monitor, FileText, Download } from "lucide-react";
import Link from "next/link";
import {
  DESKTOP_ZIP_URL,
  DESKTOP_MSI_URL,
  APK_URL,
  RELEASES_URL,
  cn,
} from "@/lib/utils";
import { Button } from "@/components/ui/button";
import { Stagger } from "../ui/reveal";

const spec = [
  { label: "Platform", value: "Windows 10/11 x64" },
  { label: "Installer", value: "MSI or portable ZIP" },
  { label: "Size", value: "≈180 MB" },
  { label: "JDK", value: "Bundled (JDK 21)" },
  { label: "Player", value: "libmpv (D3D11)" },
];

export function DownloadCta() {
  return (
    <section id="download" className="relative overflow-hidden border-t border-rule">
      <div
        aria-hidden
        className="absolute left-1/2 top-1/2 h-[520px] w-[820px] -translate-x-1/2 -translate-y-1/2 rounded-full bg-accent/15 blur-[140px]"
      />
      <div className="relative mx-auto max-w-editorial px-5 py-24 sm:px-8 md:py-36">
        <Stagger
          items={[
            <p key="eyebrow" className="eyebrow text-accent-soft">
              Get Moovio Desktop
            </p>,
            <h2
              key="h2"
              className="display mt-4 font-display font-bold text-paper"
              style={{ fontSize: "clamp(2.75rem, 8vw, 5.5rem)" }}
            >
              Install on Windows.{" "}
              <span className="text-gradient block">Stream everywhere.</span>
            </h2>,
            <p key="deck" className="mt-8 max-w-xl text-[17px] leading-relaxed text-muted md:text-[20px]">
              Choose the MSI installer for a proper Windows install with
              Start Menu entry, or the portable ZIP to run Moovio.exe from
              anywhere. Both include libmpv and are signed for v0.1.0.
            </p>,
            <div key="ctas" className="mt-12 flex flex-wrap items-center gap-4">
              <Link
                href={DESKTOP_MSI_URL}
                className={cn(
                  "press inline-flex items-center justify-center gap-3 rounded-xl px-10 py-5 text-[16px] font-semibold tracking-tight shadow-glow-accent transition-colors",
                  "bg-[#7C5CFF] text-white hover:bg-[#6a4cd9]",
                )}
              >
                <Monitor className="h-5 w-5" />
                Windows Installer (MSI)
              </Link>
              <Link
                href={DESKTOP_ZIP_URL}
                className={cn(
                  "press inline-flex items-center justify-center gap-3 rounded-xl px-10 py-5 text-[16px] font-semibold tracking-tight transition-colors",
                  "bg-[#0a0a11] border border-white/20 text-white hover:bg-[#12121e] hover:border-white/40",
                )}
              >
                <FileText className="h-5 w-5" />
                Portable ZIP
              </Link>
              <Link
                href={APK_URL}
                className={cn(
                  "press inline-flex items-center justify-center gap-3 rounded-xl px-10 py-5 text-[16px] font-semibold tracking-tight transition-colors",
                  "bg-white/[0.04] text-paper hover:bg-white/[0.08]",
                )}
              >
                <Download className="h-5 w-5" />
                Android APK
              </Link>
            </div>,
            <div key="spec" className="mt-16 block border-t border-rule pt-8">
              <div className="grid grid-cols-2 gap-x-8 gap-y-6 sm:grid-cols-5">
                {spec.map((s) => (
                  <div key={s.label}>
                    <dt className="eyebrow text-[10px] text-faint">{s.label}</dt>
                    <dd className="tabular mt-2 font-display text-[15px] font-semibold tracking-tight text-paper">
                      {s.value}
                    </dd>
                  </div>
                ))}
              </div>
            </div>,
            <div key="releases" className="mt-12 flex items-center gap-3 text-muted">
              <span className="eyebrow text-[11px]">All releases:</span>
              <Link
                href={RELEASES_URL}
                target="_blank"
                rel="noreferrer"
                className="press text-[14px] text-paper hover:text-accent-soft transition-colors"
              >
                GitHub Releases <ExternalLink className="h-3.5 w-3.5 ml-1" />
              </Link>
            </div>,
          ]}
        />
      </div>
    </section>
  );
}
