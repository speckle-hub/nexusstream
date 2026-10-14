package com.novastream.app.data.adult

/**
 * The registry of built-in adult sources. Add/remove a site here to change what Real 18+ offers.
 */
object AdultSources {

    val all: List<AdultSource> = listOf(
        // General tube sites.
        PornhubSource,
        XvideosSource,
        XnxxSource,
        HqpornerSource,
        SpankbangSource,
        XhamsterSource,
        YouPornSource,
        RedTubeSource,
        EpornerSource,
        BeegSource,
        // Expansion (§2as): KVS + aggregator + full-length + Aylo-family general tubes.
        PornTrexSource,
        SxyPrnSource,
        FullPornerSource,
        TxxxSource,
        // JAV / Japanese adult video sources.
        MissavSource,
        JableSource,
        JavGuruSource,
        SupJavSource,
        VjavSource,
        // Expansion (§2as): KVS + catalogue + WP-style + mirror-fallback JAV tubes.
        JavMostSource,
        KissJavSource,
        JavtifulSource,
        OpJavSource,
        SevenMmtvSource,
        // NOTE: HpjavSource and AvgleSource are intentionally unregistered — hpjav.tv no longer
        // resolves (DNS) and api.avgle.com is down (520), so they only produced per-source error
        // notices. Their objects/tests remain should either site come back.
    )

    fun byId(id: String?): AdultSource? = id?.let { key -> all.firstOrNull { it.id == key } }

    /** True when [addonId] belongs to a built-in adult source (vs. an installed Stremio add-on). */
    fun isBuiltIn(addonId: String?): Boolean = byId(addonId) != null
}
