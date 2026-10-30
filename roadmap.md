# Moovio — Roadmap & Working Notes

> **Branding:** the product is **Moovio**. It was released as *NovaStream* (v1.0–v1.6) and
> *NexusStream* (v1.6–v1.9); the package / `applicationId` is now `com.moovio.app` (was
> `com.novastream.app`) and the OAuth redirect scheme is `moovio://oauth` (was `novastream://oauth`).
> Historical entries below keep the names that were current at the time — read "NexusStream" /
> "NovaStream" there as "Moovio". The full rename and its consequences are in §2aw.
>
> Living document. Update the **Status** and **Changelog** sections every session so work can
> resume exactly where it left off. Last updated: 2026-10-30 — **v1.11.0 released (§2bf): the
> four-phase overhaul shipped — design tokens, shared-element morph + haptics, canvas StatsHud +
> reader physics, encrypted backups + offline smart search.** `versionCode` 24 / `versionName`
> 1.11.0; signed `:app:assembleRelease` ✅ → `dist/app-release.apk` + `dist/Moovio.apk`
> (44,129,211 bytes, SHA-256 `8B212EF4…547311`, same rotated signer as v1.10.0). Verified
> `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **327/327** (0 failures, 0 errors).
> **No tag, no GitHub Release, nothing pushed** — until that asset is uploaded the in-app updater
> still offers v1.10.4. `desktop/` and `desktop-win/` were **not** touched.
>
> Previous, 2026-10-30 — **Phase 4: encrypted backups, smart
> search & release readiness (§2be), Android only: ISSUE-15 closed (AES-256-GCM backup container,
> KeyStore-held key, legacy plaintext import only behind a confirmation), an offline natural-language
> query compiler wired into Search, a free-space floor that reclaims orphaned download chunks, and
> version metadata bumped to 1.11.0 (code 24).** Verified `:app:compileDebugKotlin` ✓ ·
> `:app:testDebugUnitTest` ✓ **327/327**.
>
> Previous, 2026-10-30 — **Phase 3: media player, manga reader
> & diagnostics (§2bd), Android only: "Stats for nerds" rebuilt as a self-ticking canvas HUD, spatial
> audio offered through stream attributes (the toggle the brief assumed does not exist on Android),
> an e-ink reading preset, bounded reader panning, off-thread page preload, rail-scoped morph keys
> and the player's sheet typography on the type scale.** Verified `:app:compileDebugKotlin` ✓ ·
> `:app:testDebugUnitTest` ✓ **306/306**. No version bump at the time.
>
> Previous, 2026-10-30 — **Phase 2: motion, gestures & haptics (§2bc), Android only: poster →
> detail shared-element morph, one haptic vocabulary, one settling spring, residual surfaces on the
> design tokens, `PosterPalette` removed and the root Gradle build repaired.** Verified
> `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **306/306** (0 failures, 0 errors; 309 − 3
> `PaletteCacheTest` cases deleted with `PosterPalette.kt`) · `./gradlew help` at the repo root ✓
> (was failing at configuration). `desktop-win/` was **not** touched; `desktop/build.gradle.kts` was,
> only to drop the duplicate Kotlin plugin versions.
>
> Previous, 2026-10-30 — **UI/UX design-system pass (§2bb), Android only: theme tokens, surface
> components and the Home + Detail layers rebuilt on a quiet direction.** Verified
> `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **309/309**. `desktop/` and `desktop-win/`
> were **not** touched and `settings.gradle.kts` is unchanged.
>
> Previous, 2026-10-24 (v1.10.4) — **source hygiene +
> test-report P0/P1 fixes built and verified (compile + 309/309 unit tests).** `versionCode` 23 /
> `versionName` 1.10.4; signed `dist/app-release.apk` + `dist/Moovio.apk` (43,963,425 bytes,
> SHA-256 `F4A5B1B2B0DE7CE90B6E598176790987C14759D4E7D2E0ACE3494A8A3A0E711B`)
> built locally, commit `479ef08` on `main`, tagged `v1.10.4`, pushed to `origin` and published as a
> GitHub Release with `app-release.apk` + `Moovio.apk` attached. Device pass completed on
> OnePlus CPH2487 (Android 16) — MangaDex NSFW chapter feed confirmed working, Aniyomi/Mihon honesty
> banners verified on both tabs, Yuzono/Keiyoushi repos seeded and browsable. **Aniyomi/Mihon
> extensions still do not execute** — runtime deferred per the honest-UI decision (§2ba,
> item 5): extensions install/enable/update but the Add-on Manager and empty states say plainly they
> are not executable, so no false "works" state is shipped. Toolchain migration (AGP ≥ 8.9,
> compileSdk 36/37, Kotlin 2.1–2.4, OkHttp 5) left for a future pass. MediaRouteButton cast fix
> structurally verified but not exercised on device; torrent watchdog unverifiable without tracker
> access.
>
> **Source hygiene pass (committed in 3335a2d):** Hard-failing adult sources removed from the active
> registry (Jable, YouPorn, RedTube, SpankBang, TXXX, JavMost, OpJav, 7MMTV — all consistently
> 403/blocked/unreachable); never-probed-live sources flagged `experimental` (Eporner, Beeg,
> PornTrex, SxyPrn, FullPorner, Missav, Xhamster, JavGuru, SupJav, Vjav, KissJav, Javtiful);
> verified/partial sources kept (Pornhub, XVideos, Xnxx, HQporner). Same treatment for NSFW Anime:
> dead hosts retired (Hanime, Hentaigasm), unproven sources flagged `experimental` (HentaiHaven,
> HStream, MuchoHentai, HentaiCity, Rule34Video, AnimeIdHentai, HenVids, Ohentai, HentaiTv,
> XAnimu), verified sources kept (HentaiMama, HentaiPlay, Haho). One dead source never blocks the
> tab — per-source error isolation degrades gracefully, and empty states say why (no sources /
> source failed / extension installed but not executable).
>
> Previous, 2026-10-23 — **extension repositories pre-seeded, parsed and installable (§2ba).** Phisher98, **CXXX 18+**, Yuzono and Keiyoushi are
> seeded by default; the Keiyoushi index is parsed from its real repository-wrapper protobuf
> (1425 extensions, 389 NSFW) and Aniyomi APK URLs resolve to `…/repo/apk/…` (250 entries, 72
> NSFW); Aniyomi/Mihon extensions now download, install, enable/disable and update into
> app-private storage, and every `nsfw = 1` entry is gated behind the new “Show NSFW extensions”
> switch. Adult CloudStream plugins (CXXX) run through the existing runtime and are wired into
> Real 18+ search only. **Still missing: executing Aniyomi/Mihon extensions** — NSFW Anime/Manga
> from those repos does not play/read yet (§2ba “What still does not work”, next phase). Verified
> `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **309/309** (+8 `RepoIndexTest`).
>
> Previous, Last updated: 2026-10-23 — **v1.10.1 released.** Closes the
> P0/P1 items in `TEST_REPORT.md` against v1.10.0: the Cast button no longer throws inside the
> player (`ThemeOverlay.Moovio.CastButton` gives `MediaRouteButton` an opaque AppCompat context),
> NSFW Manga chapter feeds ask for the non-safe MangaDex ratings (with one widening retry) instead
> of a filter that returns HTTP 200 + empty `data`, and torrent preparation has a 20 s watchdog
> that cancels and offers **Retry**. `versionCode` 20 / `versionName` 1.10.1; signed release built
> → `dist/Moovio.apk` + `dist/app-release.apk` (43,947,037 bytes, SHA-256 `8C474F67…BA2A79A`,
> signer SHA-256 `2EDB949B…351379`); committed `14e11fc`, tagged `v1.10.1`, pushed (`main` + tag)
> and **published as a GitHub Release with both APKs attached** (`releases/latest/download/
> app-release.apk` → HTTP 200 / 43,947,037 bytes, digest matches). Website hero/nav/footer bumped to
> v1.10.1 and stats to 301 (`next build` ✓, `82d50bf`).
> Verified `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **301/301** (+9
> `Patch1101Test`) · `:app:assembleDebug` ✓ · `:app:assembleRelease` ✓ (signed). Device pass still
> worth doing (§7 item 33).
>
> Previous, Last updated: 2026-10-22 — **v1.10.0 released.** Bundles the
> Moovio rename (§2aw) and the security / stability remediation (§2ax): package `com.moovio.app`,
> rotated signing key, OAuth state nonce, updater signature check, repaired CloudStream shims and
> the main-thread work removed. `versionCode` 19 / `versionName` 1.10.0; signed release built →
> `dist/Moovio.apk` + `dist/app-release.apk` (43,946,885 bytes, SHA-256 `ED8C5418…39D54F`, signer
> SHA-256 `2EDB949B…351379`); pushed (`main` + tag `v1.10.0`) and published as a GitHub Release.
> Website hero/footer/nav bumped to v1.10.0, stats to 292 (`next build` ✓). 292/292 unit tests
> green.
>
> Previous release: **v1.9.0** — bundles the Moovio rebrand (§2au: display name), the Anime4K GPU shader engine (§2au) and the
> 14 built-in SFW sources + auto-play best source (§2av). `versionCode` 18 /
> `versionName` 1.9.0; signed release built → `dist/Moovio.apk` (43,946,889 bytes, SHA-256
> `4E4E6E60…8D8D64`; `versionCode` 18 / `versionName` 1.9.0 verified in
> `output-metadata.json`); committed and pushed (`main` + tag `v1.9.0`). Website
> hero/footer bumped to Moovio v1.9.0, stats to 281 (pushed; Vercel auto-redeploys). The
> **`v1.9.0` GitHub Release is published with `Moovio.apk` + `app-release.apk` attached**,
> so the in-app update dialog and the site CTA now serve v1.9.0.
> Previous: 2026-10-15 — **Phase 38 implemented
> (see §2av):** 14 built-in SFW sources (8 movies/TV + 6 anime) with auto-play best
> source. Verified `:app:compileDebugKotlin` ✓,
> `:app:testDebugUnitTest --rerun-tasks` ✓ **281/281** (+34 `SfwSourcesTest`). Not
> pushed; no APK rebuilt this session.
> Previous: 2026-10-15 — **Phase 37 implemented
> (see §2au):** Moovio rebrand (display name) + Anime4K GPU shader engine with presets,
> tiers, persistence and in-player sheet. Verified `:app:compileDebugKotlin` ✓,
> `:app:testDebugUnitTest --rerun-tasks` ✓ **247/247** (+10 `Phase37Anime4KTest`). Not
> pushed; no APK rebuilt this session.
> Previous: 2026-10-14 — **v1.8.0 released.** Bundles
> Phase 36 (§2at: premium overhaul) on top of the §2as source expansion (13 new built-in
> sources). `versionCode` 17 / `versionName` 1.8.0; signed release built →
> `dist/NexusStream.apk` (43,741,343 bytes, SHA-256 `F98D6590…008C00`;
> `versionCode` 17 / `versionName` 1.8.0 verified in `output-metadata.json`);
> committed and pushed (`main` + tag `v1.8.0`). Website hero/footer bumped to v1.8.0,
> stats to 237 (`next build` ✓, pushed; Vercel auto-redeploys). The **`v1.8.0` GitHub
> Release is published with `NexusStream.apk` + `app-release.apk` attached**, so the
> in-app update dialog and the site CTA now serve v1.8.0.
> Previous session, Last updated: 2026-10-14 — **Phase 36: Premium UI/UX
> Overhaul (§2at).** Hero Watchlist quick action, pill-nav haptics, Real 18+ source-filter
> chips, subtitle outline color + 100 ms sync steps, next-episode pre-fetch at 2 min.
> Verified `:app:compileDebugKotlin` ✓,
> `:app:testDebugUnitTest --rerun-tasks` ✓ **237/237** (+7 `Phase36PremiumTest`). Not
> pushed; no APK rebuilt this session.
> Previous session, Last updated: 2026-10-14 — **Source expansion (§2as):
> 13 new built-in sources** — NSFW Anime: HenVids + Ohentai + Hentai.tv (JSON API) +
> XAnimu; Real 18+ general: PornTrex + SxyPrn + FullPorner + TXXX; JAV: JavMost +
> KissJAV + Javtiful + OpJav + 7MMTV. Verified `:app:compileDebugKotlin` ✓,
> `:app:testDebugUnitTest --rerun-tasks` ✓ **230/230** (+14 `MoreSourcesTest`). Not
> pushed; no APK rebuilt this session.
> Previous session, Last updated: 2026-10-14 — **v1.7.1 released.** Bundles
> Phase 35 (§2ar: unified player gesture engine + state-driven auto-hide). `versionCode` 16 /
> `versionName` 1.7.1; signed release built → `dist/NexusStream.apk` (43,708,575 bytes, SHA-256
> `CEE1669A…74027D`; `versionCode` 16 / `versionName` 1.7.1 verified in `output-metadata.json`);
> committed and pushed (`main` + tag `v1.7.1`). Website hero/footer bumped to v1.7.1, stats to
> 216 (`next build` ✓, pushed; Vercel auto-redeploys). The **`v1.7.1` GitHub Release is
> published with `NexusStream.apk` + `app-release.apk` attached**, so the in-app update dialog
> and the site CTA now serve v1.7.1.
> Previous session, Last updated: 2026-10-14 — **Phase 35: Video Player UI
> Overhaul & Touch Controller Re-architecture (§2ar).** Unified tap/double-tap/hold into one
> `detectTapGestures` interceptor, state-driven auto-hide engine (`PlayerControlsState` +
> `resetAutoHideTimer()`), 200/250 ms fades, top-bar track/speed actions, morphing play/pause,
> bottom quality badge. Verified `:app:compileDebugKotlin` ✓,
> `:app:testDebugUnitTest --rerun-tasks` ✓ **216/216** (+7 `Phase35PlayerControlsTest`).
> Not pushed; no APK rebuilt this session.
> Previous session, Last updated: 2026-10-14 — **v1.7.0 released.** Bundles
> the source expansion (§2aq: Rule34Video, AnimeIdHentai, Eporner, Beeg, JavGuru, SupJav, VJAV)
> and the player controls auto-hide fix (§2ap). `versionCode` 15 / `versionName` 1.7.0; signed
> release built → `dist/NexusStream.apk` (43,708,575 bytes, SHA-256 `16A7DB34…05F063`;
> `versionCode` 15 / `versionName` 1.7.0 verified in `output-metadata.json`); committed and
> pushed (`main` + tag `v1.7.0`). Website CTA/hero/stats bumped to v1.7.0 / 209 tests
> (`next build` ✓, pushed with the release commit; Vercel auto-redeploys). The **`v1.7.0`
> GitHub Release is published with `NexusStream.apk` + `app-release.apk` attached**, so the
> in-app update dialog and the site CTA now serve v1.7.0.
> Previous session, Last updated: 2026-10-14 — **Source expansion (§2aq):
> 7 new built-in sources** — NSFW Anime: Rule34Video + AnimeIdHentai; Real 18+ general:
> Eporner (public API) + Beeg (v6 API + HTML fallback); JAV: JavGuru (jav.guru) + SupJav +
> VJAV (confirmed layout). Verified `:app:compileDebugKotlin` ✓,
> `:app:testDebugUnitTest --rerun-tasks` ✓ **209/209** (+10 `ExpandedSourcesTest`). Not
> pushed; no APK rebuilt this session.
> Previous session, Last updated: 2026-10-14 — **Player controls auto-hide
> fix (§2ap).** `PlayerActivity.kt`: 3 s auto-hide during playing, tap-to-toggle, poke-reset
> on gesture/slider/speed/boost, keep-visible when paused/buffering/sheet-open, 300 ms
> alpha fade. Verified `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest --rerun-tasks` ✓
> **199/199**. Not pushed; no APK rebuilt this session.
> Previous session, Last updated: 2026-10-13 — **v1.6.2 released.** Bundles
> Phase 34 (§2an: Stremio add-on fixes, hold-to-2X gesture, skip-intro/outro removal).
> `versionCode` 13 / `versionName` 1.6.2; signed release built →
> `dist/NexusStream.apk` (43,692,191 bytes, SHA-256 `EFBC369E…63B7`; `versionCode` 13 /
> `versionName` 1.6.2 verified in `output-metadata.json`); committed and pushed (`main` + tag
> `v1.6.2`). Website CTA/hero bumped to v1.6.2 (`next build` ✓, pushed with the release
> commit; Vercel auto-redeploys). The **`v1.6.2` GitHub Release is published with
> `NexusStream.apk` + `app-release.apk` attached** (43,692,191 bytes each, matching the local
> build; verified `release view v1.6.2` → both assets), so the in-app update dialog and the
> site CTA now serve v1.6.2.
> Next: **website full rebuild (§2ao)** — implemented and verified this session (see §2ao);
> site pushed to `main` (Vercel auto-redeploys).
> Previous session, Last updated: 2026-10-13 — **Phase 34 implemented (§2an:
> Stremio add-on fixes, hold-to-2X gesture, skip-intro/outro removal).** Verified
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest --rerun-tasks` ✓ **199/199** (+10
> `Phase34StremioTest`). Not pushed; no APK rebuilt this session. Device pass still needed
> (Torrentio/CyberFlix stream resolution, add-on toggle stability, hold-to-2X feel, skip-UI
> absence).
> Previous session, Last updated: 2026-10-13 — **v1.6.1 released.** Bundles
> Phase 32 (§2am: scraper race engine + Real 18+ speed optimization) on top of the v1.6.0
> CloudStream runtime. `versionCode` 12 / `versionName` 1.6.1; signed release built →
> `dist/NexusStream.apk` (43,675,807 bytes, SHA-256 `A64EC17F…51C3A`; `versionCode` 12 /
> `versionName` 1.6.1 verified in `output-metadata.json`); committed (`1258f17`) and pushed
> (`main` + tag `v1.6.1`). Website CTA/hero bumped to v1.6.1 (`next build` ✓, pushed with the
> release commit; Vercel auto-redeploys). The **`v1.6.1` GitHub Release is published with
> `NexusStream.apk` + `app-release.apk` attached** (43,675,807 bytes each, asset digest
> `sha256:a64ec17f…51c3a` matching the local build; verified `releases/latest` → v1.6.1),
> so the in-app update dialog and the site CTA now serve v1.6.1.
> Previous session, Last updated: 2026-10-13 — **Phase 32: Stream Scraper
> Performance & Real 18+ Speed Optimization (§2am).** Providers now race concurrently with a
> 5 s fail-fast ceiling and completion-order progressive yield (first valid playable link
> paints immediately); Real 18+ home/search publish as each source lands with slow-mirror
> quarantine + list-HTML page cache; catalog/stream fan-outs parallelized and bounded.
> Verified `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest --rerun-tasks` ✓ **189/189**
> (+13 `ScraperRaceTest`). Not pushed; no APK rebuilt this session. Device pass still needed
> (cold Real 18+ tab paint, stream-list first-paint, back-nav instant).
> Previous session, Last updated: 2026-10-12 — **CloudStream provider runtime
> (§2al): real `.cs3` extensions now load and run in-process.** Depends on the CloudStream
> `library` (JitPack 4.7.0) + app-module shims; Add-on Manager install downloads + loads a
> provider, Search/Detail/Streams surface its results, and the phisher98 / yuzono / Keiyoushi
> repos are offered as one-tap **Recommended** (addable, not seeded). `versionCode` 11 /
> `versionName` 1.6.0; source committed + pushed to `main`. Verified
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓ **176/176** (+5), `:app:assembleDebug` ✓.
> Device pass (real provider playback) still outstanding. Aniyomi/Mihon runtime not yet built.
> Signed release built → `dist/NovaStream.apk` (43,659,423 bytes, SHA-256 `43CC67BF…385B9D`);
> tagged `v1.6.0`, pushed, and the **`v1.6.0` GitHub Release is published with `app-release.apk`
> attached** (asset digest `sha256:43cc67bf…85b9d`; verified `releases/latest` → v1.6.0 and
> `releases/latest/download/app-release.apk` → HTTP 200 / 43,659,423 bytes), so the in-app update
> dialog now serves v1.6.0. Website CTA/hero bumped to v1.6.0 (`next build` ✓).
> Previous session, Last updated: 2026-10-12 — **v1.5.1 released.** P0 hotfix
> (§2ak) shipped: `versionCode` 10 / `versionName` 1.5.1; signed release built →
> `dist/NovaStream.apk` (40,140,687 bytes, SHA-256 `A58E3D2B…7E1151`); tagged `v1.5.1` and pushed.
> Previous session, Last updated: 2026-10-12 — **P0 hotfix: Play-crash guardrails (§2ak).**
> Player launch path hardened (validated intent extras, sanitized subs/episodes/headers, guarded
> ExoPlayer build + media load, Cast button fallback, `video/mp4` typo fix); verified
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓ **171/171** (+10 `PlayerStartupTest`).
> Not pushed; no APK rebuilt this session. Device pass still needed (logcat confirmation + Play
> from detail/torrent/downloads on GMS and non-GMS devices).
> Previous session, Last updated: 2026-10-11 — **v1.5.0 released.** Bundles
> Phase 28 (brand icon + NSFW Hub overhaul, §2ah), Phase 29 (sleep timer, swipe zones, subtitle
> styling, storage header, §2ai) and Phase 30 (tracker OAuth + bidirectional sync, hide/pin rows,
> Chromecast, §2aj). `versionCode` 9 / `versionName` 1.5.0; signed release built →
> `dist/NovaStream.apk` (40,140,687 bytes, SHA-256 `CEAAEBEC…46139`); tagged `v1.5.0` and pushed.
> The **`v1.5.0` GitHub Release is published with `app-release.apk` attached** (asset digest
> `sha256:ceaaebec…46139`; verified `releases/latest/download/app-release.apk` → HTTP 200 /
> 40,140,687 bytes), so the in-app update dialog and the site CTA now serve v1.5.0; website
> CTA/hero bumped to v1.5.0 (`next build` ✓).
> Verified: `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓ **161/161**,
> `:app:assembleRelease` ✓ (signed).
> Previous session, Last updated: 2026-10-11 — **Phase 30: Cloud Sync, Anime
> Trackers & Customizable Dashboard.**
> Trackers went from configured-but-dead to working both directions: **browser OAuth** for
> Trakt/AniList/MyAnimeList (user-supplied client credentials, PKCE for MAL, deep-link callback
> exchanging codes for tokens), **push on playback** (completion + 60 s heartbeat with
> episode/season numbers) and **Trakt watched import**; the Home dashboard gained **hide rows**
> and **pin-to-top** next to the existing reorder; and the player has a real **Chromecast route
> button + session handoff** to the Default Media Receiver. Verified:
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓ **161/161** (+10 `TrackerOAuthTest`,
> +8 `HomeRowsTest`). Full detail in **§2aj**.
> Previous session, Last updated: 2026-10-11 — **Phase 29: Player Enhancements &
> Offline Downloads Library.**
> The player gained a **sleep timer** (15/30/45/60 min + end-of-episode, top-bar button + live
> "Sleep Nm" chip + picker sheet, pause + progress snapshot on expiry), spec-literal **swipe
> zones** (brightness left-half / volume right-half / horizontal scrub anywhere, dominant-axis
> locking), an **in-player subtitle style overlay** (size, font color, background, position —
> live-applied and persisted to Settings, next to the existing track picker + sync offset), and
> the Library Downloads tab gained an **offline storage summary** header. Verified:
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓ **143/143** (+7 `SleepTimerTest`
> cases). Full detail in **§2ai**.
> Previous session, Last updated: 2026-10-11 — **Phase 28: Branding, App Icon
> Replacement & NSFW Hub Overhaul.**
> The launcher icon now matches the NexusStream brand (solid purple rounded-square + white "M"
> lettermark with stream dot: new `ic_launcher_foreground` vector, `ic_launcher_background`
> `#FF7C5CFF`, fresh PNGs in all five densities); public Home / search / Library rails are
> desanitized of adult content (`MediaType.isAdult`/`MediaItem.isAdult` + safety-net scrubbing in
> `CatalogRepository`, `HomeViewModel`, `LibraryViewModel`, `HomeScreen`, `LibraryScreen` — the
> Library NSFW chip is gone); and the gated NSFW Hub opens on a new **Private** dashboard with
> isolated adult Continue Watching / Continue Reading / bookmarks plus management tools (clear
> adult history/bookmarks, both opt-ins, Lock now). Verified: `:app:compileDebugKotlin` ✓,
> `:app:testDebugUnitTest` ✓ **136/136** (+7 `NsfwIsolationTest` cases). Full detail in **§2ah**.
> Previous session, Last updated: 2026-10-11 — **Phase 27 (Part B): Updater UI Revamp.**
> The auto-updater download flow now reports **real-time progress**: `downloadApkWithProgress()` streams the
> APK in 8 KB chunks with throttled `DownloadProgress(downloadedBytes, totalBytes)` callbacks (fraction 0..1
> + percent 0..100, null-safe on chunked responses), and `UpdateAvailableDialog` swaps its indeterminate
> spinner for a determinate **LinearProgressIndicator** plus a tabular `Downloading update… 45% (18.2 MB /
> 40.5 MB)` readout — "Later" becomes **Cancel** mid-download (aborts the job, deletes the partial, parks
> the dialog back in idle) while outside-tap/Back is ignored so the job is never orphaned. Verified:
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest --rerun-tasks` ✓ **129/129** (+5 `UpdateCheckerTest`
> cases). Full detail in **§2ag**.
> Previous session, Last updated: 2026-10-11 — **Phase 27 (Part A): UI & Layout Polish Fixes (§2af).**
> Nav-bar padding, detail stats overflow, dangling header, poster × hit area; verified **124/124**.
> Previous session, Last updated: 2026-10-11 — **Phase 26: P0 Bug Fixes & Crash Prevention (§2ae).**
> Top 5 critical audit issues fixed (manga downloads, detail TMDB guard, video cache key, Stremio dedupe);
> verified `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest --rerun-tasks` ✓ **124/124**.
> Previous session, Last updated: 2026-10-08 — **v1.4.1 released.** Three
> device-reported fixes: **switches no longer look invisible when off** (new shared `NovaSwitch`),
> the **Browse → Anime / NSFW rails now fit ~3 cards across like the Search grid** (`railCard`
> 150 → 100 dp), and **NSFW search merges AniList + Jikan + NSFW add-ons** instead of stopping at
> the first source (AniList/Jikan/MangaDex caps raised). `versionCode` 7 / `versionName` 1.4.1;
> signed release built → `dist/NovaStream.apk` (38,330,038 bytes, SHA-256 `81078DB4…B290`);
> website bumped to v1.4.1. Verified `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓
> **119/119**. Full detail in **§2ad**.
> Previous session, Last updated: 2026-10-08 — **v1.4.0 released.** Bundles the NSFW
> Hub + Browse → Anime split (§2z), the launch-time auto-update dialog + repository fallback (§2aa),
> the QoL polish pass (§2ab) and the glass-elevation + tabular-figures close-out (§2ac).
> `versionCode` 6 / `versionName` 1.4.0; signed release built → `dist/NovaStream.apk` (38,330,038
> bytes, SHA-256 `F45FA749…20C4`); website CTA/hero bumped to v1.4.0; source pushed to
> `github.com/speckle-hub/nexusstream` (`16a55a4`) and the **`v1.4.0` GitHub Release published with
> `app-release.apk` attached** (asset digest `sha256:f45fa749…20c4`; verified `releases/latest` →
> v1.4.0 and `releases/latest/download/app-release.apk` → HTTP 200 / 38,330,038 bytes), so the
> in-app update dialog and the site CTA now serve v1.4.0.
> Previous session, Last updated: 2026-10-08 — **QoL follow-up — tabular figures &
> glass elevation.** Closed the last two open §2d items. **Glass surfaces now carry a real drop
> shadow plus a top-edge highlight hairline**, and the theme maps the M3 `surfaceContainer*` roles
> (so stock M3 surfaces like sheets/menus match the app's tonal family). The **tabular-figures sweep
> is finished**: a shared `TextStyle.tnum` token now covers Library stats + download readouts,
> Settings subtitles/slider labels and the player stats overlay. Verified:
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓ **119/119**. Not pushed, no APK built.
> Full detail in **§2ac**.
> Previous session, Last updated: 2026-10-08 — **QoL polish pass (§2d backlog).**
> Four open premium-feel items landed: **empty states gained a soft accent halo and an optional
> call-to-action** ("Retry" wired into Home / Browse → Anime / Manga); **poster cards now shimmer
> while loading and fall back to a placeholder glyph on error** (Coil `SubcomposeAsyncImage`); the
> app **honours the system "remove animations" preference** — the Home hero stops auto-advancing
> and the row-entrance stagger is skipped (new `rememberReducedMotion()`); and **numeric readouts
> use tabular figures** so the Library download %/bytes/ETA no longer jitter. Verified:
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓ **119/119**. Not pushed, no APK built.
> Full detail in **§2ab**.
> Previous session, Last updated: 2026-10-08 — **Phase 25 — startup update dialog &
> repository fallback.** "Check for updates" now works with no setup: `UpdateChecker` gained a
> `DEFAULT_REPO` (`speckle-hub/nexusstream`) plus `resolveRepo()`, applied whenever Settings has no
> repository configured (the Settings row now shows the effective source, and the old "set a repo
> first" guard is gone). `MainActivity` runs a non-blocking launch-time check and, when GitHub
> reports a newer release, shows an expressive Material 3 `AlertDialog` — "New Version Available"
> with the version tag and release notes — whose **Update** action downloads the release `.apk`
> asset and launches the system installer (**Later** dismisses; the dialog is suppressed if the
> app-lock gate is still locked). Verified: `:app:compileDebugKotlin` ✓,
> `:app:testDebugUnitTest` ✓ **119/119** (6 new `UpdateCheckerTest` cases). Not pushed, no APK
> built. Full detail in **§2aa**.
> Previous session, Last updated: 2026-10-08 — **NSFW isolation & Browse → Anime.**
> The Browse segmented control no longer carries NSFW; it is now **Manga | Anime**. A new
> `ui/screens/anime/AnimeScreen.kt` serves only anime-tagged content (AniList trending/popular/
> genre rails, anime Stremio catalogs, and a debounced anime-only search) via a new `AnimeViewModel`.
> Adult content moved into a route-only **NSFW Hub** (`ui/screens/nsfw/NsfwHubScreen.kt`, the former
> `NsfwScreen`): it is no longer reachable from any public tab, opening only from Settings →
> **Content Restrictions → Open NSFW Hub**, and it keeps its PIN/biometric lock. NSFW add-ons are
> also excluded from the general Search screen and Home feed unless the user explicitly opts in via
> the "Show NSFW in general search" switch inside the hub (new `nsfwGlobalSearch` setting, default
> off). Verified: `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓ **113/113**. Not pushed,
> no APK built. Full detail in **§2z**.
> Previous session, Last updated: 2026-10-08 — **hotfix: player crash on Play.**
> Tapping Play on any content crashed because `PlayerActivity` never provided `LocalContainer`,
> while `PlayerScreen` reads `LocalContainer.current.settings` (double-tap seek from v1.2.0,
> auto-skip intro from Phase 23). Fixed by wrapping the player's `setContent` in
> `CompositionLocalProvider(LocalContainer provides container)`, matching `MainActivity` and
> `MangaReaderActivity`. Verified: `:app:compileDebugKotlin` ✓. Not pushed, no APK built.
> Previous session, Last updated: 2026-10-08 — **v1.3.0 released:**
> Phase 23 QoL set shipped — `v1.3.0` tagged (versionCode 5), GitHub Release created with
> `app-release.apk`, website CTA/hero bumped to v1.3.0 (`fae0507`). `dist/NovaStream.apk`
> refreshed; Vercel redeploy still pending a fresh credential check in this environment.
> Previous session, Last updated: 2026-10-08 — **Phase 23 — QoL features (H):**
> settings filter search box at the top of the Settings screen (group visibility is driven by the
> query matching group titles or any of their rows); per-episode watched toggle (check icon) on
> `EpisodeRow` — marking watched records a completed `WatchEntry`, toggling off clears the title's
> progress entry; stream-picker loading state is now shimmer rows instead of a text spinner;
> "Skip intro automatically" player setting (`autoSkipIntro`) auto-seeks past the intro window;
> failed video/manga downloads raise a Toast with the failure reason. Verified:
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓, `:app:assembleRelease` ✓ (signed);
> `dist/NovaStream.apk` refreshed. Previous session, Last updated: 2026-10-08 — **v1.2.0 released + website sync:**
> QoL features (see below) built and shipped as `v1.2.0` (tag pushed, GitHub Release created
> with `app-release.apk` asset so `releases/latest/download/app-release.apk` resolves; the
> NsfwScreen quick-actions fix followed as `a6151d1` with the release asset re-uploaded from the
> final source). The website's download CTA and hero version badge were bumped to v1.2.0
> (commit `2147f29`, `next build` ✓); Vercel deploy still needs a fresh `vercel login` in this
> environment (no stored CLI credentials) — the site auto-deploys on push if linked to the repo.
> Previous session, Last updated: 2026-10-08 — **QoL features (v1.2.0):**
> configurable double-tap seek (Settings → Player → “Double-tap seek”, 5/10/15/30 s, persisted
> as `doubleTapMs`), “Clear” action with confirmation on the Continue Watching / Continue
> Reading home rows, long-press `PosterQuickActionsSheet` (Open / Favourites / Share) wired into
> every poster rail and grid, Search result sorting (Relevant/Title/Year/Rating), and the player’s
> screen brightness is now remembered across launches (`playerBrightness`). Verified:
> `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓, `:app:assembleRelease` ✓ (signed);
> `dist/NovaStream.apk` refreshed. Previous session, Last updated: 2026-10-08 — **Phase 22 — Critical architecture,
> security & lifecycle fixes:**
> - **Lifecycle-aware collection everywhere:** `FlowExt.collectAsStateSafe()` and every direct
>   `collectAsState()` call (MainActivity, PlayerActivity, MangaReaderActivity, DetailScreen) now
>   route through `collectAsStateWithLifecycle()`, so StateFlow subscribers stop when the UI
>   detaches instead of emitting (and recomposing) in the background.
> - **TMDB key out of git:** `TmdbClient.API_KEY` now comes from `BuildConfig.TMDB_API_KEY`,
>   fed by the gitignored `local.properties` (`TMDB_API_KEY=`) or the same-named env var; a
>   blank key throws "TMDB_API_KEY is not configured" instead of silently calling the API.
> - **SettingsStore sharing:** all ~60 preference StateFlows switched from
>   `SharingStarted.Eagerly` to `WhileSubscribed(5000)`; the app scope keeps lightweight
>   subscriptions only for the flows that engine code reads via `.value` (player, reader,
>   NewContentWorker) so they can't regress to initial defaults.
> - **Download-tick scoping:** `MangaReaderActivity` now collects only its own keyed entry
>   (`downloads.map { it[key] }.distinctUntilChanged()`), `LibraryScreen` filters its downloads
>   by the active category chip before collection, and `DetailScreen` derives resume/episode
>   progress through `map`+`distinctUntilChanged` — unrelated download ticks or playback-progress
>   writes no longer rebuild these whole screens.
> - **Mirror failover keeps position:** `PlayerActivity.tryFailover()` captures
>   `player.currentPosition` and `seekTo()`s to it on the new mirror instead of dropping to 0:00.
> - **Concurrency & cache-key correctness:** `MangaDownloadManager.update()`/`delete()` use
>   `MutableStateFlow.update {}` (no more lost writes under concurrent chapter jobs), and
>   `httpPostJsonCached` keys its AniList cache on a SHA-256 digest of the body rather than the
>   32-bit `hashCode()`.
> Verified: `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest` ✓, `:app:assembleRelease` ✓
> (signed); `dist/NovaStream.apk` refreshed. Previous session, Last updated: 2026-10-07 — **Public GitHub release + the
> NexusStream website (§2y):** the repo is now **public** at `github.com/speckle-hub/nexusstream`
> (21 commits; `v1.1.0` tag + GitHub Release with the signed APK attached, so the in-app updater
> works), and a **Next.js 15 landing site** lives in `website/` — a dark, violet-accented single
> page (ThreeUI nebula shader hero, pure-CSS phone mockup, ecosystem marquee, glowing bento
> feature grid, animated stats, HeroUI FAQ, background-beams download CTA wired to the release
> APK, content-neutrality footer), optimized (first-load JS 240 → **216 kB**, LazyMotion,
> reduced-motion fallbacks, full SEO pack) and **deployed to Vercel**. Previous session,
> **Phase 20 — UI grid, Settings crash, reader repairs & Continue Reading (§2u):** the eight reported issues are fixed — adaptive
> grids are back to **three columns on phones** (and Library gained the side gutters it was
> missing), the **Recent Searches** section records typed queries again, the floating pill nav no
> longer hides on scroll (it is permanently visible), the hero banner carries only
> title/meta/Play-Read, the **Sources dashboard + Home row order crash** is gone, the accent color
> row scrolls, horizontal manga paging (RTL/LTR) swipes again, and reading progress for normal **+**
> NSFW manga now feeds a **Continue Reading** row on Home and in the Library. Previous session,
> **Phase 19 — High & Medium functional/engine/UI bug fixes (§2t):** the ten findings from the
> static audit of the whole app
> are fixed — max-parallel downloads wired into Media3, the Sources "disable" switch now persists
> across restarts, the custom download folder actually receives files, the player volume drag
> gesture math corrected, NSFW search moved onto the shared adaptive grid, status-bar insets
> standardised with `statusBarsPadding()`, the player's hand-rolled sheet overlays replaced with
> `ModalBottomSheet`, subtitle-language chips made selectable, manga reading-position eviction
> fixed, and the player resume/persist race removed. Previous session, **Phase 17 — NSFW Anime
> speed & correct-title matching (§2s):** a device report ("searching/opening NSFW Anime took over a
> minute"; "Boku to Misaki-sensei" opened as "Netokano: After Party…") was root-caused to
> unbounded network hops on the NSFW-anime path **and** a too-lax title matcher that let fallback
> sources (Jikan's loose `?q=`, the hentai scrapers' site search, and `hentaiFallback`'s
> `name = head.series` rename) substitute a *different* title for the one selected. Every
> NSFW-anime hop is now hard-bounded (AniList search 8 s / Jikan 6 s / add-ons 6 s, home rows 10 s
> with progressive paint, detail sources 8 s, built-in episode lookup 5 s per source + 8 s total /
> 10 s fill budget, stream resolution 12 s), NSFW-anime search is **built-ins-authoritative**
> (Stremio NSFW add-ons are a fallback only, like Real 18+), and a new significant-token matcher
> (`adultTitlesMatch`) now gates episode/stream resolution, metadata-candidate acceptance and
> `detailsByTitle` — the detail page can no longer be renamed or re-episoded to another series.
> NSFW/Real 18+ search results and the regular (SFW) paths are otherwise unchanged. Previous
> sessions: Phase 16 (§2r), Phase 15 (§2q), the downloader fix (§2p), the NSFW speed + metadata pass
> (§2o), Phase 13 (§2j), the Real 18+ expansion to 12 sources (§2k), the static-review pass (§2l),
> the NSFW-anime expansion to 9 sources (§2m) and the final UI & app checkup (§2n).
> Source-neutrality status: **implemented and built** — see §2x. Verified:
> `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **113/113**, `:app:assembleRelease` ✅
> (signed); `dist/NovaStream.apk` refreshed (38,280,882 bytes, SHA-256 `969D176D…B0A9D`).
> Release/website status: **live** — see §2y (`next build` ✅ static, prerendered; Vercel deploy
> ✅; hydration fix ✅). Device passes outstanding: §7 items 20–31 (plus the Phase 19 interaction
> checks); item 31 covers the Sources crash fix.
>
> **Name change (historical):** the app was **NexusStream** (display name only — the Kotlin package
> and `applicationId` remained `com.novastream.app` so existing installs updated in place; see §8).
>
> **Superseded (2026-10-22):** the product is now **Moovio**, the package / `applicationId` are
> `com.moovio.app`, the OAuth scheme is `moovio://oauth`, and the release key was rotated — so
> existing installs no longer update in place (see §2aw/§2ax and the decisions log in §8).

---

## 1. What this project is

Android streaming app (`com.moovio.app`) unifying Stremio add-ons, CloudStream extensions,
Aniyomi and Mihon/Keiyoushi repos into one Compose UI. **Kotlin 2.0.20 (K2) + Jetpack Compose
1.7.0 (M3)**, Media3 ExoPlayer, Coil, OkHttp/Gson/Retrofit, DataStore, manual DI
(`di/AppContainer.kt`). AGP 8.5.0. The repo also contains **`website/`** — the Next.js landing
site deployed to Vercel (§2y) — and is public at `github.com/speckle-hub/nexusstream`.
~29,600 LOC across 165 Kotlin source files in a single `:app` module (+35 test files). Unit tests
(309 cases):
`LibraryCodecTest.kt` (15), `AdultSourcesTest.kt` (13), `AdultSourcesExtraTest.kt` (11),
`HentaiSourcesExtraTest.kt` (12, incl. the Haho series/stream parses — §2w), `ExpandedSourcesTest.kt` (10, source expansion — §2aq), `StreamRankingTest.kt` (9), `ParsersTest.kt` (9),
`DownloadMaintenanceTest.kt` (8), `ExtensionCodecTest.kt` (8, `extensions.json` decode — §2v),
`TitleMatchTest.kt` (7, NSFW title matching — §2s), `NsfwIsolationTest.kt` (7, adult isolation — §2ah),
`SleepTimerTest.kt` (7, sleep-timer mapping — §2ai), `TrackerOAuthTest.kt` (10, OAuth/PKCE — §2aj),
`HomeRowsTest.kt` (8, dashboard order/visibility — §2aj), `PlayerStartupTest.kt` (10, player
launch guards — §2ak), `CloudStreamSupportTest.kt` (5, CloudStream addon-id scheme + repo policy —
§2al), `ScraperRaceTest.kt` (13, provider race policy / mirror quarantine / page cache — §2am),
`Phase34StremioTest.kt` (10, V3 stream parsing + id routing — §2an),
`Phase35PlayerControlsTest.kt` (7, touch-controller policy — §2ar),
`MoreSourcesTest.kt` (14, source expansion — §2as),
`Phase36PremiumTest.kt` (7, premium pass — §2at),
`Phase37Anime4KTest.kt` (10, Anime4K engine — §2au),
`SfwSourcesTest.kt` (34, SFW built-ins + auto-play ranking — §2av),
`OAuthAndSignatureSecurityTest.kt` (6, OAuth state nonce + APK signature trust — §2ax),
`ProviderSettingsCodecTest.kt` (5, provider-settings JSON codec — §2ax),
`PlaybackMathTest.kt` (6), `ResolutionBadgeTest.kt` (4), `PaletteCacheTest.kt` (3),
`MangaDownloadTest.kt` (3), `WatchStatsTest.kt` (3), `SearchCancellationTest.kt` (2) and
`Patch1101Test.kt` (9, MangaDex rating policy + torrent watchdog — §2az) and
`RepoIndexTest.kt` (8, Aniyomi APK resolution + Keiyoushi protobuf + update policy — §2ba).

**Identity & signing (as of v1.10.0, §2aw/§2ax):** the package / `applicationId` is
`com.moovio.app`, the OAuth redirect scheme is `moovio://oauth`, and the release signing key was
**rotated and moved out of the repository** (`~/.moovio/moovio-release.jks`). The build reads the
signing material only from the untracked `local.properties` (`moovio.storeFile` /
`moovio.storePassword` / `moovio.keyAlias` / `moovio.keyPassword`) or the `MOOVIO_*` environment
variables — never from a tracked file. The old key blob was purged from the git history
(`git filter-repo` + force-push of `main` and all tags). Consequence: **v1.10.0 does not install
over v1.9.0** (different package id *and* different key) — existing users reinstall.

---

## 2. Current focus

**Topic: v1.11.0 released — the four-phase overhaul (§2bf) — Android only.** Design tokens (§2bb),
shared-element morph + `MoovioHaptics` (§2bc), canvas `StatsHud` + reader physics (§2bd), encrypted
backups + offline smart search (§2be): all four phases are in one signed APK, and ISSUE-15 plus the
download-storage prerequisite are closed. `versionCode` 24 / `versionName` 1.11.0; `dist/` and the
badging/signature verification are in §2bf. No tag or GitHub Release was published, so nothing
downloads this build yet. Detail in §2bf.

Previous focus: **encrypted backups, smart search & release readiness (Phase 4, §2be) — Android only.**
ISSUE-15 is closed: exports are AES-256-GCM encrypted under a key generated in the Android KeyStore,
so a backup that leaks no longer carries readable Trakt/AniList tokens or cookies; a legacy plaintext
file still imports, but only after a dialog that says what is inside it. Search gained an offline
query compiler ("anime 2024 high rating", "scifi movies", "recent action") that turns words into
type/year/rating/genre/sort filters before the existing 350 ms debounce — the debounce and its
generation guard are untouched. Downloads gained a user-set free-space floor below which orphaned
cache chunks are reclaimed at launch, and the version is now 1.11.0 (code 24). Verified
`:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **327/327**. No APK, nothing pushed. Detail in
§2be.

Previous focus: **media player, manga reader & diagnostics (Phase 3, §2bd) — Android only.** The text-heavy
"Stats for nerds" chip is now a canvas HUD (`ui/player/StatsHud.kt`) that owns its own 250 ms ticker,
so a stats tick cannot recompose the control overlay behind it; the Speed & Audio sheet gained a
spatial-audio row that is **honest about what Android allows** (an app cannot switch the system
`Spatializer` on — it can only offer the stream to it via `AudioAttributes` spatialisation
behaviour); the manga reader gained a true e-ink monochrome preset and lost its unbounded panning
and its main-thread preload; and the poster → header morph key is now rail-scoped (`?from=` on the
detail route), closing the duplicate-key risk §2bc left open. `PlayerActivity`'s sheet headings moved
onto `MaterialTheme.typography` (no more `Bold` 16 sp literals) and its round controls use
`CircleShape`. Verified `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **306/306**. No
version bump, no APK, nothing pushed. Detail in §2bd.

Previous focus: **motion, gestures & haptics (Phase 2, §2bc) — Android only.** Shared-element morph from a
poster into the detail header (the `SharedTransitionLayout` scaffolding existed but nothing consumed
it), one haptic vocabulary (`MoovioHaptics`: `select`/`toggle`/`commit`) wired into the player's
brightness+volume drags, the primary CTA, the heart, season chips and Library long-press, a single
`Motion.settle()` spring driving the carousel, both nav controls and the morph, and the four residual
surfaces (`PillSegmentedControl`, `FloatingPillNavBar`, `LibraryScreen`, `NsfwHubScreen`) moved onto
`AppRadius`/typography. `PosterPalette.kt` + `PaletteCacheTest` deleted and the root Gradle build
repaired (`:desktop` was requesting a versioned Kotlin plugin after `kotlin.android` had already put
Kotlin on the root classpath). Verified `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓
**306/306** · `./gradlew help` at the repo root ✓. No version bump, no APK rebuilt, nothing pushed.
Detail in §2bc.

Previous focus: **UI/UX design-system pass (§2bb) — Android only.** The Compose component layer was rebuilt
on a quiet "projection booth" direction: `MoovioColors` (now `@Immutable`) gained a `hairline` token
and a contrast-derived `actionFill`/`onAction` pair, `GlassSurface` became the flat `SurfaceCard`,
the artwork-derived radial washes / accent CTA glow / five-hue resolution badges / duplicated
rating pills / full-pill radii are gone, the type scale dropped from Bold(700) to Medium(500), and
`@Immutable` was added to the models that pass through the lazy grids. No version bump, no APK
rebuilt, nothing pushed — `dist/` still holds v1.10.4. `desktop/` and `desktop-win/` untouched.
Verified `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **309/309**. Detail in §2bb.

Previous focus: **v1.10.2 released — repository layer (§2ba).** Runtime not included (blocked; see §2ba).
The four requested repos are pre-seeded and NSFW-aware,
Keiyoushi's real protobuf index is parsed (1425 extensions, 389 NSFW), Aniyomi APK URLs resolve,
and Aniyomi/Mihon extensions install/enable/update into app-private storage with an `nsfw = 1`
gate. Adult CloudStream plugins (CXXX) already run and are wired into Real 18+ search.

**Next up (blocking NSFW Anime/Manga content): the Aniyomi + Mihon source runtime** — load the
installed APKs against the extension libs (`com.github.mihonapp:extensions-lib:1.6.0`,
`com.github.komikku-app:aniyomi-extensions-lib:13`), adapt sources into chapters/pages and
episodes/streams, then verify on hardware. Nothing from those two repos contributes content until
that lands (see §2ba).

Previous focus: **v1.10.1 released (§2az).** The ADB test-report P0/P1 fixes shipped: the
player's `MediaRouteButton` gets an opaque theme instead of throwing `background can not be
translucent: #0`, the NSFW Manga chapter feed no longer asks MangaDex for `safe` ratings (which it
answers with HTTP 200 + empty `data`), and torrent preparation is cancelled after 20 s with a
retry dialog instead of hanging on "Preparing stream…" forever. `versionCode` 20 /
`versionName` 1.10.1; signed APK 43,947,037 bytes; tag `v1.10.1` pushed and the **GitHub Release
published with `app-release.apk` + `Moovio.apk` attached**, so the in-app updater and the site CTA
serve v1.10.1. Website at v1.10.1 / 301 tests.

Previous focus: **v1.10.0 released (§2ay).** The first Moovio-named release: package
`com.moovio.app`, the rotated signing key, the OAuth state nonce, the updater signature check, the
repaired CloudStream shims and the removed main-thread work. `versionCode` 19 /
`versionName` 1.10.0; signed release built → `dist/Moovio.apk` + `dist/app-release.apk`
(43,946,885 bytes); pushed (`main` + tag `v1.10.0`) and published as a GitHub Release. Website
bumped to v1.10.0 / 292 tests. 292/292 unit tests green.

Previous focus: **v1.9.0 released** — bundles the Moovio rebrand + Anime4K engine (§2au) and the
14 built-in SFW sources + auto-play best source (§2av). `versionCode` 18 /
`versionName` 1.9.0, signed APK built (`dist/Moovio.apk`, 43,946,889 bytes, SHA-256
`4E4E6E60…8D8D64`), source tagged + pushed, website at Moovio v1.9.0 / 281 tests, and the
**GitHub Release is published with `Moovio.apk` + `app-release.apk` attached**, so the
in-app updater and the site CTA serve v1.9.0.

Next up: none — roadmap is the working doc for whatever comes next.

Previous focus: **Phase 38 — built-in SFW sources + auto-play best source (§2av).** 14 modular
scrapers (Vidsrc, YTS, FlixHQ, MultiMovies, Rive, Flixer, PopcornMovies, SuperStream
+ AnimePahe, Gogo, KickassAnime, YugenAnime, AnimeKai, Miruro) wired into search,
catalog rows, metadata fallback and stream resolution, so Movies/TV/Anime play with
zero add-ons; Play auto-starts the best-ranked working stream (reliability →
quality → direct → seeders) with the sheet kept for manual override. Verified
`:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest --rerun-tasks` ✓ **281/281**
(+34 `SfwSourcesTest`). Not pushed; no APK rebuilt this session. Device pass still
needed (per-site reachability + playback, auto-play first-paint).

Next up: none — roadmap is the working doc for whatever comes next.

Previous focus: **Phase 37 — Moovio rebrand + Anime4K GPU shader engine (§2au).** Display name
is now **Moovio** (package/`applicationId` unchanged, like the earlier NexusStream rename, so
installs update in place); the player runs real-time Anime4K GLSL passes
(`media3-effect` `GlEffect` + `BaseGlShaderProgram`) with 4 presets × 2 GPU tiers,
persisted prefs and a live-switching sheet section. Verified
`:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest --rerun-tasks` ✓ **247/247**
(+10 `Phase37Anime4KTest`). Not pushed; no APK rebuilt this session. Device pass still
needed (sheet feel, effect on real GPU incl. HQ-tier frame drops, Off bypass).

Next up: none — roadmap is the working doc for whatever comes next.

Previous focus: **v1.8.0 released** — bundles Phase 36 (§2at: premium overhaul) on top of the
§2as source expansion (13 new built-in sources). `versionCode` 17 / `versionName`
1.8.0, signed APK built (`dist/NexusStream.apk`, 43,741,343 bytes, SHA-256
`F98D6590…008C00`), source tagged + pushed, website at v1.8.0 / 237 tests, and the
**GitHub Release is published with `NexusStream.apk` + `app-release.apk` attached**,
so the in-app updater and the site CTA serve v1.8.0.

Next up: none — roadmap is the working doc for whatever comes next.

Previous focus: **Phase 36 — Premium UI/UX Overhaul (§2at).** Audit-first: the hero carousel,
poster palette, floating pill nav, NSFW PIN/biometric vault, subtitle sheet + sync,
PiP, stream ranking + badges all already existed — the genuine deltas are a hero
Watchlist toggle, tab-switch haptics, Real 18+ source-filter chips, subtitle outline
color + 100 ms sync steps, and next-episode pre-fetch inside 2 min. Verified
`:app:compileDebugKotlin` ✓,
`:app:testDebugUnitTest --rerun-tasks` ✓ **237/237** (+7 `Phase36PremiumTest`). Not pushed;
no APK rebuilt. Device pass still needed (hero toggle, chips, sheet, pre-fetch handoff).

Next up: none — roadmap is the working doc for whatever comes next.

Previous focus: **Source expansion (§2as)** — 13 new built-in sources (HenVids, Ohentai,
Hentai.tv, XAnimu, PornTrex, SxyPrn, FullPorner, TXXX, JavMost, KissJAV, Javtiful,
OpJav, 7MMTV) following the existing `data/hentai/` + `data/adult/` modular pattern.
Verified `:app:compileDebugKotlin` ✓,
`:app:testDebugUnitTest --rerun-tasks` ✓ **230/230** (+14 `MoreSourcesTest`). Not pushed;
no APK rebuilt. Device pass still needed (reachability + playback per new site).

Next up: none — roadmap is the working doc for whatever comes next.

Previous focus: **v1.7.1 released** — bundles Phase 35 (§2ar: unified player gesture engine +
state-driven auto-hide). `versionCode` 16 / `versionName` 1.7.1, signed APK built
(`dist/NexusStream.apk`, 43,708,575 bytes, SHA-256 `CEE1669A…74027D`), source tagged +
pushed, website at v1.7.1 / 216 tests, and the **GitHub Release is published with
`NexusStream.apk` + `app-release.apk` attached**, so the in-app updater and the site
CTA serve v1.7.1.

Next up: none — roadmap is the working doc for whatever comes next.

Previous focus: **Phase 35 — Video Player UI Overhaul & Touch Controller Re-architecture (§2ar).**
The control overlay's taps intermittently failed to reveal/hide (two competing
`detectTapGestures` detectors raced for the same down event). Single-tap, double-tap seek
and hold-for-2X now share **one** unified interceptor; visibility is a strict
`PlayerControlsState` machine (VISIBLE / HIDING / HIDDEN) with a single
`resetAutoHideTimer()` job (cancel + fresh 3,000 ms); overlay fades are 200/250 ms;
top bar gains track + speed actions, the center play/pause morphs, and the bottom bar
shows a quality badge. Verified `:app:compileDebugKotlin` ✓,
`:app:testDebugUnitTest --rerun-tasks` ✓ **216/216** (+7 `Phase35PlayerControlsTest`).
Not pushed; no APK rebuilt this session. Device pass still needed (tap toggle feel,
hold-to-2X, auto-hide timing on hardware).

Next up: none — roadmap is the working doc for whatever comes next.

Previous focus: **v1.7.0 released** — bundles the source expansion (§2aq: 7 new built-in
sources) and the player controls auto-hide fix (§2ap). `versionCode` 15 / `versionName`
1.7.0, signed APK built (`dist/NexusStream.apk`, 43,708,575 bytes), source tagged +
pushed, website at v1.7.0 / 209 tests, and the **GitHub Release is published with
`NexusStream.apk` + `app-release.apk` attached**, so the in-app updater and the site
CTA serve v1.7.0.

Previous focus: **Source expansion (§2aq)** — 7 new built-in sources (Rule34Video, AnimeIdHentai,
Eporner, Beeg, JavGuru, SupJav, VJAV) following the existing `data/hentai/` + `data/adult/`
modular pattern. Verified `:app:compileDebugKotlin` ✓,
`:app:testDebugUnitTest --rerun-tasks` ✓ **209/209** (+10 `ExpandedSourcesTest`). Not pushed;
no APK rebuilt. Device pass still needed (reachability + playback per new site).

Previous focus: **Player controls auto-hide fix (§2ap)** — regression where the player overlay stayed
visible during playback. `PlayerActivity.kt`: `CONTROLS_HIDE_MS` 3 s auto-hide while
`isPlaying`, tap-to-toggle, `pokeControls()` reset on drag/double-tap/slider/seek/speed/boost,
keep-visible when paused/buffering/sheet-open, locked forces hidden, 300 ms alpha fade.
Verified `:app:compileDebugKotlin` ✓, `:app:testDebugUnitTest --rerun-tasks` ✓ **199/199**.
Not pushed; no APK rebuilt.

Previous focus: **v1.6.2 released** — bundles Phase 34 (§2an: Stremio add-on stream/toggle fixes,
hold-for-2X gesture, Skip Intro/Outro removal). `versionCode` 13 / `versionName` 1.6.2,
signed APK built, source tagged + pushed, website at v1.6.2, and the **GitHub Release is
published with `NexusStream.apk` + `app-release.apk` attached** (verified asset sizes match
the local build), so the in-app updater and the site CTA serve v1.6.2.

Next up: none — website rebuild (§2ao) shipped this session; roadmap is the working doc
for whatever comes next.

Previous focus: Phase 34 implemented — Stremio add-on stream/toggle fixes (§2an), the player
hold-for-2X gesture, and full Skip Intro/Outro removal. Verified `:app:compileDebugKotlin` ✓,
`:app:testDebugUnitTest --rerun-tasks` ✓ **199/199** (+10). Not pushed; no APK rebuilt this
session.

Previous focus: v1.6.1 released — bundles Phase 32 (§2am: parallel provider racing, Real 18+
acceleration, fail-fast fallbacks) on the v1.6.0 CloudStream runtime. `versionCode` 12 /
`versionName` 1.6.1, signed APK built, source tagged + pushed, website at v1.6.1, and the
**GitHub Release is published with `NexusStream.apk` + `app-release.apk` attached** (verified
`releases/latest` → v1.6.1, digest matches the local build), so the in-app updater and the
site CTA serve v1.6.1.

Previous focus: Phase 32 — Stream Scraper Performance & Real 18+ Speed Optimization (§2am);

Previous focus: CloudStream provider runtime (§2al, v1.6.0); **v1.5.1 released** — the P0 Play-crash hotfix (§2ak) is tagged, pushed and
published with the signed APK attached. Device pass still outstanding (logcat + Play on
GMS/non-GMS hardware).

Previous focus: P0 hotfix — Play-crash guardrails (§2ak); Phase 30 — Cloud Sync, Anime
Trackers & Customizable Dashboard (§2aj);

Previous focus: Phase 29 — Player Enhancements & Offline Downloads Library (§2ai);
Phase 28 — Branding, App Icon Replacement & NSFW Hub Overhaul (§2ah);
Phase 27 (Part B) — Updater UI Revamp with Real-Time Progress Bar (§2ag);
Phase 27 (Part A) — UI & Layout Polish Fixes (§2af); Phase 26 — P0 Bug Fixes & Crash
Prevention (§2ae); QoL follow-up (§2d close-out / §2ac);
v1.4.1 release (§2ad); QoL polish pass (§2ab).

Previous focus: NSFW isolation & Browse → Anime (§2z); the public release and the website (§2y);
source neutrality (§2x); Phase 21 (§2v) —
NSFW/Browse card alignment & the Sources crash root cause; Phase 20
(§2u) — grids/recents/hero/Settings crash/reader/Continue Reading. Legacy §2 topic text follows for
history:

**Topic: Phase 21 — NSFW / Browse layout padding & width alignment, and a real fix for the
Settings → Sources dashboard crash.** (1) The NSFW (and Browse → Manga) media rails were still
using `MediaRow`'s untouched **132 dp** default card width while the Home feed passed **150 dp**
by hand, so the same poster was a different size on every screen and the NSFW/Browse rails read as
"narrow cards"; a new `AppSpacing.railCard` (150 dp) is now the single source of truth for every
rail, and the remaining hardcoded gutters on NSFW come from `AppSpacing`. (2) The reported
Settings → Sources dashboard crash — which survived Phase 20's early-`return@Column` theory — is a
**Gson null-field decode** on a malformed `extensions.json`: a missing `name`/`id` lands as a silent
`null` in a Kotlin non-null slot, so rendering threw on entry. A new tolerant, per-entry
`ExtensionCodec` (the `LibraryCodec` pattern from Phase 13) drops undecodable entries, defaults the
name, de-dupes ids and can never throw.

Phase 21 status: **implemented and built** — full detail in **§2v**. Verified this session:
`:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest --rerun-tasks` ✅ **111/111** (103 + 8 new),
`:app:assembleRelease` ✅ (signed), `dist/NovaStream.apk` refreshed (38,264,498 bytes, SHA-256
`a2007861…8c10`). The crash fix is verified statically and by unit test only — no device/emulator
was available, so §7 item 31 is the hardware confirmation.

Previous focus: Phase 20 (§2u) — grids/recents/hero/Settings crash/reader/Continue Reading; before
that Phase 19 (§2t) — the ten static-audit fixes; Phase 18 (floating pill navigation
& expressive surfaces); the download progress UI + notifications pass; before that Phase 17 (§2s),
Phase 16
(§2r), the Phase 15 tactile polish (§2q), the downloader fix & storage cleanup (§2p), the NSFW
speed + metadata pass (§2o), the final UI & app checkup (§2n), Phase 13 (§2j) and the source
expansions (§2k/§2l/§2m). Device passes outstanding: §7 items 20 (Phase 13), 21 (Real 18+
sources), 22 (static-review fixes), 23 (hentai sources), 24 (UI checkup), 25 (NSFW pass),
26 (Phase 14), 27 (Phase 15), 28 (Phase 16), 29 (Phase 17), 30 (Phase 20) and 31 (Phase 21).

### Phase 21 — NSFW/Browse card alignment & the Sources crash root cause (2026-10-06)
Status: **implemented and built.** (1) `AppSpacing.railCard` = **150 dp** is the new single source of
rail card width; Home, Manga and NSFW all pass it to `MediaRow` instead of relying on the 132 dp
default or a literal, and the NSFW screen's remaining literals became `AppSpacing` tokens. (2) The
Sources dashboard crash was root-caused to a Gson null-field decode of `extensions.json` and fixed
with a tolerant per-entry `ExtensionCodec` (+8 unit tests) plus a sanitized read in `SourcesScreen`.
Full detail in **§2v**.

### Source hygiene — dead seeds fixed & dead hosts retired (2026-10-06)
Status: **implemented and built.** Acting on the §3 live probe: the **Hexated** CloudStream seed was
404ing on every branch, so `AddonStore.Defaults.repos` now points at the live
`JoeTinnySpace/cloudstream-extensions-hexated` mirror (the old URL joined a new
`Defaults.retiredRepoUrls` that `ensureDefaultRepos()` prunes from existing installs), and **Haho** —
whose two coded search paths 404 — now searches the live `GET /anime?q=` and expands its series
cards into episodes (with `<source type="video/…">` support added to `HentaiTubes.mediaUrls` so
Haho's extension-less filegasm streams resolve). The dead hosts **HPJAV** (DNS), **Avgle** (520) and
**Hentaigasm** (timeouts) were dropped from `AdultSources`/`HentaiSources`, and **Hanime** was
retired (search API DNS-dead + JS-shell page); their objects/tests remain should a host return.
Verified: `:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest --rerun-tasks` ✅ **113/113** ·
`:app:assembleRelease` ✅ (signed); `dist/NovaStream.apk` refreshed. Full detail in **§2w**.

### Phase 20 — UI grid, Settings crash, reader repairs & Continue Reading (2026-10-06)
Status: **implemented and built.** The eight reported issues, in order: (1) `AppSpacing.posterMin`
108 → **88 dp** so adaptive grids resolve to **three columns** on every phone from 320 dp up
(108 dp gave two columns on the common 360 dp width), with Library's two poster grids gaining the
16 dp side gutters every other grid already had; (2) Recent Searches records any query the user
actually let run, not only chip taps; (3) the floating pill nav's scroll-hide plumbing
(`FloatingNavScrollState` + `NestedScrollConnection`) is deleted — the bar is always visible;
(4) the hero banner dropped its description paragraph (and the genre from its meta line) and its
CTA now reads Play/Read appropriately; (5) the Sources dashboard + Home row order crash is fixed
(early `return@Column` replaced with a plain `if/else`, plus guards and `launchSingleTop`), and
cold-boot fetch/extension loading got bounded retries; (6) the accent row scrolls horizontally;
(7) `ZoomablePage` no longer consumes one-finger drags, so `HorizontalPager` swipes again;
(8) the manga reader records page/total into the shared history, which Home/Library split into a
**Continue Reading** row/section. Full detail in **§2u**.

### Phase 19 — High & Medium audit fixes (2026-10-05)
Status: **implemented and built.** Fixes the ten findings from the static audit: (1) `maxParallel` downloads
wired into Media3 instead of the hardcoded `3`; (2) `ExtensionRepository.reloadAll()` honours
`disabledSources` so a disabled extension stays disabled after a restart; (3) the custom download
folder (`storageUri`) is actually read (completed downloads exported into the chosen SAF folder —
Media3's `SimpleCache` still needs an app-private `File`, so the folder is a destination, not the
cache root); (4) the player volume drag gesture math (`volumeAccum.toInt()` made the 0.15 step
dead) corrected; (5) NSFW search results moved off `chunked(3)`/`Row3` onto the shared adaptive
grid; (6) hardcoded status-bar top padding replaced with `statusBarsPadding()` across Search,
Library, Browse, NSFW, Settings, Add-on Manager, Sources and HomeOrder; (7) the player's Quality &
Audio / Episodes / Speed / External `Box` overlays replaced with `ModalBottomSheet` (inner taps no
longer dismiss; Back closes the sheet); (8) subtitle-language chips in the detail stream sheet are
now selectable; (9) `SettingsStore.setMangaProgress` re-inserts updated chapters at the end so
frequently-read chapters aren't evicted; (10) `PlayerActivity` resumes the saved position before
the progress watcher can overwrite it. Full detail in **§2t**.

### Phase 18 — Floating pill navigation & expressive surfaces (2026-10-05)
Status: **implemented and built.** The docked Material `NavigationBar` is gone. A new
`FloatingPillNavBar` (`ui/nav/FloatingPillNavBar.kt`) floats a 66 dp capsule 16 dp above the
gesture inset (20 dp side margins), tinted `surface` @ 85 % with a hairline outline and 6 dp
shadow, and slides a spring-driven pill indicator behind the active tab. A shared
`FloatingNavScrollState` (provided via `LocalFloatingNavScroll`) hides the bar on downward scroll
and restores it on any upward scroll; tab screens add `LocalFloatingNavBottomPadding` to their
content padding so nothing is occluded. Browse and Library segmented controls use the new shared
`PillSegmentedControl`, and the detail primary CTA is now an elevated capsule pill carrying an
embedded watch/read progress bar. Full detail in the §6 changelog.

### Download progress UI + system notifications (2026-10-05)
Status: **implemented and built.** `VideoDownloadManager` now polls `DownloadManager` every 500 ms
while anything is queued/downloading so the Library progress bar, `%` and byte counter advance in
real time (Media3's listener only reports state transitions), and `MainActivity` requests
`POST_NOTIFICATIONS` on API 33+ so the `DownloadNotificationHelper` foreground notification
actually reaches the shade. Finished downloads additionally post a **persistent** completion
notification from the service's own listener, so they stay in the shade after the service stops.
Full detail in the §6 changelog.

### Phase 17 — NSFW Anime speed & correct-title matching (2026-10-05)
Status: **implemented and built.** Every NSFW-anime network hop is hard-bounded (search, home
rows, metadata sources, built-in episode/stream resolution), home rows paint progressively, and
a significant-token title matcher now gates episode/stream resolution, metadata acceptance and
`detailsByTitle` so a fallback source can never substitute a different series. Full detail in
**§2s**.

### Phase 16 — Best CloudStream + Stremio sources (2026-10-04)
Status: **implemented and built.** Seven default provider repos seeded (incl. Hexated), reliability-
first stream ranking with seeder tie-break, a stream-result cache, per-add-on error feedback, a
**Recommended** one-tap install row, and timeouts on regular search/catalog fetches. NSFW and
built-in adult sources are untouched. Full detail in **§2r**.

### Phase 14 — Downloader fix & storage cleanup (2026-10-04)
Status: **implemented and built.** Downloads now carry the stream's HTTP headers, retry and fall
back to backup mirrors, degrade gracefully across storage locations, and clean up partial/orphaned
chunks; Settings gained an accurate storage meter and a clear action. Full detail in **§2p**.

### Final UI & app checkup (2026-10-03)
Status: **implemented, release APK rebuilt → `dist/NovaStream.apk`.** Player now follows the live
theme (incl. its bottom sheets in light mode), every audited control meets the 48 dp floor, and
tab/reorder state survives rotation. Full detail in **§2n**.

### Phase 13 — Library crash fix, category filters & detail stream sheet
Full detail in **§2j**.

### Visual bug-fix pass (2026-10-02, post-Phase 12)
Status: **implemented, `:app:compileDebugKotlin` ✅, release APK rebuilt → `dist/NovaStream.apk`.**

- **Search field vertical centering.** `SearchField` (`Components.kt`) wraps its
  `BasicTextField` in a fixed 52 dp `Box` (`Alignment.CenterStart`) with `singleLine = true`,
  so the query text now centers with the search glyph and clear button.
- **Shared-element transitions removed.** `sharedElementFor`, `posterSharedKey` and
  `LocalSharedElementsEnabled` are deleted from `ui/nav/SharedTransition.kt`, and the call
  sites/guards in `PosterCard`, `DetailScreen`, `MediaRow`, Search, Library, Collection,
  Manga and Nsfw are gone — the unclipped `SharedTransitionScope` overlay was the source of
  posters painting over headers and being clipped mid-scroll.
- **Clipping + header layering hardened.** Every scroll container keeps `.clipToBounds()`
  (Home's `LazyColumn` added it). All top bars now carry an explicit `zIndex(10f)` and a solid
  `nova.background`/scrim: Home top scrim + `HomeTopBar`, Search header block, Browse title row
  + segmented chips, Collection header, Manga header, and Library section headers
  (`zIndex(1f)` → `10f`).
- Phase 8 §2e (shared-element poster→detail morph) and its Phase 10 scroll-gating are
  superseded by this removal; §2d items 15/16 are effectively reverted.

### Phase 11 — core player, manga reader & customization engine
Full detail in **§2h**.

### Phase 10 — device-tested UX & layout bug pass
Full detail in **§2g**.

### Phase 9 — detail hierarchy & navigation clean-up
Full detail in **§2f**.

### Phase 8 — shared-element transitions & motion
Full detail in **§2e**.

### Phase 7 (P0/P1) — foundations, typography & search
Full detail in **§2c**.

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

### Phase 6 — UX/UI pass (complete)
- **Full-bleed hero carousel (Home).** `FeaturedCarousel` now runs edge-to-edge (zero
  `contentPadding`/`pageSpacing`) with the dots overlaid inside the artwork at `BottomCenter`.
  `HeroSlide` was rewritten: height derives from the screen width (`w / 0.84`, clamped 400–540 dp),
  a radial artwork-tinted halo, top+bottom vertical scrims that dissolve into `nova.background`,
  and a center-aligned stack (FEATURED label → title → `Type • Genre • Year • rating` via the new
  `heroMeta()` helper → 2-line description → white pill **Play** button). The old boxed,
  left-aligned, rounded-corner `HeroSlide` was deleted.
- **Player timeline ticker.** `PlayerScreen` now owns `position`/`duration`/`isPlaying` state
  driven by a 250 ms `LaunchedEffect` loop plus an `onIsPlayingChanged` listener, so the
  timestamps, slider track and center play/pause icon update continuously instead of only when
  something else happened to recompose (they previously read `player.currentPosition` once per
  composition). ±10 s buttons also write `position` immediately for a snappy label.
- **Smooth scrubbing.** The bottom slider no longer seeks on every `onValueChange` (one
  `seekTo` per pixel = decoder thrash). Dragging only moves the thumb (`scrubbing`/`scrubValue`,
  ticker holds while true); `onValueChangeFinished` fires exactly one `seekTo` and hands control
  back to the ticker. The left timestamp follows the thumb while scrubbing.
- **Gesture HUDs (brightness / volume / seek).** The old centered `gestureHint` label was replaced
  by a real overlay rendered above the controls: brightness shows a vertical level bar on the
  left, volume a mirrored bar on the right (icon + live %), seek a center pill with
  position/duration. `onGestures` now *returns* the normalized level (`adjustBrightness` /
  `adjustVolume` return 0..1; a zero-delta call is a pure read used to seed the HUD on drag
  start). New `onGestureEnd` → `endGesture()` flushes the last seek and resets the volume
  accumulator. The HUD auto-hides ~1.4 s after the finger lifts (1400 ms job + fade).
- **Gesture seek throttling.** Horizontal drags accumulate into `pendingSeekMs` and flush at most
  every 300 ms (`SEEK_FLUSH_MS`, `SEEK_PX_TO_MS = 300f`), with a final exact flush on release —
  seeking once per frame tore the decoder down repeatedly and made scrubbing stutter. Volume now
  accumulates fractional drag and commits whole steps (`VOLUME_STEP = 0.15f`) instead of one step
  per frame, and `adjustVolume`/`gestureSeek`/`flushSeek`/`seekProgress` replaced `seekBy`.
- **Nav state preservation.** `SearchScreen`'s query is now `rememberSaveable` (survives tab
  switches and process death) and its `LaunchedEffect(initialQuery)` only re-searches when
  `results.isEmpty()`, so returning to the tab no longer clears the visible list; Home, Library
  and Search lists use `rememberLazyListState()` so scroll position is restored when switching
  tabs. (No `FLAG_ACTIVITY_*` flags, launch modes or `finishAffinity()` exist anywhere in the
  app, so nothing needed removing — the tab nav already uses `saveState`/`restoreState` and the
  ViewModels are scoped to their `NavBackStackEntry` via `novaViewModel`.)

---

## 2bb. UI/UX design-system pass — Android only (2026-10-30)

**Topic:** rebuild the reusable Compose UI layer (tokens → surfaces → components → Home + Detail) on
a direction derived from the product, without touching business logic, unit tests, the desktop
projects or the shipped v1.10.4 build.
**Scope guard:** `app/src/main/java/com/moovio/app/ui/**` + `data/model/Models.kt` only.
`desktop/` and `desktop-win/` have **no** diff; `settings.gradle.kts` is byte-identical; no version
bump, no `dist/` change, nothing committed or pushed.

### Direction
A private library for everything you watch and read, used on a phone in the dark.
Character: nocturnal, exact, unhurried. World: a projection booth — near-black neutrals, one violet
light source, artwork as the only colour on screen. Moments: the featured still, and the poster wall.
Signature: the violet progress rule (hero / continue-watching / reader), reused and not decorated.
The violet is kept because it is the real brand colour (launcher icon `#FF7C5CFF`), not because a
gradient looked good.

### Audit — what was actually wrong (measured, not assumed)
The bones were already deliberate (three text roles from one ramp, `AppSpacing`/`Motion` tokens,
tabular figures, reduced motion, 48 dp targets, shimmer skeletons). Three clusters were not:

| Sign | Where it lived |
|---|---|
| Poster-tinted radial wash over the artwork | `HomeScreen` hero 24 % alpha, `DetailScreen` header 32 % |
| Scrims seeded from `palette.darkVibrantColor` (hue gradient into the page) | both heroes |
| Primary CTA re-tinted per title (`palette.vibrantColor`) | `DetailScreen` |
| Accent-coloured drop shadow on the CTA (12 dp, alpha → 0.45) | `PrimaryActionButton` |
| "Glass" surface: gradient fill + 10 dp shadow + 20 % white rim highlight | `GlassSurface` (≈25 call sites) |
| 140 dp accent radial halo behind empty-state icons | `EmptyState` |
| Five hues + 35 % coloured borders on one badge | `ResolutionBadge` |
| Rating printed twice per poster (amber pill + meta line) | `PosterCard` |
| Bold(700) on every display/headline role, negative tracking at 20 sp | `MoovioTypography` |
| `FEATURED` uppercase with 0.8 sp tracking, tinted by the artwork | `HeroSlide` |
| `RoundedCornerShape(50)` on nav bar, segments, chips, badges, buttons, search clear | 20+ sites |
| `List<String>` / `List<MediaItem>` model fields, unannotated | `MediaItem`, `CatalogRow`, `WatchEntry` |

### What changed, by pass
1. **Color** — `MoovioSurfaceGlass` deleted; `MoovioAmoledBackground/Surface/Elevated` extracted as
   named tokens (the #000 swap used to be inline literals in `Theme.kt`, and cards now go true black
   too); `ActionInk`; `Color.contrastWith()` + `Color.actionFill()` return a (fill, content) pair per
   accent so all 8 user-selectable accents stay ≥ 4.5:1 (white on amber was ~1.8:1). M3
   `primary/onPrimary` now come from that pair, `secondary` is neutral (was cyan), error is toned
   (`#E5484D` dark / `#DC2626` light).
2. **Type** — display/headline roles Bold(700) → Medium(500); negative tracking reserved for 32 sp+;
   `headlineSmall` 20 sp/−0.2 → 19 sp/0; `labelSmall` 10.5 sp/0.8 → 11 sp/0.2; `bodyMedium` 13.5 → 14;
   `labelMedium` 11.5 → 12. `FEATURED` → `Featured` at white 72 %.
3. **Spacing / radius** — new `AppRadius` (chip 6 / control 10 / card 12 / panel 16) and
   `AppSpacing.tight` + `.page`; Home's `16/28/44/24/14/6 dp` literals replaced with tokens.
4. **Components** — `GlassSurface` → `SurfaceCard` (solid `surfaceElevated` + hairline; light theme
   keeps a 2 dp neutral shadow), renamed at all ~25 call sites; `PosterCard` gained a hairline edge
   and lost its bottom scrim and rating pill (rating now once, in the meta line, tabular);
   `EmptyState` halo → 64 dp hairline tile; `RatingBadge` neutral; `ResolutionBadge` 5 hues → 2 roles
   (high tiers accent, rest muted); `TagChip`/`SourceBadge`/`SearchField`/empty-state button off
   radius 50; search field 52 → 48 dp with a single-colour glyph; `LoadingRow` skeleton width now
   `AppSpacing.railCard` so the feed stops jumping when real rows land.
5. **Home** — hero wash gone; one neutral scrim (0.60 top → clear middle → page colour bottom) plus
   removal of the second 130 dp gradient that was stacked over the logo row; Play/Watchlist are
   radius-10 controls (off-white fill + hairline outline) with no Bold; dots 24/7 → 20/6.
6. **Detail** — accent is `moovio.accent` again (not artwork-derived); header halo and palette-tinted
   scrim gone, duplicate 104 dp top scrim removed; `PrimaryActionButton` loses the glow, gets radius
   10, 48 dp height and `actionFill`/`onAction` (progress bar tinted from `onAction`).
7. **Performance / stability** — `@Immutable` on `MediaItem`, `CatalogRow`, `WatchEntry` (their `List`
   fields made them unstable, so rail items could never skip) and on `MoovioColors`; the hero and
   detail header no longer call `rememberPosterPalette`, removing a per-slide/per-title bitmap pixel
   read; two full-width overdraw layers (the duplicate top scrims) deleted. Coil hardware bitmaps
   stay on — `allowHardware(false)` remains only inside the palette sampler, where it is required.
   No `Modifier.graphicsLayer` blurs / `renderEffect` glass was added: expensive inside lazy grids.

### Files
`ui/theme/Color.kt`, `ui/theme/Theme.kt`, `ui/theme/Tokens.kt`, `ui/components/Components.kt`,
`ui/screens/home/HomeScreen.kt`, `ui/screens/detail/DetailScreen.kt`, `data/model/Models.kt` — plus the
mechanical `GlassSurface` → `SurfaceCard` rename in `AddonManagerScreen`, `LibraryScreen`,
`MangaScreen`, `NsfwHubScreen`, `SettingsScreen`, `HomeOrderScreen`, `SourcesScreen`.
15 files, +460 / −388.

### Verification
`:app:compileDebugKotlin` ✓ (BUILD SUCCESSFUL, no warnings in the touched files) ·
`:app:testDebugUnitTest` ✓ **309/309** across 35 classes, `failures=0`, `errors=0` — the same
baseline v1.10.4 records, no test edited, skipped or deleted. Post-pass grep: `radialGradient` 0,
accent-coloured shadows 0, `vibrantColor` only inside `PosterPalette.kt`.
**Not verified:** nothing was rendered (no emulator run, no Chrome in this environment), so the
visual pass and the new type scale still need a device. `assembleDebug/Release` not run and no APK
rebuilt — `dist/` remains v1.10.4.

### Deliberately not done (next session)
- 30 `RoundedCornerShape(50)` and 22 `FontWeight.Bold` remain in `PlayerActivity`,
  `NsfwHubScreen`, `LibraryScreen`, `FloatingPillNavBar` and `PillSegmentedControl` — outside the
  brief's Home/Detail/components scope.
- `PosterPalette.kt` is now unused by the UI. Kept rather than deleted because `PaletteCacheTest`
  (3 cases) covers it; removing both is a clean follow-up.
- Root `./gradlew` fails at configuration **independently of this change**:
  `desktop/build.gradle.kts` declares `org.jetbrains.kotlin.jvm version "2.0.20"`, which collides with
  the Kotlin plugin already on the root classpath ("already on the classpath with an unknown
  version"). The verifications above were run with `include(":desktop")` temporarily removed and
  `settings.gradle.kts` restored byte-identically afterwards (`git diff` empty). One-line fix
  available in the desktop module; not applied here because this session is Android-only.

---

## 2bc. Phase 2 — motion, gestures & haptics (2026-10-30)

**Topic:** natural motion physics, a poster → detail shared-element morph, and structured tactile
feedback across the touch points people repeat — without adding layout jank or a bouncy-spring
vocabulary that contradicts the quiet direction §2bb settled on.
**Scope guard:** `app/src/main/java/com/moovio/app/ui/**` (plus two new files there),
`app/build.gradle.kts` and the two `build.gradle.kts` files needed to make the root build configure.
`desktop-win/` has no diff. No version bump, no `dist/` change, nothing committed or pushed.

### What the brief assumed vs. what the code actually had
Audited before editing, because three of its four workstreams were already partly built:

| Brief item | Reality found |
|---|---|
| Implement shared element transitions | `SharedTransitionLayout` + `ProvideSharedScope` already installed on **every** `NavHost` destination, but **zero** `sharedBounds`/`sharedElement` modifiers existed anywhere — the morph was scaffolding with no consumer, so navigation still cross-faded |
| Structured haptics into the player | Seek scrubbing already ticked once per whole second (`lastScrubHapticSec`); brightness/volume drags were **silent**, and cues were written inline as raw `performHapticFeedback` calls in 4 files |
| Low-bouncy springs on the carousel | The two nav controls already used `DampingRatioLowBouncy` + `StiffnessMediumLow` — the exact overshoot the §2bb direction removes. The carousel was on the pager's default snap |
| Spring swipe-to-dismiss for sheets | All seven sheets are M3 `ModalBottomSheet`, whose drag-to-dismiss is already spring-driven — nothing to add |

### Direction (unchanged from §2bb)
A private library for everything you watch and read, used on a phone in the dark. Character:
nocturnal, exact, unhurried. World: a projection booth — near-black neutrals, one violet light source,
artwork as the only colour. **Moments:** the poster → header morph, and the featured still.
**Signature:** the violet progress rule.
Motion follows from the character: weight, not bounce. A page or an indicator **decelerates into
place and stops**; it never overshoots and wobbles back.

### 1. Shared element (navigation physics)
- New `ui/components/SharedPoster.kt`: `Modifier.sharedPoster(key)` reads the two locals published by
  `ui/nav/SharedTransition.kt`, keys the state as `"poster:$key"` and sets
  `boundsTransform = BoundsTransform { _, _ -> Motion.settle<Rect>() }`. Returns `this` untouched when
  no scope is in the tree, so previews/tests need no guard.
- Applied at both ends: `PosterCard`'s artwork `Box` (source — covers Home, Browse, Library, Search
  rails) and `DetailHeader`'s backdrop `AsyncImage` (destination). Keyed on `MediaItem.key`, which is
  what makes the two ends of a navigation match.

### 2. Haptic engine
- New `ui/theme/Haptics.kt`: `@Stable class MoovioHaptics` with `select()` (light tick, `TextHandleMove`),
  `toggle()` (same weight, for state that flips) and `commit()` (`LongPress`, for a long press that
  opens a destructive confirmation) + `rememberMoovioHaptics()`. `HapticFeedback` is an interface, so
  the class stays testable.
- `PlayerActivity`: brightness/volume drags now tick once per **10 % band** (`lastLevelStep`, reset in
  `onDragStart`) — per-pixel ticks are unreadable and one long drone is worse than silence. The five
  pre-existing raw calls (double-tap seek, hold-to-boost, scrub-per-second, speed HUD) now route
  through the helper.
- `PrimaryActionButton` press, the header heart, and season chips (skipped when already selected).
- Long press on a Library card fires `commit()` from inside `PosterCard`, so all three Library grids
  got it from one change.

### 3. Motion physics
- New `Motion.settle<T>(stiffness = Spring.StiffnessLow)` — `DampingRatioNoBouncy` + low stiffness,
  generic so one curve drives `Dp` offsets, `Float` scroll targets and the morph's `Rect`.
- Carousel auto-advance: `animateScrollToPage(…, animationSpec = Motion.settle())`.
- `PillSegmentedControl` + `FloatingPillNavBar` indicators moved off `DampingRatioLowBouncy` onto
  `settle(StiffnessMediumLow)` — quicker than a page change, still no overshoot.

### 4. Residual surfaces + build
- `PillSegmentedControl`: panel radius 16 / indicator 12 (the nested step for a 4 dp inset),
  selection is a neutral `textPrimary` 10 % wash instead of an accent fill, one label weight.
- `FloatingPillNavBar`: 64 dp (was 66, off the 4 dp grid), no 85 %-alpha glass, `surfaceElevated` +
  hairline, no shadow on dark, 12-radius indicator, 20 dp icons, 4 dp inset.
- `LibraryScreen`: deleted the dead local `LibrarySegmentedControl` duplicate (nothing called it — the
  screen already used the shared control) and moved the filter chips to `AppRadius.chip` with a
  surface step + hairline instead of an accent tint.
- `NsfwHubScreen`: "Private" pill → hairline chip, two accent washes → `surfaceElevated`, three
  `Bold` dropped, remove discs now `CircleShape` (intent, not a 9999 radius).
- `PosterPalette.kt` **deleted** (zero references outside its own file) with `PaletteCacheTest.kt`
  (3 cases) and the now-unused `androidx.palette` dependency.
- Root `build.gradle.kts` declares `org.jetbrains.kotlin.jvm 2.0.20 apply false`;
  `desktop/build.gradle.kts` drops its versions. Reproduced first: *"the plugin is already on the
  classpath with an unknown version"* — `:desktop` asked for a **versioned** Kotlin plugin after
  `kotlin.android` had already put Kotlin on the root classpath. `settings.gradle.kts` needed no change.

### Files
New: `ui/components/SharedPoster.kt`, `ui/theme/Haptics.kt`. Deleted:
`ui/theme/PosterPalette.kt`, `test/…/PaletteCacheTest.kt`. Edited: `ui/theme/Tokens.kt`,
`ui/components/Components.kt`, `ui/components/PillSegmentedControl.kt`, `ui/nav/FloatingPillNavBar.kt`,
`ui/player/PlayerActivity.kt`, `ui/screens/home/HomeScreen.kt`, `ui/screens/detail/DetailScreen.kt`,
`ui/screens/library/LibraryScreen.kt`, `ui/screens/nsfw/NsfwHubScreen.kt`, `app/build.gradle.kts`,
`build.gradle.kts`, `desktop/build.gradle.kts`.

### Verification
`:app:compileDebugKotlin` ✓ BUILD SUCCESSFUL · `:app:testDebugUnitTest` ✓ **306/306**, `failures=0`,
`errors=0` (309 − the 3 `PaletteCacheTest` cases that went with the deleted class) · `./gradlew help`
at the repo root ✓ exit 0, which was failing at configuration before the plugin fix.
**Not verified:** nothing was rendered (no emulator, no device, no Chrome here), so the morph, the
springs and the haptics are compile-verified only. Frame drops were **not** measured — no device, so
"zero frame-drops" is unproven; what is true is that no new per-frame state reads were added.

### Open risk
A title can appear twice on Home (featured banner **and** a rail), so two composables can register
`poster:<key>` at once — the case Compose documents as unsupported. Expected behaviour is that the
tapped copy wins, not a crash, but it has not been proven on a device. If the morph starts from the
wrong copy, the fix is a per-source key passed down from each rail.

**Resolved in §2bd:** `sharedPoster(key, rail)` + `?from=<rail>` on the detail route make the key
`poster:<rail>:<itemKey>` on both ends, so two rails can show the same title without colliding.

### Deliberately not done
- 19 `RoundedCornerShape(50)` remain (`PlayerActivity` 9, `Components.kt` 4, `HomeScreen` 4,
  `AddonManagerScreen` 1, `SearchScreen` 1) and 14 `FontWeight.Bold` (`PlayerActivity` 13,
  `MainActivity` 1). Most are genuine circles (6–26 dp dots, icon discs) or player chrome, which this
  phase was not asked to touch.
- `PosterCard` still carries a 14 dp-odd `AppRadius` poster shape unchanged; no motion was added to
  grids, so `HomeScreen`/`BrowseScreen`/`NsfwHubScreen` lazy grids keep their recomposition behaviour.
- `PillSegmentedControl` keeps its now-inaccurate name — renaming a shared composable would touch
  every call site for a word.

---

## 2bd. Phase 3 — media player, manga reader, diagnostics & audio (2026-10-30)

**Scope:** the Phase 3 directive's four workstreams, Android only. Three of them were not what the
brief assumed — recorded first, because they changed what was built.

### What the brief assumed vs. what the code had

| Assumption | Reality |
|---|---|
| Stats for nerds is a text chip to "refactor into" a canvas | It was already a `Column` of `StatsLine` rows inside `PlayerScreen`, rebuilt from `Player` getters on every parent recomposition |
| Add a `Spatializer` toggle on API 32+ | `Spatializer` exposes `isAvailable`/`isEnabled` and **no setter** — the enable switch is user-only, in system settings. An app can only mark its stream as spatialisable |
| Reader needs "vertical parallax" and a zoom lock | Zoom lock already worked (`zoomLock` keeps `MutableZoom` across pages). There is no vertical gesture surface in a horizontally paged reader, so no parallax was invented |
| 3-page preload may stall the UI thread | `loader.enqueue` is already async; the loop that built the requests ran on the main dispatcher |
| Duplicate shared-transition keys are a risk | Confirmed: `poster:<itemKey>` collided whenever one title sat in two rails of the same feed |

### 1. Canvas diagnostics HUD — `ui/player/StatsHud.kt` (new)

- `StatsHud(player, modifier)` runs its own `LaunchedEffect` loop at 250 ms and keeps the reading in
  a local `mutableStateOf(StreamReading)`, reading `totalBufferedDuration`, `bufferedPercentage`,
  `videoFormat`/`audioFormat`, `videoSize`, `videoDecoderCounters.droppedBufferCount` and
  `playbackState`. The bandwidth estimate comes from an `AnalyticsListener` subscribed **inside** the
  HUD (`DisposableEffect` on the player), so the number it drives cannot invalidate anything outside
  this subtree — the previous shape hoisted every counter into `PlayerScreen` and recomposed the
  whole control overlay each tick.
- Drawn on a `Canvas`: a **buffer-ahead sparkline** over a 40-sample window (= 10 s at 250 ms) with a
  hairline mid-line, then three `labelSmall` + `tnum` rows — buffer ahead/percent, stream + download
  bitrate, resolution · fps · dropped — and the codec string. The sparkline is the buffer, not the
  bitrate: "is this stream keeping up" is a shape question, and the bitrates read fine as numbers.
- One accent, used only for the line. `StreamReading` is `@Immutable` and rebuilt once per tick.

### 2. Spatial audio (Speed & Audio sheet)

- `SettingsStore`: `MANGA_*` neighbour `audioSpatial` (boolean, default off) with `setAudioSpatial`;
  `MoovioApp` keeps an app-lifetime subscription like the other player settings, and
  `PlayerActivity` reads it before the player is built, so the choice survives recreation.
- `applySpatialAudio` builds `androidx.media3.common.AudioAttributes` with
  `C.SPATIALIZATION_BEHAVIOR_AUTO` (on) or `..._NEVER` (off) and hands it to `ExoPlayer` — applied
  once after `player = exo` and again on toggle. Guarded by `SDK_INT >= S_V2`.
- The row only appears when `spatializer.isAvailable && spatializer.isEnabled`, so it is never a
  switch that moves without changing anything. Copy says "Device 3D audio, when headphones support it".
- Boost (`LoudnessEnhancer` 1000 mB) and normalize (400 mB) are unchanged and still re-attach on
  `onAudioSessionIdChanged`, which the attribute change above can trigger.

### 3. Manga reader

- **E-ink preset** — new `MANGA_EINK` setting, `SwitchRow` in Settings → Manga Reader, threaded
  through `SettingsViewModel`. `mangaFilter(invert, grayscale, warm, eink)` short-circuits on `eink`:
  saturation 0 followed by contrast 1.45 pivoted on mid-grey (`out = 1.45·in − 57.4`), which is the
  range an e-ink panel resolves. It **replaces** invert/sepia rather than stacking with them.
- **Bounded panning** — `MutableZoom.panBy`/`clampPan` limit the offset to how far the page actually
  overflows the box at the current scale, so a long drag can no longer push the artwork off screen
  and leave a blank reader. Re-clamped after a pinch, so zooming out pulls a far-panned page back.
- **Preload off the main thread** — the webtoon preload loop now runs inside
  `withContext(Dispatchers.IO)`; `enqueue` still does its own dispatching.

### 4. Rail-scoped morph keys (§2bc's open risk, closed)

- `Modifier.sharedPoster(key, rail)` keys on `poster:<rail>:<key>` when a rail is given and
  `poster:<key>` otherwise.
- `PosterCard`/`MediaRow` take `rail`; `HomeScreen` passes `home:<row title>`, `AnimeScreen`
  `anime:<row title>`, `MangaScreen` `manga:<row title>`, `NsfwHubScreen` `nsfw:<row title>`.
- `Routes.detail(item, from)` appends `?from=<rail>`, `Routes.DETAIL` gained a nullable `from`
  argument, `MoovioNav` reads it and passes it to `DetailScreen(..., fromRail)`, which hands it to
  `DetailHeader`'s backdrop. Both ends of the morph therefore share one unique key.
- Cards outside a rail (library, search, collection, detail's own related row) pass `null` on both
  ends and keep the item-only key.

### 5. Player chrome on the tokens

- Sheet headings (`Quality`, `Audio`, `Subtitles`, `Sleep timer`, `Episodes`, `Open in external
  player`) use `MaterialTheme.typography.titleMedium` instead of `Bold` + hard-coded 16 sp; the
  in-sheet sub-headings use `titleSmall`; every remaining `FontWeight.Bold`/`SemiBold` in the file is
  now `Medium` (0 Bold left, was 13).
- Round 40 dp speed buttons and the 48 dp screen-lock button use `CircleShape`; the two 6 dp gesture
  bars keep `RoundedCornerShape(50)` because a capsule is the intended shape there.

### Files

New: `ui/player/StatsHud.kt`.
Changed: `ui/player/PlayerActivity.kt`, `ui/player/MangaReaderActivity.kt`,
`ui/components/SharedPoster.kt`, `ui/components/Components.kt`, `ui/nav/MoovioNav.kt`,
`ui/screens/detail/DetailScreen.kt`, `ui/screens/home/HomeScreen.kt`,
`ui/screens/anime/AnimeScreen.kt`, `ui/screens/manga/MangaScreen.kt`,
`ui/screens/nsfw/NsfwHubScreen.kt`, `ui/screens/settings/SettingsScreen.kt`, `ui/vm/ViewModels.kt`,
`data/local/SettingsStore.kt`, `MoovioApp.kt`.

### Verified / not verified

- `:app:compileDebugKotlin` ✅ (clean apart from pre-existing deprecation warnings).
- `:app:testDebugUnitTest` ✅ **306/306**, 0 failures, 0 errors (34 report files).
- **Not verified:** anything that needs a device — the HUD's tick cost, the morph starting from the
  correct copy now that keys are unique, spatial audio on real headphones, e-ink output on an e-ink
  panel, and the reader's pan clamp under a real pinch. No frame timing was measured.

### Deliberately not done
- No vertical parallax in the paged reader (no vertical gesture surface exists; adding one would
  fight the pager's horizontal drag).
- `LoudnessEnhancer`/`Spatializer` state is not mirrored back into the sheet — the sheet reflects the
  app's choice, not the OS's runtime state.
- `MainActivity` still has one `FontWeight.Bold`; it is not player chrome.

---

## 2be. Phase 4 — encrypted backups, smart search & release readiness (2026-10-30)

**Scope:** close ISSUE-15, add local natural-language search, put a floor under download storage, and
finish the housekeeping a release would need. Android only; `desktop/` and `desktop-win/` untouched.

### What the brief assumed vs. what the code had

| Assumption | Reality |
|---|---|
| Backups are encrypted with `EncryptedSharedPreferences`/`MasterKey` | They were plain JSON, secrets included. `androidx.security:security-crypto` is **deprecated upstream** (1.1.0-alpha) and would not have helped anyway: the file is not a preferences blob |
| A legacy plaintext file can be "gracefully" handled silently | It holds live tracker tokens, so silently accepting it is the bug, not the fix. It imports only after an explicit confirmation |
| Search has no filter support to extend | It had type filters and a sort selector in the UI, but the query string went to providers verbatim |
| The downloader needs orphan cleanup built | `DownloadCacheMaintenance` and `clearTemporaryFiles()` already existed — what was missing was a *trigger* and a user-set floor |

### 1. ISSUE-15 — encrypted backups

- **New `data/integrations/BackupCrypto.kt`** — two pieces, both dependency-free JCE:
  - `BackupFormat`: container layout `MAGIC("MOOVIO-BACKUP-1") || IV(12) || ciphertext+tag(16)`, with
    `isEncrypted()` so an import can tell a container from a legacy `.json` by content, not extension.
  - `BackupCipher(keyProvider)`: `AES/GCM/NoPadding`, 256-bit, a **fresh random IV per export** (IV
    reuse would break GCM outright), 128-bit tag. `keyProvider` is injected so the real cipher runs in
    JVM tests.
  - `AndroidBackupKey`: 256-bit AES key generated once in the Android KeyStore
    (`AndroidKeyStore`, alias `moovio_backup_aes`, no user-auth gate — a restore must work without a
    biometric prompt).
- **`BackupManager`** now: `exportTo` writes the encrypted container; new `inspect(uri)` returns
  `BackupInspection.Encrypted` or `BackupInspection.LegacyPlaintext` **before** touching any store;
  `importFrom(uri, allowPlaintext = false)` refuses a plaintext file unless the caller opted in
  (`PLAINTEXT_NEEDS_CONFIRMATION`). Payload `version` is 2. Snapshot assembly was factored into one
  `snapshot()` so export and its "N items" report can't disagree.
- **Settings** — export is `application/octet-stream` / `moovio-backup.mbackup`; the import picker
  accepts both types and routes a plaintext file to an **"Unencrypted backup"** dialog explaining
  what it contains before offering *Restore*.
- **Trade-off, documented in code:** the key never leaves the install, so an encrypted backup is not
  portable between devices. That is deliberate (it is what makes the file safe at rest), and it is
  why the legacy path still exists.

### 2. Smart search — `data/search/SmartQuery.kt` (new)

- `SmartSearchParser.parse(raw, currentYear)` → `SmartQuery(text, type, yearFrom, yearTo, minRating,
  genre, sort)`. Recognises: type words (movie/film, tv/series/show, anime, manga/comic), 18 genres
  (incl. the `sci-fi`/`science fiction`/`sci fi` spellings), a year (`2024`), a decade (`2020s`), a
  two-year range, `high rating`/`top`/`best` (floor 7.0), an explicit score (`rating 8.5`),
  `recent`/`latest`/`new` (last two years) and `classic`/`retro` (pre-1990).
- **Year heuristic:** a four-digit token counts as a year only up to `currentYear + 1`, so
  "Blade Runner 2049" stays a title instead of becoming a filter for an unreleased year.
- **Routing:** the compiled *type* reaches the network (the repository already searches per type);
  year, rating and genre are applied to the merged result via `applyTo()` — add-on catalogs accept no
  such parameters, so narrowing locally is the honest half. `summary()` gives one muted line under
  the field ("Anime · 2024 · rated 7+ · best rated first") and is null for plain text.
- **Pipeline untouched:** `SearchViewModel.search()` still cancels the previous job, waits
  `SEARCH_DEBOUNCE_MS` (350) and publishes only the newest `generation`. The parser runs before the
  debounce, and a query made only of filters ("high rating") publishes an empty list rather than
  asking a provider for its whole catalog.

### 3. Storage floor (ISSUE-18 prep)

- `DownloadCacheMaintenance.isBelowLimit(free, limit)` / `shouldSweep(free, limit, orphanCount)` —
  pure, and deliberately conservative: a negative `usableSpace` (a mount that just vanished) never
  triggers a sweep, and a limit of 0 means off.
- `VideoDownloadManager.sweepOrphansIfLowOnSpace(thresholdMb)` deletes only chunks no `SimpleCache`
  index references — never a completed or in-progress download — and returns bytes reclaimed.
- `SettingsStore.LOW_SPACE_MB` (default 1024, 0 = off), a Settings → Downloads row that steps
  0/256/512/1024/2048/4096 MB, and a boot-time call from `MoovioApp`'s app scope.

### 4. Release readiness

- `versionCode` 24 / `versionName` 1.11.0 in `app/build.gradle.kts`. **No APK was built and nothing
  was pushed**, so `dist/` still holds v1.10.4 and the updater will report 1.11.0 as available to a
  build made from this tree.
- Unused-import pass over the touched files: removed `Flow` (`SettingsStore`), `Switch` and
  `FontWeight` (`SettingsScreen`), `Close` (`SearchScreen`) and `asStateFlow` (`ViewModels`). The
  `getValue`/`setValue` imports stay — they are what makes `by remember` delegation compile even
  though the names never appear in the source, so a name-only scan flags them as dead.

### Files

New: `data/integrations/BackupCrypto.kt`, `data/search/SmartQuery.kt`,
`test/…/BackupCryptoTest.kt`, `test/…/SmartSearchParseTest.kt`.
Changed: `data/integrations/BackupManager.kt`, `data/download/DownloadCacheMaintenance.kt`,
`data/download/VideoDownloadManager.kt`, `data/local/SettingsStore.kt`,
`ui/screens/settings/SettingsScreen.kt`, `ui/screens/search/SearchScreen.kt`,
`ui/vm/ViewModels.kt`, `MoovioApp.kt`, `app/build.gradle.kts`.

### Verified / not verified

- `:app:compileDebugKotlin` ✅ (deprecation warnings only, all pre-existing).
- `:app:testDebugUnitTest` ✅ **327/327**, 0 failures, 0 errors, 0 skipped (+21: 8 crypto, 13 parser).
- Crypto tests cover both halves of the requirement: encrypted payloads round-trip with the exact
  bytes, tampering and a foreign key are rejected, and a legacy plaintext export is recognised as
  *not* a container and still parses into a payload with its cookie/token intact.
- **Not verified:** the KeyStore path itself (generation, and reopening the key after process death)
  needs a device — the JVM tests drive the same `BackupCipher` with a generated key. Also unverified:
  the SAF pickers' MIME filtering on a real file manager, and the boot sweep against a genuinely full
  device.

### Deliberately not done
- No `androidx.security:security-crypto` dependency (deprecated, and wrong shape for a file backup).
- No passphrase/portable-key option: encrypted backups are per-install by design.
- The parser does not strip stop words like "from"/"the", so a sentence-style query keeps its
  filler words; stripping them would damage real titles more often than it helps ("From Dusk Till
  Dawn", "The Batman").
- ISSUE-18's actual fix (exporting HLS downloads to a custom folder) is untouched — only its storage
  cleanup prerequisite landed.

---

## 2bf. v1.11.0 released — the four-phase overhaul (2026-10-30)

**Topic:** the release that carries Phases 1–4, closes ISSUE-15 and lands the download-storage
prerequisite. Android only; `desktop/` and `desktop-win/` untouched.

**Build**
- `versionCode` 24 / `versionName` 1.11.0 (`app/build.gradle.kts`).
- `:app:assembleRelease` ✅ (2 m 30 s, `--offline`) signed through `signingConfigs.release`, whose
  material comes from the untracked `local.properties` (`moovio.storeFile` / `storePassword` /
  `keyAlias` / `keyPassword`) — never from the repo.
- APK: **44,129,211 bytes**, SHA-256 `8B212EF4E1A423A1718FFEBE5D22B1914EEF37CDA1962E580C013E91EA547311`.
- Verified in the archive with `aapt dump badging`: `package com.moovio.app`, `versionCode 24`,
  `versionName 1.11.0`, `minSdk 24`, `targetSdk 34`, `compileSdk 36`, `application-label 'Moovio'`.
- Verified with `apksigner verify --print-certs`: V2 signer `CN=Moovio, OU=Release, O=Moovio, C=US`,
  certificate SHA-256 `2EDB949B325BF13B6B5CA695FF3E3068F9DA5F24C4873BA0F12D2A7D99351379` — the same
  key v1.10.0 shipped with, so an in-place upgrade from 1.10.x is possible.
- Artifacts: `dist/app-release.apk` and `dist/Moovio.apk` (byte-identical, one SHA-256). The site CTA
  fetches `releases/latest/download/app-release.apk`, so that exact asset name has to exist.
- **Not pushed, not tagged, no GitHub Release.** The upload step is manual and still outstanding, so
  the in-app updater and the website both keep serving v1.10.4 until it is done.

**Size / code**
- Main sources: **172 Kotlin files, 34,074 lines**.
- Test sources: **36 Kotlin files, 4,787 lines**, 327 cases.

**Phases in this release**

| Phase | Section | What shipped |
|---|---|---|
| 1 | §2bb | Design system: `MoovioColors` `@Immutable` + `hairline`/`actionFill`, flat `SurfaceCard`, `AppRadius`, type scale 700 → 500, tinted washes and glowing CTAs removed, **AMOLED tokens** for True Black (surfaces and cards both go #000) |
| 2 | §2bc | Motion: poster → detail **shared-element morph** (`Modifier.sharedPoster`, `Motion.settle<Rect>()`), `MoovioHaptics` (`select`/`toggle`/`commit`) across player drags, CTA, heart, season chips and Library long-press |
| 3 | §2bd | Diagnostics & reader: **canvas `StatsHud`** with its own 250 ms ticker (buffer sparkline, bitrates, dropped frames, codec), spatial-audio attributes on API 32+, e-ink reader preset, bounded panning, rail-scoped morph keys |
| 4 | §2be | Security & search: **AES-256-GCM encrypted backups** (KeyStore key, legacy plaintext import behind a confirmation), **offline natural-language query compiler** (`anime 2024 high rating`, `scifi movies`, `recent action`), download free-space floor |

**Release-note highlights (copy for the tag body)**
- AMOLED theme tokens: True Black now reaches surfaces and cards, not just the background.
- Shared-element morph: a poster grows into the detail header instead of the screens cross-fading.
- `MoovioHaptics`: one haptic vocabulary — stepped ticks on brightness/volume drags, toggles, and a
  heavier cue on long-press.
- Canvas `StatsHud`: the "stats for nerds" panel is drawn, and a stats tick no longer recomposes the
  player's controls.
- Encrypted backups: exports are AES-256-GCM containers bound to the install's KeyStore key; old
  plaintext backups still restore, after a warning that says what they contain.
- Offline smart search: typed filters (type, year, decade, rating floor, genre, recency) compile
  locally before the 350 ms debounce, and the screen shows what it understood.

**Closed by this release:** ISSUE-15 (plaintext backup secrets) — §2be; the ISSUE-18 storage
prerequisite (orphan sweep under a user free-space floor) — §2be. **Still open:** ISSUE-18 itself
(HLS export to a custom folder), ISSUE-16 (wrong tracker ids), ISSUE-17 (NSFW unlock persistence),
ISSUE-26 (no CI).

**Deferred / unverified**
- Aniyomi/Mihon **extension execution** (the toolchain, not the APK downloader) is still the next
  phase; `MangaExtensionRepository` only resolves and downloads APKs today.
- No device pass has been run for any of the four phases: the morph, haptics, HUD tick cost, e-ink
  output, KeyStore re-open after process death, the boot sweep on a full disk and every spring are
  compile- and unit-verified only. No frame timing has been measured, before or after.
- The Sudachi/emulator and tracker ids work are untouched.

---

## 2ay. v1.10.0 released (2026-10-22)

**Topic:** the first release under the **Moovio** name, bundling the rename (§2aw) and the security /
stability remediation (§2ax).

**Build**
- `versionCode` 19 / `versionName` 1.10.0 (`app/build.gradle.kts`).
- Signed with the **rotated** key: `~/.moovio/moovio-release.jks` (alias `moovio`), signer SHA-256
  `2EDB949B325BF13B6B5CA695FF3E3068F9DA5F24C4873BA0F12D2A7D99351379`, DN `CN=Moovio, OU=Release,
  O=Moovio, C=US`.
- APK: 43,946,885 bytes, SHA-256 `ED8C54188C4512B020AC1BE18F19290EEA7B617438302723D172ECD35839D54F`.
  Verified in the archive: `package com.moovio.app`, `versionCode 19`, `versionName 1.10.0`,
  `minSdk 24`, `targetSdk 34`, `application-label 'Moovio'`.
- Artifacts: `dist/app-release.apk` + `dist/Moovio.apk`, attached to the GitHub Release (the site CTA
  fetches `releases/latest/download/app-release.apk`, so that exact asset name has to exist).

**⚠️ Upgrade note for users:** this build **cannot be installed over v1.9.0**. The package id
changed (`com.novastream.app` → `com.moovio.app`) *and* the signing key was rotated, so Android
treats it as a different app — the old install has to be removed first. Settings, library and
downloads live in the old app's sandbox and do not carry over.

**Site:** hero/footer/nav badge → v1.10.0, stats → 292 unit tests, install size → ≈44 MB (the
previous "≈42 MB" hero claim never matched the artifact). `npm run build` ✓ (8/8 static pages).

**Verification:** `:app:compileDebugKotlin` ✓ · `:app:assembleDebug` ✓ · `:app:assembleRelease` ✓
· `:app:testDebugUnitTest` ✓ **292/292** (281 existing + 11 new).

---

## 2ba. Extension repositories — pre-seeded, parsed, installable (2026-10-23)

**Topic:** make the four requested extension repositories real in the Add-on Manager, NSFW-aware,
and get as far as the current architecture allows. **Status: repository layer done and verified;
extension *execution* (Aniyomi/Mihon source runtime) is the one piece still missing** — see
“What still does not work” below, which is the honest limit of this session.

### What was wrong (measured, not assumed)
| Repo | Before |
|---|---|
| phisher98 CloudStream | ✅ 86 providers parse and `.cs3` install/load works (existing runtime) — but **0** NSFW providers, so no adult content from it |
| CXXX (adult CloudStream) | not configured anywhere |
| yuzono Aniyomi | 251 entries parsed, 72 `nsfw`, but every `apk` is a **bare file name** and the files live at `<repo dir>/apk/<name>` — the stored value 404s, and install only wrote metadata |
| Keiyoushi Mihon | the `index.min.json` mirror is a **2-entry stub** and was preferred over the `.pb`; the `.pb` itself is a **Repository wrapper** (`extensions = field 101`, `apk` a nested message, `nsfw` an enum at field 7 where `3` = adult), so the old flat parser produced a handful of junk rows instead of 1425 extensions |

Nothing from Aniyomi/Mihon could ever run: their extensions implement `eu.kanade.tachiyomi.*`
source APIs and the app's extension host only knows its own `MoovioExtension` interface (CloudStream
`.cs3` is the only execution engine).

### What now works
1. **Pre-seeded repositories** (`AddonStore.Defaults.repos`): Phisher98 (movies/TV), **CXXX 18+**
   (`RemoteRepo.nsfw = true`), Yuzono (anime), Keiyoushi (manga). The 2026-10-07 source-neutral
   shipping decision is superseded at the user's request; the repos still install nothing by
   themselves. `ensureDefaultRepos()` merges them into existing installs and still prunes retired
   URLs.
2. **Keiyoushi parser rewritten** (`data/remote/KeiyoushiRepo.kt`): repository wrapper
   (`field 101`), nested `apk.url`, `nsfw == 7 enum 3`, `sources = field 8 {id,name,lang,baseUrl}`,
   version + version code, legacy flat index still supported. `fetch()` follows the URL (`.pb` →
   protobuf before the JSON stub) instead of preferring `index.min.json`.
3. **Aniyomi parser resolves download URLs** (`AniyomiRepo.resolveApk`): relative
   `aniyomi-all.x-v14.4.apk` → `…/repo/apk/aniyomi-all.x-v14.4.apk`; absolute URLs pass through.
   Version code (`code`) and the source repo are stored on each entry.
4. **Install / enable / update** (`data/repo/MangaExtensionRepository.kt`): downloads the APK into
   app-private `files/manga-ext/<pkg>.apk` (≥2 KB sanity check, package name sanitised), persists
   the descriptor (`apkPath`, `versionCode`, `nsfw`, `repoUrl`), toggles enabled state and detects
   updates (`versionCode` strictly greater; no guessing from version strings). Stored as
   `disabled` rather than `enabled`, because Gson bypasses constructors and a missing field must
   decode to the safe default (“enabled”) instead of silently disabling every old row.
5. **NSFW gating** (`RemoteRepo.nsfw`, `MangaExt.nsfw`, `CloudStreamExt.nsfw`, new
   `settings.showNsfwExtensions` default **off**): the Add-on Manager lists `nsfw = 1` extensions
   only after the “Show NSFW extensions” switch is on, and shows how many are hidden. Everything
   browsed from CXXX is stamped NSFW even though its plugin entries carry no flag.
6. **Real 18+ uses CXXX**: `CloudStreamRepository.providers(includeNsfw)` splits runnable providers
   by the plugin's repo flag; `CatalogRepository.cloudStreamSearch` excludes adult providers from
   general search and `searchRealProgressive` queries them as a Real 18+ fallback (results retyped
   `MediaType.REAL`). Adult CloudStream plugins therefore *play* today through the existing
   runtime.

### What still does not work (next phase)
- **NSFW Anime / NSFW Manga content from Yuzono/Keiyoushi does not play or read yet.** The APKs
  install, enable and update, but nothing executes their `AnimeSource` / `CatalogueSource` code,
  so they contribute no catalogues, chapters or streams. Implemented state = “downloaded and
  known”, not “running”.
- Runtime plan (sized as one axis at a time, manga first):
  1. add the extension libraries as dependencies — **not usable as-is, see the reality check below:**
     `com.github.mihonapp:extensions-lib:1.6.0` (Mihon/tachiyomi sources) and
     `com.github.komikku-app/aniyomi-extensions-lib:13` (Aniyomi anime sources) — the same pattern
     as the CloudStream `library` + shims (§2al);
  2. load an installed APK with a `PathClassLoader` whose parent is the app loader, scan its dex
     for `CatalogueSource` / `AnimeCatalogueSource` implementations, instantiate them;
  3. adapt sources into the existing seams (`nsfwMangaRows`/`search` → chapters → pages;
     NSFW-anime episodes → stream resolution), gated by `nsfw` exactly like today;
  4. verify on hardware. An AVD exists in this environment (`framed_test`), but the Emulator and a
     physical device were not exercised for this change, and extension execution is the kind of
     code that only fails on device (DexClassLoader, per-extension reflection, Jsoup/OkHttp
     runtimes).
- **Reality check (measured 2026-10-23, overriding the plan above):** step 1 is not available
  today. `com.github.mihonapp:extensions-lib:1.6.0` fails `:app:checkDebugAarMetadata` (requires
  `compileSdk 37`; this build is AGP 8.5.0 / `compileSdk 34`) and pulls `kotlin-stdlib 2.4.0`,
  `okhttp 5.4.0`, `rxjava 1.3.8`, `jsoup 1.22.2`. `com.github.komikku-app:aniyomi-extensions-lib:13`
  is the **stubs** lib only (45 classes, no host implementation), and no full Aniyomi API is
  published as an artifact. The APKs don't carry the API either: `dexdump` shows a real Keiyoushi
  extension (44 classes) defines no `HttpSource`, and a real Yuzono one (33 classes) defines no
  `SEpisode`/`Video` — both only reference them (and `injekt`). So the runtime starts with a
  toolchain migration (AGP ≥ 8.9, `compileSdk` 36/37, Gradle, Kotlin 2.1–2.4 incl. the
  Compose-compiler move, OkHttp 5) plus an Aniyomi API source.
- **Alternatives:** (a) do the toolchain migration and land Mihon first, then Aniyomi; (b) skip
  the APK runtime and add native NSFW scrapers for the top Yuzono/Keiyoushi sites in the existing
  `data/hentai` + `data/manga` pattern — smaller, but only for hand-implemented sites.

### Release (2026-10-23, v1.10.2)
Shipped the repository layer **without** claiming the runtime: `versionCode` 21 / `versionName`
1.10.2; signed APK 43,963,421 bytes, SHA-256
`EB952AB92325CF2A0029830E223F3E4BFCBE7BA49208E69D05698A2D91302D54`, signer
`2EDB949B…351379`; commit `ea2e57c`, tag `v1.10.2`; GitHub Release published with
`app-release.apk` + `Moovio.apk` (verified latest → HTTP 200 / 43,963,421 bytes, digests match);
website badges → v1.10.2, stats → 309 (`22c26a4`, `next build` ✓). Toolchain migration deliberately
**not** started, per the user's decision. Device pass still outstanding: seeded repos, Add-on
Manager NSFW switch, extension install/update, CXXX Real 18+ search.

### Files
`data/model/Models.kt` (`RemoteRepo.nsfw`, `MangaExt.{versionCode,repoUrl,apkPath,disabled}`,
`CloudStreamExt.nsfw`), `data/local/AddonStore.kt` (four seeds + comments),
`data/remote/AniyomiRepo.kt`, `data/remote/KeiyoushiRepo.kt` (rewritten),
`data/cloudstream/CloudStreamRuntime.kt` (`CloudStreamInstalled.nsfw`),
`data/repo/CloudStreamRepository.kt` (`providers(includeNsfw)`),
`data/repo/MangaExtensionRepository.kt` (new), `data/repo/AddonRepository.kt`
(`setMangaExtEnabled`), `data/repo/CatalogRepository.kt` (NSFW-aware provider search),
`data/local/SettingsStore.kt` (`showNsfwExtensions`), `di/AppContainer.kt`,
`ui/vm/ViewModels.kt`, `ui/screens/addons/AddonManagerScreen.kt`,
`test/…/RepoIndexTest.kt` (new, 8 cases), `test/…/CloudStreamSupportTest.kt` (seed policy test).

### Verification (2026-10-23)
`:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **309/309** (301 + 8 new) — the suite went
red on the two tests that encoded the *old* policy (`default repos must stay empty`, the legacy
index shape) and both were updated to the new contract, not skipped. Live parse of the real
indexes through the shipped Kotlin parsers (temporary probe, then deleted): Keiyoushi **1425
extensions, 389 NSFW, 1425/1425 absolute APK URLs**; Yuzono **250 entries, 72 NSFW, 250/250
resolved to `…/repo/apk/…`**.

Not verified: anything requiring execution of an Aniyomi/Mihon extension, and the Add-on Manager
UI on a device.

---

## 2az. v1.10.1 patch fixes — ADB test-report P0/P1 (2026-10-23)

**Topic:** close the two P0s and the P1 in `TEST_REPORT.md` (the v1.10.0 pass on a OnePlus
CPH2487 / Android 16). All three are implemented and unit-verified; **no signed release was built,
tagged or pushed** this session.

### 1. Player `MediaRouteButton` exception (P0)
- **Symptom:** `IllegalArgumentException: background can not be translucent: #0` at
  `ColorUtils.calculateContrast`, thrown from `MediaRouterThemeHelper.createThemedButtonContext`
  on every cast-button construction; the player then stopped recomposing (`uiautomator` saw 0
  nodes) until a relaunch or rotation.
- **Actual mechanism:** the report blamed the exception escaping into Compose's frame dispatch,
  but the `AndroidView` factory already wrapped construction in `runCatching` and returned a
  hidden `View` on failure — nothing propagated. The real cause is the *context*: the player runs
  on `Theme.Moovio.Player` (a platform `android:Theme.Material` descendant, no AppCompat
  attributes) over a transparent Compose window, so `android:colorBackground` resolved to `#0`
  and `calculateContrast` threw before the button could exist. The button was therefore never
  created (only the fallback icon showed), and the frame that caught it was left half-built.
- **Fix:** new `ThemeOverlay.Moovio.CastButton` (`Theme.AppCompat.NoActionBar`) with an **opaque**
  `android:colorBackground` / `android:windowBackground` (`@android:color/black`) plus
  `colorPrimary` / `colorPrimaryDark` / `colorAccent`; `PlayerActivity` now builds the button with
  `ContextThemeWrapper(ctx, R.style.ThemeOverlay_Moovio_CastButton)` and keeps the `runCatching`
  fallback (a failed init degrades to the cast-settings icon instead of a dead frame).
- Verified structurally: `aapt2 dump resources` on the built APK shows the style with both
  background attributes bound to `@android:color/black` (opaque). Hardware confirmation is §7
  item 33.

### 2. NSFW Manga chapters permanently empty (P0)
- **Symptom:** NSFW Hub → NSFW Manga listed series (MangaDex search worked) but every chapter list
  was empty; HTTP 200 with an empty `data` array.
- **Root cause (measured live, 2026-10-23):** MangaDex's `/manga/{id}/feed` returns **0 chapters
  for an adult series when the request asks for `contentRating[]=safe`**, and — the part that is
  easy to get wrong — **also 0 when no `contentRating[]` is supplied at all** (an unfiltered feed
  behaves like the SFW set). The same feed with `suggestive` + `erotica` + `pornographic` returns
  the chapters. `MangaDexClient.chapters()` picked its rating set from
  `item.type == MediaType.NSFW_MANGA`, so any adult item whose type was not `NSFW_MANGA` (older
  library/history entries, list items rebuilt from a metadata fallback) was queried with the safe
  set and could never produce chapters.
- **Fix:** new pure `MangaDexQuery` object owns the rating rules — `NSFW` (catalogue: erotica +
  pornographic), `NSFW_FEED` (feed: suggestive + erotica + pornographic, **never** `safe`),
  `ratings()` / `feedRatings()` / `ratingParams()` and `feedAttempts()`. `chapters()` now tries the
  attempt list and stops at the first non-empty answer; a SFW title that comes back empty is
  retried **once** with the NSFW set (recovers an adult series that lost its NSFW type), and there
  is deliberately no "retry without the filter" path, because that request is equivalent to the
  safe query that caused the bug.

### 3. Torrent preparation hung forever (P1)
- **Symptom:** with trackers unreachable, "Preparing stream…" never resolved and there was no
  timeout or cancel.
- **Fix:** new `TorrentWatchdog` (pure policy: `TIMEOUT_MS = 20_000`, `expired()`,
  `message()`). `TorrentStreamer.stream()` arms a main-thread watchdog when preparation starts;
  on expiry it stops the torrent, clears the pending callback and reports a retryable failure. The
  watchdog is re-armed (not fired) if the handler runs early, and is cancelled by `onStreamReady`
  / `onStreamError` / `startStream` failure / `stop()`. `DetailScreen` replaces the old
  fire-and-forget error path (toast + auto-launch of an external torrent app) with an
  `AlertDialog`: the failure reason, **Retry** (re-streams the same source), **Torrent app** (only
  when the source has a magnet) and **Cancel**.

### Files
`app/src/main/res/values/themes.xml` (cast overlay), `ui/player/PlayerActivity.kt`,
`data/remote/MangaDexQuery.kt` (new), `data/remote/MangaDexClient.kt`,
`data/remote/TorrentWatchdog.kt` (new), `data/remote/TorrentStreamer.kt`,
`ui/screens/detail/DetailScreen.kt`, `app/build.gradle.kts` (`versionCode` 20 / `1.10.1`),
`app/src/test/java/com/moovio/app/Patch1101Test.kt` (new, 9 cases).

### Verification (2026-10-23)
`:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **301/301** (292 existing + 9 new) ·
`:app:assembleDebug` ✓ (`com.moovio.app.debug`, `versionCode` 20, `versionName` 1.10.1) ·
`aapt2 dump resources` confirms the cast style's opaque background attributes.

### Release (2026-10-23, v1.10.1)
- `versionCode` 20 / `versionName` 1.10.1 (`app/build.gradle.kts`); `:app:assembleRelease` ✓ signed
  with the rotated key (`2EDB949B…351379`, DN `CN=Moovio…`).
- APK 43,947,037 bytes, SHA-256 `8C474F671DAF35A682B3683B7209DA4083B468CF0EBB9631474C089BCBA2A79A`
  (`com.moovio.app`, `versionCode` 20, `versionName` 1.10.1, `targetSdk 34`); copied to
  `dist/app-release.apk` + `dist/Moovio.apk` (both gitignored — release assets only).
- Committed `14e11fc` (code + roadmap + `TEST_REPORT.md`) with the tag `v1.10.1`; `main` and the
  tag pushed to `origin`.
- Website: hero/nav/footer `v1.10.0` → `v1.10.1`, hero + stats `292` → `301`; `npm run build` ✓
  (8/8 static pages); committed `82d50bf` and pushed (Vercel auto-redeploys).
- **GitHub Release `v1.10.1` published** with `app-release.apk` + `Moovio.apk`; verified
  `releases/latest/download/app-release.apk` → HTTP 200 / 43,947,037 bytes and both asset digests
  equal the local `8C474F67…BA2A79A`.
- Upgrade path: installs in place over v1.10.0 (same package + key); still a clean install over
  v1.9.0 and older (package id and key changed in v1.10.0).

---

## 2ax. Security / stability remediation pipeline (2026-10-22)

**Topic:** Phase 1 (security & signing), Phase 2 (stability & extension fixes) and Phase 3 (UI &
performance) of the remediation pipeline, on top of the Moovio rename (§2aw).

### Phase 1 — Security & signing
- **Key rotation.** The release keystore was committed in the first commit and is still recoverable
  from the pack, so anyone with repo read access could mint APKs Android accepts as in-place
  updates. A fresh 4096-bit RSA key (`alias moovio`) now lives **outside** the repository at
  `~/.moovio/moovio-release.jks`; the old key and `keystore.properties` are deleted.
  `build.gradle.kts` now reads the signing material **only** from the untracked `local.properties`
  (`moovio.storeFile` / `moovio.storePassword` / `moovio.keyAlias` / `moovio.keyPassword`) or the
  `MOOVIO_*` environment variables, and a release build without them fails fast with an actionable
  message instead of AGP's cryptic one.
- **OAuth state validation.** `moovio://oauth` uses a custom scheme, so any app on the device could
  fire it and have a foreign authorization code written into the user's tracker list. The connect
  flow now stores a 128-bit random nonce, carries it in `state` (`trakt:<nonce>`), and the callback
  compares it in constant time (`MessageDigest.isEqual`) and consumes it once — a redirect without a
  matching nonce is rejected.
- **Updater signature check.** `installApk` verifies the downloaded archive before handing it to the
  system installer: same package name, signed by a key the installed app trusts (current signer +
  signing lineage). A foreign-signed APK is refused with a visible "Update rejected" message.
  `ApkSignature.kt` holds the pure fingerprint/trust logic (unit-tested).

### Phase 2 — Stability & extensions
- **CloudStream shims repaired.** `CloudStreamApp.initialize` now also publishes the app context to
  `com.lagradost.api` (the library's WebView resolver read a null context before) and is called
  from `MoovioApp.onCreate` instead of only when a plugin is installed; `CommonActivity` is attached
  to `MainActivity`/`PlayerActivity` so provider toasts are no longer silent no-ops (and its
  `displayMetrics` now come from the Activity's window); the provider-settings shim now uses a
  Jackson mapper configured exactly like the library's own — the old Gson codec dropped null
  fields, which made the providers' inlined Jackson read fail and silently lose their settings.
- **Foreground-service rules.** "Auto-delete watched downloads" called
  `DownloadService.sendRemoveDownload(..., foreground = true)` from the background, which Android 12+
  refuses (`ForegroundServiceStartNotAllowedException`, swallowed) — it now goes through
  `DownloadManager.removeDownload` and releases the cached bytes directly.
- **Manga reading position restored.** The saved page was loaded *after* the pager had captured its
  initial index, so every chapter opened on page 1. The position is now loaded before the reader is
  composed and applied explicitly to the pager/list state.
- **Stable list keys.** `HomeOrderScreen` keyed its rows by title, but two add-ons can contribute a
  row with the same name — duplicate keys crash a `LazyColumn`. Titles are de-duplicated and the key
  is now `index::title`.
- **Player screen protection.** `FLAG_SECURE` was only applied in `MainActivity`, leaving the
  player (where the NSFW content plays) recordable. The player now honours the setting.

### Phase 3 — UI & performance
- Poster palette extraction (CPU-bound quantization) moved to `Dispatchers.Default`.
- The Sources dashboard no longer parses `extensions.json` on the main thread during composition and
  re-reads it on resume; a failed probe can no longer leave a row stuck on "Testing…".
- `DetailScreen` collected the cold history flow **per episode row** (one DataStore subscription and
  one full decode per row). It is now collected once at screen level and indexed by video id.

### Verification
`:app:compileDebugKotlin` and `:app:testDebugUnitTest` are green — **292 tests** (281 existing + 11
new for the OAuth nonce/state, the signature trust rules and the provider-settings codec).
`assembleDebug` builds a working APK.

### Git history
The old key blob was purged with `git filter-repo --path keystore --invert-paths`: `main` and all 16
release tags were rewritten and force-pushed (branch with `--force-with-lease` pinned to the old
head `a1b3f4a`), the reflog was expired and the object store re-packed — the blob
`681a2046…` is no longer a valid object locally and no reachable ref on GitHub points at the old
commits. GitHub still serves the orphaned blob by SHA from its own object store until its garbage
collection runs, so the key was rotated as well: the published one can no longer sign an update
that Android accepts.

**Consequence:** because the signing key changed, the next release cannot be installed over the
currently published one — existing users have to reinstall (and the app id changed to
`com.moovio.app` in §2aw anyway, so it installs as a new app regardless).

---

## 2aw. Full rename to Moovio (2026-10-22)

**Topic:** the product name is **Moovio** everywhere — code, package, docs and site. Previously the
app shipped as *NovaStream* (v1.0–v1.6) and *NexusStream* (v1.6–v1.9) with the package
`com.novastream.app`, and the three names had drifted apart (launcher label "Moovio", README
"NexusStream", project "NovaStream", APK assets named all three).

**What changed**
- **Package / identity.** `namespace` and `applicationId` → `com.moovio.app`; source tree moved to
  `app/src/main/java/com/moovio/app/` (and `app/src/test/java/com/moovio/app/`). 1118 references
  updated across 193 files. The CloudStream compatibility shim stays in `com.lagradost.cloudstream3`
  because provider dexes resolve those classes by name.
- **Brand tokens.** `NovaApp`→`MoovioApp`, `NovaStreamTheme`→`MoovioTheme`, `NovaColors`→
  `MoovioColors`, `LocalNovaColors`→`LocalMoovioColors`, `NovaTypography`→`MoovioTypography`,
  `NovaSwitch`→`MoovioSwitch`, `NovaDownloadService`→`MoovioDownloadService`,
  `NovaCastOptionsProvider`→`MoovioCastOptionsProvider`, `NexusExtension`→`MoovioExtension`,
  `novaViewModel`→`moovioViewModel`, `nova_settings`→`moovio_settings`, `Ns*` colour tokens→
  `Moovio*`, `ns_*` XML colours→`moovio_*`. Files renamed to match.
- **OAuth.** Redirect scheme `novastream://oauth` → `moovio://oauth` (manifest + `TrackerOAuth`).
- **Build/docs/site.** `rootProject.name` = `Moovio`; signing env vars `NOVASTREAM_*` →
  `MOOVIO_*`; `keystore/novastream.jks` → `keystore/moovio.jks`; README rewritten (and its stale
  version/test-count/TMDB-key claims corrected); website package renamed to `moovio-website`; the
  OpenGraph wordmark now reads "Moovio" (it still rendered "NexusStream").

**Consequences to close out before the next release**
1. **The app installs as a new package.** Existing `com.novastream.app` installs are *not* updated
   in place by the in-app updater — the APK has a different `applicationId`. Users must uninstall
   the old app (losing their library/settings, which live in the old app's sandbox) or ship a
   "keep `com.novastream.app`" build instead.
2. **Release signing key was replaced (§2ax).** `keystore/novastream.jks` was committed in the first
   commit and removed later, but the blob was recoverable from the pack — anyone with repo read
   access could mint APKs Android accepts as in-place updates. The key has been rotated (new
   4096-bit RSA key outside the repo, `alias moovio`) and the build now reads the material only
   from `local.properties` / the `MOOVIO_*` env vars. The blob was purged from the history as well
   (§2ax, `git filter-repo` + force-push of `main` and all tags). Because the key changed, the next
   release cannot be installed over the published one — existing users reinstall.
3. **GitHub repo still `speckle-hub/nexusstream`.** `UpdateChecker.DEFAULT_REPO` and the site's
   `GITHUB_URL`/`APK_URL` point at it; rename the repo (or update those constants) so the in-app
   updater and the download CTA keep working.
4. **Verify on a machine with the SDK.** `local.properties` currently points `sdk.dir` at a
   non-existent path, so `:app:compileDebugKotlin` and `:app:testDebugUnitTest` could not be run
   here. The rename was verified structurally instead: package/directory parity (0 mismatches),
   every `com.moovio.*` import resolving to a declared symbol, manifest class/theme references
   resolving, and no `com.novastream` / `NovaStream` token left in `app/src`.

---

## 2av. Phase 38 — Built-in SFW sources + auto-play best source (2026-10-15)

**Topic:** regular Movies/TV/Anime play with zero add-ons installed (14 modular
built-in scrapers in the `data/adult/` + `data/hentai/` style), and Play
auto-starting the best available stream instead of forcing a manual pick.

Status: **implemented and verified (statically + unit).** No device/`adb` in this
environment, so per-site reachability + real playback still need the device pass
below. Every parser is pinned by fixture tests (no network); every network hop is
bounded with per-source isolation, so one dead site never breaks its section.
Files and verification at the end.

### 1. Contracts + shared engine (`data/sfw/`)
- **`SfwModels.kt`**: `SfwTitle` (page URL doubles as item id, like `AdultVideo`),
  `SfwEpisode` (`sfw:` video ids, like `HentaiEpisode`), `SfwAnimeEpisode`
  (`sfwa:` ids), `SfwRow`, `SfwDetail`, `SfwAnimeTitle`, `SfwSourceException`.
- **`SfwSource.kt`**: `SfwSource` (movies/TV: `home`/`search`/`detail`/
  `streamsForEpisode`/`streamsForTmdb`, with list-defaults so resolver-only
  sources like Vidsrc implement just the TMDB hook) and `SfwAnimeSource`
  (anime: `search`/`streams` + a default series-grouped `searchTitles` that
  API sources override). Plus `sfwSeasonEpisode()` (`tt:2:5`, `S02E05`, trailing
  anime numbers), the strict `sfwSameTitle` gate (shared adult matcher), and the
  `sfwRows` concurrent-row helper.
- **`SfwEmbeds.kt`**: page → direct media → `"file"`/`"sources"` blobs →
  iframes → nested frame chain (`resolveChain`), a pure Dean-Edwards
  `jsUnpackAll` for packed player scripts, `labeledSources`, `normalizeLabel`
  (`HD`→`720p`, CAM kept verbatim), and the generic `parseTitleCards` index
  parser. Reuses `AdultHttp` (browser headers, page cache, challenge → typed
  error) exactly like the hentai sources do.

### 2. Movies & TV (+8 — search + rows; TMDB stays the metadata authority)
- **Vidsrc** (`vidsrc`, resolver-only) — TMDB-id-keyed embeds
  (`/embed/movie/{id}`, `/embed/tv/{id}/{s}/{e}`) across 4 mirrors, first
  playable mirror wins; no title matching needed.
- **YTS** (`yts`, movies-only) — public JSON API for trending/popular/latest
  rows + search; torrents resolve to `infoHash` streams with real quality +
  seeder counts, so the existing torrent path and seeder ranking work unchanged.
- **FlixHQ** (`flixhq`) — search cards, watch pages, and the documented ajax
  ladder (`season/list` → `season/episodes` → `episode/servers` → `server/{id}`
  → `{link}`), each hop fixture-pinned, mirrors walked with isolation.
- **MultiMovies** (`multimovies`, Sora-style), **Rive** (`rive`), **Flixer**
  (`flixer`), **PopcornMovies** (`popcornmovies`) — search-first indexes with
  title pages (synopsis + episode picker for series) resolving through the
  shared embed chain, each with mirror fallbacks.
- **SuperStream** (`superstream`, StreamPlay-style) — JSON app API
  (`Search`/`Detail`/`EpisodeLinks` modules) returning title records, seasons
  and `{file, label}` link lists; no markup scraping.

### 3. Anime (+6 — AniList-led, built-ins resolve episodes/streams only)
- **AnimePahe** (`animepahe`) — JSON API (`?m=search`, `?m=release` paging) with
  native anime-level `searchTitles`; episodes resolve via pahe_win windows into
  kwik embeds (packed scripts unpacked).
- **Gogo** (`gogo`, Anichi-style) — ajax keyword search, `/category/` episode
  lists, vidstream-style embeds.
- **KickassAnime** (`kickassanime`), **YugenAnime** (`yugenanime`, successor
  mirrors — the original `.tv` shut down Dec 2024), **AnimeKai** (`animekai`,
  ajax search + `/ajax/episode/list` + `/ajax/server/{id}` ladder),
  **Miruro** (`miruro`, search API + `/series/` pages) — series search,
  episode-list expansion, shared embed resolution.

### 4. Repositories (isolation + progressive + cached, NSFW-pattern)
- **`SfwRepository`**: progressive `homeRows(section)` (per-source TTL cache,
  5 s caps, mirror quarantine), ranked `searchProgressive` (relevance filter
  with unfiltered fallback, one retry on transient failure), cached `detail`,
  `streams`/`streamsForEpisode` with bounded dead-link pre-flight
  (`AdultHttp.verifyPlayable`, so Play lights up on live links only), and
  `streamsForTitle` (TMDB resolvers first when the id is known, else
  search → strict same-title + year match → episodes/S-E pick → resolve).
- **`SfwAnimeRepository`**: `episodes(title)` with query variants + strict
  series matching (the §2s correct-title guarantee), `streams(title, ep)` /
  `streams(episode)` with pre-flight, and series-level `searchTitles` for rows.

### 5. Wiring (TMDB/AniList metadata untouched)
- **`StreamRepository`**: built-in title/episode pages resolve directly
  (`sfw:`/`sfwa:` branches, like the adult branch); regular
  Movie/Series/Anime items now race Stremio + CloudStream + SFW built-ins
  concurrently and re-rank the merge (preferred hosts → quality → direct →
  seeders). Series embed-as-movie is guarded: TMDB resolvers only run with an
  exact id (+ S/E for series).
- **`CatalogRepository`**: section search + global search merge built-in titles
  (bounded, de-duplicated downstream); Browse Movie/Series/Anime tabs gain
  built-in Popular/Latest/Trending rows (Home feed intentionally unchanged).
- **`MetadataRepository`**: built-in titles get `sfwMeta` (page meta +
  episodes + related) with a `fallbackDetail` guarantee; anime titles get the
  title-preserving `sfwAnimeFallback` (mirrors `hentaiFallback`).
- **Ranking + auto-play**: `StreamRanking.rank` gained a default-off
  `directFirst` tier (existing tests pin the default order unchanged) plus pure
  `isDirectMediaUrl` / `bestPlayable` (playable-only filter → preferred →
  quality → direct → seeders); `preferredStreamHosts` now lists Torrentio +
  the known-good built-ins (ordering only — nothing is installed); the detail
  Play CTA and season bulk-download both use `bestPlayable`, so tapping Play
  starts the best working stream immediately while the sheet stays one tap away.

### Files
`data/sfw/SfwModels.kt`, `SfwSource.kt`, `SfwEmbeds.kt`, `VidsrcSource.kt`,
`YtsSource.kt`, `FlixhqSource.kt`, `MultiMoviesSource.kt`, `RiveSource.kt`,
`FlixerSource.kt`, `PopcornMoviesSource.kt`, `SuperStreamSource.kt`,
`AnimePaheSource.kt`, `GogoSource.kt`, `KickassAnimeSource.kt`,
`YugenAnimeSource.kt`, `AnimeKaiSource.kt`, `MiruroSource.kt`,
`SfwSources.kt` (8), `SfwAnimeSources.kt` (6), `SfwRepository.kt`,
`SfwAnimeRepository.kt`, `data/repo/StreamRepository.kt` (SFW race + merge),
`data/repo/StreamRanking.kt` (`directFirst`, `bestPlayable`, `isDirectMediaUrl`),
`data/repo/CatalogRepository.kt` (search + rows), `data/repo/MetadataRepository.kt`
(`sfwMeta`, fallbacks), `data/local/AddonStore.kt` (preferred hosts),
`ui/screens/detail/DetailScreen.kt` (auto-play pick), `app/src/test/.../SfwSourcesTest.kt` (+34).

### Verification (2026-10-15)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings;
  two in-session fixes: `Regex.findAll` on the packer pattern, one fixture URL missing
  the `/episode/` contract).
- `:app:testDebugUnitTest --rerun-tasks` → **281/281 tests, 0 failures, 0 errors** (+34 new;
  per-source fixtures, registry uniqueness, unpacker, S/E parsing, id round-trips,
  auto-play ordering incl. preferred-beats-4K, quality-beats-direct, direct-wins-ties).
- Not pushed; no APK rebuilt this session. Device pass still needed: per-site home rows,
  search relevance, detail + real playback per source (especially Vidsrc mirror hops,
  FlixHQ ajax drift, kwik unpacking, YTS torrent streaming, Rive/SuperStream/Flixer/
  PopcornMovies reachability, Yugen successor mirrors), and auto-play first-paint on a
  cold title with no add-ons installed.

## 2au. Phase 37 — Moovio rebrand & Anime4K GPU shader engine (2026-10-15)

**Topic:** dual milestone — (1) rebrand the display name NexusStream → **Moovio**,
(2) run real-time Anime4K GLSL enhancement passes inside the Media3 frame-processor
loop with presets, GPU tiers, persisted prefs and an in-player sheet.

Status: **implemented and verified (statically + unit).** No device/`adb` in this
environment, so GPU feel/frame-drop behaviour still needs the device pass below. Every
new path is unit-pinned where it is pure-JVM; the GL hook is total (any GPU rejection
logs and leaves playback untouched). Files and verification at the end.

### 1. Rebrand: NexusStream → Moovio (display name only)
- User-visible strings now read **Moovio**: `strings.xml` `app_name`, Home top bar,
  app-lock gate ("Moovio is locked" + biometric prompt), Settings About row + dialog
  title, download fallback filename, backup error, HTTP `UA` (`… Moovio/1.0`), and the
  updater's staged APK (`moovio-update.apk`).
- **Deliberately unchanged for compat** (same doctrine as the earlier NexusStream
  rename): package/`applicationId` `com.novastream.app` (in-place updates), theme and
  Nova-prefixed class names (technical identifiers, not branding), the `novastream://`
  OAuth deep-link scheme (issued redirect URIs), `UpdateChecker.DEFAULT_REPO`
  (`speckle-hub/nexusstream`) and the `nova_settings` DataStore name.

### 2. Anime4K engine (`ui/player/Anime4KEngine.kt`, pure JVM)
- **Presets:** `OFF` (frame-processor bypass, zero cost), `SHARPEN` (unsharp-mask edge
  restoration), `DEBLUR` (difference-of-Gaussians soft restoration), `DENOISE`
  (edge-aware 3×3 bilateral-ish cleanup). Total `parseAnime4KPreset()` falls back to Off.
- **GPU tiers:** `MID` (lightweight: fast edge recovery + debanding suffix) vs `HIGH`
  (full HQ spatial-scaling suffix). Total `parseAnime4KGpuTier()` falls back to Mid.
- `anime4KPasses()` names the ordered pass list per preset × tier (empty when Off),
  `anime4KEstimatedCost()` buckets 0/1/2, `isAnime4KHeavy()` is true only for live+HIGH,
  `anime4KEffectLabel()` renders "Off" / "Sharpen · Mid-range GPU".
- `anime4KFragmentShader()` emits the combined GLSL program bound to the
  frame-processor contract (`uTexture` sampler, `vTexCoord` varying, `uTexelSize`).

### 3. GPU hook (`ui/player/Anime4KGl.kt`, new `media3-effect:1.3.1` dep)
- `Anime4KGlEffect : GlEffect` carries the config into the loop (`isNoOp` when Off);
  `Anime4KShaderProgram : BaseGlShaderProgram` draws a full-screen quad through
  `GlProgram(vertex, anime4KFragmentShader(config))`, setting `uTexture`/`uTexelSize`
  per frame (API verified against the 1.3.1 sources: `GlEffect.toGlShaderProgram`,
  `GlProgram(String, String)`, `Size(int, int)`).
- `Anime4KEffectApplier.apply(player, config)` swaps `player.setVideoEffects(...)`
  **without rebuilding the player** (Off clears to `emptyList()`); total via
  `runCatching` + log, so a GPU rejection degrades to normal playback, never a crash.

### 4. Persistence + in-player sheet
- `SettingsStore` gained `ANIME4K_PRESET`/`ANIME4K_TIER` keys + flows (defaults
  off/mid) with validated setters (unknown keys clamp to off/mid) — the choice
  survives across playback sessions.
- `TrackSheet` (Quality & Audio) gained an **Anime4K Enhancement** section: live
  status label, one `TrackRow` per preset, one per GPU tier, and an HQ warning when a
  heavy config is active. `PlayerScreen` collects the flows and re-applies via a
  `LaunchedEffect(preset, tier)`, so switching is live mid-playback.

### Files
`res/values/strings.xml` (`app_name`), `res/values/colors.xml` + `drawable/ic_launcher_foreground.xml`
(brand comments), `data/remote/Http.kt` (UA), `data/download/VideoDownloadManager.kt`
(fallback name), `data/integrations/BackupManager.kt` (error text),
`data/integrations/UpdateChecker.kt` (staged APK name), `ui/MainActivity.kt` (lock strings),
`ui/screens/home/HomeScreen.kt` (top bar), `ui/screens/settings/SettingsScreen.kt` (About),
`ui/player/Anime4KEngine.kt` (new policy), `ui/player/Anime4KGl.kt` (new GL hook),
`ui/player/PlayerActivity.kt` (sheet section + live apply), `data/local/SettingsStore.kt`
(preset/tier keys), `app/build.gradle.kts` (`media3-effect:1.3.1`),
`app/src/test/.../Phase37Anime4KTest.kt` (+10).

### Verification (2026-10-15)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings;
  one in-session fix: sheet callbacks capture `LocalContainer.current.settings` in
  composition instead of reading `settings` with no receiver).
- `:app:testDebugUnitTest --rerun-tasks` → **247/247 tests, 0 failures, 0 errors** (+10 new;
  parse fallbacks, Off emptiness, per-preset pass distinctness, Lite/HQ suffixes, heavy
  gating, cost ordering, labels, GLSL bindings).
- Not pushed; no APK rebuilt this session. Device pass still needed: sheet feel, effect
  visible on a real GPU per preset, HQ-tier frame drops on mid-range hardware, Off
  bypass, and preference survival across sessions.

## 2at. Phase 36 — Premium UI/UX Overhaul (2026-10-14)

**Topic:** elevate the app toward a flagship premium feel across home, the NSFW Hub,
the player and playback performance.

Status: **implemented and verified (statically + unit).** Audit first: most of the
request already existed and was verified rather than rebuilt — the auto-scrolling
hero carousel with palette-tinted glow/scrims and Play/Read CTA (Phase 3/4a/6), the
Material You poster-palette extraction (Phase 4a), the floating glass pill nav
(Phase 18/20), the NSFW PIN/biometric vault (NsfwLock + Content Restrictions),
the in-player subtitle style sheet + ±10 s sync offset (Phase 29), PiP with
auto-enter (Phase 11), and reliability-first stream ranking + resolution badges
(Phase 16/9). The genuine deltas below are the only new code. No device/`adb` in
this environment, so feel/timing items need the device pass. Files and verification
at the end.

### 1. Cinematic home hero & pill nav
- **Hero Watchlist quick action (new).** `HomeViewModel` gained an SFW-only
  `favorites` flow + `toggleFavorite()` (adult stays hub-side); each `HeroSlide`
  now pairs its Play/Read pill with an outlined **Watchlist/Saved** pill (heart
  glyph, haptic tick) that toggles the library favourite in place.
- **Tab-switch haptics (new).** `FloatingPillNavBar` fires one tactile tick per
  tab change (skipped when re-tapping the active tab).

### 2. Real 18+ source-filter chips
- **New pure `ui/screens/nsfw/NsfwFilters.kt`** (`applySourceFilter()`): a selected
  source id keeps only that source's rails and results (`null` = "All"); built-in
  rows/results match on `addonId`, Stremio add-on rows hide under a built-in
  filter instead of leaking through.
- `NsfwSection` gained optional `sourceOptions` / `selectedSource` /
  `onSelectSource` (defaults keep anime/manga pixel-identical); the Real 18+ tab
  renders an **All + per-source** `TagChip` row above the grid with a
  source-aware empty state.

### 3. Player subtitles & sync
- **Outline color (new).** `SubtitleStyle.outlineKey` (default black) flows into
  `CaptionStyleCompat`'s edge color instead of the hardcoded black; new
  `SettingsStore` `SUB_OUTLINE` key/flow/setter surfaces it in Settings >
  Subtitles (black/white/yellow/cyan/green) and in the in-player style sheet —
  both stay in sync through `updateSubtitleStyle()`.
- **100 ms sync steps.** The sync slider quantizes to 100 ms (`steps = 199`)
  instead of 500 ms; `clampSubtitleSyncMs` already represents any 100 ms value.
- **Audio delay: deliberately not built.** Media3 ExoPlayer exposes no
  audio-track delay API, and emulating one (transcode/ffmpeg) is out of scope —
  shipping a slider that does nothing would be fake UI. The existing speed,
  boost and normalize controls remain the audio side.

### 4. Next-episode pre-fetch
- **New pure `shouldPrefetchNextEpisode()`** (`PlaybackMath.kt`,
  `NEXT_EPISODE_PREFETCH_MS` 120 s): fires inside the final two minutes when a
  next episode exists and hasn't been pre-fetched for this episode.
- `PlayerActivity`'s progress watcher calls it per tick and background-resolves
  the next episode once (`prefetchedIndex`, reset on every episode switch). The
  result is discarded on purpose — the resolve warms `StreamRepository`'s ranked
  cache — so the 15 s autoplay countdown transitions with zero resolve delay.
  Silent and best-effort: a failed pre-fetch just resolves on demand later.

### Files
`ui/vm/ViewModels.kt` (hero favorites), `ui/screens/home/HomeScreen.kt` (Watchlist CTA),
`ui/nav/FloatingPillNavBar.kt` (haptics), `ui/screens/nsfw/NsfwFilters.kt` (new),
`ui/screens/nsfw/NsfwHubScreen.kt` (chips), `data/local/SettingsStore.kt` (`SUB_OUTLINE`),
`ui/player/PlayerActivity.kt` (outline, 100 ms steps, pre-fetch),
`ui/player/PlaybackMath.kt` (policy), `ui/screens/settings/SettingsScreen.kt` (outline row),
`app/src/test/.../Phase36PremiumTest.kt` (+7).

### Verification (2026-10-14)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings;
  one in-session fix: a duplicated `maybeAutoplay()` line).
- `:app:testDebugUnitTest --rerun-tasks` → **237/237 tests, 0 failures, 0 errors** (+7 new;
  chip filtering incl. unknown-id, pre-fetch window edges, 100 ms clamp values).
- Not pushed; no APK rebuilt this session. Device pass still needed: hero toggle feel,
  chip narrowing on rails + search, outline rendering on native captions, sync slider
  granularity, and the pre-fetch → autoplay handoff on a real series.

## 2as. Source expansion — 13 new built-in sources (2026-10-14)

**Topic:** broaden built-in NSFW Anime + Real 18+ coverage again, following the
established modular pattern (`HentaiSource` / `AdultSource` + registry +
per-source error isolation, so one dead site never breaks its tab).

Status: **implemented and verified (statically + unit).** No device/`adb` in this environment,
so live reachability + ExoPlayer playback per new site still need the device pass below.
Every parser is pinned by fixture tests (no network). Files and verification at the end.

### 1. NSFW Anime (+4 — search + streams; AniList/Jikan metadata untouched)
- **HenVids** (`henvids`, `HenVidsSource.kt`) — series-based tube: episode pages
  `/hentai/{slug}/` (no series expansion needed), `/search/{q}/` + query-param
  fallbacks, `/hentai/` card filter excluding tag/brand/series shelves.
- **Ohentai** (`ohentai`, `OhentaiSource.kt`) — classic PHP tube: `detail.php?vid=`
  cards, `tagsearch.php?tag=` as the confirmed search endpoint, site-relative
  posters absolutised, `resolvedStreams` for playback.
- **Hentai.tv** (`hentaitv`, `HentaiTvSource.kt`) — clean JSON API
  (`GET /api/search?q=` → `videos[]` with slug/title/ep/cover/embedUrl): results
  are already episode-level; streams resolve from the `embedUrl` (nhplayer page)
  or the `/watch/{slug}` fallbacks. First JSON-API hentai source (Gson, no
  markup scraping for discovery).
- **XAnimu** (`xanimu`, `XAnimuSource.kt`) — high-volume Rule34 tube: confirmed
  `/?s=` search, `/{numericId}-{slug}/` video permalinks, numeric-taxonomy-root
  exclusions (`/tv-shows/`, `/games/`, `/movies/`, `/comics/`, tags, actors).

### 2. Real 18+ general (+4 — full `AdultSource`: home/search/categories/detail)
- **PornTrex** (`porntrex`, `PornTrexSource.kt`) — KVS tube: `/latest-updates/` +
  `/top-rated/` + `/most-popular/` rows, `/search/{q}/` search, live-or-static
  `/categories/` list, `/video/{id}` cards, `TubeMedia` streams.
- **SxyPrn** (`sxyprn`, `SxyPrnSource.kt`) — community aggregator: front-page wall
  + top-viewed/top-pop charts, `/post/{id}.html` cards, tag-shelf search; posts
  link to third-party file hosts, so direct streams are best-effort (`TubeMedia`
  + one iframe hop) and may legitimately be empty per entry.
- **FullPorner** (`fullporner`, `FullPornerSource.kt`) — full-length tube:
  `/watch/{hash}` cards, featured + `/home/{n}/` rows, live-or-static categories,
  `TubeMedia` streams.
- **TXXX** (`txxx`, `TxxxSource.kt`) — Aylo-family tube mirroring `YouPornSource`:
  `/best/` + `/newest/` + `/most_viewed/` rows, `/?search=` search,
  `media_definitions` API streams with `TubeMedia` fallback.

### 3. Real 18+ JAV (+5 — full `AdultSource`)
- **JavMost** (`javmost`, `JavMostSource.kt`, bot-walled from here) — WP-style with
  `/?s=` + `/search/` fallbacks and taxonomy-aware `parseList`.
- **KissJAV** (`kissjav`, `KissJavSource.kt`, live host `kissjav.li`) — KVS tube:
  `/latest-updates/` + most-viewed + `/categories/jav/` + `/categories/fc2ppv/`
  rows, `/search/` search, live-or-static categories.
- **Javtiful** (`javtiful`, `JavtifulSource.kt`) — code-keyed catalogue:
  `/video/{id}/{slug}` cards, `?sort=popular_week|most_viewed` + uncensored +
  reducing-mosaic rows, `/category/{slug}` list, `/search?q=` + `/videos?search=`
  fallbacks.
- **OpJav** (`opjav`, `OpJavSource.kt`, 403 from here) — WP-style, same shape as
  JavMost with its own static shelves.
- **7MMTV** (`7mmtv`, `SevenMmtvSource.kt`, rotating hosts) — primary `7mmtv.sx`
  with the `7mmtv.tv` mirror as fetch fallback, WP-style search + cards.

### Kept guarantees
- Progressive loading + per-source error isolation unchanged: the repositories
  iterate the registries, so new sources inherit timeouts, quarantine, caches and
  notices automatically. Registries: adult 15 → **24**, hentai 9 → **13**.
- NSFW Anime discovery stays AniList-led (built-ins resolve episodes/streams only).

### Files
`data/hentai/HenVidsSource.kt`, `data/hentai/OhentaiSource.kt`,
`data/hentai/HentaiTvSource.kt`, `data/hentai/XAnimuSource.kt`,
`data/hentai/HentaiSources.kt` (registry 9 → 13),
`data/adult/PornTrexSource.kt`, `data/adult/SxyPrnSource.kt`,
`data/adult/FullPornerSource.kt`, `data/adult/TxxxSource.kt`,
`data/adult/JavMostSource.kt`, `data/adult/KissJavSource.kt`,
`data/adult/JavtifulSource.kt`, `data/adult/OpJavSource.kt`,
`data/adult/SevenMmtvSource.kt`, `data/adult/AdultSources.kt` (registry 15 → 24),
`app/src/test/.../MoreSourcesTest.kt` (+14).

### Verification (2026-10-14)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **230/230 tests, 0 failures, 0 errors** (+14 new;
  one in-session fix: HenVids fixture now uses an "Episode N" title so the shared
  `seriesOf`/`episodeOf` contract holds).
- Not pushed; no APK rebuilt this session. Device pass still needed: per-site home rows,
  search relevance, detail + real playback (especially Hentai.tv embedUrls, SxyPrn
  file-host embeds, OpJav/JavMost/7MMTV bot-wall reachability, and XAnimu numeric
  taxonomy drift).

## 2ar. Phase 35 — Video Player UI Overhaul & Touch Controller Re-architecture (2026-10-14)

**Topic:** the player control overlay's taps intermittently failed to reveal/hide the
controls (state sync issue between fragmented touch listeners). Clean top-to-bottom
overhaul of `PlayerActivity.kt`'s UI controller and gesture architecture.

Status: **implemented and verified (statically + unit).** No device/`adb` in this environment,
so tap feel, hold-to-2X timing and the 3 s fade are reasoned from composition structure;
every touched path compiles + all unit tests pass. Files and verification at the end.

### 1. Unified touch & gesture architecture
- **Root cause:** single-tap/double-tap lived in one `detectTapGestures` detector while
  hold-for-2X lived in a *second* `detectTapGestures(onPress)` detector on the same
  surface — both raced for the same down event, so taps intermittently never toggled.
- **Fix:** all three share **one** `detectTapGestures(onTap + onDoubleTap + onPress)`
  interceptor (Phase 35 unified tap interceptor), so arbitration is deterministic.
- **Deterministic single-tap** via the pure `playerTapAction()` policy: visible → hide
  now + cancel the timer; hidden → reveal + fresh 3,000 ms countdown; locked → ignored
  (only the lock toggle acts).
- **Preserved:** double-tap L/R seek ∓`doubleTapMs` with ripple + haptic; hold ≥
  `HOLD_TO_BOOST_MS` boosts to 2.0x with the top-center `"2X Speed ▶▶"` pill, restoring
  the saved speed on release; drag scrubbing shows HUD tooltips and pauses auto-hide
  while `scrubbing` (pinch-to-zoom and the drag zones are untouched).

### 2. State-driven auto-hide engine
- **New pure `ui/player/PlayerControls.kt`** (repo pattern: pure helper + unit test):
  `PlayerControlsState` (VISIBLE / HIDING / HIDDEN), `PLAYER_CONTROLS_HIDE_MS` 3000L,
  `shouldAutoHideControls()` (playing + ready + no sheet + unlocked + not scrubbing +
  no error), `playerTapAction()` and `forcedControlsState()` (locked → HIDDEN;
  paused / buffering / error / sheet / scrubbing → VISIBLE; active playback → null,
  timer owns it).
- **Single timer:** `autoHideJob` is the only countdown — `resetAutoHideTimer()`
  cancels any pending job and launches a new 3,000 ms delay; `hideControlsNow()`
  cancels and hides. Every interaction (tap, seek, slider, speed/boost/normalize,
  track selection, 2X release, unlock) funnels through it (`pokeControls()` is kept
  as an alias so existing call sites behave identically).
- The `LaunchedEffect` now only enforces forced states and (re)arms the timer when
  eligibility changes — it no longer owns its own `delay()`.

### 3. Modernized overlay & animations
- Fades `fadeIn(tween(300))/fadeOut(tween(300))` → **`fadeIn(tween(200))` /
  `fadeOut(tween(250))`** — no abrupt pops.
- **Top bar:** adds Subtitle/Audio track + Speed actions next to Back / title /
  external / Cast / PiP / rotate / sleep / lock.
- **Center:** play/pause icon morphs through `AnimatedContent` (`playPauseMorph`);
  ±10 s buttons + double-tap ripple unchanged.
- **Bottom bar:** position / duration + scrubber now carry a **quality badge**
  (`StreamSource.quality`, new `qualityLabel` param, hidden when blank).

### Files
`ui/player/PlayerControls.kt` (new state machine + policy),
`ui/player/PlayerActivity.kt` (unified interceptor, engine, overlay),
`app/src/test/.../Phase35PlayerControlsTest.kt` (+7).

### Verification (2026-10-14)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings;
  two in-session fixes: duplicate `qualityLabel` param, local `val … get()` → plain derived val).
- `:app:testDebugUnitTest --rerun-tasks` → **216/216 tests, 0 failures, 0 errors** (+7 new;
  hide ceiling, auto-hide eligibility, tap determinism, locked ignore, forced states).
- Not pushed; no APK rebuilt this session. Device pass still needed: tap show/hide feel,
  double-tap seek + ripple, hold-to-2X boost + pill, 3 s fade during playback, timer reset
  on scrub/speed change, stay-visible when paused/buffering/sheet, lock toggle isolation.

## 2aq. Source expansion — 7 new built-in sources (2026-10-14)

**Topic:** broaden built-in NSFW Anime + Real 18+ coverage with high-priority sites, following
the established modular pattern (`HentaiSource` / `AdultSource` + registry + per-source error
isolation, so one dead site never breaks its tab).

Status: **implemented and verified (statically + unit).** No device/`adb` in this environment,
so live reachability + ExoPlayer playback per new site still need the device pass below. Every
parser is pinned by fixture tests (no network). Files and verification at the end.

### 1. NSFW Anime (+2 — search + streams; AniList/Jikan metadata untouched)
- **Rule34Video** (`rule34video`, `Rule34VideoSource.kt`) — tube archive of rule34/parody
  animation: search `/search/{query}/` (confirmed pattern), video pages `/video/{id}/{slug}/`;
  shared `HentaiTubes.parseCards` (`/video/` filter) + `resolvedStreams` (page → iframe →
  nested frame).
- **AnimeIdHentai** (`animeidhentai`, `AnimeIdHentaiSource.kt`) — WordPress-style index:
  search `/?s=` → `/search/{query}/` → `www.` mirror fallback; generic `parseCards` with the
  site-chrome filter; `resolvedStreams` for playback.

### 2. Real 18+ general (+2 — full `AdultSource`: home/search/categories/detail)
- **Eporner** (`eporner`, `EpornerSource.kt`) — public JSON API v2 for listings/search
  (`order` = latest / top-week / top-month; entries carry page URL + thumb + duration);
  `/cats/` parse with a curated 12-category fallback; streams from the video page via
  `TubeMedia`.
- **Beeg** (`beeg`, `BeegSource.kt`) — v6 JSON API for index/search (`videos[]` + `file`
  quality→URL map, highest-first, page-`Referer` headers) with HTML cards as fallback;
  per-video endpoint for streams, `TubeMedia` on the page as fallback.

### 3. Real 18+ JAV (+3 — full `AdultSource`)
- **JavGuru** (`javguru`, `JavGuruSource.kt`, base `jav.guru`) — WordPress tube: front page +
  `/category/english-subbed/` + `/category/decensored/` rows, `/?s=` search, taxonomy-aware
  `parseList`, live-or-static categories, `TubeMedia` streams.
- **SupJav** (`supjav`, `SupJavSource.kt`) — catalogue with `/search/` + `/?s=` fallbacks and
  maker/tag/category-aware `parseList` (maker/director/tag/category/page excluded).
- **VJAV** (`vjav`, `VjavSource.kt`, confirmed layout) — `/videos/latest-updates/1/` +
  `/videos/most-popular/1/` + `/videos/top-rated/1/` rows, `/search/1/?s=` search,
  `/video…` card parser with listing-chrome exclusion.

### Kept guarantees
- Progressive loading + per-source error isolation unchanged: the repositories iterate the
  registries, so new sources inherit timeouts, quarantine, caches and notices automatically.
- NSFW Anime discovery stays AniList-led (built-ins resolve episodes/streams only).

### Files
`data/hentai/Rule34VideoSource.kt`, `data/hentai/AnimeIdHentaiSource.kt`,
`data/hentai/HentaiSources.kt` (registry 7 → 9), `data/adult/EpornerSource.kt`,
`data/adult/BeegSource.kt`, `data/adult/JavGuruSource.kt`, `data/adult/SupJavSource.kt`,
`data/adult/VjavSource.kt`, `data/adult/AdultSources.kt` (registry 10 → 15),
`app/src/test/.../ExpandedSourcesTest.kt` (+10),
`AdultSourcesExtraTest.kt` + `HentaiSourcesExtraTest.kt` (registry guards extended).

### Verification (2026-10-14)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings;
  one in-session fix: a shadowing local `id` in `BeegSource.detail` renamed to `videoId`).
- `:app:testDebugUnitTest --rerun-tasks` → **209/209 tests, 0 failures, 0 errors** (+10 new;
  listing/API/stream-shape fixtures per source + expanded registry uniqueness guards).
- Not pushed; no APK rebuilt this session. Device pass still needed: per-site home rows,
  search relevance, detail + real playback (especially Beeg file-map URLs, Eporner API
  reachability, VJAV/supjav markup drift, and the javguru.com webcam-clone vs jav.guru
  distinction).

## 2ap. Player controls auto-hide & fade timeout (2026-10-14)

**Topic:** regression — video player controls overlay stayed permanently visible during
playback instead of auto-hiding.

Status: **implemented and verified (statically + unit).** No device/`adb` in this environment,
so the 3 s fade is reasoned from composition structure; every touched path compiles + all
unit tests pass. Files and verification at the end.

### 1. Auto-hide timer (`ui/player/PlayerActivity.kt`)
- New `CONTROLS_HIDE_MS = 3000L` + `isBuffering` (ticker + `onPlaybackStateChanged`) +
  `controlsPoke` counter + `pokeControls()` (show + bump poke to restart the timer).
- New `LaunchedEffect(isPlaying, isBuffering, sheetOpen, locked, scrubbing,
  controlsVisible, controlsPoke)`: locked forces hidden; sheet-open / scrubbing /
  paused / buffering forces visible; otherwise while playing + ready it `delay(3 s)` then
  hides. Hidden stays hidden until tap (no auto-show).
- **Tap-to-toggle:** single tap hides immediately when visible, `pokeControls()` when hidden.
- **Reset on interaction:** drag (brightness/volume/seek) + drag-end/cancel, double-tap seek,
  ±10 s buttons, play/pause, slider drag + release, speed/boost/normalize change, 2X-boost
  release all call `pokeControls()`.
- **Keep-visible guards:** `sheetOpen = showTracks || showEpisodes || showSpeedAudio ||
  showExternal || showSleep` — any open sheet pins controls visible.

### 2. Smooth fade
- Controls `AnimatedVisibility` `fadeIn()/fadeOut()` → `fadeIn(tween(300))` /
  `fadeOut(tween(300))` — 300 ms alpha instead of a snap.

### Files
`ui/player/PlayerActivity.kt` (auto-hide effect, poke wiring, fade durations).

### Verification (2026-10-14)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **199/199 tests, 0 failures, 0 errors**.
- Not pushed; no APK rebuilt this session. Device pass still needed: 3 s fade during playback,
  tap show/hide, timer reset on scrub/speed change, stay-visible when paused/buffering/sheet.

## 2ao. Website full rebuild — skill-backed motion system (2026-10-13)

**Topic:** rebuild `website/` (Next.js 15 single page) on the four installed skills
(`website/.agents/skills/`: `apple-design`, `transitions-dev`, `transitions-polish`,
`transitions-ui-generation`) plus the Playwright CLI for screenshot verification.
Brand rethought (not just re-skinned): deeper void `#06060C`, signal-mint `#3DDC97`
secondary, tighter display type, reordered page (Stats band up after Ecosystem).

Status: **implemented and verified.** `next build` ✓ (first-load JS 216 → **174 kB**),
Playwright screenshot sweep ✓ (desktop hero + full page, mobile hero, Features/FAQ/CTA
sections — stagger entrances, accordion, tilt, pop-in digits, dropdown menu all confirmed
on-screen). Committed + pushed to `main` (Vercel auto-redeploys).

**Topic:** rebuild `website/` (Next.js 15 single page: Navbar → Hero → EcosystemMarquee →
Features → Stats → FAQ → DownloadCta → Footer) on the four installed skills
(`website/.agents/skills/`: `apple-design`, `transitions-dev`, `transitions-polish`,
`transitions-ui-generation`) plus the Playwright CLI for screenshot verification.

Status: **planned, scope approved (full rebuild), execution not started — no code touched.**
Open question: keep the dark-violet brand + section order, or rethink branding/structure.

### What shipped (execution log)

**Phase 0 — motion tokens.** Full `transitions-dev/_root.css` token scale pasted once into
`app/globals.css` (`--duration-*`, `--ease-*`, `--distance-*`, `--scale-*`, `--blur-*` +
per-transition tunables); `.glass` upgraded to a real material (saturate + top-edge
highlight); new `.display-tight` type utility (apple-design tracking discipline).

**Phase 1 — motion system (`apple-design`).** All framer-motion entrances converted to the
critically damped house spring (`bounce: 0, duration: 0.4`); Navbar is a true translucent
material on scroll; dead `TextGenerateEffect` deleted.

**Phase 2 — per-section transitions (`transitions-dev`, verbatim snippets + hooks).**
New `components/ui/`: `reveal.tsx` (`Stagger` = `texts-reveal`), `tilt.tsx` (`card-tilt`
orchestration, touch-scroll-safe), `stat-number.tsx` (`number-pop-in`), `gen-reveal.tsx` +
copied `tgen/ui-generation.{js,css}` runtime. Hero: stagger copy, `t-shimmer` version pill,
`Tilt` + `data-t-generate="skeleton"` phone reveal. Stats: pop-in digits
(199 / 4 / 25k / 2X). FAQ: custom `t-acc` accordion (HeroUI Accordion removed — part of the
JS diet). Navbar: mobile `t-dropdown` menu (origin top-right, close-asymmetry 150 ms).
DownloadCta + Footer: `learn-more-hover` chevron links, shimmer pills. Copy refreshed to
v1.6.2 truth (199 tests, hold-for-2X, ≈42 MB). Page reordered: Stats band after Ecosystem.

**Phase 3 — polish (`transitions-polish`).** Remaining hardcoded motion mapped to tokens
(nav transitions, link hovers); ambient loops left alone (no matching token usage, per the
usage-first doctrine).

**Phase 4 — verify.** `next build` ✓, Playwright sweep ✓, then commit.

### Phase 0 — motion tokens
Install `transitions-dev/_root.css` once into `app/globals.css` (durations, easings,
distances, scales, blur); everything below references the tokens.

### Phase 1 — motion system (`apple-design`)
Springs as the house default (critically damped `1.0` / response `0.3–0.4`, bounce only on
flick-driven motion); Navbar as a true translucent material; size-specific type
tracking/leading; reduced-motion audit.

### Phase 2 — per-section transitions (`transitions-dev`)
Hero `texts-reveal` + `card-tilt` on `PhoneMockup`; Stats `number-pop-in`/`spinning-counter`;
FAQ `accordion`; Navbar `menu-dropdown` + `tabs-sliding`; DownloadCta `shimmer-text` +
`learn-more-hover`. Optional hero moment: `transitions-ui-generation` skeleton reveal on the
phone mockup when scrolled into view.

### Phase 3 — polish (`transitions-polish`)
Tokenize remaining hardcoded motion; open/close asymmetry (opens 250 ms, closes 150 ms);
stagger totals under ~300 ms.

### Phase 4 — verify
`next build` ✓ + Playwright screenshot sweep per section (desktop + mobile), then commit.

## 2an. Phase 34 — Stremio Add-on Fixes, Hold-to-2X Speed Gesture & Skip Feature Removal (2026-10-13)

**Topic:** resolve Stremio add-on playback/crash issues, add a modern press-and-hold 2X player
gesture, and delete the broken skip-intro/outro machinery.

Status: **implemented and verified (statically + unit).** No device/`adb` in this environment,
so playback behaviour is reasoned from the protocol + composition structure; every touched path
is unit-pinned where it is pure-JVM. Files and verification at the end.

### 1. Stremio add-on stream fetching & Add-on Manager toggle crash
- **`data/remote/StremioClient.kt`:** `enc()` is now path-safe — preserves literal `:` and emits
  `%20` for spaces (was `URLEncoder`, which turned episode ids like `tt123:1:2` into
  `tt123%3A1%3A2` and broke manifest-V3 stream URLs on Torrentio/CyberFlix). `fetchStreams()`
  and `fetchMeta()` are total (malformed JSON → empty/`null` instead of throwing), and
  `parseStream()` is hardened per-entry: safe primitive extraction (JsonNull no longer throws),
  blank strings treated as absent, `sources`/`behaviorHints.proxyHeaders` read defensively, and
  the whole parser wrapped in `runCatching` — one poisoned stream can no longer void the list,
  so valid `url` (CyberFlix) and `infoHash` (Torrentio) entries yield playable streams with
  their CDN headers (`Referer`/`User-Agent`/`Cookie`) and `filename` titles intact.
- **`data/repo/AddonRepository.kt`:** every read-modify-write is serialized behind a `Mutex`
  and guarded by `runCatching` with Gson-hole-safe predicates — rapid enable/disable toggles no
  longer interleave `first()` + `saveAddons()` (the NPE / lost-write path). `updateAll()`
  snapshots ids first instead of iterating the live list (the ConcurrentModificationException
  path); `enabledAddons()`/`enabledWithResource()`/`syncAllRepos()` degrade to empty instead of
  throwing on holey rows.
- **`ui/vm/ViewModels.kt` (`AddonViewModel`):** `setEnabled()`/`remove()`/`updateAll()` are
  `runCatching`-guarded so a store failure surfaces as a message (or silently) instead of
  crashing the manager.

### 2. Modern hold-for-2X player gesture (`ui/player/PlayerActivity.kt`)
- `PlayerScreen` gains `isBoosting` + `boostRestoreSpeed` state and a `HOLD_TO_BOOST_MS = 400 ms`
  `onPress` tap-gesture stage: a press held past the threshold (while playing, unlocked) sets
  `player.setPlaybackSpeed(2f)` with a long-press haptic; release restores the user's saved
  `speed` and clears the state. Short taps never boost, so single-tap (controls) and double-tap
  (seek ±`doubleTapMs`) behaviour is unchanged, and the stage consumes nothing so the existing
  pinch/drag/tap handlers still receive the press.
- Clean top-center pill (`"2X Speed ▶▶"`, 65 % black capsule) fades in/out with the boost
  (`AnimatedVisibility`, 200 ms fade).

### 3. Skip Intro & Skip Outro removal
- Deleted from `PlayerActivity`: `DEFAULT_INTRO_END_MS`/`DEFAULT_OUTRO_MS`, the
  `introEndMs`/`outroStartMs` `PlayerScreen` params + call site, the skip-window computation,
  the auto-skip `LaunchedEffect`, and the Skip Intro/Outro button row (no background interval
  checking remains — the 250 ms ticker only drives playhead/buffer/stats now).
- Deleted the dead `autoSkipIntro` plumbing end-to-end: `SettingsStore` key/flow/setter,
  `SettingsViewModel` exposure/setter, the Settings search-filter mention, and the
  `StreamSource.introStartMs/introEndMs/outroStartMs` marker fields (no other producer/consumer
  existed).

### Files
`data/remote/StremioClient.kt` (path-safe `enc`, total fetches, hardened `parseStream`),
`data/repo/AddonRepository.kt` (mutation `Mutex` + totalled reads), `ui/vm/ViewModels.kt`
(guarded add-on toggle paths), `data/model/Models.kt` (marker fields removed),
`data/local/SettingsStore.kt` + `ui/screens/settings/SettingsScreen.kt` (dead auto-skip
setting removed), `ui/player/PlayerActivity.kt` (skip UI deleted, hold-to-2X added),
`app/src/test/.../Phase34StremioTest.kt` (+10).

### Verification (2026-10-13)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **199/199 tests, 0 failures, 0 errors** (+10 new;
  url/infoHash acceptance, JsonNull/blank/non-object totality, proxy-header + filename
  extraction, `candidateRequests` series/anilist forms, `chooseRequest` prefix/fallback).
- Not pushed; no APK rebuilt this session. Device pass still needed: Torrentio/CyberFlix
  movie + series-episode stream resolution, rapid add-on toggle stability, hold-to-2X boost
  feel + pill, and skip-UI absence across episodes.

## 2am. Phase 32 — Stream Scraper Performance & Real 18+ Speed Optimization (2026-10-13)

**Topic:** kill the noticeable delay when loading content and fetching video streams,
especially in the Real 18+ hub — parallel provider racing, faster Real 18+ metadata, and
fail-fast fallbacks so one hung server never stalls the UI or video startup.

Status: **implemented and verified (statically + unit).** No device/`adb` in this environment,
so wall-clock gains are reasoned from the fan-out structure + unit-pinned policy; every
previously unbounded or order-blocked hop is now bounded and completion-ordered. Files and
verification at the end.

### 1. Asynchronous parallel provider racing
- **New pure `data/repo/ScraperRace.kt`** (repo pattern: pure helper + unit test):
  `PROVIDER_TIMEOUT_MS` / `SUBTITLE_TIMEOUT_MS` / `DETAIL_TIMEOUT_MS` = **5 s**,
  `isPlayableCandidate()` pre-filter (blank/non-http/thumbnail/subtitle URLs never win),
  `raceResult()` cancellation-safe bound (timeouts degrade to failure, genuine caller
  cancellation rethrows — never recorded as a mirror failure), `MirrorHealth` (quarantine
  after 2 consecutive failures, 5 min TTL, injectable clock) and `PageCache` (64-entry /
  5-min TTL LRU for list HTML, injectable clock).
- **`StreamRepository`:** `STREAM_TIMEOUT_MS` 12 s → **5 s** (+ `SUBTITLE_TIMEOUT_MS` 5 s);
  `stremioStreamsDetailed()` rewritten from registry-order `async/await` to completion-order
  `launch` + `Mutex` — each provider's partial result yields via new optional `onPartial`
  as soon as the first valid playable link resolves; MOVIE path races the Stremio fan-out
  against the CloudStream title match concurrently (was sequential `await` then `await`).
- **`DetailViewModel.loadStreams`:** passes `onPartial` through `streamsForDetailed()` and
  paints `streams.value` on the first non-empty partial (request-guarded), so the stream
  sheet fills progressively instead of waiting on every hoster.

### 2. Real 18+ content & metadata acceleration
- **`AdultRepository.home/searchDetailedProgressive`:** fixed the head-of-line block — both
  used `async` fan-out but `await`ed **in registry order**, so slow source 0 gated fast
  source N. Both now `launch` per source under a `Mutex` and invoke `onUpdate` in
  **completion order**; each source races under `HOME_TIMEOUT_MS` 5 s (home was unbounded)
  and quarantined mirror ids are skipped (`MirrorHealth`).
- **`AdultHttp`:** new `getList()` fast path for catalog/home rows — in-memory `PageCache`
  first (back-navigation is instant, no network), else single-attempt `httpGetList`
  (short timeouts, no 429 retry); `get()` itself now also reads/writes the `PageCache`, so
  detail HTML benefits too. `verifyPlayable`
  now pre-filters via `isPlayableCandidate`, caps concurrency with `Semaphore(8)` and bounds
  each probe under 5 s.
- **`Http`:** shared OkHttp `ConnectionPool(8, 5 min)` + `Dispatcher(32 total / 6 per host)`
  so 12 concurrent scrapes reuse connections; new `listClient` (6 s connect / 8 s read,
  redirects followed, no retry — the timeouts bound the chain) behind new `httpGetList()` — initial list loads skip
  redirect chains and fail fast, detail/stream paths keep the full retrying client.
- **Thumbnails:** Coil disk/memory image cache (256 MB, `NovaApp`) already covers posters —
  verified, unchanged; the new `PageCache` covers the parsed-catalog side.

### 3. Fail-fast & fallback mechanisms
- Every fan-out now degrades per-provider instead of gating: `CatalogRepository.safeRow`
  (was unbounded) + `rowsForSection`/`searchInternal`/`globalSearch` (were sequential
  phases) + `cloudStreamSearch` + Stremio `ADDON/CATALOG` caps tightened 6–7 s → **5 s**;
  `AdultRepository.detail/categories` wrapped in 5 s; `HentaiRepository.STREAM_TIMEOUT`
  12 s → **5 s** (+ bounded verify). Timeouts surface as ordinary per-source notices —
  the UI treatment dead sites already get — and caller cancellation is always rethrown.

### Files
`data/repo/ScraperRace.kt` (new policy + `MirrorHealth` + `PageCache`),
`data/remote/Http.kt` (pool/dispatcher, `listClient`, `httpGetList`),
`data/adult/AdultHttp.kt` (`getList` + page cache, bounded `verifyPlayable`),
`data/adult/AdultRepository.kt` (completion-order home/search, 5 s caps, quarantine),
`data/repo/StreamRepository.kt` (5 s caps, completion-order progressive `onPartial`,
concurrent MOVIE race), `data/repo/CatalogRepository.kt` (bounded `safeRow`, parallel
rows/search phases, 5 s caps), `data/hentai/HentaiRepository.kt` (5 s stream cap),
`ui/vm/ViewModels.kt` (progressive `loadStreams` paint),
`app/src/test/.../ScraperRaceTest.kt` (+13).

### Verification (2026-10-13)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **189/189 tests, 0 failures, 0 errors** (+13 new;
  policy ceiling pinned to the 3–5 s budget, `raceResult` timeout/cancellation/error paths,
  quarantine TTL, page-cache expiry/eviction).
- Not pushed; no APK rebuilt this session. Device pass still needed: cold Real 18+ tab
  first-paint, stream-sheet first-link latency, back-nav instant (page cache), and no
  regression on empty/error states.

## 2aj. Phase 30 — Cloud Sync, Anime Trackers & Customizable Dashboard (2026-10-11)

**Topic:** Trakt/AniList/MAL sync, dashboard customization, and Cast/DLNA output.

Status: **implemented and verified.** Audit first: the foundations existed but the headline
feature was dead — `ScrobbleManager.syncAll` had **zero callers**, so every provider toggle and
pasted token did nothing; the dashboard had reorder but no hide/pin; the player had a Cast icon
that only opened system settings. This phase wires the missing links and adds the new surfaces.
Files and verification at the end.

### 1. Trakt.tv integration (watch history & progress sync)
- **OAuth inside Settings (new).** The app ships no client secrets, so `TrackerOAuth.kt` builds
  browser authorize URLs from credentials the user pastes from their own provider app
  registration (new OAuth-credentials dialog + per-row Connect/Reconnect buttons for
  Trakt/AniList/MAL; pasted tokens stay as the fallback). The `novastream://oauth` redirect
  lands in the new transparent `OAuthCallbackActivity` (manifest `VIEW`/`BROWSABLE` filter),
  which exchanges the code — JSON for Trakt/AniList, form + PKCE verifier for MAL — and stores
  the token exactly like a pasted one. Every failure degrades to a toast.
- **Bidirectional sync.** Push: playback now fires `ScrobbleEvent`s (completion ≥ 90 % once per
  title plus a ~60 s progress heartbeat, with episode/season numbers) through the previously
  dead `syncAll` path, and Trakt calls carry the user's OAuth client id as the required
  `trakt-api-key` header (placeholder kept for pasted-token setups). Pull: new
  `pullWatchedIds` reads Trakt `sync/watched` shows+movies, and Settings → "Import watched
  from Trakt" marks matching local entries complete via the pure-tested
  `parseTraktWatchedImdbIds` join (no IMDB id → skipped, never guessed).

### 2. AniList & MyAnimeList auto-sync
- **OAuth login:** same browser flow as Trakt — AniList is a plain code grant, MAL adds PKCE
  S256 (hand-rolled base64url + `SecureRandom` verifier, no Android APIs; the RFC 7636 vector
  is a unit test). The existing per-provider mutations already send episode progress + CURRENT /
  COMPLETED status, so anime episodes reaching completion now update the tracker lists
  automatically once connected. Score sync is out of scope (no scoring UI exists to source it).
- Other providers (Kitsu/SIMKL) keep their token-paste rows; watched import reports unsupported
  for them instead of guessing id mappings.

### 3. Customizable home dashboard & pinned catalogs
- **Hide rows (new).** `hiddenHomeRows` setting + `applyRowVisibility` (exact-title match) applied
  in `HomeScreen` after the saved order; `HomeOrderScreen` rows gain eye toggles (hidden titles
  dim) with instant persistence.
- **Pin to top (new).** `pinRowToTop` moves any rail to the front of the saved order (unhiding it
  when hidden) via a double-chevron button per row — genre/catalog rails included, since every
  rail is just a titled row. Reorder (Phase 12) is unchanged.

### 4. Chromecast & DLNA support
- **Real Cast sender (new).** `media3-cast` + `mediarouter` + `play-services-cast-framework`
  dependencies, `NovaCastOptionsProvider` targeting the Default Media Receiver (`CC1AD845`, so
  no custom receiver registration), manifest options-provider metadata, a real
  `MediaRouteButton` in the player top bar (device discovery + system chooser), and a guarded
  `SessionManagerListener` (registered in `onResume`, removed in `onPause`) that loads the
  current stream URL (mp4 vs HLS content type) onto the receiver once per URL and pauses local
  playback so audio never doubles. No-Play-services devices get an empty route list, never a
  crash (`runCatching` on `CastContext`).

### Files
`data/integrations/TrackerOAuth.kt` (new pure OAuth/PKCE builders), `data/integrations/ScrobbleManager.kt`
(push client-id header, Trakt pull + `parseTraktWatchedImdbIds`), `data/local/SettingsStore.kt`
(OAuth credential/verifier/hidden-row keys), `ui/player/OAuthCallbackActivity.kt` (new deep-link
exchange), `ui/player/NovaCastOptionsProvider.kt` (new), `ui/player/PlayerActivity.kt` (scrobble
triggers, Cast session + route button replacing the settings shortcut),
`ui/screens/settings/SettingsScreen.kt` (Connect buttons, credentials dialog, watched import),
`ui/screens/settings/HomeOrderScreen.kt` (hide/pin + pure helpers), `ui/screens/home/HomeScreen.kt`
(hidden filter), `ui/vm/ViewModels.kt` (credential + import actions), `AndroidManifest.xml`
(OAuth filter, Cast metadata), `app/build.gradle.kts` (Cast deps),
`app/src/test/.../TrackerOAuthTest.kt` (+10), `app/src/test/.../HomeRowsTest.kt` (+8).

### Verification (2026-10-11)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings; one
  in-session API fix: `RemoteMediaClient.load(MediaInfo, Boolean)` is deprecated, so the handoff
  uses the single-arg `load(MediaInfo)` autoplay overload).
- `:app:testDebugUnitTest` → **161/161 tests, 0 failures, 0 errors** (+18 new).
- Not pushed; no APK rebuilt this session. Device pass still needed: full OAuth grants against
  real Trakt/AniList/MAL app registrations, completion + heartbeat arrivals on each tracker,
  Trakt import marking, hide/pin persistence across restarts, and casting to a real receiver
  (connect, title handoff, local pause, disconnect).

---

## 2ak. P0 hotfix — Play-crash guardrails (2026-10-12)

**Topic:** crash on tapping Play (P0 regression report against the v1.5.0 player).

Status: **implemented and verified (statically + unit).** No device/`adb` is available in this
environment, so there is no logcat — the cause was root-caused by static analysis plus
binary verification against the Gradle-cached libraries, and every candidate edge now fails
gracefully instead of crashing. Files and verification at the end.

### 1. Root-cause inspection (no logcat available)
- `MediaItem.Builder.setUri(String?)` tolerates null (verified via `javap` on
  `media3-common-1.3.1`: `setUri(String)` delegates to `setUri(Uri)` with no `build()`
  validation), and `MediaRouteButton` is a plain `View` with no AppCompat-theme requirement
  (verified on `mediarouter-1.7.0`) — so neither is a deterministic crash on its own.
- The actual holes on the Play path are the unguarded edges around them: unvalidated intent
  extras, the Gson null-field hole (`headers`/`alternates`/`subs`/`episodes` can carry
  runtime nulls in non-null slots — the same bug class as Phase 13/21), an unguarded
  ExoPlayer build + `setMediaItem`/`prepare`, an unguarded Cast button init on non-GMS
  devices, and a real `videos/mp4` content-type typo in the Cast handoff.

### 2. Defend-and-recover guardrails
- **New pure `ui/player/PlayerStartup.kt`** (repo pattern: pure helper + unit test):
  `isValidPlayerItem` / `isPlayableStreamSource` / `sanitizeSubtitleTracks` /
  `sanitizeEpisodes` / `safeStreamHeaders` / `safeStreamAlternates` — every helper is total
  (per-entry `runCatching`, never throws, even on Gson-poisoned payloads).
- **`PlayerActivity.onCreate`:** item/source validated before use (bad intent → `Log.e` +
  toast + `finish`, never a crash); episodes/subs sanitized; headers read through
  `safeStreamHeaders`; ExoPlayer build and `setMediaItem`/`prepare` each wrapped with
  `Log.e` breadcrumbs + toast + `finish` fallback.
- **`buildMediaItem`:** media id read defensively; each subtitle config built per-entry
  (`mapIndexedNotNull` + `runCatching`) so one poisoned sub can't kill the item.
- **`applySource` / `tryFailover`:** sanitized subs/headers/alternates; failover candidates
  trimmed, blank-filtered and de-duped; episode-switch load failure toasts instead of throwing.
- **Cast (Phase 30):** `MediaRouteButton` creation wrapped — on failure the top bar recomposes
  to the pre-Phase-30 Cast icon opening the system cast-settings shortcut
  (`openCastSettings()` restored); `onCastConnected` ignores blank URLs, reads the title
  defensively, and the handoff content type is fixed to `video/mp4`.
- **Review follow-ups:** `tryFailover()` (reachable from the ExoPlayer error callback) is fully
  contained — a holey `copy()` sheds to a clean `StreamSource(url)` and any throw becomes
  `false`; `reload()` mirrors `applySource`'s containment; episode/auto-download stream
  picking uses `isPlayableStreamSource` (blank URLs no longer win over good mirrors) with
  sanitized headers/alternates into the download queue.
- **Logging:** file-level `PLAYER_TAG` (top-level composables can't see private companion
  members — same lesson as `GESTURE_SLOP_PX`) used for all six `Log.e` breadcrumbs, so the
  next field report identifies itself in logcat.

### Files
`ui/player/PlayerStartup.kt` (new pure guards), `ui/player/PlayerActivity.kt` (launch
validation, guarded build/prepare/failover, Cast fallback, typo fix),
`app/src/test/.../PlayerStartupTest.kt` (+10).

### Verification (2026-10-12)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (one in-session fix: `TAG` moved from the
  companion to file scope as `PLAYER_TAG`).
- `:app:testDebugUnitTest` → **171/171 tests, 0 failures, 0 errors** (+10 new; incl. a live
  reproduction of the Gson hole — `"{}"` decodes to runtime nulls, the raw access throws,
  the guard absorbs it).
- Local + remote paths reasoned through (detail direct/torrent via the guarded `intent` path;
  Library downloads via `playDownloaded`, whose stale-JSON items are now rejected gracefully
  instead of NPEing) — **device pass still needed**: logcat confirmation plus Play from
  detail/torrent/downloads on GMS and non-GMS hardware.
- Not pushed; no APK rebuilt this session.

### Release (2026-10-12, v1.5.1)
- `versionCode` 10 / `versionName` 1.5.1; `:app:compileDebugKotlin` ✓,
  `:app:testDebugUnitTest` ✓ **171/171**, `:app:assembleRelease` ✓ (signed).
- `dist/NovaStream.apk` refreshed (**40,140,687 bytes**, SHA-256
  `A58E3D2B12C030E556BFA0DC27AF820301EA4D8D360E6BDD5E65E4E27E7E1151`;
  `versionCode` 10 / `versionName` 1.5.1 verified in `output-metadata.json`, `PlayerStartup`
  verified in the release dex); tagged `v1.5.1` and pushed.
- **GitHub Release `NexusStream v1.5.1` published with `app-release.apk` attached**
  (40,140,687 bytes, asset digest `sha256:a58e3d2b…e1151` matching the local build;
  verified `releases/latest` → v1.5.1 and `releases/latest/download/app-release.apk` →
  HTTP 200 / 40,140,687 bytes), so the in-app update dialog now serves v1.5.1.
- Website CTA/hero bumped to v1.5.1 (`next build` ✓) and pushed (Vercel auto-redeploys).

---

## 2al. CloudStream provider runtime — run real `.cs3` extensions in-process (2026-10-12)

**Topic: make the provider repositories "fully work" — an installed CloudStream extension must
produce real, playable stream links.** Recon first: the repos already parse (CloudStream
`repo.json` → `plugins.json` hop, Aniyomi `index.min.json`, Keiyoushi `index.pb` + JSON mirror),
but "install" only stored name/icon/version metadata — nothing ran. The app's own `data/ext` engine
uses a custom `NexusExtension` contract that real `.cs3` / Mihon `.apk` files do not implement, so a
bridge was required.

Status: **implemented; compiles + unit-tested; device pass required.**

### 1. CloudStream runtime dependency
- Added `com.github.recloudstream.cloudstream:library:4.7.0` (JitPack) as `implementation`, which
  brought its real dependency tree (NiceHttp, jsoup, Jackson, Ktor, kotlinx-serialization, rhino,
  NewPipeExtractor, …). `-Xskip-metadata-version-check` is set because the library is built with a
  newer Kotlin than the project's 2.0.20. **4.7.0** was required over 4.8.0: the latter declares
  `minCompileSdk=36` while the project is on `compileSdk 34` / AGP 8.5; 4.7.0 declares
  `minCompileSdk=1`.

### 2. App-module shims (`com.lagradost.cloudstream3`)
Providers are compiled against the CloudStream **app**, so inlined `CloudStreamApp.getKey/setKey`,
`CommonActivity.showToast`, and `DataStore` member calls must resolve at runtime. Added
`CloudStreamApp`, `CommonActivity`, and `utils/DataStore` shims (SharedPreferences-backed settings,
real Toasts) mirroring the upstream public surface.

### 3. In-process loader (`data/cloudstream/CloudStreamRuntime.kt`)
`PathClassLoader(cs3File, appClassLoader)` → read `manifest.json` → instantiate the
`BasePlugin` → call `load()`, which self-registers every `MainAPI` provider into `APIHolder`.
`unload()` removes the plugin's providers. Mirrors CloudStream's own `PluginManager.loadPlugin`.

### 4. Adapter + repository
- `CloudStreamSources`: search → `MediaItem` (`addonId = cloudstream:<provider>`, `id` = provider
  page URL), `load()` → episodes (`Video.id` carries the provider "data" string), `loadLinks()` →
  app `StreamSource`s (url / quality / referer+headers). `metaDetail()` builds the detail page.
- `CloudStreamRepository`: download `.cs3` into app-private storage, load, persist
  (`AddonStore.cloudStreamPlugins`), uninstall, and reload every installed plugin on launch.

### 5. Wiring
- Add-on Manager **Install** for a CloudStream extension now downloads **and loads** the plugin.
- **Search** merges loaded providers' results; **Detail** and **StreamRepository** resolve
  CloudStream items, and movies additionally get provider links matched by title.
- The player already consumes `StreamSource` (url + headers), so links play with no further work.

### 6. Recommended repositories (not seeded)
The three requested repos — **phisher98** (CloudStream), **yuzono** (Aniyomi), **Keiyoushi**
(Mihon) — are offered as one-tap **Recommended** entries in their tabs. Per the owner's choice they
are **not** auto-seeded (`Defaults.repos` stays empty; the app remains source-neutral), and their
URLs were removed from `retiredRepoUrls` so adding one is not pruned on the next launch.

### Verification
- `:app:compileDebugKotlin` ✓ · `:app:testDebugUnitTest` ✓ **176/176** (+5 `CloudStreamSupportTest`)
  · `:app:assembleDebug` ✓ (no duplicate classes).
- `:app:assembleRelease` ✓ (signed, `CN=NovaStream`) → `dist/NovaStream.apk` (43,659,423 bytes,
  SHA-256 `43CC67BFADA7E67DBB7628199019932167F63F6FE06A1696D1F4F408C2385B9D`); `v1.6.0` tagged +
  pushed, and the GitHub Release published with `app-release.apk` attached (digest
  `sha256:43cc67bfada7e67dbb7628199019932167f63f6fe06a1696d1f4f408c2385b9d`).
- **Still needed:** a device pass — install a phisher98 provider, confirm providers load, search
  returns items, and a title returns playable links. The `.cs3` dex + provider network paths cannot
  be exercised on the JVM.

### Known gaps / next
- **Aniyomi (yuzono) & Mihon (Keiyoushi) runtime** is not built yet: those extensions extend
  `eu.kanade.tachiyomi.*`, a different API, and need their own loader shim.
- **Legacy CloudStream `Plugin`** (extends `BasePlugin`, `load(context)`, resources) is not shimmed —
  only the modern `BasePlugin` + `registerMainAPI` shape loads.

---

## 2ai. Phase 29 — Player Enhancements & Offline Downloads Library (2026-10-11)

**Topic:** player controls (gestures, sleep timer), subtitle/audio customization, and offline
downloads management.

Status: **implemented and verified.** Audit first: most of this phase already existed and was
verified rather than rebuilt — the Media3 swipe gestures (Phase 6), the Quality/Audio/Subtitle
track picker with sync offset (Phase 15), the subtitle preference keys + custom overlay styling
(Phase 11/15) and the Library Downloads tab with thumbnails, offline playback and deletion were
all in place. The genuine deltas this session are the **sleep timer**, the **spec-literal swipe
zones**, the **in-player subtitle style overlay** and the **storage summary header**. Files and
verification at the end.

### 1. Player gesture controls & sleep timer (`PlayerActivity.kt` / Media3)
- **Swipe zones, spec-literal.** The old handler split the screen into 18 % brightness/volume
  edges around a center seek area; it now matches the requested layout — vertical swipe on the
  **left half** adjusts screen brightness, vertical swipe on the **right half** adjusts audio
  volume, horizontal swipe **anywhere** scrubs. The mode locks to the dominant axis once the drag
  passes a 24 px slop (`GESTURE_SLOP_PX`), so a slightly diagonal swipe never flips mid-gesture
  between volume and seek, and the HUD seeds only after the lock (no more wrong-HUD flash from
  touch-down jitter). Throttled seek flushing, HUD and double-tap behaviour are unchanged.
- **Sleep timer (new).** `ui/player/SleepTimer.kt` holds the pure option mapping
  (`OFF / 15 / 30 / 45 / 60 min / END_OF_EPISODE`, `delayMs()`, sheet `label()`,
  `sleepStatusLabel()`); the Activity owns a cancellable `sleepJob` plus
  `sleepOptionState`/`sleepDeadlineState`. Timed options pause playback, snapshot progress and
  disarm with a "Sleep timer ended" toast; end-of-episode cancels any autoplay countdown at arm
  time, suppresses re-triggering in the progress watcher, and fires at the natural end. Expiry is
  deliberately pause-only: the controls hold a non-null player reference, so releasing ExoPlayer
  while the screen is open would crash subsequent seeks — `onDestroy` still owns the release.
- **Sleep UI.** Top-bar Timer/TimerOff button (amber when armed), a live "Sleep Nm" /
  "Sleep: episode end" AssistChip in the bottom chip row (ticks with the 250 ms playhead ticker),
  and a `SleepTimerSheet` bottom sheet with all six options, the remaining readout and
  re-arm/off semantics.

### 2. Advanced subtitle & audio track selector
- **Audio track picker:** already existed — `TrackSheet` lists every embedded/external Media3
  audio group with Default + per-track overrides. Verified, unchanged.
- **Subtitle customization (new overlay).** `TrackSheet` gained a "Subtitle style" section next
  to the existing stream list + sync-offset slider: text-size slider (60–200 %), four font-color
  chips (white/yellow/cyan/green), background-opacity slider (0–100 %) and position slider
  (0–30 %). Edits flow through `updateSubtitleStyle()` — clamped, written to the shared
  `SettingsStore` keys (so Settings > Subtitles stays in sync) and applied live via a new
  `LaunchedEffect(subtitleStyle, playerView)` plus the state-driven time-shifted overlay.
  Outline/shadow needs no control: `applySubtitleStyle` already forces
  `EDGE_TYPE_OUTLINE` with a black edge on every caption.

### 3. Dedicated offline downloads library tab (`LibraryScreen.kt`)
- Already existed as the Library "Downloads" section: completed video/anime downloads and manga
  chapters with thumbnails, pause/resume/delete rows and direct offline playback (`CacheDataSource`
  reads through the shared download cache; manga pages resolve from disk). Verified, kept.
- **New: storage summary header.** `DownloadsStorageCard` sits atop the tab with the tabular
  total bytes across videos + chapters and the "N videos · M chapters stored offline" count, so
  the tab answers storage usage at a glance. Per-row bytes/percent/speed/ETA and deletion
  actions are unchanged.

### Files
`ui/player/SleepTimer.kt` (new pure helper), `ui/player/PlayerActivity.kt` (sleep state/jobs,
watcher hook, gesture zones, `SleepTimerSheet`, `TrackSheet` style section, live style plumbing),
`ui/screens/library/LibraryScreen.kt` (`DownloadsStorageCard`),
`app/src/test/.../SleepTimerTest.kt` (+7 timer cases).

### Verification (2026-10-11)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings; one
  in-session fix: `GESTURE_SLOP_PX` moved from the companion to file scope — top-level
  composables cannot see private companion members).
- `:app:testDebugUnitTest` → **143/143 tests, 0 failures, 0 errors** (+7 new).
- Not pushed; no APK rebuilt this session. Device pass still needed: half-zone gesture feel +
  HUD, timer firing (timed and episode-end, incl. no-autoplay suppression), in-player style edits
  applying to both native and time-shifted captions, and the storage header totals.

---

## 2ah. Phase 28 — Branding, App Icon Replacement & NSFW Hub Overhaul (2026-10-11)

**Topic:** first batch of the new roadmap updates — launcher icon branding, public-surface
desanitization, and the secure NSFW Continue Watching hub + hub UI overhaul.

Status: **implemented and verified.** Files and verification at the end.

### 1. App icon replacement (`res/mipmap` & Manifest)
- New `drawable/ic_launcher_foreground.xml`: the website logo's crisp white "M" zigzag lettermark
  plus the stream dot (the dot is two 180° arc paths — VectorDrawable has no `<circle>`, and the
  first build caught `attribute android:cx not found` at resource-link time), on artwork-only
  transparency so any launcher skin stays legible.
- `values/colors.xml` → `ic_launcher_background` is now solid brand purple `#FF7C5CFF` (was the
  near-black `#FF0A0A0F`); both `mipmap-anydpi-v26` adaptive XMLs pair it with the foreground.
- Fresh legacy PNGs generated for all five densities (`mdpi` 48 / `hdpi` 72 / `xhdpi` 96 /
  `xxhdpi` 144 / `xxxhdpi` 192, square + round): purple rounded-square (square) / purple disc
  (round) with the white "M" stroke + dot scaled from the website `icon.svg` geometry.
- `AndroidManifest.xml` needed no change — it already points at `@mipmap/ic_launcher` /
  `@mipmap/ic_launcher_round` (minSdk 24, so legacy PNGs still serve pre-O launchers).

### 2. Homepage & global history desanitization
- New model-level rule: `MediaType.isAdult` (exactly `NSFW_ANIME` / `NSFW_MANGA` / `REAL`) and
  `MediaItem.isAdult` (`data/model/Models.kt`) — the single discriminator every layer uses.
- `CatalogRepository`: `homeRows()` scrubs adult items per row (emptied rows dropped);
  `searchInternal()` drops adult items for every SFW section; `globalSearch()` drops adult items
  at the *item* level whenever the hub opt-in is off (the add-on flag filter alone could not stop
  a mis-tagged catalog leaking one in).
- `HomeViewModel.continueWatching` / `continueReading` now exclude adult entries; `LibraryViewModel`
  `favorites` / `history` exclude adult entries; `HomeScreen` (rails + hero pool) and
  `LibraryScreen.ActivityGrid` repeat the exclusion as defense-in-depth.
- `LibraryScreen.LibraryFilter` lost its `NSFW` chip — the public Library is Movies / TV / Anime
  only; adult favourites/history surface nowhere outside the hub.

### 3. Dedicated secure NSFW Continue Watching hub (`NsfwViewModel`)
- New isolated flows, collected solely by the PIN/biometric-gated hub:
  `nsfwContinueWatching` (adult video: NSFW anime + Real 18+, unfinished),
  `nsfwContinueReading` (NSFW manga, unfinished), `nsfwFavorites` (adult bookmarks).
- Management actions: `removeNsfwWatch(key)`, `clearNsfwHistory()` (SFW progress kept),
  `removeNsfwFavorite(item)`, `clearNsfwFavorites()` (SFW favourites kept). Adult progress is
  still stored in the shared history — it is only *surfaced* behind the unlock gate.

### 4. NSFW Hub UI polish & overhaul
- Hub tabs are now **Private | NSFW Anime | NSFW Manga | Real 18+** — the Private dashboard is the
  hub's front door: unlocked-state status card (accent-tinted lock glyph + Lock now), isolated
  Continue Watching / Continue Reading rails (progress cards with ×/long-press remove + Clear),
  Private bookmarks rail (110 dp cards + Clear), jump cards into the three browse tabs, and a
  "Private library tools" management card (both opt-in switches + counted Clear adult history /
  Clear private bookmarks buttons, every destructive path behind `ConfirmRemoveDialog`).
- Header gained the dark/purple **Private** pill plus a Lock-now action (visible when a lock is
  configured and the hub is unlocked); the old top-level search opt-in row moved into the tools
  card, and a second switch for "Include NSFW add-ons in Real 18+" (`autoNsfwFromAddons`) was added
  alongside it.

### Files
`res/drawable/ic_launcher_foreground.xml`, `res/values/colors.xml`,
`res/mipmap-{mdpi,hdpi,xhdpi,xxhdpi,xxxhdpi}/ic_launcher{,_round}.png`,
`data/model/Models.kt` (`isAdult`), `data/repo/CatalogRepository.kt` (row/search scrubs),
`ui/vm/ViewModels.kt` (public filters + hub flows), `ui/screens/home/HomeScreen.kt` (rail/hero guards),
`ui/screens/library/LibraryScreen.kt` (chip removal + grid guards),
`ui/screens/nsfw/NsfwHubScreen.kt` (Private dashboard overhaul),
`app/src/test/.../NsfwIsolationTest.kt` (+7 isolation cases).

### Verification (2026-10-11)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings; one
  new-resource failure fixed in-session: `<circle>` is not a VectorDrawable element).
- `:app:testDebugUnitTest` → **136/136 tests, 0 failures, 0 errors** (+7 new).
- Not pushed; no APK rebuilt this session. Device pass still needed: launcher icon rendering
  (adaptive + legacy, light/dark), public rails showing zero adult posters after adult playback,
  hub Private tab showing that same playback only when unlocked, and the clear/lock management
  paths.

---

## 2ag. Phase 27 (Part B) — Updater UI Revamp with Real-Time Progress Bar (2026-10-11)

**Topic:** replace the updater's generic spinner with a detailed, real-time progress bar.

Status: **implemented and verified.** Files and verification at the end.

### 1. Progress-emitting downloader (`UpdateChecker.kt`)
- New pure-JVM `DownloadProgress(downloadedBytes, totalBytes?)` with `fraction: Float?` (0..1, null when
  the total is unknown/non-positive) and `percent: Int?` (0..100, clamped, rounded), plus
  `formatUpdateBytes()` (MB/KB/B bands) and `downloadProgressLabel()` (`Downloading update… 45%
  (18.2 MB / 40.5 MB)`, degrading to `Downloading update… 18.2 MB` on chunked responses and to a bare
  `Downloading update…` before the first byte).
- New `downloadApkWithProgress(context, url, onProgress)` streams the response in 8 KB chunks,
  `ensureActive()`-checked per chunk so cancellation aborts immediately, deletes any stale partial
  before starting, emits an initial 0-byte snapshot, then throttles to ~1 emission per 100 ms or per
  percent-point (so the dialog recomposes ~10×/s, not per chunk), and deletes the partial on both
  failure and cancellation — rethrowing `CancellationException` explicitly because `runCatching`
  would otherwise swallow it into a silent `null`. The old `downloadApk(context, url)` is kept as a
  progress-blind delegate (still used by the Settings "Check for updates" row).

### 2. Revamped dialog (`MainActivity.kt` → `StartupUpdateCheck` + `UpdateAvailableDialog`)
- `StartupUpdateCheck` now owns `progress: DownloadProgress?` (keyed on the release tag so a new release
  resets it) and the active `downloadJob: Job?`. The Update action launches the job with a progress
  callback writing straight into Compose state; completion clears `downloading`/job state and either
  fires the installer or reports failure, while `CancellationException` parks the dialog back in idle
  with a "Download cancelled." notice instead of dismissing it.
- `UpdateAvailableDialog` gains `progress` + `onCancel` params. While downloading, the notes column adds
  a `LinearProgressIndicator` (determinate via `progress = { fraction }` when the total is known,
  indeterminate otherwise) and the tabular (`tnum`) status line from `downloadProgressLabel()`. The
  Update button stays disabled with its spinner, **"Later" becomes "Cancel"** (aborts the job, partial
  deleted, dialog returns to idle for a retry), and `onDismissRequest` is ignored mid-download so an
  outside-tap/Back can never silently orphan the job.

### Files
`data/integrations/UpdateChecker.kt` (`DownloadProgress`, `formatUpdateBytes`, `downloadProgressLabel`,
`downloadApkWithProgress`, `downloadApk` delegate), `ui/MainActivity.kt` (job + progress state, revamped
dialog), `app/src/test/.../UpdateCheckerTest.kt` (+5 progress cases).

### Verification (2026-10-11)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **129/129 tests, 0 failures, 0 errors** (+5 new).
- Not pushed; no APK rebuilt this session. Device pass still needed: real release-asset download showing
  live percent/bytes, Cancel → idle + retry, installer hand-off after a completed bar.

---

## 2af. Phase 27 (Part A) — UI & Layout Polish Fixes (2026-10-11)

**Topic:** the 4 UI/layout fixes identified in the audit.

Status: **implemented and verified.** Files and verification at the end.

### 1. System navigation-bar padding (pushed destinations)
- **Problem:** bottom content and action rows on pushed destinations (no bottom tab bar) could slide
  under the system gesture/navigation bar — `DetailScreen` (fixed 40 dp bottom), `SourcesScreen` /
  `HomeOrderScreen` (32 dp), `CollectionScreen` (32 dp), `AddonManagerScreen` tabs (16 dp all-round)
  and the route-only `NsfwHubScreen` (24 dp + a `LocalFloatingNavBottomPadding` that is 0 with no bar)
  all used fixed bottom padding with no system inset.
- **Fix:** root `Column`/`Box` of each pushed destination now applies `Modifier.navigationBarsPadding()`
  (import added per file) with an explanatory comment — the list itself keeps its existing content padding
  and the inset is applied outside it, so edge-to-edge artwork/status-bar behaviour is unchanged. Tab
  screens (`Home`, `Search`, `Anime`, `Manga`, `Library`, `Settings`) already clear the bar through
  `LocalFloatingNavBottomPadding` (which sums `FloatingNavBarHeight + 16 dp + navInset + 12 dp`), so they
  needed no change; `FloatingPillNavBar` itself already offsets by `navInset + 16 dp`.

### 2. Detail stats text overflow (`DetailScreen.kt` → `MetaStatsRow`)
- **Problem:** the votes / release-date / status / episode-count row was an unconstrained `Row` with no
  `maxLines`/`overflow` — a long string ran past the screen edge.
- **Fix:** the row is now `Modifier.fillMaxWidth()`; the rating badge keeps its fixed pill, and every
  trailing stat gets `maxLines = 1`, `TextOverflow.Ellipsis` and `Modifier.weight(1f, fill = false)` so
  the set shares the available width and truncates instead of overflowing.

### 3. Dangling "Continue Watching" header (`LibraryScreen.kt` → `ActivityGrid`)
- **Problem:** the header rendered unconditionally while its cards rendered only `if (watched.isNotEmpty())`
  — a user with only manga progress saw an empty "Continue Watching" header above "Continue Reading".
- **Fix:** the header now renders only `if (watched.isNotEmpty() || (watched.isEmpty() && reading.isEmpty()))`
  — i.e. when it has cards or when the whole section is empty and the empty state needs its anchor.

### 4. Poster "×" touch-target overlap (`Components.kt` → `PosterCard`)
- **Problem:** the 48 dp centred hit box at 6 dp inset covered most of the poster's top-left corner and
  swallowed taps meant to open the title.
- **Fix:** hit box tucked into the corner — 40 dp @ 2 dp inset, `contentAlignment = TopStart` with the
  22 dp chip offset 4 dp inside (glyph 12 dp) — so the tappable region hugs the corner instead of
  spreading over the artwork while staying close to the accessibility floor.

### Files
`ui/screens/detail/DetailScreen.kt` (nav padding + stats row), `ui/screens/sources/SourcesScreen.kt`,
`ui/screens/settings/HomeOrderScreen.kt`, `ui/screens/collection/CollectionScreen.kt`,
`ui/screens/addons/AddonManagerScreen.kt`, `ui/screens/nsfw/NsfwHubScreen.kt` (nav padding),
`ui/screens/library/LibraryScreen.kt` (header guard), `ui/components/Components.kt` (× hit area).

### Verification (2026-10-11)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only pre-existing deprecation warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **124/124 tests, 0 failures, 0 errors**.
- Not pushed; no APK rebuilt this session (UI-only pass — device check still needed for gesture-bar
  clearance, stats truncation and the corner hit area).

---

## 2ae. Phase 26 — P0 Bug Fixes & Crash Prevention (2026-10-11)

**Topic:** top 5 critical P0 issues identified in the audit.

Status: **implemented and verified.** Files and verification at the end.

### 1. Fix Manga Chapter Downloads (`MangaDownloadManager.kt`)
- **Instant state insertion & start:** `MangaDownloadManager.enqueue()` previously updated `_downloads` through a private `update(key)` helper that quietly dropped any key not already present in `_downloads.value`. Consequently, newly enqueued chapters were never added to the in-memory state until the app restarted and `loadFromDisk()` ran, and the subsequent `start(key)` immediately bailed out (`_downloads.value[key] ?: return@launch`). `enqueue()` now inserts directly into `_downloads` via `_downloads.update { it + (queued.key to queued) }`, persists metadata, and starts downloading immediately.
- **ENOSPC / disk-full safety:** Wrapped `file.writeBytes(data)` in a `runCatching` block. On disk write failure (e.g. storage full / `ENOSPC`), the partial file is safely cleaned up, the chapter status is marked as `FAILED` with descriptive error details, `notifyFailed` alerts the user, and metadata is persisted without crashing the coroutine or application.

### 2. Guard Detail Page Against Missing/Failed TMDB Keys (`ViewModels.kt` & `TmdbClient.kt`)
- **Graceful fallback on missing/blank TMDB key or network failure:** In `DetailViewModel`, `buildSeasons()` calls inside `viewModelScope.launch` (both in `load()` and `fillBuiltInEpisodes()`) are wrapped in `runCatching` blocks with a dedicated `fallbackBasicSeasons()` fallback. If `BuildConfig.TMDB_API_KEY` is missing or blank, or if network requests throw `IOException`, or if `MetadataRepository.detail(item)` returns null, the detail page synthesizes basic metadata from `item` and falls back to Stremio-style episode grouping instead of crashing.
- **Safe JSON array & object parsing:** Added `optJsonArray()` and `optJsonObject()` helpers in `TmdbClient.kt`. Replaced unsafe direct `getAsJsonArray()` / `getAsJsonObject()` calls across all endpoints (`results`, `genre_ids`, `tv_results`, `movie_results`, `credits`, `cast`, `crew`, `seasons`, `episodes`, `videos`, `alternative_titles`, `recommendations`) to guarantee malformed or mismatched JSON types never trigger fatal `ClassCastException` or `IllegalStateException` crashes.

### 3. Fix Video Download Cache Key (`VideoDownloadManager.kt`)
- **Media3 custom cache key alignment:** Added `.setCustomCacheKey(id)` to Media3 `DownloadRequest.Builder` in `VideoDownloadManager.buildRequest()`. Previously, Media3 defaulted the cache key to the stream URL while `DownloadExporter.export()` queried `SimpleCache.getCachedSpans(id)` by the download UUID `id`, causing `getCachedSpans()` to return empty spans and leaving exported files empty. Aligning `customCacheKey` with `id` allows `DownloadExporter` to stream downloaded cache spans into the user's custom storage folder.

### 4. Deduplicate Stremio Catalog Rails (`StremioClient.kt`, `CatalogRepository.kt`, `Components.kt`)
- **Deduplication across catalog fetch and rails:** Applied `distinctBy { it.key }` across `StremioClient.fetchCatalog()`, `CatalogRepository.stremioRowsFor()`, `stremioNsfwRows()`, `safeRow()`, `nsfwRow()`, and `loadCatalog()`.
- **UI Compose crash prevention:** Added `val uniqueItems = items.distinctBy { it.key }` before `itemsIndexed()` in `MediaRow` (`Components.kt`). This eliminates duplicate item keys from add-on feeds and completely prevents fatal `IllegalArgumentException: Key was already used` crashes when rendering `LazyRow` rails.

### Files
`data/download/MangaDownloadManager.kt`, `data/remote/TmdbClient.kt`, `ui/vm/ViewModels.kt`, `data/download/VideoDownloadManager.kt`, `data/remote/StremioClient.kt`, `data/repo/CatalogRepository.kt`, `ui/components/Components.kt`, `app/src/test/java/com/moovio/app/Phase26P0FixesTest.kt`.

### Verification (2026-10-11)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (exit status 0).
- `:app:testDebugUnitTest --rerun-tasks` → **124/124 tests, 0 failures, 0 errors** (17 test result classes; +5 new tests in `Phase26P0FixesTest.kt`).

---

## 2ad. v1.4.1 — switch visibility, rail density & full NSFW search (2026-10-08)

**Topic:** three device-reported fixes.

Status: **implemented and built.** Files and verification at the end.

### 1. Toggles looked invisible when off
- Material3's `Switch` defaults paint the **unchecked** track with `surfaceVariant` and the thumb
  with `outline`; on this theme those sit within a hair of the card behind them (and vanish entirely
  under True AMOLED black), so a switch that was *off* read as invisible. New shared **`NovaSwitch`**
  + `novaSwitchColors()` in `ui/components/Components.kt` give the off state a visible track
  (`outline` @ 45 %), thumb (`textSecondary`) and border, and a solid accent track when on.
- Every switch now uses it: Settings (`SwitchRow`, `ScrobbleRow`), the NSFW Hub's "Show NSFW in
  general search" opt-in, the Sources dashboard, the Add-on Manager, and the player's Audio boost /
  Normalize volume rows.

### 2. Browse → Anime / NSFW rails showed ~2 cards where Search showed 3
- `AppSpacing.railCard` was **150 dp**, which fits only about two cards across a 360 dp phone
  (`(360 − 32) / 150 ≈ 2.2`) while the Search/grid posters sit at ~101 dp three-across — so the
  Browse → Anime and NSFW rails read as "2 rows" next to Search's 3. Lowered to **100 dp** so every
  rail (Home, Browse → Manga/Anime, NSFW) matches the Search grid's 3-across density on phones
  (`3·100 + 2·12 ≤ 360 − 32`). It is a shared token, so all rails stay consistent.

### 3. NSFW search was capped at the first source that answered
- `CatalogRepository.searchNsfwAnime` returned as soon as AniList answered, so an NSFW search was
  capped at one source's page and never reached Jikan or the installed NSFW add-ons. It now queries
  **AniList + Jikan + NSFW add-ons concurrently and merges** the results (de-duplicated, built-ins
  first), with each hop still bounded by its own timeout — so the search costs the slowest hop, not
  their sum, and returns everything the sources have.
- Per-source caps raised so nothing is cut short: AniList `perPage` 40 → **50** (its maximum); Jikan's
  adult search now pages twice (25 → up to **50**, with each page fail-safe so a 504 on page 2 can't
  discard page 1); NSFW Manga search raises MangaDex's `limit` to **100** (its maximum).

### Files
`ui/components/Components.kt`, `ui/theme/Tokens.kt`, `ui/screens/settings/SettingsScreen.kt`,
`ui/screens/nsfw/NsfwHubScreen.kt`, `ui/screens/sources/SourcesScreen.kt`,
`ui/screens/addons/AddonManagerScreen.kt`, `ui/player/PlayerActivity.kt`,
`data/repo/CatalogRepository.kt`, `data/remote/AniListClient.kt`, `data/remote/JikanClient.kt`.

### Verification (2026-10-08)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (exit status 0; only pre-existing deprecation
  warnings).
- `:app:testDebugUnitTest` → **119/119, 0 failures, 0 errors**.
- `:app:assembleRelease` → **BUILD SUCCESSFUL** (signed); `dist/NovaStream.apk` refreshed
  (38,330,038 bytes, SHA-256 `81078DB4…B290`); the APK was verified to carry `versionName` **1.4.1**
  (and no `1.4.0`).
- Device pass still needed for the visuals (switch contrast, rail density) and the live NSFW search.

---

## 2ac. QoL follow-up — tabular figures finished & glass elevation (2026-10-08)

**Topic:** close the last two open items from the §2d "premium-feel" backlog — finish the
app-wide tabular-figures sweep (#23) and build the glass elevation system (#17).

Status: **implemented and built.** Files and verification at the end.

### 1. Tabular figures, finished (#23)
- New shared token: `val TextStyle.tnum` in `ui/theme/Tokens.kt` (`copy(fontFeatureSettings =
  "tnum")`). It is a no-op for word text, so it is safe on mixed labels.
- Applied it everywhere numeric text was still proportional:
  - **Library stats dashboard** — the `StatCard` headline value and the top-genre counts.
  - **Library download rows** — manga %/bytes and video %/bytes/speed/ETA (downloading, completed
    and paused) now go through the shared token instead of an inline copy.
  - **Settings** — `SettingsRow`/`SwitchRow` subtitles (covers the byte totals and the
    "N MB" cache size), the playback-speed `Nx` label and the subtitle-size `N%` label.
  - **Player stats overlay** — the `StatsLine` value column (FPS / buffer % / dropped frames),
    which updates every tick.
  - (`RatingBadge` / `ResolutionBadge` already had it.)

### 2. Glass elevation system (#17)
- `GlassSurface` now renders **two elevation cues**: a real drop shadow behind the shape
  (`Modifier.shadow`, 10 dp dark / 6 dp light, with softer ambient/spot colours in light mode so it
  never reads as a smudge) and a **top-edge highlight hairline** — a transparent→highlight→transparent
  horizontal gradient drawn along the top rim after the content, as if light catches the edge. The
  surface was previously only a flat gradient fill + 1 dp outline.
- **M3 `surfaceContainer*` roles** are now mapped in `NovaStreamTheme` (both schemes):
  `surfaceContainerLowest/Low/`/`High/Highest` ladder out of the app's own `background` / `surface`
  / `surfaceElevated` (higher tiers are `lerp`ed a few percent toward white in dark, black in
  light). Stock Material3 components that pick a container by elevation now sit in the app's tonal
  family instead of falling back to the light baseline.

### Files
`ui/theme/Tokens.kt`, `ui/theme/Theme.kt`, `ui/components/Components.kt`,
`ui/screens/library/LibraryScreen.kt`, `ui/screens/settings/SettingsScreen.kt`,
`ui/player/PlayerActivity.kt`.

### Verification (2026-10-08)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (exit status 0; only the repo's pre-existing
  deprecation warnings). Confirms the `surfaceContainer*` params exist on this Material3 (Compose
  BOM 2024.09.02).
- `:app:testDebugUnitTest` → **119/119, 0 failures, 0 errors**.
- Not pushed; no APK rebuilt this session.
- Device pass still needed for the visuals (shadow weight in light/dark, highlight visibility,
  tabular alignment) — none of it renders headlessly here.

---

## 2ab. QoL polish pass — empty states, poster loading, reduced motion, tabular figures (2026-10-08)

**Topic:** clear four of the open items from the §2d "premium-feel" backlog (#20, #21, #22 and a
first slice of #23), all inside the shared component layer so every screen benefits.

Status: **implemented and built.** Files and verification at the end.

### 1. Empty states get a glow + a call to action (#22)
- `EmptyState` now draws a soft **accent radial halo** behind its icon (a 140 dp accent→transparent
  gradient) and takes optional `actionLabel` / `onAction` arguments that render a filled pill
  button. Previously every empty screen was a dead end — one icon in a rounded square.
- **"Retry"** is wired wherever a refresh exists: Home (`error` + empty-feed states →
  `HomeViewModel.refresh`), Browse → Anime (`AnimeViewModel.refresh`), and Browse → Manga
  (`MangaViewModel.refresh`).

### 2. Poster placeholder / error / crossfade (#20)
- `PosterCard`'s artwork moved from `AsyncImage` to Coil's **`SubcomposeAsyncImage`**: a loading
  poster now shows the shared `ShimmerBox` sweep instead of a blank dark rectangle (the old
  behaviour while scrolling a grid), and a failed load degrades to the placeholder glyph rather
  than an empty box. Crossfade already comes from the app-wide `ImageLoader` (`NovaApp` sets
  `.crossfade(true)`), so no per-image change was needed.

### 3. Reduced motion (#21)
- New `ui/theme/ReducedMotion.kt` → `rememberReducedMotion()`, read once from
  `Settings.Global.TRANSITION_ANIMATION_SCALE` (the closest cross-version signal to
  `prefers-reduced-motion`; true when animations are disabled).
- `HomeScreen.FeaturedCarousel` no longer auto-advances when reduced motion is on (`LaunchedEffect
  (items, reducedMotion)` bails out), and `Components.StaggeredEntrance` snaps its fade straight to
  the final frame instead of delaying + animating each row item.

### 4. Tabular figures (#23, partial)
- Applied `fontFeatureSettings = "tnum"` to the Library download readouts (manga %/bytes and video
  %/bytes/speed/ETA for downloading, completed and paused states) so ticking counters no longer
  re-flow their neighbour text. `RatingBadge` / `ResolutionBadge` already had it; other numeric text
  (Settings byte rows, the stats dashboard) is left for a follow-up sweep — hence #23 stays open.

### Files
`ui/components/Components.kt`, `ui/theme/ReducedMotion.kt` (new),
`ui/screens/home/HomeScreen.kt`, `ui/screens/anime/AnimeScreen.kt`,
`ui/screens/manga/MangaScreen.kt`, `ui/screens/library/LibraryScreen.kt`.

### Verification (2026-10-08)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (exit status 0; only the repo's pre-existing
  deprecation warnings).
- `:app:testDebugUnitTest` → **119/119, 0 failures, 0 errors**.
- Not pushed; no APK rebuilt this session.
- Device pass still needed for the visuals (shimmer timing, halo contrast, reduced-motion behaviour)
  since none of it can be rendered headlessly here.

---

## 2aa. Phase 25 — GitHub auto-update dialog & repository fallback (2026-10-08)

**Topic:** make the built-in updater work with zero configuration and surface a launch-time dialog
when a newer GitHub release exists.

Status: **implemented and built.** Files and verification at the end.

### 1. Default repository fallback
- `UpdateChecker` gained `const val DEFAULT_REPO = "speckle-hub/nexusstream"` and
  `fun resolveRepo(configured: String) = configured.trim().ifBlank { DEFAULT_REPO }`; `check(repo)`
  now queries the resolved repo and builds `pageUrl` from it, so a blank/absent setting no longer
  disables the feature.
- `SettingsScreen`'s **Check for updates** action dropped its `if (updateRepo.isBlank())` guard, and
  the **Update repository** row now displays `UpdateChecker.resolveRepo(updateRepo)` (the effective
  source: the user's repo, or the built-in default). The repository editor itself is unchanged.

### 2. Startup update dialog
- `MainActivity` composes a new private `StartupUpdateCheck()` **inside** the app-lock gate (so the
  dialog only appears once the app is unlocked). It fires `UpdateChecker.check(...)` from a
  `LaunchedEffect`, i.e. off the startup path — the UI never waits on the network.
- When `ReleaseInfo.isNewerThan(BuildConfig.VERSION_NAME)` is true it shows `UpdateAvailableDialog`:
  an expressive M3 `AlertDialog` (28 dp corner, themed `surfaceElevated`) with a `SystemUpdate`
  icon, **"New Version Available"** + the release tag, and the release-notes body in a
  height-capped, scrollable column.
- **Update** downloads the release's attached `.apk` via `downloadApk` and launches the installer
  via `installApk` (a spinner shows while downloading); if the release has no APK asset the button
  reads **Open release** and opens the GitHub page instead, and a download failure is reported
  inline. **Later** (and dismissing) simply closes the dialog so the user keeps using the app.

### Files
`data/integrations/UpdateChecker.kt`, `ui/MainActivity.kt`,
`ui/screens/settings/SettingsScreen.kt`, `app/src/test/.../UpdateCheckerTest.kt` (new).

### Verification (2026-10-08)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (exit status 0; only the repo's pre-existing
  deprecation warnings).
- `:app:testDebugUnitTest` → **119/119, 0 failures, 0 errors** (6 new `UpdateCheckerTest` cases
  covering the `DEFAULT_REPO` fallback, trimmed configured repos, and `isNewerThan` for
  newer/equal/older/missing-segment tags).
- Not pushed; no APK rebuilt this session.

---

## 2z. NSFW isolation & Browse → Anime (2026-10-08)

**Topic:** split the public Browse experience from adult content. Two device-facing changes: (1)
replace the NSFW segment on `BrowseScreen` with an **Anime** segment, and (2) move all NSFW
surfaces into a dedicated, isolated, route-only hub that general Search and Home never touch.

Status: **implemented and built.** Files and verification at the end.

### 1. Browse → Anime (NSFW segment removed)
- `BrowseScreen`'s segmented control was `Manga | NSFW`; it is now **`Manga | Anime`**, and segment 1
  renders the new `ui/screens/anime/AnimeScreen.kt`.
- `AnimeScreen` serves **only anime-tagged content**: it reuses `CatalogRepository.rowsForSection(
  MediaType.ANIME)` (AniList trending / popular / Action / Romance / Fantasy rails **plus** Stremio
  add-on catalogs mapped to `ANIME`) and `CatalogRepository.search(query, MediaType.ANIME)`. It gets
  the same adaptive grid, shared `SearchField`, long-press `PosterQuickActionsSheet` and
  `AppSpacing.railCard` rail width as the other browse sections.
- New `AnimeViewModel` mirrors `NsfwViewModel`'s search discipline: 350 ms debounce, one in-flight job
  per section (each keystroke cancels the previous), a monotonic generation so only the newest query
  may publish, and a browse-row reload on clear so an empty screen can recover. The `embedded` flag
  suppresses its own title row when hosted by Browse.
- The dead private `SegmentedControl` composable in `BrowseScreen.kt` (superseded by
  `PillSegmentedControl`) was deleted along with its now-unused imports.

### 2. Isolated NSFW Hub
- `ui/screens/nsfw/NsfwScreen.kt` was renamed/reworked into **`NsfwHubScreen.kt`** (same package, so
  `NsfwLockScreen`/`hashPin` are reused). It keeps the three sub-tabs (NSFW Anime / NSFW Manga / Real
  18+), the adaptive grid, the per-source error notice and the PIN/biometric lock — but now has its
  own **Back** affordance and is a **pushed, route-only** destination.
- `Routes.NSFW` is no longer mounted by any tab. Its `NovaNav` entry now uses the push transitions
  (`detailEnter`/`detailExit`) so it reads as a screen opened from elsewhere; the bottom bar stays
  hidden (it is not a tab route). The **only** entry point is a new Settings row
  **Content Restrictions → Open NSFW Hub** (`nav.navigate(Routes.NSFW) { launchSingleTop = true }`).
- The Settings group previously titled "NSFW" was renamed **"Content Restrictions"** and now leads
  with that hub row; the settings filter still finds it by "NSFW" (the group's row labels include
  it). The hub continues to enforce `nsfwLock` + PIN/biometric before rendering anything.

### 3. Keeping NSFW out of general Search / Home
- The leak was in add-on search: `CatalogRepository.globalSearch()` queried **every** installed
  add-on (including NSFW ones), and `searchInternal()` for a SFW section passed `nsfwOnly = false`,
  so adult add-on catalogs could surface in a normal search.
- New `SettingsStore` preference **`nsfwGlobalSearch`** (default **false**), exposed in the hub as a
  **"Show NSFW in general search"** switch. `globalSearch()` now filters NSFW add-ons out unless it
  is on; `addonSearch()` gained an `includeNsfw` argument so SFW sections query only non-NSFW add-ons
  by default (NSFW sections remain NSFW-only). The Home feed already excluded NSFW add-ons
  (`stremioRowsFor` filters `!it.nsfw`) and uses SFW AniList/TMDB rails, so it was already clean —
  no change needed there.
- Library's NSFW **filter chip** is untouched: it only filters the user's own favourites / history /
  downloads, and is not a discovery or recommendation surface.

### Files
`ui/screens/browse/BrowseScreen.kt`, `ui/screens/anime/AnimeScreen.kt` (new),
`ui/screens/nsfw/NsfwHubScreen.kt` (replaces `NsfwScreen.kt`), `ui/nav/NovaNav.kt`,
`ui/screens/settings/SettingsScreen.kt`, `ui/vm/ViewModels.kt` (`AnimeViewModel` +
`SettingsViewModel.nsfwGlobalSearch`), `data/repo/CatalogRepository.kt`, `data/local/SettingsStore.kt`.

### Verification (2026-10-08)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only the repo's pre-existing deprecation
  warnings).
- `:app:testDebugUnitTest` → **113/113, 0 failures, 0 errors** (15 result files).
- Not pushed; no APK rebuilt this session (per the user's instruction to compile + test only).

---

## 2c. Phase 7 — UI refinement (P0 foundations + P1 polish)

**Topic: make the app read as premium.** Driven by a full audit of the UI layer
(`ui/theme`, `ui/components`, `ui/nav` and all nine screens). The app already had a strong dark
palette, dynamic poster palettes and a polished hero; what was missing was a *system*.

### P0 — foundations
- **Design tokens (`ui/theme/Tokens.kt`).** New `AppSpacing` (`screen` 16 dp, `item` 12 dp,
  `section` 24 dp, `inset` 12 dp, `posterMin` 108 dp) and `Motion` (`FAST` 150, `MEDIUM` 250,
  `SLOW` 400, `PRESS` 120, `PRESS_SCALE` 0.96). Screens were hardcoding their own gutters, gaps
  and `tween(...)` durations, which is why the rails and grids never quite lined up and why
  transitions felt unrelated to each other.
- **Adaptive poster grids.** `CollectionScreen` used `GridCells.Fixed(3)`; `LibraryScreen` and
  `SearchScreen` faked grids with `chunked(3)` + `Row` + `weight(1f)`. So a 7" phone and a tablet
  got the same three columns, the same poster was a *different size on every screen*, and the last
  row of each grid had phantom `Spacer(weight(1f))` fillers. All three now use
  `GridCells.Adaptive(minSize = AppSpacing.posterMin)`.
  - `LibraryScreen` became a **single `LazyVerticalGrid`** with `GridItemSpan(maxLineSpan)` headers
    and download rows (a nested vertical grid inside a `LazyColumn` has unbounded height and
    crashes). `GridRow` is deleted; scroll state is now `rememberLazyGridState()`.
- **Touch targets.** `PosterCard`'s remove `×` was a **24 dp** clickable and Continue Watching's was
  26 dp — well under the 48 dp accessibility floor for a *destructive* action. Both now wrap a
  24 dp visual chip in a 48 dp touch target, so the artwork stays covered as little as possible
  while the tap area meets the minimum.
- **Contrast.** `NsTextTertiary` was `#6A6A7A` (~3.5:1 on `NsBackground`) — below WCAG AA 4.5:1 for
  body text, and it's used for the metadata line under *every* poster. Now `#8A8A9C` (~5.1:1);
  the light-mode tertiary moved `#8888A0` → `#6E6E85`.
- **Press feedback.** `PosterCard` and `GlassSurface` had **no press state at all**, so the whole
  app felt inert to tap. Both now shrink to 0.96× while held via a `MutableInteractionSource` +
  `animateFloatAsState`. The scale is applied in the draw phase (`Modifier.scale`), so pressing
  never causes a layout shift.
- **Real shimmer.** `ShimmerBox` pulsed a flat block's alpha, which reads as "nothing is happening"
  rather than "something is loading". It now sweeps a `linearGradient` highlight across the box on a
  1200 ms `LinearEasing` loop. Added `PosterCardSkeleton` (artwork + title + meta, matching
  `PosterCard`'s real layout) so skeletons don't visibly reflow into results.

### P1 — typography + search
- **Full type scale.** `NovaTypography` defined only 8 of M3's roles and set **no tracking and no
  line height**. Consequence: screens bolted on `fontWeight = FontWeight.Bold` at ~15 call sites,
  and `MaterialTheme.typography.labelSmall` (the hero's "FEATURED") silently fell back to M3
  defaults — the classic "this looks like a sample app" tell. Now all 15 roles are defined with
  per-size tracking (tight as type grows: `-0.8` sp at 36 sp; wide on uppercase micro-labels:
  `+0.8` sp at `labelSmall`), explicit line heights (~1.45× on body copy), and the redundant
  `FontWeight.Bold` overrides were stripped from the screens so weight is token-controlled.
  `RatingBadge` uses `fontFeatureSettings = "tnum"` so rating digits don't jitter.
- **Custom search field.** The raw `OutlinedTextField` was the most default-looking component in the
  app: hard 1 dp outline, no fill, no clear affordance. Replaced with a new `SearchField`
  (filled `surfaceElevated`, 16 dp pill, hairline border, `BasicTextField`, focus-tinted search
  glyph, 36 dp circular clear button that only appears when there's something to clear, and a
  pulsing accent dot while the debounced query is in flight).
- **Search suggestions.** The empty state was one centred "Start typing" block — a dead end. It now
  offers **recent searches** (persisted) plus curated **trending terms** as chips.
- **Recent-search persistence.** New `RECENT_SEARCHES` preference in `SettingsStore` (newline-joined,
  newest-first, capped at `MAX_RECENT` 8, minimum `MIN_SEARCH_LENGTH` 2 characters). It's written by
  `SearchViewModel.search(q, remember = true)`, which only the chip taps and initial query use — live
  keystrokes pass `remember = false` so pausing mid-word doesn't pollute the list.
- **Search results grid.** Results moved from `chunked(3)` to the adaptive grid, and the loading
  state from a centred "Searching…" *text* to nine `PosterCardSkeleton`s, so the grid doesn't jump
  when results land.

### Files
`ui/theme/Tokens.kt` (new), `ui/theme/Theme.kt`, `ui/theme/Color.kt`,
`ui/components/Components.kt`, `ui/screens/search/SearchScreen.kt` (rewritten),
`ui/screens/library/LibraryScreen.kt`, `ui/screens/collection/CollectionScreen.kt`,
`ui/screens/home/HomeScreen.kt`, `ui/screens/detail/DetailScreen.kt`, `ui/screens/manga/MangaScreen.kt`,
`ui/screens/nsfw/NsfwScreen.kt`, `ui/screens/settings/SettingsScreen.kt`,
`ui/screens/addons/AddonManagerScreen.kt`, `ui/vm/ViewModels.kt`, `data/local/SettingsStore.kt`.

### Verification
`:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` → **30 tests / 0 failures** ✅ ·
`:app:assembleDebug` + `:app:assembleRelease` ✅ (44 MB debug / 37 MB release) ·
`dist/NovaStream.apk` refreshed. Needs a device pass (§7 item 14).

### Still on the table (not yet done — see §2d for the audit)
Real shadow + top-highlight elevation on glass, the 6→5 tab restructure (Manga/NSFW → Browse), and
the Detail screen's CTA hierarchy.

---

## 2v. Phase 21 — NSFW/Browse card alignment & the Sources crash root cause (2026-10-06)

**Topic: fix the two device-reported issues from a physical inspection** — (1) NSFW / Browse poster
cards looked narrow with wide blank side margins next to the Home feed, and (2) Settings →
Sources & Network → **Sources dashboard** still crashed on entry even after Phase 20's fix.

Status: **implemented and built.** Source list and verification at the end.

### 1. NSFW / Browse layout padding & width alignment
- **What actually differed from Home.** Comparing the three screens modifier-for-modifier:
  `HomeScreen` renders its rails with `MediaRow(…, cardWidth = 150)`, while `NsfwScreen` and
  `MangaScreen` (the two Browse segments) called `MediaRow(row.title, row.items, onClick)` and
  therefore inherited the component's **132 dp** default. Same component, same screen, two card
  sizes — that is the "narrow cards" half of the report. The gutters themselves were already
  consistent (Home rails and the NSFW grid both use `AppSpacing.screen` = 16 dp).
- **Fix — one token, every rail.** `AppSpacing.railCard` = **150 dp** is now the single source of
  rail card width; `HomeScreen`, `MangaScreen` and `NsfwScreen` all pass it explicitly, so a rail
  card is identical on Home, Browse → Manga and Browse → NSFW (and no screen hard-codes a width
  again).
- **"Wide side margins" verified as a non-issue.** The NSFW results grid uses
  `GridCells.Adaptive(minSize = AppSpacing.posterMin)`; against the Compose 1.7.0 sources
  (`LazyGridDsl.kt` → `calculateCrossAxisCellSizes` → `calculateCellsCrossAxisSizeImpl`) every cell
  gets `availableSize − padding` distributed evenly, so the grid **fills the viewport exactly**
  with only the intended 16 dp gutters and 12 dp gaps — there is no centred/inset container and no
  max-width clamp anywhere on the NSFW or Browse screens. The remaining literals on NSFW (the
  search field's and the source-notice banner's `16.dp`) now use `AppSpacing.screen` too, so all
  three screens compute their layout from the same tokens.

### 2. Sources dashboard crash — root-caused and fixed
- **Phase 20's theory was wrong.** The early `return@Column` shape was removed, and the screen
  still crashed, so that was not the cause. (It is also not a crash mechanism: `Column`'s measure
  policy hands a non-weighted child `mainAxisMax = constraints.maxHeight` — bounded — so the
  `Column { header; Lazy*(fillMaxSize()) }` shape every grid screen uses is safe; verified against
  `foundation-layout-1.7.0-sources`.)
- **The real cause — a Gson null-field decode.** `ExtensionStore.list()` used
  `Gson().fromJson(json, Array<ExtensionDescriptor>::class.java)`. Gson has no concept of Kotlin
  nullability and instantiates the data class through `Unsafe` (it has no no-arg constructor), so a
  JSON entry **missing a field** decodes that field to a silent **`null` in a Kotlin non-null
  slot**. The dashboard then did two things with those values and both end the process on entry:
  `Text(desc.name, …)` passes null into the compiler's `checkNotNullParameter` intrinsic → **NPE**,
  and `items(installed, key = { it.id })` hands `LazyColumn` a **null key** (and `IllegalArgumentException`
  on a duplicate). An `extensions.json` written by an older build, or truncated mid-write, is
  enough. This is *exactly* the bug class Phase 13 fixed for the Library.
- **Fix — a tolerant, per-entry codec.** New `data/ext/ExtensionCodec.kt` (pure JVM, no Android
  types, so it is directly unit-testable — the same shape as `LibraryCodec`): parses element-wise,
  **drops** any entry that cannot load anyway (missing/blank `id`, `entryClass` or `apkPath`),
  **defaults** `name` to the id instead of trusting it, keeps `version` optional, **de-dupes ids**
  (a duplicate is a lazy-list key collision), tolerates a single bare object, and returns an empty
  list for a truncated/corrupt/unparseable file instead of throwing. `ExtensionStore.list()` /
  `save()` now go through it, and `ExtensionRepository.installed()` is additionally wrapped so it
  can never throw while a screen is composing.
- **Belt-and-braces on the screen.** `SourcesScreen` now reads the list through a `runCatching`
  fallback (an unreadable snapshot degrades to the empty state instead of an uncaught exception),
  filters blank ids and de-dupes before keying the list, which is the "safe fallback state" the
  screen needs on entry.
- **Tests.** `ExtensionCodecTest.kt` — 8 cases: missing `name` defaults to the id (the original
  crash), explicit `null` fields treated as absent, optional `version`, one bad entry dropped while
  the rest survive, duplicate ids keep the first, hostile payloads (`{not json`, `"s"`, `42`,
  `[1,2,3]`, `{}`) decode to an empty list, a bare object is tolerated, and a full round-trip.

### Files
`ui/theme/Tokens.kt` (`AppSpacing.railCard`), `ui/screens/home/HomeScreen.kt`,
`ui/screens/manga/MangaScreen.kt`, `ui/screens/nsfw/NsfwScreen.kt`,
`data/ext/ExtensionCodec.kt` (new), `data/ext/ExtensionRepository.kt`,
`ui/screens/sources/SourcesScreen.kt`, `app/src/test/java/com/moovio/app/ExtensionCodecTest.kt` (new).

### Verification (2026-10-06)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only the repo's pre-existing deprecation
  warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **111/111, 0 failures, 0 errors** (15 result files; the
  8 new cases are `ExtensionCodecTest`).
- `:app:assembleRelease` → **BUILD SUCCESSFUL**, signed.
- `dist/NovaStream.apk` refreshed: **38,264,498 bytes**, SHA-256
  `a2007861dce7fc1697c16fe83c469cfddc1916b4b1e44dcea73664c8f59c8c10`; `ExtensionCodec` (7 occurrences),
  `railCard` and `SourcesScreen` (32) verified in the release dex (`classes3.dex`).
- **Device pass still required — §7 item 31.** No device or emulator was available in this session
  (no AVDs installed, no `adb` target attached), so the crash fix is a static + unit-test result:
  the decode path is proven, but the only way to confirm the screen renders on real storage with a
  pre-existing malformed `extensions.json` is hardware.

---

## 2w. Source hygiene — dead seeds fixed & dead hosts retired (2026-10-06)

**Topic:** act on the §3 live probe — three of its findings were network-independent (a dead seed
repo, a source pointed at two 404 search paths, and dead hosts), so they were fixed now.

Status: **implemented and built.** Files and verification at the end.

### 1. Hexated provider repo re-pointed
- The seeded `CloudStream (hexated)` repo pointed at
  `hexated/cloudstream-extensions-hexated/builds/repo.json`, which **404s on every branch**
  (`builds`, `master`, `main`). Live replacement: **`JoeTinnySpace/cloudstream-extensions-hexated`
  `builds/repo.json`** (200, a manifest with `pluginLists` → plugins.json), the hand-off mirror for
  the retired org.
- `AddonStore.Defaults.repos` now seeds the live URL, and a new `Defaults.retiredRepoUrls` plus a
  matching prune in `AddonRepository.ensureDefaultRepos()` removes the dead URL from **existing**
  installs (`ensureDefaultRepos` only merges *missing* defaults, so the dead entry would otherwise
  linger). This mirrors the existing `retiredHosts` add-on migration.

### 2. Haho re-pointed to its live search and made playable
- Haho's `HahoSource` used `GET /api/search?v=` then `GET /search?v=` — **both 404**. The live search
  is **`GET /anime?q=`**, whose `Anime Index` list (`ul.loop.anime-loop`) returns **series** cards
  (`<a href="…/anime/{slug}" title="{Title}">`). Because `HentaiSource.search` must return
  *episodes*, the source now expands each of the top **3** series in parallel by fetching
  `/anime/{slug}` and reading its `episode-loop`
  (`<a href="…/anime/{slug}/{n}" title="Watch … Ep. {n} …">`) into numbered episodes.
- Streams resolve from the episode page: `/anime/{slug}/{n}` → its single `<iframe src="…/embed?v=…">`
  → the embed's `<video><source src="https://s1.filegasm.com/…?download_token=…" type="video/mp4">`.
  Those CDN URLs are **extension-less**, which the shared extension matcher missed, so
  `HentaiTubes.mediaUrls` now also extracts `src` from `<source … type="video/…">` tags (`video/`
  only, so a `text/vtt` subtitle can't sneak in). Haho then flows through the existing
  `resolvedStreams` two-hop iframe path.
- (The `/anime/{slug}/{n}?v=` links on the search page are the "Latest Videos" sidebar, not results —
  the page's own `Anime Index` badge counts only the series matches, so they are correctly ignored.)

### 3. Dead hosts retired from the registries
- Removed from `AdultSources.all`: **`HpjavSource`** (`hpjav.tv` DNS-dead) and **`AvgleSource`**
  (`api.avgle.com` 520 origin down).
- Removed from `HentaiSources.all`: **`HentaigasmSource`** (30 s timeouts) and **`HanimeSource`**
  (`search.htv-services.com` DNS-dead and its HTML page a JS shell).
- The source objects and their parser tests are kept — the fix is registry-level, so a host that
  comes back is a one-line re-register — but they no longer run on every load, so the per-source
  error notices stop.

### Files
`data/local/AddonStore.kt`, `data/repo/AddonRepository.kt`, `data/hentai/HahoSource.kt`,
`data/hentai/HentaiTubes.kt`, `data/adult/AdultSources.kt`, `data/hentai/HentaiSources.kt`,
`app/src/test/java/com/moovio/app/AdultSourcesExtraTest.kt`,
`app/src/test/java/com/moovio/app/HentaiSourcesExtraTest.kt`.

### Verification (2026-10-06)
- Live probes: hexated old URL **404** / `JoeTinnySpace` **200**; Haho `GET /anime?q=test` **200**
  with 2 series cards (`wmisaeil`, `g1y64web`), and a series page listing its numbered episodes
  (`g1y64web`: 6); the episode page carries an `/embed?v=` iframe whose page holds 4
  `<source type="video/mp4">` filegasm URLs. Dead: `hpjav.tv` + `search.htv-services.com` DNS-fail,
  `api.avgle.com` no response, `hentaigasm.com` timeout.
- `:app:compileDebugKotlin` → BUILD SUCCESSFUL (only pre-existing deprecation warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **113/113, 0 failures, 0 errors** (15 result files; the
  Haho JSON fixture became two HTML tests, plus a `<source type="video/…">` extractor case).
- `:app:assembleRelease` → BUILD SUCCESSFUL, signed; `dist/NovaStream.apk` refreshed
  (**38,280,882 bytes**, SHA-256
  `52cef60411d1fa2b7b0fa03666671deef0679a29bb9394e1aeef577e50a2ec9d`); `JoeTinnySpace` /
  `retiredRepoUrls` verified in the release dex.
- **Device pass still required — §7 items 21/23/31.** The Haho chain is verified from the sandbox
  against live markup, but whether ExoPlayer plays the `filegasm` CDN URLs (Referer/token) is hardware.

---

## 2y. Public GitHub release + the NexusStream website (2026-10-07)

**Topic: ship the source-neutral app to a public repo and give it a marketing site.** Two
deliverables: the GitHub publication itself, and `website/` — a Vercel-deployed landing page.

Status: **both live.** Verification at the end.

### 1. GitHub publication
- **Pre-flight hygiene (done before pushing):** verified `keystore/`, `keystore.properties` and
  `local.properties` are untracked and have never been in history; the only secret-ish values in
  the tree are the embedded TMDB key (owner decision: keep — free read-only key, extractable from
  the APK anyway) and the *old* README's keystore password in history (harmless — the `.jks`
  itself is not and must never be committed). README rewritten for NexusStream (feature set,
  build/signing docs, content-neutrality section, no secrets printed); `.gitignore` gained
  `.kotlin/`.
- **Published:** `https://github.com/speckle-hub/nexusstream` (public). Local `master` → `main`,
  21 commits pushed (`6fcb362` at the time). Repo-local commit identity:
  `nexusstream-dev <nexusstream-dev@users.noreply.github.com>`.
- **Release:** annotated tag `v1.1.0` pushed; GitHub Release `NexusStream v1.1.0` created with
  the signed APK as asset `app-release.apk` (38,280,882 bytes). Direct-download link used by the
  site: `releases/latest/download/app-release.apk`. This also powers the in-app updater
  (`UpdateChecker` reads the repo's latest release).

### 2. The website (`website/`)
- **Stack:** Next.js 15 (App Router, fully static) · TypeScript · Tailwind CSS v3.4 ·
  framer-motion (LazyMotion) · HeroUI `~2.7` · lucide-react · `@designcodeio/threeui` (ThreeUI
  Community, MIT). Aceternity-style components are hand-ported into `components/ui/` (no
  dependency); 21st.dev served as design-pattern reference (its components are prompt/CLI-copied).
- **Sections:** glass navbar · hero (ThreeUI `NebulaBackground` iframe hue-shifted violet +
  Spotlight + Sparkles canvas + grid) with a **pure-CSS phone mockup** of the app's home feed
  (hero banner, poster rails, floating pill nav) and floating notification chips · ecosystem
  marquee (`InfiniteMovingCards`) · bento feature grid (`BentoGrid` + `GlowingCard`, six cards
  with mini CSS mockups: player HUD, manga pages, download bars, source toggles, accent swatches,
  lock) · animated stats (`CountUp`) · HeroUI FAQ accordion · BackgroundBeams download CTA wired
  to the release APK · footer with the content-neutrality notice.
- **Fix during bring-up:** hydration mismatch in the moving-border button — a `<div>` inside
  `<svg>` (invalid nesting) plus SSR/CSR transform drift; rewritten as an SVG-native
  `<m.circle>` beam, mount-gated, unique gradient IDs per instance.
- **Optimization pass:** first-load JS 240 → **216 kB**, page chunk 134 → **92 kB**
  (`LazyMotion domAnimation` + all `motion.*` → `m.*`, `optimizePackageImports`,
  `poweredByHeader` off); Sparkles pauses offscreen (IntersectionObserver) and renders one static
  frame under `prefers-reduced-motion`; Nebula swaps to a static gradient under reduced motion;
  all keyframe animations disabled under reduced motion. SEO pack: `metadataBase`
  (`NEXT_PUBLIC_SITE_URL`, default baked to the deployment URL), Twitter card, SVG favicon
  (the N logo), build-time generated `opengraph-image` (1200×630), `sitemap.xml`, `robots.txt`,
  branded 404.
- **Deployment:** Vercel, root directory `website`, no build-time env needed;
  `NEXT_PUBLIC_SITE_URL` documented in `website/README.md` for canonical/OG/sitemap URLs.
  Initial URL: `https://website-zeta-one-92.vercel.app` (project rename adds a prettier domain
  alongside the original — re-point the fallback if adopted). Every push to `main` touching
  `website/` redeploys.

### Files (new)
`website/` — `package.json`, `next.config.mjs`, `tailwind.config.ts`, `tsconfig.json`,
`postcss.config.mjs`, `app/{layout,page,providers,globals.css,icon.svg,opengraph-image.tsx,
sitemap.ts,robots.ts,not-found.tsx}`, `components/ui/*` (spotlight, sparkles, nebula, bento-grid,
glowing-card, moving-border, infinite-moving-cards, text-generate-effect, background-beams,
count-up), `components/PhoneMockup.tsx`, `components/sections/*` (Navbar, LogoMark, Hero,
EcosystemMarquee, Features, Stats, Faq, DownloadCta, Footer), `lib/utils.ts`, `README.md`.

### Verification (2026-10-07)
- `npm install` ✅ (HeroUI pinned `~2.7.11` — 2.8 bundles Tailwind v4 types/runtime, incompatible
  with the Tailwind v3.4 setup; see §8).
- `next build` ✅ — all routes prerendered static (`/`, `/_not-found`, `/icon.svg`,
  `/opengraph-image`, `/robots.txt`, `/sitemap.xml`); first-load JS 216 kB.
- Dev-server smoke: page renders, hydration error fixed, Download button fetches the release APK ✅.
- Vercel deploy ✅ (owner-verified in the dashboard).

---

## 2x. Source neutrality — public-release prep (2026-10-07)

**Topic: make the public GitHub repo defensible — the app must not ship pointing at
infringing-capable sources.** The player/extension engines stay (neutral technology, like a
browser or VLC's protocol support); everything the app *preinstalls or recommends* is now legal.

Status: **implemented and built.** Files and verification at the end.

### What was removed
- **All 7 seeded provider repositories** (`AddonStore.Defaults.repos` → empty): CloudStream
  Phisher / Hexated (JoeTinnySpace mirror) / community, Aniyomi yuzono / official, Mihon/Keiyoushi
  pb + json. The repo *clients* (`CloudStreamRepo`/`AniyomiRepo`/`KeiyoushiRepo`) stay — they parse
  formats for repos the user adds.
- **Seeded stream add-ons** (`Defaults.streamAddons` → empty): Torrentio and ThePirateBay+ are no
  longer installed on first launch or re-seeded by the `ensureDefaults()` migration.
- **Recommended stream providers**: the Add-on Manager's one-tap row no longer offers Torrentio /
  ThePirateBay+ / MediaFusion; it now (re)offers only the two seeded legal add-ons (Cinemeta,
  OpenSubtitles v3).
- **Preferred-host ranking** (`Defaults.preferredStreamHosts` → empty): `StreamRanking.rank` keeps
  the mechanism but nothing is preferred by default, so streams rank by quality → seeders.

### What stays (deliberately)
- **Cinemeta** (metadata) and **OpenSubtitles v3** (subtitles) remain seeded — legal services.
- All free-API metadata paths (TMDB / AniList / MangaDex / Jikan / Kitsu), the Media3 player,
  torrent/HLS *protocol* support (user-supplied links), the extension engine, and the built-in
  adult/hentai sources (unchanged per the owner's decision).
- `RepoSyncWorker` and the Add-on Manager browser work for user-added repos.

### Migration for existing installs
- `Defaults.retiredHosts` += `torrentio.strem.fun`, `thepiratebay-plus.strem.fun`,
  `mediafusion.elfhosted.com` → `ensureDefaults()` prunes those add-ons on next launch.
- `Defaults.retiredRepoUrls` += all 7 previously seeded repo URLs → `ensureDefaultRepos()` prunes
  them; with `Defaults.repos` empty, nothing is merged back.

### UI gap closed
The Add-on Manager's provider tabs had **no add-repo UI at all** (repos were only ever seeded), so
removing the seeds would have made CloudStream/Aniyomi/Mihon dead ends. `ProviderTab` now has an
"Add repository URL" field (name derived from the host), a remove action per repo, and an empty
state explaining how to add one.

### Also updated
Detail screen's empty-stream hint (no longer names a bundled provider), `StreamRanking` /
`AddonRepository` / `Models` / `CloudStreamRepo` comments, `StreamRankingTest` (neutral fixture
names), README (new "Content neutrality" section; Sources & extensions rewritten), `.gitignore`
(`.kotlin/`), README build/signing docs for a fresh clone.

### Files
`data/local/AddonStore.kt`, `data/repo/AddonRepository.kt`, `data/repo/StreamRanking.kt`,
`data/model/Models.kt`, `data/remote/CloudStreamRepo.kt`, `ui/screens/addons/AddonManagerScreen.kt`,
`ui/screens/detail/DetailScreen.kt`, `app/src/test/java/com/moovio/app/StreamRankingTest.kt`,
`README.md`, `.gitignore`.

### Verification (2026-10-07)
- `:app:compileDebugKotlin` → BUILD SUCCESSFUL (only pre-existing deprecation warnings).
- `:app:testDebugUnitTest` → **113/113, 0 failures** (StreamRankingTest fixtures renamed after the
  first run caught a space-vs-hyphen matching mistake in the renamed test data).
- `:app:assembleRelease` → BUILD SUCCESSFUL, signed; `dist/NovaStream.apk` refreshed
  (**38,280,882 bytes**, SHA-256 `969D176D…B0A9D`).
- **Device pass:** on next launch an existing install should drop the removed add-ons/repos; a
  fresh install gets only Cinemeta + OpenSubtitles and empty provider tabs with the add-URL field.

---

## 2u. Phase 20 — UI grid, Settings crash, reader repairs & Continue Reading (2026-10-06)

**Topic: fix the eight issues reported after Phase 19** — poster grids collapsing to two columns,
the Recent Searches list disappearing, the floating pill nav hiding itself on scroll, an
over-crowded hero banner, two Settings sub-screens that crash on entry, a clipped accent-color row,
horizontal manga paging that never advances, and manga reading progress that never showed up
anywhere.

Status: **implemented and built.** Source list and verification at the end.

### 1. Adaptive grids back to three columns (Search / NSFW / Browse, Collection, Library)
- **Why two columns appeared.** `GridCells.Adaptive` resolves
  `floor((availableWidth + spacing) / (minSize + spacing))` columns, where `availableWidth` is the
  viewport minus the content padding. With `posterMin = 108.dp`, 16 dp gutters and 12 dp gaps a
  **360 dp** phone — still the single most common logical width — got
  `floor((360 − 32 + 12) / 120) = 2` cards per row, so half the grid's capacity (and a lot of black
  space around the pair) was wasted and the feed needed extra scrolling.
- **Fix.** `AppSpacing.posterMin` is now **88 dp**, which gives
  `floor(340 / 100) = 3` columns at 360 dp, 3 at 320 / 393 / 411 dp (cards ≈ 101–118 dp) and
  4+ on tablets. Every adaptive poster grid inherits the change — `SearchScreen`, `NsfwScreen`,
  `CollectionScreen` and both of `LibraryScreen`'s — instead of each screen hardcoding its own
  cell width.
- **Side padding.** Search / NSFW / Collection already used `AppSpacing.screen` (16 dp) start/end
  padding; `DownloadsGrid` and `ActivityGrid` in the Library had **none**, so their cards ran flush
  into the screen edges while the section headers above them were inset. Both now carry the same
  16 dp gutters.

### 2. Recent Searches restored (SearchScreen)
- **Root cause.** The section was never *removed*: the empty state still renders
  "Recent" → `SuggestionHeader` + `SuggestionWrap` → "Trending" whenever the query field is blank.
  What disappeared was the **data** — `SearchViewModel.search()` only called
  `SettingsStore.recordSearch()` when `remember = true`, which only chip taps and the initial
  pre-filled query pass. Anyone who searches by typing (the normal case) never recorded a single
  term, so `recents` stayed empty and the section never rendered.
- **Fix.** `search()` now records **any query the user actually let run**: the 350 ms debounce plus
  per-keystroke job cancellation already collapses a typed word into one request, so a prefix is
  only recorded if the user deliberately paused on it. Chip taps are still recorded even when they
  find nothing (`remember`). The 2-char minimum, 8-entry cap, de-duplication and newest-first
  ordering in `SettingsStore.recordSearch` are unchanged, as is the display rule.

### 3. Floating pill nav permanently visible
- **Removed** the whole scroll-hide mechanism: `FloatingNavScrollState`, `LocalFloatingNavScroll`
  and `Modifier.floatingNavScroll()` are deleted from `ui/nav/FloatingPillNavBar.kt`, together with
  its `NestedScrollConnection` and the `.floatingNavScroll()` call sites in Home, Browse/Manga,
  Search, Library (×3), NSFW and Settings.
- `NovaNav`'s bar `AnimatedVisibility` is now keyed on `showBar` alone (it still animates in/out
  when a tab destination is entered or left), and the per-route `navScroll.show()` effect is gone.
- `LocalFloatingNavBottomPadding` **stays**: the bar still floats over the content, so the lists
  keep the bottom clearance that stops their last row from being un-tappable.

### 4. Hero banner cleanup (HomeScreen)
- The 2-line overview paragraph is gone from `HeroSlide` — it was the bulk of the text crowding
  the artwork out of the banner.
- `heroMeta` is now just **Type · Year · Rating** (the genre chip was dropped too, per the
  requested "Title, Media Type, Year, Rating badge" set); the `FEATURED` label and the white pill
  CTA remain.
- The CTA is now honest about the destination: **Read** (with the `MenuBook` glyph) for manga
  instead of always promising **Play** — driven by the new `MediaItem.isReadable`.

### 5. Settings sub-screen crash + cold-boot resilience
- **The crash (Sources dashboard / Home row order).** Root-caused by static analysis (no device or
  emulator was available in this session, per instruction): those two screens were **the only two
  in the whole app** that left the root `Column { … }` content lambda with an early
  `return@Column` after drawing their empty state — every other screen branches with `if/else` or
  `when`, and both of these screens hit their empty state on entry (no extensions installed; row
  order still unseeded on the first frame). An early return out of an *inline* composable content
  lambda skips the group-closing bookkeeping the Compose compiler emitted for that lambda, leaving
  the composer's group stack unbalanced; the damage shows up on the next recomposition or
  navigation — i.e. "tapping the row crashes the app". Both now use the same
  `if (empty) { EmptyState(…) } else { LazyColumn(…) }` shape as `CollectionScreen`.
  - Hardening on top: `HomeOrderScreen`'s seeding `LaunchedEffect` and its `setHomeOrder` write are
    `runCatching`-guarded (an unreadable row snapshot degrades to the empty state), and both
    Settings rows navigate with `launchSingleTop` so a double-tap cannot stack two copies.
- **Cold boot (Problem A — flaky metadata/posters, dropped extensions).**
  - `ExtensionRepository.reloadAll()` now retries the descriptor read once **and** each extension
    load once (300 ms apart), keeps the per-extension isolation, and never throws — a cold-boot
    hiccup (dex opt directory still settling, storage not ready) no longer silently drops a source
    for the whole session.
  - `NovaApp` routes its cold-boot collectors through a new `collectSafely(...)`. `appScope` has no
    `CoroutineExceptionHandler`, so a single bad emission (e.g. `DownloadManagerProvider`
    rebuilding its cache) escaped to the default handler and killed the app; each collector now
    skips a failed value and keeps collecting. `ensureDefaults()` also retries once after 3 s so a
    flaky first connection still seeds the default add-ons/repos.
  - `Http.httpGet()` (the path every TMDB / AniList / MangaDex metadata + poster fetch uses) is
    now a bounded retry: 3 attempts with 250/500 ms backoff for **transient** failures only
    (transport errors, 5xx, 408, 429). Other 4xx answers are final, and `CancellationException` is
    rethrown so ISSUE-10's lesson still holds.

### 6. Scrollable accent color row (SettingsScreen)
- The `Row` of `Accents` swatches is wrapped in `Modifier.horizontalScroll(rememberScrollState())`,
  so the accents that used to be clipped past the right edge (leaving a dead strip of blank space)
  are reachable on a narrow phone.

### 7. Horizontal manga paging (MangaReaderActivity)
- **Root cause.** `ZoomablePage` installed `detectTransformGestures` on every page. That helper
  **consumes all position changes once a pan passes the touch-slop threshold** — including plain
  one-finger drags — and pointer events are dispatched child-first, so the parent
  `HorizontalPager` saw its drag consumed and cancelled its own touch-slop detection: Right→Left
  and Left→Right never advanced. Webtoon was unaffected because it renders plain `AsyncImage`s.
- **Fix.** A hand-rolled `awaitEachGesture` loop that only claims events that are genuinely a
  pinch (2+ pointers) or a pan of an already-zoomed image. A single finger at 1× consumes nothing,
  so the pager gets the swipe; zoom still clamps at 1×–4×, resets when it returns to fit, and
  pinch/pan haptics and the `beyondViewportPageCount = 3` preloading are untouched.

### 8. Continue Reading for manga (normal + NSFW)
- **Recording.** `MangaReaderActivity` passes an `onProgress(page, total)` callback into
  `ReaderScreen`, fired by the same `savePage` that persists the per-chapter resume point. It
  writes a `WatchEntry(item, videoId = chapterId, videoTitle = chapterTitle,
  positionMs = pagesRead, durationMs = totalPages)` through `LibraryStore.recordWatch`, so reading
  progress lands in the **same history list** as playback progress (incognito still suppresses it,
  the 100-entry cap and newest-first de-dupe apply, and the reader's own
  `SettingsStore.setMangaProgress` resume point is unchanged).
- **Split.** New `MediaItem.isReadable` (`MANGA` / `NSFW_MANGA`) lets the UI divide one history
  list without a schema change: `HomeViewModel.continueWatching` now excludes reading entries and
  a new `HomeViewModel.continueReading` selects only them (both filtered at <95 % complete).
- **UI.** Home renders a second row, **Continue Reading**, reusing the Continue Watching card
  (menu-book glyph instead of the play glyph, chapter title as the subtitle, `×`/long-press removal
  wired to the same dialog). Library's "My Library" grid gained a matching **Continue Reading**
  section; its empty state only shows when *neither* section has entries, and `Stats` keeps reading
  the same combined history.
- `progress` is `pagesRead / totalPages`, so the card's progress bar is a reading percentage and a
  chapter read to ≥95 % drops out of the row just like a finished episode.

### Files
`ui/theme/Tokens.kt`, `data/model/Models.kt` (`MediaItem.isReadable`),
`ui/vm/ViewModels.kt` (`SearchViewModel.search`, `HomeViewModel.continueReading`),
`ui/nav/FloatingPillNavBar.kt`, `ui/nav/NovaNav.kt`, `ui/screens/home/HomeScreen.kt`,
`ui/screens/search/SearchScreen.kt`, `ui/screens/manga/MangaScreen.kt`,
`ui/screens/nsfw/NsfwScreen.kt`, `ui/screens/library/LibraryScreen.kt`,
`ui/screens/settings/SettingsScreen.kt`, `ui/screens/settings/HomeOrderScreen.kt`,
`ui/screens/sources/SourcesScreen.kt`, `ui/player/MangaReaderActivity.kt`,
`data/ext/ExtensionRepository.kt`, `data/remote/Http.kt`, `NovaApp.kt`.

### Verification (2026-10-06)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only the repo's pre-existing deprecation
  warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **103/103, 0 failures, 0 errors** (14 result files).
- `:app:assembleRelease` → **BUILD SUCCESSFUL**, signed.
- `dist/NovaStream.apk` refreshed: **38,264,498 bytes**, SHA-256 `aae43aa3…eabd4`; the new
  symbols (`Continue Reading`, `collectSafely`, `isReadable`) are present in the release dex.
- **Device pass still required** for all eight (§7 item 30): the crash fix and the pager gesture
  change especially are runtime behaviours that static analysis can only take so far.

---

## 2t. High & Medium audit fixes (2026-10-05)

**Topic: fix the ten findings from the static audit of the whole app** (four settings/features that
were silently inert, and six UI/UX defects). The audit is source-level; the fixes below are the
engine wiring, the gesture math and the UI consistency issues it surfaced.

Status: **implemented and built.** All ten fixes landed; source list and verification below.

### 1. Inert settings / engine wiring
- **Max parallel downloads** — `data/download/DownloadManagerProvider.kt` hardcoded
  `setMaxParallelDownloads(3)` and never read `SettingsStore.maxParallel`, so the Settings control
  (and its backup/restore) had no effect. The provider now exposes `setMaxParallel(context, value)`
  (clamped 1–6) that both applies to the live `DownloadManager` and seeds new holders, and `NovaApp`
  collects `settings.maxParallel` and pushes it on every change.
- **Persistent source disabling** — `data/ext/ExtensionRepository.kt` loaded *every* installed
  extension on startup (`reloadAll()`), ignoring `SettingsStore.disabledSources`, so the Sources
  dashboard's disable switch was session-only. `ExtensionRepository` now takes the `SettingsStore`
  and `reloadAll()` filters out disabled ids; the Sources screen keeps its existing
  enable/disable load/unload behaviour.
- **Custom download folder** — `SettingsStore.storageUri` was written by the SAF picker and shown
  in Settings but never read. A completed download is now exported into the chosen SAF folder
  (`data/download/DownloadExporter.kt`, best-effort streamed copy through framework
  `DocumentsContract` — no new dependency — driven from `VideoDownloadManager`'s completion
  listener). Note the hard constraint: Media3's shared
  `SimpleCache` is constructed against a fixed app-private `File` and cannot be pointed at a
  `content://` tree, so the folder is a **destination for exports**, not the cache root — the
  Settings label is updated to say so.
- **Player volume drag gesture** — `PlayerActivity.adjustVolume` compared `abs(volumeAccum)` to
  `VOLUME_STEP` (0.15) but committed `volumeAccum.toInt()`, which truncates toward zero, so a step
  only landed once the accumulator reached 1.0 and any drag shorter than a full accumulator was
  ignored (and `endGesture()` then reset it). Fixed to
  `val steps = (volumeAccum / VOLUME_STEP).toInt(); volumeAccum -= steps * VOLUME_STEP`.

### 2. UI / UX
- **NSFW adaptive grid** — `NsfwScreen.kt` still faked results with `chunked(3)` + `Row3`; results
  now use the same `LazyVerticalGrid(GridCells.Adaptive(AppSpacing.posterMin))` as Search/Library/
  Collection, so tablet/landscape cards match the rest of the app (and the phantom spacer fillers
  on partial rows are gone).
- **Status-bar insets** — Search, Library, Browse, NSFW, Settings, Add-on Manager, Sources and
  HomeOrder hardcoded their top padding (40–48 dp); all now use `statusBarsPadding()`, matching
  Home/Detail/Collection.
- **Player sheets** — the Quality & Audio, Episodes, Speed and External sheets were hand-rolled
  `Box{...}.clickable{onDismiss}` overlays: tapping inside dismissed them and system Back finished
  the whole player. They are now `ModalBottomSheet`s (inner taps don't dismiss, Back closes the
  sheet).
- **Subtitle-language chips** — `DetailScreen`'s `StreamSheet` rendered `TagChip(t.lang) {}` with a
  no-op lambda; the chips now select the corresponding subtitle track (toggling a selection state).
- **Manga reading-position eviction** — `SettingsStore.setMangaProgress` re-inserted an existing
  chapter key in place, so a frequently-read chapter stayed at its old position and could be
  dropped by the `takeLast(200)` trim. Updating a key now removes it before re-inserting.
- **Player resume race** — `PlayerActivity.onCreate` started the progress watcher before
  `resumeLastPosition()`, so a quick pause/back could persist position 0 over the saved spot. The
  watcher is now started after the resume has been read.

### Verification (2026-10-05)
- `:app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only the pre-existing deprecation warnings).
- `:app:testDebugUnitTest --rerun-tasks` → **103/103, 0 failures, 0 errors** (14 result files).
- `:app:assembleDebug` + `:app:assembleRelease` → **BUILD SUCCESSFUL**, signed.
- `dist/NovaStream.apk` refreshed: **38,264,498 bytes**, SHA-256 `fbf2e046…95ec8`.
- Device pass still required for the interaction fixes (bottom sheets, subtitle-language chips,
  volume drag, custom-folder export).

---

## 2s. NSFW Anime speed & correct-title matching (2026-10-05)

**Topic: fix the two device-reported NSFW-anime problems — (1) searching/loading taking over a
minute, and (2) clicking "Boku to Misaki-sensei" opening a completely different title
("Netokano: After Party…") with its episodes.** AniList must stay the fast primary; the
title/episodes/streams that open must always belong to the item the user selected.

Status: **implemented and built** — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅
**103/103** (7 new), `:app:assembleDebug` + `:app:assembleRelease` ✅ (46,003,430 / 38,248,114
bytes, signed), `dist/NovaStream.apk` refreshed (SHA-256 `91a44109…18BB`). Needs a device pass
(§7 item 29).

### 1. Root causes found
- **The minute-long wait was unbounded awaits, not slow AniList.** `HentaiRepository.episodes()`
  awaited **all 9 sources with no timeout** (Http: 20 s connect / 30 s read + 429 retries), for up
  to 3 query variants, and `fillBuiltInEpisodes` could run it 3 more times per detail open; the
  NSFW-anime home rows had no per-row bound (the flaky `Jikan Top` row gated the whole tab) and
  `stremioNsfwRows` was missing the `CATALOG_TIMEOUT_MS` cap its SFW twin got in Phase 16;
  `searchNsfwAnime` awaited the Stremio add-on fan-out on **every** search and its AniList call
  was unbounded; `related()` and the detail's metadata sources were unbounded too.
- **The wrong title opened because "relevance" was effectively `any substring > 0`.**
  `adultRelevanceScore` counts *every* token, including filler like **"to" — which is a
  substring of "Netokano"** — so a loose site-search hit passed `rel > 0`. Three places then
  substituted it for the selected title: `JikanClient.detailsByTitle` explicitly fell back to
  `candidates.firstOrNull { isAdult } ?: first()` (a random adult entry from Jikan's fuzzy `?q=`),
  `MetadataRepository.detail` let any candidate win on metadata **score** regardless of title, and
  `hentaiFallback` **renamed the page** with `name = head.series` (plus swapped the poster),
  splicing a different series' episode list into it.

### 2. Speed — every NSFW-anime hop is now hard-bounded
- **Search (`CatalogRepository.searchNsfwAnime`).** AniList primary under `ANILIST_TIMEOUT_MS`
  8 s → Jikan only if empty, 6 s → Stremio NSFW add-ons **only as a fallback** when both built-ins
  come back empty, 6 s (same built-ins-authoritative rule Real 18+ uses, ISSUE-9). A normal search
  now returns at AniList speed instead of always waiting on the add-on fan-out; worst degraded
  path is ~20 s instead of an open-ended OkHttp hang.
- **Home rows (`nsfwAnimeRows`).** Every row runs concurrently under `ROW_TIMEOUT_MS` 10 s and a
  new `nsfwRow()` helper; the section emits **progressively** (`onUpdate` per finished row, in
  registry order) so `NsfwViewModel` paints AniList rows in about a second and drops the skeleton
  without waiting for `Jikan Top`. `stremioNsfwRows` now wraps `fetchCatalog` in the Phase-16
  `CATALOG_TIMEOUT_MS` 7 s cap it was missing.
- **Detail (`MetadataRepository.detail`).** For `NSFW_ANIME` each candidate source runs under
  `NSFW_SOURCE_TIMEOUT_MS` 8 s; the existing early break (stop once AniList returns a synopsis)
  and `related()`'s NSFW branch (8 s) are bounded as well. Episode fill (`fillBuiltInEpisodes`)
  got an overall `EPISODE_FILL_BUDGET_MS` 10 s budget across its ≤3 title variants.
- **Built-in episodes/streams (`HentaiRepository`).** Per-source `SOURCE_TIMEOUT_MS` 5 s, a
  `TOTAL_BUDGET_MS` 8 s budget for the whole episode lookup across query variants, and
  `STREAM_TIMEOUT_MS` 12 s on stream resolution (aligned with Phase 16's per-add-on stream cap).
  Timeouts surface as ordinary per-source failures — exactly how a dead site is already treated —
  and caller cancellation is rethrown (a private `bounded()` helper catches
  `TimeoutCancellationException` but never a real `CancellationException`, per ISSUE-10).

### 3. Correct-title matching — one shared gate, applied at every substitution point
- **New matcher (`data/adult/AdultSource.kt`).** `adultSignificantTokens()` drops filler words
  ("to", "the", "no", … — falling back to the raw tokens when a query is *nothing but*
  fillers), and `adultTitlesMatch(expected, actual)` requires at least one significant word of
  the expected title to appear in the candidate. Case/punctuation/word-order insensitive; a title
  with **no** usable keywords never claims a match (the caller then keeps what the user selected).
- **Episode & stream resolution (`HentaiRepository.episodes`).** A source hit is only accepted as
  "this title" when `adultTitlesMatch(query, series + title)` — loose site-search/popular
  fallbacks are rejected before ranking. Since `StreamRepository` resolves built-in streams
  through the same `episodes()` lookup, streams can no longer come from a different series either.
- **Metadata acceptance (`MetadataRepository.detail`).** A candidate whose `name` (or any
  `altTitles` entry) isn't recognisably the selected item is **skipped** (`sameTitle()`) instead
  of being allowed to win on score — so a wrong Jikan/add-on meta can neither replace a good
  AniList record nor become `best`.
- **`hentaiFallback` never renames the page.** It keeps the selected item's title and poster (only
  borrowing a poster when the catalog item has none) and takes the episode list only when the
  head series matches the requested title.
- **`JikanClient.detailsByTitle` requires a credible match.** Exact (case/punctuation-insensitive)
  match first, then shared-significant-word match across `title_english` / `title` / `titles[]`;
  the `first adult entry ?: first candidate` fallbacks are **deleted** — no match means no detail
  from Jikan, and the caller falls back to what the user actually selected.

### Files
`data/adult/AdultSource.kt`, `data/hentai/HentaiRepository.kt`, `data/repo/MetadataRepository.kt`,
`data/repo/CatalogRepository.kt`, `data/remote/JikanClient.kt`, `ui/vm/ViewModels.kt`,
`app/src/test/java/com/moovio/app/TitleMatchTest.kt` (new).

### Verification
`:app:compileDebugKotlin` ✅ (only pre-existing deprecation warnings) ·
`:app:testDebugUnitTest` → **103 tests / 0 failures** ✅ (7 new: filler-token stripping,
filler-only fallback, the reported "Boku to Misaki-sensei" vs "Netokano: After Party" rejection,
punctuation/case/suffix matches, candidate-side words can't fake a match, no-keyword titles never
match) · `:app:assembleDebug` + `:app:assembleRelease` ✅ (46,003,430 / 38,248,114 bytes, signed)
· `dist/NovaStream.apk` refreshed — new symbols (`adultTitlesMatch`, `boundedSearch`, `nsfwRow`)
verified present in the release dex. **Needs a device pass (§7 item 29)** — the minute-long wait
and the wrong-title repro only prove themselves against live endpoints on hardware.

---

## 2r. Best CloudStream + Stremio sources — Movies / TV / Anime (2026-10-04)

**Topic: make regular Movies / TV / Anime work well out of the box on the strongest CloudStream +
Stremio ecosystems, without touching the NSFW / built-in adult sources.** Priority order: Movies &
TV reliability > Anime reliability > overall speed & stability.

Status: **implemented and built** — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ 96/96
(9 new), `:app:assembleDebug` + `:app:assembleRelease` ✅ (38,248,114 bytes release, signed),
`dist/NovaStream.apk` refreshed (SHA-256 `70ADDA87…3EBD`). Needs a device pass (§7 item 28).

### 1. Pre-seed / recommend the strongest sources
- **Default provider repos expanded to 7** (`AddonStore.Defaults.repos`): CloudStream **Phisher**,
  **Hexated** (`hexated/cloudstream-extensions-hexated`) and **community**; Aniyomi **yuzono** and
  **official**; Mihon/Keiyoushi **pb** and **json** mirrors. `AddonRepository.ensureDefaultRepos()`
  **merges** any missing defaults into the stored list for existing installs too (it previously only
  seeded when the whole list was empty), so no one has to hunt for Hexated/Phisher by hand.
- **Torrentio / TPB+ / MediaFusion first-class.** Torrentio and ThePirateBay+ stay auto-seeded;
  `AddonStore.Defaults.recommendedStreamAddons` lists them (plus **MediaFusion** and
  **OpenSubtitles v3**) and the Add-on Manager's Stremio tab now renders a **Recommended** row with
  one-tap install for whichever is missing. Retired `v3-chill` is pruned on startup.

### 2. More robust CloudStream + Stremio integration
- **Reliability-first stream ranking.** New pure `data/repo/StreamRanking.kt`: `rank()` de-duplicates
  streams and orders the preferred stream hosts (`preferredStreamHosts` = torrentio,
  thepiratebay-plus, mediafusion) first, then higher quality (`qualityRank`), then healthier swarms
  (`seedersOf` parses Torrentio's `👤 1,234`, plus `1234 seeders` / `Seeds: 1234`). Streams are
  enriched with a parsed seeder count when the add-on didn't report one.
- **Per-add-on timeout + collect-all errors.** `StreamRepository.stremioStreamsDetailed()` runs every
  enabled stream add-on in parallel under a hard `STREAM_TIMEOUT_MS` (12 s) so one hung host can no
  longer stall the list, and returns a `StreamsResult(streams, errors)`. `DetailViewModel` exposes
  `streamErrors`; the stream picker shows a muted **“Some add-ons could not be reached”** list under
  an empty result, and the empty hint now points at Settings → Add-on Manager.
- **Dead-domain migration.** `ensureDefaults()` drops add-ons on `retiredHosts` and re-seeds any
  missing first-class stream providers, so an existing install keeps a working playback source
  after a provider moves.

### 3. Speed & stability of catalogs + search
- **Short result cache.** Regular (Movie/Series/Anime) stream lookups are cached for 5 minutes
  (bounded to 64 entries) keyed by `addonId|id|videoId`, so re-opening a title or re-tapping an
  episode is instant. NSFW/Real 18+ results are never cached.
- **Timeouts on regular search & catalogs.** `CatalogRepository` now wraps Stremio add-on search
  (`ADDON_TIMEOUT_MS` 6 s, both `addonSearch` and `globalSearch`) and catalog-row / `loadCatalog`
  fetches (`CATALOG_TIMEOUT_MS` 7 s) in `withTimeout`, so one slow host degrades one row instead of
  blocking Home or search.
- **Repo-browse feedback.** `AddonViewModel.browseCloudStream/Aniyomi/Keiyoushi` now set a
  “Repository unavailable or empty” message instead of silently showing an empty list when a repo is
  down.

### 4. NSFW / built-in adult sources untouched
`StreamRepository`'s adult (`AdultSources.byId`) and `NSFW_ANIME` (`HentaiRepository`) branches are
unchanged: adult items still resolve their own direct streams, NSFW-anime still leads with built-in
results and only queries NSFW-flagged add-ons, and neither path uses the regular stream cache.

### Files
`data/local/AddonStore.kt`, `data/repo/AddonRepository.kt`, `data/repo/StreamRanking.kt` (new),
`data/repo/StreamRepository.kt`, `data/repo/CatalogRepository.kt`, `data/model/Models.kt`
(`StreamSource.seeders`), `ui/vm/ViewModels.kt`, `ui/screens/detail/DetailScreen.kt`,
`ui/screens/addons/AddonManagerScreen.kt`,
`app/src/test/java/com/moovio/app/StreamRankingTest.kt` (new).

### Verification
`:app:compileDebugKotlin` ✅ (only pre-existing deprecation warnings) ·
`:app:testDebugUnitTest` → **96 tests / 0 failures** ✅ (9 new: seeder parsing, preferred ordering,
quality/seeders tie-breaks, de-dup) · `:app:assembleDebug` + `:app:assembleRelease` ✅
(38,248,114 bytes release, signed) · `dist/NovaStream.apk` refreshed. **Needs a device pass
(§7 item 28)** — ranking order, live add-on error text and search latency only prove themselves
against real add-ons on a device.

---

## 2q. Tactile polish & high-value quick wins (2026-10-04)

**Topic: five targeted quick wins — tactile haptics, subtitle timing precision, a playback-stats
overlay, a warm manga filter, and storage automation.**

Status: **implemented and built** — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ 87/87
(6 new), `:app:assembleDebug` + `:app:assembleRelease` ✅ (45,958,786 / 38,231,730 bytes, signed),
`dist/NovaStream.apk` refreshed (SHA-256 `DB214614…9BD6`). Needs a device pass (§7 item 27).

### 1. Tactile haptics & feedback
- **Player.** `LocalHapticFeedback` (`HapticFeedbackType.TextHandleMove`) now fires on: dragging the
  seek bar (throttled to once per whole second of content, not per pixel), the ±10 s centre buttons,
  and the double-tap-to-skip ripple. `ui/player/PlayerActivity.kt`.
- **Manga reader.** Both the paged and webtoon readers click on every page turn — one shared
  `LaunchedEffect` keyed on the current page covers swipe, tap and the volume-key commands alike
  (the initial page load is skipped so opening a chapter doesn't buzz).

### 2. Player features & subtitle precision
- **Subtitle sync offset.** Media3 1.3 has **no** subtitle-offset API, so the player now buffers the
  caption timeline from `Player.Listener.onCues(CueGroup)` and, when an offset is set, hides the
  native `SubtitleView` and renders a **time-shifted overlay** at `position - offset` (capped at 200
  buffered cue sets). A new slider in the bottom sheet's Subtitles section offers **±10 s in 0.5 s
  steps** with a Reset; the value persists (`SettingsStore.subtitleSyncMs`). Pure selection logic
  lives in `PlaybackMath.activeCueIndex` so it is unit-tested.
- **Stats for nerds.** A **Stats** chip toggles a compact overlay showing resolution, video codec,
  track bitrate, estimated bandwidth, FPS, buffer (seconds · %), playback state and dropped frames.
  Format comes from the selected `Tracks` group; dropped frames and bandwidth come from an
  `AnalyticsListener`, refreshed by the player's existing 250 ms ticker.

### 3. Manga reader & storage automation
- **Warm / sepia filter.** A third colour-matrix option (`mangaFilter(invert, grayscale, warm)`)
  applies a gentle sepia tint alongside the existing invert/grayscale, toggled under
  Settings → Manga Reader and persisted.
- **Auto-delete watched downloads.** New Settings → Downloads toggle;
  `VideoDownloadManager.deleteDownloadsForItem(itemKey)` removes an item's downloads **and their
  cached media** (`removeCache = true`), fired once when playback passes 90 % in the player's
  progress recorder.

### Files
`ui/player/PlaybackMath.kt` (new), `ui/player/PlayerActivity.kt`, `ui/player/MangaReaderActivity.kt`,
`data/local/SettingsStore.kt`, `data/download/VideoDownloadManager.kt`, `ui/vm/ViewModels.kt`,
`ui/screens/settings/SettingsScreen.kt`,
`app/src/test/java/com/moovio/app/PlaybackMathTest.kt` (new).

### Verification
`:app:compileDebugKotlin` ✅ (only pre-existing deprecation warnings) ·
`:app:testDebugUnitTest` → **87 tests / 0 failures** ✅ (6 new: cue selection, subtitle-sync clamp
+ labels, bitrate/FPS formatting) · `:app:assembleDebug` + `:app:assembleRelease` ✅
(45,958,786 / 38,231,730 bytes, signed) · `dist/NovaStream.apk` refreshed. **Needs a device pass
(§7 item 27)** — haptics, the time-shifted caption overlay and the storage automation only prove
themselves on hardware.

---

## 2p. Downloader fix & storage cleanup (2026-10-04)

**Topic: fix three critical offline problems — video downloads failing immediately or mid-transfer,
orphaned partial files silently bloating storage, and an inaccurate storage meter.**

Status: **implemented and built** — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ 81/81
(8 new), `:app:assembleDebug` + `:app:assembleRelease` ✅ (45,912,458 / 38,215,346 bytes, signed),
`dist/NovaStream.apk` refreshed (SHA-256 `2676D9DF…CB24`). Needs a device pass (§7 item 26).

### 1. Downloader failure diagnosis & fix
- **HTTP headers / cookies now reach the downloader.** The player already sent a stream's
  `Referer`/`Cookie`/UA through an OkHttp data source, but Media3's `DownloadManager` was built with
  a single bare `DefaultHttpDataSource` (UA only), so header-gated hosts (Pornhub's `hm-h` CDN,
  Cloudflare-fronted sites, hotlink-protected media) failed the download while the same URL played
  fine. New `DownloadHeaders.downloadRequestHeaders()` mirrors the player's interceptor (stream
  headers win; the user's global custom identity fills gaps; UA falls back to the app UA), and a new
  per-download **`HeaderAwareDownloaderFactory`** decodes the stream's headers from the request's
  `data` payload (Media3's `DownloadRequest` has no header field) and bakes them into a fresh
  `DefaultHttpDataSource` per download. Because each download gets its own `DefaultDownloaderFactory`,
  **HLS segment/key requests and progressive range requests inherit the same headers**, not just the
  first playlist URL. `DownloadManagerProvider` now uses the public
  `DownloadManager(context, WritableDownloadIndex, DownloaderFactory)` constructor with a shared
  `DefaultDownloadIndex` + the shared `SimpleCache`.
- **Storage fallback.** The cache directory is resolved through a writability probe: app-private
  `filesDir/downloads/media` (the historic location, so existing downloads survive the upgrade)
  → app-specific external storage (`getExternalFilesDir(DIRECTORY_MOVIES)`, no runtime permission)
  → internal `cacheDir`. A location that cannot be created/written silently degrades instead of
  crashing the offline stack on launch. (The SAF folder remains for exports/backups — Media3's
  `SimpleCache` is constructed against a fixed directory and cannot be relocated mid-session.)
- **Network / SSL errors, retries & mirror fallback.** `DownloadManager.setMinRetryCount(4)` gives
  transient failures an exponential-backoff retry. When a download still ends `STATE_FAILED` and the
  stream carried backup `alternates`, `VideoDownloadManager.maybeFailover()` transparently removes the
  dead entry and re-enqueues the next mirror (reusing the primary headers), thread-guarded and
  persisted via the request metadata's `attempted` list so it survives a process restart and can
  never loop.

### 2. Orphaned download cleanup & storage management
- **Automatic partial-file cleanup.** New pure `DownloadCacheMaintenance` object identifies chunk
  files (`.exo`/`.tmp`/`.part`/`.vtt`) that no `SimpleCache` span references, and
  `VideoDownloadManager.clearTemporaryFiles()` removes every non-completed download (releasing its
  spans via the framework) **and** deletes those orphaned chunks left by a process kill mid-download.
  Deleting or failing over a download now passes `removeCache = true` to
  `DownloadService.sendRemoveDownload`, so a cancelled/failed download's media is released instead of
  lingering on disk with nothing in the Downloads tab pointing at it (the main source of the reported
  ~1 GB bloat).
- **Clear action in Settings.** Settings → Storage now has **Clear download cache & temporary
  files**, wired to `SettingsViewModel.clearDownloadCache()`; it also shows a live breakdown —
  **Completed downloads**, **Temporary / partial files** (with the orphaned portion), and the
  reclaimable total.
- **Accurate storage meter.** `VideoDownloadManager.storageStats()` separates bytes held by
  indexed completed downloads from the rest of `SimpleCache.getCacheSpace()` (partial/failed chunks
  and playback cache) and from on-disk orphan bytes the index does not know about. The metadata
  cache is reported separately as before.

### Files
`data/download/DownloadHeaders.kt` (new), `data/download/DownloadCacheMaintenance.kt` (new),
`data/download/DownloadManagerProvider.kt`, `data/download/VideoDownloadManager.kt`,
`ui/screens/detail/DetailScreen.kt`, `ui/player/PlayerActivity.kt`, `ui/vm/ViewModels.kt`,
`ui/screens/settings/SettingsScreen.kt`,
`app/src/test/java/com/moovio/app/DownloadMaintenanceTest.kt` (new).

### Verification
`:app:compileDebugKotlin` ✅ (only pre-existing deprecation warnings) ·
`:app:testDebugUnitTest` → **81 tests / 0 failures** ✅ (8 new: header merging and orphan-file
detection) · `:app:assembleDebug` + `:app:assembleRelease` ✅ (45,912,458 / 38,215,346 bytes,
signed) · `dist/NovaStream.apk` refreshed. **Needs a device pass (§7 item 26)** — header-gated
playback, mirror failover and storage reclaim only prove themselves against live hosts.

---

## 2o. NSFW speed + metadata + notice (2026-10-04)

**Topic: fix three NSFW-section problems — slow NSFW-Anime search and hard-failing detail pages,
slow Real 18+ search, and an oversized "sources unavailable" banner.**

Status: **implemented and built** — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ 73/73,
`:app:assembleDebug` + `:app:assembleRelease` ✅ (46,090,540 / 38,215,346 bytes, signed),
`dist/NovaStream.apk` refreshed (SHA-256 `5F3E9ACA…F785`). Needs a device pass (§7 item 25).

### 1. NSFW Anime — fast search + a metadata fallback that never hard-fails
- **Search**. NSFW-anime search used to call AniList **and** then Jikan **sequentially** (Jikan even
  retried once with a 700 ms pause and could 504), so an AniList-fast query still waited on Jikan.
  `CatalogRepository.search()` now routes `NSFW_ANIME` through a dedicated `searchNsfwAnime()`:
  **AniList is the primary source**, the Stremio NSFW add-ons run **concurrently** alongside it, and
  **Jikan is consulted only when AniList returns nothing**, bounded by a 6 s `withTimeout`. Results
  are filtered to the NSFW-anime type and de-duped.
  *(Superseded 2026-10-05 by §2s: the add-ons are now a **fallback** consulted only when both
  built-ins return nothing, and every hop — AniList included — carries a hard timeout.)*
- **Detail metadata fallback chain**. `MetadataRepository.detail()` for an `anilist` NSFW item now
  tries **AniList → `JikanClient.detailsByTitle()`**, and for a `jikan` item **Jikan-MAL → AniList**
  (by MAL id). The loop **stops after AniList when the record already carries a synopsis**, so the
  slow fallback is never paid for when the fast source succeeds.
- **New `JikanClient.details(malId)` / `JikanClient.detailsByTitle(title)`** map Jikan's
  `/anime/{id}/full` payload into a full `MetaDetail` (poster, synopsis, score, genres, studios,
  status, episode count, Japanese/synonym titles) with `addonId = "jikan"`.
- **Never hard-fail.** When **both** AniList and Jikan fail for an `NSFW_ANIME` item,
  `MetadataRepository.detail()` returns a detail built from the **built-in hentai sources**
  (`HentaiRepository.episodes()` supplies the series name, poster and episode list) merged over the
  catalog item's own title/poster/year/description, instead of `null` — so the detail screen renders
  instead of showing "Couldn't load details".
  *(Amended 2026-10-05 by §2s: the fallback **keeps the selected item's title and poster** — only a
  title-matching episode list is merged in; the old `name = head.series` rename is what opened a
  different series than the one the user clicked.)*
- **Episodes whenever possible.** For a normal (AniList/Jikan) NSFW-anime detail, the existing
  `DetailViewModel.fillBuiltInEpisodes()` still fills the episode list from the built-in hentai
  sources; for the hentai-only fallback the episodes are already in the detail, so the list appears
  without a second search. Streams resolve through `StreamRepository` as before.

### 2. Real 18+ — much faster search
- **Progressive results.** `AdultRepository.searchDetailedProgressive(query, onUpdate)` emits a
  ranked partial list after **each** source finishes; `CatalogRepository.searchRealProgressive()`
  forwards it through; `NsfwViewModel.searchReal()` paints `realResults`/`realSearchErrors` as they
  land (generation-guarded). The complete list is still returned at the end — but the first results
  appear immediately, so **one slow/hung source can no longer block the whole search**.
- **Per-source timeout.** Each source is wrapped in `withTimeout(SEARCH_TIMEOUT_MS = 8 s)`; a
  timeout is **not** retried (retrying a hung host only doubles the wait) while transient
  non-timeout failures are still retried once as before.
- **Concurrency cap.** A `Semaphore(10)` bounds how many of the twelve sources scrape at once, so
  shared hosts are less likely to rate-limit the burst.
- **Result caching.** Ranked searches are cached by normalised query for `SEARCH_TTL_MS = 5 min`
  (`SEARCH_CACHE_MAX = 32`, LRU); re-typing or returning to the tab is instant.

### 3. Compact, dismissible "sources unavailable" notice
- `NsfwScreen`'s banner is now a **single compact line** — "N sources unavailable" — inside a slim
  rounded surface, instead of the old multi-line block that occupied half the page.
- It has a **× dismiss** button (hidden until new errors arrive) and the summary row is **tappable
  to expand** the per-source detail lines only when the user wants them.

### Files
`data/remote/JikanClient.kt`, `data/repo/MetadataRepository.kt`, `data/repo/CatalogRepository.kt`,
`data/adult/AdultRepository.kt`, `ui/vm/ViewModels.kt`, `ui/screens/nsfw/NsfwScreen.kt`.

### Verification
`:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` → **73 tests / 0 failures** ✅ ·
`:app:assembleDebug` + `:app:assembleRelease` ✅ (46,090,540 / 38,215,346 bytes, signed) ·
`dist/NovaStream.apk` refreshed. **Needs a device pass (§7 item 25)** — the fallback path and the
progressive paint only prove themselves on hardware against live endpoints.

---

## 2n. Final UI & app checkup — player theming, touch targets & rotation state

**Topic: close out the last "this doesn't match the rest of the app" items — the video player
ignoring the user's theme, sub-48 dp touch targets, and UI state that resets on rotation.**

Status: **implemented and built — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ 73/73,
`:app:assembleRelease` ✅ (38,198,962 bytes, signed), `dist/NovaStream.apk` refreshed
(SHA-256 `4A2A0BF0…C246`).** Needs a device pass (§7 item 24).

### 1. Video player theme consistency
- **Live settings, not a hardcoded theme.** `PlayerActivity.setContent` collects
  `settings.theme` / `accent` / `dynamicColor` / `amoledBlack` and passes them into
  `NovaStreamTheme(darkTheme, accentKey, dynamicColor, amoledBlack)` (system mode resolves via
  `isSystemInDarkTheme()`), replacing the hardcoded `NovaStreamTheme(darkTheme = true, accentKey =
  "violet")`. Same pattern the manga reader got in §2l item 7, so the player now honours custom
  accents, Material You and AMOLED pitch-black like every other screen. Text drawn directly on the
  video stays white by design.
- **Sheets use theme colours.** The track, episode, speed/audio and external-player bottom sheets
  took their background from a hardcoded `0xFF14141C` and their accent from `0xFF7C5CFF`; they now
  use `MaterialTheme.colorScheme.surface` and `LocalNovaColors.current.accent` (`TrackRow`'s
  selection highlight and check icon included).
- **Sheet text follows the theme too (completed this pass).** With the sheet surface tracking the
  theme, the leftover `Color.White` headings and rows were unreadable on a light surface. Sheet
  headings, track rows, unselected speed-chip labels, the audio boost/normalize rows and the
  external-player heading now use `LocalNovaColors` `textPrimary`/`textSecondary`; the autoplay
  countdown card uses `colorScheme.onSurface`/`onSurfaceVariant`; the speed chips switch from
  `White@12%` to `surfaceElevated` when unselected. The control-bar `AssistChip`s keep a fixed
  translucent-black container + white label (`AssistChipDefaults.assistChipColors`) because they
  float over the video scrim in both themes, as do the controls, gesture HUD and seek ripple.

### 2. Touch-target & component standardization (all to the 48 dp floor)
- **`SearchField` clear button** (`Components.kt`) 36 → **48 dp** (glyph stays 18 dp inside it).
- **NSFW search box** (`NsfwScreen.kt`) — each section now renders the shared `SearchField` instead
  of a raw `OutlinedTextField`, so it gets the filled pill, the in-flight pulsing dot (`searching`)
  and the standard clear affordance (`onClear`) that Search already had.
- **Subtitle colour swatches** (`SettingsScreen.kt`) — each swatch is a **48 dp** target with the
  34 dp visual dot centred inside it.
- **Detail `CircleIconButton`** (`DetailScreen.kt`) — **48 dp** target around the 40 dp disc, so
  back/heart over artwork meet the floor without growing visually.

### 3. State preservation & layout polish
- **Rotation-safe state.** `AddonManagerScreen`'s tab index and `HomeOrderScreen`'s working order
  are `rememberSaveable` (the order is a plain `List<String>`, directly saveable), so rotating no
  longer drops the user back to the Stremio tab or resets an in-progress reorder; the HomeOrder
  reseed stays guarded on `order.isEmpty()` so a restored copy isn't clobbered by `LaunchedEffect`.
- **Derived home-row order.** `HomeScreen` computes `remember(rawRows, homeOrder) {
  applyRowOrder(rawRows, homeOrder) }` — one keyed derivation rather than recomputing on every
  pass.
- **Collection header inset.** `CollectionScreen`'s header replaced the hardcoded `top = 44.dp`
  with `statusBarsPadding()`, so notches and tall status bars render the title correctly (the same
  fix Phase 10 applied to Home and Detail).

### Files
`ui/player/PlayerActivity.kt`, `ui/components/Components.kt`, `ui/screens/nsfw/NsfwScreen.kt`,
`ui/screens/settings/SettingsScreen.kt`, `ui/screens/detail/DetailScreen.kt`,
`ui/screens/addons/AddonManagerScreen.kt`, `ui/screens/settings/HomeOrderScreen.kt`,
`ui/screens/home/HomeScreen.kt`, `ui/screens/collection/CollectionScreen.kt`.

### Verification
`:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` → **73 tests / 0 failures** ✅ ·
`:app:assembleRelease` ✅ (38,198,962 bytes, signed) · `dist/NovaStream.apk` refreshed.
**Needs a device pass (§7 item 24)** — light-theme sheet legibility and rotation behaviour only
prove themselves on hardware.

---

## 2m. NSFW-anime source expansion — 7 new hentai sources

**Topic: NSFW Anime had only HentaiMama + HentaiPlay; add the priority and secondary hentai sites
in the same modular style as the existing `data/hentai/` package.**

Status: **implemented and built — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ 73/73
(10 new parser cases), `:app:assembleDebug` + `:app:assembleRelease` ✅, `dist/NovaStream.apk`
refreshed.** Live reachability needs a device pass (§7 item 23) — most of these sites are
Cloudflare-protected and could not be probed from the build environment.

### New sources (all implement the existing `HentaiSource` contract)
- **Hanime** (`hanime`, `HanimeSource.kt`) — hanime.tv is a Cloudflare-fronted SPA, so it is scraped
  through its own JSON endpoints: search = `POST search.htv-services.com/` (`hits[]` -> `slug`/
  `name`/`poster_url`), streams = `GET /api/v8/video?id={id}` ->
  `videos_manifest.servers[].streams[].url`. HTML fallbacks for both.
- **HentaiHaven** (`hentaihaven`, `HentaiHavenSource.kt`) — WordPress `?s=` search; `.xxx` host
  first, `.org` mirror second.
- **HStream** (`hstream`, `HStreamSource.kt`) — hstream.moe; `/hentai/…` permalinks; HLS/MP4 from
  the page or its player frame.
- **Hentaigasm** (`hentaigasm`, `HentaigasmSource.kt`) — WordPress `?s=` search.
- **MuchoHentai** (`muchohentai`, `MuchoHentaiSource.kt`) — WordPress `?s=` search.
- **HentaiCity** (`hentaicity`, `HentaiCitySource.kt`) — WordPress `?s=` search.
- **Haho** (`haho`, `HahoSource.kt`) — `/api/search?v=` JSON first (defensive key lookup), search
  page fallback.

### Shared helper
- **`HentaiTubes.kt` (new)** — the pieces every site needs: a direct-media-URL extractor
  (`.mp4`/`.m3u8`, escaped JS strings, preview-image safe), `qualityOf`, the page-`Referer` stream
  builder, a generic WordPress-style card parser (splits on `<article>`/post-class blocks so menu
  links can't masquerade as videos; tag-stripped inner text, `title=`/`alt` and heading fallbacks),
  `isContentLink` site-chrome filter, and a `resolvedStreams` resolver that tries the page itself,
  then one embedded iframe, then a second nested frame.

### Registry & wiring (no changes needed)
`HentaiSources.all` now lists **9**. `HentaiRepository` iterates it for both `episodes()` and
`streams()`, so the new sites inherit parallel querying, per-source error isolation, relevance
ranking, the 10-minute TTL caches and `AdultHttp.verifyPlayable()` pre-flight automatically — **one
broken site still cannot take down the section**, and AniList metadata is untouched.

### Tests
`app/src/test/java/com/moovio/app/HentaiSourcesExtraTest.kt` (new) — 10 cases: Hanime
`hits` and `videos_manifest` parsing, Haho JSON parsing (incl. an item with no URL), the media-URL
extractor (snapshot files ignored), stream tagging, the WordPress card parser (nested-`<img>`
anchor), HStream permalink filtering, the episode/series helpers, `isContentLink`, and a registry
sanity check (9 unique ids).

### Known caveats / next
- **Not probed live** (Cloudflare / adult endpoints are blocked from the build environment). The
  parsers are written to each site's known shape with HTML fallbacks; expect the device pass to
  surface drift, and each source fails independently by design.
- **HStream, Hentaigasm, MuchoHentai, HentaiCity** rely on the generic WordPress card parser and
  the one/two-hop embed resolver — sites whose player is a third-party host (streamtape, doodstream,
  …) will yield no direct stream, and the Stremio NSFW add-ons then answer as fallback.
- **Hanime** is the highest-quality source when its endpoints are reachable; its JSON shape is the
  most likely to change.
- **Haho** parsing is deliberately tolerant (searches several keys) because its API shape was not
  verifiable.

---

## 2l. Static-review bug-fix pass (all 10 issues)

**Topic: fix the 10 problems found in the static review of the UI + navigation layer.**

Status: **implemented and built — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ 63/63,
`:app:assembleRelease` ✅, `dist/NovaStream.apk` refreshed.** Behaviours that only manifest at
runtime (bottom-bar visibility, the spinner reset, biometric persistence) still need a device pass
(§7 item 22).

### Navigation & app logic
1. **Search tab route mismatch (`NovaNav.kt`).** The Search destination is registered as
   `search?q={q}` but its tab is `search`, so `currentRoute in tabs` never matched: the bottom bar
   hid on the Search tab, it never highlighted, and `tabs.indexOfFirst` returned -1 so Search was
   never persisted. Added `String.routeBase()` (strips any `?…` tail) and `tabForRoute(route)`,
   used by `showBar`, the per-tab `selected` flag and the tab-persistence `indexOfFirst` —
   argument-bearing routes (`collection/{type}`, `detail/…`) are unaffected.
2. **NSFW search spinners could stick (`ViewModels.kt`).** `searchAnime`/`searchManga` set
   `animeSearching`/`mangaSearching = true` but the "query too short" early return (and a cancelled
   in-flight job) never cleared them. The early return now sets the flag `false`, and the search
   body runs in `try/finally` that clears the flag on success, failure **and** cancellation (only
   when this request is still the newest, so a superseding job owns it otherwise).
3. **Stream picker wouldn't retry (`DetailScreen.kt`).** `pickStreams` skipped the lookup whenever
   the target id was unchanged, so an episode whose resolve failed reopened a permanently empty
   sheet. It now reloads when the target changed **or** `streams.isEmpty() && !streamsLoading`.

### UI & accessibility
4. **Continue Watching remove target (`HomeScreen.kt`).** The `×` was a 26 dp clickable; it is now
   a 48 dp touch target wrapping the same visible 26 dp chip (matching `PosterCard`), keeping the
   destructive action above the accessibility floor.
5. **Double empty state (`HomeScreen.kt`).** On a load failure the feed rendered *both* "Nothing
   here yet" and "Couldn't load content". Now a single mutually-exclusive `when`: loading → skeleton
   rows, else error → error state, else empty → empty state.
6. **Search "Clear" recents (`SearchScreen.kt`).** The bare clickable `Text` is now a 48 dp
   `Box` with the standard ripple.

### Consistency, security & robustness
7. **Manga reader theme (`MangaReaderActivity.kt`).** Replaced the hardcoded
   `NovaStreamTheme(darkTheme = true, accentKey = "violet")` with the app's live settings (theme
   mode, accent, Material You, AMOLED black), so the reader matches the rest of the app.
8. **App-lock persistence (`MainActivity.kt`).** `unlocked` now uses `rememberSaveable`, so a
   rotation / Activity recreation no longer re-triggers the biometric prompt when already unlocked.
9. **Volume keys only when loaded (`MangaReaderActivity.kt`).** The reader reports readiness to the
   Activity via a new `onReady` callback; `onKeyDown` only consumes volume keys when pages are
   actually shown, so they keep normal volume behaviour while loading / on error.
10. **Carousel restart (`HomeScreen.kt`).** `FeaturedCarousel`'s auto-advance `LaunchedEffect` is
    keyed on the `items` list (was `items.size`), so a refreshed feed restarts the loop even when
    the item count is unchanged.

### Files
`ui/nav/NovaNav.kt`, `ui/vm/ViewModels.kt`, `ui/screens/detail/DetailScreen.kt`,
`ui/screens/home/HomeScreen.kt`, `ui/screens/search/SearchScreen.kt`,
`ui/player/MangaReaderActivity.kt`, `ui/MainActivity.kt`.

---

## 2k. Real 18+ source expansion — xHamster / YouPorn / RedTube + JAV sites

**Topic: more built-in Real 18+ coverage — three general tube sites and four JAV / Japanese adult
video sources, on top of the original five.**

Status: **implemented and built — `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ 63/63
(11 new parser cases), `:app:assembleDebug` + `:app:assembleRelease` ✅, `dist/NovaStream.apk`
refreshed.** Live reachability of each new site still needs a device pass (§7 item 21) — these
sites bot-block / geo-block irregularly and could not be probed from the build environment.

### New sources (all implement the existing `AdultSource` contract)
- **xHamster** (`xhamster`, `XhamsterSource.kt`) — `/newest`, `/best`, `/trending`, `/search/{q}`,
  `/categories`; streams from the embedded `xplayerSettings`/`sources` blob via `TubeMedia`.
- **YouPorn** (`youporn`, `YouPornSource.kt`) — a Pornhub-family (Aylo) tube: `/most_viewed/`,
  `/best/`, `/recent/`, `/search/?query=`. Streams prefer the site's
  `api/video/media_definitions/{id}/` JSON endpoint (`mediaDefinitions` shape), falling back to the
  page markup.
- **RedTube** (`redtube`, `RedTubeSource.kt`) — same Aylo family: `/best`, `/newest`, `/?search=`,
  `media_definitions/{id}/` endpoint + page fallback. Cards are numeric `/{id}` links.
- **MissAV** (`missav`, `MissavSource.kt`) — JAV: `/en/new`, `/en/release`, `/en/trending`,
  `/en/today`, `/en/search/{q}`, `/en/genres/{slug}`. Video codes carry a digit, so section links
  are filtered out of listings; streams come from the `surrit.com` `.m3u8` on the page.
- **Jable** (`jable`, `JableSource.kt`) — JAV: `/latest-updates/`, `/hot/`, `/new/`,
  `/search/{q}/`, `/tags/{slug}/`; streams from `var hlsUrl = '…'` / `<source>`.
- **HPJAV** (`hpjav`, `HpjavSource.kt`) — JAV WordPress tube: `/lastest-updates/`, `/popular/`,
  `/?s={q}`; streams from the JW Player `file:`/`sources` block.
- **Avgle** (`avgle`, `AvgleSource.kt`) — JAV with a **clean JSON API**
  (`api.avgle.com/v1`): `/videos/{page}?o=bv|bw|tr`, `/search/{q}/{page}`, `/categories`,
  `/video/{vid}` → `video_url`. Degrades to a clear per-source notice when the API is down.

### Shared helper
- **`TubeMedia.kt` (new)** — one media extractor for every scraper that reads playable URLs out of
  page markup: absolute **and** protocol-relative `.mp4`/`.m3u8` (including escaped `https:\/\/…`
  JS strings), quality labelling from the URL suffix / `/1080.mp4` segment / a sibling
  `quality`/`label` key, ad/placeholder filtering, highest-first ordering, and the shared
  `data-src`/`src` thumbnail helper. `TubeMedia.definitions()` parses the Aylo `mediaDefinitions`
  JSON shape shared by YouPorn/RedTube.

### Registry & wiring (no changes needed)
`AdultSources.all` now lists all 12. Because the repository iterates that list, the new sources
inherit progressive per-source painting, the 10-min per-source home cache, per-source error
isolation (`NsfwViewModel.realErrors`), keyword-ranked search and stream pre-flight automatically —
**one broken site still cannot take down the Real 18+ tab.**

### Tests
`app/src/test/java/com/moovio/app/AdultSourcesExtraTest.kt` (new) — 11 cases: xHamster /
YouPorn / RedTube / Jable / MissAV / HPJAV listing parsing, YouPorn `mediaDefinitions` → quality
streams, `TubeMedia` protocol-relative + escaped-URL extraction with ordering, the Jable HLS stream,
Avgle JSON list parsing (incl. zero-duration handling) and a registry sanity check (12 unique ids).

### Known caveats / next
- **These could not be probed live** (adult endpoints are blocked from the build environment), so
  the parsers are written to the sites' known/current shapes with multi-fallback extraction rather
  than verified against a live response the way the original five were. Expect the device pass to
  surface markup drift on some sites; each one fails independently by design.
- **Avgle** is the most likely to be fully dead (it has been intermittent for years); the API path
  is correct when reachable and otherwise shows a notice.
- **MissAV** hides its manifest behind an obfuscated JS packer on some pages; the plain `.m3u8` is
  picked up when present, but a packed page may yield no streams (the Stremio NSFW add-ons then
  answer as fallback).
- JAV titles are scraped as-is (often the studio code); metadata quality depends on the site.

---

## 2j. Phase 13 — Library crash fix, category filters & Detail stream sheet

**Topic: stop the Library from dying on a stored favourite, make Library browsable by category,
and get the raw stream list off the Detail page.**

Status: **implemented, both APKs rebuilt — `:app:compileDebugKotlin` ✅,
`:app:testDebugUnitTest` ✅ 52/52, `:app:assembleRelease` ✅ (38,166,194 bytes, signed).
`dist/NovaStream.apk` refreshed. No device pass yet.**

### 1. Library crash fix (the P0)

**Root cause.** Gson does not honour Kotlin nullability: a favourite/history JSON element with no
`type` field (or an enum value the current `MediaType` doesn't know) decoded into a `MediaItem`
whose `type` slot held a runtime `null`. The first thing the UI did with it was
`item.type.id` while building the detail route (`ui/nav/NovaNav.kt` → `Routes.detail`), which
threw an NPE. Compounding it, `LibraryStore.decode<T>` wrapped a whole-list `fromJson` in one
`try/catch` returning `emptyList()`, so a single malformed element silently wiped the user's
entire library.

**Fix (three layers):**
- **New `data/local/LibraryCodec.kt`** — pure-JVM, element-wise tolerant codec for both lists.
  Per-element sanitization (bad entries dropped, the rest survive); `type` resolved from the enum
  name (`"MOVIE"`), the catalog id (`"movie"`), or the add-on's `stremioType`, finally falling
  back to `MediaType.MOVIE` — never null; `poster`/`backdrop`/`rating`/`description` read as
  null-safe; `genres` accepted as an array, a lone string or garbage (else empty); rating rejects
  non-finite/nonsense; `positionMs`/`durationMs`/`updatedAt` defaulted and clamped; history
  entries with no salvageable `item` are dropped; dedupe by `item.key` (first = newest wins);
  `null`/`""`/`{not json`/`"movie"`/`42`/`[1,2,3]`/`{}` all decode to an empty list instead of
  throwing.
- **`LibraryStore.kt`** — every read and write now goes through `LibraryCodec`
  (`decodeFavorites`/`decodeHistory`/`encodeFavorites`/`encodeHistory`); the old
  `TypeToken` + `Http.gson` `decode<T>` helper is gone.
- **`LibraryViewModel` (`ui/vm/ViewModels.kt`)** — a second, per-element defensive filter
  (`filterSafe`) drops any entry whose key/title is unusable, so one hostile title can no longer
  blank a section; **`StatsTab`** wraps `WatchStats.compute` in `runCatching` with an empty-stats
  fallback so a bad history entry can't take the tab down either.

**Also hardened:** `ActivityGrid` LazyGrid keys are now section-prefixed (`hist:` / `fav:`) and
`DownloadsGrid`'s are `vid:` / `ch:` — a title present in both Continue Watching and Favorites
previously produced duplicate keys in one grid. (Compose 1.7 does not throw on duplicates —
verified in the foundation sources — but it can reuse the wrong item state.)

**Tests:** new `app/src/test/java/com/moovio/app/LibraryCodecTest.kt`, 15 cases covering
missing/unknown `type`, partial+malformed arrays, duplicate keys, null/garbage optional fields,
genre/rating coercion, history with a missing `item`, hostile payloads, and a round trip.

### 2. Library category filter chips

- New private `LibraryFilter` enum in `LibraryScreen.kt`: **All / Movies / TV Shows / Anime /
  NSFW**, rendered as a horizontal chip bar (`LibraryFilterBar`) directly under the segmented
  control, shown for the **Downloads** and **My Library** sections only (hidden for Stats).
  `rememberSaveable` keeps the selection across tab switches.
- Mapping: Movies → `MOVIE`; TV Shows → `SERIES`; Anime → `ANIME` **+ `MANGA`** (manga has no
  chip of its own and would otherwise only be reachable via "All"); NSFW → `NSFW_ANIME`,
  `NSFW_MANGA`, `REAL`. `null`/unknown types only ever match **All**.
- **Downloads are filtered too.** `VideoDownload.matches` decodes `itemJson` through
  `LibraryCodec` to get the `MediaItem.type`; `MangaDownload` gained a trailing
  **`nsfw: Boolean = false`** field (`data/download/MangaDownloadManager.kt`, persisted
  automatically in `meta.json`, legacy snapshots default to false), passed as
  `nsfw = item.type == MediaType.NSFW_MANGA` at both `manager.enqueue(...)` call sites in
  `ui/player/MangaReaderActivity.kt` (the composable now takes an `nsfw` param wired from the
  activity's `item`).
- Section headers only render for a specific category when that category has content; under
  **All** both sections keep their original empty states. Scroll state lives in
  `rememberLazyGridState()` inside the section composable, so changing a chip re-filters in
  place without resetting scroll (the chip bar itself sits outside the scrollable grid).

### 3. Detail screen overhaul

- **Clean header/hero unchanged in structure** (backdrop, poster, title, alt title, rating,
  votes, year, status, count) — the crowding fix was about what came *below* it.
- **Seasons are now a picker, not an accordion.** `DetailViewModel.toggleSeason` →
  **`selectSeason`** (no collapse — one season stays selected, so the page never stacks every
  season's cards). `SeasonRow` (poster + expand arrow) is deleted and replaced by
  **`SeasonPickerRow`**: a horizontal scroll row of chips under the section header, with the
  episode-count hint and the bulk **download-season** action moved into the section header's
  trailing slot.
- **Episode cards rebuilt.** `EpisodeRow` is now: fixed 16:9 thumbnail placeholder (artwork or a
  play glyph) → `E3 · Title` (1 line) → **duration + air date** → **saved-progress bar** with a
  `32:10 / 45:00` readout. The 2-line synopsis/overview was dropped from the row.
  - `Video` gained **`runtimeMin: Int?`** (trailing, default null), populated from TMDB's
    `seasonEpisodes` (`eo.runtime`, minutes) in `data/remote/TmdbClient.kt`.
  - Progress comes from `LibraryStore.history` (collected directly in `DetailScreen`) keyed by
    `videoId` → `positionMs`/`durationMs`/`progress`.
- **Streams moved into a bottom sheet.** The entire inline "Streams (n) / Finding streams…"
  section is **removed** from the page body. New `StreamSheet` uses Material3
  **`ModalBottomSheet`** (+ `rememberModalBottomSheetState(skipPartiallyExpanded = true)`),
  bounded to 560 dp with a `weight(1f, fill = false)` list so a short mirror list collapses the
  sheet. It shows: title/episode sub-heading, resolving state, the existing `StreamRow` mirrors
  (quality badge, add-on attribution, offline download), and a subtitle-language chip row.
  - Tapping an episode now calls `pickStreams(episode)` → `vm.loadStreams(v)` then opens the
    sheet (instead of `loadStreams` + a long inline list).
  - A new outlined **`PlaylistPlay`** icon button sits beside Play/Trailer in the CTA row and
    opens the sheet for the current target — for a series it targets the episode already on
    screen (`pickStreams(selectedVideo)`) so the selection is never silently cleared.
  - `play()` closes the sheet **first** (the sheet owns its own window, so the torrent
    "Preparing stream…" overlay would otherwise render behind it).
- **Play stays meaningful for series.** A `LaunchedEffect(expandedSeason, seasonEpisodes)` picks
  the selected season's first episode and resolves its streams, so Play is enabled without
  opening the picker. `DetailViewModel.load()` no longer issues a title-level
  `loadStreams(null)` when seasons exist (that raced the episode lookup and could leave Play
  pointing at an empty result); it clears `streamsLoading` instead so a failed episode fetch
  can't leave a permanent "Finding streams…".
- **`loadStreams` is now generation-guarded** (`++streamsRequest`; only the newest request may
  publish), so tapping episode 3 then 7 can no longer leave episode 7 showing episode 3's mirrors.

### Files touched
`data/local/LibraryCodec.kt` (new), `data/local/LibraryStore.kt`,
`data/model/Models.kt` (`Video.runtimeMin`), `data/remote/TmdbClient.kt`,
`data/download/MangaDownloadManager.kt` (`MangaDownload.nsfw`),
`ui/player/MangaReaderActivity.kt`, `ui/vm/ViewModels.kt`,
`ui/screens/library/LibraryScreen.kt`, `ui/screens/detail/DetailScreen.kt`,
`app/src/test/java/com/moovio/app/LibraryCodecTest.kt` (new).

### Verification (done)
- `gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL** (only the repo's pre-existing
  deprecation warnings for `Icons.Filled.ArrowBack`/`MenuBook` etc.; the new `PlaylistPlay`
  import uses the AutoMirrored variant so it adds no warning).
- `gradlew :app:testDebugUnitTest` → **BUILD SUCCESSFUL, 52 tests, 0 failures, 0 errors**
  (13 AdultSources + 15 LibraryCodec + 3 MangaDownload + 3 PaletteCache + 9 Parsers +
  4 ResolutionBadge + 2 SearchCancellation + 3 WatchStats).
- `gradlew :app:assembleRelease` → **BUILD SUCCESSFUL** (minify/R8 stays off, `versionCode 2`,
  `versionName 1.1.0`).
- `app/build/outputs/apk/release/app-release.apk` (38,166,194 bytes, signed) copied to
  **`dist/NovaStream.apk`** — byte size is sane against the prior 38,149,810-byte build.
- `LibraryCodecTest` (15 cases) + `ResolutionBadgeTest` (4) + `WatchStatsTest` (3) are all in the
  52-test run above.

### What is LEFT (resume here)
1. **Device pass** (cannot be done from this session): Library opens with a pre-existing
   malformed favourite; category chips on both sections + scroll retention; Detail page on a TMDB
   series (season chips, episode duration/progress, Play pre-targeting ep1), episode tap → sheet,
   header `PlaylistPlay` button, torrent play from the sheet (overlay visible), and a Real 18+
   title. Tracked as **§7 item 20**.
2. Optional tidy: re-read `DetailScreen.kt` + `LibraryScreen.kt` for leftover unused
   imports/params; re-run compile + tests if anything changes.

---

## 2i. Phase 12 — integrations, extension manager, downloader & security

**Topic: make the app extensible, self-managing and safe.**

### 1. Source & extension manager
- **Extension dashboard** (`ui/screens/sources/SourcesScreen.kt`, route `sources`). Lists every
  installed dynamic extension with version, an enable/disable switch (loads/unloads through
  `ExtensionHost`, persisted in `SettingsStore.disabledSources`) and a **Test** button that runs the
  extension's `catalog(null)` and reports **latency + item count** (`data/integrations/SourceTester`).
- **Multi-source failover.** `StreamSource.alternates` (new) carries backup mirrors; `PlayerActivity`
  installs an `onPlayerError` listener that transparently switches to the next untried URL and
  re-prepares playback, only surfacing an error when every mirror is exhausted.
- **Custom HTTP headers.** Global User-Agent / Referer / Cookie are configurable (Settings →
  Sources & Network) and applied by `Http.globalIdentity()` to every outbound request, plus an
  OkHttp interceptor on the player's data source — but only when the stream didn't already supply
  that header, so a source's own Referer always wins.

### 2. Downloader & offline upgrades
- **Background service.** The Media3 `NovaDownloadService` (foreground `dataSync`) already owns the
  queue; Phase 12 adds a user-set **max parallel downloads** (`SettingsStore.maxParallel`, cycled in
  Settings) and keeps the pause/resume/retry/delete logic.
- **Storage selector.** A SAF `OpenDocumentTree` picker persists a custom folder URI
  (`SettingsStore.storageUri`) and takes a persistable permission; the same SAF plumbing backs
  backup/restore. (Downloads themselves still live in the Media3 `SimpleCache` under app storage —
  the custom folder is used for exports; moving the cache needs an `ExoPlayer` cache relocation.)
- **Auto-download next episode.** When an episode nears its end, `PlayerActivity.autoDownloadAhead()`
  resolves and queues the episode *after* the autoplaying one, gated on the toggle and the
  Wi-Fi-only preference (checked with `ConnectivityManager`).

### 3. Discovery, sync & backup
- **Home row reordering** (`ui/screens/settings/HomeOrderScreen.kt`, route `home_order`). Rows are
  dynamic, so instead of fixed drag-and-drop it lists the live rows with up/down priority controls
  and persists the title order; `applyRowOrder` stably sorts the home feed by it.
- **Watch statistics.** The Library gains a third **Stats** tab (`WatchStats.compute`) showing total
  watch time, in-progress/completed counts, favourites, a by-type breakdown and a top-genres bar
  chart. Pure logic, covered by `WatchStatsTest`.
- **Scrobbling & two-way sync** (`data/integrations/ScrobbleManager.kt`). Tokens for Trakt, AniList,
  MyAnimeList, Kitsu and SIMKL are pasted in Settings and used for best-effort history pushes;
  providers without a shared id are reported as skipped rather than guessed (see caveats).
- **Backup & restore** (`data/integrations/BackupManager.kt`). Exports favourites, history, home
  order, custom headers, update repo and scrobble tokens to a user-chosen `.json` via SAF, and
  restores it back.

### 4. Security & system integration
- **App lock.** A startup gate (`AppLockGate` in `MainActivity`) blocks all content behind a
  `BiometricPrompt` (biometric + device-credential); it fails open when no secure lock exists.
- **Incognito mode.** Toggle that flips `LibraryStore.incognito`, making `recordWatch` a no-op so
  playback continues without writing history.
- **Screen protection.** `FLAG_SECURE` toggle applied to the window to block screenshots/recents.
- **Release updater.** `UpdateChecker` queries the configured GitHub repo's latest release, compares
  tags and reports; when an `.apk` asset exists the settings sheet downloads it to cache and fires
  the system installer via the existing `FileProvider` (`REQUEST_INSTALL_PACKAGES` added,
  `file_paths.xml` already covers `cache/`).
- **Background workers.** New `NewContentWorker` (periodic 6 h, network-constrained) diffs the home
  feed against a persisted seen-set and posts a "N new titles" notification when the toggle is on.
- **TV/D-Pad.** Compose focus is honoured throughout; the new dashboard/order screens use standard
  focusable Material buttons and switches so a remote can navigate them.

### Files
`data/local/SettingsStore.kt`, `di/AppContainer.kt`, `NovaApp.kt`, `data/remote/Http.kt`,
`data/local/LibraryStore.kt`, `data/model/Models.kt`, `data/sync/NewContentWorker.kt` (new),
`data/integrations/` (new: `WatchStats`, `BackupManager`, `UpdateChecker`, `ScrobbleManager`,
`SourceTester`), `ui/MainActivity.kt`, `ui/screens/sources/SourcesScreen.kt` (new),
`ui/screens/settings/HomeOrderScreen.kt` (new), `ui/screens/settings/SettingsScreen.kt`,
`ui/screens/library/LibraryScreen.kt`, `ui/screens/home/HomeScreen.kt`, `ui/vm/ViewModels.kt`,
`ui/nav/NovaNav.kt`, `ui/player/PlayerActivity.kt`, `AndroidManifest.xml`,
`app/src/test/java/com/moovio/app/WatchStatsTest.kt` (new).

### Verification
`:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` → **37 tests / 0 failures** ✅ (3 new) ·
`:app:assembleDebug` + `:app:assembleRelease` ✅ (45.8 MB debug / 38 MB release) ·
`dist/NovaStream.apk` refreshed. **Needs a device pass (§7 item 19).**

### Known limitations / honest caveats
- **Scrobbling needs id mapping.** Title→provider-id mapping (a real AniList→MAL/Trakt resolver) is
  not implemented, so a provider syncs only when the item already carries its id (numeric for
  AniList/MAL/Kitsu, `tt…` for Trakt/SIMKL) and is otherwise reported as *skipped*. Tokens are
  pasted manually — there is no interactive OAuth flow.
- **Storage selector is for exports, not the media cache.** Relocating the shared Media3
  `SimpleCache` to a SAF tree is not done; downloads remain in app storage.
- **Auto-crop border trim** (Phase 11) and **release-update install** are best-effort; the installer
  requires the user to allow installs from this source (system prompt).
- **FLAG_SECURE/incognito** simply gate local logging/rendering; they are not a hardened sandbox.

---

## 2h. Phase 11 — core player, manga reader & customization engine

**Topic: make the player and reader feel like a purpose-built app, and give the theme real knobs.**

### 1. Video player & playback
- **Picture-in-Picture.** `onUserLeaveHint` already entered PiP; it now only fires while actually
  playing, and on API 31+ sets `setAutoEnterEnabled(true)` so the system's swipe-to-home gesture
  enters PiP without a flicker.
- **Speed & audio sheet.** New bottom sheet with 0.5x–2.0x speed chips plus **Audio boost** and
  **Normalize volume** toggles. Speed drives `ExoPlayer.setPlaybackSpeed` and is persisted;
  boost/normalize attach a `LoudnessEnhancer` to the player's audio session (best-effort — silently
  no-ops where the effect is unavailable).
- **Next-episode overlay at 15 s.** The autoplay watcher now fires when **15 s remain** (it used a
  95%-of-duration check, which for a 45-min episode popped the overlay two minutes early).
- **Skip Intro / Outro.** Action buttons appear while inside the intro/outro window and seek past it.
  Windows use real chapter markers when a stream supplies them (`StreamSource.introEndMs` /
  `outroStartMs`, new optional fields) and otherwise fall back to a conservative 90 s heuristic.
- **External player hand-off.** A sheet offers **VLC** and **MPV** (explicit package intents)
  and a generic "Other apps" chooser; a missing player falls back to the system chooser.
- **Gestures.** Left-edge vertical drag = brightness, right-edge = volume, horizontal = seek
  (existing, with the batched-flush smoothing). **New:** double-tap left/right seeks ∓10 s with an
  expanding-ring ripple and a ±10s label; **pinch-to-zoom** cycles the video scale mode
  (Fit → Stretch → Fill → 16:9 → 21:9). The pinch handler is hand-rolled with `awaitEachGesture`
  so it only claims two-finger gestures and never starves the single-finger drag handler.
- **Overlays & controls.** The existing `TrackSheet` covers quality + audio + subtitle tracks;
  the top bar adds a **Cast/DLNA** action (opens the system cast/route chooser, best-effort) and a
  **lock** toggle that hides the chrome and disables gestures until tapped to unlock.
- **Subtitle styling.** User-set text size, colour, background opacity and vertical offset are
  applied to the `PlayerView`'s `SubtitleView` via a `CaptionStyleCompat`.

### 2. Customization & theme engine
- **Dynamic colors + presets.** New **Material You** toggle (`dynamicDarkColorScheme` on API 31+)
  that replaces the manual accent with the system primary, plus named preset accents
  **Neon Purple · Emerald · Crimson · Sunset Orange** in the picker (`ui/theme/Color.kt`).
- **True AMOLED pitch black.** Toggle swaps the dark background/surface to `#000000` and the
  elevated surface to `#0B0B0B`; because every screen reads `LocalNovaColors`, it propagates app-wide.
- **Subtitle options.** Size, four preset text colours, background opacity and vertical offset,
  all in Settings → Subtitles and consumed by the player.

### 3. Manga reader power features
- **Reading modes.** LTR / RTL / Webtoon (existing) plus **double-page landscape spreads**
  (paired pages, correctly ordered for RTL).
- **Image processing.** **Night mode** (colour inversion), **grayscale**, and **auto-crop borders** —
  applied as a Compose `ColorMatrix` `ColorFilter` on each page (no extra render pass when off).
  Border crop is a heuristic: a ~6% scale-and-clip that trims uniform margins (documented below).
- **Zoom lock.** Pinch zoom is hoisted into a shared `MutableZoom`; with the lock on it persists
  across page turns, otherwise it resets per page.
- **Precise position saving.** Page/scroll index is persisted per chapter (`SettingsStore`
  `MANGA_PROGRESS`, capped at 200 entries) and restored on open.
- **Volume-key page turns.** The activity intercepts `KEYCODE_VOLUME_UP/DOWN` and emits through a
  `MutableSharedFlow` the active reader collects; toggleable in Settings.

### Files
`data/local/SettingsStore.kt`, `data/model/Models.kt`, `ui/vm/ViewModels.kt`, `ui/MainActivity.kt`,
`ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/screens/settings/SettingsScreen.kt`,
`ui/player/PlayerActivity.kt`, `ui/player/MangaReaderActivity.kt`.

### Verification
`:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` → **34 tests / 0 failures** ✅ ·
`:app:assembleDebug` + `:app:assembleRelease` ✅ (45.6 MB debug / 38 MB release) ·
`dist/NovaStream.apk` refreshed. **Needs a device pass (§7 item 18)** — PiP, gestures, the
LoudnessEnhancer and the Cast chooser are all runtime-only behaviours.

### Known limitations / honest caveats
- **Cast/DLNA is best-effort** — it opens the system cast/wireless-display chooser rather than
  integrating the Cast SDK; true in-player remote playback would need `androidx.mediarouter` +
  the Cast SDK as new dependencies.
- **Auto-crop borders** is a scale-and-clip heuristic, not pixel-level white-margin detection;
  it trims roughly uniform margins. Real detection would decode each bitmap and scan rows/cols.
- **Skip Intro/Outro** uses stream-provided markers when present, else a fixed 90 s window — not
  learned per-show intro detection.
- **Audio boost/normalize** are `LoudnessEnhancer` gain presets, not a multiband compressor.
- Subtitle styling is applied when the player view is created; changing it mid-playback takes
  effect on the next open.

---

## 2g. Phase 10 — device-tested UX & layout bug pass

**Topic: six layout/functionality bugs found on device after Phase 9.**

### 1. Home hero now draws edge-to-edge behind the status bar
`enableEdgeToEdge()` + `WindowCompat.setDecorFitsSystemWindows(false)` were already set, but the
Home feed still started with a solid top bar at a hardcoded `44.dp` inset, so the hero sat *under*
the status bar only after a black strip forced it down.
- The `LazyColumn` top padding is gone. When a hero exists it is the **first item and draws from
y=0**, behind the status bar.
- `HomeTopBar` (logo + refresh/add-ons) is now a **floating overlay** on top of the hero, with
  `statusBarsPadding()` so it clears the system bar, plus a dedicated 130 dp top black→transparent
  gradient scrim so the logo is legible over bright artwork. Its text/icons switch to white in
  overlay mode.
- When there is **no** hero (empty/loading feed) the top bar falls back to a normal first list row
  (still inset-aware) and nothing overlays content.
- `ui/screens/home/HomeScreen.kt`.

### 2. Detail header title no longer collides with the back/heart buttons
The header was a fixed `aspectRatio(16f/11f)` box with the poster+title block bottom-anchored. On
narrow phones the 2:3 poster (96 dp wide → 144 dp tall) plus a 3-line title grew **upward into the
app bar**, overlapping the translucent back button.
- Header height is now derived from the screen width (`screenWidthDp * 0.95f`, clamped 320–460 dp),
  matching the hero idiom, so the title block always clears the controls.
- The app-bar row uses `statusBarsPadding()` instead of a hardcoded `top = 36.dp`, so the buttons
  sit below the real system status bar on every device.
- Title capped at 2 lines so a long title can't re-grow the block.
- `ui/screens/detail/DetailScreen.kt`.

### 3. Genre chips spacing
The genre chip row sat immediately under the primary CTA row with no gap. Added an
`AppSpacing.item` (12 dp) spacer above the chips. `ui/screens/detail/DetailScreen.kt`.

### 4. Search field was visually dead (typing did nothing)
Root cause was a state-binding bug, not an input bug: `SearchScreen` passed
`onValueChange = { vm.search(it) }` but **never wrote the `rememberSaveable` `query`**, so the
field's `value` never changed and every keystroke looked unregistered (the ViewModel was actually
receiving them). Now `onValueChange = { query = it; vm.search(it) }`.
`ui/screens/search/SearchScreen.kt`.

### 5. Poster clipping / scroll glitches in grids
Compose 1.7 draws shared elements through the `SharedTransitionScope` **overlay**, which is not
clipped by the scrolling container below it — while a grid/rail scrolls, a poster could paint over
the header, search field or segmented control. Fixes:
- New `LocalSharedElementsEnabled` composition local (`ui/nav/SharedTransition.kt`);
   `sharedElementFor` becomes a no-op when it is `false`. *(Superseded 2026-10-02: shared
   elements were removed entirely along with the local.)*
- Every scroll container publishes
  `LocalSharedElementsEnabled provides !listState.isScrollInProgress`, so morphing is suspended
  during scroll and restored when the list settles (so poster → detail still morphs). Applied in
  `MediaRow`, Library (both grids), Search, Collection, Manga and NSFW.
- `Modifier.clipToBounds()` on each grid/list viewport and on the `AnimatedContent` that hosts the
  Browse sections, so no card can ever paint outside its container.
- `zIndex(1f)` on the Browse/Library headers and segmented controls so chrome always layers above
  content.

### 6. Library restructured into two sections
`LibraryScreen` stacked downloaded videos, downloaded chapters, Continue Watching and Favourites in
one long scroll. It now has a **segmented control** (same component style as Browse) with two tabs:
- **Downloads** — downloaded videos + downloaded manga chapters only.
- **My Library** — Continue Watching + Favourites (the removal `×`/long-press flow is unchanged).

Each tab owns its own `LazyVerticalGrid` and scroll state; the selected tab is `rememberSaveable`
so returning to the Library keeps the section. Complete file rewrite:
`ui/screens/library/LibraryScreen.kt`.

### Files
`ui/screens/home/HomeScreen.kt`, `ui/screens/detail/DetailScreen.kt`,
`ui/screens/search/SearchScreen.kt`, `ui/screens/browse/BrowseScreen.kt`,
`ui/screens/collection/CollectionScreen.kt`, `ui/screens/manga/MangaScreen.kt`,
`ui/screens/nsfw/NsfwScreen.kt`, `ui/screens/library/LibraryScreen.kt` (rewritten),
`ui/components/Components.kt`, `ui/nav/SharedTransition.kt`.

### Verification
`:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` → **34 tests / 0 failures** ✅ ·
`:app:assembleDebug` + `:app:assembleRelease` ✅ (46 MB debug / 38 MB release) ·
`dist/NovaStream.apk` refreshed. **Needs a device pass (§7 item 17)** — the hero/status-bar and
header-inset fixes especially only prove themselves on hardware with a tall status bar.

---

## 2f. Phase 9 — detail hierarchy & navigation clean-up

**Topic: action hierarchy and bottom-bar structure.** Phases 7–8 fixed *how things look and move*.
Phase 9 fixes *what things are competing with each other for*.

### 1. Detail screen CTA hierarchy
The detail screen had three actions of comparable weight: a filled **Play**, an outlined **Trailer**,
and a **full-width** "Add to Library" button stacked underneath. A full-width button outranks the
primary action no matter what colour it is, so "In Library" was winning a fight it shouldn't be in.

- **One dominant CTA.** `PrimaryActionButton` fills with the artwork-derived accent and carries a
  matching outer glow (`shadow(elevation, ambientColor = accent, spotColor = accent)`), faded in over
  `Motion.MEDIUM` when enabled and dropped to zero elevation when disabled. It keeps the app's
  standard 0.96 press shrink, and shows a spinner while streams resolve instead of the word "Play".
- **Library moved to the header.** The heart is now a compact circular icon button in a new detail
  app bar, beside Back. It is a secondary toggle and belongs there.
- **Header app bar.** A dedicated 104 dp top scrim (black 55% → transparent) plus a translucent
  circular backdrop behind *both* header buttons. Previously the back arrow sat directly on raw
  artwork and disappeared against a bright frame. See `CircleIconButton`.
- **Trailer** demoted to a matching-height outlined icon button rather than a second filled CTA.

### 2. Bottom navigation: six tabs → five
`Home · Manga · NSFW · Search · Library · Settings` → **`Home · Search · Browse · Library · Settings`**

Six items was crowded enough to truncate labels, and it gave the gated adult section the same
prominence as Home. Manga and NSFW are now one **Browse** destination (`ui/screens/browse/BrowseScreen.kt`)
behind a pill segmented control, cross-fading between the two sections.

- Both sections are rendered `embedded = true`, suppressing their own title rows so the host header
  isn't duplicated — `MangaScreen`/`NsfwScreen` gained that parameter rather than being rewritten.
- Each section keeps its own ViewModel and `rememberSaveable` state, so switching segments preserves
  rows, search queries and the NSFW sub-tab (the same guarantee §6/Phase 6 added for NSFW's own tabs).
- **Active-tab feedback:** the selected icon scales to 1.12× over `Motion.FAST` and its label goes
  bold, so the active pill is unmistakable before the indicator colour even lands.

**Migration fix (important).** The cold-boot tab restore persists an *index* into the tab list. Going
from six tabs to five **and reordering them** would have silently pointed returning users at the wrong
destination — e.g. an old index of `1` (Manga) would resolve to `Search`. Added
`SettingsStore.CURRENT_TAB_LAYOUT_VERSION`: `lastTabSnapshot()` returns `FALLBACK_TAB` (Home) when the
stored index predates the current layout, and `NovaApp` stamps the new version after reading it. The
`Routes.MANGA` / `Routes.NSFW` destinations stay registered so those screens remain reachable by route.

### 3. Stream resolution badges
The streams list rendered quality through the generic accent-tinted `RatingBadge`, which carries no
meaning — "480p" and "2160p HDR" looked identical. Replaced with `ResolutionBadge`:

- `resolutionTierOf()` normalises inconsistent add-on strings into `ResolutionTier`
  (HDR / UHD / FHD / HD / SD / UNKNOWN). **HDR is matched before the numeric resolution**, so
  "2160p HDR" doesn't collapse to plain UHD and lose the one bit worth showing.
- Colour encodes the tier (HDR gold → 4K violet → 1080p accent → 720p neutral → SD muted) with a
  matching hairline border and `tnum` figures, so the best stream is identifiable at a glance.
- **Unrecognised quality is never guessed** — it falls back to showing the literal add-on string
  rather than claiming a resolution the stream may not have.
- `StreamRow` no longer repeats the raw quality in its subtitle, which would otherwise duplicate the
  badge.
- **`ResolutionBadgeTest` (4 cases, → 34 total)** covering the common labels, HDR precedence,
  case/whitespace insensitivity, and the unknown/missing fallbacks.

### 4. Press feedback
`ExpandableText`'s "Read more" was a bare `clickable` with no visual response and a small hit area;
it now uses the app's standard 0.96 press shrink with a 32 dp minimum target.

### Files
`ui/screens/browse/BrowseScreen.kt` (new), `ui/nav/NovaNav.kt`, `ui/screens/detail/DetailScreen.kt`,
`ui/components/Components.kt`, `ui/screens/manga/MangaScreen.kt`, `ui/screens/nsfw/NsfwScreen.kt`,
`data/local/SettingsStore.kt`, `app/src/test/java/com/moovio/app/ResolutionBadgeTest.kt` (new).

### Verification
`:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` → **34 tests / 0 failures** ✅ ·
`:app:assembleDebug` + `:app:assembleRelease` ✅ (45 MB debug / 38 MB release) ·
`dist/NovaStream.apk` refreshed. **Needs a device pass (§7 item 16)** — in particular the tab-index
migration, which only manifests on an install that already had a saved tab.

### Known risks (untestable without a device)
- **Tab-index migration:** verify by setting a tab on the old build, then upgrading. An unrecognised
  layout should land on Home rather than the wrong tab.
- `BrowseScreen` composes `MangaScreen`/`NsfwScreen` inside an `AnimatedContent`. The NSFW **lock
  gate** still runs (it lives in `NsfwScreen`), but confirm switching to the NSFW segment while
  locked still shows the lock screen rather than content.
- The detail heart moved from a full-width button to a 40 dp header icon — confirm it still toggles
  and that `DetailViewModel.isFavorite` is unaffected.

---

## 2e. Phase 8 — shared-element transitions & navigation polish

**Topic: motion.** Phases 1–7 built a coherent visual system but the app still *moved* like a set of
disconnected pages: tapping a poster hard-cut to the detail screen, rows popped in all at once, and
bottom tabs swapped instantly.

### Toolchain bump (required)
`SharedTransitionLayout` / `Modifier.sharedElement` **only exist in Compose 1.7+**. The project was
on Compose BOM 2024.06.00 (1.6.8) with Kotlin 1.9.24 and compose-compiler 1.5.14, which cannot reach
them — Kotlin 2.0 moves the Compose compiler into its own Gradle plugin whose version must match the
Kotlin version. Bumped to:

| | Before | After |
|---|---|---|
| Kotlin | 1.9.24 | **2.0.20** (K2 compiler) |
| Compose compiler | `kotlinCompilerExtensionVersion = "1.5.14"` | **`org.jetbrains.kotlin.plugin.compose` 2.0.20** |
| Compose BOM | 2024.06.00 (1.6.8) | **2024.09.02** (1.7.0) |
| AGP | 8.5.0 | 8.5.0 (unchanged) |

The K2 compiler produced **exactly one** incompatibility across the whole codebase:
`HorizontalPager(beyondBoundsPageCount = 3)` in `MangaReaderActivity` — renamed to
`beyondViewportPageCount` in 1.7. Everything else compiled untouched, and all 30 tests still passed.
All three new artifacts were already in the local Gradle cache, so this resolved offline.

### 1. Shared-element poster → detail morph
- **`ui/nav/SharedTransition.kt` (new).** Two CompositionLocals and one helper:
  - `LocalSharedTransitionScopeProvider` — Compose 1.7.0 exposes `SharedTransitionScope` **only as a
    receiver** on the `SharedTransitionLayout` content lambda (there is no
    `LocalSharedTransitionScope` in this release; it landed later), so the receiver is captured once
    at the top of the NavHost and published down to the screens.
  - `LocalAnimatedVisibilityScope` — `Modifier.sharedElement` also needs the *destination's*
    `AnimatedVisibilityScope`, which navigation-compose creates per destination but doesn't expose.
    Each `composable { }` body publishes its own via `ProvideSharedScope(shared, dest) { … }`.
  - `Modifier.sharedElementFor(itemKey)` — pairs source and destination purely by key
    (`posterSharedKey`). **Degrades to a no-op** when either scope is absent (previews, unit tests,
    any screen rendered outside the NavHost), so it's safe to call unconditionally.
- **Both halves tagged.** `PosterCard`'s artwork Box and `DetailScreen`'s header `AsyncImage` call
  `.sharedElementFor(item.key)`. The `boundsTransform` is a single `tween(Motion.SLOW)`, which
  animates position, size and corner radius together — so a 2:3 poster grows into the 16:11 header
  without the corners popping.
- `SharedTransitionLayout` now wraps the entire `NavHost`, and all nine destinations publish their
  scopes, so the morph works from **every** entry point (Home rails, Search, Library, See-all grid,
  Continue Watching) without per-screen wiring.

### 2. Staggered row entrance
`MediaRow` items now fade from 0 and rise the last **12 dp** into place, 40 ms apart. `MediaRow`
switched from `items` to `itemsIndexed` so the index is the lazy-provided one (an earlier
`items.indexOf(item)` would have been both O(n) per item and wrong under recycling).

The animation is keyed on `LaunchedEffect(Unit)`, **not** on the index: a `LazyRow` recycles slots,
so re-keying on the index replays the fade from 0 whenever a different item lands in the same slot —
a visible flash mid-scroll. The delay is capped at `MAX_STAGGER_INDEX = 8`, so the last visible card
never waits more than 320 ms.

### 3. Animated tab + push transitions
- **Tabs** cross-fade (`Motion.MEDIUM`) with a 1/12-width horizontal drift whose direction follows tab
  order, so moving right slides in from the right. `popEnter`/`popExit` are mirrored for Back.
- **Pushed destinations** (detail, collection, add-on manager) get a vertical push — fade plus an
  `it / 8` rise — which reads as "a page was pushed" rather than "another tab slid".
- Previously *no* destination declared transitions, so navigation-compose used its instant default.

### Files
`build.gradle.kts`, `app/build.gradle.kts`, `ui/nav/SharedTransition.kt` (new), `ui/nav/NovaNav.kt`,
`ui/components/Components.kt`, `ui/screens/detail/DetailScreen.kt`,
`ui/player/MangaReaderActivity.kt`.

### Verification
`:app:compileDebugKotlin` ✅ · `:app:testDebugUnitTest` → **30 tests / 0 failures** ✅ ·
`:app:assembleDebug` + `:app:assembleRelease` ✅ (45 MB debug / 38 MB release) ·
`dist/NovaStream.apk` refreshed. **Needs a device pass (§7 item 15)** — shared-element transitions
and K2 codegen are exactly the kind of thing that only proves itself at runtime.

### Known risks (untestable without a device)
- The K2 compiler is a different code generator; while everything compiles and all tests pass,
  runtime behaviour of reflection/Gson paths (`Http.gson`, `MediaItem` deserialization in
  `LibraryScreen.playDownloaded`) is worth re-checking on-device.
- Shared elements require the two destinations to be composed simultaneously during the transition.
  If a poster's morph looks like a hard cut, the likely cause is that screen's `sharedElementFor`
  isn't inside a `ProvideSharedScope` block.

---

## 2d. UI audit backlog (from the premium-feel review)

| # | Item | Size | Notes |
|---|---|---|---|
| ~~15~~ | ~~Shared-element poster → detail transition~~ | — | ✅ Done in Phase 8 (§2e), **removed in the 2026-10-02 visual bug-fix pass** |
| ~~16~~ | ~~Row stagger entrance + tab cross-fades~~ | — | ✅ Done in Phase 8 (§2e) — stagger/tab motion kept; shared elements removed |
| ~~17~~ | ~~Glass elevation system~~ | — | ✅ Done in the QoL follow-up (§2ac) — real shadow + top-edge highlight hairline on `GlassSurface`; M3 `surfaceContainer*` roles mapped in the theme |
| ~~18~~ | ~~Detail CTA hierarchy~~ | — | ✅ Done in Phase 9 (§2f) |
| ~~19~~ | ~~6 → 5 bottom tabs~~ | — | ✅ Done in Phase 9 (§2f) |
| ~~20~~ | ~~`AsyncImage` placeholder / error / crossfade~~ | — | ✅ Done in the QoL polish pass (§2ab) — `SubcomposeAsyncImage` shimmer loading + error glyph; crossfade from the app-wide ImageLoader |
| ~~21~~ | ~~`prefers-reduced-motion`~~ | — | ✅ Done (§2ab) — `rememberReducedMotion()` stops the hero auto-advance and the row stagger |
| ~~22~~ | ~~`EmptyState` with CTA + glow~~ | — | ✅ Done (§2ab) — accent halo + optional CTA; "Retry" wired into Home / Anime / Manga |
| ~~23~~ | ~~Tabular figures app-wide~~ | — | ✅ Done (§2ab + §2ac) — shared `TextStyle.tnum`; ratings/resolutions, Library stats + downloads, Settings subtitles/sliders and the player stats overlay |

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

## 3. API & source status snapshot

### Live probe — 2026-10-06 (browser UA, curl from the build sandbox)

Method: hit the exact endpoints the clients call (listing **and** search paths) with 20–35 s
timeouts, then grep each response for the precise markers its parser needs — a 200 that the parser
then can't read is a broken source, not a working one. Re-run before trusting any row; these sites
churn constantly.

**Metadata / infrastructure**

| Endpoint | Verdict | Evidence |
|---|---|---|
| TMDB API (`fbc3631…`) | ✅ working | `/configuration` 200 (1.1 KB), `/trending/all/week` 200 (11.7 KB) — key still valid |
| TMDB image CDN | ✅ working | real `poster_path` → 200 (20 KB) |
| AniList GraphQL | ✅ working | POST query → 200 + `data` |
| MangaDex API + uploads CDN | ✅ working | `/manga` 200, `uploads.mangadex.org` 200 |
| Kitsu / SIMKL / GitHub API | ✅ working | 200 each |
| MyAnimeList API v2 | ✅ reachable | 405 unauthenticated (expected) |
| Trakt API | ✅ reachable | 412 without api headers (expected) |
| **Jikan v4** | ❌ **unreachable now** | 3× curl timeouts (21 s) **and** an independent second egress (`read_url`) also timed out at 20 s — so it is not a sandbox block. Impact: the NSFW-anime "Jikan Top" row and the Jikan search fallback come back empty; AniList-primary paths are unaffected |

**Repo indexes**

| Repo | Verdict | Evidence |
|---|---|---|
| CloudStream phisher98 | ✅ working | manifest 200, `pluginLists` → plugins.json |
| CloudStream community (recloudstream) | ✅ working | manifest 200 |
| **CloudStream hexated (seeded default)** | ❌ **404 on every branch** | `builds/repo.json`, `master/repo.json` and `main/repo.json` under `hexated/cloudstream-extensions-hexated` all 404. Live mirror: **`JoeTinnySpace/cloudstream-extensions-hexated/builds/repo.json` 200** ("Hexated providers repository"); `redtrillix/…` 404; `self-similarity/MegaRepo` 200 |
| Aniyomi yuzono | ✅ working | 76.5 KB of real entries |
| Aniyomi official | ⚠️ stub | 200 but **1 entry** ("Aniyomi: Google Drive") — superseded by yuzono |
| Keiyoushi `index.pb` | ✅ working | 200 (108.8 KB; needs the gzip-inflate path) |
| Keiyoushi `index.min.json` (seeded 2nd entry) | ⚠️ stub | 200 but 765 B — literally one "Outdated App" record, so the JSON fallback yields nothing |
| Stremio: cinemeta / torrentio / thepiratebay-plus / mediafusion / opensubtitles-v3 | ✅ working | all five manifests 200 |

**Built-in adult sources (Real 18+)**

| Source | Endpoint | Parser vs live markup |
|---|---|---|
| Pornhub | ⚠️ intermittent | search returned 200 (1.07 MB) once, then **connection resets** on every retry; listing `/video?o=mv` timed out ×2. Rate-limited/geo-blocked from here — parser could not be checked |
| XVideos | ⚠️ partial | `/tags` 200 · search `/?k=` timed out ×2 |
| XNXX | ✅ working | listing 200 (slow, 11 s) · search 200 · markers ✅ (82 card blocks) |
| xHamster | ✅ working | `/newest` + `/search/` 200 · markers ✅ (10 `/videos/…` anchors) |
| HQporner | ❌ unreachable | `/top` + `/?q=` timed out ×2 |
| SpankBang | ❌ Cloudflare | 403 "Just a moment" (expected; source shows a bot-protection notice) |
| MissAV | ❌ Cloudflare | 403 "Just a moment" |
| Jable | ❌ Cloudflare | 403 "Just a moment" |
| YouPorn | ❌ effectively broken | search 200 but a **19 KB stub with 0 `/watch/` links** (consent/geo shell); `/most_viewed/` timed out ×2 |
| RedTube | ❌ blocked | `/best` → 302 then stall; `/?search=` timeouts |
| HPJAV | ❌ domain dead | instant connection failure (DNS) — `hpjav.tv` no longer resolves |
| Avgle | ❌ dead | `api.avgle.com/v1` → **520** (origin down); both hosts time out |

**Built-in hentai sources (NSFW anime)**

| Source | Endpoint | Parser vs live markup |
|---|---|---|
| HentaiMama | ✅ working | `/search/<q>` 200 · `<article class="series-card">` ×6, `sc-title` ×6 ✅ |
| HentaiPlay | ✅ working | `/search/<q>` 200 · `<div id="post-` ×12, `entry-title` ×17 ✅ |
| HentaiHaven | ✅ working (primary) | `.xxx` 200 · 147 card markers ✅; the `.org` mirror is dead (timeout) |
| HStream | ✅ working | `/search?q=` 200 · 25 `/hentai/` permalinks ✅ |
| HentaiCity | ✅ working | `/?s=` 200 · 316 markers ✅ |
| MuchoHentai | ✅ working | `/?s=` 200 (failed once, OK on retry) · 15 markers ✅ |
| **Hanime** | ❌ **search broken** | `search.htv-services.com` → **DNS NXDOMAIN**, and the `/search?search=` fallback page is an **Astro JS shell** (65 KB, zero `/videos/hentai/` links) — both of the source's paths yield nothing |
| Hentaigasm | ❌ unreachable | 30 s timeouts ×2 |
| **Haho** | ❌ **both coded endpoints 404** | `/api/search?v=` and `/search?v=` and `/search/<q>` all 404. The site's real search is **`GET https://haho.moe/anime?q=<q>` → 200 with results** |

**Actionable findings — status (source hygiene pass, 2026-10-06 — §2w):**
1. ✅ **done** — `AddonStore.Defaults.repos` now seeds the live `JoeTinnySpace/cloudstream-extensions-hexated` mirror; `Defaults.retiredRepoUrls` + `AddonRepository.ensureDefaultRepos()` prune the dead `hexated/…` URL from existing installs.
2. ✅ **done** — `HahoSource` now searches the live `GET /anime?q=` (series cards) and expands each match into `/anime/{slug}/{n}` episodes; `HentaiTubes.mediaUrls` additionally reads `<source … type="video/…">` so Haho's extension-less filegasm streams resolve.
3. ✅ **done** — `HanimeSource` is retired (unregistered from `HentaiSources.all`): its search API is DNS-dead and its HTML page is a JS shell. The object/tests remain if it returns.
4. ⬜ **open (product decision)** — the seeded **Keiyoushi JSON** entry is a stub (the `.pb` path is the working one) and the **Aniyomi official** index is a 1-entry stub (yuzono is live).
5. ✅ **done** — `HpjavSource` / `AvgleSource` dropped from `AdultSources.all` and `HentaigasmSource` from `HentaiSources.all`, so every load stops reporting those per-source errors. Objects/tests remain if the hosts come back.

> Caveat: 403-Cloudflare and timeout rows can be caused by this sandbox's IP/geo rather than a truly
dead site — a phone on a residential/mobile network may fare better (that is what §7 items 21/23/31
are for). Rows confirmed as *code or markup* problems (hexated 404, Haho 404s, Hanime DNS, HPJAV
DNS, Avgle 520, Keiyoushi/Aniyomi stubs) are independent of network location.

### Audit-time snapshot (2026-09-30) — kept for history

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
  — **superseded 2026-10-22:** the material now comes from `local.properties`
  (`moovio.storeFile` / `moovio.storePassword` / `moovio.keyAlias` / `moovio.keyPassword`) or the
  `MOOVIO_*` env vars, the key lives outside the repo and was rotated (§2ax).
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

### 🟠 ISSUE-15 — Backup export writes tracker tokens and cookies in plain text
- **Symptom:** the JSON backup contains the Trakt/AniList/MAL access tokens and the global
  `Cookie` header, unencrypted, in a file the user picks anywhere on the device.
- **Risk:** anyone with read access to the exported file (cloud backup, shared folder) gets
  working tracker sessions.
- **Fix:** done in §2be — AES-256-GCM with a key generated in the Android KeyStore
  (`data/integrations/BackupCrypto.kt`). Container is `MAGIC + 12-byte IV + ciphertext/tag`, so an
  exported file carries no readable secrets and a tampered one fails the tag. A legacy plaintext file
  still imports, but only after a dialog that names what it contains.
- **Files:** `data/integrations/BackupManager.kt`, `data/integrations/BackupCrypto.kt`,
  `ui/screens/settings/SettingsScreen.kt`
- **Status:** **closed (§2be)** — compile + unit verified (encrypted round trip, tamper and
  foreign-key rejection, legacy plaintext recognition/parse). The KeyStore path itself is
  device-only, so it is still unverified on hardware.

### 🟠 ISSUE-16 — Scrobbling posts foreign ids to AniList / MAL / Kitsu
- **Symptom:** a movie watched in the app is scrobbled with a TMDB id where the provider expects
  its own id, so tracker history fills with wrong entries.
- **Root cause:** `ScrobbleManager` sends the item's `tmdbId` to every provider; only Trakt
  resolves a provider id.
- **Fix:** resolve the provider's own id (or skip the provider) instead of sending the TMDB id.
- **Files:** `data/integrations/ScrobbleManager.kt` (`:73`, `:131`, `:142`)

### 🟠 ISSUE-17 — NSFW unlock survives a restart
- **Symptom:** the NSFW area can be open on the next launch.
- **Root cause:** the unlock flag is persisted in DataStore; the auto-lock only re-locks when
  *all* activities stop, so a process death in the foreground leaves it open.
- **Fix:** keep the unlock in memory (an `AppContainer` `MutableStateFlow`), persist only the
  PIN/biometric preference.
- **Files:** `data/local/SettingsStore.kt` (`:142`), `ui/screens/nsfw/NsfwHubScreen.kt`

### 🟠 ISSUE-18 — HLS downloads cannot be exported to a custom folder

> **Prerequisite done (§2be, shipped in v1.11.0):** the downloader now reclaims orphaned cache
> chunks when free space drops under a user-set floor (`DownloadCacheMaintenance.isBelowLimit` /
> `shouldSweep`, `VideoDownloadManager.sweepOrphansIfLowOnSpace`, `SettingsStore.LOW_SPACE_MB`
> default 1024 MB, swept at boot from `MoovioApp`). The export-to-custom-folder part below is still
> **open**.
- **Symptom:** "Copy downloads to folder" produces nothing for HLS titles.
- **Root cause:** Media3 caches HLS segments under per-segment keys, but the exporter looks the
  download up by the download id, so `getCachedSpans` is empty and the export bails out.
- **Fix:** export HLS as a directory (playlist + segments) or remux with Media3's
  `DownloadManager`; never concatenate segments into one unplayable file.
- **Files:** `data/download/DownloadExporter.kt` (`:46`), `data/download/VideoDownloadManager.kt` (`:536`)

### 🟡 ISSUE-19 — Manga download "pause" reports a failure
- **Symptom:** pausing a chapter download shows "Download failed".
- **Root cause:** `runCatching` swallows the `CancellationException` thrown by `pause()`, so the
  cancelled job overwrites the `PAUSED` status with `FAILED`.
- **Fix:** use the repo's `runCatchingCancellable` helper and check `isActive` before writing.
- **Files:** `data/download/MangaDownloadManager.kt` (`:166`)

### 🟡 ISSUE-20 — Player is released while the composition still holds it
- **Symptom:** rare "player released" errors on exit; the 250 ms ticker and `PlayerView` can
  touch a released `ExoPlayer`.
- **Root cause:** `player?.release()` runs in `onDestroy`, but Compose disposes the composition
  only after the decor view detaches.
- **Fix:** detach `playerView.player` and release in `onStop`, or guard the ticker with a
  `released` flag.
- **Files:** `ui/player/PlayerActivity.kt` (`:1060`, `:1435`)

### 🟡 ISSUE-21 — HTTP 429 backoff blocks OkHttp dispatcher threads
- **Symptom:** up to 3 × 5 s of `Thread.sleep` per request on a shared dispatcher thread; a
  cancelled coroutine still blocks it.
- **Fix:** move the retry into the suspend layer (it already has a backoff loop) or use a
  cancellable delay; keep the interceptor to a single attempt.
- **Files:** `data/remote/Http.kt` (`:33`, `:48-66`)

### 🟡 ISSUE-22 — The website's `npm run lint` cannot succeed
- **Symptom:** `eslint` is not a dependency and no ESLint config exists, so the script always
  fails; `next lint` is also removed in Next 16.
- **Fix:** add `eslint` + `eslint-config-next` with a flat config, or replace the script with
  `tsc --noEmit`.
- **Files:** `website/package.json`, `website/`

### 🟡 ISSUE-23 — The website references Tailwind colour tokens that do not exist
- **Symptom:** the drag-rail progress fill has no background, arrow buttons get Tailwind's default
  border, and the "Drag" label renders at full white.
- **Root cause:** `border-ink`, `text-ink`, `bg-ink`, `text-mist` are not defined in
  `tailwind.config.ts` (the palette is `void/paper/muted/faint/accent/signal/vermilion/…`).
- **Fix:** map them to existing tokens or extend the theme.
- **Files:** `website/components/ui/drag-rail.tsx` (`:279-308`), `website/components/PhoneMockup.tsx`,
  `website/tailwind.config.ts`

### 🟡 ISSUE-24 — The website is invisible without JavaScript
- **Symptom:** every major section is `opacity: 0` until an `IntersectionObserver` adds
  `is-shown`; any JS error leaves a blank page.
- **Fix:** add a no-JS fallback (`<noscript>` style or `@media (scripting: none)`) and drive
  visibility from CSS where possible.
- **Files:** `website/app/globals.css` (`:208-223`)

### 🟡 ISSUE-25 — `next.config.mjs` has no `output: "export"` although the README promises a static build
- **Symptom:** `npm run build` produces a Node server bundle; a static host serves nothing.
- **Fix:** add `output: "export"` + `images: { unoptimized: true }`, or correct the README and
  document the Vercel/Node target.
- **Files:** `website/next.config.mjs`, `website/README.md`

### 🟡 ISSUE-26 — No CI, no `androidTest`, no instrumentation
- **Symptom:** "292 tests, all green" is asserted, never enforced; nothing covers the Compose UI,
  DataStore, WorkManager workers, the download stack, the extension engine or the CloudStream
  shims (the highest-risk code in the repo has zero tests).
- **Fix:** add a GitHub Actions workflow running `compileDebugKotlin` + `testDebugUnitTest` +
  `assembleRelease` on every push, and grow the JVM-testable surface (the shims, `Http`,
  `SettingsStore`, the download managers).
- **Files:** `.github/workflows/`, `app/src/test/`

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

### Build & sign locally (as of v1.10.0, §2ax)

`local.properties` (untracked, git-ignored) needs a real `sdk.dir` — it used to point at a
non-existent Linux path, which is why the build could not run on the dev machine:

```properties
sdk.dir=C:/Users/<you>/AppData/Local/Android/Sdk
TMDB_API_KEY=…                 # optional; metadata falls back to the add-ons
moovio.storeFile=C:/secure/moovio-release.jks
moovio.storePassword=…
moovio.keyAlias=moovio
moovio.keyPassword=…
```

The signing values may instead come from `MOOVIO_STORE_FILE` / `MOOVIO_STORE_PASSWORD` /
`MOOVIO_KEY_ALIAS` / `MOOVIO_KEY_PASSWORD` (the file wins). **The keystore must live outside the
repository** — the previous one was committed once and had to be rotated (§8).

```bash
./gradlew :app:compileDebugKotlin      # compile
./gradlew :app:testDebugUnitTest       # 292 unit tests
./gradlew :app:assembleDebug           # debug APK
./gradlew :app:assembleRelease         # signed release APK (needs the material above)

# what the release actually is:
$ANDROID_HOME/build-tools/34.0.0/aapt2 dump badging app/build/outputs/apk/release/app-release.apk | head -1
#   package: name='com.moovio.app' versionCode='19' versionName='1.10.0' …
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs app/build/outputs/apk/release/app-release.apk
#   Signer #1 certificate SHA-256 digest: 2edb949b…351379

# the published artifact, end to end:
curl -sL -o /tmp/apk https://github.com/speckle-hub/nexusstream/releases/latest/download/app-release.apk
sha256sum /tmp/apk   # ED8C54188C4512B020AC1BE18F19290EEA7B617438302723D172ECD35839D54F
```

Website: `cd website && npm run build` (Next.js 15 static export; 8/8 pages). `npm run lint` is
currently **broken** — `eslint` is not a dependency and no config exists (see §4 ISSUE-15).

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
| 2026-10-01 | **Phase 6 — Home hero:** `FeaturedCarousel` goes full-bleed (0 content padding, dots overlaid BottomCenter); `HeroSlide` rewritten full-bleed with screen-width-derived height (400–540 dp clamp), radial artwork glow, top/bottom scrims and a center-aligned content stack (title, `Type • Genre • Year • rating`, description, white Play pill); old boxed `HeroSlide` deleted. |
| 2026-10-01 | **Phase 6 — player timeline:** `PlayerScreen` gained `position`/`duration`/`isPlaying` state with a 250 ms ticker + `onIsPlayingChanged` listener; slider now uses `onValueChange`/`onValueChangeFinished` (one seek on release, thumb-follows-finger while scrubbing) instead of seeking per pixel. |
| 2026-10-01 | **Phase 6 — gesture HUD + smooth seek:** brightness/volume/seek show an animated HUD (vertical level bars on the edges, seek pill in the center) that fades 1.4 s after release; `onGestures` returns the level, new `onGestureEnd`; horizontal seeks are batched and flushed every 300 ms (`SEEK_FLUSH_MS`) plus once on release (removes the stutter from one `seekTo` per frame); volume commits whole steps (`VOLUME_STEP`) from a fractional accumulator. |
| 2026-10-01 | **Phase 6 — nav state:** `SearchScreen` query → `rememberSaveable` with a re-search guard (`results.isEmpty()`), so returning to the tab keeps term + results; Home/Library/Search use `rememberLazyListState()`. Confirmed no `FLAG_ACTIVITY_*`/launch-mode flags exist to strip. |
| 2026-10-01 | **Phase 6 fix — NSFW sub-tab/search lost on Back:** returning from a detail page used to reset the NSFW section to the NSFW Anime tab with an empty search box (the ViewModel still held the results, but the UI showed browse rows). `NsfwContent`'s sub-tab index and all three per-section queries are now `rememberSaveable`, with the queries hoisted into the destination so sub-tab switches can't drop them either; `NsfwViewModel` tracks `animeSearchedFor`/`mangaSearchedFor`/`realSearchedFor` so a query restored after process death re-runs its search exactly once, while a query the ViewModel already holds is never re-queried. |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **30 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL; `dist/NovaStream.apk` refreshed. Device pass (§7) still pending. |
| 2026-10-01 | **Built-in NSFW-anime stream sources:** new modular `data/hentai/` package (`HentaiSource` interface, `HentaiMamaSource` + `HentaiPlaySource`, `HentaiSources` registry, `HentaiRepository` aggregator with query variants, series grouping, relevance scoring and 10-min caches). NSFW Anime now lists episodes from the sources (AniList metadata untouched) and `StreamRepository.streamsFor()` resolves built-in streams first, with enabled Stremio NSFW add-ons appended as extra/fallback. Added `httpPostForm()` to `Http.kt` (admin-ajax form POST). See §10. |
| 2026-10-01 | Live verification of the new chain end-to-end: HentaiMama search → series → episode → `admin-ajax.php get_player_contents` → `?dt_embed=` iframe → direct `gdvid.info/….mp4` (`206 video/mp4`, valid `ftyp`); HentaiPlay search → episode page → `<source>` direct MP4 (`206`, works without Referer). |
| 2026-10-01 | `:app:compileDebugKotlin` ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL; `dist/NovaStream.apk` refreshed. Unit tests skipped this session (explicitly requested). |
| 2026-10-02 | **Phase 7 (P0) — design tokens + adaptive grids:** new `ui/theme/Tokens.kt` (`AppSpacing`, `Motion`). `CollectionScreen`/`LibraryScreen`/`SearchScreen` moved off `GridCells.Fixed(3)` and `chunked(3)` onto `GridCells.Adaptive(minSize = 108.dp)` — Library is now one `LazyVerticalGrid` with full-span headers (nested grid-in-column crashes on unbounded height) and `GridRow` is deleted. |
| 2026-10-02 | **Phase 7 (P0) — a11y + feel:** remove `×` touch targets on `PosterCard`/Continue Watching raised from 24/26 dp to 48 dp (visual chip stays small); `NsTextTertiary` `#6A6A7A` → `#8A8A9C` (~3.5:1 → ~5.1:1, was below WCAG AA) and light-mode tertiary `#8888A0` → `#6E6E85`; press feedback (0.96× scale over 120 ms) added to `PosterCard` and `GlassSurface`; `ShimmerBox` now sweeps a gradient instead of pulsing alpha, plus a new `PosterCardSkeleton`. |
| 2026-10-02 | **Phase 7 (P1) — typography:** `NovaTypography` completed from 8 to all 15 M3 roles with per-size tracking (-0.8 sp at 36 sp → +0.8 sp on `labelSmall`), explicit line heights (~1.45× body), and the ~15 redundant `fontWeight = FontWeight.Bold` call sites stripped so weight is token-controlled. `RatingBadge` uses `fontFeatureSettings = "tnum"`. |
| 2026-10-02 | **Phase 7 (P1) — search:** raw `OutlinedTextField` replaced with a custom filled `SearchField` (`BasicTextField`, hairline pill, focus-tinted glyph, 36 dp clear button, pulsing in-flight dot). Empty state now offers recent searches + trending terms as chips. Recent searches persisted via a new `RECENT_SEARCHES` DataStore preference (newest-first, capped at 8, min 2 chars) written only on deliberate searches, not live keystrokes. Results are an adaptive grid; loading shows 9 shimmer skeletons instead of a centred "Searching…" text. |
| 2026-10-02 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **30 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (44 MB / 37 MB); `dist/NovaStream.apk` refreshed. |
| 2026-10-02 | **Toolchain:** Kotlin **1.9.24 → 2.0.20** (K2), compose compiler `1.5.14` → `org.jetbrains.kotlin.plugin.compose` 2.0.20, Compose BOM **2024.06.00 → 2024.09.02** (1.6.8 → 1.7.0). Required for `SharedTransitionLayout` / `Modifier.sharedElement`, which don't exist below Compose 1.7. AGP left at 8.5.0. All three artifacts were already in the local Gradle cache (resolved offline). K2 produced exactly **one** incompatibility: `HorizontalPager(beyondBoundsPageCount=)` → `beyondViewportPageCount=` in `MangaReaderActivity`. All 30 tests passed unchanged. |
| 2026-10-02 | **Phase 8 — shared-element poster → detail morph:** new `ui/nav/SharedTransition.kt` captures `SharedTransitionScope` from the layout receiver (Compose 1.7.0 has no `LocalSharedTransitionScope`) and publishes it plus each destination's `AnimatedVisibilityScope` via `ProvideSharedScope`. `Modifier.sharedElementFor(item.key)` tags both `PosterCard`'s artwork and `DetailScreen`'s header image; degrades to a no-op outside the NavHost. `SharedTransitionLayout` wraps the NavHost and all 9 destinations publish scopes, so the morph works from every entry point. |
| 2026-10-02 | **Phase 8 — motion:** `MediaRow` items now fade in and rise 12 dp, 40 ms apart (`itemsIndexed`; `LaunchedEffect(Unit)` so slot recycling can't replay the fade). Tabs cross-fade at `Motion.MEDIUM` with a 1/12-width directional horizontal drift (mirrored for Back); detail/collection/add-ons get a vertical push. Previously every destination used navigation-compose's instant default. |
| 2026-10-02 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **30 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45 MB / 38 MB); `dist/NovaStream.apk` refreshed. **Device pass (§7 item 15) still pending** — K2 codegen and shared elements both need on-device confirmation. |
| 2026-10-02 | **Phase 9 — detail CTA hierarchy:** `PrimaryActionButton` (accent fill + accent-tinted outer glow, spinner while resolving, standard 0.96 press shrink) is now the single dominant action. The full-width "Add to Library" button was **removed** and the heart moved into a new detail app bar as a compact circular toggle. Added a 104 dp top scrim plus translucent circular backdrops behind both header buttons — previously the back arrow sat on raw artwork and vanished against bright frames. Trailer demoted to an outlined icon button of matching height. |
| 2026-10-02 | **Phase 9 — bottom bar 6 → 5 tabs:** `Home · Search · Browse · Library · Settings`. Manga + NSFW merged into a new `ui/screens/browse/BrowseScreen.kt` behind a pill segmented control with a cross-fade between sections. `MangaScreen`/`NsfwScreen` gained an `embedded` flag that suppresses their own title rows (rather than being rewritten) and each keeps its own ViewModel + `rememberSaveable` state. Selected tab icon now scales 1.12× and its label goes bold. |
| 2026-10-02 | **Phase 9 — tab-index migration:** the cold-boot tab restore persists an *index* into the tab list, so going 6 → 5 tabs **and reordering them** would have silently opened returning users' apps on the wrong tab. Added `SettingsStore.CURRENT_TAB_LAYOUT_VERSION`; `lastTabSnapshot()` returns `FALLBACK_TAB` (Home) on a version mismatch and `NovaApp` stamps the new version after reading. `Routes.MANGA`/`Routes.NSFW` stay registered so both screens remain routable. |
| 2026-10-02 | **Phase 9 — resolution badges:** streams no longer use the generic accent-tinted `RatingBadge` (which made "480p" and "2160p HDR" look identical). New `ResolutionBadge` + `resolutionTierOf()` normalises inconsistent add-on strings to `ResolutionTier` (HDR/UHD/FHD/HD/SD/UNKNOWN), **matching HDR before the numeric resolution** so "2160p HDR" doesn't collapse to plain UHD, and **never guessing** an unrecognised quality. Added `ResolutionBadgeTest` (4 cases → **34 tests**). `ExpandableText`'s "Read more" gained press feedback and a 32 dp target. |
| 2026-10-02 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **34 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45 MB / 38 MB); `dist/NovaStream.apk` refreshed. **Device pass (§7 item 16) still pending** — the tab-index migration especially only manifests on an install that already had a saved tab. |
| 2026-10-02 | **Phase 10 — Home hero edge-to-edge:** removed the `44.dp` top content padding; the hero is now the first item and draws from y=0 behind the status bar. `HomeTopBar` floats over it (`statusBarsPadding()` + a 130 dp black→transparent scrim, white logo/icons), falling back to a normal inset-aware first row when there is no hero. `ui/screens/home/HomeScreen.kt`. |
| 2026-10-02 | **Phase 10 — detail header insets:** header height now derives from screen width (`*0.95f`, clamped 320–460 dp) instead of a fixed 16:11, the app-bar row uses `statusBarsPadding()` instead of `top = 36.dp`, and the title is capped at 2 lines — the title block no longer grows up into the back/heart buttons on narrow phones. Genre chips gained a 12 dp spacer above them. `ui/screens/detail/DetailScreen.kt`. |
| 2026-10-02 | **Phase 10 — search field fix:** `SearchScreen` never wrote its `rememberSaveable` `query`, so `SearchField.value` never changed and typed input looked dead. `onValueChange` now sets `query = it` before dispatching the debounced search. `ui/screens/search/SearchScreen.kt`. |
| 2026-10-02 | **Phase 10 — grid clipping / shared-element bleed:** new `LocalSharedElementsEnabled` local; `sharedElementFor` no-ops when disabled. All scroll containers (MediaRow, Library, Search, Collection, Manga, NSFW) publish `!isScrollInProgress`, so the Compose 1.7 transition overlay can't draw a scrolling poster over a header/search field. Added `clipToBounds()` to every viewport (and the Browse `AnimatedContent`) plus `zIndex(1f)` on headers/segmented controls. `ui/nav/SharedTransition.kt`, `ui/components/Components.kt`, six screens. |
| 2026-10-02 | **Phase 10 — Library restructure:** split into two segmented-control tabs — **Downloads** (video downloads + downloaded chapters) and **My Library** (Continue Watching + Favourites). Each tab has its own grid + scroll state; selected tab is `rememberSaveable`. `ui/screens/library/LibraryScreen.kt` rewritten. |
| 2026-10-02 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **34 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (46 MB / 38 MB); `dist/NovaStream.apk` refreshed. **Device pass (§7 item 17) pending.** |
| 2026-10-02 | **Phase 11 — player:** bottom sheet with 0.5x–2.0x speed + Audio boost / Normalize toggles (`LoudnessEnhancer` on the player audio session); autoplay overlay moved from 95%-of-duration to **15 s remaining**; **Skip Intro/Outro** buttons (stream markers via new `StreamSource.introEndMs`/`outroStartMs`, else a 90 s heuristic); VLC/MPV hand-off sheet; double-tap ±10 s with expanding-ring ripple; pinch-to-zoom cycles Fit/Stretch/Fill/16:9/21:9 (hand-rolled `awaitEachGesture` so it only claims two-finger gestures); screen-lock toggle; Cast/DLNA action; subtitle styling via `CaptionStyleCompat`. PiP now only enters while playing and sets `setAutoEnterEnabled` on API 31+. `ui/player/PlayerActivity.kt`. |
| 2026-10-02 | **Phase 11 — theme engine:** Material You dynamic color (`dynamicDark/LightColorScheme`, API 31+) that replaces the manual accent with the system primary; named preset accents **Neon Purple / Emerald / Crimson / Sunset Orange**; **True AMOLED black** (`#000000` background+surface, `#0B0B0B` elevated) propagated app-wide via `LocalNovaColors`; subtitle size/colour/opacity/offset settings. `ui/theme/Theme.kt`, `ui/theme/Color.kt`, `ui/MainActivity.kt`, `data/local/SettingsStore.kt`, `ui/vm/ViewModels.kt`, `ui/screens/settings/SettingsScreen.kt`. |
| 2026-10-02 | **Phase 11 — manga reader:** landscape **double-page spreads** (RTL-correct ordering); night-mode **invert** + **grayscale** as a Compose `ColorMatrix` filter; **zoom lock** across pages; per-chapter **position saving/restore** (`MANGA_PROGRESS`); **auto-crop borders** (heuristic scale-and-clip); **volume-key page turns** via a `MutableSharedFlow` the active reader collects. `ui/player/MangaReaderActivity.kt`. |
| 2026-10-02 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **34 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45.6 MB / 38 MB); `dist/NovaStream.apk` refreshed. **Device pass (§7 item 18) pending** — PiP, gestures, the audio effect and the Cast chooser are runtime-only. |
| 2026-10-02 | **Phase 12 — source & extension manager:** new `SourcesScreen` dashboard (per-extension enable/disable + a **Test** button reporting latency/item count via `SourceTester`); `SettingsStore.disabledSources`; `StreamSource.alternates` + `PlayerActivity` `onPlayerError` **mid-playback mirror failover**; global **custom User-Agent/Referer/Cookie** applied by `Http.globalIdentity()` and an OkHttp interceptor on the player data source (source-specific headers still win). `ui/screens/sources/SourcesScreen.kt`, `data/integrations/SourceTester.kt`, `data/remote/Http.kt`, `ui/player/PlayerActivity.kt`. |
| 2026-10-02 | **Phase 12 — downloader & offline:** user-set **max parallel downloads**; SAF **storage-folder picker** (persistable URI); **auto-download the next episode** on completion, gated on a toggle + Wi-Fi-only check via `ConnectivityManager`. `data/local/SettingsStore.kt`, `ui/player/PlayerActivity.kt`, `ui/screens/settings/SettingsScreen.kt`. |
| 2026-10-02 | **Phase 12 — discovery, sync & backup:** **Home row reordering** screen (up/down priority, persisted, stably applied by `applyRowOrder`); **watch-statistics dashboard** (new Library Stats tab, `WatchStats.compute`); **JSON backup/restore** of favourites/history/settings/tokens via SAF (`BackupManager`); **multi-provider scrobbling** for Trakt/AniList/MAL/Kitsu/SIMKL with manually-pasted tokens (`ScrobbleManager`). `ui/screens/settings/HomeOrderScreen.kt` (new), `ui/screens/library/LibraryScreen.kt`, `data/integrations/BackupManager.kt`, `data/integrations/ScrobbleManager.kt`. |
| 2026-10-02 | **Phase 12 — security & system:** **app lock** startup gate (`BiometricPrompt` + device-credential, fails open); **incognito mode** (`LibraryStore.incognito` makes `recordWatch` a no-op); **`FLAG_SECURE`** toggle; **GitHub release updater** with in-app APK install via the existing `FileProvider` (+`REQUEST_INSTALL_PACKAGES`); new periodic **`NewContentWorker`** that notifies on genuinely new feed titles. `ui/MainActivity.kt`, `data/integrations/UpdateChecker.kt`, `data/sync/NewContentWorker.kt` (new), `AndroidManifest.xml`. |
| 2026-10-02 | Added `WatchStatsTest` (3 cases). `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **37 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45.8 MB / 38 MB); `dist/NovaStream.apk` refreshed. **Device pass (§7 item 19) pending.** |
| 2026-10-03 | **Phase 13 — Library crash fix (P0):** root-caused the Favourites crash to Gson decoding a missing/unknown `type` onto the non-null `MediaItem.type` slot (silent runtime `null` → NPE at `item.type.id` in `Routes.detail`), compounded by an all-or-nothing `fromJson` that blanked the whole list on one bad element. New pure-JVM `data/local/LibraryCodec.kt` sanitizes **per element** (bad entries dropped, rest survive; enum name / catalog id / `stremioType` all resolve the type, defaulting to `MOVIE`; null-safe optional fields; clamped numbers; dedupe by key) and `LibraryStore`/`LibraryViewModel` now route every read/write through it, with a second `filterSafe` guard in the VM and `runCatching` around `WatchStats.compute`. Added `LibraryCodecTest` (15 cases → **52 tests**). |
| 2026-10-03 | **Phase 13 — Library category chips:** new `LibraryFilter` enum (All/Movies/TV/Anime/NSFW) rendered as a horizontal `LibraryFilterBar` under the segmented control for Downloads and My Library. Filters both the video/chapter downloads **and** the saved-title grids in place (scroll retained); Anime also matches `MANGA`; `VideoDownload.matches` decodes `itemJson` through `LibraryCodec`; `MangaDownload` gained an `nsfw` flag fed from `MediaType.NSFW_MANGA`. |
| 2026-10-03 | **Phase 13 — Detail overhaul:** seasons are now a `SeasonPickerRow` chip row (`toggleSeason` → `selectSeason`, no collapse); `EpisodeRow` rebuilt as thumbnail + `E3 · Title` + duration/air date + saved-progress bar (`Video.runtimeMin` from TMDB `eo.runtime`); the inline stream list moved into a bounded Material3 **`StreamSheet`** `ModalBottomSheet` opened by an episode tap or the new header `PlaylistPlay` button; `loadStreams` is generation-guarded; `Play` pre-targets the selected season's first episode and `play()` closes the sheet before the torrent overlay. |
| 2026-10-03 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **52 tests / 0 failures** ✅; `:app:assembleRelease` → BUILD SUCCESSFUL (38,166,194 bytes, signed); `dist/NovaStream.apk` refreshed. **Device pass (§7 item 20) pending.** |
| 2026-10-03 | **Real 18+ expansion — 3 general tubes:** new `XhamsterSource` (`/newest`, `/best`, `/trending`, `/search/{q}`, `/categories`), `YouPornSource` (`/most_viewed/`, `/best/`, `/recent/`, `/search/?query=`) and `RedTubeSource` (`/best`, `/newest`, `/?search=`). YouPorn/RedTube prefer the Aylo `api/video/media_definitions/{id}/` JSON endpoint; all fall back to page-markup extraction. |
| 2026-10-03 | **Real 18+ expansion — 4 JAV sources:** `MissavSource` (`/en/new|release|trending|today`, search, genres; `surrit.com` `.m3u8`), `JableSource` (`/latest-updates/`, `/hot/`, `/new/`, search, tags; `var hlsUrl`), `HpjavSource` (WordPress `/lastest-updates/`, `/popular/`, `?s=`; JW Player `file:`), and `AvgleSource` (clean JSON API `api.avgle.com/v1` → `video_url`). |
| 2026-10-03 | **Shared `TubeMedia.kt`** added: one extractor for absolute/protocol-relative/escaped MP4+HLS URLs, quality labelling, ad/placeholder filtering, highest-first ordering, the `definitions()` Aylo-JSON parser and the common thumbnail helper. `AdultSources.all` now lists **12** sources; progressive loading, per-source caching, error isolation, ranked search and stream pre-flight all apply automatically. Added `AdultSourcesExtraTest` (11 cases → **63 tests**). These seven sources are **not** live-probed (see §2k/§9); the parsers use multi-fallback extraction. |
| 2026-10-03 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **63 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45.9 MB debug / 38.2 MB release, signed); `dist/NovaStream.apk` refreshed (38,182,578 bytes). **Device passes (§7 items 20 & 21) pending.** |
| 2026-10-03 | **Static-review fixes (navigation + logic):** `NovaNav.kt` now matches the tab route to the `search?q={q}` destination via a new `routeBase()`/`tabForRoute()` helper, so the bottom bar stays visible on Search, the tab highlights, and Search persists as the last tab; `NsfwViewModel.searchAnime`/`searchManga` reset their `*Searching` flags on the short-query early return **and** in a `try/finally` (covers cancellation); `DetailScreen.pickStreams` retries when the stream list is empty and not loading. |
| 2026-10-03 | **Static-review fixes (UI/a11y):** Home Continue Watching `×` is now a 48 dp touch target (visible chip unchanged at 26 dp); Home's empty and error states are mutually exclusive (were stacked); the Search "Clear" recents link is a 48 dp ripple target. |
| 2026-10-03 | **Static-review fixes (consistency/security):** the manga reader now uses the live theme settings (Material You / accent / AMOLED) instead of a hardcoded dark-violet theme, and only consumes volume keys once pages are shown (new `onReady` callback); `AppLockGate`'s `unlocked` uses `rememberSaveable` so rotation no longer re-prompts; the Home carousel `LaunchedEffect` is keyed on the item list. |
| 2026-10-03 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **63 tests / 0 failures** ✅; `:app:assembleRelease` → BUILD SUCCESSFUL; `dist/NovaStream.apk` refreshed (38,182,578 bytes). **Device pass (§7 item 22) pending.** |
| 2026-10-03 | **NSFW-anime expansion — 7 new hentai sources:** `HanimeSource` (hanime.tv JSON: `POST search.htv-services.com/` + `GET /api/v8/video` `videos_manifest`), `HentaiHavenSource`, `HStreamSource`, `HentaigasmSource`, `MuchoHentaiSource`, `HentaiCitySource` (WordPress `?s=` search) and `HahoSource` (`/api/search?v=` JSON). `HentaiSources.all` now lists **9**; `HentaiRepository` gives them parallel querying, per-source error isolation, relevance ranking, TTL caches and stream pre-flight automatically, with AniList metadata untouched. |
| 2026-10-03 | **Shared `HentaiTubes.kt`** added: direct-media-URL extractor (snapshot-safe), `qualityOf`, page-referer stream builder, a WordPress-style card parser (post-block scoped, nested-`<img>` anchors, tag-stripped titles), `isContentLink` chrome filter and a one/two-hop embed resolver. Added `HentaiSourcesExtraTest` (10 cases → **73 tests**). The seven sources are **not** live-probed (see §2m/§10). |
| 2026-10-03 | `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **73 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (46.1 MB debug / 38.2 MB release, signed); `dist/NovaStream.apk` refreshed (38,198,962 bytes). **Device pass (§7 item 23) pending.** |
| 2026-10-03 | **Final UI & app checkup — player theme + touch targets + rotation state:** `PlayerActivity` observes the live theme settings (mode / accent / Material You / AMOLED, system mode via `isSystemInDarkTheme()`) instead of a hardcoded `NovaStreamTheme(darkTheme = true, accentKey = "violet")`; the track/episode/speed/external bottom sheets take their surface from `MaterialTheme.colorScheme.surface` and their accent from `LocalNovaColors` (was `0xFF14141C` / `0xFF7C5CFF`). `SearchField`'s clear button 36 → **48 dp**; the NSFW search box is a `SearchField` (was a raw `OutlinedTextField`, so it gains the in-flight dot + clear affordance); subtitle colour swatches are **48 dp** targets; detail `CircleIconButton` is a **48 dp** target around its 40 dp disc. `AddonManagerScreen`'s tab and `HomeOrderScreen`'s working order → `rememberSaveable` (rotation keeps tab + in-progress reorder); `HomeScreen` row order → keyed `remember(rawRows, homeOrder) { applyRowOrder(...) }`; `CollectionScreen` header `top = 44.dp` → `statusBarsPadding()`. |
| 2026-10-03 | **Checkup follow-up — player sheets readable in light theme:** the sheets now sit on a themed surface but their text was still hardcoded `Color.White` (invisible on a light surface). Headings, track rows, unselected speed-chip labels, boost/normalize rows and the external-player heading use `LocalNovaColors` text roles; the countdown card uses `onSurface`/`onSurfaceVariant`; unselected speed chips use `surfaceElevated`; the control-bar `AssistChip`s keep a fixed translucent-black container + white label since they float over video. `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **73 tests / 0 failures** ✅; `:app:assembleRelease` → BUILD SUCCESSFUL (38,198,962 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `4A2A0BF0…C246`). **Device pass (§7 item 24) pending.** |

| 2026-10-04 | **NSFW search speed + metadata fallback (§2o):** new `JikanClient.details(malId)` / `detailsByTitle(title)` map Jikan's `/anime/{id}/full` into a full `MetaDetail`. `MetadataRepository.detail()` now tries **AniList → Jikan** for NSFW anime (stopping early when AniList already has a synopsis), and returns a catalog-item fallback instead of `null` when both fail — the detail page no longer hard-fails, and the built-in hentai sources still fill the episode list. NSFW-anime search now routes through a new `CatalogRepository.searchNsfwAnime()`: **AniList primary**, Stremio NSFW add-ons concurrent, **Jikan only if AniList is empty** (6 s cap). `data/remote/JikanClient.kt`, `data/repo/MetadataRepository.kt`, `data/repo/CatalogRepository.kt`. |
| 2026-10-04 | **Real 18+ search speed (§2o):** `AdultRepository.searchDetailedProgressive` emits ranked partial results after each source; `CatalogRepository.searchRealProgressive` forwards them and `NsfwViewModel.searchReal` paints them generation-guarded, so one slow/hung source no longer blocks the search. Each source is bounded by `withTimeout(8 s)` (timeouts not retried), concurrency is capped with a `Semaphore(10)`, and ranked searches are cached by query for 5 min (LRU 32). `data/adult/AdultRepository.kt`, `data/repo/CatalogRepository.kt`, `ui/vm/ViewModels.kt`. |
| 2026-10-04 | **Compact dismissible notice (§2o):** the NSFW "sources unavailable" banner is now a single slim line with an **×** to dismiss; tapping the summary expands the per-source details on demand. `ui/screens/nsfw/NsfwScreen.kt`. `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **73 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (46,090,540 / 38,215,346 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `5F3E9ACA…F785`). **Device pass (§7 item 25) pending.** |
| 2026-10-04 | **Phase 14 — downloader HTTP headers (§2p):** downloads failed on header-gated hosts because Media3's `DownloadManager` used one bare `DefaultHttpDataSource` (UA only) while the player sent each stream's Referer/Cookie. New `downloadRequestHeaders()` mirrors the player's interceptor and a per-download `HeaderAwareDownloaderFactory` decodes headers from the request `data` payload into a fresh upstream source, so HLS segments and range requests inherit them too. New files `data/download/DownloadHeaders.kt`, `data/download/DownloadCacheMaintenance.kt`. |
| 2026-10-04 | **Phase 14 — storage fallback, retries & mirror failover (§2p):** `DownloadManagerProvider` resolves the cache dir with a writability chain (`filesDir` → `getExternalFilesDir(DIRECTORY_MOVIES)` → `cacheDir`), sets `setMinRetryCount(4)`, and `VideoDownloadManager.maybeFailover()` re-enqueues a failed download's backup mirror (persisted `attempted` list, no loops). Download call sites in `DetailScreen`/`PlayerActivity` now pass `source.headers` + `source.alternates`. |
| 2026-10-04 | **Phase 14 — cleanup + storage meter (§2p):** `DownloadCacheMaintenance` detects unreferenced chunk files (`.exo`/`.tmp`/`.part`/`.vtt`); `clearTemporaryFiles()` drops non-completed downloads and deletes orphans; `storageStats()` separates completed bytes / temporary cache / orphan bytes. Settings → Storage gained a live breakdown and a **Clear download cache & temporary files** action. Added `DownloadMaintenanceTest` (8 cases → **81 tests**). `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **81 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45,912,458 / 38,215,346 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `2676D9DF…CB24`). **Device pass (§7 item 26) pending.** |
| 2026-10-04 | **Phase 15 — haptics (§2q):** `LocalHapticFeedback` (`TextHandleMove`) fires on playhead scrubbing (throttled to one tick per whole second), the ±10 s buttons and double-tap skip; the manga paged/webtoon readers click on every page turn (swipe, tap and volume-key alike, skipping the initial load). `ui/player/PlayerActivity.kt`, `ui/player/MangaReaderActivity.kt`. |
| 2026-10-04 | **Phase 15 — subtitle sync + Stats overlay (§2q):** Media3 1.3 exposes no subtitle-offset API, so the player buffers `onCues` and, when an offset is set, hides native captions and renders a **time-shifted overlay** at `position - offset`; a Subtitles slider offers **±10 s in 0.5 s steps** (persisted). A new **Stats** chip shows resolution, codec, track bitrate, bandwidth, FPS, buffer, state and dropped frames (from an `AnalyticsListener`). New `ui/player/PlaybackMath.kt`. |
| 2026-10-04 | **Phase 15 — warm filter + auto-delete (§2q):** a third manga colour-matrix option (`mangaFilter(invert, grayscale, warm)`) adds a warm/sepia tint (Settings → Manga Reader toggle). Settings → Downloads gained **Auto-delete watched downloads**, and `VideoDownloadManager.deleteDownloadsForItem()` removes an item's downloads + cached media once playback passes 90 %. Added `PlaybackMathTest` (6 cases → **87 tests**). `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **87 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45,958,786 / 38,231,730 bytes, signed);`dist/NovaStream.apk` refreshed (SHA-256 `DB214614…9BD6`). **Device pass (§7 item 27) pending.** |
| 2026-10-04 | **Phase 16 — best CloudStream + Stremio sources (§2r):** `AddonStore.Defaults.repos` expanded to **7** providers (CloudStream Phisher/**Hexated**/community; Aniyomi yuzono/official; Mihon Keiyoushi pb+json) and `ensureDefaultRepos()` now **merges** missing defaults for existing installs too. New pure `data/repo/StreamRanking.kt` ranks streams **reliability-first** (preferred hosts = Torrentio/TPB+/MediaFusion), then quality, then parsed seeders; `StreamRepository` gained `StreamsResult`, a 12 s per-add-on timeout, per-add-on error collection and a 5-minute result cache for regular Movie/Series/Anime lookups. `DetailScreen` shows a **“Some add-ons could not be reached”** list under an empty picker; the Add-on Manager's Stremio tab gained a **Recommended** one-tap install row. `CatalogRepository` now bounds regular add-on search (6 s) and catalog fetches (7 s). **NSFW / built-in adult paths unchanged.** Added `StreamRankingTest` (9 cases → **96 tests**). `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **96 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (38,248,114 bytes release, signed); `dist/NovaStream.apk` refreshed (SHA-256 `70ADDA87…3EBD`). **Device pass (§7 item 28) pending.** |
| 2026-10-05 | **Phase 17 — NSFW-anime speed (§2s):** every NSFW-anime network hop is now hard-bounded — search (AniList 8 s / Jikan 6 s / add-ons 6 s) and now **built-ins-authoritative** (Stremio NSFW add-ons queried only when both built-ins return nothing, same rule as Real 18+), home rows 10 s each with **progressive paint** through a new `nsfwRow()` helper, `stremioNsfwRows` finally gets the `CATALOG_TIMEOUT_MS` 7 s cap its SFW twin had, detail metadata sources 8 s each, `related()` 8 s, and `HentaiRepository` gets 5 s per source / 8 s per episode lookup / 12 s per stream resolution plus a 10 s `EPISODE_FILL_BUDGET_MS` across title variants. Caller cancellation is rethrown by a private `bounded()` helper. `data/repo/CatalogRepository.kt`, `data/repo/MetadataRepository.kt`, `data/hentai/HentaiRepository.kt`, `ui/vm/ViewModels.kt`. |
| 2026-10-05 | **Phase 17 — correct-title matching (§2s):** new `adultSignificantTokens()` / `adultTitlesMatch()` in `data/adult/AdultSource.kt` (filler words like "to" are never a match — "to" is a substring of "Netokano", which is how "Boku to Misaki-sensei" resolved to a different series). Gates added at every substitution point: `HentaiRepository.episodes` only accepts a source hit whose series/title shares a significant word with the query (streams resolve through the same lookup, so they belong to the selected title too); `MetadataRepository.detail` skips candidates whose name/altTitles aren't recognisably the selected item instead of letting them win on score, and `hentaiFallback` no longer renames the page (`name = head.series`) or swaps its poster; `JikanClient.detailsByTitle` requires an exact-or-token match and its "first adult entry ?: first candidate" fallbacks are deleted. Added `TitleMatchTest` (7 cases → **103 tests**). `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **103 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (46,003,430 / 38,248,114 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `91a44109…18BB`). **Device pass (§7 item 29) pending.** |
| 2026-10-05 | **NSFW Anime — clearing the search no longer strands the tab on "Nothing here yet":** the NSFW-anime browse rows were fetched **once** in `NsfwViewModel.init`, and clearing a search only reset the search state — never the browse rows — so if that initial load had failed (or its progressive paint was wiped when `nsfwAnimeRows` threw, e.g. `stremioNsfwRows` failing after AniList rows had already painted), the section stayed on its empty state with no way back. The fetch moved into a guarded `loadAnimeRows()` that only replaces the current rows on a real result (`getOrNull()` — a failed/cancelled load can no longer wipe rows), and `searchAnime` now calls `reloadAnimeRowsIfEmpty()` when the query is cleared, refetching so exiting a search always restores the catalogue rows. `ui/vm/ViewModels.kt`. `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **103 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (46,079,742 / 38,248,114 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `DC31DDB9…D7A2`). |
| 2026-10-05 | **Download progress UI + system notifications:** Media3's `DownloadManager.Listener` only reports state transitions, so the Library rows sat at "0% · 0 B" until a download finished. `VideoDownloadManager` now runs a 500 ms **progress ticker** while anything is `QUEUED`/`DOWNLOADING`, polling `DownloadManager.getCurrentDownloads()` and republishing each snapshot (speed/ETA derived from successive samples), stopping once nothing is left to download; a shared `applyDownload()` is used by both the listener and the ticker and skips no-op emissions, so the progress bar/percentage/bytes update smoothly. Notification side: `POST_NOTIFICATIONS` was declared but never requested, so on API 33+ the foreground download notification was silently dropped from the shade — `MainActivity` now requests it once per launch when not granted, and `NovaDownloadService.getForegroundNotification()` builds the progress notification via `DownloadNotificationHelper` with a real content intent (tap to open the app). `data/download/VideoDownloadManager.kt`, `data/download/NovaDownloadService.kt`, `ui/MainActivity.kt`. `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **103 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45,977,084 / 38,248,114 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `0421B698…4B5132`). **Device pass pending** for the notification permission prompt and live progress. |
| 2026-10-05 | **Persistent download-completion notification:** the base `DownloadService` ties its notification to the service, so a finished download vanished from the shade the moment the queue drained and the service stopped. `NovaDownloadService` now registers its own `DownloadManager.Listener` and, on `STATE_COMPLETED`, posts a separate `DownloadNotificationHelper.buildDownloadCompletedNotification` through the system `NotificationManager` under a per-download id (base 5000, clear of the foreground id 4801) — titled with the download's name and tap-to-open, it stays visible until the user dismisses it, independent of the service lifecycle. `data/download/NovaDownloadService.kt`. `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **103 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (45,978,174 / 38,248,114 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `8CBBBC87…4FC7E6`). **Device pass pending.** |
| 2026-10-05 | **Phase 18 — floating pill navigation + expressive surfaces (§2t):** the docked `NavigationBar` is replaced by a floating capsule (new `ui/nav/FloatingPillNavBar.kt`) — `RoundedCornerShape(50)`, held 16 dp above `WindowInsets.navigationBars` with 20 dp side margins, a `surface` backdrop at 85 % opacity, a 1 dp `outlineVariant` border at 25 % and a 6 dp shadow — with a spring-driven sliding indicator behind the active tab (`animateDpAsState`, low-bouncy). A shared `FloatingNavScrollState` exposes a `NestedScrollConnection` so the bar **slides off-screen on downward scroll and returns on any upward scroll** on Home, Browse, Search and Library; `NovaNav` mounts it in an outer `Box` over the `NavHost`, providing `LocalFloatingNavScroll` / `LocalFloatingNavBottomPadding` locals. Library/Browse top filters (Downloads/My Library/Stats, All/Movies/TV/Anime) became pill segmented controls via a new shared `ui/components/PillSegmentedControl.kt`, and the detail hero's primary action is now an elevated floating capsule with an **embedded mini progress bar** ("Resume Ep. 4" / "Read Ch. 12"). Every primary screen adds `LocalFloatingNavBottomPadding` to its list/grid bottom inset so content scrolls cleanly under the bar. New `ui/nav/FloatingPillNavBar.kt`, `ui/components/PillSegmentedControl.kt`; updated `ui/nav/NovaNav.kt`, `ui/screens/{home,search,manga,nsfw,library,settings,browse,detail}/*.kt`. `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **103 tests / 0 failures** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (46,175,796 / 38,264,498 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `311ABC0E…26952`). **Device pass pending** for the floating bar's slide/hide, the pill segmented controls and the capsule CTA. |
| 2026-10-05 | **Phase 19 — inert settings/engine wiring (§2t):** `DownloadManagerProvider` hardcoded `setMaxParallelDownloads(3)` and ignored `SettingsStore.maxParallel`, so the setting (and its backup/restore) did nothing; it now exposes `setMaxParallel(context, value)` (clamped 1–6) that applies to the live manager and seeds future holders, with `NovaApp` collecting `maxParallel` and pushing it on every change. `ExtensionRepository.reloadAll()` loaded *every* installed extension on startup and ignored `disabledSources`, so disabling a source lasted only until restart; it now takes the `SettingsStore` and filters disabled ids. `SettingsStore.storageUri` was written by the SAF picker and displayed but never read — completed downloads are now exported into the chosen folder by a new `data/download/DownloadExporter.kt` (framework `DocumentsContract`, no new dependency), driven from `VideoDownloadManager`'s completion callback. `data/download/DownloadManagerProvider.kt`, `data/ext/ExtensionRepository.kt`, `di/AppContainer.kt`, `NovaApp.kt`, `data/download/DownloadExporter.kt`, `data/download/VideoDownloadManager.kt`. |
| 2026-10-05 | **Phase 19 — player volume gesture + resume race (§2t):** `adjustVolume` compared `abs(volumeAccum)` to `VOLUME_STEP` (0.15) but committed `volumeAccum.toInt()`, which truncates toward zero — a step only landed once the accumulator reached 1.0, so every shorter drag changed nothing and `endGesture()` then discarded it; fixed to `val steps = (volumeAccum / VOLUME_STEP).toInt(); volumeAccum -= steps * VOLUME_STEP`. Separately, `onCreate` started the progress watcher before `resumeLastPosition()`, so a quick pause/back could persist position 0 over the saved spot; a `@Volatile resumeResolved` gate now blocks `recordProgress()` until the resume read completes, and the watcher starts only afterwards. `ui/player/PlayerActivity.kt`. |
| 2026-10-05 | **Phase 19 — UI/UX audit fixes (§2t):** NSFW search results moved off `chunked(3)`/`Row3` onto the shared `LazyVerticalGrid(GridCells.Adaptive(AppSpacing.posterMin))` (full-span rails via `GridItemSpan(maxLineSpan)`). Hardcoded top padding (40–48 dp) replaced with `statusBarsPadding()` on NSFW/Settings/Add-on Manager/Sources/HomeOrder (Search/Library/Browse already done). The player's Quality & Audio / Episodes / Speed / External hand-rolled `Box{…}.clickable{onDismiss}` overlays became `ModalBottomSheet`s, so inner taps no longer dismiss and system Back closes the sheet instead of the Activity. `DetailScreen`'s stream-sheet subtitle-language `TagChip`s (previously `TagChip(t.lang) {}`) are now selectable and hand the chosen track first to the player (`PlayerActivity` marks only the first subtitle config `SELECTION_FLAG_DEFAULT`). `SettingsStore.setMangaProgress` now removes an existing chapter key before re-inserting so frequently-read chapters aren't evicted by `takeLast(200)`. `ui/screens/nsfw/NsfwScreen.kt`, `ui/player/PlayerActivity.kt`, `ui/screens/detail/DetailScreen.kt`, `ui/screens/{settings,addons,sources}/*.kt`, `data/local/SettingsStore.kt`. |
| 2026-10-05 | **Phase 19 verified:** `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **103 tests / 0 failures / 0 errors** ✅; `:app:assembleDebug` + `:app:assembleRelease` → BUILD SUCCESSFUL (46,024,836 / 38,264,498 bytes, signed); `dist/NovaStream.apk` refreshed (SHA-256 `fbf2e046…95ec8`). **Device pass pending** for the bottom sheets, subtitle chips, volume drag and custom-folder export. |
| 2026-10-06 | **Phase 20 — grids + recents + hero + accent picker (§2u):** `AppSpacing.posterMin` 108 → **88 dp**, so `GridCells.Adaptive` resolves **3 columns** on every phone from 320–411 dp (108 dp gave only 2 on the common 360 dp width, wasting the sides of the screen and forcing extra scrolling); `LibraryScreen`'s Downloads and My Library grids gained the 16 dp start/end gutters every other grid already had. `SearchViewModel.search()` now records any query the user actually let run (the 350 ms debounce already collapses a typed word into one request) instead of only chip taps — which is why the Recent Searches section had an empty list and never rendered. `HeroSlide` dropped its description paragraph and the genre from its meta line (now Type · Year · Rating) and its CTA reads **Play**/**Read** according to `MediaItem.isReadable`. The Settings accent row is wrapped in `horizontalScroll` so the clipped swatches are reachable. `ui/theme/Tokens.kt`, `ui/vm/ViewModels.kt`, `ui/screens/home/HomeScreen.kt`, `ui/screens/library/LibraryScreen.kt`, `ui/screens/settings/SettingsScreen.kt`, `data/model/Models.kt`. |
| 2026-10-06 | **Phase 20 — floating pill nav always visible (§2u):** deleted `FloatingNavScrollState`, `LocalFloatingNavScroll` and `Modifier.floatingNavScroll()` (the `NestedScrollConnection` that slid the bar off-screen on downward scroll — direction detection felt inverted and auto-hiding the primary nav was intrusive) and removed its `.floatingNavScroll()` call sites from Home, Browse/Manga, Search, Library (×3), NSFW and Settings; `NovaNav` keys the bar's `AnimatedVisibility` on `showBar` alone. `LocalFloatingNavBottomPadding` is kept so content still clears the overlaying bar. `ui/nav/FloatingPillNavBar.kt`, `ui/nav/NovaNav.kt` + 7 screens. |
| 2026-10-06 | **Phase 20 — Sources dashboard / Home row order crash + cold-boot resilience (§2u):** both screens were the **only two in the app** returning from the root `Column { … }` content lambda with an early `return@Column` after their empty state (both hit it on entry), which skips the lambda's group-closing bookkeeping and corrupts the composer for the next frame/navigation — replaced with the standard `if/else`; `HomeOrderScreen`'s row seeding and persistence are `runCatching`-guarded and both Settings rows navigate with `launchSingleTop`. Cold boot: `ExtensionRepository.reloadAll()` retries the descriptor read and each extension load once (300 ms) and never throws; `NovaApp`'s collectors now run through a new `collectSafely(...)` (an exception in `appScope` has no handler and used to kill the app), `ensureDefaults()` retries once after 3 s; `Http.httpGet()` retries transient failures (transport / 5xx / 408 / 429) up to 3× with 250/500 ms backoff, rethrowing cancellation and treating other 4xx as final. `ui/screens/sources/SourcesScreen.kt`, `ui/screens/settings/HomeOrderScreen.kt`, `ui/screens/settings/SettingsScreen.kt`, `data/ext/ExtensionRepository.kt`, `data/remote/Http.kt`, `NovaApp.kt`. |
| 2026-10-06 | **Phase 20 — manga reader paging + Continue Reading (§2u):** `ZoomablePage` replaced `detectTransformGestures` (which consumed one-finger drags after the touch-slop threshold, so the parent `HorizontalPager` cancelled — RTL/LTR never advanced while Webtoon worked) with a hand-rolled `awaitEachGesture` that only claims real pinches or pans of an already-zoomed image; a single finger at 1× is left to the pager. The reader's `savePage` now also writes `WatchEntry(item, videoId = chapter, videoTitle, positionMs = pagesRead, durationMs = totalPages)` through `LibraryStore.recordWatch`, and `HomeViewModel`/`LibraryScreen` split the shared history on the new `MediaItem.isReadable` into **Continue Watching** and **Continue Reading** rows/sections for normal + NSFW manga. `ui/player/MangaReaderActivity.kt`, `ui/vm/ViewModels.kt`, `ui/screens/home/HomeScreen.kt`, `ui/screens/library/LibraryScreen.kt`. |
| 2026-10-06 | **Phase 20 verified:** `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **103 tests / 0 failures / 0 errors** ✅; `:app:assembleRelease` → BUILD SUCCESSFUL (signed); `dist/NovaStream.apk` refreshed (**38,264,498 bytes**, SHA-256 `aae43aa3…eabd4`; new symbols `Continue Reading`, `collectSafely`, `isReadable` verified in the release dex). **Device pass pending — §7 item 30.** |
| 2026-10-06 | **Phase 21 — NSFW/Browse card width aligned with Home (§2v):** `HomeScreen` passed `MediaRow(cardWidth = 150)` by hand while `NsfwScreen` and `MangaScreen` (both Browse segments) called `MediaRow` without a width and inherited its **132 dp** default — the same poster was therefore a different size on Home, Browse → Manga and Browse → NSFW, and the NSFW rails read as "narrow cards with wide side margins". New `AppSpacing.railCard` = **150 dp** is now the single source of rail card width and all three screens pass it explicitly; NSFW's remaining hardcoded 16 dp gutters moved to `AppSpacing.screen`. The results grid was verified to already fill the viewport (Compose 1.7.0 `GridCells.Adaptive` → `calculateCellsCrossAxisSizeImpl` distributes `availableSize − padding` evenly; no max-width clamp exists on either screen). `ui/theme/Tokens.kt`, `ui/screens/home/HomeScreen.kt`, `ui/screens/manga/MangaScreen.kt`, `ui/screens/nsfw/NsfwScreen.kt`. |
| 2026-10-06 | **Phase 21 — Sources dashboard crash root-caused and fixed (§2v):** Phase 20's early-`return@Column` theory was wrong (and is not a crash mechanism — `Column` hands a non-weighted child a bounded `mainAxisMax`, so `Column { header; Lazy*(fillMaxSize()) }` is safe). The real cause is a **Gson null-field decode**: `ExtensionStore.list()` used `Gson().fromJson(…, Array<ExtensionDescriptor>::class.java)`, and Gson instantiates Kotlin data classes through `Unsafe`, so a JSON entry missing a field yields a silent `null` in a **non-null** slot. `SourcesScreen` then hit the compiler's `checkNotNullParameter` NPE on `Text(desc.name)` and fed `LazyColumn` a null key. New `data/ext/ExtensionCodec.kt` decodes element-wise — drops entries missing `id`/`entryClass`/`apkPath`, defaults `name` to the id, keeps `version` optional, de-dupes ids, tolerates a bare object and any unparseable file, and never throws; `ExtensionStore.list()`/`save()` use it, `ExtensionRepository.installed()` is wrapped, and `SourcesScreen` reads through a `runCatching` fallback plus a blank-id filter/de-dupe. `data/ext/ExtensionCodec.kt` (new), `data/ext/ExtensionRepository.kt`, `ui/screens/sources/SourcesScreen.kt`, `app/src/test/java/com/moovio/app/ExtensionCodecTest.kt` (new, 8 cases). |
| 2026-10-06 | **Phase 21 verified:** `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **111 tests / 0 failures / 0 errors** ✅ (15 result files; +8 `ExtensionCodecTest`); `:app:assembleRelease` → BUILD SUCCESSFUL (signed); `dist/NovaStream.apk` refreshed (**38,264,498 bytes**, SHA-256 `a2007861…8c10`; `ExtensionCodec` / `railCard` / `SourcesScreen` verified in the release dex). **Device pass pending — §7 item 31** (no device or emulator was available: no AVDs installed and no `adb` target attached). |
| 2026-10-06 | **Source hygiene — dead seeds fixed & dead hosts retired (§2w, from the §3 live probe):** `AddonStore.Defaults.repos` now seeds the live `JoeTinnySpace/cloudstream-extensions-hexated` mirror (the old `hexated/…` URL 404s on every branch) and a new `Defaults.retiredRepoUrls` + `AddonRepository.ensureDefaultRepos()` prunes the dead URL from existing installs. `HahoSource` was re-pointed from its two 404 paths to the live `GET /anime?q=`, parsing the `anime-loop` series cards and expanding each into `/anime/{slug}/{n}` episodes (in parallel, capped at 3 series); `HentaiTubes.mediaUrls` now also reads `<source … type="video/…">` tags so Haho's extension-less filegasm streams resolve (subtitle tracks excluded). Dead hosts removed: `HpjavSource` (hpjav.tv DNS-dead) and `AvgleSource` (api.avgle.com 520) from `AdultSources.all`, `HentaigasmSource` (timeouts) and `HanimeSource` (search API DNS-dead + JS-shell page) from `HentaiSources.all`; objects/tests retained. `data/local/AddonStore.kt`, `data/repo/AddonRepository.kt`, `data/hentai/HahoSource.kt`, `data/hentai/HentaiTubes.kt`, `data/adult/AdultSources.kt`, `data/hentai/HentaiSources.kt`, `AdultSourcesExtraTest.kt`, `HentaiSourcesExtraTest.kt`. |
| 2026-10-06 | **Source hygiene verified:** `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **113 tests / 0 failures / 0 errors** ✅ (15 result files; +2 net — the Haho JSON test became two HTML tests and a new `mediaUrls` source-tag case); `:app:assembleRelease` → BUILD SUCCESSFUL (signed); `dist/NovaStream.apk` refreshed (**38,280,882 bytes**, SHA-256 `52cef604…2ec9d`; `JoeTinnySpace` / `retiredRepoUrls` verified in the release dex). Live-probed during the pass: hexated mirror 200 (old 404), Haho `/anime?q=` 200 with 2 series, its series page's 6 episodes, and the episode→`/embed`→`<source type="video/…">` filegasm URLs; hpjav.tv + search.htv-services.com DNS-dead, api.avgle.com down, hentaigasm.com timeout. **Device pass still required — §7 items 21/23/31.** |
| 2026-10-07 | **Repo prepared for public GitHub upload:** README rewritten for NexusStream (current feature set, build/signing docs via untracked `keystore.properties`, no secrets printed); `.gitignore` gained `.kotlin/`; verified nothing sensitive is tracked (keystore/`, `keystore.properties`, `local.properties` have never been committed). Pending work committed as "Implement Phases 7-21" + "Prepare README and gitignore for publishing". |
| 2026-10-07 | **Source-neutrality pass (§2x):** `AddonStore.Defaults.repos` and `Defaults.streamAddons` are now **empty** — all 7 seeded provider repos (CloudStream Phisher/Hexated/community, Aniyomi yuzono/official, Keiyoushi pb+json) and the seeded Torrentio/ThePirateBay+ stream add-ons are removed; the Add-on Manager's one-tap row now offers only **Cinemeta** and **OpenSubtitles v3**; `preferredStreamHosts` is empty so streams rank quality → seeders. `retiredHosts`/`retiredRepoUrls` extended so existing installs are pruned of every removed seed on next launch. `ProviderTab` gained the Add-on Manager's first **add-repository-URL** UI (plus per-repo remove and an empty state) — the provider tabs were previously unusable without seeds. Detail empty-stream hint and related comments/tests neutralized; README gained a "Content neutrality" section. `data/local/AddonStore.kt`, `data/repo/AddonRepository.kt`, `data/repo/StreamRanking.kt`, `data/model/Models.kt`, `data/remote/CloudStreamRepo.kt`, `ui/screens/addons/AddonManagerScreen.kt`, `ui/screens/detail/DetailScreen.kt`, `StreamRankingTest.kt`, `README.md`, `.gitignore`. |
| 2026-10-07 | **Source-neutrality verified:** `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest` → **113 tests / 0 failures** ✅ (StreamRankingTest fixtures renamed to neutral hosts — first run caught a space-vs-hyphen mismatch in the renamed data); `:app:assembleRelease` → BUILD SUCCESSFUL (signed); `dist/NovaStream.apk` refreshed (**38,280,882 bytes**, SHA-256 `969D176D…B0A9D`). **Device pass:** fresh install should seed only Cinemeta + OpenSubtitles; upgraded installs should drop the removed add-ons/repos. |
| 2026-10-07 | **Published to GitHub (§2y):** repo public at `github.com/speckle-hub/nexusstream` — local `master` renamed to `main`, 21 commits pushed, repo-local identity `nexusstream-dev`. Annotated tag `v1.1.0` pushed; GitHub Release created with the signed APK attached (`app-release.apk`, 38,280,882 bytes) — the in-app updater is now functional. |
| 2026-10-07 | **Website built (§2y):** new `website/` — Next.js 15 + TypeScript + Tailwind v3.4 + framer-motion + HeroUI `~2.7` + ThreeUI nebula, with hand-ported Aceternity-style primitives. Sections: glass navbar, layered hero with a pure-CSS phone mockup of the app, ecosystem marquee, glowing bento feature grid, animated stats, HeroUI FAQ, beams download CTA wired to the release APK, content-neutrality footer. `next build` ✅ (static). |
| 2026-10-07 | **Website fix:** hydration mismatch in the moving-border button (`<div>` inside `<svg>` + SSR/CSR transform drift) — rewritten SVG-native (`<m.circle>`, mount-gated, unique gradient IDs). |
| 2026-10-07 | **Website optimization pass:** first-load JS 240 → **216 kB**, page chunk 134 → **92 kB** (LazyMotion `domAnimation`, `motion.*` → `m.*`, `optimizePackageImports`); Sparkles pauses offscreen, Nebula/all keyframes honor `prefers-reduced-motion`; SEO pack — `metadataBase` (`NEXT_PUBLIC_SITE_URL`), Twitter card, SVG favicon, generated `opengraph-image`, `sitemap.xml`, `robots.txt`, branded 404; `poweredByHeader` off. Deployed to **Vercel** (root dir `website`, auto-redeploy on push). |
| 2026-10-08 | **v1.4.0 released:** NSFW isolation + Browse → Anime (§2z), launch-time GitHub auto-update dialog + default-repo fallback (§2aa), and the QoL/visual pass — empty-state halos + Retry, poster shimmer/error, reduced motion, app-wide tabular figures, glass elevation + M3 `surfaceContainer*` (§2ab/§2ac). `versionCode` 6 / `versionName` 1.4.0. Verified `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **119/119**, `:app:assembleRelease` ✅ (signed); `dist/NovaStream.apk` refreshed (38,330,038 bytes, SHA-256 `F45FA749…20C4`); website CTA/hero → v1.4.0; source pushed (`16a55a4`) and the **`v1.4.0` GitHub Release published with `app-release.apk`** (asset digest `sha256:f45fa749…20c4`; `releases/latest` → v1.4.0). |
| 2026-10-08 | **v1.4.1 released (§2ad):** device-report fixes — shared `NovaSwitch` (off state was near-invisible on this theme), `AppSpacing.railCard` 150 → **100 dp** so Browse → Anime/NSFW rails match the Search 3-across density, and `searchNsfwAnime` now **merges AniList + Jikan + NSFW add-ons** (no early return) with caps raised (AniList 40→50, Jikan paged to 50, MangaDex →100). `versionCode` 7 / `versionName` 1.4.1. Verified `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **119/119**, `:app:assembleRelease` ✅ (signed); `dist/NovaStream.apk` refreshed (38,330,038 bytes, SHA-256 `81078DB4…B290`, versionName verified 1.4.1); website → v1.4.1. |
| 2026-10-11 | **Phase 26 — P0 Bug Fixes & Crash Prevention (§2ae):** top 5 audit issues fixed — (1) `MangaDownloadManager.enqueue` directly inserts new downloads into `_downloads` StateFlow so downloads start immediately without restart; `file.writeBytes` wrapped in `runCatching` preventing `ENOSPC`/disk-full crashes. (2) `DetailViewModel.buildSeasons` and `loadSeasonEpisodes` calls guarded in `viewModelScope.launch` with fallback to basic metadata/seasons on missing/blank TMDB keys and network IOExceptions; hardened `TmdbClient` with `optJsonArray`/`optJsonObject` against malformed JSON responses. (3) Media3 `DownloadRequest.Builder` sets `.setCustomCacheKey(id)` in `VideoDownloadManager` so cache keys match download IDs, allowing `DownloadExporter` to write files to the custom folder. (4) Stremio catalog fetching and rails deduplicated with `distinctBy { it.key }` across `StremioClient`, `CatalogRepository`, and `MediaRow` to prevent `IllegalArgumentException: Key was already used` Compose crashes. Added `Phase26P0FixesTest` (5 cases → **124 tests**). Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **124/124 tests / 0 failures** ✅. |
| 2026-10-11 | **Phase 27 (Part A) — UI & Layout Polish Fixes (§2af):** 4 audit items — (1) `navigationBarsPadding()` on pushed destinations (Detail, Sources, Home Order, Collection, Add-on Manager, NSFW Hub; tab screens already covered via `LocalFloatingNavBottomPadding`). (2) `MetaStatsRow` constrained (`fillMaxWidth` + `maxLines = 1` + `Ellipsis` + shared weight). (3) "Continue Watching" header guarded (`watched.isNotEmpty()` or fully empty). (4) Poster "×" hit area tucked to corner (48 dp @ 6 dp → 40 dp @ 2 dp top-start, 22 dp chip). Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **124/124 / 0 failures** ✅. Not pushed, no APK. |
| 2026-10-11 | **Phase 27 (Part B) — Updater UI Revamp (§2ag):** `downloadApkWithProgress()` streams APK in 8 KB `ensureActive()`-checked chunks with throttled `DownloadProgress` callbacks (fraction/percent null-safe, partial deleted on fail/cancel, `CancellationException` rethrown); `UpdateAvailableDialog` shows determinate `LinearProgressIndicator` + tabular `Downloading update… 45% (18.2 MB / 40.5 MB)` line, Update disabled, "Later"→"Cancel" mid-download (aborts job, parks dialog in idle), outside-tap/Back ignored while downloading. `downloadApk()` kept as delegate for Settings. Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **129/129 / 0 failures** ✅ (+5 `UpdateCheckerTest`). Not pushed, no APK. |
| 2026-10-11 | **v1.4.2 released:** Phase 26 + 27A + 27B shipped as commit `2ce2156` (`versionCode` 8 / `versionName` 1.4.2; compile ✅, 129/129 tests ✅, signed `:app:assembleRelease` ✅ → `dist/NovaStream.apk`, 38,330,042 bytes, SHA-256 `5AB52C87…4C6E`); annotated tag `v1.4.2` pushed; **GitHub Release `NexusStream v1.4.2` published with `app-release.apk` attached** (38,330,042 bytes) so `releases/latest/download/app-release.apk` now serves v1.4.2 to the in-app updater and the site CTA; website hero badge + download CTA bumped to v1.4.2 (`next build` ✅, 216 kB first-load) and pushed as `8ced0ca` (Vercel auto-redeploys on push). |
| 2026-10-12 | **v1.5.1 released:** P0 Play-crash hotfix (§2ak) shipped — `versionCode` 10 / `versionName` 1.5.1; `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **171/171**, `:app:assembleRelease` ✅ (signed) → `dist/NovaStream.apk` (40,140,687 bytes, SHA-256 `A58E3D2B…7E1151`; `PlayerStartup` verified in the release dex); tagged `v1.5.1` and pushed; **GitHub Release `NexusStream v1.5.1` published with `app-release.apk` attached** so `releases/latest/download/app-release.apk` now serves v1.5.1. Website CTA/hero bumped to v1.5.1 (`next build` ✅, pushed; Vercel auto-redeploys). |
| 2026-10-13 | **v1.6.1 released:** bundles Phase 32 (§2am) on the v1.6.0 CloudStream runtime — `versionCode` 12 / `versionName` 1.6.1; `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest --rerun-tasks` ✅ **189/189**, `:app:assembleRelease` ✅ (signed) → `dist/NexusStream.apk` + `dist/NovaStream.apk` + `dist/app-release.apk` (43,675,807 bytes, SHA-256 `A64EC17F…51C3A`; `output-metadata.json` verified); tagged `v1.6.1`; website CTA/hero → v1.6.1 (`next build` ✅, 216 kB first-load). **GitHub Release `NexusStream v1.6.1` published with `NexusStream.apk` + `app-release.apk` attached** (43,675,807 bytes each, digest `sha256:a64ec17f…51c3a`; verified `releases/latest` → v1.6.1), so the in-app updater + site CTA serve v1.6.1. |
| 2026-10-13 | **Phase 32 — Stream Scraper Performance & Real 18+ Speed Optimization (§2am):** new `ScraperRace.kt` policy (5 s provider/subtitle/detail ceilings, `isPlayableCandidate`, cancellation-safe `raceResult`, `MirrorHealth` quarantine, `PageCache`); `StreamRepository` 12 s → 5 s with completion-order progressive `onPartial` + concurrent MOVIE race; `DetailViewModel.loadStreams` paints the first partial; `AdultRepository` home/search fixed from registry-order awaits to completion-order `launch` + 5 s caps + quarantine; `AdultHttp.getList` page-cache + fast fetch, bounded `verifyPlayable`; `Http` pool/dispatcher tuning + `listClient`/`httpGetList`; `CatalogRepository` bounded `safeRow` + parallel rows/search phases (caps → 5 s); `HentaiRepository` stream cap → 5 s. Added `ScraperRaceTest` (13 cases → **189 tests**). Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **189/189 / 0 failures** ✅. Not pushed, no APK. |
| 2026-10-14 | **Player controls auto-hide fix (§2ap):** `CONTROLS_HIDE_MS` 3 s auto-hide while playing, tap-to-toggle, `pokeControls()` reset on gesture/slider/speed/boost, keep-visible when paused/buffering/sheet-open, 300 ms alpha fade (`fadeIn(tween(300))/fadeOut(tween(300))`). Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **199/199 / 0 failures** ✅. Not pushed, no APK. |
| 2026-10-14 | **Source expansion (§2aq):** 7 new built-in sources — NSFW Anime `Rule34VideoSource` (`/search/` → `/video/` cards) + `AnimeIdHentaiSource` (`/?s=` → `/search/` → www-mirror); general `EpornerSource` (public JSON API v2 + `/cats/`/static categories + `TubeMedia` streams) + `BeegSource` (v6 API index/search/file-map streams + HTML fallback); JAV `JavGuruSource` (jav.guru WP), `SupJavSource` (maker/tag-aware), `VjavSource` (confirmed `/videos/` + `/search/1/?s=` layout). Registries 10 → 15 adult / 7 → 9 hentai; isolation/progressive loading inherited. Added `ExpandedSourcesTest` (10 cases → **209 tests**). Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **209/209 / 0 failures** ✅. Not pushed, no APK. |
| 2026-10-14 | **v1.7.0 released:** bundles §2aq (7 new built-in sources) + §2ap (player controls auto-hide) — `versionCode` 15 / `versionName` 1.7.0; `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest --rerun-tasks` ✅ **209/209**, `:app:assembleRelease` ✅ (signed) → `dist/NexusStream.apk` + `dist/app-release.apk` (43,708,575 bytes, SHA-256 `16A7DB34…05F063`; `output-metadata.json` verified); tagged `v1.7.0`; website CTA/hero → v1.7.0, stats → 209 (`next build` ✅, 174 kB first-load). **GitHub Release `NexusStream v1.7.0` published with `NexusStream.apk` + `app-release.apk` attached**, so the in-app updater + site CTA serve v1.7.0. |
| 2026-10-14 | **Phase 35 — Video Player UI Overhaul & Touch Controller Re-architecture (§2ar):** unified tap/double-tap/hold into one `detectTapGestures` interceptor (two detectors raced for the same down event, breaking tap toggles); new pure `ui/player/PlayerControls.kt` state machine (`PlayerControlsState` VISIBLE/HIDING/HIDDEN, `PLAYER_CONTROLS_HIDE_MS` 3000L, `shouldAutoHideControls`/`playerTapAction`/`forcedControlsState`); single `autoHideJob` via `resetAutoHideTimer()` (cancel + fresh countdown, `pokeControls()` kept as alias); fades → 200/250 ms; top-bar track + speed actions; `AnimatedContent` play/pause morph; bottom quality badge (`StreamSource.quality` → `qualityLabel`). Added `Phase35PlayerControlsTest` (7 cases → **216 tests**). Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **216/216 / 0 failures** ✅. Not pushed, no APK. |
| 2026-10-14 | **v1.7.1 released:** bundles Phase 35 (§2ar) — `versionCode` 16 / `versionName` 1.7.1; `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest --rerun-tasks` ✅ **216/216**, `:app:assembleRelease` ✅ (signed) → `dist/NexusStream.apk` + `dist/app-release.apk` (43,708,575 bytes, SHA-256 `CEE1669A…74027D`; `output-metadata.json` verified); tagged `v1.7.1`; website hero/footer → v1.7.1, stats → 216 (`next build` ✅, 153 kB first-load). **GitHub Release `NexusStream v1.7.1` published with `NexusStream.apk` + `app-release.apk` attached**, so the in-app updater + site CTA serve v1.7.1. |
| 2026-10-14 | **Source expansion (§2as):** 13 new built-in sources — NSFW Anime `HenVidsSource` (`/hentai/` cards) + `OhentaiSource` (`detail.php?vid=` + `tagsearch.php` search) + `HentaiTvSource` (`/api/search?q=` JSON, episode-level, nhplayer embeds) + `XAnimuSource` (confirmed `/?s=` + numeric-permalink filter); general `PornTrexSource` (KVS) + `SxyPrnSource` (aggregator, best-effort file-host streams) + `FullPornerSource` (`/watch/` hash) + `TxxxSource` (Aylo `media_definitions` mirror of YouPorn); JAV `JavMostSource` (WP) + `KissJavSource` (KVS `kissjav.li`) + `JavtifulSource` (`/video/{id}` codes) + `OpJavSource` (WP) + `SevenMmtvSource` (sx + tv-mirror fallback). Registries 15 → 24 adult / 9 → 13 hentai; isolation/progressive loading inherited. Added `MoreSourcesTest` (14 cases → **230 tests**). Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **230/230 / 0 failures** ✅. Not pushed, no APK. |
| 2026-10-14 | **Phase 36 — Premium UI/UX Overhaul (§2at):** audit-first — hero carousel, poster palette, pill nav, NSFW vault, subtitle sheet + sync, PiP and stream ranking/badges verified existing. New: hero **Watchlist** pill (`HomeViewModel.favorites` + `toggleFavorite`, haptic), pill-nav **tab haptics**, Real 18+ **source-filter chips** (new pure `NsfwFilters.applySourceFilter`, `NsfwSection` options, source-aware empty state), subtitle **outline color** (`SubtitleStyle.outlineKey` + `SUB_OUTLINE` key, Settings + in-player sheet) and **100 ms sync steps** (`steps = 199`), next-episode **pre-fetch** inside 2 min (`shouldPrefetchNextEpisode`, `prefetchedIndex`, cache-warming resolve). Audio delay deliberately not built (no Media3 API — would be fake UI). Added `Phase36PremiumTest` (7 cases → **237 tests**). Verified: `:app:compileDebugKotlin` ✅; `:app:testDebugUnitTest --rerun-tasks` → **237/237 / 0 failures** ✅. Not pushed, no APK. |
| 2026-10-14 | **v1.8.0 released:** bundles Phase 36 (§2at) + §2as source expansion (13 new built-in sources) — `versionCode` 17 / `versionName` 1.8.0; `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest --rerun-tasks` ✅ **237/237**, `:app:assembleRelease` ✅ (signed) → `dist/NexusStream.apk` + `dist/app-release.apk` (43,741,343 bytes, SHA-256 `F98D6590…008C00`; `output-metadata.json` verified); tagged `v1.8.0`; website hero/footer → v1.8.0, stats → 237 (`next build` ✅, 103 kB shared). **GitHub Release `NexusStream v1.8.0` published with `NexusStream.apk` + `app-release.apk` attached**, so the in-app updater + site CTA serve v1.8.0. |
| 2026-10-15 | **v1.9.0 released:** bundles the Moovio display-name rebrand + the Anime4K GPU shader engine (§2au) and the 14 built-in SFW sources with auto-play best source (§2av) — `versionCode` 18 / `versionName` 1.9.0; `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **281/281**, `:app:assembleRelease` ✅ (signed) → `dist/Moovio.apk` + `dist/app-release.apk` (43,946,889 bytes, SHA-256 `4E4E6E60…8D8D64`); tagged `v1.9.0`; website hero/footer → Moovio v1.9.0, stats → 281 (`next build` ✅). **GitHub Release `Moovio v1.9.0` published with `Moovio.apk` + `app-release.apk` attached**, so the in-app updater + site CTA serve v1.9.0. |
| 2026-10-22 | **Full rename to Moovio (§2aw):** `com.novastream.app` → `com.moovio.app` (namespace + `applicationId`, 1118 references across 193 files) and every brand token renamed (`MoovioApp`, `MoovioTheme`, `LocalMoovioColors`, `MoovioExtension`, `MoovioDownloadService`, `moovioViewModel`, `Ns*` → `Moovio*` colour tokens, `ns_*` XML colours); OAuth scheme `novastream://oauth` → `moovio://oauth`; `Theme.NovaStream` → `Theme.Moovio`; `rootProject.name` = Moovio; README rewritten (stale version/test-count/TMDB-key claims corrected); roadmap retitled; website package renamed `moovio-website`; **the OpenGraph wordmark still rendered "NexusStream" — now "Moovio"**; stale `dist/NexusStream.apk` + `dist/NovaStream.apk` deleted. Verified structurally (package/dir parity, import resolution, manifest/theme/resource references) — **and then compiled**: `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **281/281**, `:app:assembleDebug` ✅ (53,038,688 bytes) after `local.properties` was pointed at the real SDK (`sdk.dir` had pointed at a non-existent Linux path). |
| 2026-10-22 | **Security / stability remediation (§2ax):** (1) **release key rotated** — a fresh 4096-bit RSA key (`alias moovio`) at `~/.moovio/moovio-release.jks` outside the repo; `keystore/novastream.jks` + `keystore.properties` deleted; `build.gradle.kts` reads the material only from `local.properties` / `MOOVIO_*` env vars and a release build without it now fails fast with an actionable message; (2) **OAuth state validation** — 128-bit random nonce in `state`, constant-time compare, one-shot consumption (a foreign app could previously push its own authorization code into the user's tracker list); (3) **updater signature check** — `installApk` verifies package + trusted signer before the system installer sees the file; (4) **CloudStream shims repaired** — app context published to `com.lagradost.api`, `CommonActivity` attached (provider toasts were no-ops), `CloudStreamApp.initialize` called at app start, provider settings now use a Jackson mapper configured like the library's (Gson dropped null fields → providers' inlined Jackson read failed and silently lost settings); (5) **download removal no longer starts a foreground service** (Android 12+ `ForegroundServiceStartNotAllowedException` was swallowed, so watched downloads were never reclaimed); (6) **saved manga reading position restored** (the pager captured its initial page before the position loaded → every chapter opened on page 1); (7) `FLAG_SECURE` applied to the player; (8) home-row order screen keys made unique (duplicate titles crashed the LazyColumn); (9) poster-palette extraction and extension JSON parsing moved off the main thread, detail-screen history collected once per screen instead of once per episode row. |
| 2026-10-22 | **Git history purged of the old key:** `git filter-repo --path keystore --invert-paths`; `main` force-pushed with `--force-with-lease` pinned to the old head `a1b3f4a`; **all 16 release tags rewritten and force-pushed** (two of them still pointed into the pre-rewrite history and would have kept the key blob reachable); reflog expired, object store re-packed — blob `681a2046…` is no longer a valid object locally and no reachable ref on GitHub points at the old commits. GitHub still serves the orphaned blob by SHA until its own GC runs, which is why the key was rotated. Verified: `git diff origin/main HEAD` = exactly the 213-file change set (no content lost by the rewrite). |
| 2026-10-30 | **UI/UX design-system pass, Android only (§2bb):** `MoovioColors` now `@Immutable` with `hairline` + contrast-derived `actionFill`/`onAction` (all 8 accents ≥ 4.5:1); `GlassSurface` → flat `SurfaceCard` at ~25 call sites; removed the poster-tinted radial washes (Home 24 % / Detail 32 %), the accent CTA glow, the 140 dp empty-state halo, the five-hue resolution badge, the duplicated rating pill and the pill-everywhere radii (new `AppRadius`); type scale Bold(700) → Medium(500) with tracking only at 32 sp+; `FEATURED` → `Featured`; `@Immutable` on `MediaItem`/`CatalogRow`/`WatchEntry`; palette extraction dropped from both heroes. `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **309/309**. No version bump, no APK, not pushed. `desktop/` + `desktop-win/` untouched. |
| 2026-10-22 | **v1.10.0 released (§2ay):** the first Moovio-named release — `versionCode` 19 / `versionName` 1.10.0; `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **292/292** (281 + 11 new: `OAuthAndSignatureSecurityTest` 6, `ProviderSettingsCodecTest` 5), `:app:assembleDebug` ✅, `:app:assembleRelease` ✅ (signed with the **rotated** key — signer SHA-256 `2EDB949B…351379`, DN `CN=Moovio, OU=Release, O=Moovio, C=US`) → `dist/Moovio.apk` + `dist/app-release.apk` (43,946,885 bytes, SHA-256 `ED8C5418…39D54F`); archive verified in-place (`package com.moovio.app`, `versionCode 19`, `versionName 1.10.0`, minSdk 24 / targetSdk 34, label `Moovio`). Pushed (`main` + tag `v1.10.0`), **GitHub Release `Moovio v1.10.0` published with `app-release.apk` + `Moovio.apk` attached and marked Latest** — `releases/latest/download/app-release.apk` now serves v1.10.0 (re-downloaded and hash-matched end to end). Website hero/footer/nav → v1.10.0, stats → 292, install size → ≈44 MB (`next build` ✅, 8/8 static pages). **Upgrade note in the release body: v1.10.0 cannot be installed over v1.9.0** (package id *and* signing key changed) — old installs must be removed, and their settings/library/downloads do not carry over. |
| 2026-10-30 | **Phase 2 — motion, gestures & haptics (§2bc):** wired the poster → detail **shared-element morph** (new `Modifier.sharedPoster`: `SharedTransitionLayout`/`ProvideSharedScope` were installed on every destination but nothing consumed them, so navigation still cross-faded) with `Motion.settle<Rect>()` as its bounds transform, sourced from `PosterCard`'s artwork and landed on `DetailHeader`'s backdrop; new `MoovioHaptics` (`select`/`toggle`/`commit`, `@Stable`, remembered) replacing raw `performHapticFeedback` calls in 4 files and driving the player's brightness/volume drags per 10 % band, the primary CTA, the heart, season chips and Library long-press (from inside `PosterCard`); new `Motion.settle<T>()` (`DampingRatioNoBouncy` + `StiffnessLow`) for the carousel auto-advance, both nav indicators and the morph — the brief's `DampingRatioLowBouncy` was already in use on the nav controls and was removed, not extended. `PillSegmentedControl` (16/12 radii, neutral selection wash), `FloatingPillNavBar` (64 dp, no glass, no dark shadow), `LibraryScreen` (dead `LibrarySegmentedControl` duplicate deleted, chips on `AppRadius`) and `NsfwHubScreen` (hairline chip, 3 `Bold` dropped) finished the §2bb cleanup; `PosterPalette.kt` + `PaletteCacheTest` (3 cases) + `androidx.palette` deleted; root Gradle repaired (`:desktop` asked for a versioned Kotlin plugin after `kotlin.android` had claimed it). `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **306/306**, `./gradlew help` at root ✅. No version bump, no APK, not pushed. Rendered/motion verification and frame-drop measurement still need a device; duplicate `poster:<key>` across Home rails is the open risk. |
| 2026-10-30 | **Phase 3 — media player, manga reader, diagnostics & audio (§2bd):** "Stats for nerds" rebuilt as `ui/player/StatsHud.kt`, a `Canvas` HUD that owns its own 250 ms ticker (buffer-ahead sparkline over 40 samples = 10 s, buffered %, stream + download bitrate, resolution/fps/dropped, codec metadata) so a tick can no longer recompose the control overlay. **Spatial audio is offered honestly**: `Spatializer` has no setter, so the sheet row (shown only when `isAvailable && isEnabled`) sets the stream's `AudioAttributes` spatialisation behaviour (`C.SPATIALIZATION_BEHAVIOR_AUTO`/`_NEVER`) on the ExoPlayer, applied at player creation and on toggle, persisted as `audioSpatial`. Reader: new e-ink monochrome preset (`MANGA_EINK`, saturation 0 + contrast 1.45) that replaces invert/sepia, `MutableZoom.panBy`/`clampPan` bounding the pan to the page's real overflow, and the 3-page webtoon preload moved onto `Dispatchers.IO`. Morph keys are now rail-scoped — `sharedPoster(key, rail)`, `rail` threaded through `PosterCard`/`MediaRow` from Home/Anime/Manga/NSFW, and `?from=<rail>` on the detail route read back into `DetailHeader` — which closes §2bc's duplicate-key risk. Player chrome: sheet headings on `titleMedium`/`titleSmall`, every `Bold`/`SemiBold` (13) dropped to `Medium`, round 40 dp/48 dp controls on `CircleShape`. `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **306/306**. No version bump, no APK, not pushed. Device passes (HUD cost, morph origin, spatial audio, e-ink panel, pan clamp) and all frame-timing measurement are still outstanding. |
| 2026-10-30 | **Phase 4 — encrypted backups, smart search & release readiness (§2be):** **ISSUE-15 closed.** New `data/integrations/BackupCrypto.kt` — `BackupFormat` (`MOOVIO-BACKUP-1` magic + 12-byte IV + GCM tag) and `BackupCipher` (AES-256-GCM, fresh IV per export, injected key provider), with the key generated in `AndroidBackupKey` (AndroidKeyStore, no user-auth gate) — no `security-crypto` dependency added. `BackupManager` exports the container, gained `inspect(uri)` returning `Encrypted` vs `LegacyPlaintext` *before* any store is touched, and `importFrom(…, allowPlaintext = false)` refuses a plaintext file unless the caller opted in; Settings shows an "Unencrypted backup" confirmation, exports as `moovio-backup.mbackup` (`application/octet-stream`) and accepts both formats on import. New `data/search/SmartQuery.kt`: offline parser for type words, 18 genres, a year/decade/range (`2024`, `2020s`, two years), a rating floor (`high rating` = 7.0, `rating 8.5`), `recent`/`classic` and sorting, wired into `SearchViewModel` *before* the 350 ms debounce (cancellation + generation guard unchanged); type routes to the API, year/rating/genre narrow the merged result locally, and a muted one-line `summary()` shows what was understood. A four-digit token only counts as a year up to `currentYear + 1`, so "Blade Runner 2049" stays a title. Storage: `DownloadCacheMaintenance.isBelowLimit`/`shouldSweep`, `VideoDownloadManager.sweepOrphansIfLowOnSpace` (orphaned chunks only, never a playable download), `SettingsStore.LOW_SPACE_MB` (default 1024, 0 = off) with a Settings → Downloads ladder and a boot-time sweep in `MoovioApp`. `versionCode` 24 / `versionName` 1.11.0. `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **327/327** (+21: 8 crypto, 13 parser). No APK built, nothing pushed. Unverified on hardware: the KeyStore path, SAF MIME filtering, the boot sweep on a full disk. |
| 2026-10-30 | **v1.11.0 released (§2bf):** bundles the four-phase overhaul — §2bb design tokens (incl. AMOLED surfaces), §2bc shared-element morph + `MoovioHaptics`, §2bd canvas `StatsHud` + reader physics, §2be encrypted backups + offline smart search — and closes ISSUE-15 plus the ISSUE-18 storage prerequisite. `versionCode` 24 / `versionName` 1.11.0; `:app:assembleRelease` ✅ signed with the same rotated key as v1.10.0 (V2 cert SHA-256 `2EDB949B…351379`) → `dist/app-release.apk` + `dist/Moovio.apk` (44,129,211 bytes, SHA-256 `8B212EF4…547311`, byte-identical); badging verified (`com.moovio.app`, code 24, name 1.11.0, minSdk 24, targetSdk 34, label `Moovio`). `:app:compileDebugKotlin` ✅, `:app:testDebugUnitTest` ✅ **327/327** (36 test sources, 4,787 lines; main 172 files, 34,074 lines). **No tag and no GitHub Release** — the updater still serves v1.10.4 until the asset is uploaded. |

---




## 7. Where to pick up next

0. **Post-v1.10.0 follow-ups (new):**
   - **Device pass on the renamed app.** `com.moovio.app` is a new app id: verify a *fresh* install
     (no old data to lean on), that `MoovioApp` boots, the CloudStream provider list loads (the
     repaired shims), a provider's toast is visible, and a provider's setting survives a restart
     (the Jackson codec fix). Then verify the *upgrade* story is communicated: the old
     `com.novastream.app` install cannot be updated in place, so the old app has to be uninstalled.
   - **Rename the GitHub repo** (`speckle-hub/nexusstream` → `speckle-hub/moovio`) and update
     `UpdateChecker.DEFAULT_REPO`, `website/lib/utils.ts` (`GITHUB_URL`, `APK_URL`,
     `RELEASES_URL`) and the Trakt client-id fallback string. Until then the updater and the site
     CTA still work, but the URLs contradict the brand.
   - **Clear the open backlog** in §4: ISSUE-15 (plaintext backup secrets) and ISSUE-16 (wrong
     tracker ids) are the two that touch user data; ISSUE-18 (HLS export) is the one users hit
     most often.
0b. **CI** — no workflow exists, so "292 tests, all green" is asserted rather than enforced
   (ISSUE-26).
0c. **Design-system follow-ups (new, §2bb) —** (a) device pass on the new type scale and surfaces:
   Home hero legibility under the logo row, `SurfaceCard` in dark **and** light, True AMOLED
   (cards now go #000 too), a bright accent (Amber/Neon Purple) on the CTA to confirm the derived
   `actionFill` ink, poster hairlines on 3-across grids, and the stream sheet's two-role
   `ResolutionBadge`; (b) **done in §2bc** — `NsfwHubScreen`, `LibraryScreen`, `FloatingPillNavBar`
   and `PillSegmentedControl` are on the tokens now; `PlayerActivity`'s 13 `Bold` and 9 pill radii
   were finished in §2bd, leaving only `MainActivity` (1 `Bold`); (c) **done** —
   `PosterPalette.kt` deleted with its 3 tests and the `androidx.palette` dependency; (d) **done** —
   the root `./gradlew` configures again.
0d. **Phase 2 follow-ups (new, §2bc) —** (a) device pass on the poster → header morph: confirm the
   frame actually grows from 2:3 into the wide header with no flash, and that it still lands when the
   source poster was inside a `LazyRow` that had scrolled; (b) **check duplicate keys** — a title on
   Home can sit in both the featured banner and a rail, so two composables can register
   `poster:<key>` (the case Compose documents as unsupported); thread a per-rail key through
   `sharedPoster` if the morph starts from the wrong copy; (c) feel the haptics on real hardware
   (brightness/volume bands, heart, season chips, Library long press) and the `settle()` springs at
   60/120 Hz; (d) measure frame timing in `HomeScreen`/`BrowseScreen`/`NsfwHubScreen` if anything
   looks uneven — no frame-drop measurement has been taken, before or after.
   **Status:** (b) is now **fixed in §2bd** (rail-scoped keys via `?from=`); (a), (c) and (d) still
   need hardware.
0e. **Phase 3 follow-ups (new, §2bd) —** (a) watch the HUD on a device: confirm 250 ms ticks cost
   nothing visible and that the bitrate sparkline settles instead of jittering on a churning stream;
   (b) spatial audio on real headphones — a device whose `Spatializer` is *off* should hide the row
   entirely, one that is on should audible-pan with the switch; (c) e-ink preset on an actual e-ink
   panel (and on a normal screen, where it should read as hard monochrome, not grey); (d) the
   reader's new pan clamp under a real pinch near the page edge; (e) with rail keys in place, check
   that the morph now starts from the copy that was tapped when the same title appears twice on Home,
   including when the tapped rail had been scrolled.
0f. **Phase 4 follow-ups (new, §2be) —** (a) on a device: export a backup, confirm the file is
   opaque, then restore it after a process death — that is the only way to prove the KeyStore key is
   re-openable; (b) open an old v1 `.json` backup and confirm the confirmation dialog appears and
   *Cancel* leaves the library untouched; (c) try the SAF picker in a real file manager — the export
   is `application/octet-stream` and some managers grey it out for the import filter; (d) the boot
   sweep needs a device that is genuinely under the floor (fill the cache, set the floor above free
   space) to prove it reclaims only orphans; (e) smart search on real catalog data: "anime 2024 high
   rating" and "scifi movies" through add-ons that return partial metadata, where a missing `year`
   drops an item when a year filter is active — decide whether that should fall back to no filter.
0g. **Release follow-through for v1.11.0 (new, §2bf) —** the APK is built and verified but **not
   published**. To finish the release: tag `v1.11.0` on the commit that contains the four phases,
   push it, and publish a GitHub Release with `app-release.apk` + `Moovio.apk` attached and marked
   Latest (the site CTA and the in-app updater both resolve
   `releases/latest/download/app-release.apk`). Until then `dist/` is ahead of what users get. All
   four phases remain **device-unverified** — §7 0d/0e/0f lists exactly what to poke on hardware
   first (morph origin, HUD tick cost, KeyStore re-open, boot sweep).
1. **Manual verification pass** on a device/emulator:
   - Fresh install → Home populates; open a movie → stream sources listed (Torrentio / TPB+).
   - Settings → Add-ons → CloudStream repo → provider list is **no longer empty**.
   - NSFW → NSFW Anime → Jikan "Top" row populates even while the filtered Jikan endpoint 504s.
   - NSFW → NSFW Anime → search returns results (Jikan fallback or AniList).
   - Manga → open a licensed title (e.g. One Piece) → chapter list shows only readable chapters.
2. Run the device pass in step 1, then archive a release build: `./gradlew :app:assembleRelease`
   (signing material comes from the untracked `local.properties` / `MOOVIO_*` env vars — §5).
3. Test suites live in `app/src/test/java/com/moovio/app/` — **292 tests** across 33 files; the
   full per-file breakdown is in §1. If more clients need coverage (the CloudStream shims, `Http`,
   `SettingsStore`, the download managers, the extension engine), extract the pure logic and add
   cases there — the shims are the highest-risk untested code in the repo.
4. Optional later: flip `isMinifyEnabled = true`, smoke-test on a device, ship.
   (The keystore was already rotated on 2026-10-22 — §2ax/§8 — and the old blob purged from the
   history, so "rotate the keystore if the app is ever published" is done.)
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
12. **Re-test Phase 6 on a device:** Home hero fills the screen width (no card gutter), dots sit
    on the artwork and each slide shows title/meta/description/Play; player timestamps + slider
    track move live during playback, dragging the slider doesn't stutter and seeks once on
    release; left-edge drag → brightness bar + %, right-edge drag → volume bar + %, horizontal
    drag → seek pill, each fading ~1.4 s after release; switch Home → Search → back → scroll
    position and search term/results are both still there. **NSFW section:** Real 18+ → search →
    open a video → Back → must land on Real 18+ with the same term *and* results (not NSFW
    Anime); switching sub-tabs (NSFW Anime ⇄ Real 18+) must keep each section's typed query.
13. **Re-test the built-in NSFW-anime sources** (see §10): with **no add-ons installed**, NSFW →
    NSFW Anime → open a title → an episode list appears within a few seconds and the Streams panel
    shows a `HentaiMama` / `HentaiPlay` entry; Play must actually start (ExoPlayer must send the
    site-root Referer). With Stremio NSFW add-ons enabled, their entries must appear *after* the
    built-in ones, and a title the scrapers don't know must still fall back to them cleanly.
14. **Re-test Phase 7 on a device** (see §2c): poster grids in Library / Search / “See all” must
    **reflow with the screen width** (rotate to landscape and use a tablet — the column count
    should change, and the same card should now be the same size in Home, Library and Search).
    Tap and **hold** any poster and the card should shrink slightly; tapping the `×` should still
    show the confirmation dialog and the chip must still be easy to hit near the poster's edge.
    Clear the text in the new search field → recent terms and trending chips appear; tapping a chip
    searches; a cleared query removes it from recents. Type a query → nine shimmer poster skeletons
    should appear, then the results grid **without the layout jumping**. Poster metadata lines
    (year · rating) should be readable against the background. Confirm nothing regressed on
    Home / Detail / Manga / NSFW / Settings.
15. **Re-test Phase 8 on a device** (see §2e): **tap a poster on Home, Search, Library and "See all" —
    it should morph into the detail header** (the poster grows and the corners round over ~400 ms)
    rather than hard-cutting, and Back should reverse it. If a morph looks like a cut, that screen's
    `PosterCard` isn't inside a `ProvideSharedScope` block. On Home, rows should fade + rise in
    sequence ~40 ms apart; **scroll fast and confirm no item flashes back to invisible** (that would
    mean the stagger re-keyed on the lazy index). Switching bottom tabs should cross-fade with a
    slight sideways drift; Detail/Add-on Manager should push up from the bottom. **Re-check the Gson
    and Media3 paths** — `Library → Downloaded Videos → play` (deserializes `MediaItem` via
    `Http.gson`) and ordinary playback — since K2 changed the code generator underneath.
16. **Re-test Phase 9 on a device** (see §2f): the bottom bar should have **five** items (Home,
    Search, Browse, Library, Settings) with the selected one scaled up and bold. Open a detail page —
    **Play/Read** should be the only filled, glowing button; the heart should be a small circular
    button in the top-right of the header, and both header buttons must stay readable over a *bright*
    poster. **Tab-index migration (test this explicitly):** on the old build pick a non-Home tab and
    force-stop, then install this build — it should open on **Home**, not on whatever the stale index
    happened to point at. Then confirm the new index persists correctly (pick Library, force-stop,
    reopen → Library). **Browse tab:** Manga and NSFW must both load behind the segmented control,
    each keeping its own rows/search/sub-tab when you switch between them, and switching to NSFW
    **while the NSFW lock is enabled** must still show the lock screen rather than content. Streams
    list: a 4K/HDR stream should get a gold "HDR" or violet "4K" badge, 1080p the accent badge, and
    an unrecognised quality must show the add-on's literal text rather than a guessed resolution.

17. **Re-test Phase 10 on a device** (see §2g): the Home hero must fill the screen edge-to-edge
    **behind the status bar** with the NexusStream logo floating over it (white, readable) — no black
    strip above the artwork; with an empty feed the logo should sit as a normal top row. On a detail
    page the title must sit clearly **below** the back/heart buttons with no overlap, and the genre
    chips must have a visible gap under the Play/Read button. **Type in the top Search field —
    characters must actually appear** and results must follow. Scroll any grid (Search, Library,
    Favourites, Browse → Manga/NSFW, “See all”) fast and confirm **no poster paints over the header,
    search field or segmented control**; a poster tap after the scroll settles must still morph into
    the detail header. The Library must show two tabs — **Downloads** (videos + chapters) and
    **My Library** (Continue Watching + Favourites) — with the `×`/long-press removal still working.

18. **Re-test Phase 11 on a device** (see §2h): **Player** — leave the app mid-playback and confirm
    it drops into PiP (and that PiP is skipped when paused); open the Speed sheet, set 1.5x and
    confirm playback speeds up and survives a re-open; toggle Audio boost/Normalize and listen for
    the level change; play a series episode and confirm the **Next episode in 15…** overlay appears
    in the bottom-right with **Play now / Cancel**, and that **Skip Intro/Outro** show while inside
    the window and seek past it; double-tap the left/right half for a ±10 s ripple; pinch to cycle
    Fit/Stretch/Fill/16:9/21:9; tap the lock and confirm chrome hides and gestures stop until
    unlocked; tap Cast (opens the system chooser or reports none); open the external sheet → VLC/MPV
    when installed. **Theme** — toggle Material You and confirm the accent follows the wallpaper
    (API 31+); toggle True AMOLED and confirm backgrounds are true black; change each subtitle
    size/colour/opacity/offset and confirm they apply on the next playback. **Manga reader** — in
    landscape with double-page on, two pages should sit side by side (correct order in RTL); toggle
    invert/grayscale; pinch-zoom then turn a page — with zoom lock on the level persists, off it
    resets; close and reopen a chapter and confirm it returns to the saved page; toggle auto-crop
    and confirm uniform margins are trimmed; with volume-key page turns on, the volume rocker
    should page instead of changing volume.

19. **Re-test Phase 12 on a device** (see §2i): **Sources** — open Settings → Sources dashboard, run
    **Test** on an installed extension (latency + item count, or a clear error), and toggle one off
    then on and confirm it unloads/reloads. **Failover** — play a stream whose primary URL you can
    break (e.g. airplane-mode toggle or a dead mirror) and confirm it switches to a backup rather
    than dropping out. **Headers** — set a custom User-Agent/Referer and confirm requests still work.
    **Downloads** — change max parallel, pick a backup folder, and with auto-download on play a
    series episode near its end and confirm the *following* episode appears in Downloads. **Home
    rows** — reorder under Settings → Home row order and confirm the Home feed matches after a cold
    start. **Stats** — the Library Stats tab should show hours/genres from real history. **Backup** —
    export a `.json`, wipe favourites, then import and confirm they return. **Scrobbling** — add a
    token, enable a provider, finish a title whose id matches that provider and confirm the sync (or
    the honest "skipped — no id" message). **Security** — enable app lock and relaunch (prompt
    appears); enable incognito and confirm Continue Watching stops updating; enable screen
    protection and confirm screenshots are blocked. **Update** — set a repo and run Check for
    updates; with a newer release+APK it should download and open the installer.

20. **Re-test Phase 13 on a device** (see §2j): **Library crash fix** — with a *pre-existing*
    malformed favourite in storage (a hand-edited DataStore value, or an app upgraded from the old
    build), the Library must open and render the **remaining** favourites instead of crashing or
    blanking the section. **Category chips** — under Downloads and My Library, each of
    All/Movies/TV Shows/Anime/NSFW must filter in place **without resetting the scroll position**,
    and downloads must filter to the right category too (a Real 18+ download should not appear
    under Movies). **Detail page** (a TMDB series) — season chips select without collapsing;
    episode rows show thumbnail + `E3 · Title` + duration/air date + a saved-progress bar; **Play
    is pre-targeted at the selected season's first episode**; tapping an episode opens the
    **Stream Selection bottom sheet** (not an inline list); the header `PlaylistPlay` button opens
    the    same sheet for the on-screen episode; playing a torrent from the sheet shows the
    "Preparing stream…" overlay (sheet closes first).

21. **Re-test the expanded Real 18+ sources on a device** (see §2k): the Real 18+ tab should now
    show rows for **xHamster, YouPorn, RedTube, MissAV, Jable, HPJAV** and (when reachable)
    **Avgle** alongside the original five — a dead site must show only its own notice while the
    rest keep loading, and the tab must still paint progressively (cached sources instantly, the
    rest as they land). For each new source, open a title → confirm the thumbnail/title resolve,
    then tap Play and confirm a stream actually starts. Report which sources need markup updates —
    these seven were **not** probed live (see the ⚠️ table in §9), so expect drift on some. Search
    a keyword with a source-specific term and confirm relevance filtering behaves.

22. **Re-test the static-review fixes on a device** (see §2l): tap the **Search** tab → the bottom
    bar must stay visible, the Search item must highlight, and after force-stop/reopen Search must
    be restored as the last tab. Type ≥2 chars then delete back to 1 in the NSFW Anime **and** NSFW
    Manga searches → the in-flight spinner must clear (no permanently stuck "searching" state).
    Open an episode whose stream resolve fails, then tap it again → the sheet must re-attempt
    instead of staying empty. Home: the Continue Watching `×` must be easy to hit near the poster
    edge; force a feed error (airplane mode + refresh) → only the error state shows, not two
    stacked empty states. Search → empty state → the "Clear" recents tap target must be comfortable.
    Manga reader: switch accent/Material You/AMOLED in Settings then open a chapter → it must match;
    rotate (or recreate) the app with the app lock on → **no** second biometric prompt; open a
    chapter and press volume while it is still loading → volume changes (keys are not consumed until
    pages show); leave the reader with volume-key page turns on → the rocker must turn pages.
    Finally, pull-to-refresh Home and confirm the featured carousel keeps auto-advancing.

23. **Re-test the expanded NSFW-anime sources on a device** (see §2m): with **no** Stremio NSFW
    add-ons installed, open an NSFW-anime title and confirm the episode list + Streams panel now
    resolve from the built-in sources; a stream from any of **Hanime, HentaiHaven, HStream,
    Hentaigasm, MuchoHentai, HentaiCity, Haho** should appear when reachable. A Cloudflare-blocked
    or dead site must show only its own notice while the other sources keep resolving, and the
    Stremio NSFW add-ons must still appear alongside the built-in entries as fallback. Report which
    sources return nothing — these seven were **not** probed live (see the ⚠️ table in §10), so
    expect markup drift, especially on the WordPress sites and any player hosted by a third party.

24. **Re-test the final UI checkup on a device** (see §2n): **Player theme** — set Settings →
    Theme to **Light**, open a video and open each bottom sheet (Quality & Audio, Episodes, Speed,
    External player) → headings, rows and chips must be dark-on-light and readable (no
    white-on-white), with the selected row/chip still in the accent; change the accent and confirm
    the sheet highlight follows it; toggle Material You and True AMOLED and confirm the player
    matches the rest of the app. The control-bar chips (Reload / Quality & Audio / Speed /
    Episodes) must stay legible **over the video** in both themes, and controls/HUD text over the
    footage must stay white. **Touch targets** — the search field's ×, the NSFW search box, the
    subtitle colour swatches and the detail back/heart circles should all be comfortable to hit
    (≥48 dp) with no visible size regression; the NSFW box must show the pulsing dot while a
    search is in flight and clear on the ×. **Rotation** — Add-on Manager: pick the 3rd tab, rotate
    → still on that tab. Settings → Home rows: press up/down a few times, rotate → the in-progress
    order survives (and is still persisted after a cold start). Collection ("See all"): rotate →
    the header sits below the status bar with no clipped title, including on a tall status-bar /
    notch device.

25. **Re-test the NSFW speed + metadata + notice pass on a device** (see §2o): **NSFW Anime search**
    should feel as fast as normal anime search — type a word and results appear without waiting on
    Jikan (which is only consulted when AniList returns nothing). Open a title that previously
    showed **"Couldn't load details"** (e.g. *Boku to Misaki-sensei*) — it must now render (AniList,
    else Jikan, else the catalog item itself) and show an **episode list** filled from the built-in
    hentai sources where any site can provide one. **Real 18+ search** — results must start showing
    while slower sources are still loading, a hung site must not hold the list, re-typing the same
    query must return instantly (5-min cache), and the search must not hang past ~8 s per source.
    **Notice** — the "sources unavailable" banner must be a single compact line with an **×** to
    dismiss; tapping it expands the per-source detail, and it must not cover half the page.

26. **Re-test the Phase 14 downloader fixes on a device** (see §2p): **Header-gated download** —
    download a Pornhub (or other adult) stream and an HLS episode that need a page Referer/Cookie;
    the download must start and complete instead of failing immediately or mid-transfer, and the
    completed item must play offline from the shared cache. **Mirror failover** — queue a stream
    whose primary URL is dead but which has `alternates`; the failed entry should be replaced by
    the backup and the download should finish (or, with no mirrors left, show one clean failure).
    **Storage fallback** — confirm the app boots and downloads still work with app-private storage
    available (the historic path); on a build variant with external storage unmounted, confirm it
    degrades to the next location instead of crashing. **Cleanup + meter** — with a failed/partial
    download and some orphaned bytes present, Settings → Storage must show the Completed /
    Temporary-orphaned split, and **Clear download cache & temporary files** must remove the
    temporary/orphaned bytes (and drop the failed entries from the Downloads tab) while leaving
    completed downloads intact.

27. **Re-test Phase 15 on a device** (see §2q): **Haptics** — drag the seek bar (one tick per
    second of content, not a continuous buzz), tap ±10 s and double-tap to skip (one tick each),
    and turn manga pages by swipe and by volume key (one tick per turn, none on chapter open).
    **Subtitle sync** — open Quality & Audio → Subtitles, move the Sync offset slider and confirm
    captions shift earlier/later (native captions hide while an offset is set and the overlay
    renders instead); Reset returns to native rendering; the value survives reopening the player.
    **Stats for nerds** — toggle Stats and confirm resolution / codec / bitrate / bandwidth / FPS /
    buffer / state / dropped-frames update live and that switching quality changes the readout.
    **Warm filter** — enable it under Settings → Manga Reader and confirm pages take a sepia tint
    (and that it composes with invert/grayscale). **Auto-delete watched** — download a title, enable
    the toggle, play past 90 % and confirm the download and its cached bytes disappear from the
    Downloads tab / storage meter.
28. **Re-test Phase 16 on a device** (see §2r): open a few Movies / TV episodes / Anime titles on a
    fresh install and confirm streams appear with **Torrentio / TPB+ entries first** (reliability-
    first order), higher quality and more seeders leading within an add-on, and that a genuine
    4K/1080p entry is present when one exists. Kill/rename an add-on host (or use airplane-mode for
    one add-on) and confirm the picker shows **“Some add-ons could not be reached”** with the add-on
    name rather than a blank sheet, and that the empty hint points at Settings → Add-on Manager.
    Re-open the same title within 5 minutes and confirm the stream list is **instant** (cache). In
    the Add-on Manager, confirm the **Recommended** row offers Torrentio / TPB+ / MediaFusion /
    OpenSubtitles v3 one-tap installs and that browsing a deliberately-dead repo reports
    “Repository unavailable or empty”.    Time Home and a search with a slow add-on enabled and confirm
    they still return within the 6–7 s bounds instead of hanging.
29. **Re-test Phase 17 on a device** (see §2s) — this is the device report that prompted the pass:
    **Speed** — open NSFW → Browse → NSFW Anime and confirm the first rows paint in about a
    second (progressive; `Jikan Top` may land later or drop without holding the tab), then type a
    query in NSFW Anime search and confirm results come back at AniList speed (well under a few
    seconds — no minute-long spinner). Open a title and confirm the detail page renders in a few
    seconds: AniList metadata first, episode fill stopping at its 10 s budget, streams resolving
    under their 12 s cap with per-source notices for anything that times out. **Matching** —
    search **“Boku to Misaki-sensei”**, tap it, and confirm the page that opens is titled **Boku to
    Misaki-sensei** — not “Netokano: After Party…” (or anything else) — with that title's poster
    and its **own** episode list; the streams for an episode must belong to the same title. Sanity
    checks: a title with no built-in episode match still renders correctly (AniList metadata,
    Stremio NSFW add-ons as stream fallback, no wrong series spliced in); Jikan-only paths open
    the right title or fall back to the catalog item rather than a random adult entry; the NSFW
    Stremio add-on fallback still appears when AniList *and* Jikan both return nothing.

30. **Re-test Phase 20 on a device** (see §2u) — this pass was written and verified statically (no
    device/emulator in this session), so every item needs hardware: **Grids** — open Search, NSFW,
    Browse and "See all" on a 360 dp-class phone and confirm **three poster columns per row** with
    16 dp gutters (rotate/resize to landscape or a tablet and the count should still grow, not
    shrink); the Library's Downloads and My Library cards must be inset from the screen edges like
    the headers above them. **Recent Searches** — clear the Search field and confirm the Recent
    chips appear after a typed search (they only ever recorded chip taps before). **Floating nav** —
    the pill bar must stay on screen while scrolling down and up on Home/Browse/Search/Library/Settings,
    and content must still clear it at the bottom. **Hero** — the banner shows FEATURED, title,
    Type · Year · Rating and the Play/Read pill only (no description paragraph), and manga banners
    say **Read**. **Settings crash** — from Settings → Sources & Network tap **Sources dashboard**
    and **Home row order** (both with no extensions installed / before rows load, and again after)
    — neither may crash, and the Home rows screen must populate its reorder list. **Accent picker** —
    the accent row scrolls horizontally to the last swatch. **Manga paging** — open a chapter in
    Right→Left and Left→Right and swipe: pages must advance (and still turn via volume keys and tap),
    pinch-to-zoom must still work and must not turn pages while zoomed; Webtoon must be unchanged.
    **Continue Reading** — read a chapter of a normal manga and an NSFW manga (leave mid-way), then
    confirm a **Continue Reading** row on Home and a matching section under Library → My Library
    showing the chapter title and reading percentage, that tapping it opens the right detail page,
    that the `×` removes it, that finishing (≥95 %) hides it, and that incognito mode writes nothing.

31. **Re-test Phase 21 on a device** (see §2v) — both items are static+unit-verified only, so
    hardware is the confirmation: **Card size** — open Home, then Browse → Manga and Browse → NSFW,
    and confirm a rail card is **the same size on all three** (the NSFW/Manga rails used to draw at
    `MediaRow`'s 132 dp default while Home passed 150 dp); scroll a rail to its end and confirm the
    16 dp start/end gutters match Home's, and rotate to landscape/tablet to confirm the results grid
    still grows columns and fills the width with no new side margins. **Sources dashboard** — with a
    **pre-existing malformed `extensions.json`** (the strongest reproduction: `adb shell` into
    `/data/data/com.moovio.app/files/extensions.json` and delete the `"name"` key from one
    entry, or drop its `"id"`) open Settings → Sources & Network → **Sources dashboard**: it must
    render the remaining extensions instead of crashing, the affected entry should show its id as
    the title, and toggling it off/on plus **Test** must still work. Also check the normal cases
    again (no extensions installed → empty state; a well-formed install list → rows); **Home row
    order** right below it must keep working. If it *does* still crash, capture the logcat stack
    (`adb logcat -b crash`) — the fix is data-level, so a remaining crash would be a different
    exception and the stack is the fastest way to identify it.

32. **Re-test Phase 35 on a device** (see §2ar) — static + unit-verified only, so
    hardware is the confirmation: **Single-tap** toggles deterministically (visible →
    hides immediately, hidden → reveals + fresh 3 s countdown); **double-tap** L/R
    seeks ∓ interval with ripple; **hold** boosts to 2X with the pill and restores on
    release (short taps never boost); controls **fade** (200/250 ms) after 3 s of
    playing, stay visible when paused/buffering/sheet-open/scrubbing, and hide to the
    lock toggle only when locked; top-bar track/speed icons open their sheets; the
    bottom **quality badge** shows the stream quality (hidden when the source reports
    none); every interaction (seek, slider, speed, boost) restarts the countdown.

33. **Re-test the v1.10.1 patch fixes on a device** (see §2az) — all three were verified by
    compile + unit tests only, so hardware is the confirmation: **Cast** — open any video and tap
    the cast button: `logcat` must show **no** `background can not be translucent` / "init failed"
    line, the real route picker must open (or report "no devices"), and the player must keep
    recomposing afterwards (open the Quality sheet after tapping cast). **NSFW Manga** — NSFW Hub →
    NSFW Manga → open a title: the chapter list must fill (it was empty in the v1.10.0 report) and
    a chapter must read; also open a *normal* manga to confirm no NSFW chapters leak into its
    list. **Torrent timeout** — on a network that blocks trackers, play a torrent stream: after
    ~20 s the spinner must close and the failure dialog must appear with **Retry**; Retry must
    restart preparation (spinner returns) and Cancel must leave the page usable; with trackers
    reachable, normal torrent playback must be unaffected.

> **Repo note:** this directory is now a git repository. Commits so far: initial snapshot, player
> autoplay/gestures/reader modes, WorkManager repo sync, and the extension engine.
>
> The commit identity was supplied per-command (`git -c user.name=… -c user.email=…`) instead of
> writing it to `.git/config`.

---

## 8. Decisions log

| Date | Decision | Rationale |
|---|---|---|
| 2026-10-23 | **Pre-seed the provider repositories, including an adult one (CXXX)** (supersedes the 2026-10-07 "ship source-neutral" decision) | The user asked for the four repositories to be usable out of the box, so `Defaults.repos` now carries Phisher98, CXXX, Yuzono and Keiyoushi. Nothing is auto-installed from them and the adult repo is flagged `nsfw`, so its providers are reachable only from the NSFW area. Cost: a public build now ships links to repositories that host unlicensed and adult sources — the same posture the user accepted when asking for them by URL, but it is a deliberate reversal of the neutrality rule, not an oversight. |
| 2026-10-23 | **The NSFW chapter feed asks for the non-safe MangaDex ratings and never falls back to an unfiltered request** | Probed live: `/manga/{id}/feed` on a pornographic series returns 0 chapters for `contentRating[]=safe` **and** 0 with no `contentRating[]` at all (the API defaults to the SFW set), while `suggestive`+`erotica`+`pornographic` returns them. The report's suggested fallback ("retry without the filter") therefore cannot work — it re-asks the safe question. The feed asks for `NSFW_FEED` (never `safe`) and a SFW title that comes back empty is widened once to that same set, which recovers an adult series whose `MediaItem` lost its `NSFW_MANGA` type. |
| 2026-10-23 | **`MediaRouteButton` gets its own opaque theme instead of another try/catch** | The v1.10.0 report read the exception as escaping the Compose frame loop; the factory already caught it. The throw happens *before* a button exists, because `MediaRouterThemeHelper` computes a contrast ratio against `android:colorBackground`, which is transparent in this player (platform Material theme + transparent Compose window). Catching it kept the cast button permanently dead; giving the button an AppCompat-based theme with an opaque background fixes the cause and the `runCatching` stays as the last line of defence. |
| 2026-10-22 | **Rename the package too, not just the display name** (supersedes the 2026-10-01 "display name only" decision) | The product is Moovio; leaving `com.novastream.app` in the package, the OAuth scheme, the theme names and every brand token kept three names alive across the code, the docs, the site and the release assets. The rename is mechanical (1118 references, verified by package/dir parity + import resolution + a real compile), and the cost — losing in-place updates — is unavoidable *anyway*, because the signing key had to be rotated (§2ax). Doing both at once means one reinstall instead of two breaking changes. |
| 2026-10-22 | **Rotate the release key and keep it out of the repository** (supersedes the 2026-09-30 "keep the existing keystore" decision) | The old key was committed in the first commit and stayed recoverable from the pack, so anyone with repo read access could mint an APK that Android accepts as an in-place update of this app — and the update dialog installs whatever the release asset says. The key now lives in `~/.moovio/`, the build reads it only from the untracked `local.properties`/`MOOVIO_*` env vars, and the blob was purged from the history. Cost: the next release cannot update an existing install in place. That is the right trade — a key that anyone can use to push code to every user's phone is worse than an inconvenience. |
| 2026-10-22 | **Sign the updater's APK before handing it to the system installer** | Android's own installer refuses a foreign-signed package, but only *after* the user has been shown an install prompt for it, and the release-asset URL is a single point of compromise. Verifying package + signer first turns "the release account was hijacked" from "every user gets an install prompt" into "the update is refused with a visible reason". |
| 2026-10-22 | **The OAuth redirect carries a one-shot random nonce** | `moovio://oauth` is a custom scheme, so any app on the device can fire it. Without a nonce, a hostile app could hand the callback its own authorization code and have the resulting token written into the user's tracker list. The nonce is generated with `SecureRandom`, stored in DataStore before the browser hop, compared with `MessageDigest.isEqual` and consumed on use. |
| 2026-10-22 | **Provider settings are stored with the CloudStream library's own Jackson configuration** | The provider dexes carry upstream's *read* path inlined, and upstream reads with Jackson. The shim wrote with the app's Gson, which omits null fields — a Jackson/Kotlin read then fails with `MissingKotlinParameterException` and the provider's setting is silently lost. The library's mapper is not public API, so an equivalent instance (Kotlin module + `FAIL_ON_UNKNOWN_PROPERTIES` off) is built in `ProviderSettingsCodec`; Gson stays as a fallback. |
| 2026-10-22 | **Download removal goes through `DownloadManager.removeDownload`, not a foreground service start** | `DownloadService.sendRemoveDownload(..., foreground = true)` has to *start* the download service, which Android 12+ refuses from the background — and the auto-delete path runs exactly there (player `onPause`/`onStop`). The exception was swallowed, so watched downloads were never reclaimed. The manager API needs no service start and the cached bytes are released explicitly. |
| 2026-10-22 | **The `applicationId` change is accepted rather than reverted** | Keeping `com.novastream.app` would preserve in-place updates, but the name is the product's identity and the key rotation had already broken that path. A reinstall is a one-time cost; a permanent mismatch between the app's name and its package is a permanent cost. The release notes state the upgrade requirement up front. |
| 2026-10-07 | **Ship source-neutral for the public repo: no preinstalled stream providers or provider repos** | A public app that auto-installs Torrentio/TPB+ and points at piracy-provider repos is a DMCA/takedown magnet and arguably facilitates infringement; the same app with a neutral add-on *manager* is precedented (Kodi, CloudStream's repo-less distribution). Only legal seeds remain (Cinemeta metadata, OpenSubtitles). Cost: out-of-box playback needs one user-installed stream add-on, so the Detail empty-state and Add-on Manager now explain that path; the provider tabs gained a real add-repo UI since seeds no longer exist. Player/extension/torrent *protocol* support stays — neutral technology, same posture as VLC/ffmpeg. Removed seeds are pruned from existing installs via the retired-lists so an upgraded install matches a fresh one. |
| 2026-10-07 | **Website lives in `website/` inside the app repo, deployed to Vercel (not GitHub Pages or a second repo)** | One repo = one place for issues, releases and the site; Vercel's root-directory deploy builds only `website/` and redeploys on every push to `main`, and Next.js is the native stack of the requested component libraries (Aceternity/HeroUI/ThreeUI are React). A separate repo would only buy independent history the project doesn't need. |
| 2026-10-07 | **HeroUI pinned to `~2.7.11` with Tailwind v3.4** | HeroUI 2.8 bundles Tailwind v4 types (the build fails type-checking the `heroui()` plugin) and its runtime targets the TW4 CSS-first config. The Aceternity-style components and the existing config are TW3-style (`tailwind.config.ts`), so the site stays on the last TW3-native HeroUI line. Revisit when the Aceternity ports are migrated to CSS-first config. |
| 2026-10-07 | **framer-motion via LazyMotion `domAnimation` + `m.*` everywhere** | The full `motion` bundle costs ~24 kB of the first-load JS on a page that only uses plain DOM/SVG animations; LazyMotion cut the page chunk 134 → 92 kB with no visual change. Rule for future components: never import `motion`, always `m` from `framer-motion`. |---|
| 2026-10-06 | **Phase 20: keep `GridCells.Adaptive`, retuned to 88 dp, rather than reverting to `GridCells.Fixed(3)`** | Fixed(3) would hit the reported "three columns on a phone" exactly but re-breaks what Phase 7 fixed — a 7" phone and a tablet would again get identically-sized cards, and the last row would need phantom fillers back. Dropping the adaptive minimum to 88 dp lands three columns on every phone width from 320–411 dp (the 360 dp case was `floor(340/120) = 2`) while still widening on large screens. Trade-off: a ~96 dp card on very wide tablets where 108 dp used to give slightly larger art. |
| 2026-10-06 | **Phase 20: the floating pill nav is permanently visible — scroll-driven hiding removed** | The `NestedScrollConnection` hide-on-scroll shipped in Phase 18 in one session and was reported as both "inverted" and "intrusive": cumulative-drag direction heuristics are inherently surprising, and hiding primary navigation costs more than the pixels it saves. Keeping `LocalFloatingNavBottomPadding` means nothing changed except the bar's visibility. |
| 2026-10-06 | **Phase 21: the Sources-dashboard crash was a Gson null-field decode, not the early `return@Column` Phase 20 removed** | Phase 20 inferred the crash from the two screens' unusual `return` shape and the report survived it, which falsifies the theory. The actual mechanism is the same one Phase 13 documented for the Library: Gson bypasses Kotlin constructors via `Unsafe` and writes missing JSON fields as `null` into **non-null** slots, so `Text(desc.name)` throws inside its parameter check and a null `id` breaks `LazyColumn`'s keys. The durable fix is therefore the established repo pattern — a tolerant, per-entry, JVM-testable codec — rather than more UI-level guarding, because the UI cannot distinguish "legitimately absent" from "corrupted" once the null has been decoded. |
| 2026-10-06 | **Phase 21: one shared `AppSpacing.railCard` rather than a per-screen card width** | The mismatch was invisible in code review precisely because `MediaRow`'s default silently applied: two call sites omitting an argument looked fine while rendering at 132 dp next to Home's 150 dp. Naming the width as a token forces every rail call site to state its intent and makes a future change a one-line edit instead of a hunt through screens. |
| 2026-10-06 | **Phase 20: manga reading progress shares the playback history store instead of getting its own** | `LibraryStore`'s `WatchEntry` already models "item + position + duration + updated-at", so `positionMs = pagesRead / durationMs = totalPages` gives a correct reading percentage with **no schema, codec, backup or Stats change** — the split is purely presentational (`MediaItem.isReadable`). Trade-off: `WatchStats` now counts a chapter as ≤ a few ms of "watch time", which is immaterial and already covered by its `runCatching` fallback; a dedicated reading store would have meant a second list to back up, restore and cap. |
| 2026-10-05 | **Phase 17: NSFW-anime search is built-ins-authoritative (add-ons are fallback-only)** | It used to await the Stremio NSFW add-on fan-out on every search, so even a fast AniList hit paid the add-on timeout. Mirroring the rule Real 18+ already follows (ISSUE-9) returns normal searches at AniList speed; the cost is that add-on-only titles appear only when AniList *and* Jikan both come back empty — accepted, because built-in results are the ones the app can match, rank and verify. |
| 2026-10-05 | **Phase 17: a fallback may never rename or re-episode the selected title** | Wrong content is strictly worse than missing content. `adultTitlesMatch` (significant-word overlap; filler like "to" never counts) gates episode/stream resolution, metadata-candidate acceptance and `JikanClient.detailsByTitle`, and `hentaiFallback` keeps the selected item's title/poster. Trade-off: a title whose only match is a zero-significant-word collision now gets *no* built-in episodes (Stremio NSFW add-ons answer instead) rather than a plausible-but-wrong series. |
| 2026-10-05 | **Phase 17: every NSFW-anime hop gets a hard timeout** | The "over a minute" report was unbounded awaits (20 s connect / 30 s read + 429 retries) stacking across the episode lookup, home rows, detail sources and the add-on fan-out. Bounds degrade to the same per-source failure the UI already renders as a notice; caller cancellation is rethrown (`bounded()` catches only `TimeoutCancellationException`) so ISSUE-10's lesson still holds. Worst case is now a bounded ~20–26 s on a fully degraded network instead of an open-ended hang. |
| 2026-10-04 | **Phase 16: reliability-first stream ranking puts preferred add-ons above higher quality elsewhere** | The request was explicitly to "prefer the most reliable sources first", so `StreamRanking.rank()` sorts by preferred host (Torrentio/TPB+/MediaFusion) **before** quality and seeders, meaning a known-good 720p can outrank an unlisted 4K. Trade-off accepted: the preferred list is only the vetted first-class stream providers, and a user's own add-ons still sort by quality. Runtime-measured against real add-ons remains a device-pass item (§7 item 28). |
| 2026-10-04 | **Phase 16: Hexated/Phisher/community repos are seeded by default; MediaFusion is recommend-only** | Repos are cheap to seed (they only populate the Add-on Manager browser) so all 7 are merged on startup. MediaFusion needs per-user configuration and a dead auto-seed would degrade the out-of-box experience, so it is offered as a one-tap **Recommended** install instead of being enabled silently. |
| 2026-10-02 | **Bump to Kotlin 2.0.20 + Compose 1.7.0** (was 1.9.24 + 1.6.8); AGP left at 8.5.0 | `SharedTransitionLayout` / `Modifier.sharedElement` only exist in Compose 1.7+, and Kotlin 2.0 requires the Compose compiler to move into its own Gradle plugin versioned to match Kotlin — so reaching 1.7 meant crossing both boundaries together. Chosen over hand-rolling a bounds morph on 1.6.8 because the real API is maintained by the Compose team and gets fixes. K2's cost proved to be a single API rename (`beyondBoundsPageCount` → `beyondViewportPageCount`); all 30 tests passed unchanged. AGP was deliberately left alone — Kotlin 2.0 supports 7.3.3–8.3.x officially and 8.5.0 works, so bumping it would have been churn for no observed gain. Residual risk is runtime codegen (Gson/reflection), called out in §7 item 15. |
| 2026-10-02 | **Phase 12: scrobbling uses pasted tokens + id-matching, not full OAuth** | A real Trakt/MAL/Kitsu OAuth flow needs per-provider client ids/secrets and a redirect receiver, and *correct* syncing needs a title→provider-id resolver across five catalogs. Rather than ship a half-working OAuth dance, the integration surface is real (settings, tokens, HTTP calls) but only syncs when the item already carries that provider's id and otherwise reports "skipped". Called out in §2h/§2i so it isn't mistaken for a finished integration. |
| 2026-10-02 | **Phase 12: storage selector scopes to exports/backups, not the media cache** | Media3's shared `SimpleCache` is constructed once at a fixed app-private path and read by the player; relocating it to a SAF tree would require tearing down/rebuilding the cache + download manager and handling revoked permissions mid-download. The SAF picker is therefore used for JSON exports (and to remember the user's preferred folder) while downloads stay in app storage. |
| 2026-10-04 | **Phase 14: keep the media cache in app-private storage, with external/cache dirs as fallbacks** | SimpleCache is constructed once against a fixed directory, so flipping the default to `getExternalFilesDir` would silently discard every existing download on upgrade (and leave the old internal cache unreachable by the new orphan cleaner — the opposite of the cleanup goal). The writability chain therefore tries the historic `filesDir/downloads/media` first and degrades to external/internal-cache only when that location can't be written. The SAF tree stays scoped to exports. |
| 2026-10-04 | **Phase 15: subtitle sync is a client-side cue overlay, not a player API** | Media3 1.3 ships no subtitle-offset method (and upgrading the whole Media3 stack just for it is risky). Instead the player records `Player.Listener.onCues` into a bounded timeline and, when an offset is set, hides the native `SubtitleView` and re-renders the cue set active at `position - offset`. Trade-off: the offset overlay ignores per-cue positioning/animation from the caption file (it renders centred text with the user's colour/size/opacity), while offset 0 keeps the full native renderer. |
| 2026-10-02 | **Phase 11: Cast/DLNA + auto-crop + audio boost are best-effort, not framework integrations** | A real Cast integration needs `androidx.mediarouter` + the Cast SDK, and true white-border cropping needs per-bitmap pixel scanning — both are heavy new dependencies/IO for the payoff. The Cast button opens the system route chooser, crop is a 6% scale-and-clip, and boost/normalize are `LoudnessEnhancer` gain presets. Called out in §2h so they aren't mistaken for full implementations. |
| 2026-09-30 | **Keep the existing release keystore** (do not rotate) — **SUPERSEDED 2026-10-22, see above** | The `.jks` + password are public (README/history), so the key is already "burned" — but the keystore is *deliberately* shipped so the repo builds a signed APK out of the box, and rotating would change the signing identity and break in-place updates for any installed build. Password is now read from untracked `keystore.properties` or env vars. If this is ever distributed publicly, rotate then. |
| 2026-10-01 | **Extension engine trust model** | Loaded extensions run in-process with app privileges; this is **not** a security sandbox. Loading is restricted to app-private storage and failures are contained per extension. Only load user-installed, trusted extensions; a real sandbox would need a separate process + restricted classloader. |
| 2026-10-01 | **Rename: display name only** — **SUPERSEDED 2026-10-22, see above** | The app is NexusStream in the UI, but the Kotlin package and `applicationId` stay `com.novastream.app`, so the signing identity and in-place updates are preserved. Renaming the package later would change the app identity. |
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
  AdultSources.kt     registry: 24 sources (14 general + 10 JAV; HPJAV/Avgle retired — §2w;
                      + Eporner/Beeg + JavGuru/SupJav/VJAV — §2aq;
                      + PornTrex/SxyPrn/FullPorner/TXXX + JavMost/KissJAV/Javtiful/OpJav/7MMTV — §2as)
  AdultHttp.kt        browser-like headers; 403/503/challenge -> typed AdultSourceException;
                      probe()/verifyPlayable() pre-flight media URLs with the player's headers
  TubeMedia.kt        shared media extractor: absolute/protocol-relative/escaped MP4+HLS, quality
                      labelling, ad filtering, thumbnail helper, `definitions()` JSON parser
  PornhubSource.kt    listing, search, categories, detail (mediaDefinitions -> HLS per quality)
  XvideosSource.kt    listing, search, categories, detail (html5player MP4 + HLS)
  XnxxSource.kt       same X-style markup as XVideos
  HqpornerSource.kt   listing + best-effort third-party embed resolution
  SpankbangSource.kt  Cloudflare-gated; graceful error
  XhamsterSource.kt   /newest, /best, /trending, search, categories; xplayer config extraction
  YouPornSource.kt    /most_viewed, /best, /recent, search; media_definitions API + page fallback
  RedTubeSource.kt    /best, /newest, search; media_definitions API + page fallback
  MissavSource.kt     JAV: /en/new|release|trending|today, search, genres; surrit .m3u8
  JableSource.kt      JAV: /latest-updates, /hot, /new, search, tags; var hlsUrl / <source>
  PornTrexSource.kt   KVS tube (/latest-updates, /top-rated, /most-popular, /search/,
                      /categories/; `/video/{id}` cards) (§2as)
  SxyPrnSource.kt     aggregator (front wall + top charts, `/post/` cards, tag-shelf
                      search; best-effort file-host streams) (§2as)
  FullPornerSource.kt full-length tube (`/watch/{hash}` cards, featured + `/home/`) (§2as)
  TxxxSource.kt       Aylo tube (`media_definitions` API + page fallback, like YouPorn) (§2as)
  JavMostSource.kt    JAV WordPress tube (`/?s=` + `/search/`, taxonomy-aware list) (§2as)
  KissJavSource.kt    JAV KVS tube (`kissjav.li`: latest + most-viewed + jav/fc2ppv rows) (§2as)
  JavtifulSource.kt   JAV catalogue (`/video/{id}` codes, sort rows, `/category/`) (§2as)
  OpJavSource.kt      JAV WordPress tube (`/?s=` + `/search/`, static shelves) (§2as)
  SevenMmtvSource.kt  JAV tube (`7mmtv.sx` + `7mmtv.tv` mirror fallback) (§2as)
  EpornerSource.kt    general tube via public JSON API v2 (latest/top-week/top-month, search,
                      /cats/ + static fallback); streams via TubeMedia (§2aq)
  BeegSource.kt       general tube via v6 JSON API (index/search/video file-map) + HTML
                      fallback (§2aq)
  JavGuruSource.kt    JAV WordPress tube (jav.guru: front page, english-subbed/decensored
                      archives, ?s= search) (§2aq)
  SupJavSource.kt     JAV catalogue (/search + ?s= fallbacks, maker/tag-aware list) (§2aq)
  VjavSource.kt       JAV tube (vjav.com: /videos/latest-updates|most-popular|top-rated,
                      /search/1/?s=) (§2aq)
  HpjavSource.kt      JAV WordPress: /lastest-updates, /popular, ?s= search; JW Player file: (retired — §2w, hpjav.tv DNS-dead)
  AvgleSource.kt      JAV JSON API: /videos, /search, /categories, /video/{vid} -> video_url (retired — §2w, api down 520)
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
| xHamster | ⚠️ built to current markup, **not probed live** | ⚠️ | ⚠️ `xplayerSettings` via `TubeMedia` |
| YouPorn | ⚠️ not probed live | ⚠️ | ⚠️ `media_definitions` API + page fallback |
| RedTube | ⚠️ not probed live | ⚠️ | ⚠️ `media_definitions` API + page fallback |
| MissAV | ⚠️ not probed live | ⚠️ | ⚠️ `surrit.com` `.m3u8`; packer may hide it |
| Jable | ⚠️ not probed live | ⚠️ | ⚠️ `var hlsUrl` / `<source>` |
| Eporner | ⚠️ not probed live (§2aq) | ⚠️ JSON API v2 | ⚠️ video page via `TubeMedia` |
| Beeg | ⚠️ not probed live (§2aq) | ⚠️ v6 API + HTML fallback | ⚠️ `file`-map streams + `TubeMedia` fallback |
| JavGuru | ⚠️ not probed live (§2aq) | ⚠️ `/?s=` + `/search/` | ⚠️ post page via `TubeMedia` |
| SupJav | ⚠️ not probed live (§2aq) | ⚠️ `/search/` + `/?s=` | ⚠️ post page via `TubeMedia` |
| VJAV | ⚠️ not probed live (§2aq) | ⚠️ `/search/1/?s=` | ⚠️ video page via `TubeMedia` |
| PornTrex | ✅ homepage live (§2as) | ⚠️ KVS `/search/` shape | ⚠️ video page via `TubeMedia` |
| SxyPrn | ✅ homepage live (§2as) | ⚠️ tag-shelf search | ⚠️ file-host embeds (best-effort) |
| FullPorner | ✅ homepage live (§2as) | ⚠️ `/search/` shape | ⚠️ watch page via `TubeMedia` |
| TXXX | ❌ unreachable (§2as) | ⚠️ Aylo `/?search=` shape | ⚠️ `media_definitions` + `TubeMedia` |
| JavMost | ❌ blocked (§2as) | ⚠️ WP `/?s=` shape | ⚠️ post page via `TubeMedia` |
| KissJAV | ✅ homepage live (§2as, `kissjav.li`) | ⚠️ KVS `/search/` shape | ⚠️ video page via `TubeMedia` |
| Javtiful | ✅ homepage live (§2as) | ⚠️ `/search?q=` shape | ⚠️ video page via `TubeMedia` |
| OpJav | ❌ blocked 403 (§2as) | ⚠️ WP `/?s=` shape | ⚠️ post page via `TubeMedia` |
| 7MMTV | ❌ unreachable (§2as) | ⚠️ WP `/?s=` shape | ⚠️ post page via `TubeMedia` |
| HPJAV | **retired** (§2w — hpjav.tv DNS-dead) | — | — |
| Avgle | **retired** (§2w — api.avgle.com 520) | — | — |

> ⚠️ = parser written to the site's known/current shape with multi-fallback extraction, but **not**
> verified against a live response (adult endpoints are blocked from the build environment). See
> §2k; device pass is §7 item 21.

### Tests
`app/src/test/java/com/moovio/app/AdultSourcesTest.kt` — 13 tests over trimmed **real** markup
(Pornhub list ×2 + streams ×2 incl. headers/get_media-skip, XV list + streams, XNXX list, HQporner
list ×2 + embed/iframe, `adultAbsoluteUrl`, duration/quality helpers).

`app/src/test/java/com/moovio/app/AdultSourcesExtraTest.kt` (new, 11 tests) — parser coverage
for the seven added sources plus `TubeMedia` and the registry (§2k).

`app/src/test/java/com/moovio/app/ExpandedSourcesTest.kt` (new, 10 tests — §2aq) —
fixture coverage for Eporner (API + listing), Beeg (API + file-map streams), JavGuru, SupJav,
VJAV, Rule34Video and AnimeIdHentai, plus the expanded registry uniqueness guards.

`app/src/test/java/com/moovio/app/MoreSourcesTest.kt` (new, 14 tests — §2as) —
fixture coverage for HenVids, Ohentai, Hentai.tv (JSON), XAnimu, PornTrex, SxyPrn,
FullPorner, TXXX, JavMost, KissJAV, Javtiful, OpJav and 7MMTV, plus the expanded
registry uniqueness guards.

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

---

## 10. Built-in NSFW-anime stream sources (Hentai)

**Goal:** the NSFW Anime section plays with **zero Stremio add-ons installed**. AniList keeps doing
what it always did (catalog, search, detail, related); only *which episodes exist* and *where they
play* come from built-in scrapers. Stremio NSFW add-ons remain enabled as extra / fallback entries.

### Architecture (modular — add/remove a site in one line, mirrors `data/adult/`)
```
data/hentai/
  HentaiModels.kt       HentaiEpisode (series/title/url/episode/poster, toVideo(), videoId),
                        HentaiEpisodes / HentaiStreams (results + per-source errors)
  HentaiSource.kt       interface: search(query) -> episode-level entries, streams(episode)
  HentaiSources.kt      registry: 13 sources (HentaiMama/HentaiPlay + 5 added in §2m + Rule34Video/
                        AnimeIdHentai — §2aq + HenVids/Ohentai/Hentai.tv/XAnimu — §2as;
                        Hanime/Hentaigasm retired — §2w) + byId()/isBuiltIn()
  HentaiTubes.kt        shared helpers: media-URL extractor, qualityOf, page-referer stream builder,
                        WordPress card parser, isContentLink, one/two-hop embed resolver (§2m)
  HentaiMamaSource.kt   dooplay series pages + admin-ajax mirror resolution (see chain below)
  HentaiPlaySource.kt   episode-per-page; the page already carries a direct MP4 <source>
  HanimeSource.kt       hanime.tv JSON: search.htv-services.com + /api/v8/video videos_manifest (retired — §2w)
  HentaiHavenSource.kt  WordPress ?s= search; .xxx host + .org mirror
  HStreamSource.kt      hstream.moe; /hentai/ permalinks; HLS/MP4 from page or player frame
  HentaigasmSource.kt   WordPress ?s= search (retired — §2w)
  MuchoHentaiSource.kt  WordPress ?s= search
  HentaiCitySource.kt   WordPress ?s= search
  HahoSource.kt         GET /anime?q= series search -> per-series /anime/{slug}/{n} episode expansion
  Rule34VideoSource.kt  tube archive: /search/{q}/ -> /video/{id} cards (§2aq)
  AnimeIdHentaiSource.kt WordPress index: /?s= -> /search/ -> www-mirror fallback (§2aq)
  HenVidsSource.kt    series tube: /hentai/{slug} episode cards, /search/ fallbacks (§2as)
  OhentaiSource.kt    PHP tube: detail.php?vid= cards, tagsearch.php search (§2as)
  HentaiTvSource.kt   JSON API (/api/search?q=, episode-level, nhplayer embeds) (§2as)
  XAnimuSource.kt     Rule34 tube: confirmed /?s=, numeric-permalink filter (§2as)
  HentaiRepository.kt   parallel search + query variants + series grouping + relevance ranking,
                        episode picking, per-source errors, 10-min TTL caches (48 entries), and
                        AdultHttp.verifyPlayable() pre-flight of every stream it returns
```

### How it plugs in (AniList metadata untouched)
- `DetailViewModel.load()` sets `streamsLoading` up front, and when an NSFW-anime detail arrives
  with **no** episodes (AniList never sends any for adult titles) it calls
  `HentaiRepository.episodes(...)` with the detail's English title then its romaji/native/synonym
  titles (≤3), splices the found episodes into `MetaDetail.videos` and re-runs `buildSeasons()`.
  Poster / synopsis / cast / related / genres still come from AniList.
- Episode ids are `hentai:<sourceId>:<episode page url>` (`HentaiEpisode.videoId`), so
  `StreamRepository.streamsFor()` knows instantly that a Stremio add-on cannot interpret them.
- `StreamRepository.streamsFor()` for `MediaType.NSFW_ANIME` runs the built-in resolution and the
  enabled Stremio add-ons **concurrently**, returns `builtIn.ranked() + stremio.ranked()` (built-in
  first, dedup keeping the built-in copy), and skips Stremio entirely for `hentai:` ids.
- `Http.httpPostForm()` was added for the WordPress `admin-ajax.php` form POST the mirror loader
  uses; it accepts header overrides so the request carries the page `Referer` and a browser UA.

### Resolution chains (probed live against the real pages)
| Source | Chain |
|---|---|
| HentaiMama | `GET /search/{title}` → `article.series-card` (series) → `GET /tvshows/{slug}` → `a.dt-se-item` / `Watch Ep n` (episode) → `GET /episodes/{slug}` → `data-post` + `#option-{n}` → `POST wp-admin/admin-ajax.php` (`action=get_player_contents&a=&i=`) → JSON `n-1` holds the `/?dt_embed=` iframe → `GET {iframe}` → jwplayer page → direct `.mp4`/`.m3u8` (`gdvid.info`/`hentaidoge.org`) |
| HentaiPlay | `GET /search/{title}` → `<div id="post-">` cards (episode pages) → `GET {episode}` → `<source src="…mp4">` direct |

### Live verification
| Source | Search | Episodes | Streams |
|---|---|---|---|
| HentaiMama | ✅ `hentaimama.io`, mirror `www.hentaimama.com` | ✅ `dt-se-item` + `Watch Ep n` fallback | ✅ full chain → `206 video/mp4`, valid `ftyp` (Referer: site root) |
| Rule34Video | ⚠️ not probed live (§2aq) | ⚠️ `/video/` cards | ⚠️ page/embed via `resolvedStreams` |
| HenVids | ✅ homepage live (§2as) | ⚠️ `/hentai/` cards | ⚠️ page/embed via `resolvedStreams` |
| Ohentai | ✅ homepage + tagsearch live (§2as) | ✅ `detail.php?vid=` cards | ⚠️ page/embed via `resolvedStreams` |
| Hentai.tv | ✅ `/api/search` live (§2as) | ✅ episode-level JSON | ⚠️ nhplayer embed pages |
| XAnimu | ✅ homepage + `/?s=` live (§2as) | ✅ numeric-permalink cards | ⚠️ page/embed via `resolvedStreams` |
| AnimeIdHentai | ⚠️ not probed live (§2aq) | ⚠️ WP post cards | ⚠️ page/embed via `resolvedStreams` |
| HentaiPlay | ✅ (needs the punctuation-stripped query variant) | ✅ one episode per page | ✅ `206 video/mp4`, no Referer required |
| Hanime | **retired** (§2w — search host DNS-dead) | — | — |
| HentaiHaven | ⚠️ not probed live | ⚠️ post cards | ⚠️ page `<source>` / embed (≤2 hops) |
| HStream | ⚠️ not probed live (Cloudflare) | ⚠️ `/hentai/…` cards | ⚠️ HLS/MP4 from page/embed |
| Hentaigasm | **retired** (§2w — timeouts) | — | — |
| MuchoHentai | ⚠️ not probed live | ⚠️ post cards | ⚠️ page `<source>` / embed |
| HentaiCity | ⚠️ not probed live | ⚠️ post cards | ⚠️ page `<source>` / embed |
| Haho | ✅ probed live 2026-10-06 (§2w): `GET /anime?q=` → `anime-loop` series cards | ✅ series page → `/anime/{slug}/{n}` expansion (parallel, capped 3) | ⚠️ episode page → `/embed?v=` → `<source type="video/…">` filegasm URLs (extension-less; now extracted) |

> ⚠️ = written to the site's known shape with HTML fallbacks, but **not** verified against a live
> response (Cloudflare/adult endpoints are blocked from the build environment). See §2m; device
> pass is §7 item 23.

### Known caveats / next
- The seven sources added in §2m could **not** be probed live; each fails independently by design,
  and sites whose player is a third-party host (streamtape, doodstream, …) will yield no direct
  stream (the Stremio NSFW add-ons then answer as fallback).
- Other sites evaluated and rejected earlier still stand: `hentaifox` is galleries-only,
  `mp4hentai` only exposes file-host links, `tube.hentaistream.com` has no extractable source.
- HentaiMama's site-assigned `EP n` badges do not always line up with AniList's episode numbering
  (extras/OVAs get their own numbers). `HentaiRepository.pickEpisode()` therefore only falls back
  to a single-episode site; otherwise it returns *nothing* for a mismatched number so the Stremio
  add-ons can answer instead of playing the wrong episode.
- Episode discovery adds ~2–4 s to the first open of an NSFW-anime detail (search + expansion);
  it is cached for 10 minutes, so back-navigation is instant. The Streams panel says
  "Finding streams…" for that whole window instead of flashing "No streams found".
- Parser tests now exist: `HentaiSourcesExtraTest.kt` (10 cases, added in §2m) covers Hanime,
  Haho, the `HentaiTubes` extractor/card parser and the registry. The original two sources' parsers
  remain `internal` and are still a candidate for a `HentaiSourcesTest` fixture file.
- Device pass still outstanding: does HentaiMama's `gdvid.info` MP4 actually play in ExoPlayer, and
  do the Stremio NSFW fallback entries still appear alongside the built-in ones — plus reachability
  of the seven new sources (§7 item 23).

---

## Outstanding items (post-source-hygiene pass)

### Fixed in this session

| Item | What was done |
|------|---------------|
| Real 18+ hard-failing sources | Removed Jable, YouPorn, RedTube, SpankBang, TXXX, JavMost, OpJav, 7MMTV from `AdultSources.all`. Their source objects/tests remain. |
| Real 18+ experimental sources | Flagged `experimental = true` on Eporner, Beeg, PornTrex, SxyPrn, FullPorner, Missav, Xhamster, JavGuru, SupJav, Vjav, KissJav, Javtiful. |
| Real 8+ verified/partial sources | Kept Pornhub (intermittent), XVideos (partial), Xnxx (working), HQporner (partial, third-party embed). |
| NSFW Anime dead hosts | Hanime and Hentaigasm already unregistered (DNS-dead / timeouts). |
| NSFW Anime experimental sources | Flagged `experimental = true` on HentaiHaven, HStream, MuchoHentai, HentaiCity, Rule34Video, AnimeIdHentai, HenVids, Ohentai, HentaiTv, XAnimu. |
| NSFW Anime verified sources | Kept HentaiMama, HentaiPlay, Haho. |
| AdultSource / HentaiSource interfaces | Added `val experimental: Boolean = false` property for UI to flag unverified sources. |
| Per-source error isolation | Already in `AdultRepository` / `HentaiRepository` — one dead source never blocks the tab. |

### Still outstanding (next session)

| # | Item | Status | Plan |
|---|------|--------|------|
| 1 | **Aniyomi / Mihon extension execution** | Blocked | APKs install/enable/update but do not execute. Toolchain migration (AGP ≥ 8.9, compileSdk 36/37, Kotlin 2.1–2.4, OkHttp 5) required to load `com.github.mihonapp:extensions-lib:1.6.0` / `com.github.komikku-app:aniyomi-extensions-lib:13`. **Do not ship a fake runtime.** UI already honest: extensions show as "installed/toggled" only, no "play" claim. Device-verified: "Installed but not yet executable" banner on both Aniyomi and Mihon tabs. |
| 2 | Device pass (§7 items 20–31) | ✅ **Completed** | Real-device verification done on OnePlus CPH2487/Android 16: MangaDex NSFW chapter feed populated, Yuzono + Keiyoushi repos seeded & browsable, Add-on Manager NSFW switch present, extension install/enable/update UI present, CXXX Real 18+ search path present. Honesty UI confirmed on Aniyomi/Mihon tabs. |
| 3 | Torrent watchdog edge case | Mitigated | 20 s watchdog with Retry dialog added in v1.10.1. Still can't connect on networks without tracker access (expected). Not device-verifiable on this network. |
| 4 | Check for updates / Export backup | Not tested | Deferred to post-release QA (TEST_REPORT P2). |
| 6 | Cast / MediaRouteButton | Fixed | v1.10.1: `ThemeOverlay.Moovio.CastButton` opaque theme + `runCatching` guard. Already in code; structurally verified via `aapt2`. Not exercised on device (cast not available on test network). |
| 7 | MangaDex empty NSFW feed | ✅ Fixed + device-verified | v1.10.1: NSFW feed asks for non-safe ratings only. Device-verified on CPH2487 — chapter lists now populate correctly. |

### Version note

Bumped with the source-hygiene rebuild: `versionCode` 23 / `versionName` 1.10.4 — signed
`dist/app-release.apk` + `dist/Moovio.apk` rebuilt (43,963,425 bytes, SHA-256
`F4A5B1B2B0DE7CE90B6E598176790987C14759D4E7D2E0ACE3494A8A3A0E711B`, signer
`2EDB949B…351379`, verified with `apksigner`);
`:app:testDebugUnitTest` ✓ **309/309**.
Tagged `v1.10.4`, pushed (`main` + tag) and **published as a GitHub Release with
`Moovio.apk` + `app-release.apk` attached** (`releases/latest/download/app-release.apk` →
HTTP 200 / 43,963,425 bytes, digest matches the local build), so the in-app updater and the
site CTA now serve v1.10.4. Website bumped to v1.10.4 / 309 tests (`next build` ✓).
