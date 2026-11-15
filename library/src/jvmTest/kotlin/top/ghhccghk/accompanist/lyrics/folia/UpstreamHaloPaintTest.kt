package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import androidx.compose.ui.graphics.ImageBitmap
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

/** Reuse the browser glyph coverage to isolate shadows from platform font rasterization. */
class UpstreamHaloPaintTest {
    @Test fun opacityAppliesToCombinedShadows() {
        val fixtures=File("../verification/classic-fixtures/halo")
        val source=ImageIO.read(File(fixtures,"shadow-source.png"))
        val width=source.width; val height=source.height
        val count=width*height
        val input=source.getRGB(0,0,width,height,null,0,width)
        val masks=blurredMasks(bitmapFromArgb(width,height,input),intArrayOf(10,20))
        fun alpha(image:ImageBitmap)=IntArray(count).also {image.readPixels(it)}.map {it ushr 24}
        val inner=alpha(masks[0]); val outer=alpha(masks[1]); val combined=alpha(compositeGlowMasks(masks))
        combined.forEachIndexed {i,a ->
            assertEquals(255-(255-inner[i])*(255-outer[i])/255.0,a.toDouble(),1.0)
        }
        for ((index,radius) in listOf(20,40).withIndex()) {
            val reference=ImageIO.read(File(fixtures,"single-$radius.png")).getRGB(0,0,width,height,null,0,width).map {it ushr 24}
            val actual=alpha(masks[index])
            val errors=actual.indices.filter {actual[it]>0 || reference[it]>0}.map {abs(actual[it]-reference[it])}
            assertTrue(errors.average()<.01,"Isolated $radius px shadow MAE: ${errors.average()}")
            assertTrue(errors.max()<=1,"Isolated shadow maximum coverage error")
        }
        val report=StringBuilder("opacity\toldForegroundMAE\tcombinedForegroundMAE\tcombinedMaxError\n")
        for (opacity in listOf(1.0,.5,.1)) {
            val reference=ImageIO.read(File(fixtures,"halo-$opacity.png".replace("1.0","1")))
                .getRGB(0,0,width,height,null,0,width).map {it ushr 24}
            val old=IntArray(count) {i -> (255-(255-inner[i]*opacity)*(255-outer[i]*opacity)/255).roundToInt()}
            val actual=IntArray(count) {i -> (combined[i]*opacity).roundToInt()}
            val foreground=(0 until count).filter {reference[it]>0 || old[it]>0 || actual[it]>0}
            val oldError=foreground.map {abs(reference[it]-old[it])}.average()
            val error=foreground.map {abs(reference[it]-actual[it])}.average()
            val maxError=foreground.maxOf {abs(reference[it]-actual[it])}
            report.append("$opacity\t$oldError\t$error\t$maxError\n")
            // Behavioral assertion: opacity covers the combined group, independently of blur accuracy.
            actual.forEachIndexed {i,a -> assertEquals(combined[i]*opacity,a.toDouble(),1.0)}
            if(opacity==1.0) assertTrue(error<.01,"Original combined halo foreground MAE: $error")
            if(opacity<1) assertTrue(error<oldError,"Group compositing must improve the actual original halo at opacity=$opacity")
        }
        File("../build/verification/halo-kotlin").apply {mkdirs()}.resolve("comparison.tsv").writeText(report.toString())
    }
}
