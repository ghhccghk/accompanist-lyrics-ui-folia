package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import com.mocharealm.accompanist.lyrics.core.model.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import kotlinx.coroutines.CancellationException
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PublicFumeTest {
    @Test fun wholeSongPreparationAndSystemFontScaleRestore()=runDesktopComposeUiTest(width=800,height=600){
        val content=SyncedLyrics((0 until 120).map{SyncedLine("Synthetic stanza $it across the page",null,it*3000+700,it*3000+2800)})
        val font=fumePublicFont();var scale by mutableFloatStateOf(1f);var status:FoliaStatus?=null
        setContent{CompositionLocalProvider(LocalDensity provides Density(1f,scale)){
            FoliaLyricsView(content,{FoliaPlaybackSnapshot(1000,false)},Modifier.fillMaxSize(),style=FoliaStyle.Fume(),
                typography=FoliaTypography(main=font),options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false),onStatus={status=it})
        }}
        fun awaitReady(){waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0};waitForIdle()}
        fun pixels():IntArray{val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        awaitReady();val expected=pixels()
        runOnIdle{scale=1.4f;status=null};awaitReady();assertFalse(expected.contentEquals(pixels()),"system font scale must relayout article")
        runOnIdle{scale=1f;status=null};awaitReady();assertTrue(expected.contentEquals(pixels()),"whole-song font scale restore")
    }
    @Test fun callbacksReadinessReplacementAndLifecycle()=runDesktopComposeUiTest(width=800,height=600){
        val core=fumePublicFixture();val lines=normalize(core).lines.map(::fumeLyric);val font=fumePublicFont()
        var prepared:FumePreparedStage?=null;var ready=false;var public by mutableStateOf(false)
        var mounted by mutableStateOf(true);var content by mutableStateOf(core);var time by mutableStateOf(1000L)
        var offset by mutableIntStateOf(0);var width by mutableIntStateOf(800)
        var tuning by mutableStateOf(FumeTuning(layoutSeed="FumeSceneFixture"))
        var style:FoliaStyle? by mutableStateOf(null);var subtitle by mutableStateOf(TextStyle(fontSize=18.sp))
        var options by mutableStateOf(FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false))
        var clicked:ISyncedLine?=null;var pressed:ISyncedLine?=null;var parents=0;var status:FoliaStatus?=null
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435)).clickable(
            interactionSource=remember{MutableInteractionSource()},indication=null){parents++}){if(mounted){
            if(public)FoliaLyricsView(content,{FoliaPlaybackSnapshot(time,false)},Modifier.width(width.dp).height(600.dp),
                style=style?:FoliaStyle.Fume(tuning),typography=FoliaTypography(main=font,subtitle=subtitle),
                colors=FoliaColors(active=Color(0xfff17b4d)),options=options.copy(offsetMillis=offset),
                onStatus={status=it},onLineClicked={clicked=it},onLinePressed={pressed=it})
            else FumeArticleStage(lines,{time/1000.0},{0},font,FumeFrameTheme(FumeInk(223,229,236),FumeInk(241,123,77)),
                Modifier.fillMaxSize(),layoutSeed="FumeSceneFixture",onPrepared={prepared=it},onState={r,_->ready=r})
        }}}
        fun pixels():IntArray{waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
        fun awaitReady(){waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0};waitForIdle()}
        waitUntil(timeoutMillis=30000){ready};val p=assertNotNull(prepared);val frame=p.track!!.sample(1.0)
        val block=p.article.blocks.first();val row=p.drawing.focusGeometry.getValue(0).rows.first();val camera=frame.state.pose
        val rowCenter=Offset(((block.x+row.left+row.width/2-camera.x)*camera.scale+400).toFloat(),
            ((block.y+row.top+block.lineHeight/2-camera.y)*camera.scale+300).toFloat())
        val grid=(0..59).flatMap{y->(0..79).map{x->Offset(x*10f+2,y*10f+2)}}
        val center=grid.firstOrNull{fumeHitTest(p,frame,it)==0}?:rowCenter
        val hit=assertNotNull(fumeHitTest(p,frame,center));assertNull(fumeHitTest(p,frame,Offset(-100000f,-100000f)))
        val expected=pixels();runOnIdle{public=true};awaitReady();assertTrue(expected.contentEquals(pixels()),"public actual core")
        onRoot().performTouchInput{click(center)};runOnIdle{assertSame(core.lines[hit],clicked);assertEquals(0,parents);clicked=null}
        onRoot().performTouchInput{longClick(center)};runOnIdle{assertSame(core.lines[hit],pressed);assertNull(clicked)}
        // Find empty stage space using the same inverse transform, not a hardcoded text corner.
        val blank=grid.first{fumeHitTest(p,frame,it)==null}
        onRoot().performTouchInput{click(blank)};runOnIdle{assertEquals(1,parents);assertNull(clicked)}
        mainClock.advanceTimeBy(2000);assertTrue(expected.contentEquals(pixels()),"pause")
        runOnIdle{time=10200};pixels();runOnIdle{time=1000};assertTrue(expected.contentEquals(pixels()),"reverse seek")
        runOnIdle{offset=125;time=1125};assertTrue(expected.contentEquals(pixels()),"offset once")
        onRoot().performTouchInput{click(center)};runOnIdle{assertSame(core.lines[hit],clicked);assertEquals(core.lines[hit].start+125,clicked!!.start+offset)}
        runOnIdle{subtitle=TextStyle(fontSize=23.sp);tuning=tuning.copy(textHoldRatio=.3f,showPrintSymbols=false)}
        awaitReady();runOnIdle{assertTrue(status!!.diagnostics.none{it.startsWith("FumePrepar")})}
        runOnIdle{tuning=tuning.copy(textHoldRatio=1f,showPrintSymbols=true);width=380;status=null};awaitReady()
        runOnIdle{width=800;status=null};awaitReady();assertTrue(expected.contentEquals(pixels()),"resize restore")
        runOnIdle{style=FoliaStyle.Pendolo()};pixels();runOnIdle{style=null;status=null};awaitReady()
        assertTrue(expected.contentEquals(pixels()),"theme restore")
        runOnIdle{options=options.copy(reducedMotion=true,quality=FoliaQuality.Minimal);status=null};awaitReady()
        val reduced=pixels();mainClock.advanceTimeBy(1000);assertTrue(reduced.contentEquals(pixels()),"reduced pause")
        runOnIdle{assertEquals(FoliaQuality.Minimal,status!!.actualQuality);mounted=false}
        val detached=pixels();mainClock.advanceTimeBy(1000);assertTrue(detached.contentEquals(pixels()),"dispose")
        // Cancel an in-flight whole-song job by replacing its actual document.
        runOnIdle{content=SyncedLyrics((0..119).map{SyncedLine("Synthetic stanza $it",null,it*3000+700,it*3000+2800)});mounted=true}
        waitForIdle();runOnIdle{content=core;options=options.copy(reducedMotion=false,quality=FoliaQuality.Standard);status=null}
        awaitReady();assertTrue(expected.contentEquals(pixels()),"replacement and remount")
        runOnIdle{content=SyncedLyrics(emptyList());clicked=null};pixels()
        onRoot().performTouchInput{click(center)};runOnIdle{assertNull(clicked)}
    }

    @Test fun tailQueriesAndCancellationRemainDeterministic()=runDesktopComposeUiTest(width=800,height=600){
        var prepared:FumePreparedStage?=null
        val lines=normalize(fumePublicFixture()).lines.map(::fumeLyric);val font=fumePublicFont()
        setContent{FumeArticleStage(lines,{1.0},{0},font,FumeFrameTheme(FumeInk(223,229,236),FumeInk(241,123,77)),
            onPrepared={prepared=it})}
        waitUntil(timeoutMillis=30000){prepared!=null};val p=prepared!!
        val track=p.track!!;val end=lines.maxOf{it.renderEnd}+2
        val times=listOf(end+.1,end+5.1,end+1.2,end+10.2)
        val expected=times.associateWith(track::sample)
        for(t in times.reversed())assertEquals(expected[t],track.sample(t),"tail order")
        assertNotEquals(track.sample(end),track.sample(end+5.1),"idle float must continue after last text")
        var checks=0
        assertFailsWith<CancellationException>{FumeCameraTrack(p.article,p.drawing.focusGeometry,listOf(FumeFocusEpoch(0.0,-1)),
            FumeCameraTrackConfig(p.viewport,p.overviewStart),end){checks++;throw CancellationException("test cancel")}}
        assertEquals(1,checks,"preparation must observe cancellation before camera integration")
        // Preparation cancellation context must not be consulted by later UI samples.
        track.sample(1.0);track.sample(end+2.0)
    }
}
