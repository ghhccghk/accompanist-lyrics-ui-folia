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
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import java.util.Base64
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PendoloPlaybackWheelTest {
    @Test fun calm()=capture("latin",380,"calm")
    @Test fun normal()=capture("latin",380,"normal")
    @Test fun chaotic()=capture("latin",380,"chaotic")
    @Test fun wrap()=capture("wrap",380,"normal")
    @Test fun arabic()=capture("arabic",800,"normal")
    @Test fun cjk()=capture("cjk",800,"normal")
    private fun capture(scene: String,width: Int,intensityName: String)=runDesktopComposeUiTest(width=800,height=600) {
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val f=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first { it.startsWith("$scene\t") }.split('\t')
        val subtitles=when(scene){"arabic"->PendoloSubtitleSource("بالعالم","مرحبا");"cjk"->PendoloSubtitleSource("星光","向远方");else->PendoloSubtitleSource("beyond","Amber echoes")}
        val lines=(0 until 8).map { i->PendoloStageLine(PendoloLyric(decode(f[1]),f[2].toDouble()+i*4,f[3].toDouble()+i*4,
            f[4].split('|').map { token->val w=token.split('~');PendoloTimedText(decode(w[0]),w[1].toDouble()+i*4,w[2].toDouble()+i*4) }),subtitles) }
        val font=when(scene){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        val intensity=PendoloIntensity.valueOf(intensityName.uppercase())
        val track=PendoloTickTrack(lines.map { PendoloLineTime(it.lyric.start,it.lyric.end) },intensity)
        var t by mutableStateOf(0.0);var mounted by mutableStateOf(true)
        var sourceRotation by mutableStateOf<Double?>(null)
        var observed:List<PendoloStageRow> = emptyList()
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if(mounted) {
                    val r=sourceRotation
                    if(r==null) PendoloPlaybackWheel(lines,{t},style,Modifier.width(width.dp).height(600.dp),intensity,onRows={observed=it})
                    else PendoloPlaybackWheel(lines,{t},style,Modifier.width(width.dp).height(600.dp),intensity,wheelRotationOverride={r},onRows={observed=it})
                }
            }
        }
        val output=File("../build/verification/pendolo-tick-stage-kotlin").apply { mkdirs() }
        val isolated=File("../build/verification/pendolo-tick-stage-source-rotation-kotlin").apply { mkdirs() }
        val frames=File("../verification/pendolo-fixtures/tick-stage-frames.tsv").readLines().filterNot { it.startsWith('#')||it.isBlank() }.map { it.split('\t') }
            .filter { it[1]==scene&&it[2].toInt()==width&&it[3]==intensityName }
        val source=File("../verification/pendolo-fixtures/tick-stage-rows.tsv").readLines().filterNot { it.startsWith('#')||it.isBlank() }.map { it.split('\t') }.groupBy { it[0] }
        val sourceWidths=File("../verification/pendolo-fixtures/tick-stage-widths.tsv").readLines().filterNot { it.startsWith('#')||it.isBlank() }.map { it.split('\t') }.groupBy { it[0] }
        val residual=mutableListOf("# file field original native error"); val isolatedResidual=mutableListOf("# file row field original native error")
        val widthResidual=mutableListOf("# file row field original native error")
        val hashes=linkedMapOf<String,String>()
        fun image()=captureToImage().toAwtImage().getSubimage(0,0,width,600)
        fun hash():String {
            val pic=image();val digest=MessageDigest.getInstance("SHA-256")
            for(p in pic.getRGB(0,0,width,600,null,0,width))for(shift in listOf(24,16,8,0))digest.update((p ushr shift).toByte())
            return digest.digest().joinToString(""){"%02x".format(it)}
        }
        fun set(frame:List<String>){runOnIdle{t=frame[4].toDouble();sourceRotation=null};waitForIdle()}
        for(f in frames){
            set(f);val key=f[0];val actual=track.at(t)
            assertEquals(f[5].toDouble(),actual.target,"$key target")
            assertEquals(source.getValue(key).map{it[1].toInt()},observed.map{it.item.index},"$key indices")
            for((e,row)in sourceWidths.getValue(key).zip(observed)) {
                for((field,pair)in listOf(e[6].toDouble(),e[7].toDouble()).zip(listOf(row.width,row.blockHeight)).withIndex())
                    widthResidual+=listOf(key,row.item.index,field,pair.first,pair.second,abs(pair.first-pair.second)).joinToString("\t")
            }
            val values=listOf(actual.value,actual.velocity,actual.wheelRotation,actual.textCorrection)
            for((field,pair)in f.drop(6).map(String::toDouble).zip(values).withIndex())
                residual+=listOf(key,"tick$field",pair.first,pair.second,abs(pair.first-pair.second)).joinToString("\t")
            fun geometry(rotation:Double,dest:MutableList<String>){
                for((e,row)in source.getValue(key).zip(observed)){
                    val original=e[3].split(',').map(String::toDouble)+e[2].toDouble()
                    val native=pendoloRotatedContentRect(row,rotation,0.0,300.0)+pendoloRotatingOpacity(row.item.angleDeg,rotation,row.item.alpha)
                    for((field,pair)in original.zip(native).withIndex())dest+=listOf(key,row.item.index,field,pair.first,pair.second,abs(pair.first-pair.second)).joinToString("\t")
                }
            }
            geometry(actual.wheelRotation,residual)
            ImageIO.write(image(),"png",output.resolve("$key.png"));hashes[key]=hash()
            val rotation=f[8].toDouble();runOnIdle{sourceRotation=rotation};waitForIdle()
            geometry(rotation,isolatedResidual);ImageIO.write(image(),"png",isolated.resolve("$key.png"))
        }
        output.resolve("$scene-$intensityName-residual.tsv").writeText(residual.joinToString("\n")+"\n")
        output.resolve("$scene-$intensityName-widths.tsv").writeText(widthResidual.joinToString("\n")+"\n")
        isolated.resolve("$scene-$intensityName-residual.tsv").writeText(isolatedResidual.joinToString("\n")+"\n")
        for(f in frames.shuffled(Random(419))){set(f);assertEquals(hashes[f[0]],hash(),"seek ${f[0]}")}
        val paused=hash();mainClock.advanceTimeBy(2000);waitForIdle();assertEquals(paused,hash(),"pause")
        runOnIdle{mounted=false};waitForIdle();runOnIdle{mounted=true};waitForIdle();assertEquals(paused,hash(),"remount")
        output.resolve("$scene-$intensityName-revisit.txt").writeText("24 frames + random revisits,pause,remount pixel-identical\n")
    }
}
