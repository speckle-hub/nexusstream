package com.novastream.app

import com.google.gson.JsonParser
import com.novastream.app.data.remote.CloudStreamRepo
import com.novastream.app.data.remote.KeiyoushiRepo
import com.novastream.app.data.remote.Protobuf
import com.novastream.app.data.remote.StremioClient
import com.novastream.app.data.remote.maybeGunzip
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

/**
 * Pure-JVM parser tests (no Android/network). These cover the pieces most likely to break
 * silently: the Keiyoushi protobuf reader + gzip handling, the CloudStream provider parser,
 * the Stremio manifest parser, and NSFW detection.
 */
class ParsersTest {

    // ---- Protobuf wire format --------------------------------------------------

    private fun varint(v: Long): ByteArray {
        val out = ArrayList<Byte>()
        var x = v
        while (true) {
            if (x and 0x7FL.inv() == 0L) {
                out.add(x.toByte()); break
            }
            out.add(((x and 0x7F) or 0x80).toByte())
            x = x ushr 7
        }
        return out.toByteArray()
    }

    private fun fieldString(num: Int, s: String): ByteArray {
        val b = s.toByteArray(Charsets.UTF_8)
        return varint(((num shl 3) or 2).toLong()) + varint(b.size.toLong()) + b
    }

    private fun fieldVarint(num: Int, v: Long): ByteArray = varint(((num shl 3) or 0).toLong()) + varint(v)

    private fun fieldBytes(num: Int, b: ByteArray): ByteArray =
        varint(((num shl 3) or 2).toLong()) + varint(b.size.toLong()) + b

    private fun bytesOf(vararg parts: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        parts.forEach { out.write(it) }
        return out.toByteArray()
    }

    @Test
    fun `protobuf decodes a length-delimited string field`() {
        val data = byteArrayOf(0x0A, 0x02, 'h'.code.toByte(), 'i'.code.toByte())
        val fields = Protobuf.decode(data)
        assertEquals(1, fields.size)
        assertEquals(1, fields[0].number)
        assertEquals(2, fields[0].wireType)
        assertEquals("hi", fields[0].asString)
    }

    // ---- gzip handling ---------------------------------------------------------

    @Test
    fun `maybeGunzip inflates gzipped bytes`() {
        val raw = "hello nova".toByteArray(Charsets.UTF_8)
        val gz = ByteArrayOutputStream().apply { GZIPOutputStream(this).use { it.write(raw) } }.toByteArray()
        assertArrayEquals(raw, maybeGunzip(gz))
    }

    @Test
    fun `maybeGunzip passes plain bytes through untouched`() {
        val raw = byteArrayOf(0x0A, 0x02, 0x68, 0x69)
        assertArrayEquals(raw, maybeGunzip(raw))
    }

    // ---- Keiyoushi index.pb ----------------------------------------------------

    @Test
    fun `keiyoushi protobuf index decodes into a MangaExt`() {
        val extension = bytesOf(
            fieldString(1, "TestExt"),
            fieldString(2, "com.example.test"),
            fieldString(3, "https://example.com/test.apk"),
            fieldString(4, "en"),
            fieldVarint(5, 7L),
            fieldString(6, "1.2.3"),
            fieldString(8, "https://example.com/icon.png"),
        )
        val index = fieldBytes(1, extension)

        val list = KeiyoushiRepo.parsePb(index)
        assertEquals(1, list.size)
        val ext = list[0]
        assertEquals("TestExt", ext.name)
        assertEquals("com.example.test", ext.pkg)
        assertEquals("https://example.com/test.apk", ext.apk)
        assertEquals("en", ext.lang)
        assertEquals("1.2.3", ext.version)
        assertEquals("https://example.com/icon.png", ext.icon)
        assertFalse(ext.nsfw)
    }

    @Test
    fun `keiyoushi index survives being gzip-compressed first`() {
        val extension = bytesOf(
            fieldString(1, "GzExt"),
            fieldString(2, "com.example.gz"),
            fieldString(3, "https://example.com/gz.apk"),
        )
        val index = fieldBytes(1, extension)
        val gz = ByteArrayOutputStream().apply { GZIPOutputStream(this).use { it.write(index) } }.toByteArray()

        val list = KeiyoushiRepo.parsePb(maybeGunzip(gz))
        assertEquals(1, list.size)
        assertEquals("GzExt", list[0].name)
    }

    // ---- Stremio manifest ------------------------------------------------------

    @Test
    fun `stremio manifest parses catalogs and search extra`() {
        val json = JsonParser.parseString(
            """
            {"id":"com.test.addon","name":"Test Addon","types":["movie"],
             "resources":["catalog","meta"],
             "catalogs":[{"type":"movie","id":"top","name":"Top",
               "extra":[{"name":"search","isRequired":false}]}]}
            """.trimIndent()
        ).asJsonObject

        val addon = StremioClient.parseManifest(json, "https://example.com/manifest.json")
        assertEquals("com.test.addon", addon.id)
        assertEquals("Test Addon", addon.name)
        assertEquals(1, addon.catalogs.size)
        assertEquals("top", addon.catalogs[0].id)
        assertTrue(addon.catalogs[0].extra.any { it.name == "search" })
        assertFalse(addon.nsfw)
    }

    @Test
    fun `stremio manifest flags NSFW add-ons`() {
        val json = JsonParser.parseString(
            """{"id":"org.example.porn","name":"XXX Porn Addon","types":["movie"],"resources":["stream"]}"""
        ).asJsonObject
        assertTrue(StremioClient.parseManifest(json, "https://x/manifest.json").nsfw)
        assertTrue(StremioClient.isNsfw("Hentai Streams", null, null))
        assertFalse(StremioClient.isNsfw("Cinemeta", "official catalogs", "com.linvo.cinemeta"))
    }

    // ---- CloudStream provider --------------------------------------------------

    @Test
    fun `cloudstream provider entry parses all fields`() {
        val o = JsonParser.parseString(
            """
            {"name":"My Provider","url":"https://example.com/My.cs3","description":"desc",
             "iconUrl":"https://example.com/i.png","version":25,"language":"en",
             "internalName":"MyProvider","tvTypes":["Movie","TvSeries"]}
            """.trimIndent()
        ).asJsonObject

        val ext = CloudStreamRepo.parse(o)!!
        assertEquals("My Provider", ext.name)
        assertEquals("https://example.com/My.cs3", ext.url)
        assertEquals("https://example.com/i.png", ext.icon)
        assertEquals("25", ext.version)
        assertEquals("en", ext.language)
        assertEquals(listOf("Movie", "TvSeries"), ext.tvTypes)
    }

    @Test
    fun `cloudstream provider without a url is rejected`() {
        val o = JsonParser.parseString("""{"name":"No Url","description":"x"}""").asJsonObject
        assertEquals(null, CloudStreamRepo.parse(o))
    }
}
