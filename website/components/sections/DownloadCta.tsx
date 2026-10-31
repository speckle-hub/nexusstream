import Link from "next/link";
import { ArrowUpRight } from "lucide-react";
import {
  ANDROID_APK_SIZE,
  ANDROID_MIN_SDK,
  ANDROID_VERSION,
  APK_URL,
  DESKTOP_MSI_URL,
  DESKTOP_VERSION,
  DESKTOP_ZIP_URL,
  RELEASES_URL,
  TEST_COUNT,
} from "@/lib/utils";

const androidSpec = [
  ["Version", ANDROID_VERSION],
  ["Size", ANDROID_APK_SIZE],
  ["Requires", ANDROID_MIN_SDK],
  ["Tests", TEST_COUNT],
];

export function DownloadCta() {
  return (
    <section id="download" className="border-b border-rule">
      <div className="shell py-24 md:py-28">
        <p className="mono text-[12px] text-amber">00:41:09</p>
        <h2 className="mt-5 max-w-[20ch] text-[clamp(2.2rem,5vw,3.6rem)] font-medium leading-[1.05] tracking-[-0.03em] text-paper">
          Install {ANDROID_VERSION} and point it at your sources.
        </h2>
        <p className="mt-6 max-w-[56ch] text-[16px] leading-[1.65] text-muted">
          The APK is signed with the same key as every 1.11 release, so it updates in place.
          Downloads are served from GitHub Releases.
        </p>

        <div className="mt-10 flex flex-wrap items-center gap-x-6 gap-y-4">
          <Link href={APK_URL} className="btn btn-primary">
            Download the APK
            <span className="mono text-[11px] opacity-70">{ANDROID_APK_SIZE}</span>
          </Link>
          <Link
            href={RELEASES_URL}
            target="_blank"
            rel="noreferrer"
            className="link-quiet text-[14px]"
          >
            All releases{" "}
            <ArrowUpRight className="ml-1 inline h-3.5 w-3.5" strokeWidth={1.5} />
          </Link>
        </div>

        <dl className="mt-12 grid grid-cols-2 gap-x-8 gap-y-6 border-t border-rule pt-7 sm:grid-cols-4">
          {androidSpec.map(([label, value]) => (
            <div key={label}>
              <dt className="mono text-[11px] text-faint">{label}</dt>
              <dd className="mono mt-2 text-[14px] text-paper">{value}</dd>
            </div>
          ))}
        </dl>

        <div className="mt-14 grid gap-8 rounded-[14px] border border-rule bg-ink-raised p-6 md:grid-cols-[1fr_auto] md:items-center md:p-7">
          <div>
            <p className="text-[15px] font-medium text-paper">
              <span className="mono mr-2 text-[12px] text-faint">Windows</span>
              Desktop client v{DESKTOP_VERSION}
            </p>
            <p className="mt-2 max-w-[52ch] text-[14px] leading-[1.6] text-muted">
              A separate native build for Windows 10 and 11, with libmpv playback and a wide
              desktop layout. Same account-free model, its own release cadence.
            </p>
          </div>
          <div className="flex flex-wrap gap-3">
            <Link href={DESKTOP_MSI_URL} className="btn btn-quiet">
              MSI installer
            </Link>
            <Link href={DESKTOP_ZIP_URL} className="btn btn-quiet">
              Portable ZIP
            </Link>
          </div>
        </div>
      </div>
    </section>
  );
}
