// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 dioramaSceneLayout.ts / dioramaTextRaster.ts / sceneFrame.ts.
// Complete shaped-line slice adaptation; source mapping: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.blurPremultiplied

internal data class DioramaActiveSlice(val unit: DioramaUnit, val image: ImageBitmap, val centerPx: Double,
    val rangeLeft: Float, val rangeRight: Float, val halo: ImageBitmap, val imageOffsetPx: Double = 0.0)

/** Full-shaped slices and the original registered halo / crisp soul / base order.
 * Text and halo textures are prepared once, never from the display-frame loop. */
internal class DioramaActiveText private constructor(val slices: List<DioramaActiveSlice>,
    val lineAdvance: Double, val middleBaseline: Double) {
    fun drawBase(scope: DrawScope, time: Double, view: DioramaPerspective, frame: DioramaFrame,
        placement: DioramaPlacement, primary: DioramaLinearColor, accent: DioramaLinearColor,
        gradient: Double = 1.0, keywords: Map<Int, DioramaLinearColor> = emptyMap(),
        baseRenderer: DioramaActiveBaseRenderer? = null) =
        draw(scope, time, view, frame, placement, primary, accent,
            DioramaUnitTuning(glow = 0.0, soul = 0.0, gradient = gradient), keywords,baseRenderer=baseRenderer)

    fun draw(scope: DrawScope, time: Double, view: DioramaPerspective, frame: DioramaFrame,
        placement: DioramaPlacement, primary: DioramaLinearColor, accent: DioramaLinearColor,
        tuning: DioramaUnitTuning = DioramaUnitTuning(),
        keywords: Map<Int, DioramaLinearColor> = emptyMap(), life: Double = 1.0,
        fog: DioramaFog? = null, toneMapping: DioramaToneMapping = DioramaToneMapping.None,
        breath: Double = 1.0, baseRenderer: DioramaActiveBaseRenderer? = null) {
        val states = slices.mapIndexed { i, slice ->
            dioramaUnitFrame(time, slice.unit.start, slice.unit.end,
                dioramaMediaEnvelope(time, slice.unit.start, slice.unit.end), primary, accent, tuning, keywords[i], life = life, breath = breath)
        }
        drawFrames(scope,view,frame,placement,states,fog,toneMapping,baseRenderer)
    }

    /** Shared renderer for independently resolved material states. Media playback
     * resolves analytic states above; finite source controls replay their OWN
     * core clocks/history. Neither path re-rasterizes or changes shaping here. */
    fun drawFrames(scope: DrawScope, view: DioramaPerspective, frame: DioramaFrame,
        placement: DioramaPlacement, states: List<DioramaUnitFrame>, fog: DioramaFog? = null,
        toneMapping: DioramaToneMapping = DioramaToneMapping.None,
        baseRenderer: DioramaActiveBaseRenderer? = null) = with(scope) {
        require(states.size==slices.size) { "One resolved frame per prepared text unit is required" }
        val fit = dioramaFrameFit(lineAdvance * .62 / 128, 5.2, view.fov, view.width / view.height) * placement.scale
        val unitScale = fit * .62 / 128
        val right = dioramaPlaneAxis(frame, placement, 1.0, 0.0)
        val up = dioramaPlaneAxis(frame, placement, 0.0, 1.0)
        val normal = right.cross(up).normalized()
        val x = right * unitScale
        val y = up * unitScale
        val center = dioramaComposeLocal(frame, placement.offsetR, placement.offsetU, 0.0)
        fun plane(slice: DioramaActiveSlice, image: ImageBitmap, alpha: Double, color: DioramaLinearColor,
            depth: Double = 0.0, lift: Double = 0.0, scale: Double = 1.0, additive: Boolean = false,
            sampleBase: Boolean = false) {
            val origin = center + x * (slice.centerPx + slice.imageOffsetPx * scale) + normal * (depth * fit) + up * (lift * fit) -
                x * (image.width / 2.0 * scale) + y * (image.height / 2.0 * scale)
            if(sampleBase&&baseRenderer?.draw(image,this,view,origin,x*scale,y*-scale,color,
                    alpha.toFloat(),fog,toneMapping)==true)return
            drawDioramaPlane(image,view,origin,x * scale,y * -scale,alpha.toFloat(),color.srgb(toneMapping),additive,fog)
        }
        // Three sorts by renderOrder: all17 glow, all18 soul, then all19 bodies.
        // Rendering three layers per unit changes overlapping ghosts and halos.
        for ((i, slice) in slices.withIndex()) {
            val state = states[i]
            if (state.glowVisible) plane(slice, slice.halo, state.glowOpacity, state.fill, depth = -.01, additive = true)
        }
        for ((i, slice) in slices.withIndex()) {
            val state = states[i]
            if (state.soulVisible) plane(slice, slice.image, state.soulOpacity, state.fill,
                depth = -.005, lift = state.soulLift, scale = state.soulScale, additive = true)
        }
        for ((i, slice) in slices.withIndex()) {
            val state = states[i]
            plane(slice, slice.image, state.baseOpacity, state.fill, sampleBase = true)
        }
    }

    companion object {
        fun prepare(line: DioramaLyric, base: TextStyle, measurer: TextMeasurer,
            resolver: FontFamily.Resolver, checkpoint: () -> Unit = {}): DioramaActiveText {
            checkpoint()
            val density = Density(1f, 1f)
            val style = base.copy(fontSize = 128.sp, lineHeight = TextUnit.Unspecified,
                fontWeight = base.fontWeight ?: FontWeight.Bold, textAlign = TextAlign.Left,
                textDirection = TextDirection.ContentOrLtr, color = Color.White, shadow = Shadow.None)
            val text = line.text.replace(Regex("[\t\n\u000c\r]"), " ")
            val layout = measurer.measure(AnnotatedString(text), style, softWrap = false,
                density = density, layoutDirection = LayoutDirection.Ltr)
            checkpoint()
            val advance = if (text.isEmpty()) 0.0 else layout.getPathForRange(0, text.length).getBounds().width.toDouble()
            val metrics = naturalFontMetrics(style, resolver, density)
            val ratio = dioramaTypographicRatio(style, resolver) ?: (metrics.ascent / (metrics.ascent + metrics.descent)).toDouble()
            val middle = floor(128 * ratio * 64 + .5) / 64 - 64
            val padding = 90 // ceil(128*.7), original active-unit padding
            val origin = Offset(padding.toFloat(), (180 + middle - paintedNaturalBaseline(layout, metrics)).toFloat())
            val units = dioramaUnits(line)
            val ownership = dioramaShapedColumns(layout,units,max(1,ceil(advance).toInt())+padding*2,padding,checkpoint)
            val slices = units.mapIndexed { index, unit ->
                checkpoint()
                val range = layout.getPathForRange(unit.charStart, unit.charEnd).getBounds()
                val width = max(1, ceil(range.width.toDouble()).toInt())
                val columns = ownership[index]
                // Integer texture crops keep the complete paragraph's raster
                // phase. Preserve the original unit pivot separately so soul
                // scaling does not pivot around an asymmetrical ink crop.
                val left = min(floor(range.left.toDouble()).toInt()-padding,
                    (columns.minOfOrNull { it.first } ?: padding)-padding)
                val right = max(ceil(range.right.toDouble()).toInt()+padding,
                    (columns.maxOfOrNull { it.last+1 } ?: padding)-padding)
                val image = ImageBitmap(max(1,right-left),360)
                val shift = -padding-left
                CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
                    for (column in columns) {
                        checkpoint()
                        clipRect((column.first+shift).toFloat(),0f,(column.last+1+shift).toFloat(),360f) {
                            // Draw the SAME complete shaped paragraph into the
                            // unit's integer-owned window. Do not first allocate a
                            // potentially enormous full-line bitmap: a long line
                            // of small units must not need one giant GPU texture.
                            // Only the integer raster origin changes, never the
                            // shaping run, selection ranges, unit pivot or clocks.
                            drawText(layout,Color.White,origin+Offset(shift.toFloat(),0f),shadow=Shadow.None)
                        }
                    }
                }
                val center = range.left + width / 2.0 - advance / 2
                DioramaActiveSlice(unit, image, center, range.left, range.right, prepareDioramaHalo(image,checkpoint),
                    left + image.width/2.0 - advance/2 - center)
            }
            checkpoint()
            return DioramaActiveText(slices, advance, middle)
        }
    }
}

/** Three's material channels are linear; encode exactly once for Compose. */
internal fun DioramaLinearColor.srgb(toneMapping: DioramaToneMapping = DioramaToneMapping.None): Color {
    val mapped=if(toneMapping==DioramaToneMapping.AcesFilmic)dioramaAcesFilmic(this)else this
    fun channel(v: Double) = (if (v < .0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - .055).toFloat()
    val r=channel(mapped.red);val g=channel(mapped.green);val b=channel(mapped.blue)
    // NoToneMapping may emit >1 before encoded fog. Preserve that input until
    // fog has mixed it; clamping first changes even an ordinary LDR framebuffer.
    // Existing in-gamut direct-Three controls retain identical byte packing.
    return Color(r,g,b,colorSpace=if(maxOf(r,g,b)>1f)ColorSpaces.ExtendedSrgb else ColorSpaces.Srgb)
}

/** Original pure-shadow ladder: shadowColor modulates source coverage BEFORE
 * the blur, rather than multiplying an already rounded 8-bit blur result. */
internal fun prepareDioramaHalo(body: ImageBitmap, checkpoint: () -> Unit = {}): ImageBitmap {
    checkpoint()
    val halo = ImageBitmap(body.width, body.height)
    val passes = listOf(10.24f to .95f, 10.24f to .85f, 20.48f to .55f, 29.696f to .3f)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(halo), Size(body.width.toFloat(), body.height.toFloat())) {
        for ((sigma, alpha) in passes) {
            checkpoint()
            val shadowSource = ImageBitmap(body.width, body.height)
            CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, Canvas(shadowSource), size) {
                drawImage(body, alpha = alpha)
            }
            val blur = blurPremultiplied(shadowSource, sigma)
            checkpoint()
            drawImage(blur.image, topLeft = blur.origin, blendMode = BlendMode.Plus)
        }
    }
    checkpoint()
    return halo
}
