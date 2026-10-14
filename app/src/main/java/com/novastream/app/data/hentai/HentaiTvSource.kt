package com.novastream.app.data.hentai

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.novastream.app.data.adult.AdultHttp
import com.novastream.app.data.model.StreamSource
import java.net.URLEncoder

/**
 * Hentai.tv (hentai.tv) built-in source.
 *
 * Unlike the scraper sources, hentai.tv exposes a clean JSON API:
 * `GET /api/search?q={query}` → `{"videos":[{id, slug, title, ep, views, cover,
 * thumb, embedUrl, duration, quality, …}]}`. Results are already episode-level
 * (title + episode number), so no series expansion is needed and AniList/Jikan
 * metadata stays untouched. Streams resolve from the entry's `embedUrl`
 * (nhplayer page carrying direct media) or, when absent, from the watch-page
 * fallbacks through [HentaiTubes.resolvedStreams].
 */
object HentaiTvSource : HentaiSource {

    override val id = "hentaitv"
    override val name = "Hentai.tv"
    private const val BASE = "https://hentai.tv"

    override suspend fun search(query: String): List<HentaiEpisode> {
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        val json = runCatching { AdultHttp.get("$BASE/api/search?q=$q", referer = "$BASE/") }
            .getOrNull()
            ?: runCatching { AdultHttp.get("$BASE/api/search?query=$q", referer = "$BASE/") }
                .getOrNull()
            ?: return emptyList()
        return parseSearch(json)
    }

    override suspend fun streams(episode: HentaiEpisode): List<StreamSource> {
        // Embed pages carry the media directly; watch pages embed them one hop down.
        if (episode.url.startsWith("https://nhplayer.com/")) {
            val embed = runCatching { AdultHttp.get(episode.url, referer = "$BASE/") }.getOrNull()
                ?: return emptyList()
            val direct = HentaiTubes.toStreams(embed, this, episode.url)
            if (direct.isNotEmpty()) return direct
        }
        val page = runCatching { AdultHttp.get(episode.url, referer = "$BASE/") }.getOrNull()
            ?: return emptyList()
        return HentaiTubes.resolvedStreams(page, this, episode.url)
    }

    internal fun parseSearch(json: String): List<HentaiEpisode> {
        val videos = runCatching {
            Gson().fromJson(json, SearchResponse::class.java)?.videos.orEmpty()
        }.getOrDefault(emptyList())
        val out = ArrayList<HentaiEpisode>()
        val seen = HashSet<String>()
        for (v in videos) {
            val title = v.title?.trim().orEmpty()
            if (title.length < 2) continue
            val ep = v.ep
            val url = v.embedUrl?.takeIf { it.startsWith("http") }
                ?: v.slug?.takeIf { it.isNotBlank() }?.let { "$BASE/watch/$it" }
                ?: continue
            if (!seen.add(url)) continue
            val poster = v.cover?.let { abs(it) } ?: v.thumb?.let { abs(it) }
            out.add(
                HentaiEpisode(
                    sourceId = id,
                    series = title,
                    title = if (ep != null && ep > 0) "$title EP $ep" else title,
                    url = url,
                    episode = ep?.takeIf { it > 0 },
                    poster = poster,
                )
            )
        }
        return out
    }

    private fun abs(path: String): String? {
        val t = path.trim()
        if (t.isEmpty()) return null
        return when {
            t.startsWith("http") -> t
            t.startsWith("//") -> "https:$t"
            t.startsWith("/") -> BASE + t
            else -> "$BASE/$t"
        }
    }

    private data class SearchResponse(
        @SerializedName("videos") val videos: List<TvVideo>? = null,
    )

    private data class TvVideo(
        @SerializedName("slug") val slug: String? = null,
        @SerializedName("title") val title: String? = null,
        @SerializedName("ep") val ep: Int? = null,
        @SerializedName("cover") val cover: String? = null,
        @SerializedName("thumb") val thumb: String? = null,
        @SerializedName("embedUrl") val embedUrl: String? = null,
        @SerializedName("duration") val duration: String? = null,
        @SerializedName("quality") val quality: String? = null,
    )
}
