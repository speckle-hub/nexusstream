package com.moovio.desktop.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.Icons
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.imageio.ImageIO
import java.net.URL

/**
 * Network image for desktop, decoded with ImageIO into a Compose ImageBitmap.
 * No external image-loading library needed for v1 posters/backdrops.
 */
@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentDescription: String = "Poster",
    shape: androidx.compose.foundation.shape.Shape = RoundedCornerShape(8.dp),
    fallbackColor: Color = Color(0xFF181A24),
) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(url) {
        image = null
        if (url.isNotBlank()) {
            try {
                val awt = withContext(Dispatchers.Default) { ImageIO.read(URL(url)) }
                if (awt != null) {
                    image = awt.toImageBitmap()
                }
            } catch (e: Exception) {
                // leave image null → fallback box
            }
        }
    }

    Box(
        modifier = modifier.size(120.dp, 180.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (image != null) {
            Box(
                modifier = Modifier.fillMaxSize().clip(shape),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = BitmapPainter(image!!),
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(fallbackColor).clip(shape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Movie,
                    contentDescription = null,
                    tint = Color(0xFF3A3F4C),
                )
            }
        }
    }
}

