package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaBubbleResizeTest {
    @Test fun latin()=capture("latin")
    @Test fun wrap()=capture("wrap")
    private fun capture(scene:String)=runDesktopComposeUiTest(width=480,height=300) {
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val f=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first{it.startsWith("$scene\t")}.split('\t')
        val line=CappellaLine(decode(f[1]),f[2].toDouble(),f[3].toDouble(),
            f[4].split('|').map{v->val w=v.split('~');CappellaWord(decode(w[0]),w[1].toDouble(),w[2].toDouble())})
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        var time by mutableStateOf(.7);var right by mutableStateOf(false);var prepared:CappellaBubblePrepared?=null
        val bg=Color(0xff162435)
        setContent { Box(Modifier.fillMaxSize().background(bg).padding(start=16.dp,top=32.dp)) {
            val sender=CappellaSender(if(right)CappellaSide.RIGHT else CappellaSide.LEFT,if(right)8 else 2)
            CappellaBubbleTarget(line,{time},style,cappellaBubbleColors(sender,Color(0xffdfe5ec),Color(0xfff17b4d),Color(0xffadb9c8),bg),
                Modifier.fillMaxSize(),CappellaBubbleStyle(maxTextWidth=160.0),right=right,resizeFrom=.7,onPrepared={prepared=it})
        } }
        waitForIdle();val p=assertNotNull(prepared);val resize=CappellaBubbleResize(p.plan,p.sizes,.7)
        val fixtures=File("../verification/cappella-fixtures/resize-observations.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[0]==scene}
        val out=File("../build/verification/cappella-resize-kotlin").apply{mkdirs()}
        val numeric=mutableListOf("# scene side time originalFrameWidth originalFrameHeight nativeFrameWidth nativeFrameHeight widthError heightError")
        fun pixels()=captureToImage().let{img->IntArray(img.width*img.height).also{img.readPixels(it)}}
        for(side in listOf(false,true)) {
            val frames=fixtures.filter{it[2]==if(side)"right"else"left"};val states=linkedMapOf<Double,IntArray>()
            runOnIdle{right=side}
            for(r in frames) {
                val t=r[3].toDouble();runOnIdle{time=t};waitForIdle();states[t]=pixels()
                val actual=resize.at(t)
                numeric+=listOf(scene,r[2],t,r[4],r[5],actual.width,actual.height,
                    kotlin.math.abs(actual.width-r[4].toDouble()),kotlin.math.abs(actual.height-r[5].toDouble())).joinToString("\t")
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$scene-160-${r[2]}-${(t*1000).toInt()}.png"))
            }
            for((t,expected)in states.entries.shuffled(Random(243))){runOnIdle{time=t};waitForIdle();assertContentEquals(expected,pixels(),"seek $scene/$side/$t")}
            val frozen=pixels();mainClock.advanceTimeBy(2000);waitForIdle();assertContentEquals(frozen,pixels(),"pause $scene/$side")
        }
        out.resolve("$scene-residual.tsv").writeText(numeric.joinToString("\n")+"\n")
        // Original DOM fractional dimensions are serialized on a 1/64px grid.
        // Dynamic residuals are reported separately; never substitute targets
        // or change source observations to force a continuous-playback pass.
        val final=resize.at(3.5);assertEquals(p.sizes.last(),final,"settled whole phrase")
    }
}
