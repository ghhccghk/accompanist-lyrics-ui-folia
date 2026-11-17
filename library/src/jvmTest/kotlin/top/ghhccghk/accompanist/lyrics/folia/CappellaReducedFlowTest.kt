package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaReducedFlowTest {
    @Test fun rapidCalm()=verify("rapid",CappellaIntensity.CALM)
    @Test fun rapidNormal()=verify("rapid",CappellaIntensity.NORMAL)
    @Test fun rapidChaotic()=verify("rapid",CappellaIntensity.CHAOTIC)
    @Test fun overlapCalm()=verify("overlap",CappellaIntensity.CALM)
    @Test fun overlapNormal()=verify("overlap",CappellaIntensity.NORMAL)
    @Test fun overlapChaotic()=verify("overlap",CappellaIntensity.CHAOTIC)

    private fun verify(timeline:String,intensity:CappellaIntensity)=runDesktopComposeUiTest(width=380,height=480) {
        fun records(name:String)=File("../verification/cappella-fixtures/$name.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        fun decode(text:String)=String(Base64.getDecoder().decode(text),Charsets.UTF_8)
        // Use the same pressure inputs through the real published core adapter.
        val lyrics=SyncedLyrics(records("presence-inputs").filter{it[0]==timeline}.map { r ->
            KaraokeLine.MainKaraokeLine(r[5].split('|').map { token -> val w=token.split('~')
                KaraokeSyllable(decode(w[0]),(w[1].toDouble()*1000).roundToInt(),(w[2].toDouble()*1000).roundToInt()) },
                null,KaraokeAlignment.Unspecified,(r[3].toDouble()*1000).roundToInt(),(r[4].toDouble()*1000).roundToInt())
        })
        val lines=cappellaCoreLines(normalize(lyrics)).map{it.stage}
        val font=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),fontWeight=FontWeight.Bold)
        var prep:CappellaStagePreparation?=null;var track:CappellaPlaybackPreparation?=null
        var t by mutableStateOf(1.02);var public by mutableStateOf(true);var mounted by mutableStateOf(true)
        val options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false,reducedMotion=true,
            animationIntensity=FoliaAnimationIntensity.valueOf(intensity.name.lowercase().replaceFirstChar{it.uppercase()}))
        setContent {
            val density=LocalDensity.current;val resolver=LocalFontFamilyResolver.current;val measurer=rememberTextMeasurer()
            val prepared=remember{CappellaStagePreparation(lines,"Synthetic title",cappellaStageSize(380.0),intensity,measurer,font,density,resolver)}
            val animation=remember{CappellaPlaybackPreparation(prepared,480.0)}
            SideEffect {prep=prepared;track=animation}
            Box(Modifier.fillMaxSize().background(Color(0xff162435))) { if(mounted) {
                if(public) FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot((t*1000).roundToLong(),false)},Modifier.fillMaxSize(),
                    style=FoliaStyle.Cappella(CappellaTuning(title="Synthetic title")),typography=FoliaTypography(main=font),
                    colors=FoliaColors(active=Color(0xfff17b4d)),options=options)
                else CappellaTargetStage(lines,"Synthetic title",{t},{cappellaCurrentLine(lines,t)},font,
                    intensity=intensity,reducedMotion=true,playbackMotion=true)
            } }
        }
        waitForIdle()
        val p=assertNotNull(prep);val a=assertNotNull(track)
        val out=File("../build/verification/cappella-reduced-flow-kotlin").apply{mkdirs()}
        val rows=mutableListOf("# time id actualTop targetTop actualHeight targetHeight")
        var peak=0.0
        val times=records("presence-frames").filter{it[1]==timeline&&it[2]==intensity.name.lowercase()}.map{it[3].toDouble()}
        for(time in times) {
            val reduced=a.at(time,true);var top=p.size.top
            assertEquals(p.at(time,cappellaCurrentLine(lines,time),480.0).map{it.message.id},reduced.map{it.layout.message.id},"reduced live order")
            for(row in reduced) {
                val size=p.rowSize(row.layout.message,row.active,time)
                val height=max(40.0,size.bubble.height)
                rows+=listOf(time,row.layout.message.id,row.layout.rowTop,top,row.layout.rowHeight,height).joinToString("\t")
                peak=max(peak,max(abs(top-row.layout.rowTop),abs(height-row.layout.rowHeight)))
                assertEquals(1.0,row.layout.scale,"reduced scale")
                top+=height+12
            }
            assertEquals(reduced,a.at(time,true),"query order must not mutate reduced state")
        }
        out.resolve("$timeline-${intensity.name.lowercase()}.tsv").writeText(rows.joinToString("\n")+"\n")
        fun pixels():IntArray {waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
        val expected=pixels()
        ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$timeline-${intensity.name.lowercase()}-1020.png"))
        runOnIdle{public=false};assertContentEquals(expected,pixels(),"public reduced core bridge")
        mainClock.advanceTimeBy(1000);assertContentEquals(expected,pixels(),"paused")
        runOnIdle{t=2.2};pixels();runOnIdle{t=1.02};assertContentEquals(expected,pixels(),"reverse seek")
        runOnIdle{mounted=false};pixels();runOnIdle{mounted=true};assertContentEquals(expected,pixels(),"remount")
        assertTrue(peak<1e-9,"reduced layout must use current targets, not animated flow: $peak px")
    }
}
