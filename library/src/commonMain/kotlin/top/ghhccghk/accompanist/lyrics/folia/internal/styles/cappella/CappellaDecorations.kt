// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 CappellaBubbleParts.tsx / cappellaIntensity.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Adaptations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.math.*

internal data class CappellaPalette(val primary: Color = Color(0xffdfe5ec),
    val accent: Color = Color(0xfff17b4d), val secondary: Color = Color(0xffadb9c8),
    val background: Color = Color(0xff162435))
internal data class CappellaDecoration(val palette: CappellaPalette, val intensity: CappellaIntensity,
    val epoch: Double)
internal data class CappellaPlacement(val x: Double, val y: Double, val scale: Double = 1.0,
    val opacity: Double = 1.0)
internal data class CappellaEffects(val glowOpacity: Double, val glowDuration: Double,
    val rightAlpha: Double, val leftAlpha: Double, val shadowAlpha: Double)
internal fun cappellaEffects(intensity: CappellaIntensity) = when(intensity) {
    CappellaIntensity.CALM -> CappellaEffects(.26,2.2,.26,.14,.24)
    CappellaIntensity.NORMAL -> CappellaEffects(.4,1.8,.34,.18,.34)
    CappellaIntensity.CHAOTIC -> CappellaEffects(.52,1.35,.42,.24,.42)
}

/** One media phase; no wall-clock animation or frame accumulation. */
internal fun cappellaSweepPhase(time: Double, epoch: Double, duration: Double): Double =
    ((time-epoch).coerceAtLeast(0.0)/duration).let { it-floor(it) }
internal fun cappellaTimestamp(seconds: Double): String {
    if(!seconds.isFinite() || seconds<0) return "0:00"
    val total=floor(seconds).toLong()
    return "${total/60}:${(total%60).toString().padStart(2,'0')}"
}
internal fun cappellaOutline(w: Float, h: Float, d: Float, right: Boolean, inset: Float = 0f): Path =
    Path().apply { val r=max(0f,24*d-inset);val small=max(0f,6*d-inset)
        addRoundRect(RoundRect(Rect(inset,inset,w-inset,h-inset),CornerRadius(r),CornerRadius(r),
            CornerRadius(if(right)small else r),CornerRadius(if(right)r else small))) }

internal fun DrawScope.withCappellaPlacement(placement: CappellaPlacement?, block: DrawScope.()->Unit) {
    if(placement==null){block();return}
    val alpha=placement.opacity.toFloat().coerceIn(0f,1f)
    val d=density
    // A real group alpha, not multiplication of each overlapping draw operation.
    if(alpha<1f) drawContext.canvas.saveLayer(Rect(Offset.Zero,size),Paint().apply{this.alpha=alpha})
    try { withTransform({translate((placement.x*d).toFloat(),(placement.y*d).toFloat())
        scale(placement.scale.toFloat(),placement.scale.toFloat(),Offset.Zero)}) { block() }
    } finally { if(alpha<1f)drawContext.canvas.restore() }
}

internal data class CappellaShadow(val image: ImageBitmap, val origin: Offset, val color: Color)
/** CSS outer shadows: spread precedes blur, then knock out the original border box.
 * Masks are prepared when a target size changes, never once per animation frame. */
internal fun cappellaShadows(width: Double,height: Double,density: Density,right: Boolean,
    active: Boolean,decoration: CappellaDecoration?,circle: Boolean = false): List<CappellaShadow> {
    if(decoration==null)return emptyList()
    val d=density.density
    val specs=if(active)listOf(Triple(18f,48f,0f))else listOf(Triple(10f,15f,-3f),Triple(4f,6f,-4f))
    return specs.map { (offset,blur,spread)->
        val sigma=blur*d/2;val pad=ceil(3*sigma+abs(spread*d)).toInt()+2
        val w=(width*d).toFloat();val h=(height*d).toFloat()
        val bitmap=ImageBitmap(ceil(w+2*pad).toInt(),ceil(h+2*pad).toInt())
        CanvasDrawScope().draw(density,LayoutDirection.Ltr,Canvas(bitmap),Size(bitmap.width.toFloat(),bitmap.height.toFloat())) {
            withTransform({translate(pad.toFloat(),pad.toFloat())}) {
                if(circle)drawCircle(Color.White,radius=w/2+spread*d,center=Offset(w/2,h/2))
                else drawPath(cappellaOutline(w,h,d,right,-spread*d),Color.White)
            }
        }
        val blurred=blurPremultiplied(bitmap,sigma)
        val color=if(active)cappellaMix(decoration.palette.background,decoration.palette.accent,.2,
            cappellaEffects(decoration.intensity).shadowAlpha)else Color.Black.copy(alpha=.1f)
        CappellaShadow(blurred.image,blurred.origin+Offset(-pad.toFloat(),-pad+offset*d),color)
    }
}
internal fun DrawScope.drawCappellaShadows(shadows: List<CappellaShadow>,outline: Path) {
    clipPath(outline,ClipOp.Difference) { shadows.forEach { drawImage(it.image,it.origin,colorFilter=ColorFilter.tint(it.color)) } }
}
/** CSS 105deg duplicated sweep, evaluated in the full 200% gradient box. */
internal fun DrawScope.drawCappellaSweep(w:Float,h:Float,time:Double,right:Boolean,
    decoration:CappellaDecoration,reducedMotion:Boolean) {
    val effects=cappellaEffects(decoration.intensity)
    val phase=if(reducedMotion)0.0 else cappellaSweepPhase(time,decoration.epoch,effects.glowDuration)
    val width=w*2;val theta=105*PI/180
    val dx=sin(theta).toFloat();val dy=-cos(theta).toFloat()
    val length=abs(width*dx)+abs(h*dy)
    val center=Offset(width/2-(phase*w).toFloat(),h/2)
    val color=Color.White.copy(alpha=((if(right)effects.rightAlpha else effects.leftAlpha)*effects.glowOpacity).toFloat())
    drawRect(Brush.linearGradient(0f to Color.Transparent,.23f to color,.34f to Color.Transparent,
        .5f to Color.Transparent,.73f to color,.84f to Color.Transparent,1f to Color.Transparent,
        start=center-Offset(dx,dy)*(length/2),end=center+Offset(dx,dy)*(length/2)),size=Size(w,h))
}
