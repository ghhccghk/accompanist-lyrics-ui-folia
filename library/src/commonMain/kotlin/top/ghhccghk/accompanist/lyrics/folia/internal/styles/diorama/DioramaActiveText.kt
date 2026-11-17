// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 dioramaSceneLayout.ts / dioramaTextRaster.ts / sceneFrame.ts.
// Complete shaped-line slice adaptation; source mapping: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

internal data class DioramaActiveSlice(val unit: DioramaUnit, val image: ImageBitmap, val centerPx: Double,
    val rangeLeft: Float, val rangeRight: Float)

/** Base reveal/gradient layer only. A complete shaped paragraph is shared by
 * all slices: joining, ligatures and EGC clocks never become per-character
 * shaping. Glow/soul rasters and original continuous camera remain separate. */
internal class DioramaActiveText private constructor(val slices: List<DioramaActiveSlice>,
    val lineAdvance: Double, val middleBaseline: Double) {
    fun drawBase(scope: DrawScope, time: Double, view: DioramaPerspective, frame: DioramaFrame,
        placement: DioramaPlacement, primary: DioramaLinearColor, accent: DioramaLinearColor,
        gradient: Double = 1.0, keywords: Map<Int, DioramaLinearColor> = emptyMap()) = with(scope) {
        val fit = dioramaFrameFit(lineAdvance * .62 / 128, 5.2, view.fov, view.width / view.height) * placement.scale
        val unitScale = fit * .62 / 128
        val x = dioramaPlaneAxis(frame, placement, 1.0, 0.0) * unitScale
        val y = dioramaPlaneAxis(frame, placement, 0.0, 1.0) * unitScale
        val center = dioramaComposeLocal(frame, placement.offsetR, placement.offsetU, 0.0)
        for ((i, slice) in slices.withIndex()) {
            val state = dioramaUnitFrame(time, slice.unit.start, slice.unit.end,
                dioramaMediaEnvelope(time, slice.unit.start, slice.unit.end), primary, accent,
                DioramaUnitTuning(glow = 0.0, soul = 0.0, gradient = gradient), keywords[i])
            val origin = center + x * slice.centerPx - x * (slice.image.width / 2.0) + y * (slice.image.height / 2.0)
            val h = view.planeHomography(origin, x, y * -1.0)
            val matrix = Matrix().apply {
                this[0,0] = h[0].toFloat(); this[1,0] = h[1].toFloat(); this[3,0] = h[2].toFloat()
                this[0,1] = h[3].toFloat(); this[1,1] = h[4].toFloat(); this[3,1] = h[5].toFloat()
                this[0,3] = h[6].toFloat(); this[1,3] = h[7].toFloat(); this[3,3] = h[8].toFloat()
            }
            withTransform({ transform(matrix) }) {
                drawImage(slice.image, alpha = state.baseOpacity.toFloat(), colorFilter = ColorFilter.tint(state.fill.srgb()))
            }
        }
    }

    companion object {
        fun prepare(line: DioramaLyric, base: TextStyle, measurer: TextMeasurer,
            resolver: FontFamily.Resolver): DioramaActiveText {
            val density = Density(1f, 1f)
            val style = base.copy(fontSize = 128.sp, lineHeight = TextUnit.Unspecified,
                fontWeight = base.fontWeight ?: FontWeight.Bold, textAlign = TextAlign.Left,
                textDirection = TextDirection.ContentOrLtr, color = Color.White, shadow = Shadow.None)
            val text = line.text.replace(Regex("[\t\n\u000c\r]"), " ")
            val layout = measurer.measure(AnnotatedString(text), style, softWrap = false,
                density = density, layoutDirection = LayoutDirection.Ltr)
            val advance = if (text.isEmpty()) 0.0 else layout.getPathForRange(0, text.length).getBounds().width.toDouble()
            val metrics = naturalFontMetrics(style, resolver, density)
            val ratio = dioramaTypographicRatio(style, resolver) ?: (metrics.ascent / (metrics.ascent + metrics.descent)).toDouble()
            val middle = floor(128 * ratio * 64 + .5) / 64 - 64
            val slices = dioramaUnits(line).map { unit ->
                val range = layout.getPathForRange(unit.charStart, unit.charEnd).getBounds()
                val width = max(1, ceil(range.width.toDouble()).toInt())
                val padding = 90 // ceil(128*.7), original active-unit padding
                val image = ImageBitmap(width + padding * 2, 360) // ceil(128*1.4)+180
                val origin = Offset(padding - range.left, (180 + middle - paintedNaturalBaseline(layout, metrics)).toFloat())
                CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
                    // Slice the complete shaped run at unit advances. Do not let
                    // neighbouring glyphs appear in another unit's halo padding.
                    clipRect(padding.toFloat(), 0f, padding + range.width, image.height.toFloat()) {
                        drawText(layout, Color.White, origin, shadow = Shadow.None)
                    }
                }
                DioramaActiveSlice(unit, image, range.left + width / 2.0 - advance / 2,
                    range.left, range.right)
            }
            return DioramaActiveText(slices, advance, middle)
        }
    }
}

/** Three's material channels are linear; encode exactly once for Compose. */
internal fun DioramaLinearColor.srgb(): Color {
    fun channel(v: Double) = (if (v < .0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - .055).toFloat()
    return Color(channel(red), channel(green), channel(blue))
}
