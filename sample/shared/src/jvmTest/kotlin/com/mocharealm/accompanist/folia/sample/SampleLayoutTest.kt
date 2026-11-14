package com.mocharealm.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class SampleLayoutTest {
    @Test fun releasedProgressDragSeeksOnceAndKeepsPause() = runDesktopComposeUiTest(width = 900, height = 740) {
        val session = SampleSession()
        setContent { FoliaSampleApp(session = session, settingsStore = null) }
        onNodeWithContentDescription("演示进度").performTouchInput {
            down(Offset(10f, centerY))
            moveTo(Offset(width * .6f, centerY), delayMillis = 200)
            up()
        }
        waitForIdle()
        runOnIdle {
            assertTrue(session.clock.snapshot.positionMillis in 10_000..14_000)
            assertEquals(1, session.clock.snapshot.discontinuity)
            assertFalse(session.clock.snapshot.isPlaying)
        }
    }
    @Test fun interruptedProgressDragDiscardsPreviewWithoutSeeking() = runDesktopComposeUiTest(width = 900, height = 740) {
        val session = SampleSession()
        var visible by mutableStateOf(true)
        setContent { FoliaSampleApp(visible, session, settingsStore = null) }
        val progress = onNodeWithContentDescription("演示进度")
        progress.performTouchInput {
            down(Offset(10f, centerY))
            moveTo(Offset(width * .6f, centerY), delayMillis = 200)
        }
        runOnIdle { visible = false }
        waitForIdle()
        assertEquals(0f, progress.fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current)
        runOnIdle { assertEquals(0, session.clock.snapshot.positionMillis); assertFalse(session.clock.snapshot.isPlaying) }
    }
    @Test fun preferencesRestoreAndSaveThroughStore() = runDesktopComposeUiTest(width = 900, height = 740) {
        var saved = SampleSettings(options = com.mocharealm.accompanist.lyrics.folia.FoliaOptions(showTranslation = false)).encode()
        val store = object : SettingsStore {
            override suspend fun read() = saved
            override suspend fun write(value: String) { saved = value }
        }
        val session = SampleSession()
        setContent { FoliaSampleApp(session = session, settingsStore = store) }
        onNodeWithText("译文").performClick()
        onNodeWithText("译文 ✓").assertExists()
        runOnIdle { assertTrue(SampleSettings.decode(saved).options.showTranslation) }
    }
    @Test fun retainedSessionVisibilityDisposalAndKeyboard() = runDesktopComposeUiTest(width = 900, height = 740) {
        val session = SampleSession()
        var visible by mutableStateOf(true)
        var attached by mutableStateOf(true)
        setContent { if (attached) FoliaSampleApp(visible, session, settingsStore = null) }
        onRoot().performKeyInput { pressKey(Key.DirectionRight) }
        runOnIdle { assertEquals(5_000, session.clock.snapshot.positionMillis); session.clock.play(); visible = false }
        waitForIdle()
        runOnIdle { assertFalse(session.clock.snapshot.isPlaying); session.clock.seek(6_000); session.clock.changeSpeed(1.5); attached = false }
        waitForIdle()
        runOnIdle { attached = true; visible = true }
        onNodeWithText("1.5×").assertExists()
        runOnIdle { assertEquals(6_000, session.clock.snapshot.positionMillis); session.clock.play(); attached = false }
        waitForIdle()
        runOnIdle { assertFalse(session.clock.snapshot.isPlaying) }
    }
    @Test fun scenesControlsAndResizeKeepState() = runDesktopComposeUiTest(width = 1100, height = 800) {
        var width by mutableStateOf(1100.dp)
        setContent { Box(Modifier.width(width).height(740.dp)) { FoliaSampleApp(settingsStore = null) } }
        onNodeWithText("演示时钟 · 无音频").assertExists()
        onNodeWithText("1.0×").performClick()
        onNodeWithText("1.5×").assertExists()
        onNodeWithText("循环").performClick()
        onNodeWithText("循环 ✓").assertExists()
        val directory = File("build/verification").apply { mkdirs() }
        ImageIO.write(captureToImage().toAwtImage(), "png", File(directory, "sample-wide.png"))
        runOnIdle { width = 390.dp }
        onNodeWithText("1.5×").assertExists()
        onNodeWithText("场景").performClick()
        onNodeWithText("Unicode 与完整塑形").performClick()
        onNodeWithText("Unicode 与完整塑形").assertExists()
        ImageIO.write(captureToImage().toAwtImage(), "png", File(directory, "sample-narrow.png"))
    }
}
