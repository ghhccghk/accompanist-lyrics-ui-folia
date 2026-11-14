package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.ClassicLine
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Exact same fixture/font/surface as classic-browser/main.tsx; no sample chrome. */
@OptIn(ExperimentalTestApi::class)
class ClassicParityTest {
    @Test fun fixedFontCoreCheckpoints() = runDesktopComposeUiTest(width = 800, height = 600) {
        fun line(a: String, b: String, start: Int, middle: Int, end: Int) = KaraokeLine.MainKaraokeLine(
            listOf(KaraokeSyllable(a, start, middle), KaraokeSyllable(b, middle, end)),
            null, KaraokeAlignment.Unspecified, start, end)
        val document = normalize(SyncedLyrics(listOf(line("Amber ", "echo", 700, 1300, 1900),
            line("Silver ", "light", 3200, 3650, 4100))))
        val timeline = VisualTimeline(document.lines)
        val font = FontFamily(Font(File("../verification/assets/NotoSans-Bold.ttf"), FontWeight.Bold))
        val style = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 58.56.sp)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false))
        var avoid by mutableStateOf(false)
        setContent {
            val selection = timeline.at(snapshot.positionMillis * 1000)
            Box(Modifier.fillMaxSize().background(Color(0xFF162435)).padding(horizontal = 32.dp), contentAlignment = Alignment.Center) {
                val epochs = selection.exiting + listOfNotNull(selection.primary)
                epochs.forEach { epoch -> key(epoch) {
                    ClassicLine(document.lines[epoch.line], { snapshot }, style, FoliaColors(), FoliaOptions(),
                        ClassicTuning(breathingFloatMultiplier = 0f, avoidOverlap = avoid), 356.dp,
                        enterUs = epoch.enterUs, leaveUs = epoch.leaveUs)
                } }
            }
        }
        val directory = File("../build/verification/classic-kotlin").apply { mkdirs() }
        val checkpoints = listOf(0,700,750,850,1000,1200,1300,1500,1800,1900,2100,2220,2300,2500,3000,3200,3400,3700,4100,4400,4700)
        listOf(false, true).forEach { mode ->
            runOnIdle { avoid = mode }
            checkpoints.forEach { time ->
                runOnIdle { snapshot = FoliaPlaybackSnapshot(time.toLong(), false, snapshot.discontinuity + 1) }
                waitForIdle()
                val image = captureToImage().toAwtImage()
                assertEquals(800, image.width); assertEquals(600, image.height)
                ImageIO.write(image, "png", File(directory, "${if(mode) "avoid" else "default"}-${time}ms.png"))
            }
        }
        File(directory, "conditions.txt").writeText("800x600 density1; NotoSans-Bold.ttf pinned SHA in verification/assets/README.md; same main.tsx input. Paused direct media reconstruction. Core only, no chrome/subtitles. JVM ${System.getProperty("java.version")}.\n")
    }
}
