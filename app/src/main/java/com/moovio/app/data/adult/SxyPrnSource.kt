package com.moovio.app.data.adult

import com.moovio.app.data.model.StreamSource

/**
 * SxyPrn built-in source (sxyprn.com).
 *
 * A community aggregator rather than a tube: the front page ("Porn Wall") and
 * the `/popular/top-viewed.html` + `/popular/top-pop.html` charts list posts
 * (`/post/{id}.html`) whose bodies link out to third-party file hosts
 * (doodstream, vidara, lulustream, streamcash). Tag shelves
 * (`/{Tag}.html?sm=trending`) double as the search endpoint.
 *
 * Caveat: posts rarely carry directly playable media — most streams resolve to
 * file-host embeds. [TubeMedia] picks up whatever direct MP4/HLS the post page
 * (or its first iframe) exposes; when nothing playable exists the entry simply
 * yields no streams and the tab's per-source isolation keeps the rest working.
 */
object SxyPrnSource : AdultSource {

    override val id = "sxyprn"
    override val name = "SxyPrn"
    private const val BASE = "https://www.sxyprn.com"

    override suspend fun home(): List<AdultRow> = adultHome(
        this,
        defs = listOf(
            "Hot now" to "$BASE/",
            "Top viewed" to "$BASE/popular/top-viewed.html",
            "Top rated" to "$BASE/popular/top-pop.html",
        ),
    ) { url -> parseList(fetch(url)) }

    override suspend fun search(query: String): List<AdultVideo> {
        val q = encode(query)
        val tag = query.trim().replaceFirstChar { it.uppercase() }
        val candidates = listOf(
            "$BASE/${encode(tag)}.html?sm=trending",
            "$BASE/search/$q/0.html",
        )
        var html: String? = null
        for (url in candidates) {
            html = runCatching { fetch(url) }.getOrNull()
            if (!html.isNullOrBlank()) break
        }
        return parseList(html ?: return emptyList())
    }

    override suspend fun categories(): List<AdultCategory> =
        STATIC_CATEGORIES.map { (slug, label) -> AdultCategory(slug, label, "$BASE/$slug.html?sm=trending") }

    override suspend fun category(category: AdultCategory): List<AdultVideo> =
        parseList(fetch(category.url))

    override suspend fun detail(url: String): AdultDetail {
        val html = fetch(url)
        val title = adultUnescape(html.ogContent("og:title"))
            ?: adultCollapse(html.capture("""<h1[^>]*>(.*?)</h1>""")?.let { stripTags(it) })
            ?: postTitle(html)
            ?: "SxyPrn post"
        return AdultDetail(
            video = AdultVideo(
                sourceId = id,
                url = url,
                title = title,
                thumbnail = adultUnescapeJson(html.ogContent("og:image"))
                    ?: TubeMedia.thumb(html),
            ),
            streams = postStreams(html, url),
            related = runCatching { parseList(html).filterNot { it.url == url }.take(12) }
                .getOrDefault(emptyList()),
        )
    }

    private suspend fun fetch(url: String): String = AdultHttp.get(url, referer = "$BASE/")

    internal fun parseList(html: String): List<AdultVideo> {
        val out = ArrayList<AdultVideo>()
        for (m in adultRx("""<a\s[^>]*href="((?:https?://[^"]*sxyprn\.com)?/post/[a-z0-9]+\.html)"[^>]*>""")
            .findAll(html)) {
            val href = m.groupValues[1]
            val window = html.substring(maxOf(0, m.range.first - 500), minOf(html.length, m.range.last + 500))
            val title = adultUnescape(m.value.capture("""\stitle="([^"]{3,200})""""))
                ?: adultCollapse(postTitle(window))
                ?: continue
            if (isJunkTitle(title)) continue
            out.add(
                AdultVideo(
                    sourceId = id,
                    url = absolute(href),
                    title = title,
                    thumbnail = TubeMedia.thumb(window),
                    duration = window.capture("""([0-9]{1,2}:[0-9]{2}(?::[0-9]{2})?)"""),
                )
            )
        }
        return out.distinctBy { it.url }
    }

    /** Post titles live in the anchor text/title or the `{…}` description line. */
    private fun postTitle(window: String): String? {
        val braced = window.capture("""\{([^{}]{10,220})\}""")?.let { stripTags(it) }
        if (!braced.isNullOrBlank() && !isJunkTitle(braced)) return adultCollapse(braced)
        return null
    }

    private fun isJunkTitle(title: String): Boolean {
        val t = title.trim()
        if (t.length < 3) return true
        val lower = t.lowercase()
        return lower in setOf("post thumb", "external link!", "watch/download:", "full hd click here") ||
            (t.startsWith("http") && !t.contains(" "))
    }

    /**
     * Post page streams: direct media first, then one embedded file-host frame
     * (dood/vidara/lulustream embeds sometimes carry a resolvable `file:`).
     */
    internal suspend fun postStreams(html: String, pageUrl: String): List<StreamSource> {
        val direct = TubeMedia.streams(html, this, referer = pageUrl)
        if (direct.isNotEmpty()) return direct
        val frame = html.capture("""<iframe[^>]*\ssrc="([^"]+)"""")
            ?.let { if (it.startsWith("http")) it else null }
            ?: return emptyList()
        val embed = runCatching { AdultHttp.get(frame, referer = pageUrl) }.getOrNull()
            ?: return emptyList()
        return TubeMedia.streams(embed, this, referer = frame)
    }

    private fun absolute(href: String): String = when {
        href.startsWith("http") -> href
        href.startsWith("//") -> "https:$href"
        else -> BASE + href
    }

    private fun encode(q: String): String = java.net.URLEncoder.encode(q, "UTF-8")

    private val STATIC_CATEGORIES = listOf(
        "Milf" to "Milf",
        "Teen" to "Teen",
        "Anal" to "Anal",
        "Asian" to "Asian",
        "Blonde" to "Blonde",
        "Hardcore" to "Hardcore",
        "Onlyfans" to "OnlyFans",
        "Creampie" to "Creampie",
    )
}
