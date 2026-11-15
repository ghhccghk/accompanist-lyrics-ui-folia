package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.ClassicLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.ClassicStage
import java.io.File
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.ClassicGeometry
import javax.imageio.ImageIO
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.LocalClassicSoftwareLayers
import kotlin.test.*

/** Exact same fixture/font/surface as classic-browser/main.tsx; no sample chrome. */
@OptIn(ExperimentalTestApi::class)
class ClassicParityTest {
    @Test fun fixedFontCoreCheckpoints() = capture(false)
    @Test fun softwareBlurCoreCheckpoints() = capture(true)
    @Test fun continuousCoreSequence() = capture(false, sequence = true)
    private fun capture(software: Boolean, sequence: Boolean = false) = runDesktopComposeUiTest(width = 800, height = 600) {
        fun line(a: String, b: String, start: Int, middle: Int, end: Int) = KaraokeLine.MainKaraokeLine(
            listOf(KaraokeSyllable(a, start, middle), KaraokeSyllable(b, middle, end)),
            null, KaraokeAlignment.Unspecified, start, end)
        val document = normalize(SyncedLyrics(listOf(line("Amber ", "echo", 700, 1300, 1900),
            line("Silver ", "light", 3200, 3650, 4100))))
        val timeline = VisualTimeline(document.lines)
        val font = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold))
        val style = TextStyle(fontFamily = font, fontWeight = FontWeight.Bold, fontSize = 48.sp, lineHeight = 58.56.sp)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false))
        var avoid by mutableStateOf(false)
        val geometries = mutableMapOf<Pair<Boolean, Int>, ClassicGeometry>()
        setContent {
          CompositionLocalProvider(LocalClassicSoftwareLayers provides software) {
            val selection = timeline.at(snapshot.positionMillis * 1000)
            ClassicStage(Modifier.fillMaxSize().background(Color(0xFF162435))) {
                val epochs = selection.exiting + listOfNotNull(selection.primary)
                epochs.forEach { epoch -> key(epoch) {
                    ClassicLine(document.lines[epoch.line], { snapshot }, style, FoliaColors(), FoliaOptions(),
                        ClassicTuning(breathingFloatMultiplier = 0f, avoidOverlap = avoid), 356.dp,
                        enterUs = epoch.enterUs, leaveUs = epoch.leaveUs,
                        onLayout = { geometries[avoid to epoch.line] = it })
                } }
            }
          }
        }
        val directory = File("../build/verification/${if(sequence) "classic-continuous-kotlin" else if(software) "classic-software-kotlin" else "classic-kotlin"}").apply { mkdirs() }
        val checkpoints = if(sequence) ((0..4800 step 40).toList()+listOf(1500,3700)).sorted()
            else listOf(0,700,750,850,1000,1200,1300,1500,1800,1900,2100,2220,2300,2500,3000,3200,3400,3700,4100,4400,4700)
        listOf(false, true).forEach { mode ->
            var retained: IntArray? = null
            runOnIdle { avoid = mode }
            checkpoints.forEach { time ->
                runOnIdle { snapshot = FoliaPlaybackSnapshot(time.toLong(), false, snapshot.discontinuity + 1) }
                waitForIdle()
                val image = captureToImage().toAwtImage()
                assertEquals(800, image.width); assertEquals(600, image.height)
                if(time==1500 || time==3700) {
                    val bright=image.getRGB(0,0,image.width,image.height,null,0,image.width).count {p ->
                        ((p ushr 16) and 255)>180 && ((p ushr 8) and 255)>180
                    }
                    assertTrue(bright>1000,"Core frame must contain visible body glyphs at $time ms")
                }
                if(time==1500) retained=image.getRGB(0,0,image.width,image.height,null,0,image.width)
                ImageIO.write(image, "png", File(directory, "${if(mode) "avoid" else "default"}-${time}ms.png"))
            }
            // Reverse seek after all exit frames must rebuild the same software layers.
            runOnIdle { snapshot=FoliaPlaybackSnapshot(1500,false,snapshot.discontinuity+1) };waitForIdle()
            val reconstructed=captureToImage().toAwtImage()
            assertContentEquals(retained!!,reconstructed.getRGB(0,0,800,600,null,0,800))
            waitForIdle()
            val paused=captureToImage().toAwtImage()
            assertContentEquals(retained,paused.getRGB(0,0,800,600,null,0,800))
        }
        File(directory, "conditions.txt").writeText("800x600 density1; FoliaParityLatin-Bold.ttf pinned SHA in verification/assets/README.md; same main.tsx input. Paused direct media reconstruction. Core only, no chrome/subtitles. Software layers=$software; JVM ${System.getProperty("java.version")}.\n")
        if(sequence) File(directory,"times.txt").writeText(checkpoints.joinToString("\n")+"\n")
        File(directory, "layout.tsv").writeText("mode\tline\tindex\tleft\ttop\twidth\theight\tmargin\tlineHeight\trowGap\tpaddingTop\tpaddingBottom\n" +
            geometries.entries.joinToString("\n") { (key, g) -> g.boxes.mapIndexed { i,b ->
                listOf(if(key.first) "avoid" else "default",key.second,i,b.left,b.top,b.width,b.height,g.margins[i],g.height,g.rowGap,g.paddingTop,g.paddingBottom).joinToString("\t")
            }.joinToString("\n") })
        File("../verification/classic-fixtures/browser-layout.tsv").readLines().filterNot {it.startsWith("#") || it.isBlank()}.forEach { row ->
            val c=row.split('\t'); val g=geometries.getValue((c[0]=="avoid") to c[1].toInt())
            val i=c[2].toInt(); val b=g.boxes[i]
            val actual=listOf(b.left.toDouble(),b.top.toDouble(),b.width.toDouble(),b.height.toDouble(),g.margins[i],
                g.height.toDouble(),g.rowGap.toDouble(),g.paddingTop.toDouble(),g.paddingBottom.toDouble())
            actual.forEachIndexed { j,value ->
                // DOM offset* rounds to integral CSS pixels; margin is serialized to 0.1px.
                assertEquals(c[j+3].toDouble(),value,if(j==4) .0001 else .51,"column ${j+3}: $row")
            }
        }
    }
}
