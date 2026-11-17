// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 utils/lyrics/graphemeTiming.ts and cappella/cappellaReveal.ts.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import kotlin.math.max

internal data class CappellaSyllable(val text: String, val start: Double, val end: Double)
internal data class CappellaWord(val text: String, val start: Double, val end: Double,
    val syllables: List<CappellaSyllable> = emptyList())
internal data class CappellaLine(val text: String, val start: Double, val end: Double,
    val words: List<CappellaWord>, val agent: String? = null)
internal data class CappellaGlyph(val text: String, val start: Double, val end: Double)

internal fun cappellaGraphemes(text: String): List<String> =
    graphemeBoundaries(text).sorted().zipWithNext { a, b -> text.substring(a, b) }

private fun even(text: String, start: Double, end: Double): List<CappellaGlyph> {
    val glyphs = cappellaGraphemes(text)
    val unit = max(end - start, 0.0) / max(glyphs.size, 1)
    return glyphs.mapIndexed { i, g -> CappellaGlyph(g, start + unit * i,
        if (i == glyphs.lastIndex) end else start + unit * (i + 1)) }
}

/** Raw parser syllables remain timed individually, as in the source. Display clusters
 * are reconciled below; a syllable boundary never forces a grapheme to be drawn alone. */
private fun wordTimings(word: CappellaWord): List<CappellaGlyph> =
    if (word.syllables.isEmpty()) even(word.text, word.start, word.end)
    else word.syllables.flatMap { even(it.text, it.start, it.end) }

/** Source matching includes gaps with the next word and trailing punctuation with
 * the latest resolved END. Do not replace this with a whole-line linear timer. */
internal fun cappellaLineTimeline(line: CappellaLine): List<CappellaGlyph> {
    val chars = cappellaGraphemes(line.text)
    if (chars.isEmpty()) return emptyList()
    if (line.words.isEmpty()) return even(line.text, line.start, line.end)
    val result = MutableList<CappellaGlyph?>(chars.size) { null }
    var cursor = 0
    var last = line.start
    for (word in line.words) {
        val gs = cappellaGraphemes(word.text)
        if (gs.isEmpty()) continue
        val matched = (cursor..chars.size - gs.size).firstOrNull { at ->
            gs.indices.all { chars[at + it] == gs[it] }
        }
        val start = matched ?: cursor
        val end = minOf(start + gs.size, chars.size)
        for (i in cursor until start) result[i] = CappellaGlyph(chars[i], word.start, word.start)
        val raw = wordTimings(word)
        // Source syllable flattening can split an EGC (e + combining accent,
        // ZWJ emoji). Union the covering timing intervals only in that case.
        var offset = 0
        val edges = graphemeBoundaries(word.text)
        val safe = raw.all { offset += it.text.length; offset in edges }
        val timings = if (safe) raw else {
            var pos = 0
            val spans = raw.map { val a = pos; pos += it.text.length; Triple(a, pos, it) }
            edges.sorted().zipWithNext { a, b ->
                val cover = spans.filter { it.second > a && it.first < b }.map { it.third }
                CappellaGlyph(word.text.substring(a, b), cover.minOfOrNull { it.start } ?: word.start,
                    cover.maxOfOrNull { it.end } ?: word.end)
            }
        }
        for (i in 0 until end - start) {
            val timing = timings.getOrNull(i) ?: even(gs[i], word.start, word.end).firstOrNull() ?: continue
            result[start + i] = timing.copy(text = chars[start + i])
            last = max(last, timing.end)
        }
        cursor = max(cursor, end)
    }
    return chars.mapIndexed { i, g -> result[i] ?: CappellaGlyph(g, last, last) }
}

internal class CappellaRevealPlan(line: CappellaLine) {
    val timeline = cappellaLineTimeline(line)
    val characters = timeline.map { it.text }
    val revealTimes = timeline.map { it.start }
    val fadeMillis = timeline.map { max((it.end - it.start) * 1000, 40.0) }
    val bubbleTargetTimes = revealTimes.map { it - .2 }
    val timestampReady = if (timeline.isEmpty()) line.end else
        timeline.indices.fold(line.start) { latest, i ->
            if (revealTimes[i].isFinite()) max(latest, revealTimes[i] + fadeMillis[i] / 1000) else latest
        }

    fun visibleCount(time: Double): Int = countAt(revealTimes, time)
    fun bubbleCount(time: Double): Int = countAt(bubbleTargetTimes, time)

    /** Normalized media-time progress for a renderer-owned CSS ease-out fade; pause/seek cannot accumulate wall time. */
    fun progress(index: Int, time: Double): Double =
        ((time - revealTimes[index]) / (fadeMillis[index] / 1000)).coerceIn(0.0, 1.0)

    companion object {
        // Intentionally retains original binary-search semantics, including malformed
        // non-monotonic parser times. Validation does not silently sort the text.
        internal fun countAt(times: List<Double>, time: Double): Int {
            var low = 0; var high = times.size
            while (low < high) {
                val mid = (low + high) / 2
                if (times[mid] <= time) low = mid + 1 else high = mid
            }
            return low
        }
    }
}
