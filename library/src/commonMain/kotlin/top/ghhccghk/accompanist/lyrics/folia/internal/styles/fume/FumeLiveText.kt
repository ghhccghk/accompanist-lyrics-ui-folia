// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 fumeCanvasText.ts (complete shaped-run clipping).
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

internal fun FumeInk.color() = Color(red / 255f, green / 255f, blue / 255f, alpha.toFloat())
private data class FumePaintedGlyph(val index: Int, val left: Float, val right: Float)
private class FumePaintedSegment(val glyphs: List<FumePaintedGlyph>, val blank: Boolean,
    private val layout: TextLayoutResult, private val rasterDensity: Density, val textOrigin: Offset,
    val bounds: Rect, createMask: () -> ParagraphRaster) {
    val mask by lazy(createMask)
    // One most-recent radius, not an unbounded time/color cache. Released with the prepared block.
    private var lastSigma = Float.NaN
    private var lastBlur: BlurredPixels? = null
    fun shadow(sigma: Float): BlurredPixels {
        if (lastSigma != sigma) {
            val pad = ceil(3 * sigma + 2).toInt()
            val padded = ImageBitmap(mask.image.width + pad * 2, mask.image.height + pad * 2)
            CanvasDrawScope().draw(rasterDensity, LayoutDirection.Ltr, Canvas(padded), Size(padded.width.toFloat(), padded.height.toFloat())) {
                drawImage(mask.image, Offset(pad.toFloat(), pad.toFloat()))
            }
            val blurred = blurPremultiplied(padded, sigma)
            lastBlur = BlurredPixels(blurred.image, blurred.origin - Offset(pad.toFloat(), pad.toFloat()))
            lastSigma = sigma
        }
        return checkNotNull(lastBlur)
    }
    fun glow(scope: DrawScope, paint: FumePaint) = with(scope) {
        if (paint.blur > 0 && paint.shadow.alpha > 0 && paint.fill.alpha > 0) {
            val blurred = shadow((paint.blur / 2 * rasterDensity.density).toFloat())
            drawImage(blurred.image, mask.origin + blurred.origin,
                colorFilter = ColorFilter.tint(paint.shadow.alpha(paint.shadow.alpha * paint.fill.alpha).color()))
        }
        drawImage(mask.image, mask.origin, colorFilter = ColorFilter.tint(paint.fill.color()))
    }
    fun draw(scope: DrawScope, paint: FumePaint, left: Float, right: Float, opacity: Float = 1f) = with(scope) {
        if (right <= left) return@with
        clipRect(left, bounds.top, right, bounds.bottom) {
            // A Canvas shadow is generated from the source's alpha, then clipped with its run.
            if (paint.blur > 0 && paint.shadow.alpha > 0 && paint.fill.alpha > 0) {
                val blurred = shadow((paint.blur / 2 * rasterDensity.density).toFloat())
                drawImage(blurred.image, mask.origin + blurred.origin,
                    colorFilter = ColorFilter.tint(paint.shadow.alpha(paint.shadow.alpha * paint.fill.alpha).color()), alpha = opacity)
            }
            drawText(layout, paint.fill.color().copy(alpha = (paint.fill.alpha * opacity).toFloat()), textOrigin, shadow = Shadow.None)
        }
    }
}
private data class FumePaintedRow(val segments: List<FumePaintedSegment>, val whole: FumePaintedSegment,
    val baseline: Float)

/** Prepared rows retain the full TextLayoutResult for shaping and all original EGC times.
 * World coordinates are converted only at draw. This internal path is under visual calibration. */
internal class FumeLiveText private constructor(private val block: FumeArticleBlock,
    private val rasterDensity: Density, private val rows: List<FumePaintedRow>, val focus: FumeFocusBlock) {
    val reveal = FumeReveal(block.line)
    // At most the two static crossfade paints. Never key by frame time/camera scale.
    private val snapshots = linkedMapOf<FumePaint, Pair<ImageBitmap, Offset>>()
    internal fun releaseStaticSnapshots() { snapshots.clear() }

    private fun snapshot(paint: FumePaint): Pair<ImageBitmap, Offset> {
        snapshots.remove(paint)?.let { snapshots[paint] = it; return it }
        val pixels = rasterDensity.density
        val padding = ceil(maxOf(block.fontPx * .32, paint.blur + block.fontPx * .08, 4.0))
        val image = ImageBitmap(ceil((block.width + 2 * padding) * pixels).toInt().coerceAtLeast(1),
            ceil((block.height + 2 * padding) * pixels).toInt().coerceAtLeast(1))
        val origin = Offset(((block.x - padding) * pixels).toFloat(), ((block.y - padding) * pixels).toFloat())
        CanvasDrawScope().draw(rasterDensity, LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
            withTransform({ translate(-origin.x, -origin.y) }) {
                for (row in rows) row.whole.draw(this, paint, row.whole.bounds.left, row.whole.bounds.right)
            }
        }
        val result = image to origin
        if (snapshots.size >= 2) snapshots.remove(snapshots.keys.first())
        snapshots[paint] = result
        return result
    }

    fun draw(scope: DrawScope, time: Double, nextStart: Double?, theme: FumeFrameTheme,
        hold: Double = 1.0, fadeDuration: Double = 2.4, overviewRestore: Double = 0.0, showStamp: Boolean = true,
        liveShadowScale: Double = 1.0) = with(scope) {
        require(liveShadowScale.isFinite() && liveShadowScale > 0)
        fun livePaint(paint: FumePaint) = if (liveShadowScale == 1.0) paint else paint.copy(blur = paint.blur / liveShadowScale)
        val timing = fumeBlockTiming(block, time, nextStart, hold)
        val layers = fumeStaticLayers(block, timing, theme, time, fadeDuration, overviewRestore)
        if (layers != null) {
            for (layer in layers) {
                val (image, origin) = snapshot(layer.paint)
                drawImage(image, origin, alpha = layer.alpha.toFloat())
            }
            return@with
        }
        fumeLineGlow(block, timing, theme, time)?.let { glow ->
            for (row in rows) for (segment in row.segments) if (!segment.blank)
                segment.glow(this, livePaint(glow))
        }
        val scale = rasterDensity.density
        for (row in rows) for (segment in row.segments) {
            var run: FumePaint? = null
            var left = 0f; var right = 0f
            fun flush() { run?.let { segment.draw(this, it, left, right) }; run = null }
            for (glyph in segment.glyphs) {
                val frame = fumeGlyphFrame(block, reveal, timing, theme, time, glyph.index,
                    (glyph.right - glyph.left).toDouble() / scale, showStamp)
                // Preserve source command order: stamps precede a pending run's flush.
                frame.stamp?.let { stamp ->
                    drawStamp(this, stamp.copy(paint = livePaint(stamp.paint)), Offset(glyph.left, row.baseline), scale)
                }
                if (frame.paint.fill.alpha <= .002) { flush(); continue }
                val paint = livePaint(frame.paint)
                if (run != paint) {
                    flush(); run = paint; left = glyph.left; right = glyph.right
                } else { left = min(left, glyph.left); right = max(right, glyph.right) }
            }
            flush()
        }
    }

    private fun drawStamp(scope: DrawScope, stamp: FumeStamp, origin: Offset, scale: Float) = with(scope) {
        val center = origin + Offset((stamp.centerX * scale).toFloat(), (stamp.centerY * scale).toFloat())
        val size = Size((stamp.width * scale).toFloat(), (stamp.height * scale).toFloat())
        val topLeft = center - Offset(size.width / 2, size.height / 2)
        if (stamp.paint.blur > 0 && stamp.paint.shadow.alpha > 0 && stamp.paint.fill.alpha > 0) {
            val sigma = (stamp.paint.blur * scale / 2).toFloat()
            val pad = ceil(sigma * 3 + 2).toInt()
            val integer = Offset(floor(topLeft.x) - pad, floor(topLeft.y) - pad)
            val mask = ImageBitmap(ceil(topLeft.x + size.width - integer.x + pad).toInt().coerceAtLeast(1),
                ceil(topLeft.y + size.height - integer.y + pad).toInt().coerceAtLeast(1))
            CanvasDrawScope().draw(rasterDensity, LayoutDirection.Ltr, Canvas(mask), Size(mask.width.toFloat(), mask.height.toFloat())) {
                drawRect(Color.White, topLeft - integer, size)
            }
            val blur = blurPremultiplied(mask, sigma)
            drawImage(blur.image, integer + blur.origin, colorFilter = ColorFilter.tint(
                stamp.paint.shadow.alpha(stamp.paint.shadow.alpha * stamp.paint.fill.alpha).color()))
        }
        drawRect(stamp.paint.fill.color(), topLeft, size)
    }

    companion object {
        fun prepare(block: FumeArticleBlock, measure: FumeTextMeasure, resolver: FontFamily.Resolver,
            density: Density): FumeLiveText {
            val static = prepareFumeStaticText(block, measure, resolver, density, Color.White)
            val edges = graphemeBoundaries(block.line.text).sorted()
            val scale = density.density
            val style = measure.style(block.fontPx, block.hero)
            val metrics = naturalFontMetrics(style, resolver, density)
            val middle = fumeMiddleBaseline(metrics.ascent, metrics.descent, with(density) { style.fontSize.toPx() })
            val cjk = Regex("[\u4e00-\u9fff\u3040-\u30ff\uac00-\ud7af]").containsMatchIn(block.line.text)
            val baselineOffset = block.lineHeight * if (cjk) .52 else .5
            val focusRows = mutableListOf<FumeFocusRow>()
            val rows = static.rows.map { row ->
                val layout = measure.naturalLayout(row.text, block.hero, block.fontPx)
                val first = edges.indexOf(row.startUtf16)
                val last = edges.indexOf(row.endUtf16)
                check(first >= 0 && last >= first) { "A Fume row boundary split an extended grapheme" }
                val offsets = (first..last).map { index ->
                    val count = edges[index] - row.startUtf16
                    if (count == 0) 0.0 else layout.getPathForRange(0, count).getBounds().width.toDouble() / scale
                }
                focusRows += FumeFocusRow(first,last,row.left,row.top,row.width.toDouble(),offsets)
                val baseline = ((block.y + row.top + baselineOffset) * scale).toFloat() + middle
                val origin = Offset(((block.x + row.left) * scale).toFloat(), baseline - paintedNaturalBaseline(layout, metrics))
                val clipPad = ceil(max(block.fontPx * 3, 80.0) * scale).toInt()
                val rowBounds = Rect(origin.x - clipPad, origin.y - clipPad, origin.x + row.width * scale + clipPad, origin.y + layout.size.height + clipPad)
                val glyphs = edges.zipWithNext().mapIndexedNotNull { i, (a,b) ->
                    if (a < row.startUtf16 || b > row.endUtf16) null else {
                        val bounds = layout.getPathForRange(a - row.startUtf16, b - row.startUtf16).getBounds()
                        FumePaintedGlyph(i, origin.x + bounds.left, origin.x + bounds.right)
                    }
                }
                fun segment(selected: List<FumePaintedGlyph>, blank: Boolean, whole: Boolean): FumePaintedSegment {
                    val left = selected.minOfOrNull { it.left } ?: origin.x
                    val right = selected.maxOfOrNull { it.right } ?: origin.x
                    val bounds = if (whole) rowBounds else Rect(left, rowBounds.top, right, rowBounds.bottom)
                    return FumePaintedSegment(selected, blank, layout, density, origin, bounds) {
                        val pad = ceil(block.fontPx * .12 * scale + 4).toInt()
                        val maskOrigin = Offset(floor(left) - pad, floor(origin.y) - pad)
                        val mask = ImageBitmap(ceil(right - maskOrigin.x + pad).toInt().coerceAtLeast(1), layout.size.height + pad * 2)
                        CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(mask), Size(mask.width.toFloat(), mask.height.toFloat())) {
                            withTransform({ translate(-maskOrigin.x, -maskOrigin.y) }) {
                                clipRect(if (whole) rowBounds.left else left, rowBounds.top, if (whole) rowBounds.right else right, rowBounds.bottom) {
                                    withTransform({ translate(origin.x, origin.y) }) { drawParagraphShadowSource(layout) }
                                }
                            }
                        }
                        ParagraphRaster(mask, maskOrigin)
                    }
                }
                var offset = row.startUtf16
                val segments = segmentWords(row.text).map { word ->
                    val start = offset; offset += word.text.length
                    val selected = glyphs.filter { edges[it.index] >= start && edges[it.index] < offset }
                    segment(selected, word.text.all(::fumeWhitespace), false)
                }
                FumePaintedRow(segments, segment(glyphs, false, true), (baseline - middle))
            }
            return FumeLiveText(block, density, rows, FumeFocusBlock(block, focusRows))
        }
    }
}
