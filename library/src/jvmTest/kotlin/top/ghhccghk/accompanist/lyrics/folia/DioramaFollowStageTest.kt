package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.*
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaFollowStageTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        val rows=File("../verification/diorama-fixtures/follow-v1/scene.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[2].toInt()==width}
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val seed=dioramaHashSeed("DioramaProbe");val frame=buildDioramaPath(12,seed)[3]
        val placement=dioramaTextPlacement(3,seed)
        var current by mutableIntStateOf(0);var mode by mutableStateOf("finite");var mounted by mutableStateOf(true)
        var builds=0;var camera:DioramaCameraTrack?=null;var drawn:DioramaFollowState?=null
        val ownPoses=mutableMapOf<String,DioramaFollowState>()
        setContent {
            val r=rows[current];val resolver=LocalFontFamilyResolver.current
            val prepared=if(mounted)remember(r[1]) {
                val font=when(r[1]){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
                val syllables=r[6].split(';').map{it.split(',').let{KaraokeSyllable(decode(it[0]),(it[1].toDouble()*1000).roundToInt(),(it[2].toDouble()*1000).roundToInt())}}
                // Published KaraokeLine concatenates syllable text. Preserve
                // real display spaces with the original gap's next-start time.
                val spaced=syllables.flatMapIndexed { i,s ->
                    if(i==0)listOf(s)else listOf(KaraokeSyllable(" ",s.start,s.start),s)
                }
                val original=KaraokeLine.MainKaraokeLine(spaced,null,KaraokeAlignment.Unspecified,700,syllables.last().end)
                val core=dioramaCoreLines(normalize(SyncedLyrics(listOf(original)))).single()
                assertSame(original,core.source)
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)))
                val text=DioramaActiveText.prepare(core.lyric,style,TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver)
                assertEquals(r[7].toDouble()*128/.62,text.lineAdvance,.001,"own source-conditioned advance")
                val rawWidth=text.lineAdvance*.62/128
                val fit=dioramaFrameFit(rawWidth,5.2,55.0,width/600.0)*placement.scale
                val subject=DioramaCameraSubject(DioramaCameraLine(core.lyric),frame,placement,DioramaShot.orbit,rawWidth*fit,3)
                val track=DioramaCameraTrack(listOf(subject),width.toDouble(),600.0,8.92,seed)
                for(fps in listOf(60,144)) {
                    val probes=rows.filter{it[1]==r[1]&&it[3].toInt()==fps}.map{it[4].toDouble()}
                    val times=(listOf(0.0)+probes+(1..ceil(8.92*fps).toInt()).map{it.toDouble()/fps}.filter{it<=8.92}).distinct().sorted()
                    var state=DioramaFollowState();var previous=0.0
                    for(t in times) {
                        state=dioramaFollowStep(state,track.target(t),frame,placement,DioramaShot.orbit,t,.7,
                            subject.line.renderEnd,t-previous,.42,width/600.0).state
                        previous=t
                        if(t in probes)ownPoses["${r[1]}-$fps-$t"]=state
                    }
                }
                builds++;text to track
            }else null
            SideEffect{camera=prepared?.second}
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if(prepared!=null) {
                    val time=r[4].toDouble()
                    val state=if(mode=="finite")ownPoses.getValue("${r[1]}-${r[3]}-$time")else prepared.second.at(time)
                    drawn=state
                    fun color(i:Int)=r[i].split(',').map(String::toDouble).let{DioramaLinearColor(it[0],it[1],it[2])}
                    prepared.first.draw(this,time,DioramaPerspective(state.position,state.look,width.toDouble(),600.0,orientation=state.orientation),
                        frame,placement,color(8),color(9))
                }
            }
        }
        val out=File("../build/verification/diorama-follow-kotlin").apply{mkdirs()}
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        var positionPeak=0.0;var orientationPeak=0.0
        val mediaFrames=mutableMapOf<Int,IntArray>()
        val scheduleFrames=mutableMapOf<String,IntArray>()
        for(i in rows.indices) {
            runOnIdle{current=i;mode="finite"};pixels()
            val r=rows[i];val s=drawn!!
            val actual=listOf(s.position.x,s.position.y,s.position.z,s.look.x,s.look.y,s.look.z,
                s.orientation!!.x,s.orientation.y,s.orientation.z,s.orientation.w)
            for((field,v)in actual.withIndex())assertEquals(r[10+field].toDouble(),v,1e-9,"own finite ${r[0]} field$field")
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("finite-${r[0]}"))
            runOnIdle{mode="media"};mediaFrames[i]=pixels()
            val key="${r[1]}-${r[4]}"
            scheduleFrames[key]?.let{assertTrue(it.contentEquals(mediaFrames.getValue(i)),"same media at different source RAF ${r[0]}")}
            scheduleFrames[key]=mediaFrames.getValue(i)
            val m=drawn!!
            val d=s.position-m.position;positionPeak=maxOf(positionPeak,abs(d.x),abs(d.y),abs(d.z))
            orientationPeak=maxOf(orientationPeak,1-abs(s.orientation.dot(m.orientation!!)))
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("media-${r[0]}"))
        }
        assertEquals(3,builds,"no reprepare on time / follow mode / source fps")
        for(i in listOf(rows.lastIndex,0,7,3,18,10)) {
            runOnIdle{current=i;mode="media"};assertTrue(mediaFrames[i]!!.contentEquals(pixels()),"reverse seek ${rows[i][0]}")
        }
        val frozen=pixels();val count=camera!!.preparedNodeCount;mainClock.advanceTimeBy(1000)
        assertTrue(frozen.contentEquals(pixels()));assertEquals(count,camera!!.preparedNodeCount)
        runOnIdle{mounted=false};pixels();assertNull(camera)
        runOnIdle{mounted=true};assertTrue(frozen.contentEquals(pixels()),"remount")
        out.resolve("numeric-$width.txt").writeText("sourceFrames=${rows.size} finitePoseGate=1e-9 mediaPositionResidual=$positionPeak mediaQuaternionOneMinusDot=$orientationPeak\nNo raw RAF equivalence claim for adaptive media track\n")
    }
}
