package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.BodyParagraphRaster
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.rasterizeParagraph
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class BodyParagraphRasterTest {
    @Test fun oneInkCachePreservesRecordedImagesAndRebuildsOnSeek() = runDesktopComposeUiTest {
        lateinit var paragraph: TextLayoutResult
        setContent {
            val measured = rememberTextMeasurer().measure("office A\u0301", TextStyle(
                fontFamily = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold)),
                fontWeight = FontWeight.Bold, fontSize = 48.sp), softWrap = false)
            SideEffect { paragraph = measured }
        }
        runOnIdle {
            val density = Density(1f)
            val native = BodyParagraphRaster(paragraph, density, nativeInk = true)
            val warm = Color(0xfff2b45d)
            val first = native.rasterFor(warm)
            val retained = first.image.pixels()
            assertSame(first, native.rasterFor(warm), "Pause must reuse the same image")
            val other = native.rasterFor(Color(0xff91e8e1))
            assertNotSame(first.image, other.image, "Recorded images must not be mutated in place")
            assertContentEquals(retained, first.image.pixels())
            assertFalse(retained.contentEquals(other.image.pixels()), "Ink must affect native coverage")
            val rebuilt = native.rasterFor(warm)
            assertNotSame(first, rebuilt, "Retain only the most recent ink, not color history")
            assertContentEquals(retained, rebuilt.image.pixels(), "Reverse seek must reconstruct directly")
            assertEquals(first.origin, rebuilt.origin)

            val transparent = Color(0f, 0f, 0f, 0f)
            assertTrue(native.rasterFor(transparent).image.pixels().all { it ushr 24 == 0 })
            // Compare the complete shaped paragraph, including its ligature/combining mark.
            val translucent = Color(0x80415d8a)
            val expected = rasterizeParagraph(paragraph, density, translucent)
            val actual = native.rasterFor(translucent)
            assertEquals(expected.origin, actual.origin)
            assertContentEquals(expected.image.pixels(), actual.image.pixels())

            val fallback = BodyParagraphRaster(paragraph, density, nativeInk = false)
            val white = fallback.rasterFor(warm)
            assertSame(white, fallback.rasterFor(translucent), "Unverified backends retain white coverage")
            assertContentEquals(rasterizeParagraph(paragraph, density).image.pixels(), white.image.pixels())
        }
    }
    private fun ImageBitmap.pixels() = IntArray(width * height).also { readPixels(it) }
}
