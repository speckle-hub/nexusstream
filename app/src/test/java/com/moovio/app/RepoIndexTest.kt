package com.moovio.app

import com.moovio.app.data.model.MangaExt
import com.moovio.app.data.remote.AniyomiRepo
import com.moovio.app.data.remote.KeiyoushiRepo
import com.moovio.app.data.repo.MangaExtensionRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

/**
 * Provider-repository plumbing (v1.10.2): APK URL resolution, the current Keiyoushi protobuf
 * format, the NSFW rating enum and the extension update policy.
 *
 * Everything here is pure JVM — the live indexes were probed separately (see roadmap §2ba):
 * Keiyoushi serves 1425 extensions in a repository wrapper (nsfw at `Extension.7 == 3`), yuzono
 * serves 251 JSON entries (72 `nsfw`), and every Aniyomi APK lives at `<repo dir>/apk/<name>`.
 */
class RepoIndexTest {

    // ---- Aniyomi / Mihon APK URLs ----------------------------------------------

    @Test
    fun `a bare apk name resolves into the repository apk directory`() {
        assertEquals(
            "https://raw.githubusercontent.com/yuzono/anime-repo/repo/apk/aniyomi-all.x-v14.4.apk",
            AniyomiRepo.resolveApk(
                "https://raw.githubusercontent.com/yuzono/anime-repo/repo/index.min.json",
                "aniyomi-all.x-v14.4.apk",
            ),
        )
    }

    @Test
    fun `absolute apk urls are left untouched`() {
        val absolute = "https://github.com/keiyoushi/extensions/releases/download/abc/tachiyomi-all.x-v1.6.4.apk"
        assertEquals(
            absolute,
            AniyomiRepo.resolveApk("https://github.com/keiyoushi/extensions/raw/repo/index.pb", absolute),
        )
    }

    @Test
    fun `a blank apk stays blank`() {
        assertEquals("", AniyomiRepo.resolveApk("https://example.com/repo/index.min.json", "  "))
    }

    // ---- Keiyoushi index.pb (current repository-wrapper format) -----------------

    @Test
    fun `the wrapped keiyoushi index decodes extensions with apk, sources and version`() {
        val source = bytesOf(
            fieldVarint(1, 42L),
            fieldString(2, "MangaDex"),
            fieldString(3, "en"),
            fieldString(4, "https://mangadex.org"),
        )
        val extension = bytesOf(
            fieldString(1, "MangaDex"),
            fieldString(2, "eu.kanade.tachiyomi.extension.all.mangadex"),
            // apk is a nested message holding the URL at field 1.
            fieldBytes(3, fieldString(1, "https://example.com/mangadex.apk")),
            fieldString(4, "1.6"),
            fieldVarint(5, 106000L),
            fieldString(6, "1.6.0"),
            fieldVarint(7, 2L),
            fieldBytes(8, source),
        )
        val repository = bytesOf(
            fieldString(1, "Keiyoushi"),
            fieldString(2, "KEI"),
            fieldString(3, "9add655a78e96c4e"),
            fieldBytes(101, fieldBytes(1, extension)),
        )

        val list = KeiyoushiRepo.parsePb(repository, "https://example.com/repo/index.pb")
        assertEquals(1, list.size)
        val ext = list[0]
        assertEquals("MangaDex", ext.name)
        assertEquals("eu.kanade.tachiyomi.extension.all.mangadex", ext.pkg)
        assertEquals("https://example.com/mangadex.apk", ext.apk)
        assertEquals("1.6.0", ext.version)
        assertEquals(106000L, ext.versionCode)
        assertEquals(1, ext.sources.size)
        assertEquals("MangaDex", ext.sources[0].name)
        assertEquals("en", ext.sources[0].lang)
        assertEquals("https://mangadex.org", ext.sources[0].baseUrl)
        assertFalse("rating 2 is not adult", ext.nsfw)
        assertEquals("https://example.com/repo/index.pb", ext.repoUrl)
    }

    @Test
    fun `the adult rating in the index marks the extension NSFW`() {
        fun entry(name: String, rating: Long) = fieldBytes(
            1,
            bytesOf(
                fieldString(1, name),
                fieldString(2, "eu.kanade.tachiyomi.extension.all.$name"),
                fieldBytes(3, fieldString(1, "https://example.com/$name.apk")),
                fieldVarint(5, 1L),
                fieldVarint(7, rating),
            ),
        )

        val repository = fieldBytes(101, entry("HentaiFox", 3L) + entry("Asura Scans", 1L))
        val list = KeiyoushiRepo.parsePb(repository).associateBy { it.name }
        assertTrue(list.getValue("HentaiFox").nsfw)
        assertFalse(list.getValue("Asura Scans").nsfw)
    }

    @Test
    fun `the repository wrapper is not mistaken for extensions`() {
        // Before this fix every top-level field was parsed as an extension, so the repo name, its
        // key fingerprint and the website blob showed up as junk rows.
        val repository = bytesOf(
            fieldString(1, "Keiyoushi"),
            fieldString(3, "9add655a78e96c4ec7a53ef89dccb557cb5d767489fac5e785d671a5a75d4da2"),
            fieldString(4, "https://keiyoushi.github.io"),
        )
        assertTrue(KeiyoushiRepo.parsePb(repository).isEmpty())
    }

    // ---- Update policy ---------------------------------------------------------

    @Test
    fun `an update is offered only for an installed extension with a newer code`() {
        val installed = MangaExt(
            name = "X", pkg = "pkg.x", apk = "https://e/x.apk",
            versionCode = 10L, apkPath = "/data/files/x.apk",
        )
        assertTrue(MangaExtensionRepository.isUpdateAvailable(installed, installed.copy(versionCode = 11L)))
        assertFalse(MangaExtensionRepository.isUpdateAvailable(installed, installed.copy(versionCode = 10L)))
        assertFalse(MangaExtensionRepository.isUpdateAvailable(installed, installed.copy(versionCode = 9L)))
    }

    @Test
    fun `uninstalled or unversioned entries never report an update`() {
        val candidate = MangaExt(name = "X", pkg = "pkg.x", apk = "https://e/x.apk", versionCode = 99L)
        assertFalse(MangaExtensionRepository.isUpdateAvailable(null, candidate))
        // Downloaded descriptor without a stored APK (metadata only) is not "installed".
        assertFalse(MangaExtensionRepository.isUpdateAvailable(candidate.copy(apkPath = null), candidate))
        // Missing version codes give nothing to compare.
        assertFalse(
            MangaExtensionRepository.isUpdateAvailable(
                MangaExt(name = "X", pkg = "p.x", apk = "a", apkPath = "/p", versionCode = null),
                candidate,
            ),
        )
    }

    // ---- Protobuf helpers ------------------------------------------------------

    private fun varint(value: Long): ByteArray {
        var v = value
        val out = ByteArrayOutputStream()
        while (true) {
            val b = (v and 0x7F).toInt()
            v = v ushr 7
            if (v == 0L) { out.write(b); return out.toByteArray() }
            out.write(b or 0x80)
        }
    }

    private fun fieldVarint(number: Int, value: Long): ByteArray =
        varint((number shl 3).toLong()) + varint(value)

    private fun fieldString(number: Int, value: String): ByteArray {
        val bytes = value.toByteArray(Charsets.UTF_8)
        return varint((number shl 3).toLong() or 2L) + varint(bytes.size.toLong()) + bytes
    }

    private fun fieldBytes(number: Int, bytes: ByteArray): ByteArray =
        varint((number shl 3).toLong() or 2L) + varint(bytes.size.toLong()) + bytes

    private fun bytesOf(vararg parts: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        parts.forEach { out.write(it) }
        return out.toByteArray()
    }
}
