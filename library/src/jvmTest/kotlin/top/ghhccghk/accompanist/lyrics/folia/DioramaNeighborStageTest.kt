package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.*
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class DioramaNeighborStageTest {
    private fun rows(file:String,family:String="neighbors")=File("../verification/diorama-fixtures/$family-v1/$file.tsv").readLines()
        .filterNot { it.isBlank()||it.startsWith('#') }.map { it.split('\t') }

    @Test fun originalSingleSegmentWindows() {
        val controls=rows("windows")
        assertEquals(37,controls.size)
        for(row in controls) {
            val expected=row.getOrElse(2){""}.split(',').filter{it.isNotEmpty()&&it!="-"}.map(String::toInt)
            assertEquals(expected,dioramaMountedTextIndices(row[1].toInt(),row[0].toInt()).toList())
        }
        assertFailsWith<IllegalArgumentException>{dioramaMountedTextIndices(-1,3)}
        assertFailsWith<IllegalArgumentException>{dioramaMountedTextIndices(3,3)}
    }

    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    @Test fun fogNarrow()=capture(380,true)
    @Test fun fogWide()=capture(800,true)
    private fun capture(width:Int,withFog:Boolean=false)=runDesktopComposeUiTest(width=width,height=600) {
        val family=if(withFog)"fog" else "neighbors"
        val cases=rows("frames",family).filter{it[2].toInt()==width}
        val controls=rows("neighbors",family).groupBy{it[0]}
        // Actual Three conversions below are independently checked against each
        // original material row; constants are input palette, never geometry.
        fun linear(c:Int):Double {val s=c/255.0;return if(s<=.04045)s/12.92 else ((s+.055)/1.055).pow(2.4)}
        fun color(r:Int,g:Int,b:Int)=DioramaLinearColor(linear(r),linear(g),linear(b))
        val body=color(223,229,236);val accent=color(241,123,77);val secondary=color(117,141,170)
        var current by mutableIntStateOf(0);var before by mutableStateOf(false)
        var mounted by mutableStateOf(true);var reduced by mutableStateOf(false);var offset by mutableIntStateOf(0)
        var prepared:DioramaTextPlayback?=null;var builds=0;var coreObjects:List<KaraokeLine.MainKaraokeLine> = emptyList()
        setContent {
            val scene=cases[current][1];val resolver=LocalFontFamilyResolver.current
            val playback=if(mounted)remember(scene) {
                val tokens=when(scene){"arabic"->listOf("مرحبا","بالعالم");"cjk"->listOf("星光","回响","向远方");else->listOf("Synthetic","mirror","plane")}
                val lines=(0 until 8).map { i ->
                    val syllables=tokens.flatMapIndexed { j,text ->
                        val start=700+i*2400+(j*1600.0/tokens.size).roundToInt()
                        val end=700+i*2400+((j+1)*1600.0/tokens.size).roundToInt()
                        (if(j>0)listOf(KaraokeSyllable(" ",start,start))else emptyList())+KaraokeSyllable(text,start,end)
                    }
                    KaraokeLine.MainKaraokeLine(syllables,null,KaraokeAlignment.Unspecified,700+i*2400,2300+i*2400)
                }
                coreObjects=lines
                val font=when(scene){"arabic"->"FoliaParityBidi-Bold.ttf";"cjk"->"FoliaParityChatCJK-Bold.otf";else->"FoliaParityLatin-Bold.ttf"}
                val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/$font"),FontWeight.Bold)))
                builds++
                DioramaTextPlayback.prepare(SyncedLyrics(lines),style,TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver,
                    width.toDouble(),600.0,seedName="DioramaProbe",endTime=19.9)
            }else null
            SideEffect{prepared=playback}
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                if(playback!=null) {
                    val row=cases[current]
                    val view=DioramaPerspective(DioramaVec(row[6].toDouble(),row[7].toDouble(),row[8].toDouble()),
                        DioramaVec(row[9].toDouble(),row[10].toDouble(),row[11].toDouble()),width.toDouble(),600.0)
                    // Original fixture stores LYRIC time. Build the media input
                    // using the public inverse before calling the bridge.
                    val time=playback.lyricTime(row[5].toDouble()+offset/1000.0,offset)
                    if(before && !withFog) {
                        // Exact former production stage: only current active
                        // text; no source geometry, no neighbour placeholder.
                        val i=playback.camera.subjectIndex(time);val s=playback.camera.subjects[i]
                        playback.lines[i].text.draw(this,time,view,s.frame,s.placement,body,accent)
                    }else playback.drawStage(this,time,view,body,accent,secondary,reduced,
                        fog=if(withFog&&!before)DioramaFog(Color(0xff162435))else null,toneMapping=DioramaToneMapping.None,breath=1.0)
                }
            }
        }
        val out=File("../build/verification/diorama-$family-kotlin").apply{mkdirs()}
        out.resolve("complete-$width.txt").delete()
        fun pixels():IntArray {waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
        val captures=mutableMapOf<Int,IntArray>();var geometryPeak=0.0
        for(i in cases.indices) {
            runOnIdle{current=i;before=true};pixels()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("before-${cases[i][0]}"))
            runOnIdle{before=false};captures[i]=pixels()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("after-${cases[i][0]}"))
            val playback=prepared!!;val row=cases[i];val index=row[3].toInt()
            assertSame(coreObjects[index],playback.sourceAt(row[5].toDouble()).source)
            assertSame(playback.lines.first().neighbor,playback.lines.last().neighbor,"same text shares only immutable plain raster")
            val view=DioramaPerspective(DioramaVec(row[6].toDouble(),row[7].toDouble(),row[8].toDouble()),
                DioramaVec(row[9].toDouble(),row[10].toDouble(),row[11].toDouble()),width.toDouble(),600.0)
            val observed=controls.getValue(row[0])
            assertEquals(observed.map{it[1].toInt()},dioramaMountedTextIndices(index,8).filter{it!=index})
            for(c in observed) {
                val j=c[1].toInt();val line=playback.lines[j];val subject=playback.camera.subjects[j]
                assertEquals(c[2].toInt(),line.neighbor.advancePx,"own raster advance")
                assertEquals(c[3].toInt(),line.neighbor.fontPx)
                val frame=dioramaNeighborFrame(j,index,line.neighbor.advancePx,line.neighbor.fontPx,subject.frame,subject.placement,view,body,secondary)
                val actual=listOf(frame.fit,frame.opacity,frame.color.red,frame.color.green,frame.color.blue,
                    frame.center.x,frame.center.y,frame.center.z,frame.depth)
                for((k,value)in actual.withIndex()) {
                    val want=c[4+k].toDouble();geometryPeak=max(geometryPeak,abs(want-value))
                    assertEquals(want,value,1e-9,"original ${row[0]} neighbor$j field$k")
                }
            }
        }
        assertEquals(3,builds,"time, view and current line must not reprepare rasters")
        for(i in listOf(cases.lastIndex,0,4,7,1)) {
            runOnIdle{current=i};assertTrue(captures.getValue(i).contentEquals(pixels()),"arbitrary seek restore")
        }
        val frozen=pixels();mainClock.advanceTimeBy(1500);assertTrue(frozen.contentEquals(pixels()),"pause freezes stage")
        runOnIdle{offset=250};assertTrue(frozen.contentEquals(pixels()),"media offset applied once")
        runOnIdle{reduced=true};pixels();runOnIdle{reduced=false};assertTrue(frozen.contentEquals(pixels()),"setting restore")
        runOnIdle{mounted=false};pixels();assertNull(prepared)
        runOnIdle{mounted=true};assertTrue(frozen.contentEquals(pixels()),"unmount/remount")
        out.resolve("complete-$width.txt").writeText("frames=${cases.size} geometryPeak=$geometryPeak originalGate=1e-9\nActual core stage, fixed source view; fragmentFog=$withFog; not full original CameraRig, scene or device acceptance\n")
    }
}
