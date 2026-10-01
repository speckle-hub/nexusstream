package com.novastream.app.ui.player

import android.app.PictureInPictureParams
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.StreamSource
import com.novastream.app.data.model.SubtitleTrack
import com.novastream.app.ui.theme.NovaStreamTheme
import okhttp3.OkHttpClient

/**
 * Full-featured video player built on Media3 ExoPlayer.
 * Supports quality/track selection, subtitle tracks, PiP, and orientation locking.
 */
@UnstableApi
class PlayerActivity : ComponentActivity() {

    private var player: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null

    private lateinit var item: MediaItem
    private lateinit var source: StreamSource
    private var subtitles: List<SubtitleTrack> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        item = readItem(intent) ?: run { finish(); return }
        source = readSource(intent) ?: run { finish(); return }

        // Honor the "Subtitles enabled" and "Preferred quality" settings.
        val settings = (application as NovaApp).container.settings
        val subsEnabled = settings.subsEnabled.value
        val preferredQuality = settings.preferredQuality.value
        subtitles = if (subsEnabled) readSubs(intent) else emptyList()

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(30_000, 120_000, 1_500, 3_000)
            .build()
        val selector = DefaultTrackSelector(this).apply {
            var params = buildUponParameters().setAllowVideoMixedMimeTypeAdaptiveness(true)
            // Cap the max video size so ExoPlayer pre-selects the requested quality.
            qualityCap(preferredQuality)?.let { (w, h) -> params = params.setMaxVideoSize(w, h) }
            setParameters(params)
        }
        trackSelector = selector
        val exo = ExoPlayer.Builder(this)
            .setTrackSelector(selector)
            .setLoadControl(loadControl)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory()))
            .build()
        player = exo
        exo.playWhenReady = true
        exo.setMediaItem(buildMediaItem())
        exo.prepare()

        setContent {
            NovaStreamTheme(darkTheme = true, accentKey = "violet") {
                PlayerScreen(
                    player = exo,
                    title = item.title,
                    subtitle = source.name ?: source.title,
                    onBack = { finish() },
                    onPip = { enterPip() },
                    onRotate = { toggleOrientation() },
                    onExternal = { openExternal() },
                    onReload = { reload() },
                )
            }
        }
    }

    /**
     * OkHttp-backed source that replays [StreamSource.headers] on every request (page Referer,
     * age cookie, user agent). Pornhub's CDN answers 410/412 to ExoPlayer's default request — no
     * Referer, no cookie — which is why some streams were listed but never started.
     */
    private fun dataSourceFactory(): DataSource.Factory {
        val headers = source.headers
        val local = DefaultDataSource.Factory(this)
        if (headers.isEmpty()) return local
        val http = OkHttpDataSource.Factory(OkHttpClient()).apply {
            setDefaultRequestProperties(headers)
        }
        return DefaultDataSource.Factory(this, http)
    }

    private fun buildMediaItem(): Media3Item {
        val builder = Media3Item.Builder()
            .setUri(source.playableUrl)
            .setMediaId(item.key)
        val subs = subtitles.map { s ->
            Media3Item.SubtitleConfiguration.Builder(Uri.parse(s.url))
                .setMimeType(mimeFor(s.url))
                .setLanguage(s.lang)
                .setLabel(s.lang.uppercase())
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .build()
        }
        if (subs.isNotEmpty()) builder.setSubtitleConfigurations(subs)
        return builder.build()
    }

    private fun reload() {
        player?.let {
            it.setMediaItem(buildMediaItem())
            it.prepare()
            it.playWhenReady = true
        }
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
        if (isInPip) hideSystemBars() else hideSystemBars()
    }

    private fun hideSystemBars() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onPause() {
        super.onPause()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || !isInPictureInPictureMode) player?.pause()
    }

    override fun onDestroy() {
        player?.release()
        player = null
        super.onDestroy()
    }

    /** Max video dimensions for a preferred-quality label, or null for "Auto". */
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

        fun intent(context: Context, item: MediaItem, source: StreamSource, subtitles: List<SubtitleTrack>): Intent {
            val i = Intent(context, PlayerActivity::class.java)
            i.putExtra(EXT_ITEM, com.google.gson.Gson().toJson(item))
            i.putExtra(EXT_SOURCE, com.google.gson.Gson().toJson(source))
            i.putExtra(EXT_SUBS, com.google.gson.Gson().toJson(subtitles))
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
            val arr = com.google.gson.Gson().fromJson(t, Array<SubtitleTrack>::class.java)
            arr.toList()
        }.getOrDefault(emptyList())
    }
}

@UnstableApi
@Composable
private fun PlayerScreen(
    player: ExoPlayer,
    title: String,
    subtitle: String?,
    onBack: () -> Unit,
    onPip: () -> Unit,
    onRotate: () -> Unit,
    onExternal: () -> Unit,
    onReload: () -> Unit,
) {
    var controlsVisible by remember { mutableStateOf(true) }
    var showTracks by remember { mutableStateOf(false) }
    var tracks by remember { mutableStateOf<Tracks?>(null) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onTracksChanged(t: Tracks) { tracks = t }
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
            modifier = Modifier.fillMaxSize().clickable { controlsVisible = !controlsVisible },
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
                    IconButton(onClick = { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)) }) {
                        Icon(Icons.Filled.Replay10, "Back 10", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                    IconButton(onClick = { if (player.isPlaying) player.pause() else player.play() }, modifier = Modifier.size(72.dp)) {
                        Icon(
                            if (player.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            "Play/Pause", tint = Color.White, modifier = Modifier.size(64.dp),
                        )
                    }
                    IconButton(onClick = { player.seekTo(player.currentPosition + 10_000) }) {
                        Icon(Icons.Filled.Forward10, "Forward 10", tint = Color.White, modifier = Modifier.size(40.dp))
                    }
                }

                // Bottom bar
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(16.dp)) {
                    val dur = player.duration.coerceAtLeast(0)
                    val pos = player.currentPosition.coerceAtLeast(0)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(fmt(pos), color = Color.White, fontSize = 12.sp)
                        Slider(
                            value = if (dur > 0) pos.toFloat() / dur else 0f,
                            onValueChange = { if (dur > 0) player.seekTo((it * dur).toLong()) },
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                        )
                        Text(fmt(dur), color = Color.White, fontSize = 12.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        AssistChip(onClick = onReload, label = { Text("Reload", color = Color.White) })
                        AssistChip(onClick = { showTracks = true }, label = { Text("Quality & Subs", color = Color.White) })
                    }
                }
            }
        }

        if (showTracks) {
            TrackSheet(tracks = tracks, player = player, onDismiss = { showTracks = false })
        }
    }
}

@UnstableApi
@Composable
private fun TrackSheet(tracks: Tracks?, player: ExoPlayer, onDismiss: () -> Unit) {
    val videoGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_VIDEO } ?: emptyList() }
    val textGroups = remember(tracks) { tracks?.groups?.filter { it.type == C.TRACK_TYPE_TEXT } ?: emptyList() }
    var selectedVideo by remember { mutableStateOf(-1) }
    var selectedText by remember { mutableStateOf(-1) }

    Box(Modifier.fillMaxSize().background(Color(0xAA000000)).clickable { onDismiss() }) {
        Surface(
            color = Color(0xFF14141C),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.6f),
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
                            fmt.bitrate.takeIf { it > 0 }?.let { append(" · ${it / 1000} kbps") }
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
private fun TrackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable { onClick() }.padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = if (selected) Color(0xFF7C5CFF) else Color.White, modifier = Modifier.weight(1f))
        if (selected) Icon(Icons.Filled.Check, null, tint = Color(0xFF7C5CFF))
    }
}

private fun fmt(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
