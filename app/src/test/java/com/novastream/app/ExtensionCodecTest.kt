package com.novastream.app

import com.novastream.app.data.ext.ExtensionCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Regression tests for the Sources dashboard crash (Phase 21).
 *
 * Gson maps a missing JSON field onto a Kotlin non-null slot as a silent `null`, so an
 * `extensions.json` entry written by an older build — or truncated mid-write — decoded into an
 * `ExtensionDescriptor` whose `name`/`id` were null at runtime. The dashboard rendered `desc.name`
 * through `Text(...)` (an NPE from the compiler's non-null parameter check) and used `desc.id` as a
 * `LazyColumn` key, so opening Settings → Sources dashboard crashed on entry.
 */
class ExtensionCodecTest {

    @Test
    fun `descriptor without a name falls back to its id instead of a null`() {
        val json = """[{"id":"demo","entryClass":"com.x.Demo","apkPath":"/data/demo.apk"}]"""
        val ext = ExtensionCodec.decode(json).single()
        // A null here is the original crash: `Text(desc.name)` throws when name is null.
        assertEquals("demo", ext.name)
        assertEquals("demo", ext.id)
    }

    @Test
    fun `explicit null fields are treated as absent`() {
        val json = """[{"id":"demo","name":null,"version":null,"entryClass":"C","apkPath":"/p"}]"""
        val ext = ExtensionCodec.decode(json).single()
        assertEquals("demo", ext.name)
        assertNull(ext.version)
    }

    @Test
    fun `version is optional`() {
        val ext = ExtensionCodec.decode("""{"id":"a","entryClass":"C","apkPath":"/p"}""").single()
        assertNull(ext.version)
    }

    @Test
    fun `entry missing any required field is dropped while the rest survive`() {
        val json = """
            [
                {"id":"ok","name":"Kept","entryClass":"C","apkPath":"/p"},
                {"name":"No id","entryClass":"C","apkPath":"/p"},
                {"id":"no-entry","name":"No entry class","apkPath":"/p"},
                {"id":"no-apk","name":"No apk path","entryClass":"C"},
                {"id":"","name":"Blank id","entryClass":"C","apkPath":"/p"},
                {"id":"ok-2","name":"Also kept","entryClass":"C","apkPath":"/p"}
            ]
        """.trimIndent()
        val installed = ExtensionCodec.decode(json)
        assertEquals(listOf("ok", "ok-2"), installed.map { it.id })
    }

    @Test
    fun `duplicate ids keep only the first occurrence`() {
        // A duplicate id would collide as a LazyColumn key — the other half of the crash.
        val json = """
            [
                {"id":"same","name":"First","entryClass":"C","apkPath":"/p"},
                {"id":"same","name":"Second","entryClass":"C","apkPath":"/p"}
            ]
        """.trimIndent()
        val installed = ExtensionCodec.decode(json)
        assertEquals(1, installed.size)
        assertEquals("First", installed.single().name)
    }

    @Test
    fun `unusable payloads decode to an empty list instead of throwing`() {
        for (payload in listOf(null, "", "   ", "{not json", "\"demo\"", "42", "[1,2,3]", "{}")) {
            assertTrue("payload=$payload", ExtensionCodec.decode(payload).isEmpty())
        }
    }

    @Test
    fun `a single bare object is tolerated as a one-entry install list`() {
        val ext = ExtensionCodec.decode("""{"id":"solo","name":"Solo","entryClass":"C","apkPath":"/p"}""").single()
        assertEquals("solo", ext.id)
        assertEquals("Solo", ext.name)
    }

    @Test
    fun `encode and decode round-trip a well-formed descriptor`() {
        val original = ExtensionCodec.decode(
            """{"id":"demo","name":"Demo source","version":"1.2.3","entryClass":"com.x.Demo","apkPath":"/data/demo.apk"}""",
        ).single()
        val restored = ExtensionCodec.decode(ExtensionCodec.encode(listOf(original))).single()
        assertEquals(original, restored)
    }
}
