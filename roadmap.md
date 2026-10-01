# NexusStream — Roadmap & Working Notes

> Living document. Update the **Status** and **Changelog** sections every session so work can
> resume exactly where it left off. Last updated: 2026-10-01 (Phase 4 wrap-up + library removals).
>
> **Name change:** the app is now **NexusStream** (display name only — the Kotlin package and
> `applicationId` remain `com.novastream.app` so existing installs update in place; see §8).

---

## 1. What this project is

Android streaming app (`com.novastream.app`) unifying Stremio add-ons, CloudStream extensions,
Aniyomi and Mihon/Keiyoushi repos into one Compose UI. Kotlin + Jetpack Compose (M3), Media3
ExoPlayer, Coil, OkHttp/Gson/Retrofit, DataStore, manual DI (`di/AppContainer.kt`).
~6,600 LOC across 62 Kotlin files in a single `:app` module. Unit tests (30 cases):
`app/src/test/java/com/novastream/app/ParsersTest.kt` (9), `AdultSourcesTest.kt` (13),
`SearchCancellationTest.kt` (2), `PaletteCacheTest.kt` (3) and `MangaDownloadTest.kt` (3).

---

## 2. Current focus

**Topic: Phase 5 — library management (remove from Favourites / Continue Watching).**

Status: **Phase 5 complete.** Build green (30 tests / 0 failures). Both curated lists are now
editable: Favourites and Continue Watching can be removed from the Library grid, and Continue
Watching can be removed straight from the Home row. No APK has been rebuilt yet — a device pass
(§7) is still pending for this phase.

### Rename (display name only)
`strings.xml` app label, the Home title and Settings "About" row, and the shared HTTP User-Agent
now read **NexusStream**. Package/`applicationId` intentionally unchanged (in-place updates).

### Phase 1 — UI/UX polish (complete)
- **Home:** hero banner got an ambient drop-shadow + hairline outline so dark art separates from
  the background; `PosterCard` now uses `padding(top = 6.dp)` and a single-line metadata row in
  `textSecondary` (`year · rating`); all poster corners unified via a shared `PosterShape` (12.dp).
- **Nav:** compact active pill (smaller icon) + one cohesive Material Filled icon set.
- **Manga/NSFW:** Manga's "Powered by MangaDex" is now a right-aligned `SourceBadge` pill; NSFW
  tab row has a 12.dp spacer above the search field; rating badges are 0.5-alpha glass overlays.
- **Search/Library:** `EmptyState` gained a centered vector-icon anchor; unselected `TagChip`s
  now carry an explicit outline so they read as clickable.
- **Settings/Add-ons:** clickable settings rows show a chevron; the active accent swatch has an
  outer ring; the Stremio add-on list shows `[Stremio] [N Catalogs] [NSFW]` chips instead of a raw
  metadata dump.

### Phase 2 — core features (complete)
- **NSFW auto-lock on background** — `NovaApp` counts started activities and resets
  `nsfwUnlocked=false` only when the whole app is backgrounded (opening the player does not lock).
- **Auto NSFW from add-ons** — `realRowsWithErrors()`/`searchReal()` now honor the
  `autoNsfwFromAddons` flag (Real 18+ excludes Stremio NSFW add-ons when off).
- **Last-tab restore** — the active tab is persisted on change and restored on cold boot.
- **Disk + image caching** — `MetadataCache` wired through a new `Http.cache` + `httpGetCached` /
  `httpPostJsonCached` (6 h TTL) for TMDB, AniList and MangaDex; Coil gets an explicit
  `ImageLoader` with a 256 MB disk cache + memory cache in `NovaApp`.
- **Preferred quality / subtitles** — `PlayerActivity` reads `preferredQuality` (caps max video
  size) and `subsEnabled` (skips subtitle configs when off).
- **Autoplay next episode** — `DetailScreen` passes the ordered episode list + current index;
  `PlayerActivity` shows a 5-second countdown at 95%/end and can jump straight to the next one.
- **Player QoL** — edge-drag brightness (left) / volume (right), horizontal seek, audio-track
  selection in the track sheet, and an episode side-sheet drawer.
- **Manga reader modes** — Webtoon (vertical scroll), Right-to-Left, and Left-to-Right, with
  3-page preloading (pager beyond-bounds + explicit Coil enqueues).
- **Repository index sync** — a network-constrained `RepoSyncWorker` (12 h) refreshes
  CloudStream/Aniyomi/Keiyoushi indexes; repo clients read through the disk cache; a manual
  “Sync” action exists in the Add-on Manager.
- **Extension execution engine** — `data/ext/` adds a `NexusExtension` contract, a sandboxed
  `DexClassLoader` host (app-private storage only), and an `ExtensionRepository` that imports,
  loads, persists and unloads `.apk`/`.dex`/`.extension` files; installed extensions re-load on
  start. See the trust-model note in §8.

### Phase 3 — device-report fixes (complete)
- **Home navigation (ISSUE-11).** Opening Settings then tapping **Home** used to bounce straight
  back to Settings. The cold-boot "restore last tab" effect keyed off `currentRoute == HOME`, so
  returning Home re-triggered it and re-navigated to the saved tab — Home flashed and reverted.
  The restore is now a true one-shot: it reads the persisted index via a new
  `SettingsStore.lastTabSnapshot()`, fires once from `LaunchedEffect(Unit)` only while still on the
  start destination, and a `restored` flag gates tab persistence until it has settled.
  `ui/nav/NovaNav.kt`, `data/local/SettingsStore.kt`.
- **Real 18+ speed (ISSUE-12).** Home rows are now emitted **progressively** — `AdultRepository.home`
  takes an `onUpdate` callback invoked after *each* source finishes, so the tab paints the first
  rows and drops its skeleton instead of waiting for the slowest site. Each source is cached
  **individually** (`SOURCE_TTL_MS` 10 min), so re-entering the tab serves cached sources instantly
  and only refetches stale ones; rows are always emitted in registry order. `NsfwViewModel` consumes
  the progressive stream; `CatalogRepository.realRowsProgressive` appends Stremio NSFW rows in a
  final snapshot. `data/adult/AdultRepository.kt`, `data/repo/CatalogRepository.kt`, `ui/vm/ViewModels.kt`.
- **Continue Watching (ISSUE-13).** `LibraryStore.recordWatch` existed but was **never called** —
  the player didn't persist anything, so the section could never populate. `PlayerActivity` now
  snapshots playback (title, episode, position, duration) every ~5 s while playing, on episode
  switch, and in `onPause`/`onStop`/`onDestroy`, writing through a new app-lifetime
  `NovaApp.appScope` so the save survives Activity teardown. It also **resumes** at the last saved
  position (unless ≥95% watched). Home filters out finished titles (>95%) and shows the rest.
  `ui/player/PlayerActivity.kt`, `NovaApp.kt`, `ui/vm/ViewModels.kt`.
- **Home carousel (ISSUE-14).** The single static hero became an auto-sliding `HorizontalPager`
  carousel: swipeable featured banners (trending first, backdrop art preferred, up to 8) with a
  title, rating/year/genres, a 2-line description and a Play button, plus animated dot indicators.
  Auto-advance every 5 s pauses while the user is dragging. `ui/screens/home/HomeScreen.kt`.

### Phase 4 — dynamic theming + offline downloads (complete)
- **Dynamic poster palette (Material You style).** `ui/theme/PosterPalette.kt` derives
  vibrant/dark-vibrant/muted/dominant swatches from artwork via Coil + the Palette API, caches the
  RGB values in the shared metadata cache (30-day TTL), and exposes `rememberPosterPalette`. The
  detail header and home hero use `animateColorAsState` for artwork-tinted gradient scrims, an
  ambient radial halo, the FEATURED label and the primary CTA tint.
- **Offline manga chapter downloader.** `data/download/MangaDownloadManager.kt` queues chapter
  images into `files/downloads/manga/{mangaId}/{chapterId}/` with a reactive
  `StateFlow<Map<String, MangaDownload>>` (queued/downloading/paused/completed/failed), an
  `index.json` of ordered page filenames and a resumable `meta.json`. `MangaReaderActivity` is
  offline-first (renders local files when present) and has a download action; `LibraryScreen`
  lists chapters with progress, speed, pause/resume/delete.
- **Native video downloader (Media3).** `data/download/DownloadManagerProvider.kt` owns one shared
  `SimpleCache` (`files/downloads/media`, 4 GB LRU) + `StandaloneDatabaseProvider` + a Media3
  `DownloadManager` (3 parallel downloads, `Requirements.NETWORK`). `NovaDownloadService` is a
  foreground (`dataSync`) `DownloadService` that renders progress via `DownloadNotificationHelper`;
  `VideoDownloadManager` enqueues progressive MP4 and HLS through `DownloadService.sendAddDownload`
  (HLS hinted with `MimeTypes.APPLICATION_M3U8`, original item/URL stashed in `DownloadRequest.data`)
  and maps `Download`s to a reactive `StateFlow<VideoDownload>` with status, %, bytes, speed, ETA and
  pause/resume/delete (`setStopReason`). Media3's own downloaders handle alternate audio, byte-range
  and `#EXT-X-KEY` playlists. `PlayerActivity` reads through the same `CacheDataSource`, so a
  completed download plays offline automatically; the Detail stream list keeps a per-stream download
  action; `LibraryScreen` has a downloaded-videos section with offline playback.
- **Bulk "Download season" action.** Every season row on `DetailScreen` has a download button that
  resolves each episode's streams (sequentially, to spare the add-ons) and queues the first playable
  one — one tap to queue a whole season. The button swaps to a spinner while it resolves, and a toast
  reports how many episodes were queued.
- **Tests:** `PaletteCacheTest` (3), `MangaDownloadTest` (3) → **30 tests** (the naive-HLS
  `HlsDownloadTest` was removed along with the WorkManager downloader it covered).

### Phase 5 — library removal actions (complete)
- **Favourites removal (Library → Favourites).** Every poster in the Favourites grid carries a
  small `×` badge (TopStart, so it never collides with the rating badge) and also responds to a
  long-press; both open the same `ConfirmRemoveDialog` ("Remove from favourites?"). The Detail
  screen's heart/"In Library" button keeps working as before for un-favouriting from there.
- **Continue Watching removal (Home row + Library grid).** The Home carousel-style row has a `×`
  on each card's thumbnail (and long-press works too); the Library's Continue Watching grid uses
  the same `×`/long-press affordances as Favourites. Confirming clears the saved watch progress
  via the new `LibraryStore.removeWatch(key)`, so the entry disappears from both screens at once
  (both read the same DataStore flow).
- **Plumbing.** `LibraryStore` gained non-toggling `removeFavorite(key)` / `removeWatch(key)`
  (the old `LibraryViewModel.removeFavorite` called `toggleFavorite`, which could re-add an
  item that was already gone); `HomeViewModel.removeContinueWatching(entry)` and
  `LibraryViewModel.removeWatch(key)` wrap them. `PosterCard` gained optional `onLongClick` /
  `onRemove` parameters (default `null` → no change for existing call sites), and
  `ui/components/Components.kt` gained a shared `ConfirmRemoveDialog` so both remove paths are
  worded identically and always require a deliberate second tap.

---

## 2a. Search reliability (whole app + Real 18+)

**Topic: Search reliability (whole app + Real 18+).**

Status: **code fixes complete**, rebuilt both APKs; needs a device re-test (§7).

### Whole-app search (ISSUE-10)
Symptom: any search sometimes "glitches" and shows a completely different result set. Root cause:
`SearchScreen` fired `SearchViewModel.search` on **every keystroke** with no debounce and no
cancellation, so responses landed out of order (results for an earlier prefix overwrote the final
query); switching the filter chip raced with an in-flight global search; and — the subtle one —
`runCatching` **swallows `CancellationException`**, so a "cancelled" search kept running and could
still publish. Fixed with a 350 ms debounce, per-query job cancellation, a monotonic generation id
so only the newest request may publish, and a `runCatchingCancellable` helper that rethrows
cancellation. Applied to the main search and to all three NSFW section searches.

### Real 18+ search (ISSUE-9)
What was wrong and what changed (see ISSUE-9):
- Search fired a network request on **every keystroke**, with no debounce and no cancellation, so
  typing one word fired ~25 concurrent requests, hammered the tube sites into rate-limiting
  (empty results), and let a slow earlier response overwrite a newer query's results — the
  "confused / irrelevant" symptom. `NsfwViewModel` now debounces 350 ms and cancels the previous
  job; queries shorter than 2 chars never reach the network.
- `AdultRepository.search` swallowed every failure and returned `[]` with no explanation. It now
  **retries each source once** (timeouts/blocks are transient) and returns `Search(items, errors)`, so
  the UI can say *why* an empty search is empty.
- Results are **relevance-filtered and ranked** against the query keywords: only titles containing
  at least one keyword are shown (most matches first), with an automatic fallback to the unfiltered
  set if the filter would otherwise empty a non-empty result — search never returns nothing while
  real results exist.
- Real 18+ search now treats the **built-in sources as authoritative**; NSFW Stremio add-ons are
  queried only as a fallback when the built-ins return nothing, so add-on results can't pollute or
  outrank them.

Verification: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **24 tests / 0 failures** ✅;
`:app:assembleDebug` + `:app:assembleRelease` ✅ (43 MB debug / 36 MB release). Needs a **device pass**.

---

## 2b. Previous focus — Real 18+ device-report fixes (slow load, wrong titles/thumbs, streams that won't play).

Status: **code fixes complete**, rebuilt both APKs; needs a device re-test (§7).

What was wrong and what changed:
- **Pornhub titles** were reading `data-title="Content Partner"`; now reads the `.title` block /
  anchor `title=` / img `alt` (with a lookbehind so the partner badge can never win). Thumbnails
  now come from `src`/`data-poster`/`data-path` as well as `data-image`. Detail title/thumb fall
  back to `og:title`/`og:image`.
- **HQporner titles** were reading the `play images` slideshow trigger; now prefers `<h2>`, then
  anchor text, then `click-trigger` (junk-filtered), then `alt`, then the slug. Streams: the first
  `<iframe>` is an ad — the player frame (`mydaddy.cc/video/…`) is used instead, and the embed's
  **protocol-relative** `//host/….mp4` URLs (previously invisible to an `https?://` regex) are now
  captured, labelled from URL/`label` context, and retried once.
- **Streams listed but wouldn't play**: Pornhub's `hm-h.phncdn.com` answers 410/412 without a
  `Referer` of the exact `view_video.php` page **and** the age cookies. `StreamSource` gained
  `headers`, every adult source populates them, `PlayerActivity` plays through an
  OkHttpDataSource that replays them, and `AdultHttp.verifyPlayable()` pre-flights each URL with
  the same headers — only definitively-dead links are dropped (timeouts/429 are kept), so Play
  lights up only when something real will play.
- **Slow Real 18+**: listings and category rows now fetch **concurrently** per source
  (`adultHome`), the three NSFW sections load **in parallel with per-section flags** (Real fills
  independently of Anime/Manga), `AdultRepository` short-TTL caches home (60 s) and detail (10 min
  — also kills the double ~1 MB detail download), and search empty-state waits for
  `loading || searching`.

Verification: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → 21 tests / 0 failures ✅;
`:app:assembleDebug` + `:app:assembleRelease` ✅ (both APKs rebuilt). Still needs a **device pass**.

Every remote endpoint the app calls was probed live with the exact URLs/params used in code.
Findings are tracked as issues below.

*How the audit was run:* `curl` against each endpoint, replicating the request the client builds
(TMDB key, AniList GraphQL bodies, MangaDex query params, Stremio protocol URLs, extension repo
URLs). Re-run any time with the commands in §5.

---

## 3. API status snapshot (2026-09-30)

| Source | Verdict | Notes |
|---|---|---|
| TMDB (`fbc3631...`) | ✅ working | popular/top_rated/now_playing/upcoming, tv/*, trending, discover, search/multi (`include_adult=true` honored), find, external_ids, detail `append_to_response`, season episodes |
| AniList GraphQL | ✅ working | list, `isAdult:true` NSFW, detail query |
| MangaDex | ✅ working | `/manga` (safe + nsfw + tags), `/feed`, `/at-home` — 200 (429s if hammered) |
| Stremio Cinemeta | ✅ working | manifest/meta/catalog/search (catalog issues a 307 that OkHttp follows) |
| Stremio OpenSubtitles v3 | ✅ working | manifest + subtitles |
| Aniyomi repo (yuzono) | ✅ working | `index.min.json` matches parser |
| Keiyoushi mirror | ✅ working | via `index.min.json` mirror (JSON parser) |
| Jikan (plain) | ✅ working | `/top/anime`, `/anime` without filters |
| Jikan (filtered) | ❌ down | `filter=` / `rating=` / `sfw=` → **504** upstream (MyAnimeList unreachable) |
| v3-chill.strem.io (seeded) | ❌ dead | DNS NXDOMAIN — was the only seeded stream add-on |
| CloudStream repo (phisher98) | ❌ broken client | repo.json is a manifest (`pluginLists`), parser expects `extensions[]`/`data[]` |

> Snapshot taken at audit time. The three ❌ rows are **client-side** issues that have since been
> fixed (ISSUE-1, 2, 3 in §4); the Jikan 504 itself remains an upstream outage that the client now
> works around.

---

## 4. Issue backlog

Legend: 🔴 core feature broken · 🟠 degraded · 🟡 latent/cosmetic · ✅ done

### ✅ ISSUE-1 — Seed a working stream add-on (was: dead default `v3-chill`)
- **Symptom:** fresh install has no stream provider (Cinemeta = metadata, OpenSubtitles = subs),
  so detail pages list no playable sources.
- **Root cause:** `AddonStore.Defaults.stremioAddons` seeded `https://v3-chill.strem.io/manifest.json`,
  which no longer resolves.
- **Fix:** replace with verified-live `torrentio.strem.fun` + `thepiratebay-plus.strem.fun`;
  dedupe the list; add a migration that prunes retired hosts and re-seeds replacements for
  existing installs (default seeding only ran on an empty list before).
- **Files:** `data/local/AddonStore.kt`, `data/repo/AddonRepository.kt`

### ✅ ISSUE-2 — CloudStream repo parser returns nothing
- **Symptom:** CloudStream provider browser always empty.
- **Root cause:** `CloudStreamRepo.fetch` looks for `extensions[]`/`data[]`; phisher98 `repo.json`
  is `{name, iconUrl, pluginLists:[ "<url to plugins.json>" ]}` — the real provider array lives
  one hop away and is an array of `{name, url, description, iconUrl, version, language, tvTypes, ...}`.
- **Fix:** resolve `pluginLists[]`, fetch each plugin list, parse arrays; also accept inline
  `extensions`/`data`/`plugins`; dedupe by url.
- **Files:** `data/remote/CloudStreamRepo.kt`

### ✅ ISSUE-3 — Jikan filtered queries 504 → NSFW anime/search empty
- **Symptom:** `JikanClient.nsfwTop()` / `nsfwSearch()` fail; silently degrade to empty.
- **Root cause:** Jikan 504s on `filter=`/`rating=`/`sfw=` while upstream MAL is unreachable.
- **Fix:** retry filtered query, then fall back to the plain endpoint and filter to adult
  ratings client-side (`rating` field starts with `Rx` / `R+`).
- **Files:** `data/remote/JikanClient.kt`
- **Note:** server-side fault; fallback keeps content flowing.

### ✅ ISSUE-4 — Keiyoushi `index.pb` fallback is dead (gzip)
- **Symptom:** latent — the `.pb` path cannot parse.
- **Root cause:** GitHub serves `index.pb` **gzip-compressed** (`1f 8b 08`); `httpGetBytes` →
  `Protobuf.decode` never decompresses.
- **Fix:** detect gzip magic and inflate before decoding. Mirrors still work today, but the
  fallback is no longer dead.
- **Files:** `data/remote/Http.kt`, `data/remote/KeiyoushiRepo.kt`

### ✅ ISSUE-5 — MangaDex external chapters open to a blank reader
- **Symptom:** licensed titles (e.g. One Piece → MangaPlus) list chapters that show no pages.
- **Root cause:** chapters carrying `externalUrl` have no at-home pages, but `chapters()` lists them.
- **Fix:** skip chapters that declare `externalUrl` so the reader only offers readable chapters.
- **Files:** `data/remote/MangaDexClient.kt`

### ✅ ISSUE-6 — MangaDex at-home rate limiting (429)
- Added a `RetryOn429Interceptor` to `Http.client`: retries **GET** requests up to 3× honouring
  `Retry-After`, else exponential backoff 500ms→2s (capped 5s). Benefits every client.
- **Files:** `data/remote/Http.kt`

### ✅ ISSUE-7 — `TmdbClient.searchAnime` sent `with_genres` to `/search/tv`
- TMDB ignores `with_genres` on search. Now fetches `/search/tv` and keeps only results whose
  `genre_ids` contains 16 (Animation), client-side.
- **Files:** `data/remote/TmdbClient.kt` (note: `TmdbClient.searchAnime` is currently unused —
  `CatalogRepository` uses `AniListClient.searchAnime` — but it is correct now).

### ✅ ISSUE-10 — Whole-app search "glitches" and shows a different result set
- **Symptom:** searching anywhere sometimes returns a completely different set of results than
  the typed query.
- **Root cause:** (a) `SearchScreen` called `vm.search` on **every keystroke** with no debounce and
  no cancellation, so out-of-order responses could overwrite the final query; (b) the filter-chip
  handler raced with an in-flight global search; (c) `runCatching { … }` catches
  `CancellationException`, so even a "cancelled" search ran to completion and published.
- **Fix:** `SearchViewModel` debounces (350 ms), cancels the previous job, and stamps each request
  with a monotonic generation id so only the newest result may publish; `runCatchingCancellable`
  rethrows cancellation instead of swallowing it; same guard applied to `NsfwViewModel`'s three
  section searches.
- **Files:** `ui/vm/ViewModels.kt`, `ui/screens/search/SearchScreen.kt` (call sites unchanged)

### ✅ ISSUE-9 — Real 18+ search unreliable (empty / stale / irrelevant)
- **Symptom:** Real 18+ search sometimes returns correct results, sometimes nothing or wrong /
  irrelevant ones.
- **Root cause:** (a) the search box fired a request on **every keystroke** with no debounce or
  cancellation, so a single word produced dozens of concurrent requests (rate-limit/empty) and an
  older response could land after a newer one; (b) `AdultRepository.search` swallowed all errors →
  silent empty; (c) no relevance filtering; (d) built-in and Stremio NSFW add-on results were mixed
  with no priority.
- **Fix:** debounce (350 ms) + per-section job cancellation in `NsfwViewModel`; per-source **retry**
  and `Search(items, errors)` in `AdultRepository.searchDetailed`; keyword relevance filter + ranking
  (with unfiltered fallback); `CatalogRepository.searchReal` makes built-ins authoritative and
  queries Stremio NSFW add-ons only as a fallback; UI surfaces search errors when results are empty.
- **Files:** `data/adult/AdultRepository.kt`, `data/adult/AdultSource.kt`, `data/repo/CatalogRepository.kt`,
  `ui/vm/ViewModels.kt`, `ui/screens/nsfw/NsfwScreen.kt`

### ✅ ISSUE-8 — Repo hygiene (2 decisions made — see §8)
- ✅ Added `.gitignore` (secrets, build output, IDE).
- ✅ Externalized signing credentials: `app/build.gradle.kts` now reads an untracked
  `keystore.properties` (created locally) or env vars `NOVASTREAM_STORE_PASSWORD` /
  `NOVASTREAM_KEY_ALIAS` / `NOVASTREAM_KEY_PASSWORD`. Password no longer in the build script.
- ✅ Added a unit-test source set + `junit:junit:4.13.2`; 9 parser tests (Protobuf, gzip,
  Keiyoushi index, Stremio manifest, CloudStream provider).
- ✅ **Keystore rotation — decided NOT to rotate** (see §8).
- ✅ **R8 minify — decided to keep OFF for now**; `proguard-rules.pro` prepared (see §8).
- ⬜ `local.properties` is still on disk (buildable); it is now gitignored.
- ⬜ `usesCleartextTraffic=true` intentionally left (add-ons may use http).

### ✅ ISSUE-11 — Can't return to Home from Settings (Home "glitches")
- **Symptom:** after opening Settings, tapping the Home tab flashes and bounces back to Settings;
  every other tab works.
- **Root cause:** the "restore last tab" effect was keyed on `currentRoute == HOME`, so tapping Home
  re-triggered it, read the saved tab (Settings) and navigated straight back.
- **Fix:** restore is now a one-shot `LaunchedEffect(Unit)` that reads the persisted index via
  `SettingsStore.lastTabSnapshot()` and only runs while still on the start destination; a `restored`
  flag prevents persisting the initial Home route before the restore settles.
- **Files:** `ui/nav/NovaNav.kt`, `data/local/SettingsStore.kt`

### ✅ ISSUE-12 — Real 18+ still loads too slowly
- **Symptom:** Real 18+ home takes a long time to appear; one slow/blocked site gates the tab.
- **Fix:** progressive per-source emission (`AdultRepository.home(onUpdate)`), per-source 10-min
  cache with instant re-entry, order-stable merging; `NsfwViewModel` paints rows and clears its
  skeleton on the first source that lands.
- **Files:** `data/adult/AdultRepository.kt`, `data/repo/CatalogRepository.kt`, `ui/vm/ViewModels.kt`

### ✅ ISSUE-13 — Continue Watching never populates
- **Symptom:** Home's "Continue Watching" section stays empty no matter what is played.
- **Root cause:** `LibraryStore.recordWatch` (and `DetailViewModel.recordWatch`) were never invoked;
  nothing persisted playback progress.
- **Fix:** `PlayerActivity` records progress on a 5 s cadence, on episode switch and on
  pause/stop/destroy via the app-lifetime scope, and resumes from the saved position; Home hides
  entries watched past 95%.
- **Files:** `ui/player/PlayerActivity.kt`, `NovaApp.kt`, `ui/vm/ViewModels.kt`

### ✅ ISSUE-14 — Home needs a real featured carousel
- **Fix:** auto-sliding swipeable `HorizontalPager` carousel (backdrop art, title, rating/year/genres,
  2-line description, Play button) with animated dot indicators and a drag-aware 5 s auto-advance.
- **Files:** `ui/screens/home/HomeScreen.kt`

---

## 5. How to re-verify

```bash
# TMDB
curl -s "https://api.themoviedb.org/3/movie/popular?api_key=fbc3631da233efa41745ae30297ff63f&language=en-US" | head -c 200

# AniList
curl -s -X POST https://graphql.anilist.co -H 'Content-Type: application/json' \
  --data '{"query":"{ Page(page:1,perPage:5){ media(type: ANIME, isAdult:false, sort: POPULARITY_DESC){ id title{romaji} } } }"}'

# MangaDex
curl -s "https://api.mangadex.org/manga?limit=5&includes%5B%5D=cover_art&order%5BfollowedCount%5D=desc&contentRating%5B%5D=safe"

# Jikan (plain vs filtered — filtered may 504 when MAL is down)
curl -s "https://api.jikan.moe/v4/top/anime?limit=1"
curl -s -o /dev/null -w "%{http_code}\n" "https://api.jikan.moe/v4/top/anime?rating=rx&sfw=false"

# Stremio defaults
for u in https://v3-cinemeta.strem.io/manifest.json https://torrentio.strem.fun/manifest.json \
         https://thepiratebay-plus.strem.fun/manifest.json https://opensubtitles-v3.strem.io/manifest.json; do
  echo "$(curl -sL -o /dev/null -w '%{http_code}' "$u")  $u"; done

# CloudStream repo -> plugins
curl -sL "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/builds/repo.json"
curl -sL "https://raw.githubusercontent.com/phisher98/cloudstream-extensions-phisher/refs/heads/builds/plugins.json" | head -c 300

# Keiyoushi (pb is gzip; JSON mirror is what works)
curl -sIL "https://github.com/keiyoushi/extensions/raw/repo/index.pb" | head -1
```

Build: `./gradlew :app:compileDebugKotlin` (JDK 17 + Android SDK 34; `local.properties` must point at SDK).

---

## 6. Changelog

| Date | Change |
|---|---|
| 2026-09-30 | Created roadmap; ran full API audit; filed ISSUE-1…8. |
| 2026-09-30 | Fixed ISSUE-1…5 (default add-ons + migration, CloudStream parser, Jikan fallback, gzip pb, MangaDex external chapters). |
| 2026-09-30 | `:app:compileDebugKotlin` → BUILD SUCCESSFUL (only pre-existing warnings). |
| 2026-09-30 | Fixed ISSUE-6 (429 backoff) and ISSUE-7 (TMDB anime search filter). ISSUE-8: added `.gitignore`, externalized signing to `keystore.properties`/env, added 9 unit tests. |
| 2026-09-30 | `:app:testDebugUnitTest` → 9 tests / 0 failures; `:app:signingReport` → release signing resolves. |
| 2026-09-30 | Decisions (§8): keep current keystore; keep R8 off. Expanded `proguard-rules.pro` (torrent/jlibtorrent keeps) so minify is a one-line flip later. |
| 2026-09-30 | `:app:assembleRelease` → BUILD SUCCESSFUL, signed `app-release.apk` (36.8 MB). |
| 2026-09-30 | **Feature:** built-in modular adult sources for Real 18+ (Pornhub, XVideos, XNXX, HQporner, SpankBang) — rows/search/categories/detail/direct streams, no add-ons required. See §9. |
| 2026-09-30 | Added `AdultSourcesTest` (7 tests, real markup). `:app:testDebugUnitTest` → 16 tests / 0 failures; release APK rebuilt (36.9 MB). |
| 2026-09-30 | **Real 18+ fixes (device report):** correct Pornhub/HQporner titles+thumbnails, `StreamSource.headers` + OkHttpDataSource playback (Pornhub 410 fix), `mydaddy.cc` iframe + protocol-relative MP4s, parallel per-section NSFW loading, TTL caches, stream pre-flight (`verifyPlayable`), Play-button gating. |
| 2026-09-30 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **21 tests / 0 failures** (5 new); `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL. |
| 2026-10-01 | **Real 18+ search reliability (ISSUE-9):** keystroke debounce + job cancellation, per-source retry with `Search(items, errors)`, keyword relevance ranking (unfiltered fallback), built-in-authoritative `searchReal` with Stremio NSFW fallback, UI search-error notice. Added 1 relevance test. |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **22 tests / 0 failures**; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (43 MB / 36 MB). |
| 2026-10-01 | **Whole-app search reliability (ISSUE-10):** 350 ms debounce + per-query job cancellation + generation guard in `SearchViewModel` and all `NsfwViewModel` searches; `runCatchingCancellable` rethrows cancellation (plain `runCatching` swallowed it, so superseded searches still published). Added `SearchCancellationTest` (2). |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **24 tests / 0 failures**; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (43 MB / 36 MB). |
| 2026-10-01 | **Rename:** display name → **NexusStream** (strings, Home title, Settings About, UA); package/applicationId unchanged. |
| 2026-10-01 | **Phase 1 UI/UX polish:** hero shadow+outline, `PosterShape`(12.dp) unification, card title padding + single-line metadata, compact nav pill + cohesive icons, Manga `SourceBadge`, NSFW tab spacer, empty-state icons, filter-chip outlines, settings chevrons, accent ring, add-on chips. |
| 2026-10-01 | **Phase 2 (partial):** NSFW auto-lock on background, auto-NSFW-from-addons gate, last-tab restore, `Http.cache` metadata caching (6 h TTL) for TMDB/AniList/MangaDex, Coil disk+memory cache, preferred-quality/subtitle handling in the player. |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → 24 tests / 0 failures; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (43 MB / 36 MB). |
| 2026-10-01 | **Phase 2 complete:** autoplay-next overlay (+episode plumbing), player gestures + audio tracks + episode drawer, manga Webtoon/RTL/LTR modes + preload, WorkManager repo sync, sandboxed DEX/APK extension engine (`data/ext/`). |
| 2026-10-01 | Initialized git; commits: initial snapshot → player/reader → repo sync → extension engine. |
| 2026-10-01 | `:app:testDebugUnitTest` → **24 tests / 0 failures**; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (43 MB / 36 MB). |
| 2026-10-01 | **Phase 3 — device-report fixes:** ISSUE-11 Home nav one-shot tab restore; ISSUE-12 Real 18+ progressive per-source loading + per-source cache; ISSUE-13 Continue Watching recording + resume in `PlayerActivity`; ISSUE-14 auto-sliding Home carousel with dot indicators. |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **24 tests / 0 failures**; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL. |
| 2026-10-01 | **Audit fixes (High):** biometric unlock (MainActivity → `FragmentActivity`); manga reader Retry (`loadAttempt` key); `cacheMeta` toggle wired to `Http.cacheEnabled`; TMDB `include_adult=false`; NSFW lock screen always offers a PIN path. |
| 2026-10-01 | **Audit fixes (Medium/Low):** `TorrentStreamer` de-dupes its listener; `MetadataCache` uses SHA-256 keys + 64 MB eviction; network config drops the user-CA trust anchor; `keystore/novastream.jks` untracked (`git rm --cached`, gitignored); About reads `BuildConfig.VERSION_NAME`; manga reader defaults to RTL; NSFW search flags split per section. |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → 24 tests / 0 failures; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL; `dist/NovaStream.apk` refreshed. |
| 2026-10-01 | **Phase 4a — Palette engine:** Coil+Palette swatch extraction, cached RGB values, animated artwork-tinted gradients/halos/CTA on the detail header and home hero. `PaletteCacheTest` (+3 → 27 tests). |
| 2026-10-01 | **Phase 4b — Manga downloader:** `MangaDownloadManager`, offline-first reader, Library download list with pause/resume/delete. `MangaDownloadTest` (+3 → 30 tests). |
| 2026-10-01 | **Phase 4c — Video downloader (initial):** WorkManager progressive/HLS downloader with foreground notification (progress/speed/ETA/cancel), range resume, offline-first player, downloads UI. `HlsDownloadTest` (+4 → 34 tests). |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **34 tests / 0 failures**; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL. |
| 2026-10-01 | **Phase 4 wrap-up — Media3 downloader:** replaced the naive WorkManager + HLS-segment-concatenation downloader with Media3 `DownloadManager`/`DownloadService` (`DownloadManagerProvider` shared `SimpleCache`+DB, `NovaDownloadService` foreground `dataSync` service, rewritten `VideoDownloadManager`); `PlayerActivity` reads through the shared `CacheDataSource`; deleted `VideoDownloadWorker`/`HlsPlaylist`/`HlsDownloadTest`. Handles alternate audio, byte-range and encrypted `#EXT-X-KEY` HLS. |
| 2026-10-01 | **Phase 4 wrap-up — bulk season download:** per-season download button on `DetailScreen` resolves each episode's streams (sequentially) and queues the first playable one, with a resolving spinner and a queued-count toast. |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **30 tests / 0 failures**; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL; `dist/NovaStream.apk` refreshed. |
| 2026-10-01 | **Phase 5 — library removals:** Favourites and Continue Watching are now editable. `×` badge + long-press → shared `ConfirmRemoveDialog` on both Library grids and on the Home Continue Watching row; `LibraryStore.removeFavorite(key)` / `removeWatch(key)` are non-toggling single-item removals (clears saved watch progress); `HomeViewModel.removeContinueWatching`, `LibraryViewModel.removeFavorite`/`removeWatch`; `PosterCard` gained optional `onLongClick`/`onRemove`. Detail heart button unchanged (un-favourites as before). |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **30 tests / 0 failures** ✅. APK **not** rebuilt this session — device pass (§7) still pending. |

---

## 7. Where to pick up next

1. **Manual verification pass** on a device/emulator:
   - Fresh install → Home populates; open a movie → stream sources listed (Torrentio / TPB+).
   - Settings → Add-ons → CloudStream repo → provider list is **no longer empty**.
   - NSFW → NSFW Anime → Jikan "Top" row populates even while the filtered Jikan endpoint 504s.
   - NSFW → NSFW Anime → search returns results (Jikan fallback or AniList).
   - Manga → open a licensed title (e.g. One Piece) → chapter list shows only readable chapters.
2. Run the device pass in step 1, then archive a release build: `./gradlew :app:assembleRelease`
   (signing now comes from `keystore.properties`).
3. Test suites live in `app/src/test/java/com/novastream/app/` — `ParsersTest.kt` (9),
   `AdultSourcesTest.kt` (13), `SearchCancellationTest.kt` (2), `PaletteCacheTest.kt` (3) and
   `MangaDownloadTest.kt` (3).
   If more clients need coverage (MangaDex chapter filtering, AniList query building), extract the
   pure logic and add cases there.
4. Optional later: flip `isMinifyEnabled = true`, smoke-test on a device, ship. Optional later:
   rotate the keystore if the app is ever published.
5. **Re-test Real 18+ search on a device** (see §2b/ISSUE-9): typing a word issues one request
   (no request storm), results are relevant to the query, and an empty search explains itself when
   sources are down instead of silently returning nothing.
6. **Re-test the other Real 18+ fixes** (see §2b/§9): Pornhub & HQporner titles/thumbnails correct,
   detail title/thumbnail correct, Play enabled only with a real stream, playback actually starts
   (Pornhub Referer/cookie path), tab fills progressively instead of stalling. SpankBang and
   HQporner's `mydaddy.cc` embed remain the fragile ones (probe timeouts are tolerated by design).
7. **Re-test whole-app search** (see ISSUE-10): type quickly in the top Search screen — results must
   always match the final query (no flicker to a different set), switching the Movies/TV/Anime/Manga
   filter chips must show the right section, and a slow earlier response must never overwrite it.
8. **Phase 2 follow-ups:** wire the extension engine into the Add-on Manager UI (import flow +
   installed list) and add a full CloudStream/Mihon compatibility layer on top of `NexusExtension`;
   add on-device verification of the player gestures and autoplay overlay.
9. **Re-test Phase 3 on a device** (see ISSUE-11…14): open Settings then tap Home → lands on Home
   (no bounce); Real 18+ paints rows as sources land and re-enters instantly; play something, back
   out, kill and reopen → it appears under Continue Watching and resumes at the saved spot; the
   Home carousel auto-slides, swipes, shows dots and opens the detail page from Play.
10. **Re-test Phase 4 on a device:** detail/home artwork tints the gradient, halo and buttons;
    download a manga chapter, airplane-mode, open it from Library → renders from disk; download an
    MP4 and an HLS stream via Media3 → the `dataSync` foreground notification shows progress and
    Library plays them offline from the shared cache. Bump the 4 GB cache limit in
    `DownloadManagerProvider` if long seasons ever hit it. On a season row, tap the download button
     → all its episodes are queued in one go (a toast reports the count); encrypted `#EXT-X-KEY`
     streams download fine but still need the key online at playback time.
11. **Re-test Phase 5 on a device** (build first: `./gradlew :app:assembleDebug` — no APK has been
    rebuilt since these changes landed): Library → Favourites → tap the `×` (or long-press) →
    confirm → the poster disappears and stays gone after a cold start; same for Continue Watching
    in the Library grid; on Home, tap the `×` on a Continue Watching card → confirm → the card
    vanishes and its progress is cleared (re-watching starts from the beginning, not the old
    position); the Detail heart still toggles "Add to Library" ↔ "In Library"; a Cancel tap must
    not remove anything.

> **Repo note:** this directory is now a git repository. Commits so far: initial snapshot, player
> autoplay/gestures/reader modes, WorkManager repo sync, and the extension engine.
>
> The commit identity was supplied per-command (`git -c user.name=… -c user.email=…`) instead of
> writing it to `.git/config`.

---

## 8. Decisions log

| Date | Decision | Rationale |
|---|---|---|
| 2026-09-30 | **Keep the existing release keystore** (do not rotate) | The `.jks` + password are public (README/history), so the key is already "burned" — but the keystore is *deliberately* shipped so the repo builds a signed APK out of the box, and rotating would change the signing identity and break in-place updates for any installed build. Password is now read from untracked `keystore.properties` or env vars. If this is ever distributed publicly, rotate then. |
| 2026-10-01 | **Extension engine trust model** | Loaded extensions run in-process with app privileges; this is **not** a security sandbox. Loading is restricted to app-private storage and failures are contained per extension. Only load user-installed, trusted extensions; a real sandbox would need a separate process + restricted classloader. |
| 2026-10-01 | **Rename: display name only** | The app is NexusStream in the UI, but the Kotlin package and `applicationId` stay `com.novastream.app`, so the signing identity and in-place updates are preserved. Renaming the package later would change the app identity. |
| 2026-09-30 | **Keep R8 minify OFF** (`isMinifyEnabled = false`) | Shrinking reflection/JNI paths (Gson models, Media3, jlibtorrent) fails only at runtime, and no device was available to verify. A larger APK is preferable to a release that might crash. `proguard-rules.pro` was expanded with the needed keeps, so enabling is a one-line change once a device smoke-test is possible. |
| 2026-10-01 | **Use Media3 `DownloadManager`/`DownloadService` for video downloads** (replacing the hand-rolled WorkManager HLS downloader) | Complex HLS is hard to get right by hand: alternate audio renditions, byte-range segments and `#EXT-X-KEY` encryption all defeat a naive "download master → pick highest variant → concatenate segments" approach. Media3 ships a maintained HLS downloader that shares the playback demuxer, plus a persistent download index, requirement scheduling, a foreground service base class and notification helpers. Cost: an `@UnstableApi` surface and a single shared `SimpleCache` that the player must also read through (constructed once in `DownloadManagerProvider`). |

---

## 9. Built-in adult sources (Real 18+)

**Goal:** Real 18+ should work like a good CloudStream-style app — built-in scrapers, content on
first open, no add-ons required. Stremio NSFW add-ons remain as an *extra* source.

### Architecture (modular — add/remove a site in one line)
```
data/adult/
  AdultModels.kt      AdultVideo / AdultRow / AdultCategory / AdultDetail / AdultSourceException
  AdultSource.kt      interface + adultRows()/adultCategoryRows()/adultHome() + parse helpers
                      (adultAbsoluteUrl, adultPlayHeaders, ogContent, adultCollapse)
  AdultSources.kt     registry: listOf(PornhubSource, XvideosSource, XnxxSource, HqpornerSource, SpankbangSource)
  AdultHttp.kt        browser-like headers; 403/503/challenge -> typed AdultSourceException;
                      probe()/verifyPlayable() pre-flight media URLs with the player's headers
  PornhubSource.kt    listing, search, categories, detail (mediaDefinitions -> HLS per quality)
  XvideosSource.kt    listing, search, categories, detail (html5player MP4 + HLS)
  XnxxSource.kt       same X-style markup as XVideos
  HqpornerSource.kt   listing + best-effort third-party embed resolution
  SpankbangSource.kt  Cloudflare-gated; graceful error
  AdultRepository.kt  aggregates all sources in parallel; collects per-source errors
```

### How it plugs in (no new routing)
- Items are ordinary `MediaItem`s: `type = REAL`, `addonId = <sourceId>`, `id = <page URL>`.
- `CatalogRepository.realRowsWithErrors()` = **built-in rows + Stremio NSFW rows**, plus errors.
- `CatalogRepository.search(q, REAL)` queries the built-in sources (+ Stremio NSFW add-ons).
- `MetadataRepository.detail()` returns early for a built-in source and builds `MetaDetail` purely
  from the adult site — **TMDB/AniList are never touched for Real 18+**.
- `StreamRepository.streamsFor()` returns early, resolves the site's own direct streams, and runs
  them through `AdultHttp.verifyPlayable()` (same headers the player will send) before listing them.
- Every adult `StreamSource` carries `headers` (UA + page `Referer` + Pornhub age cookie);
  `PlayerActivity` feeds them to an `OkHttpDataSource` so playback matches the probe exactly.
- UI: Real tab shows a "Some sources are unavailable" notice from `NsfwViewModel.realErrors`;
  one dead source never breaks the tab. Listings + category rows fetch concurrently per source and
  the three NSFW sections load in parallel (`anime/manga/realLoading`), so Real fills independently.

### Live verification of each source (structures mapped by probing the real pages)
| Source | Listing rows | Search | Detail + streams |
|---|---|---|---|
| Pornhub | ✅ `/video`, `?o=mv`, `?o=tr`, categories | ✅ `/video/search?search=` | ✅ `mediaDefinitions` -> HLS per quality |
| XVideos | ✅ `/` + tag rows | ✅ `/?k=` | ✅ `html5player.setVideoUrl*` MP4 + HLS |
| XNXX | ⚠️ `/` markup differs; `/tags/<tag>` works; section pages flaky | ✅ `/search/<q>` | ✅ `html5player.setVideoUrl*` |
| HQporner | ✅ `/`, `/top`, `/top/week`, `/top/month`, categories | ✅ `/?q=` | ⚠️ third-party embed (mydaddy.cc) — best-effort |
| SpankBang | ❌ 403 Cloudflare | ❌ | ❌ — clear "bot protection" notice shown |

### Tests
`app/src/test/java/com/novastream/app/AdultSourcesTest.kt` — 12 tests over trimmed **real** markup
(Pornhub list ×2 + streams ×2 incl. headers/get_media-skip, XV list + streams, XNXX list, HQporner
list ×2 + embed/iframe, `adultAbsoluteUrl`, duration/quality helpers).

### Known caveats / next
- These sites are intermittently rate-limited / bot-blocked; some paths returned 000/404 from this
  sandbox at times. Reliability is inherently variable — the design degrades per source.
- **SpankBang** could not be verified (Cloudflare) — it will show a clear notice until reachable.
- **HQporner** streams come from the `mydaddy.cc` embed (`//host/….mp4`, retried once); the CDN
  answered 404/000 from this sandbox (possibly geo/CDN), so probes failing with a *timeout* are
  kept while definitive 4xx/410s are dropped.
- Stream pre-flight adds a short parallel delay before the streams list appears; that is the price
  of a Play button that is only enabled when something will actually play.
- Needs the device pass: how many rows load, and whether streams actually play in ExoPlayer.
- **Search** (ISSUE-9): relevance is a keyword filter on the scraped title, so a site returning a
  genuinely *relevant* clip whose title omits the query word is filtered out — the unfiltered
  fallback only kicks in when the filtered set is completely empty. Watch device results to see if
  the filter is too strict for broad queries.
- Easy follow-ups: per-source enable/disable toggles, pagination, on-device diagnostics.
