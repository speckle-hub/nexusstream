package com.moovio.app

import com.moovio.app.data.adult.AdultSources
import com.moovio.app.data.adult.FullPornerSource
import com.moovio.app.data.adult.JavMostSource
import com.moovio.app.data.adult.JavtifulSource
import com.moovio.app.data.adult.KissJavSource
import com.moovio.app.data.adult.OpJavSource
import com.moovio.app.data.adult.PornTrexSource
import com.moovio.app.data.adult.SevenMmtvSource
import com.moovio.app.data.adult.SxyPrnSource
import com.moovio.app.data.adult.TxxxSource
import com.moovio.app.data.hentai.HenVidsSource
import com.moovio.app.data.hentai.HentaiSources
import com.moovio.app.data.hentai.HentaiTvSource
import com.moovio.app.data.hentai.OhentaiSource
import com.moovio.app.data.hentai.XAnimuSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests for the source expansion (§2as): HenVids + Ohentai + Hentai.tv +
 * XAnimu (NSFW anime), PornTrex + SxyPrn + FullPorner + TXXX (general tubes)
 * and JavMost + KissJAV + Javtiful + OpJav + 7MMTV (JAV). Fixtures are trimmed
 * representative markup/JSON for each site's known shape, so these pin the
 * scraping contract without a network call. Live reachability still needs a
 * device pass (these sites churn, bot-block and geo-block regularly).
 */
class MoreSourcesTest {

    @Test
    fun `henvids parses hentai cards and skips shelves`() {
        val html = """
            <div class="grid">
              <a href="https://henvids.com/hentai/shoujo-ramune-7">Shoujo Ramune Episode 7</a>
              <a href="https://henvids.com/tag/anal">Anal</a>
              <a href="https://henvids.com/brand/queen-bee">Queen Bee</a>
              <a href="https://henvids.com/series/shoujo-ramune">Shoujo Ramune</a>
            </div>
        """.trimIndent()

        val episodes = HenVidsSource.parseCards(html)
        assertEquals(1, episodes.size)
        assertEquals("Shoujo Ramune Episode 7", episodes[0].title)
        assertEquals("Shoujo Ramune", episodes[0].series)
        assertEquals(7, episodes[0].episode)
        assertEquals("henvids", episodes[0].sourceId)
    }

    @Test
    fun `ohentai parses detail cards and absolutises posters`() {
        val html = """
            <div class="videos">
              <a href="detail.php?vid=NDY2Mw==">The Fastest GMan Collection</a>
              <img src="video_data/The%20Fastest%20GMan%20Collection/Episode-1/Episode-1.png">
              <a href="tagsearch.php?tag=Milf">Milf</a>
            </div>
        """.trimIndent()

        val episodes = OhentaiSource.parseCards(html)
        assertEquals(1, episodes.size)
        assertEquals("The Fastest GMan Collection", episodes[0].title)
        assertEquals("https://ohentai.org/detail.php?vid=NDY2Mw==", episodes[0].url)
        assertTrue(episodes[0].poster?.startsWith("https://ohentai.org/") == true)
    }

    @Test
    fun `hentaitv search parses episode-level json with embed urls`() {
        val json = """
            {"videos":[
              {"id":"abc1","slug":"some-title-episode-1","title":"Some Title","ep":1,
               "cover":"/uploads/media/2023/09/poster_1.jpg","thumb":"/uploads/media/2023/09/cover_1.jpg",
               "embedUrl":"https://nhplayer.com/v/xyz123/","duration":"23:03","quality":"1080p"},
              {"id":"abc2","slug":"other-title-episode-2","title":"Other Title","ep":2,
               "cover":null,"thumb":null,"embedUrl":null,"duration":"25:05","quality":"1080p"}
            ]}
        """.trimIndent()

        val episodes = HentaiTvSource.parseSearch(json)
        assertEquals(2, episodes.size)
        assertEquals("Some Title", episodes[0].series)
        assertEquals(1, episodes[0].episode)
        assertEquals("https://nhplayer.com/v/xyz123/", episodes[0].url)
        assertEquals("https://hentai.tv/uploads/media/2023/09/poster_1.jpg", episodes[0].poster)
        assertEquals("https://hentai.tv/watch/other-title-episode-2", episodes[1].url)
        assertEquals("hentaitv", episodes[0].sourceId)
    }

    @Test
    fun `xanimu parses numeric video cards and skips taxonomy`() {
        val html = """
            <div class="list">
              <a href="https://xanimu.com/260290-some-video-title/">
                <img src="https://xanimu.com/storage/2025/03/abc.jpg" alt="Some Video Title">
              </a>
              <a href="https://xanimu.com/tag/2217-ntr/">ntr</a>
              <a href="https://xanimu.com/actor/1404-lara-croft/">Lara Croft</a>
              <a href="https://xanimu.com/24-tv-shows/26-pokemon/">Pokemon</a>
            </div>
        """.trimIndent()

        val episodes = XAnimuSource.parseCards(html)
        assertEquals(1, episodes.size)
        assertEquals("Some Video Title", episodes[0].title)
        assertEquals("https://xanimu.com/260290-some-video-title/", episodes[0].url)
    }

    @Test
    fun `porntrex parses video cards and skips categories`() {
        val html = """
            <div class="list-videos">
              <a href="https://www.porntrex.com/video/3363195/some-video-title" title="Some Video Title">
                <img data-src="https://ptx.cdntrex.com/contents/1.jpg" alt="Some Video Title">
              </a>
              <a href="https://www.porntrex.com/categories/amateur/">Amateur</a>
            </div>
        """.trimIndent()

        val items = PornTrexSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Some Video Title", items[0].title)
        assertEquals("https://www.porntrex.com/video/3363195/some-video-title", items[0].url)
    }

    @Test
    fun `sxyprn parses post cards and skips junk`() {
        val html = """
            <div class="wall">
              <a href="/post/6ace7aa644c44.html" title="Some Post Title #Milf #Blowjob">Some Post Title</a>
              <a href="/blog/Only-Fans/0.html">Only Fans</a>
            </div>
        """.trimIndent()

        val items = SxyPrnSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Some Post Title #Milf #Blowjob", items[0].title)
        assertEquals("https://www.sxyprn.com/post/6ace7aa644c44.html", items[0].url)
    }

    @Test
    fun `fullporner parses watch cards`() {
        val html = """
            <div class="featured">
              <a href="/watch/6acf724b0e5f775a6fbbc42b">
                <img alt="Some Full Movie Title">
              </a>
              <a href="/category/anal">anal</a>
            </div>
        """.trimIndent()

        val items = FullPornerSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Some Full Movie Title", items[0].title)
        assertEquals("https://fullporner.com/watch/6acf724b0e5f775a6fbbc42b", items[0].url)
    }

    @Test
    fun `txxx parses watch cards with quality`() {
        val html = """
            <div class="list-videos">
              <a href="/watch/1234567/some-txxx-title/" title="Some Txxx Title">
                <img data-src="https://cdn.txxx.com/1.jpg" alt="Some Txxx Title">
              </a>
            </div>
        """.trimIndent()

        val items = TxxxSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Some Txxx Title", items[0].title)
        assertEquals("https://www.txxx.com/watch/1234567/some-txxx-title/", items[0].url)
    }

    @Test
    fun `kissjav parses kvs video cards`() {
        val html = """
            <div class="list-videos">
              <a href="https://kissjav.li/video/859876/some-jav-title/" title="Some Jav Title">
                <img data-src="https://cdn.kissjav.li/1.jpg" alt="Some Jav Title">
              </a>
              <a href="https://kissjav.li/categories/jav/">Uncensored</a>
            </div>
        """.trimIndent()

        val items = KissJavSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Some Jav Title", items[0].title)
        assertEquals("https://kissjav.li/video/859876/some-jav-title/", items[0].url)
    }

    @Test
    fun `javtiful parses code-keyed video cards`() {
        val html = """
            <div class="videos">
              <a href="/video/107099/lulu-435" title="LULU-435 Some Javtiful Title">
                <img src="/uploads/1.jpg" alt="LULU-435 Some Javtiful Title">
              </a>
              <a href="/category/big-tits">Big Tits</a>
            </div>
        """.trimIndent()

        val items = JavtifulSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("LULU-435 Some Javtiful Title", items[0].title)
        assertEquals("https://javtiful.com/video/107099/lulu-435", items[0].url)
    }

    @Test
    fun `javmost wordpress fallback parses permalinks and skips taxonomy`() {
        val html = """
            <article class="post">
              <a href="https://javmost.com/abcd-123-some-title/" title="ABCD-123 Some Title">
                <img src="https://javmost.com/img/1.jpg" alt="ABCD-123 Some Title">
              </a>
              <a href="https://javmost.com/category/censored/">Censored</a>
            </article>
        """.trimIndent()

        val items = JavMostSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("ABCD-123 Some Title", items[0].title)
    }

    @Test
    fun `opjav wordpress fallback parses permalinks`() {
        val html = """
            <article class="post">
              <a href="https://opjav.com/efgh-456-some-title/" title="EFGH-456 Some Title">
                <img src="https://opjav.com/img/1.jpg" alt="EFGH-456 Some Title">
              </a>
            </article>
        """.trimIndent()

        val items = OpJavSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("EFGH-456 Some Title", items[0].title)
    }

    @Test
    fun `7mmtv parses permalinks on either host`() {
        val html = """
            <article class="post">
              <a href="https://7mmtv.sx/ijkl-789-some-title/" title="IJKL-789 Some Title">
                <img src="https://7mmtv.sx/img/1.jpg" alt="IJKL-789 Some Title">
              </a>
            </article>
        """.trimIndent()

        val items = SevenMmtvSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("IJKL-789 Some Title", items[0].title)
        assertEquals("https://7mmtv.sx/ijkl-789-some-title/", items[0].url)
    }

    @Test
    fun `expanded registries expose all new sources with unique ids`() {
        val adultIds = AdultSources.all.map { it.id }
        assertEquals(adultIds.size, adultIds.distinct().size)
        for (id in listOf(
            "porntrex", "sxyprn", "fullporner",
            "kissjav", "javtiful",
        )) {
            assertTrue("missing adult source $id", id in adultIds)
        }
        // Source hygiene pass: these consistently 403/blocked/unreachable hosts were removed
        // from the active registry (see AdultSources). Their source objects/parser tests remain,
        // but a regression that re-registers one should fail here.
        for (id in listOf("txxx", "javmost", "opjav", "7mmtv")) {
            assertNull("removed adult source re-registered: $id", AdultSources.byId(id))
        }
        val hentaiIds = HentaiSources.all.map { it.id }
        assertEquals(hentaiIds.size, hentaiIds.distinct().size)
        for (id in listOf("henvids", "ohentai", "hentaitv", "xanimu")) {
            assertTrue("missing hentai source $id", id in hentaiIds)
        }
    }
}
