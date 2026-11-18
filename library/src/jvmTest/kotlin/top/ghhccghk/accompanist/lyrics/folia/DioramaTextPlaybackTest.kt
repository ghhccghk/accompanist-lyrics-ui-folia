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
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaTextPlaybackTest {
    @Test fun incomingFlightUsesValueAnchorLiveCoreTargetsAndMediaReconstruction()=runDesktopComposeUiTest(width=800,height=600) {
        val outgoingLine=SyncedLine("Synthetic outgoing",null,700,2600)
        val first=SyncedLine("Incoming complete text",null,700,1500)
        val second=SyncedLine("Live target changes in flight",null,1900,2700)
        val core=SyncedLyrics(listOf(first,second))
        var viewport by mutableIntStateOf(800);var mounted by mutableStateOf(true)
        var time by mutableDoubleStateOf(1.6);var offset by mutableIntStateOf(0)
        var reduced by mutableStateOf(false);var prepared:DioramaTextPlayback?=null
        var anchor:DioramaTextTransition?=null;var builds=0
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val style=remember{TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))}
            val cut=remember {
                val outgoing=DioramaTextPlayback.prepare(SyncedLyrics(listOf(outgoingLine)),style,
                    TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,800.0,600.0,
                    seedName="Outgoing mirror",endTime=4.2)
                outgoing.transitionAt(1.2,1,200).also {
                    assertEquals(outgoing.camera.at(1.0),it.previous,"capture uses media - offset")
                    assertEquals(outgoing.camera.subjects.single().frame.position,it.outgoingOrigin)
                }
            }
            val playback=if(mounted)remember(viewport,cut) {
                builds++
                DioramaTextPlayback.prepare(core,style,TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,
                    viewport.toDouble(),600.0,shots=listOf(DioramaShot.orbit,DioramaShot.orbit),
                    seedName="Incoming mirror",endTime=4.2,transition=cut)
            }else null
            SideEffect{prepared=playback;anchor=cut}
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                playback?.draw(this,time,DioramaLinearColor(.74,.78,.84),DioramaLinearColor(.88,.20,.08),offset,reduced)
            }
        }
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val baseline=pixels();val initial=prepared!!;val cut=anchor!!
        val origin=cut.outgoingOrigin+dioramaTransitionOffset(dioramaHashSeed("Incoming mirror"),1)
        assertEquals(origin,initial.camera.subjects.first().frame.position)
        // Source dt0 still evaluates goal+(current-goal), with cancellation
        // roundoff. Preserve the source operator; compare numerical coordinates.
        val start=initial.camera.at(0.0).position
        assertEquals(cut.previous.position.x,start.x,1e-12,"no initial far seek snap x")
        assertEquals(cut.previous.position.y,start.y,1e-12,"no initial far seek snap y")
        assertEquals(cut.previous.position.z,start.z,1e-12,"no initial far seek snap z")
        assertEquals(cut.previous.velocity,initial.camera.at(0.0).velocity)
        assertSame(first,initial.sourceAt(1.6).source);assertSame(second,initial.sourceAt(2.0).source)
        assertEquals(2.1,initial.mediaStart(initial.sourceAt(2.1,200),200),1e-12)
        assertNotEquals(initial.camera.target(1.899999).eye,initial.camera.target(1.9).eye,"target stays live, not a fixed flight spline")
        assertTrue(baseline.toSet().size>2,"incoming text is actually drawn")
        runOnIdle{time=1.8;offset=200};assertTrue(baseline.contentEquals(pixels()),"offset once")
        runOnIdle{time=1.6;offset=0}
        val times=listOf(0.0,.7,1.6,1.899999,1.9,1.900001,2.4,3.199,3.2,3.201,4.2)
        val poses=times.associateWith{initial.camera.at(it)}
        val views=times.associateWith{initial.camera.perspectiveAt(it).eye}
        for(fps in listOf(24,60,144)) {
            repeat(fps*4){initial.camera.perspectiveAt(it.toDouble()/fps)}
            for(t in times.reversed()) {
                assertEquals(poses[t],initial.camera.at(t),"base refs display schedule $fps t$t")
                assertEquals(views[t],initial.camera.perspectiveAt(t).eye,"visual offsets display schedule $fps t$t")
            }
        }
        for(t in times.reversed()){runOnIdle{time=t};pixels()}
        runOnIdle{time=1.6};assertTrue(baseline.contentEquals(pixels()),"backward seek restores flight")
        mainClock.advanceTimeBy(1800);assertTrue(baseline.contentEquals(pixels()),"pause freezes camera and text")
        runOnIdle{reduced=true;time=.9};pixels()
        runOnIdle{time=1.4};pixels()
        // Reduced motion fixes the camera per subject; media highlight still
        // follows the lyric clock (it must not freeze at a different media time).
        assertEquals(initial.camera.at(.9,true),initial.camera.at(1.4,true))
        runOnIdle{reduced=false;time=1.6};assertTrue(baseline.contentEquals(pixels()))
        assertEquals(1,builds,"no reprepare for media/settings/offset")
        runOnIdle{viewport=380};assertFalse(baseline.contentEquals(pixels()))
        runOnIdle{viewport=800};assertTrue(baseline.contentEquals(pixels()),"resize reconstructs from same value anchor")
        runOnIdle{mounted=false};pixels();assertNull(prepared)
        runOnIdle{mounted=true};assertTrue(baseline.contentEquals(pixels()),"remount")
        assertEquals(4,builds)
    }
    @Test fun realCoreTextOffsetResizeReduceMotionAndRelease()=runDesktopComposeUiTest(width=800,height=600) {
        val first=SyncedLine("Synthetic opening line",null,700,1500)
        val second=SyncedLine("Another actual core line",null,1900,2700)
        val core=SyncedLyrics(listOf(first,second))
        var viewport by mutableIntStateOf(800);var mounted by mutableStateOf(true)
        var time by mutableDoubleStateOf(1.0);var offset by mutableIntStateOf(0)
        var reduced by mutableStateOf(false);var prepared:DioramaTextPlayback?=null;var count=0
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val playback=if(mounted)remember(viewport) {
                count++
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                DioramaTextPlayback.prepare(core,style,TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,
                    viewport.toDouble(),600.0,seedName="DioramaProbe",endTime=4.0)
            }else null
            SideEffect{prepared=playback}
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                playback?.draw(this,time,DioramaLinearColor(.74,.78,.84),DioramaLinearColor(.88,.20,.08),offset,reduced)
            }
        }
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val baseline=pixels();val playback=prepared!!
        assertTrue(baseline.toSet().size>2,"actual text must be drawn")
        assertSame(first,playback.sourceAt(1.0).source)
        // Existing public contract is media - offset (DESIGN.md:338).
        // The old internal-only check followed the implementation's reversed
        // convention; preserve that failure in the offset-contract evidence.
        assertSame(first,playback.sourceAt(1.7,200).source)
        assertSame(second,playback.sourceAt(2.1,200).source)
        for((i,subject)in playback.camera.subjects.withIndex())
            assertEquals(dioramaPickShot(i,playback.lines.map{it.core.lyric},dioramaHashSeed("DioramaProbe")),subject.shot,
                "default core path uses real weighted picker, no invented sections")
        assertEquals(2.1,playback.mediaStart(playback.sourceAt(2.1,200),200),1e-12)
        runOnIdle{time=1.2;offset=200};assertTrue(baseline.contentEquals(pixels()),"offset exactly once")
        // Cross the actual line switch and both sides of its exact boundary.
        for(t in listOf(1.899999,1.9,1.900001,2.2)) {runOnIdle{time=t;offset=0};pixels()}
        val later=pixels();assertFalse(later.contentEquals(baseline))
        runOnIdle{time=1.0};assertTrue(baseline.contentEquals(pixels()),"backward seek")
        runOnIdle{reduced=true};val reducedPixels=pixels()
        runOnIdle{reduced=false};assertTrue(baseline.contentEquals(pixels()),"settings restore")
        assertFalse(reducedPixels.contentEquals(baseline));assertEquals(1,count,"no reprepare for time/settings")
        runOnIdle{viewport=380};assertFalse(baseline.contentEquals(pixels()),"narrow projection")
        runOnIdle{viewport=800};assertTrue(baseline.contentEquals(pixels()),"resize restore")
        mainClock.advanceTimeBy(1500);assertTrue(baseline.contentEquals(pixels()),"pause freeze")
        runOnIdle{mounted=false};pixels();assertNull(prepared)
        runOnIdle{mounted=true};assertTrue(baseline.contentEquals(pixels()),"remount reconstruction")
    }
}
