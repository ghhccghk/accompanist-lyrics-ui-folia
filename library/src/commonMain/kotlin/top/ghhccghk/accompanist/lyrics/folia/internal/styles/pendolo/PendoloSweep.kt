// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 PendoloActiveLyricSweep.tsx and utils/lyrics/graphemeTiming.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Native shaping adaptation: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import kotlin.math.*

internal data class PendoloTimedText(val text: String, val start: Double, val end: Double,
    val syllables: List<PendoloTimedText> = emptyList())
internal data class PendoloLyric(val text: String, val start: Double, val end: Double,
    val words: List<PendoloTimedText> = emptyList())
internal data class PendoloGlyphTime(val text: String, val start: Double, val end: Double)

internal fun pendoloGraphemes(text: String): List<String> =
    graphemeBoundaries(text).sorted().zipWithNext { a, b -> text.substring(a, b) }

private fun even(text: String, start: Double, end: Double): List<PendoloGlyphTime> {
    val chars = pendoloGraphemes(text)
    val unit = max(end - start, 0.0) / max(chars.size, 1)
    return chars.mapIndexed { i, c -> PendoloGlyphTime(c, start + unit * i,
        if (i == chars.lastIndex) end else start + unit * (i + 1)) }
}

/** Original forward word matching, next-word gap timing and last-resolved-end
 * punctuation timing. Parser pieces never become independently shaped glyphs. */
internal fun pendoloLineTimeline(line: PendoloLyric): List<PendoloGlyphTime> {
    val chars = pendoloGraphemes(line.text)
    if (line.words.isEmpty()) return even(line.text, line.start, line.end)
    val result = MutableList<PendoloGlyphTime?>(chars.size) { null }
    var cursor = 0
    var last = line.start
    for (word in line.words) {
        val gs = pendoloGraphemes(word.text)
        if (gs.isEmpty()) continue
        val start = (cursor..chars.size - gs.size).firstOrNull { at -> gs.indices.all { chars[at + it] == gs[it] } } ?: cursor
        val end = min(start + gs.size, chars.size)
        for (i in cursor until start) result[i] = PendoloGlyphTime(chars[i], word.start, word.start)
        val pieces = if (word.syllables.isEmpty()) even(word.text, word.start, word.end)
            else word.syllables.flatMap { even(it.text, it.start, it.end) }
        // The original flattens parser syllables. Only when that cuts an EGC,
        // reconcile the display cluster with its covering source timing span.
        val edges = graphemeBoundaries(word.text)
        var offset = 0
        val safe = pieces.all { offset += it.text.length; offset in edges }
        val timings = if (safe) pieces else {
            var position = 0
            val spans = pieces.map { val a = position; position += it.text.length; Triple(a, position, it) }
            edges.sorted().zipWithNext { a, b ->
                val cover = spans.filter { it.second > a && it.first < b }.map { it.third }
                PendoloGlyphTime(word.text.substring(a, b), cover.minOfOrNull { it.start } ?: word.start,
                    cover.maxOfOrNull { it.end } ?: word.end)
            }
        }
        for (i in 0 until end - start) {
            val t = timings.getOrNull(i) ?: even(gs[i], word.start, word.end).firstOrNull() ?: continue
            result[start + i] = t.copy(text = chars[start + i])
            last = max(last, t.end)
        }
        cursor = max(cursor, end)
    }
    return chars.mapIndexed { i, c -> result[i] ?: PendoloGlyphTime(c, last, last) }
}

internal data class PendoloSweepFrame(val width: Double, val opacity: Double, val edge: Double) {
    fun maskAlpha(x: Double): Double {
        val solid = max(width - edge, 0.0)
        return when {
            x < solid -> 1.0
            x < width -> 1 - .16 * (x - solid) / (width - solid)
            x < width + edge -> .84 * (1 - (x - width) / edge)
            else -> 0.0
        }
    }
}

/** Preserve non-monotone prefix advances (e.g. Arabic joining). These offsets
 * measure full shaped prefixes, never sums of isolated UTF-16 characters. */
internal fun pendoloSweepFrame(timings: List<PendoloGlyphTime>, offsets: List<Double>,
    rowWidth: Double, lineEnd: Double, fontPx: Double, time: Double): PendoloSweepFrame {
    val full = offsets.lastOrNull() ?: rowWidth
    val fill = when {
        timings.isEmpty() -> if (time >= lineEnd) full else 0.0
        time <= timings.first().start -> 0.0
        else -> {
            var width = full
            for ((i, t) in timings.withIndex()) {
                val a = offsets.getOrNull(i) ?: 0.0
                val b = offsets.getOrNull(i + 1) ?: a
                if (time < t.start) { width = a; break }
                if (time <= t.end) { width = a + (b - a) * ((time - t.start) / max(.001, t.end - t.start)); break }
            }
            width
        }
    }
    return PendoloSweepFrame(fill, if (time < (timings.firstOrNull()?.start ?: lineEnd)) 0.0 else 1.0,
        min(max(fontPx * .42, 8.0), 16.0))
}
