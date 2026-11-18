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
