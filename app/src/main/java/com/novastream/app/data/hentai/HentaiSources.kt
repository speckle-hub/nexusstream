package com.novastream.app.data.hentai

/**
 * The registry of built-in NSFW-anime stream sources. Add/remove a site here to change what the
 * hentai tab can play without any Stremio add-on installed.
 */
object HentaiSources {

    val all: List<HentaiSource> = listOf(
        // Original pair.
        HentaiMamaSource,
        HentaiPlaySource,
        // Priority additions.
        HentaiHavenSource,
        HStreamSource,
        // Secondary additions.
        MuchoHentaiSource,
        HentaiCitySource,
        HahoSource,
        // NOTE: HanimeSource and HentaigasmSource are intentionally unregistered — Hanime's
        // search API (search.htv-services.com) is DNS-dead and its HTML page is a JS shell, and
        // hentaigasm.com times out, so both only produced per-source error notices. Their
        // objects/tests remain should either site come back.
    )

    fun byId(id: String?): HentaiSource? = id?.let { key -> all.firstOrNull { it.id == key } }

    /** True when [addonId] belongs to a built-in hentai source (vs. an installed Stremio add-on). */
    fun isBuiltIn(addonId: String?): Boolean = byId(addonId) != null
}
