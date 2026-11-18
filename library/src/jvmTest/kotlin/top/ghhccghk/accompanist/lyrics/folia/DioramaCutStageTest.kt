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
import kotlinx.coroutines.CancellationException
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaCutStageTest {
    @Test fun boundedSnapshotCoversBothOriginalWindowsForShortAndLongSongs() {
        for(total in (1..128).toList()+10000)for(outgoing in listOf(0,minOf(1,total-1),total/2,total-1).distinct()) {
            val retained=dioramaDepartureTextIndices(outgoing,total)
            assertTrue(retained.size<=6)
            for(current in total..total+2)for(departing in listOf(outgoing,null)) {
                val required=dioramaTransitionTextIndices(current,departing,total+3).filter{it<total}
                assertTrue(retained.containsAll(required),"old count$total out$outgoing current$current window$departing")
            }
        }
    }
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        fun tsv(file:String)=File("../verification/diorama-fixtures/cut-stage-v1/$file.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        val rows=tsv("scene").filter{it[1].toInt()==width}
        val materials=tsv("neighbors").groupBy{it[0]}
        fun core(n:Int,label:String)=SyncedLyrics((0 until n).map{SyncedLine("Synthetic $label line $it",null,700+it*1400,1500+it*1400)})
        val oldCore=core(8,"outgoing");val core=core(3,"incoming")
        val seed=dioramaHashSeed("Incoming mirror")
        val primary=rows.first()[6].split(',').map(String::toDouble).let{DioramaLinearColor(it[0],it[1],it[2])}
        val secondary=rows.first()[7].split(',').map(String::toDouble).let{DioramaLinearColor(it[0],it[1],it[2])}
        val accent=rows.first()[8].split(',').map(String::toDouble).let{DioramaLinearColor(it[0],it[1],it[2])}
        var current by mutableIntStateOf(0);var mode by mutableStateOf("finite");var mounted by mutableStateOf(true)
        var offset by mutableIntStateOf(0);var reduced by mutableStateOf(false);var viewport by mutableIntStateOf(width)
        var builds=0;var drawn:DioramaPerspective?=null;var playback:DioramaTextPlayback?=null
        val finiteViews=mutableMapOf<String,DioramaPerspective>()
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val ready=if(mounted)remember(viewport) {
                builds++
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                val measurer=TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr)
                fun prepare(c:SyncedLyrics,name:String,cut:DioramaTextCut?=null,checkpoint:()->Unit={})=
                    DioramaTextPlayback.prepare(c,style,measurer,resolver,viewport.toDouble(),600.0,seedName=name,
                        endTime=if(c===oldCore)12.0 else 5.0,shots=List(c.lines.size){DioramaShot.orbit},cut=cut,checkpoint=checkpoint)
                val old=prepare(oldCore,"Outgoing mirror");val cut=old.cutAt(7.0,1,200)
                assertEquals(listOf(2,3,4,5,6,7),cut.departure.planes.map{it.index})
                assertEquals(8,cut.departure.sourceSpan);assertEquals(4,cut.departure.outgoingIndex)
                for(p in cut.departure.planes) {
                    assertSame(old.lines[p.index].neighbor,p.text,"reuse necessary plain raster only")
                    assertEquals(old.camera.subjects[p.index].frame,p.frame)
                    assertEquals(old.camera.subjects[p.index].placement,p.placement)
                }
                val result=prepare(core,"Incoming mirror",cut)
                assertSame(cut.departure,result.departure)
                assertEquals(8,result.globalStart)
                val sourceWidths=rows.first()[9].split(',').map(String::toDouble)
                for((i,line)in (old.lines+result.lines).withIndex())
                    assertEquals(sourceWidths[i]*128/.62,line.text.lineAdvance,.001,"independent shaped advance$i")
                assertEquals(4,result.outgoingTextIndexAt(0.0));assertNull(result.outgoingTextIndexAt(3.2))
                assertEquals(rows.first{it[3]=="3.2"}[18].split(',').map(String::toInt),
                    result.mountedTextIndicesAt(3.2),"source old tail window after flight; current global9 keeps7, not6")
                assertEquals(listOf(8,9,10),result.mountedTextIndicesAt(3.5),"tail leaves at actual line2 boundary")
                // No silent loss of a third still-visible corridor. Ordinary
                // seeks never create a cut, nor can snapshots chain recursively.
                assertFailsWith<IllegalArgumentException>{result.cutAt(1.0,2)}
                val next=result.cutAt(4.5,2)
                assertEquals(3,next.departure.sourceSpan)
                assertEquals(listOf(0,1,2),next.departure.planes.map{it.index})
                assertTrue(next.departure.planes.all{it.text===result.lines[it.index].neighbor})
                var checks=0
                assertFailsWith<CancellationException>{prepare(core,"Incoming mirror",cut){if(++checks==4)throw CancellationException("test")}}
                assertEquals(4,checks);assertEquals(cut.anchor,old.transitionAt(6.8,1),"failed prepare preserves previous scene")
                fun advance(track:DioramaCameraTrack,previous:DioramaFollowState,t:Double,dt:Double,flight:DioramaCameraFlight?):DioramaFollowResult {
                    val s=track.subjects[track.subjectIndex(t)]
                    return dioramaFollowStep(previous,track.target(t),s.frame,s.placement,s.shot,t,s.line.line.start,s.line.renderEnd,
                        dt,.42,viewport/600.0,flight=flight?.at(t,.42))
                }
                for(fps in listOf(60,144)) {
                    var state=DioramaFollowState();var last=0.0
                    val outgoing=(listOf(0.0,6.8)+(1..ceil(6.8*fps).toInt()).map{it.toDouble()/fps}.filter{it<=6.8}).distinct().sorted()
                    for(t in outgoing){state=advance(old.camera,state,t,t-last,null).state;last=t}
                    val flight=DioramaCameraFlight(state.position,result.camera.target(0.0).eye,seed,1)
                    val probes=rows.filter{it[2].toInt()==fps}.map{it[3].toDouble()}
                    val ticks=(listOf(0.0)+probes+(1..ceil(4.6*fps).toInt()).map{it.toDouble()/fps}.filter{it<=4.6}).distinct().sorted()
                    last=0.0
                    for(t in ticks) {
                        val step=advance(result.camera,state,t,t-last,flight);state=step.state;last=t
                        if(t in probes)finiteViews["$fps-$t"]=DioramaPerspective(step.renderedPosition,state.look,
                            viewport.toDouble(),600.0,orientation=step.renderedOrientation)
                    }
                }
                result
            }else null
            SideEffect{playback=ready}
            val r=rows[current]
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if(ready!=null) {
                    val time=ready.lyricTime(if(offset==0)r[3].toDouble() else (r[3].toDouble()*1000+offset)/1000,offset)
                    val view=if(mode=="finite")finiteViews.getValue("${r[2]}-$time")else ready.camera.perspectiveAt(time,reduced)
                    drawn=view;ready.drawStage(this,time,view,primary,accent,secondary,reduced)
                }
            }
        }
        val out=File("../build/verification/diorama-cut-stage-kotlin").apply{mkdirs()}
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        fun orientation(view:DioramaPerspective)=DioramaQuaternion.basis(view.right,view.up,view.forward * -1.0)
        val mediaFrames=mutableMapOf<Int,IntArray>();val schedules=mutableMapOf<String,IntArray>()
        var finitePeak=0.0;var materialPeak=0.0;var mediaPeak=0.0;var quaternionPeak=0.0;var materialCount=0
        for(i in rows.indices) {
            runOnIdle{current=i;mode="finite"};pixels()
            val r=rows[i];val time=r[3].toDouble();val p=playback!!;val view=drawn!!;val q=orientation(view)
            val sourceQ=DioramaQuaternion(r[13].toDouble(),r[14].toDouble(),r[15].toDouble(),r[16].toDouble())
            val sign=if(q.dot(sourceQ)<0)-1 else 1
            val values=listOf(view.eye.x,view.eye.y,view.eye.z,q.x*sign,q.y*sign,q.z*sign,q.w*sign)
            for((field,value)in values.withIndex()) {
                val expected=r[10+field].toDouble();finitePeak=max(finitePeak,abs(value-expected))
                assertEquals(expected,value,1e-9,"original finite ${r[0]} field$field")
            }
            assertSame(core.lines[r[4].toInt()-8],p.sourceAt(time).source)
            assertEquals(r[5].toInt().takeIf{it>=0},p.outgoingTextIndexAt(time))
            assertEquals(r[18].split(',').map(String::toInt),p.mountedTextIndicesAt(time))
            val source=materials.getValue(r[0]).associateBy{it[1].toInt()}
            val neighbors=p.neighborFramesAt(time,view,primary,secondary)
            assertEquals(source.keys,neighbors.map{it.index}.toSet(),"no missing/leaked text nodes")
            assertEquals(source.values.sortedWith(compareByDescending<List<String>>{it[11].toDouble()}.thenBy{it[1].toInt()})
                .map{it[1].toInt()},neighbors.map{it.index},"transparent centre-depth draw order")
            for(n in neighbors) {
                val expected=source.getValue(n.index)
                assertEquals(expected[2]=="1",dioramaIsOutgoing(n.index,r[4].toInt(),p.outgoingTextIndexAt(time)))
                val actual=listOf(n.opacity,n.fit,n.color.red,n.color.green,n.color.blue,n.center.x,n.center.y,n.center.z,n.depth)
                for((field,value)in actual.withIndex()) {
                    materialPeak=max(materialPeak,abs(expected[3+field].toDouble()-value))
                    assertEquals(expected[3+field].toDouble(),value,1e-9,"source neighbor ${r[0]} ${n.index} field$field")
                }
                materialCount++
            }
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("finite-${r[0]}"))
            runOnIdle{mode="media"};mediaFrames[i]=pixels()
            schedules[r[3]]?.let{assertTrue(it.contentEquals(mediaFrames.getValue(i)),"source display fps cannot affect media path")}
            schedules[r[3]]=mediaFrames.getValue(i)
            val m=drawn!!;val d=m.eye-view.eye
            mediaPeak=maxOf(mediaPeak,abs(d.x),abs(d.y),abs(d.z));quaternionPeak=maxOf(quaternionPeak,1-abs(q.dot(orientation(m))))
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("media-${r[0]}"))
        }
        assertEquals(1,builds,"stable ticks never prepare a whole song or snapshot")
        val p=playback!!;val expectedStates=(0..6).associate{it*.7 to p.camera.at(it*.7)}
        for(fps in listOf(24,60,144)) {
            repeat(fps*4){p.camera.at(it.toDouble()/fps)}
            for((t,s)in expectedStates.entries.reversed())assertEquals(s,p.camera.at(t))
        }
        for(i in listOf(rows.lastIndex,0,7,3,18,10)) {
            runOnIdle{current=i;mode="media"};assertTrue(mediaFrames.getValue(i).contentEquals(pixels()),"reverse seek")
        }
        // Integer-ms time/offset semantics shared with normal playback.
        val exact=rows.indexOfFirst{it[3]=="2.1"};runOnIdle{current=exact;offset=200}
        assertTrue(mediaFrames.getValue(exact).contentEquals(pixels()),"offset applied once including camera and tail window")
        val paused=pixels();mainClock.advanceTimeBy(1200);assertTrue(paused.contentEquals(pixels()),"pause")
        runOnIdle{reduced=true};val reducedPixels=pixels()
        mainClock.advanceTimeBy(500);assertTrue(reducedPixels.contentEquals(pixels()))
        runOnIdle{reduced=false;offset=0};assertTrue(paused.contentEquals(pixels()))
        runOnIdle{viewport=if(width==800)380 else 800};pixels()
        runOnIdle{viewport=width};assertTrue(paused.contentEquals(pixels()),"resize restore")
        runOnIdle{mounted=false};pixels();assertNull(playback)
        runOnIdle{mounted=true};assertTrue(paused.contentEquals(pixels()),"remount")
        assertEquals(4,builds)
        out.resolve("numeric-$width.txt").writeText("sourceFrames=${rows.size} finitePosePeak=$finitePeak materialRows=$materialCount materialPeak=$materialPeak gate=1e-9 adaptiveMediaVsSourceFinitePositionResidual=$mediaPeak quaternionOneMinusDot=$quaternionPeak\nMedia includes outgoing history; not local-only/RAF equivalence or public/Android acceptance\n")
    }
}
