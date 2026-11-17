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
class CadenzaAccentsTest {
    private val palette=CadenzaPalette(Color(0xff8ab3df),Color(0xffd6f456),listOf("Amber amber" to Color(0xff5bcefa),"echoes echo" to Color(0xfff5a9b8),
        "向远方" to Color(0xff5bcefa),"星光 回响" to Color(0xfff5a9b8)))
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private fun rows(name:String)=File("../verification/cadenza-fixtures/$name.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
    private fun line(scene:String):CadenzaLyric {
        val f=rows("sceneInputs").first{it[0]==scene}
        return annotateCadenzaLine(CadenzaLyric(decode(f[1]),f[2].toDouble(),f[3].toDouble(),f[4].split('|').map{d->val w=d.split('~');CadenzaWord(decode(w[0]),w[1].toDouble(),w[2].toDouble())},chorus=true))
    }
    @Test fun keywordColorsMatchOriginalResolver() {
        for(r in rows("accentColors"))assertEquals(cadenzaInk(r[1]),palette.active(decode(r[0])),"word '${decode(r[0])}'")
    }
    @Test fun stageSpaceRingsMatchOriginalCanvasCalls() {
        var count=0;var maximum=0.0
        for((key,expected)in rows("accentRings").groupBy{Triple(it[0],it[1].toInt(),it[2].toDouble())}){
            val(scene,width,time)=key;val l=line(scene)
            val ps=rows("motionPlacements").filter{it[0]=="$scene-$width-60"}.map{r->
                val n=r[7].split(',').map(String::toDouble);val w=l.words[r[2].toInt()]
                CadenzaPlacement(decode(r[1]),CadenzaFragment(r[2].toInt(),w,decode(r[3]),palette.encoded(w.text),0.0,n[2],r[4].toInt(),r[5].toInt(),r[6].toInt(),emptyList()),
                    n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9],n[10],n[11],false)
            }
            val targets=cadenzaTargets(time,l,ps,width.toDouble(),600.0)
            val actual=targets.mapNotNull{t->cadenzaChorusRipple(l,t)?.let{t to it}}
            assertEquals(expected.size,actual.size,"rings at $key")
            actual.zip(expected).forEach{(a,e)->val(t,r)=a
                // Canvas stores its lineWidth in float precision, as does Stroke.
                listOf(r.x,r.y,r.radius,r.stroke.toFloat().toDouble()).zip(e[4].split(',').map(String::toDouble)).forEach{(v,q)->
                    maximum=maxOf(maximum,kotlin.math.abs(v-q));assertEquals(q,v,1e-8,"ring geometry at $key")}
                val assigned=decode(e[5]);val alpha=assigned.substringAfterLast(',').trim().removeSuffix(")").toDouble()
                assertEquals(alpha,r.alpha,1e-10,"raw Canvas alpha at $key")
                assertNull(cadenzaChorusRipple(l.copy(chorus=false),t))
                assertNull(cadenzaChorusRipple(l,t.copy(placement=t.placement.copy(interlude=true))))
                count++
            }
        }
        File("../build/verification/cadenza-accents-kotlin").apply{mkdirs()}.resolve("rings.txt").writeText("rings=$count\nmaximum_geometry_delta=$maximum\n")
    }
    @Test fun latin()=capture("latin",380)
    @Test fun wrap()=capture("wrap",380)
    @Test fun arabic()=capture("arabic",800)
    @Test fun cjk()=capture("cjk",800)
    private fun capture(scene:String,width:Int)=runDesktopComposeUiTest(width=width,height=600){
        val name=when(scene){"arabic"->"NotoSansArabic-Bold.ttf";"cjk"->"FoliaParityCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$name"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        val l=line(scene);var time by mutableStateOf(.7)
        setContent{CadenzaTargetStage(l,{time},style,Modifier.fillMaxSize().background(Color(0xff162435)),palette=palette)}
        waitForIdle();val out=File("../build/verification/cadenza-accents-kotlin").apply{mkdirs()}
        fun pixels()=captureToImage().let{img->IntArray(img.width*img.height).also{img.readPixels(it)}}
        val captures=mutableMapOf<Double,IntArray>()
        for(t in listOf(.7,.75,.85,1.3,1.6,2.2,2.8,3.05)){runOnIdle{time=t};waitForIdle()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$scene-$width-${(t*1000).toInt()}.png"));captures[t]=pixels()}
        for((t,expected)in captures.entries.shuffled(Random(391))){runOnIdle{time=t};waitForIdle();assertContentEquals(expected,pixels(),"colored chorus seek $scene/$t")}
        val frozen=pixels();mainClock.advanceTimeBy(2000);waitForIdle();assertContentEquals(frozen,pixels())
    }
}
