package top.ghhccghk.accompanist.folia.sample

import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.graphics.toAwtImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Ordinary test process deliberately has no experimental library on its path. */
@OptIn(ExperimentalTestApi::class)
class ExperimentalMaskControlsTest {
    private var previousLibrary: String? = null
    @BeforeTest fun selectMissingLibrary() {
        previousLibrary = System.getProperty("folia.mask.library")
        System.setProperty("folia.mask.library", File("build/missing-native-test/folia_mask_experiment.dll").absolutePath)
    }
    @AfterTest fun restoreLibraryChoice() {
        previousLibrary?.let { System.setProperty("folia.mask.library", it) }
            ?: System.clearProperty("folia.mask.library")
    }

    @Test fun diagnosticsPreservePhaseAndActualLinkerReason() {
        for (detail in listOf("library libfolia_mask_experiment.so not found", "cannot locate symbol missing_symbol", "No implementation found for version")) {
            val message = nativeMaskLoadFailure("JNI 自检", UnsatisfiedLinkError(detail))
            assertTrue(message.contains(detail))
            assertTrue(message.contains("JNI 自检"))
            assertTrue(message.contains("-PfoliaExperimentalMask=true"))
            assertTrue(message.contains("folia_mask_experiment.dll"))
            assertTrue(message.contains("-Dfolia.mask.library="))
            assertFalse(message.contains("请安装 Rust 实验 APK"))
        }
    }
    @Test fun freshLaunchAndUnavailableSwitchAreHonest() = runDesktopComposeUiTest(width = 420, height = 800) {
        val session = SampleSession()
        setContent { FoliaSampleApp(session = session, settingsStore = null) }
        onNodeWithText("选择歌曲").assertIsDisplayed()
        onNodeWithText("设置").performClick()
        onNodeWithText("实际：Kotlin").assertExists()
        onNodeWithContentDescription("实验 Rust 辉光遮罩").performClick()
        runOnIdle {
            assertTrue(session.maskBackend.requested)
            assertNull(session.maskBackend.processor)
            assertNotNull(session.maskBackend.loadError)
            assertTrue(session.maskBackend.loadError!!.contains("UnsatisfiedLinkError"))
            assertTrue(session.maskBackend.loadError!!.contains("-PfoliaExperimentalMask=true"))
        }
        onNodeWithText("实际：Kotlin（Rust 不可用／已回退）").assertExists()
        val folder = File("build/verification").apply { mkdirs() }
        ImageIO.write(captureToImage().toAwtImage(), "png", File(folder, "native-unavailable-settings.png"))
        onNodeWithContentDescription("实验 Rust 辉光遮罩").performClick()
        onNodeWithText("实际：Kotlin").assertExists()
        runOnIdle { assertFalse(SampleSession().maskBackend.requested) }
    }

    @Test fun demoSettingsExposeTheSameSessionSwitch() = runDesktopComposeUiTest(width = 800, height = 700) {
        val session = SampleSession(true)
        session.clock.seek(8000)
        setContent { FoliaSampleApp(session = session, settingsStore = null) }
        onNodeWithText("设置").performClick()
        onNodeWithContentDescription("实验 Rust 辉光遮罩").performScrollTo().performClick()
        runOnIdle { assertTrue(session.maskBackend.requested); assertNotNull(session.maskBackend.loadError) }
        onNodeWithContentDescription("实验 Rust 辉光遮罩").performClick()
        runOnIdle { assertFalse(session.maskBackend.requested); assertEquals(8000, session.clock.snapshot.positionMillis) }
    }
}
