package com.moovio.desktop.player

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.moovio.desktop.domain.model.MediaItem

/**
 * Holds player UI state (what's loaded, what's shown). The actual native player lives in
 * [MpvPlayer]; this controller is purely presentational and backend-agnostic.
 */
class PlayerController(val player: MpvPlayer) {
    private val _media = MutableStateFlow<MediaItem?>(null)
    private val _streamUrl = MutableStateFlow<String?>(null)
    private val _showControls = MutableStateFlow(true)
    private val _fullscreen = MutableStateFlow(false)
    private val _sheetOpen = MutableStateFlow(false)

    val media: StateFlow<MediaItem?> = _media.asStateFlow()
    val streamUrl: StateFlow<String?> = _streamUrl.asStateFlow()
    val showControls: StateFlow<Boolean> = _showControls.asStateFlow()
    val isFullscreen: StateFlow<Boolean> = _fullscreen.asStateFlow()
    val isSheetOpen: StateFlow<Boolean> = _sheetOpen.asStateFlow()

    // Delegate playback state straight from the native player.
    val isPlaying: StateFlow<Boolean> = player.isPlaying
    val positionMs: StateFlow<Long> = player.positionMs
    val durationMs: StateFlow<Long> = player.durationMs
    val error: StateFlow<String?> = player.error

    fun load(item: MediaItem, streamUrl: String) {
        _media.value = item
        _streamUrl.value = streamUrl
        _showControls.value = true
        _sheetOpen.value = false
        player.load(item, streamUrl)
        player.play()
    }

    fun play() = player.play()
    fun pause() = player.pause()
    fun togglePlayback() = player.togglePlayback()
    fun seek(positionMs: Long) = player.seek(positionMs)
    fun seekRelative(deltaMs: Long) = player.seekRelative(deltaMs)
    fun setFullscreen(fullscreen: Boolean) {
        _fullscreen.value = fullscreen
        player.setFullscreen(fullscreen)
    }
    fun setSpeed(speed: Float) = player.setSpeed(speed)
    fun setVolume(volume: Float) = player.setVolume(volume)
    fun toggleControls() = _showControls.value = !_showControls.value
    fun toggleSheet() = _sheetOpen.value = !_sheetOpen.value
    fun close() = player.close()
}
