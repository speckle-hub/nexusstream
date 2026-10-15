package com.novastream.app.data.sfw

/**
 * The registry of built-in movie/TV sources. Add/remove a site here to change
 * what regular Movies & TV resolve without any installed add-on.
 */
object SfwSources {

    val all: List<SfwSource> = listOf(
        VidsrcSource,
        YtsSource,
        FlixhqSource,
        MultiMoviesSource,
        RiveSource,
        FlixerSource,
        PopcornMoviesSource,
        SuperStreamSource,
    )

    fun byId(id: String?): SfwSource? = id?.let { key -> all.firstOrNull { it.id == key } }

    /** True when [addonId] belongs to a built-in SFW movie/TV source. */
    fun isBuiltIn(addonId: String?): Boolean = byId(addonId) != null
}
