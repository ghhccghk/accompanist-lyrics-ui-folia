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
    val rangeLeft: Float, val rangeRight: Float, val halo: ImageBitmap, val imageOffsetPx: Double = 0.0,
    val body:ImageBitmap=image)

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
            plane(slice, slice.body, state.baseOpacity, state.fill, sampleBase = true)
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
            // Existing host-preserving Windows foreground coverage is verified
            // for the source's independent CJK/kana/symbol display class. Keep
            // canonical grayscale input for blur and soul; never redraw glyphs
            // separately or change layout/ownership to select a raster policy.
            val bodyStyle=if(dioramaSeparateGlyphUnits(units))classicBodyRasterStyle(style)else style
            val bodyLayout=if(bodyStyle==style)layout else measurer.measure(AnnotatedString(text),bodyStyle,
                softWrap=false,density=density,layoutDirection=LayoutDirection.Ltr)
            val ownership = dioramaShapedColumns(layout,units,max(1,ceil(advance).toInt())+padding*2,padding,checkpoint)
            // The source anchors a ceil-sized RTL word from the RIGHT edge;
            // a left-anchored canonical crop does not have that raster phase.
            // Only safe whitespace-separated, single-direction RTL words use
            // that registration, still drawing this SAME complete paragraph.
            // A cross-unit cluster or mixed bidi must retain canonical column
            // ownership/phase; never reshape fragments or discard clocks.
            val direction=if(text.isNotEmpty())layout.getParagraphDirection(0)else ResolvedTextDirection.Ltr
            val sourceWords=direction==ResolvedTextDirection.Rtl&&units.isNotEmpty()&&units.all { unit ->
                (unit.charStart==0||text[unit.charStart-1].isWhitespace())&&
                    (unit.charEnd==text.length||text[unit.charEnd].isWhitespace())
            }&&text.indices.all{text[it].isWhitespace()||layout.getBidiRunDirection(it)==direction}
            fun measuredAdvance(value:String):Double {
                if(value.isEmpty())return 0.0
                val measured=measurer.measure(AnnotatedString(value),style,softWrap=false,
                    density=density,layoutDirection=LayoutDirection.Ltr)
                return measured.getPathForRange(0,value.length).getBounds().width.toDouble()
            }
            val slices = units.mapIndexed { index, unit ->
                checkpoint()
                val range = layout.getPathForRange(unit.charStart, unit.charEnd).getBounds()
                val width = max(1, ceil(if(sourceWords)measuredAdvance(unit.text)else range.width.toDouble()).toInt())
                val columns = ownership[index]
                // Canonical crops keep complete-run phase for cross-unit
                // clusters/mixed bidi. Safe RTL words use original right-edge
                // registration instead; neither branch shapes separate glyphs.
                val left = min(floor(range.left.toDouble()).toInt()-padding,
                    (columns.minOfOrNull { it.first } ?: padding)-padding)
                val right = max(ceil(range.right.toDouble()).toInt()+padding,
                    (columns.maxOfOrNull { it.last+1 } ?: padding)-padding)
                val image = ImageBitmap(if(sourceWords)width+padding*2 else max(1,right-left),360)
                val shift = if(sourceWords)-range.left else (-padding-left).toFloat()
                CanvasDrawScope().draw(density, LayoutDirection.Ltr, Canvas(image), Size(image.width.toFloat(), image.height.toFloat())) {
                    for (column in columns) {
                        checkpoint()
                        clipRect((column.first+shift).toFloat(),0f,(column.last+1+shift).toFloat(),360f) {
                            // Draw the SAME complete shaped paragraph into the
                            // unit's integer-owned window. Do not first allocate a
                            // potentially enormous full-line bitmap: a long line
                            // of small units must not need one giant GPU texture.
                            // Only raster placement changes, never the shaping
                            // run, selection ranges or internal clocks.
                            drawText(layout,Color.White,origin+Offset(shift.toFloat(),0f),shadow=Shadow.None)
                        }
                    }
                }
                val body=if(bodyLayout===layout)image else ImageBitmap(image.width,image.height).also { target ->
                    CanvasDrawScope().draw(density,LayoutDirection.Ltr,Canvas(target),Size(target.width.toFloat(),target.height.toFloat())) {
                        for(column in columns) {
                            checkpoint()
                            clipRect((column.first+shift).toFloat(),0f,(column.last+1+shift).toFloat(),360f) {
                                drawText(bodyLayout,Color.White,origin+Offset(shift.toFloat(),0f),shadow=Shadow.None)
                            }
                        }
                    }
                }
                val center = if(sourceWords) {
                    val start=measuredAdvance(text.substring(0,unit.charStart))+width/2.0
                    if(direction==ResolvedTextDirection.Rtl)advance/2-start else -advance/2+start
                }else range.left + width / 2.0 - advance / 2
                DioramaActiveSlice(unit, image, center, range.left, range.right, prepareDioramaHalo(image,checkpoint),
                    if(sourceWords)0.0 else left + image.width/2.0 - advance/2 - center,body)
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
