package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.LocalClassicSoftwareLayers
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class TranslucentClassicTest {
    @Test fun liveAlphaChangesMatchFreshComponentAndSeekOnBothLayerPaths() = runDesktopComposeUiTest(width=800,height=600) {
        val lyrics=SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(listOf(
            KaraokeSyllable("Amber ",700,1300),KaraokeSyllable("echo",1300,1900)),null,KaraokeAlignment.Unspecified,700,1900)))
        val family=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val style=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=48.sp)
        var ink by mutableStateOf(Color(0x8091e8e1))
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1500,false))
        var generation by mutableStateOf(0)
        var minimal by mutableStateOf(false)
        var software by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalClassicSoftwareLayers provides software) {
                key(generation) {
                    FoliaLyricsView(lyrics,{snapshot},Modifier.fillMaxSize().background(Color(0xff162435)),
                        typography=FoliaTypography(main=style),colors=FoliaColors(text=Color(0xff415d8a),active=ink),
                        options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false,
                            quality=if(minimal)FoliaQuality.Minimal else FoliaQuality.Standard),
                        style=FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier=0f)))
                }
            }
        }
        val output=File("../build/verification/classic-translucent-live").apply {mkdirs()}
        fun pixels()=IntArray(800*600).also {captureToImage().readPixels(it)}
        for(fallback in listOf(false,true)) {
            runOnIdle {software=fallback};waitForIdle()
            for(alpha in listOf(128,64,0,255,128)) {
                runOnIdle {ink=Color((alpha.toLong() shl 24) or 0x91e8e1)};waitForIdle()
                val live=pixels()
                ImageIO.write(captureToImage().toAwtImage(),"png",output.resolve("software-$fallback-alpha-$alpha.png"))
                runOnIdle {generation++};waitForIdle()
                assertContentEquals(live,pixels(),"Alpha=$alpha live vs fresh; software=$fallback")
                mainClock.advanceTimeBy(1000);waitForIdle()
                assertContentEquals(live,pixels(),"Paused translucent masks")
                runOnIdle {snapshot=FoliaPlaybackSnapshot(750,false,snapshot.discontinuity+1)};waitForIdle()
                runOnIdle {snapshot=FoliaPlaybackSnapshot(1500,false,snapshot.discontinuity+1)};waitForIdle()
                assertContentEquals(live,pixels(),"Translucent seek reconstruction")
                if(alpha==0) {
                    runOnIdle {minimal=true};waitForIdle()
                    assertContentEquals(live,pixels(),"Transparent ink has no halo, exactly like no-shadow quality")
                    runOnIdle {minimal=false};waitForIdle()
                }
            }
        }
    }
}
