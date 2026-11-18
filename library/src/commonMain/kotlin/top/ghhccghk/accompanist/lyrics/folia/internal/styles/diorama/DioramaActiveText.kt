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
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.blurPremultiplied

internal data class DioramaActiveSlice(val unit: DioramaUnit, val image: ImageBitmap, val centerPx: Double,
    val rangeLeft: Float, val rangeRight: Float, val halo: ImageBitmap)

/** Full-shaped slices and the original registered halo / crisp soul / base order.
 * Text and halo textures are prepared once, never from the display-frame loop. */
internal class DioramaActiveText private constructor(val slices: List<DioramaActiveSlice>,
    val lineAdvance: Double, val middleBaseline: Double) {
    fun drawBase(scope: DrawScope, time: Double, view: DioramaPerspective, frame: DioramaFrame,
        placement: DioramaPlacement, primary: DioramaLinearColor, accent: DioramaLinearColor,
        gradient: Double = 1.0, keywords: Map<Int, DioramaLinearColor> = emptyMap()) =
        draw(scope, time, view, frame, placement, primary, accent,
            DioramaUnitTuning(glow = 0.0, soul = 0.0, gradient = gradient), keywords)

    fun draw(scope: DrawScope, time: Double, view: DioramaPerspective, frame: DioramaFrame,
        placement: DioramaPlacement, primary: DioramaLinearColor, accent: DioramaLinearColor,
        tuning: DioramaUnitTuning = DioramaUnitTuning(),
        keywords: Map<Int, DioramaLinearColor> = emptyMap(), life: Double = 1.0) = with(scope) {
        val fit = dioramaFrameFit(lineAdvance * .62 / 128, 5.2, view.fov, view.width / view.height) * placement.scale
        val unitScale = fit * .62 / 128
        val right = dioramaPlaneAxis(frame, placement, 1.0, 0.0)
        val up = dioramaPlaneAxis(frame, placement, 0.0, 1.0)
        val normal = right.cross(up).normalized()
        val x = right * unitScale
        val y = up * unitScale
        val center = dioramaComposeLocal(frame, placement.offsetR, placement.offsetU, 0.0)
        val states = slices.mapIndexed { i, slice ->
            dioramaUnitFrame(time, slice.unit.start, slice.unit.end,
                dioramaMediaEnvelope(time, slice.unit.start, slice.unit.end), primary, accent, tuning, keywords[i], life = life)
        }
        fun plane(slice: DioramaActiveSlice, image: ImageBitmap, alpha: Double, color: Color,
            depth: Double = 0.0, lift: Double = 0.0, scale: Double = 1.0, additive: Boolean = false) {
            val origin = center + x * slice.centerPx + normal * (depth * fit) + up * (lift * fit) -
                x * (image.width / 2.0 * scale) + y * (image.height / 2.0 * scale)
            drawDioramaPlane(image,view,origin,x * scale,y * -scale,alpha.toFloat(),color,additive)
        }
        // Three sorts by renderOrder: all17 glow, all18 soul, then all19 bodies.
        // Rendering three layers per unit changes overlapping ghosts and halos.
        for ((i, slice) in slices.withIndex()) {
            val state = states[i]
            if (state.glowVisible) plane(slice, slice.halo, state.glowOpacity, state.fill.srgb(), depth = -.01, additive = true)
        }
        for ((i, slice) in slices.withIndex()) {
            val state = states[i]
            if (state.soulVisible) plane(slice, slice.image, state.soulOpacity, state.fill.srgb(),
                depth = -.005, lift = state.soulLift, scale = state.soulScale, additive = true)
        }
        for ((i, slice) in slices.withIndex()) {
            val state = states[i]
            plane(slice, slice.image, state.baseOpacity, state.fill.srgb())
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
                    range.left, range.right, prepareDioramaHalo(image))
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

/** Original pure-shadow ladder: shadowColor modulates source coverage BEFORE
 * the blur, rather than multiplying an already rounded 8-bit blur result. */
internal fun prepareDioramaHalo(body: ImageBitmap): ImageBitmap {
    val halo = ImageBitmap(body.width, body.height)
    val passes = listOf(10.24f to .95f, 10.24f to .85f, 20.48f to .55f, 29.696f to .3f)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(halo), Size(body.width.toFloat(), body.height.toFloat())) {
        for ((sigma, alpha) in passes) {
            val shadowSource = ImageBitmap(body.width, body.height)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(shadowSource), size) {
                drawImage(body, alpha = alpha)
            }
            val blur = blurPremultiplied(shadowSource, sigma)
            drawImage(blur.image, topLeft = blur.origin, blendMode = BlendMode.Plus)
        }
    }
    return halo
}
