// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 dioramaSceneFrame.ts:updateDioramaActiveUnits,
// dioramaSceneUnits.ts:resolveGradientEnergy and scene constants.
// Reconstructible envelope adaptation: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import kotlin.math.*

/** THREE.Color values are LINEAR, not encoded sRGB channels. */
internal data class DioramaLinearColor(val red: Double, val green: Double, val blue: Double) {
    fun mix(b: DioramaLinearColor, t: Double) = DioramaLinearColor(
        red + (b.red - red) * t, green + (b.green - green) * t, blue + (b.blue - blue) * t)
    operator fun times(k: Double) = DioramaLinearColor(red * k, green * k, blue * k)
}
internal data class DioramaEnvelope(val light: Double, val soul: Double)
internal data class DioramaUnitTuning(val glow: Double = 1.0, val soul: Double = 1.0,
    val soulActive: Boolean = true, val gradient: Double = 1.0)
internal data class DioramaUnitFrame(val envelope: DioramaEnvelope, val fill: DioramaLinearColor,
    val baseOpacity: Double, val glowOpacity: Double, val glowVisible: Boolean,
    val soulOpacity: Double, val soulLift: Double, val soulScale: Double, val soulVisible: Boolean)

private fun smooth(value: Double): Double { val t = value.coerceIn(0.0, 1.0); return t * t * (3 - 2 * t) }

internal fun dioramaGradientEnergy(now: Double, start: Double, end: Double): Double {
    if (now <= start) return 0.0
    if (now < end) return smooth((now - start) / max(end - start, .001))
    val since = now - end
    return if (since <= .35) 1.0 else 1 - smooth((since - .35) / 1.8)
}

/** Exact shared source exponential step, independent of any display loop. */
internal fun dioramaEnvelopeStep(current: Double, target: Double, attack: Double, release: Double, delta: Double) =
    current + (target - current) * (1 - exp(-(if (target > current) attack else release) * delta))

/** Media-domain solution with initial charge0 before this unit's own interval.
 * No stored previous frame, integer frame grid or replay. Original Float32 RAF
 * boundary sampling is compared separately and is not claimed identical. */
internal fun dioramaMediaEnvelope(now: Double, start: Double, end: Double): DioramaEnvelope {
    fun charge(attack: Double, release: Double): Double {
        if (now <= start || end <= start) return 0.0
        val duration = min(now, end) - start
        return (1 - exp(-attack * duration)) * exp(-release * max(0.0, now - end))
    }
    return DioramaEnvelope(charge(14.0, 4.5), charge(12.0, 2.2))
}

/** Original material/transform assignment from already resolved charges.
 * Glow and soul read the SAME fill, never tint independently. No audio is
 * fabricated: power defaults0 and breath defaults1 when no explicit host data. */
internal fun dioramaUnitFrame(now: Double, start: Double, end: Double, envelope: DioramaEnvelope,
    primary: DioramaLinearColor, accent: DioramaLinearColor, tuning: DioramaUnitTuning = DioramaUnitTuning(),
    keyword: DioramaLinearColor? = null, life: Double = 1.0, breath: Double = 1.0,
    power: Double = 0.0): DioramaUnitFrame {
    var tint = accent
    val separation = abs(accent.red - primary.red) + abs(accent.green - primary.green) + abs(accent.blue - primary.blue)
    if (separation < .4) {
        val deficit = 1 - separation / .4
        val p = (primary.red + primary.green + primary.blue) / 3
        val a = (accent.red + accent.green + accent.blue) / 3
        val target = if (p > .5) a * (1 - .5 * deficit) else a + (1 - a) * .55 * deficit
        tint = tint.mix(DioramaLinearColor(target, target, target), deficit)
    }
    val gradientStrength = min(1.5, tuning.gradient)
    val gradientHot = ((gradientStrength - .1) / 1.4).coerceIn(0.0, 1.0)
    val deep = tint * (.8 - .18 * gradientHot)
    val current = now >= start && now < end
    val sung = now >= end
    val sungMix = if (sung) 1.0 else if (current) ((now - start) / max(end - start, .001)).coerceIn(0.0, 1.0) else 0.0
    val energy = dioramaGradientEnergy(now, start, end) * gradientStrength
    val opacity = min(1.0, (.5 + (.92 - .5) * sungMix + .08 * min(1.0, energy)) * life)
    val target = keyword ?: if (tuning.gradient > 0) deep else tint
    val progress = if (tuning.gradient > 0) energy else envelope.light * 1.15
    val fill = primary.mix(target, progress.coerceIn(0.0, 1.0))
    val glowLevel = envelope.light * life * breath * (.6 + .4 * power) * min(1.5, tuning.glow)
    val strength = min(1.5, tuning.soul)
    val flight = if (sung) smooth((now - end) / .5) else 0.0
    val onGlyph = (1 - flight) * (if (tuning.soulActive) strength else 0.0)
    val flown = flight * strength
    val soulOpacity = min(1.0, .6 * life * envelope.soul * (onGlyph + flown))
    return DioramaUnitFrame(envelope, fill, opacity, min(1.0, .9 * glowLevel), glowLevel > .012,
        soulOpacity, .62 * (.06 * onGlyph + .5 * flown), 1 + .1 * onGlyph + .3 * flown, soulOpacity > .015)
}

/** Finite source operator for numerical parity only. Float32 storage matches
 * original typed arrays; callers must not use display-frame accumulation as
 * the public time model. */
internal fun dioramaUnitStep(now: Double, start: Double, end: Double, delta: Double, previous: DioramaEnvelope,
    primary: DioramaLinearColor, accent: DioramaLinearColor, tuning: DioramaUnitTuning,
    keyword: DioramaLinearColor? = null, life: Double = 1.0, breath: Double = 1.0, power: Double = 0.0): DioramaUnitFrame {
    val target = if (now >= start && now < end) 1.0 else 0.0
    val next = DioramaEnvelope(
        dioramaEnvelopeStep(previous.light.toFloat().toDouble(), target, 14.0, 4.5, delta).toFloat().toDouble(),
        dioramaEnvelopeStep(previous.soul.toFloat().toDouble(), target, 12.0, 2.2, delta).toFloat().toDouble())
    return dioramaUnitFrame(now, start, end, next, primary, accent, tuning, keyword, life, breath, power)
}
