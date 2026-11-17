package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.*

internal fun fumePublicFixture()=SyncedLyrics(listOf(
    KaraokeLine.MainKaraokeLine(listOf(KaraokeSyllable("Amber ",700,1200),KaraokeSyllable("echoes ",1200,2000),
        KaraokeSyllable("beyond",2000,2800)),null,KaraokeAlignment.Unspecified,700,2800),
    SyncedLine("A second phrase",null,3000,5000),SyncedLine("Quiet afterglow",null,5400,7600),
    SyncedLine("An invented paragraph whose text wraps naturally. ".repeat(5)+"\nA final echo",null,8000,11000)))
internal fun fumePublicFont()=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),fontWeight=FontWeight.Bold)

@OptIn(ExperimentalTestApi::class)
class PublicFumeStageTest {
    @Test fun narrowSmooth()=capture(380,false,0)
    @Test fun narrowSmoothGlow()=capture(380,false,1)
    @Test fun narrowStepped()=capture(380,true,0)
    @Test fun narrowSteppedGlow()=capture(380,true,1)
    @Test fun wideSmooth()=capture(800,false,0)
    @Test fun wideSmoothGlow()=capture(800,false,1)
    @Test fun wideStepped()=capture(800,true,0)
    @Test fun wideSteppedGlow()=capture(800,true,1)
    @Test fun fractionalSmooth()=capture(801,false,0)
    @Test fun fractionalSmoothGlow()=capture(801,false,1)
    @Test fun fractionalStepped()=capture(801,true,0)
    @Test fun fractionalSteppedGlow()=capture(801,true,1)

    private fun capture(width:Int,stepped:Boolean,glow:Int)=runDesktopComposeUiTest(width=width,height=600){
        val core=fumePublicFixture();val lines=normalize(core).lines.map(::fumeLyric);val font=fumePublicFont()
        val times=listOf(0L,700,750,1000,1300,2600,3000,3150,4200,5400,5600,6300,7600,8300,9500,10000,10700,11500)
        var time by mutableStateOf(1000L);var public by mutableStateOf(false);var mounted by mutableStateOf(true)
        var prepared:FumePreparedStage?=null;var ready=false;var status:FoliaStatus?=null
        val tuning=FumeTuning(layoutSeed="FumeSceneFixture",textHoldRatio=.3f,glowIntensity=glow.toFloat(),
            trackingMode=if(stepped)FumeTrackingMode.Stepped else FumeTrackingMode.Smooth,
            wordColors=listOf(FoliaWordColor("amber echo",Color(0xff30caff))))
        val theme=FumeFrameTheme(FumeInk(223,229,236),FumeInk(241,123,77),glow=glow.toDouble(),wordColors=listOf("amber echo" to FumeInk(48,202,255)))
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435))){if(mounted){
            if(public)FoliaLyricsView(core,{FoliaPlaybackSnapshot(time,false)},Modifier.fillMaxSize(),
                style=FoliaStyle.Fume(tuning),typography=FoliaTypography(main=font),colors=FoliaColors(active=Color(0xfff17b4d)),
                options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false),onStatus={status=it})
            else FumeArticleStage(lines,{time/1000.0},{lines.indexOfLast{time/1000.0>=it.start&&time/1000.0<=it.renderEnd}},font,theme,
                Modifier.fillMaxSize(),hold=.3f.toDouble(),stepped=stepped,layoutSeed="FumeSceneFixture",
                onPrepared={prepared=it},onState={r,_->ready=r})
        }}}
        fun hash():String{
            val image=captureToImage();val pixels=IntArray(image.width*image.height);image.readPixels(pixels)
            val digest=MessageDigest.getInstance("SHA-256")
            pixels.forEach{p->for(s in listOf(24,16,8,0))digest.update((p ushr s).toByte())}
            return digest.digest().joinToString(""){"%02x".format(it)}
        }
        waitUntil(timeoutMillis=30000){ready};waitForIdle();val track=assertNotNull(prepared).track!!
        val expected=times.associateWith{t->runOnIdle{time=t};waitForIdle();hash()}
        runOnIdle{time=1000;public=true};waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0};waitForIdle()
        val out=File("../build/verification/fume-public-kotlin").apply{mkdirs()}
        val report=mutableListOf("# time\tx\ty\tscale\trestore\tnodes")
        for(t in times){
            runOnIdle{time=t};waitForIdle();assertEquals(expected[t],hash(),"actual-core public/internal whole article $width/$glow/$stepped/$t")
            val f=track.sample(t/1000.0);val p=f.state.pose
            report+=listOf(t/1000.0,p.x,p.y,p.scale,f.restore,track.preparedNodeCount).joinToString("\t")
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$width-$glow-$stepped-$t.png"))
        }
        out.resolve("$width-$glow-$stepped.tsv").writeText(report.joinToString("\n")+"\n")
        for(t in times.shuffled(Random(318))){runOnIdle{time=t};waitForIdle();assertEquals(expected[t],hash(),"random seek")}
        val frozen=hash();mainClock.advanceTimeBy(2000);waitForIdle();assertEquals(frozen,hash(),"pause")
        runOnIdle{mounted=false};waitForIdle();runOnIdle{time=1000;status=null;mounted=true}
        waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0};waitForIdle();assertEquals(expected[1000],hash(),"remount")
    }
}
