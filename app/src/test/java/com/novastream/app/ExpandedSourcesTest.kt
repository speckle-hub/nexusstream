package com.novastream.app

import com.novastream.app.data.adult.AdultSources
import com.novastream.app.data.adult.BeegSource
import com.novastream.app.data.adult.EpornerSource
import com.novastream.app.data.adult.JavGuruSource
import com.novastream.app.data.adult.SupJavSource
import com.novastream.app.data.adult.VjavSource
import com.novastream.app.data.adult.adultDuration
import com.novastream.app.data.hentai.AnimeIdHentaiSource
import com.novastream.app.data.hentai.HentaiSources
import com.novastream.app.data.hentai.Rule34VideoSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests for the source expansion (§2aq): Rule34Video + AnimeIdHentai (NSFW anime),
 * Eporner + Beeg (general tubes) and JavGuru + SupJav + VJAV (JAV). Fixtures are trimmed
 * representative markup/JSON for each site's known shape, so these pin the scraping contract
 * without a network call. Live reachability still needs a device pass (these sites churn,
 * bot-block and geo-block regularly).
 */
class ExpandedSourcesTest {

    @Test
    fun `rule34video listing parses video cards and skips navigation`() {
        val html = """
            <div class="thumbs">
              <a href="https://rule34video.com/video/3881625/some-title/">
                <img src="https://rule34video.com/thumb/1.jpg" alt="Some Rule34 Title">
              </a>
              <a href="https://rule34video.com/models/some-model/">Some Model</a>
              <a href="https://rule34video.com/search/test/">Search test</a>
            </div>
        """.trimIndent()

        val episodes = Rule34VideoSource.parseCards(html)
        assertEquals(1, episodes.size)
        assertEquals("Some Rule34 Title", episodes[0].title)
        assertEquals("https://rule34video.com/video/3881625/some-title/", episodes[0].url)
        assertEquals("https://rule34video.com/thumb/1.jpg", episodes[0].poster)
        assertEquals("rule34video", episodes[0].sourceId)
    }

    @Test
    fun `animeidhentai wordpress card parses title and poster`() {
        val html = """
            <article class="post-1 post type-post">
              <a href="https://animeidhentai.com/some-series-episode-2/" title="Some Series Episode 2">
                <img data-src="https://animeidhentai.com/img/1.jpg" alt="Some Series Episode 2">
              </a>
            </article>
            <nav><a href="https://animeidhentai.com/category/hentai/">Category</a></nav>
        """.trimIndent()

        val episodes = AnimeIdHentaiSource.parseCards(html)
        assertEquals(1, episodes.size)
        assertEquals("Some Series Episode 2", episodes[0].title)
        assertEquals("Some Series", episodes[0].series)
        assertEquals(2, episodes[0].episode)
        assertEquals("https://animeidhentai.com/some-series-episode-2/", episodes[0].url)
    }

    @Test
    fun `eporner api parses videos with thumbs and durations`() {
        val json = """
            {"count_total":2,"videos":[
              {"id":12345,"title":"Eporner Test Video","keywords":"test","views":999,
               "url":"https://www.eporner.com/video-ABC123/some-title/",
               "added":"2026-01-01","length_sec":754,"length_min":"12:34",
               "embed":"https://www.eporner.com/embed/ABC123/",
               "default_thumb":{"src":"https://img.eporner.com/x.jpg"}},
              {"id":67890,"title":"Second Video","url":"https://www.eporner.com/video-DEF456/other/",
               "length_sec":0,"default_thumb":{"src":"https://img.eporner.com/y.jpg"}}
            ]}
        """.trimIndent()

        val items = EpornerSource.parseApi(json)
        assertEquals(2, items.size)
        assertEquals("Eporner Test Video", items[0].title)
        assertEquals("https://www.eporner.com/video-ABC123/some-title/", items[0].url)
        assertEquals("https://img.eporner.com/x.jpg", items[0].thumbnail)
        assertEquals(adultDuration(754), items[0].duration)
        assertEquals("999", items[0].views)
        // A zero length must not render as "0:00".
        assertEquals(null, items[1].duration)
    }

    @Test
    fun `eporner listing parses a video card`() {
        val html = """
            <div class="video-card">
              <a href="/video-ABC123/some-title" title="Eporner Card Title">
                <img data-src="https://img.eporner.com/c.jpg" alt="Eporner Card Title">
                <span class="duration">12:34</span>
              </a>
            </div>
        """.trimIndent()

        val items = EpornerSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Eporner Card Title", items[0].title)
        assertEquals("https://www.eporner.com/video-ABC123/some-title", items[0].url)
    }

    @Test
    fun `beeg api parses videos and file map yields labelled streams`() {
        val json = """
            {"videos":[
              {"id":112233,"title":"Beeg Test Video","duration":600,
               "image":"https://img.beeg.com/1.jpg","views":1234,
               "file":{"720p":"https://video.beeg.com/112233/720.mp4",
                       "480p":"https://video.beeg.com/112233/480.mp4"}},
              {"id":445566,"title":"Second Video","duration":0}
            ]}
        """.trimIndent()

        val items = BeegSource.parseApi(json)
        assertEquals(2, items.size)
        assertEquals("Beeg Test Video", items[0].title)
        assertEquals("https://beeg.com/112233", items[0].url)
        assertEquals(adultDuration(600), items[0].duration)
        assertEquals(null, items[1].duration)
    }

    @Test
    fun `beeg file map builds highest-first streams with referer`() {
        val json = """
            {"file":{"480p":"https://video.beeg.com/1/480.mp4",
                     "720p":"https://video.beeg.com/1/720.mp4"}}
        """.trimIndent()
        val o = com.google.gson.JsonParser.parseString(json).asJsonObject

        val streams = BeegSource.streamsOf(o, "https://beeg.com/1")
        assertEquals(2, streams.size)
        assertEquals("720p", streams[0].quality)
        assertEquals("480p", streams[1].quality)
        assertEquals("https://beeg.com/1", streams[0].headers["Referer"])
        assertEquals("beeg", streams[0].addonId)
    }

    @Test
    fun `javguru listing parses posts and skips taxonomy links`() {
        val html = """
            <article class="post">
              <a href="https://jav.guru/abc-123-english-subbed/" title="ABC-123 English Subbed">
                <img data-src="https://jav.guru/img/x.jpg" alt="ABC-123 English Subbed">
              </a>
            </article>
            <nav>
              <a href="https://jav.guru/category/english-subbed/">English Subbed</a>
              <a href="https://jav.guru/tag/some-tag/">Some Tag</a>
            </nav>
        """.trimIndent()

        val items = JavGuruSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("ABC-123 English Subbed", items[0].title)
        assertEquals("https://jav.guru/abc-123-english-subbed/", items[0].url)
        assertEquals("https://jav.guru/img/x.jpg", items[0].thumbnail)
    }

    @Test
    fun `supjav listing parses posts and skips maker and tag links`() {
        val html = """
            <div class="post">
              <a href="https://supjav.com/ssis-001-english-subtitle/" title="SSIS-001 Test Title">
                <img data-src="https://supjav.com/img/y.jpg" alt="SSIS-001 Test Title">
              </a>
            </div>
            <nav>
              <a href="https://supjav.com/category/maker/switch">SWITCH</a>
              <a href="https://supjav.com/tag/anchorwoman">Anchorwoman</a>
            </nav>
        """.trimIndent()

        val items = SupJavSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("SSIS-001 Test Title", items[0].title)
        assertEquals("https://supjav.com/ssis-001-english-subtitle/", items[0].url)
    }

    @Test
    fun `vjav listing parses video cards and skips listing chrome`() {
        val html = """
            <div class="video-block">
              <a href="https://vjav.com/videos/12345/some-jav-title/" title="Some JAV Title">
                <img data-src="https://vjav.com/thumb/z.jpg" alt="Some JAV Title">
                <span class="duration">1:02:05</span>
              </a>
              <a href="https://vjav.com/videos/latest-updates/1/">Latest</a>
              <a href="https://vjav.com/models/some-model/">Model</a>
            </div>
        """.trimIndent()

        val items = VjavSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Some JAV Title", items[0].title)
        assertEquals("https://vjav.com/videos/12345/some-jav-title/", items[0].url)
        assertEquals("1:02:05", items[0].duration)
    }

    @Test
    fun `expanded registries expose all new sources with unique ids`() {
        val adultIds = AdultSources.all.map { it.id }
        assertEquals(adultIds.size, adultIds.distinct().size)
        listOf("eporner", "beeg", "javguru", "supjav", "vjav").forEach { id ->
            assertTrue("missing adult source: $id", AdultSources.byId(id) != null)
        }
        assertTrue(AdultSources.isBuiltIn("eporner"))

        val hentaiIds = HentaiSources.all.map { it.id }
        assertEquals(hentaiIds.size, hentaiIds.distinct().size)
        listOf("rule34video", "animeidhentai").forEach { id ->
            assertTrue("missing hentai source: $id", HentaiSources.byId(id) != null)
        }
        assertTrue(HentaiSources.isBuiltIn("rule34video"))
    }
}
