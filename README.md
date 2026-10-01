# NovaStream — Premium Streaming App for Android

A production-ready, fully offline-installable Android app that unifies **Stremio add-ons**,
**CloudStream extensions**, **Aniyomi** and **Mihon/Keiyoushi** repositories into one polished,
premium streaming experience — Movies, TV Shows, Anime, Manga, and a fully separated NSFW area.

Built with **Kotlin + Jetpack Compose (Material 3)**, **Media3 ExoPlayer**, **Coil**, **OkHttp/Gson**
and **DataStore**. No annotation processors — a lightweight manual DI container keeps the build fast
and reliable.

---

## 📦 Deliverables

| File | Description |
|------|-------------|
| `app/build/outputs/apk/release/app-release.apk` | **Signed release APK** (install this) |
| `app/build/outputs/apk/debug/app-debug.apk` | Debug APK |

- **Package:** `com.novastream.app`
- **Min SDK:** 24 (Android 7.0) · **Target/Compile SDK:** 34 (Android 14)
- **Release signing:** keystore `keystore/novastream.jks` (alias `novastream`, pass `novastream`)

### Install
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
```
Or copy the APK to your phone and tap it (enable "Install unknown apps" for your file manager).

---

## ✨ Feature Overview

### Content sections
1. **Home / Library** — unified discovery for **Movies, TV Shows, Anime** with *Continue Watching*
   and your favorites, plus per-row catalogs from every enabled Stremio add-on.
2. **Manga** — normal manga only (MangaDex), with installed manga-extension browsing.
3. **NSFW (separate tab, gated)** — three **independent** sub-sections, each with its own search bar:
   - **NSFW Anime** — AniList GraphQL + Jikan (MAL, rating `rx`).
   - **NSFW Manga** — MangaDex (adult content ratings).
   - **Real 18+** — starts empty and **auto-populates from installed/enabled NSFW Stremio add-ons**,
     grouped by add-on.

### Metadata & posters
| Section | Source |
|---------|--------|
| Movies / TV / Anime | TMDB API (`fbc3631da233efa41745ae30297ff63f`) |
| Normal Manga | MangaDex API |
| NSFW Anime | AniList GraphQL / Jikan (MAL) |
| NSFW Manga | MangaDex (NSFW ratings) |
| Real 18+ | Installed NSFW Stremio add-on metadata |

### Add-on / extension manager
Install, enable/disable, update and remove add-ons across **four ecosystems**:
- **Stremio** — full protocol: `manifest.json`, `catalog/{type}/{id}.json`, `meta/{type}/{id}.json`,
  `stream/{type}/{id}.json`, `subtitles/{type}/{id}.json`.
- **CloudStream** — `phisher98/cloudstream-extensions-phisher` repo.json.
- **Aniyomi** — `yuzono/anime-repo` index.min.json.
- **Mihon / Keiyoushi** — `keiyoushi/extensions` index.pb (decoded with a dependency-free
  protobuf wire-format reader, with a JSON mirror fallback).

### Player
- **Media3 ExoPlayer** with HLS + DASH support.
- **Quality / track selection** (video track overrides, auto mode).
- **Subtitle track selection** (SRT / VTT / SSA) from subtitles add-ons.
- **Stream source switching** and reload.
- **Picture-in-Picture**, rotation lock, seek ±10s, scrub bar.
- **External player** support (opens `video/*` via `ACTION_VIEW`).
- **Manga reader** — horizontal pager, pinch-to-zoom, dim overlay.

### Polish
- Dark-first **glassmorphism** UI, elevated cards, smooth animations, animated bottom navigation.
- Clean detail pages: backdrop header, rating badges, genres, synopsis, cast, episodes/chapters,
  stream sources and related titles.
- **Search** — global + per-section, 3-column poster grid.
- **Library** — favorites + continue watching (persisted).
- **Settings** — theme (dark/light/system), accent color, player preferences, add-on management,
  NSFW lock (PIN + biometric), clear cache, about.
- Offline metadata caching, plus loading / error / empty states everywhere.

---

## 🔒 NSFW privacy
The NSFW tab is locked by default. Enable **Settings → NSFW → Lock** to set a **PIN** and/or
**biometric** unlock. The three NSFW sub-sections are completely independent and never mix with
normal content. "Real 18+" only ever shows entries coming from NSFW Stremio add-ons you installed.

---

## 🏗️ Architecture

```
com.novastream.app
├── NovaApp.kt                 Application; builds AppContainer; seeds defaults
├── di/AppContainer.kt         Manual DI (settings, stores, repositories)
├── data/
│   ├── model/Models.kt        MediaItem, StreamSource, SubtitleTrack, Video, MetaDetail, Addon…
│   ├── remote/                Http, StremioClient, TmdbClient, MangaDexClient,
│   │                          AniListClient, JikanClient, CloudStreamRepo, AniyomiRepo,
│   │                          KeiyoushiRepo, Protobuf
│   ├── local/                 SettingsStore, AddonStore, LibraryStore, MetadataCache
│   └── repo/                  AddonRepository, CatalogRepository, MetadataRepository,
│                              StreamRepository
└── ui/
    ├── MainActivity.kt        Edge-to-edge Compose host
    ├── nav/NovaNav.kt         6-tab shell + detail route
    ├── theme/                 NovaColors palette, accents, typography
    ├── components/            GlassSurface, PosterCard, MediaRow, Shimmer, EmptyState…
    ├── vm/                    ViewModels + LocalContainer + collectAsStateSafe
    ├── player/                PlayerActivity (ExoPlayer), MangaReaderActivity
    └── screens/               home, manga, nsfw, search, library, settings, addons, detail
```

**Data flow:** UI → ViewModel (StateFlow) → Repository → Remote client (OkHttp/Gson) →
Stremio / TMDB / MangaDex / AniList / Jikan / extension repos. Settings, add-ons, library and
metadata cache persist via **DataStore** + file cache.

---

## 🔧 Build from source

Requirements: JDK 17, Android SDK (platform 34, build-tools 34.0.0), Gradle 8.7.

```bash
export ANDROID_HOME=/path/to/android-sdk
./gradlew assembleRelease        # signed release APK
./gradlew assembleDebug          # debug APK
```

`local.properties` must point at your SDK: `sdk.dir=/path/to/android-sdk`.

---

## ℹ️ Notes
- The app is **fully offline-installable** — all dependencies are bundled in the APK.
- First launch seeds three default Stremio add-ons (Cinemeta, a stream add-on, OpenSubtitles) so
  the Home feed populates immediately; manage them in **Settings → Add-ons**.
- TMDB, MangaDex, AniList and Jikan are public APIs and require an internet connection for content.
