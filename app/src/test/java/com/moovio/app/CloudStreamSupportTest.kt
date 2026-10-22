package com.moovio.app

import com.moovio.app.data.cloudstream.CloudStreamSources
import com.moovio.app.data.local.AddonStore
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM tests for the CloudStream integration glue (no Android, no network).
 *
 * The live runtime (loading `.cs3` dex + running providers) can only be exercised on a device;
 * what we can pin down here is the id scheme the UI relies on and the "recommended, not seeded"
 * repository policy.
 */
class CloudStreamSupportTest {

    @Test
    fun `addon prefix is recognised and stripped`() {
        assertEquals("Phisher98", CloudStreamSources.providerNameOf("cloudstream:Phisher98"))
        assertEquals("A Provider With Spaces", CloudStreamSources.providerNameOf("cloudstream:A Provider With Spaces"))
    }

    @Test
    fun `non-cloudstream addon ids are not providers`() {
        assertNull(CloudStreamSources.providerNameOf("tmdb"))
        assertNull(CloudStreamSources.providerNameOf(null))
        assertNull(CloudStreamSources.providerNameOf("anilist"))
    }

    @Test
    fun `best match prefers the exact title`() {
        val items = listOf(
            MediaItem("http://site/a", MediaType.MOVIE, "Avatar"),
            MediaItem("http://site/b", MediaType.MOVIE, "Avatar: The Way of Water"),
        )
        assertEquals("http://site/a", CloudStreamSources.bestMatch(items, "avatar")?.id)
    }

    @Test
    fun `best match falls back to a containing title then null`() {
        val items = listOf(MediaItem("http://site/b", MediaType.MOVIE, "Avatar: The Way of Water"))
        assertEquals("http://site/b", CloudStreamSources.bestMatch(items, "avatar")?.id)
        assertNull(CloudStreamSources.bestMatch(items, "Titanic"))
        assertNull(CloudStreamSources.bestMatch(emptyList(), "anything"))
    }

    @Test
    fun `the three provider repositories are recommended but never seeded`() {
        // The app still ships source-neutral: nothing is preinstalled...
        assertTrue("default repos must stay empty", AddonStore.Defaults.repos.isEmpty())

        val recommended = AddonStore.Defaults.recommendedRepos
        val urls = recommended.map { it.second.url }
        assertTrue(urls.any { it.contains("phisher98/cloudstream-extensions-phisher") })
        assertTrue(urls.any { it.contains("yuzono/anime-repo") })
        assertTrue(urls.any { it.contains("keiyoushi/extensions") })

        // ...but they are offered as one-tap adds, so they must NOT be pruned as "retired".
        assertTrue(
            "recommended repos must not be on the retired list",
            recommended.none { r -> AddonStore.Defaults.retiredRepoUrls.contains(r.second.url) },
        )
    }
}
