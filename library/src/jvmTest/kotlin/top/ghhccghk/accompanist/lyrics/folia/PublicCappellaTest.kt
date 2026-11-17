package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.*
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PublicCappellaTest {
    @Test fun explicitNestedIdentityReachesPublicDrawingAndOriginalCallback()=runDesktopComposeUiTest(width=800,height=600){
        val back=KaraokeLine.AccompanimentKaraokeLine(listOf(KaraokeSyllable("Guest",700,2800)),null,KaraokeAlignment.Unspecified,700,2800)
        val lead=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Lead",700,2800)),null,KaraokeAlignment.Unspecified,700,2800,accompanimentLines=listOf(back))
        val lyrics=SyncedLyrics(listOf(lead));val agents=listOf(CappellaLineAgent(0,"lead"),CappellaLineAgent(0,"guest",0))
        val lines=cappellaCoreLines(normalize(lyrics),mapOf(SourceRef(0) to "lead",SourceRef(0,0) to "guest")).map{it.stage}
        var public by mutableStateOf(false);var clicked:ISyncedLine?=null;var rows:List<CappellaRowLayout> = emptyList()
        val font=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),fontWeight=FontWeight.Bold)
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435))){
            if(public)FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(1600,false)},Modifier.fillMaxSize(),
                style=FoliaStyle.Cappella(CappellaTuning(title="Synthetic title",agents=agents)),typography=FoliaTypography(main=font),
                colors=FoliaColors(active=Color(0xfff17b4d)),options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false),
                onLineClicked={clicked=it})
            else CappellaTargetStage(lines,"Synthetic title",{1.6},{1},font,Modifier.fillMaxSize(),playbackMotion=true,onRows={rows=it})
        }}
        fun pixels():IntArray{waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
        val expected=pixels();val row=rows.single{it.message.line==1}
        assertEquals(CappellaSide.LEFT,row.message.sender.side)
        val center=Offset((row.bubbleX+row.bubbleWidth/2).toFloat(),(row.bubbleY+row.bubbleHeight/2).toFloat())
        runOnIdle{public=true};assertTrue(expected.contentEquals(pixels()),"public explicit nested agent map")
        onRoot().performTouchInput{click(center)};runOnIdle{assertSame(back,clicked)}
    }
    @Test fun transformedCallbacksOriginalIdentityAndLifecycle()=runDesktopComposeUiTest(width=800,height=600){
        val source=SyncedLine("Amber echoes beyond",null,700,2800)
        val lyrics=SyncedLyrics(listOf(source));val lines=cappellaCoreLines(normalize(lyrics)).map{it.stage}
        var content by mutableStateOf(lyrics);var time by mutableStateOf(1600L)
        var public by mutableStateOf(false);var attached by mutableStateOf(true)
        var offset by mutableStateOf(0);var width by mutableStateOf(800)
        var style:FoliaStyle by mutableStateOf(FoliaStyle.Cappella(CappellaTuning(title="Synthetic title")))
        var options by mutableStateOf(FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false))
        var clicked:ISyncedLine?=null;var pressed:ISyncedLine?=null;var parentClicks=0
        var status:FoliaStatus?=null;var rows:List<CappellaRowLayout> = emptyList()
        val font=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),fontWeight=FontWeight.Bold)
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435)).clickable(
            interactionSource=remember{MutableInteractionSource()},indication=null){parentClicks++}){if(attached){
            if(public)FoliaLyricsView(content,{FoliaPlaybackSnapshot(time,false)},Modifier.width(width.dp).height(600.dp),
                style=style,typography=FoliaTypography(main=font),colors=FoliaColors(active=Color(0xfff17b4d)),
                options=options.copy(offsetMillis=offset),onStatus={status=it},onLineClicked={clicked=it},onLinePressed={pressed=it})
            else CappellaTargetStage(lines,"Synthetic title",{(time-offset)/1000.0},{cappellaCurrentLine(lines,(time-offset)/1000.0)},
                font,Modifier.fillMaxSize(),playbackMotion=true,onRows={rows=it})
        }}}
        fun pixels():IntArray{waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
        val expected=pixels();val row=rows.single{it.message.line==0}
        val center=Offset((row.bubbleX+row.bubbleWidth/2).toFloat(),(row.bubbleY+row.bubbleHeight/2).toFloat())
        assertEquals(0,cappellaHitTest(rows,center));assertNull(cappellaHitTest(listOf(row.copy(opacity=0.0)),center))
        assertNull(cappellaHitTest(rows,Offset(row.bubbleX.toFloat()+.01f,row.bubbleY.toFloat()+.01f)),"rounded empty corner")
        runOnIdle{public=true};assertTrue(expected.contentEquals(pixels()),"source line-only EGC time fallback")
        onNodeWithContentDescription(source.content).assertExists()
        onRoot().performTouchInput{click(center)};runOnIdle{assertSame(source,clicked);assertEquals(0,parentClicks);clicked=null}
        onRoot().performTouchInput{longClick(center)};runOnIdle{assertSame(source,pressed);assertNull(clicked)}
        onRoot().performTouchInput{click(Offset(2f,2f))};runOnIdle{assertEquals(1,parentClicks);assertNull(clicked)}
        mainClock.advanceTimeBy(2000);assertTrue(expected.contentEquals(pixels()),"pause")
        runOnIdle{time=2300};pixels();runOnIdle{time=1600};assertTrue(expected.contentEquals(pixels()),"seek reconstruction")
        runOnIdle{offset=125;time=1725};assertTrue(expected.contentEquals(pixels()),"offset once")
        onRoot().performTouchInput{click(center)};runOnIdle{assertSame(source,clicked);assertEquals(825,clicked!!.start+offset)}
        runOnIdle{width=380};pixels();runOnIdle{width=800};assertTrue(expected.contentEquals(pixels()),"responsive restore")
        runOnIdle{style=FoliaStyle.Pendolo()};pixels()
        runOnIdle{style=FoliaStyle.Cappella(CappellaTuning(title="Synthetic title"))};assertTrue(expected.contentEquals(pixels()),"theme restore")
        runOnIdle{options=options.copy(reducedMotion=true,quality=FoliaQuality.Minimal)}
        val reduced=pixels();runOnIdle{assertEquals(FoliaQuality.Minimal,assertNotNull(status).actualQuality)}
        mainClock.advanceTimeBy(1000);assertTrue(reduced.contentEquals(pixels()))
        runOnIdle{content=SyncedLyrics(emptyList());clicked=null};pixels();onRoot().performTouchInput{click(center)}
        runOnIdle{assertNull(clicked);assertEquals(2,parentClicks);content=lyrics;options=options.copy(reducedMotion=false,quality=FoliaQuality.Standard)}
        assertTrue(expected.contentEquals(pixels()),"document replacement")
        runOnIdle{attached=false};val empty=pixels();mainClock.advanceTimeBy(1000);assertTrue(empty.contentEquals(pixels()),"disposed")
        runOnIdle{attached=true};assertTrue(expected.contentEquals(pixels()),"remount")
    }
}
