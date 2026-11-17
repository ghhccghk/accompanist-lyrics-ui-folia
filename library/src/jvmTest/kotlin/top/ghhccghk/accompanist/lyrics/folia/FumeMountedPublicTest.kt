package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class FumeMountedPublicTest {
    @Test fun narrowSmooth()=capture(380,false)
    @Test fun narrowStepped()=capture(380,true)
    @Test fun wideSmooth()=capture(800,false)
    @Test fun wideStepped()=capture(800,true)
    private fun capture(width:Int,stepped:Boolean)=runDesktopComposeUiTest(width=width,height=600){
        val core=fumePublicFixture();val lines=normalize(core).lines.map(::fumeLyric);val font=fumePublicFont()
        val rows=File("../verification/fume-fixtures/mounted-frames.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}
            .map{it.split('\t')}.filter{it[1].toInt()==width&&it[2].toBoolean()==stepped}
        assertEquals(23,rows.size)
        var time by mutableStateOf(1000L);var public by mutableStateOf(false);var mounted by mutableStateOf(true)
        var ready=false;var status:FoliaStatus?=null;var prepared:FumePreparedStage?=null
        val theme=FumeFrameTheme(FumeInk(223,229,236),FumeInk(241,123,77),wordColors=listOf("amber echo" to FumeInk(48,202,255)))
        val tuning=FumeTuning(layoutSeed="FumeSceneFixture",textHoldRatio=.3f,
            trackingMode=if(stepped)FumeTrackingMode.Stepped else FumeTrackingMode.Smooth,
            wordColors=listOf(FoliaWordColor("amber echo",Color(0xff30caff))))
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435))){if(mounted){
            if(public)FoliaLyricsView(core,{FoliaPlaybackSnapshot(time,false)},Modifier.fillMaxSize(),style=FoliaStyle.Fume(tuning),
                typography=FoliaTypography(main=font),colors=FoliaColors(active=Color(0xfff17b4d)),
                options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false),onStatus={status=it})
            else FumeArticleStage(lines,{time/1000.0},{lines.indexOfLast{time/1000.0>=it.start&&time/1000.0<=it.renderEnd}},font,theme,
                Modifier.fillMaxSize(),hold=.3f.toDouble(),stepped=stepped,layoutSeed="FumeSceneFixture",stageEntryEnabled=false,
                onState={r,_->ready=r},onPrepared={prepared=it})
        }}}
        waitUntil(timeoutMillis=30000){ready};waitForIdle();val p=assertNotNull(prepared)
        val before=File("../build/verification/fume-mounted-kotlin-before-entry").apply{mkdirs()}
        val after=File("../build/verification/fume-mounted-kotlin").apply{mkdirs()}
        for(r in rows){runOnIdle{time=r[3].toLong()};waitForIdle();ImageIO.write(captureToImage().toAwtImage(),"png",before.resolve(r[0]))}
        runOnIdle{time=1000;public=true};waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0};waitForIdle()
        val report=mutableListOf("# file x y cameraScale parentScale vx vy focusX focusY velocityScale focusScale retargetStart duration bridge")
        val expected=mutableMapOf<Long,IntArray>()
        fun pixels():IntArray{val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        for(r in rows){
            val t=r[3].toLong();runOnIdle{time=t};waitForIdle()
            val s=fumeStageEntryScale(t/1000.0,p.article.firstStart)
            assertEquals(r[7].toDouble(),s,1e-6,"actual original parent scale $width/$stepped/$t")
            val state=p.track!!.sample(t/1000.0).state;val q=state.pose
            report+=listOf(r[0],q.x,q.y,q.scale,s,q.vx,q.vy,q.fx,q.fy,q.vs,q.fs,
                state.retarget.started,state.retarget.duration,state.retarget.bridge).joinToString("\t")
            expected[t]=pixels();ImageIO.write(captureToImage().toAwtImage(),"png",after.resolve(r[0]))
        }
        after.resolve("$width-$stepped.tsv").writeText(report.joinToString("\n")+"\n")
        for(t in listOf(800L,0L,1150L,717L,690L,1000L)){
            runOnIdle{time=t};waitForIdle();assertTrue(expected[t]!!.contentEquals(pixels()),"entry reverse seek $t")
        }
        runOnIdle{time=800};waitForIdle();val frozen=pixels();mainClock.advanceTimeBy(2000);waitForIdle()
        assertTrue(frozen.contentEquals(pixels()),"pause freezes mid-entry")
        runOnIdle{mounted=false};waitForIdle();runOnIdle{time=1000;status=null;mounted=true}
        waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0};runOnIdle{time=800};waitForIdle()
        assertTrue(frozen.contentEquals(pixels()),"remount reconstructs mid-entry")
        assertEquals(1.0,fumeStageEntryScale(.8,p.article.firstStart,true))
    }
}
