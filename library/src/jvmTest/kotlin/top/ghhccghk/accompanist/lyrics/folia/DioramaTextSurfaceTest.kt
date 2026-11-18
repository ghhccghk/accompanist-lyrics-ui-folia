// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.*
import java.io.File
import javax.imageio.ImageIO
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import kotlin.math.abs
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaTextSurfaceTest {
    @Test fun actualTransparentThreeRgbaThenCss()=runDesktopComposeUiTest(width=192,height=192) {
        val rows=File("../verification/diorama-fixtures/text-surface-v1/colors.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        assertEquals(108,rows.size)
        setContent { Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
            assertTrue(tryDioramaTextSurface {
                for((i,r) in rows.withIndex()) {
                    val color=DioramaLinearColor(r[0].toDouble(),r[1].toDouble(),r[2].toDouble()).srgb()
                    val p=Offset((i%12*16).toFloat(),(i/12*16).toFloat())
                    for(layer in 3..5)if(r[layer].toFloat()>0)drawRect(color,p,Size(16f,16f),alpha=r[layer].toFloat(),
                        blendMode=if(layer<5)BlendMode.Plus else BlendMode.SrcOver)
                }
            })
        } }
        waitForIdle();val image=captureToImage();val pixels=IntArray(192*192).also{image.readPixels(it)}
        var peak=0
        val report=rows.mapIndexed { i,r ->
            val argb=pixels[(i/12*16+8)*192+i%12*16+8]
            val native=listOf(argb shr 16 and 255,argb shr 8 and 255,argb and 255)
            val delta=native.mapIndexed{channel,v->abs(v-r[10+channel].toInt())}.max()
            peak=maxOf(peak,delta)
            (listOf(i)+r+native+delta).joinToString("\t")
        }
        val out=File("../build/verification/diorama-text-surface-kotlin").apply{mkdirs()}
        ImageIO.write(image.toAwtImage(),"png",out.resolve("colors.png"))
        out.resolve("colors.tsv").writeText("# case originalInputsRawRGBAAndCssRGB nativeRGB3 peak\n"+report.joinToString("\n")+"\n")
        out.resolve("numeric.txt").writeText("originalThreeCases=${rows.size} channelPeak=$peak gate=2\n")
        assertTrue(peak<=2,"Independent original transparent framebuffer/CSS gate2, got $peak")
    }

    @Test fun budgetExceptionClipAndMatrixRestoration() {
        assertEquals(800L*600*8,dioramaTextSurfaceBytes(Size(800f,600f)))
        assertEquals(32L*1024*1024,dioramaTextSurfaceBytes(Size(2048f,2048f)))
        for(size in listOf(Size.Zero,Size(Float.NaN,1f),Size(Float.POSITIVE_INFINITY,1f),
            Size(-1f,1f),Size(9000f,1f),Size(4096f,4096f)))assertNull(dioramaTextSurfaceBytes(size))
        val bitmap=ImageBitmap(8,8);val canvas=Canvas(bitmap);val native=canvas.skiaCanvas
        val count=native.saveCount;val matrix=native.localToDeviceAsMatrix33.mat.copyOf()
        val failure=IllegalStateException("Surface draw error")
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,canvas,Size(8f,8f)) {
            // CanvasDrawScope owns a save of its own. Check the surface against
            // the canvas on entry, and the outer caller again after scope exit.
            val scopeCount=native.saveCount
            val scopeMatrix=native.localToDeviceAsMatrix33.mat.copyOf()
            var calls=0
            assertSame(failure,assertFailsWith<IllegalStateException> {
                tryDioramaTextSurface {
                    calls++;drawContext.canvas.translate(2f,3f)
                    drawContext.canvas.clipRect(0f,0f,1f,1f)
                    drawContext.canvas.save();throw failure
                }
            })
            assertEquals(1,calls);assertEquals(scopeCount,native.saveCount)
            assertContentEquals(scopeMatrix,native.localToDeviceAsMatrix33.mat)
            drawRect(Color.Red)
            val pixels=IntArray(64).also{bitmap.readPixels(it)}
            assertTrue(pixels.all{it==Color.Red.toArgb()},"No clip/transform/surface survives failed draw")
            scale(1000f,1000f) {
                assertFalse(tryDioramaTextSurface{error("Device-space budget must reject host scaling")})
            }
            assertTrue(tryDioramaTextSurface{calls++})
            assertEquals(2,calls);assertEquals(scopeCount,native.saveCount)
        }
        assertEquals(count,native.saveCount)
        assertContentEquals(matrix,native.localToDeviceAsMatrix33.mat)
    }
}
