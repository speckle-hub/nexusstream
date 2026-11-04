package com.moovio.app

import com.moovio.app.data.mihon.MihonExtensionRuntime
import com.moovio.app.data.mihon.MihonExtensionSources
import com.moovio.app.data.model.AddonKind
import com.moovio.app.data.model.MangaExt
import com.moovio.app.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * JVM coverage for the adapters that turn an installed Aniyomi/Mihon source into the app's own
 * models (roadmap §2bs).
 *
 * No APK is loaded here: the fakes reproduce the public-field / getter shape that
 * [MihonExtensionRuntime] reads reflectively, which is the part that can be tested without a
 * device. DEX scanning, `PathClassLoader` and the source's network calls stay device-only.
 */
class MihonExtensionSourcesTest {

    class FakeAnimeSource {
        @JvmField val name = "Sample Anime"
        @JvmField val baseUrl = "https://anime.test"
    }

    class FakeMangaSource {
        @JvmField val name = "Sample Manga"
    }

    class GetterNamedSource {
        fun getName() = "Getter Name"
    }

    // ---- Type mapping ---------------------------------------------------------

    @Test
    fun `anime sources map to ANIME and NSFW_ANIME`() {
        val source = FakeAnimeSource()
        assertEquals(MediaType.ANIME, MihonExtensionSources.mediaType(source, isNsfw = false))
        assertEquals(MediaType.NSFW_ANIME, MihonExtensionSources.mediaType(source, isNsfw = true))
    }

    @Test
    fun `manga sources map to MANGA and NSFW_MANGA`() {
        val source = FakeMangaSource()
        assertEquals(MediaType.MANGA, MihonExtensionSources.mediaType(source, isNsfw = false))
        assertEquals(MediaType.NSFW_MANGA, MihonExtensionSources.mediaType(source, isNsfw = true))
    }

    // ---- Identity -------------------------------------------------------------

    @Test
    fun `addon id is prefixed and reversible`() {
        assertEquals("mihon:Sample Manga", MihonExtensionRuntime.addonId(FakeMangaSource()))
        assertEquals("Sample Manga", MihonExtensionRuntime.sourceNameOf("mihon:Sample Manga"))
    }

    @Test
    fun `source name of ignores other addon schemes`() {
        assertNull(MihonExtensionRuntime.sourceNameOf("cloudstream:Something"))
        assertNull(MihonExtensionRuntime.sourceNameOf(null))
        assertNull(MihonExtensionRuntime.sourceNameOf(""))
    }

    @Test
    fun `source name reads the field first then the getter`() {
        assertEquals("Sample Anime", MihonExtensionSources.sourceName(FakeAnimeSource()))
        assertEquals("Getter Name", MihonExtensionSources.sourceName(GetterNamedSource()))
    }

    @Test
    fun `base url is read from the source field`() {
        assertEquals("https://anime.test", MihonExtensionSources.baseUrl(FakeAnimeSource()))
        assertNull(MihonExtensionSources.baseUrl(FakeMangaSource()))
    }

    // ---- Chapter / episode adaptation -----------------------------------------

    @Test
    fun `chapter map becomes a video`() {
        val video = MihonExtensionSources.chapterToVideo(
            mapOf(
                "url" to "https://m.test/ch/1",
                "chapter_title" to "Chapter 1",
                "chapter_number" to 1.0f,
            ),
        )
        assertEquals("https://m.test/ch/1", video.id)
        assertEquals("Chapter 1", video.title)
        assertEquals(1, video.episode ?: -1)
    }

    @Test
    fun `chapter map falls back to episode fields and a generic title`() {
        val video = MihonExtensionSources.chapterToVideo(
            mapOf("url" to "https://a.test/ep/2", "episode_number" to 2),
        )
        assertEquals("Chapter", video.title)
        assertEquals(2, video.episode ?: -1)
    }

    @Test
    fun `chapter map with no url still produces a video`() {
        val video = MihonExtensionSources.chapterToVideo(mapOf("chapter_title" to "Bonus"))
        assertEquals("", video.id)
        assertEquals("Bonus", video.title)
        assertNull(video.episode)
    }

    // ---- Add-on Manager section classification ---------------------------------

    private fun ext(pkg: String, repoUrl: String? = null): MangaExt =
        MangaExt(name = "X", pkg = pkg, apk = "", repoUrl = repoUrl)

    @Test
    fun `aniyomi extensions are classified by package namespace`() {
        assertEquals(
            AddonKind.ANIYOMI,
            ext("eu.kanade.tachiyomi.animeextension.all.animeonsen").section,
        )
    }

    @Test
    fun `extensions installed from an anime repo are classified as aniyomi`() {
        assertEquals(
            AddonKind.ANIYOMI,
            ext("com.example.manga", "https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json").section,
        )
    }

    @Test
    fun `mihon extensions stay in the mihon section`() {
        // Package contains "anime" but not the Aniyomi namespace: it must not leak into Aniyomi.
        assertEquals(
            AddonKind.KEIYOUSHI,
            ext("eu.kanade.tachiyomi.extension.en.animegirls", "https://raw.githubusercontent.com/keiyoushi/extensions/repo/index.pb").section,
        )
        assertEquals(AddonKind.KEIYOUSHI, ext("eu.kanade.tachiyomi.extension.all.ahottie").section)
    }

    // ---- Page → stream ---------------------------------------------------------

    @Test
    fun `manga page becomes a stream source`() {
        val stream = MihonExtensionSources.pageToStreamSource(
            mapOf("page_url" to "https://cdn.test/v/720.mp4", "quality" to "720p"),
            FakeAnimeSource(),
        )
        assertEquals("https://cdn.test/v/720.mp4", stream.url)
        assertEquals("720p", stream.quality)
        assertEquals("720p", stream.title)
        assertEquals("Sample Anime", stream.name)
        assertEquals("mihon:Sample Anime", stream.addonId)
        assertEquals("mihon", stream.description)
        assertEquals(false, stream.needsTorrent)
    }

    @Test
    fun `aniyomi video urls are served as streams`() {
        // Aniyomi anime episodes carry the playable URL in `video_url`; before §2bs that field was
        // read into the page map but never consulted, so an episode resolved to no streams at all.
        val stream = MihonExtensionSources.pageToStreamSource(
            mapOf("video_url" to "https://cdn.test/ep/1.m3u8", "quality" to "1080p"),
            FakeAnimeSource(),
        )
        assertEquals("https://cdn.test/ep/1.m3u8", stream.url)
        assertEquals("1080p", stream.quality)
    }

    @Test
    fun `alternate page url fields are honoured in order`() {
        assertEquals(
            "https://cdn.test/orig.jpg",
            MihonExtensionSources.pageToStreamSource(mapOf("page_url_orig" to "https://cdn.test/orig.jpg"), FakeMangaSource()).url,
        )
        assertEquals(
            "https://cdn.test/plain.mp4",
            MihonExtensionSources.pageToStreamSource(mapOf("url" to "https://cdn.test/plain.mp4"), FakeMangaSource()).url,
        )
    }

    @Test
    fun `a page map with no url yields no playable url`() {
        val stream = MihonExtensionSources.pageToStreamSource(emptyMap(), FakeMangaSource())
        assertNull(stream.url)
        assertNull(stream.quality)
    }
}
