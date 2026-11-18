package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.*

/** Expected bytes come from actual locked Three framebuffer reads, not a
 * second implementation of the ACES formula. All original 960 cases retained. */
@OptIn(ExperimentalTestApi::class)
class DioramaToneMappingTest {
    @Test fun actualShaderColorsFogAndBlending() = runDesktopComposeUiTest(width=256,height=240) {
        val rows=File("../verification/diorama-fixtures/tonemapping-v1/colors.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
        assertEquals(960,rows.size)
        val white=ImageBitmap(8,8).also {
            Canvas(it).drawRect(Rect(0f,0f,8f,8f),Paint().apply { color=Color.White })
        }
        val fog=DioramaFog(Color(0xff162435))
        val view=DioramaPerspective(DioramaVec(0.0,0.0,0.0),DioramaVec(0.0,0.0,-1.0),256.0,240.0)
        setContent { Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
            for((i,r) in rows.withIndex()) {
                val color=DioramaLinearColor(r[1].toDouble(),r[2].toDouble(),r[3].toDouble())
                    .srgb(DioramaToneMapping.valueOf(r[0]))
                translate((i%32*8).toFloat(),(i/32*8).toFloat()) {
                    drawDioramaFogMask(white,view,DioramaVec(0.0,0.0,-r[6].toDouble()),
                        DioramaVec(1.0,0.0,0.0),DioramaVec(0.0,1.0,0.0),r[4].toFloat(),color,r[5]=="1",fog)
                }
            }
        } }
        waitForIdle()
        val image=captureToImage()
        val pixels=IntArray(image.width*image.height).also { image.readPixels(it) }
        var opaquePeak=0;var fullPeak=0
        val report=rows.mapIndexed { i,r ->
            val argb=pixels[(i/32*8+4)*image.width+i%32*8+4]
            val rgb=listOf(argb shr 16 and 255,argb shr 8 and 255,argb and 255)
            val delta=rgb.mapIndexed { channel,v -> abs(v-r[7+channel].toInt()) }.max()
            fullPeak=maxOf(fullPeak,delta)
            if(r[4]=="1" && r[5]=="0" && r[6]=="6")opaquePeak=maxOf(opaquePeak,delta)
            (listOf(i)+r.take(7)+r.drop(7).take(3)+rgb+delta).joinToString("\t")
        }
        val out=File("../build/verification/diorama-tonemapping-kotlin").apply { mkdirs() }
        ImageIO.write(image.toAwtImage(),"png",out.resolve("colors.png"))
        out.resolve("comparison.tsv").writeText("# index mapping RGB3 alpha additive depth originalRGB3 nativeRGB3 peak\n"+report.joinToString("\n")+"\n")
        out.resolve("numeric.txt").writeText("originalShaderCases=${rows.size} opaquePeak=$opaquePeak gate=1 fullBlendFogPeak=$fullPeak gate=2\n")
        assertTrue(opaquePeak<=1,"Opaque ACES/None color rounding: $opaquePeak >1 byte")
        assertTrue(fullPeak<=2,"Full native normal/additive/fog: $fullPeak >2 bytes")
    }
}
