package com.novastream.app.ui.player

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
import android.util.Rational
import android.view.View
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import com.novastream.app.ui.vm.LocalContainer
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
import com.novastream.app.NovaApp
import com.novastream.app.data.download.DownloadManagerProvider
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.download.VideoKind
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.data.model.Video
import com.novastream.app.data.remote.Http
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.ui.components.NovaSwitch
import com.novastream.app.ui.theme.LocalNovaColors
import com.novastream.app.ui.theme.NovaStreamTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Full-featured video player built on Media3 ExoPlayer.
 *
 * Supports quality/track selection, subtitle + audio track selection, PiP, orientation locking,
 * edge-drag brightness/volume, horizontal seek gestures, an episode side-sheet, and a 5-second
 * autoplay-next countdown.
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
    private var subtitleStyle = SubtitleStyle()
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

        item = readItem(intent) ?: run { finish(); return }
        val first = readSource(intent) ?: run { finish(); return }
        episodes = readEpisodes(intent)
        index = readIndex(intent)

        val settings = (application as NovaApp).container.settings
        val container = (application as NovaApp).container
        autoplay = settings.autoplay.value
        subtitlesEnabled = settings.subsEnabled.value
        playbackSpeed = settings.playerSpeed.value
        audioBoost = settings.audioBoost.value
        audioNormalize = settings.audioNormalize.value
        subtitleSyncMs = settings.subtitleSyncMs.value
        autoDeleteWatched = settings.autoDeleteWatched.value
        subtitleStyle = SubtitleStyle(
            scale = settings.subtitleScale.value,
            colorKey = settings.subtitleColor.value,
            bgOpacity = settings.subtitleBg.value,
            bottomOffset = settings.subtitleOffset.value,
        )
        val preferredQuality = settings.preferredQuality.value

        source = first
        httpFactory.setDefaultRequestProperties(source.headers)
        subs = if (subtitlesEnabled) readSubs(intent) else emptyList()
        titleState.value = if (index >= 0) episodeTitle(episodes.getOrNull(index)) else item.title
        subtitleState.value = first.name ?: first.title
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
        val exo = ExoPlayer.Builder(this)
            .setTrackSelector(selector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
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
        exo.setMediaItem(buildMediaItem())
        exo.prepare()
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
            NovaStreamTheme(
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
                    subtitleStyle = subtitleStyle,
                    subtitleSyncMs = subtitleSyncMs,
                    onSubtitleSync = {
                        subtitleSyncMs = clampSubtitleSyncMs(it)
                        lifecycleScope.launch { settings.setSubtitleSyncMs(subtitleSyncMs) }
                    },
                    initialSpeed = playbackSpeed,
                    audioBoostOn = audioBoost,
                    audioNormalizeOn = audioNormalize,
                    introEndMs = source.introEndMs ?: source.introStartMs,
                    outroStartMs = source.outroStartMs,
                    onSpeedChange = { setPlaybackSpeed(it) },
                    onAudioBoost = { setAudioBoost(it) },
                    onAudioNormalize = { setAudioNormalize(it) },
                    onOpenVlc = { openInPackage("org.videolan.vlc") },
                    onOpenMpv = { openInPackage("is.xyz.mpv") },
                    onOpenOther = { openExternal() },
                    onCast = { openCast() },
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
            .setMediaId(item.key)
        val configs = subs.mapIndexed { idx, s ->
            Media3Item.SubtitleConfiguration.Builder(Uri.parse(s.url))
                .setMimeType(mimeFor(s.url))
                .setLanguage(s.lang)
                .setLabel(s.lang.uppercase())
                // Only the first track (the language the user picked in the detail sheet) is
                // pre-selected; the rest stay available but inactive.
                .setSelectionFlags(if (idx == 0) C.SELECTION_FLAG_DEFAULT else 0)
                .build()
        }
        if (configs.isNotEmpty()) builder.setSubtitleConfigurations(configs)
        return builder.build()
    }

    /** Load a new source into the existing player (used by reload and episode switching). */
    private fun applySource(newSource: StreamSource, newSubs: List<SubtitleTrack>) {
        source = newSource
        subs = newSubs
        failedUrls.clear()
        autoTriggered = false
        countdownState.value = null
        val exo = player ?: return
        httpFactory.setDefaultRequestProperties(newSource.headers)
        exo.setMediaItem(buildMediaItem())
        exo.prepare()
        exo.playWhenReady = true
    }

    /**
     * Try the next backup mirror for this stream. Returns false when there are no alternatives left.
     * Candidate order is the primary URL followed by [StreamSource.alternates].
     */
    private fun tryFailover(): Boolean {
        val candidates = (listOfNotNull(source.playableUrl) + source.alternates).distinct()
        val next = candidates.firstOrNull { it !in failedUrls && it != source.playableUrl } ?: return false
        source.playableUrl?.let { failedUrls += it }
        source = source.copy(url = next)
        httpFactory.setDefaultRequestProperties(source.headers)
        Toast.makeText(this, "Switching to backup source\u2026", Toast.LENGTH_SHORT).show()
        val resumeAt = player?.currentPosition?.coerceAtLeast(0L) ?: 0L
        player?.let {
            it.setMediaItem(buildMediaItem())
            it.prepare()
            it.seekTo(resumeAt)
            it.playWhenReady = true
        }
        return true
    }

    private fun reload() {
        autoTriggered = false
        player?.let {
            it.setMediaItem(buildMediaItem())
            it.prepare()
            it.playWhenReady = true
        }
    }

    /** Resolve streams for an episode and switch playback to it. */
    private fun playEpisode(target: Int) {
        if (target !in episodes.indices) return
        cancelCountdown()
        recordProgress()
        val ep = episodes[target]
        lifecycleScope.launch {
            val container = (application as NovaApp).container
            val streams = runCatching { container.streamRepository.streamsFor(item, ep.id) }
                .getOrDefault(emptyList())
            val playable = streams.firstOrNull { it.playableUrl != null } ?: streams.firstOrNull()
            if (playable?.playableUrl == null) {
                Toast.makeText(this@PlayerActivity, "No playable stream for the next episode", Toast.LENGTH_SHORT).show()
                return@launch
            }
            val foundSubs = if (subtitlesEnabled) {
                runCatching { container.streamRepository.subtitlesFor(item, ep.id) }.getOrDefault(emptyList())
            } else emptyList()

            index = target
            indexState.value = target
            titleState.value = episodeTitle(ep)
            subtitleState.value = buildString {
                append("S${ep.season ?: 1} E${ep.episode ?: (target + 1)}")
                playable.name?.takeIf { it.isNotBlank() }?.let { append(" \u00b7 $it") }
            }
            applySource(playable, foundSubs)
        }
    }

    private fun cancelCountdown() {
        countdownJob?.cancel()
        countdownJob = null
        countdownState.value = null
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
        val settings = (application as NovaApp).container.settings
        if (!settings.autoDownloadNext.value) return
        if (settings.autoDownloadWifiOnly.value && !onWifi()) return
        val target = index + 2
        if (target !in episodes.indices) return
        val ep = episodes[target]
        lifecycleScope.launch {
            val container = (application as NovaApp).container
            val streams = runCatching { container.streamRepository.streamsFor(item, ep.id) }
                .getOrDefault(emptyList())
            val source = streams.firstOrNull { it.playableUrl != null } ?: return@launch
            val url = source.playableUrl ?: return@launch
            val kind = if (url.contains(".m3u8", ignoreCase = true)) VideoKind.HLS else VideoKind.PROGRESSIVE
            val label = buildString {
                append(item.title)
                if (ep.season != null && ep.episode != null) append(" \u00b7 S${ep.season}E${ep.episode}")
            }
            container.videoDownloadManager.enqueue(
                item, url, label, ep.title, kind,
                headers = source.headers,
                alternates = source.alternates,
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
                    maybeAutoplay()
                }
                // Persist roughly every 5 s while playing, so closing the app never loses more
                // than a few seconds and Continue Watching stays current.
                if (++ticks % 5 == 0 && p.isPlaying) recordProgress()
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
            (application as NovaApp).container.videoDownloadManager.deleteDownloadsForItem(item.key)
        }
        val ep = episodes.getOrNull(index)
        val entry = WatchEntry(
            item = item,
            videoId = ep?.id,
            videoTitle = ep?.title,
            positionMs = position,
            durationMs = duration,
        )
        (application as NovaApp).appScope.launch {
            runCatching { (application as NovaApp).container.libraryStore.recordWatch(entry) }
        }
    }

    /** Seek to the last saved position for this title/episode, when there is one worth resuming. */
    private suspend fun resumeLastPosition() {
        val videoId = episodes.getOrNull(index)?.id
        val entry = runCatching {
            (application as NovaApp).container.libraryStore.history.first()
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
            lifecycleScope.launch { (application as NovaApp).container.settings.setPlayerBrightness(brightnessValue) }
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

    /** Best-effort Cast/DLNA action: open the system cast/route chooser when available. */
    private fun openCast() {
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

    private fun setPlaybackSpeed(speed: Float) {
        playbackSpeed = speed.coerceIn(0.5f, 2f)
        player?.setPlaybackSpeed(playbackSpeed)
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
        progressJob?.cancel()
        recordProgress()
        runCatching { loudnessEnhancer?.release() }
        loudnessEnhancer = null
        player?.release()
        player = null
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

/** Heuristic intro/outro windows used when a stream supplies no chapter markers. */
private const val DEFAULT_INTRO_END_MS = 90_000L
private const val DEFAULT_OUTRO_MS = 90_000L

/** Cap on buffered caption events kept for the subtitle-sync overlay (~a few minutes of cues). */
private const val CUE_HISTORY_LIMIT = 200

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
)

private fun subtitleColor(key: String): Int = when (key) {
    "yellow" -> 0xFFFFEB3B.toInt()
    "cyan" -> 0xFF35D6E0.toInt()
    "green" -> 0xFF7CFF7C.toInt()
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
        android.graphics.Color.BLACK,
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
    onGestures: (GestureMode, Float) -> Float,
    onGestureEnd: () -> Unit,
    subtitleStyle: SubtitleStyle,
    subtitleSyncMs: Int,
    onSubtitleSync: (Int) -> Unit,
    initialSpeed: Float,
    audioBoostOn: Boolean,
    audioNormalizeOn: Boolean,
    introEndMs: Long?,
    outroStartMs: Long?,
    onSpeedChange: (Float) -> Unit,
    onAudioBoost: (Boolean) -> Unit,
    onAudioNormalize: (Boolean) -> Unit,
    onOpenVlc: () -> Unit,
    onOpenMpv: () -> Unit,
    onOpenOther: () -> Unit,
    onCast: () -> Unit,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showTracks by remember { mutableStateOf(false) }
    var showEpisodes by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf<Tracks?>(null) }
    // Phase 11 player state.
    var showSpeedAudio by remember { mutableStateOf(false) }
    var showExternal by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(initialSpeed) }
    var boost by remember { mutableStateOf(audioBoostOn) }
    var normalize by remember { mutableStateOf(audioNormalizeOn) }
    val doubleTapMs by LocalContainer.current.settings.doubleTapMs.collectAsStateWithLifecycle()
    var locked by remember { mutableStateOf(false) }
    var scaleMode by remember { mutableStateOf(ScaleMode.FIT) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    // Ripple shown on a double-tap seek: direction (-1 back / +1 forward) and a pulse counter.
    var rippleDir by remember { mutableStateOf(0) }
    var ripplePulse by remember { mutableIntStateOf(0) }

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
                    var mode = GestureMode.SEEK
                    detectDragGestures(
                        onDragStart = { offset ->
                            mode = when {
                                offset.x < width * 0.18f -> GestureMode.BRIGHTNESS
                                offset.x > width * 0.82f -> GestureMode.VOLUME
                                else -> GestureMode.SEEK
                            }
                            // Seed the HUD with the current level (a zero-delta call is a read).
                            showHud(mode, onGestures(mode, 0f))
                        },
                        onDrag = { change, drag ->
                            change.consume()
                            val level = when (mode) {
                                // Vertical drag on the edges; downward decreases.
                                GestureMode.BRIGHTNESS, GestureMode.VOLUME ->
                                    onGestures(mode, -drag.y / 260f)
                                GestureMode.SEEK -> onGestures(GestureMode.SEEK, drag.x)
                            }
                            showHud(mode, level)
                        },
                        onDragEnd = { onGestureEnd(); scheduleHudHide() },
                        onDragCancel = { onGestureEnd(); scheduleHudHide() },
                    )
                }
                // Double-tap left/right = rewind/forward by the configured interval, single tap toggles UI.
                .pointerInput(locked) {
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
                        },
                        onTap = { controlsVisible = !controlsVisible },
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

        AnimatedVisibility(visible = controlsVisible && !locked, enter = fadeIn(), exit = fadeOut()) {
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
                    IconButton(onClick = onCast) { Icon(Icons.Filled.Cast, "Cast / DLNA", tint = Color.White) }
                    IconButton(onClick = onPip) { Icon(Icons.Filled.PictureInPictureAlt, "PiP", tint = Color.White) }
                    IconButton(onClick = onRotate) { Icon(Icons.Filled.ScreenRotation, "Rotate", tint = Color.White) }
                    IconButton(onClick = { locked = true; controlsVisible = false }) { Icon(Icons.Filled.LockOpen, "Lock controls", tint = Color.White) }
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
                    }) {
                        Icon(Icons.Filled.Replay10, "Back 10", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }, modifier = Modifier.size(72.dp)) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play/Pause", tint = Color.White, modifier = Modifier.size(64.dp),
                        )
                    }
                    IconButton(onClick = {
                        val target = player.currentPosition + 10_000
                        player.seekTo(target); position = target
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
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
                        // Haptic ticks once per whole second of content scrubbed, not per pixel.
                        var lastScrubHapticSec by remember { mutableIntStateOf(-1) }
                        Slider(
                            value = if (scrubbing) scrubValue else if (dur > 0) position.toFloat() / dur else 0f,
                            // Dragging only moves the thumb — no seek spam per pixel moved.
                            onValueChange = {
                                scrubValue = it; scrubbing = true
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
                    TextButton(onClick = onNext) { Text("Play now", color = LocalNovaColors.current.accent) }
                    TextButton(onClick = onCancelCountdown) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // ---- Skip Intro / Outro (chapter/timestamp markers) ------------------
        // Uses real markers when the stream provides them, otherwise a conservative window.
        val introEnd = introEndMs ?: DEFAULT_INTRO_END_MS
        val outroStart = outroStartMs
            ?: if (duration > 0) (duration - DEFAULT_OUTRO_MS).coerceAtLeast(0) else Long.MAX_VALUE
        val skipIntro = position in 5_000 until introEnd
        val skipOutro = duration > 0 && position >= outroStart

        // Auto-skip intro: jump straight past it when the setting is on. keyed on skipIntro so it
        // fires once per intro window and not on every recomposition.
        val autoSkipIntro by LocalContainer.current.settings.autoSkipIntro.collectAsStateWithLifecycle()
        LaunchedEffect(autoSkipIntro, skipIntro, introEnd) {
            if (autoSkipIntro && skipIntro) {
                player.seekTo(introEnd)
                position = introEnd
            }
        }

        if (!locked && (skipIntro || skipOutro)) {
            Row(
                Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (skipIntro) {
                    Button(onClick = { player.seekTo(introEnd); position = introEnd }) { Text("Skip Intro") }
                }
                if (skipOutro) {
                    Button(onClick = { player.seekTo(duration); position = duration }) { Text("Skip Outro") }
                }
            }
        }

        // ---- Screen-lock toggle ---------------------------------------------
        if (locked) {
            Box(
                Modifier.align(Alignment.CenterEnd).padding(end = 16.dp).size(48.dp)
                    .clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.45f))
                    .clickable { locked = false; controlsVisible = true },
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
                onSpeed = { speed = it; onSpeedChange(it) },
                onBoost = { boost = it; onAudioBoost(it) },
                onNormalize = { normalize = it; onAudioNormalize(it) },
                onDismiss = { showSpeedAudio = false },
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
                onDismiss = { showTracks = false },
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
    onDismiss: () -> Unit,
) {
    val nova = LocalNovaColors.current
    val videoGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_VIDEO } ?: emptyList() }
    val audioGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO } ?: emptyList() }
    val textGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT } ?: emptyList() }
    var selectedVideo by remember { mutableStateOf(-1) }
    var selectedAudio by remember { mutableStateOf(-1) }
    var selectedText by remember { mutableStateOf(-1) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).padding(16.dp)) {
                Text("Quality", color = nova.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Text("Audio", color = nova.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Text("Subtitles", color = nova.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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

                    // Phase 15: caption timing correction (±10 s in 0.5 s steps).
                    item {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Sync offset ${formatSubtitleSync(subtitleSyncMs)}",
                            color = nova.textPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                        )
                        Text(
                            "Delay or advance captions to fix out-of-sync subtitles",
                            color = nova.textTertiary,
                            fontSize = 11.sp,
                        )
                        Slider(
                            value = subtitleSyncMs.toFloat(),
                            onValueChange = { onSubtitleSync((it / 500f).roundToInt() * 500) },
                            valueRange = -SUBTITLE_SYNC_MAX_MS.toFloat()..SUBTITLE_SYNC_MAX_MS.toFloat(),
                            steps = 39,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (subtitleSyncMs != 0) {
                            TextButton(onClick = { onSubtitleSync(0) }) {
                                Text("Reset", color = nova.accent, fontSize = 12.sp)
                            }
                        }
                    }
                }
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
    val nova = LocalNovaColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.7f).padding(16.dp)) {
                Text("Episodes", color = nova.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
    val nova = LocalNovaColors.current
    val accent = nova.accent
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Theme-derived text so the rows stay legible on the sheet in light mode too.
        Text(label, color = if (selected) accent else nova.textPrimary, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = accent)
    }
}

/**
 * On-screen indicator for the edge gestures: a vertical level bar for brightness/volume, or a
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
    val nova = LocalNovaColors.current
    val speeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Playback speed", color = nova.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 10.dp),
                ) {
                    items(speeds) { s ->
                        val selected = kotlin.math.abs(s - speed) < 0.01f
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (selected) nova.accent else nova.surfaceElevated)
                                .clickable { onSpeed(s) }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            Text("${trimSpeed(s)}x", color = if (selected) Color.White else nova.textPrimary)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Audio boost", color = nova.textPrimary)
                        Text("Loudness enhancer for quiet dialogue", color = nova.textSecondary, fontSize = 12.sp)
                    }
                    NovaSwitch(checked = boost, onCheckedChange = onBoost)
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Normalize volume", color = nova.textPrimary)
                        Text("Even out loud and quiet scenes", color = nova.textSecondary, fontSize = 12.sp)
                    }
                    NovaSwitch(checked = normalize, onCheckedChange = onNormalize)
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
    val nova = LocalNovaColors.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Open in external player", color = nova.textPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
