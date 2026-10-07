import { Github, Scale } from "lucide-react";
import { GITHUB_URL } from "@/lib/utils";
import { LogoMark } from "./LogoMark";

export function Footer() {
  return (
    <footer className="border-t border-white/[0.06] bg-ink/60">
      <div className="mx-auto max-w-6xl px-6 py-14">
        <div className="flex flex-col items-start justify-between gap-10 md:flex-row md:items-center">
          <div>
            <div className="flex items-center gap-2.5">
              <LogoMark className="h-9 w-9" />
              <span className="font-display text-xl font-bold tracking-tight text-mist">
                Nexus<span className="text-gradient">Stream</span>
              </span>
            </div>
            <p className="mt-3 max-w-sm text-sm leading-relaxed text-muted">
              One beautiful Android app for movies, TV, anime and manga.
              Free, open source and source-neutral by design.
            </p>
          </div>

          <div className="flex flex-col gap-4">
            <a
              href={GITHUB_URL}
              target="_blank"
              rel="noreferrer"
              className="flex items-center gap-2 text-sm text-muted transition hover:text-mist"
            >
              <Github className="h-4 w-4" /> github.com/speckle-hub/nexusstream
            </a>
            <div className="text-xs text-muted/70">
              Components: Aceternity UI · HeroUI · ThreeUI · design refs via 21st.dev
            </div>
          </div>
        </div>

        <div className="mt-12 flex items-start gap-3 rounded-2xl border border-white/[0.07] bg-white/[0.02] p-5">
          <Scale className="mt-0.5 h-4 w-4 shrink-0 text-accent-soft" />
          <p className="text-xs leading-relaxed text-muted">
            <span className="font-semibold text-mist">Content neutrality.</span>{" "}
            NexusStream is a player and organizer, not a content source. It ships with no stream
            providers, no provider repositories and no catalogs of its own. Metadata comes from free
            public APIs; any streaming capability comes exclusively from third-party add-ons that
            the user chooses to install. Respect the licenses of content in your region.
          </p>
        </div>

        <div className="mt-10 flex flex-col items-center justify-between gap-3 border-t border-white/[0.05] pt-6 text-xs text-muted/70 md:flex-row">
          <span>© {new Date().getFullYear()} NexusStream contributors. All rights reserved.</span>
          <span>Not affiliated with Stremio, CloudStream, Aniyomi, Mihon or MangaDex.</span>
        </div>
      </div>
    </footer>
  );
}
