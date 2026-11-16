// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 fumeBlockFrame.ts / fumeCanvasText.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import kotlin.math.*

/** Integer sRGB channels match the source's Math.round before Canvas composition. */
internal data class FumeInk(val red: Int, val green: Int, val blue: Int, val alpha: Double = 1.0) {
    fun alpha(value: Double) = copy(alpha = value.coerceIn(0.0, 1.0))
    fun mix(to: FumeInk, amount: Double, alpha: Double = 1.0): FumeInk {
        val q = amount.coerceIn(0.0, 1.0)
        fun channel(a: Int, b: Int) = floor(a + (b - a) * q + .5).toInt()
        return FumeInk(channel(red, to.red), channel(green, to.green), channel(blue, to.blue), alpha.coerceIn(0.0, 1.0))
    }
}
internal data class FumeFrameTheme(val primary: FumeInk, val accent: FumeInk,
    val intensity: String = "normal", val glow: Double = 1.0,
    val wordColors: List<Pair<String, FumeInk>> = emptyList()) {
    val activeGlow = (when (intensity) { "chaotic" -> 1.15; "calm" -> .72; else -> .92 }) * glow
    val passedGlow = (when (intensity) { "chaotic" -> .95; "calm" -> .35; else -> .62 }) * glow
    fun activeColor(word: String): FumeInk {
        val current = word.trim(::fumeWhitespace)
        if (current.isEmpty()) return accent
        // Source wordColoring.ts has a narrower CJK range than fumeMath.ts. Preserve it.
        val cjk = Regex("[\u4e00-\u9fa5\u3040-\u30ff\uac00-\ud7af]").containsMatchIn(current)
        fun normalize(value: String) = value.lowercase().replace(Regex("[^a-z0-9_]"), "")
        val match = wordColors.firstOrNull { (text, _) ->
            val target = text.trim(::fumeWhitespace)
            target.isNotEmpty() && if (cjk) target.contains(current) || current.contains(target)
            else normalize(current).let { token -> token.isNotEmpty() &&
                target.split(Regex("[\\s\u00a0\u1680\u2000-\u200a\u2028\u2029\u202f\u205f\u3000\ufeff]+"))
                    .map(::normalize).contains(token) }
        }
        return match?.second ?: accent
    }
}
internal data class FumeBlockTiming(val waiting: Double, val active: Double, val dimmed: Boolean,
    val passed: FumePassedStyle, val standard: FumePassedStyle, val baselineOffset: Double,
    val end: Double, val cutoff: Double, val revealComplete: Boolean, val cutoffReached: Boolean,
    val duration: Double, val trail: Double, val staticState: String?)

internal fun fumeBlockTiming(block: FumeArticleBlock, time: Double, nextStart: Double?, hold: Double): FumeBlockTiming {
    val duration = max(block.line.renderEnd - block.line.start, .18)
    val trail = (duration * if (block.hero) .42 else .52).coerceIn(.45, 1.45)
    val cjk = Regex("[\u4e00-\u9fff\u3040-\u30ff\uac00-\ud7af]").containsMatchIn(block.line.text)
    val cutoff = fumePassCutoff(block.line, nextStart)
    return FumeBlockTiming(if (block.hero) .06 else .035, if (block.hero) .985 else .92,
        hold < 1, fumePassedStyle(block.hero, hold < 1), fumePassedStyle(block.hero, false),
        block.lineHeight * if (cjk) .52 else .5, block.line.renderEnd, cutoff,
        time >= block.line.end, time >= cutoff, duration, trail,
        if (time < block.line.start) "waiting" else if (time >= block.line.renderEnd + trail) "passed" else null)
}
internal data class FumePaint(val fill: FumeInk, val blur: Double = 0.0,
    val shadow: FumeInk = FumeInk(0, 0, 0, 0.0))
internal data class FumeStaticLayer(val paint: FumePaint, val alpha: Double)
internal fun fumeStaticLayers(block: FumeArticleBlock, timing: FumeBlockTiming, theme: FumeFrameTheme,
    time: Double, fadeDuration: Double, overviewRestore: Double): List<FumeStaticLayer>? {
    val state = timing.staticState ?: return null
    fun paint(style: FumePassedStyle) = FumePaint(theme.primary.alpha(style.opacity),
        (2 + block.fontPx * .1) * .65 * theme.passedGlow * style.glow,
        theme.primary.alpha(style.shadowBase))
    if (state == "waiting") return listOf(FumeStaticLayer(FumePaint(theme.primary.alpha(timing.waiting)), 1.0))
    val dim = if (timing.dimmed) fumePassedDim(time, timing.end + timing.trail, fadeDuration) * (1 - overviewRestore) else 1.0
    return when {
        timing.dimmed && dim <= 0 -> listOf(FumeStaticLayer(paint(timing.standard), 1.0))
        timing.dimmed && dim < 1 -> listOf(FumeStaticLayer(paint(timing.standard), 1 - dim), FumeStaticLayer(paint(timing.passed), dim))
        else -> listOf(FumeStaticLayer(paint(timing.passed), 1.0))
    }
}
internal data class FumeStamp(val centerX: Double, val centerY: Double, val width: Double,
    val height: Double, val paint: FumePaint)
internal data class FumeGlyphFrame(val paint: FumePaint, val stamp: FumeStamp?)
internal fun fumeLineGlow(block: FumeArticleBlock, timing: FumeBlockTiming, theme: FumeFrameTheme, time: Double): FumePaint? {
    if (time < block.line.start || time > timing.cutoff) return null
    val envelope = fumeGlowEnvelope(fumeVisualProgress(block.line.start, timing.duration, time, timing.cutoff))
    val alpha = ((if (block.hero) .16 else .12) + envelope * if (block.hero) .26 else .2) * theme.glow
    val blur = ((if (block.hero) 12 else 8) + envelope * block.fontPx * if (block.hero) .7 else .52) * theme.glow
    return FumePaint(theme.accent.alpha(alpha), floor(blur + .5), theme.accent.alpha(alpha * 1.35))
}

/** Media-time sample only. Geometry is relative to the EGC's advance origin and Canvas middle
 * baseline. The renderer clips a complete shaped segment; this does not rasterize an EGC alone. */
internal fun fumeGlyphFrame(block: FumeArticleBlock, reveal: FumeReveal, timing: FumeBlockTiming,
    theme: FumeFrameTheme, time: Double, index: Int, advance: Double, showStamp: Boolean): FumeGlyphFrame {
    val printed = reveal.count(time)
    val isPrinted = timing.revealComplete || index < printed
    val frontier = printed > 0 && index == printed && printed < reveal.graphemes.size && !timing.revealComplete && !timing.cutoffReached
    var paint = FumePaint(theme.primary.alpha(if (isPrinted) timing.active else if (frontier) .82 else timing.waiting))
    val range = reveal.ranges.lastOrNull { index >= it.start && index < it.end } ?: return FumeGlyphFrame(paint, null)
    val wordDuration = max(range.word.end - range.word.start, .08)
    val wordProgress = ((time - range.word.start) / wordDuration).coerceIn(0.0, 1.0)
    val count = max(range.end - range.start, 1)
    val local = index - range.start
    val glyphTime = if (range.word.syllables.isNotEmpty()) range.timings.getOrNull(min(local, max(range.timings.size - 1, 0))) else null
    val start = glyphTime?.start ?: (range.word.start + local.toDouble() / count * wordDuration)
    val end = glyphTime?.finish ?: (range.word.start + (local + 1.0) / count * wordDuration)
    val duration = max(end - start, .001)
    val progress = (if (glyphTime != null) (time - start) / duration + .16 else wordProgress * count - local + .16).coerceIn(0.0, 1.0)
    val eased = 1 - (1 - progress).pow(3)
    val active = theme.activeColor(range.word.text)
    val trailStart = start + duration * .18
    val trail = fumeVisualProgress(trailStart, timing.trail, time, timing.cutoff).pow(1.35)
    paint = when {
        timing.cutoffReached || time > trailStart -> {
            // The source checks cutoff *before* a future word's start.
            if (!timing.cutoffReached && time < range.word.start) FumePaint(theme.primary.alpha(timing.waiting))
            else FumePaint(active.mix(theme.primary, .18 + trail * .82, timing.active + (timing.standard.opacity - timing.active) * trail),
                (2 + block.fontPx * .1) * (1 - trail * .35) * theme.passedGlow * timing.standard.glow,
                active.mix(theme.primary, .55 + trail * .45, timing.standard.shadowBase + (1 - trail) * timing.standard.shadowTrail))
        }
        time < range.word.start -> FumePaint(theme.primary.alpha(timing.waiting))
        else -> FumePaint(theme.primary.mix(active, .22 + eased * .78, timing.waiting + (timing.active - timing.waiting) * eased),
            (4 + block.fontPx * .22) * eased * theme.activeGlow, active.alpha(.4 + eased * .44))
    }
    paint = paint.copy(blur = if (paint.blur.isFinite()) max(0.0, floor(paint.blur + .5)) else 0.0)
    var stamp: FumeStamp? = null
    if (showStamp && reveal.graphemes[index].any { !fumeWhitespace(it) }) {
        val lead = min(max(wordDuration / count, .04) * .86, timing.duration * .16).coerceIn(.055, if (block.hero) .2 else .16)
        val release = lead * .42
        val windowStart = trailStart - lead
        val windowEnd = trailStart + release
        val p = fumeVisualProgress(windowStart, windowEnd - windowStart, time, timing.cutoff)
        if (p > 0 && p < 1) {
            val phase = fumeVisualProgress(trailStart, max(windowEnd - trailStart, .001), time, timing.cutoff)
            val dropping = phase <= 0
            val drop = if (dropping) 1 - (1 - fumeVisualProgress(windowStart, max(trailStart - windowStart, .001), time, timing.cutoff)).pow(3) else 1.0
            val fade = if (phase < .5) 4 * phase.pow(3) else 1 - (-2 * phase + 2).pow(3) / 2
            val pulse = if (dropping) .18 + .82 * drop.pow(.78) else (1 - fade).pow(1.2)
            val cjk = Regex("[\u4e00-\u9fff\u3040-\u30ff\uac00-\ud7af]").containsMatchIn(reveal.graphemes[index])
            val width = max(advance * .88, block.fontPx * if (cjk) .56 else .38) + block.fontPx * if (block.hero) .18 else .12
            val height = block.fontPx * if (block.hero) .72 else .62
            val y = -block.fontPx * .38 - block.lineHeight * (if (block.hero) .24 else .2) * (1 - drop)
            stamp = FumeStamp(advance * .5, y, width, height,
                FumePaint(active.alpha(pulse * if (block.hero) .82 else .72),
                    (8 + block.fontPx * .24) * pulse * theme.activeGlow, active.alpha(.56 * pulse)))
        }
    }
    return FumeGlyphFrame(paint, stamp)
}
