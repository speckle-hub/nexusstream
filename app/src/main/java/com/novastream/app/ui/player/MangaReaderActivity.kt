@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.novastream.app.ui.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ViewStream
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.novastream.app.NovaApp
import com.novastream.app.data.download.DownloadStatus
import com.novastream.app.data.download.MangaDownload
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.Video
import com.novastream.app.data.model.WatchEntry
import com.novastream.app.data.remote.MangaDexClient
import com.novastream.app.ui.theme.NovaStreamTheme
import com.novastream.app.ui.vm.LocalContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** How pages are laid out. */
private enum class ReadingMode(val label: String) {
    LTR("Left \u2192 Right"),
    RTL("Right \u2192 Left"),
    WEBTOON("Webtoon"),
}

/**
 * Full-screen manga chapter reader. Supports three reading modes — Webtoon (vertical scroll),
 * Right-to-Left (manga) and Left-to-Right (western comic) — with pinch-to-zoom, page preloading,
 * and a brightness/dim overlay.
 */
class MangaReaderActivity : ComponentActivity() {

    private lateinit var item: MediaItem
    private lateinit var chapter: Video

    /** Volume-key page turns: -1 = previous, +1 = next. The active reader collects these. */
    private val pageCommands = MutableSharedFlow<Int>(extraBufferCapacity = 8)
    private var volumeKeysEnabled = true

    /** True once the chapter's pages are loaded and shown — volume keys only act then. */
    private var readerReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        item = readItem(intent) ?: run { finish(); return }
        chapter = readChapter(intent) ?: run { finish(); return }
        val app = application as NovaApp
        val container = app.container
        val appScope = app.appScope
        volumeKeysEnabled = container.settings.mangaVolumeKeys.value

        setContent {
            // Use the app's real theme engine here too, so Material You / custom accent / AMOLED
            // black apply inside the reader instead of a hardcoded dark-violet theme.
            val themeMode by container.settings.theme.collectAsStateWithLifecycle("dark")
            val accent by container.settings.accent.collectAsStateWithLifecycle("violet")
            val dynamicColor by container.settings.dynamicColor.collectAsStateWithLifecycle(false)
            val amoledBlack by container.settings.amoledBlack.collectAsStateWithLifecycle(false)
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
                androidx.compose.runtime.CompositionLocalProvider(LocalContainer provides container) {
                    ReaderScreen(
                        mangaId = item.id,
                        title = item.title,
                        chapterTitle = chapter.title ?: "Chapter",
                        chapterId = chapter.id,
                        nsfw = item.type == com.novastream.app.data.model.MediaType.NSFW_MANGA,
                        pageCommands = pageCommands,
                        onReady = { readerReady = it },
                        onBack = { finish() },
                        // Reading progress (page / total pages) is written into the shared history
                        // list so Home and Library can offer a "Continue Reading" row — the same
                        // store the player uses for Continue Watching, gated on incognito there.
                        onProgress = { page, total ->
                            if (total > 0) {
                                val entry = WatchEntry(
                                    item = item,
                                    videoId = chapter.id,
                                    videoTitle = chapter.title ?: "Chapter",
                                    positionMs = (page + 1).coerceIn(0, total).toLong(),
                                    durationMs = total.toLong(),
                                )
                                appScope.launch { container.libraryStore.recordWatch(entry) }
                            }
                        },
                    )
                }
            }
        }
    }

    /** Intercept the volume rocker for page turns (when enabled in Settings). */
    @Suppress("DEPRECATION")
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Only page-turn once the chapter is actually loaded and shown; while it is loading or in an
        // error/empty state the keys must keep their normal volume behaviour.
        if (volumeKeysEnabled && readerReady) {
            when (keyCode) {
                KeyEvent.KEYCODE_VOLUME_DOWN -> { pageCommands.tryEmit(1); return true }
                KeyEvent.KEYCODE_VOLUME_UP -> { pageCommands.tryEmit(-1); return true }
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    companion object {
        private const val EXT_ITEM = "r_item"
        private const val EXT_CH = "r_ch"

        fun intent(context: Context, item: MediaItem, chapter: Video): Intent {
            val i = Intent(context, MangaReaderActivity::class.java)
            i.putExtra(EXT_ITEM, com.google.gson.Gson().toJson(item))
            i.putExtra(EXT_CH, com.google.gson.Gson().toJson(chapter))
            return i
        }

        private fun readItem(i: Intent): MediaItem? = runCatching {
            com.google.gson.Gson().fromJson(i.getStringExtra(EXT_ITEM), MediaItem::class.java)
        }.getOrNull()

        private fun readChapter(i: Intent): Video? = runCatching {
            com.google.gson.Gson().fromJson(i.getStringExtra(EXT_CH), Video::class.java)
        }.getOrNull()
    }
}

@Composable
private fun ReaderScreen(
    mangaId: String,
    title: String,
    chapterTitle: String,
    chapterId: String,
    nsfw: Boolean = false,
    pageCommands: SharedFlow<Int>,
    onReady: (Boolean) -> Unit,
    onBack: () -> Unit,
    onProgress: (page: Int, total: Int) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val manager = LocalContainer.current.mangaDownloadManager
    val scope = rememberCoroutineScope()
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dim by remember { mutableStateOf(0f) }
    var mode by remember { mutableStateOf(ReadingMode.RTL) }
    // Bumped by the Retry button so the load effect actually re-runs for the same chapter.
    var loadAttempt by remember { mutableIntStateOf(0) }
    val download by remember(mangaId, chapterId) {
        manager.downloads
            .map { it["$mangaId::$chapterId"] }
            .distinctUntilChanged()
    }.collectAsStateWithLifecycle(manager.downloads.value["$mangaId::$chapterId"])
    val settings = LocalContainer.current.settings
    val invert by settings.mangaInvert.collectAsStateWithLifecycle()
    val grayscale by settings.mangaGrayscale.collectAsStateWithLifecycle()
    val warm by settings.mangaWarm.collectAsStateWithLifecycle()
    val crop by settings.mangaCrop.collectAsStateWithLifecycle()
    val doublePage by settings.mangaDoublePage.collectAsStateWithLifecycle()
    val zoomLock by settings.mangaZoomLock.collectAsStateWithLifecycle()
    // Saved reading position for this chapter (restored once when the pages land).
    var initialPage by remember { mutableIntStateOf(0) }
    LaunchedEffect(chapterId) {
        initialPage = runCatching { settings.mangaProgress("$mangaId::$chapterId") }.getOrNull() ?: 0
    }
    val savePage: (Int) -> Unit = { p ->
        scope.launch { settings.setMangaProgress("$mangaId::$chapterId", p) }
        // Keep the shared history in step too, so the reading position is resume-able from
        // Home/Library and not only from the reader's own per-chapter store.
        onProgress(p, pages.size)
    }

    LaunchedEffect(chapterId, loadAttempt) {
        loading = true
        error = null
        // Offline-first: render straight from disk when the chapter has been downloaded.
        val local = manager.localPages(mangaId, chapterId)
        if (local != null) {
            pages = local.map { it.toURI().toString() }
            loading = false
            return@LaunchedEffect
        }
        val res = withContext(Dispatchers.IO) {
            runCatching { MangaDexClient.chapterPages(chapterId) }
        }
        res.onSuccess { pages = it }
            .onFailure { error = it.message ?: "Failed to load pages" }
        loading = false
    }

    // Tell the Activity when the reader can actually turn pages, so it only claims the volume keys
    // then (see `MangaReaderActivity.onKeyDown`).
    LaunchedEffect(loading, error, pages) {
        onReady(!loading && error == null && pages.isNotEmpty())
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = Color(0xFF7C5CFF))
            }
            error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error!!, color = Color.White)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { pages = emptyList(); loadAttempt++ }) { Text("Retry") }
                }
            }
            pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No pages available", color = Color.White.copy(alpha = 0.7f))
            }
            mode == ReadingMode.WEBTOON -> WebtoonReader(
                pages = pages,
                context = context,
                invert = invert,
                grayscale = grayscale,
                warm = warm,
                crop = crop,
                pageCommands = pageCommands,
                initialPage = initialPage,
                onPage = savePage,
            )
            else -> PagedReader(
                pages = pages,
                mode = mode,
                invert = invert,
                grayscale = grayscale,
                warm = warm,
                crop = crop,
                doublePage = doublePage,
                zoomLock = zoomLock,
                pageCommands = pageCommands,
                initialPage = initialPage,
                onPage = savePage,
            )
        }

        // Top bar
        Row(
            Modifier.fillMaxWidth().padding(12.dp).align(Alignment.TopCenter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White) }
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(chapterTitle, color = Color.White.copy(alpha = 0.6f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AssistChip(
                onClick = {
                    mode = when (mode) {
                        ReadingMode.LTR -> ReadingMode.RTL
                        ReadingMode.RTL -> ReadingMode.WEBTOON
                        ReadingMode.WEBTOON -> ReadingMode.LTR
                    }
                },
                label = { Text(mode.label, color = Color.White, fontSize = 11.sp) },
                leadingIcon = {
                    Icon(
                        if (mode == ReadingMode.WEBTOON) Icons.Filled.ViewStream else Icons.AutoMirrored.Filled.MenuBook,
                        null, tint = Color.White, modifier = Modifier.size(16.dp),
                    )
                },
            )
            val downloaded = download?.status == DownloadStatus.COMPLETED
            IconButton(onClick = {
                if (downloaded) return@IconButton
                if (pages.isNotEmpty()) {
                    manager.enqueue(MangaDownload(mangaId, title, chapterId, chapterTitle, null, pages, nsfw = nsfw))
                } else {
                    scope.launch {
                        val fetched = withContext(Dispatchers.IO) {
                            runCatching { MangaDexClient.chapterPages(chapterId) }.getOrDefault(emptyList())
                        }
                        if (fetched.isNotEmpty()) {
                            manager.enqueue(MangaDownload(mangaId, title, chapterId, chapterTitle, null, fetched, nsfw = nsfw))
                        }
                    }
                }
            }) {
                Icon(
                    if (downloaded) Icons.Filled.DownloadDone else Icons.Filled.Download,
                    "Download chapter",
                    tint = if (downloaded) Color(0xFF35E0A1) else Color.White,
                )
            }
            IconButton(onClick = { dim = if (dim > 0f) 0f else 0.45f }) {
                Icon(Icons.Filled.Brightness6, "Dim", tint = Color.White)
            }
        }

        // Dim overlay (tap-through disabled)
        if (dim > 0f) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = dim)))
        }
    }
}

/**
 * Horizontal pager layout; RTL reverses direction and landscape can show two-page spreads.
 *
 * Also handles volume-key page turns, zoom-lock across pages, and precise position saving.
 */
@Composable
private fun PagedReader(
    pages: List<String>,
    mode: ReadingMode,
    invert: Boolean,
    grayscale: Boolean,
    warm: Boolean,
    crop: Boolean,
    doublePage: Boolean,
    zoomLock: Boolean,
    pageCommands: SharedFlow<Int>,
    initialPage: Int,
    onPage: (Int) -> Unit,
) {
    val isLandscape = LocalConfiguration.current.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val spread = doublePage && isLandscape
    val pageCount = if (spread) (pages.size + 1) / 2 else pages.size
    val pagerState = rememberPagerState(
        initialPage = initialPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
        pageCount = { pageCount.coerceAtLeast(1) },
    )
    // Shared zoom state so "lock zoom across pages" survives a page turn.
    val zoom = remember { MutableZoom() }
    if (!zoomLock) {
        LaunchedEffect(pagerState.currentPage) { zoom.reset() }
    }
    val filter = remember(invert, grayscale, warm) { mangaFilter(invert, grayscale, warm) }
    val haptic = LocalHapticFeedback.current
    var firstPage by remember { mutableStateOf(true) }

    // Volume keys / navigation commands.
    LaunchedEffect(pageCommands) {
        pageCommands.collect { dir ->
            val target = (pagerState.currentPage + dir).coerceIn(0, (pageCount - 1).coerceAtLeast(0))
            if (target != pagerState.currentPage) pagerState.animateScrollToPage(target)
        }
    }
    // Persist the reading position as pages turn, and click haptically on each turn (covers
    // swipe, tap-to-turn and the volume-key commands alike).
    LaunchedEffect(pagerState.currentPage) {
        onPage(if (spread) pagerState.currentPage * 2 else pagerState.currentPage)
        if (firstPage) firstPage = false else haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            // Compose three pages beyond the viewport so Coil preloads the next few pages.
            // (Renamed from `beyondBoundsPageCount` to `beyondViewportPageCount` in Compose 1.7.)
            beyondViewportPageCount = 3,
            reverseLayout = mode == ReadingMode.RTL,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            if (spread) {
                Row(Modifier.fillMaxSize()) {
                    val firstIdx = page * 2
                    val secondIdx = firstIdx + 1
                    // In right-to-left mode the lower page number sits on the right.
                    val leftIdx = if (mode == ReadingMode.RTL) secondIdx else firstIdx
                    val rightIdx = if (mode == ReadingMode.RTL) firstIdx else secondIdx
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        pages.getOrNull(leftIdx)?.let { ZoomablePage(it, zoom, filter, crop) }
                    }
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        pages.getOrNull(rightIdx)?.let { ZoomablePage(it, zoom, filter, crop) }
                    }
                }
            } else {
                ZoomablePage(pages[page], zoom, filter, crop)
            }
        }
        Surface(
            color = Color(0xCC000000),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
        ) {
            val label = if (spread) {
                val first = pagerState.currentPage * 2 + 1
                val second = (pagerState.currentPage * 2 + 2).coerceAtMost(pages.size)
                "$first\u2013$second / ${pages.size}"
            } else "${pagerState.currentPage + 1} / ${pages.size}"
            Text(
                label,
                color = Color.White, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

/**
 * Vertical continuous-scroll layout with explicit preloading of the next three pages, plus the
 * same night-mode / crop filters and volume-key navigation as the paged reader.
 */
@Composable
private fun WebtoonReader(
    pages: List<String>,
    context: Context,
    invert: Boolean,
    grayscale: Boolean,
    warm: Boolean,
    crop: Boolean,
    pageCommands: SharedFlow<Int>,
    initialPage: Int,
    onPage: (Int) -> Unit,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialPage.coerceAtLeast(0))
    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }
    val filter = remember(invert, grayscale, warm) { mangaFilter(invert, grayscale, warm) }
    val haptic = LocalHapticFeedback.current
    var firstPage by remember { mutableStateOf(true) }
    val cropMod = if (crop) Modifier.graphicsLayer(scaleX = 1.06f, scaleY = 1.06f, clip = true) else Modifier

    LaunchedEffect(firstVisible, pages) {
        val loader = context.imageLoader
        for (i in (firstVisible + 1)..(firstVisible + 3)) {
            pages.getOrNull(i)?.let { url ->
                runCatching { loader.enqueue(ImageRequest.Builder(context).data(url).build()) }
            }
        }
        onPage(firstVisible)
        if (firstPage) firstPage = false else haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Volume keys / navigation commands scroll by one page.
    LaunchedEffect(pageCommands) {
        pageCommands.collect { dir ->
            val target = (listState.firstVisibleItemIndex + dir).coerceIn(0, (pages.size - 1).coerceAtLeast(0))
            listState.animateScrollToItem(target)
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(pages.size) { i ->
                AsyncImage(
                    model = pages[i],
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    colorFilter = filter,
                    modifier = Modifier.fillMaxWidth().then(cropMod),
                )
            }
        }
        val total = pages.size
        if (total > 0) {
            Surface(
                color = Color(0xCC000000),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
            ) {
                Text(
                    "${firstVisible + 1} / $total",
                    color = Color.White, fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/**
 * Holds zoom across pages so "lock zoom" can keep the same scale when turning the page.
 * When the lock is off, [PagedReader] resets it on every page change.
 */
private class MutableZoom {
    var scale by mutableStateOf(1f)
    var offset by mutableStateOf(Offset.Zero)
    fun reset() { scale = 1f; offset = Offset.Zero }
}

/**
 * Night-mode / grayscale / warm-sepia colour filter built from the reader settings. `null` when
 * none is on, so Coil skips the extra render pass entirely.
 */
private fun mangaFilter(invert: Boolean, grayscale: Boolean, warm: Boolean): ColorFilter? {
    if (!invert && !grayscale && !warm) return null
    val cm = ColorMatrix()
    if (grayscale) cm.setToSaturation(0f)
    if (invert) {
        cm.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    -1f, 0f, 0f, 0f, 255f,
                    0f, -1f, 0f, 0f, 255f,
                    0f, 0f, -1f, 0f, 255f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
    }
    if (warm) {
        // Gentle sepia tint: warms the paper colour and softens blue light.
        cm.timesAssign(
            ColorMatrix(
                floatArrayOf(
                    0.42f, 0.78f, 0.18f, 0f, 10f,
                    0.36f, 0.70f, 0.16f, 0f, 4f,
                    0.28f, 0.55f, 0.13f, 0f, -6f,
                    0f, 0f, 0f, 1f, 0f,
                )
            )
        )
    }
    return ColorFilter.colorMatrix(cm)
}

@Composable
private fun ZoomablePage(url: String, zoom: MutableZoom, filter: ColorFilter?, crop: Boolean) {
    Box(
        Modifier.fillMaxSize()
            .clipToBounds()
            // Pinch-to-zoom without swallowing the pager's swipe.
            //
            // `detectTransformGestures` consumes every pan once it passes the touch-slop
            // threshold — including plain one-finger drags — so the parent `HorizontalPager`
            // saw its drag consumed and cancelled, which is why Right->Left / Left->Right never
            // advanced while Webtoon (no zoomable page) worked. This loop only claims events
            // that are actually a pinch (two+ pointers) or a pan of an already-zoomed image;
            // a single finger at 1x is left entirely to the pager.
            .pointerInput(zoom) {
                awaitEachGesture {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.none { it.pressed }) break
                        val pointers = event.changes.count { it.pressed }
                        val zoomChange = event.calculateZoom()
                        val pan = event.calculatePan()

                        if (pointers > 1) {
                            if (zoomChange != 1f) {
                                val next = (zoom.scale * zoomChange).coerceIn(1f, 4f)
                                zoom.scale = next
                                if (next <= 1f) zoom.offset = Offset.Zero
                                event.changes.forEach { change ->
                                    if (change.position != change.previousPosition) change.consume()
                                }
                            }
                        } else if (zoom.scale > 1f && pan != Offset.Zero) {
                            // Zoomed in: one finger pans the image instead of turning the page.
                            zoom.offset += pan
                            event.changes.forEach { change ->
                                if (change.position != change.previousPosition) change.consume()
                            }
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            colorFilter = filter,
            modifier = Modifier
                .fillMaxSize()
                // Auto white-border crop: scale up slightly and clip the layer.
                .graphicsLayer(
                    scaleX = zoom.scale * if (crop) 1.06f else 1f,
                    scaleY = zoom.scale * if (crop) 1.06f else 1f,
                    translationX = zoom.offset.x,
                    translationY = zoom.offset.y,
                    clip = crop,
                ),
        )
    }
}
