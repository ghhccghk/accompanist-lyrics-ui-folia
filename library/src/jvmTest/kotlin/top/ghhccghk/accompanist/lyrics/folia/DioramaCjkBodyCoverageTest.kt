// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import androidx.compose.ui.graphics.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaCjkBodyCoverageTest {
    @Test fun bodyIsSeparateAndHaloSourceUnchanged()=runDesktopComposeUiTest(width=800,height=200) {
        var captured=false
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val prepared=remember {
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityChatCJK-Bold.otf"),FontWeight.Bold)))
                val measurer=TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr)
                listOf("星光 回响 向远方","星光回响").map{DioramaActiveText.prepare(DioramaLyric(it,.7,3.1),style,measurer,resolver)}
            }
            SideEffect {
                for((scene,text)in prepared.withIndex())for((i,s)in text.slices.withIndex()) {
                    assertEquals(s.image.width,s.body.width);assertEquals(s.image.height,s.body.height)
                    if(System.getProperty("os.name").startsWith("Windows"))assertNotSame(s.image,s.body)
                    val out=File("../build/verification/diorama-cjk-body-coverage").apply{mkdirs()}
                    ImageIO.write(s.image.toAwtImage(),"png",out.resolve("$scene-unit-$i-gray.png"))
                    ImageIO.write(s.body.toAwtImage(),"png",out.resolve("$scene-unit-$i-body.png"))
                    ImageIO.write(s.halo.toAwtImage(),"png",out.resolve("$scene-unit-$i-halo.png"))
                }
                captured=true
            }
        }
        waitForIdle();assertTrue(captured)
        val mixed=dioramaUnits(DioramaLyric("星A",0.0,1.0))
        assertFalse(dioramaSeparateGlyphUnits(mixed))
        val clocks=dioramaUnits(DioramaLyric("星光",0.0,1.0))
        assertTrue(dioramaSeparateGlyphUnits(clocks));assertTrue(clocks.all{it.timeline.single().clocks.isNotEmpty()})
    }
}
