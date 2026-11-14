package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import java.io.File
import org.jetbrains.skia.Image
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class RendererTest {
    @Test fun pausedSeekRebuildsSameFrameAndClickRetainsSource() = runDesktopComposeUiTest(width = 800, height = 600) {
        val original = KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Hello ", 1_000, 2_000),
            KaraokeSyllable("light", 2_000, 3_000)), "合成字幕", KaraokeAlignment.Unspecified, 1_000, 3_000)
        val lyrics = SyncedLyrics(listOf(original))
        var position by mutableStateOf(FoliaPlaybackSnapshot(1_500, false))
        var clicked: ISyncedLine? = null
        var quality by mutableStateOf(FoliaQuality.Standard)
        setContent { Box(Modifier.fillMaxSize().background(Color(0xFF162435))) {
            FoliaLyricsView(lyrics, { position }, Modifier.fillMaxSize(), options = FoliaOptions(quality = quality), onLineClicked = { clicked = it })
        } }
        onNodeWithContentDescription("Hello light").assertExists().performClick()
        assertSame(original, clicked)
        onNodeWithText("合成字幕").assertExists()
        val first = captureToImage()
        val firstPixels = IntArray(first.width * first.height)
        first.readPixels(firstPixels)
        runOnIdle { position = FoliaPlaybackSnapshot(2_500, false, 1) }
        waitForIdle()
        runOnIdle { position = FoliaPlaybackSnapshot(1_500, false, 2) }
        waitForIdle()
        val restored = captureToImage()
        val restoredPixels = IntArray(restored.width * restored.height)
        restored.readPixels(restoredPixels)
        assertContentEquals(firstPixels, restoredPixels)
        val directory = File("build/verification").apply { mkdirs() }
        javax.imageio.ImageIO.write(restored.toAwtImage(), "png", File(directory, "classic-800x600-1500ms.png"))
        runOnIdle { quality = FoliaQuality.Minimal }
        waitForIdle()
        javax.imageio.ImageIO.write(captureToImage().toAwtImage(), "png", File(directory, "classic-minimal-800x600-1500ms.png"))
    }
}
