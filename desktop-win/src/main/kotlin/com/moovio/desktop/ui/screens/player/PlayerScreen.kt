package com.moovio.desktop.ui.screens.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.VolumeOff
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.moovio.desktop.di.AppContainer
import com.moovio.desktop.domain.StreamRanking
import com.moovio.desktop.domain.model.MediaItem
import com.moovio.desktop.domain.model.PlaybackRequest
import com.moovio.desktop.domain.model.StreamSource
import com.moovio.desktop.domain.model.WatchProgress
import com.moovio.desktop.player.MpvPlayer
import com.moovio.desktop.player.MpvSnapshot
import com.moovio.desktop.player.MpvTrack
import com.moovio.desktop.ui.components.CircleIconButton
import com.moovio.desktop.ui.components.GhostButton
import com.moovio.desktop.ui.components.LoadingIndicator
import com.moovio.desktop.ui.components.PrimaryButton
import com.moovio.desktop.ui.components.RemoteImage
import com.moovio.desktop.ui.components.formatDuration
import com.moovio.desktop.ui.nav.AppState
import com.moovio.desktop.ui.theme.AccentColor
import com.moovio.desktop.ui.theme.Palette
import com.moovio.desktop.ui.theme.glass
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(app: AppState, container: AppContainer, onExit: () -> Unit) {
    val target = app.playTarget
    val settings by container.settings.state.collectAsState()
    val library by container.library.state.collectAsState()

    var resolving by remember { mutableStateOf(true) }
    var startupMessage by remember { mutableStateOf("Finding the best stream…") }
    var error by remember { mutableStateOf<String?>(null) }
    var playback by remember { mutableStateOf<PlaybackRequest?>(null) }
    var candidates by remember { mutableStateOf<List<StreamSource>>(emptyList()) }
    var player by remember { mutableStateOf<MpvPlayer?>(null) }
    var showSources by remember { mutableStateOf(false) }
    var sourceFailures by remember { mutableStateOf<List<String>>(emptyList()) }

    // Close the current mpv instance whenever it is replaced or the screen leaves.
    DisposableEffect(player) {
        val instance = player
        onDispose { runCatching { instance?.close() } }
    }

    // 1) Resolve a stream (unless the caller already selected one).
    LaunchedEffect(target?.media?.key, target?.season, target?.episode, target?.source?.fingerprint) {
        val request = target ?: return@LaunchedEffect
        resolving = true
        error = null
        startupMessage = "Finding the best stream…"
        val includeAdult = request.media.adult && container.isNsfwUnlocked

        val detail = request.detail ?: withContext(Dispatchers.IO) {
            runCatching { container.metadata.detail(request.media, includeAdult) }.getOrNull()
        }

        fun start(source: StreamSource, startMs: Long) {
            playback = PlaybackRequest(
                media = request.media,
                source = source,
                startPositionMs = startMs,
                season = request.season,
                episode = request.episode,
                episodeTitle = request.episodeTitle,
            )
        }

        val resume = resolveResumePosition(request, container)
        if (request.source != null) {
            start(request.source, resume)
            return@LaunchedEffect
        }

        val result = withContext(Dispatchers.IO) {
            container.streams.resolve(
                media = detail?.toMediaItem() ?: request.media,
                detail = detail,
                season = request.season,
                episode = request.episode,
                includeAdult = includeAdult,
            )
        }
        candidates = result.sources
        sourceFailures = result.failures.map { "${it.addon}: ${it.message}" }
        val best = pickBest(result.sources, settings.preferredQuality, settings.autoPlayBest)
        if (best != null) {
            start(best, resume)
        } else {
            resolving = false
            error = if (result.sources.isEmpty()) {
                "No streams were found for this title. Install a stream add-on in Settings → Add-ons."
            } else {
                null
            }
            showSources = result.sources.isNotEmpty()
        }
    }

    // 2) Boot libmpv once a source is chosen.
    LaunchedEffect(playback?.source?.url) {
        val request = playback ?: return@LaunchedEffect
        val url = request.source.url ?: return@LaunchedEffect
        startupMessage = "Starting libmpv…"
        val opened = withContext(Dispatchers.IO) {
            runCatching { MpvPlayer.open(settings) }
        }
        val result = opened.getOrElse {
            error = it.message ?: "Could not start the player."
            resolving = false
            return@LaunchedEffect
        }
        val instance = result.getOrElse {
            error = it.message ?: "Could not start libmpv."
            resolving = false
            return@LaunchedEffect
        }
        if (!instance.awaitReady()) {
            error = "libmpv could not create its renderer. Try a different libmpv build in Settings → System."
            instance.close()
            resolving = false
            return@LaunchedEffect
        }
        player = instance
        instance.setVolume(settings.playerVolume)
        instance.load(url, request.source.headers, request.startPositionMs)
        resolving = false
    }

    // 3) Poll playback state + persist progress.
    LaunchedEffect(player) {
        val instance = player ?: return@LaunchedEffect
        while (isActive) {
            withContext(Dispatchers.IO) { runCatching { instance.poll() } }
            delay(450)
        }
    }

    val snapshotFlow: kotlinx.coroutines.flow.Flow<MpvSnapshot> =
        player?.snapshot ?: kotlinx.coroutines.flow.emptyFlow<MpvSnapshot>()
    val snapshot by snapshotFlow.collectAsState(initial = MpvSnapshot())

    val currentMedia = playback?.media ?: target?.media
    val latestSnapshot = androidx.compose.runtime.rememberUpdatedState(snapshot)
    val latestPlayback = androidx.compose.runtime.rememberUpdatedState(playback)
    val latestPlayer = androidx.compose.runtime.rememberUpdatedState(player)

    // Persist every 5 seconds while playing, and once more on exit.
    LaunchedEffect(player, playback) {
        if (playback == null) return@LaunchedEffect
        while (isActive) {
            delay(5_000)
            val current = player?.snapshot?.value ?: continue
            persistProgress(container, playback!!, current)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            val request = latestPlayback.value ?: return@onDispose
            val current = latestPlayer.value?.snapshot?.value ?: latestSnapshot.value
            persistProgress(container, request, current)
        }
    }

    fun leave() {
        playback?.let { persistProgress(container, it, player?.snapshot?.value ?: snapshot) }
        app.closePlayer()
        onExit()
    }

    if (target == null) {
        LaunchedEffect(Unit) { onExit() }
        return
    }

    val focusRequester = remember { FocusRequester() }
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteraction by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var scrubbing by remember { mutableStateOf(false) }
    var scrubValue by remember { mutableFloatStateOf(0f) }
    var audioMenu by remember { mutableStateOf(false) }
    var subMenu by remember { mutableStateOf(false) }
    var speedMenu by remember { mutableStateOf(false) }

    fun poke() {
        controlsVisible = true
        lastInteraction = System.currentTimeMillis()
    }

    LaunchedEffect(player, snapshot.paused, controlsVisible, lastInteraction) {
        if (!controlsVisible) return@LaunchedEffect
        val paused = snapshot.paused
        delay(3_000)
        if (!paused && System.currentTimeMillis() - lastInteraction >= 2_900) {
            controlsVisible = false
        }
    }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val duration = snapshot.durationMs.coerceAtLeast(0)
    val position = if (scrubbing) (scrubValue * duration).toLong() else snapshot.positionMs

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Spacebar -> {
                        player?.togglePause(); poke(); true
                    }
                    Key.DirectionLeft -> {
                        player?.seekBy(-10_000); poke(); true
                    }
                    Key.DirectionRight -> {
                        player?.seekBy(10_000); poke(); true
                    }
                    Key.DirectionUp -> {
                        val v = (player?.snapshot?.value?.volume ?: 100) + 5
                        player?.setVolume(v); poke(); true
                    }
                    Key.DirectionDown -> {
                        val v = (player?.snapshot?.value?.volume ?: 100) - 5
                        player?.setVolume(v); poke(); true
                    }
                    Key.F -> {
                        app.updateFullscreen(!app.fullscreen); poke(); true
                    }
                    Key.M -> {
                        player?.toggleMute(); poke(); true
                    }
                    Key.Escape -> {
                        if (app.fullscreen) {
                            app.updateFullscreen(false)
                        } else {
                            leave()
                        }
                        true
                    }
                    else -> false
                }
            }
            .focusRequester(focusRequester)
            .focusable()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Move || event.type == PointerEventType.Enter) {
                            controlsVisible = true
                            lastInteraction = System.currentTimeMillis()
                        }
                    }
                }
            },
    ) {
        // Video surface
        BoxWithConstraintsVideo(
            snapshot = snapshot,
            player = player,
        )

        // Click-to-toggle controls (ignore clicks on the control bars: they sit above this).
        Box(
            Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) {
                    controlsVisible = !controlsVisible
                    poke()
                },
        )

        // Startup / loading
        if (resolving) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LoadingIndicator(Modifier.size(44.dp))
                    Spacer(Modifier.height(14.dp))
                    Text(startupMessage, style = MaterialTheme.typography.bodyMedium, color = Palette.textSecondary)
                }
            }
        }

        error?.let { message ->
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    Modifier
                        .widthIn(max = 520.dp)
                        .glass(RoundedCornerShape(20.dp), alpha = 0.08f)
                        .background(Palette.surface.copy(alpha = 0.9f), RoundedCornerShape(20.dp))
                        .padding(30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(Icons.Rounded.ErrorOutline, null, Modifier.size(34.dp), tint = Palette.warning)
                    Spacer(Modifier.height(14.dp))
                    Text("Playback unavailable", style = MaterialTheme.typography.headlineMedium, color = Palette.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        message,
                        style = MaterialTheme.typography.bodySmall,
                        color = Palette.textTertiary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("Back", onClick = { leave() })
                        if (candidates.isNotEmpty()) {
                            GhostButton("Choose a source", onClick = {
                                error = null
                                showSources = true
                            })
                        }
                    }
                }
            }
        }

        if (snapshot.buffering && !resolving) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                LoadingIndicator(Modifier.size(38.dp))
            }
        }

        // Top bar
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { -it / 3 },
            exit = fadeOut() + slideOutVertically { -it / 3 },
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.82f), Color.Transparent),
                        ),
                    )
                    .padding(horizontal = 26.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircleIconButton(Icons.Rounded.ArrowBack, "Back", onClick = { leave() }, size = 40.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        playback?.episodeTitle ?: currentMedia?.name ?: "",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                    )
                    val subtitle = buildString {
                        playback?.let { req ->
                            if (req.season != null && req.episode != null) {
                                append("S").append(req.season).append(" · E").append(req.episode).append("   ")
                            }
                            append(req.source.addonName)
                            req.source.quality?.let { append(" · ").append(it) }
                        }
                    }.ifBlank { currentMedia?.type?.label.orEmpty() }
                    if (subtitle.isNotBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.labelSmall,
                            color = Palette.textSecondary,
                            maxLines = 1,
                        )
                    }
                }
                if (settings.autoPlayBest && snapshot.videoHeight > 0) {
                    Text(
                        "${snapshot.videoWidth}×${snapshot.videoHeight}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Palette.textTertiary,
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
                CircleIconButton(
                    Icons.Rounded.HighQuality,
                    "Sources",
                    onClick = { showSources = true },
                    size = 40.dp,
                )
            }
        }

        // Bottom controls
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn() + slideInVertically { it / 3 },
            exit = fadeOut() + slideOutVertically { it / 3 },
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                        ),
                    )
                    .padding(start = 30.dp, end = 30.dp, top = 46.dp, bottom = 20.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        formatDuration(position),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.width(12.dp))
                    Slider(
                        value = if (duration > 0) (if (scrubbing) scrubValue else (position.toFloat() / duration)).coerceIn(0f, 1f) else 0f,
                        onValueChange = {
                            scrubbing = true
                            scrubValue = it
                            poke()
                        },
                        onValueChangeFinished = {
                            player?.seekTo((scrubValue * duration).toLong())
                            scrubbing = false
                            poke()
                        },
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = AccentColor,
                            activeTrackColor = AccentColor,
                            inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                        ),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        formatDuration(duration),
                        style = MaterialTheme.typography.labelMedium,
                        color = Palette.textSecondary,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleIconButton(
                        icon = if (snapshot.paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = "Play/Pause",
                        onClick = {
                            player?.togglePause()
                            poke()
                        },
                        size = 44.dp,
                        filled = true,
                    )
                    Spacer(Modifier.width(18.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            if (snapshot.muted || snapshot.volume == 0) Icons.Rounded.VolumeOff else Icons.Rounded.VolumeUp,
                            "Mute",
                            Modifier.size(19.dp).clickable {
                                player?.toggleMute(); poke()
                            },
                            tint = Color.White,
                        )
                        Slider(
                            value = if (snapshot.muted) 0f else snapshot.volume / 100f,
                            onValueChange = {
                                player?.setVolume((it * 100).toInt())
                                poke()
                            },
                            modifier = Modifier.width(92.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.22f),
                            ),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TrackMenu(
                        icon = Icons.Rounded.Audiotrack,
                        contentDescription = "Audio tracks",
                        expanded = audioMenu,
                        onExpandedChange = { audioMenu = it },
                        tracks = snapshot.audioTracks,
                        selectedId = snapshot.audioId,
                        emptyLabel = "No audio tracks",
                        onSelect = { player?.selectAudio(it); poke() },
                    )
                    Spacer(Modifier.width(4.dp))
                    TrackMenu(
                        icon = Icons.Rounded.ClosedCaption,
                        contentDescription = "Subtitles",
                        expanded = subMenu,
                        onExpandedChange = { subMenu = it },
                        tracks = snapshot.subtitleTracks,
                        selectedId = snapshot.subtitleId,
                        emptyLabel = "No subtitles",
                        includeOff = true,
                        onSelect = { player?.selectSubtitle(it); poke() },
                    )
                    Spacer(Modifier.width(4.dp))
                    Box {
                        CircleIconButton(
                            Icons.Rounded.PlayArrow,
                            "Speed",
                            onClick = { speedMenu = true },
                            size = 38.dp,
                        )
                        DropdownMenu(expanded = speedMenu, onDismissRequest = { speedMenu = false }) {
                            listOf(0.75, 1.0, 1.25, 1.5, 2.0).forEach { speed ->
                                DropdownMenuItem(
                                    text = { Text("${speed}x") },
                                    onClick = {
                                        player?.setSpeed(speed)
                                        speedMenu = false
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.width(4.dp))
                    CircleIconButton(
                        if (app.fullscreen) Icons.Rounded.FullscreenExit else Icons.Rounded.Fullscreen,
                        "Fullscreen",
                        onClick = { app.updateFullscreen(!app.fullscreen); poke() },
                        size = 38.dp,
                    )
                }
            }
        }

        // Ended: next episode / replay
        if (snapshot.ended) {
            Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.72f)),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Finished",
                        style = MaterialTheme.typography.displayMedium,
                        color = Color.White,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        currentMedia?.name.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Palette.textSecondary,
                    )
                    Spacer(Modifier.height(22.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrimaryButton("Replay", Icons.Rounded.PlayArrow, onClick = {
                            player?.seekTo(0)
                            player?.play()
                        })
                        GhostButton("Back to details", onClick = { leave() })
                    }
                }
            }
        }

        if (showSources) {
            SourcePicker(
                sources = candidates,
                failures = sourceFailures,
                current = playback?.source,
                onSelect = { source ->
                    showSources = false
                    error = null
                    resolving = true
                    playback = playback?.copy(source = source, startPositionMs = 0)
                },
                onDismiss = { showSources = false },
                onOpenSettings = {
                    app.closePlayer()
                    onExit()
                    app.navigateTab(com.moovio.desktop.ui.nav.Screen.Settings)
                },
            )
        }
    }
}

@Composable
private fun BoxWithConstraintsVideo(
    snapshot: MpvSnapshot,
    player: MpvPlayer?,
) {
    Box(Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.BoxWithConstraints(Modifier.fillMaxSize()) {
            val density = LocalDensity.current
            val maxWidthPx = with(density) { maxWidth.toPx() }.toInt()
            val maxHeightPx = with(density) { maxHeight.toPx() }.toInt()
            val aspect = if (snapshot.videoWidth > 0 && snapshot.videoHeight > 0) {
                snapshot.videoWidth.toFloat() / snapshot.videoHeight
            } else {
                16f / 9f
            }
            val capW = min(maxWidthPx, 1600)
            val capH = min(maxHeightPx, 900)
            var renderW = capW
            var renderH = (capW / aspect).toInt()
            if (renderH > capH) {
                renderH = capH
                renderW = (capH * aspect).toInt()
            }
            renderW = max(2, renderW - (renderW % 2))
            renderH = max(2, renderH - (renderH % 2))

            LaunchedEffect(player, renderW, renderH) {
                player?.setRenderSize(renderW, renderH)
            }

            val frameState = player?.frame
            Canvas(Modifier.fillMaxSize()) {
                val frame = frameState?.value ?: return@Canvas
                val srcAspect = frame.width.toFloat() / frame.height
                val boxAspect = size.width / size.height
                val dstWidth: Float
                val dstHeight: Float
                if (boxAspect > srcAspect) {
                    dstHeight = size.height
                    dstWidth = dstHeight * srcAspect
                } else {
                    dstWidth = size.width
                    dstHeight = dstWidth / srcAspect
                }
                drawImage(
                    image = frame.bitmap,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(frame.width, frame.height),
                    dstOffset = IntOffset(
                        ((size.width - dstWidth) / 2f).toInt(),
                        ((size.height - dstHeight) / 2f).toInt(),
                    ),
                    dstSize = IntSize(dstWidth.toInt(), dstHeight.toInt()),
                )
            }
        }
    }
}

@Composable
private fun TrackMenu(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    tracks: List<MpvTrack>,
    selectedId: Int?,
    emptyLabel: String,
    includeOff: Boolean = false,
    onSelect: (Int?) -> Unit,
) {
    Box {
        CircleIconButton(icon, contentDescription, onClick = { onExpandedChange(true) }, size = 38.dp)
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            if (includeOff) {
                DropdownMenuItem(
                    text = { Text("Off", color = if (selectedId == null) AccentColor else Palette.textPrimary) },
                    onClick = { onSelect(null) },
                )
            }
            if (tracks.isEmpty()) {
                DropdownMenuItem(
                    text = { Text(emptyLabel, color = Palette.textTertiary) },
                    onClick = { onExpandedChange(false) },
                )
            }
            tracks.forEach { track ->
                DropdownMenuItem(
                    text = {
                        Text(
                            buildString {
                                append(track.title.ifBlank { track.language.ifBlank { "Track ${track.id}" } })
                                if (track.language.isNotBlank() && track.title.isNotBlank()) append(" · ${track.language}")
                                if (track.codec.isNotBlank()) append("  (${track.codec})")
                            },
                            color = if (track.selected) AccentColor else Palette.textPrimary,
                        )
                    },
                    onClick = { onSelect(track.id) },
                )
            }
        }
    }
}

@Composable
private fun SourcePicker(
    sources: List<StreamSource>,
    failures: List<String>,
    current: StreamSource?,
    onSelect: (StreamSource) -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.8f)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .widthIn(max = 680.dp)
                .fillMaxWidth(0.62f)
                .glass(RoundedCornerShape(20.dp), alpha = 0.08f)
                .background(Palette.surface.copy(alpha = 0.94f), RoundedCornerShape(20.dp))
                .clickable(enabled = false) {}
                .padding(24.dp),
        ) {
            Text("Sources", style = MaterialTheme.typography.headlineMedium, color = Palette.textPrimary)
            Text(
                "Quality, add-on and swarm size — only direct HTTP/HLS streams play in v1.",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.textTertiary,
            )
            Spacer(Modifier.height(16.dp))
            Column(Modifier.height(360.dp).fillMaxWidth()) {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(sources, key = { it.fingerprint + it.addonId }) { source ->
                        val playable = source.isPlayable
                        val active = source.fingerprint == current?.fingerprint
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(11.dp))
                                .background(
                                    if (active) AccentColor.copy(alpha = 0.14f)
                                    else Color.White.copy(alpha = 0.04f),
                                )
                                .clickable(enabled = playable) { onSelect(source) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    source.displayTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (playable) Palette.textPrimary else Palette.textTertiary,
                                    maxLines = 2,
                                )
                                Text(
                                    buildString {
                                        append(source.addonName)
                                        source.quality?.let { append("  ·  ").append(it) }
                                        source.seeders?.let { append("  ·  ").append(it).append(" seeders") }
                                        if (source.isTorrent) append("  ·  torrent")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Palette.textTertiary,
                                )
                            }
                            if (active) {
                                Icon(Icons.Rounded.LockOpen, null, Modifier.size(15.dp), tint = AccentColor)
                            } else if (playable) {
                                Icon(Icons.Rounded.PlayArrow, null, Modifier.size(17.dp), tint = AccentColor)
                            } else {
                                Text("n/a", style = MaterialTheme.typography.labelSmall, color = Palette.textTertiary)
                            }
                        }
                    }
                    if (sources.isEmpty()) {
                        item {
                            Text(
                                "No streams returned. Install a stream add-on to get started.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Palette.textTertiary,
                                modifier = Modifier.padding(vertical = 20.dp),
                            )
                        }
                    }
                    if (failures.isNotEmpty()) {
                        item {
                            Text(
                                failures.joinToString("\n"),
                                style = MaterialTheme.typography.labelSmall,
                                color = Palette.warning,
                                modifier = Modifier.padding(top = 6.dp),
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Add-ons", onClick = onOpenSettings)
                GhostButton("Close", onClick = onDismiss)
            }
        }
    }
}

private fun pickBest(
    sources: List<StreamSource>,
    preferredQuality: String,
    autoPlay: Boolean,
): StreamSource? {
    val playable = StreamRanking.rank(sources).filter { it.isPlayable }
    if (playable.isEmpty()) return null
    if (!autoPlay) return playable.first()
    val target = when (preferredQuality) {
        "2160p" -> 2160
        "1080p" -> 1080
        "720p" -> 720
        else -> 0
    }
    if (target == 0) return playable.first()
    return playable
        .filter { StreamRanking.qualityRank(it.quality) in 1..target }
        .maxByOrNull { StreamRanking.qualityRank(it.quality) }
        ?: playable.first()
}

private fun resolveResumePosition(
    target: com.moovio.desktop.domain.model.PlayTarget,
    container: AppContainer,
): Long {
    if (target.startPositionMs > 0) return target.startPositionMs
    if (!container.settings.current.autoResume) return 0
    val saved = container.library.progressFor(target.media.key) ?: return 0
    if (saved.finished) return 0
    if (target.season != null && saved.season != null && target.season != saved.season) return 0
    if (target.episode != null && saved.episode != null && target.episode != saved.episode) return 0
    return saved.positionMs
}

private fun persistProgress(
    container: AppContainer,
    request: PlaybackRequest,
    snapshot: MpvSnapshot,
) {
    if (snapshot.durationMs <= 0 || snapshot.positionMs <= 0) return
    val position = if (snapshot.ended) snapshot.durationMs else snapshot.positionMs
    container.library.recordProgress(
        WatchProgress(
            mediaKey = request.media.key,
            mediaId = request.media.id,
            type = request.media.type,
            name = request.media.name,
            poster = request.media.poster,
            background = request.media.background,
            season = request.season,
            episode = request.episode,
            episodeTitle = request.episodeTitle,
            positionMs = position,
            durationMs = snapshot.durationMs,
            updatedAt = System.currentTimeMillis(),
            adult = request.media.adult,
        ),
    )
}
