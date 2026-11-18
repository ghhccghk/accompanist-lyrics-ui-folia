package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import org.jetbrains.skia.Image
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaFogPlaneTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        val rows=File("../verification/diorama-fixtures/fog-plane-v1/planes.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[1].toInt()==width}
        val glyph=Image.makeFromEncoded(File("../verification/diorama-fixtures/plane-raster.png").readBytes()).toComposeImageBitmap()
        val solid=ImageBitmap(glyph.width,glyph.height).also {
            Canvas(it).drawRect(Rect(0f,0f,it.width.toFloat(),it.height.toFloat()),Paint().apply{color=Color.White})
        }
        var current by mutableIntStateOf(0);var fogEnabled by mutableStateOf(true)
        setContent {Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
            val r=rows[current];fun v(i:Int)=DioramaVec(r[i].toDouble(),r[i+1].toDouble(),r[i+2].toDouble())
            val image=if(r[2]=="solid")solid else glyph
            assertEquals(r[14].toInt(),image.width);assertEquals(r[15].toInt(),image.height)
            val view=DioramaPerspective(DioramaVec(0.0,0.0,0.0),DioramaVec(0.0,0.0,-1.0),width.toDouble(),600.0)
            drawDioramaPlane(image,view,v(5),v(8),v(11),r[4].toFloat(),Color(0xffdfe5ec),r[3]=="1",
                if(fogEnabled)DioramaFog(Color(0xff162435))else null)
        }}
        val out=File("../build/verification/diorama-fog-plane-kotlin").apply{mkdirs()}
        out.resolve("complete-$width.txt").delete()
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val snapshots=mutableMapOf<Int,IntArray>()
        for(i in rows.indices) {
            runOnIdle{current=i;fogEnabled=false};pixels()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("before-${rows[i][0]}"))
            runOnIdle{fogEnabled=true};snapshots[i]=pixels()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("after-${rows[i][0]}"))
        }
        for(i in rows.indices.reversed()) {runOnIdle{current=i};assertTrue(snapshots.getValue(i).contentEquals(pixels()),"reverse media query")}
        val paused=pixels();mainClock.advanceTimeBy(1700);assertTrue(paused.contentEquals(pixels()),"pause freezes fog")
        out.resolve("complete-$width.txt").writeText("cases=${rows.size}\n")
    }
}
