package com.novastream.app

import com.novastream.app.ui.theme.PosterPalette
import com.novastream.app.ui.theme.PosterPaletteExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Pure-JVM tests for the palette cache codec (the Palette library itself needs Android). */
class PaletteCacheTest {

    @Test
    fun `palette round-trips through json`() {
        val original = PosterPalette(
            vibrant = 0xFF7C5CFF.toInt(),
            darkVibrant = 0xFF1A1A25.toInt(),
            muted = 0xFF2A2A38.toInt(),
            darkMuted = 0xFF12121A.toInt(),
            dominant = 0xFF0A0A0F.toInt(),
            onVibrant = 0xFFFFFFFF.toInt(),
        )
        val decoded = PosterPaletteExtractor.decode(PosterPaletteExtractor.encode(original))
        assertEquals(original, decoded)
    }

    @Test
    fun `blank and malformed palette json decode to null`() {
        assertNull(PosterPaletteExtractor.decode(null))
        assertNull(PosterPaletteExtractor.decode(""))
        assertNull(PosterPaletteExtractor.decode("not json"))
    }

    @Test
    fun `fallback palette is fully opaque`() {
        val f = PosterPalette.Fallback
        assertEquals(0xFF, (f.vibrant ushr 24) and 0xFF)
        assertEquals(0xFF, (f.darkVibrant ushr 24) and 0xFF)
    }
}
