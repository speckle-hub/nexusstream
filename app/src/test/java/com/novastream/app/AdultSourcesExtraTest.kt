package com.novastream.app

import com.novastream.app.data.adult.AdultSources
import com.novastream.app.data.adult.AvgleSource
import com.novastream.app.data.adult.HpjavSource
import com.novastream.app.data.adult.JableSource
import com.novastream.app.data.adult.MissavSource
import com.novastream.app.data.adult.RedTubeSource
import com.novastream.app.data.adult.TubeMedia
import com.novastream.app.data.adult.XhamsterSource
import com.novastream.app.data.adult.YouPornSource
import com.novastream.app.data.adult.adultDuration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests for the sources added in the Real 18+ expansion (xHamster / YouPorn / RedTube and
 * the JAV sources). The fixtures are trimmed representative markup / JSON for each site's current
 * shape, so these pin the scraping contract without a network call. Live reachability of each site
 * still needs a device pass (the sites bot-block/geo-block irregularly).
 */
class AdultSourcesExtraTest {

    @Test
    fun `xhamster listing parses title thumbnail and duration`() {
        val html = """
            <div class="thumb-list__item">
              <a href="/videos/big-test-video-1234567" title="Big Test Video" class="thumb-list__link">
                <img class="thumb-image-container__image" src="https://thumb.xhamster.com/abc.jpg" alt="Big Test Video">
                <span class="thumb-image-container__duration">12:34</span>
              </a>
            </div>
        """.trimIndent()

        val items = XhamsterSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Big Test Video", items[0].title)
        assertEquals("https://xhamster.com/videos/big-test-video-1234567", items[0].url)
        assertEquals("https://thumb.xhamster.com/abc.jpg", items[0].thumbnail)
        assertEquals("12:34", items[0].duration)
    }

    @Test
    fun `youporn listing parses a card`() {
        val html = """
            <div class="video-box">
              <a href="/watch/12345678/some-title/" title="Some YouPorn Title" class="video-box-image">
                <img data-src="https://img.youporn.com/x.jpg" alt="Some YouPorn Title">
                <span class="video-duration">10:00</span>
              </a>
            </div>
        """.trimIndent()

        val items = YouPornSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Some YouPorn Title", items[0].title)
        assertEquals("https://www.youporn.com/watch/12345678/some-title/", items[0].url)
        assertEquals("https://img.youporn.com/x.jpg", items[0].thumbnail)
        assertEquals("10:00", items[0].duration)
    }

    @Test
    fun `youporn mediaDefinitions yields quality-labelled hls streams`() {
        val json = """
            [
              {"quality":"1080","height":1080,"format":"hls","videoUrl":"https:\/\/cdn.youporn.com\/hls\/1\/master.m3u8?h=a"},
              {"quality":"720","height":720,"format":"hls","videoUrl":"https:\/\/cdn.youporn.com\/hls\/2\/master.m3u8?h=b"}
            ]
        """.trimIndent()

        val streams = YouPornSource.parseDefinitions(json, "https://www.youporn.com/watch/1/x/")
        assertEquals(2, streams.size)
        assertEquals("1080p", streams[0].quality)
        assertEquals("720p", streams[1].quality)
        assertEquals("https://cdn.youporn.com/hls/1/master.m3u8?h=a", streams[0].url)
        assertEquals("https://www.youporn.com/watch/1/x/", streams[0].headers["Referer"])
    }

    @Test
    fun `redtube listing parses a card`() {
        val html = """
            <div class="video-card">
              <a href="/12345678/some-title" title="RedTube Test Title">
                <img data-src="https://img.redtube.com/rt.jpg" alt="RedTube Test Title">
                <span class="duration">09:12</span>
              </a>
            </div>
        """.trimIndent()

        val items = RedTubeSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("RedTube Test Title", items[0].title)
        assertEquals("https://www.redtube.com/12345678/some-title", items[0].url)
        assertEquals("https://img.redtube.com/rt.jpg", items[0].thumbnail)
    }

    @Test
    fun `TubeMedia extracts protocol-relative and escaped mp4 plus m3u8`() {
        val html = """
            <script>
            var player = { sources: [
              { file: "//cdn.example/x/720.mp4", label: "720p" },
              { file: "https://cdn.example/x/1080p.mp4" },
              { file: "https://cdn.example/hls/master.m3u8" }
            ]};
            </script>
        """.trimIndent()

        val urls = TubeMedia.urls(html)
        assertEquals(3, urls.size)
        assertTrue(urls.contains("https://cdn.example/x/720.mp4"))
        assertTrue(urls.contains("https://cdn.example/hls/master.m3u8"))
        // Highest quality first; an unlabelled manifest is labelled HLS.
        val streams = TubeMedia.streams(html, HpjavSource, referer = "https://example.com/page")
        assertEquals("1080p", streams[0].quality)
        assertEquals("720p", streams[1].quality)
        assertEquals("HLS", streams[2].quality)
        assertEquals("https://example.com/page", streams[0].headers["Referer"])
    }

    @Test
    fun `jable listing parses title from the h6 block`() {
        val html = """
            <div class="video-img-box mb-4">
              <a href="https://jable.tv/videos/test-123/">
                <img data-src="https://img.jable.tv/test.jpg" alt="Jable Test">
              </a>
              <h6 class="title"><a href="https://jable.tv/videos/test-123/">Jable Test Title</a></h6>
            </div>
        """.trimIndent()

        val items = JableSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Jable Test Title", items[0].title)
        assertEquals("https://jable.tv/videos/test-123/", items[0].url)
        assertEquals("https://img.jable.tv/test.jpg", items[0].thumbnail)
    }

    @Test
    fun `jable detail page yields an hls stream`() {
        val html = """
            <script>var hlsUrl = 'https://s1.jable.tv/test/playlist.m3u8';</script>
        """.trimIndent()

        val streams = TubeMedia.streams(html, JableSource, referer = "https://jable.tv/videos/test-123/")
        assertEquals(1, streams.size)
        assertEquals("HLS", streams[0].quality)
        assertEquals("https://s1.jable.tv/test/playlist.m3u8", streams[0].url)
    }

    @Test
    fun `missav listing keeps video codes and skips section links`() {
        val html = """
            <div class="thumbnail group">
              <a href="https://missav.ws/en/ssis-001" title="MissAV Test Title">
                <img data-src="https://img.missav.ws/x.jpg" alt="MissAV Test Title">
              </a>
            </div>
            <nav>
              <a href="https://missav.ws/en/new">New</a>
              <a href="https://missav.ws/en/genres">Genres</a>
            </nav>
        """.trimIndent()

        val items = MissavSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("MissAV Test Title", items[0].title)
        assertEquals("https://missav.ws/en/ssis-001", items[0].url)
    }

    @Test
    fun `hpjav listing parses a numeric post link`() {
        val html = """
            <article class="post">
              <a href="https://hpjav.tv/12345678/test-video/" title="HPJAV Test Title">
                <img data-src="https://img.hpjav.tv/x.jpg" alt="HPJAV Test Title">
              </a>
            </article>
        """.trimIndent()

        val items = HpjavSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("HPJAV Test Title", items[0].title)
        assertEquals("https://hpjav.tv/12345678/test-video/", items[0].url)
        assertEquals("https://img.hpjav.tv/x.jpg", items[0].thumbnail)
    }

    @Test
    fun `avgle api list parses videos`() {
        val json = """
            {"success":true,"response":{"videos":[
              {"vid":"abc123","title":"Avgle Test Video","keyword":"test","views":12345,"duration":754,
               "thumbnail":"https://image.avgle.com/abc123/1.jpg"},
              {"vid":"def456","title":"Second Video","views":1,"duration":0}
            ],"total_pages":1}}
        """.trimIndent()

        val items = AvgleSource.parseList(json)
        assertEquals(2, items.size)
        assertEquals("Avgle Test Video", items[0].title)
        assertEquals("https://avgle.com/video/abc123", items[0].url)
        assertEquals("https://image.avgle.com/abc123/1.jpg", items[0].thumbnail)
        assertEquals(adultDuration(754), items[0].duration)
        assertEquals("12345", items[0].views)
        // A zero duration must not be shown as "0:00".
        assertNull(items[1].duration)
    }

    @Test
    fun `registry exposes the expanded source list with unique ids`() {
        val ids = AdultSources.all.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
        listOf(
            "pornhub", "xvideos", "xnxx", "hqporner", "spankbang",
            "xhamster", "youporn", "redtube",
            "missav", "jable",
        ).forEach { id ->
            assertTrue("missing source: $id", AdultSources.byId(id) != null)
        }
        assertTrue(AdultSources.isBuiltIn("xhamster"))
        // Retired hosts (§3): hpjav.tv no longer resolves (DNS) and api.avgle.com is down (520),
        // so neither is registered any more — a regression that re-adds one should fail here.
        assertNull(AdultSources.byId("hpjav"))
        assertNull(AdultSources.byId("avgle"))
    }
}
