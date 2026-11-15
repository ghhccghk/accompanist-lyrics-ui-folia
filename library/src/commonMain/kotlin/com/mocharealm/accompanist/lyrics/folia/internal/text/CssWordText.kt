package com.mocharealm.accompanist.lyrics.folia.internal.text

/** CSS whitespace-nowrap collapses ASCII whitespace, including at block edges.
 * Keep source boundaries so removing invisible space never retimes visible glyphs. */
internal data class CssWordText(val text: String, val boundaries: IntArray)
private fun collapsible(c: Char) = c == ' ' || c == '\t' || c == '\n' || c == '\r' || c == '\u000c'
/** Canvas text measurement replaces ASCII whitespace one-for-one, without collapsing it. */
internal fun canvasWordText(source: String) = source.map { if (collapsible(it)) ' ' else it }.joinToString("")
internal fun cssWordText(source: String): CssWordText {
    val result = StringBuilder()
    val boundaries = IntArray(source.length + 1)
    var i = 0
    while (i < source.length) {
        boundaries[i] = result.length
        if (!collapsible(source[i])) {
            result.append(source[i++]); boundaries[i] = result.length
        } else {
            var end = i + 1
            while (end < source.length && collapsible(source[end])) end++
            if (result.isNotEmpty() && end < source.length) result.append(' ')
            while (i < end) boundaries[++i] = result.length
        }
    }
    return CssWordText(result.toString(), boundaries)
}
