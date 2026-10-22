package com.moovio.app

import com.moovio.app.data.model.CatalogRow
import com.moovio.app.data.model.MediaItem
import com.moovio.app.data.model.MediaType
import com.moovio.app.ui.screens.settings.applyRowOrder
import com.moovio.app.ui.screens.settings.applyRowVisibility
import com.moovio.app.ui.screens.settings.pinRowToTop
import org.junit.Assert.assertEquals
import org.junit.Test

/** Phase 12/30 home dashboard: row ordering, hiding and pin-to-top (pure logic). */
class HomeRowsTest {

    private fun row(title: String) = CatalogRow(
        title = title,
        items = listOf(MediaItem(id = title, type = MediaType.MOVIE, title = title)),
    )

    @Test
    fun `empty order keeps the natural row order`() {
        val rows = listOf(row("A"), row("B"))
        assertEquals(listOf("A", "B"), applyRowOrder(rows, emptyList()).map { it.title })
    }

    @Test
    fun `saved order sorts known rows first`() {
        val rows = listOf(row("A"), row("B"), row("C"))
        assertEquals(
            listOf("C", "A", "B"),
            applyRowOrder(rows, listOf("C", "A")).map { it.title },
        )
    }

    @Test
    fun `hidden rows are dropped`() {
        val rows = listOf(row("A"), row("B"), row("C"))
        assertEquals(
            listOf("A", "C"),
            applyRowVisibility(rows, setOf("B")).map { it.title },
        )
    }

    @Test
    fun `empty hidden set keeps everything`() {
        val rows = listOf(row("A"), row("B"))
        assertEquals(2, applyRowVisibility(rows, emptySet()).size)
    }

    @Test
    fun `hiding is exact match only`() {
        val rows = listOf(row("Trending Now"), row("Trending"))
        assertEquals(
            listOf("Trending Now"),
            applyRowVisibility(rows, setOf("Trending")).map { it.title },
        )
    }

    @Test
    fun `pin moves the title to the front`() {
        assertEquals(
            listOf("C", "A", "B"),
            pinRowToTop(listOf("A", "B", "C"), "C"),
        )
    }

    @Test
    fun `pin deduplicates an already ordered title`() {
        assertEquals(
            listOf("B", "A", "C"),
            pinRowToTop(listOf("A", "B", "C"), "B"),
        )
    }

    @Test
    fun `order then visibility composes`() {
        val rows = listOf(row("A"), row("B"), row("C"))
        val ordered = applyRowOrder(rows, listOf("C", "A", "B"))
        assertEquals(
            listOf("C", "B"),
            applyRowVisibility(ordered, setOf("A")).map { it.title },
        )
    }
}
