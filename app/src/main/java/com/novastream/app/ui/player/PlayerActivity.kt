package com.novastream.app.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.util.Rational
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem as Media3Item
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.novastream.app.NovaApp
import com.novastream.app.data.download.DownloadManagerProvider
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.data.model.Video
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.ui.theme.NovaStreamTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import kotlin.math.abs

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

    private var brightnessValue = 0.5f
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
        autoplay = settings.autoplay.value
        subtitlesEnabled = settings.subsEnabled.value
        val preferredQuality = settings.preferredQuality.value

        source = first
        httpFactory.setDefaultRequestProperties(source.headers)
        subs = if (subtitlesEnabled) readSubs(intent) else emptyList()
        titleState.value = if (index >= 0) episodeTitle(episodes.getOrNull(index)) else item.title
        subtitleState.value = first.name ?: first.title
        episodesState.value = episodes
        indexState.value = index
        brightnessValue = window.attributes.screenBrightness.takeIf { it in 0f..1f } ?: 0.5f

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
        exo.setMediaItem(buildMediaItem())
        exo.prepare()
        startProgressWatcher()
        resumeLastPosition()

        setContent {
            NovaStreamTheme(darkTheme = true, accentKey = "violet") {
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
    private val httpFactory: OkHttpDataSource.Factory by lazy { OkHttpDataSource.Factory(OkHttpClient()) }

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
        val configs = subs.map { s ->
            Media3Item.SubtitleConfiguration.Builder(Uri.parse(s.url))
                .setMimeType(mimeFor(s.url))
                .setLanguage(s.lang)
                .setLabel(s.lang.uppercase())
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }
        if (configs.isNotEmpty()) builder.setSubtitleConfigurations(configs)
        return builder.build()
    }

    /** Load a new source into the existing player (used by reload and episode switching). */
    private fun applySource(newSource: StreamSource, newSubs: List<SubtitleTrack>) {
        source = newSource
        subs = newSubs
        autoTriggered = false
        countdownState.value = null
        val exo = player ?: return
        httpFactory.setDefaultRequestProperties(newSource.headers)
        exo.setMediaItem(buildMediaItem())
        exo.prepare()
        exo.playWhenReady = true
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

    /** Start the 5-second autoplay-next countdown when a following episode exists. */
    private fun maybeAutoplay() {
        if (!autoplay) return
        if (index < 0 || index >= episodes.size - 1) return
        if (countdownJob?.isActive == true) return
        countdownJob = lifecycleScope.launch {
            for (second in 5 downTo 1) {
                countdownState.value = second
                delay(1000)
            }
            countdownState.value = null
            playEpisode(index + 1)
        }
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
                if (dur > 0 && p.currentPosition >= (dur * 0.95f).toLong() && !autoTriggered) {
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
        val p = player ?: return
        val position = p.currentPosition.coerceAtLeast(0)
        val duration = p.duration.takeIf { it > 0 } ?: 0
        if (position <= 0 && duration <= 0) return
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
    private fun resumeLastPosition() {
        val videoId = episodes.getOrNull(index)?.id
        lifecycleScope.launch {
            val entry = runCatching {
                (application as NovaApp).container.libraryStore.history.first()
                    .firstOrNull { it.item.key == item.key && it.videoId == videoId }
            }.getOrNull() ?: return@launch
            val dur = entry.durationMs
            val resume = entry.positionMs
            val finished = dur > 0 && resume >= (dur * 0.95f).toLong()
            if (resume > 0 && !finished) runCatching { player?.seekTo(resume) }
        }
    }

    /** Edge-drag brightness delta → the new normalized level (what the gesture HUD shows). */
    private fun adjustBrightness(delta: Float): Float {
        if (delta != 0f) {
            brightnessValue = (brightnessValue + delta).coerceIn(0.02f, 1f)
            window.attributes = window.attributes.apply { screenBrightness = brightnessValue }
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
                val steps = volumeAccum.toInt() // truncates toward zero
                volumeAccum -= steps
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
            runCatching { enterPictureInPictureMode(params) }
        }
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

private enum class GestureMode { BRIGHTNESS, VOLUME, SEEK }

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
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showTracks by remember { mutableStateOf(false) }
    var showEpisodes by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf<Tracks?>(null) }

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
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    val width = size.width.toFloat()
                    var mode = GestureMode.SEEK
                    var startX = 0f
                    detectDragGestures(
                        onDragStart = { offset ->
                            startX = offset.x
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
                .clickable { controlsVisible = !controlsVisible },
        )

        AnimatedVisibility(visible = controlsVisible, enter = fadeIn(), exit = fadeOut()) {
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
                    IconButton(onClick = onExternal) { Icon(Icons.Filled.OpenInNew, "External", tint = Color.White) }
                    IconButton(onClick = onPip) { Icon(Icons.Filled.PictureInPictureAlt, "PiP", tint = Color.White) }
                    IconButton(onClick = onRotate) { Icon(Icons.Filled.ScreenRotation, "Rotate", tint = Color.White) }
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
                        Slider(
                            value = if (scrubbing) scrubValue else if (dur > 0) position.toFloat() / dur else 0f,
                            // Dragging only moves the thumb — no seek spam per pixel moved.
                            onValueChange = { scrubValue = it; scrubbing = true },
                            // One seek when the finger lifts, then the ticker resumes control.
                            onValueChangeFinished = {
                                if (dur > 0) player.seekTo((scrubValue * dur).toLong())
                                position = if (dur > 0) (scrubValue * dur).toLong() else 0
                                scrubbing = false
                            },
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        )
                        Text(fmt(dur), color = Color.White, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onReload, label = { Text("Reload", color = Color.White) })
                        AssistChip(onClick = { showTracks = true }, label = { Text("Quality & Audio", color = Color.White) })
                        if (episodes.isNotEmpty()) {
                            AssistChip(onClick = { showEpisodes = true }, label = { Text("Episodes", color = Color.White) })
                        }
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
                color = Color(0xE614141C),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
            ) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Next episode in $seconds\u2026",
                        color = Color.White,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                    TextButton(onClick = onNext) { Text("Play now", color = Color(0xFF7C5CFF)) }
                    TextButton(onClick = onCancelCountdown) { Text("Cancel", color = Color.White.copy(alpha = 0.7f)) }
                }
            }
        }

        if (showTracks) {
            TrackSheet(tracks = tracks, player = player, onDismiss = { showTracks = false })
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

@UnstableApi
@Composable
private fun TrackSheet(tracks: Tracks?, player: ExoPlayer, onDismiss: () -> Unit) {
    val videoGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_VIDEO } ?: emptyList() }
    val audioGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_AUDIO } ?: emptyList() }
    val textGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT } ?: emptyList() }
    var selectedVideo by remember { mutableStateOf(-1) }
    var selectedAudio by remember { mutableStateOf(-1) }
    var selectedText by remember { mutableStateOf(-1) }

    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable { onDismiss() }) {
        Surface(
            color = Color(0xFF14141C),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.7f),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Quality", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Text("Audio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        Text("Subtitles", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                }
            }
        }
    }
}

@Composable
private fun EpisodeSheet(
    episodes: List<Video>,
    currentIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable { onDismiss() }) {
        Surface(
            color = Color(0xFF14141C),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.7f),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Episodes", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
}

@Composable
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (selected) Color(0xFF7C5CFF) else Color.White, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = Color(0xFF7C5CFF))
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
