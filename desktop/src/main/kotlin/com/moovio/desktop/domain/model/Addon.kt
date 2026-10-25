package com.moovio.desktop.domain.model

data class Addon(
    val id: String,
    val name: String,
    val manifestUrl: String? = null,
    val isInstalled: Boolean = false,
    val isEnabled: Boolean = true,
)
