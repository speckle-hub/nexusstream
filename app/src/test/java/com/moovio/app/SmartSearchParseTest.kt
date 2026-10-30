package com.moovio.app

import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.data.search.SmartSearchParser
import com.moovio.app.data.search.SmartSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The offline query compiler behind SearchScreen. `currentYear` is pinned so the relative filters
 * ("recent", the year ceiling) are deterministic.
 */
class SmartSearchParseTest {

    private val now = 2026

    private fun item(
        id: String,
        type: MediaType = MediaType.MOVIE,
        year: String? = null,
        rating: Double? = null,
        genres: List<String> = emptyList(),
    ) = MediaItem(id = id, type = type, title = id, year = year, rating = rating, genres = genres)

    // ---- parsing ---------------------------------------------------------------

    @Test
    fun `type, year and rating words compile together`() {
        val q = SmartSearchParser.parse("anime 2024 high rating", now)

        assertEquals(MediaType.ANIME, q.type)
        assertEquals(2024, q.yearFrom)
        assertEquals(2024, q.yearTo)
        assertEquals(7.0, q.minRating!!, 0.001)
        assertEquals(SmartSort.RATING, q.sort)
        assertEquals("no leftover text to search for", "", q.text)
        assertTrue(q.hasFilters)
        assertEquals("Anime · 2024 · rated 7+ · best rated first", q.summary())
    }

    @Test
    fun `a genre word becomes a filter and a search term`() {
        val q = SmartSearchParser.parse("scifi movies", now)

        assertEquals(MediaType.MOVIE, q.type)
        assertEquals("Science Fiction", q.genre)
        // Nothing but filters was typed, so the genre itself has to be the query the provider sees.
        assertEquals("science fiction", q.text)
    }

    @Test
    fun `recent narrows the year window and sorts newest first`() {
        val q = SmartSearchParser.parse("recent action", now)

        assertEquals("Action", q.genre)
        assertEquals(now - 2, q.yearFrom)
        assertNull(q.yearTo)
        assertEquals(SmartSort.RECENT, q.sort)
        assertEquals("action", q.text)
    }

    @Test
    fun `a title ending in a number is not turned into a year filter`() {
        val q = SmartSearchParser.parse("blade runner 2049", now)

        assertNull("2049 is not a released year yet", q.yearFrom)
        assertNull(q.yearTo)
        assertEquals("blade runner 2049", q.text)
        assertFalse(q.hasFilters)
        assertNull(q.summary())
    }

    @Test
    fun `a decade expands to its bounds`() {
        val q = SmartSearchParser.parse("2010s comedy", now)

        assertEquals(2010, q.yearFrom)
        assertEquals(2019, q.yearTo)
        assertEquals("Comedy", q.genre)
    }

    @Test
    fun `two years read as a range`() {
        val q = SmartSearchParser.parse("movies 2010 2015", now)

        assertEquals(MediaType.MOVIE, q.type)
        assertEquals(2010, q.yearFrom)
        assertEquals(2015, q.yearTo)
    }

    @Test
    fun `an explicit score overrides the default floor`() {
        val q = SmartSearchParser.parse("rating 8.5", now)

        assertEquals(8.5, q.minRating!!, 0.001)
        assertEquals(SmartSort.RATING, q.sort)
        assertEquals("rating vocabulary never reaches the provider", "", q.text)
    }

    @Test
    fun `plain text is left alone`() {
        val q = SmartSearchParser.parse("one piece", now)

        assertEquals("one piece", q.text)
        assertNull(q.type)
        assertNull(q.genre)
        assertFalse(q.hasFilters)
    }

    @Test
    fun `an empty query has no filters and no text`() {
        val q = SmartSearchParser.parse("", now)
        assertEquals("", q.text)
        assertFalse(q.hasFilters)
    }

    // ---- applying the compiled filters ----------------------------------------

    @Test
    fun `year, type, genre and rating narrow the result set`() {
        val results = listOf(
            item("match", year = "2024", rating = 8.2, genres = listOf("Action")),
            item("too old", year = "1999", rating = 9.0, genres = listOf("Action")),
            item("wrong type", type = MediaType.SERIES, year = "2024", rating = 7.5, genres = listOf("Action")),
            item("no year", year = null, rating = 8.0, genres = listOf("Action")),
            item("wrong genre", year = "2024", rating = 8.0, genres = listOf("Drama")),
            item("too low", year = "2024", rating = 6.1, genres = listOf("Action")),
        )

        val filtered = SmartSearchParser.parse("action movies 2024 high rating", now).applyTo(results)

        assertEquals(listOf("match"), filtered.map { it.id })
    }

    @Test
    fun `genre matching accepts the spellings add-ons actually use`() {
        val results = listOf(
            item("a", genres = listOf("Sci-Fi")),
            item("b", genres = listOf("Science Fiction")),
            item("c", genres = listOf("scifi")),
            item("d", genres = listOf("Drama")),
        )

        val filtered = SmartSearchParser.parse("scifi", now).applyTo(results)

        assertEquals(listOf("a", "b", "c"), filtered.map { it.id })
    }

    @Test
    fun `rating sort puts the best first and recent sort the newest`() {
        // Every rating clears the 7.0 floor, so this isolates the two sort orders from the filter.
        val results = listOf(
            item("old best", year = "2024", rating = 9.1),
            item("new ok", year = "2026", rating = 7.5),
            item("mid", year = "2025", rating = 7.4),
        )

        assertEquals(
            listOf("old best", "new ok", "mid"),
            SmartSearchParser.parse("high rating", now).applyTo(results).map { it.id },
        )
        assertEquals(
            listOf("new ok", "mid", "old best"),
            SmartSearchParser.parse("recent", now).applyTo(results).map { it.id },
        )
    }

    @Test
    fun `no filters leaves the provider order untouched`() {
        val results = listOf(item("b"), item("a"))
        assertEquals(listOf("b", "a"), SmartSearchParser.parse("dune", now).applyTo(results).map { it.id })
    }
}
