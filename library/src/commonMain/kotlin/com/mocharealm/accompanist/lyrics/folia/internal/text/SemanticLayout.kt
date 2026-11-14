// Folia cjkSemanticLayout.ts / wordSegmentation.ts, fixed-source ledger in docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.text

import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine

internal data class WordSegment(val text: String, val wordLike: Boolean)
internal expect fun segmentWords(text: String): List<WordSegment>
internal data class LayoutUnit(val words: List<DisplayUnit>, val semantic: Boolean) {
    val text: String get() = words.joinToString("") { it.text }
}
private val cjk = Regex("[\u4e00-\u9fa5\u3040-\u30ff\uac00-\ud7af]")
private fun key(text: String) = text.filterNot { it.isWhitespace() }

/** Exact alignment only. ICU dictionary/Unicode versions may differ from browser Intl;
 * failed alignment falls back to parser words, just as the pinned upstream does. */
internal fun semanticLayoutUnits(line: NormalizedLine): List<LayoutUnit> {
    val words = line.fragments.map { fragment ->
        timedDisplayUnit(line.text.substring(fragment.begin, fragment.end), fragment.startUs, fragment.endUs,
            listOf(fragment.copy(begin = 0, end = fragment.end - fragment.begin)))
    }
    val fallback = words.map { LayoutUnit(listOf(it), false) }
    if (!cjk.containsMatchIn(line.text) || words.isEmpty()) return fallback
    val units = mutableListOf<LayoutUnit>()
    var cursor = 0
    fun whitespace() {
        while (units.isNotEmpty() && cursor < words.size && key(words[cursor].text).isEmpty()) {
            units[units.lastIndex] = units.last().copy(words = units.last().words + words[cursor++])
        }
    }
    for (segment in segmentWords(line.text)) {
        val target = key(segment.text)
        if (target.isEmpty()) continue
        whitespace()
        val start = cursor
        var collected = ""
        while (cursor < words.size && collected.length < target.length) {
            collected += key(words[cursor++].text)
            if (!target.startsWith(collected)) return fallback
        }
        if (collected != target || cursor == start) return fallback
        val group = words.subList(start, cursor).toList()
        if (!segment.wordLike && units.isNotEmpty())
            units[units.lastIndex] = units.last().copy(words = units.last().words + group)
        else units += LayoutUnit(group, segment.wordLike && cjk.containsMatchIn(group.joinToString("") { it.text }) && group.count { key(it.text).isNotEmpty() } > 1)
    }
    whitespace()
    return if (cursor == words.size && units.isNotEmpty()) units else fallback
}

/** Semantic CJK groups flatten to their original words in classic, not one animated blob. */
internal fun displayWords(line: NormalizedLine): List<DisplayUnit> = buildList {
    val pending = mutableListOf<DisplayUnit>()
    fun flush() { addAll(attachStickyWords(pending)); pending.clear() }
    semanticLayoutUnits(line).forEach { unit ->
        if (unit.semantic) { flush(); addAll(unit.words) }
        else pending += if (unit.words.size == 1) unit.words.single() else {
            val merged = attachStickyWords(unit.words)
            // Non-word segment attachment in the semantic stage preserves its source members.
            if (merged.size == 1) merged.single() else {
                val text = unit.text
                val first = unit.words.first(); val last = unit.words.last()
                timedDisplayUnit(text, first.startUs, last.endUs, unit.words.flatMapIndexed { i, w ->
                    val shift = unit.words.take(i).sumOf { it.text.length }
                    w.fragments.map { it.copy(begin = it.begin + shift, end = it.end + shift) }
                })
            }
        }
    }
    flush()
}
