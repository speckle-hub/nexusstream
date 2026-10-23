package com.moovio.app.data.remote

import com.moovio.app.data.model.MangaExt
import com.moovio.app.data.model.MangaSource

/**
 * Parser for the Mihon / Keiyoushi extension index.
 *
 * ### Wire format (verified live 2026-10-23)
 * `index.pb` is gzip-compressed and decodes as a **repository** message, not a bare list:
 *
 * ```
 * Repository {                     // Keiyoushi's current format
 *   name = 1, shortCode = 2, signingKeyFingerprint = 3, website/discord = 4,
 *   repeated Extension extensions = 101
 * }
 * Extension {
 *   name = 1, pkg = 2,
 *   apk = 3 { url = 1, ... }        // nested message, NOT a bare string
 *   versionLib = 4, code = 5, version = 6,
 *   nsfw = 7 (enum: 3 == adult)     // 1/2 are SFW ratings, 3 is the adult catalogue
 *   repeated Source sources = 8 { id = 1, name = 2, lang = 3, baseUrl = 4 }
 * }
 * ```
 *
 * The previous implementation treated every top-level length-delimited field as an extension, so
 * the real index (1425 extensions) produced a handful of junk rows (the repo name, the key
 * fingerprint, the website blob and the whole extension array). The legacy shape — a bare list of
 * extensions at the top level — is still accepted, because the `ParsersTest` fixture uses it and
 * older mirrors may too.
 *
 * `index.min.json` in the Keiyoushi repo is a **stub** (2 "Outdated App" records), and the old
 * client preferred it whenever it parsed non-empty, so the tab showed nothing usable. The `fetch`
 * path is now driven by the URL: `.pb` → protobuf, anything else → JSON.
 */
object KeiyoushiRepo {

    /** `nsfw` enum value that marks an extension as adult in the current index format. */
    private const val NSFW_ENUM_ADULT = 3L

    suspend fun fetch(repoUrl: String): List<MangaExt> {
        val isPb = repoUrl.contains("index.pb", ignoreCase = true)
        if (!isPb) return AniyomiRepo.fetch(repoUrl)
        // index.pb is served gzip-compressed; inflate it before decoding the wire format.
        val bytes = maybeGunzip(httpGetBytes(repoUrl))
        val parsed = parsePb(bytes, repoUrl)
        if (parsed.isNotEmpty()) return parsed
        // A mislabelled repo: fall back to the JSON mirror only when the protobuf path yields
        // nothing at all (the published JSON is a stub, so it is a last resort, not a preference).
        val jsonUrl = repoUrl.replace("index.pb", "index.min.json")
        return if (jsonUrl != repoUrl) AniyomiRepo.fetch(jsonUrl) else emptyList()
    }

    fun parsePb(bytes: ByteArray, repoUrl: String? = null): List<MangaExt> {
        val top = Protobuf.decode(bytes)
        // Current format: the extensions live in the repository wrapper at field 101.
        val wrapped = top
            .filter { it.number == 101 && it.wireType == 2 && it.bytes != null }
            .flatMap { field -> parseExtensionList(field.bytes!!, repoUrl) }
        if (wrapped.isNotEmpty()) return wrapped

        // Legacy format: a bare `repeated Extension extensions = 1`.
        val legacy = parseExtensionList(bytes, repoUrl)
        if (legacy.isNotEmpty()) return legacy

        // Some builds wrap in a single nested message; retry one level down.
        return top
            .filter { it.wireType == 2 && it.bytes != null }
            .flatMap { field -> parseExtensionList(field.bytes!!, repoUrl) }
    }

    /** Decode a `repeated Extension` payload, keeping only entries that look like extensions. */
    private fun parseExtensionList(bytes: ByteArray, repoUrl: String?): List<MangaExt> =
        Protobuf.decode(bytes).mapNotNull { field ->
            if (field.wireType != 2 || field.bytes == null) null
            else parseExtension(field.bytes!!, repoUrl)
        }

    private fun parseExtension(bytes: ByteArray, repoUrl: String?): MangaExt? {
        val fields = Protobuf.decode(bytes)
        val strings = LinkedHashMap<Int, String>()
        var code = 0L
        var nsfwEnum = 0L
        var nestedApk: String? = null
        val sources = ArrayList<MangaSource>()

        for (f in fields) {
            when (f.wireType) {
                2 -> {
                    val raw = f.bytes ?: continue
                    when (f.number) {
                        // apk is a nested message whose field 1 is the download URL.
                        3 -> nestedApk = firstHttpUrl(raw) ?: f.asString
                        // sources: repeated nested Source messages (8 current, 9 legacy). In the
                        // legacy shape field 8 was the icon URL, so keep the string when the
                        // payload does not decode as a Source.
                        8, 9 -> parseSource(raw)?.let { sources.add(it) }
                            ?: f.asString?.let { strings[f.number] = it }
                        else -> f.asString?.let { strings[f.number] = it }
                    }
                }
                0 -> when (f.number) {
                    5 -> code = f.varint
                    7 -> nsfwEnum = f.varint
                }
            }
        }

        val name = strings[1]?.takeIf { it.isNotBlank() } ?: return null
        val pkg = strings[2]?.takeIf { it.contains('.') } ?: ""
        // Field 4 is the extension-lib version in the current format ("1.6") and the language
        // code in the legacy one ("en", "all"); a short lowercase token is a language.
        val legacyLang = strings[4]?.takeIf { it.length <= 3 && it.all { c -> c.isLetter() } }
        val apk = nestedApk?.takeIf { it.startsWith("http") }
            ?: strings.values.firstOrNull { it.startsWith("http") && it.endsWith(".apk") }
            ?: strings[3]?.takeIf { it.startsWith("http") }
            ?: ""

        return MangaExt(
            name = name,
            pkg = pkg,
            apk = apk,
            icon = strings[10]?.takeIf { it.startsWith("http") }
                ?: strings[8]?.takeIf { it.startsWith("http") }
                ?: strings.values.firstOrNull { it.startsWith("http") && !it.endsWith(".apk") },
            version = strings[6] ?: strings[4]?.takeIf { it.contains('.') },
            lang = sources.firstOrNull()?.lang ?: legacyLang,
            nsfw = nsfwEnum == NSFW_ENUM_ADULT || StremioClient.isNsfw(name, null, pkg),
            sources = sources,
            versionCode = code.takeIf { it > 0 },
            repoUrl = repoUrl,
        )
    }

    /** First `http…` string inside a nested message (used for the apk URL and icons). */
    private fun firstHttpUrl(bytes: ByteArray): String? =
        Protobuf.decode(bytes)
            .firstOrNull { it.wireType == 2 && it.asString?.startsWith("http") == true }
            ?.asString

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
