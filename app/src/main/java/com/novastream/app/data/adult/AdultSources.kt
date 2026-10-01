package com.novastream.app.data.adult

/**
 * The registry of built-in adult sources. Add/remove a site here to change what Real 18+ offers.
 */
object AdultSources {

    val all: List<AdultSource> = listOf(
        PornhubSource,
        XvideosSource,
        XnxxSource,
        HqpornerSource,
        SpankbangSource,
    )

    fun byId(id: String?): AdultSource? = id?.let { key -> all.firstOrNull { it.id == key } }

    /** True when [addonId] belongs to a built-in adult source (vs. an installed Stremio add-on). */
    fun isBuiltIn(addonId: String?): Boolean = byId(addonId) != null
}
