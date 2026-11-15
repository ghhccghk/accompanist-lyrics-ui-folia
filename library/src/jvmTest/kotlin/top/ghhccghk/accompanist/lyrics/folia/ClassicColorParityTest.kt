package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.LocalClassicNativeBodyInk
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.text.displayUnits
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.classicBodyColor
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Full public component, same input/ink/font/times as actual pinned Classic, both raster paths. */
@OptIn(ExperimentalTestApi::class)
class ClassicColorParityTest {
    @Test fun actualBrowserBodyColorsMatchMediaSampling() {
        val fixtures=File("../verification/classic-fixtures/scenes.tsv").readLines().map {it.split('\t')}
        val documents=listOf("baseline","arabic","cjk").associateWith {scene->
            val syllables=if(scene=="baseline")listOf(KaraokeSyllable("Amber ",700,1300),KaraokeSyllable("echo",1300,1900))
                else fixtures.filter {it.size==6&&it[0]==scene}.map {KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())}
            normalize(SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(syllables,null,KaraokeAlignment.Unspecified,700,1900)))).lines.single()
        }
        var checked=0
        File("../verification/classic-fixtures/body-colors.tsv").readLines().filterNot {it.startsWith("#")||it.isBlank()}.forEach {row->
            val c=row.split('\t');val line=documents.getValue(c[0]);val index=c[3].toInt()
            val word=displayUnits(line)[index]
            val profile=renderProfile(line.startUs,line.endUs,line.fragments.last().endUs)
            val frame=sampleWord(c[2].toLong()*1000,line.startUs,word.startUs,word.endUs,profile,wordPose(line.startUs,index,true),false)
            val (base,active)=if(c[1]=="warm")Color(0xff808080) to Color(0xfff2b45d) else Color(0xff415d8a) to Color(0xff91e8e1)
            val expected=Color(c[4].toInt(),c[5].toInt(),c[6].toInt())
            assertEquals(expected,classicBodyColor(base,active,frame.mix),row)
            checked++
        }
        // Eight mounted checkpoints; at 2800ms the original line has fully exited.
        assertEquals(128,checked,"8 times x 2 palettes x (2 Latin + 2 Arabic + 4 CJK words)")
    }
    @Test fun latin() = capture("baseline",800,"FoliaParityLatin-Bold.ttf")
    @Test fun arabic() = capture("arabic",800,"NotoSansArabic-Bold.ttf")
    @Test fun cjk() = capture("cjk",600,"FoliaParityCJK-Bold.otf")
    @Test fun latinBodyOnly() = capture("baseline",800,"FoliaParityLatin-Bold.ttf",true)
    @Test fun arabicBodyOnly() = capture("arabic",800,"NotoSansArabic-Bold.ttf",true)
    @Test fun cjkBodyOnly() = capture("cjk",600,"FoliaParityCJK-Bold.otf",true)
    private fun capture(scene:String,width:Int,fontFile:String,bodyOnly:Boolean=false)=runDesktopComposeUiTest(width=width,height=600) {
        val syllables=if(scene=="baseline")listOf(KaraokeSyllable("Amber ",700,1300),KaraokeSyllable("echo",1300,1900))
            else File("../verification/classic-fixtures/scenes.tsv").readLines().map {it.split('\t')}
                .filter {it.size==6&&it[0]==scene}.map {KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())}
        val lyrics=SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(syllables,null,KaraokeAlignment.Unspecified,700,1900)))
        val family=FontFamily(Font(File("../verification/assets/$fontFile"),FontWeight.Bold))
        val style=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=(width*.06f).sp)
        val warm=FoliaColors(text=Color(0xff808080),active=Color(0xfff2b45d))
        val cool=FoliaColors(text=Color(0xff415d8a),active=Color(0xff91e8e1))
        var colors by mutableStateOf(warm)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false))
        var native by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalClassicNativeBodyInk provides native) {
                FoliaLyricsView(lyrics,{snapshot},Modifier.fillMaxSize().background(Color(0xff162435)),
                    typography=FoliaTypography(main=style,secondary=style),colors=colors,
                    options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false,
                        quality=if(bodyOnly)FoliaQuality.Minimal else FoliaQuality.Standard),
                    style=FoliaStyle.Classic(ClassicTuning(breathingFloatMultiplier=0f)))
            }
        }
        val times=listOf(750L,850L,1000L,1200L,1500L,1900L,2100L,2300L,2800L)
        for(path in listOf(false,true))for((palette,ink)in listOf("warm" to warm,"cool" to cool)) {
            val output=File("../build/verification/classic-colors-${if(bodyOnly)"body-" else ""}${if(path)"native" else "tinted"}").apply {mkdirs()}
            runOnIdle {native=path;colors=ink}
            val retained=mutableMapOf<Long,IntArray>()
            for(time in times) {
                runOnIdle {snapshot=FoliaPlaybackSnapshot(time,false,snapshot.discontinuity+1)};waitForIdle()
                val bitmap=captureToImage()
                retained[time]=IntArray(width*600).also {bitmap.readPixels(it)}
                ImageIO.write(bitmap.toAwtImage(),"png",output.resolve("$scene-$palette-${time}ms.png"))
            }
            for(time in times.reversed()) {
                runOnIdle {snapshot=FoliaPlaybackSnapshot(time,false,snapshot.discontinuity+1)};waitForIdle()
                val pixels=IntArray(width*600).also {captureToImage().readPixels(it)}
                assertContentEquals(retained.getValue(time),pixels,"Reverse seek $scene/$palette native=$path $time")
                mainClock.advanceTimeBy(1000);waitForIdle()
                val paused=IntArray(width*600).also {captureToImage().readPixels(it)}
                assertContentEquals(pixels,paused,"Paused media color $scene/$palette native=$path $time")
            }
        }
    }
}
