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
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaLoopStageTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        val rows=File("../verification/diorama-fixtures/loop-stage-v1/scene.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[1].toInt()==width}
        val originals=(0..2).map{SyncedLine("Synthetic loop line $it",null,700+it*1400,1500+it*1400)}
        val seed=dioramaHashSeed("Incoming mirror")
        var current by mutableIntStateOf(0);var mode by mutableStateOf("finite");var mounted by mutableStateOf(true)
        var builds=0;var drawn:DioramaPerspective?=null;var playback:DioramaTextPlayback?=null
        val finiteViews=mutableMapOf<String,DioramaPerspective>()
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val loop=if(mounted)remember {
                builds++
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                val ready=DioramaTextPlayback.prepare(SyncedLyrics(originals),style,
                    TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,width.toDouble(),600.0,
                    seedName="Incoming mirror",endTime=5.0,shots=List(3){DioramaShot.orbit},
                    transition=DioramaTextTransition(DioramaFollowState(),DioramaVec(0.0,0.0,0.0),2))
                val result=ready.loopAt(4.2,3)
                val sourceWidths=rows.first()[9].split(',').map(String::toDouble)
                for(i in 0..2)assertEquals(sourceWidths[i]*128/.62,result.lines[i].text.lineAdvance,.001,"own full-shaped advance line$i")
                val track=ready.camera
                fun advance(previous:DioramaFollowState,t:Double,dt:Double,flight:DioramaCameraFlight):DioramaFollowResult {
                    val subject=track.subjects[track.subjectIndex(t)]
                    return dioramaFollowStep(previous,track.target(t),subject.frame,subject.placement,subject.shot,t,
                        subject.line.line.start,subject.line.renderEnd,dt,.42,width/600.0,flight=flight.at(t,.42))
                }
                for(fps in listOf(60,144)) {
                    var state=DioramaFollowState();var last=0.0
                    val incoming=DioramaCameraFlight(state.position,track.target(0.0).eye,seed,2)
                    val outgoing=(listOf(0.0,4.2)+(1..ceil(4.2*fps).toInt()).map{it.toDouble()/fps}.filter{it<=4.2}).distinct().sorted()
                    for(t in outgoing){state=advance(state,t,t-last,incoming).state;last=t}
                    val flight=DioramaCameraFlight(state.position,track.target(0.0).eye,seed,3)
                    val probes=rows.filter{it[2].toInt()==fps}.map{it[3].toDouble()}
                    val ticks=(listOf(0.0)+probes+(1..ceil(4.5*fps).toInt()).map{it.toDouble()/fps}.filter{it<=4.5}).distinct().sorted()
                    last=0.0
                    for(t in ticks){
                        val step=advance(state,t,t-last,flight);state=step.state;last=t
                        if(t in probes)finiteViews["$fps-$t"]=DioramaPerspective(step.renderedPosition,state.look,
                            width.toDouble(),600.0,orientation=step.renderedOrientation)
                    }
                }
                result
            }else null
            SideEffect{playback=loop}
            val r=rows[current]
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if(loop!=null) {
                    val time=r[3].toDouble()
                    val view=if(mode=="finite")finiteViews.getValue("${r[2]}-$time")else loop.camera.perspectiveAt(time)
                    drawn=view
                    fun color(i:Int)=r[i].split(',').map(String::toDouble).let{DioramaLinearColor(it[0],it[1],it[2])}
                    loop.drawStage(this,time,view,color(6),color(8),color(7),toneMapping=DioramaToneMapping.None)
                }
            }
        }
        val out=File("../build/verification/diorama-loop-stage-kotlin").apply{mkdirs()}
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        fun orientation(view:DioramaPerspective)=DioramaQuaternion.basis(view.right,view.up,view.forward * -1.0)
        val mediaFrames=mutableMapOf<Int,IntArray>();val schedules=mutableMapOf<String,IntArray>()
        var finitePeak=0.0;var mediaPeak=0.0;var quaternionPeak=0.0
        for(i in rows.indices) {
            runOnIdle{current=i;mode="finite"};pixels()
            val r=rows[i];val view=drawn!!;val q=orientation(view)
            val sourceQ=DioramaQuaternion(r[13].toDouble(),r[14].toDouble(),r[15].toDouble(),r[16].toDouble())
            val sign=if(q.dot(sourceQ)<0)-1 else 1
            val values=listOf(view.eye.x,view.eye.y,view.eye.z,q.x*sign,q.y*sign,q.z*sign,q.w*sign)
            for((field,value)in values.withIndex()) {
                val expected=r[10+field].toDouble();finitePeak=max(finitePeak,abs(value-expected))
                assertEquals(expected,value,1e-9,"original finite ${r[0]} field$field")
            }
            assertSame(originals[r[4].toInt()],playback!!.sourceAt(r[3].toDouble()).source)
            assertEquals(r[5].toInt().takeIf{it>=0},playback!!.outgoingTextIndexAt(r[3].toDouble()))
            assertEquals(r[18].split(',').map(String::toInt),playback!!.mountedTextIndicesAt(r[3].toDouble()))
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("finite-${r[0]}"))
            runOnIdle{mode="media"};mediaFrames[i]=pixels()
            schedules[r[3]]?.let{assertTrue(it.contentEquals(mediaFrames.getValue(i)),"source fps cannot affect production camera")}
            schedules[r[3]]=mediaFrames.getValue(i)
            val m=drawn!!;val d=m.eye-view.eye
            mediaPeak=maxOf(mediaPeak,abs(d.x),abs(d.y),abs(d.z));quaternionPeak=maxOf(quaternionPeak,1-abs(q.dot(orientation(m))))
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("media-${r[0]}"))
        }
        assertEquals(1,builds,"no stable-frame text/camera preparation")
        for(i in listOf(rows.lastIndex,0,7,3,18,10)) {
            runOnIdle{current=i;mode="media"};assertTrue(mediaFrames.getValue(i).contentEquals(pixels()),"reverse seek")
        }
        val paused=pixels();mainClock.advanceTimeBy(1200);assertTrue(paused.contentEquals(pixels()))
        runOnIdle{mounted=false};pixels();assertNull(playback)
        runOnIdle{mounted=true};assertTrue(paused.contentEquals(pixels()),"remount")
        out.resolve("numeric-$width.txt").writeText("sourceFrames=${rows.size} finitePosePeak=$finitePeak gate=1e-9 adaptiveMediaVsSourceFinitePositionResidual=$mediaPeak quaternionOneMinusDot=$quaternionPeak\nMedia residual includes prior outgoing reconstruction; no local-only or raw RAF equivalence claim\n")
    }
}
