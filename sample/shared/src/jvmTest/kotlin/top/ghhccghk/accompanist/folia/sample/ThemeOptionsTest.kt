package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ThemeOptionsTest {
    @Test fun narrowMenuSelectsEveryRendererWithoutChangingPlayback() = verifyOptions(320)
    @Test fun wideMenuSelectsEveryRendererWithoutChangingPlayback() = verifyOptions(900)

    private fun verifyOptions(width: Int) = runDesktopComposeUiTest(width=width,height=700) {
        val session=SampleSession()
        val snapshot=session.songs.snapshot
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                        LyricThemeControls(session)
                    }
                }
            }
        }
        onNodeWithText("7 个校准预览主题，尚未完成正式视觉验收。").assertExists()
        onNodeWithText("镜台 Diorama：内部验证中，暂未开放。").assertExists()
        onNodeWithTag("lyric-theme-Diorama").assertDoesNotExist()
        for(theme in SampleLyricTheme.entries) {
            onNodeWithTag("lyric-theme-${theme.name}").performScrollTo().assertIsDisplayed().performClick().assertIsSelected()
            onNodeWithText(theme.displayName).assertExists()
            onNodeWithText(theme.name).assertExists()
            for(other in SampleLyricTheme.entries.filter { it!=theme }) {
                onNodeWithTag("lyric-theme-${other.name}").assertIsNotSelected()
            }
            runOnIdle {
                assertEquals(theme,session.lyricTheme)
                assertEquals(theme.name,session.lyricStyle::class.simpleName)
                assertFalse(session.demo)
                assertEquals(snapshot,session.songs.snapshot)
                assertNull(session.songs.current)
            }
        }
        onNodeWithTag("lyric-theme-Classic").performScrollTo().performClick()
        val out=File("../../build/verification/theme-options").apply { mkdirs() }
        ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("menu-$width.png"))
    }
}
