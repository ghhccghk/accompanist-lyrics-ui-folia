// Copyright (c) 2026 chthollyphile
// Partita flex/chunk geometry, fixed Folia cc6226e4. SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.LayoutBox
import kotlin.math.*

internal data class PartitaRowGeometry(val box: LayoutBox, val words: List<LayoutBox>)
internal data class PartitaGeometry(val rows: List<PartitaRowGeometry>, val height: Float)

/** VisualizerPartita serializes each clamp operand, not the resulting used size. */
internal fun partitaFontSize(base: Double, viewportWidth: Double, fit: Double): Double {
    val scale = base / (viewportWidth * .055).coerceIn(40.0, 72.0) * fit
    fun fixed(value: Double) = floor(value * 1000 + .5) / 1000
    return (fixed(5.5 * scale) * viewportWidth / 100)
        .coerceIn(fixed(2.5 * scale) * 16, fixed(4.5 * scale) * 16)
}

/** Intrinsic nowrap rows, not classic flex-wrap. All arguments/results are CSS px. */
internal fun partitaGeometry(layout: PartitaLayout, widths: List<List<Double>>, font: Double,
    spacing: List<PartitaSpacing>? = null): PartitaGeometry {
    require(widths.size == layout.rows.size)
    val lineHeight = floor(font * 1.22 * 64) / 64
    // Inline-flex combines the line boxes and padding, stored in LayoutUnits.
    fun lu(v: Double) = floor(v * 64 + .000001) / 64
    val paddingX = 8.0
    val paddingY = lu(3.2)
    val heights = layout.rows.indices.map { lineHeight + paddingY * 2 }
    val tops = layout.rows.indices.map { spacing?.get(it)?.top ?: 0.0 }
    val bottoms = layout.rows.indices.map { spacing?.get(it)?.bottom ?: layout.rows[it].marginBottom }
    val gaps = layout.rows.indices.map { i -> spacing?.get(i)?.gaps ?: List(widths[i].size) { 12.8 } }
    val rowWidths = layout.rows.indices.map { i -> widths[i].sum() + gaps[i].sumOf { lu(it) } + paddingX * 2 }
    val width = rowWidths.maxOrNull() ?: 0.0
    val contentHeight = layout.rows.indices.sumOf { heights[it] + lu(tops[it]) + lu(bottoms[it]) }
    val height = if (spacing == null) max(384.0, contentHeight) else contentHeight
    var y = (height - contentHeight) / 2
    val rows = layout.rows.mapIndexed { i, row ->
        y += lu(tops[i])
        val left = (width - rowWidths[i]) / 2
        val rtl = top.ghhccghk.accompanist.lyrics.folia.internal.text.isRtlLyric(row.words.joinToString(" ") { it.text })
        var x = paddingX
        val boxes = widths[i].indices.map { w ->
            LayoutBox((if (rtl) rowWidths[i] - x - widths[i][w] else x).toFloat(), paddingY.toFloat(),
                widths[i][w].toFloat(), lineHeight.toFloat()).also { x += widths[i][w] + lu(gaps[i][w]) }
        }
        PartitaRowGeometry(LayoutBox(left.toFloat(), y.toFloat(), rowWidths[i].toFloat(), heights[i].toFloat()), boxes)
            .also { y += heights[i] + lu(bottoms[i]) }
    }
    return PartitaGeometry(rows, height.toFloat())
}
