package com.novastream.app

import com.novastream.app.data.download.MangaDownload
import com.novastream.app.data.download.mangaPageFileNames
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MangaDownloadTest {

    @Test
    fun `filenames preserve order and infer extension`() {
        val names = mangaPageFileNames(
            listOf(
                "https://cdn/p1.jpg",
                "https://cdn/p2.png",
                "https://cdn/p3.webp?token=abc",
                "https://cdn/no-extension",
            )
        )
        assertEquals(
            listOf("page_0000.jpg", "page_0001.png", "page_0002.webp", "page_0003.jpg"),
            names,
        )
    }

    @Test
    fun `unknown extension falls back to jpg`() {
        val names = mangaPageFileNames(listOf("https://cdn/page.tiff"))
        assertEquals("page_0000.jpg", names.first())
    }

    @Test
    fun `progress is derived from pages done`() {
        val d = MangaDownload(
            mangaId = "m",
            mangaTitle = "Manga",
            chapterId = "c",
            chapterTitle = "Chapter 1",
            total = 10,
            downloaded = 5,
        )
        assertEquals(0.5f, d.progress, 0.0001f)
        assertTrue(d.key == "m::c")
    }
}
