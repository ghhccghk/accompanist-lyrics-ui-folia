package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import org.jetbrains.skia.Image
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaClipCanvasTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        val rows=File("../verification/diorama-fixtures/clipping-v1/planes.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[1].toInt()==width}
        val texture=Image.makeFromEncoded(File("../verification/diorama-fixtures/plane-raster.png").readBytes()).toComposeImageBitmap()
        var current by mutableIntStateOf(0);var legacy by mutableStateOf(true)
        setContent {Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
            val r=rows[current]
            fun v(i:Int)=DioramaVec(r[i].toDouble(),r[i+1].toDouble(),r[i+2].toDouble())
            val view=DioramaPerspective(DioramaVec(0.0,0.0,0.0),DioramaVec(0.0,0.0,-1.0),width.toDouble(),600.0,near=.5,far=2.0)
            val origin=v(2);val x=v(5);val y=v(8)
            assertEquals(texture.width,r[11].toInt());assertEquals(texture.height,r[12].toInt())
            if(legacy) {
                // Exact old unbounded image path, kept ONLY in verification for
                // the paired failure evidence. No production feature switch.
                val h=view.planeHomography(origin,x,y)
                val m=Matrix().apply {
                    this[0,0]=h[0].toFloat();this[1,0]=h[1].toFloat();this[3,0]=h[2].toFloat()
                    this[0,1]=h[3].toFloat();this[1,1]=h[4].toFloat();this[3,1]=h[5].toFloat()
                    this[0,3]=h[6].toFloat();this[1,3]=h[7].toFloat();this[3,3]=h[8].toFloat()
                }
                withTransform({transform(m)}){drawImage(texture,colorFilter=ColorFilter.tint(Color.White))}
            }else drawDioramaPlane(texture,view,origin,x,y)
        }}
        val out=File("../build/verification/diorama-clipping-kotlin").apply{mkdirs()}
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val snapshots=mutableMapOf<Int,IntArray>()
        for(i in rows.indices) {
            runOnIdle{current=i;legacy=true};pixels()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("before-${rows[i][0]}"))
            runOnIdle{legacy=false};val p=pixels();snapshots[i]=p
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("after-${rows[i][0]}"))
            if(listOf("before-near","beyond-far","backface").any{rows[i][0].startsWith(it)})
                assertEquals(setOf(Color(0xff162435).toArgb()),p.toSet(),"actual empty plane ${rows[i][0]}")
        }
        for(i in rows.indices.reversed()) {runOnIdle{current=i};assertTrue(snapshots.getValue(i).contentEquals(pixels()),"seek restore")}
        out.resolve("complete-$width.txt").writeText("cases=${rows.size}\n")
    }
}
