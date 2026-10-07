package com.novastream.app

import com.novastream.app.data.adult.adultSignificantTokens
import com.novastream.app.data.adult.adultTitlesMatch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the matcher that decides "is this the same title as the one the user selected?".
 *
 * It exists because the old any-token overlap let a loose site/Jikan search result count as a
 * match — filler word "to" is a substring of "Netokano", so "Boku to Misaki-sensei" resolved to
 * a completely different series. These cases lock in both directions: real matches stay permissive
 * (punctuation, case, word order, romaji variants), unrelated titles never match.
 */
class TitleMatchTest {

    @Test
    fun `filler words are stripped from significant tokens`() {
        assertEquals(listOf("boku", "misaki", "sensei"), adultSignificantTokens("Boku to Misaki-sensei"))
        assertEquals(listOf("party"), adultSignificantTokens("The Party of To"))
    }

    @Test
    fun `filler-only query falls back to the raw tokens so it never matches nothing`() {
        // Degenerate queries stay permissive — the fallback exists so a query made only of
        // stopwords can't silently reject everything.
        assertEquals(listOf("to"), adultSignificantTokens("to"))
        assertTrue(adultTitlesMatch("to", "Anything With To"))
    }

    @Test
    fun `the reported wrong-title case does not match`() {
        assertFalse(adultTitlesMatch("Boku to Misaki-sensei", "Netokano: After Party"))
        assertFalse(adultTitlesMatch("Boku to Misaki-sensei", "Netokano After Party Episode 3"))
    }

    @Test
    fun `punctuation case and word order differences still match`() {
        assertTrue(adultTitlesMatch("Boku to Misaki-sensei", "Boku to Misaki Sensei!!"))
        assertTrue(adultTitlesMatch("Boku to Misaki-sensei", "boku to misaki-sensei"))
        // Shared significant word on either side of a subtitle/season suffix.
        assertTrue(adultTitlesMatch("Boku to Misaki-sensei", "Boku to Misaki-sensei: Another Page"))
        assertTrue(adultTitlesMatch("Boku to Misaki-sensei: Another Page", "Boku to Misaki-sensei"))
    }

    @Test
    fun `a word only in the candidate cannot fake a match`() {
        // "to" appearing in the *candidate* must not matter when the expected title's real
        // keywords are absent.
        assertFalse(adultTitlesMatch("Netokano", "Something to Something"))
    }

    @Test
    fun `a title with no usable keywords never claims a match`() {
        // Nothing to compare -> the caller must fall back to what the user actually selected.
        assertFalse(adultTitlesMatch("a", "Anything At All"))
        assertFalse(adultTitlesMatch("", "Anything At All"))
    }

    @Test
    fun `matching is case insensitive and whitespace tolerant`() {
        assertTrue(adultTitlesMatch("  BOKU to Misaki-Sensei ", "boku  to misaki sensei"))
    }
}
