// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 cadenzaEnvelopes.ts and renderHints.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Provenance/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import kotlin.math.*

internal enum class CadenzaReveal { NORMAL, FAST, INSTANT }
internal enum class CadenzaTransition { NORMAL, FAST, NONE }
internal enum class CadenzaStatus { WAITING, PASSED, ACTIVE }
internal data class CadenzaWord(val text: String, val start: Double, val end: Double,
    val syllables: List<CadenzaWord> = emptyList())
internal data class CadenzaHints(val rawDuration: Double, val renderEnd: Double,
    val transition: CadenzaTransition, val reveal: CadenzaReveal)
internal data class CadenzaLyric(val text: String, val start: Double, val end: Double,
    val words: List<CadenzaWord>, val hints: CadenzaHints? = null)
internal data class CadenzaGlyphTime(val text: String, val start: Double, val end: Double)
internal data class CadenzaTransitionTiming(val enter: Double, val exit: Double, val hold: Double)
internal data class CadenzaTiming(val hints: CadenzaHints?, val end: Double, val reveal: CadenzaReveal,
    val lastWordEnd: Double, val transition: CadenzaTransitionTiming)
internal data class CadenzaLineEnvelope(val opacity: Double = 1.0, val scale: Double = 1.0, val blur: Double = 0.0)

internal fun cadenzaGraphemes(text: String): List<String> =
    graphemeBoundaries(text).sorted().zipWithNext { a, b -> text.substring(a, b) }
internal fun cadenzaIsCjk(text: String) = text.any { it in '\u4e00'..'\u9fa5' || it in '\u3040'..'\u30ff' || it in '\uac00'..'\ud7af' }
private fun unit(value: Double) = value.coerceIn(0.0, 1.0)
private fun mix(from: Double, to: Double, amount: Double) = from + (to - from) * amount
private fun easeOut(value: Double) = 1 - (1 - unit(value)).pow(3)

internal fun cadenzaTransitionTiming(raw: Double, mode: CadenzaTransition, reveal: CadenzaReveal): CadenzaTransitionTiming =
    when (mode) {
        CadenzaTransition.NONE -> CadenzaTransitionTiming(0.0, 0.0, 0.0)
        CadenzaTransition.FAST -> CadenzaTransitionTiming((raw * .45).coerceIn(.045, .06),
            (raw * .22).coerceIn(.03, .04), if (reveal == CadenzaReveal.INSTANT) 0.0 else .03)
        CadenzaTransition.NORMAL -> CadenzaTransitionTiming((max(raw, .12) * .34).coerceIn(.22, .42),
            (max(raw, .12) * .18).coerceIn(.18, .32), if (reveal == CadenzaReveal.INSTANT) 0.0 else .06)
    }

/** Unlike getLineRenderHints, the Cadenza source does NOT infer hints when absent. */
internal fun cadenzaTiming(line: CadenzaLyric): CadenzaTiming {
    val hint = line.hints
    val reveal = hint?.reveal ?: CadenzaReveal.NORMAL
    return CadenzaTiming(hint, hint?.renderEnd ?: line.end, reveal, line.words.lastOrNull()?.end ?: line.end,
        cadenzaTransitionTiming(hint?.rawDuration ?: max(line.end - line.start, 0.0),
            hint?.transition ?: CadenzaTransition.NORMAL, reveal))
}

internal fun cadenzaWordStatus(time: Double, timing: CadenzaTiming, word: CadenzaWord): CadenzaStatus {
    val lookahead = when (timing.reveal) { CadenzaReveal.FAST -> .045; CadenzaReveal.INSTANT -> 0.0; else -> .18 }
    val end = if (timing.reveal == CadenzaReveal.INSTANT) timing.end else word.end
    return when { time >= word.start - lookahead && time <= end -> CadenzaStatus.ACTIVE
        time > end -> CadenzaStatus.PASSED; else -> CadenzaStatus.WAITING }
}
internal fun cadenzaWordProgress(time: Double, mode: CadenzaReveal, word: CadenzaWord): Double =
    if (mode == CadenzaReveal.INSTANT) if (time < word.start) 0.0 else 1.0
    else unit((time - word.start) / max(word.end - word.start, if (mode == CadenzaReveal.FAST) .045 else .01))

private fun keyframedGlow(progress: Double): Double = when {
    progress <= 0 || progress >= 1 -> 0.0
    progress < .3 -> easeOut(progress / .3)
    else -> 1 - unit((progress - .3) / .7)
}
internal fun cadenzaWordGlow(time: Double, timing: CadenzaTiming, word: CadenzaWord): Double {
    if (timing.reveal == CadenzaReveal.INSTANT) return if (time < word.start || time > timing.end) 0.0
        else keyframedGlow(unit((time - word.start) / .067))
    if (time < word.start) return 0.0
    val fast = timing.reveal == CadenzaReveal.FAST
    val duration = max(word.end - word.start, if (fast) .045 else .1)
    if (time <= word.end) {
        val progress = unit((time - word.start) / duration)
        val rise = if (fast) .14 else .18
        val fall = if (fast) .82 else .9
        return when { progress < rise -> easeOut(progress / rise); progress < fall -> 1.0
            else -> mix(1.0, if (fast) .92 else .9, (progress - fall) / if (fast) .18 else .1) }
    }
    val fade = unit((time - word.end) / if (fast) .14 else .9)
    return (if (fast) 1.0 else .9) * (1 - fade).pow(2)
}
internal fun cadenzaCharGlow(time: Double, word: CadenzaWord, index: Int, count: Int,
    timings: List<CadenzaGlyphTime> = emptyList()): Double {
    val single = max(word.end - word.start, .1) / max(count, 1)
    val timing = timings.getOrNull(index)
    val duration = timing?.let { max(it.end - it.start, .001) } ?: single
    val elapsed = time - (timing?.start ?: (word.start + single * index))
    val active = if (elapsed <= 0) 0.0 else keyframedGlow(elapsed / (duration * 6))
    return if (time <= word.end) active else active * (1 - unit((time - word.end) / .9)).pow(2)
}
internal fun cadenzaBodyMix(time: Double, timing: CadenzaTiming, word: CadenzaWord): Double {
    if (time < word.start) return 0.0
    if (timing.reveal == CadenzaReveal.INSTANT) return if (time <= timing.end) 1.0 else 0.0
    if (time <= word.end) return cadenzaWordProgress(time, timing.reveal, word)
    return 1 - unit((time - word.end) / if (timing.reveal == CadenzaReveal.FAST) .12 else .8)
}
internal fun cadenzaLineEnvelope(time: Double, line: CadenzaLyric?, timing: CadenzaTiming): CadenzaLineEnvelope {
    if (line == null || timing.hints?.transition == CadenzaTransition.NONE) return CadenzaLineEnvelope()
    val fast = timing.hints?.transition == CadenzaTransition.FAST
    val (enter, exit, hold) = timing.transition
    val passStart = max(timing.lastWordEnd, line.start) + hold
    val preEnter = if (fast) 0.0 else min(.1, enter * .35)
    val enterProgress = easeOut((time - (line.start - preEnter)) / (enter + preEnter))
    var opacity = mix(if (fast) .65 else 0.0, 1.0, enterProgress)
    var scale = mix(if (fast) .97 else .9, 1.0, enterProgress)
    var blur = mix(if (fast) 4.0 else 10.0, 0.0, enterProgress)
    val exitStart = if (fast) maxOf(line.start + enter + .01, passStart, timing.end - exit)
        else max(passStart, timing.end - exit)
    val exitProgress = easeOut((time - exitStart) / exit)
    if (exitProgress > 0) {
        opacity = if (fast) mix(opacity, 0.0, exitProgress) else opacity * (1 - exitProgress)
        scale = mix(scale, if (fast) 1.03 else 1.1, exitProgress)
        blur = max(blur, mix(0.0, if (fast) 6.0 else 20.0, exitProgress))
    }
    return CadenzaLineEnvelope(unit(opacity), scale, blur)
}
internal fun cadenzaPassedDrift(time: Double, word: CadenzaWord): Double {
    if (time <= word.end) return 0.0
    val v = unit((time - word.end) / 5)
    return if (v < .5) 2 * v * v else 1 - (-2 * v + 2).pow(2) / 2
}

/** Original three CSS shadow layers: blurScale is accepted upstream but unused. */
internal fun cadenzaShadowAlphas(intensity: Double): List<Double> {
    val value = intensity.coerceIn(0.0, 1.6)
    return if (value <= .01) emptyList() else listOf(min(.98, value), min(.92, value * .92), min(.35, value * .26))
}
