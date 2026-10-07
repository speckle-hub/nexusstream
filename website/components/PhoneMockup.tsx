import { Bell, Bookmark, Compass, Home, Library, Play, Search, Settings, Star } from "lucide-react";
import { cn } from "@/lib/utils";

const posters = [
  { g: "from-indigo-500 via-violet-600 to-fuchsia-600", title: "Neon Divide", meta: "2026 · 9.1" },
  { g: "from-sky-500 via-cyan-500 to-teal-400", title: "Tide Signal", meta: "2025 · 8.7" },
  { g: "from-orange-500 via-rose-500 to-red-600", title: "Ash Runner", meta: "2026 · 8.9" },
  { g: "from-emerald-500 via-green-600 to-lime-600", title: "Verdant", meta: "2024 · 8.4" },
  { g: "from-purple-500 via-accent to-blue-600", title: "Low Orbit", meta: "2026 · 9.3" },
];

const rail2 = [
  { g: "from-amber-400 to-orange-600", title: "Sunfall Saga" },
  { g: "from-cyan-400 to-blue-600", title: "Blue Hour" },
  { g: "from-pink-400 to-rose-600", title: "Petal Code" },
  { g: "from-lime-400 to-emerald-600", title: "Moss Vale" },
  { g: "from-violet-400 to-purple-700", title: "Night Ward" },
];

function Poster({ g, title, meta, w = "w-[74px]" }: { g: string; title: string; meta?: string; w?: string }) {
  return (
    <div className={cn("shrink-0", w)}>
      <div className={cn("relative aspect-[2/3] overflow-hidden rounded-lg bg-gradient-to-br shadow-md", g)}>
        <div className="absolute inset-0 bg-gradient-to-t from-black/50 via-transparent to-white/10" />
        {meta && (
          <div className="absolute right-1 top-1 flex items-center gap-0.5 rounded-full bg-black/45 px-1.5 py-0.5 backdrop-blur-sm">
            <Star className="h-[7px] w-[7px] fill-amber-300 text-amber-300" />
            <span className="text-[6px] font-semibold text-white">{meta.split("·")[1]?.trim()}</span>
          </div>
        )}
      </div>
      <div className="mt-1 truncate text-[7px] font-medium text-white/85">{title}</div>
      {meta && <div className="truncate text-[6px] text-white/40">{meta}</div>}
    </div>
  );
}

/**
 * A pure-CSS recreation of the NexusStream home feed in a phone frame —
 * hero banner, poster rails and the floating pill navigation.
 */
export function PhoneMockup({ className }: { className?: string }) {
  return (
    <div className={cn("relative select-none", className)}>
      {/* frame */}
      <div className="relative mx-auto w-[270px] rounded-[44px] border border-white/10 bg-[#0b0b13] p-[10px] shadow-card shadow-glow-soft">
        <div className="absolute left-1/2 top-[18px] z-20 h-[22px] w-[90px] -translate-x-1/2 rounded-full bg-black" />
        {/* screen */}
        <div className="relative h-[560px] overflow-hidden rounded-[34px] bg-void">
          {/* status bar */}
          <div className="absolute left-0 right-0 top-0 z-10 flex items-center justify-between px-6 pt-3 text-[8px] font-medium text-white/80">
            <span>9:41</span>
            <div className="flex items-center gap-1">
              <div className="h-[7px] w-[10px] rounded-[2px] border border-white/60 p-[1px]"><div className="h-full w-3/4 rounded-[1px] bg-white/80" /></div>
            </div>
          </div>

          {/* hero banner */}
          <div className="relative h-[218px] w-full overflow-hidden">
            <div className="absolute inset-0 bg-gradient-to-br from-indigo-600 via-violet-700 to-fuchsia-700" />
            <div className="absolute inset-0 bg-[radial-gradient(ellipse_at_70%_20%,rgba(255,255,255,0.25),transparent_55%)]" />
            <div className="absolute inset-x-0 bottom-0 h-24 bg-gradient-to-t from-void to-transparent" />
            <div className="absolute inset-x-0 bottom-0 flex flex-col items-center pb-3">
              <span className="mb-1 rounded-full border border-white/30 px-2 py-[2px] text-[6px] font-bold tracking-[0.2em] text-white/80">FEATURED</span>
              <div className="font-display text-[15px] font-bold text-white">Aurora Protocol</div>
              <div className="mt-0.5 flex items-center gap-1.5 text-[7px] text-white/70">
                <span>Series</span><span>·</span><span>2026</span><span>·</span>
                <span className="flex items-center gap-0.5"><Star className="h-[7px] w-[7px] fill-amber-300 text-amber-300" />9.2</span>
              </div>
              <div className="mt-2 flex items-center gap-1 rounded-full bg-white px-4 py-1.5 text-[8px] font-bold text-black">
                <Play className="h-[8px] w-[8px] fill-black" /> Play
              </div>
            </div>
            <div className="absolute bottom-[72px] flex w-full justify-center gap-1">
              {[0, 1, 2, 3, 4].map((d) => (
                <div key={d} className={cn("h-[3px] rounded-full", d === 0 ? "w-3 bg-white" : "w-[3px] bg-white/40")} />
              ))}
            </div>
          </div>

          {/* rail 1 */}
          <div className="mt-3 px-3">
            <div className="mb-1.5 flex items-center justify-between">
              <span className="font-display text-[9px] font-semibold text-white/90">Trending Movies</span>
              <span className="text-[7px] text-accent-soft">See all</span>
            </div>
            <div className="flex gap-2 overflow-hidden">
              {posters.map((p) => <Poster key={p.title} {...p} />)}
            </div>
          </div>

          {/* rail 2 */}
          <div className="mt-3 px-3">
            <div className="mb-1.5 flex items-center justify-between">
              <span className="font-display text-[9px] font-semibold text-white/90">Popular Anime</span>
              <span className="text-[7px] text-accent-soft">See all</span>
            </div>
            <div className="flex gap-2 overflow-hidden">
              {rail2.map((p) => <Poster key={p.title} {...p} />)}
            </div>
          </div>

          {/* floating pill nav */}
          <div className="absolute inset-x-4 bottom-2.5 flex items-center justify-between rounded-full border border-white/10 bg-elevated/85 px-4 py-2 backdrop-blur-md">
            <Home className="h-3.5 w-3.5 text-accent-soft" />
            <Search className="h-3.5 w-3.5 text-white/45" />
            <Compass className="h-3.5 w-3.5 text-white/45" />
            <Library className="h-3.5 w-3.5 text-white/45" />
            <Settings className="h-3.5 w-3.5 text-white/45" />
          </div>
        </div>
      </div>

      {/* floating chips */}
      <div className="glass animate-float absolute -left-16 top-24 hidden items-center gap-2 rounded-2xl px-3.5 py-2.5 shadow-card md:flex">
        <Bell className="h-3.5 w-3.5 text-accent-soft" />
        <div>
          <div className="text-[10px] font-semibold text-mist">Download complete</div>
          <div className="text-[9px] text-muted">Aurora Protocol · S1 E4</div>
        </div>
      </div>
      <div className="glass animate-float-slow absolute -right-14 bottom-32 hidden items-center gap-2 rounded-2xl px-3.5 py-2.5 shadow-card md:flex">
        <Bookmark className="h-3.5 w-3.5 text-accent-soft" />
        <div>
          <div className="text-[10px] font-semibold text-mist">Continue Reading</div>
          <div className="text-[9px] text-muted">Chapter 128 · 64%</div>
        </div>
      </div>
    </div>
  );
}
