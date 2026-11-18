package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.*
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.ColorAlphaType
import kotlin.math.max
import kotlin.test.*

/** Input diagnostic only: real original GPU mip texels are compared externally
 * with the rejected premultiplied-mask reduction, not fed to production. */
@OptIn(ExperimentalTestApi::class)
class DioramaMipRasterTest {
    @Test fun sharedInputPremultipliedPyramid()=runDesktopComposeUiTest(width=80,height=80) {
        val bytes=File("../verification/diorama-fixtures/mounted-raster-v1/cache-7.png").readBytes()
        assertEquals("be0bf017e888adb86c6cd22e490704c8d3e24e758456a8eec3b1141fbf912ef9",
            MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)})
        val out=File("../build/verification/diorama-mounted-mips-kotlin").apply{mkdirs()}
        var image=Image.makeFromEncoded(bytes).toComposeImageBitmap()
        val pixels=IntArray(image.width*image.height).also{image.readPixels(it)}
        // Separate OPAQUE color occupancy from alpha, instead of erasing
        // original transparent-texel RGB through premultiplied mask storage.
        val occupancy=ByteArray(pixels.size*4)
        for(i in pixels.indices) {
            val value=if(pixels[i] ushr 24>0)255 else 0
            occupancy[i*4]=value.toByte();occupancy[i*4+1]=value.toByte();occupancy[i*4+2]=value.toByte();occupancy[i*4+3]=255.toByte()
        }
        var color=Image.makeRaster(ImageInfo.makeS32(image.width,image.height,ColorAlphaType.OPAQUE),occupancy,image.width*4).toComposeImageBitmap()
        var linearColor=ImageBitmap(image.width,image.height,config=ImageBitmapConfig.F16,colorSpace=ColorSpaces.LinearSrgb)
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(linearColor),Size(image.width.toFloat(),image.height.toFloat())) {
            drawImage(color)
        }
        for(level in 0..5) {
            assertEquals(max(1,1650 shr level),image.width)
            assertEquals(max(1,232 shr level),image.height)
            if(level in listOf(0,4,5))ImageIO.write(image.toAwtImage(),"png",out.resolve("mip-$level.png"))
            if(level in listOf(0,4,5))ImageIO.write(color.toAwtImage(),"png",out.resolve("occupancy-$level.png"))
            if(level in listOf(0,4,5))ImageIO.write(linearColor.toAwtImage(),"png",out.resolve("linear-occupancy-$level.png"))
            if(level<5) {
                val previous=image;val width=max(1,image.width/2);val height=max(1,image.height/2)
                image=ImageBitmap(width,height)
                CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(image),Size(width.toFloat(),height.toFloat())) {
                    drawImage(previous,dstSize=IntSize(width,height),filterQuality=FilterQuality.Low)
                }
                val oldColor=color;color=ImageBitmap(width,height)
                CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(color),Size(width.toFloat(),height.toFloat())) {
                    drawImage(oldColor,dstSize=IntSize(width,height),filterQuality=FilterQuality.Low)
                }
                val oldLinear=linearColor;linearColor=ImageBitmap(width,height,config=ImageBitmapConfig.F16,colorSpace=ColorSpaces.LinearSrgb)
                CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(linearColor),Size(width.toFloat(),height.toFloat())) {
                    drawImage(oldLinear,dstSize=IntSize(width,height),filterQuality=FilterQuality.Low)
                }
            }
        }
    }
}
