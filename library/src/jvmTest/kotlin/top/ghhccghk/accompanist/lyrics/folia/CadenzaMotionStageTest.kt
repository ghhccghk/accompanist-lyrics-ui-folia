package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CadenzaMotionStageTest {
    @Test fun latin()=capture("latin",380)
    @Test fun wrap()=capture("wrap",380)
    @Test fun arabic()=capture("arabic",800)
    @Test fun cjk()=capture("cjk",800)
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private fun capture(scene:String,width:Int)=runDesktopComposeUiTest(width=width,height=600){
        val f=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first{it.split('\t')[0]==scene}.split('\t')
        val line=annotateCadenzaLine(CadenzaLyric(decode(f[1]),f[2].toDouble(),f[3].toDouble(),f[4].split('|').map{d->val w=d.split('~');CadenzaWord(decode(w[0]),w[1].toDouble(),w[2].toDouble())}))
        val name=when(scene){"arabic"->"NotoSansArabic-Bold.ttf";"cjk"->"FoliaParityCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$name"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        var time by mutableStateOf(.7)
        setContent{CadenzaTargetStage(line,{time},style,Modifier.fillMaxSize().background(Color(0xff162435)),motion=true)}
        waitForIdle()
        val out=File("../build/verification/cadenza-motion-kotlin").apply{mkdirs()}
        val frames=File("../verification/cadenza-fixtures/motionFrames.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}
            .map{it.split('\t')}.filter{it[1]==scene&&it[2].toInt()==width}
        fun pixels()=captureToImage().let{img->IntArray(img.width*img.height).also{img.readPixels(it)}}
        val captures=mutableMapOf<Double,IntArray>()
        for(frame in frames){val t=frame[3].toDouble();runOnIdle{time=t};waitForIdle()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(frame[0]));captures[t]=pixels()}
        for((t,expected)in captures.entries.shuffled(Random(901))){runOnIdle{time=t};waitForIdle();assertContentEquals(expected,pixels(),"motion random seek $scene/$t")}
        val frozen=pixels();mainClock.advanceTimeBy(2000);waitForIdle();assertContentEquals(frozen,pixels(),"paused media freezes response")
    }
}
