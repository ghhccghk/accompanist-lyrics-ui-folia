// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 cappellaBubbleMetrics.ts, CappellaBubbleParts.tsx (text/frame).
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

internal data class CappellaBubbleSize(val width: Double, val height: Double)
internal data class CappellaBubbleStyle(val fontPx: Double = 24.12, val maxTextWidth: Double = 220.0,
    val paddingX: Double = 20.0, val paddingY: Double = 16.0, val minHeight: Double = 68.974,
    val minWidth: Double = 72.0)
internal data class CappellaBubbleColors(val background: Color, val border: Color, val text: Color)

/** Source mixColors mixes integer sRGB channels (not Compose's perceptual lerp). */
internal fun cappellaMix(a: Color, b: Color, ratio: Double, alpha: Double): Color {
    val x = a.toArgb(); val y = b.toArgb()
    fun channel(shift: Int): Int = floor(((x ushr shift) and 255) +
        (((y ushr shift) and 255) - ((x ushr shift) and 255)) * ratio + .5).toInt().coerceIn(0,255)
    return Color(channel(16), channel(8), channel(0), floor(alpha * 255 + .5).toInt())
}
internal fun cappellaBubbleColors(sender: CappellaSender, primary: Color, accent: Color,
    secondary: Color, background: Color): CappellaBubbleColors {
    if (sender.side == CappellaSide.RIGHT) return CappellaBubbleColors(cappellaMix(accent,primary,.18,.94),
        cappellaMix(accent,primary,.34,.3),background)
    val tone = (sender.avatar % 9) / 8.0
    val amount = .18 + tone * .62
    return CappellaBubbleColors(cappellaMix(secondary,accent,amount,1.0),
        cappellaMix(secondary,accent,min(amount + .18,1.0),.26),primary)
}

/** Prepared sizes only retain dimensions. Paragraph objects are not cached for
 * every prefix (which would retain quadratic shaped text). */
internal class CappellaBubbleMeasure(private val measurer: TextMeasurer, base: TextStyle,
    private val density: Density, val config: CappellaBubbleStyle,
    private val resolver: androidx.compose.ui.text.font.FontFamily.Resolver) {
    val lineHeight = config.fontPx * 1.45
    val style = base.copy(fontSize = (floor(config.fontPx * 100 + 1e-9) / 100).sp,
        fontWeight = base.fontWeight ?: FontWeight.Normal, textAlign = TextAlign.Left,
        textDirection = base.textDirection.takeUnless { it == TextDirection.Unspecified } ?: TextDirection.Ltr,
        lineHeight = TextUnit.Unspecified, textMotion = TextMotion.Animated)

    fun layout(text: String, width: Double = config.maxTextWidth): TextLayoutResult {
        val safe = text.ifEmpty { " " }
        return measurer.measure(safe, style, constraints = Constraints(maxWidth =
            ceil(width * density.density).toInt().coerceAtLeast(1)))
    }
    private fun textBox(text: String): CappellaBubbleSize {
        val result = layout(text)
        val width = (0 until result.lineCount).maxOfOrNull {
            val start=result.getLineStart(it)
            val end=result.getLineEnd(it).let { e -> if(e>start&&text.getOrNull(e-1)=='\n')e-1 else e }
            val row=text.ifEmpty{" "}.substring(start,end)
            // pretext's pre-wrap metrics retain a trailing space's advance.
            // Native getLineRight hangs it; selection coverage preserves it.
            // Source sums segment Canvas advances in double precision. Measuring
            // the entire line in one Float loses enough precision to cross ceil
            // at integer boundaries (e.g. the hyphenated 16.56px chat fixture).
            val segments=mutableListOf<String>()
            for(s in segmentWords(row)) {
                if(s.text=="-"&&segments.isNotEmpty())segments[segments.lastIndex]+=s.text else segments+=s.text
            }
            segments.sumOf { text->val natural=measurer.measure(text,style,softWrap=false)
                cappellaCanvasAdvance(text,style,resolver,density)?:
                    (natural.getPathForRange(0,text.length).getBounds().width/density.density.toDouble()) }
        } ?: 0.0
        return CappellaBubbleSize(width,max(result.lineCount,1)*lineHeight)
    }
    fun size(text: String): CappellaBubbleSize = textBox(text).let {
        CappellaBubbleSize(ceil(min(max(it.width,config.fontPx),config.maxTextWidth)+config.paddingX*2+2),
            ceil(it.height+config.paddingY*2+2))
    }
    /** The title frame uses DOM fit-content/auto instead of the pretext ceil table. */
    fun titleSize(text: String): CappellaBubbleSize = textBox(text).let {
        CappellaBubbleSize(ceil((min(it.width,config.maxTextWidth)+config.paddingX*2+2)*64)/64,
            floor((it.height+config.paddingY*2+2)*64+1e-9)/64)
    }
    fun sizes(plan: CappellaRevealPlan): List<CappellaBubbleSize> {
        val edges = graphemeBoundaries(plan.characters.joinToString("")).sorted()
        val text = plan.characters.joinToString("")
        return edges.map { size(text.substring(0,it)) }
    }
}

internal data class CappellaBubblePrepared(val sizes: List<CappellaBubbleSize>, val plan: CappellaRevealPlan)

/** Isolated actual chat bubble. Geometry is source target geometry: the .2s
 * frame resize transition, row springs, glow sweep and message-stack movement
 * are intentionally NOT claimed by this primitive's calibration. */
@Composable
internal fun CappellaBubbleTarget(line: CappellaLine, time: () -> Double, textStyle: TextStyle,
    colors: CappellaBubbleColors, modifier: Modifier = Modifier,
    config: CappellaBubbleStyle = CappellaBubbleStyle(), right: Boolean = false,
    active: Boolean = true, reducedMotion: Boolean = false,
    sizeOverride: CappellaBubbleSize? = null,
    placement: CappellaPlacement? = null, decoration: CappellaDecoration? = null,
    resizeFrom: Double? = null,
    onPrepared: (CappellaBubblePrepared) -> Unit = {}) {
    val density = LocalDensity.current; val resolver = LocalFontFamilyResolver.current
    val measurer = rememberTextMeasurer(cacheSize = 32)
    val measure = remember(textStyle,config,density,resolver) { CappellaBubbleMeasure(measurer,textStyle,density,config,resolver) }
    val plan = remember(line) { CappellaRevealPlan(line) }
    val sizes = remember(measure,plan) { measure.sizes(plan) }
    val observer by rememberUpdatedState(onPrepared)
    LaunchedEffect(plan,sizes) { observer(CappellaBubblePrepared(sizes,plan)) }
    val clock by rememberUpdatedState(time)
    val counts by remember(plan,active) { derivedStateOf {
        if(active) clock().let { plan.visibleCount(it) to plan.bubbleCount(it) }
        else plan.characters.size to plan.characters.size
    } }
    val (count,target) = counts
    val resize=remember(plan,sizes,resizeFrom) { resizeFrom?.let{CappellaBubbleResize(plan,sizes,it)} }
    val box = sizeOverride ?: resize?.at(clock()) ?: sizes[target.coerceIn(sizes.indices)]
    val width = max(box.width,config.minWidth)
    val height = max(box.height,config.minHeight)
    val shown = plan.characters.take(count).joinToString("")
    val layout = remember(shown,width,measure) { measure.layout(shown,
        max(1.0,width - config.paddingX * 2 - 2)) }
    val metrics = remember(measure,resolver,density) { naturalFontMetrics(measure.style,resolver,density) }
    // CSS line boxes use unrounded line-height; the platform shaping layout is
    // measured whole, then each complete row is shifted to its natural baseline.
    val rows = remember(layout,shown,measure) {
        (0 until layout.lineCount).map { i ->
            val start = layout.getLineStart(i)
            val end = layout.getLineEnd(i).let { if (it > start && shown.getOrNull(it-1) == '\n') it-1 else it }
            Triple(start,end,measurer.measure(shown.substring(start,end).ifEmpty { " " },measure.style,softWrap=false))
        }
    }
    val edges = remember(shown) { graphemeBoundaries(shown).sorted() }
    val ink = remember(rows,edges,active,reducedMotion) {
        if(!active||reducedMotion)emptyList() else rows.map { (start,end,paragraph) ->
            cappellaInkRuns(paragraph,edges.zipWithNext().mapIndexedNotNull { i,(a,b) ->
                if(a<start||a>=end)null else CappellaInkGlyph(i,a-start,min(b,end)-start)
            },cappellaInkClusters(paragraph.layoutInput.text.text,measure.style,resolver,density))
        }
    }
    val shadows=remember(width,height,density,right,active,decoration) {
        cappellaShadows(width,height,density,right,active,decoration)
    }
    Canvas(modifier) {
      withCappellaPlacement(placement) {
        val t = clock()
        val d = density.density
        val w = (width*d).toFloat(); val h = (height*d).toFloat()
        val radius = 24f*d; val small = 6f*d
        val outline = Path().apply { addRoundRect(RoundRect(Rect(0f,0f,w,h),
            CornerRadius(radius),CornerRadius(radius),CornerRadius(if(right)small else radius),
            CornerRadius(if(right)radius else small))) }
        drawCappellaShadows(shadows,outline)
        drawPath(outline,colors.background)
        // CSS borders are inset, so the stroke's center is half a pixel inside.
        val border = Path().apply { addRoundRect(RoundRect(Rect(.5f*d,.5f*d,w-.5f*d,h-.5f*d),
            CornerRadius(radius-.5f*d),CornerRadius(radius-.5f*d),
            CornerRadius((if(right)small else radius)-.5f*d),CornerRadius((if(right)radius else small)-.5f*d))) }
        drawPath(border,colors.border,style=Stroke(d))
        clipPath(outline) {
            if(active&&decoration!=null)drawCappellaSweep(w,h,t,right,decoration,reducedMotion)
            for ((row, entry) in rows.withIndex()) {
                val (_,_,paragraph) = entry
                val base = cssFontBaseline(metrics.ascent,metrics.descent,(measure.lineHeight*d).toFloat())
                val top = ((config.paddingY+1+row*measure.lineHeight)*d).toFloat() +
                    base-paintedNaturalBaseline(paragraph,metrics)
                val origin = Offset(((config.paddingX+1)*d).toFloat(),top)
                // Original CappellaText returns the complete text node for
                // passed messages. Caret selections are not glyph ink masks:
                // clipping each EGC drops overhang and can blend a ligature or
                // bidi run twice even when every character is fully opaque.
                // Reduced motion has no character fade and uses the same row.
                if (!active || reducedMotion) {
                    drawText(paragraph,colors.text,topLeft=origin,shadow=Shadow.None)
                    continue
                }
                val runs=ink[row]
                val alphas=runs.map{cappellaEaseOut(plan.progress(it.index,t)).toFloat()}
                drawCappellaInk(paragraph,runs,alphas,colors.text,origin)
            }
        }
      }
    }
}

/** CSS ease-out = cubic-bezier(0,0,.58,1); solve x(u), then y(u). */
internal fun cappellaEaseOut(progress: Double): Double {
    if (progress <= 0) return 0.0
    if (progress >= 1) return 1.0
    var lo=0.0; var hi=1.0
    repeat(40) { val u=(lo+hi)/2;val x=3*(1-u)*u*u*.58+u*u*u
        if(x<progress)lo=u else hi=u }
    val u=(lo+hi)/2
    return 3*(1-u)*u*u+u*u*u
}
