package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.ClassicLine
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class RendererTest {
    @Test fun maskBudgetFallbackCanRecoverWithoutReplacingContent() = runDesktopComposeUiTest(width = 800, height = 600) {
        val original = KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Light", 1_000, 2_000)),
            null, KaraokeAlignment.Unspecified, 1_000, 2_000)
        val line = normalize(SyncedLyrics(listOf(original))).lines.single()
        var budget by mutableLongStateOf(16L * 1024 * 1024)
        var actual: FoliaQuality? = null
        setContent {
            ClassicLine(line, { FoliaPlaybackSnapshot(1_500, false) }, TextStyle(fontSize = 48.sp),
                FoliaColors(), FoliaOptions(), ClassicTuning(), 360.dp,
                maskBudgetBytes = budget, onPrepared = { quality, _ -> actual = quality })
        }
        waitForIdle()
        runOnIdle { assertEquals(FoliaQuality.Standard, actual); budget = 0 }
        waitForIdle()
        runOnIdle { assertEquals(FoliaQuality.Minimal, actual); budget = 16L * 1024 * 1024 }
        waitForIdle()
        runOnIdle { assertEquals(FoliaQuality.Standard, actual) }
    }

    @Test fun statusFollowsPrimaryAcrossGapsAndReplacement() = runDesktopComposeUiTest(width = 800, height = 600) {
        fun line(text: String, start: Int, end: Int) = KaraokeLine.MainKaraokeLine(
            listOf(KaraokeSyllable(text, start, end)), null, KaraokeAlignment.Unspecified, start, end)
        var lyrics by mutableStateOf(SyncedLyrics(listOf(line("First", 1_000, 2_000), line("Next", 4_000, 5_000))))
        var position by mutableStateOf(FoliaPlaybackSnapshot(1_500, false))
        var status: FoliaStatus? = null
        setContent { FoliaLyricsView(lyrics, { position }, Modifier.fillMaxSize(),
            options = FoliaOptions(quality = FoliaQuality.Reduced), onStatus = { status = it }) }
        waitForIdle()
        runOnIdle {
            assertEquals(1, status?.mainUnitCount)
            assertEquals(FoliaQuality.Reduced, status?.actualQuality)
            position = FoliaPlaybackSnapshot(3_500, false, 1)
        }
        waitForIdle()
        runOnIdle {
            assertEquals(0, status?.mainUnitCount)
            assertEquals(FoliaQuality.Minimal, status?.actualQuality)
            assertFalse("GlowBudgetExceeded" in status!!.diagnostics)
            position = FoliaPlaybackSnapshot(4_500, false, 2)
        }
        waitForIdle()
        runOnIdle {
            assertEquals(1, status?.mainUnitCount)
            assertEquals(FoliaQuality.Reduced, status?.actualQuality)
            lyrics = SyncedLyrics(emptyList())
        }
        waitForIdle()
        runOnIdle {
            assertEquals(0, status?.mainUnitCount)
            assertEquals(FoliaQuality.Minimal, status?.actualQuality)
            assertTrue(status!!.diagnostics.isEmpty())
        }
    }

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
        onNodeWithContentDescription("Hello light").assertExists()
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
        onNodeWithContentDescription("Hello light").performTouchInput { click(Offset(4f, 4f)) }
        assertNull(clicked)
        val bright = firstPixels.indexOfFirst { ((it ushr 16) and 255) > 210 && ((it ushr 8) and 255) > 210 }
        assertTrue(bright >= 0)
        onRoot().performTouchInput { click(Offset((bright % first.width).toFloat(), (bright / first.width).toFloat())) }
        assertSame(original, clicked)
        clicked = null
        onNodeWithContentDescription("Hello light").performClick()
        assertSame(original, clicked)
    }
}
