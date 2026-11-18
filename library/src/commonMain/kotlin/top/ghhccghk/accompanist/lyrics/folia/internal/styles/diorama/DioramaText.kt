// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 graphemeTiming.ts / dioramaSceneLayout.ts:145-205.
// Shaping-safe parser boundary adaptation and source mapping: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import kotlin.math.max

internal data class DioramaTimedText(val text: String, val start: Double, val end: Double,
    val syllables: List<DioramaTimedText> = emptyList())
internal data class DioramaLyric(val text: String, val start: Double, val end: Double,
    val words: List<DioramaTimedText> = emptyList())
internal data class DioramaClock(val charStart: Int, val charEnd: Int, val start: Double, val end: Double)
internal data class DioramaGlyph(val text: String, val start: Double, val end: Double,
    val wordIndex: Int?, val clocks: List<DioramaClock>)
internal data class DioramaUnit(val text: String, val charStart: Int, val charEnd: Int,
    val start: Double, val end: Double, val timeline: List<DioramaGlyph>)
internal data class DioramaCoreLine(val lyric: DioramaLyric, val source: ISyncedLine,
    val ref: SourceRef, val translation: String?, val phonetic: String?)

private fun graphemes(text: String) = graphemeBoundaries(text).sorted()
    .zipWithNext { a, b -> text.substring(a, b) }

private fun even(text: String, start: Double, end: Double, word: Int?): List<DioramaGlyph> {
    val edges = graphemeBoundaries(text).sorted()
    val count = edges.size - 1
    if (count <= 0) return emptyList()
    val unit = max(end - start, 0.0) / count
    return (0 until count).map { i ->
        val a = start + unit * i
        val b = if (i == count - 1) end else start + unit * (i + 1)
        DioramaGlyph(text.substring(edges[i], edges[i + 1]), a, b, word,
            listOf(DioramaClock(edges[i], edges[i + 1], a, b)))
    }
}

/** Parser boundaries do not become shaping boundaries. Reconcile only unsafe
 * EGC splits, retaining every covering source interval inside the display EGC. */
private fun wordTimeline(word: DioramaTimedText, index: Int): List<DioramaGlyph> {
    if (word.syllables.isEmpty()) return even(word.text, word.start, word.end, index)
    var offset = 0
    val clocks = word.syllables.flatMap { syllable ->
        val base = offset
        offset += syllable.text.length
        even(syllable.text, syllable.start, syllable.end, index).flatMap { glyph ->
            glyph.clocks.map { it.copy(charStart = it.charStart + base, charEnd = it.charEnd + base) }
        }
    }
    return graphemeBoundaries(word.text).sorted().zipWithNext { a, b ->
        val covering = clocks.filter { it.charEnd > a && it.charStart < b }
        DioramaGlyph(word.text.substring(a, b), covering.minOfOrNull { it.start } ?: word.start,
            covering.maxOfOrNull { it.end } ?: word.end, index, covering)
    }
}

/** Same forward matching, gap and punctuation rules as original shared timing.
 * wordIndex is retained: whitespace/alignment cannot invent a word identity. */
internal fun dioramaLineTimeline(line: DioramaLyric): List<DioramaGlyph> {
    val chars = graphemes(line.text)
    if (line.words.isEmpty()) return even(line.text, line.start, line.end, null)
    val result = MutableList<DioramaGlyph?>(chars.size) { null }
    val offsets = chars.runningFold(0) { sum, char -> sum + char.length }
    var cursor = 0
    var last = line.start
    for ((wordIndex, word) in line.words.withIndex()) {
        val gs = graphemes(word.text)
        if (gs.isEmpty()) continue
        val start = (cursor..chars.size - gs.size).firstOrNull { at ->
            gs.indices.all { chars[at + it] == gs[it] }
        } ?: cursor
        val end = minOf(start + gs.size, chars.size)
        for (i in cursor until start) result[i] = DioramaGlyph(chars[i], word.start, word.start, null,
            listOf(DioramaClock(offsets[i], offsets[i + 1], word.start, word.start)))
        val timings = wordTimeline(word, wordIndex)
        var wordOffset = 0
        for (i in 0 until end - start) {
            val timing = timings.getOrNull(i) ?: even(gs[i], word.start, word.end, wordIndex).firstOrNull() ?: continue
            val shift = offsets[start + i] - wordOffset
            result[start + i] = timing.copy(text = chars[start + i], clocks = timing.clocks.map {
                it.copy(charStart = it.charStart + shift, charEnd = it.charEnd + shift)
            })
            wordOffset += gs[i].length
            last = max(last, timing.end)
        }
        cursor = max(cursor, end)
    }
    return chars.mapIndexed { i, char -> result[i] ?: DioramaGlyph(char, last, last, null,
        listOf(DioramaClock(offsets[i], offsets[i + 1], last, last))) }
}

// Exact upstream class, including bullets, geometric shapes and half-width kana.
private val separateUnit = Regex("[\u2e80-\u9fff\u3040-\u30ff\uf900-\ufaff\uff66-\uff9f\u2022\u00b7\u25a0-\u25ff]")
/** Source's independent display-glyph class; not UTF-16 character iteration. */
internal fun dioramaSeparateGlyphUnits(units:List<DioramaUnit>):Boolean =
    units.isNotEmpty()&&units.all{it.timeline.size==1&&separateUnit.containsMatchIn(it.text)}
private fun String.blankInSource() = all { it.isWhitespace() || it == '\ufeff' }

internal fun dioramaUnits(line: DioramaLyric, timeline: List<DioramaGlyph> = dioramaLineTimeline(line)): List<DioramaUnit> {
    require(timeline.joinToString("") { it.text } == line.text) { "Diorama timeline must address the full displayed line" }
    val offsets = timeline.runningFold(0) { sum, glyph -> sum + glyph.text.length }
    val units = mutableListOf<DioramaUnit>()
    var i = 0
    while (i < timeline.size) {
        val glyph = timeline[i]
        if (glyph.text.blankInSource()) { i++; continue }
        var j = i + 1
        if (!separateUnit.containsMatchIn(glyph.text)) {
            while (j < timeline.size && timeline[j].wordIndex == glyph.wordIndex &&
                !timeline[j].text.blankInSource() && !separateUnit.containsMatchIn(timeline[j].text)) j++
        }
        units += DioramaUnit(line.text.substring(offsets[i], offsets[j]), offsets[i], offsets[j],
            glyph.start, timeline[j - 1].end, timeline.subList(i, j).toList())
        i = j
    }
    return units
}

/** Published-core bridge. Declared line times and original refs survive; adjacent
 * parser fragments join only when a boundary cuts an EGC, with inner clocks kept. */
internal fun dioramaCoreLines(document: NormalizedDocument): List<DioramaCoreLine> = document.lines.map { line ->
    val edges = graphemeBoundaries(line.text)
    val pending = mutableListOf<DioramaTimedText>()
    val words = buildList {
        line.fragments.forEachIndexed { i, f ->
            pending += DioramaTimedText(line.text.substring(f.begin, f.end), f.startUs / 1e6, f.endUs / 1e6)
            if (f.end in edges || i == line.fragments.lastIndex) {
                add(if (pending.size == 1) pending.single() else DioramaTimedText(pending.joinToString("") { it.text },
                    pending.minOf { it.start }, pending.maxOf { it.end }, pending.toList()))
                pending.clear()
            }
        }
    }
    DioramaCoreLine(DioramaLyric(line.text, line.source.start / 1000.0, line.source.end / 1000.0, words),
        line.source, line.ref, line.translation, line.phonetic)
}
