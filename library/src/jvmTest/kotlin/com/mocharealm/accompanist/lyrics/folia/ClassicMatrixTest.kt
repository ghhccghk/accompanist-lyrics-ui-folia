package com.mocharealm.accompanist.lyrics.folia

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
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ClassicMatrixTest {
    @Test fun arabicWords() = capture("arabic",800)
    @Test fun narrowWrappedLine() = capture("wrap",380)
    @Test fun stickyContractions() = capture("group",600)
    @Test fun combiningAndLigatures() = capture("cluster",800)
    @Test fun fastLine() = capture("fast",800)
    @Test fun instantLine() = capture("instant",800)
    @Test fun cjkTimedGroups() = capture("cjk",600)
    @Test fun collapsedWhitespace() = capture("whitespace",800)

    private fun capture(scene:String,width:Int) = runDesktopComposeUiTest(width=width,height=600) {
        val rows=File("../verification/classic-fixtures/scenes.tsv").readLines().filterNot {it.startsWith("#")}.map {it.split('\t')}.filter {it[0]==scene}
        val source=KaraokeLine.MainKaraokeLine(rows.map {KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},null,
            KaraokeAlignment.Unspecified,rows[0][1].toInt(),rows[0][2].toInt())
        val document=normalize(SyncedLyrics(listOf(source)));val timeline=VisualTimeline(document.lines)
        val fontFile=when(scene) {"arabic"->"NotoSansArabic-Bold.ttf";"cjk"->"FoliaParityCJK-Bold.otf";else->"NotoSans-Bold.ttf"}
        val family=FontFamily(Font(File("../verification/assets/$fontFile"),FontWeight.Bold))
        val style=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=(width*.06f).coerceIn(36f,72f).sp)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false));var avoid by mutableStateOf(false)
        val layouts=mutableMapOf<Boolean,ClassicGeometry>()
        setContent {
            val selection=timeline.at(snapshot.positionMillis*1000)
            Box(Modifier.fillMaxSize().background(Color(0xFF162435)).padding(horizontal=32.dp),contentAlignment=Alignment.Center) {
                (selection.exiting+listOfNotNull(selection.primary)).forEach {epoch->key(epoch) {
                    ClassicLine(document.lines[epoch.line],{snapshot},style,FoliaColors(),FoliaOptions(),
                        ClassicTuning(breathingFloatMultiplier=0f,avoidOverlap=avoid),356.dp,
                        enterUs=epoch.enterUs,leaveUs=epoch.leaveUs,onLayout={layouts[avoid]=it})
                }}
            }
        }
        val output=File("../build/verification/classic-matrix-kotlin").apply {mkdirs()}
        for(mode in listOf(false,true)) {
            runOnIdle {avoid=mode}
            for(time in listOf(700,750,800,850,900,1000,1200,1500,1900,2100,2300,2800,3000)) {
                runOnIdle {snapshot=FoliaPlaybackSnapshot(time.toLong(),false,snapshot.discontinuity+1)};waitForIdle()
                val image=captureToImage().toAwtImage()
                if(time==(if(scene=="instant") 750 else if(scene=="fast") 800 else 1500)) {
                    val visible=image.getRGB(0,0,image.width,image.height,null,0,image.width).count {((it ushr 16) and 255)>100}
                    assertTrue(visible>20,"$scene must draw visible text at $time ms")
                }
                ImageIO.write(image,"png",File(output,"$scene-${if(mode) "avoid" else "default"}-${time}ms.png"))
            }
        }
        val expected=File("../verification/classic-fixtures/browser-matrix-layout.tsv").readLines().filterNot {it.startsWith("#")}.map {it.split('\t')}.filter {it[0]==scene}
        assertTrue(expected.isNotEmpty())
        for(c in expected) {
            val g=layouts.getValue(c[1]=="avoid");val i=c[4].toInt();val b=g.boxes[i]
            val values=listOf(b.left.toDouble(),b.top.toDouble(),b.width.toDouble(),b.height.toDouble(),g.margins[i],g.height.toDouble(),g.rowGap.toDouble(),g.paddingTop.toDouble(),g.paddingBottom.toDouble())
            values.forEachIndexed {j,value->assertEquals(c[j+5].toDouble(),value,if(j==4) .0001 else .51,"$scene column ${j+5}: $c")}
        }
    }
}
