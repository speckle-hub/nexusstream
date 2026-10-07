package com.novastream.app

import com.novastream.app.ui.components.ResolutionTier
import com.novastream.app.ui.components.resolutionTierOf
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Quality strings come from add-ons and scrapers, which are wildly inconsistent ("4k", "2160p",
 * "1080p", "720", "HD", sometimes junk). The classifier normalizes them so the stream badge can
 * encode a tier in its colour.
 */
class ResolutionBadgeTest {

    @Test
    fun `recognises the common resolution labels`() {
        assertEquals(ResolutionTier.UHD, resolutionTierOf("4k"))
        assertEquals(ResolutionTier.UHD, resolutionTierOf("2160p"))
        assertEquals(ResolutionTier.UHD, resolutionTierOf("UHD"))
        assertEquals(ResolutionTier.FHD, resolutionTierOf("1080p"))
        assertEquals(ResolutionTier.FHD, resolutionTierOf("FHD"))
        assertEquals(ResolutionTier.HD, resolutionTierOf("720p"))
        assertEquals(ResolutionTier.HD, resolutionTierOf("HD"))
        assertEquals(ResolutionTier.SD, resolutionTierOf("480p"))
        assertEquals(ResolutionTier.SD, resolutionTierOf("SD"))
    }

    @Test
    fun `hdr outranks the plain resolution it is combined with`() {
        // "2160p HDR" must not be classified as bare UHD, otherwise the badge loses the one bit of
        // information that makes it worth showing.
        assertEquals(ResolutionTier.HDR, resolutionTierOf("2160p HDR"))
        assertEquals(ResolutionTier.HDR, resolutionTierOf("1080p HDR"))
        assertEquals(ResolutionTier.HDR, resolutionTierOf("Dolby Vision"))
        assertEquals(ResolutionTier.HDR, resolutionTierOf("10bit"))
    }

    @Test
    fun `matching is case and whitespace insensitive`() {
        assertEquals(ResolutionTier.UHD, resolutionTierOf("  4K  "))
        assertEquals(ResolutionTier.FHD, resolutionTierOf("1080P"))
        assertEquals(ResolutionTier.HD, resolutionTierOf("Hd"))
    }

    @Test
    fun `unrecognised or missing quality is unknown rather than guessed`() {
        // The badge falls back to the literal string in this case; inventing a tier would claim a
        // resolution the stream may not have.
        assertEquals(ResolutionTier.UNKNOWN, resolutionTierOf(null))
        assertEquals(ResolutionTier.UNKNOWN, resolutionTierOf(""))
        assertEquals(ResolutionTier.UNKNOWN, resolutionTierOf("   "))
        assertEquals(ResolutionTier.UNKNOWN, resolutionTierOf("auto"))
        assertEquals(ResolutionTier.UNKNOWN, resolutionTierOf("some-addon-custom-thing"))
    }
}
