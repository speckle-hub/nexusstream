package com.novastream.app

import com.novastream.app.data.local.LibraryCodec
import com.novastream.app.data.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the Library crash fixed in Phase 13.
 *
 * A favourite written without a `type` (or with a value the current [MediaType] does not know)
 * used to decode into a `MediaItem` whose `type` was null at runtime; the first thing the UI did
 * with it — `item.type.id` while building the detail route — threw an NPE and took the Library
 * tab down. Worse, the old whole-list `fromJson` discarded *every* favourite as soon as one entry
 * was malformed.
 */
class LibraryCodecTest {

    // ---- malformed / partial favourites -----------------------------------

    @Test
    fun `favourite with no type decodes with a usable default`() {
        val json = """[{"id":"tt0111161","title":"The Shawshank Redemption"}]"""
        val item = LibraryCodec.decodeFavorites(json).single()
        assertEquals(MediaType.MOVIE, item.type)
        assertEquals("The Shawshank Redemption", item.title)
        // The route builder reads `item.type.id`; a null here is the original crash.
        assertEquals("movie", item.type.id)
    }

    @Test
    fun `unknown type strings fall back instead of yielding null`() {
        val item = LibraryCodec.decodeFavorites("""[{"id":"1","type":"banana"}]""").single()
        assertEquals(MediaType.MOVIE, item.type)
    }

    @Test
    fun `type resolves from the enum name or the catalog id`() {
        fun typeOf(json: String) =
            LibraryCodec.decodeFavorites("""[{"id":"1","type":"$json"}]""").single().type

        assertEquals(MediaType.SERIES, typeOf("SERIES"))
        assertEquals(MediaType.SERIES, typeOf("tv"))
        assertEquals(MediaType.NSFW_MANGA, typeOf("nsfw_manga"))
        assertEquals(MediaType.ANIME, typeOf("anime"))
        // Falls back to the add-on's own declared type when `type` itself is unresolvable.
        assertEquals(
            MediaType.ANIME,
            LibraryCodec.decodeFavorites("""[{"id":"1","type":"??","stremioType":"anime"}]""").single().type,
        )
    }

    @Test
    fun `one malformed element does not discard the whole library`() {
        val json = """
            [
                {"id":"ok-1","type":"MOVIE","title":"Kept"},
                "not-an-object",
                {"type":"MOVIE","title":"No id at all"},
                {"id":"ok-2","type":"SERIES","title":"Also kept"}
            ]
        """.trimIndent()
        val items = LibraryCodec.decodeFavorites(json)
        assertEquals(listOf("ok-1", "ok-2"), items.map { it.id })
    }

    @Test
    fun `duplicate keys keep only the first occurrence`() {
        val json = """
            [
                {"id":"same","type":"MOVIE","title":"First"},
                {"id":"same","type":"MOVIE","title":"Second"}
            ]
        """.trimIndent()
        val items = LibraryCodec.decodeFavorites(json)
        assertEquals(1, items.size)
        assertEquals("First", items.single().title)
    }

    @Test
    fun `optional fields default instead of staying null`() {
        val json = """[{"id":"1","type":"MOVIE","title":"Bare"}]"""
        val item = LibraryCodec.decodeFavorites(json).single()
        assertNull(item.poster)
        assertNull(item.backdrop)
        assertNull(item.rating)
        assertEquals(emptyList<String>(), item.genres)
    }

    @Test
    fun `null-valued fields are read as null and empty lists`() {
        val json = """
            [{"id":"1","type":"MOVIE","title":"T","poster":null,"rating":null,
              "genres":null,"description":null}]
        """.trimIndent()
        val item = LibraryCodec.decodeFavorites(json).single()
        assertNull(item.poster)
        assertNull(item.rating)
        assertEquals(emptyList<String>(), item.genres)
    }

    @Test
    fun `genres survive as an array, a lone string or garbage`() {
        fun genresOf(json: String) =
            LibraryCodec.decodeFavorites("""[{"id":"1","title":"T","genres":$json}]""").single().genres

        assertEquals(listOf("Action", "Drama"), genresOf("""["Action","Drama"]"""))
        assertEquals(listOf("Comedy"), genresOf("\"Comedy\""))
        assertEquals(emptyList<String>(), genresOf("""{"nested":true}"""))
        assertEquals(emptyList<String>(), genresOf("null"))
        assertEquals(listOf("Horror"), genresOf("""["Horror","",null]"""))
    }

    @Test
    fun `rating is parsed from a number or a string and rejects nonsense`() {
        fun ratingOf(json: String) =
            LibraryCodec.decodeFavorites("""[{"id":"1","title":"T","rating":$json}]""").single().rating

        assertEquals(7.5, ratingOf("7.5")!!, 0.0)
        assertEquals(8.0, ratingOf("\"8.0\"")!!, 0.0)
        assertNull(ratingOf("\"tbd\""))
        assertNull(ratingOf("null"))
    }

    // ---- malformed / partial history -------------------------------------

    @Test
    fun `history entry without an item is dropped, the rest survive`() {
        val json = """
            [
                {"videoId":"a","positionMs":10,"durationMs":100},
                {"item":{"id":"kept","type":"SERIES","title":"Kept"},
                 "videoId":"b","positionMs":50,"durationMs":100}
            ]
        """.trimIndent()
        val history = LibraryCodec.decodeHistory(json)
        assertEquals(1, history.size)
        assertEquals("kept", history.single().item.key.substringAfter("::"))
        assertEquals(50L, history.single().positionMs)
    }

    @Test
    fun `history entry whose item lost its type still decodes`() {
        val json = """[{"item":{"id":"x","title":"No type"},"positionMs":1000,"durationMs":2000}]"""
        val entry = LibraryCodec.decodeHistory(json).single()
        assertEquals(MediaType.MOVIE, entry.item.type)
        assertEquals(1000L, entry.positionMs)
        assertEquals(2000L, entry.durationMs)
        assertTrue(entry.updatedAt > 0L)
    }

    @Test
    fun `history keeps stored order and de-duplicates by title`() {
        val json = """
            [
                {"item":{"id":"a","type":"MOVIE","title":"A"},"positionMs":2},
                {"item":{"id":"a","type":"MOVIE","title":"A"},"positionMs":9},
                {"item":{"id":"b","type":"MOVIE","title":"B"},"positionMs":4}
            ]
        """.trimIndent()
        val history = LibraryCodec.decodeHistory(json)
        assertEquals(listOf(2L, 4L), history.map { it.positionMs })
    }

    // ---- hostile input ----------------------------------------------------

    @Test
    fun `unusable payloads decode to an empty list instead of throwing`() {
        for (payload in listOf(null, "", "   ", "{not json", "\"movie\"", "42", "[1,2,3]", "{}")) {
            assertTrue("payload=$payload", LibraryCodec.decodeFavorites(payload).isEmpty())
            assertTrue("payload=$payload", LibraryCodec.decodeHistory(payload).isEmpty())
        }
    }

    @Test
    fun `a single bare object is tolerated as a one-element library`() {
        val item = LibraryCodec.decodeFavorites("""{"id":"solo","type":"MOVIE","title":"Solo"}""").single()
        assertEquals("solo", item.id)
    }

    // ---- round trip -------------------------------------------------------

    @Test
    fun `encode and decode round-trip a well-formed favourite`() {
        val original = LibraryCodec.decodeFavorites(
            """
            [{"id":"tt1","type":"NSFW_ANIME","title":"Kept","poster":"p","year":"2024",
              "rating":8.25,"genres":["Drama"],"addonId":"tmdb","stremioType":"hentai"}]
            """.trimIndent(),
        ).single()
        val restored = LibraryCodec.decodeFavorites(LibraryCodec.encodeFavorites(listOf(original))).single()
        assertEquals(original, restored)
        assertFalse(restored.genres.isEmpty())
    }
}
