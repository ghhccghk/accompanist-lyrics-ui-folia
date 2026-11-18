package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.motion.DampedSpring
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PendoloFollowerStageTest {
    @Test fun regularChaotic()=capture("regular","chaotic")
    @Test fun tiedNormal()=capture("tied","normal")
    @Test fun tiedChaotic()=capture("tied","chaotic")
    private fun capture(timing:String,intensityName:String)=runDesktopComposeUiTest(width=380,height=600) {
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val input=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first{it.startsWith("latin\t")}.split('\t')
        val step=if(timing=="tied")0 else 4000
        val sources=(0 until 8).map{i->KaraokeLine.MainKaraokeLine(input[4].split('|').map{token->
            val w=token.split('~');KaraokeSyllable(decode(w[0]),(w[1].toDouble()*1000).roundToInt()+i*step,(w[2].toDouble()*1000).roundToInt()+i*step)},
            "beyond",KaraokeAlignment.Unspecified,700+i*step,2800+i*step,"Amber echoes")}
        val lyrics=SyncedLyrics(sources);val bridged=pendoloCoreLines(normalize(lyrics));val lines=bridged.map{it.stage}
        sources.zip(bridged).forEach{(source,line)->assertSame(source,line.source)}
        val intensity=PendoloIntensity.valueOf(intensityName.uppercase())
        val publicIntensity=if(intensityName=="normal")FoliaAnimationIntensity.Normal else FoliaAnimationIntensity.Chaotic
        val track=PendoloTickTrack(lines.map{PendoloLineTime(it.lyric.start,it.lyric.end,it.renderEnd)},intensity)
        val c=pendoloTickConfig(intensity)
        val old=DampedSpring(-1.0,if(timing=="tied")7.0 else 0.0,0.0,c.stiffness,c.damping,c.mass)
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),
            fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        var time by mutableLongStateOf(950);var public by mutableStateOf(true);var legacy by mutableStateOf(false)
        var attached by mutableStateOf(true);var observed:List<PendoloStageRow> = emptyList()
        setContent{Box(Modifier.fillMaxSize().background(Color(0xff162435))){if(attached){
            if(public)FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(time,false)},Modifier.fillMaxSize(),
                typography=FoliaTypography(main=style,subtitle=style),colors=FoliaColors(active=Color(0xfff17b4d)),
                options=FoliaOptions(showNextLine=false,animationIntensity=publicIntensity),style=FoliaStyle.Pendolo())
            else PendoloPlaybackWheel(lines,{time/1000.0},style,Modifier.width(380.dp).height(600.dp),intensity,
                wheelRotationOverride=if(legacy){{-(old.sample(time/1000.0-.7).value-track.at(time/1000.0).target)*100/8}}else null,
                onRows={observed=it})
        }}}
        fun pixels():IntArray {waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val dir=File("../build/verification/pendolo-follower-stage-kotlin").apply{mkdirs()}
        val frames=File("../verification/pendolo-fixtures/follower-stage-v1/frames.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[1]==timing&&it[2]==intensityName}
        val rows=File("../verification/pendolo-fixtures/follower-stage-v1/rows.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.groupBy{it[0]}
        assertEquals(9,frames.size)
        val saved=mutableMapOf<Long,IntArray>();val report=mutableListOf("# file sourceValue legacyValue nativeValue sourceRotation legacyRotation nativeRotation")
        for(f in frames){
            runOnIdle{time=f[3].toLong();public=true;legacy=false};val actual=track.at(time/1000.0)
            assertEquals(f[4].toDouble(),actual.target)
            assertEquals(f[5].toDouble(),actual.value,1e-9,"source actual follower value ${f[0]}")
            assertEquals(f[6].toDouble(),actual.wheelRotation,1e-9,"source actual follower rotation ${f[0]}")
            val image=pixels();saved[time]=image;ImageIO.write(captureToImage().toAwtImage(),"png",dir.resolve(f[0]))
            runOnIdle{public=false};assertTrue(image.contentEquals(pixels()),"public real-core vs internal ${f[0]}")
            assertEquals(rows.getValue(f[0]).map{it[1].toInt()},observed.map{it.item.index})
            runOnIdle{legacy=true};pixels();ImageIO.write(captureToImage().toAwtImage(),"png",dir.resolve("legacy-${f[0]}"))
            val oldValue=old.sample(time/1000.0-.7).value;val oldRotation=-(oldValue-actual.target)*100/8
            report+=listOf(f[0],f[5],oldValue,actual.value,f[6],oldRotation,actual.wheelRotation).joinToString("\t")
        }
        for(f in frames.shuffled(Random(515))){runOnIdle{time=f[3].toLong();public=true;legacy=false};assertTrue(saved.getValue(time).contentEquals(pixels()),"seek")}
        val paused=pixels();mainClock.advanceTimeBy(1000);assertTrue(paused.contentEquals(pixels()),"pause")
        runOnIdle{attached=false};pixels();runOnIdle{attached=true};assertTrue(paused.contentEquals(pixels()),"remount")
        dir.resolve("$timing-$intensityName.tsv").writeText(report.joinToString("\n")+"\n")
    }
}
