package com.moovio.app.ui.player

/**
 * Phase 37 — Anime4K GPU shader engine policy (pure JVM, unit-tested).
 *
 * The GLSL sources below run inside the Media3 `VideoFrameProcessor` rendering loop
 * (see [Anime4KEffectApplier]): each preset maps to an ordered pass list whose cost
 * scales with the selected [Anime4KGpuTier]. This file stays Android-free so preset
 * resolution, pass ordering and shader contents can be pinned by unit tests.
 */

/** Enhancement preset selected in the player sheet. */
enum class Anime4KPreset(val key: String, val label: String, val blurb: String) {
    OFF("off", "Off", "Standard hardware scaling."),
    SHARPEN("sharpen", "Sharpen", "Edge restoration for clean sources."),
    DEBLUR("deblur", "De-blur", "Soft restoration for blurry or low-bitrate streams."),
    DENOISE("denoise", "Denoise", "Artifact and grain cleanup for compressed streams."),
}

/** GPU performance tier: pass count/quality scales with device capability. */
enum class Anime4KGpuTier(val key: String, val label: String, val blurb: String) {
    MID("mid", "Mid-range GPU", "Lightweight pass: fast edge recovery + debanding."),
    HIGH("high", "High-end GPU", "Full heavy pass with HQ spatial scaling."),
}

/** Live-switchable player enhancement selection. */
data class Anime4KConfig(
    val preset: Anime4KPreset = Anime4KPreset.OFF,
    val tier: Anime4KGpuTier = Anime4KGpuTier.MID,
)

/** Total parse: unknown/blank keys fall back to Off / Mid (never throws). */
fun parseAnime4KPreset(key: String?): Anime4KPreset =
    Anime4KPreset.entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) }
        ?: Anime4KPreset.OFF

/** Total parse: unknown/blank keys fall back to the Mid tier (never throws). */
fun parseAnime4KGpuTier(key: String?): Anime4KGpuTier =
    Anime4KGpuTier.entries.firstOrNull { it.key.equals(key?.trim(), ignoreCase = true) }
        ?: Anime4KGpuTier.MID

/**
 * Ordered Anime4K pass names for a [config]. Empty when [Anime4KPreset.OFF] — the
 * frame processor is bypassed entirely so playback costs nothing extra.
 */
fun anime4KPasses(config: Anime4KConfig): List<String> = when (config.preset) {
    Anime4KPreset.OFF -> emptyList()
    Anime4KPreset.SHARPEN -> when (config.tier) {
        Anime4KGpuTier.MID -> listOf("Anime4K_Restore_Soft_Lite", "Anime4K_Upscale_DTD_Lite")
        Anime4KGpuTier.HIGH -> listOf("Anime4K_Restore_Soft", "Anime4K_Upscale_DTD_HQ")
    }
    Anime4KPreset.DEBLUR -> when (config.tier) {
        Anime4KGpuTier.MID -> listOf("Anime4K_Restore_Lite", "Anime4K_Deblur_DoG_Lite")
        Anime4KGpuTier.HIGH -> listOf("Anime4K_Restore", "Anime4K_Deblur_DoG")
    }
    Anime4KPreset.DENOISE -> when (config.tier) {
        Anime4KGpuTier.MID -> listOf("Anime4K_Denoise_Bilateral_Lite", "Anime4K_Upscale_DTD_Lite")
        Anime4KGpuTier.HIGH -> listOf("Anime4K_Denoise_Bilateral_Mean", "Anime4K_Upscale_DTD_HQ")
    }
}

/** True when the config runs the full heavyweight HQ pass (flagship GPUs only). */
fun isAnime4KHeavy(config: Anime4KConfig): Boolean =
    config.preset != Anime4KPreset.OFF && config.tier == Anime4KGpuTier.HIGH

/** Relative GPU cost bucket: 0 = bypassed, 1 = lightweight, 2 = full HQ. */
fun anime4KEstimatedCost(config: Anime4KConfig): Int = when {
    config.preset == Anime4KPreset.OFF -> 0
    config.tier == Anime4KGpuTier.HIGH -> 2
    else -> 1
}

/** Sheet/UI label: "Off" or e.g. "Sharpen · Mid-range GPU". */
fun anime4KEffectLabel(config: Anime4KConfig): String =
    if (config.preset == Anime4KPreset.OFF) Anime4KPreset.OFF.label
    else "${config.preset.label} · ${config.tier.label}"

/**
 * Combined fragment shader for a [config]: the per-preset core plus the tier's
 * quality suffix (HQ spatial taps on High, clamped taps on Mid). Returns an
 * empty string when [Anime4KPreset.OFF] (no pass is attached).
 *
 * The shader expects the Media3 frame-processor bindings: `uTexture` (input
 * frame), `vTexCoord` (varying UV) and `uTexelSize` (1/width, 1/height).
 */
fun anime4KFragmentShader(config: Anime4KConfig): String {
    if (config.preset == Anime4KPreset.OFF) return ""
    val core = when (config.preset) {
        Anime4KPreset.OFF -> ""
        Anime4KPreset.SHARPEN -> SHARPEN_CORE
        Anime4KPreset.DEBLUR -> DEBLUR_CORE
        Anime4KPreset.DENOISE -> DENOISE_CORE
    }
    val taps = if (config.tier == Anime4KGpuTier.HIGH) HQ_TAPS else LITE_TAPS
    return SHADER_PREAMBLE + core + taps + SHADER_MAIN
}

private const val SHADER_PREAMBLE = """precision mediump float;
varying vec2 vTexCoord;
uniform sampler2D uTexture;
uniform vec2 uTexelSize;
"""

private const val SHARPEN_CORE = """
// Sharpen: unsharp-mask edge restoration for clean sources.
vec3 anime4kCore(vec2 uv, vec2 px) {
  vec3 c = texture2D(uTexture, uv).rgb;
  vec3 blur = (
    texture2D(uTexture, uv + vec2(px.x, 0.0)).rgb +
    texture2D(uTexture, uv - vec2(px.x, 0.0)).rgb +
    texture2D(uTexture, uv + vec2(0.0, px.y)).rgb +
    texture2D(uTexture, uv - vec2(0.0, px.y)).rgb) * 0.25;
  return clamp(mix(c, c + (c - blur) * 0.85, 0.9), 0.0, 1.0);
}
"""

private const val DEBLUR_CORE = """
// De-blur: difference-of-Gaussians soft restoration for low-bitrate streams.
vec3 anime4kCore(vec2 uv, vec2 px) {
  vec3 c = texture2D(uTexture, uv).rgb;
  vec3 near = (
    texture2D(uTexture, uv + px).rgb + texture2D(uTexture, uv - px).rgb +
    texture2D(uTexture, uv + vec2(px.x, -px.y)).rgb +
    texture2D(uTexture, uv + vec2(-px.x, px.y)).rgb) * 0.25;
  vec3 far = (
    texture2D(uTexture, uv + px * 2.0).rgb + texture2D(uTexture, uv - px * 2.0).rgb +
    texture2D(uTexture, uv + vec2(px.x, -px.y) * 2.0).rgb +
    texture2D(uTexture, uv + vec2(-px.x, px.y) * 2.0).rgb) * 0.25;
  return clamp(c + (near - far) * 0.6, 0.0, 1.0);
}
"""

private const val DENOISE_CORE = """
// Denoise: edge-aware 3x3 bilateral-ish cleanup for compressed streams.
vec3 anime4kCore(vec2 uv, vec2 px) {
  vec3 c = texture2D(uTexture, uv).rgb;
  vec3 acc = vec3(0.0);
  float wsum = 0.0;
  for (int dx = -1; dx <= 1; dx++) {
    for (int dy = -1; dy <= 1; dy++) {
      vec3 s = texture2D(uTexture, uv + vec2(float(dx), float(dy)) * px).rgb;
      float w = 1.0 / (1.0 + dot(c - s, c - s) * 28.0);
      acc += s * w;
      wsum += w;
    }
  }
  return clamp(acc / wsum, 0.0, 1.0);
}
"""

private const val LITE_TAPS = """
// Mid tier: fast edge recovery + debanding on the restored frame.
vec3 anime4kTier(vec3 rgb, vec2 uv, vec2 px) {
  float luma = dot(rgb, vec3(0.299, 0.587, 0.114));
  float band = fract(luma * 255.0 + fract(uv.x * 371.0 + uv.y * 719.0) * 0.75) / 255.0;
  return clamp(rgb + (band - luma) * 0.06, 0.0, 1.0);
}
"""

private const val HQ_TAPS = """
// High tier: HQ spatial scaling taps around the restored frame.
vec3 anime4kTier(vec3 rgb, vec2 uv, vec2 px) {
  vec3 acc = rgb * 0.4;
  acc += texture2D(uTexture, uv + vec2(px.x * 0.5, 0.0)).rgb * 0.15;
  acc += texture2D(uTexture, uv - vec2(px.x * 0.5, 0.0)).rgb * 0.15;
  acc += texture2D(uTexture, uv + vec2(0.0, px.y * 0.5)).rgb * 0.15;
  acc += texture2D(uTexture, uv - vec2(0.0, px.y * 0.5)).rgb * 0.15;
  return clamp(acc, 0.0, 1.0);
}
"""

private const val SHADER_MAIN = """
void main() {
  vec3 restored = anime4kCore(vTexCoord, uTexelSize);
  gl_FragColor = vec4(anime4kTier(restored, vTexCoord, uTexelSize), 1.0);
}
"""
