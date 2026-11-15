package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.*

/** Original useOverlapFit budgets below 80px, including its .5 font-size floor. */
@OptIn(ExperimentalTestApi::class)
class ClassicShortStageTest {
    @Test fun calm() = capture(FoliaAnimationIntensity.Calm)
    @Test fun normal() = capture(FoliaAnimationIntensity.Normal)
    @Test fun chaotic() = capture(FoliaAnimationIntensity.Chaotic)

    private fun capture(intensity: FoliaAnimationIntensity) = runDesktopComposeUiTest(width=800,height=300) {
        val source=SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(
            listOf(KaraokeSyllable("Amber ",700,1300),KaraokeSyllable("echo",1300,1900)),
            null,KaraokeAlignment.Unspecified,700,1900)))
        val line=normalize(source).lines.single()
        val family=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val style=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=48.sp)
        val options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false,animationIntensity=intensity)
        var height by mutableIntStateOf(300)
        var avoid by mutableStateOf(false)
        var public by mutableStateOf(false)
        var geometry:ClassicGeometry?=null
        setContent {
            val modifier=Modifier.requiredSize(800.dp,height.dp).background(Color(0xFF162435))
            val tuning=ClassicTuning(breathingFloatMultiplier=0f,avoidOverlap=avoid)
            if(public) FoliaLyricsView(source,{FoliaPlaybackSnapshot(1500,false)},modifier,
                typography=FoliaTypography(secondary=style),options=options,style=FoliaStyle.Classic(tuning))
            else ClassicStage(modifier) {
                ClassicLine(line,{FoliaPlaybackSnapshot(1500,false)},style,FoliaColors(),options,tuning,
                    classicHeightBudget(height.toFloat(),1f).dp,onLayout={geometry=it})
            }
        }
        val rows=File("../verification/classic-fixtures/short-stage.tsv").readLines()
            .filterNot {it.startsWith("#") || it.isBlank()}.map {it.split('\t')}
            .filter {it[0]==intensity.name.lowercase()}
        assertEquals(12,rows.size)
        val out=File("../build/verification/short-stage-kotlin").apply {mkdirs()}
        val failures=mutableListOf<String>()
        val seen=mutableMapOf<Pair<Int,Boolean>,IntArray>()
        for(c in rows+rows.reversed()) {
            val h=c[2].toInt();val mode=c[3].toBoolean()
            runOnIdle {height=h;avoid=mode};waitForIdle()
            val frame=captureToImage()
            val pixels=IntArray(800*300).also {frame.readPixels(it)}
            mainClock.advanceTimeBy(1000);waitForIdle()
            assertContentEquals(pixels,IntArray(pixels.size).also {captureToImage().readPixels(it)},"Paused $c")
            seen[h to mode]?.let {assertContentEquals(it,pixels,"Restored height $c")}
            seen[h to mode]=pixels
            ImageIO.write(frame.toAwtImage().getSubimage(0,0,800,h),"png",
                out.resolve("${intensity.name.lowercase()}-800x$h-${if(mode) "avoid" else "default"}.png"))
            val actualHeight=geometry!!.height
            if(abs(c[5].toFloat()-actualHeight)>.51f) failures+="Original height ${c[5]}, Kotlin $actualHeight: $c"
            runOnIdle {public=true};waitForIdle()
            assertContentEquals(pixels,IntArray(pixels.size).also {captureToImage().readPixels(it)},"Public/core $c")
            runOnIdle {public=false};waitForIdle()
        }
        // Capture all differing heights before reporting, to retain useful visual
        // evidence on a failed run. The source fixture and tolerance stay fixed.
        assertTrue(failures.isEmpty(),failures.distinct().joinToString("\n"))
    }
}
