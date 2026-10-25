package com.moovio.desktop

import com.moovio.desktop.data.net.parseJsonObject
import com.moovio.desktop.data.remote.StremioClient
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StremioManifestTest {

    private val client = StremioClient(com.moovio.desktop.data.net.Http())

    @Test
    fun `parses manifest catalogs, prefixes and resources`() {
        val json = parseJsonObject(
            """
            {
              "id": "org.example.streams",
              "version": "1.4.0",
              "name": "Example Streams",
              "description": "A test add-on",
              "types": ["movie", "series"],
              "resources": [
                "catalog",
                {"name": "stream", "types": ["movie", "series"], "idPrefixes": ["tt"]}
              ],
              "catalogs": [
                {"type": "movie", "id": "top", "name": "Top", "extra": [{"name": "search", "isRequired": true}]}
              ],
              "idPrefixes": ["tt"]
            }
            """.trimIndent(),
        )

        val addon = client.parseManifest(json, "https://example.com", "https://example.com/manifest.json")
        assertEquals("Example Streams", addon.name)
        assertEquals("1.4.0", addon.version)
        assertTrue(addon.supportsCatalog)
        assertTrue(addon.supportsStreams)
        assertTrue(addon.types.contains("movie"))
        assertEquals(listOf("tt"), addon.idPrefixes)
        assertEquals("Top", addon.catalogs.single().name)
        assertTrue(addon.catalogs.single().searchable)
    }

    @Test
    fun `treats malformed optional fields as empty`() {
        val json = parseJsonObject("""{"id":"x","name":"Minimal"}""")
        val addon = client.parseManifest(json, "https://x.test/", "https://x.test/manifest.json")
        assertEquals("Minimal", addon.name)
        assertTrue(addon.catalogs.isEmpty())
        assertTrue(addon.resources.isEmpty())
    }

    @Test
    fun `normalizes manifest base urls`() {
        assertEquals("https://example.com", client.base("https://example.com/manifest.json"))
        assertEquals("https://example.com", client.base("https://example.com/"))
        assertEquals("https://example.com/addon", client.base("https://example.com/addon/manifest.json"))
    }
}
