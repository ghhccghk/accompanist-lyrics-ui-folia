// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 pendoloTextLayout.ts / monetLyricsModel.ts:measureMonetGraphemeOffsets.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md for platform differences.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.cappellaCanvasAdvance
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

internal data class PendoloTextRow(val text: String, val width: Double, val graphemeStart: Int,
    val graphemeEnd: Int, val offsets: List<Double>, val paragraph: TextLayoutResult)
internal data class PendoloPreparedText(val rows: List<PendoloTextRow>, val lineHeight: Double,
    val height: Double, val metrics: NaturalFontMetrics)

/** Independent platform wrapping and complete-run shaping. The shared native
 * Canvas-advance adapter was first calibrated for Cappella; no source geometry
 * or browser-generated expected row is read here. */
internal fun preparePendoloText(text: String, fontPx: Double, width: Double, measurer: TextMeasurer,
    base: TextStyle, density: Density, resolver: FontFamily.Resolver,
    lineHeight: Double = floor(fontPx * 1.2 + .5)): PendoloPreparedText {
    val style = classicBodyRasterStyle(base.copy(fontSize = fontPx.sp, lineHeight = TextUnit.Unspecified,
        textAlign = TextAlign.Left, textMotion = TextMotion.Animated,
        textDirection = base.textDirection.takeUnless { it == TextDirection.Unspecified } ?: TextDirection.Ltr))
    val safe = text.ifEmpty { " " }
    fun advance(s: String): Double {
        if (s.isEmpty()) return 0.0
        // pretext measures complete word segments and retains pre-wrap spaces.
        val segments = mutableListOf<String>()
        for (part in segmentWords(s)) {
            if (part.text == "-" && segments.isNotEmpty()) segments[segments.lastIndex] += part.text else segments += part.text
        }
        return segments.sumOf { value -> cappellaCanvasAdvance(value, style, resolver, density) ?:
            (measurer.measure(value, style, softWrap = false).getPathForRange(0, value.length).getBounds().width / density.density.toDouble()) }
    }
    val layout = measurer.measure(safe, style, constraints = Constraints(maxWidth =
        ceil(max(1.0, width) * density.density).toInt().coerceAtLeast(1)))
    var cursor = 0
    val rows = (0 until layout.lineCount).map { i ->
        val a = layout.getLineStart(i)
        val b = layout.getLineEnd(i).let { e -> if (e > a && safe[e - 1] == '\n') e - 1 else e }
        check(a in graphemeBoundaries(safe) && b in graphemeBoundaries(safe)) { "Pendolo platform wrap cuts a grapheme" }
        val row = safe.substring(a, b)
        val chars = pendoloGraphemes(row)
        val start = cursor
        cursor += chars.size
        val edges = graphemeBoundaries(row).sorted()
        // Original prefix measurement uses normal whitespace, so a trailing
        // ASCII space has zero *additional* width; row layout uses pre-wrap.
        val offsets = edges.map { end -> advance(row.substring(0, end).trimEnd(' ')) }
        PendoloTextRow(row, advance(row), start, cursor, offsets,
            measurer.measure(row.ifEmpty { " " }, style, softWrap = false))
    }
    return PendoloPreparedText(rows, lineHeight, max(rows.size, 1) * lineHeight, naturalFontMetrics(style, resolver, density))
}
