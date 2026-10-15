package com.novastream.app.ui.player

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import androidx.media3.common.VideoFrameProcessingException
import androidx.media3.common.util.GlProgram
import androidx.media3.common.util.GlUtil
import androidx.media3.common.util.Size
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BaseGlShaderProgram
import androidx.media3.effect.GlEffect
import androidx.media3.effect.GlShaderProgram
import androidx.media3.exoplayer.ExoPlayer

private const val ANIME4K_TAG = "Anime4K"

/**
 * Phase 37 — a [GlEffect] carrying the player's [Anime4KConfig] into the Media3
 * `VideoFrameProcessor` rendering loop. [Anime4KPreset.OFF] reports itself as a
 * no-op so the frame processor skips the pass entirely.
 */
@UnstableApi
class Anime4KGlEffect(val config: Anime4KConfig) : GlEffect {
    override fun toGlShaderProgram(context: Context, useHdr: Boolean): GlShaderProgram =
        Anime4KShaderProgram(config)

    override fun isNoOp(inputWidth: Int, inputHeight: Int): Boolean =
        config.preset == Anime4KPreset.OFF
}

/**
 * Single-frame GLSL program running the combined Anime4K fragment shader from
 * [anime4KFragmentShader] over each decoded frame. The vertex stage draws a plain
 * full-screen quad and derives UVs from the vertex position, so no texture
 * transform matrix is needed.
 */
@UnstableApi
class Anime4KShaderProgram(config: Anime4KConfig) :
    BaseGlShaderProgram(
        /* useHighPrecisionColorComponents= */ false,
        /* texturePoolCapacity= */ 1,
    ) {
    private val glProgram: GlProgram
    private var frameWidth = 0
    private var frameHeight = 0

    init {
        val fragment = anime4KFragmentShader(config)
        require(fragment.isNotEmpty()) { "Anime4K OFF has no shader" }
        glProgram = try {
            GlProgram(VERTEX_SHADER, fragment)
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    override fun configure(inputWidth: Int, inputHeight: Int): Size {
        frameWidth = inputWidth
        frameHeight = inputHeight
        return Size(inputWidth, inputHeight)
    }

    override fun drawFrame(inputTexId: Int, presentationTimeUs: Long) {
        try {
            glProgram.use()
            glProgram.setSamplerTexIdUniform("uTexture", inputTexId, /* texUnitIndex= */ 0)
            glProgram.setFloatsUniform(
                "uTexelSize",
                floatArrayOf(1f / frameWidth.coerceAtLeast(1), 1f / frameHeight.coerceAtLeast(1)),
            )
            glProgram.setBufferAttribute("aPosition", QUAD_COORDS, /* elementSize= */ 2)
            glProgram.bindAttributesAndUniforms()
            GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, /* first= */ 0, /* count= */ 4)
            GlUtil.checkGlError()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e, presentationTimeUs)
        }
    }

    override fun release() {
        super.release()
        try {
            glProgram.delete()
        } catch (e: GlUtil.GlException) {
            throw VideoFrameProcessingException(e)
        }
    }

    companion object {
        private const val VERTEX_SHADER = """attribute vec4 aPosition;
varying vec2 vTexCoord;
void main() {
  gl_Position = aPosition;
  vTexCoord = (aPosition.xy * 0.5) + 0.5;
}
"""

        /** Full-screen quad as a triangle strip: bottom-left, bottom-right, top-left, top-right. */
        private val QUAD_COORDS = floatArrayOf(
            -1f, -1f,
            1f, -1f,
            -1f, 1f,
            1f, 1f,
        )
    }
}

/**
 * Applies an [Anime4KConfig] to a live [ExoPlayer] without rebuilding it, so the
 * player sheet can switch presets/tiers mid-playback. Total: any failure (e.g. a
 * device whose GPU rejects the program) logs and returns `false`, leaving the
 * current stream untouched instead of crashing playback.
 */
@UnstableApi
object Anime4KEffectApplier {
    fun apply(player: ExoPlayer, config: Anime4KConfig): Boolean = runCatching {
        if (config.preset == Anime4KPreset.OFF) {
            player.setVideoEffects(emptyList())
        } else {
            player.setVideoEffects(listOf(Anime4KGlEffect(config)))
        }
        Log.d(ANIME4K_TAG, "applied ${anime4KEffectLabel(config)} passes=${anime4KPasses(config)}")
        true
    }.onFailure { e ->
        Log.w(ANIME4K_TAG, "effect apply failed for ${anime4KEffectLabel(config)}", e)
    }.getOrDefault(false)
}
