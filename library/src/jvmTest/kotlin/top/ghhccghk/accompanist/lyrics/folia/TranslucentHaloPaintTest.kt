package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.*

/** Actual original shadow targets; reuse its source coverage to isolate compositing. */
class TranslucentHaloPaintTest {
    @Test fun colorAlphaBelongsToEachShadowBeforeSpanOpacity() {
        val fixtures=File("../verification/classic-fixtures")
        val source=ImageIO.read(fixtures.resolve("halo/shadow-source.png"))
        val width=source.width;val height=source.height
        val bitmap=bitmapFromArgb(width,height,source.getRGB(0,0,width,height,null,0,width))
        val output=File("../build/verification/halo-translucent-kotlin").apply {mkdirs()}
        val report=StringBuilder("reveal,ink_alpha,span_opacity,foreground_pixels,before_alpha_mae,after_alpha_mae,before_max,after_max\n")
        val failures=mutableListOf<String>()
        fun pixels(image:ImageBitmap)=IntArray(width*height).also {image.readPixels(it)}
        fun paint(mask:ImageBitmap,ink:Color,opacity:Float,correct:Boolean=false)=ImageBitmap(width,height).also {image->
            CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(width.toFloat(),height.toFloat())) {
                if(correct) GlowMaskPaint(ink).draw(this,mask,Offset.Zero,opacity)
                else drawImage(mask,alpha=opacity,colorFilter=ColorFilter.tint(ink))
            }
        }
        for((reveal,radii) in listOf("normal" to intArrayOf(10,20),"fast" to intArrayOf(9,16),"instant" to intArrayOf(7,12))) {
            val masks=blurredMasks(bitmap,radii)
            val whitePair=compositeGlowMasks(masks)
            for(alpha in listOf(0,64,128,192,255)) for(opacity in listOf(1f,.5f,.1f)) {
                val ink=Color((alpha.toLong() shl 24) or 0x91e8e1)
                val name="$reveal-a$alpha-o${if(opacity==1f) "1" else opacity.toString()}.png"
                val expected=ImageIO.read(fixtures.resolve("halo-translucent/$name")).getRGB(0,0,width,height,null,0,width)
                val before=paint(whitePair,ink,opacity)
                val after=paint(compositeGlowMasks(masks,ink.alpha),ink,opacity,correct=true)
                val old=pixels(before);val actual=pixels(after)
                val selected=actual.indices.filter {(expected[it] ushr 24)>0||(old[it] ushr 24)>0||(actual[it] ushr 24)>0}
                fun error(values:IntArray)=selected.map {abs((expected[it] ushr 24)-(values[it] ushr 24))}
                val oldErrors=error(old);val newErrors=error(actual)
                val oldMean=oldErrors.average().takeUnless {it.isNaN()}?:0.0
                val newMean=newErrors.average().takeUnless {it.isNaN()}?:0.0
                report.append("$reveal,$alpha,$opacity,${selected.size},$oldMean,$newMean,${oldErrors.maxOrNull()?:0},${newErrors.maxOrNull()?:0}\n")
                ImageIO.write(before.toAwtImage(),"png",output.resolve("before-$name"))
                ImageIO.write(after.toAwtImage(),"png",output.resolve("after-$name"))
                if(reveal=="normal" && alpha==64 && opacity==.1f) {
                    // Reproduce the rejected intermediate path as a separate artifact:
                    // correct per-shadow ink, but span opacity folded into bitmap paint.
                    val rejected=paint(compositeGlowMasks(masks,ink.alpha),ink.copy(alpha=1f),opacity)
                    ImageIO.write(rejected.toAwtImage(),"png",output.resolve("rejected-direct-opacity-$name"))
                }
                if(alpha in 1..254) {
                    if(newMean>=oldMean) failures += "$name: $newMean vs $oldMean"
                } else assertContentEquals(old,actual,"Opaque/transparent behavior must remain exact: $name")
                if(alpha==0) assertTrue(actual.all {it ushr 24==0})
            }
        }
        output.resolve("comparison.csv").writeText(report.toString())
        assertTrue(failures.isEmpty(),"Original translucent shadow must improve: ${failures.joinToString()}")
    }

    @Test fun independentInkAndSpanAlphasHaveDifferentCompositionOrder() {
        val first=bitmapFromArgb(1,1,intArrayOf(0x80ffffff.toInt()))
        val second=bitmapFromArgb(1,1,intArrayOf(0x80ffffff.toInt()))
        fun alpha(mask:ImageBitmap)=IntArray(1).also {mask.readPixels(it)}.single() ushr 24
        // Two 50% masks carrying 50% ink: .25 + .25 * .75 = .4375.
        // Applying 50% only after flattening would instead yield .375.
        assertEquals(.4375*255,alpha(compositeGlowMasks(listOf(first,second),.5f)).toDouble(),1.0)
        assertEquals(64,alpha(compositeGlowMasks(listOf(first),.5f)))
        assertSame(first,compositeGlowMasks(listOf(first)))
        assertFailsWith<IllegalArgumentException> {compositeGlowMasks(listOf(first),Float.NaN)}
        assertFailsWith<IllegalArgumentException> {compositeGlowMasks(listOf(first),-1f)}
    }
}
