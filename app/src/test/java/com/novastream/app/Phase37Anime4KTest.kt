package com.novastream.app

import com.novastream.app.ui.player.Anime4KConfig
import com.novastream.app.ui.player.Anime4KGpuTier
import com.novastream.app.ui.player.Anime4KPreset
import com.novastream.app.ui.player.anime4KEffectLabel
import com.novastream.app.ui.player.anime4KEstimatedCost
import com.novastream.app.ui.player.anime4KFragmentShader
import com.novastream.app.ui.player.anime4KPasses
import com.novastream.app.ui.player.isAnime4KHeavy
import com.novastream.app.ui.player.parseAnime4KGpuTier
import com.novastream.app.ui.player.parseAnime4KPreset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 37 Anime4K engine (pure JVM): preset/tier parsing, pass ordering,
 * GPU cost tiers, labels and the combined GLSL shader contents.
 */
class Phase37Anime4KTest {

    @Test
    fun `known keys parse to their preset and tier`() {
        assertEquals(Anime4KPreset.SHARPEN, parseAnime4KPreset("sharpen"))
        assertEquals(Anime4KPreset.DEBLUR, parseAnime4KPreset("deblur"))
        assertEquals(Anime4KPreset.DENOISE, parseAnime4KPreset("denoise"))
        assertEquals(Anime4KPreset.OFF, parseAnime4KPreset("off"))
        assertEquals(Anime4KGpuTier.HIGH, parseAnime4KGpuTier("high"))
        assertEquals(Anime4KGpuTier.MID, parseAnime4KGpuTier("mid"))
    }

    @Test
    fun `unknown blank and null keys fall back safely`() {
        assertEquals(Anime4KPreset.OFF, parseAnime4KPreset("ultra"))
        assertEquals(Anime4KPreset.OFF, parseAnime4KPreset(""))
        assertEquals(Anime4KPreset.OFF, parseAnime4KPreset(null))
        assertEquals(Anime4KGpuTier.MID, parseAnime4KGpuTier("flagship"))
        assertEquals(Anime4KGpuTier.MID, parseAnime4KGpuTier(""))
        assertEquals(Anime4KGpuTier.MID, parseAnime4KGpuTier(null))
    }

    @Test
    fun `off attaches no passes and no shader`() {
        val config = Anime4KConfig(Anime4KPreset.OFF, Anime4KGpuTier.HIGH)
        assertTrue(anime4KPasses(config).isEmpty())
        assertTrue(anime4KFragmentShader(config).isEmpty())
        assertEquals(0, anime4KEstimatedCost(config))
        assertFalse(isAnime4KHeavy(config))
        assertEquals("Off", anime4KEffectLabel(config))
    }

    @Test
    fun `every live preset has passes on both tiers`() {
        val live = listOf(Anime4KPreset.SHARPEN, Anime4KPreset.DEBLUR, Anime4KPreset.DENOISE)
        for (preset in live) {
            for (tier in Anime4KGpuTier.entries) {
                val config = Anime4KConfig(preset, tier)
                assertTrue("$preset/$tier", anime4KPasses(config).isNotEmpty())
                assertTrue("$preset/$tier", anime4KFragmentShader(config).isNotEmpty())
            }
        }
    }

    @Test
    fun `pass names differ per preset`() {
        val sharpen = anime4KPasses(Anime4KConfig(Anime4KPreset.SHARPEN, Anime4KGpuTier.MID))
        val deblur = anime4KPasses(Anime4KConfig(Anime4KPreset.DEBLUR, Anime4KGpuTier.MID))
        val denoise = anime4KPasses(Anime4KConfig(Anime4KPreset.DENOISE, Anime4KGpuTier.MID))
        assertTrue(sharpen != deblur && deblur != denoise && sharpen != denoise)
    }

    @Test
    fun `high tier runs hq passes while mid stays lite`() {
        val mid = anime4KPasses(Anime4KConfig(Anime4KPreset.SHARPEN, Anime4KGpuTier.MID))
        val high = anime4KPasses(Anime4KConfig(Anime4KPreset.SHARPEN, Anime4KGpuTier.HIGH))
        assertTrue(mid.any { it.endsWith("Lite") })
        assertTrue(high.any { it.endsWith("HQ") })
    }

    @Test
    fun `only high tier live configs are heavy`() {
        assertTrue(isAnime4KHeavy(Anime4KConfig(Anime4KPreset.SHARPEN, Anime4KGpuTier.HIGH)))
        assertFalse(isAnime4KHeavy(Anime4KConfig(Anime4KPreset.SHARPEN, Anime4KGpuTier.MID)))
        assertFalse(isAnime4KHeavy(Anime4KConfig(Anime4KPreset.OFF, Anime4KGpuTier.HIGH)))
    }

    @Test
    fun `cost ordering is off below mid below high`() {
        assertEquals(0, anime4KEstimatedCost(Anime4KConfig(Anime4KPreset.DENOISE, Anime4KGpuTier.MID).copy(preset = Anime4KPreset.OFF)))
        assertEquals(1, anime4KEstimatedCost(Anime4KConfig(Anime4KPreset.DENOISE, Anime4KGpuTier.MID)))
        assertEquals(2, anime4KEstimatedCost(Anime4KConfig(Anime4KPreset.DENOISE, Anime4KGpuTier.HIGH)))
    }

    @Test
    fun `labels name the preset and tier`() {
        assertEquals(
            "Sharpen · Mid-range GPU",
            anime4KEffectLabel(Anime4KConfig(Anime4KPreset.SHARPEN, Anime4KGpuTier.MID)),
        )
        assertEquals(
            "Denoise · High-end GPU",
            anime4KEffectLabel(Anime4KConfig(Anime4KPreset.DENOISE, Anime4KGpuTier.HIGH)),
        )
    }

    @Test
    fun `shader carries the frame processor bindings and a main entry`() {
        for (preset in listOf(Anime4KPreset.SHARPEN, Anime4KPreset.DEBLUR, Anime4KPreset.DENOISE)) {
            for (tier in Anime4KGpuTier.entries) {
                val glsl = anime4KFragmentShader(Anime4KConfig(preset, tier))
                assertTrue("$preset/$tier", "sampler2D uTexture" in glsl)
                assertTrue("$preset/$tier", "uTexelSize" in glsl)
                assertTrue("$preset/$tier", "vTexCoord" in glsl)
                assertTrue("$preset/$tier", "void main()" in glsl)
                assertTrue("$preset/$tier", "gl_FragColor" in glsl)
            }
        }
    }
}
