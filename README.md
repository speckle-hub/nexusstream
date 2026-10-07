# NexusStream

A unified streaming app for Android that brings **Stremio add-ons**, **CloudStream extensions**,
**Aniyomi** and **Mihon/Keiyoushi** repositories into one polished Jetpack Compose UI — Movies,
TV Shows, Anime, Manga, and a fully separated, lockable NSFW area.

Built with **Kotlin 2.0 (K2) + Jetpack Compose 1.7 (Material 3)**, **Media3 ExoPlayer**, **Coil**,
**OkHttp/Gson/Retrofit** and **DataStore**, with a lightweight manual DI container.

> The display name is **NexusStream**; the package / `applicationId` remains `com.novastream.app`.

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
    resolve from **7 built-in hentai sources**, with hard timeouts and strict title matching so a
    fallback source can never substitute a different series.
  - **NSFW Manga** — MangaDex adult ratings.
  - **Real 18+** — **10 built-in adult sources** with progressive per-source loading, relevance-
    ranked search and per-source error isolation; Stremio NSFW add-ons merge in as extra rows.

### Player (Media3 ExoPlayer)
- HLS + DASH + progressive playback, quality/audio/subtitle track sheets, subtitle styling and a
  client-side **subtitle sync offset** overlay.
- Gesture control: edge brightness/volume drags, throttled seek scrubbing, double-tap ±10 s,
  pinch-to-zoom scale modes, gesture HUDs with haptics.
- Autoplay-next countdown, Skip Intro/Outro, mid-playback **mirror failover**, external player
  hand-off (VLC/MPV), Picture-in-Picture, stats-for-nerds overlay, playback speed + audio boost.

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
- Add-on manager for all four ecosystems: **Stremio** (full protocol), **CloudStream**
  (Phisher / Hexated / community repos), **Aniyomi** and **Mihon/Keiyoushi**.
- Recommended one-tap stream providers (Torrentio, ThePirateBay+, MediaFusion, OpenSubtitles v3);
  reliability-first stream ranking (preferred hosts → quality → seeders).
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
com.novastream.app
├── NovaApp.kt                 Application; builds AppContainer; seeds defaults
├── di/AppContainer.kt         Manual DI
├── data/
│   ├── model/                 MediaItem, StreamSource, MetaDetail, …
│   ├── remote/                Http, TmdbClient, AniListClient, JikanClient, MangaDexClient,
│   │                          StremioClient, CloudStreamRepo, AniyomiRepo, KeiyoushiRepo
│   ├── local/                 SettingsStore, AddonStore, LibraryStore (+ tolerant codecs)
│   ├── repo/                  CatalogRepository, MetadataRepository, StreamRepository, …
│   ├── adult/                 Built-in Real 18+ sources + AdultRepository
│   ├── hentai/                Built-in NSFW-anime sources + HentaiRepository
│   ├── download/              Media3 download stack, manga downloader, export/cleanup
│   ├── ext/                   Dynamic extension engine (DexClassLoader host)
│   └── integrations/          Backup, scrobbling, updater, watch stats, source tester
└── ui/
    ├── nav/                   5-tab shell (Home · Search · Browse · Library · Settings)
    ├── theme/                 Tokens, palette, typography
    ├── components/            PosterCard, MediaRow, SearchField, PillSegmentedControl, …
    ├── player/                PlayerActivity, MangaReaderActivity
    └── screens/               home, search, browse, manga, nsfw, library, settings, …
```

**Data flow:** UI → ViewModel (StateFlow) → Repository → Remote client (OkHttp/Gson) →
Stremio / TMDB / MangaDex / AniList / Jikan / extension repos. Settings, library and caches
persist via DataStore + file cache.

---

## Build from source

Requirements: **JDK 17**, **Android SDK 34** (platform + build-tools), Gradle wrapper included.

1. Clone the repo.
2. Create `local.properties` at the root pointing at your SDK:
   ```properties
   sdk.dir=C:\\path\\to\\android-sdk
   ```
3. Build:
   ```bash
   ./gradlew assembleDebug      # debug APK (works out of the box)
   ./gradlew testDebugUnitTest  # 113 unit tests
   ```

### Release builds (signing)

Release signing credentials are **not** in the repo. Provide your own keystore via an untracked
`keystore.properties` at the repo root:

```properties
storeFile=keystore/your.jks
storePassword=…
keyAlias=…
keyPassword=…
```

or the environment variables `NOVASTREAM_STORE_PASSWORD`, `NOVASTREAM_KEY_ALIAS` and
`NOVASTREAM_KEY_PASSWORD`, then:

```bash
./gradlew assembleRelease
```

R8 minify is currently **off** (`proguard-rules.pro` is prepared — enabling is a one-line change).

### Notes
- **Min SDK 24** · **Target SDK 34** · versionName `1.1.0`.
- A TMDB API key is embedded in `TmdbClient.kt` for out-of-the-box use; swap in your own if you
  fork.
- `roadmap.md` is the project's living working document (full history, decisions log, device-test
  checklist).
