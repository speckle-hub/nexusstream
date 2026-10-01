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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import coil.compose.AsyncImage
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.Video
import com.novastream.app.data.remote.MangaDexClient
import com.novastream.app.ui.theme.NovaStreamTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Full-screen manga chapter reader: horizontal pager, pinch-to-zoom, and a
 * brightness/dim overlay for comfortable reading.
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

        setContent {
            NovaStreamTheme(darkTheme = true, accentKey = "violet") {
                ReaderScreen(
                    title = item.title,
                    chapterTitle = chapter.title ?: "Chapter",
                    chapterId = chapter.id,
                    onBack = { finish() },
                )
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
    title: String,
    chapterTitle: String,
    chapterId: String,
    onBack: () -> Unit,
) {
    var pages by remember { mutableStateOf<List<String>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var dim by remember { mutableStateOf(0f) }

    LaunchedEffect(chapterId) {
        loading = true
        error = null
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
                    Button(onClick = { pages = emptyList(); loading = true; error = null }) { Text("Retry") }
                }
            }
            pages.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No pages available", color = Color.White.copy(alpha = 0.7f))
            }
            else -> {
                val pagerState = rememberPagerState(pageCount = { pages.size })
                HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    ZoomablePage(pages[page])
                }
                // Progress
                Surface(
                    color = Color(0xCC000000),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.align(Alignment.BottomCenter).padding(20.dp),
                ) {
                    Text(
                        "${pagerState.currentPage + 1} / ${pages.size}",
                        color = Color.White, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                    )
                }
            }
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
