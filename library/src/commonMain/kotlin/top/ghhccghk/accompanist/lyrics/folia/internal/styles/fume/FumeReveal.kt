// Copyright (c) 2026 chthollyphile and contributors
// Adapted from fixed Folia cc6226e4: fumeReveal.ts, fumeTextStyle.ts, fumeMath.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import kotlin.math.*

internal data class FumeWord(val text: String, val start: Double, val end: Double,
    val syllables: List<FumeWord> = emptyList())
internal data class FumeLyric(val text: String, val start: Double, val end: Double,
    val words: List<FumeWord> = emptyList(), val chorus: Boolean = false,
    /** Raw last parser endpoint, distinct from the maximum endpoint of a
     * display EGC coalesced from overlapping/unsorted parser pieces. */
    val rawLastWordEnd:Double?=null) {
    // Keep source seconds through comparisons; rounding renderEnd to microseconds changes boundaries.
    val renderEnd: Double get() {
        val duration = max(end - start, 0.0)
        if (duration < .10) return max(end, start + .067)
        val fast = duration < .18
        val enter = if (fast) (duration * .45).coerceIn(.045, .06)
            else (max(duration, .12) * .34).coerceIn(.22, .42)
        val exit = if (fast) (duration * .22).coerceIn(.03, .04)
            else (max(duration, .12) * .18).coerceIn(.18, .32)
        val passed = max(rawLastWordEnd ?: words.lastOrNull()?.end ?: end, start) + if (fast) .03 else .06
        val exitStart = if (fast) maxOf(start + enter + .01, passed, end - exit)
            else max(passed, end - exit)
        return max(end, exitStart + exit)
    }
}
internal data class FumeGlyphTime(val begin: Int, val end: Int, val start: Double, val finish: Double)
internal data class FumeWordRange(val wordIndex: Int, val word: FumeWord, val start: Int, val end: Int,
    val timings: List<FumeGlyphTime>)
internal fun fumeGraphemes(text: String): List<String> =
    graphemeBoundaries(text).sorted().zipWithNext { a, b -> text.substring(a, b) }

/** Original syllables remain available, even when a parser split falls inside a shaped EGC. */
internal fun fumeWordTimings(word: FumeWord): List<FumeGlyphTime> {
    fun even(text: String, start: Double, end: Double, offset: Int): List<FumeGlyphTime> {
        val edges = graphemeBoundaries(text).sorted()
        val count = edges.size - 1
        val unit = max(end - start, 0.0) / max(count, 1)
        return (0 until count).map { i -> FumeGlyphTime(offset + edges[i], offset + edges[i + 1],
            start + unit * i, if (i == count - 1) end else start + unit * (i + 1)) }
    }
    if (word.syllables.isEmpty()) return even(word.text, word.start, word.end, 0)
    var offset = 0
    val pieces = word.syllables.flatMap {
        val times = even(it.text, it.start, it.end, offset)
        offset += it.text.length
        times
    }
    return graphemeBoundaries(word.text).sorted().zipWithNext { a, b ->
        val covering = pieces.filter { it.end > a && it.begin < b }
        FumeGlyphTime(a, b, covering.minOfOrNull { it.start } ?: word.start,
            covering.maxOfOrNull { it.finish } ?: word.end)
    }
}

/** No classic sticky/CJK grouping: Fume consumes the parser's words directly. Only unsafe EGC
 * boundaries are joined, retaining the exact source fragments as syllables. */
internal fun fumeLyric(line: NormalizedLine): FumeLyric {
    val edges = graphemeBoundaries(line.text)
    val words = mutableListOf<FumeWord>()
    val pending = mutableListOf<FumeWord>()
    line.fragments.forEachIndexed { i, fragment ->
        pending += FumeWord(line.text.substring(fragment.begin, fragment.end), fragment.startUs / 1e6, fragment.endUs / 1e6)
        if (fragment.end in edges || i == line.fragments.lastIndex) {
            words += if (pending.size == 1) pending.single() else FumeWord(
                pending.joinToString("") { it.text }, pending.minOf { it.start }, pending.maxOf { it.end }, pending.toList())
            pending.clear()
        }
    }
    return FumeLyric(line.text, line.source.start / 1000.0, line.source.end / 1000.0, words,
        rawLastWordEnd=line.fragments.lastOrNull()?.endUs?.div(1e6))
}

internal class FumeReveal(val line: FumeLyric) {
    val graphemes = fumeGraphemes(line.text)
    val ranges: List<FumeWordRange>
    init {
        var cursor = 0
        ranges = line.words.filter { it.text.isNotEmpty() }.mapIndexed { i, word ->
            val start = cursor.coerceIn(0, graphemes.size)
            var end = (start + fumeGraphemes(word.text).size).coerceIn(start, graphemes.size)
            // JS /\s/ includes FEFF, but excludes Unicode NEL.
            while (end < graphemes.size && graphemes[end].any(::fumeWhitespace)) end++
            cursor = end
            FumeWordRange(i, word, start, end, fumeWordTimings(word))
        }
    }
    private fun wordProgress(range: FumeWordRange, time: Double): Double =
        if (range.word.end <= range.word.start) if (time >= range.word.end) 1.0 else 0.0
        else ((time - range.word.start) / max(range.word.end - range.word.start, .08)).coerceIn(0.0, 1.0)

    private fun rangeCount(range: FumeWordRange, time: Double): Int {
        val length = max(range.end - range.start, 0)
        if (length == 0 || time < range.word.start) return 0
        if (range.word.syllables.isNotEmpty() && range.timings.isNotEmpty()) {
            if (time >= range.word.end) return length
            var printed = 0
            range.timings.take(length).forEachIndexed { i, timing -> if (time >= timing.start) printed = i + 1 }
            return printed.coerceIn(0, length)
        }
        val progress = wordProgress(range, time)
        if (progress >= 1) return length
        return floor(progress * length + .2).toInt().coerceIn(if (progress > 0) 1 else 0, length)
    }
    fun count(time: Double): Int {
        val size = graphemes.size
        if (size == 0 || time < line.start) return 0
        if (time >= line.end) return size // raw end, not renderEnd
        if (ranges.isEmpty()) {
            val progress = ((time - line.start) / max(line.end - line.start, .12)).coerceIn(0.0, 1.0)
            return floor(progress * size + if (progress > 0) 1 else 0).toInt().coerceIn(0, size)
        }
        var printed = 0
        for (range in ranges) {
            val partial = rangeCount(range, time)
            printed = range.start + partial
            if (partial < range.end - range.start) return printed.coerceIn(0, size)
        }
        return printed.coerceIn(0, size)
    }
    /** Focus moves continuously at word-level time even when syllable printing is discrete. */
    fun progress(time: Double): Double {
        val size = graphemes.size.toDouble()
        if (size == 0.0 || time < line.start) return 0.0
        if (time >= line.end) return size
        if (ranges.isEmpty()) return (((time - line.start) / max(line.end - line.start, .12)) * size).coerceIn(0.0, size)
        var printed = 0.0
        for (range in ranges) {
            if (time < range.word.start) return printed.coerceIn(0.0, size)
            val progress = wordProgress(range, time)
            printed = range.start + progress * max(range.end - range.start, 0)
            if (progress < 1) return printed.coerceIn(0.0, size)
        }
        return printed.coerceIn(0.0, size)
    }
}

internal fun fumeWhitespace(c: Char) = c in '\u0009'..'\u000D' || c == '\u0020' || c == '\u00A0' ||
    c == '\u1680' || c in '\u2000'..'\u200A' || c == '\u2028' || c == '\u2029' ||
    c == '\u202F' || c == '\u205F' || c == '\u3000' || c == '\uFEFF'
internal fun fumePassCutoff(line: FumeLyric, nextStart: Double?): Double =
    if (nextStart == null || !nextStart.isFinite()) line.renderEnd else min(line.renderEnd, nextStart)
internal fun fumeVisualProgress(start: Double, duration: Double, time: Double, cutoff: Double): Double {
    val end = max(start + .001, min(start + max(duration, .001), cutoff))
    return ((time - start) / max(end - start, .001)).coerceIn(0.0, 1.0)
}
internal data class FumePassedStyle(val opacity: Double, val glow: Double, val shadowBase: Double, val shadowTrail: Double)
internal fun fumePassedStyle(hero: Boolean, dimmed: Boolean) = if (dimmed)
    FumePassedStyle(if (hero) .11 else .075, 0.0, 0.0, 0.0)
    else FumePassedStyle(if (hero) .74 else .58, 1.0, .1, .16)
internal fun fumePassedDim(time: Double, passedAt: Double, duration: Double): Double {
    if (!time.isFinite() || !passedAt.isFinite() || !duration.isFinite() || duration <= 0) return 1.0
    return (max(time - passedAt, 0.0) / duration).coerceIn(0.0, 1.0).pow(3)
}
internal fun fumePassedDuration(lines: List<FumeLyric>, holdRatio: Double): Double {
    if (holdRatio >= 1) return Double.POSITIVE_INFINITY
    val valid = lines.filter { it.start.isFinite() && it.renderEnd.isFinite() && it.renderEnd >= it.start }.sortedBy { it.start }
    return (if (valid.size <= 1) 8 * holdRatio else max(valid.last().renderEnd - valid.first().start, 0.0) * holdRatio).coerceIn(2.4, 130.0)
}
internal fun fumeGlowEnvelope(progress: Double, peak: Double = .8): Double {
    val q = progress.coerceIn(0.0, 1.0)
    val p = peak.coerceIn(.05, .95)
    return if (q <= p) 1 - (1 - q / p).pow(3) else 1 - ((q - p) / (1 - p)).pow(3)
}
