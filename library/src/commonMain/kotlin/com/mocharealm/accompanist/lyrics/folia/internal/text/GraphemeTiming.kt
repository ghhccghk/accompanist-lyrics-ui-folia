// Adapted from Folia graphemeTiming.ts / cjkSemanticLayout.ts (fixed source in docs/upstream.md).
// SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.text

import com.mocharealm.accompanist.lyrics.folia.internal.model.TimedFragment
import kotlin.math.roundToLong

/** UTF-16 ranges address ONE shaped layout; timing never requires character-by-character shaping. */
internal fun evenGraphemeTimings(text: String, startUs: Long, endUs: Long): List<TimedGrapheme> {
    val edges = graphemeBoundaries(text).sorted()
    val count = edges.size - 1
    if (count <= 0) return emptyList()
    val duration = (endUs - startUs).coerceAtLeast(0)
    return (0 until count).map { i -> TimedGrapheme(edges[i], edges[i + 1],
        startUs + (duration.toDouble() * i / count).roundToLong(),
        if (i == count - 1) endUs else startUs + (duration.toDouble() * (i + 1) / count).roundToLong()) }
}

/** Subdivide each source syllable before grouping. A grapheme spanning source boundaries
 * keeps the covering time span; combining marks and ZWJ sequences remain indivisible. */
internal fun sourceGraphemeTimings(text: String, fragments: List<TimedFragment>, startUs: Long, endUs: Long): List<TimedGrapheme> {
    if (fragments.isEmpty()) return evenGraphemeTimings(text, startUs, endUs)
    val pieces = fragments.flatMap { fragment ->
        evenGraphemeTimings(text.substring(fragment.begin, fragment.end), fragment.startUs, fragment.endUs)
            .map { it.copy(begin = it.begin + fragment.begin, end = it.end + fragment.begin) }
    }
    return coalesceGraphemeTimings(text, pieces, startUs, endUs)
}

/** A shaped grapheme can cross parser boundaries; preserve both timelines when joining runs. */
internal fun coalesceGraphemeTimings(text: String, pieces: List<TimedGrapheme>, startUs: Long, endUs: Long): List<TimedGrapheme> =
    graphemeBoundaries(text).sorted().zipWithNext { begin, end ->
        val covering = pieces.filter { it.end > begin && it.begin < end }
        TimedGrapheme(begin, end, covering.minOfOrNull { it.startUs } ?: startUs,
            covering.maxOfOrNull { it.endUs } ?: endUs)
    }

internal fun timedDisplayUnit(text: String, startUs: Long, endUs: Long, fragments: List<TimedFragment>): DisplayUnit =
    DisplayUnit(text, startUs, endUs, fragments, sourceGraphemeTimings(text, fragments, startUs, endUs))

internal val trailingPunctuation = setOf(',', '.', ';', ':', '!', '?', '，', '。', '！', '？', '、', '：', '；',
    '）', '】', '》', '」', '』', '〉', '〕', '］', ')', '}', ']', '"', '\'', '’', '”')
