package com.novastream.app

import com.novastream.app.data.hentai.HahoSource
import com.novastream.app.data.hentai.HanimeSource
import com.novastream.app.data.hentai.HentaiSources
import com.novastream.app.data.hentai.HentaiTubes
import com.novastream.app.data.hentai.HStreamSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests for the hentai sources added in the NSFW-anime expansion. The fixtures are trimmed
 * representative markup/JSON for each site's current shape, so these pin the scraping contract
 * without a network call. Live reachability still needs a device pass (several of these are
 * Cloudflare-protected).
 */
class HentaiSourcesExtraTest {

    @Test
    fun `hanime search JSON parses hits into episodes`() {
        val json = """
            {"hits":[
              {"name":"Some Hentai Episode 2","slug":"some-hentai-episode-2","poster_url":"https://hanime.tv/img/x.jpg"},
              {"name":"Other Title","slug":"other-title","poster_url":"https://hanime.tv/img/y.jpg"}
            ],"nbHits":2}
        """.trimIndent()

        val episodes = HanimeSource.parseSearch(json)
        assertEquals(2, episodes.size)
        assertEquals("Some Hentai Episode 2", episodes[0].title)
        assertEquals("Some Hentai", episodes[0].series)
        assertEquals(2, episodes[0].episode)
        assertEquals("https://hanime.tv/videos/hentai/some-hentai-episode-2", episodes[0].url)
        assertEquals("https://hanime.tv/img/x.jpg", episodes[0].poster)
        assertEquals("hanime", episodes[0].sourceId)
    }

    @Test
    fun `hanime videos_manifest yields stream urls`() {
        val json = """
            {"videos_manifest":{"servers":[
              {"name":"h","streams":[
                {"url":"https://v2.hanime.tv/hls/1/playlist.m3u8","height":720},
                {"url":"https://v2.hanime.tv/mp4/1/720.mp4","height":720}
              ]}
            ]}}
        """.trimIndent()

        val urls = HanimeSource.parseManifest(json)
        assertEquals(2, urls.size)
        assertEquals("https://v2.hanime.tv/hls/1/playlist.m3u8", urls[0])
    }

    @Test
    fun `haho search page parses series cards and skips site chrome`() {
        val html = """
            <li class="list-group-item">
              <a href="https://haho.moe/anime/wmisaeil" data-toggle="popover" title="Asgaldh: Waikyoku no Testament" data-content="Based on the game by Zone">Asgaldh</a>
            </li>
            <li class="list-group-item">
              <a href="https://haho.moe/anime/g1y64web" title="Shin Bible Black" rel="bookmark">Shin Bible Black</a>
            </li>
            <a href="https://haho.moe/anime/search" title="Search">Search</a>
            <nav><a href="https://haho.moe/anime" title="Anime">Anime</a></nav>
        """.trimIndent()

        val series = HahoSource.parseSeries(html)
        assertEquals(2, series.size)
        assertEquals("Asgaldh: Waikyoku no Testament", series[0].title)
        assertEquals("https://haho.moe/anime/wmisaeil", series[0].url)
        assertEquals("Shin Bible Black", series[1].title)
        assertEquals("https://haho.moe/anime/g1y64web", series[1].url)
    }

    @Test
    fun `haho series page expands anchors into numbered episodes`() {
        val html = """
            <ul class="loop episode-loop list">
              <li class="episode2131">
                <a href="https://haho.moe/anime/g1y64web/1" title="Watch Shin Bible Black Ep. 1 - Revival">
                  <div class="episode-number">Episode 1</div>
                </a>
              </li>
              <li class="episode2132">
                <a href="https://haho.moe/anime/g1y64web/2" title="Watch Shin Bible Black Ep. 2 - Reunion">
                  <div class="episode-number">Episode 2</div>
                </a>
              </li>
            </ul>
        """.trimIndent()

        val episodes = HahoSource.parseEpisodes(
            html,
            HahoSource.HahoSeries("Shin Bible Black", "https://haho.moe/anime/g1y64web"),
        )
        assertEquals(2, episodes.size)
        assertEquals("Shin Bible Black", episodes[0].series)
        assertEquals(1, episodes[0].episode)
        assertEquals("Shin Bible Black Ep. 1 - Revival", episodes[0].title)
        assertEquals("https://haho.moe/anime/g1y64web/1", episodes[0].url)
        assertEquals("haho", episodes[0].sourceId)
        assertEquals(2, episodes[1].episode)
    }

    @Test
    fun `mediaUrls extracts direct mp4 and hls and ignores snapshot files`() {
        val html = """
            <source src="https://cdn.example/x/720.mp4" type="video/mp4">
            <script>var hls = 'https://cdn.example/x/master.m3u8';</script>
            <img src="https://cdn.example/x/poster.mp4_snapshot_00.10.webp">
        """.trimIndent()

        val urls = HentaiTubes.mediaUrls(html)
        assertEquals(2, urls.size)
        assertTrue(urls.contains("https://cdn.example/x/720.mp4"))
        assertTrue(urls.contains("https://cdn.example/x/master.m3u8"))
    }

    @Test
    fun `mediaUrls reads extension-less source tags but not subtitle tracks`() {
        val html = """
            <video id="player" controls>
              <source data-fluid-hd src="https://s1.filegasm.com/1a6e2b1c5cb474dc?download_token=abc" title="1080p" type="video/mp4">
              <source src="https://s1.filegasm.com/1409e4f2cfb1f4df?download_token=def" title="480p" type="video/mp4">
              <source src="https://haho.moe/subtitles.vtt" type="text/vtt">
            </video>
        """.trimIndent()

        val urls = HentaiTubes.mediaUrls(html)
        assertEquals(2, urls.size)
        assertTrue(urls.contains("https://s1.filegasm.com/1a6e2b1c5cb474dc?download_token=abc"))
        assertTrue(urls.contains("https://s1.filegasm.com/1409e4f2cfb1f4df?download_token=def"))
    }

    @Test
    fun `toStreams tags quality and the page referer`() {
        val html = """<source src="https://cdn.example/x/720.mp4" type="video/mp4">"""
        val streams = HentaiTubes.toStreams(html, HanimeSource, "https://hanime.tv/")
        assertEquals(1, streams.size)
        assertEquals("720p", streams[0].quality)
        assertEquals("hanime", streams[0].addonId)
        assertEquals("https://hanime.tv/", streams[0].headers["Referer"])
    }

    @Test
    fun `wordpress card parser reads title thumbnail and episode`() {
        val html = """
            <article class="post-123 post type-post">
              <a href="https://site.example/some-series-episode-1/" title="Some Series Episode 1">
                <img data-src="https://img.example/1.jpg" alt="Some Series Episode 1">
              </a>
            </article>
        """.trimIndent()

        val episodes = HentaiTubes.parseCards(html, "https://site.example", "test", HentaiTubes::isContentLink)
        assertEquals(1, episodes.size)
        assertEquals("Some Series Episode 1", episodes[0].title)
        assertEquals("Some Series", episodes[0].series)
        assertEquals(1, episodes[0].episode)
        assertEquals("https://site.example/some-series-episode-1/", episodes[0].url)
        assertEquals("https://img.example/1.jpg", episodes[0].poster)
    }

    @Test
    fun `hstream listing only accepts hentai permalinks`() {
        val html = """
            <article>
              <a href="https://hstream.moe/hentai/123-some-title">Some HStream Title</a>
            </article>
            <article>
              <a href="https://hstream.moe/about">About</a>
            </article>
        """.trimIndent()

        val episodes = HStreamSource.parseCards(html)
        assertEquals(1, episodes.size)
        assertEquals("Some HStream Title", episodes[0].title)
        assertEquals("https://hstream.moe/hentai/123-some-title", episodes[0].url)
        assertEquals("hstream", episodes[0].sourceId)
    }

    @Test
    fun `episode and series helpers behave`() {
        assertEquals(3, HentaiTubes.episodeOf("Show Name Episode 3"))
        assertEquals(5, HentaiTubes.episodeOf("Show - 05"))
        assertEquals("Show Name", HentaiTubes.seriesOf("Show Name Episode 3"))
        // A title with no episode marker keeps its own name.
        assertEquals("Some Movie", HentaiTubes.seriesOf("Some Movie"))
    }

    @Test
    fun `isContentLink rejects site chrome`() {
        assertTrue(HentaiTubes.isContentLink("/some-title/"))
        assertFalse(HentaiTubes.isContentLink("/category/hentai/"))
        assertFalse(HentaiTubes.isContentLink("/about"))
        assertFalse(HentaiTubes.isContentLink("#"))
    }

    @Test
    fun `hentai registry exposes all sources with unique ids`() {
        val ids = HentaiSources.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        listOf(
            "hentaimama", "hentaiplay",
            "hentaihaven", "hstream",
            "muchohentai", "hentaicity", "haho",
        ).forEach { id -> assertTrue("missing source: $id", HentaiSources.byId(id) != null) }
        assertTrue(HentaiSources.isBuiltIn("haho"))
        // Retired hosts (§3): the Hanime search API is DNS-dead and hentaigasm.com times out, so
        // neither is registered any more — a regression that re-adds one should fail here.
        assertNull(HentaiSources.byId("hanime"))
        assertNull(HentaiSources.byId("hentaigasm"))
    }
}
