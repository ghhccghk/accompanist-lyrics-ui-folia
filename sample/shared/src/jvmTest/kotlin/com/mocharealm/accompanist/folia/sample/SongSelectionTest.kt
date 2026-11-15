package com.mocharealm.accompanist.folia.sample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class SongSelectionTest {
    @Test fun defaultIsSelectionAndCancelNeverStartsDemoClock()=runDesktopComposeUiTest(width=390,height=740) {
        val session=SampleSession()
        setContent{FoliaSampleApp(session=session,settingsStore=null)}
        onNodeWithText("选择歌曲").performClick()
        onNodeWithText("音频文件（必选）").assertIsDisplayed()
        onNodeWithText("歌词文件（可选）").assertIsDisplayed()
        onNodeWithText("播放所选歌曲").assertIsNotEnabled()
        onNodeWithText("翻译文件（可选）").assertIsNotEnabled()
        onNodeWithText("取消").performClick()
        runOnIdle{assertNull(session.songs.current);assertFalse(session.demo);assertFalse(session.clock.snapshot.isPlaying)}
        val out=File("build/verification/local-audio").apply{mkdirs()}
        ImageIO.write(captureToImage().toAwtImage(),"png",File(out,"waiting-narrow.png"))
        onNodeWithText("开发／演示").performClick()
        onNodeWithText("演示时钟 · 无音频").assertExists()
        onNodeWithText("返回本地歌曲").performClick()
        onNodeWithText("选择歌曲").assertIsDisplayed()
    }
}
