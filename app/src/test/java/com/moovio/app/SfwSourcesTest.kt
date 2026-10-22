package com.moovio.app

import com.moovio.app.data.model.MediaType
import com.moovio.app.data.model.StreamSource
import com.moovio.app.data.repo.StreamRanking
import com.moovio.app.data.sfw.AnimeKaiSource
import com.moovio.app.data.sfw.AnimePaheSource
import com.moovio.app.data.sfw.FlixerSource
import com.moovio.app.data.sfw.FlixhqSource
import com.moovio.app.data.sfw.GogoSource
import com.moovio.app.data.sfw.KickassAnimeSource
import com.moovio.app.data.sfw.MiruroSource
import com.moovio.app.data.sfw.MultiMoviesSource
import com.moovio.app.data.sfw.PopcornMoviesSource
import com.moovio.app.data.sfw.RiveSource
import com.moovio.app.data.sfw.SfwAnimeEpisode
import com.moovio.app.data.sfw.SfwAnimeSources
import com.moovio.app.data.sfw.SfwEmbeds
import com.moovio.app.data.sfw.SfwEpisode
import com.moovio.app.data.sfw.SfwSources
import com.moovio.app.data.sfw.SuperStreamSource
import com.moovio.app.data.sfw.VidsrcSource
import com.moovio.app.data.sfw.YtsSource
import com.moovio.app.data.sfw.YugenAnimeSource
import com.moovio.app.data.sfw.sfwSeasonEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Phase 38 SFW built-ins (pure JVM): parser fixtures per source, registry
 * guards, the shared embed/js-unpack helpers, season/episode parsing, video-id
 * round-trips and the auto-play ranking policy. No network is touched.
 */
class SfwSourcesTest {

    // ---- Registries ----

    @Test
    fun `movie tv registry holds eight sources with unique ids and names`() {
        assertEquals(8, SfwSources.all.size)
        assertEquals(8, SfwSources.all.map { it.id }.distinct().size)
        assertEquals(8, SfwSources.all.map { it.name }.distinct().size)
    }

    @Test
    fun `anime registry holds six sources with unique ids and names`() {
        assertEquals(6, SfwAnimeSources.all.size)
        assertEquals(6, SfwAnimeSources.all.map { it.id }.distinct().size)
        assertEquals(6, SfwAnimeSources.all.map { it.name }.distinct().size)
    }

    @Test
    fun `no id collides across the movie tv and anime registries`() {
        val movieIds = SfwSources.all.map { it.id }.toSet()
        val animeIds = SfwAnimeSources.all.map { it.id }.toSet()
        assertTrue(movieIds.intersect(animeIds).isEmpty())
    }

    // ---- Vidsrc ----

    @Test
    fun `vidsrc builds movie and tv embed urls`() {
        assertEquals(
            "https://vidsrc.xyz/embed/movie/12345",
            VidsrcSource.embedUrl("https://vidsrc.xyz", "12345", null, null),
        )
        assertEquals(
            "https://vidsrc.xyz/embed/tv/12345/2/5",
            VidsrcSource.embedUrl("https://vidsrc.xyz", "12345", 2, 5),
        )
    }

    // ---- YTS ----

    private val ytsList = """{"data":{"movies":[
        {"id":123,"title_english":"Dune","title":"Dune","medium_cover_image":"https://img/d.jpg","year":2021,"rating":8.5},
        {"id":124,"title_english":"","title":"","medium_cover_image":"","year":0}
    ]}}"""

    @Test
    fun `yts parses list movies skipping blank entries`() {
        val titles = YtsSource.parseList(ytsList)
        assertEquals(1, titles.size)
        assertEquals("Dune", titles.first().title)
        assertEquals("2021", titles.first().year)
        assertEquals(MediaType.MOVIE, titles.first().kind)
        assertTrue(titles.first().url.contains("123"))
    }

    @Test
    fun `yts parses torrents into infohash streams with seeders`() {
        val movie = """{"torrents":[
            {"hash":"abc123def456abc123def456abc123def456abcd","quality":"1080p","type":"bluray","seeds":150},
            {"hash":"short","quality":"720p","type":"web","seeds":3}
        ]}"""
        val streams = YtsSource.parseTorrents(
            com.google.gson.JsonParser.parseString(movie).asJsonObject,
        )
        assertEquals(1, streams.size)
        assertEquals("ABC123DEF456ABC123DEF456ABC123DEF456ABCD", streams.first().infoHash)
        assertEquals("1080p", streams.first().quality)
        assertEquals(150, streams.first().seeders)
    }

    // ---- FlixHQ ----

    @Test
    fun `flixhq parses season ids episode entries and server entries`() {
        val seasons = """<li><a data-id="111" class="x">Season 1</a></li>
            <li><a data-id="112" class="x">Season 2</a></li>"""
        assertEquals(listOf(1 to "111", 2 to "112"), FlixhqSource.seasonIds(seasons))

        val episodes = """<a data-id="222" data-num="3" class="eps-item">Episode 3</a>"""
        assertEquals(listOf(3 to "Episode 3"), FlixhqSource.episodeEntries(episodes))

        val servers = """<a data-id="333" class="server">UpCloud</a>"""
        assertEquals(listOf("333" to "UpCloud"), FlixhqSource.serverEntries(servers))
    }

    @Test
    fun `flixhq recognises watch links`() {
        assertTrue(FlixhqSource.isWatchLink("/movie/watch-dune-123"))
        assertTrue(FlixhqSource.isWatchLink("/tv/watch-friends-456"))
        assertFalse(FlixhqSource.isWatchLink("/trending/movies"))
    }

    // ---- MultiMovies ----

    @Test
    fun `multimovies parses episode links with season and episode numbers`() {
        val html = """<a href="/episode/naruto-season-2-episode-5">Naruto S2 E5</a>"""
        val links = MultiMoviesSource.episodeLinks(html, "Naruto")
        assertEquals(1, links.size)
        assertEquals(2, MultiMoviesSource.seasonOf(links.first().first, 0))
        assertEquals(5, MultiMoviesSource.episodeOf(links.first().first, 1))
        assertTrue(MultiMoviesSource.isTitleLink("/movies/dune-2021"))
        assertTrue(MultiMoviesSource.isTvLink("/tvshows/friends"))
    }

    // ---- Rive / Flixer / PopcornMovies ----

    @Test
    fun `rive recognises title and tv links`() {
        assertTrue(RiveSource.isTitleLink("/movie/dune"))
        assertTrue(RiveSource.isTitleLink("/tv/friends"))
        assertFalse(RiveSource.isTitleLink("/trending"))
    }

    @Test
    fun `flixer recognises title and tv links`() {
        assertTrue(FlixerSource.isTitleLink("/movie/dune"))
        assertTrue(FlixerSource.isTvLink("/tv/friends"))
        assertFalse(FlixerSource.isTitleLink("/search/x"))
    }

    @Test
    fun `popcornmovies recognises title and tv links`() {
        assertTrue(PopcornMoviesSource.isTitleLink("/movies/dune"))
        assertTrue(PopcornMoviesSource.isTvLink("/tv-series/friends"))
        assertFalse(PopcornMoviesSource.isTitleLink("/dmca"))
    }

    // ---- SuperStream ----

    private val superTitles = """{"data":[
        {"id":"m1","title":"Dune","poster":"https://img/d.jpg","year":"2021","type":"movie"},
        {"id":"s1","title":"Friends","poster":"https://img/f.jpg","year":"1994","type":"tv_series"}
    ]}"""

    @Test
    fun `superstream parses title records with kinds`() {
        val titles = SuperStreamSource.parseTitles(superTitles, MediaType.MOVIE)
        assertEquals(2, titles.size)
        assertEquals(MediaType.MOVIE, titles[0].kind)
        assertEquals(MediaType.SERIES, titles[1].kind)
        assertEquals("2021", titles[0].year)
    }

    @Test
    fun `superstream parses file links`() {
        val links = SuperStreamSource.parseLinks(
            com.google.gson.JsonParser.parseString(
                """[{"file":"https://cdn/x.mp4","label":"1080p"},"https://cdn/y.m3u8"]""",
            ),
        )
        assertEquals(2, links.size)
        assertEquals("1080p", links[0].second)
        assertNull(links[1].second)
    }

    // ---- AnimePahe ----

    @Test
    fun `animepahe parses anime search records`() {
        val titles = AnimePaheSource.parseSearch(
            """{"data":[{"title":"Naruto","session":"abc123","poster":"https://img/n.jpg"}]}""",
        )
        assertEquals(1, titles.size)
        assertEquals("Naruto", titles.first().title)
        assertTrue(titles.first().url.endsWith("/anime/abc123"))
    }

    @Test
    fun `animepahe parses release episodes`() {
        val title = com.moovio.app.data.sfw.SfwAnimeTitle("animepahe", "https://animepahe.ru/anime/abc123", "Naruto")
        val eps = AnimePaheSource.parseRelease(
            """{"data":[{"episode":1,"session":"eps1","title":"Enter Naruto"}]}""",
            title,
        )
        assertEquals(1, eps.size)
        assertEquals(1, eps.first().episode)
        assertTrue(eps.first().url.contains("/play/abc123/eps1"))
    }

    @Test
    fun `animepahe finds pahe windows on a play page`() {
        val windows = AnimePaheSource.paheWindows(
            """var x = "https://pahe.win/e/abc123";""",
            "https://animepahe.ru/play/a/b",
        )
        assertEquals(listOf("https://pahe.win/e/abc123"), windows)
    }

    // ---- Gogo ----

    @Test
    fun `gogo parses ajax search cards`() {
        val series = GogoSource.parseSearch(
            """<div><a href="/category/naruto" title="Naruto"><img src="https://img/n.jpg"></a></div>""",
            "https://gogoanime3.co",
        )
        assertEquals(1, series.size)
        assertEquals("Naruto", series.first().title)
        assertTrue(series.first().url.endsWith("/category/naruto"))
    }

    @Test
    fun `gogo parses category episode links in order`() {
        val series = GogoSource.GogoSeries("Naruto", "https://gogoanime3.co/category/naruto", null)
        val eps = GogoSource.parseEpisodes(
            """<ul><li><a href="/naruto-episode-5" title="Naruto Episode 5">EP 5</a></li>
               <li><a href="/naruto-episode-2">EP 2</a></li></ul>""",
            series,
        )
        assertEquals(listOf(2, 5), eps.map { it.episode })
    }

    // ---- KickassAnime ----

    @Test
    fun `kickassanime parses series cards and episode anchors`() {
        val series = KickassAnimeSource.parseSeries(
            """<div><a href="/anime/naruto" title="Naruto"><img src="https://img/n.jpg"></a></div>""",
            "https://kickassanime.com.es",
        )
        assertEquals(1, series.size)
        val eps = KickassAnimeSource.parseEpisodes(
            """<a href="/anime/naruto/episode-7" title="Episode 7">7</a>""",
            series.first(),
        )
        assertEquals(7, eps.first().episode)
    }

    // ---- YugenAnime ----

    @Test
    fun `yugenanime parses series cards and watch anchors`() {
        val series = YugenAnimeSource.parseSeries(
            """<div><a href="/anime/123/naruto" title="Naruto">Naruto</a></div>""",
            "https://yugenanime.tv",
        )
        assertEquals(1, series.size)
        val eps = YugenAnimeSource.parseEpisodes(
            """<a href="/watch/123/naruto/5">Episode 5</a>""",
            series.first(),
        )
        assertEquals(5, eps.first().episode)
    }

    // ---- AnimeKai ----

    @Test
    fun `animekai parses watch cards skipping episode links`() {
        val series = AnimeKaiSource.parseSeries(
            """<a href="/watch/naruto-abc12" title="Naruto">Naruto</a>
               <a href="/watch/naruto-abc12?ep=5">EP 5</a>""",
            "https://animekai.to",
        )
        assertEquals(1, series.size)
        assertEquals("Naruto", series.first().title)
    }

    @Test
    fun `animekai parses ep links watch refs and server ids`() {
        val series = AnimeKaiSource.KaiSeries("Naruto", "https://animekai.to/watch/naruto-abc12", null)
        val eps = AnimeKaiSource.parseEpisodes(
            """<a href="/watch/naruto-abc12?ep=5">EP 5</a>""",
            series,
        )
        assertEquals(5, eps.first().episode)
        assertEquals("abc12" to 5, AnimeKaiSource.watchRef("/watch/naruto-abc12?ep=5"))
        assertEquals(
            listOf("99"),
            AnimeKaiSource.serverIds("""{"html":"<a data-id=\"99\">S1</a>"}"""),
        )
    }

    // ---- Miruro ----

    @Test
    fun `miruro parses api search records`() {
        val series = MiruroSource.parseApiSearch(
            """{"data":[{"title":"Naruto","slug":"naruto-dk6r","image":"https://img/n.jpg"}]}""",
            "https://www.miruro.com",
        )
        assertEquals(1, series.size)
        assertTrue(series.first().url.endsWith("/series/naruto-dk6r"))
    }

    @Test
    fun `miruro parses watch episode anchors`() {
        val series = MiruroSource.MiruroSeries("One Piece", "https://www.miruro.com/series/one-piece-dk6r", null)
        val eps = MiruroSource.parseEpisodes(
            """<a href="/watch/abc/one-piece/12">Episode 12</a>""",
            series,
        )
        assertEquals(12, eps.first().episode)
    }

    // ---- Shared embed helpers ----

    @Test
    fun `embeds extract direct media source tags and file blobs`() {
        val html = """<video><source src="https://cdn/x.mp4" type="video/mp4"></video>
            <script>var p = {"file":"https:\/\/cdn\/y.m3u8"};</script>"""
        val urls = SfwEmbeds.mediaUrls(html)
        assertTrue(urls.contains("https://cdn/x.mp4"))
        assertTrue(urls.contains("https://cdn/y.m3u8"))
    }

    @Test
    fun `embeds extract labeled sources and normalise labels`() {
        val labeled = SfwEmbeds.labeledSources(
            """{"file":"https://cdn/x.mp4","label":"1080p"}""",
        )
        assertEquals(1, labeled.size)
        assertEquals("1080p", SfwEmbeds.normalizeLabel(labeled.first().second))
        assertEquals("720p", SfwEmbeds.normalizeLabel("HD"))
        assertEquals("CAM", SfwEmbeds.normalizeLabel("CAM"))
    }

    @Test
    fun `embeds unpack dean edwards packed scripts`() {
        val packed = """eval(function(p,a,c,k,e,d){e=function(c){return c.toString(36)};""" +
            """if(!''.replace(/^/,String)){while(c--){d[c]=k[c]||c}k=[function(e){return d[e]}];""" +
            """e=function(){return'\\w+'};c=1};while(c--){if(k[c]){p=p.replace(""" +
            """new RegExp('\\b'+e(c)+'\\b','g'),k[c])}}return p}""" +
            """('0="1://2/3.4";',5,5,'file|https|example|com|m3u8'.split('|'),0,{}))"""
        val unpacked = SfwEmbeds.jsUnpackAll(packed)
        assertTrue(unpacked.contains("https://example/com.m3u8"))
        assertTrue(SfwEmbeds.mediaUrls(unpacked).contains("https://example/com.m3u8"))
    }

    @Test
    fun `embeds absolutise iframe sources`() {
        val srcs = SfwEmbeds.iframeSrcs(
            """<iframe src="//embed.site/e/1"></iframe><iframe src="/e/2"></iframe>""",
            "https://site.com",
        )
        assertTrue(srcs.contains("https://embed.site/e/1"))
        assertTrue(srcs.contains("https://site.com/e/2"))
    }

    // ---- Season episode parsing ----

    @Test
    fun `season episode parses stremio ids sxxexx and trailing numbers`() {
        assertEquals(2 to 5, sfwSeasonEpisode("tt123:2:5", "tt123"))
        assertEquals((null to null), sfwSeasonEpisode("tt123", "tt123"))
        assertEquals(2 to 5, sfwSeasonEpisode("Show S02E05", "other"))
        assertEquals((null to 12), sfwSeasonEpisode("naruto-12", "other"))
    }

    // ---- Video id round trips ----

    @Test
    fun `sfw episode video ids round trip`() {
        val ep = SfwEpisode("flixhq", "Dune", "Dune", "https://flixhq.to/movie/x", null, null)
        val (sid, url) = SfwEpisode.fromVideoId(ep.videoId)!!
        assertEquals("flixhq", sid)
        assertEquals("https://flixhq.to/movie/x", url)
        assertNull(SfwEpisode.fromVideoId("tt123:1:2"))
    }

    @Test
    fun `sfw anime episode video ids round trip`() {
        val ep = SfwAnimeEpisode("gogo", "Naruto", "Episode 5", "https://gogo/x-episode-5", 5)
        val (sid, url) = SfwAnimeEpisode.fromVideoId(ep.videoId)!!
        assertEquals("gogo", sid)
        assertEquals("https://gogo/x-episode-5", url)
        assertNull(SfwAnimeEpisode.fromVideoId("hentai:x:y"))
    }

    // ---- Auto-play ranking ----

    @Test
    fun `bestPlayable prefers reliability then quality then direct then seeders`() {
        val reliableSd = StreamSource(url = "https://t.io/sd.mp4", quality = "480p", addonName = "Torrentio")
        val unknown4k = StreamSource(url = "https://x/embed", quality = "4K", addonName = "Random")
        val direct1080 = StreamSource(url = "https://cdn/a.mp4", quality = "1080p", addonName = "Random")
        val embed1080 = StreamSource(url = "https://embed/e/1", quality = "1080p", addonName = "Random")
        val seedy720 = StreamSource(url = "https://cdn/b.mp4", quality = "720p", seeders = 900, addonName = "Random")
        val plain720 = StreamSource(url = "https://cdn/c.mp4", quality = "720p", seeders = 3, addonName = "Random")
        val dead = StreamSource(title = "nothing")
        val list = listOf(unknown4k, embed1080, plain720, dead, seedy720, direct1080, reliableSd)
        assertEquals(reliableSd, StreamRanking.bestPlayable(list, listOf("torrentio")))
        // Quality still outranks directness: a 4K embed beats a 1080p file.
        assertEquals(unknown4k, StreamRanking.bestPlayable(listOf(unknown4k, direct1080), emptyList()))
        assertEquals(
            direct1080,
            StreamRanking.bestPlayable(listOf(embed1080, direct1080), emptyList()),
        )
        assertEquals(
            seedy720,
            StreamRanking.bestPlayable(listOf(plain720, seedy720), emptyList()),
        )
        assertNull(StreamRanking.bestPlayable(listOf(dead)))
    }

    @Test
    fun `isDirectMediaUrl separates files from embed pages`() {
        assertTrue(StreamRanking.isDirectMediaUrl("https://cdn/a.mp4"))
        assertTrue(StreamRanking.isDirectMediaUrl("https://cdn/a.m3u8?token=x"))
        assertFalse(StreamRanking.isDirectMediaUrl("https://embed/e/1"))
        assertFalse(StreamRanking.isDirectMediaUrl(null))
    }
}
