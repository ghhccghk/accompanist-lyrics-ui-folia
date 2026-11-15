package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class WaitingStageTest {
    @Test fun publicStageUsesSameMediaTimeAcrossOffsetPauseSeekAndAllVoices() =
        runDesktopComposeUiTest(width=800,height=600) {
            val backing=KaraokeLine.AccompanimentKaraokeLine(listOf(KaraokeSyllable("Backing",21000,25000)),
                null,KaraokeAlignment.Unspecified,21000,25000)
            val source=SyncedLyrics(listOf(
                KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Amber",10000,12000)),
                    null,KaraokeAlignment.Unspecified,10000,12000,accompanimentLines=listOf(backing)),
                KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Echo",36000,38000)),
                    null,KaraokeAlignment.Unspecified,36000,38000)))
            var lyrics by mutableStateOf(source)
            var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false))
            var offset by mutableIntStateOf(0)
            var reduced by mutableStateOf(false)
            setContent {
                FoliaLyricsView(lyrics,{snapshot},Modifier.fillMaxSize().background(Color(0xFF162435)),
                    options=FoliaOptions(showNextLine=false,showTranslation=false,showPhonetic=false,
                        offsetMillis=offset,reducedMotion=reduced))
            }
            val out=File("build/verification/waiting-stage").apply{mkdirs()}
            val images=mutableMapOf<Int,List<Int>>()
            val grid=listOf(-1,0,1500,5000,9999,10000,11000,12000,18000,22000,25000,25301,29000,35000,36000,40000,60000)
            // Backing becomes the active visual line at 21s: its 25s exit must finish before dots.
            val waiting=setOf(0,1500,5000,9999,18000,25301,29000,35000)
            for(pass in 0..1) for(t in if(pass==0) grid else grid.reversed()) {
                runOnIdle{snapshot=FoliaPlaybackSnapshot(t.toLong(),false,snapshot.discontinuity+1)}
                if(t in waiting) onNodeWithContentDescription("等待演唱").assertExists("media=$t pass=$pass")
                else onNodeWithContentDescription("等待演唱").assertDoesNotExist()
                val img=captureToImage().toAwtImage()
                val pixels=img.getRGB(0,0,800,600,null,0,800).toList()
                if(pass==0) {images[t]=pixels;ImageIO.write(img,"png",File(out,"public-$t.png"))}
                else assertEquals(images[t],pixels,"Reverse revisit $t")
            }
            runOnIdle{snapshot=FoliaPlaybackSnapshot(5000,false,200)}
            val paused=captureToImage().toAwtImage()
            Thread.sleep(100)
            assertEquals(paused.getRGB(0,0,800,600,null,0,800).toList(),
                captureToImage().toAwtImage().getRGB(0,0,800,600,null,0,800).toList())
            runOnIdle{offset=500;snapshot=snapshot.copy(positionMillis=5500,discontinuity=201)}
            assertEquals(images[5000],captureToImage().toAwtImage().getRGB(0,0,800,600,null,0,800).toList(),
                "Offset shares the lyric media-time sign")
            runOnIdle{reduced=true}
            onNodeWithContentDescription("等待演唱").assertExists()
            ImageIO.write(captureToImage().toAwtImage(),"png",File(out,"public-reduced.png"))
            runOnIdle{lyrics=SyncedLyrics(emptyList())}
            onNodeWithContentDescription("等待演唱").assertDoesNotExist()
            onNodeWithText("暂无可显示歌词").assertExists()
        }
}
