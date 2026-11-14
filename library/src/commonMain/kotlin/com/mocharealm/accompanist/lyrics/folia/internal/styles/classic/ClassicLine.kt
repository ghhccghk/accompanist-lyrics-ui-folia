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
import androidx.compose.ui.graphics.lerp
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
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.blurredMask
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.hitClassicUnit
import kotlin.math.*

private data class Cell(val unit: DisplayUnit, val layout: TextLayoutResult, val center: Offset, val pose: Pose,
    val innerGlow: ImageBitmap? = null, val outerGlow: ImageBitmap? = null, val glowPadding: Int = 0)
private data class Prepared(val cells: List<Cell>, val height: Float, val quality: FoliaQuality = FoliaQuality.Minimal)
private class DrawnFrame { var timeUs: Long? = null; var center = Offset.Zero }
private data class LineFrame(val alpha: Float, val scale: Float, val breathingY: Float)
private fun lineFrame(time: Long, lineStart: Long, enterUs: Long, leaveUs: Long?, profile: RenderProfile,
    reducedMotion: Boolean, breathing: Float, density: Float): LineFrame {
    val exitDuration = when (profile.mode) { Reveal.Normal -> 300_000f; Reveal.Fast -> 160_000f; Reveal.Instant -> 120_000f }
    val exit = leaveUs?.let { ((time - it) / exitDuration).coerceIn(0f, 1f) } ?: 0f
    val enter = if (profile.mode == Reveal.Instant || reducedMotion) 1f else ((time - enterUs) / 220_000f).coerceIn(0f, 1f)
    return LineFrame(enter * (1 - exit), if (reducedMotion) 1f else .9f + .1f * enter + .1f * exit,
        if (reducedMotion) 0f else sin((time - lineStart) / 1_000_000.0 * 2 * PI / 7).toFloat() * 14 * breathing * density)
}

/** Measured only on content/geometry/font changes; time is read exclusively in draw. */
@Composable
internal fun ClassicLine(line: NormalizedLine, playback: () -> FoliaPlaybackSnapshot,
    textStyle: TextStyle, colors: FoliaColors, options: FoliaOptions, tuning: ClassicTuning,
    heightBudget: Dp, modifier: Modifier = Modifier, enterUs: Long = line.startUs,
    leaveUs: Long? = null, onPrepared: (FoliaQuality, Int) -> Unit = { _, _ -> },
    onClick: (() -> Unit)? = null, onLongPress: (() -> Unit)? = null) {
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val units = remember(line) { displayUnits(line) }
    val density = LocalDensity.current
    val currentPlayback by rememberUpdatedState(playback)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        val budgetPx = with(density) { heightBudget.toPx() }.coerceAtLeast(80f)
        val prepared = remember(units, textStyle, widthPx, budgetPx, density, tuning, measurer, options.quality, colors.active) {
            val safeScale = if (tuning.avoidOverlap) 2.15f else 1.75f
            fun prepare(fontScale: Float): Prepared {
                val style = textStyle.copy(fontSize = textStyle.fontSize * fontScale, textAlign = TextAlign.Center)
                val cellWidth = maxOf(1, (widthPx / safeScale).toInt())
                val measured = units.map { unit -> measurer.measure(AnnotatedString(unit.text), style,
                    constraints = Constraints(maxWidth = cellWidth)) }
                val gap = with(density) { (4 + 12 * tuning.wordSpacing).dp.toPx() }
                val rows = mutableListOf<MutableList<Int>>(mutableListOf())
                var used = 0f
                measured.forEachIndexed { index, text ->
                    val needed = text.size.width * safeScale + gap
                    if (used > 0 && used + needed > widthPx) { rows += mutableListOf<Int>(); used = 0f }
                    rows.last() += index
                    used += needed
                }
                val cells = mutableListOf<Cell>()
                var top = 36f * density.density
                rows.forEach { row ->
                    val height = row.maxOfOrNull { measured[it].size.height * safeScale } ?: 0f
                    val total = row.sumOf { (measured[it].size.width * safeScale + gap).toDouble() }.toFloat() - gap
                    var left = (widthPx - total) / 2
                    row.forEach { index ->
                        val text = measured[index]
                        val width = text.size.width * safeScale
                        cells += Cell(units[index], text, Offset(left + width / 2, top + height / 2),
                            wordPose(line.startUs, index, tuning.enableWordRotation))
                        left += width + gap
                    }
                    top += height + gap
                }
                return Prepared(cells, top + 36f * density.density)
            }
            var result = prepare(1f)
            var fontScale = 1f
            repeat(3) {
                if (result.height > budgetPx && fontScale > .5f) {
                    fontScale = (fontScale * budgetPx / result.height * .98f).coerceAtLeast(.5f)
                    result = prepare(fontScale)
                }
            }
            // Fixed, bounded masks. Their alpha changes in draw; glyph shadows never change per frame.
            val padding = ceil(72 * density.density).toInt()
            val bytesPerLayer = result.cells.sumOf { (it.layout.size.width + 2L * padding) * (it.layout.size.height + 2L * padding) * 4L }
            val layers = if (options.quality == FoliaQuality.Standard) 2 else if (options.quality == FoliaQuality.Reduced) 1 else 0
            if (layers == 0 || bytesPerLayer * layers > 16L * 1024 * 1024) result
            else result.copy(quality = options.quality, cells = result.cells.map { cell ->
                fun mask(radius: Float): ImageBitmap {
                    val bitmap = ImageBitmap(cell.layout.size.width + padding * 2, cell.layout.size.height + padding * 2)
                    CanvasDrawScope().draw(density, LayoutDirection.Ltr, BitmapCanvas(bitmap), Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
                        drawRect(Color.Transparent, blendMode = BlendMode.Clear)
                        drawText(cell.layout, Color.White, Offset(padding.toFloat(), padding.toFloat()))
                    }
                    return blurredMask(bitmap, maxOf(1, (radius * density.density / 2).toInt()))
                }
                val duration = line.endUs - line.startUs
                cell.copy(innerGlow = mask(if (duration < 100_000) 14f else if (duration < 180_000) 18f else 20f),
                    outerGlow = if (layers == 2) mask(if (duration < 100_000) 24f else if (duration < 180_000) 32f else 40f) else null, glowPadding = padding)
            })
        }
        val notify by rememberUpdatedState(onPrepared)
        LaunchedEffect(prepared) { notify(prepared.quality, prepared.cells.size) }
        val profile = remember(line) { renderProfile(line.startUs, line.endUs, line.fragments.lastOrNull()?.endUs ?: line.endUs) }
        val drawn = remember(prepared) { DrawnFrame() }
        val click by rememberUpdatedState(onClick)
        val longPress by rememberUpdatedState(onLongPress)
        fun hit(point: Offset): Boolean {
            val time = drawn.timeUs ?: return false
            val body = lineFrame(time, line.startUs, enterUs, leaveUs, profile, options.reducedMotion, tuning.breathingFloatMultiplier, density.density)
            if (body.alpha <= .001f) return false
            return prepared.cells.asReversed().any { cell ->
                val frame = sampleWord(time, enterUs, cell.unit.startUs, cell.unit.endUs, profile, cell.pose, options.reducedMotion)
                hitClassicUnit(point, drawn.center, body.scale, body.breathingY, cell.center, frame,
                    density.density, cell.layout.size.width.toFloat(), cell.layout.size.height.toFloat())
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
        Canvas(Modifier.fillMaxWidth().height(with(density) { prepared.height.toDp() }).then(input)) {
            val time = (currentPlayback().positionMillis - options.offsetMillis) * 1000
            drawn.timeUs = time; drawn.center = center
            val body = lineFrame(time, line.startUs, enterUs, leaveUs, profile, options.reducedMotion, tuning.breathingFloatMultiplier, density.density)
            withTransform({
                translate(0f, body.breathingY)
                scale(body.scale, body.scale, center)
            }) {
                prepared.cells.forEach { cell ->
                    val frame = sampleWord(time, enterUs, cell.unit.startUs, cell.unit.endUs,
                        profile, cell.pose, options.reducedMotion)
                    val alpha = (frame.alpha * body.alpha).coerceIn(0f, 1f)
                    if (alpha > .001f) withTransform({
                        translate(cell.center.x + frame.x * density.density, cell.center.y + frame.y * density.density)
                        rotate(frame.rotation, Offset.Zero)
                        scale(frame.scale, frame.scale, Offset.Zero)
                    }) {
                        val topLeft = Offset(-cell.layout.size.width / 2f, -cell.layout.size.height / 2f)
                        if (options.quality != FoliaQuality.Minimal && frame.glow > .001f) {
                            val glowOrigin = topLeft - Offset(cell.glowPadding.toFloat(), cell.glowPadding.toFloat())
                            cell.outerGlow?.let { drawImage(it, glowOrigin, alpha = frame.glow * alpha * .35f, colorFilter = glowColor) }
                            cell.innerGlow?.let { drawImage(it, glowOrigin, alpha = frame.glow * alpha * .5f, colorFilter = glowColor) }
                        }
                        drawText(cell.layout, lerp(colors.text, colors.active, frame.mix), topLeft, alpha = alpha)
                    }
                }
            }
        }
    }
}
