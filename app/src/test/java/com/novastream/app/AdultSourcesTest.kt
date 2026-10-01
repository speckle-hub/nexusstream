package com.novastream.app

import com.novastream.app.data.adult.HqpornerSource
import com.novastream.app.data.adult.PornhubSource
import com.novastream.app.data.adult.XnxxSource
import com.novastream.app.data.adult.Xtubes
import com.novastream.app.data.adult.XvideosSource
import com.novastream.app.data.adult.adultAbsoluteUrl
import com.novastream.app.data.adult.adultDuration
import com.novastream.app.data.adult.adultQualityFromUrl
import com.novastream.app.data.adult.adultQueryTokens
import com.novastream.app.data.adult.adultRelevanceScore
import com.novastream.app.data.adult.adultTitleMatches
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Parser tests for the built-in adult sources. The fixtures are trimmed excerpts of the real
 * markup returned by each site (verified live), so these pin the scraping contract without a
 * network call.
 */
class AdultSourcesTest {

    @Test
    fun `pornhub listing parses a card`() {
        val html = """
            <li class="pcVideoListItem js-pop videoblock videoBox" id="v490572925"
                data-video-id="490572925" data-video-vkey="6a9981b0128b1">
                <a href="/view_video.php?viewkey=6a9981b0128b1" title="Test Title One" class="fade">
                    <img src="blank.png" data-image="https://cdn.example/img1.jpg"
                         data-mediumthumb="https://cdn.example/t1.jpg" />
                </a>
                <var class="duration">18:14</var>
                <div class="videoDetailsBlock"><span class="views"><var>197K</var> views</span></div>
            </li>
        """.trimIndent()

        val items = PornhubSource.parseList(html)
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("Test Title One", item.title)
        assertTrue(item.url.endsWith("viewkey=6a9981b0128b1"))
        assertEquals("https://cdn.example/img1.jpg", item.thumbnail)
        assertEquals("18:14", item.duration)
        assertEquals("197K", item.views)
    }

    @Test
    fun `pornhub mediaDefinitions yields HLS streams per quality`() {
        val html = """
            flashvars_490572925 = {"video_title":"X","mediaDefinitions":[
              {"group":1,"height":1080,"format":"hls","videoUrl":"https:\/\/hv-h.phncdn.com\/hls\/1\/master.m3u8?h=abc","quality":"1080","segmentFormats":{"audio":"ts_aac"}},
              {"group":1,"height":720,"format":"hls","videoUrl":"https:\/\/hv-h.phncdn.com\/hls\/2\/master.m3u8?h=def","quality":"720"}
            ]};
        """.trimIndent()

        val streams = PornhubSource.parseStreams(html)
        assertEquals(2, streams.size)
        assertEquals("1080p", streams[0].quality)
        assertEquals("720p", streams[1].quality)
        assertEquals("https://hv-h.phncdn.com/hls/1/master.m3u8?h=abc", streams[0].url)
    }

    @Test
    fun `xvideos listing parses an item with metadata`() {
        val html = """
            <div class="mozaique">
            <div id="video_ubieilo8fa2" data-id="71600765" data-eid="ubieilo8fa2" class="frame-block thumb-block">
              <div class="thumb-inside"><div class="thumb">
                <a href="/video.ubieilo8fa2/some_slug"><img src="blank.gif" data-src="https://thumb.example/xv_14_t.jpg" /></a>
                <span class="top-right-tags"><span class="video-hd-mark">720p</span></span>
              </div></div>
              <div class="thumb-under">
                <p class="title"><a href="/video.ubieilo8fa2/some_slug">A Great Test Video <span class="duration">21 min</span></a></p>
                <p class="metadata"><span class="bg"><span class="duration">21 min</span><span><a href="/studio"><span class="name">Studio X</span></a><span> - 670.2k <span class="sprfluous">Views</span></span></span></p>
              </div>
            </div>
        """.trimIndent()

        val items = XvideosSource.parseList(html)
        assertEquals(1, items.size)
        val item = items[0]
        assertEquals("A Great Test Video", item.title)
        assertEquals("https://www.xvideos.com/video.ubieilo8fa2/some_slug", item.url)
        assertEquals("https://thumb.example/xv_14_t.jpg", item.thumbnail)
        assertEquals("21 min", item.duration)
        assertEquals("670.2k", item.views)
        assertEquals("720p", item.quality)
        assertEquals("Studio X", item.uploader)
    }

    @Test
    fun `x-style detail page yields HLS plus MP4 streams`() {
        val html = """
            <script>
            html5player.setVideoUrlLow('https://mp4-cdn.example/x/0/video_240p.mp4?secure=a');
            html5player.setVideoUrlHigh('https://mp4-cdn.example/x/0/video_360p.mp4?secure=b');
            html5player.setVideoHLS('https://hls-cdn.example/x/0/hls.m3u8');
            html5player.setVideoTitle('A Title');
            </script>
        """.trimIndent()

        val streams = Xtubes.parseStreams(html, "xvideos", "XVideos")
        assertEquals(3, streams.size)
        assertEquals("HLS", streams[0].quality)
        assertEquals("360p", streams[1].quality)
        assertEquals("240p", streams[2].quality)
        assertEquals("https://mp4-cdn.example/x/0/video_360p.mp4?secure=b", streams[1].url)
    }

    @Test
    fun `xnxx listing parses a card`() {
        val html = """
            <div class="mozaique">
            <div id="video_13aobrc4" data-id="66003111" data-eid="13aobrc4" class="thumb-block">
              <div class="thumb-inside"><div class="thumb">
                <a href="/video-13aobrc4/some_slug"><img src="blank.gif" data-src="https://thumb.example/xn_3_t.jpg" /></a>
              </div></div>
              <div class="thumb-under"><p class="title"><a href="/video-13aobrc4/some_slug">Ancestry Test Leads <span class="duration">12 min</span></a></p></div>
            </div>
        """.trimIndent()

        val items = XnxxSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Ancestry Test Leads", items[0].title)
        assertEquals("https://www.xnxx.com/video-13aobrc4/some_slug", items[0].url)
        assertEquals("https://thumb.example/xn_3_t.jpg", items[0].thumbnail)
    }

    @Test
    fun `hqporner listing parses title and main thumbnail`() {
        val html = """
            <a href="/hdporn/127731-test_drive_the_mom_too.html" class="image featured non-overlay">
              <div class="w403px" onmouseleave='defaultImage("//fastporndelivery.hqporner.com/imgs/39/12/1951e2cceb4262f_main.jpg","cover_127731")'></div>
            </a>
            <a href="/hdporn/127731-test_drive_the_mom_too.html" class="click-trigger">test drive the mom too</a>
            <a href="/hdporn/127448-im_no_lady_you_can_test.html" class="click-trigger">i'm no lady, you can test</a>
        """.trimIndent()

        val items = HqpornerSource.parseList(html)
        assertEquals(2, items.size)
        assertEquals("test drive the mom too", items[0].title)
        assertEquals("https://hqporner.com/hdporn/127731-test_drive_the_mom_too.html", items[0].url)
        assertEquals("https://fastporndelivery.hqporner.com/imgs/39/12/1951e2cceb4262f_main.jpg", items[0].thumbnail)
    }

    @Test
    fun `duration and quality helpers behave`() {
        assertEquals("18:14", adultDuration(1094))
        assertEquals("1:02:05", adultDuration(3725))
        assertEquals("360p", adultQualityFromUrl("https://x/0/video_360p.mp4?secure=b"))
        assertEquals(null, adultQualityFromUrl("https://x/0/hls.m3u8"))
    }

    @Test
    fun `pornhub listing uses the title block instead of the partner badge`() {
        val html = """
            <li class="pcVideoListItem js-pop videoblock videoBox" id="v490572925"
                data-video-id="490572925" data-video-vkey="9f17a2b3c4d5e">
                <a href="/view_video.php?viewkey=9f17a2b3c4d5e" class="fade" data-title="Content Partner">
                    <img src="https://thumb.phncdn.com/pics/1/abc.jpg" alt="Real Amateur Title Here" />
                </a>
                <div class="videoDetailsBlock">
                    <div class="title">
                        <a href="/view_video.php?viewkey=9f17a2b3c4d5e" class="">
                                    Real Amateur Title Here                    </a>
                    </div>
                    <span class="views"><var>18.1K</var> views</span>
                    <span class="time">12:34</span>
                </div>
            </li>
        """.trimIndent()

        val items = PornhubSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Real Amateur Title Here", items[0].title)
        assertEquals("https://thumb.phncdn.com/pics/1/abc.jpg", items[0].thumbnail)
        assertEquals("12:34", items[0].duration)
        assertEquals("18.1K", items[0].views)
    }

    @Test
    fun `pornhub streams carry referer and age cookie headers`() {
        val html = """
            {"mediaDefinitions":[
              {"height":720,"format":"hls","videoUrl":"https:\/\/eu-h.phncdn.com\/hls\/1\/master.m3u8?h=1","quality":"720"},
              {"height":0,"format":"hls","videoUrl":"https:\/\/www.pornhub.com\/video\/get_media?k=1","quality":"1080"}
            ]};
        """.trimIndent()

        val pageUrl = "https://www.pornhub.com/view_video.php?viewkey=abc"
        val streams = PornhubSource.parseStreams(html, pageUrl = pageUrl)
        // The dead `get_media` endpoint (answers `application/json`) must never be listed.
        assertEquals(1, streams.size)
        assertEquals("https://eu-h.phncdn.com/hls/1/master.m3u8?h=1", streams[0].url)
        assertEquals(pageUrl, streams[0].headers["Referer"])
        assertEquals("accessAgeDisclaimerPH=1; age_verified=1", streams[0].headers["Cookie"])
        assertTrue(streams[0].headers["User-Agent"].orEmpty().contains("Chrome"))
    }

    @Test
    fun `hqporner listing prefers the h2 title over play images`() {
        val html = """
            <a href="/hdporn/90001-something_wicked_came.html" class="image featured non-overlay">
              <div class="w403px" onmouseleave='defaultImage("//fastporndelivery.hqporner.com/imgs/1/90001_main.jpg","cover_90001")'></div>
            </a>
            <div class="click-trigger"><a href="/hdporn/90001-something_wicked_came.html" class="click-trigger">play images</a></div>
            <a href="/hdporn/90001-something_wicked_came.html">play images</a>
            <h2><a href="/hdporn/90001-something_wicked_came.html">Something Wicked Came To Town</a></h2>
        """.trimIndent()

        val items = HqpornerSource.parseList(html)
        assertEquals(1, items.size)
        assertEquals("Something Wicked Came To Town", items[0].title)
        assertEquals("https://hqporner.com/hdporn/90001-something_wicked_came.html", items[0].url)
        assertEquals(
            "https://fastporndelivery.hqporner.com/imgs/1/90001_main.jpg",
            items[0].thumbnail,
        )
    }

    @Test
    fun `hqporner embed yields protocol-relative mp4 streams with referer`() {
        val embed = """
            <script>
            var player = { sources: [
              { file: \"//s29.bigcdn.cc/pubs/vidlib/90001/360.mp4\", label: "360p"},
              { file: \"//s29.bigcdn.cc/pubs/vidlib/90001/720.mp4\", label: "720p"},
              { file: \"https://s29.bigcdn.cc/pubs/vidlib/90001/1080.mp4\"}
            ]};
            </script>
        """.trimIndent()

        val streams = HqpornerSource.directStreams(embed, referer = "https://mydaddy.cc/video/abc123")
        assertTrue(streams != null && streams.isNotEmpty())
        assertEquals(3, streams!!.size)
        // Sorted highest-first: 1080p, 720p, 360p.
        assertEquals("1080p", streams[0].quality)
        assertEquals("https://s29.bigcdn.cc/pubs/vidlib/90001/720.mp4", streams[1].url)
        assertEquals("360p", streams[2].quality)
        assertEquals("https://mydaddy.cc/video/abc123", streams[0].headers["Referer"])
        // Ad frames must never be treated as the player.
        val withAd = """<iframe src="https://a.adtng.com/some/zone"></iframe>
            <iframe src="//mydaddy.cc/video/hash42" allowfullscreen></iframe>"""
        assertEquals("//mydaddy.cc/video/hash42", HqpornerSource.playerIframe(withAd))
    }

    @Test
    fun `search relevance helpers tokenise, match and rank`() {
        assertEquals(listOf("big", "tits"), adultQueryTokens("Big Tits!"))
        // Single-character and purely-punctuation queries yield no tokens (nothing to filter on).
        assertEquals(emptyList<String>(), adultQueryTokens("A B"))

        val tokens = adultQueryTokens("real amateur")
        assertTrue(adultTitleMatches("Real Amateur Video", tokens))
        assertTrue(!adultTitleMatches("Cooking Show", tokens))
        // No tokens means everything matches, so short queries never empty the list.
        assertTrue(adultTitleMatches("Anything", emptyList()))

        assertEquals(2, adultRelevanceScore("Real Amateur Video", tokens))
        assertEquals(1, adultRelevanceScore("Amateur Cook", tokens))
        assertEquals(0, adultRelevanceScore("Cooking Show", tokens))
    }

    @Test
    fun `absolute url helper normalises protocol-relative and escaped urls`() {
        assertEquals("https://cdn.example/x.mp4", adultAbsoluteUrl("//cdn.example/x.mp4"))
        assertEquals("https://cdn.example/x.mp4", adultAbsoluteUrl("""https:\/\/cdn.example\/x.mp4"""))
        assertEquals("https://cdn.example/x.mp4", adultAbsoluteUrl("\"https://cdn.example/x.mp4\""))
        assertEquals(null, adultAbsoluteUrl("blob:null"))
        assertEquals(null, adultAbsoluteUrl(null))
    }
}
