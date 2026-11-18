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
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaOffsetBoundaryTest {
    @Test fun exactCoreMillisecondBoundaryAndSubMillisecondNeighbors()=runDesktopComposeUiTest(width=800,height=600) {
        val first=SyncedLine("Before boundary",null,700,1500)
        val second=SyncedLine("Exact source boundary",null,2100,2900)
        var prepared:DioramaTextPlayback?=null
        var time by mutableDoubleStateOf(2.1);var offset by mutableIntStateOf(0)
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val playback=remember {
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                DioramaTextPlayback.prepare(SyncedLyrics(listOf(first,second)),style,
                    TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,800.0,600.0,
                    seedName="Offset boundary",endTime=4.2)
            }
            SideEffect{prepared=playback}
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                playback.draw(this,time,DioramaLinearColor(.74,.78,.84),DioramaLinearColor(.88,.20,.08),offset)
            }
        }
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val exact=pixels();val playback=prepared!!
        val offsets=listOf(-1373,-201,-200,-1,0,1,137,199,200,999)
        for(value in offsets) {
            val media=(2100+value)/1000.0
            assertEquals(2.1,playback.lyricTime(media,value),0.0,"exact millisecond source time offset$value")
            assertSame(second,playback.sourceAt(media,value).source,"no preceding line at exact boundary offset$value")
            assertSame(first,playback.sourceAt(media-1e-7,value).source,"strict before, no epsilon offset$value")
            assertSame(second,playback.sourceAt(media+1e-7,value).source,"strict after offset$value")
            assertEquals(media,playback.mediaStart(playback.sourceAt(media,value),value),0.0,"click inverse in milliseconds")
            runOnIdle{time=media;offset=value};assertTrue(exact.contentEquals(pixels()),"same full text/camera at exact offset boundary")
        }
        assertEquals(1.0300001,playback.lyricTime(1.3430001,313),1e-12,"fractional media time not quantized to ms")
        assertEquals(0.0,playback.lyricTime(-1.0,200));assertEquals(4.2,playback.lyricTime(50.0,-200))
        assertFailsWith<IllegalArgumentException>{playback.lyricTime(Double.NaN,200)}
    }
}
