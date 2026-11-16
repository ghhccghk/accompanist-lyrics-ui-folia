// Folia classic adaptation: source ledger in docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic

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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.drawscope.clipPath
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.rememberClassicLayer
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.drawClassicLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.unit.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.key.*
import androidx.compose.ui.semantics.*
import top.ghhccghk.accompanist.lyrics.folia.*
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.blurredMasks
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.compositeGlowMasks
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.GlowBudget
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.GlowMaskPaint
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.BodyParagraphRaster
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.LocalClassicNativeBodyInk
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.drawParagraphShadowSource
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.hitClassicUnit
import kotlin.math.*

private data class GlyphGlow(val timing: TimedGrapheme, val origin: Offset, val mask: ImageBitmap)
private data class Cell(val unit: DisplayUnit, val layout: TextLayoutResult, val center: Offset, val isolatedCenter: Offset, val pose: Pose,
    val boxWidth: Float, val boxHeight: Float, val sourceBoundaries: IntArray, val paintTop: Float,
    val glows: List<GlyphGlow> = emptyList(), val body: BodyParagraphRaster? = null)
private data class Prepared(val cells: List<Cell>, val height: Float, val geometry: ClassicGeometry,
    val quality: FoliaQuality = FoliaQuality.Minimal)
private class DrawnFrame { var timeUs: Long? = null; var center = Offset.Zero; var contentOffset = Offset.Zero; var isolated = false }
internal fun classicBodyColor(a: Color, b: Color, q: Float): Color {
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
    val maskProcessor = top.ghhccghk.accompanist.lyrics.folia.LocalFoliaMaskProcessor.current
    val measurer = rememberTextMeasurer(cacheSize = 64)
    val units = remember(line) { displayUnits(line) }
    val density = LocalDensity.current
    val fontResolver = LocalFontFamilyResolver.current
    val viewport = LocalClassicViewport.current
    val nativeBodyInk = LocalClassicNativeBodyInk.current
    val currentPlayback by rememberUpdatedState(playback)
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthPx = with(density) { maxWidth.toPx() }.coerceAtLeast(1f)
        // useOverlapFit uses the actual padded area even in a short window;
        // its .5 font-scale floor, not an invented minimum area, limits shrink.
        val budgetPx = with(density) { heightBudget.toPx() }.coerceAtLeast(0f)
        val prepared = remember(units, textStyle, widthPx, budgetPx, density, tuning.avoidOverlap,
            tuning.wordSpacing, tuning.enableWordRotation, measurer, fontResolver, options.quality, options.animationIntensity, maskBudgetBytes, viewport, nativeBodyInk, colors.active.alpha, maskProcessor) {
            fun prepare(fontScale: Float): Prepared {
                val base=textStyle.fontSize.value
                fun cssRound(value:Float) = floor(value*1000+.5f)/1000
                // The pinned Chromium font cache uses hundredths of a CSS pixel for
                // glyph metrics, independently of the used CSS line-height precision.
                fun glyphSize(value:Float) = floor(value*100+.00001f)/100
                val rendered=if(base<=36f || base>=72f) cssRound(base/16*fontScale)*16 else cssRound(6*fontScale)*base/6
                val style = textStyle.copy(fontSize = glyphSize(rendered).sp,
                    // CSS word boxes have intrinsic fractional widths. A Compose
                    // paragraph's integer container must not right-align that
                    // width and add an extra fractional origin in RTL words.
                    lineHeight = TextUnit.Unspecified, textAlign = TextAlign.Left,
                    textDirection = classicTextDirection(textStyle, line.text),
                    textMotion = textStyle.textMotion ?: TextMotion.Animated)
                val renderedTexts = units.map { cssWordText(it.text) }
                val measured = renderedTexts.map { word -> measurer.measure(AnnotatedString(word.text), style,
                    softWrap = false) }
                // Paragraph height has already been rounded, so deriving descent
                // from it can move a fitted-size CSS baseline by one pixel.
                val fontMetrics = naturalFontMetrics(style, fontResolver, density)
                val measurementStyle=style.copy(fontSize=glyphSize(base*fontScale).sp)
                val rawWidths = units.map { unit -> measurer.measure(AnnotatedString(canvasWordText(unit.text)),measurementStyle,softWrap=false)
                    .let { it.getPathForRange(0,it.layoutInput.text.length).getBounds().width.toDouble() } }
                // CSS collapses trailing whitespace in the word's box, while its canvas spacing
                // measurement includes that whitespace. Keep both metrics, and the timed text.
                // Blink rounds an inline block's used width up to a LayoutUnit (1/64 CSS px).
                // Keep canvas spacing metrics unrounded; they serve a different purpose.
                val boxWidths = measured.map { layout ->
                    val width=layout.getPathForRange(0,layout.layoutInput.text.length).getBounds().width.toDouble()
                    ceil(width/density.density*64)/64*density.density
                }
                val interlude = line.text == "......"
                val poses = units.indices.map { wordPose(line.startUs, it, tuning.enableWordRotation, interlude, options.animationIntensity) }
                val rtl = isRtlLyric(line.text)
                val font = with(density) { (textStyle.fontSize*fontScale).toPx() }.toDouble()
                val lineFont = with(density) { rendered.sp.toPx() }.toDouble()
                val geometry = classicGeometry(rawWidths, boxWidths, font, widthPx.toDouble(), poses, line.startUs,
                    tuning.wordSpacing, tuning.avoidOverlap, rtl, density.density.toDouble(),lineFont,interlude,options.animationIntensity)
                val cells = units.indices.map { index ->
                    val box = geometry.boxes[index]
                    // CSS places the painted baseline. SkParagraph's reported
                    // baseline can differ from its rounded text-blob baseline.
                    // GlowWord is a will-change-transform layer: static layout offsets
                    // snap in CSS pixels before its continuous Motion transform.
                    // Keep geometry fractional for layout/avoidance measurements.
                    val left = viewport?.let { (it.width - widthPx) / 2 } ?: 0f
                    val top = viewport?.let { classicLineTopInStage(it.height, geometry.height, density.density) } ?: 0f
                    Cell(units[index], measured[index], Offset(classicWordPaintOffset(box.left, left, density.density) + box.width / 2,
                        classicWordPaintOffset(box.top, top, density.density) + box.height / 2),
                        Offset(classicWordPaintOffset(box.left, 0f, density.density) + box.width / 2,
                            classicWordPaintOffset(box.top, 0f, density.density) + box.height / 2),
                        poses[index], box.width, box.height, renderedTexts[index].boundaries,
                        cssFontBaseline(fontMetrics.ascent/density.density,
                            fontMetrics.descent/density.density,box.height/density.density)*density.density-paintedNaturalBaseline(measured[index],fontMetrics))
                }
                return Prepared(cells, geometry.height, geometry)
            }
            var result = prepare(1f)
            var fontScale = 1f
            while (tuning.avoidOverlap && fontScale > .5f) {
                // The source tests availableHeight / integer offsetHeight < 1.
                // A 76.046875px line has offsetHeight=76 and already fits 76px;
                // testing its fractional height would shrink one extra time.
                val offsetHeight=floor(result.height/density.density+.5f)*density.density
                val ratio=budgetPx / offsetHeight
                if (!(ratio < 1f)) break
                val next = (fontScale * ratio * .98f).coerceAtLeast(.5f)
                if (next >= fontScale - .005f) break
                fontScale = next
                result = prepare(fontScale)
            }
            result = result.copy(cells=result.cells.map {
                val bodyStyle=classicBodyRasterStyle(it.layout.layoutInput.style)
                val bodyLayout=if(bodyStyle==it.layout.layoutInput.style) it.layout
                    else measurer.measure(it.layout.layoutInput.text,bodyStyle,softWrap=false)
                it.copy(body=BodyParagraphRaster(bodyLayout,density,nativeBodyInk))
            })
            // Fixed, bounded masks. Their alpha changes in draw; glyph shadows never change per frame.
            val padding = ceil(72 * density.density).toInt()
            fun visibleTimings(cell: Cell) = cell.unit.graphemes.filter { cell.sourceBoundaries[it.end] > cell.sourceBoundaries[it.begin] }
            fun glyphPath(cell: Cell, timing: TimedGrapheme) = cell.layout.getPathForRange(cell.sourceBoundaries[timing.begin], cell.sourceBoundaries[timing.end])
            fun glyphBounds(cell: Cell, timing: TimedGrapheme) = glyphPath(cell, timing).getBounds()
            val bytesPerLayer = result.cells.sumOf { cell -> visibleTimings(cell).sumOf { timing ->
                val bounds = glyphBounds(cell, timing)
                (ceil(bounds.width).toLong() + 2L * padding + 1) * (ceil(bounds.height).toLong() + 2L * padding + 1) * 4L
            } }
            val layers = GlowBudget.layers(options.quality, bytesPerLayer, maskBudgetBytes)
            if (layers == 0) result
            else result.copy(quality = if (layers == 2) FoliaQuality.Standard else FoliaQuality.Reduced, cells = result.cells.map { cell ->
                val duration = line.endUs - line.startUs
                val radii = floatArrayOf(if (duration < 100_000) 14f else if (duration < 180_000) 18f else 20f,
                    if (duration < 100_000) 24f else if (duration < 180_000) 32f else 40f)
                cell.copy(glows = visibleTimings(cell).map { timing ->
                    val path = glyphPath(cell, timing)
                    val bounds = glyphBounds(cell, timing)
                    val origin = Offset(floor(bounds.left) - padding, floor(bounds.top) - padding)
                    val bitmap = ImageBitmap(ceil(bounds.right - origin.x + padding).toInt().coerceAtLeast(1),
                        ceil(bounds.bottom - origin.y + padding).toInt().coerceAtLeast(1))
                    CanvasDrawScope().draw(density, LayoutDirection.Ltr, BitmapCanvas(bitmap), Size(bitmap.width.toFloat(), bitmap.height.toFloat())) {
                        drawRect(Color.Transparent, blendMode = BlendMode.Clear)
                        withTransform({ translate(-origin.x, -origin.y) }) {
                            // Slice coverage from one fully shaped run; never reshape individual characters.
                            // A positive subpixel shadow selects the text-shadow glyph
                            // coverage, without visibly spreading it. Foreground AA has
                            // different coverage; keep the complete shaped paragraph.
                            clipPath(path) { drawParagraphShadowSource(cell.layout) }
                        }
                    }
                    val masks = blurredMasks(bitmap, FloatArray(layers) { maxOf(1f, radii[it] * density.density / 2) }, maskProcessor)
                    GlyphGlow(timing, origin, compositeGlowMasks(masks, colors.active.alpha))
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
            val body = sampleLine(time, enterUs, leaveUs, profile, options.reducedMotion, tuning.breathingFloatMultiplier, density.density, options.animationIntensity)
            if (body.alpha <= .001f) return false
            return prepared.cells.asReversed().any { cell ->
                val frame = sampleWord(time, enterUs, cell.unit.startUs, cell.unit.endUs, profile, cell.pose, options.reducedMotion, options.animationIntensity)
                val center = if(drawn.isolated) cell.isolatedCenter else cell.center
                hitClassicUnit(point, drawn.center, body.scale, body.breathingY, center + drawn.contentOffset, frame,
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
        // Ink alpha is already applied to each shadow before flattening. The
        // animated grapheme opacity below still applies to the whole pair.
        val glowPaint = remember(colors.active) { GlowMaskPaint(colors.active) }
        val bodyLayers = prepared.cells.map { rememberClassicLayer() }
        val wordLayers = prepared.cells.map { rememberClassicLayer() }
        val lineLayer = rememberClassicLayer()
        val linePadding = remember(prepared, density) {
            val reach = prepared.cells.maxOfOrNull { hypot(it.boxWidth,it.boxHeight)*it.pose.scale*.75f } ?: 0f
            ceil(reach+160*density.density).toInt()
        }
        Canvas(Modifier.fillMaxWidth().height(with(density) { prepared.height.toDp() }).then(input)) {
            val time = (currentPlayback().positionMillis - options.offsetMillis) * 1000
            val body = sampleLine(time, enterUs, leaveUs, profile, options.reducedMotion, tuning.breathingFloatMultiplier, density.density, options.animationIntensity)
            val enterDuration = when(profile.mode) {Reveal.Normal -> 300_000L; Reveal.Fast -> 160_000L; Reveal.Instant -> 0L}
            // transitionEnd changes blur(0px) to none. The former still creates
            // a raster surface; it is not interchangeable with no filter.
            val filterPresent = !options.reducedMotion && ((profile.mode != Reveal.Instant && motionElapsedUs(time-enterUs) <= enterDuration) || (leaveUs != null && time >= leaveUs))
            drawn.isolated = filterPresent || body.blur > 0f || (body.alpha > 0f && body.alpha < 1f)
            val rasterOffset = if(drawn.isolated && viewport != null) {
                fun snap(value:Float) = floor(value/density.density+.5f)*density.density
                Offset(snap((viewport.width-size.width)/2)-(viewport.width-size.width)/2,
                    snap(classicStageTop(viewport.height,density.density)) +
                        snap(classicLineTopInStage(viewport.height,prepared.height,density.density)) -
                        (viewport.height-size.height)/2)
            } else Offset.Zero
            val centerCorrection = viewport?.let {
                val stageTop = classicStageTop(it.height, density.density)
                val paintedStageTop = floor(stageTop / density.density + .5f) * density.density
                classicLineCenter(it.height, prepared.height, density.density) + paintedStageTop - stageTop - it.height / 2
            } ?: 0f
            drawn.timeUs = time
            drawn.contentOffset = if(drawn.isolated) rasterOffset else Offset(0f,(size.height - prepared.height) / 2 + centerCorrection)
            drawn.center = if(drawn.isolated) Offset(size.width/2,prepared.height/2)+rasterOffset else center+Offset(0f,centerCorrection)
            lineLayer.alpha = body.alpha
            lineLayer.blurSigma = body.blur
            lineLayer.forceOffscreen = filterPresent
            lineLayer.record(this,size=IntSize(size.width.toInt()+linePadding*2,size.height.toInt()+linePadding*2)) {
            withTransform({translate(linePadding.toFloat(),linePadding.toFloat())}) {
            withTransform({
                translate(0f, body.breathingY)
                scale(body.scale, body.scale, drawn.center-rasterOffset)
                translate(drawn.contentOffset.x-rasterOffset.x, drawn.contentOffset.y-rasterOffset.y)
            }) {
                prepared.cells.forEachIndexed { cellIndex, cell ->
                    val frame = sampleWord(time, enterUs, cell.unit.startUs, cell.unit.endUs,
                        profile, cell.pose, options.reducedMotion, options.animationIntensity)
                    val alpha = frame.alpha.coerceIn(0f, 1f)
                    if (alpha > .001f) withTransform({
                        val center = if(drawn.isolated) cell.isolatedCenter else cell.center
                        translate(center.x + frame.x * density.density, center.y + frame.y * density.density)
                        rotate(frame.rotation, Offset.Zero)
                        scale(frame.scale, frame.scale, Offset.Zero)
                    }) {
                        // Preserve the shaped paragraph; position its baseline using CSS rounded
                        // ascent/descent and floored half-leading, including negative leading.
                        val topLeft = Offset(-cell.boxWidth / 2f, -cell.boxHeight / 2f + cell.paintTop)
                        val wordLayer = wordLayers[cellIndex]
                        val wordPadding = ceil(96*density.density).toInt()
                        wordLayer.alpha = alpha
                        wordLayer.record(this,size=IntSize(cell.layout.size.width+wordPadding*2,cell.layout.size.height+wordPadding*2)) {
                        withTransform({translate(wordPadding.toFloat(),wordPadding.toFloat())}) {
                        cell.glows.forEach { glyph ->
                            val glow = sampleGraphemeGlow(time, enterUs, cell.unit.startUs, cell.unit.endUs,
                                glyph.timing.startUs, glyph.timing.endUs, cell.unit.graphemes.size, profile,
                                glyph.timing.startSeconds,glyph.timing.endSeconds)
                            if (glow > .001f) {
                                val origin = glyph.origin
                                glowPaint.draw(this, glyph.mask, origin, glow)
                            }
                        }
                        val color = classicBodyColor(colors.text, colors.active, frame.mix)
                        if(frame.blur > .01f) {
                            val layer = bodyLayers[cellIndex]
                            val padding = ceil(32*density.density).toInt()
                            layer.blurSigma = frame.blur*density.density
                            layer.record(this,size=IntSize(cell.layout.size.width+padding*2,cell.layout.size.height+padding*2)) {
                                cell.body!!.draw(this,color,Offset(padding.toFloat(),padding.toFloat()))
                            }
                            withTransform({translate(-padding.toFloat(),-padding.toFloat())}) {drawClassicLayer(layer)}
                        } else cell.body!!.draw(this,color,Offset.Zero)
                        }
                        }
                        withTransform({translate(topLeft.x-wordPadding,topLeft.y-wordPadding)}) {drawClassicLayer(wordLayer)}
                    }
                }
            }
            }
            }
            withTransform({translate(-linePadding+rasterOffset.x,-linePadding+rasterOffset.y)}) { drawClassicLayer(lineLayer) }
        }
    }
}
