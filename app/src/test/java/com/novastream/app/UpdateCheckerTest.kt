package com.novastream.app

import com.novastream.app.data.integrations.ReleaseInfo
import com.novastream.app.data.integrations.UpdateChecker
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
}
