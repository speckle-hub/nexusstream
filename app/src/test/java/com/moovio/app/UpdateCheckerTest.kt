package com.moovio.app

import com.moovio.app.data.integrations.DownloadProgress
import com.moovio.app.data.integrations.ReleaseInfo
import com.moovio.app.data.integrations.UpdateChecker
import com.moovio.app.data.integrations.downloadProgressLabel
import com.moovio.app.data.integrations.formatUpdateBytes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateCheckerTest {

    private fun release(tag: String) = ReleaseInfo(
        tag = tag,
        name = tag,
        notes = "",
        apkUrl = "https://example.com/app-release.apk",
        pageUrl = "https://github.com/speckle-hub/nexusstream/releases/latest",
    )

    // ---- default repository fallback -------------------------------------------

    @Test
    fun `blank or unset repository falls back to the built-in default`() {
        assertEquals("speckle-hub/nexusstream", UpdateChecker.DEFAULT_REPO)
        assertEquals(UpdateChecker.DEFAULT_REPO, UpdateChecker.resolveRepo(""))
        assertEquals(UpdateChecker.DEFAULT_REPO, UpdateChecker.resolveRepo("   "))
    }

    @Test
    fun `a configured repository is used verbatim, trimmed`() {
        assertEquals("someone/other", UpdateChecker.resolveRepo("someone/other"))
        assertEquals("someone/other", UpdateChecker.resolveRepo("  someone/other  "))
    }

    // ---- version comparison ----------------------------------------------------

    @Test
    fun `a higher tag is newer`() {
        assertTrue(release("v1.4.0").isNewerThan("1.3.0"))
        assertTrue(release("1.4.0").isNewerThan("v1.3.0"))
        assertTrue(release("v2.0.0").isNewerThan("1.9.9"))
        assertTrue(release("v1.3.1").isNewerThan("1.3.0"))
    }

    @Test
    fun `an equal tag is not newer`() {
        assertFalse(release("v1.3.0").isNewerThan("1.3.0"))
        assertFalse(release("1.3.0").isNewerThan("v1.3.0"))
    }

    @Test
    fun `an older tag is not newer`() {
        assertFalse(release("v1.2.0").isNewerThan("1.3.0"))
        assertFalse(release("v1.3.0").isNewerThan("1.4.0"))
    }

    @Test
    fun `missing segments compare as zero`() {
        assertTrue(release("v1.3.1").isNewerThan("1.3"))
        assertFalse(release("v1.3").isNewerThan("1.3.0"))
    }

    // ---- download progress --------------------------------------------------------

    @Test
    fun `fraction and percent track bytes over total`() {
        val half = DownloadProgress(50, 100)
        assertEquals(0.5f, half.fraction!!, 0.001f)
        assertEquals(50, half.percent)
        assertEquals(0, DownloadProgress(0, 100).percent)
        assertEquals(100, DownloadProgress(100, 100).percent)
    }

    @Test
    fun `progress clamps overruns and guards bad totals`() {
        assertEquals(100, DownloadProgress(150, 100).percent)
        assertNull(DownloadProgress(10, null).fraction)
        assertNull(DownloadProgress(10, null).percent)
        assertNull(DownloadProgress(10, 0).fraction)
        assertNull(DownloadProgress(10, -4).percent)
        assertEquals(0, DownloadProgress(-5, 100).percent)
    }

    @Test
    fun `byte formatter uses MB KB B bands`() {
        assertEquals("38.3 MB", formatUpdateBytes(38_330_038))
        assertEquals("512 KB", formatUpdateBytes(512_000))
        assertEquals("128 B", formatUpdateBytes(128))
    }

    @Test
    fun `label shows percent and byte stats when total is known`() {
        assertEquals(
            "Downloading update… 45% (18.2 MB / 40.5 MB)",
            downloadProgressLabel(DownloadProgress(18_200_000, 40_500_000)),
        )
    }

    @Test
    fun `label degrades gracefully without a total`() {
        assertEquals(
            "Downloading update… 18.2 MB",
            downloadProgressLabel(DownloadProgress(18_200_000, null)),
        )
        assertEquals("Downloading update…", downloadProgressLabel(null))
    }
}
