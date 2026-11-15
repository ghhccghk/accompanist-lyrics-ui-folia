package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Public renderer at CSS/device densities: static layout snap must not quantize media motion. */
@OptIn(ExperimentalTestApi::class)
class LayoutPaintParityTest {
    @Test fun wideDensity1() = capture(800, 1)
    @Test fun wideDensity2() = capture(800, 2)
    @Test fun narrowDensity1() = capture(380, 1)
    @Test fun narrowDensity2() = capture(380, 2)
    private fun capture(width: Int, dpr: Int) = runDesktopComposeUiTest(width = width * dpr, height = 600 * dpr) {
        val lyrics = SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(listOf(
            KaraokeSyllable("Amber ",700,1300),KaraokeSyllable("echo",1300,1900)),
            null,KaraokeAlignment.Unspecified,700,1900)))
        val family = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(750,false))
        var avoid by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(dpr.toFloat(),1f)) {
                FoliaLyricsView(lyrics,{snapshot},Modifier.fillMaxSize().background(Color(0xFF162435)),
                    typography=FoliaTypography(secondary=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold)),
                    options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false),
                    style=FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier=0f,avoidOverlap=avoid)))
            }
        }
        val out = File("../build/verification/layout-phase-public-kotlin").apply {mkdirs()}
        for (mode in listOf(false,true)) {
            val frames = mutableMapOf<Long,IntArray>()
            val times = listOf(750L,1000L,1500L,1900L,2100L)
            for (ms in times + times.reversed()) {
                runOnIdle {avoid=mode;snapshot=FoliaPlaybackSnapshot(ms,false,snapshot.discontinuity+1)};waitForIdle()
                val frame=captureToImage()
                val pixels=IntArray(width*600*dpr*dpr).also {frame.readPixels(it)}
                mainClock.advanceTimeBy(1000);waitForIdle()
                assertContentEquals(pixels,IntArray(pixels.size).also {captureToImage().readPixels(it)},"Paused $width/$dpr/$mode/$ms")
                if(ms in frames) assertContentEquals(frames.getValue(ms),pixels,"Reverse seek $width/$dpr/$mode/$ms")
                else {
                    frames[ms]=pixels
                    ImageIO.write(frame.toAwtImage(),"png",out.resolve("dpr$dpr-$width-${if(mode) "avoid" else "default"}-${ms}ms.png"))
                }
            }
        }
        out.resolve("conditions-$width-dpr$dpr.txt").writeText("CSS ${width}x600; device density$dpr/fontScale1; public FoliaLyricsView; pinned FoliaParityLatin-Bold; original baseline synthetic line; no captions; media snapshots paused/forward/reverse; no breathing; original time grid in capture-layout-phase.mjs\n")
    }
}
