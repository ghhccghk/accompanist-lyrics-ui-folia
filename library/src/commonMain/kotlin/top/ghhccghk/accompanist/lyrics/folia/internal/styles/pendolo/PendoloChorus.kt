// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 pendoloMotionProfile.ts / PendoloActiveLyricSweep.tsx.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import kotlin.math.ceil
import kotlin.math.floor

internal data class PendoloChorusPresentation(
    val active: Boolean, val accentMix: Double, val haloOpacity: Double,
    val haloScale: Double, val glowMultiplier: Double, val duration: Double,
)

/** Playback-active means the source current/focal line, not transport playing.
 * Pausing must freeze a chorus, not switch its visual treatment off. */
internal fun pendoloChorus(isChorus: Boolean, isPlaybackActive: Boolean,
    intensity: PendoloIntensity = PendoloIntensity.NORMAL): PendoloChorusPresentation {
    val active = isChorus && isPlaybackActive
    val profile = when (intensity) {
        PendoloIntensity.CALM -> doubleArrayOf(.22, 1.012, .72, .52)
        PendoloIntensity.NORMAL -> doubleArrayOf(.34, 1.026, 1.0, .42)
        PendoloIntensity.CHAOTIC -> doubleArrayOf(.5, 1.045, 1.42, .3)
    }
    return PendoloChorusPresentation(active, if (active) .58 else .32,
        if (active) profile[0] else 0.0, if (active) profile[1] else 1.0,
        if (active) profile[2] else 0.0, profile[3])
}

internal data class PendoloChorusFilter(val firstSigma: Double, val secondSigma: Double, val padding: Int)
internal fun pendoloChorusFilter(fontPx: Double, multiplier: Double): PendoloChorusFilter {
    require(fontPx.isFinite() && fontPx > 0 && multiplier.isFinite() && multiplier >= 0)
    return PendoloChorusFilter(floor(fontPx * .3 * multiplier + .5),
        floor(fontPx * .62 * multiplier + .5), ceil(fontPx * .7 * multiplier + 10).toInt())
}
