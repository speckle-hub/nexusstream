package com.moovio.desktop.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.moovio.desktop.domain.model.MediaItem

/**
 * Thin libmpv abstraction. Implementations are responsible for loading libmpv (or mpv_kmp)
 * and keeping playback state in sync. One failing backend never breaks the UI — see
 * [UnavailableMpvPlayer].
 */
interface MpvPlayer {
    val isPlaying: StateFlow<Boolean>
    val positionMs: StateFlow<Long>
    val durationMs: StateFlow<Long>
    val volume: StateFlow<Float>
    val error: StateFlow<String?>

    fun load(item: MediaItem, streamUrl: String)
    fun play()
    fun pause()
    fun togglePlayback()
    fun seek(positionMs: Long)
    fun seekRelative(deltaMs: Long)
    fun setFullscreen(fullscreen: Boolean)
    fun setSpeed(speed: Float)
    fun setVolume(volume: Float)
    fun close()
}

sealed class MpvError {
    object NotAvailable : MpvError()
    data class Native(val message: String) : MpvError()
}

/** Fallback when libmpv cannot be loaded: reports unavailability instead of crashing. */
class UnavailableMpvPlayer private constructor() : MpvPlayer {
    override val isPlaying: StateFlow<Boolean> = MutableStateFlow(false).asStateFlow()
    override val positionMs: StateFlow<Long> = MutableStateFlow(0L).asStateFlow()
    override val durationMs: StateFlow<Long> = MutableStateFlow(0L).asStateFlow()
    override val volume: StateFlow<Float> = MutableStateFlow(100f).asStateFlow()
    override val error: StateFlow<String?> =
        MutableStateFlow("libmpv is not bundled on this host — use an external player").asStateFlow()

    override fun load(item: MediaItem, streamUrl: String) = Unit
    override fun play() = Unit
    override fun pause() = Unit
    override fun togglePlayback() = Unit
    override fun seek(positionMs: Long) = Unit
    override fun seekRelative(deltaMs: Long) = Unit
    override fun setFullscreen(fullscreen: Boolean) = Unit
    override fun setSpeed(speed: Float) = Unit
    override fun setVolume(volume: Float) = Unit
    override fun close() = Unit

    companion object {
        val Instance: MpvPlayer = UnavailableMpvPlayer()
    }
}
