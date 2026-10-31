import Link from "next/link";
import { GITHUB_URL } from "@/lib/utils";

const faqs = [
  {
    q: "What is Moovio?",
    a: "An Android app that plays movies, TV, anime and manga from sources you point it at: Stremio add-ons, CloudStream extensions and built-in scrapers. There is also a separate Windows client built with Compose Multiplatform and libmpv.",
  },
  {
    q: "Which download should I take?",
    a: "For the phone, the signed APK from GitHub Releases: 44.1 MB, Android 7.0 or newer, installs in place over 1.11.0. The Windows client is a separate v0.1.1 build, available as an MSI installer or a portable ZIP.",
  },
  {
    q: "Do I need an account?",
    a: "No. There is no sign-in, no sync service and no telemetry. Trackers for Trakt, AniList and MyAnimeList are optional and use your own client credentials.",
  },
  {
    q: "Does Moovio come with content?",
    a: "No. Nothing is bundled or hosted. Moovio is a player and a library: it shows what the add-ons and sources you install return, and whether a given source works depends on that source, not on the app.",
  },
  {
    q: "How is the adult section handled?",
    a: "It sits behind a PIN or biometric lock, unlocks for the current session only, and is excluded from Home, Search and Library unless you opt in from inside the hub. Adult sources sit behind a switch, and the tab says plainly when a source is unreachable instead of showing an empty grid.",
  },
  {
    q: "Is it open source?",
    a: "Yes. The app is MIT-licensed and the repository is public, with 335 passing unit tests and release builds signed and attached to each GitHub release.",
  },
];

export function Faq() {
  return (
    <section id="faq" className="border-b border-rule">
      <div className="shell py-24 md:py-28">
        <div className="grid gap-10 lg:grid-cols-[0.72fr_1fr] lg:gap-16">
          <div>
            <p className="mono text-[12px] text-amber">00:36:22</p>
            <h2 className="mt-5 max-w-[16ch] text-[clamp(1.9rem,3.6vw,2.9rem)] font-medium leading-[1.1] tracking-[-0.02em] text-paper">
              Questions worth answering before you install.
            </h2>
            <p className="mt-5 max-w-[34ch] text-[14px] leading-[1.6] text-muted">
              If something here is missing, the issue tracker is the right place for it.
            </p>
            <Link
              href={`${GITHUB_URL}/issues`}
              target="_blank"
              rel="noreferrer"
              className="link-quiet mt-4 inline-block text-[13.5px] underline decoration-rule underline-offset-4"
            >
              Open an issue
            </Link>
          </div>

          <div>
            {faqs.map((f, i) => (
              <details
                key={f.q}
                className="group border-t border-rule last:border-b"
                open={i === 0}
              >
                <summary className="flex cursor-pointer list-none items-start gap-5 py-5 [&::-webkit-details-marker]:hidden">
                  <span className="mono mt-1 shrink-0 text-[11px] text-faint">
                    {String(i + 1).padStart(2, "0")}
                  </span>
                  <span className="flex-1 text-[15.5px] font-medium leading-[1.4] text-paper">
                    {f.q}
                  </span>
                  <span
                    aria-hidden
                    className="mt-0.5 shrink-0 text-[16px] leading-none text-muted transition-transform duration-200 group-open:rotate-45"
                  >
                    +
                  </span>
                </summary>
                <p className="max-w-[62ch] pb-6 pl-[44px] pr-2 text-[14px] leading-[1.65] text-muted">
                  {f.a}
                </p>
              </details>
            ))}
          </div>
        </div>
      </div>
    </section>
  );
}
