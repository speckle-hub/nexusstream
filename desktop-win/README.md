# Moovio Desktop (Windows)

A native **Windows desktop streaming client** built with **Kotlin + Compose Multiplatform** and
**libmpv**, packaged as a real Windows app. This is a standalone Gradle project, separate from the
Android app in `../app` — it does not share a build, Android SDK, or Gradle configuration.

```
desktop-win/          ← this project (Windows target)
../app/               ← Android app (unchanged)
```

- **UI:** Compose Multiplatform desktop 1.7.3, Material 3, dark cinematic theme, glass surfaces,
  animated hero, hover states, keyboard-first navigation.
- **Playback:** libmpv through a small hand-written JNA binding. Video frames are pulled through
  mpv's software render API so Compose can draw its own controls on top. Hardware decoding is
  enabled by default (`hwdec=auto-safe`); only the final colour-conversion/scale pass is on the CPU,
  capped at 1600×900.
- **Metadata:** Cinemeta (Stremio, no key) and AniList (GraphQL, no key). Optional TMDB key for
  better artwork.
- **Sources:** Stremio add-on protocol (manifest / catalog / meta / stream / subtitles fields),
  installed by manifest URL.

---

## What v1 does

| Area | Status |
|---|---|
| Sidebar: Home · Movies · TV · Anime · Manga · NSFW · Library · Settings | ✅ |
| Full-bleed hero carousel (auto-slide, manual dots, hover pause) | ✅ |
| Continue Watching + metadata rows (Cinemeta + AniList + add-on catalogs) | ✅ |
| Global search (debounced, Ctrl+K, add-on catalogs, failures isolated) | ✅ |
| Detail: backdrop, rating, year, genres, cast, synopsis, seasons/episodes | ✅ |
| Play → auto-picks the best playable stream; source picker with quality/seeders | ✅ |
| libmpv player: keyboard shortcuts, resume position, audio/subtitle tracks, speed, fullscreen | ✅ |
| Library: favourites + Continue Watching, remove with confirmation, persisted JSON | ✅ |
| NSFW: PBKDF2 PIN gate, separate add-ons only, never in home/search/library | ✅ |
| Add-ons: install/enable/remove, NSFW marking, recommended list | ✅ |
| Windows packaging: portable app image (ZIP) + MSI installer | ✅ |
| Manga reader | Stub (planned v1.1) — see *Out of scope* |

---

## Requirements

- **Windows 10/11 x64**
- **JDK 21** (`jpackage` ships with the JDK; no separate WiX install needed — see packaging)
- Internet access for metadata and add-ons
- ~4 GB free disk during the first build (Gradle deps + libmpv + JDK runtime image)

The Gradle wrapper is included. All commands below run from `desktop-win/` using Git Bash,
PowerShell, or `cmd` (`gradlew.bat` works too).

---

## Quick start

```bash
cd desktop-win

# 1. Fetch a prebuilt libmpv-2.dll (Windows x64, ~120 MB, not committed)
./gradlew fetchLibmpv

# 2. Run the desktop app
./gradlew run
```

If you already have a libmpv build, skip step 1 and point **Settings → System → libmpv path** at
your `libmpv-2.dll`. The app searches, in order:

1. `Settings → System` override
2. `-Dmoovio.mpv.lib=...`
3. `%APPDATA%\Moovio\runtime\libmpv-2.dll`
4. `native/mpv/libmpv-2.dll` (development)
5. next to `Moovio.exe` or in `app/resources/` (packaged app)

If the DLL is missing, the UI opens normally and the player shows an actionable message instead of
crashing.

### First run

- Only **Cinemeta** (metadata) is seeded. Moovio ships **no stream providers**.
- To play something from an add-on, install one under **Settings → Add-ons**. Start with the
  recommended list; direct HTTP/HLS streams work, torrent-only results are marked *not in v1*.
- The NSFW area requires you to set a PIN first and to explicitly mark an add-on as **NSFW-only**.

---

## Build a Windows app

```bash
# Portable app image + ZIP (recommended, no system-wide install needed)
./gradlew portableZip
# → build/distributions/Moovio-1.0.0-windows-x64.zip
#   unzip anywhere → Moovio.exe

# MSI installer
./gradlew packageMsi
# → build/compose/binaries/main/msi/Moovio-1.0.0.msi
```

`libmpv-2.dll` is staged into the installer/app image automatically (via
`packaging/windows/`, populated by the `stageMpv` task from `native/mpv/`) and ends up at
`Moovio/app/resources/libmpv-2.dll` next to the launcher — the runtime searches both that folder and
the launcher folder. The portable ZIP contains exactly one copy of the DLL.

Diagnostics (including the resolved libmpv path and crash traces) are written to
`%APPDATA%\Moovio\logs\moovio.log`.

The Compose Gradle plugin downloads **WiX 3.11** into `build/wix` on the first MSI build; you do not
need to install it system-wide. An app-image only build (`./gradlew createDistributable`) does not
need WiX.

> **Distribution note:** the prebuilt libmpv from `shinchiro/mpv-winbuild-cmake` is **GPLv2+**. If
> you redistribute Moovio with that DLL, the combined distribution must comply with the GPL
> (source availability, notices). Swapping in an LGPL-built libmpv changes that, but is your
> responsibility. The DLL is intentionally not committed to this repository.

---

## Keyboard shortcuts

| Key | Action |
|---|---|
| `Ctrl+K` | Global search (Esc closes) |
| `Ctrl+1…6` | Home · Movies · TV · Anime · Manga · Library |
| `Space` | Play / pause |
| `←` / `→` | Seek −10 s / +10 s |
| `↑` / `↓` | Volume +5 / −5 |
| `M` | Mute |
| `F` | Fullscreen |
| `Esc` | Exit fullscreen → close player → back |
| `Enter` | Open focused poster card |

Mouse: hover reveals controls and card actions; click the video to toggle the control overlay.
Controls auto-hide after ~3 s of playback.

---

## Architecture

```
desktop-win/src/main/kotlin/com/moovio/desktop/
├── Main.kt                  Compose window, dark native title bar, app lifecycle
├── di/AppContainer.kt       Manual DI: stores, clients, repositories, session state
├── domain/
│   ├── model/Models.kt      MediaItem, MetaDetail, StreamSource, Addon, WatchProgress…
│   └── StreamRanking.kt     Quality/seeders parsing + deterministic ranking
├── data/
│   ├── net/                 JDK HttpClient wrapper + defensive JSON helpers
│   ├── remote/              CinemetaClient, AniListClient, StremioClient, TmdbClient
│   ├── local/Stores.kt      Atomic JSON stores: settings, library, add-ons
│   └── repo/                Catalog, Metadata, Stream, Addon repositories
├── player/                  LibMpv JNA binding, MpvPlayer (SW renderer), locator
├── security/PinHasher.kt    PBKDF2-HMAC-SHA256 PIN hashing
└── ui/
    ├── App.kt               Shell, shortcuts, search overlay, player overlay, toasts
    ├── nav/                 AppState (route stack), animated sidebar
    ├── theme/               Palette, accents, typography, glass modifiers
    ├── components/          Hero, rows, poster cards, episode rows, primitives
    └── screens/             home, browse, search, detail, player, library, nsfw,
                             settings, manga
```

**Data flow:** `Composable → state (StateFlow/Compose state) → Repository → HTTP / Stremio add-on`
and back. Settings/library/add-ons persist as atomic JSON writes under
`%APPDATA%\Moovio\` (`settings.json`, `library.json`, `addons.json`). Poster/backdrop images are
cached by Coil in memory; metadata responses use short TTL caches.

**Failure isolation:** every add-on request has its own timeout and error bucket. A dead stream
add-on removes its own results and reports its name in the picker; home rows, search and the app
stay up. Missing metadata falls back Cinemeta → AniList → title search.

### Stream resolution

1. Card carries provider id + optional IMDb id.
2. Details enrich it (AniList external links → IMDb).
3. Enabled stream add-ons are queried in parallel (`stream/{type}/{id}.json`), 8–12 s each.
4. `StreamRanking` de-duplicates by URL/infoHash and orders: direct URL → quality → seeders.
5. Auto-play picks the best playable direct stream, respecting the preferred-quality setting.
6. The result is loaded into libmpv with per-stream proxy headers.

---

## NSFW area

- The PIN is 4–8 digits, salted and hashed with **PBKDF2-HMAC-SHA256 (150k iterations)**; only the
  hash + salt are stored.
- The area is unlocked **for the session only** and re-locks when the app closes.
- Adult add-ons are invisible in Home, Search, Browse and the Library. Only add-ons you mark
  **NSFW-only** feed the private area; results are filtered again on the way out.
- A PIN is a privacy screen, not encryption: anyone with file access to `%APPDATA%\Moovio` can read
  the library JSON. Use BitLocker/EFS if that matters.

---

## Tests

```bash
./gradlew test
# 24 tests: ranking, PIN hashing, JSON persistence, library behavior, Stremio manifest,
# local mock-add-on stream resolution + failure isolation.

# Live/network + real playback checks (libmpv DLL required):
MOOVIO_LIVE_TESTS=1 MOOVIO_MPV_TEST=1 ./gradlew test
```

The live suite verifies Cinemeta/AniList catalogs and metadata, Home hero/row composition, a local
mock Stremio add-on, and two real libmpv playbacks (frames rendered, duration reported, resume at
4 s). Last verified locally: **24/24 passing**, end-to-end stream → libmpv playback confirmed.

---

## Out of scope for v1

- Manga reader runtime (the sidebar entry is an intentional, honest stub).
- Torrent / infoHash playback (direct HTTP/HLS only; torrent results are labelled and rejected).
- CloudStream plugin host, Aniyomi/Mihon extension runtime.
- macOS/Linux targets, downloads, scrobbling, cloud sync, external player hand-off.
- HDR tone-mapping nuance with the software renderer.

## Content neutrality

Moovio is a player and organizer. It ships **no stream providers and no content**; metadata comes
from public APIs (Cinemeta, AniList, optional TMDB) and playback capability only from third-party
add-ons the user installs. Respect copyright and the laws of your region.
