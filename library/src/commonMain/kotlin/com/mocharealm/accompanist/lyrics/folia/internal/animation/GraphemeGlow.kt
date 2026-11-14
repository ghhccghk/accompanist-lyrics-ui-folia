// Port of pinned Folia wordGlow.ts; see docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.animation

/** CSS cubic Bézier: solve x first, then evaluate y (Framer easeInOut/easeOut). */
internal fun cubicEase(t: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val x = t.coerceIn(0f, 1f)
    if (x == 0f || x == 1f) return x
    fun curve(u: Double, a: Float, b: Float): Double {
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
internal fun easeInOut(t: Float) = cubicEase(t, .42f, 0f, .58f, 1f)
internal fun easeOut(t: Float) = cubicEase(t, 0f, 0f, .58f, 1f)

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
    wordStartUs: Long, charStartUs: Long, charEndUs: Long): Float {
    val delay = if (mode == Reveal.Normal && total > 1) maxOf(0, charStartUs - wordStartUs) else 0
    if (elapsedUs < delay) return 0f
    val duration = when (mode) {
        Reveal.Instant -> minOf(durationUs, 120_000)
        Reveal.Fast -> durationUs.coerceIn(120_000, 200_000)
        Reveal.Normal -> if (total > 1) maxOf(charEndUs - charStartUs, 1_000) * 6 else durationUs
    }.coerceAtLeast(1)
    val q = ((elapsedUs - delay).toFloat() / duration).coerceIn(0f, 1f)
    val peak = when (mode) { Reveal.Instant -> .35f; Reveal.Fast -> .4f; Reveal.Normal -> if (total > 1) .3f else .9f }
    val easing: (Float) -> Float = if (mode == Reveal.Instant) ::easeOut else ::easeInOut
    return if (q < peak) easing(q / peak) else if (mode == Reveal.Normal && total <= 1) 1f
        else 1 - easing((q - peak) / (1 - peak))
}

internal fun passedGraphemeGlow(value: Float, elapsedUs: Long, mode: Reveal): Float {
    val duration = when (mode) { Reveal.Normal -> 900_000; Reveal.Fast -> 220_000; Reveal.Instant -> 120_000 }
    return value * (1 - easeOut(elapsedUs.toFloat() / duration))
}

internal fun sampleGraphemeGlow(timeUs: Long, lineEnterUs: Long, wordStartUs: Long, wordEndUs: Long,
    charStartUs: Long, charEndUs: Long, total: Int, profile: RenderProfile): Float {
    val begin = maxOf(lineEnterUs, wordStartUs - profile.lookaheadUs)
    if (timeUs < begin) return 0f
    val end = activeWordEnd(wordStartUs, wordEndUs, profile).coerceAtLeast(begin)
    val value = activeGraphemeGlow(minOf(timeUs, end) - begin, profile.mode,
        displayDuration(wordStartUs, wordEndUs, profile), total, wordStartUs, charStartUs, charEndUs)
    return if (timeUs <= end) value else passedGraphemeGlow(value, timeUs - end, profile.mode)
}
