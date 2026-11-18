// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PublicDioramaTest {
    @Test fun actualCoreProjectionCallbacksClockAndLifecycle()=runDesktopComposeUiTest(width=800,height=600){
        fun song()=SyncedLyrics(listOf(SyncedLine("Synthetic mirror",null,700,2600),SyncedLine("Second stanza",null,3100,4600)))
        val original=song();var core by mutableStateOf(original,referentialEqualityPolicy())
        val font=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
        var p:DioramaTextPlayback?=null;var ready=false;var public by mutableStateOf(false)
        var mounted by mutableStateOf(true);var time by mutableStateOf(1600L);var offset by mutableIntStateOf(0)
        var width by mutableIntStateOf(800);var fontScale by mutableFloatStateOf(1f)
        var theme by mutableStateOf<FoliaStyle>(FoliaStyle.Diorama());var status:FoliaStatus?=null
        var options by mutableStateOf(FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false))
        var clicked:ISyncedLine?=null;var pressed:ISyncedLine?=null;var parents=0
        setContent{CompositionLocalProvider(LocalDensity provides Density(1f,fontScale)){
            Box(Modifier.fillMaxSize().background(Color(0xff162435)).clickable(
                interactionSource=remember{MutableInteractionSource()},indication=null){parents++}){
                if(mounted)if(public)FoliaLyricsView(core,{FoliaPlaybackSnapshot(time,false)},
                    Modifier.width(width.dp).height(600.dp),style=theme,typography=FoliaTypography(main=font),
                    options=options.copy(offsetMillis=offset),onStatus={status=it},
                    onLineClicked={clicked=it},onLinePressed={pressed=it})
                else DioramaPlaybackStage(core,{time/1000.0},font,Modifier.fillMaxSize(),
                    options=options,onPrepared={p=it},onState={r,_->ready=r})
            }
        }}
        fun pixels():IntArray{waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        fun awaitReady(){waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0&&status!!.diagnostics.none{it.startsWith("DioramaPrepar")}};waitForIdle()}
        waitUntil(timeoutMillis=30000){ready};val prepared=assertNotNull(p)
        val view=prepared.camera.perspectiveAt(1.6)
        val grid=(0..74).flatMap{y->(0..99).map{x->Offset(x*8f+3,y*8f+3)}}
        val point=grid.first{prepared.hitTest(1.6,view,it)==0}
        val blank=grid.first{prepared.hitTest(1.6,view,it)==null}
        val expected=pixels();assertTrue(expected.toSet().size>10,"real text/perspective is rendered")
        runOnIdle{public=true};awaitReady();assertTrue(expected.contentEquals(pixels()),"public routes actual core to the same stage")
        val out=File("../build/verification/diorama-public").apply{mkdirs()}
        ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("public-800.png"))
        onRoot().performTouchInput{click(point)};runOnIdle{assertSame(original.lines[0],clicked);assertEquals(0,parents);clicked=null}
        onRoot().performTouchInput{longClick(point)};runOnIdle{assertSame(original.lines[0],pressed);assertNull(clicked)}
        onRoot().performTouchInput{click(blank)};runOnIdle{assertEquals(1,parents);assertNull(clicked)}
        mainClock.advanceTimeBy(2000);assertTrue(expected.contentEquals(pixels()),"paused media freezes camera and all text layers")
        runOnIdle{time=4200};pixels();runOnIdle{time=1600};assertTrue(expected.contentEquals(pixels()),"backward seek")
        runOnIdle{offset=200;time=1800};assertTrue(expected.contentEquals(pixels()),"offset once")
        onRoot().performTouchInput{click(point)};runOnIdle{assertSame(original.lines[0],clicked);assertEquals(900,clicked!!.start+offset)}
        runOnIdle{theme=FoliaStyle.Diorama(DioramaTuning(enforceRasterBudget=false));status=null}
        awaitReady();assertTrue(expected.contentEquals(pixels()),"budget bypass rebuild preserves in-budget rendering")
        runOnIdle{theme=FoliaStyle.Diorama();status=null};awaitReady()
        runOnIdle{width=380;status=null};awaitReady()
        ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("public-380.png"))
        runOnIdle{width=800;status=null};awaitReady();assertTrue(expected.contentEquals(pixels()),"resize restore")
        runOnIdle{fontScale=1.35f;status=null};awaitReady();assertFalse(expected.contentEquals(pixels()),"system type scale changes text geometry")
        runOnIdle{fontScale=1f;status=null};awaitReady();assertTrue(expected.contentEquals(pixels()),"type scale restore")
        runOnIdle{options=options.copy(reducedMotion=true,quality=FoliaQuality.Minimal)};pixels()
        val reduced=pixels();mainClock.advanceTimeBy(1500);assertTrue(reduced.contentEquals(pixels()),"reduced pause")
        runOnIdle{theme=FoliaStyle.Classic()};pixels()
        runOnIdle{theme=FoliaStyle.Diorama();options=options.copy(reducedMotion=false,quality=FoliaQuality.Standard);status=null}
        awaitReady();assertTrue(expected.contentEquals(pixels()),"theme restore")
        runOnIdle{mounted=false};val detached=pixels();mainClock.advanceTimeBy(500);assertTrue(detached.contentEquals(pixels()))
        // Structural equality must not keep the previous document's callback refs.
        val replacement=song();runOnIdle{core=replacement;mounted=true;status=null;clicked=null}
        awaitReady();assertTrue(expected.contentEquals(pixels()),"remount")
        onRoot().performTouchInput{click(point)};runOnIdle{assertSame(replacement.lines[0],clicked)}
        runOnIdle{core=SyncedLyrics(emptyList());clicked=null};pixels()
        onRoot().performTouchInput{click(point)};runOnIdle{assertNull(clicked)}
    }

    @Test fun boundsRejectBeforeAllocationAndUnknownInputsStayExplicit(){
        val budget=DioramaRasterBudget(128)
        budget.reserve(4,4,2);assertEquals(128,budget.bytes)
        assertFailsWith<IllegalStateException>{budget.reserve(1,1,1)};assertEquals(128,budget.bytes)
        assertFailsWith<IllegalStateException>{DioramaRasterBudget().reserve(8193,1,1)}
        DioramaRasterBudget(limit=0,enforce=false).reserve(8193,8193,3)
        assertFailsWith<IllegalArgumentException>{DioramaTuning(fontScale=Float.NaN)}
        assertEquals(Color.Transparent,DioramaTuning().fogColor)
        assertTrue(DioramaTuning().enforceRasterBudget)
    }

    @Test fun oversizedPreparationReportsFailureAndReplacementRecovers()=runDesktopComposeUiTest(width=380,height=600){
        var core by mutableStateOf(SyncedLyrics(listOf(SyncedLine("W".repeat(180),null,700,2600))))
        var status:FoliaStatus?=null
        setContent{FoliaLyricsView(core,{FoliaPlaybackSnapshot(1600,false)},Modifier.fillMaxSize(),
            style=FoliaStyle.Diorama(),onStatus={status=it})}
        waitUntil(timeoutMillis=30000){status?.diagnostics?.any{it.startsWith("DioramaPreparationFailed:")}==true}
        onAllNodes(hasText("镜台文字纹理过大",substring=true)).assertCountEquals(1)
        runOnIdle{assertEquals(0,status!!.mainUnitCount);core=SyncedLyrics(listOf(SyncedLine("Recovered",null,700,2600)));status=null}
        waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0}
        runOnIdle{assertTrue(status!!.diagnostics.none{it.startsWith("DioramaPrepar")})}
    }
}
