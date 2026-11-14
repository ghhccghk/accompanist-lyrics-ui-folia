package com.mocharealm.accompanist.lyrics.folia.internal.text

import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine
import com.mocharealm.accompanist.lyrics.folia.internal.model.TimedFragment

internal expect fun graphemeBoundaries(text: String): Set<Int>

internal data class TimedGrapheme(val begin: Int, val end: Int, val startUs: Long, val endUs: Long)
internal data class DisplayUnit(val text: String, val startUs: Long, val endUs: Long,
    /** Original syllables with UTF-16 ranges relative to the complete shaped run. */
    val fragments: List<TimedFragment> = emptyList(),
    val graphemes: List<TimedGrapheme> = emptyList(),
    val sourceGraphemes: List<TimedGrapheme> = graphemes)

/** Conservative shaping policy: complex scripts stay one shared run; Latin joins inside words. */
internal fun displayUnits(line: NormalizedLine): List<DisplayUnit> {
    if (line.fragments.isEmpty() || line.text.any { it.code in 0x0590..0x1CFF }) {
        return listOf(timedDisplayUnit(line.text, line.startUs, line.endUs, line.fragments))
    }
    val logicalWords = displayWords(line)
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
            // Ordinary source words joined solely for shaping retain syllable-derived timing.
            // A sticky display word already carries the upstream retimed grapheme envelope.
            groups += if (pending.size == 1) pending.single() else timedDisplayUnit(line.text.substring(begin, offset), start, end, fragments)
            begin = offset; start = Long.MAX_VALUE; end = Long.MIN_VALUE
            pending.clear()
        }
    }
    return groups
}
