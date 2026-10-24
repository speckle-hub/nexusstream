package com.moovio.app.data.hentai

/**
 * The registry of built-in NSFW-anime stream sources. Add/remove a site here to change what the
 * hentai tab can play without any Stremio add-on installed.
 */
object HentaiSources {

    /** Sources verified to return real episode listings and/or playable streams. */
    private val verified = listOf(
        HentaiMamaSource,   // ✅ probed live — series + episodes + direct MP4/HLS
        HentaiPlaySource,   // ✅ probed live — direct MP4, no Referer required
        HahoSource,         // ✅ probed live 2026-10-06 — series + episodes + filegasm streams
    )

    /** Sources never probed live against a real response (adult/Cloudflare endpoints blocked
     * from the build environment). Kept in the registry but flagged experimental; they degrade
     * per-source and never block the NSFW Anime tab. */
    private val experimental = listOf(
        HentaiHavenSource,
        HStreamSource,
        MuchoHentaiSource,
        HentaiCitySource,
        Rule34VideoSource,
        AnimeIdHentaiSource,
        HenVidsSource,
        OhentaiSource,
        HentaiTvSource,
        XAnimuSource,
    )

    val all: List<HentaiSource> = verified + experimental

    /**
     * Hygiene quarantine, mirroring `AdultSources.includeExperimental`: when false (the default),
     * [active] excludes the never-probed [experimental] sources. Episode/stream resolution keeps
     * resolving quarantined sources by id ([byId]) so previously opened titles still work.
     */
    @Volatile
    var includeExperimental: Boolean = false

    /** The sources the NSFW-anime episode/stream lookups actually query, honouring the quarantine. */
    val active: List<HentaiSource>
        get() = if (includeExperimental) all else verified

    /** Sources removed from the active registry:
     *  Hanime (search API DNS-dead + JS-shell page), Hentaigasm (30 s timeouts).
     *  Their objects and parser tests remain should a host return. */

    fun byId(id: String?): HentaiSource? = id?.let { key -> all.firstOrNull { it.id == key } }

    /** True when [addonId] belongs to a built-in hentai source (vs. an installed Stremio add-on). */
    fun isBuiltIn(addonId: String?): Boolean = byId(addonId) != null
}
