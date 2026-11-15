// Port of pinned Folia wordGlow.ts; see docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.animation

/** CSS cubic Bézier: solve x first, then evaluate y (Framer easeInOut/easeOut). */
internal fun cubicEase(t: Double, x1: Double, y1: Double, x2: Double, y2: Double): Float {
    val x = t.coerceIn(0.0, 1.0)
    if (x == 0.0 || x == 1.0) return x.toFloat()
    fun curve(u: Double, a: Double, b: Double): Double {
        val v = 1 - u
        return 3 * v * v * u * a + 3 * v * u * u * b + u * u * u
    }
    var lo = 0.0; var hi = 1.0; var mid = .5
    // Match the pinned Motion runtime's 12 subdivisions and 1e-7 x tolerance.
    repeat(12) {
        mid = (lo + hi) / 2
        val error = curve(mid, x1, x2) - x
        if (kotlin.math.abs(error) <= .0000001) return curve(mid, y1, y2).toFloat()
        if (error > 0) hi = mid else lo = mid
    }
    return curve(mid, y1, y2).toFloat()
}
internal fun cubicEase(t: Float, x1: Float, y1: Float, x2: Float, y2: Float) =
    cubicEase(t.toDouble(),x1.toDouble(),y1.toDouble(),x2.toDouble(),y2.toDouble())
internal fun easeInOut(t: Double) = cubicEase(t, .42, 0.0, .58, 1.0)
internal fun easeOut(t: Double) = cubicEase(t, 0.0, 0.0, .58, 1.0)
internal fun easeInOut(t: Float) = easeInOut(t.toDouble())
internal fun easeOut(t: Float) = easeOut(t.toDouble())

internal fun activeWordEnd(startUs: Long, endUs: Long, profile: RenderProfile): Long = when (profile.mode) {
    Reveal.Normal -> endUs
    Reveal.Fast -> minOf(profile.renderEndUs, maxOf(endUs, startUs + 120_000))
    Reveal.Instant -> profile.renderEndUs
}
internal fun displayDuration(startUs: Long, endUs: Long, profile: RenderProfile) = maxOf(
    activeWordEnd(startUs, endUs, profile) - startUs,
    when (profile.mode) { Reveal.Normal -> 100_000; Reveal.Fast -> 120_000; Reveal.Instant -> 80_000 })

/** Elapsed time is relative to activation, not the first grapheme's nominal start. */
internal fun activeGraphemeGlow(elapsedUs: Long, mode: Reveal, durationUs: Long, total: Int,
    wordStartUs: Long, charStartUs: Long, charEndUs: Long,
    charStartSeconds:Double=charStartUs/1e6, charEndSeconds:Double=charEndUs/1e6): Float {
    val delayMs = if (mode == Reveal.Normal && total > 1) maxOf(0.0,charStartSeconds-wordStartUs/1e6)*1000 else 0.0
    val elapsedMs=motionElapsedUs(elapsedUs)/1000.0-delayMs
    if (elapsedMs < 0) return 0f
    val durationMs = when (mode) {
        Reveal.Instant -> minOf(durationUs/1000.0,120.0)
        Reveal.Fast -> (durationUs/1000.0).coerceIn(120.0,200.0)
        Reveal.Normal -> if (total > 1) maxOf(charEndSeconds-charStartSeconds,.001)*6*1000 else durationUs/1000.0
    }.coerceAtLeast(.001)
    val peak = when (mode) { Reveal.Instant -> .35; Reveal.Fast -> .4; Reveal.Normal -> if (total > 1) .3 else .9 }
    val peakMs=durationMs*peak
    val easing: (Double) -> Float = if (mode == Reveal.Instant) ::easeOut else ::easeInOut
    return if (elapsedMs < peakMs) easing(elapsedMs/peakMs) else if (mode == Reveal.Normal && total <= 1) 1f
        else 1 - easing((elapsedMs-peakMs)/(durationMs-peakMs))
}

internal fun passedGraphemeGlow(value: Float, elapsedUs: Long, mode: Reveal): Float {
    val duration = when (mode) { Reveal.Normal -> 900_000; Reveal.Fast -> 220_000; Reveal.Instant -> 120_000 }
    return value * (1 - easeOut(motionElapsedUs(elapsedUs).toDouble() / duration))
}

internal fun sampleGraphemeGlow(timeUs: Long, lineEnterUs: Long, wordStartUs: Long, wordEndUs: Long,
    charStartUs: Long, charEndUs: Long, total: Int, profile: RenderProfile,
    charStartSeconds:Double=charStartUs/1e6, charEndSeconds:Double=charEndUs/1e6): Float {
    val begin = maxOf(lineEnterUs, wordStartUs - profile.lookaheadUs)
    if (timeUs < begin) return 0f
    val end = activeWordEnd(wordStartUs, wordEndUs, profile).coerceAtLeast(begin)
    val value = activeGraphemeGlow(minOf(timeUs, end) - begin, profile.mode,
        displayDuration(wordStartUs, wordEndUs, profile), total, wordStartUs, charStartUs, charEndUs,charStartSeconds,charEndSeconds)
    return if (timeUs <= end) value else passedGraphemeGlow(value, timeUs - end, profile.mode)
}
