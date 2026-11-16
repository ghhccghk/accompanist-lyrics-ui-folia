package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.*
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.renderProfile
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class TiltParityTest {
    @Test fun normalWrapNarrow()=capture("normal","wrap",380)
    @Test fun normalWrapWide()=capture("normal","wrap",800)
    @Test fun tiltedWrapNarrow()=capture("tilt","wrap",380)
    @Test fun tiltedWrapWide()=capture("tilt","wrap",800)
    @Test fun singleWrapNarrow()=capture("single","wrap",380)
    @Test fun singleWrapWide()=capture("single","wrap",800)
    @Test fun normalArabicNarrow()=capture("normal","arabic",380)
    @Test fun normalArabicWide()=capture("normal","arabic",800)
    @Test fun tiltedArabicNarrow()=capture("tilt","arabic",380)
    @Test fun tiltedArabicWide()=capture("tilt","arabic",800)
    @Test fun singleArabicNarrow()=capture("single","arabic",380)
    @Test fun singleArabicWide()=capture("single","arabic",800)
    private fun capture(mode:String,name:String,width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        val input=File("../verification/classic-fixtures/scenes.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}
            .map{it.split('\t')}.filter{it[0]==name}
        val line=normalize(SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(input.map{
            KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},null,KaraokeAlignment.Unspecified,
            input.first()[1].toInt(),input.first()[2].toInt())))).lines.single()
        val end=renderProfile(line.startUs,line.endUs,line.fragments.last().endUs).renderEndUs
        val font=FontFamily(Font(File("../verification/assets/${if(name=="arabic")"NotoSansArabic-Bold.ttf" else "FoliaParityLatin-Bold.ttf"}"),FontWeight.Bold))
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false))
        var geometry=emptyList<TiltRowBox>()
        val out=File("../build/verification/tilt-kotlin").apply{mkdirs()}
        val config=TiltConfig(if(mode=="single")0.0 else .75,if(mode=="normal")0.0 else 1.0)
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF162435))) {
                if(snapshot.positionMillis*1000>=line.startUs && snapshot.positionMillis*1000<end+450_000)
                    TiltLine(line,{snapshot},TextStyle(fontFamily=font,fontWeight=FontWeight.Bold),FoliaColors(),FoliaOptions(),
                        config=config,leaveUs=end,onLayout={geometry=it})
            }
        }
        val times=listOf(700L,750L,850L,1000L,1200L,1500L,1900L,2100L,2400L,3000L,3200L,3500L)
        val previous=mutableMapOf<Long,IntArray>()
        for(ms in times+times.reversed()) {
            runOnIdle{snapshot=FoliaPlaybackSnapshot(ms,false,snapshot.discontinuity+1)};waitForIdle()
            val image=captureToImage();val pixels=IntArray(image.width*image.height).also{image.readPixels(it)}
            mainClock.advanceTimeBy(1000);waitForIdle()
            assertContentEquals(pixels,IntArray(pixels.size).also{captureToImage().readPixels(it)},"Pause $mode/$name/$width/$ms")
            previous[ms]?.let{assertContentEquals(it,pixels,"Reverse seek $mode/$name/$width/$ms")};previous[ms]=pixels
            ImageIO.write(image.toAwtImage(),"png",out.resolve("$mode-$name-$width-${ms}ms.png"))
            if(ms==1500L) {
                val expected=File("../verification/tilt-fixtures/mounted-rows.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}
                    .map{it.split('\t')}.filter{it[0]==mode && it[1]==name && it[2].toInt()==width && it[3]=="1500"}
                assertEquals(expected.map{String(Base64.getDecoder().decode(it[5]),Charsets.UTF_8)},geometry.map{it.text})
                val exact=File("../verification/tilt-fixtures/mounted-geometry.tsv").readLines()
                    .filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}
                    .filter{it[0]==mode && it[1]==name && it[2].toInt()==width}
                if(name=="wrap")geometry.forEachIndexed { i,g ->
                    // Compare untransformed CSS content width, not rounded
                    // offsetWidth (which adds up to 0.5px measurement error).
                    // Arabic per-EGC DOM versus intact shaping stays open.
                    assertTrue(abs(exact[i][6].toDouble()-g.box.width)<=.51,
                        "Original inline-block row width $mode/$width/$i: expected ${exact[i][6]}, actual ${g.box.width}")
                }
                val report=geometry.mapIndexed { i,g ->
                    val e=expected[i]
                    listOf(mode,name,width,i,g.box.left,g.box.top,g.box.width,g.box.height,g.fontSize,
                        e[8],e[9],e[10],e[11],e[7]).joinToString("\t")
                }
                out.resolve("$mode-$name-$width-geometry.tsv").writeText(report.joinToString("\n")+"\n")
            }
        }
    }
}
