import Link from "next/link";
import { Home, Library, Play, Search, Settings } from "lucide-react";
import {
  ANDROID_APK_SIZE,
  ANDROID_MIN_SDK,
  ANDROID_VERSION,
  APK_URL,
  DESKTOP_MSI_URL,
  DESKTOP_VERSION,
  TEST_COUNT,
} from "@/lib/utils";

const facts = [
  ["Minimum", ANDROID_MIN_SDK],
  ["Tests", `${TEST_COUNT} unit`],
  ["Account", "Not required"],
];

const posters = [
  { title: "Nine Rivers", tone: "#191A1C", progress: 62 },
  { title: "Paper Lanterns", tone: "#201C17", progress: 18 },
  { title: "Salt & Static", tone: "#181A18", progress: 84 },
];

export function Hero() {
  return (
    <section id="top" className="relative border-b border-rule">
      <div className="shell">
        <div className="grid gap-16 pb-20 pt-28 lg:grid-cols-[1.02fr_0.98fr] lg:items-center lg:gap-12 lg:pb-28 lg:pt-36">
          <div>
            <p className="rise mono text-[12px] text-muted">
              Android {ANDROID_VERSION} · 31 October 2026
            </p>

            <span aria-hidden className="beam rise rise-2 mt-7 w-14" />

            <h1 className="rise rise-2 mt-8 max-w-[13ch] text-[clamp(2.6rem,6vw,4.5rem)] font-medium leading-[1.04] tracking-[-0.03em] text-paper">
              Every source. One player.
            </h1>

            <p className="rise rise-3 mt-7 max-w-[52ch] text-[16.5px] leading-[1.65] text-muted">
              Moovio plays movies, TV, anime and manga from the sources you already use:
              Stremio add-ons, CloudStream extensions and fourteen built-in scrapers. One
              search, one library, one player, no account.
            </p>

            <div className="rise rise-3 mt-9 flex flex-wrap items-center gap-x-7 gap-y-4">
              <Link href={APK_URL} className="btn btn-primary">
                Download for Android
                <span className="mono text-[11px] opacity-70">{ANDROID_APK_SIZE}</span>
              </Link>
              <Link href={DESKTOP_MSI_URL} className="link-quiet text-[14px]">
                Windows build <span className="mono text-[12px]">v{DESKTOP_VERSION}</span>
              </Link>
            </div>

            <dl className="mt-12 grid max-w-md grid-cols-3 gap-6 border-t border-rule pt-6">
              {facts.map(([k, v]) => (
                <div key={k}>
                  <dt className="mono text-[11px] text-faint">{k}</dt>
                  <dd className="mt-2 text-[14px] text-paper">{v}</dd>
                </div>
              ))}
            </dl>
          </div>

          <div className="rise rise-3 flex justify-center lg:justify-end">
            <PhoneView />
          </div>
        </div>
      </div>
    </section>
  );
}

/**
 * The real Home feed at phone scale. This is the page's one rich surface: the still and the
 * poster wall carry the colour, everything around them is a hairline and a label.
 */
function PhoneView() {
  return (
    <div className="w-full max-w-[308px]">
      <div className="rounded-[34px] border border-rule bg-ink-raised p-2 shadow-[0_30px_60px_-40px_rgba(0,0,0,0.9)]">
        <div className="overflow-hidden rounded-[26px] border border-rule-soft bg-ink">
          <div className="flex items-center justify-between px-4 pt-3">
            <span className="mono text-[10px] text-faint">9:41</span>
            <span className="mono text-[10px] text-faint">5G</span>
          </div>

          <div className="flex items-center justify-between px-4 pb-3 pt-5">
            <span className="text-[15px] font-medium text-paper">Home</span>
            <Search className="h-4 w-4 text-muted" strokeWidth={1.5} />
          </div>

          <div className="mx-4 overflow-hidden rounded-[12px] border border-rule-soft">
            <div className="relative h-[124px] w-full">
              <div
                className="absolute inset-0"
                style={{
                  background:
                    "radial-gradient(120% 95% at 78% 0%, #3D2C19 0%, #191410 52%, #0C0B0A 100%)",
                }}
              />
              <div className="absolute left-3 top-3 flex items-center gap-1.5 rounded-full bg-amber px-2 py-[3px] text-[10px] font-medium text-ink">
                <Play className="h-2.5 w-2.5" strokeWidth={2} />
                Play
              </div>
              <div className="absolute inset-x-3 bottom-3">
                <p className="text-[13px] font-medium text-paper">Aurora Protocol</p>
                <p className="mono mt-1 text-[10px] text-muted">2026 · S1 E4 · 47 min</p>
              </div>
            </div>
          </div>

          <div className="px-4 pt-4">
            <p className="mono text-[10px] text-faint">Continue watching</p>
            <div className="mt-2 grid grid-cols-3 gap-2">
              {posters.map((p) => (
                <div key={p.title}>
                  <div
                    className="relative aspect-[2/3] overflow-hidden rounded-[8px] border border-rule-soft"
                    style={{
                      background: `linear-gradient(180deg, ${p.tone} 0%, #0C0C0B 100%)`,
                    }}
                  >
                    <span className="absolute inset-x-1.5 bottom-2 line-clamp-2 text-[9px] leading-[1.25] text-paper/90">
                      {p.title}
                    </span>
                  </div>
                  <div className="mt-1.5 h-px w-full bg-rule">
                    <div className="h-px bg-amber/70" style={{ width: `${p.progress}%` }} />
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="mt-4 flex items-center justify-around border-t border-rule-soft py-3">
            <Home className="h-4 w-4 text-paper" strokeWidth={1.5} />
            <Search className="h-4 w-4 text-faint" strokeWidth={1.5} />
            <Library className="h-4 w-4 text-faint" strokeWidth={1.5} />
            <Settings className="h-4 w-4 text-faint" strokeWidth={1.5} />
          </div>
        </div>
      </div>
    </div>
  );
}
