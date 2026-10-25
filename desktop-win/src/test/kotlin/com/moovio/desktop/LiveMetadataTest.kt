package com.moovio.desktop

import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.remote.AniListClient
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.domain.model.MediaType
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking

/**
 * Network integration checks. They are skipped unless MOOVIO_LIVE_TESTS=1 so the normal suite
 * stays deterministic and offline-friendly.
 */
class LiveMetadataTest {

    private val enabled = System.getenv("MOOVIO_LIVE_TESTS") == "1"

    @Test
    fun `cinemeta returns a movie catalog and metadata`() = runBlocking {
        if (!enabled) return@runBlocking
        val client = CinemetaClient(Http())
        val catalog = client.catalog(MediaType.MOVIE, "top")
        assertTrue(catalog.isNotEmpty())
        assertTrue(catalog.first().poster != null)

        val detail = client.meta(MediaType.MOVIE, "tt0111161")
        assertNotNull(detail)
        assertTrue(detail.name.contains("Shawshank", ignoreCase = true))
        assertTrue((detail.rating ?: 0.0) > 7.0)
    }

    @Test
    fun `cinemeta search finds a well-known title`() = runBlocking {
        if (!enabled) return@runBlocking
        val client = CinemetaClient(Http())
        val results = client.search(MediaType.MOVIE, "The Matrix")
        assertTrue(results.isNotEmpty())
        assertTrue(results.any { it.name.contains("Matrix", ignoreCase = true) })
    }

    @Test
    fun `anilist trending and detail expose episodes`() = runBlocking {
        if (!enabled) return@runBlocking
        val client = AniListClient(Http())
        val trending = client.trending(10)
        assertTrue(trending.isNotEmpty())
        val detail = client.detail(trending.first().id)
        assertNotNull(detail)
        assertTrue(detail.name.isNotBlank())
        assertTrue(detail.episodes.isNotEmpty(), "AniList detail should expose an episode list")
    }
}
