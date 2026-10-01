package com.novastream.app.data.remote

import com.novastream.app.data.model.MangaExt
import com.novastream.app.data.model.MangaSource

/**
 * Parser for the Mihon / Keiyoushi extension index (`index.pb`).
 *
 * The file is a protobuf `ExtensionIndex { repeated Extension extensions = 1 }`.
 * Each Extension carries name/pkg/apk/lang/code/version/nsfw/icon/sources. We decode
 * generically and apply light heuristics so the client keeps working even if the
 * upstream schema gains fields.
 */
object KeiyoushiRepo {

    suspend fun fetch(repoUrl: String): List<MangaExt> {
        // Prefer the JSON mirror when present (cheaper + exact); fall back to the .pb.
        val jsonUrl = repoUrl.replace("index.pb", "index.min.json")
        if (jsonUrl != repoUrl) {
            try {
                val list = AniyomiRepo.fetch(jsonUrl)
                if (list.isNotEmpty()) return list
            } catch (_: Exception) { /* fall through to pb */ }
        }
        // index.pb is served gzip-compressed; inflate it before decoding the wire format.
        val bytes = maybeGunzip(httpGetBytes(repoUrl))
        return parsePb(bytes)
    }

    fun parsePb(bytes: ByteArray): List<MangaExt> {
        val top = Protobuf.decode(bytes)
        val out = ArrayList<MangaExt>()
        for (f in top) {
            if (f.wireType == 2 && f.bytes != null) {
                // field 1 = extensions (repeated). Accept any length-delimited entry.
                val ext = parseExtension(f.bytes)
                if (ext != null) out.add(ext)
            }
        }
        // Some builds wrap in a single nested message; retry one level if nothing found.
        if (out.isEmpty()) {
            for (f in top) {
                if (f.wireType == 2 && f.bytes != null) {
                    val inner = Protobuf.decode(f.bytes)
                    for (g in inner) {
                        if (g.wireType == 2 && g.bytes != null) parseExtension(g.bytes)?.let { out.add(it) }
                    }
                }
            }
        }
        return out
    }

    private fun parseExtension(bytes: ByteArray): MangaExt? {
        val fields = Protobuf.decode(bytes)
        val strings = LinkedHashMap<Int, String>()
        var code = 0L
        var nsfw = false
        val sources = ArrayList<MangaSource>()

        for (f in fields) {
            when (f.wireType) {
                2 -> {
                    val s = f.asString ?: continue
                    if (f.number == 9 || (f.number > 8 && looksLikeNestedSource(f.bytes))) {
                        parseSource(f.bytes!!)?.let { sources.add(it) }
                    } else {
                        strings[f.number] = s
                    }
                }
                0 -> when (f.number) {
                    5 -> code = f.varint
                    7 -> nsfw = f.asBool
                }
            }
        }

        val name = strings[1] ?: strings.values.firstOrNull { it.isNotBlank() && !it.startsWith("http") } ?: return null
        val pkg = strings[2] ?: strings.values.firstOrNull { it.contains(".") && !it.startsWith("http") } ?: ""
        var apk = strings[3] ?: strings.values.firstOrNull { it.startsWith("http") && it.endsWith(".apk") } ?: ""
        if (apk.isBlank()) apk = strings.values.firstOrNull { it.startsWith("http") } ?: ""
        val lang = strings[4]
        val version = strings[6] ?: strings.values.firstOrNull { it.matches(Regex("\\d+\\.\\d+.*")) }

        return MangaExt(
            name = name,
            pkg = pkg,
            apk = apk,
            icon = strings[8],
            version = version,
            lang = lang,
            nsfw = nsfw || StremioClient.isNsfw(name, null, pkg),
            sources = sources,
        )
    }

    private fun looksLikeNestedSource(bytes: ByteArray?): Boolean {
        if (bytes == null || bytes.size < 3) return false
        val inner = Protobuf.decode(bytes)
        return inner.any { it.wireType == 2 && it.asString?.startsWith("http") == true } ||
            inner.any { it.number == 2 && it.wireType == 2 }
    }

    private fun parseSource(bytes: ByteArray): MangaSource? {
        val fields = Protobuf.decode(bytes)
        var id = 0L
        var name = ""
        var lang: String? = null
        var baseUrl: String? = null
        for (f in fields) {
            when (f.wireType) {
                0 -> if (f.number == 1) id = f.varint
                2 -> when (f.number) {
                    2 -> name = f.asString ?: name
                    3 -> lang = f.asString
                    4 -> baseUrl = f.asString
                }
            }
        }
        if (name.isBlank()) return null
        return MangaSource(id, name, lang, baseUrl)
    }
}
