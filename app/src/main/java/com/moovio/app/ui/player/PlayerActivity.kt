package com.moovio.app.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.util.Rational
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import androidx.mediarouter.media.MediaRouteSelector
import com.google.android.gms.cast.CastMediaControlIntent
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastSession
import com.google.android.gms.cast.framework.SessionManagerListener
import com.moovio.app.ui.vm.LocalContainer
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.text.Cue
import androidx.media3.common.text.CueGroup
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.moovio.app.MoovioApp
import com.lagradost.cloudstream3.CommonActivity
import com.moovio.app.data.download.DownloadManagerProvider
import com.moovio.app.data.integrations.ScrobbleEvent
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.download.VideoKind
import com.moovio.app.data.model.SubtitleTrack
import com.moovio.app.data.model.Video
import com.moovio.app.data.remote.Http
import com.moovio.app.data.model.WatchEntry
import com.moovio.app.ui.components.MoovioSwitch
import com.moovio.app.ui.theme.LocalMoovioColors
import com.moovio.app.ui.theme.MoovioTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Full-featured video player built on Media3 ExoPlayer.
 *
 * Supports quality/track selection, subtitle + audio track selection, PiP, orientation locking,
 * swipe gestures (brightness left / volume right / seek), an episode side-sheet, a sleep timer,
 * a 5-second autoplay-next countdown and a press-and-hold 2X speed boost (Phase 34).
 */
@UnstableApi
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    private lateinit var item: MediaItem
    private var episodes: List<Video> = emptyList()
    private var index = -1

    private var autoplay = true
    private var subtitlesEnabled = true

    private var source: StreamSource = StreamSource()
    private var subs: List<SubtitleTrack> = emptyList()

    private val titleState = mutableStateOf("")
    private val subtitleState = mutableStateOf<String?>(null)
    private val countdownState = mutableStateOf<Int?>(null)
    private val episodesState = mutableStateOf<List<Video>>(emptyList())
    private val indexState = mutableStateOf(-1)

    private var countdownJob: Job? = null
    private var progressJob: Job? = null
    private var autoTriggered = false
    /**
     * Phase 36 pre-fetch: the episode index whose successor's streams were already
     * background-resolved (-1 = none). Reset on every episode switch so each episode
     * pre-fetches its own next exactly once.
     */
    private var prefetchedIndex = -1
    /** Phase 30: completion scrobble fires once per title (reset on every source switch). */
    private var scrobbled = false
    /**
     * Sleep timer (Phase 29): a fixed-delay [sleepJob] or an armed end-of-episode pause.
     * [sleepDeadlineState] is a `SystemClock.elapsedRealtime()` deadline for the timed options
     * (null when disarmed or episode-driven) so the UI can render a live "Sleep Nm" readout.
     */
    private var sleepJob: Job? = null
    private val sleepOptionState = mutableStateOf(SleepTimerOption.OFF)
    private val sleepDeadlineState = mutableStateOf<Long?>(null)
    /**
     * False until [resumeLastPosition] has read (and seeked to) the saved position. While false,
     * [recordProgress] refuses to persist, so a quick pause/back on entry can't overwrite the
     * saved spot with position 0 before the resume happens.
     */
    @Volatile
    private var resumeResolved = false

    private var brightnessValue = 0.5f
    /** Playback speed + audio processing (Phase 11). */
    private var playbackSpeed = 1f
    private var audioBoost = false
    private var audioNormalize = false
    /**
     * Live subtitle look (Phase 29): Compose state so in-player style edits re-render the
     * time-shifted overlay and re-apply to the PlayerView immediately (see `PlayerScreen`).
     */
    private val subtitleStyleState = mutableStateOf(SubtitleStyle())
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var audioSessionId = 0
    /** Phase 15: caption timing correction (ms) and auto-delete-watched. */
    private var subtitleSyncMs = 0
    private var autoDeleteWatched = false
    private var autoDeleted = false
    /** Mirror URLs that already failed for the current source, so failover never loops. */
    private val failedUrls = mutableSetOf<String>()
    /** Fractional volume steps accumulated from the edge drag (committed in whole steps). */
    private var volumeAccum = 0f
    /** Horizontal drag translated to a pending seek, flushed at most every [SEEK_FLUSH_MS]. */
    private var pendingSeekMs = 0L
    private var lastSeekFlushMs = 0L
    private val audioManager by lazy { getSystemService(AUDIO_SERVICE) as AudioManager }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        // P0 guardrails: a malformed intent (or a Gson-hole payload with runtime
        // nulls in non-null slots) finishes with a log line instead of crashing.
        val parsedItem = readItem(intent)
        if (!isValidPlayerItem(parsedItem)) {
            Log.e(PLAYER_TAG, "aborting launch: item missing or invalid in intent")
            finish(); return
        }
        item = requireNotNull(parsedItem)
        val first = readSource(intent)
        if (!isPlayableStreamSource(first)) {
            Log.e(PLAYER_TAG, "aborting launch: source missing or has no playable URL")
            Toast.makeText(this, "This stream can't be played.", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        episodes = sanitizeEpisodes(readEpisodes(intent))
        index = readIndex(intent)

        val settings = (application as MoovioApp).container.settings
        val container = (application as MoovioApp).container
        // CloudStream providers report through `CommonActivity.showToast` — attach this Activity so
        // those messages are visible while a stream plays.
        runCatching { CommonActivity.setActivityInstance(this) }
        // Screen protection: the player is the most sensitive surface in the app (it is where the
        // NSFW content plays), so the "Block screenshots" setting has to cover it. MainActivity
        // only guarded the shell, which left the player recordable and in the recents thumbnail.
        if (settings.flagSecure.value) {
            window.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        }
        autoplay = settings.autoplay.value
        subtitlesEnabled = settings.subsEnabled.value
        playbackSpeed = settings.playerSpeed.value
        audioBoost = settings.audioBoost.value
        audioNormalize = settings.audioNormalize.value
        subtitleSyncMs = settings.subtitleSyncMs.value
        autoDeleteWatched = settings.autoDeleteWatched.value
        subtitleStyleState.value = SubtitleStyle(
            scale = settings.subtitleScale.value,
            colorKey = settings.subtitleColor.value,
            bgOpacity = settings.subtitleBg.value,
            bottomOffset = settings.subtitleOffset.value,
            outlineKey = settings.subtitleOutline.value,
        )
        val preferredQuality = settings.preferredQuality.value

        source = requireNotNull(first)
        httpFactory.setDefaultRequestProperties(safeStreamHeaders(source))
        subs = if (subtitlesEnabled) sanitizeSubtitleTracks(readSubs(intent)) else emptyList()
        titleState.value = if (index >= 0) episodeTitle(episodes.getOrNull(index)) else item.title
        subtitleState.value = source.name ?: source.title
        episodesState.value = episodes
        indexState.value = index
        brightnessValue = settings.playerBrightness.value.takeIf { it in 0f..1f }
            ?: window.attributes.screenBrightness.takeIf { it in 0f..1f } ?: 0.5f
        window.attributes = window.attributes.apply { screenBrightness = brightnessValue }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 120_000, 1_500, 3_000)
            .build()
        val selector = DefaultTrackSelector(this).apply {
            var params = buildUponParameters().setAllowVideoMixedMimeTypeAdaptiveness(true)
            qualityCap(preferredQuality)?.let { (w, h) -> params = params.setMaxVideoSize(w, h) }
            setParameters(params)
        }
        trackSelector = selector
        val exo = try {
            ExoPlayer.Builder(this)
                .setTrackSelector(selector)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
                .build()
        } catch (t: Throwable) {
            Log.e(PLAYER_TAG, "ExoPlayer init failed", t)
            Toast.makeText(this, "Player unavailable: ${t.message ?: "init failed"}", Toast.LENGTH_LONG).show()
            finish(); return
        }
        player = exo
        exo.playWhenReady = true
        exo.setPlaybackSpeed(playbackSpeed)
        exo.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(sessionId: Int) {
                audioSessionId = sessionId
                applyAudioEffects(sessionId)
            }

            // Multi-source failover: if a mirror dies mid-playback, transparently switch to the
            // next URL instead of dropping the user out of the player.
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                if (!tryFailover()) {
                    Toast.makeText(this@PlayerActivity, "Playback failed: ${error.errorCodeName}", Toast.LENGTH_SHORT).show()
                }
            }
        })
        try {
            exo.setMediaItem(buildMediaItem())
            exo.prepare()
        } catch (t: Throwable) {
            Log.e(PLAYER_TAG, "media load failed for ${runCatching { item.key }.getOrDefault("?")}", t)
            Toast.makeText(this, "This stream can't be played.", Toast.LENGTH_SHORT).show()
            finish(); return
        }
        // Resume first, then start persisting: the watcher must not record position 0 over the
        // saved spot before the seek lands (see [resumeResolved]).
        lifecycleScope.launch {
            resumeLastPosition()
            resumeResolved = true
            startProgressWatcher()
        }

        setContent {
            // Honor the user's live theme here too — custom accent, Material You and AMOLED black
            // all apply inside the player instead of a hardcoded dark-violet theme (matching the
            // manga reader). The overlay text stays white because it always sits on video.
            CompositionLocalProvider(LocalContainer provides container) {
            val themeMode by settings.theme.collectAsStateWithLifecycle("dark")
            val accent by settings.accent.collectAsStateWithLifecycle("violet")
            val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle(false)
            val amoledBlack by settings.amoledBlack.collectAsStateWithLifecycle(false)
            val dark = when (themeMode) {
                "light" -> false
                "system" -> isSystemInDarkTheme()
                else -> true
            }
            MoovioTheme(
                darkTheme = dark,
                accentKey = accent,
                dynamicColor = dynamicColor,
                amoledBlack = amoledBlack,
            ) {
                PlayerScreen(
                    player = exo,
                    title = titleState.value,
                    subtitle = subtitleState.value,
                    countdown = countdownState.value,
                    episodes = episodesState.value,
                    currentIndex = indexState.value,
                    onBack = { finish() },
                    onPip = { enterPip() },
                    onRotate = { toggleOrientation() },
                    onExternal = { openExternal() },
                    onReload = { reload() },
                    onCancelCountdown = { cancelCountdown() },
                    onNext = { playEpisode(index + 1) },
                    onSelectEpisode = { playEpisode(it) },
                    qualityLabel = source.quality,
                    subtitleStyle = subtitleStyleState.value,
                    subtitleSyncMs = subtitleSyncMs,
                    onSubtitleSync = {
                        subtitleSyncMs = clampSubtitleSyncMs(it)
                        lifecycleScope.launch { settings.setSubtitleSyncMs(subtitleSyncMs) }
                    },
                    onSubtitleStyle = { updateSubtitleStyle(it) },
                    sleepOption = sleepOptionState.value,
                    sleepDeadlineMs = sleepDeadlineState.value,
                    onSleepTimer = { setSleepTimer(it) },
                    initialSpeed = playbackSpeed,
                    audioBoostOn = audioBoost,
                    audioNormalizeOn = audioNormalize,
                    onSpeedChange = { setPlaybackSpeed(it) },
                    onAudioBoost = { setAudioBoost(it) },
                    onAudioNormalize = { setAudioNormalize(it) },
                    onOpenVlc = { openInPackage("org.videolan.vlc") },
                    onOpenMpv = { openInPackage("is.xyz.mpv") },
                    onOpenOther = { openExternal() },
                    onCastSettings = { openCastSettings() },
                    onGestures = { mode, amount ->
                        // Returns the normalized level (0..1) so the gesture HUD can render it.
                        when (mode) {
                            GestureMode.BRIGHTNESS -> adjustBrightness(amount)
                            GestureMode.VOLUME -> adjustVolume(amount)
                            GestureMode.SEEK -> { gestureSeek(amount); seekProgress() }
                        }
                    },
                    onGestureEnd = { endGesture() },
                )
            }
            }
        }
    }

    private fun episodeTitle(ep: Video?): String {
        if (ep == null) return item.title
        val label = ep.title?.takeIf { it.isNotBlank() } ?: "Episode ${ep.episode ?: "?"}"
        return label
    }

    /**
     * OkHttp-backed source that replays [StreamSource.headers] on every request (page Referer,
     * age cookie, user agent). A single factory is reused so episode switches can just update the
     * default request properties.
     */
    private val httpFactory: OkHttpDataSource.Factory by lazy {
        // Adds the user's global custom HTTP identity (Phase 12) when the stream itself did not
        // supply that header — so a source's own Referer/Cookie still wins.
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            val req = chain.request()
            val b = req.newBuilder()
            if (req.header("User-Agent").isNullOrBlank() && Http.customUserAgent.isNotBlank()) {
                b.header("User-Agent", Http.customUserAgent)
            }
            if (req.header("Referer").isNullOrBlank() && Http.customReferer.isNotBlank()) {
                b.header("Referer", Http.customReferer)
            }
            if (req.header("Cookie").isNullOrBlank() && Http.customCookie.isNotBlank()) {
                b.header("Cookie", Http.customCookie)
            }
            chain.proceed(b.build())
        }.build()
        OkHttpDataSource.Factory(client)
    }

    /**
     * Playback reads through the same [SimpleCache] the downloader writes to, so a completed
     * download plays offline automatically (and streaming still works on a cache miss).
     */
    private val dataSourceFactory: DataSource.Factory by lazy {
        val upstream = DefaultDataSource.Factory(this, httpFactory)
        runCatching {
            androidx.media3.datasource.cache.CacheDataSource.Factory()
                .setCache(DownloadManagerProvider.cache(this))
                .setUpstreamDataSourceFactory(upstream)
                .setFlags(androidx.media3.datasource.cache.CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
        }.getOrDefault(upstream)
    }

    private fun buildMediaItem(): Media3Item {
        val builder = Media3Item.Builder()
            .setUri(source.playableUrl)
            .setMediaId(runCatching { item.key }.getOrDefault("unknown"))
        // Each subtitle config is built defensively: one poisoned entry (Gson null URL)
        // must not take down the whole media item.
        val configs = subs.mapIndexedNotNull { idx, s ->
            runCatching {
                Media3Item.SubtitleConfiguration.Builder(Uri.parse(s.url))
                .setMimeType(mimeFor(s.url))
                .setLanguage(s.lang)
                .setLabel(s.lang.uppercase())
                // Only the first track (the language the user picked in the detail sheet) is
                // pre-selected; the rest stay available but inactive.
                .setSelectionFlags(if (idx == 0) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
            }.getOrNull()
        }
        if (configs.isNotEmpty()) builder.setSubtitleConfigurations(configs)
        return builder.build()
    }

    /** Load a new source into the existing player (used by reload and episode switching). */
    private fun applySource(newSource: StreamSource, newSubs: List<SubtitleTrack>) {
        source = newSource
        subs = sanitizeSubtitleTracks(newSubs)
        failedUrls.clear()
        autoTriggered = false
        scrobbled = false
        countdownState.value = null
        val exo = player ?: return
        httpFactory.setDefaultRequestProperties(safeStreamHeaders(newSource))
        runCatching {
            exo.setMediaItem(buildMediaItem())
            exo.prepare()
        }.onFailure { e ->
            Log.e(PLAYER_TAG, "episode-switch load failed", e)
            Toast.makeText(this, "This stream can't be played.", Toast.LENGTH_SHORT).show()
            return
        }
        exo.playWhenReady = true
    }

    /**
     * Try the next backup mirror for this stream. Returns false when there are no alternatives left.
     * Candidate order is the primary URL followed by [StreamSource.alternates].
     *
     * Runs inside the ExoPlayer error callback, so the whole body is contained: any throw
     * becomes a `false` (which shows the playback-failed toast) instead of a crash. In
     * particular `copy()` on a Gson-hole instance re-runs the generated null-checks, so a
     * holey source is shed for a clean one carrying just the next URL.
     */
    private fun tryFailover(): Boolean {
        return runCatching {
            val current = source.playableUrl
            val candidates = (listOfNotNull(current) + safeStreamAlternates(source))
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
            val next = candidates.firstOrNull { it !in failedUrls && it != current } ?: return false
            current?.let { failedUrls += it }
            source = runCatching { source.copy(url = next) }
                .getOrElse { StreamSource(url = next) }
            httpFactory.setDefaultRequestProperties(safeStreamHeaders(source))
            Toast.makeText(this, "Switching to backup source\u2026", Toast.LENGTH_SHORT).show()
            val resumeAt = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
            player?.let {
                it.setMediaItem(buildMediaItem())
                it.prepare()
                it.seekTo(resumeAt)
                it.playWhenReady = true
            }
            true
        }.getOrDefault(false)
    }

    private fun reload() {
        autoTriggered = false
        player?.let {
            runCatching {
                it.setMediaItem(buildMediaItem())
                it.prepare()
            }.onFailure { e ->
                Log.e(PLAYER_TAG, "reload failed", e)
                Toast.makeText(this, "This stream can't be played.", Toast.LENGTH_SHORT).show()
                return
            }
            it.playWhenReady = true
        }
    }

    /** Resolve streams for an episode and switch playback to it. */
    private fun playEpisode(target: Int) {
        if (target !in episodes.indices) return
        cancelCountdown()
        prefetchedIndex = -1
        recordProgress()
        val ep = episodes[target]
        lifecycleScope.launch {
            val container = (application as MoovioApp).container
            val streams = runCatching { container.streamRepository.streamsFor(item, ep.id) }
                .getOrDefault(emptyList())
            val playable = streams.firstOrNull { isPlayableStreamSource(it) } ?: streams.firstOrNull()
            if (!isPlayableStreamSource(playable)) {
                Toast.makeText(this@PlayerActivity, "No playable stream for the next episode", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val episodeSource = requireNotNull(playable)
            val foundSubs = if (subtitlesEnabled) {
                runCatching { container.streamRepository.subtitlesFor(item, ep.id) }.getOrDefault(emptyList())
            } else emptyList()

            index = target
            indexState.value = target
            titleState.value = episodeTitle(ep)
            subtitleState.value = buildString {
                append("S${ep.season ?: 1} E${ep.episode ?: (target + 1)}")
                episodeSource.name?.takeIf { it.isNotBlank() }?.let { append(" \u00b7 $it") }
            }
            applySource(episodeSource, foundSubs)
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        countdownState.value = null
    }

    /**
     * Push one watch event to every enabled tracker (Phase 30 push direction — Trakt history,
     * AniList episode progress/status, MAL status). Silent and best-effort: provider failures
     * are already contained inside `ScrobbleManager`, and the outer `runCatching` guards the
     * launch itself so scrobbling can never disturb playback.
     */
    private fun fireScrobble(progress: Double) {
        val ep = episodes.getOrNull(index)
        val event = ScrobbleEvent(
            item = item,
            progress = progress,
            episode = ep?.episode,
            season = ep?.season,
        )
        val app = application as MoovioApp
        app.appScope.launch { runCatching { app.container.scrobbleManager.syncAll(event) } }
    }

    /**
     * Arm, change or disarm the sleep timer (Phase 29).
     *
     * Timed options launch a countdown job; end-of-episode cancels any autoplay countdown (the
     * current episode must be allowed to finish) and fires from the progress watcher instead.
     */
    private fun setSleepTimer(option: SleepTimerOption) {
        sleepJob?.cancel()
        sleepJob = null
        when {
            option == SleepTimerOption.OFF -> {
                sleepOptionState.value = SleepTimerOption.OFF
                sleepDeadlineState.value = null
            }
            option == SleepTimerOption.END_OF_EPISODE -> {
                cancelCountdown()
                sleepOptionState.value = option
                sleepDeadlineState.value = null
                Toast.makeText(this, "Sleep timer: end of episode", Toast.LENGTH_SHORT).show()
            }
            else -> {
                val ms = option.delayMs() ?: return
                sleepOptionState.value = option
                sleepDeadlineState.value = SystemClock.elapsedRealtime() + ms
                sleepJob = lifecycleScope.launch {
                    delay(ms)
                    fireSleepTimer()
                }
                Toast.makeText(this, "Sleep timer: ${option.label()}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Sleep timer expiry: snapshot progress, pause playback and disarm.
     *
     * Deliberately pause-only: the controls hold a non-null player reference, so releasing the
     * ExoPlayer while the screen is open would crash subsequent seeks — the normal activity
     * teardown (`onDestroy`) still owns the release.
     */
    private fun fireSleepTimer() {
        sleepJob?.cancel()
        sleepJob = null
        sleepOptionState.value = SleepTimerOption.OFF
        sleepDeadlineState.value = null
        recordProgress()
        player?.pause()
        Toast.makeText(this, "Sleep timer ended", Toast.LENGTH_SHORT).show()
    }

    /**
     * Phase 36 pre-fetch: background-resolve the next episode's streams while the
     * current one is still playing. The result is deliberately discarded — the
     * resolve warms `StreamRepository`'s short-lived ranked cache (regular
     * Movie/Series/Anime lookups), so the autoplay countdown finds warm links.
     * Best-effort and silent: failures just mean the countdown resolves on demand.
     */
    private fun prefetchNextEpisode() {
        val next = episodes.getOrNull(index + 1) ?: return
        lifecycleScope.launch {
            runCatching {
                (application as MoovioApp).container.streamRepository.streamsFor(item, next.id)
            }
        }
    }

    /** Start the autoplay-next countdown (from [AUTOPLAY_COUNTDOWN_SECONDS]) when a next episode exists. */
    private fun maybeAutoplay() {
        if (!autoplay) return
        if (index < 0 || index >= episodes.size - 1) return
        if (countdownJob?.isActive == true) return
        autoDownloadAhead()
        countdownJob = lifecycleScope.launch {
            for (second in AUTOPLAY_COUNTDOWN_SECONDS downTo 1) {
                countdownState.value = second
                delay(1000)
            }
            countdownState.value = null
            playEpisode(index + 1)
        }
    }

    /**
     * Pre-fetch the episode *after* the one about to autoplay, so the offline queue stays ahead.
     * Respects the Wi-Fi-only preference.
     */
    private fun autoDownloadAhead() {
        val settings = (application as MoovioApp).container.settings
        if (!settings.autoDownloadNext.value) return
        if (settings.autoDownloadWifiOnly.value && !onWifi()) return
        val target = index + 2
        if (target !in episodes.indices) return
        val ep = episodes[target]
        lifecycleScope.launch {
            val container = (application as MoovioApp).container
            val streams = runCatching { container.streamRepository.streamsFor(item, ep.id) }
                .getOrDefault(emptyList())
            val source = streams.firstOrNull { isPlayableStreamSource(it) } ?: return@launch
            val url = source.playableUrl
            if (url.isNullOrBlank()) return@launch
            val kind = if (url.contains(".m3u8", ignoreCase = true)) VideoKind.HLS else VideoKind.PROGRESSIVE
            val label = buildString {
                append(item.title)
                if (ep.season != null && ep.episode != null) append(" \u00b7 S${ep.season}E${ep.episode}")
            }
            container.videoDownloadManager.enqueue(
                item, url, label, ep.title, kind,
                headers = safeStreamHeaders(source),
                alternates = safeStreamAlternates(source),
            )
        }
    }

    /** True when the active network is Wi-Fi or Ethernet (used for the auto-download gate). */
    private fun onWifi(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager ?: return true
        val caps = cm.activeNetwork?.let { cm.getNetworkCapabilities(it) } ?: return false
        return caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    /** Poll playback so autoplay fires at 95% as well as on natural end (and save progress). */
    private fun startProgressWatcher() {
        progressJob?.cancel()
        progressJob = lifecycleScope.launch {
            var ticks = 0
            while (isActive) {
                delay(1000)
                val p = player ?: continue
                val dur = p.duration
                // Fire the next-episode overlay when ~15 s remain (was a 95%-of-duration check,
                // which for a 45-min episode popped the overlay a full two minutes early).
                val remaining = dur - p.currentPosition
                if (dur > 0 && remaining in 0..AUTOPLAY_COUNTDOWN_SECONDS * 1000L && !autoTriggered) {
                    autoTriggered = true
                    // A sleep-at-episode-end timer suppresses the autoplay countdown: the episode
                    // is allowed to finish, and the timer fires below instead of advancing.
                    if (sleepOptionState.value != SleepTimerOption.END_OF_EPISODE) maybeAutoplay()
                }
                // Sleep-at-episode-end: the playhead reached the natural end — pause there.
                if (sleepOptionState.value == SleepTimerOption.END_OF_EPISODE && dur > 0 && remaining <= 0) {
                    fireSleepTimer()
                }
                // Phase 36 pre-fetch: inside the final two minutes, background-resolve the
                // next episode's streams once (warming StreamRepository's short cache), so
                // the 15 s autoplay countdown below transitions with zero resolve delay.
                if (shouldPrefetchNextEpisode(remaining, dur, index + 1 in episodes.indices, prefetchedIndex == index)) {
                    prefetchedIndex = index
                    prefetchNextEpisode()
                }
                // Persist roughly every 5 s while playing, so closing the app never loses more
                // than a few seconds and Continue Watching stays current.
                if (++ticks % 5 == 0 && p.isPlaying) recordProgress()
                // Phase 30 scrobble push: completion (once per title) plus a progress heartbeat
                // every ~60 s, carrying episode/season numbers for anime trackers.
                if (p.isPlaying && dur > 0) {
                    val progress = p.currentPosition.coerceAtLeast(0).toDouble() / dur
                    if (!scrobbled && progress >= 0.9) {
                        scrobbled = true
                        fireScrobble(progress)
                    } else if (ticks % 60 == 0) {
                        fireScrobble(progress)
                    }
                }
            }
        }
    }

    /**
     * Snapshot the current playback state into Continue Watching. Runs on the app scope so it is
     * not cancelled when the Activity is torn down (back press, PiP close, process navigation).
     */
    private fun recordProgress() {
        // Never persist before the saved position has been resolved, or the initial position 0
        // would clobber the resume point (see [resumeResolved]).
        if (!resumeResolved) return
        val p = player ?: return
        val position = p.currentPosition.coerceAtLeast(0)
        val duration = p.duration.takeIf { it > 0 } ?: 0
        if (position <= 0 && duration <= 0) return
        // Phase 15: reclaim storage once a watched title passes 90% (once per session).
        if (autoDeleteWatched && !autoDeleted && duration > 0 && position >= (duration * 0.9f)) {
            autoDeleted = true
            (application as MoovioApp).container.videoDownloadManager.deleteDownloadsForItem(item.key)
        }
        val ep = episodes.getOrNull(index)
        val entry = WatchEntry(
            item = item,
            videoId = ep?.id,
            videoTitle = ep?.title,
            positionMs = position,
            durationMs = duration,
        )
        (application as MoovioApp).appScope.launch {
            runCatching { (application as MoovioApp).container.libraryStore.recordWatch(entry) }
        }
    }

    /** Seek to the last saved position for this title/episode, when there is one worth resuming. */
    private suspend fun resumeLastPosition() {
        val videoId = episodes.getOrNull(index)?.id
        val entry = runCatching {
            (application as MoovioApp).container.libraryStore.history.first()
                .firstOrNull { it.item.key == item.key && it.videoId == videoId }
        }.getOrNull() ?: return
        val dur = entry.durationMs
        val resume = entry.positionMs
        val finished = dur > 0 && resume >= (dur * 0.95f).toLong()
        if (resume > 0 && !finished) runCatching { player?.seekTo(resume) }
    }

    /** Edge-drag brightness delta → the new normalized level (what the gesture HUD shows). */
    private fun adjustBrightness(delta: Float): Float {
        if (delta != 0f) {
            brightnessValue = (brightnessValue + delta).coerceIn(0.02f, 1f)
            window.attributes = window.attributes.apply { screenBrightness = brightnessValue }
            lifecycleScope.launch { (application as MoovioApp).container.settings.setPlayerBrightness(brightnessValue) }
        }
        return brightnessValue
    }

    /**
     * Edge-drag volume delta. Fractional movement is accumulated and committed one whole step
     * at a time, so a slow drag nudges the volume instead of jumping a step per frame.
     * Returns the current level (0..1) for the gesture HUD.
     */
    private fun adjustVolume(delta: Float): Float {
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        if (max <= 0) return 0f
        if (delta != 0f) {
            volumeAccum += delta
            if (abs(volumeAccum) >= VOLUME_STEP) {
                // Convert the accumulated drag into whole VOLUME_STEP units. Dividing first is
                // essential: `volumeAccum.toInt()` truncates toward zero, so a step was only
                // committed once the accumulator reached 1.0 — every drag shorter than that
                // (and smaller than one full accumulator) changed the volume by nothing at all,
                // and `endGesture()` then discarded it.
                val steps = (volumeAccum / VOLUME_STEP).toInt()
                volumeAccum -= steps * VOLUME_STEP
                val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                runCatching {
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (cur + steps).coerceIn(0, max), 0)
                }
            }
        }
        return audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / max.toFloat()
    }

    /**
     * Accumulate a horizontal drag into a pending seek and flush at most every [SEEK_FLUSH_MS].
     *
     * Calling [Player.seekTo] on every drag event (roughly once per frame) tears the decoder
     * down repeatedly and makes the scrub stutter; batching keeps playback smooth and
     * [endGesture] lands the exact final position on release.
     */
    private fun gestureSeek(deltaPx: Float) {
        pendingSeekMs += (deltaPx * SEEK_PX_TO_MS).toLong()
        if (SystemClock.elapsedRealtime() - lastSeekFlushMs >= SEEK_FLUSH_MS) flushSeek()
    }

    private fun flushSeek() {
        lastSeekFlushMs = SystemClock.elapsedRealtime()
        val p = player ?: return
        val delta = pendingSeekMs
        pendingSeekMs = 0
        if (delta == 0L) return
        val dur = p.duration
        val target = (p.currentPosition + delta).coerceIn(0, if (dur > 0) dur else Long.MAX_VALUE)
        p.seekTo(target)
    }

    /** Drag released/cancelled: land the final seek and reset the fractional volume accumulator. */
    private fun endGesture() {
        flushSeek()
        volumeAccum = 0f
    }

    /** Normalized playhead position for the seek gesture HUD. */
    private fun seekProgress(): Float {
        val p = player ?: return 0f
        val dur = p.duration
        return if (dur > 0) (p.currentPosition.toFloat() / dur).coerceIn(0f, 1f) else 0f
    }

    private fun mimeFor(url: String): String = when {
        url.endsWith(".vtt", true) -> androidx.media3.common.MimeTypes.TEXT_VTT
        url.endsWith(".ass", true) || url.endsWith(".ssa", true) -> androidx.media3.common.MimeTypes.TEXT_SSA
        else -> androidx.media3.common.MimeTypes.APPLICATION_SUBRIP
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        // Only worth entering PiP while something is actually playing.
        if (player?.isPlaying != true) return
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .apply {
                // API 31+: let the system auto-enter PiP on the swipe-to-home gesture.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) setAutoEnterEnabled(true)
            }
            .build()
        runCatching { enterPictureInPictureMode(params) }
    }

    private fun toggleOrientation() {
        requestedOrientation = if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    private fun openExternal() {
        val url = source.playableUrl ?: return
        val i = Intent(Intent.ACTION_VIEW).apply { setDataAndType(Uri.parse(url), "video/*") }
        runCatching { startActivity(i) }
    }

    /** Pre-Phase-30 fallback: open the system cast/wireless-display settings when available. */
    private fun openCastSettings() {
        val intents = buildList {
            add(Intent(Settings.ACTION_CAST_SETTINGS))
            add(Intent("android.settings.WIFI_DISPLAY_SETTINGS"))
        }
        for (i in intents) {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { startActivity(i) }.isSuccess) return
        }
        Toast.makeText(this, "No cast/DLNA target available on this device", Toast.LENGTH_SHORT).show()
    }

    /** Launch a specific external player by package, falling back to the generic chooser. */
    private fun openInPackage(pkg: String) {
        val url = source.playableUrl ?: return
        val i = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(Uri.parse(url), "video/*")
            setPackage(pkg)
        }
        val ok = runCatching { startActivity(i) }.isSuccess
        if (!ok) {
            Toast.makeText(this, "Player not installed \u2014 opening chooser", Toast.LENGTH_SHORT).show()
            openExternal()
        }
    }

    // ---- Phase 30 Chromecast -------------------------------------------------
    // The player top bar hosts a real MediaRouteButton (route discovery + device picker); when a
    // Cast session connects, the current stream URL is handed to the Default Media Receiver and
    // local playback pauses so audio never doubles. Everything is guarded: devices without Play
    // services simply get a route button with no destinations.

    private var castLoadedUrl: String? = null

    private val castListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession) {}
        override fun onSessionStarted(session: CastSession, sessionId: String) = onCastConnected()
        override fun onSessionStartFailed(session: CastSession, error: Int) {}
        override fun onSessionEnding(session: CastSession) {}
        override fun onSessionEnded(session: CastSession, error: Int) { castLoadedUrl = null }
        override fun onSessionResuming(session: CastSession, sessionId: String) {}
        override fun onSessionResumed(session: CastSession, wasSuspended: Boolean) = onCastConnected()
        override fun onSessionResumeFailed(session: CastSession, error: Int) {}
        override fun onSessionSuspended(session: CastSession, reason: Int) {}
    }

    private fun castSessionManager() =
        runCatching { CastContext.getSharedInstance(this)?.sessionManager }.getOrNull()

    /** Hand the current stream to the connected receiver (once per URL) and pause locally. */
    private fun onCastConnected() {
        val url = source.playableUrl
        if (url.isNullOrBlank()) return
        if (castLoadedUrl == url) return
        val remote = castSessionManager()?.currentCastSession?.remoteMediaClient ?: return
        val metadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE).apply {
            putString(MediaMetadata.KEY_TITLE, runCatching { item.title }.getOrDefault(""))
        }
        val media = MediaInfo.Builder(url)
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(if (url.contains(".m3u8", ignoreCase = true)) "application/x-mpegurl" else "video/mp4")
            .setMetadata(metadata)
            .build()
        // Single-arg load autoplays on the receiver (the two-arg overload is deprecated).
        runCatching { remote.load(media) }
        castLoadedUrl = url
        player?.pause()
        Toast.makeText(this, "Casting to TV", Toast.LENGTH_SHORT).show()
    }

    private fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2f)
        player?.setPlaybackSpeed(playbackSpeed)
    }

    /**
     * Apply an in-player subtitle style edit (Phase 29): update the live state (the overlay and
     * the PlayerView re-apply pick it up) and persist it to the shared Settings so the Subtitles
     * group shows the same values. Each field is clamped to its Settings range.
     */
    private fun updateSubtitleStyle(style: SubtitleStyle) {
        val clamped = style.copy(
            scale = style.scale.coerceIn(0.6f, 2f),
            bgOpacity = style.bgOpacity.coerceIn(0f, 1f),
            bottomOffset = style.bottomOffset.coerceIn(0f, 0.3f),
        )
        subtitleStyleState.value = clamped
        val settings = (application as MoovioApp).container.settings
        lifecycleScope.launch {
            settings.setSubtitleScale(clamped.scale)
            settings.setSubtitleColor(clamped.colorKey)
            settings.setSubtitleBg(clamped.bgOpacity)
            settings.setSubtitleOffset(clamped.bottomOffset)
            settings.setSubtitleOutline(clamped.outlineKey)
        }
    }

    private fun setAudioBoost(enabled: Boolean) {
        audioBoost = enabled
        applyAudioEffects(audioSessionId)
    }

    private fun setAudioNormalize(enabled: Boolean) {
        audioNormalize = enabled
        applyAudioEffects(audioSessionId)
    }

    /**
     * Attach a [LoudnessEnhancer] to the player's audio session.
     *
     * "Boost" lifts quiet dialogue, "Normalize" applies a gentle lift so loud/quiet scenes even
     * out; both are best-effort and silently no-op on devices that reject the effect.
     */
    private fun applyAudioEffects(sessionId: Int) {
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        if (sessionId == 0) return
        val gainMb = when {
            audioBoost -> 1000
            audioNormalize -> 400
            else -> 0
        }
        if (gainMb == 0) return
        runCatching {
            val enh = LoudnessEnhancer(sessionId)
            enh.setTargetGain(gainMb)
            enh.enabled = true
            loudnessEnhancer = enh
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        enterPip()
    }

    override fun onResume() {
        super.onResume()
        // Phase 30: listen for Cast sessions while the player is in front.
        castSessionManager()?.addSessionManagerListener(castListener, CastSession::class.java)
    }

    override fun onPictureInPictureModeChanged(isInPip: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPip, newConfig)
        hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onPause() {
        super.onPause()
        // Phase 30: stop listening for Cast sessions when the player leaves the front.
        castSessionManager()?.removeSessionManagerListener(castListener, CastSession::class.java)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) player?.pause()
        // Save the exact spot before we might be torn down.
        recordProgress()
    }

    override fun onStop() {
        super.onStop()
        recordProgress()
    }

    override fun onDestroy() {
        countdownJob?.cancel()
        sleepJob?.cancel()
        progressJob?.cancel()
        recordProgress()
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        player?.release()
        player = null
        // Detach the CloudStream toast host only when this Activity is really going away.
        if (isFinishing) {
            runCatching { CommonActivity.setActivityInstance(null) }
            runCatching { window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
        }
        super.onDestroy()
    }

    /** Max video dimensions for a preferred-quality label, or null for \"Auto\". */
    private fun qualityCap(quality: String): Pair<Int, Int>? = when (quality) {
        "4K" -> 3840 to 2160
        "1080p" -> 1920 to 1080
        "720p" -> 1280 to 720
        "480p" -> 854 to 480
        else -> null
    }

    companion object {
        private const val EXT_ITEM = "p_item"
        private const val EXT_SOURCE = "p_source"
        private const val EXT_SUBS = "p_subs"
        private const val EXT_EPISODES = "p_episodes"
        private const val EXT_INDEX = "p_index"

        /** Horizontal drag pixels → milliseconds of seek (matches the original gesture feel). */
        private const val SEEK_PX_TO_MS = 300f

        /** Minimum gap between seeks while scrubbing, so playback never stutters. */
        private const val SEEK_FLUSH_MS = 300L

        /** How much vertical drag equals one full volume sweep (fraction of the track). */
        private const val VOLUME_STEP = 0.15f

        /** Seconds of countdown shown in the bottom-right before auto-playing the next episode. */
        private const val AUTOPLAY_COUNTDOWN_SECONDS = 15

        fun intent(
            context: Context,
            item: MediaItem,
            source: StreamSource,
            subtitles: List<SubtitleTrack>,
            episodes: List<Video> = emptyList(),
            index: Int = -1,
        ): Intent {
            val i = Intent(context, PlayerActivity::class.java)
            val gson = com.google.gson.Gson()
            i.putExtra(EXT_ITEM, gson.toJson(item))
            i.putExtra(EXT_SOURCE, gson.toJson(source))
            i.putExtra(EXT_SUBS, gson.toJson(subtitles))
            i.putExtra(EXT_EPISODES, gson.toJson(episodes))
            i.putExtra(EXT_INDEX, index)
            return i
        }

        private fun readItem(i: Intent): MediaItem? = runCatching {
            com.google.gson.Gson().fromJson(i.getStringExtra(EXT_ITEM), MediaItem::class.java)
        }.getOrNull()

        private fun readSource(i: Intent): StreamSource? = runCatching {
            com.google.gson.Gson().fromJson(i.getStringExtra(EXT_SOURCE), StreamSource::class.java)
        }.getOrNull()

        private fun readSubs(i: Intent): List<SubtitleTrack> = runCatching {
            val t = i.getStringExtra(EXT_SUBS) ?: return emptyList()
            com.google.gson.Gson().fromJson(t, Array<SubtitleTrack>::class.java).toList()
        }.getOrDefault(emptyList())

        private fun readEpisodes(i: Intent): List<Video> = runCatching {
            val t = i.getStringExtra(EXT_EPISODES) ?: return emptyList()
            com.google.gson.Gson().fromJson(t, Array<Video>::class.java).toList()
        }.getOrDefault(emptyList())

        private fun readIndex(i: Intent): Int = i.getIntExtra(EXT_INDEX, -1)
    }
}

/** Log tag for player launch/cast diagnostics (file-level: top-level composables
 * cannot see private companion members). */
private const val PLAYER_TAG = "PlayerActivity"

/**
 * Drag distance in px before a swipe locks to its dominant axis (brightness/volume vs seek),
 * so finger jitter on touch-down never flashes the wrong HUD. File-level (not companion)
 * because the PlayerScreen gesture handler is a top-level composable.
 */
private const val GESTURE_SLOP_PX = 24f

/** Cap on buffered caption events kept for the subtitle-sync overlay (~a few minutes of cues). */
private const val CUE_HISTORY_LIMIT = 200

/** Phase 34: press-and-hold duration before the player boosts to 2X speed. */
private const val HOLD_TO_BOOST_MS = 400L

private enum class GestureMode { BRIGHTNESS, VOLUME, SEEK }

/**
 * Subtitle look, applied to the Media3 [PlayerView]'s `SubtitleView`. Values come from the
 * Settings > Subtitles group; `colorKey` maps to a concrete colour in [subtitleColor].
 */
private data class SubtitleStyle(
    val scale: Float = 1f,
    val colorKey: String = "white",
    val bgOpacity: Float = 0.4f,
    val bottomOffset: Float = 0.05f,
    /** Caption edge/outline color key (same palette as [colorKey] plus `"black"`). */
    val outlineKey: String = "black",
)

private fun subtitleColor(key: String): Int = when (key) {
    "yellow" -> 0xFFFFEB3B.toInt()
    "cyan" -> 0xFF35D6E0.toInt()
    "green" -> 0xFF7CFF7C.toInt()
    "black" -> 0xFF000000.toInt()
    else -> 0xFFFFFFFF.toInt()
}

/** Video scaling modes cycled by the pinch-to-zoom gesture. */
private enum class ScaleMode(val label: String, val resizeMode: Int) {
    FIT("Fit", AspectRatioFrameLayout.RESIZE_MODE_FIT),
    STRETCH("Stretch", AspectRatioFrameLayout.RESIZE_MODE_FILL),
    FILL("Fill", AspectRatioFrameLayout.RESIZE_MODE_ZOOM),
    WIDE_16_9("16:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_WIDTH),
    WIDE_21_9("21:9", AspectRatioFrameLayout.RESIZE_MODE_FIXED_HEIGHT),
    ;

    fun next(): ScaleMode = entries[(ordinal + 1) % entries.size]
}

/** Apply the user's subtitle preferences onto a freshly-created [PlayerView]. */
private fun applySubtitleStyle(view: PlayerView, style: SubtitleStyle) {
    val sv = view.subtitleView ?: return
    sv.setApplyEmbeddedStyles(false)
    val bg = (style.bgOpacity.coerceIn(0f, 1f) * 255).toInt().shl(24)
    // Media3's public CaptionStyleCompat constructor takes an optional typeface as a 6th arg.
    val captionStyle = CaptionStyleCompat(
        subtitleColor(style.colorKey),
        bg,
        android.graphics.Color.TRANSPARENT,
        CaptionStyleCompat.EDGE_TYPE_OUTLINE,
        subtitleColor(style.outlineKey),
        null,
    )
    sv.setStyle(captionStyle)
    sv.setFractionalTextSize(androidx.media3.ui.SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * style.scale)
    sv.setBottomPaddingFraction(style.bottomOffset.coerceIn(0f, 0.3f))
}

@UnstableApi
@Composable
private fun PlayerScreen(
    player: ExoPlayer,
    title: String,
    subtitle: String?,
    countdown: Int?,
    episodes: List<Video>,
    currentIndex: Int,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onRotate: () -> Unit,
    onExternal: () -> Unit,
    onReload: () -> Unit,
    onCancelCountdown: () -> Unit,
    onNext: () -> Unit,
    onSelectEpisode: (Int) -> Unit,
    qualityLabel: String?,
    onGestures: (GestureMode, Float) -> Float,
    onGestureEnd: () -> Unit,
    subtitleStyle: SubtitleStyle,
    subtitleSyncMs: Int,
    onSubtitleSync: (Int) -> Unit,
    onSubtitleStyle: (SubtitleStyle) -> Unit,
    sleepOption: SleepTimerOption,
    sleepDeadlineMs: Long?,
    onSleepTimer: (SleepTimerOption) -> Unit,
    initialSpeed: Float,
    audioBoostOn: Boolean,
    audioNormalizeOn: Boolean,
    onSpeedChange: (Float) -> Unit,
    onAudioBoost: (Boolean) -> Unit,
    onAudioNormalize: (Boolean) -> Unit,
    onOpenVlc: () -> Unit,
    onOpenMpv: () -> Unit,
    onOpenOther: () -> Unit,
    onCastSettings: () -> Unit,
) {
    var showTracks by remember { mutableStateOf(false) }
    var showEpisodes by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf<Tracks?>(null) }
    // Phase 11 player state.
    var showSpeedAudio by remember { mutableStateOf(false) }
    var showExternal by remember { mutableStateOf(false) }
    var showSleep by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(initialSpeed) }
    var boost by remember { mutableStateOf(audioBoostOn) }
    var normalize by remember { mutableStateOf(audioNormalizeOn) }
    val doubleTapMs by LocalContainer.current.settings.doubleTapMs.collectAsStateWithLifecycle()
    // ---- Phase 37: Anime4K enhancement (persisted preset + GPU tier) ----
    // Applied to the live player on every change, so the sheet switches passes mid-playback.
    val anime4KPresetKey by LocalContainer.current.settings.anime4KPreset.collectAsStateWithLifecycle()
    val anime4KGpuTierKey by LocalContainer.current.settings.anime4KGpuTier.collectAsStateWithLifecycle()
    val anime4KConfig = Anime4KConfig(parseAnime4KPreset(anime4KPresetKey), parseAnime4KGpuTier(anime4KGpuTierKey))
    // Captured once (composition context) so the sheet callbacks can persist below.
    val anime4KSettings = LocalContainer.current.settings
    LaunchedEffect(anime4KPresetKey, anime4KGpuTierKey) {
        Anime4KEffectApplier.apply(player, anime4KConfig)
    }
    var locked by remember { mutableStateOf(false) }
    var scaleMode by remember { mutableStateOf(ScaleMode.FIT) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    // Ripple shown on a double-tap seek: direction (-1 back / +1 forward) and a pulse counter.
    var rippleDir by remember { mutableStateOf(0) }
    var ripplePulse by remember { mutableIntStateOf(0) }

    // ---- Phase 34: hold-for-2X --------------------------------------------
    // Temporary speed boost while the finger is held down. `speed` stays the user's saved
    // default; only the ExoPlayer rate changes, restored on release.
    var isBoosting by remember { mutableStateOf(false) }
    var boostRestoreSpeed by remember { mutableStateOf(initialSpeed) }

    // ---- Phase 15: haptics, stats overlay & subtitle-sync buffer -----------
    val haptic = LocalHapticFeedback.current
    var showStats by remember { mutableStateOf(false) }
    var droppedFrames by remember { mutableLongStateOf(0L) }
    var bandwidthBps by remember { mutableLongStateOf(0L) }
    var bufferMs by remember { mutableLongStateOf(0L) }
    var bufferedPct by remember { mutableIntStateOf(0) }
    // Captioned cue timeline (presentation time -> cue set). The subtitle-sync overlay replays an
    // earlier or later cue set than the native SubtitleView would show, because Media3 1.3 has no
    // built-in subtitle offset. Bounded so it can't grow without limit.
    val cueHistory = remember { mutableStateListOf<Pair<Long, List<Cue>>>() }

    // ---- Timeline state ---------------------------------------------------
    // Nothing in composition read the player's clock before, so the timestamps and slider only
    // changed when something else happened to recompose. A periodic ticker now drives them.
    var position by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    // While the thumb is being dragged the ticker holds, so the slider never fights the finger.
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableStateOf(0f) }

    // ---- Gesture HUD ------------------------------------------------------
    var hudMode by remember { mutableStateOf<GestureMode?>(null) }
    var hudLevel by remember { mutableStateOf(0f) }
    var hudVisible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var hudHideJob by remember { mutableStateOf<Job?>(null) }

    // ---- Controls auto-hide: Phase 35 state-driven engine ----------------
    // Strict machine (VISIBLE / HIDING / HIDDEN) owned by PlayerControls.kt.
    // A single autoHideJob is the only timer: resetAutoHideTimer() cancels any
    // pending job and starts a fresh 3,000 ms countdown; a tap when visible
    // hides immediately and cancels the timer instead.
    var controlsState by remember { mutableStateOf(PlayerControlsState.VISIBLE) }
    val controlsVisible = controlsState == PlayerControlsState.VISIBLE
    var isBuffering by remember { mutableStateOf(player.playbackState == Player.STATE_BUFFERING) }
    var hasPlaybackError by remember { mutableStateOf(false) }
    var autoHideJob by remember { mutableStateOf<Job?>(null) }
    fun cancelAutoHideTimer() {
        autoHideJob?.cancel()
        autoHideJob = null
    }
    fun resetAutoHideTimer() {
        if (locked) return
        controlsState = PlayerControlsState.VISIBLE
        cancelAutoHideTimer()
        autoHideJob = scope.launch {
            delay(PLAYER_CONTROLS_HIDE_MS)
            controlsState = PlayerControlsState.HIDDEN
        }
    }
    fun hideControlsNow() {
        cancelAutoHideTimer()
        if (!locked) controlsState = PlayerControlsState.HIDDEN
    }
    fun onSingleTap() {
        when (playerTapAction(controlsState, locked)) {
            PlayerTapAction.HIDE_NOW -> hideControlsNow()
            PlayerTapAction.REVEAL_AND_RESTART_TIMER -> resetAutoHideTimer()
            PlayerTapAction.IGNORED_LOCKED -> Unit
        }
    }
    // Back-compat alias: every interaction funnels into the single timer.
    fun pokeControls() = resetAutoHideTimer()

    fun showHud(mode: GestureMode, level: Float) {
        hudHideJob?.cancel()
        hudMode = mode
        hudLevel = level.coerceIn(0f, 1f)
        hudVisible = true
    }

    /** Keep the HUD up for ~1.4 s after the finger lifts, then fade it out. */
    fun scheduleHudHide() {
        hudHideJob?.cancel()
        hudHideJob = scope.launch { delay(1400); hudVisible = false }
    }

    // Periodic ticker: refreshes the playhead, duration and play/pause state every 250 ms.
    LaunchedEffect(player) {
        while (isActive) {
            if (!scrubbing) {
                position = player.currentPosition.coerceAtLeast(0)
                duration = player.duration.coerceAtLeast(0)
            }
            isPlaying = player.isPlaying
            isBuffering = player.playbackState == Player.STATE_BUFFERING
            // Buffer state for the Stats overlay.
            bufferMs = player.totalBufferedDuration.coerceAtLeast(0)
            bufferedPct = player.bufferedPercentage
            delay(250)
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(t: Tracks) { tracks = t }
            // Instant play/pause feedback instead of waiting for the next tick.
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!scrubbing) {
                    position = player.currentPosition.coerceAtLeast(0)
                    duration = player.duration.coerceAtLeast(0)
                }
            }
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
            }
            // Record the caption timeline so the sync overlay can shift it in time.
            override fun onCues(cueGroup: CueGroup) {
                cueHistory.add(player.currentPosition.coerceAtLeast(0) to cueGroup.cues)
                while (cueHistory.size > CUE_HISTORY_LIMIT) cueHistory.removeAt(0)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    // Stats-for-Nerds counters that only the analytics listener exposes.
    DisposableEffect(player) {
        val analytics = object : AnalyticsListener {
            override fun onDroppedVideoFrames(eventTime: AnalyticsListener.EventTime, dropped: Int, elapsedMs: Long) {
                droppedFrames += dropped
            }

            override fun onBandwidthEstimate(
                eventTime: AnalyticsListener.EventTime,
                totalLoadTimeMs: Int,
                totalBytesLoaded: Long,
                bitrateEstimate: Long,
            ) {
                bandwidthBps = bitrateEstimate
            }
        }
        player.addAnalyticsListener(analytics)
        onDispose { player.removeAnalyticsListener(analytics) }
    }

    val sheetOpen = showTracks || showEpisodes || showSpeedAudio || showExternal || showSleep
    // Phase 35 auto-hide engine: forced states win (locked → hidden; paused /
    // buffering / error / sheet / scrubbing → visible). Otherwise the single
    // autoHideJob owns the 3 s countdown — this effect only (re)arms it when
    // the eligibility changes, while every user interaction funnels through
    // resetAutoHideTimer().
    LaunchedEffect(isPlaying, isBuffering, sheetOpen, locked, scrubbing, hasPlaybackError) {
        val forced = forcedControlsState(locked, isPlaying, isBuffering, sheetOpen, scrubbing, hasPlaybackError)
        if (forced != null) {
            cancelAutoHideTimer()
            controlsState = forced
            return@LaunchedEffect
        }
        if (controlsState == PlayerControlsState.VISIBLE) resetAutoHideTimer()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    // Apply the user's subtitle styling to this view's SubtitleView.
                    applySubtitleStyle(this, subtitleStyle)
                    // When a sync offset is active the native captions are hidden and our own
                    // time-shifted overlay renders instead (Media3 has no subtitle offset API).
                    subtitleView?.visibility = if (subtitleSyncMs == 0) View.VISIBLE else View.GONE
                    playerView = this
                }
            },
            modifier = Modifier
                .fillMaxSize()
                // Pinch-to-zoom cycles the scale modes (Fit → Stretch → Fill → 16:9 → 21:9).
                // Implemented manually so it only claims *two-finger* gestures — a stock
                // detectTransformGestures would consume single-finger pans and starve the
                // brightness/volume/seek drag handler below.
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)
                        var zoomAccum = 1f
                        var event: PointerEvent
                        do {
                            event = awaitPointerEvent()
                            val pressed = event.changes.count { it.pressed }
                            if (pressed >= 2) {
                                zoomAccum *= event.calculateZoom()
                                if (zoomAccum > 1.25f || zoomAccum < 0.8f) {
                                    scaleMode = scaleMode.next()
                                    zoomAccum = 1f
                                }
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        } while (event.changes.any { it.pressed })
                    }
                }
                .pointerInput(locked) {
                    if (locked) return@pointerInput
                    val width = size.width.toFloat()
                    // Phase 29 swipe zones: the left half owns brightness, the right half volume,
                    // and a horizontal swipe anywhere scrubs. The mode locks to the dominant axis
                    // once the drag passes touch slop, so a slightly diagonal swipe never flips
                    // mid-gesture between (say) volume and seek.
                    var startX = 0f
                    var totalX = 0f
                    var totalY = 0f
                    var mode = GestureMode.SEEK
                    var modeLocked = false
                    detectDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
                            totalX = 0f
                            totalY = 0f
                            modeLocked = false
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            if (!modeLocked) {
                                totalX += drag.x
                                totalY += drag.y
                                if (abs(totalX) + abs(totalY) < GESTURE_SLOP_PX) return@detectDragGestures
                                modeLocked = true
                                mode = if (abs(totalX) > abs(totalY)) {
                                    GestureMode.SEEK
                                } else if (startX < width / 2f) {
                                    GestureMode.BRIGHTNESS
                                } else {
                                    GestureMode.VOLUME
                                }
                                // Seed the HUD with the current level (a zero-delta call is a read).
                                showHud(mode, onGestures(mode, 0f))
                                pokeControls()
                                return@detectDragGestures
                            }
                            val level = when (mode) {
                                // Vertical drag; downward decreases.
                                GestureMode.BRIGHTNESS, GestureMode.VOLUME ->
                                    onGestures(mode, -drag.y / 260f)
                                GestureMode.SEEK -> onGestures(GestureMode.SEEK, drag.x)
                            }
                            showHud(mode, level)
                            pokeControls()
                        },
                        onDragEnd = { onGestureEnd(); scheduleHudHide(); pokeControls() },
                        onDragCancel = { onGestureEnd(); scheduleHudHide(); pokeControls() },
                    )
                }
                // Phase 35 unified tap interceptor: single-tap, double-tap seek and
                // hold-for-2X share ONE detectTapGestures so they arbitrate the same
                // down event deterministically. (Two separate detectors raced for
                // the press, which is why taps intermittently failed to toggle.)
                // Single-tap: visible → hide now + cancel timer; hidden → reveal
                // + fresh 3 s countdown (see onSingleTap). Double-tap L/R seeks
                // ∓doubleTapMs with ripple. Hold ≥ HOLD_TO_BOOST_MS boosts to 2X.
                .pointerInput(locked, speed, doubleTapMs) {
                    if (locked) return@pointerInput
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            val forward = offset.x >= size.width / 2f
                            val delta = if (forward) doubleTapMs.toLong() else -doubleTapMs.toLong()
                            val target = (player.currentPosition + delta).coerceAtLeast(0)
                            player.seekTo(target)
                            position = target
                            rippleDir = if (forward) 1 else -1
                            ripplePulse++
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            pokeControls()
                        },
                        onTap = { onSingleTap() },
                        onPress = {
                            // Wait for a quick release; a timeout means the finger is held.
                            val releasedQuickly = withTimeoutOrNull(HOLD_TO_BOOST_MS) {
                                tryAwaitRelease()
                            }
                            if (releasedQuickly == null) {
                                if (player.isPlaying && !isBoosting) {
                                    boostRestoreSpeed = speed.coerceIn(0.5f, 2f)
                                    runCatching { player.setPlaybackSpeed(2f) }
                                    isBoosting = true
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }
                                // Park here until the finger lifts, then restore.
                                runCatching { tryAwaitRelease() }
                                if (isBoosting) {
                                    runCatching { player.setPlaybackSpeed(boostRestoreSpeed.coerceIn(0.5f, 2f)) }
                                    isBoosting = false
                                    pokeControls()
                                }
                            }
                        },
                    )
                },
        )

        // Keep the PlayerView's scale mode in sync with the pinch gesture.
        LaunchedEffect(scaleMode, playerView) {
            playerView?.resizeMode = scaleMode.resizeMode
        }

        // Native captions show only when there is no time-shift offset.
        LaunchedEffect(subtitleSyncMs, playerView) {
            playerView?.subtitleView?.visibility = if (subtitleSyncMs == 0) View.VISIBLE else View.GONE
        }

        // In-player style edits re-apply to the PlayerView live (the time-shifted overlay reads
        // `subtitleStyle` straight from composition, so it needs no effect of its own).
        LaunchedEffect(subtitleStyle, playerView) {
            playerView?.let { applySubtitleStyle(it, subtitleStyle) }
        }

        AnimatedVisibility(
            visible = controlsVisible && !locked,
            enter = fadeIn(tween(200)),
            exit = fadeOut(tween(250)),
        ) {
            Box(
                Modifier.fillMaxSize().background(
                    Brush.verticalGradient(
                        listOf(Color(0xCC000000), Color(0x22000000), Color(0xCC000000)),
                    ),
                ),
            ) {
                // Top bar
                Row(
                    Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        if (!subtitle.isNullOrBlank())
                            Text(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    IconButton(onClick = { showExternal = true }) { Icon(Icons.Filled.OpenInNew, "Open in external player", tint = Color.White) }
                    // Phase 30 Chromecast: the real route picker (device discovery + chooser).
                    // Devices without Play services simply see no cast destinations; if the
                    // button itself can't init, fall back to the system cast-settings
                    // shortcut instead of crashing the player.
                    var castFallback by remember { mutableStateOf(false) }
                    if (castFallback) {
                        IconButton(onClick = onCastSettings) {
                            Icon(Icons.Filled.Cast, "Cast / DLNA", tint = Color.White)
                        }
                    } else {
                        AndroidView(
                            factory = { ctx ->
                                runCatching {
                                    MediaRouteButton(ctx).apply {
                                        routeSelector = MediaRouteSelector.Builder()
                                            .addControlCategory(CastMediaControlIntent.categoryForCast(CAST_RECEIVER_APP_ID))
                                            .build()
                                    }
                                }.getOrElse { e ->
                                    Log.e(PLAYER_TAG, "MediaRouteButton init failed; using settings fallback", e)
                                    scope.launch { castFallback = true }
                                    View(ctx).apply { visibility = View.GONE }
                                }
                            },
                            modifier = Modifier.size(48.dp),
                        )
                    }
                    IconButton(onClick = onPip) { Icon(Icons.Filled.PictureInPictureAlt, "PiP", tint = Color.White) }
                    IconButton(onClick = onRotate) { Icon(Icons.Filled.ScreenRotation, "Rotate", tint = Color.White) }
                    IconButton(onClick = { showTracks = true; pokeControls() }) { Icon(Icons.Filled.Subtitles, "Subtitle and audio tracks", tint = Color.White) }
                    IconButton(onClick = { showSpeedAudio = true; pokeControls() }) { Icon(Icons.Filled.Speed, "Playback speed", tint = Color.White) }
                    // Sleep timer: armed state reads amber so it is visible at a glance over video.
                    IconButton(onClick = { showSleep = true }) {
                        Icon(
                            if (sleepOption == SleepTimerOption.OFF) Icons.Filled.Timer else Icons.Filled.TimerOff,
                            "Sleep timer",
                            tint = if (sleepOption == SleepTimerOption.OFF) Color.White else Color(0xFFFFD54F),
                        )
                    }
                    IconButton(onClick = { locked = true; hideControlsNow() }) { Icon(Icons.Filled.LockOpen, "Lock controls", tint = Color.White) }
                }

                // Center controls
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(28.dp),
                ) {
                    IconButton(onClick = {
                        val target = (player.currentPosition - 10_000).coerceAtLeast(0)
                        player.seekTo(target); position = target
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pokeControls()
                    }) {
                        Icon(Icons.Filled.Replay10, "Back 10", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = { if (player.isPlaying) player.pause() else player.play(); pokeControls() }, modifier = Modifier.size(72.dp)) {
                        AnimatedContent(targetState = isPlaying, label = "playPauseMorph") { playing ->
                            Icon(
                                if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                "Play/Pause", tint = Color.White, modifier = Modifier.size(64.dp),
                            )
                        }
                    }
                    IconButton(onClick = {
                        val target = player.currentPosition + 10_000
                        player.seekTo(target); position = target
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        pokeControls()
                    }) {
                        Icon(Icons.Filled.Forward10, "Forward 10", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }

                // Bottom bar
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
                    // While dragging, show the thumb position; otherwise the ticker's playhead.
                    val dur = duration
                    val pos = if (scrubbing) (scrubValue * dur).toLong() else position
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt(pos), color = Color.White, fontSize = 12.sp)
                        if (!qualityLabel.isNullOrBlank()) {
                            Spacer(Modifier.width(8.dp))
                            Surface(
                                color = Color.White.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(6.dp),
                            ) {
                                Text(
                                    qualityLabel,
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                )
                            }
                        }
                        // Haptic ticks once per whole second of content scrubbed, not per pixel.
                        var lastScrubHapticSec by remember { mutableIntStateOf(-1) }
                        Slider(
                            value = if (scrubbing) scrubValue else if (dur > 0) position.toFloat() / dur else 0f,
                            // Dragging only moves the thumb — no seek spam per pixel moved.
                            onValueChange = {
                                scrubValue = it; scrubbing = true
                                pokeControls()
                                if (dur > 0) {
                                    val sec = (it * dur / 1000L).toInt()
                                    if (sec != lastScrubHapticSec) {
                                        lastScrubHapticSec = sec
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }
                            },
                            // One seek when the finger lifts, then the ticker resumes control.
                            onValueChangeFinished = {
                                if (dur > 0) player.seekTo((scrubValue * dur).toLong())
                                position = if (dur > 0) (scrubValue * dur).toLong() else 0
                                scrubbing = false
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                pokeControls()
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        )
                        Text(fmt(dur), color = Color.White, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // These chips float over the video scrim, so the container stays a fixed
                        // translucent black in every theme (a themed light container would put
                        // white label text on a light pill).
                        val chipColors = AssistChipDefaults.assistChipColors(
                            containerColor = Color.Black.copy(alpha = 0.55f),
                            labelColor = Color.White,
                        )
                        AssistChip(onClick = onReload, label = { Text("Reload") }, colors = chipColors)
                        AssistChip(onClick = { showTracks = true }, label = { Text("Quality & Audio") }, colors = chipColors)
                        AssistChip(onClick = { showSpeedAudio = true }, label = { Text("Speed ${trimSpeed(speed)}x") }, colors = chipColors)
                        // Armed sleep timer readout (ticks with the 250 ms playhead ticker above).
                        sleepStatusLabel(sleepOption, sleepDeadlineMs, SystemClock.elapsedRealtime())?.let { label ->
                            AssistChip(onClick = { showSleep = true }, label = { Text(label) }, colors = chipColors)
                        }
                        if (episodes.isNotEmpty()) {
                            AssistChip(onClick = { showEpisodes = true }, label = { Text("Episodes") }, colors = chipColors)
                        }
                        AssistChip(
                            onClick = { showStats = !showStats },
                            label = { Text(if (showStats) "Stats \u25cf" else "Stats") },
                            colors = chipColors,
                        )
                    }
                }
            }
        }

        // Gesture HUD — brightness (left), volume (right) or seek (center). Rendered after the
        // controls so it sits on top of them, and fades ~1.4 s after the finger lifts.
        AnimatedVisibility(
            visible = hudVisible && hudMode != null,
            enter = fadeIn(),
            exit = fadeOut(tween(400)),
            modifier = Modifier.align(
                when (hudMode) {
                    GestureMode.VOLUME -> Alignment.CenterEnd
                    GestureMode.SEEK -> Alignment.Center
                    else -> Alignment.CenterStart
                },
            ).padding(horizontal = 28.dp),
        ) {
            hudMode?.let { mode ->
                GestureHud(mode = mode, level = hudLevel, position = position, duration = duration)
            }
        }

        // Phase 34: hold-for-2X indicator — clean top-center pill while boosted.
        AnimatedVisibility(
            visible = isBoosting,
            enter = fadeIn(),
            exit = fadeOut(tween(200)),
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 72.dp),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(50),
            ) {
                Text(
                    "2X Speed ▶▶",
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                )
            }
        }

        // Autoplay-next overlay
        countdown?.let { seconds ->
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    // The card itself is a themed surface, so its text follows the theme rather
                    // than staying white (which vanished on a light surface).
                    Text(
                        "Next episode in $seconds\u2026",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TextButton(onClick = onNext) { Text("Play now", color = LocalMoovioColors.current.accent) }
                    TextButton(onClick = onCancelCountdown) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ---- Screen-lock toggle ---------------------------------------------
        if (locked) {
            Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(48.dp)
                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f))
                    .clickable { locked = false; resetAutoHideTimer() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Filled.Lock, "Unlock controls", tint = Color.White)
            }
        }

        // ---- Double-tap seek ripple -----------------------------------------
        if (ripplePulse > 0) {
            DoubleTapRipple(
                pulse = ripplePulse,
                forward = rippleDir >= 0,
                modifier = Modifier.align(Alignment.Center),
            )
        }

        // ---- Stats for nerds overlay ----------------------------------------
        if (showStats) {
            StatsOverlay(
                tracks = tracks,
                videoSize = player.videoSize,
                bufferMs = bufferMs,
                bufferedPct = bufferedPct,
                playbackState = player.playbackState,
                droppedFrames = droppedFrames,
                bandwidthBps = bandwidthBps,
                modifier = Modifier.align(Alignment.TopStart).padding(top = 72.dp, start = 16.dp),
            )
        }

        // ---- Time-shifted subtitle overlay (sync offset) ---------------------
        // Media3 1.3 has no subtitle-offset API, so when an offset is set the native captions are
        // hidden and the buffered cue timeline is replayed at `position - offset` instead.
        if (subtitleSyncMs != 0) {
            val times = cueHistory.map { it.first }
            val activeIndex = activeCueIndex(times, position - subtitleSyncMs)
            val activeCues = if (activeIndex >= 0) cueHistory[activeIndex].second else emptyList()
            if (activeCues.isNotEmpty()) {
                Column(
                    Modifier.align(Alignment.BottomCenter)
                        .padding(horizontal = 32.dp)
                        .padding(bottom = (48 + subtitleStyle.bottomOffset * 260).dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    activeCues.forEach { cue ->
                        val text = cue.text?.toString().orEmpty()
                        if (text.isNotBlank()) {
                            Text(
                                text,
                                color = Color(subtitleColor(subtitleStyle.colorKey)),
                                fontSize = (16f * subtitleStyle.scale).sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .padding(vertical = 2.dp)
                                    .background(Color.Black.copy(alpha = subtitleStyle.bgOpacity))
                                    .padding(horizontal = 10.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }

        if (showSpeedAudio) {
            SpeedAudioSheet(
                speed = speed,
                boost = boost,
                normalize = normalize,
                onSpeed = { speed = it; onSpeedChange(it); pokeControls() },
                onBoost = { boost = it; onAudioBoost(it); pokeControls() },
                onNormalize = { normalize = it; onAudioNormalize(it); pokeControls() },
                onDismiss = { showSpeedAudio = false; pokeControls() },
            )
        }
        if (showExternal) {
            ExternalPlayerSheet(
                onVlc = { showExternal = false; onOpenVlc() },
                onMpv = { showExternal = false; onOpenMpv() },
                onOther = { showExternal = false; onOpenOther() },
                onDismiss = { showExternal = false },
            )
        }

        if (showTracks) {
            TrackSheet(
                tracks = tracks,
                player = player,
                subtitleSyncMs = subtitleSyncMs,
                onSubtitleSync = onSubtitleSync,
                subtitleStyle = subtitleStyle,
                onSubtitleStyle = onSubtitleStyle,
                anime4KPreset = anime4KConfig.preset,
                anime4KTier = anime4KConfig.tier,
                onAnime4KPreset = { preset ->
                    scope.launch { anime4KSettings.setAnime4KPreset(preset.key) }
                    pokeControls()
                },
                onAnime4KTier = { tier ->
                    scope.launch { anime4KSettings.setAnime4KGpuTier(tier.key) }
                    pokeControls()
                },
                onDismiss = { showTracks = false },
            )
        }
        if (showSleep) {
            SleepTimerSheet(
                current = sleepOption,
                deadlineMs = sleepDeadlineMs,
                onSelect = { onSleepTimer(it); showSleep = false },
                onDismiss = { showSleep = false },
            )
        }
        if (showEpisodes) {
            EpisodeSheet(
                episodes = episodes,
                currentIndex = currentIndex,
                onSelect = { showEpisodes = false; onSelectEpisode(it) },
                onDismiss = { showEpisodes = false },
            )
        }
    }
}

/**
 * Minimal "Stats for nerds" panel: resolution, codec, track + estimated bitrate, FPS, buffer state
 * and dropped frames. Values update on the player's 250 ms ticker plus the analytics listener.
 */
@UnstableApi
@Composable
private fun StatsOverlay(
    tracks: Tracks?,
    videoSize: androidx.media3.common.VideoSize,
    bufferMs: Long,
    bufferedPct: Int,
    playbackState: Int,
    droppedFrames: Long,
    bandwidthBps: Long,
    modifier: Modifier = Modifier,
) {
    val format = remember(tracks) {
        runCatching {
            val groups = tracks?.groups
            val video = groups?.firstOrNull { it.type == C.TRACK_TYPE_VIDEO && it.isSelected }
                ?: groups?.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
            video?.getTrackFormat(0)
        }.getOrNull()
    }
    val resolution = when {
        format != null && format.width > 0 -> "${format.width}x${format.height}"
        videoSize.width > 0 -> "${videoSize.width}x${videoSize.height}"
        else -> "\u2014"
    }
    val codec = listOfNotNull(format?.sampleMimeType, format?.codecs)
        .joinToString(" \u00b7 ")
        .ifBlank { "\u2014" }
    val fps = format?.frameRate?.let { formatFps(it) }.orEmpty().ifBlank { "\u2014" }
    val stateLabel = when (playbackState) {
        Player.STATE_BUFFERING -> "buffering"
        Player.STATE_READY -> "ready"
        Player.STATE_ENDED -> "ended"
        else -> "idle"
    }
    Column(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color.Black.copy(alpha = 0.62f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Text("Stats for nerds", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        StatsLine("Resolution", resolution)
        StatsLine("Codec", codec)
        StatsLine("Track bitrate", formatBitrate(format?.bitrate?.toLong() ?: 0L))
        StatsLine("Bandwidth", formatBitrate(bandwidthBps))
        StatsLine("FPS", fps)
        StatsLine("Buffer", "%.1fs \u00b7 %d%%".format(bufferMs / 1000.0, bufferedPct))
        StatsLine("State", stateLabel)
        StatsLine("Dropped", droppedFrames.toString())
    }
}

@Composable
private fun StatsLine(label: String, value: String) {
    Row {
        Text(
            label,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 10.sp,
            modifier = Modifier.width(84.dp),
        )
        Text(
            value,
            // Tabular figures: the stats overlay updates every tick, so the value must not jitter.
            style = androidx.compose.ui.text.TextStyle(fontSize = 10.sp, fontFeatureSettings = "tnum"),
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@UnstableApi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrackSheet(
    tracks: Tracks?,
    player: ExoPlayer,
    subtitleSyncMs: Int,
    onSubtitleSync: (Int) -> Unit,
    subtitleStyle: SubtitleStyle,
    onSubtitleStyle: (SubtitleStyle) -> Unit,
    anime4KPreset: Anime4KPreset,
    anime4KTier: Anime4KGpuTier,
    onAnime4KPreset: (Anime4KPreset) -> Unit,
    onAnime4KTier: (Anime4KGpuTier) -> Unit,
    onDismiss: () -> Unit,
) {
    val moovio = LocalMoovioColors.current
    val videoGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_VIDEO } ?: emptyList() }
    val audioGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO } ?: emptyList() }
    val textGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT } ?: emptyList() }
    var selectedVideo by remember { mutableStateOf(-1) }
    var selectedAudio by remember { mutableStateOf(-1) }
    var selectedText by remember { mutableStateOf(-1) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).padding(16.dp)) {
                Text("Quality", color = moovio.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    item {
                        TrackRow("Auto", selectedVideo == -1) {
                            selectedVideo = -1
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                                .build()
                        }
                    }
                    items(videoGroups) { group ->
                        val fmt = group.getTrackFormat(0)
                        val label = buildString {
                            append(fmt.height.takeIf { it > 0 }?.let { "${it}p" } ?: "Video")
                            fmt.bitrate.takeIf { it > 0 }?.let { append(" \u00b7 ${it / 1000} kbps") }
                        }
                        val idx = videoGroups.indexOf(group)
                        TrackRow(label, selectedVideo == idx) {
                            selectedVideo = idx
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                .build()
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Audio", color = moovio.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    item {
                        TrackRow("Default", selectedAudio == -1) {
                            selectedAudio = -1
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .build()
                        }
                    }
                    items(audioGroups) { group ->
                        val fmt = group.getTrackFormat(0)
                        val label = fmt.label ?: fmt.language ?: "Audio"
                        val idx = audioGroups.indexOf(group)
                        TrackRow(label, selectedAudio == idx) {
                            selectedAudio = idx
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                .build()
                        }
                    }

                    item {
                        Spacer(Modifier.height(8.dp))
                        Text("Subtitles", color = moovio.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                    item {
                        TrackRow("Off", selectedText == -1) {
                            selectedText = -1
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
                                .build()
                        }
                    }
                    items(textGroups) { group ->
                        val fmt = group.getTrackFormat(0)
                        val label = fmt.label ?: fmt.language ?: "Subtitle"
                        val idx = textGroups.indexOf(group)
                        TrackRow(label, selectedText == idx) {
                            selectedText = idx
                            player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                                .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, 0))
                                .build()
                        }
                    }

                    // Phase 15: caption timing correction (±10 s in 100 ms steps, Phase 36).
                    item {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Sync offset ${formatSubtitleSync(subtitleSyncMs)}",
                            color = moovio.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            "Delay or advance captions to fix out-of-sync subtitles",
                            color = moovio.textTertiary,
                            fontSize = 11.sp,
                        )
                        Slider(
                            value = subtitleSyncMs.toFloat(),
                            onValueChange = { onSubtitleSync((it / 100f).roundToInt() * 100) },
                            valueRange = -SUBTITLE_SYNC_MAX_MS.toFloat()..SUBTITLE_SYNC_MAX_MS.toFloat(),
                            steps = 199,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (subtitleSyncMs != 0) {
                            TextButton(onClick = { onSubtitleSync(0) }) {
                                Text("Reset", color = moovio.accent, fontSize = 12.sp)
                            }
                        }
                    }

                    // Phase 29: in-player subtitle styling — the same keys as Settings >
                    // Subtitles, applied live to the PlayerView and the sync overlay via
                    // `onSubtitleStyle` (which also persists them, so Settings stays in sync).
                    // Captions render with the chosen outline color (see `applySubtitleStyle`).
                    item {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Subtitle style",
                            color = moovio.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Text size",
                                color = moovio.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(subtitleStyle.scale * 100).toInt()}%",
                                color = moovio.textTertiary,
                                fontSize = 12.sp,
                            )
                        }
                        Slider(
                            value = subtitleStyle.scale,
                            onValueChange = { onSubtitleStyle(subtitleStyle.copy(scale = it)) },
                            valueRange = 0.6f..2f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Font color", color = moovio.textSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("white", "yellow", "cyan", "green").forEach { key ->
                                val selected = subtitleStyle.colorKey == key
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(subtitleColor(key)))
                                        .border(
                                            if (selected) 3.dp else 1.dp,
                                            if (selected) moovio.accent else moovio.textTertiary,
                                            RoundedCornerShape(50),
                                        )
                                        .clickable { onSubtitleStyle(subtitleStyle.copy(colorKey = key)) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        // Phase 36: outline color — same palette plus black (the default).
                        Text("Outline color", color = moovio.textSecondary, fontSize = 13.sp)
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            listOf("black", "white", "yellow", "cyan", "green").forEach { key ->
                                val selected = subtitleStyle.outlineKey == key
                                Box(
                                    Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(subtitleColor(key)))
                                        .border(
                                            if (selected) 3.dp else 1.dp,
                                            if (selected) moovio.accent else moovio.textTertiary,
                                            RoundedCornerShape(50),
                                        )
                                        .clickable { onSubtitleStyle(subtitleStyle.copy(outlineKey = key)) },
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Background",
                                color = moovio.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(subtitleStyle.bgOpacity * 100).toInt()}%",
                                color = moovio.textTertiary,
                                fontSize = 12.sp,
                            )
                        }
                        Slider(
                            value = subtitleStyle.bgOpacity,
                            onValueChange = { onSubtitleStyle(subtitleStyle.copy(bgOpacity = it)) },
                            valueRange = 0f..1f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "Position",
                                color = moovio.textSecondary,
                                fontSize = 13.sp,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                "${(subtitleStyle.bottomOffset * 100).toInt()}%",
                                color = moovio.textTertiary,
                                fontSize = 12.sp,
                            )
                        }
                        Slider(
                            value = subtitleStyle.bottomOffset,
                            onValueChange = { onSubtitleStyle(subtitleStyle.copy(bottomOffset = it)) },
                            valueRange = 0f..0.3f,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    // Phase 37: Anime4K GPU enhancement — live preset + tier switching.
                    // The selection applies to the running player immediately and persists
                    // in Settings, so it survives across playback sessions.
                    item {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Anime4K Enhancement",
                            color = moovio.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            anime4KEffectLabel(Anime4KConfig(anime4KPreset, anime4KTier)),
                            color = moovio.accent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            anime4KPreset.blurb,
                            color = moovio.textTertiary,
                            fontSize = 11.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Anime4KPreset.entries.forEach { preset ->
                            TrackRow(preset.label, preset == anime4KPreset) { onAnime4KPreset(preset) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("GPU tier", color = moovio.textSecondary, fontSize = 13.sp)
                        Text(
                            anime4KTier.blurb,
                            color = moovio.textTertiary,
                            fontSize = 11.sp,
                        )
                        Anime4KGpuTier.entries.forEach { tier ->
                            TrackRow(tier.label, tier == anime4KTier) { onAnime4KTier(tier) }
                        }
                        if (isAnime4KHeavy(Anime4KConfig(anime4KPreset, anime4KTier))) {
                            Text(
                                "Full HQ pass — best on flagship GPUs; drop to Mid-range if frames stutter.",
                                color = moovio.accent,
                                fontSize = 11.sp,
                            )
                        }
                    }
                }
            }
    }
}

/**
 * Sleep-timer picker (Phase 29): fixed delays plus end-of-episode. Selecting an option arms it
 * immediately (the sheet closes); picking the armed option again re-arms harmlessly, and "Off"
 * disarms. The live remaining readout ticks with composition while armed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepTimerSheet(
    current: SleepTimerOption,
    deadlineMs: Long?,
    onSelect: (SleepTimerOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val moovio = LocalMoovioColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Sleep timer", color = moovio.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Playback pauses automatically when the timer ends.",
                color = moovio.textTertiary,
                fontSize = 12.sp,
            )
            sleepStatusLabel(current, deadlineMs, SystemClock.elapsedRealtime())?.let { status ->
                Spacer(Modifier.height(6.dp))
                Text(status, color = moovio.accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            SleepTimerOption.entries.forEach { option ->
                TrackRow(option.label(), option == current) { onSelect(option) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EpisodeSheet(
    episodes: List<Video>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val moovio = LocalMoovioColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).padding(16.dp)) {
                Text("Episodes", color = moovio.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                LazyColumn(Modifier.padding(top = 8.dp)) {
                    items(episodes.size) { i ->
                        val ep = episodes[i]
                        val label = buildString {
                            if ((ep.season ?: 0) > 0 && (ep.episode ?: 0) > 0) append("S${ep.season} E${ep.episode} \u00b7 ")
                            append(ep.title?.takeIf { it.isNotBlank() } ?: "Episode ${i + 1}")
                        }
                        TrackRow(label, i == currentIndex) { onSelect(i) }
                    }
                }
            }
    }
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val moovio = LocalMoovioColors.current
    val accent = moovio.accent
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Theme-derived text so the rows stay legible on the sheet in light mode too.
        Text(label, color = if (selected) accent else moovio.textPrimary, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = accent)
    }
}

/**
 * On-screen indicator for the swipe gestures: a vertical level bar for brightness/volume, or a
 * center pill with the playhead for seeks. [level] is the normalized 0..1 value for the bar.
 */
@Composable
private fun GestureHud(mode: GestureMode, level: Float, position: Long, duration: Long) {
    if (mode == GestureMode.SEEK) {
        Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(14.dp)) {
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(fmt(position), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(fmt(duration), color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
            }
        }
        return
    }
    Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(18.dp)) {
        Column(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                if (mode == GestureMode.BRIGHTNESS) Icons.Filled.BrightnessHigh else Icons.Filled.VolumeUp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.height(8.dp))
            Box(
                Modifier
                    .width(6.dp)
                    .height(110.dp)
                    .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(50)),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(level)
                        .align(Alignment.BottomStart)
                        .background(Color.White, RoundedCornerShape(50)),
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("${(level * 100).toInt()}%", color = Color.White, fontSize = 11.sp)
        }
    }
}

private fun fmt(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

/** Compact speed label: "1x", "0.5x", "1.25x". */
private fun trimSpeed(v: Float): String =
    if (v % 1f == 0f) v.toInt().toString() else v.toString().trimEnd('0').trimEnd('.')

/**
 * Playback-speed chips plus the audio boost/normalize toggles, in a bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpeedAudioSheet(
    speed: Float,
    boost: Boolean,
    normalize: Boolean,
    onSpeed: (Float) -> Unit,
    onBoost: (Boolean) -> Unit,
    onNormalize: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val moovio = LocalMoovioColors.current
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Playback speed", color = moovio.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    items(speeds) { s ->
                        val selected = kotlin.math.abs(s - speed) < 0.01f
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) moovio.accent else moovio.surfaceElevated)
                                .clickable { onSpeed(s) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("${trimSpeed(s)}x", color = if (selected) Color.White else moovio.textPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Audio boost", color = moovio.textPrimary)
                        Text("Loudness enhancer for quiet dialogue", color = moovio.textSecondary, fontSize = 12.sp)
                    }
                    MoovioSwitch(checked = boost, onCheckedChange = onBoost)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Normalize volume", color = moovio.textPrimary)
                        Text("Even out loud and quiet scenes", color = moovio.textSecondary, fontSize = 12.sp)
                    }
                    MoovioSwitch(checked = normalize, onCheckedChange = onNormalize)
                }
            }
    }
}

/** Quick hand-off to VLC / MPV (or the generic chooser). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExternalPlayerSheet(
    onVlc: () -> Unit,
    onMpv: () -> Unit,
    onOther: () -> Unit,
    onDismiss: () -> Unit,
) {
    val moovio = LocalMoovioColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Open in external player", color = moovio.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                TrackRow("VLC", false, onVlc)
                TrackRow("MPV", false, onMpv)
                TrackRow("Other apps\u2026", false, onOther)
            }
    }
}

/** Expanding-ring ripple shown for the double-tap seek. */
@Composable
private fun DoubleTapRipple(pulse: Int, forward: Boolean, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(pulse) {
        visible = true
        progress.snapTo(0f)
        progress.animateTo(1f, tween(500))
        visible = false
    }
    if (!visible) return
    Box(modifier.size(160.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = (1f - progress.value) * 0.4f),
                radius = size.minDimension / 2f * progress.value,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (forward) Icons.Filled.FastForward else Icons.Filled.FastRewind,
                contentDescription = null, tint = Color.White,
            )
            Spacer(Modifier.width(6.dp))
            Text(if (forward) "+10s" else "\u221210s", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
