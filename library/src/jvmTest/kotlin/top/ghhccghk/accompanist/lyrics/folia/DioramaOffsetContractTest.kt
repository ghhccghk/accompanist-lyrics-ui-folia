package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import kotlin.test.*

/** Independent existing host contract: DESIGN.md's media - offset and both
 * sample click handlers' original line.start + offset. Not an upstream fixture
 * or a new convention chosen from this implementation's existing output. */
@OptIn(ExperimentalTestApi::class)
class DioramaOffsetContractTest {
    @Test fun mediaTimeAndOriginalSourceSeekUseTheExistingPublicOffsetDirection()=
        runDesktopComposeUiTest(width=800,height=600) {
            val first=SyncedLine("Synthetic opening line",null,700,1500)
            val second=SyncedLine("Another actual core line",null,1900,2700)
            val core=SyncedLyrics(listOf(first,second))
            var media by mutableDoubleStateOf(1.0);var offset by mutableIntStateOf(0)
            var reduced by mutableStateOf(false);var prepared:DioramaTextPlayback?=null;var prepares=0
            setContent {
                val resolver=LocalFontFamilyResolver.current
                val stage=remember {
                    prepares++
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                    DioramaTextPlayback.prepare(core,style,TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,
                        800.0,600.0,seedName="DioramaProbe",endTime=4.0)
                }
                SideEffect { prepared=stage }
                Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                    stage.draw(this,media,DioramaLinearColor(.74,.78,.84),DioramaLinearColor(.88,.20,.08),offset,reduced)
                }
            }
            fun pixels():IntArray {waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
            val baseline=pixels();val stage=prepared!!
            assertSame(first,stage.sourceAt(1.7,200).source,"+200ms must delay the next line to2.1s, not advance it to1.7s")
            assertSame(second,stage.sourceAt(1.7,-200).source,"negative offset advances the next line")
            for(delta in listOf(-1250,-200,0,200,1250)) {
                val opts=FoliaOptions(offsetMillis=delta)
                for(ms in listOf(-500L,0L,699L,700L,1000L,1699L,1700L,1899L,1900L,2100L,4000L,6000L)) {
                    val snapshot=FoliaPlaybackSnapshot(ms,false)
                    val expected=((snapshot.positionMillis-opts.offsetMillis)/1000.0).coerceIn(0.0,4.0)
                    assertEquals(expected,stage.lyricTime(ms/1000.0,delta),1e-12,"bounded camera input $ms/$delta")
                    assertSame(if(expected>=1.9)second else first,stage.sourceAt(ms/1000.0,delta).source,"source lookup $ms/$delta")
                }
                for(line in stage.lines) {
                    val expected=(line.core.source.start.toLong()+delta)/1000.0
                    val click=stage.mediaStart(line.core,delta)
                    assertEquals(expected,click,1e-12,"same original source and sample seek contract")
                    assertEquals(line.core.lyric.start,stage.lyricTime(click,delta),1e-12,"click round trip")
                }
                runOnIdle{media=1.0+delta/1000.0;offset=delta}
                assertContentEquals(baseline,pixels(),"all glyphs, internal clocks and camera offset exactly once ($delta)")
            }
            runOnIdle{reduced=true};val reducedFrame=pixels()
            runOnIdle{media=1.0;offset=0};assertContentEquals(reducedFrame,pixels(),"reduced camera shares offset contract")
            mainClock.advanceTimeBy(1500);assertContentEquals(reducedFrame,pixels(),"paused offset has no wall clock")
            assertEquals(1,prepares,"offset/seek/settings do not reprepare song")
        }
}
