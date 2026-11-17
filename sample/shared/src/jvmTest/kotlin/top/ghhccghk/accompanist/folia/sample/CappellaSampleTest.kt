package top.ghhccghk.accompanist.folia.sample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.graphics.toAwtImage
import top.ghhccghk.accompanist.lyrics.folia.FoliaStyle
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaSampleTest {
    @Test fun localSongSettingsSelectRealCappellaWithoutStartingDemo()=runDesktopComposeUiTest(width=420,height=800){
        val session=SampleSession();val before=session.songs.snapshot
        setContent{FoliaSampleApp(session=session,settingsStore=null)}
        onNodeWithText("设置").performClick()
        onNodeWithText("群唱").performScrollTo().performClick()
        onNodeWithText("群唱：文字对话校准预览。密集消息投影、字形与辉光仍有差异。").assertExists()
        runOnIdle{assertIs<FoliaStyle.Cappella>(session.lyricStyle);assertFalse(session.demo);assertEquals(before,session.songs.snapshot);assertNull(session.songs.current)}
        val out=File("../../build/verification/cappella-public-sample").apply{mkdirs()}
        ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("local-settings.png"))
    }
    @Test fun demoRendersSelectedCappellaAndRetainsClockOnThemeChanges()=runDesktopComposeUiTest(width=800,height=700){
        val session=SampleSession(true).apply{lyricTheme=SampleLyricTheme.Cappella;clock.seek(8150)}
        setContent{FoliaSampleApp(session=session,settingsStore=null)}
        onNodeWithContentDescription("演示进度").assertExists()
        val out=File("../../build/verification/cappella-public-sample").apply{mkdirs()}
        ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("demo.png"))
        runOnIdle{session.lyricTheme=SampleLyricTheme.Classic};waitForIdle()
        runOnIdle{session.lyricTheme=SampleLyricTheme.Cappella};waitForIdle()
        runOnIdle{assertEquals(8150,session.clock.snapshot.positionMillis);assertFalse(session.clock.snapshot.isPlaying)}
    }
}
