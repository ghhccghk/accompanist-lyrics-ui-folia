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
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import java.util.Base64
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaTargetStageTest {
    @Test fun latin()=capture("latin",380)
    @Test fun wrap()=capture("wrap",800)
    @Test fun arabic()=capture("arabic",800)
    @Test fun cjk()=capture("cjk",1200)
    @Test fun mediaDecorationBoundaries() {
        assertEquals("0:00",cappellaTimestamp(Double.NaN))
        assertEquals("0:00",cappellaTimestamp(-.1))
        assertEquals("1:00",cappellaTimestamp(60.9))
        for(i in CappellaIntensity.entries) {
            val duration=cappellaEffects(i).glowDuration
            assertEquals(0.0,cappellaSweepPhase(.7,.7,duration),0.0)
            assertEquals(.5,cappellaSweepPhase(.7+duration/2,.7,duration),1e-14)
            assertEquals(0.0,cappellaSweepPhase(-1.0,.7,duration),0.0)
        }
    }
    private fun capture(scene:String,width:Int)=runDesktopComposeUiTest(width=width,height=900) {
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val f=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first{it.startsWith("$scene\t")}.split('\t')
        val lines=(0 until 8).map { i->CappellaLine(decode(f[1]),f[2].toDouble()+i*4,f[3].toDouble()+i*4,
            f[4].split('|').map{v->val w=v.split('~');CappellaWord(decode(w[0]),w[1].toDouble()+i*4,w[2].toDouble()+i*4)}) }
        val font=when(scene){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        var t by mutableStateOf(0.0);var intensity by mutableStateOf(CappellaIntensity.CALM)
        var mounted by mutableStateOf(true);var reduced by mutableStateOf(false)
        var observed:List<CappellaRowLayout> = emptyList()
        setContent { Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
            if(mounted)CappellaTargetStage(lines,"Synthetic title",{t},{lines.indexOfLast{t>=it.start&&t<=it.end}},style,
                intensity=intensity,reducedMotion=reduced,onRows={observed=it})
        } }
        fun hash():String {
            val image=captureToImage();val pixels=IntArray(image.width*image.height);image.readPixels(pixels)
            val digest=MessageDigest.getInstance("SHA-256")
            pixels.forEach{p->digest.update((p ushr 24).toByte());digest.update((p ushr 16).toByte());digest.update((p ushr 8).toByte());digest.update(p.toByte())}
            return digest.digest().joinToString(""){"%02x".format(it)}
        }
        val out=File("../build/verification/cappella-stage-visual-kotlin").apply{mkdirs()}
        val states=linkedMapOf<Pair<CappellaIntensity,Double>,String>()
        for(i in CappellaIntensity.entries)for(time in listOf(0.0,1.6,6.0,14.0,27.5)) {
            runOnIdle{intensity=i;t=time};waitForIdle();assertTrue(observed.isNotEmpty())
            states[i to time]=hash()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$scene-$width-${i.name.lowercase()}-${(time*1000).toInt()}.png"))
        }
        for((key,expected)in states.entries.shuffled(Random(187))){runOnIdle{intensity=key.first;t=key.second};waitForIdle();assertEquals(expected,hash(),"random seek $scene/$key")}
        val frozen=hash();mainClock.advanceTimeBy(2000);waitForIdle();assertEquals(frozen,hash(),"paused $scene")
        runOnIdle{mounted=false};waitForIdle();runOnIdle{mounted=true};waitForIdle();assertEquals(frozen,hash(),"remount $scene")
        runOnIdle{reduced=true;t=1.6};waitForIdle();val reduceHash=hash()
        mainClock.advanceTimeBy(2000);waitForIdle();assertEquals(reduceHash,hash(),"reduced pause $scene")
        out.resolve("$scene-revisits.txt").writeText("15 target frames;15 random revisits;pause/remount/reduced pause: pixel-identical\n")
    }
}
