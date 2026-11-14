// Folia classic adaptation: source ledger in docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.styles.classic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Canvas as BitmapCanvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import com.mocharealm.accompanist.lyrics.folia.*
import com.mocharealm.accompanist.lyrics.folia.internal.animation.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine
import com.mocharealm.accompanist.lyrics.folia.internal.text.*
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.blurredMasks
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.GlowBudget
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.hitClassicUnit
import kotlin.math.*

private data class GlyphGlow(val timing: TimedGrapheme, val origin: Offset,
    val inner: ImageBitmap, val outer: ImageBitmap?)
private data class Cell(val unit: DisplayUnit, val layout: TextLayoutResult, val center: Offset, val pose: Pose,
    val boxWidth: Float, val boxHeight: Float, val glows: List<GlyphGlow> = emptyList())
private data class Prepared(val cells: List<Cell>, val height: Float, val geometry: ClassicGeometry,
    val quality: FoliaQuality = FoliaQuality.Minimal)
private class DrawnFrame { var timeUs: Long? = null; var center = Offset.Zero }
private fun motionColor(a: Color, b: Color, q: Float): Color {
    fun channel(x:Float,y:Float) = sqrt(x*x+(y*y-x*x)*q)
    return Color(channel(a.red,b.red),channel(a.green,b.green),channel(a.blue,b.blue),a.alpha+(b.alpha-a.alpha)*q)
}

/** Measured only on content/geometry/font changes; time is read exclusively in draw. */
@Composable
internal fun ClassicLine(line: NormalizedLine, playback: () -> FoliaPlaybackSnapshot,
    textStyle: TextStyle, colors: FoliaColors, options: FoliaOptions, tuning: ClassicTuning,
    heightBudget: Dp, modifier: Modifier = Modifier, enterUs: Long = line.startUs,
    leaveUs: Long? = null, maskBudgetBytes: Long = GlowBudget.primaryBytes,
    onPrepared: (FoliaQuality, Int) -> Unit = { _, _ -> },
    onLayout: (ClassicGeometry) -> Unit = {},
    onClick: (() -> Unit)? = null, onLongPress: (() -> Unit)? = null) {
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val units = remember(line) { displayUnits(line) }
    val density = LocalDensity.current
    val currentPlayback by rememberUpdatedState(playback)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val budgetPx = with(density) { heightBudget.toPx() }.coerceAtLeast(80f)
        val prepared = remember(units, textStyle, widthPx, budgetPx, density, tuning.avoidOverlap,
            tuning.wordSpacing, tuning.enableWordRotation, measurer, options.quality, maskBudgetBytes) {
            fun prepare(fontScale: Float): Prepared {
                val style = textStyle.copy(fontSize = textStyle.fontSize * fontScale,
                    lineHeight = textStyle.fontSize * fontScale * 1.22f, textAlign = TextAlign.Start)
                val measured = units.map { unit -> measurer.measure(AnnotatedString(unit.text), style,
                    softWrap = false) }
                val rawWidths = measured.map { abs(it.getHorizontalPosition(it.layoutInput.text.length, true) - it.getHorizontalPosition(0, true)).toDouble() }
                // CSS collapses trailing whitespace in the word's box, while its canvas spacing
                // measurement includes that whitespace. Keep both metrics, and the timed text.
                val boxWidths = units.map { unit -> measurer.measure(AnnotatedString(unit.text.trimEnd()), style,
                    softWrap = false).let { abs(it.getHorizontalPosition(it.layoutInput.text.length, true) - it.getHorizontalPosition(0, true)).toDouble() } }
                val poses = units.indices.map { wordPose(line.startUs, it, tuning.enableWordRotation) }
                val rtl = measured.firstOrNull()?.getParagraphDirection(0) == androidx.compose.ui.text.style.ResolvedTextDirection.Rtl
                val font = with(density) { style.fontSize.toPx() }.toDouble()
                val geometry = classicGeometry(rawWidths, boxWidths, font, widthPx.toDouble(), poses, line.startUs,
                    tuning.wordSpacing, tuning.avoidOverlap, rtl, density.density.toDouble())
                val cells = units.indices.map { index ->
                    val box = geometry.boxes[index]
                    Cell(units[index], measured[index], Offset(box.left + box.width / 2, box.top + box.height / 2),
                        poses[index], box.width, box.height)
                }
                return Prepared(cells, geometry.height, geometry)
            }
            var result = prepare(1f)
            var fontScale = 1f
            while (tuning.avoidOverlap && result.height > budgetPx && fontScale > .5f) {
                val next = (fontScale * budgetPx / result.height * .98f).coerceAtLeast(.5f)
                if (next >= fontScale - .005f) break
                fontScale = next
                result = prepare(fontScale)
            }
            // Fixed, bounded masks. Their alpha changes in draw; glyph shadows never change per frame.
            val padding = ceil(72 * density.density).toInt()
            fun glyphBounds(cell: Cell, timing: TimedGrapheme) = cell.layout.getPathForRange(timing.begin, timing.end).getBounds()
            val bytesPerLayer = result.cells.sumOf { cell -> cell.unit.graphemes.sumOf { timing ->
                val bounds = glyphBounds(cell, timing)
                (ceil(bounds.width).toLong() + 2L * padding + 1) * (ceil(bounds.height).toLong() + 2L * padding + 1) * 4L
            } }
            val layers = GlowBudget.layers(options.quality, bytesPerLayer, maskBudgetBytes)
            if (layers == 0) result
            else result.copy(quality = if (layers == 2) FoliaQuality.Standard else FoliaQuality.Reduced, cells = result.cells.map { cell ->
                val duration = line.endUs - line.startUs
                val radii = floatArrayOf(if (duration < 100_000) 14f else if (duration < 180_000) 18f else 20f,
                    if (duration < 100_000) 24f else if (duration < 180_000) 32f else 40f)
                cell.copy(glows = cell.unit.graphemes.map { timing ->
                    val path = cell.layout.getPathForRange(timing.begin, timing.end)
                    val bounds = glyphBounds(cell, timing)
                    val origin = Offset(floor(bounds.left) - padding, floor(bounds.top) - padding)
                    val bitmap = ImageBitmap(ceil(bounds.right - origin.x + padding).toInt().coerceAtLeast(1),
                        ceil(bounds.bottom - origin.y + padding).toInt().coerceAtLeast(1))
                    CanvasDrawScope().draw(density, LayoutDirection.Ltr, BitmapCanvas(bitmap), Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
                        drawRect(Color.Transparent, blendMode = BlendMode.Clear)
                        withTransform({ translate(-origin.x, -origin.y) }) {
                            // Slice coverage from one fully shaped run; never reshape individual characters.
                            clipPath(path) { drawText(cell.layout, Color.White, Offset.Zero) }
                        }
                    }
                    val masks = blurredMasks(bitmap, IntArray(layers) { maxOf(1, (radii[it] * density.density / 2).toInt()) })
                    GlyphGlow(timing, origin, masks[0], masks.getOrNull(1))
                })
            })
        }
        val notify by rememberUpdatedState(onPrepared)
        val layoutObserver by rememberUpdatedState(onLayout)
        LaunchedEffect(prepared) { notify(prepared.quality, prepared.cells.size); layoutObserver(prepared.geometry) }
        val profile = remember(line) { renderProfile(line.startUs, line.endUs, line.fragments.lastOrNull()?.endUs ?: line.endUs) }
        val drawn = remember(prepared) { DrawnFrame() }
        val click by rememberUpdatedState(onClick)
        val longPress by rememberUpdatedState(onLongPress)
        fun hit(point: Offset): Boolean {
            val time = drawn.timeUs ?: return false
            val body = sampleLine(time, enterUs, leaveUs, profile, options.reducedMotion, tuning.breathingFloatMultiplier, density.density)
            if (body.alpha <= .001f) return false
            return prepared.cells.asReversed().any { cell ->
                val frame = sampleWord(time, enterUs, cell.unit.startUs, cell.unit.endUs, profile, cell.pose, options.reducedMotion)
                hitClassicUnit(point, drawn.center, body.scale, body.breathingY, cell.center, frame,
                    density.density, cell.boxWidth, cell.boxHeight)
            }
        }
        val input = if (onClick == null && onLongPress == null) Modifier else Modifier
            .pointerInput(prepared, options, tuning, enterUs, leaveUs) {
                detectTapGestures(onTap = { if (hit(it)) click?.invoke() }, onLongPress = { if (hit(it)) longPress?.invoke() })
            }.semantics {
                contentDescription = line.text
                onClick { click?.invoke(); true }
                onLongClick { longPress?.invoke(); true }
            }.onKeyEvent { event ->
                if (event.key != Key.Enter && event.key != Key.Spacebar) false
                else { if (event.type == KeyEventType.KeyUp) click?.invoke(); true }
            }.focusable()
        val glowColor = remember(colors.active) { ColorFilter.tint(colors.active) }
        val bodyLayers = prepared.cells.map { rememberGraphicsLayer() }
        val lineLayer = rememberGraphicsLayer()
        val linePadding = remember(prepared, density) {
            val reach = prepared.cells.maxOfOrNull { hypot(it.boxWidth,it.boxHeight)*it.pose.scale*.75f } ?: 0f
            ceil(reach+160*density.density).toInt()
        }
        Canvas(Modifier.fillMaxWidth().height(with(density) { prepared.height.toDp() }).then(input)) {
            val time = (currentPlayback().positionMillis - options.offsetMillis) * 1000
            drawn.timeUs = time; drawn.center = center
            val body = sampleLine(time, enterUs, leaveUs, profile, options.reducedMotion, tuning.breathingFloatMultiplier, density.density)
            lineLayer.alpha = body.alpha
            lineLayer.renderEffect = if(body.blur > .01f) BlurEffect(body.blur,body.blur,TileMode.Decal) else null
            lineLayer.record(size=IntSize(size.width.toInt()+linePadding*2,size.height.toInt()+linePadding*2)) {
            withTransform({translate(linePadding.toFloat(),linePadding.toFloat())}) {
            withTransform({
                translate(0f, body.breathingY)
                scale(body.scale, body.scale, drawn.center)
            }) {
                prepared.cells.forEachIndexed { cellIndex, cell ->
                    val frame = sampleWord(time, enterUs, cell.unit.startUs, cell.unit.endUs,
                        profile, cell.pose, options.reducedMotion)
                    val alpha = frame.alpha.coerceIn(0f, 1f)
                    if (alpha > .001f) withTransform({
                        translate(cell.center.x + frame.x * density.density, cell.center.y + frame.y * density.density)
                        rotate(frame.rotation, Offset.Zero)
                        scale(frame.scale, frame.scale, Offset.Zero)
                    }) {
                        // CSS splits even negative leading around the font metrics. Compose keeps
                        // the minimum natural paragraph height; center that shaped paragraph in the box.
                        val topLeft = Offset(-cell.boxWidth / 2f, -cell.layout.multiParagraph.height / 2f)
                        cell.glows.forEach { glyph ->
                            val glow = sampleGraphemeGlow(time, enterUs, cell.unit.startUs, cell.unit.endUs,
                                glyph.timing.startUs, glyph.timing.endUs, cell.unit.graphemes.size, profile)
                            if (glow > .001f) {
                                val origin = topLeft + glyph.origin
                                glyph.outer?.let { drawImage(it, origin, alpha = glow * alpha, colorFilter = glowColor) }
                                drawImage(glyph.inner, origin, alpha = glow * alpha, colorFilter = glowColor)
                            }
                        }
                        val color = motionColor(colors.text, colors.active, frame.mix)
                        if(frame.blur > .01f) {
                            val layer = bodyLayers[cellIndex]
                            val padding = ceil(32*density.density).toInt()
                            layer.renderEffect = BlurEffect(frame.blur*density.density,frame.blur*density.density,TileMode.Decal)
                            layer.record(size=IntSize(cell.layout.size.width+padding*2,cell.layout.size.height+padding*2)) {
                                drawText(cell.layout,color,Offset(padding.toFloat(),padding.toFloat()),alpha=alpha)
                            }
                            withTransform({translate(topLeft.x-padding,topLeft.y-padding)}) {drawLayer(layer)}
                        } else drawText(cell.layout, color, topLeft, alpha = alpha)
                    }
                }
            }
            }
            }
            withTransform({translate(-linePadding.toFloat(),-linePadding.toFloat())}) { drawLayer(lineLayer) }
        }
    }
}
