# Moovio Desktop (PC) — Roadmap & Working Notes

> Living document for the **Windows desktop client** in `desktop-win/`. The Android app has its own
> `roadmap.md`; the two are completely separate Gradle projects and share no build or configuration.
>
> **Status — 2026-10-25:** desktop **v1.0.0**, `main` @ `465fedd`. The app builds and runs on
> Windows x64 (`./gradlew run`), and **40 test cases execute green** — live metadata, headless UI
> flows and unit-level playback tests (the real-playback test is gated and was not run with its
> flag this session). Three commits touch this project: the v1.0 import
> (`c2ee798`), the NSFW / add-on-labelling fix (`c28b4c6`) and the playback + Continue Watching fix
> (`465fedd`).

---

## 1. What the desktop app is

A native Windows streaming client built with **Kotlin + Compose Multiplatform (desktop 1.7.3)** and
**libmpv** (hand-written JNA binding, software render path so Compose draws its own controls).

- **Metadata:** Cinemeta (Stremio, no key), AniList (GraphQL, no key), optional TMDB key for artwork.
- **Sources:** Stremio add-on protocol — manifest / catalog / meta / stream / subtitles — installed
  by manifest URL. The app ships **no stream providers of its own**.
- **Persistence:** atomic JSON under `%APPDATA%\Moovio\` (`settings.json`, `library.json`,
  `addons.json`). Logs in `%APPDATA%\Moovio\logs\moovio.log`.
- **Isolation:** every add-on call has its own timeout and error bucket; one dead add-on never takes
  down a row, a search, or the app.

Current size: **38 Kotlin source files, ~8,680 LOC** in `src/main/kotlin`, plus **14 test files,
~1,250 LOC** in `src/test/kotlin`.

Architecture, shortcuts, packaging and the libmpv lookup order are documented in
[`desktop-win/README.md`](desktop-win/README.md). This file is the status/roadmap companion.

---

## 2. Where the code lives

```
desktop-win/src/main/kotlin/com/moovio/desktop/
├── Main.kt, di/AppContainer.kt
├── domain/            models + StreamRanking (quality/seeders parsing, deterministic order)
├── data/              net (Http + JSON helpers), remote clients, JSON stores, repositories
├── player/            libmpv JNA binding, MpvPlayer, DLL locator
├── security/          PinHasher (PBKDF2-HMAC-SHA256, 150k iterations)
└── ui/
    ├── App.kt         shell, shortcuts, search overlay, player overlay, toasts
    ├── nav/           AppState (route stack), animated sidebar
    ├── theme/, components/
    └── screens/       home, browse, search, detail, player, library, nsfw, settings, manga
```

---

## 3. Feature status (v1.0)

| Area | Status | Notes |
|---|---|---|
| Sidebar: Home · Movies · TV · Anime · Manga · NSFW · Library · Settings | ✅ | Keyboard shortcuts `Ctrl+1…6`; NSFW has no shortcut by design |
| Hero carousel, Continue Watching, metadata rows | ✅ | CW lists anything with ≥2 s of saved progress; auto-slide, hover pause, reduced-motion aware |
| Global search (`Ctrl+K`, debounced) | ✅ | Add-on catalogs queried in parallel, failures isolated |
| Detail: backdrop, rating, genres, cast, seasons/episodes | ✅ | Metadata fallback Cinemeta → AniList → title search |
| Play → best playable stream, source picker | ✅ | Direct HTTP/HLS; torrent-only results labelled and rejected |
| libmpv player | ✅ | Zero-copy Skia frame path; ~30 fps / 0 dropped frames at 1600×900 on the dev PC; resume, tracks, speed, fullscreen, auto-hide controls |
| Library | ✅ | Favourites + Continue Watching, persisted JSON |
| NSFW area | ✅ | PBKDF2 PIN gate, session-only unlock, separate add-ons only |
| Add-ons | ✅ | Install/enable/remove, private-area marking, recommended list |
| Windows packaging | ✅ (documented) | `portableZip` + MSI via jpackage; not rebuilt this session |
| Manga reader | ⛔ Stub | Intentional, honest placeholder — untouched by design |

---

## 4. Update log

### 2026-10-25 — Playback smoothness + Continue Watching (`465fedd`)

**1) Playback was very laggy ("works, but unusable").**
- *Root cause:* the software render loop built a Compose bitmap for every frame
  (`Image.makeRaster` → `toComposeImageBitmap()`). Instrumenting the frame path showed that last
  call alone cost **~150 ms per 1600×900 frame**, capping a 1080p stream at **~6 fps with 141
  dropped frames** in a 5 s sample (108% CPU). Hardware decode was already active
  (`d3d11va-copy`), so hwdec/cache were not the bottleneck.
- *Fix:* libmpv now renders straight into **double-buffered Skia bitmaps** (SW renderer writes to
  `Bitmap.peekPixels().addr`, wrapped with `asComposeImageBitmap()`), removing every per-frame copy
  and allocation. Same clip, same machine: **30.0 fps, 0 drops, 46% CPU**.
- *Config hardening:* `hwdec=auto-copy-safe` (copy-back is what the SW render API needs),
  `cache-secs=30`, `demuxer-readahead-secs=20`, `stream-buffer-size=1 MiB`, `video-sync=audio`,
  `framedrop=decoder+vo`, `sws-scaler=fast-bilinear`.
- *Render sizing:* the SW target fits the window but is never upscaled past the source (a 720p
  stream is no longer blown up to 1600×900 every frame); still capped at 1600 px wide.
- *Files:* `player/MpvPlayer.kt`, `ui/screens/player/PlayerScreen.kt`.

**2) Continue Watching stayed empty after a successful playback.**
- *Root cause:* progress **was** saved (the library file held the played episode), but only at
  3.46 s — playback was crawling — and Home/Library hid everything under **15 s**. Saves also only
  happened on the 5 s tick and player exit, not on pause or app close.
- *Fix:* one shared `WatchProgress.inProgress` rule (not finished, ≥2 s) used by Home, Library and
  `LibraryStore.continueWatching()`. Progress is written every 5 s, on pause, on leaving the player
  and on window close (`LibraryStore.flush()` performs a blocking write because the async scope is
  cancelled at exit). Unchanged positions are skipped, so paused playback does not rewrite the file.
- *Refactor:* save/resume logic moved into `player/PlaybackProgress.kt`
  (`recordPlaybackProgress`, `resumePosition`) so it is testable without the player window.
- *Files:* `domain/model/Models.kt`, `data/local/Stores.kt`, `player/PlaybackProgress.kt`,
  `ui/screens/player/PlayerScreen.kt`, `ui/screens/home/HomeScreen.kt`,
  `ui/screens/library/LibraryScreen.kt`, `Main.kt`.

**Tests added:** `PlaybackProgressTest` (8 cases: recording, 3.46 s visibility, zero/finished
filtering, disk persistence, paused-tick dedupe, resume for the same episode, finished restart,
unknown duration), `HomeContinueWatchingUiTest` (Home renders a saved 3.46 s entry) and
`PlaybackContinueWatchingTest` (gated real playback → progress → Home, `MOOVIO_MPV_TEST=1`).

**Verification:** `./gradlew test` → **40 executed, 40 passed**. The fps/CPU figures come from a
temporary benchmark probe against a 1080p clip, run against the old and new code with identical
settings; the probe was deleted before committing. The gated real-playback test was not run
end-to-end in this session (per request); it runs as a no-op without the env flag.

### 2026-10-25 — NSFW unlock + add-on "NSFW-only" labelling (`c28b4c6`)

Two reported bugs, both fixed and covered by new tests.

**1) A correct PIN never revealed the private area.**
- *Cause:* `AppContainer.isNsfwUnlocked` was a plain `var`. `NsfwScreen` reads it during
  composition, but a plain field gives Compose nothing to observe, so after
  `unlockNsfw()` the lock screen never recomposed — the session was unlocked (toast shown) while
  the UI stayed on the gate.
- *Fix:* the flag is now Compose snapshot state (`mutableStateOf`) behind the same getter/methods.
  It remains **in-memory only**, so the area still re-locks when the app closes.
- *Files:* `di/AppContainer.kt`.

**2) Every add-on in Settings → Add-ons showed "NSFW-only".**
- *Cause:* the label was rendered unconditionally, independent of the add-on's `adult` flag.
- *Fix:* the "NSFW-only" badge appears only on add-ons that are actually marked; each add-on row
  gained an explicit **"Private area only"** switch (previously the only control was the
  mislabelled text, and only once already marked). Manifest detection now reads
  `behaviorHints.adult` / `behaviorHints.nsfw`, `flags.adult` / `flags.nsfw`, and top-level
  `adult` / `nsfw` — never inferred from the add-on's kind, catalogs or name.
- *Files:* `ui/screens/settings/SettingsScreen.kt`, `data/remote/StremioClient.kt`, plus the NSFW
  empty-state hint (`ui/screens/nsfw/NsfwScreen.kt`) pointing at the new switch.
- *Unchanged contract:* marked add-ons stay out of Home, Search, Browse and the Library, and only
  feed the gated section; catalog/metadata/stream repositories continue to filter on `adult`.

**Tests added**
- `NsfwAndAddonUiTest` (headless Compose UI): sets a PIN, types it, clicks **Unlock**, and asserts
  the lock gate is replaced by the private-area header; a second test asserts Settings shows **no**
  "NSFW-only" label until an add-on is explicitly marked, then exactly one.
- `NsfwSessionTest`: proves `isNsfwUnlocked` is read-tracked snapshot state and flips on
  unlock / lock.
- `StremioManifestTest`: +2 cases — adult only when the manifest flags it; built-in add-ons are not
  marked adult.
- `desktop-win/build.gradle.kts`: added `compose.uiTest` (test-only) to run the UI flows headless.

**Verification:** `./gradlew test` → 30 executed, 30 passed (live network + libmpv included).
The two regression tests were confirmed to fail when the fix is reverted. The app was launched
(`Moovio` window opens, logs clean) — automated click-through is done via the headless UI tests,
not by driving the live window.

### 2026-10-25 — Desktop v1.0 imported (`c2ee798`)

Whole client landed as a standalone Gradle project: navigation shell, catalogs, detail, libmpv
player, library, PIN-gated NSFW area, settings/add-on management, manual DI container, JSON stores,
and Windows packaging tasks (`fetchLibmpv`, `portableZip`, `packageMsi`).

---

## 5. Verification & test coverage

```bash
cd desktop-win
./gradlew test        # 40 executed cases, all green as of 465fedd
./gradlew run         # launches the app (blocks until the window closes)
```

| Test file | Cases | Covers |
|---|---|---|
| `NsfwAndAddonUiTest` | 2 | PIN unlock → private area (headless Compose UI); add-on NSFW labelling |
| `NsfwSessionTest` | 1 | Unlock flag is Compose-observable snapshot state |
| `StremioManifestTest` | 5 | Manifest parsing, malformed fields, base URLs, adult detection, built-ins |
| `SecurityAndPersistenceTest` | 4 | PIN hashing/validation, atomic JSON store, corrupt-file fallback |
| `LibraryStoreTest` | 4 | Favourites, Continue Watching, adult filtering, episode progress |
| `StreamRankingTest` | 6 | Quality/seeders parsing, dedupe, direct-over-torrent ordering |
| `StremioStreamPathTest` | 1 | Ranked streams + single add-on failure isolation |
| `HomeCatalogTest` | 1 | Home hero/rows resolve from metadata providers |
| `LiveMetadataTest` | 3 | Cinemeta catalog/metadata, AniList trending/detail |
| `EndToEndPlaybackTest` | 1 | Stremio stream → libmpv playback |
| `MpvSmokeTest` | 2 | libmpv opens a clip, frames render, resume position |
| `PlaybackProgressTest` | 8 | Player → library → Continue Watching; 3.46 s visibility, persistence, dedupe, resume rules |
| `HomeContinueWatchingUiTest` | 1 | Home renders a saved short (3.46 s) playback position |
| `PlaybackContinueWatchingTest` | 1 | Gated real playback → saved progress → Home row (`MOOVIO_MPV_TEST=1`) |

Live metadata tests run in the normal suite here; real-playback tests are gated on
`MOOVIO_MPV_TEST=1` plus a libmpv binary, and become no-ops otherwise. The README also documents
`MOOVIO_LIVE_TESTS` / `MOOVIO_MPV_TEST`.

### Test hygiene issue worth fixing

Two `@Test` functions are **silently never executed** (they are not counted above and produce no
failure):

- `LibraryStoreTest.progress persists across a new store instance`
- `SecurityAndPersistenceTest.library file round trips through atomic json store`

Both are expression bodies (`fun … = runBlocking { … }`) whose last expression is
`dir.deleteRecursively()`, so the method returns `Boolean` instead of `Unit`; JUnit 5 does not treat
a non-void method as a test. Fix: give them explicit `: Unit` return types (or end with `Unit`).
That would take the suite from 40 to 42 executed cases.

---

## 6. Behaviour contracts to keep intact

- **NSFW unlock is session-only.** In-memory only; any persistence of the unlocked flag is a bug.
- **Continue Watching = real progress.** Any title with ≥2 s of saved position and not effectively
  finished shows in Home/Library; keep the one shared `WatchProgress.inProgress` rule so the two
  surfaces cannot drift apart again.
- **The frame path must stay zero-copy.** Rendering into a Compose bitmap per frame
  (`toComposeImageBitmap`) costs ~150 ms/frame and destroyed playback; keep the double-buffered
  Skia-bitmap render path.
- **PIN is a privacy screen, not encryption.** Only salt + PBKDF2 hash are stored; library JSON is
  readable on disk by anyone with file access.
- **Adult content never leaks into public surfaces.** Adult add-ons and items are filtered in
  Home, Search, Browse and Library; results are filtered again on the way out.
- **NSFW-only means the gated section only** — marking an add-on private removes it from normal
  browsing and feeds only the private area.
- **No bundled stream providers.** Only Cinemeta is seeded; stream capability comes from add-ons
  the user installs.
- **The Manga entry is an intentional stub** — don't half-implement it; land a real reader or leave
  it as is.
- **libmpv redistribution:** the fetched DLL is GPLv2+; combined redistribution must comply with the
  GPL. The DLL is not committed to the repo.

---

## 7. Known limitations

- The private area is necessarily empty until at least one add-on is marked **Private area only**
  (the empty state explains this). There is no bundled adult add-on.
- Reinstalling a marked add-on by manifest URL resets its `adult` flag to whatever the manifest
  says — a user's explicit marking is not preserved across reinstall (candidate fix, §8).
- Torrent / `infoHash` playback is not supported in v1; such streams are labelled and rejected.
- Windows x64 only. No macOS/Linux target.
- Packaging tasks are documented but were not rebuilt/re-smoked this session.
- `desktop-win/README.md` still says "24 tests"; the suite is now 40 executed (42 functions).
- Streams without a known duration (some live HLS) are not recorded in Continue Watching; VOD
  streams with mpv-reported duration are.
- The gated real-playback test (`PlaybackContinueWatchingTest`) was not run end-to-end this session;
  the fps/CPU measurements came from a temporary benchmark probe. The live GUI has not been
  click-driven by an agent — flows are covered by headless Compose UI tests.

---

## 8. Roadmap

Candidates, **not commitments** — order is a proposal.

| Priority | Item | Why / notes |
|---|---|---|
| P1 | Manga reader runtime | Only remaining sidebar entry that is a stub; README calls it planned v1.1. Needs a source decision (add-on catalog vs. direct source) before UI work. |
| P1 | Test hygiene pass | Fix the two silently-ignored tests (§5), refresh the README test count (40 executed / 42 functions), keep the UI tests as regression guards. |
| P2 | Run the gated playback suite on a clean Windows box | `MOOVIO_MPV_TEST=1` on real Stremio streams (HLS + MP4) to confirm fps/drops and progress across a full 30 min session; this session only benchmarked a test clip. |
| P2 | Preserve user NSFW marking on add-on reinstall/update | `install()` currently overwrites `adult` from the manifest; keep an existing explicit mark when re-installing the same add-on. |
| P2 | Faster path into a private add-on | From the NSFW empty state, jump straight to Settings → Add-ons (and/or mark an installed add-on there) instead of only showing instructions. |
| P2 | Torrent / `infoHash` playback | Large: needs a torrent engine or external hand-off; explicitly out of v1 scope. |
| P3 | Packaging smoke test | Rebuild `portableZip` / MSI and launch on a clean Windows profile (incl. libmpv lookup paths). |
| P3 | Downloads / offline library | Not present in v1; would reuse the library store. |
| P3 | Scrobbling / cloud sync, external player hand-off, macOS/Linux targets | Out of scope for v1; revisit after the P1/P2 items. |
| Backlog | Desktop extension runtimes (CloudStream / Aniyomi) | Android-side work exists; desktop would need its own host. |
| Backlog | HDR tone-mapping nuance, accessibility/screen-reader pass | Polish, no design started. |

---

## 9. Working on the desktop

```bash
cd desktop-win

./gradlew fetchLibmpv    # once: prebuilt libmpv-2.dll (~120 MB, not committed)
./gradlew run            # launch the app
./gradlew test           # unit + headless UI + live metadata tests
MOOVIO_MPV_TEST=1 ./gradlew test --tests "*PlaybackContinueWatchingTest"  # real network playback
./gradlew portableZip    # build/distributions/Moovio-1.0.0-windows-x64.zip
./gradlew packageMsi     # build/compose/binaries/main/msi/Moovio-1.0.0.msi
```

Useful when verifying without touching a real profile:

```bash
JAVA_TOOL_OPTIONS="-Dmoovio.data.dir=C:/Users/<you>/AppData/Local/Temp/moovio-verify" ./gradlew run
```

`AppPaths` honours `-Dmoovio.data.dir`; tests use temporary directories, and the UI tests point the
app at a throwaway data dir. The libmpv DLL is looked up in Settings override →
`-Dmoovio.mpv.lib` → `%APPDATA%\Moovio\runtime` → `native/mpv` → next to the launcher/`app/resources`.

---

## 10. Keeping this document useful

Update this file at the end of every desktop session:

1. Bump the **Status** block (date, commit, what is green).
2. Add a dated **Update log** entry: what changed, why, files, and how it was verified.
3. Refresh **Verification** numbers if tests were added/removed.
4. Move finished items out of **Roadmap** and into the log; don't leave stale promises.
5. Keep the Android roadmap (`roadmap.md`) and this file separate — cross-reference only.
