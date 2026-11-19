// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 dioramaTextRaster.ts:rasterDioramaLine. Complete Compose
// paragraph shaping and projective Canvas adaptation; see docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.naturalFontMetrics
import top.ghhccghk.accompanist.lyrics.folia.internal.text.paintedNaturalBaseline
import top.ghhccghk.accompanist.lyrics.folia.internal.text.classicBodyRasterStyle
import kotlin.math.*

/** OS/2 typographic ascent fraction, when exposed by the platform font API.
 * Android's current public Typeface resolver does not expose font tables. */
internal expect fun dioramaTypographicRatio(style: TextStyle, resolver: FontFamily.Resolver): Double?

/** One complete shaped neighbouring-line raster, not an active-unit/glow stage.
 * Fixed source texture em is independent of viewport density/system font scale.
 * Prepare once per content/font change; camera queries never reshape the text. */
internal class DioramaPlainText private constructor(val raster:DioramaRaster, val advancePx: Int,
    val fontPx: Int, val padding: Int, val middleBaseline: Double, val hasTypographicMetrics: Boolean) {
    val image:ImageBitmap get()=raster.image
    val worldWidth: Double get() = advancePx * .62 / fontPx

    /** Real perspective/FrontSide/near-far clipping, same source basis/yaw/roll.
     * Optional fragment fog does not stand in for inter-plane depth testing. */
    fun draw(scope: DrawScope, view: DioramaPerspective, frame: DioramaFrame,
        placement: DioramaPlacement, color: Color = Color.White, alpha: Float = 1f,
        fog: DioramaFog? = null) = with(scope) {
        val plane = plane(view, frame, placement, color, alpha, fog)
        drawDioramaPlane(plane.image, view, plane.origin, plane.x, plane.y, plane.alpha, plane.color, fog = plane.fog)
    }

    fun plane(view: DioramaPerspective, frame: DioramaFrame, placement: DioramaPlacement,
        color: Color, alpha: Float, fog: DioramaFog?): DioramaTextPlane {
        val scale = dioramaFrameFit(worldWidth, 5.2, view.fov, view.width / view.height) * placement.scale
        val x = dioramaPlaneAxis(frame, placement, 1.0, 0.0) * (scale * .62 / fontPx)
        val y = dioramaPlaneAxis(frame, placement, 0.0, 1.0) * (scale * .62 / fontPx)
        val center = dioramaComposeLocal(frame, placement.offsetR, placement.offsetU, 0.0)
        val origin = center - x * (image.width / 2.0) + y * (image.height / 2.0)
        return DioramaTextPlane(image, origin, x, y * -1.0, alpha, color, false, fog)
    }

    companion object {
        fun prepare(text: String, baseStyle: TextStyle, measurer: TextMeasurer,
            resolver: FontFamily.Resolver,
            reserveRaster:(Int,Int,Int)->Unit={_,_,_->},
            retain:(ImageBitmap)->DioramaRaster=::DioramaRaster): DioramaPlainText {
            val density = Density(1f, 1f)
            var fontPx = 128
            // HTML Canvas normalizes these ASCII whitespace characters to spaces.
            // UTF16 offsets and the source timing text remain untouched elsewhere.
            val singleLine = text.replace(Regex("[\t\n\u000c\r]"), " ")
            // Same browser-foreground coverage policy already used by the other
            // text styles. Windows only; explicit host paragraph raster settings
            // and Android/other-OS defaults are retained. Active halo input is
            // separate and must not inherit foreground LCD/contrast settings.
            fun style() = classicBodyRasterStyle(baseStyle).copy(fontSize = fontPx.sp, lineHeight = TextUnit.Unspecified,
                fontWeight = baseStyle.fontWeight ?: FontWeight.Bold, textAlign = TextAlign.Left,
                textDirection = TextDirection.ContentOrLtr, color = Color.White, shadow = Shadow.None)
            fun measure() = measurer.measure(AnnotatedString(singleLine), style(), softWrap = false, density = density, layoutDirection = LayoutDirection.Ltr)
            var layout = measure()
            // Paragraph line width hangs trailing spaces; Canvas measureText
            // includes them. The complete selection preserves those advances.
            fun advance() = max(1, ceil(if (singleLine.isEmpty()) 0.0 else
                layout.getPathForRange(0, singleLine.length).getBounds().width.toDouble()).toInt())
            var advancePx = advance()
            // Source calculates padding BEFORE shrinking, and keeps that value.
            val padding = ceil(fontPx * .2).toInt()
            if (advancePx + padding * 2 > 4096) {
                fontPx = max(24, floor(fontPx * (4096.0 - padding * 2) / advancePx).toInt())
                layout = measure(); advancePx = advance()
            }
            val imageWidth=advancePx+padding*2;val imageHeight=ceil(fontPx*1.4).toInt()+padding*2
            reserveRaster(imageWidth,imageHeight,1)
            val image = ImageBitmap(imageWidth,imageHeight)
            val metrics = naturalFontMetrics(style(), resolver, density)
            val typographic = dioramaTypographicRatio(style(), resolver)
            val ratio = typographic ?: (metrics.ascent / (metrics.ascent + metrics.descent)).toDouble()
            val middle = floor(fontPx * ratio * 64 + .5) / 64 - fontPx / 2.0
            val origin = Offset(padding - layout.getLineLeft(0),
                (image.height / 2.0 + middle - paintedNaturalBaseline(layout, metrics)).toFloat())
            CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image),
                Size(image.width.toFloat(), image.height.toFloat())) {
                drawText(layout, Color.White, origin, shadow = Shadow.None)
            }
            return DioramaPlainText(retain(image), advancePx, fontPx, padding, middle, typographic != null)
        }
    }
}
