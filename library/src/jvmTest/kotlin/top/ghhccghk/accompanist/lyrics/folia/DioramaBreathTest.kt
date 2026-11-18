package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import kotlin.math.PI
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaBreathTest {
    @Test fun originalPhaseRangeAndReducedMotionArePure() {
        assertEquals(.9,dioramaBreathAt(0.0))
        assertEquals(1.0,dioramaBreathAt(PI/(2*1.9)),1e-15)
        assertEquals(.8,dioramaBreathAt(3*PI/(2*1.9)),1e-15)
        val samples=(0..200).map { it/13.0 }
        val expected=samples.associateWith { dioramaBreathAt(it) }
        for(t in samples.shuffled(Random(725))) {
            assertEquals(expected[t],dioramaBreathAt(t))
            assertTrue(dioramaBreathAt(t) in .8..1.0)
            assertEquals(1.0,dioramaBreathAt(t,true))
        }
        assertFailsWith<IllegalArgumentException> { dioramaBreathAt(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { dioramaBreathAt(Double.POSITIVE_INFINITY) }
    }

    @Test fun actualTextBreathFreezesAndReconstructsWithOffsetCutLoopAndResize() = runDesktopComposeUiTest(width=800,height=600) {
        fun core(n:Int,label:String)=SyncedLyrics((0 until n).map {
            SyncedLine("Synthetic $label line $it",null,700+it*1400,1500+it*1400)
        })
        val oldCore=core(8,"outgoing");val core=core(3,"incoming")
        val primary=DioramaLinearColor(.738,.784,.839);val accent=DioramaLinearColor(.88,.198,.074)
        var viewport by mutableIntStateOf(800);var mounted by mutableStateOf(true)
        var media by mutableDoubleStateOf(1.2);var offset by mutableIntStateOf(0)
        var reduced by mutableStateOf(false);var override by mutableStateOf<Double?>(null)
        var prepared:DioramaTextPlayback?=null;var builds=0
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val style=remember { TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))) }
            val measurer=remember { TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr) }
            val old=remember { DioramaTextPlayback.prepare(oldCore,style,measurer,resolver,800.0,600.0,seedName="Outgoing mirror",endTime=12.0) }
            val cut=remember { old.cutAt(6.8,1).also {
                assertEquals(old.breathAt(6.8),dioramaBreathAt(it.anchor.breathSeconds))
                val loop=old.loopAt(6.8,1)
                assertEquals(old.breathAt(6.8),loop.breathAt(0.0))
                assertSame(old.lines,loop.lines)
            } }
            val ready=if(mounted)remember(viewport) {
                builds++
                DioramaTextPlayback.prepare(core,style,measurer,resolver,viewport.toDouble(),600.0,
                    seedName="Incoming mirror",endTime=5.0,cut=cut).also {
                    assertEquals(6.8,it.breathOriginSeconds)
                    assertEquals(old.breathAt(6.8),it.breathAt(0.0))
                    assertEquals(11.4,it.transitionAt(4.6,2).breathSeconds,1e-12)
                }
            }else null
            SideEffect { prepared=ready }
            Canvas(Modifier.size(viewport.dp,600.dp).background(Color(0xff162435))) {
                if(ready!=null) {
                    val t=ready.lyricTime(media,offset);val s=ready.camera.subjects[ready.camera.subjectIndex(t)]
                    val center=dioramaComposeLocal(s.frame,s.placement.offsetR,s.placement.offsetU,0.0)
                    val normal=dioramaPlaneAxis(s.frame,s.placement,1.0,0.0).cross(dioramaPlaneAxis(s.frame,s.placement,0.0,1.0)).normalized()
                    // Fixed readable view isolates glow modulation from the
                    // separately verified follower. It is not an upstream PNG.
                    val view=DioramaPerspective(center+normal*5.2,center,viewport.toDouble(),600.0)
                    ready.drawStage(this,t,view,primary,accent,reducedMotion=reduced,breath=override)
                }
            }
        }
        fun pixels():IntArray { waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also { im.readPixels(it) } }
        val baseline=pixels();assertEquals(1,builds)
        val ready=prepared!!
        assertSame(core.lines[0],ready.sourceAt(1.2).source)
        assertEquals(dioramaBreathAt(6.8+2.1),ready.breathAt(2.3,200))
        assertEquals(ready.breathAt(1.2),ready.breathAt(1.0,-200))
        mainClock.advanceTimeBy(1700);assertTrue(baseline.contentEquals(pixels()),"UI time must not advance glow breath")
        runOnIdle { override=1.0 };assertFalse(baseline.contentEquals(pixels()),"production glow must use the nonconstant phase")
        runOnIdle { override=null };assertTrue(baseline.contentEquals(pixels()))
        val snapshots=mutableMapOf<Double,IntArray>()
        for(t in listOf(.9,1.1,1.2,1.45,2.6)) { runOnIdle { media=t };snapshots[t]=pixels() }
        for(t in listOf(2.6,1.2,.9,1.45,1.1)) { runOnIdle { media=t };assertTrue(snapshots.getValue(t).contentEquals(pixels())) }
        runOnIdle { media=1.3;offset=200 };assertTrue(snapshots.getValue(1.1).contentEquals(pixels()),"same offset for text and breath")
        runOnIdle { media=1.2;offset=0;viewport=380 };pixels()
        assertEquals(6.8,prepared!!.breathOriginSeconds)
        runOnIdle { viewport=800 };assertTrue(baseline.contentEquals(pixels()),"resize restore retains explicit cut phase")
        runOnIdle { mounted=false };pixels();assertNull(prepared)
        runOnIdle { mounted=true };assertTrue(baseline.contentEquals(pixels()),"remount restores immutable phase")
        runOnIdle { reduced=true };val reducedPixels=pixels()
        assertEquals(1.0,prepared!!.breathAt(media,offset,true))
        runOnIdle { override=1.0 };assertTrue(reducedPixels.contentEquals(pixels()),"reduced motion removes periodic glow modulation")
        File("../build/verification/diorama-mounted-kotlin/breath-contract.txt").also { it.parentFile.mkdirs() }.writeText(
            "Actual full-shaped stage: nonconstant glow differs from breath1; pause/UI-time, reverse seek, offset, cut/loop phase, resize restore, remount and reduced motion passed. No wall RAF/device/memory equivalence claim.\n")
    }
}
