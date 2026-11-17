package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PublicCadenzaTest {
    private fun font(name:String="FoliaParityLatin-Bold.ttf")=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$name"),FontWeight.Bold)),
        fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
    private val options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false)
    private val colors=FoliaColors(active=Color(0xfff17b4d))

    @Test fun publicPauseSeekResizeOffsetInputAndDisposal()=runDesktopComposeUiTest(width=800,height=600){
        val source=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Amber echo",700,1900)),null,KaraokeAlignment.Unspecified,700,1900)
        val lyrics=SyncedLyrics(listOf(source));val line=cadenzaLyric(normalize(lyrics).lines.single())
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1500,false));var public by mutableStateOf(false)
        var attached by mutableStateOf(true);var width by mutableStateOf(800);var opts by mutableStateOf(options)
        var clicked:ISyncedLine?=null;var pressed:ISyncedLine?=null;var status:FoliaStatus?=null
        val tuning=CadenzaTuning();val style=font()
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435))){if(attached){
            if(public)FoliaLyricsView(lyrics,{snapshot},Modifier.size(width.dp,600.dp),style=FoliaStyle.Cadenza(tuning),
                typography=FoliaTypography(main=style),options=opts,colors=colors,onStatus={status=it},onLineClicked={clicked=it},onLinePressed={pressed=it})
            else CadenzaTargetStage(line,{(snapshot.positionMillis-opts.offsetMillis)/1000.0},style,Modifier.size(width.dp,600.dp),motion=true,
                fontScale=tuning.fontScale.toDouble(),widthRatio=tuning.widthRatio.toDouble(),palette=CadenzaPalette(colors.text,colors.active))
        }}}
        fun pixels():IntArray{waitForIdle();val img=captureToImage();return IntArray(img.width*img.height).also{img.readPixels(it)}}
        val expected=pixels();runOnIdle{public=true};assertContentEquals(expected,pixels(),"public entry uses Cadenza response, not classic")
        mainClock.advanceTimeBy(2000);assertContentEquals(expected,pixels(),"pause freezes response")
        runOnIdle{snapshot=FoliaPlaybackSnapshot(800,false,1)};pixels();runOnIdle{snapshot=FoliaPlaybackSnapshot(1500,false,2)}
        assertContentEquals(expected,pixels(),"random seek reconstructs")
        runOnIdle{opts=opts.copy(offsetMillis=100);snapshot=FoliaPlaybackSnapshot(1600,false,3)}
        assertContentEquals(expected,pixels(),"offset once")
        runOnIdle{width=380};pixels();runOnIdle{width=800};assertContentEquals(expected,pixels(),"resize recomputes")
        onRoot().performTouchInput{click(Offset(400f,250f))};runOnIdle{assertSame(source,clicked);clicked=null}
        onRoot().performTouchInput{click(Offset(2f,2f))};runOnIdle{assertNull(clicked)}
        onRoot().performTouchInput{longClick(Offset(400f,250f))};runOnIdle{assertSame(source,pressed)}
        runOnIdle{opts=opts.copy(reducedMotion=true,quality=FoliaQuality.Minimal)}
        val reduced=pixels();runOnIdle{assertEquals(FoliaQuality.Minimal,assertNotNull(status).actualQuality)}
        mainClock.advanceTimeBy(1000);assertContentEquals(reduced,pixels())
        runOnIdle{attached=false};val empty=pixels();assertFalse(empty.contentEquals(reduced))
        mainClock.advanceTimeBy(1000);assertContentEquals(empty,pixels(),"disposed component has no renderer job")
        runOnIdle{attached=true;opts=options;snapshot=FoliaPlaybackSnapshot(1500,false,4)}
        assertContentEquals(expected,pixels(),"reattach at same media time")
    }

    @Test fun overlapResumptionAndGapUseSourceTimelineWithoutClassicExitLayers()=runDesktopComposeUiTest(width=380,height=600){
        fun source(text:String,start:Int,end:Int)=KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable(text,start,end)),null,KaraokeAlignment.Unspecified,start,end)
        val lyrics=SyncedLyrics(listOf(source("long amber",700,5000),source("brief echo",1400,1700),source("beyond",5400,5800)))
        val lines=normalize(lyrics).lines.map(::cadenzaLyric);val timeline=CadenzaTimeline(lines);val style=font();val tuning=CadenzaTuning()
        var ms by mutableStateOf(0L);var public by mutableStateOf(false)
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435))){
            if(public)FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(ms,false)},Modifier.fillMaxSize(),style=FoliaStyle.Cadenza(tuning),
                typography=FoliaTypography(main=style),colors=colors,options=options)
            else {val selection=timeline.at(ms/1000.0);selection.active?.let{i->key(i,selection.entered){
                CadenzaTargetStage(lines[i],{ms/1000.0},style,Modifier.fillMaxSize(),motion=true,motionStart=selection.entered!!,
                    fontScale=tuning.fontScale.toDouble(),widthRatio=tuning.widthRatio.toDouble(),palette=CadenzaPalette(colors.text,colors.active))
            }}}
        }}
        fun pixels():IntArray{waitForIdle();val img=captureToImage();return IntArray(img.width*img.height).also{img.readPixels(it)}}
        val times=listOf(0L,700,850,1400,1700,2079,2080,2081,2500,5000,5379,5380,5381,5400,5500,5800,6180,6181)
        val captures=mutableMapOf<Long,IntArray>()
        for(time in times){runOnIdle{ms=time;public=false};val expected=pixels();runOnIdle{public=true}
            assertContentEquals(expected,pixels(),"source selection at $time");captures[time]=pixels()}
        for(time in times.shuffled(Random(814))){runOnIdle{ms=time};assertContentEquals(captures.getValue(time),pixels(),"history-independent public seek at $time")}
    }

    @Test fun lineOnlyCoreLyricsAreVisibleWithoutInventingWordClock()=runDesktopComposeUiTest(width=380,height=600){
        val lyrics=SyncedLyrics(listOf(SyncedLine("Line timing only",translation=null,start=700,end=2800)))
        val normalized=normalize(lyrics).lines.single();val adapted=cadenzaLyric(normalized)
        assertEquals(1,adapted.words.size);assertEquals(normalized.text,adapted.words.single().text)
        assertEquals(CadenzaReveal.INSTANT,cadenzaTiming(adapted).reveal)
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435))){FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(1500,false)},
            Modifier.fillMaxSize(),style=FoliaStyle.Cadenza(),typography=FoliaTypography(main=font()),colors=colors,options=options)}}
        waitForIdle();onNodeWithContentDescription("Line timing only").assertExists()
        val im=captureToImage();val pixels=IntArray(im.width*im.height);im.readPixels(pixels)
        assertTrue(pixels.any{it!=Color(0xff162435).toArgb()},"line-only content is rendered")
    }
}
