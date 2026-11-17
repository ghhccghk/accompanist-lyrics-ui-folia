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
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PublicPendoloTest {
    private fun font()=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),fontWeight=FontWeight.Bold)
    @Test fun transformedInputPauseOffsetAndLifecycle()=runDesktopComposeUiTest(width=800,height=600){
        val source=SyncedLine("Amber echo",null,700,2800)
        val lyrics=SyncedLyrics(listOf(source));val lines=pendoloCoreLines(normalize(lyrics)).map{it.stage}
        val track=PendoloTickTrack(lines.map{PendoloLineTime(it.lyric.start,it.lyric.end,it.renderEnd)})
        var content by mutableStateOf(lyrics)
        var t by mutableStateOf(900L);var public by mutableStateOf(false);var attached by mutableStateOf(true)
        var offset by mutableStateOf(0);var width by mutableStateOf(800);var style:FoliaStyle by mutableStateOf(FoliaStyle.Pendolo())
        var options by mutableStateOf(FoliaOptions(showNextLine=false))
        var clicked:ISyncedLine?=null;var pressed:ISyncedLine?=null;var status:FoliaStatus?=null;var parentClicks=0
        var rows:List<PendoloStageRow> = emptyList();val textStyle=font()
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435)).clickable(interactionSource=remember{MutableInteractionSource()},indication=null){parentClicks++}){if(attached){
            if(public)FoliaLyricsView(content,{FoliaPlaybackSnapshot(t,false)},Modifier.width(width.dp).height(600.dp),
                typography=FoliaTypography(main=textStyle,subtitle=textStyle),style=style,
                options=options.copy(offsetMillis=offset),colors=FoliaColors(active=Color(0xfff17b4d)),
                onStatus={status=it},onLineClicked={clicked=it},onLinePressed={pressed=it})
            else PendoloPlaybackWheel(lines,{(t-offset)/1000.0},textStyle,Modifier.width(width.dp).height(600.dp),onRows={rows=it})
        }}}
        fun pixels():IntArray{waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
        val expected=pixels();val row=rows.single();val rotation=track.at(.9).wheelRotation
        val rect=pendoloRotatedContentRect(row,rotation,0.0,300.0)
        val center=Offset((rect[0]+rect[2]/2).toFloat(),(rect[1]+rect[3]/2).toFloat())
        assertEquals(0,pendoloHitTest(rows,center,rotation,Offset(0f,300f)){1.0})
        assertNull(pendoloHitTest(rows,center,200.0,Offset(0f,300f)){1.0},"invisible arc")
        assertNull(pendoloHitTest(rows,Offset(2f,2f),rotation,Offset(0f,300f)){1.0})
        runOnIdle{public=true};assertTrue(expected.contentEquals(pixels()),"line-only source uses original interpolated EGC sweep")
        onNodeWithContentDescription("Amber echo").assertExists()
        onRoot().performTouchInput{click(center)};runOnIdle{assertSame(source,clicked);assertEquals(0,parentClicks);clicked=null}
        onRoot().performTouchInput{longClick(center)};runOnIdle{assertSame(source,pressed);assertNull(clicked)}
        onRoot().performTouchInput{click(Offset(2f,2f))};runOnIdle{assertNull(clicked);assertEquals(1,parentClicks)}
        val replacement=SyncedLine("New source",null,700,2800)
        runOnIdle{content=SyncedLyrics(listOf(replacement));clicked=null};pixels()
        onRoot().performTouchInput{click(center)};runOnIdle{assertSame(replacement,clicked)}
        runOnIdle{content=lyrics;clicked=null};pixels()
        mainClock.advanceTimeBy(2000);assertTrue(expected.contentEquals(pixels()),"pause freezes all media animations")
        runOnIdle{t=2200};pixels();runOnIdle{t=900};assertTrue(expected.contentEquals(pixels()),"backward seek")
        runOnIdle{offset=125;t=1025};assertTrue(expected.contentEquals(pixels()),"offset once")
        onRoot().performTouchInput{click(center)};runOnIdle{assertSame(source,clicked);assertEquals(825,clicked!!.start+offset)}
        runOnIdle{width=380};pixels();runOnIdle{width=800};assertTrue(expected.contentEquals(pixels()),"resize")
        runOnIdle{style=FoliaStyle.Classic()};pixels();runOnIdle{style=FoliaStyle.Pendolo()};assertTrue(expected.contentEquals(pixels()),"theme restoration")
        runOnIdle{options=options.copy(reducedMotion=true,quality=FoliaQuality.Minimal)}
        val reduced=pixels();runOnIdle{assertEquals(FoliaQuality.Minimal,assertNotNull(status).actualQuality)}
        mainClock.advanceTimeBy(1000);assertTrue(reduced.contentEquals(pixels()))
        runOnIdle{attached=false};val empty=pixels();assertFalse(empty.contentEquals(reduced))
        mainClock.advanceTimeBy(1000);assertTrue(empty.contentEquals(pixels()),"no draw task after disposal")
        runOnIdle{attached=true;options=FoliaOptions(showNextLine=false);offset=0;t=900}
        assertTrue(expected.contentEquals(pixels()),"remount")
    }
}
