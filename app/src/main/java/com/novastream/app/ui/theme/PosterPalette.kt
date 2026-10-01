package com.novastream.app.ui.theme

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.novastream.app.data.local.MetadataCache
import com.novastream.app.data.remote.Http
import com.novastream.app.ui.vm.LocalContainer

/**
 * A small, serializable slice of a [Palette] derived from poster/backdrop artwork.
 *
 * Stored as raw ARGB ints so it can be cached on disk (see [PosterPaletteExtractor]) and turned
 * into Compose [Color]s on demand. Kept dependency-light so the theme layer never needs the
 * full `Palette` object at call sites.
 */
data class PosterPalette(
    val vibrant: Int,
    val darkVibrant: Int,
    val muted: Int,
    val darkMuted: Int,
    val dominant: Int,
    val onVibrant: Int,
) {
    val vibrantColor: Color get() = Color(vibrant)
    val darkVibrantColor: Color get() = Color(darkVibrant)
    val mutedColor: Color get() = Color(muted)
    val darkMutedColor: Color get() = Color(darkMuted)
    val dominantColor: Color get() = Color(dominant)
    val onVibrantColor: Color get() = Color(onVibrant)

    companion object {
        val Fallback = PosterPalette(
            vibrant = 0xFF7C5CFF.toInt(),
            darkVibrant = 0xFF1A1A25.toInt(),
            muted = 0xFF2A2A38.toInt(),
            darkMuted = 0xFF12121A.toInt(),
            dominant = 0xFF12121A.toInt(),
            onVibrant = 0xFFFFFFFF.toInt(),
        )
    }
}

/**
 * Extracts a [PosterPalette] from artwork loaded through Coil and caches the resulting RGB values
 * in the shared [MetadataCache] so re-visiting a title never re-runs extraction.
 */
object PosterPaletteExtractor {

    /** Palettes rarely change; a month-long TTL is effectively permanent while still expiring. */
    private const val TTL_MS = 30L * 24 * 60 * 60 * 1000

    /** Downscale target fed to Palette — extraction only needs broad color regions. */
    private const val SAMPLE_SIZE = 160

    suspend fun palette(context: Context, cache: MetadataCache, url: String): PosterPalette? {
        if (url.isBlank()) return null
        val key = "palette:$url"
        decode(cache.get(key, TTL_MS))?.let { return it }

        val bitmap = loadBitmap(context, url) ?: return null
        val generated = Palette.from(bitmap)
            .clearFilters()
            .maximumColorCount(24)
            .generate()
        val result = fromPalette(generated)
        cache.put(key, encode(result))
        return result
    }

    /** Pull the useful swatches out of a generated [Palette], degrading gracefully when sparse. */
    fun fromPalette(p: Palette): PosterPalette {
        val vibrant = p.vibrantSwatch
        val darkVibrant = p.darkVibrantSwatch
        val muted = p.mutedSwatch
        val darkMuted = p.darkMutedSwatch
        val dominant = p.dominantSwatch
        val base = vibrant?.rgb ?: dominant?.rgb ?: PosterPalette.Fallback.vibrant
        return PosterPalette(
            vibrant = base,
            darkVibrant = darkVibrant?.rgb ?: vibrant?.rgb ?: PosterPalette.Fallback.darkVibrant,
            muted = muted?.rgb ?: dominant?.rgb ?: PosterPalette.Fallback.muted,
            darkMuted = darkMuted?.rgb ?: PosterPalette.Fallback.darkMuted,
            dominant = dominant?.rgb ?: PosterPalette.Fallback.dominant,
            onVibrant = vibrant?.titleTextColor ?: PosterPalette.Fallback.onVibrant,
        )
    }

    private suspend fun loadBitmap(context: Context, url: String): Bitmap? = runCatching {
        val request = ImageRequest.Builder(context)
            .data(url)
            .allowHardware(false)
            .size(SAMPLE_SIZE)
            .build()
        val result = context.imageLoader.execute(request)
        (result as? SuccessResult)?.drawable?.toBitmap()
    }.getOrNull()

    internal fun encode(p: PosterPalette): String = Http.gson.toJson(p)

    internal fun decode(json: String?): PosterPalette? =
        if (json.isNullOrBlank()) null
        else runCatching { Http.gson.fromJson(json, PosterPalette::class.java) }.getOrNull()
}

/**
 * Compose helper: asynchronously derive the palette for [url], returning null until it resolves.
 * Keyed on the URL so switching titles re-extracts, and extraction is cached across recompositions.
 */
@Composable
fun rememberPosterPalette(url: String?): PosterPalette? {
    val context = LocalContext.current
    val cache = LocalContainer.current.cache
    var palette by remember(url) { mutableStateOf<PosterPalette?>(null) }
    LaunchedEffect(url) {
        palette = if (url.isNullOrBlank()) null
        else runCatching { PosterPaletteExtractor.palette(context, cache, url) }.getOrNull()
    }
    return palette
}
