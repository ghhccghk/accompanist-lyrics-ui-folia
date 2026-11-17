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
class CadenzaTargetStageTest {
    @Test fun latinNarrow()=capture("latin",380)
    @Test fun latinWide()=capture("latin",800)
    @Test fun wrapNarrow()=capture("wrap",380)
    @Test fun wrapWide()=capture("wrap",800)
    @Test fun arabicNarrow()=capture("arabic",380)
    @Test fun arabicWide()=capture("arabic",800)
    @Test fun cjkNarrow()=capture("cjk",380)
    @Test fun cjkWide()=capture("cjk",800)
    private fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
    private fun capture(scene:String,width:Int)=runDesktopComposeUiTest(width=width,height=600){
        val f=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first{it.split('\t')[0]==scene}.split('\t')
        val line=annotateCadenzaLine(CadenzaLyric(decode(f[1]),f[2].toDouble(),f[3].toDouble(),f[4].split('|').map{d->val w=d.split('~');CadenzaWord(decode(w[0]),w[1].toDouble(),w[2].toDouble())}))
        val name=when(scene){"arabic"->"NotoSansArabic-Bold.ttf";"cjk"->"FoliaParityCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$name"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        var time by mutableStateOf(.7);var glow by mutableStateOf(0.0);var ready:CadenzaPreparedText?=null
        setContent{CadenzaTargetStage(line,{time},style,Modifier.fillMaxSize().background(Color(0xff162435)),glow=glow,onPrepared={ready=it})}
        waitForIdle();val prepared=assertNotNull(ready)
        val original=File("../verification/cadenza-fixtures/targetGeometry.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[0]==scene&&it[1].toInt()==width}
        assertEquals(original.map{decode(it[2])},prepared.placements.map{it.id},"source wrapping/fragment order")
        original.zip(prepared.placements).forEach{(e,p)->
            val actual=listOf(p.x,p.y,p.width,p.height,p.scale)
            e[3].split(',').map(String::toDouble).zip(actual).forEachIndexed{i,(a,b)->
                assertEquals(a,b,1e-3,"independent font geometry $scene/$width/${p.id} field$i")
            }
        }
        val out=File("../build/verification/cadenza-target-kotlin").apply{mkdirs()}
        fun b64(s:String)=Base64.getEncoder().encodeToString(s.toByteArray(Charsets.UTF_8))
        out.resolve("$scene-$width.tsv").writeText((listOf("# fontPx,lineHeight,maxWidth",listOf(prepared.size.fontPx,prepared.size.lineHeight,prepared.size.maxWidth).joinToString(","),
            "# id64\twordIndex\ttext64\tx,y,width,height,scale")+prepared.placements.map{p->listOf(b64(p.id),p.fragment.wordIndex,b64(p.fragment.text),listOf(p.x,p.y,p.width,p.height,p.scale).joinToString(",")).joinToString("\t")}).joinToString("\n")+"\n")
        val times=listOf(.7,.75,.85,1.3,1.6,2.2,2.8,3.05)
        fun pixels()=captureToImage().let{img->IntArray(img.width*img.height).also{img.readPixels(it)}}
        for(g in listOf(0.0,1.0)){
            runOnIdle{glow=g};waitForIdle();val captures=mutableMapOf<Double,IntArray>()
            for(t in times){runOnIdle{time=t};waitForIdle();ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$scene-$width-${g.toInt()}-${(t*1000).toInt()}.png"));captures[t]=pixels()}
            for((t,expected)in captures.entries.shuffled(Random(173))){runOnIdle{time=t};waitForIdle();assertContentEquals(expected,pixels(),"random revisit $scene/$width/$g/$t")}
            val frozen=pixels();mainClock.advanceTimeBy(1000);waitForIdle();assertContentEquals(frozen,pixels(),"target stage uses media time only")
        }
    }
}
