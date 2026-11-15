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
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.*
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PartitaParityTest {
    // Separate scenes keep each UI-test coroutine bounded without dropping any
    // original frame, reverse visit or pause assertion from the complete matrix.
    @Test fun narrowCalmWrap() = capture(380,"wrap",FoliaAnimationIntensity.Calm)
    @Test fun narrowNormalWrap() = capture(380,"wrap",FoliaAnimationIntensity.Normal)
    @Test fun narrowChaoticWrap() = capture(380,"wrap",FoliaAnimationIntensity.Chaotic)
    @Test fun wideCalmWrap() = capture(800,"wrap",FoliaAnimationIntensity.Calm)
    @Test fun wideNormalWrap() = capture(800,"wrap",FoliaAnimationIntensity.Normal)
    @Test fun wideChaoticWrap() = capture(800,"wrap",FoliaAnimationIntensity.Chaotic)
    @Test fun narrowCalmArabic() = capture(380,"arabic",FoliaAnimationIntensity.Calm)
    @Test fun narrowNormalArabic() = capture(380,"arabic",FoliaAnimationIntensity.Normal)
    @Test fun narrowChaoticArabic() = capture(380,"arabic",FoliaAnimationIntensity.Chaotic)
    @Test fun wideCalmArabic() = capture(800,"arabic",FoliaAnimationIntensity.Calm)
    @Test fun wideNormalArabic() = capture(800,"arabic",FoliaAnimationIntensity.Normal)
    @Test fun wideChaoticArabic() = capture(800,"arabic",FoliaAnimationIntensity.Chaotic)
    @Test fun narrowCalmGroup() = capture(380,"group",FoliaAnimationIntensity.Calm)
    @Test fun narrowNormalGroup() = capture(380,"group",FoliaAnimationIntensity.Normal)
    @Test fun narrowChaoticGroup() = capture(380,"group",FoliaAnimationIntensity.Chaotic)
    @Test fun wideCalmGroup() = capture(800,"group",FoliaAnimationIntensity.Calm)
    @Test fun wideNormalGroup() = capture(800,"group",FoliaAnimationIntensity.Normal)
    @Test fun wideChaoticGroup() = capture(800,"group",FoliaAnimationIntensity.Chaotic)
    @Test fun narrowCalmTight() = capture(380,"tight",FoliaAnimationIntensity.Calm)
    @Test fun narrowNormalTight() = capture(380,"tight",FoliaAnimationIntensity.Normal)
    @Test fun narrowChaoticTight() = capture(380,"tight",FoliaAnimationIntensity.Chaotic)
    @Test fun wideCalmTight() = capture(800,"tight",FoliaAnimationIntensity.Calm)
    @Test fun wideNormalTight() = capture(800,"tight",FoliaAnimationIntensity.Normal)
    @Test fun wideChaoticTight() = capture(800,"tight",FoliaAnimationIntensity.Chaotic)
    @Test fun narrowCalmAvoid() = capture(380,"wrap",FoliaAnimationIntensity.Calm,true)
    @Test fun narrowNormalAvoid() = capture(380,"wrap",FoliaAnimationIntensity.Normal,true)
    @Test fun narrowChaoticAvoid() = capture(380,"wrap",FoliaAnimationIntensity.Chaotic,true)
    @Test fun wideCalmAvoid() = capture(800,"wrap",FoliaAnimationIntensity.Calm,true)
    @Test fun wideNormalAvoid() = capture(800,"wrap",FoliaAnimationIntensity.Normal,true)
    @Test fun wideChaoticAvoid() = capture(800,"wrap",FoliaAnimationIntensity.Chaotic,true)
    private fun capture(width: Int, name: String, intensity: FoliaAnimationIntensity, avoid:Boolean=false) = runDesktopComposeUiTest(width=width,height=600) {
        val sceneRows=(File("../verification/classic-fixtures/scenes.tsv").readLines()+File("../verification/partita-fixtures/scenes.tsv").readLines())
            .filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}
        val expected=File("../verification/partita-fixtures/chunk-motion${if(avoid)"-avoid" else if(name=="tight")"-tight" else ""}.tsv").readLines()
            .filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}
        var scene by mutableStateOf("wrap")
        var level by mutableStateOf(FoliaAnimationIntensity.Calm)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false))
        var geometry: PartitaGeometry?=null
        val out=File("../build/verification/partita-kotlin").apply{mkdirs()}
        setContent {
            val input=remember(scene){sceneRows.filter{it[0]==scene}}
            val line=remember(input){normalize(SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(
                input.map{KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},null,KaraokeAlignment.Unspecified,
                input.first()[1].toInt(),input.first()[2].toInt())))).lines.single()}
            val index=remember(line){VisualTimeline(listOf(line))}
            val font=remember(scene){FontFamily(Font(File("../verification/assets/${if(scene=="arabic")"NotoSansArabic-Bold.ttf" else "FoliaParityLatin-Bold.ttf"}"),FontWeight.Bold))}
            Box(Modifier.fillMaxSize().background(Color(0xFF162435))) {
                (index.at(snapshot.positionMillis*1000).let{it.exiting+listOfNotNull(it.primary)}).forEach { epoch ->key(scene,level,epoch) {
                    PartitaLine(line,{snapshot},TextStyle(fontFamily=font,fontWeight=FontWeight.Bold,
                        fontSize=(width*.055).coerceIn(40.0,72.0).sp),FoliaColors(),FoliaOptions(animationIntensity=level),
                        config=PartitaConfig(avoidOverlap=avoid),
                        enterUs=epoch.enterUs,leaveUs=epoch.leaveUs,onLayout={geometry=it})
                } }
            }
        }
        run {
            runOnIdle { scene=name;level=intensity;snapshot=FoliaPlaybackSnapshot(0,false) }
            val times=if(name=="tight")listOf(700L,750L,800L,850L,900L,950L,1000L,1050L,1150L,1300L,1500L,1750L)
                else listOf(700L,750L,850L,1000L,1150L,1300L,1500L,1750L,2100L,2500L,2800L,3000L)
            val seen=mutableMapOf<Long,IntArray>()
            for(ms in times+times.reversed()) {
                runOnIdle{snapshot=FoliaPlaybackSnapshot(ms,false,snapshot.discontinuity+1)};waitForIdle()
                val image=captureToImage();val pixels=IntArray(image.width*image.height).also{image.readPixels(it)}
                mainClock.advanceTimeBy(1000);waitForIdle()
                assertContentEquals(pixels,IntArray(pixels.size).also{captureToImage().readPixels(it)},"Pause $name/$ms")
                seen[ms]?.let {assertContentEquals(it,pixels,"Reverse seek $name/$ms")}
                seen[ms]=pixels
                ImageIO.write(image.toAwtImage(),"png",out.resolve("${intensity.name.lowercase()}-$name-$width${if(avoid)"-avoid" else ""}-${ms}ms.png"))
                if(ms==1500L) {
                    val records=expected.filter{it[0]==intensity.name.lowercase()&&it[1]==name&&it[2].toInt()==width&&it[3]=="1500"}
                    val actual=geometry!!
                    assertEquals(records.size,actual.rows.size)
                    records.forEachIndexed { i,e ->
                        val box=actual.rows[i].box
                        // Default offsetTop is relative to the centered inner
                        // column; with fit, the first positive margin-top is
                        // inside that column and must NOT be subtracted away.
                        listOf(box.left,box.top-(if(avoid)0f else actual.rows.first().box.top),box.width,box.height).forEachIndexed { j,v ->
                            assertEquals(e[10+j].toDouble(),v.toDouble(),.51,"$intensity/$name/$width row$i component$j")
                        }
                    }
                }
            }
        }
    }
}
