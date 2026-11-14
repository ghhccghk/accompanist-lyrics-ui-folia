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
    return graphemeBoundaries(text).sorted().zipWithNext { begin, end ->
        val covering = pieces.filter { it.end > begin && it.begin < end }
        TimedGrapheme(begin, end, covering.minOfOrNull { it.startUs } ?: startUs,
            covering.maxOfOrNull { it.endUs } ?: endUs)
    }
}

internal fun timedDisplayUnit(text: String, startUs: Long, endUs: Long, fragments: List<TimedFragment>): DisplayUnit =
    DisplayUnit(text, startUs, endUs, fragments, sourceGraphemeTimings(text, fragments, startUs, endUs))

private val suffix = Regex("^(s|t|m|d|ll|re|ve|em)$", RegexOption.IGNORE_CASE)
private val directSuffix = Regex("^['’](s|t|m|d|ll|re|ve|em)$", RegexOption.IGNORE_CASE)
internal val trailingPunctuation = setOf(',', '.', ';', ':', '!', '?', '，', '。', '！', '？', '、', '：', '；',
    '）', '】', '》', '」', '』', '〉', '〕', '］', ')', '}', ']', '"', '\'', '’', '”')
private fun attachable(text: String) = text.trimEnd().lastOrNull()?.isLetterOrDigit() == true

/** Sticky contraction/punctuation stage. Upstream display words evenly retime sticky groups;
 * original syllables and their source timeline remain separate and inspectable. */
internal fun attachStickyWords(words: List<DisplayUnit>): List<DisplayUnit> {
    val output = mutableListOf<DisplayUnit>()
    fun append(previous: DisplayUnit, current: DisplayUnit): DisplayUnit {
        val text = previous.text + current.text
        val fragments = previous.fragments + current.fragments.map { it.copy(
            begin = it.begin + previous.text.length, end = it.end + previous.text.length) }
        return DisplayUnit(text, previous.startUs, current.endUs, fragments,
            evenGraphemeTimings(text, previous.startUs, current.endUs),
            sourceGraphemeTimings(text, fragments, previous.startUs, current.endUs))
    }
    var i = 0
    while (i < words.size) {
        val current = words[i]
        val previous = output.lastOrNull()
        val core = current.text.trim()
        val next = words.getOrNull(i + 1)
        if (previous != null && core in listOf("'", "’") && next != null && attachable(previous.text) && suffix.matches(next.text.trim())) {
            output[output.lastIndex] = append(append(previous, current), next); i += 2
        } else if (previous != null && ((directSuffix.matches(core) && attachable(previous.text)) ||
            (suffix.matches(core) && previous.text.trimEnd().lastOrNull() in listOf('\'', '’')) ||
            (core.isNotEmpty() && core.all { it in trailingPunctuation } && attachable(previous.text)))) {
            output[output.lastIndex] = append(previous, current); i++
        } else { output += current; i++ }
    }
    return output
}
