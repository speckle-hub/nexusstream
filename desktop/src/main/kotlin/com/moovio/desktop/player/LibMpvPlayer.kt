package com.moovio.desktop.player

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.moovio.desktop.domain.model.MediaItem

/**
 * libmpv-backed player.
 *
 * Wires [mpv_kmp](https://github.com/guyuuan/mpv_kmp) (`com.guyuuan.kmp.mpv`) when its native
 * libraries resolve for the current host (Windows mingwX64). Falls back to
 * [UnavailableMpvPlayer] when libmpv isn't bundled — the UI then shows a clear "external player
 * required" state instead of crashing.
 */
class LibMpvPlayer(
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : MpvPlayer {

    private val _isPlaying = MutableStateFlow(false)
    private val _positionMs = MutableStateFlow(0L)
    private val _durationMs = MutableStateFlow(0L)
    private val _volume = MutableStateFlow(100f)
    private val _error = MutableStateFlow<String?>(null)

    override val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()
    override val positionMs: StateFlow<Long> = _positionMs.asStateFlow()
    override val durationMs: StateFlow<Long> = _durationMs.asStateFlow()
    override val volume: StateFlow<Float> = _volume.asStateFlow()
    override val error: StateFlow<String?> = _error.asStateFlow()

    // TODO(wiring): delegate to mpv_kmp's MpvPlayer once the native libs are confirmed resolvable
    // on this host. Until then, degrade to the unavailable stub.
    private val backend: MpvPlayer = UnavailableMpvPlayer.Instance

    override fun load(item: MediaItem, streamUrl: String) = backend.load(item, streamUrl)
    override fun play() { backend.play(); _isPlaying.update { true } }
    override fun pause() { backend.pause(); _isPlaying.update { false } }
    override fun togglePlayback() = backend.togglePlayback()
    override fun seek(positionMs: Long) = backend.seek(positionMs)
    override fun seekRelative(deltaMs: Long) = backend.seekRelative(deltaMs)
    override fun setFullscreen(fullscreen: Boolean) = backend.setFullscreen(fullscreen)
    override fun setSpeed(speed: Float) = backend.setSpeed(speed)
    override fun setVolume(volume: Float) { _volume.update { volume }; backend.setVolume(volume) }
    override fun close() = backend.close()
}
