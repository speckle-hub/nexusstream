package com.moovio.app.data.adult

/**
 * The registry of built-in adult sources. Add/remove a site here to change what Real 18+ offers.
 */
object AdultSources {

    /** Sources confirmed to return real listings and/or playable streams (at least intermittently). */
    private val verified = listOf(
        PornhubSource,      // ⚠️ intermittent — rate-limited from build env, but returns real data
        XvideosSource,      // ⚠️ partial — /tags works, search sometimes times out
        XnxxSource,         // ✅ working — listing + search verified live
        HqpornerSource,     // ⚠️ partial — third-party embed (mydaddy.cc), best-effort
    )

    /** Sources never probed live — adult endpoints blocked from build environment.
     * Kept in the registry but flagged experimental; they degrade per-source and never block the tab. */
    private val experimental = listOf(
        EpornerSource,
        BeegSource,
        PornTrexSource,
        SxyPrnSource,
        FullPornerSource,
        MissavSource,
        XhamsterSource,
        JavGuruSource,
        SupJavSource,
        VjavSource,
        KissJavSource,
        JavtifulSource,
    )

    val all: List<AdultSource> = verified + experimental

    /**
     * Hygiene quarantine: when false (the default), [active] excludes the never-probed
     * [experimental] sources so unverified sites are not presented as working ones.
     *
     * Written synchronously by the NSFW ViewModel on toggle (so the very next row refresh sees
     * the new set) and kept in sync app-wide by the collector in `MoovioApp.onCreate`.
     * Resolution paths ([byId], [isBuiltIn]) always see every registered source, so cached or
     * deep-linked items from an experimental source still resolve while quarantined.
     */
    @Volatile
    var includeExperimental: Boolean = false

    /** The sources home/search/categories actually query, honouring the quarantine. */
    val active: List<AdultSource>
        get() = if (includeExperimental) all else verified

    /** Sources removed from the active registry because they consistently fail:
     *  Jable (Cloudflare 403), YouPorn (broken stub), RedTube (blocked/timeout),
     *  SpankBang (Cloudflare 403), TXXX (unreachable), JavMost (blocked),
     *  OpJav (blocked 403), 7MMTV (unreachable).
     *  HPJAV (DNS-dead), Avgle (520), Hanime (DNS-dead), Hentaigasm (timeouts) were retired earlier.
     *  Their source objects/tests remain should a host return. */

    fun byId(id: String?): AdultSource? = id?.let { key -> all.firstOrNull { it.id == key } }

    /** True when [addonId] belongs to a built-in adult source (vs. an installed Stremio add-on). */
    fun isBuiltIn(addonId: String?): Boolean = byId(addonId) != null
}
