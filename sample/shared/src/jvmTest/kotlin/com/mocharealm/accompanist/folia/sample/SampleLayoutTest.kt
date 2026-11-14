package com.mocharealm.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test

@OptIn(ExperimentalTestApi::class)
class SampleLayoutTest {
    @Test fun scenesControlsAndResizeKeepState() = runDesktopComposeUiTest(width = 1100, height = 800) {
        var width by mutableStateOf(1100.dp)
        setContent { Box(Modifier.width(width).height(740.dp)) { FoliaSampleApp() } }
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
