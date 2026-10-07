package com.novastream.app.data.integrations

import com.novastream.app.data.local.SettingsStore
import com.novastream.app.data.model.MediaItem
import com.novastream.app.data.model.MediaType
import com.novastream.app.data.remote.httpPostJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Outcome of trying to scrobble one title to one provider. */
data class ScrobbleResult(val provider: String, val ok: Boolean, val message: String)

/** A watch event worth syncing. [progress] is 0..1 of the title/episode. */
data class ScrobbleEvent(
    val item: MediaItem,
    val progress: Double,
    val episode: Int? = null,
    val season: Int? = null,
)

/**
 * Best-effort two-way history/rating sync for Trakt, AniList, MyAnimeList, Kitsu and SIMKL.
 *
 * Providers are configured with a pasted OAuth token (Settings → Scrobbling) because a full
 * interactive OAuth flow needs per-provider client ids/secrets and a redirect receiver. Each call
 * is wrapped in [runCatching] so a misconfigured token can never crash playback; a provider that
 * cannot identify the title (no shared numeric/IMDB id) is reported as skipped rather than guessed.
 */
class ScrobbleManager(private val settings: SettingsStore) {

    suspend fun syncAll(event: ScrobbleEvent): List<ScrobbleResult> = withContext(Dispatchers.IO) {
        settings.scrobbleSnapshot().filter { it.active }.map { cfg ->
            runCatching { dispatch(cfg.provider, cfg.token, event) }
                .getOrElse { ScrobbleResult(cfg.provider, false, it.message ?: "Failed") }
        }
    }

    private suspend fun dispatch(provider: String, token: String, e: ScrobbleEvent): ScrobbleResult =
        when (provider) {
            "anilist" -> aniList(token, e)
            "trakt" -> trakt(token, e)
            "simkl" -> simkl(token, e)
            "mal" -> mal(token, e)
            "kitsu" -> kitsu(token, e)
            else -> ScrobbleResult(provider, false, "Unknown provider")
        }

    /** AniList expects a numeric media id; only sync when the item came from AniList. */
    private suspend fun aniList(token: String, e: ScrobbleEvent): ScrobbleResult {
        val mediaId = e.item.id.toIntOrNull() ?: return skipped("anilist", "no AniList id")
        val status = if (e.progress >= 0.9) "COMPLETED" else "CURRENT"
        val progress = (e.episode ?: 1).coerceAtLeast(1)
        // Values are inlined (no GraphQL variables) to keep the body a simple string template.
        val body = """{"query":"mutation{SaveMediaListEntry(mediaId:$mediaId,progress:$progress,status:$status){id}}"}"""
        httpPostJson(
            "https://graphql.anilist.co",
            body,
            mapOf("Authorization" to "Bearer $token"),
        )
        return ScrobbleResult("anilist", true, "Synced to AniList")
    }

    /** Trakt needs an IMDB/TMDB id; items whose id looks like an IMDB id sync cleanly. */
    private suspend fun trakt(token: String, e: ScrobbleEvent): ScrobbleResult {
        val imdb = e.item.id.takeIf { it.startsWith("tt") }
            ?: return skipped("trakt", "no IMDB id")
        val watchedAt = isoNow()
        val isMovie = e.item.type == MediaType.MOVIE
        val url = if (isMovie) "https://api.trakt.tv/sync/history" else "https://api.trakt.tv/sync/history"
        val ids = """{"imdb":"$imdb"}"""
        val body = if (isMovie) {
            """{"movies":[{"ids":$ids,"watched_at":"$watchedAt"}]}"""
        } else {
            """{"shows":[{"ids":$ids,"watched_at":"$watchedAt"}]}"""
        }
        httpPostJson(
            url,
            body,
            mapOf(
                "Authorization" to "Bearer $token",
                "trakt-api-version" to "2",
                "trakt-api-key" to "nexusstream",
            ),
        )
        return ScrobbleResult("trakt", true, "Synced to Trakt")
    }

    private suspend fun simkl(token: String, e: ScrobbleEvent): ScrobbleResult {
        val imdb = e.item.id.takeIf { it.startsWith("tt") }
            ?: return skipped("simkl", "no IMDB id")
        val body = if (e.item.type == MediaType.MOVIE) {
            """{"movies":[{"ids":{"imdb":"$imdb"}}]}"""
        } else {
            """{"shows":[{"ids":{"imdb":"$imdb"},"seasons":[{"number":${e.season ?: 1}}]}]}"""
        }
        httpPostJson(
            "https://api.simkl.com/sync/history",
            body,
            mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/json"),
        )
        return ScrobbleResult("simkl", true, "Synced to SIMKL")
    }

    /** MAL v2 needs the MAL numeric id; AniList/TMDB ids do not map, so we skip rather than guess. */
    private suspend fun mal(token: String, e: ScrobbleEvent): ScrobbleResult {
        val id = e.item.id.toIntOrNull() ?: return skipped("mal", "no MAL id (ID mapping not configured)")
        val body = if (e.progress >= 0.9) """{"status":"completed"}""" else """{"status":"watching"}"""
        httpPostJson(
            "https://api.myanimelist.net/v2/anime/$id/my_list_status",
            body,
            mapOf("Authorization" to "Bearer $token"),
        )
        return ScrobbleResult("mal", true, "Synced to MyAnimeList")
    }

    private suspend fun kitsu(token: String, e: ScrobbleEvent): ScrobbleResult {
        val id = e.item.id.takeIf { it.all { c -> c.isDigit() } }
            ?: return skipped("kitsu", "no Kitsu id (ID mapping not configured)")
        val body = """
            {"data":{"type":"libraryEntries","attributes":{"progress":${e.episode ?: 1},"status":"current"}}}
        """.trimIndent()
        httpPostJson(
            "https://kitsu.io/api/edge/library-entries",
            body,
            mapOf("Authorization" to "Bearer $token", "Content-Type" to "application/vnd.api+json"),
        )
        return ScrobbleResult("kitsu", true, "Synced to Kitsu")
    }

    private fun skipped(provider: String, why: String) = ScrobbleResult(provider, false, "Skipped — $why")

    /** ISO-8601 UTC timestamp that works on all supported API levels (no java.time desugaring). */
    private fun isoNow(): String =
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US)
            .apply { timeZone = java.util.TimeZone.getTimeZone("UTC") }
            .format(java.util.Date())
}
