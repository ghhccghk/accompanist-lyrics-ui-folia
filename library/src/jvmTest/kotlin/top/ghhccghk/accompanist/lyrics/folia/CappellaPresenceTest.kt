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
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import java.util.Base64
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class CappellaPresenceTest {
    @Test fun rapidCalm()=capture("rapid",CappellaIntensity.CALM)
    @Test fun rapidNormal()=capture("rapid",CappellaIntensity.NORMAL)
    @Test fun rapidChaotic()=capture("rapid",CappellaIntensity.CHAOTIC)
    @Test fun overlapCalm()=capture("overlap",CappellaIntensity.CALM)
    @Test fun overlapNormal()=capture("overlap",CappellaIntensity.NORMAL)
    @Test fun overlapChaotic()=capture("overlap",CappellaIntensity.CHAOTIC)
    private fun capture(timeline:String,intensity:CappellaIntensity)=runDesktopComposeUiTest(width=380,height=480) {
        fun records(name:String)=File("../verification/cappella-fixtures/$name.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val lines=records("presence-inputs").filter{it[0]==timeline}.map{r->
            cappellaAnnotated(CappellaLine(decode(r[2]),r[3].toDouble(),r[4].toDouble(),r[5].split('|').map{
                val w=it.split('~');CappellaWord(decode(w[0]),w[1].toDouble(),w[2].toDouble())
            }))
        }
        val frames=records("presence-frames").filter{it[1]==timeline&&it[2]==intensity.name.lowercase()}
        assertTrue(frames.isNotEmpty())
        val original=records("presence-observations").groupBy{it[0]}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)),
            fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        var t by mutableStateOf(0.0);var mounted by mutableStateOf(true)
        var observed:List<CappellaRowLayout> = emptyList()
        setContent { Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
            if(mounted)CappellaTargetStage(lines,"Synthetic title",{t},{cappellaCurrentLine(lines,t)},style,
                intensity=intensity,playbackMotion=true,onRows={observed=it})
        } }
        fun hash():String {
            val image=captureToImage();val pixels=IntArray(image.width*image.height);image.readPixels(pixels)
            val digest=MessageDigest.getInstance("SHA-256")
            pixels.forEach{p->digest.update((p ushr 24).toByte());digest.update((p ushr 16).toByte());digest.update((p ushr 8).toByte());digest.update(p.toByte())}
            return digest.digest().joinToString(""){"%02x".format(it)}
        }
        val out=File("../build/verification/cappella-presence-kotlin").apply{mkdirs()}
        val states=linkedMapOf<Double,String>();val errors=mutableListOf<String>()
        val geometry=mutableListOf("# file row id bubbleRect avatarRect opacity rowTop rowHeight")
        for(r in frames){
            val time=r[3].toDouble();runOnIdle{t=time};waitForIdle();states[time]=hash()
            val ids=observed.map{it.message.id};val expected=original.getValue(r[0]).map{it[2]}
            if(expected!=ids)errors+="${r[0]} source=$expected native=$ids"
            for((i,n)in observed.withIndex())geometry+=listOf(r[0],i,n.message.id,
                listOf(n.bubbleX,n.bubbleY,n.bubbleWidth,n.bubbleHeight).joinToString(","),
                listOf(n.avatarX,n.avatarY,40*n.scale,40*n.scale).joinToString(","),n.opacity,n.rowTop,n.rowHeight).joinToString("\t")
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(r[0]))
        }
        out.resolve("$timeline-${intensity.name.lowercase()}-geometry.tsv").writeText(geometry.joinToString("\n")+"\n")
        for((time,expected)in states.entries.shuffled(Random(492))){runOnIdle{t=time};waitForIdle();assertEquals(expected,hash(),"seek $timeline/$intensity/$time")}
        val frozen=hash();mainClock.advanceTimeBy(2000);waitForIdle();assertEquals(frozen,hash(),"pause")
        runOnIdle{mounted=false};waitForIdle();runOnIdle{mounted=true};waitForIdle();assertEquals(frozen,hash(),"remount")
        assertTrue(errors.isEmpty(),errors.joinToString("\n"))
    }
}
