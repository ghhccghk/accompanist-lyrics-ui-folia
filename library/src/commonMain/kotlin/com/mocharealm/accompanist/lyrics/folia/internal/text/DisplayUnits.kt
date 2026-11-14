package com.mocharealm.accompanist.lyrics.folia.internal.text

import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine

internal expect fun graphemeBoundaries(text: String): Set<Int>

internal data class DisplayUnit(val text: String, val startUs: Long, val endUs: Long)

/** Conservative shaping policy: complex scripts stay one shared run; Latin joins inside words. */
internal fun displayUnits(line: NormalizedLine): List<DisplayUnit> {
    if (line.fragments.isEmpty() || line.text.any { it.code in 0x0590..0x1CFF }) {
        return listOf(DisplayUnit(line.text, line.startUs, line.endUs))
    }
    val boundaries = graphemeBoundaries(line.text)
    val groups = mutableListOf<DisplayUnit>()
    var begin = 0
    var start = Long.MAX_VALUE
    var end = Long.MIN_VALUE
    line.fragments.forEachIndexed { index, fragment ->
        start = minOf(start, fragment.startUs); end = maxOf(end, fragment.endUs)
        val offset = fragment.end
        val before = line.text.getOrNull(offset - 1)
        val after = line.text.getOrNull(offset)
        val cjk = before != null && (before.code in 0x2E80..0xA4CF || before.code in 0xF900..0xFAFF)
        val safe = offset in boundaries && (after == null || before?.isWhitespace() == true || after.isWhitespace() || cjk)
        if (safe || index == line.fragments.lastIndex) {
            groups += DisplayUnit(line.text.substring(begin, offset), start, end)
            begin = offset; start = Long.MAX_VALUE; end = Long.MIN_VALUE
        }
    }
    return groups
}
