package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.waiting.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class WaitingDotsParityTest {
    @Test fun exactSourcePhasesAtForwardAndReverseMediaTimes() {
        val out=File("build/verification/waiting").apply {mkdirs()}
        val rows=mutableListOf("duration,time,scale,alpha,dot0,dot1,dot2,maxError")
        for(duration in listOf(1,5001,6400,6401,6416,6417,7000,10000,18000)) {
            val actual=PreparedBreathingDots(1000,1000+duration,WaitingDotsDefaults())
            val source=SourcePreparedBreathingDots(1000,1000+duration,WaitingDotsDefaults())
            val times=(-1..duration+1 step 13).toList()+listOf(0,duration)+
                listOf(actual.enterEnd,actual.dipStart,actual.stillStart,actual.exitStart).map{it.toInt()}
            for(time in times+times.reversed()) {
                val a=listOf(actual.scale(time.toFloat()),actual.alpha(time.toFloat()))+
                    (0..2).map{actual.dotAlpha(it,time.toFloat())}
                val b=listOf(source.scale(time.toFloat()),source.alpha(time.toFloat()))+
                    (0..2).map{source.dotAlpha(it,time.toFloat())}
                assertEquals(b,a,"Source phase duration=$duration time=$time")
                assertEquals(source.elapsed(time+1000),actual.elapsed(time+1000))
                rows+=(listOf(duration,time)+a+0).joinToString(",")
            }
        }
        File(out,"source-phases.csv").writeText(rows.joinToString("\n"))
    }

    @Test fun stageIsStableWhenPausedAndRevisited()=runDesktopComposeUiTest(width=380,height=240) {
        var time by mutableIntStateOf(0)
        var reduced by mutableStateOf(false)
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF162435)),contentAlignment=Alignment.Center) {
                WaitingBreathingDots(0,18000,{time},reducedMotion=reduced)
            }
        }
        val out=File("build/verification/waiting").apply {mkdirs()}
        val original=mutableMapOf<Pair<Int,Boolean>,List<Int>>()
        for(pass in 0..1) for(r in listOf(false,true)) for(t in
            (if(pass==0) listOf(0,1500,3000,7000,12000,15000,17700,17900,18000)
            else listOf(18000,17900,17700,15000,12000,7000,3000,1500,0))) {
            runOnIdle{time=t;reduced=r}
            val img=captureToImage().toAwtImage()
            val pixels=img.getRGB(0,0,img.width,img.height,null,0,img.width).toList()
            Thread.sleep(40)
            val paused=captureToImage().toAwtImage()
            assertEquals(pixels,paused.getRGB(0,0,paused.width,paused.height,null,0,paused.width).toList())
            val key=t to r
            if(pass==0) {
                original[key]=pixels
                ImageIO.write(img,"png",File(out,"dots-$t-reduced-$r.png"))
            } else assertEquals(original[key],pixels,"Reverse seek $key")
            if(t==0 || t==18000) assertEquals(1,pixels.distinct().size)
            else assertTrue(pixels.distinct().size>1,"Visible waiting dots at $key")
        }
    }
}
