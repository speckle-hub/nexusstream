# Moovio

A unified streaming app for Android that brings **Stremio add-ons**, **CloudStream extensions**,
**Aniyomi** and **Mihon/Keiyoushi** repositories into one polished Jetpack Compose UI — Movies,
TV Shows, Anime, Manga, and a fully separated, lockable NSFW area.

Built with **Kotlin 2.0 (K2) + Jetpack Compose 1.7 (Material 3)**, **Media3 ExoPlayer**, **Coil**,
**OkHttp/Gson/Retrofit** and **DataStore**, with a lightweight manual DI container.

> The product name is **Moovio**. The package / `applicationId` is `com.moovio.app`
> (it was `com.novastream.app` before the rename).

---

## Features

### Discovery & content
- **Home** — full-bleed auto-sliding featured carousel, Continue Watching / Continue Reading rows,
  per-add-on catalog rows, reorderable via Settings.
- **Search** — global debounced search across Movies / TV / Anime / Manga with recent searches,
  trending chips and adaptive poster grids.
- **Browse** — Manga (MangaDex) and the NSFW area behind one segmented tab, each section keeping
  its own state.
- **NSFW area** (gated, lockable) — three independent sections:
  - **NSFW Anime** — AniList-primary metadata with bounded Jikan fallback; episodes and streams
    resolve from **built-in hentai sources**, with hard timeouts and strict title matching so a
    fallback source can never substitute a different series.
  - **NSFW Manga** — MangaDex adult ratings.
  - **Real 18+** — **built-in adult sources** with progressive per-source loading, relevance-
    ranked search and per-source error isolation; Stremio NSFW add-ons merge in as extra rows.

### Player (Media3 ExoPlayer)
- HLS + DASH + progressive playback, quality/audio/subtitle track sheets, subtitle styling and a
  client-side **subtitle sync offset** overlay.
- Gesture control: edge brightness/volume drags, throttled seek scrubbing, double-tap ±10 s,
  pinch-to-zoom scale modes, gesture HUDs with haptics.
- Autoplay-next countdown, Skip Intro/Outro, mid-playback **mirror failover**, external player
  hand-off (VLC/MPV), Picture-in-Picture, stats-for-nerds overlay, playback speed + audio boost,
  Chromecast hand-off and an Anime4K GPU shader engine.

### Manga reader
- Webtoon / RTL / LTR / landscape double-page modes, pinch zoom (+ zoom lock), per-chapter
  position saving, volume-key page turns, invert/grayscale/warm filters, offline-first rendering
  for downloaded chapters.

### Downloads & offline
- Media3 `DownloadManager`-based video downloads (HLS incl. encrypted `#EXT-X-KEY`, alternate
  audio, byte-range) with per-download HTTP headers, retries, mirror failover, progress UI and
  persistent completion notifications.
- Manga chapter downloader with pause/resume and an offline reader.
- Custom download folder (SAF export), max-parallel setting, auto-download next episode,
  auto-delete watched downloads, storage meter with orphan cleanup.

### Sources & extensions
- **Source-neutral stream providers:** no stream providers ship preinstalled (`streamAddons` is
  empty — users add Torrentio, TPB+, etc. via the Add-on Manager). Provider repositories *are*
  pre-seeded (Phisher98, CXXX 18+, Yuzono, Keiyoushi) and their extensions appear in the
  Add-on Manager for one-tap install; nothing is auto-installed from them. Only legal metadata
  (Cinemeta) and subtitle (OpenSubtitles v3) add-ons are seeded as Stremio sources.
- Add-on manager supports all four ecosystems for user-added sources: **Stremio** (full protocol),
  **CloudStream**, **Aniyomi** and **Mihon/Keiyoushi** repo formats.
- Stream ranking (quality → seeders) with per-add-on timeouts, error reporting and a short
  result cache.
- Sandboxed `.apk`/`.dex` extension engine with a Sources dashboard (enable/disable, latency
  test), custom global HTTP identity (UA/Referer/Cookie), and repo index sync.

### Personalization
- Material You dynamic color, named accent presets, true AMOLED black, dark/light/system themes,
  dynamic poster-palette tinting on the hero and detail pages.

### Privacy, sync & backup
- App lock (biometric + device credential), NSFW PIN/biometric lock, incognito mode, FLAG_SECURE.
- JSON backup/restore, home-row reordering, watch statistics, best-effort scrobbling
  (Trakt/AniList/MAL/Kitsu/SIMKL with pasted tokens), GitHub release updater.

---

## Architecture

```
com.moovio.app
├── MoovioApp.kt              Application; builds AppContainer; seeds defaults
├── di/AppContainer.kt        Manual DI
├── data/
│   ├── model/                MediaItem, StreamSource, MetaDetail, …
│   ├── remote/               Http, TmdbClient, AniListClient, JikanClient, MangaDexClient,
│   │                         StremioClient, CloudStreamRepo, AniyomiRepo, KeiyoushiRepo
│   ├── local/                SettingsStore, AddonStore, LibraryStore (+ tolerant codecs)
│   ├── repo/                 CatalogRepository, MetadataRepository, StreamRepository, …
│   ├── adult/                Built-in Real 18+ sources + AdultRepository
│   ├── hentai/               Built-in NSFW-anime sources + HentaiRepository
│   ├── sfw/                  Built-in SFW movie/TV + anime sources
│   ├── download/             Media3 download stack, manga downloader, export/cleanup
│   ├── ext/                  Dynamic extension engine (DexClassLoader host)
│   └── integrations/         Backup, scrobbling, updater, watch stats, source tester
└── ui/
    ├── nav/                  5-tab shell (Home · Search · Browse · Library · Settings)
    ├── theme/                Tokens, palette, typography
    ├── components/           PosterCard, MediaRow, SearchField, PillSegmentedControl, …
    ├── player/               PlayerActivity, MangaReaderActivity
    └── screens/              home, search, browse, manga, nsfw, library, settings, …
```

**Data flow:** UI → ViewModel (StateFlow) → Repository → Remote client (OkHttp/Gson) →
Stremio / TMDB / MangaDex / AniList / Jikan / extension repos. Settings, library and caches
persist via DataStore + file cache.

> `com.lagradost.cloudstream3` is a small compatibility shim so third-party CloudStream
> `.cs3` providers resolve their inlined `CloudStreamApp` / `CommonActivity` / `DataStore`
> calls at runtime. It is not part of the app's own package.

---

## Build from source

Requirements: **JDK 17**, **Android SDK 34** (platform + build-tools), Gradle wrapper included.

1. Clone the repo.
2. Create `local.properties` at the root pointing at your SDK and (optionally) your TMDB key:
   ```properties
   sdk.dir=C:\\path\\to\\android-sdk
   TMDB_API_KEY=your-key-here
   ```
   The TMDB key may also be supplied as the `TMDB_API_KEY` environment variable. Without it,
   metadata falls back to the add-on sources.
3. Build:
   ```bash
   ./gradlew assembleDebug      # debug APK (works out of the box)
   ./gradlew testDebugUnitTest  # 281 unit tests
   ```

### Release builds (signing)

Release signing material is **not** in the repo and never hard-coded in the build script. Provide it
through the untracked, git-ignored `local.properties` at the repo root:

```properties
moovio.storeFile=C:/secure/moovio-release.jks
moovio.storePassword=…
moovio.keyAlias=moovio
moovio.keyPassword=…
```

or the environment variables `MOOVIO_STORE_FILE`, `MOOVIO_STORE_PASSWORD`, `MOOVIO_KEY_ALIAS` and
`MOOVIO_KEY_PASSWORD` (the file wins when both are set), then:

```bash
./gradlew assembleRelease
```

A release build without that material fails fast with an actionable message instead of AGP's
cryptic "missing required property storePassword".

> The keystore itself belongs **outside** the repository. The previous release key was committed
> once and is still recoverable from git history, so it has been rotated: the old key can no
> longer sign an update, and the new one never enters the tree. The history has to be purged as
> well (see `roadmap.md` §2ax) — otherwise the old blob stays downloadable from the repository.

R8 minify is currently **off** (`proguard-rules.pro` is prepared — enabling is a one-line change).

### Notes
- **Min SDK 24** · **Target SDK 34** · versionName `1.11.2` (`versionCode` 26).
- The GitHub repository is still `speckle-hub/nexusstream`; the release assets are published as
  `Moovio.apk` and `app-release.apk`.
- `roadmap.md` is the project's living working document (full history, decisions log, device-test
  checklist). Entries pre-dating the rename keep their original product name.

## Content neutrality

Moovio is a **player and organizer**, not a content source. It ships with no stream providers and
no catalogs of its own — all media metadata comes from free public APIs (TMDB, AniList, MangaDex,
Jikan, Cinemeta) and subtitles from OpenSubtitles. Provider repositories (Phisher98, CXXX, Yuzono,
Keiyoushi) are pre-seeded for convenience but nothing is auto-installed from them; any streaming
capability comes exclusively from third-party add-ons that **the user chooses to install**; the
project does not endorse or encourage infringing use, and asks users to respect the licenses of
content in their region.
