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
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaFlightStageTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    @Test fun chronologicalSequence()=capture(800,true)
    private fun capture(width:Int,sequence:Boolean=false)=runDesktopComposeUiTest(width=width,height=600) {
        val fixture=if(sequence)"sequence" else "scene"
        val rows=File("../verification/diorama-fixtures/flight-v1/$fixture.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[2].toInt()==width}
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val seed=dioramaHashSeed("Incoming mirror")
        var current by mutableIntStateOf(0);var mode by mutableStateOf("finite");var mounted by mutableStateOf(true)
        var builds=0;var playback:DioramaTextPlayback?=null;var drawn:DioramaPerspective?=null
        val finiteViews=mutableMapOf<String,DioramaPerspective>()
        setContent {
            val r=rows[current];val resolver=LocalFontFamilyResolver.current
            val prepared=if(mounted)remember(r[1]) {
                val font=when(r[1]){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
                val syllables=r[6].split(';').map{it.split(',').let{
                    KaraokeSyllable(decode(it[0]),(it[1].toDouble()*1000).roundToInt(),(it[2].toDouble()*1000).roundToInt())}}
                val spaced=syllables.flatMapIndexed { i,s ->
                    if(i==0)listOf(s)else listOf(KaraokeSyllable(" ",s.start,s.start),s)
                }
                val original=KaraokeLine.MainKaraokeLine(spaced,null,KaraokeAlignment.Unspecified,700,syllables.last().end)
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)))
                val result=DioramaTextPlayback.prepare(SyncedLyrics(listOf(original)),style,
                    TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,width.toDouble(),600.0,
                    shots=listOf(DioramaShot.orbit),seedName="Incoming mirror",endTime=4.2,
                    transition=DioramaTextTransition(DioramaFollowState(),DioramaVec(0.0,0.0,0.0),1))
                assertSame(original,result.lines.single().core.source)
                assertEquals(r[7].toDouble()*128/.62,result.lines.single().text.lineAdvance,.001,"own shaping advance")
                val track=result.camera;val subject=track.subjects.single()
                val flight=DioramaCameraFlight(DioramaFollowState().position,track.target(0.0).eye,seed,1)
                for(fps in rows.map{it[3].toInt()}.distinct()) {
                    val probes=rows.filter{it[1]==r[1]&&it[3].toInt()==fps}.map{it[4].toDouble()}
                    val times=(listOf(0.0)+probes+(1..ceil(4.2*fps).toInt()).map{it.toDouble()/fps}.filter{it<=4.2})
                        .distinct().sorted()
                    var state=DioramaFollowState();var previous=0.0
                    for(t in times) {
                        val step=dioramaFollowStep(state,track.target(t),subject.frame,subject.placement,subject.shot,
                            t,subject.line.line.start,subject.line.renderEnd,t-previous,.42,width/600.0,flight=flight.at(t,.42))
                        state=step.state;previous=t
                        if(t in probes)finiteViews["${r[1]}-$fps-$t"]=DioramaPerspective(step.renderedPosition,
                            state.look,width.toDouble(),600.0,orientation=step.renderedOrientation)
                    }
                }
                builds++;result
            }else null
            SideEffect{playback=prepared}
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if(prepared!=null) {
                    val time=r[4].toDouble()
                    val view=if(mode=="finite")finiteViews.getValue("${r[1]}-${r[3]}-$time")else prepared.camera.perspectiveAt(time)
                    drawn=view
                    fun color(i:Int)=r[i].split(',').map(String::toDouble).let{DioramaLinearColor(it[0],it[1],it[2])}
                    // Production full core -> camera -> shaped text stage, including distance life.
                    prepared.drawStage(this,time,view,color(8),color(9),toneMapping=DioramaToneMapping.None)
                }
            }
        }
        val out=File("../build/verification/diorama-flight-kotlin").apply{mkdirs()}
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        fun orientation(view:DioramaPerspective)=DioramaQuaternion.basis(view.right,view.up,view.forward * -1.0)
        val mediaFrames=mutableMapOf<Int,IntArray>();val scheduleFrames=mutableMapOf<String,IntArray>()
        var positionPeak=0.0;var quaternionPeak=0.0;var finitePeak=0.0
        for(i in rows.indices) {
            runOnIdle{current=i;mode="finite"};pixels()
            val r=rows[i];val own=drawn!!;val q=orientation(own)
            val originalQ=DioramaQuaternion(r[13].toDouble(),r[14].toDouble(),r[15].toDouble(),r[16].toDouble())
            val sign=if(q.dot(originalQ)<0)-1 else 1
            val fields=listOf(own.eye.x,own.eye.y,own.eye.z,q.x*sign,q.y*sign,q.z*sign,q.w*sign)
            for((field,value)in fields.withIndex()) {
                finitePeak=max(finitePeak,abs(r[10+field].toDouble()-value))
                assertEquals(r[10+field].toDouble(),value,1e-9,"own finite ${r[0]} field$field")
            }
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("finite-${r[0]}"))
            runOnIdle{mode="media"};mediaFrames[i]=pixels()
            val key="${r[1]}-${r[4]}"
            scheduleFrames[key]?.let{assertTrue(it.contentEquals(mediaFrames.getValue(i)),"display schedule independence ${r[0]}")}
            scheduleFrames[key]=mediaFrames.getValue(i)
            val m=drawn!!;val d=own.eye-m.eye
            positionPeak=maxOf(positionPeak,abs(d.x),abs(d.y),abs(d.z))
            quaternionPeak=maxOf(quaternionPeak,1-abs(q.dot(orientation(m))))
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("media-${r[0]}"))
        }
        assertEquals(if(sequence)1 else 3,builds,"no per-frame reprepare")
        for(i in listOf(rows.lastIndex,0,7,3,18,10)) {
            runOnIdle{current=i;mode="media"};assertTrue(mediaFrames[i]!!.contentEquals(pixels()),"reverse seek ${rows[i][0]}")
        }
        val frozen=pixels();val count=playback!!.camera.preparedNodeCount;mainClock.advanceTimeBy(1300)
        assertTrue(frozen.contentEquals(pixels()));assertEquals(count,playback!!.camera.preparedNodeCount)
        runOnIdle{mounted=false};pixels();assertNull(playback)
        runOnIdle{mounted=true};assertTrue(frozen.contentEquals(pixels()),"remount")
        out.resolve(if(sequence)"numeric-sequence.txt" else "numeric-$width.txt").writeText("sourceFrames=${rows.size} finitePosePeak=$finitePeak finitePoseGate=1e-9 mediaPositionResidual=$positionPeak mediaQuaternionOneMinusDot=$quaternionPeak\nNo raw RAF equivalence claim for adaptive media track\n")
    }
}
