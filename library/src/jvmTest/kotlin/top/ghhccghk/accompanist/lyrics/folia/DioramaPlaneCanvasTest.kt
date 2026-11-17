package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import org.jetbrains.skia.Image
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Same already-shaped source texture isolates true perspective raster support.
 * It deliberately does not stand in for Kotlin text/CameraRig/depth acceptance. */
@OptIn(ExperimentalTestApi::class)
class DioramaPlaneCanvasTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    @Test fun fractional()=capture(801)
    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600){
        val rows=File("../verification/diorama-fixtures/planes.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}
            .map{it.split('\t')}.filter{it[1].toInt()==width}
        val image=Image.makeFromEncoded(File("../verification/diorama-fixtures/plane-raster.png").readBytes()).toComposeImageBitmap()
        var current by mutableIntStateOf(0)
        fun matrix(r:List<String>):Matrix{
            fun vec(i:Int)=DioramaVec(r[i].toDouble(),r[i+1].toDouble(),r[i+2].toDouble())
            val view=DioramaPerspective(vec(3),vec(6),width.toDouble(),600.0)
            val x=vec(12);val y=vec(15)
            // Texture pixels have downward Y; the real plane has upward Y.
            val origin=vec(9)-x*(image.width/2.0)+y*(image.height/2.0)
            val h=view.planeHomography(origin,x,y*(-1.0))
            val m=Matrix()
            m[0,0]=h[0].toFloat();m[1,0]=h[1].toFloat();m[3,0]=h[2].toFloat()
            m[0,1]=h[3].toFloat();m[1,1]=h[4].toFloat();m[3,1]=h[5].toFloat()
            m[0,3]=h[6].toFloat();m[1,3]=h[7].toFloat();m[3,3]=h[8].toFloat()
            for((px,py)in listOf(0.0 to 0.0,image.width.toDouble() to image.height.toDouble())){
                val expected=view.project(origin+x*px-y*py);assertFalse(expected.clipped)
                val actual=m.map(Offset(px.toFloat(),py.toFloat()))
                assertEquals(expected.x,actual.x.toDouble(),.001);assertEquals(expected.y,actual.y.toDouble(),.001)
            }
            return m
        }
        setContent{Canvas(Modifier.fillMaxSize().background(Color(0xff162435))){
            withTransform({transform(matrix(rows[current]))}){drawImage(image)}
        }}
        val out=File("../build/verification/diorama-projection-kotlin").apply{mkdirs()}
        fun pixels():IntArray{val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val hashes=mutableMapOf<Int,IntArray>()
        for(i in rows.indices){runOnIdle{current=i};waitForIdle();hashes[i]=pixels()
            assertTrue(hashes[i]!!.any{it!=Color(0xff162435).toArgb()},"perspective plane must draw visible text")
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(rows[i][0]))
        }
        for(i in rows.indices.reversed()){runOnIdle{current=i};waitForIdle();assertTrue(hashes[i]!!.contentEquals(pixels()))}
    }
}
