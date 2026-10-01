@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.novastream.app.ui.player

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.novastream.app.data.remote.MangaDexClient
import com.novastream.app.ui.theme.NovaStreamTheme
import com.novastream.app.ui.vm.LocalContainer
import kotlinx.coroutines.Dispatchers
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

        item = readItem(intent) ?: run { finish(); return }
        chapter = readChapter(intent) ?: run { finish(); return }
        val container = (application as NovaApp).container

        setContent {
            NovaStreamTheme(darkTheme = true, accentKey = "violet") {
                androidx.compose.runtime.CompositionLocalProvider(LocalContainer provides container) {
                    ReaderScreen(
                        mangaId = item.id,
                        title = item.title,
                        chapterTitle = chapter.title ?: "Chapter",
                        chapterId = chapter.id,
                        onBack = { finish() },
                    )
                }
            }
        }
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
    onBack: () -> Unit,
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
    val download = manager.downloads.collectAsState().value["$mangaId::$chapterId"]

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
            mode == ReadingMode.WEBTOON -> WebtoonReader(pages, context)
            else -> PagedReader(pages, mode)
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
                    manager.enqueue(MangaDownload(mangaId, title, chapterId, chapterTitle, null, pages))
                } else {
                    scope.launch {
                        val fetched = withContext(Dispatchers.IO) {
                            runCatching { MangaDexClient.chapterPages(chapterId) }.getOrDefault(emptyList())
                        }
                        if (fetched.isNotEmpty()) {
                            manager.enqueue(MangaDownload(mangaId, title, chapterId, chapterTitle, null, fetched))
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

/** Horizontal pager layout; RTL reverses direction. */
@Composable
private fun PagedReader(pages: List<String>, mode: ReadingMode) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    Box(Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            // Compose three pages beyond the viewport so Coil preloads the next few pages.
            beyondBoundsPageCount = 3,
            reverseLayout = mode == ReadingMode.RTL,
            modifier = Modifier.fillMaxSize(),
        ) { page ->
            ZoomablePage(pages[page])
        }
        Surface(
            color = Color(0xCC000000),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
        ) {
            Text(
                "${pagerState.currentPage + 1} / ${pages.size}",
                color = Color.White, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
            )
        }
    }
}

/** Vertical continuous-scroll layout with explicit preloading of the next three pages. */
@Composable
private fun WebtoonReader(pages: List<String>, context: Context) {
    val listState = rememberLazyListState()
    val firstVisible by remember { derivedStateOf { listState.firstVisibleItemIndex } }

    LaunchedEffect(firstVisible, pages) {
        val loader = context.imageLoader
        for (i in (firstVisible + 1)..(firstVisible + 3)) {
            pages.getOrNull(i)?.let { url ->
                runCatching { loader.enqueue(ImageRequest.Builder(context).data(url).build()) }
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize()) {
            items(pages.size) { i ->
                AsyncImage(
                    model = pages[i],
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxWidth(),
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

@Composable
private fun ZoomablePage(url: String) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    Box(
        Modifier.fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 4f)
                    if (scale > 1f) {
                        offsetX += pan.x
                        offsetY += pan.y
                    } else {
                        offsetX = 0f; offsetY = 0f
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().graphicsLayer(
                scaleX = scale, scaleY = scale,
                translationX = offsetX, translationY = offsetY,
            ),
        )
    }
}
