package com.moovio.desktop.player

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asComposeImageBitmap
import com.moovio.desktop.data.local.AppSettings
import com.moovio.desktop.data.net.MoovioJson
import com.moovio.desktop.data.net.arr
import com.moovio.desktop.data.net.bool
import com.moovio.desktop.data.net.int
import com.moovio.desktop.data.net.obj
import com.moovio.desktop.data.net.str
import com.sun.jna.Memory
import com.sun.jna.Pointer
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo

data class MpvTrack(
    val id: Int,
    val type: String,
    val title: String,
    val language: String,
    val codec: String,
    val selected: Boolean,
)

data class MpvSnapshot(
    val positionMs: Long = 0,
    val durationMs: Long = 0,
    val paused: Boolean = false,
    val buffering: Boolean = false,
    val ended: Boolean = false,
    val volume: Int = 100,
    val muted: Boolean = false,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val audioTracks: List<MpvTrack> = emptyList(),
    val subtitleTracks: List<MpvTrack> = emptyList(),
    val audioId: Int? = null,
    val subtitleId: Int? = null,
    val title: String? = null,
    val error: String? = null,
    val loaded: Boolean = false,
)

class VideoFrame(
    val bitmap: ImageBitmap,
    val width: Int,
    val height: Int,
    val sequence: Long,
)

/**
 * libmpv playback with the software render API.
 *
 * Why software rendering: Compose Desktop cannot composite its own controls over a native child
 * window (the `wid` embedding path), so frames are pulled through mpv's SW renderer and drawn as
 * a Compose image. Decoding is still hardware-accelerated where the machine supports it; only the
 * final conversion/scaling pass is on the CPU. The render target is capped by the UI to keep that
 * pass comfortably real-time.
 */
class MpvPlayer private constructor(
    private val lib: LibMpv,
    private val mpv: Pointer,
    val libraryPath: String,
    private val useHardwareDecode: Boolean,
) : AutoCloseable {

    @Volatile private var closed = false
    @Volatile private var renderReady = false
    @Volatile private var initError: String? = null
    @Volatile private var targetWidth = 0
    @Volatile private var targetHeight = 0
    @Volatile private var lastPollPaused = false
    private var loadStartedAt: Long = 0
    private var renderThread: Thread? = null

    private val _snapshot = MutableStateFlow(MpvSnapshot())
    val snapshot: StateFlow<MpvSnapshot> = _snapshot.asStateFlow()

    /** Read inside a draw scope (not composition) so new frames only invalidate drawing. */
    val frame = mutableStateOf<VideoFrame?>(null)

    init {
        renderThread = Thread({ renderLoop() }, "moovio-mpv").apply {
            isDaemon = true
            start()
        }
    }

    fun awaitReady(timeoutMs: Long = 4_000): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (renderReady || initError != null) return renderReady && initError == null
            Thread.sleep(10)
        }
        return false
    }

    private fun renderLoop() {
        val renderCtx = createRenderContext()
        if (renderCtx == null) {
            initError = "Could not create the libmpv software renderer"
            return
        }
        renderReady = true
        var sequence = 0L
        var renderedWidth = -1
        var renderedHeight = -1
        try {
            while (!closed) {
                val width = targetWidth
                val height = targetHeight
                val flags = runCatching { lib.mpv_render_context_update(renderCtx) }.getOrDefault(0L)
                val sizeChanged = width != renderedWidth || height != renderedHeight
                if (width > 0 && height > 0 && ((flags and FRAME_FLAG) != 0L || sizeChanged)) {
                    val drawn = renderFrame(renderCtx, width, height, ++sequence)
                    if (drawn != null) {
                        frame.value = drawn
                        renderedWidth = width
                        renderedHeight = height
                    }
                }
                Thread.sleep(if (lastPollPaused) 40 else 8)
            }
        } catch (_: InterruptedException) {
            // normal shutdown
        } finally {
            runCatching { lib.mpv_render_context_free(renderCtx) }
            renderReady = false
        }
    }

    private fun createRenderContext(): Pointer? {
        val apiType = Memory(3)
        apiType.setString(0, "sw")
        val params = Memory(16L * 2)
        params.setInt(0, RENDER_PARAM_API_TYPE)
        params.setPointer(8, apiType)
        params.setInt(16, 0)
        val result = PointerByReference()
        val code = lib.mpv_render_context_create(result, mpv, params)
        if (code < 0) {
            initError = lib.mpv_error_string(code)
            return null
        }
        return result.value
    }

    /**
     * Double-buffered Skia bitmaps that libmpv draws into directly.
     *
     * The first implementation rendered into a JNA buffer, copied it into a ByteArray, built a
     * raster Image and then called toComposeImageBitmap() — that last step measured ~150 ms per
     * 1600x900 frame and capped playback at ~6 fps. Rendering straight into the bitmap's native
     * pixels and wrapping it with asComposeImageBitmap() removes every per-frame copy.
     */
    private class SwTarget(
        val width: Int,
        val height: Int,
        val bitmap: Bitmap,
        pixelAddress: Long,
        rowBytes: Int,
    ) {
        val params = Memory(16L * 5)
        private val sizeMemory = Memory(8)
        private val formatMemory = Memory(5)
        private val strideMemory = Memory(8)

        init {
            sizeMemory.setInt(0, width)
            sizeMemory.setInt(4, height)
            formatMemory.setString(0, "rgb0")
            strideMemory.setLong(0, rowBytes.toLong())

            params.setInt(0, RENDER_PARAM_SW_SIZE)
            params.setPointer(8, sizeMemory)
            params.setInt(16, RENDER_PARAM_SW_FORMAT)
            params.setPointer(24, formatMemory)
            params.setInt(32, RENDER_PARAM_SW_STRIDE)
            params.setPointer(40, strideMemory)
            params.setInt(48, RENDER_PARAM_SW_POINTER)
            params.setPointer(56, Pointer(pixelAddress))
            params.setInt(64, 0)
        }
    }

    private var swTargets: Pair<SwTarget, SwTarget>? = null

    private fun createSwTarget(width: Int, height: Int): SwTarget? {
        val bitmap = Bitmap()
        val info = ImageInfo(width, height, ColorType.RGBA_8888, ColorAlphaType.OPAQUE)
        val allocated = runCatching { bitmap.allocPixels(info) }.getOrDefault(false)
        if (!allocated) return null
        val pixmap = bitmap.peekPixels() ?: return null
        return SwTarget(width, height, bitmap, pixmap.addr, pixmap.rowBytes)
    }

    private fun swTarget(index: Int, width: Int, height: Int): SwTarget? {
        val current = swTargets
        if (current != null && current.first.width == width && current.first.height == height) {
            return if (index == 0) current.first else current.second
        }
        val first = createSwTarget(width, height) ?: return null
        val second = createSwTarget(width, height) ?: return null
        swTargets = first to second
        return if (index == 0) first else second
    }

    private fun renderFrame(renderCtx: Pointer, width: Int, height: Int, sequence: Long): VideoFrame? {
        // Two targets alternate so a frame stays intact while Compose draws the previous one.
        val target = swTarget((sequence % 2).toInt(), width, height) ?: return null
        val code = lib.mpv_render_context_render(renderCtx, target.params)
        if (code < 0) return null
        // mpv wrote directly into the bitmap's pixels; bump the generation so Skia sees them.
        target.bitmap.notifyPixelsChanged()
        return VideoFrame(target.bitmap.asComposeImageBitmap(), width, height, sequence)
    }

    fun setRenderSize(width: Int, height: Int) {
        targetWidth = width
        targetHeight = height
    }

    fun load(url: String, headers: Map<String, String> = emptyMap(), startPositionMs: Long = 0) {
        if (closed) return
        val headerLine = headers.entries
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .joinToString(",") { "${it.key}: ${it.value}" }
        if (headerLine.isNotBlank()) {
            lib.mpv_set_option_string(mpv, "http-header-fields", headerLine)
        }
        lib.mpv_set_option_string(mpv, "start", "%.3f".format(startPositionMs.coerceAtLeast(0) / 1000.0))
        loadStartedAt = System.currentTimeMillis()
        val code = lib.mpv_command_string(mpv, "loadfile ${quote(url)} replace")
        if (code < 0) {
            _snapshot.update { it.copy(error = lib.mpv_error_string(code)) }
        } else {
            _snapshot.update { it.copy(loaded = true, error = null) }
        }
    }

    fun poll() {
        if (closed) return
        val position = getDouble("time-pos")?.let { (it * 1000).toLong() } ?: 0
        val duration = getDouble("duration")?.let { (it * 1000).toLong() } ?: 0
        val paused = getString("pause") == "yes"
        val buffering = getString("paused-for-cache") == "yes" ||
            (getDouble("cache-buffering-state")?.let { it < 100 } == true && !paused)
        val ended = getString("eof-reached") == "yes"
        val volume = getDouble("volume")?.toInt() ?: 100
        val muted = getString("mute") == "yes"
        val videoWidth = getInt("dwidth") ?: getInt("width") ?: 0
        val videoHeight = getInt("dheight") ?: getInt("height") ?: 0
        val audioId = getInt("aid")
        val subtitleId = getInt("sid")
        val tracks = parseTracks(getString("track-list"))
        val title = getString("media-title")
        val playbackError = getString("idle-active") == "yes" && System.currentTimeMillis() - loadStartedAt > 8_000
        lastPollPaused = paused

        _snapshot.update { current ->
            current.copy(
                positionMs = position,
                durationMs = duration,
                paused = paused,
                buffering = buffering && !paused,
                ended = ended,
                volume = volume,
                muted = muted,
                videoWidth = videoWidth,
                videoHeight = videoHeight,
                audioTracks = tracks.filter { it.type == "audio" },
                subtitleTracks = tracks.filter { it.type == "sub" },
                audioId = audioId,
                subtitleId = subtitleId,
                title = title,
                error = when {
                    current.error != null -> current.error
                    playbackError -> "This stream could not be played. It may be offline or unsupported."
                    else -> null
                },
                loaded = current.loaded && !playbackError,
            )
        }
    }

    private fun parseTracks(json: String?): List<MpvTrack> {
        if (json.isNullOrBlank()) return emptyList()
        return runCatching {
            val array = MoovioJson.parseToJsonElement(json).arr() ?: return emptyList()
            array.mapNotNull { element ->
                val track = element.obj() ?: return@mapNotNull null
                MpvTrack(
                    id = track.int("id") ?: return@mapNotNull null,
                    type = track.str("type") ?: "unknown",
                    title = track.str("title").orEmpty(),
                    language = track.str("lang").orEmpty(),
                    codec = track.str("codec").orEmpty(),
                    selected = track.bool("selected") == true,
                )
            }
        }.getOrDefault(emptyList())
    }

    fun play() = setStringProperty("pause", "no")
    fun pause() = setStringProperty("pause", "yes")
    fun togglePause() {
        val paused = getString("pause") == "yes"
        setStringProperty("pause", if (paused) "no" else "yes")
    }

    fun seekTo(positionMs: Long) {
        val seconds = (positionMs.coerceAtLeast(0) / 1000.0).format()
        lib.mpv_command_string(mpv, "seek $seconds absolute+exact")
    }

    fun seekBy(deltaMs: Long) {
        val seconds = (deltaMs / 1000.0).format()
        lib.mpv_command_string(mpv, "seek $seconds relative+exact")
    }

    fun setVolume(volume: Int) {
        setStringProperty("volume", volume.coerceIn(0, 130).toString())
    }

    fun toggleMute() {
        val muted = getString("mute") == "yes"
        setStringProperty("mute", if (muted) "no" else "yes")
    }

    fun setSpeed(speed: Double) {
        setStringProperty("speed", speed.coerceIn(0.25, 3.0).toString())
    }

    fun selectAudio(trackId: Int?) {
        setStringProperty("aid", trackId?.toString() ?: "no")
    }

    fun selectSubtitle(trackId: Int?) {
        setStringProperty("sid", trackId?.toString() ?: "no")
    }

    fun addSubtitle(url: String) {
        lib.mpv_command_string(mpv, "sub-add ${quote(url)} select")
    }

    fun stop() {
        runCatching { lib.mpv_command_string(mpv, "stop") }
    }

    /** Reads a libmpv property for diagnostics (e.g. `hwdec-current`, `frame-drop-count`). */
    fun diagnostic(name: String): String? = if (closed) null else getString(name)

    private fun getString(name: String): String? {
        val pointer = lib.mpv_get_property_string(mpv, name) ?: return null
        return try {
            pointer.getString(0)
        } finally {
            lib.mpv_free(pointer)
        }
    }

    private fun getDouble(name: String): Double? = getString(name)?.toDoubleOrNull()

    private fun getInt(name: String): Int? = getString(name)?.toIntOrNull()

    private fun setStringProperty(name: String, value: String) {
        if (!closed) lib.mpv_set_property_string(mpv, name, value)
    }

    private fun quote(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ") + "\""

    private fun Double.format(): String = "%.3f".format(this)

    override fun close() {
        if (closed) return
        closed = true
        runCatching { setStringProperty("pause", "yes") }
        runCatching { renderThread?.join(2_500) }
        runCatching { lib.mpv_terminate_destroy(mpv) }
        frame.value = null
    }

    companion object {
        private const val FRAME_FLAG = 1L
        private const val RENDER_PARAM_API_TYPE = 1
        private const val RENDER_PARAM_SW_SIZE = 17
        private const val RENDER_PARAM_SW_FORMAT = 18
        private const val RENDER_PARAM_SW_STRIDE = 19
        private const val RENDER_PARAM_SW_POINTER = 20

        fun open(settings: AppSettings): Result<MpvPlayer> {
            val (lib, path) = MpvLibrary.loadBest(settings.mpvLibPath)
                ?: return Result.failure(
                    IllegalStateException(
                        "libmpv-2.dll was not found. Run `./gradlew fetchLibmpv` or set the DLL path in Settings → System.",
                    ),
                )
            val mpv = lib.mpv_create()
                ?: return Result.failure(IllegalStateException("mpv_create() returned null"))

            fun option(name: String, value: String) {
                lib.mpv_set_option_string(mpv, name, value)
            }
            option("vo", "libmpv")
            option("config", "no")
            option("osc", "no")
            option("osd-level", "0")
            option("terminal", "no")
            option("msg-level", "all=error")
            option("idle", "yes")
            option("keep-open", "yes")
            option("input-default-bindings", "no")
            option("input-vo-keyboard", "no")
            option("audio-client-name", "Moovio")
            option("user-agent", "Moovio/1.0 (Windows)")
            // Copy-back hardware decoding: the software render API needs frames in system memory,
            // so auto-safe could end up decoding on the CPU entirely. -copy modes fall back
            // cleanly to software decoding when no hardware decoder is available.
            option("hwdec", if (settings.hardwareDecoding) "auto-copy-safe" else "no")
            // Network streams: keep a generous read-ahead so short stalls don't empty the cache.
            option("cache", "yes")
            option("cache-secs", "30")
            option("demuxer-readahead-secs", "20")
            option("demuxer-max-bytes", "268435456")
            option("demuxer-max-back-bytes", "67108864")
            option("stream-buffer-size", "1048576")
            // Keep audio master-clock and drop late video frames instead of falling behind when
            // the CPU-bound colour conversion can't keep up.
            option("video-sync", "audio")
            option("framedrop", "decoder+vo")
            // The render API converts YUV -> RGB and scales in software; fast bilinear is cheap
            // and indistinguishable at typical playback sizes.
            option("sws-scaler", "fast-bilinear")
            option("sub-auto", "fuzzy")
            option("volume", settings.playerVolume.toString())

            val code = lib.mpv_initialize(mpv)
            if (code < 0) {
                val message = lib.mpv_error_string(code)
                runCatching { lib.mpv_terminate_destroy(mpv) }
                return Result.failure(IllegalStateException("mpv_initialize failed: $message"))
            }
            return Result.success(
                MpvPlayer(lib, mpv, path, settings.hardwareDecoding),
            )
        }
    }
}
