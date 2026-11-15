package com.mocharealm.accompanist.lyrics.folia.internal.text

import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine
import com.mocharealm.accompanist.lyrics.folia.internal.model.TimedFragment

internal expect fun graphemeBoundaries(text: String): Set<Int>
internal expect fun isRtlLyric(text: String): Boolean

internal data class TimedGrapheme(val begin: Int, val end: Int, val startUs: Long, val endUs: Long,
    val startSeconds: Double = startUs/1e6, val endSeconds: Double = endUs/1e6)
internal data class DisplayUnit(val text: String, val startUs: Long, val endUs: Long,
    /** Original syllables with UTF-16 ranges relative to the complete shaped run. */
    val fragments: List<TimedFragment> = emptyList(),
    val graphemes: List<TimedGrapheme> = emptyList(),
    val sourceGraphemes: List<TimedGrapheme> = graphemes)

/** Keep complete words/runs shaped; whitespace separates Arabic words without breaking joining. */
internal fun displayUnits(line: NormalizedLine): List<DisplayUnit> {
    if (line.fragments.isEmpty()) {
        return listOf(timedDisplayUnit(line.text, line.startUs, line.endUs, line.fragments))
    }
    val logicalWords = displayWords(line)
    // Pinned classic animates each parser word in its exact six-dot interlude.
    // Dots have no contextual joining/ligatures to protect by merging runs.
    if (line.text == "......") return logicalWords
    val boundaries = graphemeBoundaries(line.text)
    val groups = mutableListOf<DisplayUnit>()
    var begin = 0
    var start = Long.MAX_VALUE
    var end = Long.MIN_VALUE
    var offset = 0
    val pending = mutableListOf<DisplayUnit>()
    logicalWords.forEachIndexed { index, word ->
        start = minOf(start, word.startUs); end = maxOf(end, word.endUs)
        offset += word.text.length
        pending += word
        val before = line.text.getOrNull(offset - 1)
        val after = line.text.getOrNull(offset)
        val cjk = before != null && (before.code in 0x2E80..0xA4CF || before.code in 0xF900..0xFAFF)
        val safe = offset in boundaries && (after == null || before?.isWhitespace() == true || after.isWhitespace() || cjk)
        if (safe || index == logicalWords.lastIndex) {
            var local = 0
            val fragments = pending.flatMap { part ->
                val shifted = part.fragments.map { it.copy(begin = it.begin + local, end = it.end + local) }
                local += part.text.length
                shifted
            }
            groups += if (pending.size == 1) pending.single() else {
                fun timings(source: Boolean): List<TimedGrapheme> {
                    var shift = 0
                    val pieces = pending.flatMap { part ->
                        val mapped = (if(source) part.sourceGraphemes else part.graphemes).map { it.copy(begin=it.begin+shift,end=it.end+shift) }
                        shift += part.text.length
                        mapped
                    }
                    return coalesceGraphemeTimings(line.text.substring(begin,offset),pieces,start,end)
                }
                DisplayUnit(line.text.substring(begin,offset),start,end,fragments,timings(false),timings(true))
            }
            begin = offset; start = Long.MAX_VALUE; end = Long.MIN_VALUE
            pending.clear()
        }
    }
    return groups
}
