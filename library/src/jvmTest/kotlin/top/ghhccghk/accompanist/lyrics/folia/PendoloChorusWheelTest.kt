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
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.LocalClassicSoftwareLayers
import java.io.File
import java.util.Base64
import java.security.MessageDigest
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PendoloChorusWheelTest {
    @Test fun calm()=capture("latin",380,"calm")
    @Test fun normal()=capture("latin",380,"normal")
    @Test fun chaotic()=capture("latin",380,"chaotic")
    @Test fun wrap()=capture("wrap",380,"normal")
    @Test fun arabic()=capture("arabic",800,"normal")
    @Test fun cjk()=capture("cjk",800,"normal")
    @Test fun dense()=capture("latin",380,"normal",true)
    @Test fun normalSoftware()=capture("latin",380,"normal",software=true)
    @Test fun chaoticSoftware()=capture("latin",380,"chaotic",software=true)
    private fun capture(scene: String,width: Int,intensityName: String,dense:Boolean=false,software:Boolean=false)=runDesktopComposeUiTest(width=800,height=600) {
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val f=File("../verification/cadenza-fixtures/sceneInputs.tsv").readLines().first { it.startsWith("$scene\t") }.split('\t')
        val subtitles=when(scene){"arabic"->PendoloSubtitleSource("بالعالم","مرحبا");"cjk"->PendoloSubtitleSource("星光","向远方");else->PendoloSubtitleSource("beyond","Amber echoes")}
        val step=if(dense).16 else 4.0
        val lines=(0 until 8).map { i->PendoloStageLine(PendoloLyric(decode(f[1]),f[2].toDouble()+i*step,f[3].toDouble()+i*step,
            f[4].split('|').map { token->val w=token.split('~');PendoloTimedText(decode(w[0]),w[1].toDouble()+i*step,w[2].toDouble()+i*step) }),subtitles,isChorus=i%2==0) }
        val font=when(scene){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
        val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)),fontWeight=FontWeight.Bold,textDirection=TextDirection.Ltr)
        val intensity=PendoloIntensity.valueOf(intensityName.uppercase())
        val track=PendoloTickTrack(lines.map { PendoloLineTime(it.lyric.start,it.lyric.end) },intensity)
        val colors=if(scene=="cjk")listOf(FoliaWordColor("向远方",Color(0xff8ab4f8)),FoliaWordColor("星光",Color(0xffef55ae)))
            else listOf(FoliaWordColor("Amber",Color(0xffed71b8)),FoliaWordColor("echoes",Color(0xff73dea1)),FoliaWordColor("beyond",Color(0xff8ab4f8)))
        var t by mutableStateOf(0.0);var mounted by mutableStateOf(true)
        var haloOnly by mutableStateOf(false)
        var sourceRotation by mutableStateOf<Double?>(null)
        var observed:List<PendoloStageRow> = emptyList()
        setContent { CompositionLocalProvider(LocalClassicSoftwareLayers provides software) {
            Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if(mounted) {
                    val r=sourceRotation
                    if(r==null) PendoloPlaybackWheel(lines,{t},style,Modifier.width(width.dp).height(600.dp),intensity,wordColors=colors,haloOnly=haloOnly,onRows={observed=it})
                    else PendoloPlaybackWheel(lines,{t},style,Modifier.width(width.dp).height(600.dp),intensity,wordColors=colors,haloOnly=haloOnly,wheelRotationOverride={r},onRows={observed=it})
                }
            }
        }
        }
        val output=File("../build/verification/pendolo-chorus-stage-${if(software) "software" else "kotlin"}").apply { mkdirs() }
        val halo=File("../build/verification/pendolo-chorus-halo-${if(software) "software" else "kotlin"}").apply{mkdirs()}
        val isolated=File("../build/verification/pendolo-chorus-stage-source-rotation-${if(software) "software" else "kotlin"}").apply { mkdirs() }
        val frames=File("../verification/pendolo-fixtures/chorus-stage-frames.tsv").readLines().filterNot { it.startsWith('#')||it.isBlank() }.map { it.split('\t') }
            .filter { it[1]==scene&&it[2].toInt()==width&&it[3]==intensityName && it[4].toBooleanStrict()==dense }
        val source=File("../verification/pendolo-fixtures/chorus-stage-rows.tsv").readLines().filterNot { it.startsWith('#')||it.isBlank() }.map { it.split('\t') }.groupBy { it[0] }
        val residual=mutableListOf("# file row field original native error");val isolatedResidual=mutableListOf("# file row field original native error")
        var chorusTrack:PendoloChorusTrack?=null
        val hashes=linkedMapOf<String,String>()
        fun image()=captureToImage().toAwtImage().getSubimage(0,0,width,600)
        fun hash():String {
            val pic=image();val digest=MessageDigest.getInstance("SHA-256")
            for(p in pic.getRGB(0,0,width,600,null,0,width))for(shift in listOf(24,16,8,0))digest.update((p ushr shift).toByte())
            return digest.digest().joinToString(""){"%02x".format(it)}
        }
        fun set(frame:List<String>){runOnIdle{t=frame[5].toDouble();sourceRotation=null;haloOnly=false};waitForIdle()}
        for(f in frames){
            set(f);val key=f[0];val actual=track.at(t)
            assertEquals(f[6].toDouble(),actual.target,"$key target")
            assertEquals(source.getValue(key).map{it[1].toInt()},observed.map{it.item.index},"$key indices")
            if(chorusTrack==null)chorusTrack=PendoloChorusTrack(lines.map{PendoloLineTime(it.lyric.start,it.lyric.end)},lines.map{it.isChorus},width.toDouble(),600.0,List(lines.size){observed.first().reservedHeight},intensity)
            fun geometry(rotation:Double,dest:MutableList<String>){
                for((e,row)in source.getValue(key).zip(observed)){
                    val c=chorusTrack!!.at(row.item.index,t)
                    val original=e.drop(2).take(3).map(String::toDouble)+e[5].split(',').map(String::toDouble)
                    val native=listOf(c.scale,c.haloOpacity,c.haloScale)+pendoloRotatedContentRect(row,rotation,0.0,300.0,c.scale)
                    assertEquals(row.item.active && lines[row.item.index].isChorus,c.presentation.active,"$key activation")
                    for((field,pair)in original.zip(native).withIndex())dest+=listOf(key,row.item.index,field,pair.first,pair.second,abs(pair.first-pair.second)).joinToString("\t")
                }
            }
            geometry(actual.wheelRotation,residual)
            ImageIO.write(image(),"png",output.resolve("$key.png"));hashes[key]=hash()
            val rotation=f[7].toDouble();runOnIdle{sourceRotation=rotation};waitForIdle()
            geometry(rotation,isolatedResidual);ImageIO.write(image(),"png",isolated.resolve("$key.png"))
            runOnIdle{haloOnly=true};waitForIdle();ImageIO.write(image(),"png",halo.resolve("$key.png"))
        }
        output.resolve("$scene-$intensityName-$dense-residual.tsv").writeText(residual.joinToString("\n")+"\n")
        isolated.resolve("$scene-$intensityName-$dense-residual.tsv").writeText(isolatedResidual.joinToString("\n")+"\n")
        for(f in frames.shuffled(Random(419))){set(f);assertEquals(hashes[f[0]],hash(),"seek ${f[0]}")}
        val paused=hash();mainClock.advanceTimeBy(2000);waitForIdle();assertEquals(paused,hash(),"pause")
        runOnIdle{mounted=false};waitForIdle();runOnIdle{mounted=true};waitForIdle();assertEquals(paused,hash(),"remount")
        output.resolve("$scene-$intensityName-revisit.txt").writeText("24 frames + random revisits,pause,remount pixel-identical\n")
    }
}
