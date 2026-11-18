package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Original CappellaText is a complete text node for passed messages. Native
 * coverage must equal a single shaped row, not a union of caret selections. */
@OptIn(ExperimentalTestApi::class)
class CappellaCompleteTextTest {
    @Test fun completeRowsPreserveLigaturesOverhangAndBidiInk() = runDesktopComposeUiTest(width=800,height=300) {
        data class Case(val name:String,val text:String,val font:String,val italic:Boolean=false)
        val cases=listOf(
            Case("italic","fij Wavy","FoliaParityLatin-Bold.ttf",true),
            Case("arabic","مرحبا بالعالم","FoliaParityBidi-Bold.ttf"),
            Case("mixed","Aمرحبا 12B بالعالم","FoliaParityBidi-Bold.ttf"),
            Case("latin","office affine","FoliaParityLatin-Bold.ttf"),
            Case("cjk","星光回响","FoliaParityChatCJK-Bold.otf"))
        var scene by mutableStateOf(cases.first())
        var reference by mutableStateOf(false)
        var active by mutableStateOf(false)
        var reduced by mutableStateOf(false)
        val config=CappellaBubbleStyle(fontPx=64.0,maxTextWidth=738.0)
        val out=File("../build/verification/cappella-complete-text").apply{mkdirs()}
        setContent {
            val c=scene
            val base=remember(c){TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/${c.font}"),FontWeight.Bold)),
                fontWeight=FontWeight.Bold,fontStyle=if(c.italic)FontStyle.Italic else FontStyle.Normal,
                textDirection=TextDirection.ContentOrLtr,textAlign=TextAlign.Left)}
            val resolver=LocalFontFamilyResolver.current
            val measurer=rememberTextMeasurer()
            if(reference) {
                val measure=remember(base,resolver){CappellaBubbleMeasure(measurer,base,Density(1f),config,resolver)}
                val layout=remember(base,resolver){measure.layout(c.text)}
                assertEquals(1,layout.lineCount,"single whole source row")
                val metrics=remember(base,resolver){naturalFontMetrics(measure.style,resolver,Density(1f))}
                Canvas(Modifier.fillMaxSize()) {
                    val top=17+cssFontBaseline(metrics.ascent,metrics.descent,92.8f)-paintedNaturalBaseline(layout,metrics)
                    drawText(layout,Color.White,Offset(21f,top),shadow=Shadow.None)
                }
            } else CappellaBubbleTarget(CappellaLine(c.text,0.0,1.0,emptyList()),{2.0},base,
                CappellaBubbleColors(Color.Transparent,Color.Transparent,Color.White),Modifier.fillMaxSize(),
                config=config,active=active,reducedMotion=reduced,sizeOverride=CappellaBubbleSize(780.0,160.0))
        }
        fun pixels():IntArray { waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)} }
        val report=mutableListOf("# case mode differentPixels")
        val failures=mutableListOf<String>()
        for(c in cases) {
            runOnIdle{scene=c;reference=true};val expected=pixels()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("${c.name}-complete.png"))
            for(mode in listOf("passed","reduced")) {
                runOnIdle{reference=false;active=mode=="reduced";reduced=mode=="reduced"}
                val actual=pixels();val count=expected.indices.count{expected[it]!=actual[it]}
                report+="${c.name}\t$mode\t$count"
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("${c.name}-$mode.png"))
                if(count!=0)failures+="${c.name}/$mode loses or duplicates $count complete-run pixels"
                mainClock.advanceTimeBy(300);assertContentEquals(actual,pixels(),"paused complete row")
            }
        }
        out.resolve("coverage.tsv").writeText(report.joinToString("\n")+"\n")
        assertTrue(failures.isEmpty(),failures.joinToString("\n"))
    }
}
