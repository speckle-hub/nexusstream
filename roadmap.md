# NexusStream — Roadmap & Working Notes

> Living document. Update the **Status** and **Changelog** sections every session so work can
> resume exactly where it left off. Last updated: 2026-10-08 — **v1.3.0 released:**
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
> **Name change:** the app is now **NexusStream** (display name only — the Kotlin package and
> `applicationId` remain `com.novastream.app` so existing installs update in place; see §8).

---

## 1. What this project is

Android streaming app (`com.novastream.app`) unifying Stremio add-ons, CloudStream extensions,
Aniyomi and Mihon/Keiyoushi repos into one Compose UI. **Kotlin 2.0.20 (K2) + Jetpack Compose
1.7.0 (M3)**, Media3 ExoPlayer, Coil, OkHttp/Gson/Retrofit, DataStore, manual DI
(`di/AppContainer.kt`). AGP 8.5.0. The repo also contains **`website/`** — the Next.js landing
site deployed to Vercel (§2y) — and is public at `github.com/speckle-hub/nexusstream`.
~20,900 LOC across 104 Kotlin source files in a single `:app` module (+15 test files). Unit tests
(113 cases):
`LibraryCodecTest.kt` (15), `AdultSourcesTest.kt` (13), `AdultSourcesExtraTest.kt` (11),
`HentaiSourcesExtraTest.kt` (12, incl. the Haho series/stream parses — §2w), `StreamRankingTest.kt` (9), `ParsersTest.kt` (9),
`DownloadMaintenanceTest.kt` (8), `ExtensionCodecTest.kt` (8, `extensions.json` decode — §2v),
`TitleMatchTest.kt` (7, NSFW title matching — §2s),
`PlaybackMathTest.kt` (6), `ResolutionBadgeTest.kt` (4), `PaletteCacheTest.kt` (3),
`MangaDownloadTest.kt` (3), `WatchStatsTest.kt` (3) and `SearchCancellationTest.kt` (2).

---

## 2. Current focus

**Topic: the public release and the website.** The source-neutral app (§2x) is now public on
GitHub with a proper `v1.1.0` release, and the project has a marketing site: `website/` is a
static Next.js 15 + Tailwind + HeroUI + Aceternity-style + ThreeUI landing page, optimized and
deployed to Vercel from the same repo. Full detail in **§2y**.

Previous focus: source neutrality (§2x) for the public GitHub release; Phase 21 (§2v) — NSFW/Browse
card alignment & the Sources crash root cause; Phase 20
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
`ui/screens/sources/SourcesScreen.kt`, `app/src/test/java/com/novastream/app/ExtensionCodecTest.kt` (new).

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
`app/src/test/java/com/novastream/app/AdultSourcesExtraTest.kt`,
`app/src/test/java/com/novastream/app/HentaiSourcesExtraTest.kt`.

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
`ui/screens/detail/DetailScreen.kt`, `app/src/test/java/com/novastream/app/StreamRankingTest.kt`,
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
`app/src/test/java/com/novastream/app/TitleMatchTest.kt` (new).

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
`app/src/test/java/com/novastream/app/StreamRankingTest.kt` (new).

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
`app/src/test/java/com/novastream/app/PlaybackMathTest.kt` (new).

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
`app/src/test/java/com/novastream/app/DownloadMaintenanceTest.kt` (new).

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
`app/src/test/java/com/novastream/app/HentaiSourcesExtraTest.kt` (new) — 10 cases: Hanime
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
`app/src/test/java/com/novastream/app/AdultSourcesExtraTest.kt` (new) — 11 cases: xHamster /
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

**Tests:** new `app/src/test/java/com/novastream/app/LibraryCodecTest.kt`, 15 cases covering
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
`app/src/test/java/com/novastream/app/LibraryCodecTest.kt` (new).

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
`app/src/test/java/com/novastream/app/WatchStatsTest.kt` (new).

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
`data/local/SettingsStore.kt`, `app/src/test/java/com/novastream/app/ResolutionBadgeTest.kt` (new).

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
| 17 | Glass elevation system | S–M | Real shadow + top-edge highlight hairline; M3 `surfaceContainer*` roles |
| ~~18~~ | ~~Detail CTA hierarchy~~ | — | ✅ Done in Phase 9 (§2f) |
| ~~19~~ | ~~6 → 5 bottom tabs~~ | — | ✅ Done in Phase 9 (§2f) |
| 20 | `AsyncImage` placeholder / error / crossfade | S | Blank rectangles during scroll today; 200 ms crossfade + shimmer placeholder |
| 21 | `prefers-reduced-motion` | S | The hero's `while(true)` auto-advance loop runs forever with no opt-out; Phase 8's stagger/tab motion also needs the opt-out |
| 22 | `EmptyState` with CTA + glow | S | Currently one icon in a rounded square; every empty state is currently a dead end |
| 23 | Tabular figures app-wide | S | Ratings, byte counts and ETAs jitter without `"tnum"` (done for `RatingBadge` / `ResolutionBadge` only) |

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
| 2026-10-06 | **Phase 21 — Sources dashboard crash root-caused and fixed (§2v):** Phase 20's early-`return@Column` theory was wrong (and is not a crash mechanism — `Column` hands a non-weighted child a bounded `mainAxisMax`, so `Column { header; Lazy*(fillMaxSize()) }` is safe). The real cause is a **Gson null-field decode**: `ExtensionStore.list()` used `Gson().fromJson(…, Array<ExtensionDescriptor>::class.java)`, and Gson instantiates Kotlin data classes through `Unsafe`, so a JSON entry missing a field yields a silent `null` in a **non-null** slot. `SourcesScreen` then hit the compiler's `checkNotNullParameter` NPE on `Text(desc.name)` and fed `LazyColumn` a null key. New `data/ext/ExtensionCodec.kt` decodes element-wise — drops entries missing `id`/`entryClass`/`apkPath`, defaults `name` to the id, keeps `version` optional, de-dupes ids, tolerates a bare object and any unparseable file, and never throws; `ExtensionStore.list()`/`save()` use it, `ExtensionRepository.installed()` is wrapped, and `SourcesScreen` reads through a `runCatching` fallback plus a blank-id filter/de-dupe. `data/ext/ExtensionCodec.kt` (new), `data/ext/ExtensionRepository.kt`, `ui/screens/sources/SourcesScreen.kt`, `app/src/test/java/com/novastream/app/ExtensionCodecTest.kt` (new, 8 cases). |
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

---




## 7. Where to pick up next

1. **Manual verification pass** on a device/emulator:
   - Fresh install → Home populates; open a movie → stream sources listed (Torrentio / TPB+).
   - Settings → Add-ons → CloudStream repo → provider list is **no longer empty**.
   - NSFW → NSFW Anime → Jikan "Top" row populates even while the filtered Jikan endpoint 504s.
   - NSFW → NSFW Anime → search returns results (Jikan fallback or AniList).
   - Manga → open a licensed title (e.g. One Piece) → chapter list shows only readable chapters.
2. Run the device pass in step 1, then archive a release build: `./gradlew :app:assembleRelease`
   (signing now comes from `keystore.properties`).3. Test suites live in `app/src/test/java/com/novastream/app/` — **103 tests** across
   `LibraryCodecTest.kt` (15), `AdultSourcesTest.kt` (13), `AdultSourcesExtraTest.kt` (11, new Real
   18+ sources — §2k), `HentaiSourcesExtraTest.kt` (10, new hentai sources — §2m),
   `StreamRankingTest.kt` (9, reliability-first stream ranking — §2r), `ParsersTest.kt` (9),
   `DownloadMaintenanceTest.kt` (8, download headers + orphan detection — §2p),
   `TitleMatchTest.kt` (7, NSFW title matching — §2s), `PlaybackMathTest.kt` (6, subtitle-sync cue
   selection + stats formatting — §2q), `ResolutionBadgeTest.kt` (4), `PaletteCacheTest.kt` (3),
   `MangaDownloadTest.kt` (3), `WatchStatsTest.kt` (3) and `SearchCancellationTest.kt` (2).
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
    `/data/data/com.novastream.app/files/extensions.json` and delete the `"name"` key from one
    entry, or drop its `"id"`) open Settings → Sources & Network → **Sources dashboard**: it must
    render the remaining extensions instead of crashing, the affected entry should show its id as
    the title, and toggling it off/on plus **Test** must still work. Also check the normal cases
    again (no extensions installed → empty state; a well-formed install list → rows); **Home row
    order** right below it must keep working. If it *does* still crash, capture the logcat stack
    (`adb logcat -b crash`) — the fix is data-level, so a remaining crash would be a different
    exception and the stack is the fastest way to identify it.

> **Repo note:** this directory is now a git repository. Commits so far: initial snapshot, player
> autoplay/gestures/reader modes, WorkManager repo sync, and the extension engine.
>
> The commit identity was supplied per-command (`git -c user.name=… -c user.email=…`) instead of
> writing it to `.git/config`.

---

## 8. Decisions log

| Date | Decision | Rationale |
|---|---|
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
  AdultSources.kt     registry: 10 sources (5 original + xHamster/YouPorn/RedTube + 2 JAV; HPJAV/Avgle retired — §2w)
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
| HPJAV | **retired** (§2w — hpjav.tv DNS-dead) | — | — |
| Avgle | **retired** (§2w — api.avgle.com 520) | — | — |

> ⚠️ = parser written to the site's known/current shape with multi-fallback extraction, but **not**
> verified against a live response (adult endpoints are blocked from the build environment). See
> §2k; device pass is §7 item 21.

### Tests
`app/src/test/java/com/novastream/app/AdultSourcesTest.kt` — 13 tests over trimmed **real** markup
(Pornhub list ×2 + streams ×2 incl. headers/get_media-skip, XV list + streams, XNXX list, HQporner
list ×2 + embed/iframe, `adultAbsoluteUrl`, duration/quality helpers).

`app/src/test/java/com/novastream/app/AdultSourcesExtraTest.kt` (new, 11 tests) — parser coverage
for the seven added sources plus `TubeMedia` and the registry (§2k).

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
  HentaiSources.kt      registry: 7 sources (HentaiMama/HentaiPlay + 5 added in §2m; Hanime/Hentaigasm retired — §2w) + byId()/isBuiltIn()
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
