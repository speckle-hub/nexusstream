package com.moovio.desktop

import com.moovio.desktop.data.local.AddonStore
import com.moovio.desktop.data.net.Http
import com.moovio.desktop.data.remote.AniListClient
import com.moovio.desktop.data.remote.CinemetaClient
import com.moovio.desktop.data.remote.StremioClient
import com.moovio.desktop.data.repo.CatalogRepository
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout

/** Opt-in live check that the home feed's real data sources produce usable rows. */
class HomeCatalogTest {

    private val enabled = System.getenv("MOOVIO_LIVE_TESTS") == "1"

    @Test
    fun `home hero and curated rows resolve from metadata providers`(): Unit = runBlocking {
        if (!enabled) return@runBlocking
        val temp = Files.createTempDirectory("moovio-home").toFile()
        val store = AddonStore(addonsFile = File(temp, "addons.json"))
        val catalog = CatalogRepository(
            CinemetaClient(Http()),
            AniListClient(Http()),
            StremioClient(Http()),
            store,
        )

        val hero = withTimeout(30_000) { catalog.hero() }
        assertTrue(hero.isNotEmpty(), "hero items should be available")
        assertTrue(hero.all { it.background != null })

        val movies = withTimeout(30_000) {
            catalog.curatedRows().first { it.id == "trending-movies" }.loader()
        }
        assertTrue(movies.isNotEmpty())
        assertTrue(movies.first().poster != null)

        val anime = withTimeout(30_000) {
            catalog.curatedRows().first { it.id == "anime-trending" }.loader()
        }
        assertTrue(anime.isNotEmpty())
        assertTrue(anime.all { it.type == com.moovio.desktop.domain.model.MediaType.ANIME })

        temp.deleteRecursively()
        Unit
    }
}
