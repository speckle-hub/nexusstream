package com.novastream.app.data.adult

/**
 * 7MMTV built-in JAV source (7mmtv.sx, with the .tv mirror as fallback).
 *
 * A JAV streaming tube whose hosts rotate, so every fetch tries the primary
 * base first and the mirror second. Search is the stock `/?s={query}` with a
 * `/search/{query}/` fallback; cards are generic post permalinks filtered
 * against the taxonomy/chrome paths; streams come from the post page via
 * [TubeMedia]. Per-source error isolation keeps an unreachable 7MMTV from
 * affecting the rest of the tab.
 */
object SevenMmtvSource : AdultSource {

    override val id = "7mmtv"
    override val name = "7MMTV"
    private const val BASE = "https://7mmtv.sx"
    private const val MIRROR = "https://7mmtv.tv"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Latest" to "$BASE/",
            "Popular" to "$BASE/popular/",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> {
        val q = encode(query)
        val html = runCatching { fetch("$BASE/?s=$q") }.getOrNull()
            ?: runCatching { fetch("$BASE/search/$q/") }.getOrNull()
            ?: return emptyList()
        return parseList(html)
    }

    override suspend fun categories(): List<AdultCategory> =
        STATIC_CATEGORIES.map { (slug, label) -> AdultCategory(slug, label, "$BASE/category/$slug/") }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.ogContent("og:title"))
            ?: adultCollapse(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) })
            ?: "7MMTV video"
        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = adultUnescapeJson(html.ogContent("og:image")),
                duration = adultCollapse(
                    html.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)""")
                ),
            ),
            streams = TubeMedia.streams(html, this, referer = url),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String {
        runCatching { AdultHttp.get(url, referer = BASE) }.getOrNull()?.let { return it }
        val mirror = url.replace(BASE, MIRROR)
        if (mirror != url) return AdultHttp.get(mirror, referer = MIRROR)
        throw AdultSourceException("Couldn't reach the site")
    }

    internal fun parseList(html: String, host: String = BASE): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*)?/(?!category/|tag/|genre/|studio/|actor/|director/|author/|page/|search|wp-|feed|comment)[^"#?]{3,180}/?)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            if (isChrome(href)) continue
            val window = html.substring(m.range.first, minOf(html.length, m.range.first + 2500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultUnescape(window.capture("""<img[^>]*\salt="([^"]{3,200})""""))
                ?: slugTitle(href)
            if (title.isNullOrBlank()) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = absolute(href, host),
                    title = adultCollapse(title) ?: continue,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    private fun isChrome(href: String): Boolean = CHROME_PATHS.any { href.contains(it) }

    private fun absolute(href: String, host: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("//") -> "https:$href"
        else -> host + href
    }

    private fun slugTitle(href: String): String =
        href.trim('/').substringAfterLast('/').replace('-', ' ').replace('_', ' ')
            .uppercase().ifBlank { "7MMTV video" }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val CHROME_PATHS = listOf(
        "/category/", "/tag/", "/genre/", "/studio/", "/actor/", "/director/",
        "/author/", "/page/", "/search", "/wp-", "/feed", "/comment",
    )

    private val STATIC_CATEGORIES = listOf(
        "censored" to "Censored",
        "uncensored" to "Uncensored",
        "fc2ppv" to "FC2PPV",
        "english-subbed" to "English Subbed",
    )
}
