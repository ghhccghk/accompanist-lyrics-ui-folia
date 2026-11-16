// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 fumeCanvasText.ts:createStaticBlockSnapshot.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.naturalFontMetrics
import top.ghhccghk.accompanist.lyrics.folia.internal.text.paintedNaturalBaseline
import kotlin.math.*

internal data class FumeStaticRow(val text: String, val startUtf16: Int, val endUtf16: Int,
    val left: Double, val top: Double, val width: Float)
internal data class FumeStaticSnapshot(val image: ImageBitmap, val padding: Float, val rasterScale: Float,
    val rows: List<FumeStaticRow>)
/** Canvas middle: normalize typographic ascent to an em and quantize ascent and total em
 * separately on the 1/64 grid before taking the midpoint. The half can lie on 1/128.
 * Values are physical pixels. Native hhea metrics are only a fallback when OS/2 differs. */
internal fun fumeMiddleBaseline(ascent: Float, descent: Float, fontPx: Float): Float =
    floor(fontPx * ascent / (ascent + descent) * 64 + .5f) / 64 - floor(fontPx * 64 + .5f) / 128

/** Text-only, fully printed static source layer. No passed opacity/glow, live stamps or camera.
 * Each row remains a complete shaped line; no UTF-16 character or syllable rasterization. */
internal fun prepareFumeStaticText(block: FumeArticleBlock, measure: FumeTextMeasure,
    resolver: FontFamily.Resolver, density: Density, ink: Color): FumeStaticSnapshot {
    val padding = ceil(max(block.fontPx * .32, 4.0)).toFloat()
    val scale = density.density
    val image = ImageBitmap(ceil((block.width + padding * 2) * scale).toInt().coerceAtLeast(1),
        ceil((block.height + padding * 2) * scale).toInt().coerceAtLeast(1))
    val wrapped = if (measure.countLines(block.line.text, block.hero, block.fontPx, block.innerWidth) <= 1)
        measure.naturalLayout(block.line.text, block.hero, block.fontPx)
        else measure.layout(block.line.text, block.hero, block.fontPx, block.innerWidth)
    val style = measure.style(block.fontPx, block.hero)
    val metrics = naturalFontMetrics(style, resolver, density)
    val fontPixels = with(density) { style.fontSize.toPx() }
    val middle = fumeMiddleBaseline(metrics.ascent, metrics.descent, fontPixels)
    val cjk = Regex("[\u4e00-\u9fff\u3040-\u30ff\uac00-\ud7af]").containsMatchIn(block.line.text)
    val baselineOffset = block.lineHeight * if (cjk) .52 else .5
    val rows = mutableListOf<FumeStaticRow>()
    CanvasDrawScope().draw(density, wrapped.layoutInput.layoutDirection, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
        for (row in 0 until wrapped.lineCount) {
            val start = wrapped.getLineStart(row)
            val end = wrapped.getLineEnd(row).let { if (it > start && block.line.text[it - 1] == '\n') it - 1 else it }
            val layout = measure.naturalLayout(block.line.text.substring(start, end), block.hero, block.fontPx)
            // Unlike getLineRight, the selection path retains the trailing spaces
            // that pre-wrap paints even though they hang outside the fit width.
            val width = layout.getPathForRange(0, end - start).getBounds().width / scale
            val left = if (block.hero) max((block.width - width) * .08, 0.0) else 0.0
            rows += FumeStaticRow(block.line.text.substring(start, end), start, end, left, row * block.lineHeight, width)
            val baseline = ((padding + row * block.lineHeight + baselineOffset) * scale).toFloat() + middle
            drawText(layout, ink, Offset(((padding + left) * scale).toFloat(), baseline - paintedNaturalBaseline(layout, metrics)), shadow = Shadow.None)
        }
    }
    return FumeStaticSnapshot(image, padding, scale, rows.toList())
}
