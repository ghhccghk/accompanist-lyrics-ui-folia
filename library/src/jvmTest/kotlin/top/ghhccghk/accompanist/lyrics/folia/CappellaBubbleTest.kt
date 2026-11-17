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
class CappellaBubbleTest {
    @Test fun latin() = capture("latin",160)
    @Test fun wrap() = capture("wrap",160)
    @Test fun arabic() = capture("arabic",300)
    @Test fun cjk() = capture("cjk",160)
    private fun decode(s: String) = String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private fun capture(scene: String, textWidth: Int) = runDesktopComposeUiTest(width=480,height=300) {
        val r=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first { it.split('\t')[0]==scene }.split('\t')
        val line=CappellaLine(decode(r[1]),r[2].toDouble(),r[3].toDouble(),r[4].split('|').map { v->val w=v.split('~');CappellaWord(decode(w[0]),w[1].toDouble(),w[2].toDouble()) })
        val name=when(scene){"arabic"->"NotoSansArabic-Bold.ttf";"cjk"->"FoliaParityCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$name"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        var time by mutableStateOf(.7); var right by mutableStateOf(false);var ready:CappellaBubblePrepared?=null
        val bg=Color(0xff162435)
        setContent { Box(Modifier.fillMaxSize().background(bg).padding(start=16.dp,top=32.dp)) {
            val sender=CappellaSender(if(right)CappellaSide.RIGHT else CappellaSide.LEFT,if(right)8 else 2)
            CappellaBubbleTarget(line,{time},style,cappellaBubbleColors(sender,Color(0xffdfe5ec),Color(0xfff17b4d),Color(0xffadb9c8),bg),
                Modifier.fillMaxSize(),CappellaBubbleStyle(maxTextWidth=textWidth.toDouble()),right=right,onPrepared={ready=it})
        } }
        waitForIdle();val prepared=assertNotNull(ready)
        val source=File("../verification/cappella-fixtures/bubble-sizes.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}
            .map{it.split('\t')}.filter{it[0]==scene&&it[1].toInt()==textWidth}
        assertEquals(source.size,prepared.sizes.size)
        val out=File("../build/verification/cappella-bubble-kotlin").apply{mkdirs()}
        out.resolve("$scene-sizes.tsv").writeText(prepared.sizes.mapIndexed{i,s->"$i\t${s.width}\t${s.height}"}.joinToString("\n")+"\n")
        // Source geometry must match; image residuals are measured separately.
        for((r,s)in source.zip(prepared.sizes)){
            assertEquals(r[3].toDouble(),s.width,0.0,"$scene prefix${r[2]} width")
            assertEquals(r[4].toDouble(),s.height,0.0,"$scene prefix${r[2]} height")
        }
        for(r in File("../verification/cappella-fixtures/bubble-opacity.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[0]==scene}){
            val actual=cappellaEaseOut(prepared.plan.progress(r[2].toInt(),r[1].toDouble()))
            // getComputedStyle serializes opacity to six decimal places.
            assertEquals(r[3].toDouble(),actual,1e-6,"original CSS opacity $scene/${r[1]}/${r[2]}")
        }
        fun pixels()=captureToImage().let{img->IntArray(img.width*img.height).also{img.readPixels(it)}}
        for(side in listOf(false,true)){
            runOnIdle{right=side};waitForIdle();val states=mutableMapOf<Double,IntArray>()
            for(t in listOf(.7,.85,1.3,1.6,2.8,3.5)){
                runOnIdle{time=t};waitForIdle();states[t]=pixels()
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$scene-$textWidth-${if(side)"right" else "left"}-${(t*1000).toInt()}.png"))
            }
            for((t,p)in states.entries.shuffled(Random(731))){runOnIdle{time=t};waitForIdle();assertContentEquals(p,pixels(),"seek $scene/$t")}
            val frozen=pixels();mainClock.advanceTimeBy(1000);waitForIdle();assertContentEquals(frozen,pixels(),"pause $scene")
        }
    }
}
