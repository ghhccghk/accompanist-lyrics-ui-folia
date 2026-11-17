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
class CappellaPlaybackTest {
    @Test fun latinCalm()=capture("latin",380,480,CappellaIntensity.CALM)
    @Test fun latinNormal()=capture("latin",380,480,CappellaIntensity.NORMAL)
    @Test fun latinChaotic()=capture("latin",380,480,CappellaIntensity.CHAOTIC)
    @Test fun wrap()=capture("wrap",800,600,CappellaIntensity.NORMAL)
    @Test fun arabic()=capture("arabic",800,600,CappellaIntensity.NORMAL)
    @Test fun cjk()=capture("cjk",1200,600,CappellaIntensity.NORMAL)
    private fun capture(scene:String,width:Int,height:Int,intensity:CappellaIntensity)=runDesktopComposeUiTest(width=width,height=height) {
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val f=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first{it.startsWith("$scene\t")}.split('\t')
        val lines=(0 until 8).map { i->cappellaAnnotated(CappellaLine(decode(f[1]),f[2].toDouble()+i*4,f[3].toDouble()+i*4,
            f[4].split('|').map{v->val w=v.split('~');CappellaWord(decode(w[0]),w[1].toDouble()+i*4,w[2].toDouble()+i*4)})) }
        val font=when(scene){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        val frames=File("../verification/cappella-fixtures/playback-frames.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}
            .map{it.split('\t')}.filter{it[1]==scene&&it[2].toInt()==width&&it[3].toInt()==height&&it[4]==intensity.name.lowercase()}
        val sourceRows=File("../verification/cappella-fixtures/playback-observations.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.groupBy{it[0]}
        assertEquals(35,frames.size)
        var t by mutableStateOf(0.0);var mounted by mutableStateOf(true);var reduced by mutableStateOf(false)
        var observed:List<CappellaRowLayout> = emptyList()
        setContent { Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
            if(mounted)CappellaTargetStage(lines,"Synthetic title",{t},{cappellaCurrentLine(lines,t)},style,
                intensity=intensity,reducedMotion=reduced,playbackMotion=true,onRows={observed=it})
        } }
        fun hash():String {
            val image=captureToImage();val pixels=IntArray(image.width*image.height);image.readPixels(pixels)
            val digest=MessageDigest.getInstance("SHA-256")
            pixels.forEach{p->digest.update((p ushr 24).toByte());digest.update((p ushr 16).toByte());digest.update((p ushr 8).toByte());digest.update(p.toByte())}
            return digest.digest().joinToString(""){"%02x".format(it)}
        }
        val out=File("../build/verification/cappella-playback-kotlin").apply{mkdirs()}
        val states=linkedMapOf<Double,String>();val geometry=mutableListOf("# file row id bubbleRect avatarRect opacity")
        for(r in frames){
            val time=r[5].toDouble();runOnIdle{t=time};waitForIdle();assertTrue(observed.isNotEmpty())
            assertEquals(sourceRows.getValue(r[0]).map{it[2]},observed.map{it.message.id},
                "original presence/draw order, including inclusive renderEnd and popLayout: ${r[0]}")
            states[time]=hash()
            for((i,n)in observed.withIndex())geometry+=listOf(r[0],i,n.message.id,
                listOf(n.bubbleX,n.bubbleY,n.bubbleWidth,n.bubbleHeight).joinToString(","),
                listOf(n.avatarX,n.avatarY,40*n.scale,40*n.scale).joinToString(","),n.opacity).joinToString("\t")
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(r[0]))
        }
        for((time,expected)in states.entries.shuffled(Random(937))){runOnIdle{t=time};waitForIdle();assertEquals(expected,hash(),"random seek $scene/$intensity/$time")}
        val frozen=hash();mainClock.advanceTimeBy(2000);waitForIdle();assertEquals(frozen,hash(),"pause $scene")
        runOnIdle{mounted=false};waitForIdle();runOnIdle{mounted=true};waitForIdle();assertEquals(frozen,hash(),"remount $scene")
        runOnIdle{reduced=true;t=8.8};waitForIdle();val reduceHash=hash()
        mainClock.advanceTimeBy(2000);waitForIdle();assertEquals(reduceHash,hash(),"reduced pause $scene")
        out.resolve("$scene-${intensity.name.lowercase()}-geometry.tsv").writeText(geometry.joinToString("\n")+"\n")
    }
}
