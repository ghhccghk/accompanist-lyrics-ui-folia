// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.classicBodyRasterStyle
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Input-only ablation: existing Windows foreground raster policy, not a
 * changed source oracle, timing/shaping algorithm or production candidate. */
@OptIn(ExperimentalTestApi::class)
class DioramaActiveInputCoverageTest {
    @Test fun sourceWorstFrameBodyCoverage()=runDesktopComposeUiTest(width=380,height=200) {
        val out=File("../build/verification/diorama-active-coverage-kotlin").apply{mkdirs()}
        var captured=false
        setContent {
            val resolver=LocalFontFamilyResolver.current
            val prepared=remember {
                val core=SyncedLyrics(listOf(SyncedLine("Synthetic incoming line 2",null,3500,4300)))
                val line=dioramaCoreLines(normalize(core)).single().lyric
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                val measurer=TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr)
                listOf("default" to style,"foreground" to classicBodyRasterStyle(style)).map { (name,body) ->
                    name to DioramaActiveText.prepare(line,body,measurer,resolver)
                }
            }
            SideEffect {
                val a=prepared[0].second;val b=prepared[1].second
                assertEquals(a.lineAdvance,b.lineAdvance);assertEquals(a.middleBaseline,b.middleBaseline)
                assertEquals(a.slices.size,b.slices.size)
                for(i in a.slices.indices) {
                    val x=a.slices[i];val y=b.slices[i]
                    assertEquals(x.unit,y.unit);assertEquals(x.centerPx,y.centerPx);assertEquals(x.imageOffsetPx,y.imageOffsetPx)
                    assertEquals(x.image.width,y.image.width);assertEquals(x.image.height,y.image.height)
                }
                for((name,text)in prepared)for((i,slice)in text.slices.withIndex()) {
                    ImageIO.write(slice.image.toAwtImage(),"png",out.resolve("$name-unit-$i-body.png"))
                }
                captured=true
            }
        }
        waitForIdle();assertTrue(captured)
    }
}
