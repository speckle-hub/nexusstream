package com.novastream.app.data.sfw

/**
 * The registry of built-in SFW anime sources. Add/remove a site here to change
 * what regular Anime resolves without any installed add-on. Metadata stays
 * AniList-led — these sources only resolve episode lists and streams.
 */
object SfwAnimeSources {

    val all: List<SfwAnimeSource> = listOf(
        AnimePaheSource,
        GogoSource,
        KickassAnimeSource,
        YugenAnimeSource,
        AnimeKaiSource,
        MiruroSource,
    )

    fun byId(id: String?): SfwAnimeSource? = id?.let { key -> all.firstOrNull { it.id == key } }

    /** True when [addonId] belongs to a built-in SFW anime source. */
    fun isBuiltIn(addonId: String?): Boolean = byId(addonId) != null
}
