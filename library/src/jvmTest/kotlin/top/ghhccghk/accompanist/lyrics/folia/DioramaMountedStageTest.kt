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

/** Complete original mounted components, not the old isolated orbit override.
 * Finite control follows the actual source callback order using OWN widths.
 * Production remains reconstructible media; no source pose drives drawing. */
@OptIn(ExperimentalTestApi::class)
class DioramaMountedStageTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        val rows=File("../verification/diorama-fixtures/mounted-cut-v1/steps.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[1].toInt()==width}
        val times=listOf(0.0,.7,1.6,2.099,2.1,3.199,3.2,3.201,3.499,3.5,4.5,4.6)
        val probes=rows.filter{it[0]=="incoming"&&it[3].toDouble() in times}
        fun core(n:Int,label:String)=SyncedLyrics((0 until n).map{SyncedLine("Synthetic $label line $it",null,700+it*1400,1500+it*1400)})
        val oldCore=core(8,"outgoing");val core=core(3,"incoming")
        fun color(hex:Int):DioramaLinearColor {
            fun channel(shift:Int)=((hex shr shift and 255)/255.0).let{if(it<=.04045)it/12.92 else ((it+.055)/1.055).pow(2.4)}
            return DioramaLinearColor(channel(16),channel(8),channel(0))
        }
        val primary=color(0xdfe5ec);val secondary=color(0x758daa);val accent=color(0xf17b4d)
        val fog=DioramaFog(Color(0xff162435))
        var current by mutableIntStateOf(0);var method by mutableStateOf("finite");var mounted by mutableStateOf(true)
        var prepared:DioramaTextPlayback?=null;var drawn:DioramaPerspective?=null;var builds=0
        val finiteViews=mutableMapOf<String,DioramaPerspective>()
        var controlPeak=0.0;var widthPeak=0.0;var stepCount=0
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val ready=if(mounted)remember {
                builds++
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                val measurer=TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr)
                val old=DioramaTextPlayback.prepare(oldCore,style,measurer,resolver,width.toDouble(),600.0,
                    seedName="Outgoing mirror",endTime=12.0)
                val result=DioramaTextPlayback.prepare(core,style,measurer,resolver,width.toDouble(),600.0,
                    seedName="Incoming mirror",endTime=5.0,cut=old.cutAt(6.8,1))
                controlPeak=0.0;widthPeak=0.0;stepCount=0
                for((fps,group)in rows.groupBy{it[2]}) {
                    var state=DioramaFollowState();var previousWidth=0.0;var lastWall=0.0;var flight:DioramaCameraFlight?=null
                    for(r in group) {
                        val time=r[3].toDouble();val wall=r[4].toDouble();val incoming=r[0]=="incoming"
                        val track=if(incoming)result.camera else old.camera
                        val subject=track.subjects[track.subjectIndex(time)]
                        val seed=dioramaHashSeed(if(incoming)"Incoming mirror"else"Outgoing mirror")
                        if(incoming&&flight==null)flight=DioramaCameraFlight(state.position,
                            subject.line.target(time,subject.frame,subject.placement,seed,subject.localIndex,
                                subject.shot,previousWidth,width/600.0).eye,seed,1)
                        assertEquals(r[9],subject.shot.name,"actual original default shot, not override")
                        assertEquals(r[10].toDouble(),previousWidth,1e-9,"original width read BEFORE text callback")
                        val target=subject.line.target(time,subject.frame,subject.placement,seed,subject.localIndex,
                            subject.shot,previousWidth,width/600.0)
                        val step=dioramaFollowStep(state,target,subject.frame,subject.placement,subject.shot,time,
                            subject.line.line.start,subject.line.renderEnd,wall-lastWall,.42,width/600.0,
                            flight=if(incoming)flight!!.at(time,.42)else null)
                        state=step.state;lastWall=wall;previousWidth=subject.trackWidth
                        widthPeak=max(widthPeak,abs(r[11].toDouble()-previousWidth))
                        assertEquals(r[11].toDouble(),previousWidth,1e-9,"original measured width published AFTER text callback")
                        val aq=state.alignmentOrientation!!;val q=state.orientation!!;val visual=step.renderedOrientation!!
                        val actual=listOf(state.position.x,state.position.y,state.position.z,state.velocity.x,state.velocity.y,state.velocity.z,
                            state.look.x,state.look.y,state.look.z,state.lookVelocity.x,state.lookVelocity.y,state.lookVelocity.z,state.alignment,
                            aq.x,aq.y,aq.z,aq.w,q.x,q.y,q.z,q.w,step.renderedPosition.x,step.renderedPosition.y,step.renderedPosition.z,
                            visual.x,visual.y,visual.z,visual.w)
                        for((i,v)in actual.withIndex()) {
                            controlPeak=max(controlPeak,abs(r[12+i].toDouble()-v))
                            assertEquals(r[12+i].toDouble(),v,1e-9,"mounted source ${r.take(7)} field$i")
                        }
                        if(incoming&&time in times)finiteViews["$fps-$time"]=DioramaPerspective(step.renderedPosition,state.look,
                            width.toDouble(),600.0,orientation=visual)
                        stepCount++
                    }
                }
                result
            }else null
            SideEffect{prepared=ready}
            val r=probes[current];val time=r[3].toDouble()
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if(ready!=null) {
                    val view=if(method=="finite")finiteViews.getValue("${r[2]}-$time")else ready.camera.perspectiveAt(time)
                    drawn=view;ready.drawStage(this,time,view,primary,accent,secondary,fog=fog)
                }
            }
        }
        val out=File("../build/verification/diorama-mounted-kotlin").apply{mkdirs()}
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val media=mutableMapOf<String,IntArray>();var mediaPeak=0.0
        for(i in probes.indices) {
            runOnIdle{current=i;method="finite"};pixels()
            val r=probes[i];val name="mounted-$width-${r[2]}-${(r[3].toDouble()*1000).roundToInt()}.png"
            assertSame(core.lines[r[6].toInt()-8],prepared!!.sourceAt(r[3].toDouble()).source)
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("finite-$name"))
            val finite=drawn!!;runOnIdle{method="media"};val image=pixels()
            media[r[3]]?.let{assertTrue(it.contentEquals(image),"source display schedule cannot affect media reconstruction")};media[r[3]]=image
            val d=drawn!!.eye-finite.eye;mediaPeak=maxOf(mediaPeak,abs(d.x),abs(d.y),abs(d.z))
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("media-$name"))
        }
        assertEquals(1,builds)
        for(i in listOf(0,probes.lastIndex,3,12,7)) {
            runOnIdle{current=i;method="media"};assertTrue(media.getValue(probes[i][3]).contentEquals(pixels()))
        }
        val paused=pixels();mainClock.advanceTimeBy(800);assertTrue(paused.contentEquals(pixels()))
        runOnIdle{mounted=false};pixels();assertNull(prepared)
        runOnIdle{mounted=true};assertTrue(paused.contentEquals(pixels()))
        out.resolve("numeric-$width.txt").writeText("originalMountedSteps=$stepCount finiteStatePosePeak=$controlPeak widthPeak=$widthPeak gate=1e-9 adaptiveMediaPositionResidual=$mediaPeak\nFinite control reproduces original actual callback-width order with independently prepared core geometry; production media draws native analytic material without original RAF state injection\n")
    }
}
