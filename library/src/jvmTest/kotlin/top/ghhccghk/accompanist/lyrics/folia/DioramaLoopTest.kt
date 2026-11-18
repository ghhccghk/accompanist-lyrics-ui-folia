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
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.CancellationException
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaLoopTest {
    @Test fun originalOverlappingLoopWindowsAndDepartingTextMaterials() {
        val folder=File("../verification/diorama-fixtures/loop-window-v1")
        val windows=folder.resolve("windows.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        val controls=windows.associateBy{it[0]}
        assertEquals(26,windows.size)
        for(r in windows) {
            val total=r[1].toInt();val current=r[2].toInt();val outgoing=r[3].toInt().takeIf{it>=0}
            val indices=dioramaTransitionTextIndices(current,outgoing,total)
            assertEquals(r[4].split(',').map(String::toInt),indices,"source sorted union ${r[0]}")
            assertEquals(r[5].split(',').map{it=="1"},indices.map{dioramaIsOutgoing(it,current,outgoing)},"source closest-window ties ${r[0]}")
        }
        assertEquals(emptyList(),dioramaTransitionTextIndices(0,null,0))
        val rows=folder.resolve("materials.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        assertEquals(436,rows.size)
        val seed=dioramaHashSeed("DioramaLoopWindow");val primary=DioramaLinearColor(.74,.78,.84)
        val secondary=DioramaLinearColor(.23,.31,.42)
        var peak=0.0
        for(r in rows) {
            fun d(i:Int)=r[i].toDouble()
            val control=controls.getValue(r[0]);val total=control[1].toInt();val current=control[2].toInt()
            val outgoing=control[3].toInt().takeIf{it>=0};val index=r[5].toInt()
            val frame=buildDioramaPath(total,seed)[index].let{it.copy(position=it.position+DioramaVec(3.0,2.0,-6.0))}
            val actual=dioramaNeighborFrame(index,current,300+index*37,128,frame,dioramaTextPlacement(index,seed),
                DioramaPerspective(DioramaVec(d(2),d(3),d(4)),DioramaVec(0.0,0.0,-10.0),d(1),600.0),primary,secondary,outgoing)
            val values=listOf(actual.fit,actual.opacity,actual.color.red,actual.color.green,actual.color.blue,
                actual.center.x,actual.center.y,actual.center.z,actual.depth)
            for((i,value)in values.withIndex()) {
                peak=max(peak,abs(d(7+i)-value));assertEquals(d(7+i),value,1e-9,"original material ${r[0]} index$index field$i")
            }
            assertEquals(r[6]=="1",dioramaIsOutgoing(index,current,outgoing))
        }
        File("../build/verification/diorama-loop").apply{mkdirs()}.resolve("windows.txt")
            .writeText("originalWindows=${windows.size} originalMaterialRows=${rows.size} numericPeak=$peak gate=1e-9\n")
    }
    @Test fun originalLoopGeometryAndContinuousFiniteCamera() {
        val rows=File("../verification/diorama-fixtures/loop-v1/finite.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        assertEquals(32,rows.size)
        var peak=0.0;var mediaPeak=0.0;var scenarios=0
        for(group in rows.groupBy{it.take(3)}.values) {
            val first=group.first()
            fun d(i:Int)=first[i].toDouble()
            fun v(i:Int)=DioramaVec(d(i),d(i+1),d(i+2))
            fun q(i:Int)=DioramaQuaternion(d(i),d(i+1),d(i+2),d(i+3))
            val seed=dioramaHashSeed(String(Base64.getDecoder().decode(first[0]),Charsets.UTF_8))
            val width=d(1);val epoch=first[2].toInt();val origin=v(4)
            val previous=DioramaFollowState(v(7),v(10),v(13),v(16),d(19),q(20),q(24))
            val frames=buildDioramaPath(3,seed).map{it.copy(position=it.position+origin)}
            val subjects=(0..2).map { i->
                DioramaCameraSubject(DioramaCameraLine(DioramaLyric("Synthetic loop line $i",.7+i*1.4,1.5+i*1.4)),
                    frames[i],dioramaTextPlacement(i,seed),DioramaShot.orbit,12.0,i)
            }
            val base=DioramaCameraTrack(subjects,width,600.0,4.5,seed)
            val loop=base.restarted(previous,epoch)
            assertSame(base.subjects,loop.subjects,"original same corridor, not another offset")
            val initial=loop.perspectiveAt(0.0)
            for((expected,actual)in listOf(d(49) to initial.eye.x,d(50) to initial.eye.y,d(51) to initial.eye.z))
                assertEquals(expected,actual,1e-9,"source loop first rendered position")
            val flight=DioramaCameraFlight(previous.position,base.target(0.0).eye,seed,epoch)
            var state=previous;var last=0.0
            val probes=group.associateBy{it[3].toDouble()}
            val ticks=(probes.keys+(0..270).map{it/60.0}).distinct().sorted()
            for(t in ticks) {
                val s=subjects[base.subjectIndex(t)]
                val step=dioramaFollowStep(state,base.target(t),s.frame,s.placement,s.shot,t,
                    s.line.line.start,s.line.renderEnd,t-last,.42,width/600,flight=flight.at(t,.42))
                state=step.state;last=t
                probes[t]?.let{r->
                    val a=step.state;val aq=a.alignmentOrientation!!;val oq=a.orientation!!;val rendered=step.renderedOrientation!!
                    val values=listOf(a.position.x,a.position.y,a.position.z,a.velocity.x,a.velocity.y,a.velocity.z,
                        a.look.x,a.look.y,a.look.z,a.lookVelocity.x,a.lookVelocity.y,a.lookVelocity.z,a.alignment,
                        aq.x,aq.y,aq.z,aq.w,oq.x,oq.y,oq.z,oq.w,
                        step.renderedPosition.x,step.renderedPosition.y,step.renderedPosition.z,
                        rendered.x,rendered.y,rendered.z,rendered.w)
                    for((i,value)in values.withIndex()) {
                        val expected=r[28+i].toDouble();peak=max(peak,abs(expected-value))
                        assertEquals(expected,value,1e-9,"seed${first[0]} width$width t$t field$i")
                    }
                    assertEquals(r[56]=="1",flight.at(t,.42).flying)
                    assertEquals(r[57]=="1",step.snapped)
                    val delta=loop.perspectiveAt(t).eye-step.renderedPosition
                    mediaPeak=maxOf(mediaPeak,abs(delta.x),abs(delta.y),abs(delta.z))
                }
            }
            val expected=probes.keys.associateWith{loop.at(it)}
            for(fps in listOf(24,60,144)) {
                repeat(fps*4){loop.at(it.toDouble()/fps)}
                for(t in probes.keys.reversed())assertEquals(expected[t],loop.at(t),"seek is not a loop edge")
            }
            scenarios++
        }
        File("../build/verification/diorama-loop").apply{mkdirs()}.resolve("numeric.txt")
            .writeText("originalScenarios=$scenarios sourceProbeFrames=${rows.size} finiteStateAndPosePeak=$peak finiteGate=1e-9 adaptiveMediaVsSourceFinitePositionResidual=$mediaPeak\nNo source RAF equality claim for adaptive media track\n")
    }

    @Test fun realCoreLoopReusesTextAndWorldFramesAndReconstructsWithoutAUiTicker()=runDesktopComposeUiTest(width=800,height=600) {
        val originals=(0..2).map{SyncedLine("Synthetic loop line $it",null,700+it*1400,1500+it*1400)}
        val core=SyncedLyrics(originals)
        var time by mutableDoubleStateOf(1.6);var offset by mutableIntStateOf(0);var reduced by mutableStateOf(false)
        var mounted by mutableStateOf(true);var viewport by mutableIntStateOf(800)
        var builds=0;var outgoing:DioramaTextPlayback?=null;var playback:DioramaTextPlayback?=null
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val pair=if(mounted)remember(viewport) {
                builds++
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                val ready=DioramaTextPlayback.prepare(core,style,TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,
                    viewport.toDouble(),600.0,seedName="Incoming mirror",endTime=5.0,
                    transition=DioramaTextTransition(DioramaFollowState(),DioramaVec(0.0,0.0,0.0),2))
                ready to ready.loopAt(4.4,3,200)
            }else null
            SideEffect{outgoing=pair?.first;playback=pair?.second}
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                pair?.second?.draw(this,time,DioramaLinearColor(.74,.78,.84),DioramaLinearColor(.88,.20,.08),offset,reduced)
            }
        }
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val baseline=pixels();val old=outgoing!!;val loop=playback!!
        assertSame(old.lines,loop.lines,"no text, raster or timing reprepare")
        assertSame(old.camera.subjects,loop.camera.subjects,"frames and placements retained")
        assertSame(old.lines[0].text,loop.lines[0].text);assertSame(old.lines[0].neighbor,loop.lines[0].neighbor)
        assertEquals(2,loop.outgoingTextIndexAt(0.0));assertEquals(2,loop.outgoingTextIndexAt(3.199))
        assertNull(loop.outgoingTextIndexAt(3.2));assertNull(loop.outgoingTextIndexAt(3.201))
        assertNull(old.outgoingTextIndexAt(1.0),"normal prepared song has no loop window")
        assertEquals(listOf(0,1,2),loop.mountedTextIndicesAt(.7))
        assertSame(originals[0],loop.sourceAt(.8).source);assertSame(originals[1],loop.sourceAt(2.3).source)
        assertEquals(2.3,loop.mediaStart(loop.sourceAt(2.3,200),200),1e-12,"original click target plus offset")
        val previous=old.camera.at(4.2).position;val start=loop.camera.at(0.0).position
        assertEquals(previous.x,start.x,1e-12);assertEquals(previous.y,start.y,1e-12);assertEquals(previous.z,start.z,1e-12)
        val out=File("../build/verification/diorama-loop").apply{mkdirs()}
        val frames=mutableMapOf<Double,IntArray>()
        for(t in listOf(0.0,.7,1.6,2.099,2.1,3.199,3.2,3.201,3.5,4.5)) {
            runOnIdle{time=t};frames[t]=pixels()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("core-loop-${(t*1000).roundToInt()}.png"))
        }
        assertTrue(frames.values.any{it.toSet().size>2},"actual text, not just a camera fixture")
        for(t in frames.keys.reversed()) {runOnIdle{time=t};assertTrue(frames.getValue(t).contentEquals(pixels()))}
        runOnIdle{time=1.6};mainClock.advanceTimeBy(1500);assertTrue(baseline.contentEquals(pixels()),"pause")
        runOnIdle{time=1.8;offset=200};assertTrue(baseline.contentEquals(pixels()),"offset once")
        runOnIdle{offset=0;time=1.6;reduced=true};pixels()
        assertEquals(loop.camera.at(.8,true),loop.camera.at(1.4,true))
        runOnIdle{reduced=false};assertTrue(baseline.contentEquals(pixels()))
        assertEquals(1,builds,"time/seek/settings do not prepare another loop")
        var checks=0
        assertFailsWith<CancellationException>{old.loopAt(4.4,4,200,checkpoint={if(++checks==5)throw CancellationException("test")})}
        assertEquals(5,checks);assertEquals(previous,old.camera.at(4.2).position,"cancel leaves old playback intact")
        assertFailsWith<IllegalArgumentException>{old.loopAt(4.2,0)}
        runOnIdle{viewport=380};assertFalse(baseline.contentEquals(pixels()))
        runOnIdle{viewport=800};assertTrue(baseline.contentEquals(pixels()),"resize restore")
        runOnIdle{mounted=false};pixels();assertNull(playback);assertNull(outgoing)
        runOnIdle{mounted=true};assertTrue(baseline.contentEquals(pixels()),"remount")
        assertEquals(4,builds)
    }
}
