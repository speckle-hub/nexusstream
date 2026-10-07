package com.novastream.app.data.adult

import com.novastream.app.data.model.StreamSource

/**
 * RedTube built-in source.
 *
 * Listings come from `/best`, `/newest` and the front page (Aylo markup: numeric `/{id}` links with
 * a nearby `data-src` thumbnail); search from `/?search=`. Streams first try the `media_definitions`
 * JSON endpoint (shared shape with YouPorn/Pornhub via [TubeMedia.definitions]) and fall back to
 * scanning the watch page markup.
 */
object RedTubeSource : AdultSource {

    override val id = "redtube"
    override val name = "RedTube"
    private const val BASE = "https://www.redtube.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Best" to "$BASE/best",
            "Newest" to "$BASE/newest",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> =
        parseList(fetch("$BASE/?search=" + encode(query)))

    override suspend fun categories(): List<AdultCategory> {
        val html = fetch("$BASE/categories")
        return adultRx("""<a[^>]*href="(/(?:category|tags?)/[^"/]+)"[^>]*>([^<]{2,40})</a>""")
            .findAll(html)
            .mapNotNull { m ->
                val path = m.groupValues[1]
                val label = adultUnescape(m.groupValues[2])?.trim() ?: return@mapNotNull null
                if (label.isEmpty()) null
                else AdultCategory(path.trim('/'), label, BASE + path)
            }
            .distinctBy { it.id }
            .take(24)
            .toList()
    }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.ogContent("og:title"))
            ?: adultCollapse(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) })
            ?: "RedTube video"
        val thumb = adultUnescapeJson(html.ogContent("og:image"))
        val duration = adultCollapse(
            html.capture("""<span class="[^"]*duration[^"]*">([^<]+)</span>""")
        )

        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = thumb,
                duration = duration,
            ),
            streams = resolveStreams(html, url),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = BASE)

    internal suspend fun resolveStreams(html: String, pageUrl: String): List<StreamSource> {
        val videoId = Regex("""redtube\.com/(\d{5,})""").find(pageUrl)?.groupValues?.get(1)
            ?: Regex("""/(\d{5,})""").find(pageUrl)?.groupValues?.get(1)
        if (videoId != null) {
            val json = runCatching {
                AdultHttp.get("$BASE/media_definitions/$videoId/", referer = pageUrl)
            }.getOrNull()
            if (json != null) {
                val parsed = parseDefinitions(json, pageUrl)
                if (parsed.isNotEmpty()) return parsed
            }
        }
        return TubeMedia.streams(html, this, referer = pageUrl)
    }

    internal fun parseDefinitions(json: String, pageUrl: String?): List<StreamSource> =
        TubeMedia.definitions(json, this, pageUrl)

    /**
     * Parse a listing page. Cards are `a[href="/{numericId}[/{slug}]"]`; the title comes from the
     * anchor's `title=`/`data-title`, the `<p>`/`span` title text, or the image `alt`.
     */
    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*redtube\.com)?/\d{5,}(?:/[^"#?]*)?)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val path = href.substringAfter("redtube.com").ifBlank { href }
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2200))
            val title = adultUnescape(
                m.value.capture("""\s(?:title|data-title)="([^"]{3,200})"""")
            )
                ?: adultUnescape(
                    window.capture("""class="[^"]*(?:video-title|title)[^"]*"[^>]*>([^<]{3,200})<""")
                )
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: slugTitle(path)
            if (title.isNullOrBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = if (href.startsWith("http")) href else BASE + path,
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""class="[^"]*duration[^"]*"[^>]*>([0-9:]{3,9})<""")
                        ?: window.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun slugTitle(path: String): String =
        path.trim('/').substringBefore('/')
            .let { slug -> slug.substringAfter('-', slug) }
            .replace('-', ' ')
            .ifBlank { "RedTube video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")
}
