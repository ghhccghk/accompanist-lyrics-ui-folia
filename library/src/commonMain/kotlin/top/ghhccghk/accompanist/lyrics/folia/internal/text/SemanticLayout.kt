// Folia cjkSemanticLayout.ts / wordSegmentation.ts, fixed-source ledger in docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.text

import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine

internal data class WordSegment(val text: String, val wordLike: Boolean)
internal expect fun segmentWords(text: String): List<WordSegment>
internal data class LayoutUnit(val words: List<DisplayUnit>, val semantic: Boolean, val sticky: Boolean = false) {
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

private val suffix = Regex("^(s|t|m|d|ll|re|ve|em)$",RegexOption.IGNORE_CASE)
private val direct = Regex("^['’](s|t|m|d|ll|re|ve|em)$",RegexOption.IGNORE_CASE)
private val inline = Regex("[\\p{L}\\p{N}]+['’](s|t|m|d|ll|re|ve|em)",RegexOption.IGNORE_CASE)
private fun punctuation(text:String) = text.trim().let {it.isNotEmpty() && it.all {c->c in trailingPunctuation}}
private val trailingWordCharacter = Regex("[\\p{L}\\p{N}]$")
private fun attachable(text:String) = trailingWordCharacter.containsMatchIn(text.trimEnd())

/** Sticky is a layout-unit property; semantic units must not be flattened before attachment. */
internal fun stickyLayoutUnits(units: List<LayoutUnit>): List<LayoutUnit> {
    val merged=mutableListOf<LayoutUnit>()
    fun append(unit:LayoutUnit) {merged[merged.lastIndex]=merged.last().copy(words=merged.last().words+unit.words,sticky=true)}
    var i=0
    while(i<units.size) {
        val current=units[i]; val previous=merged.lastOrNull(); val core=current.text.trim(); val next=units.getOrNull(i+1)
        when {
            previous==null -> merged.add(current)
            core in listOf("'","’") && next!=null && attachable(previous.text) && suffix.matches(next.text.trim()) -> {append(current);append(next);i++}
            direct.matches(core) && attachable(previous.text) -> append(current)
            suffix.matches(core) && previous.text.trimEnd().lastOrNull() in listOf('\'','’') -> append(current)
            punctuation(core) && attachable(previous.text) -> append(current)
            else -> merged.add(current)
        }
        i++
    }
    return merged.map {unit -> unit.copy(sticky=unit.sticky || (unit.words.size>1 &&
        (punctuation(unit.words.last().text) || (!unit.semantic && inline.containsMatchIn(unit.text))))) }
}

/** Exactly the upstream classic conversion: only sticky nonsemantic units collapse. */
internal fun displayWords(line: NormalizedLine): List<DisplayUnit> = displayWordsFromLayoutUnits(stickyLayoutUnits(semanticLayoutUnits(line)))

internal fun displayWordsFromLayoutUnits(units: List<LayoutUnit>): List<DisplayUnit> = units.flatMap {unit ->
    if(!unit.sticky || unit.semantic) unit.words else {
        var shift=0
        val fragments=unit.words.flatMap {part ->
            val mapped=part.fragments.map {it.copy(begin=it.begin+shift,end=it.end+shift)}
            shift+=part.text.length
            mapped
        }
        val first=unit.words.first();val last=unit.words.last()
        listOf(DisplayUnit(unit.text,first.startUs,last.endUs,fragments,
            evenGraphemeTimings(unit.text,first.startUs,last.endUs),
            sourceGraphemeTimings(unit.text,fragments,first.startUs,last.endUs)))
    }
}
