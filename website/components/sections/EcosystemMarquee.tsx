import { Blocks, Database, Film, Globe, Layers, LibraryBig, Package, Puzzle, Rss, Tv } from "lucide-react";
import { InfiniteMovingCards } from "../ui/infinite-moving-cards";

const items = [
  { icon: <Puzzle className="h-5 w-5" />, name: "Stremio", detail: "Full add-on protocol" },
  { icon: <Blocks className="h-5 w-5" />, name: "CloudStream", detail: "Provider repo support" },
  { icon: <Package className="h-5 w-5" />, name: "Aniyomi", detail: "Anime extension repos" },
  { icon: <LibraryBig className="h-5 w-5" />, name: "Mihon / Keiyoushi", detail: "Manga extension repos" },
  { icon: <Film className="h-5 w-5" />, name: "TMDB", detail: "Movie & TV metadata" },
  { icon: <Tv className="h-5 w-5" />, name: "AniList", detail: "Anime metadata & lists" },
  { icon: <Rss className="h-5 w-5" />, name: "MangaDex", detail: "Manga catalog & chapters" },
  { icon: <Globe className="h-5 w-5" />, name: "Jikan", detail: "MyAnimeList bridge" },
  { icon: <Layers className="h-5 w-5" />, name: "Media3 ExoPlayer", detail: "HLS · DASH · downloads" },
  { icon: <Database className="h-5 w-5" />, name: "OpenSubtitles", detail: "Subtitle provider" },
];

export function EcosystemMarquee() {
  return (
    <section id="ecosystem" className="relative py-10 md:py-14">
      <p className="mb-7 text-center text-xs font-semibold uppercase tracking-[0.28em] text-muted">
        Plays well with the ecosystems you already use
      </p>
      <InfiniteMovingCards items={items} speed="slow" />
    </section>
  );
}
