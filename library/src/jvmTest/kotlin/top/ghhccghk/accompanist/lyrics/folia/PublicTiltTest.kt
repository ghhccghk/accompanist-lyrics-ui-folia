package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.*
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PublicTiltTest {
    @Test fun publicStagePreservesPauseSeekResizeOffsetAndCallbacks()=runDesktopComposeUiTest(width=800,height=600) {
        val source=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Amber echo",700,1900)),null,
            KaraokeAlignment.Unspecified,700,1900)
        val lyrics=SyncedLyrics(listOf(source));val line=normalize(lyrics).lines.single()
        val font=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val textStyle=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1500,false))
        var public by mutableStateOf(false)
        var width by mutableStateOf(800)
        var options by mutableStateOf(FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false))
        var selected:ISyncedLine?=null;var pressed:ISyncedLine?=null
        var boxes=emptyList<TiltRowBox>()
        val tuning=TiltTuning(0f,1f)
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF162435))) {
                if(public)FoliaLyricsView(lyrics,{snapshot},Modifier.size(width.dp,600.dp),
                    style=FoliaStyle.Tilt(tuning),typography=FoliaTypography(main=textStyle),options=options,
                    onLineClicked={selected=it},onLinePressed={pressed=it})
                else TiltLine(line,{snapshot},textStyle,FoliaColors(),options,config=TiltConfig(0.0,1.0),
                    modifier=Modifier.size(width.dp,600.dp),onLayout={boxes=it})
            }
        }
        fun pixels():IntArray {waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val core=pixels()
        runOnIdle{public=true}
        assertContentEquals(core,pixels(),"Public Tilt uses the independent Tilt stage")
        mainClock.advanceTimeBy(2000)
        assertContentEquals(core,pixels(),"Pause freezes all media animation")
        runOnIdle{snapshot=FoliaPlaybackSnapshot(850,false,1)};pixels()
        runOnIdle{snapshot=FoliaPlaybackSnapshot(1500,false,2)}
        assertContentEquals(core,pixels(),"Reverse seek rebuilds the stage")
        runOnIdle{options=options.copy(offsetMillis=100);snapshot=FoliaPlaybackSnapshot(1600,false,3)}
        assertContentEquals(core,pixels(),"Offset applied once")
        runOnIdle{width=380};pixels()
        runOnIdle{width=800}
        assertContentEquals(core,pixels(),"Resize restores the same layout")
        val row=boxes.single().box
        val point=Offset(400f,300f)
        assertTrue(row.width>0)
        onRoot().performTouchInput{click(point)}
        runOnIdle{assertSame(source,selected);selected=null}
        onRoot().performTouchInput{click(Offset(2f,2f))}
        runOnIdle{assertNull(selected)}
        onRoot().performTouchInput{longClick(point)}
        runOnIdle{assertSame(source,pressed)}
        runOnIdle{options=options.copy(reducedMotion=true)}
        val reduced=pixels();mainClock.advanceTimeBy(2000)
        assertContentEquals(reduced,pixels())
    }

    @Test fun publicRetainsTheWhole450msExitIncludingTheLateTail()=runDesktopComposeUiTest(width=380,height=600) {
        val input=File("../verification/classic-fixtures/scenes.tsv").readLines().map{it.split('\t')}.filter{it[0]=="wrap"}
        val source=KaraokeLine.MainKaraokeLine(input.map{KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},null,
            KaraokeAlignment.Unspecified,700,2800)
        val lyrics=SyncedLyrics(listOf(source));val line=normalize(lyrics).lines.single()
        val timeline=VisualTimeline(listOf(line),450_000)
        val leave=generateSequence(2_800_000L){it+1000}.first{timeline.at(it).primary==null}
        assertEquals(3_180_000L,leave)
        val font=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val style=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold)
        val options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false)
        var ms by mutableStateOf(0L);var public by mutableStateOf(false)
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF162435))) {
                if(public)FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(ms,false)},Modifier.fillMaxSize(),
                    style=FoliaStyle.Tilt(TiltTuning(0f,0f)),typography=FoliaTypography(main=style),options=options)
                else if(ms*1000>=line.startUs && ms*1000<leave+450_000)
                    TiltLine(line,{FoliaPlaybackSnapshot(ms,false)},style,FoliaColors(),options,
                        config=TiltConfig(0.0,0.0),leaveUs=leave)
            }
        }
        fun pixels():IntArray {waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val out=File("../build/verification/tilt-exits-kotlin").apply{mkdirs()}
        for(elapsed in listOf(0,50,120,160,299,300,310,400,449,450,451)) {
            val time=leave/1000+elapsed
            runOnIdle{ms=time;public=false};val expected=pixels()
            runOnIdle{public=true}
            assertContentEquals(expected,pixels(),"Retained source-time exit at $time")
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("${elapsed}ms.png"))
        }
    }
}
