package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic.*
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class TiltSubtitleStageTest {
    @Test fun publicCoreStageWithActualTranslationAtAccessibleSizes()=runDesktopComposeUiTest(width=800,height=600) {
        val input=File("../verification/classic-fixtures/scenes.tsv").readLines().map{it.split('\t')}.filter{it[0]=="wrap"}
        val source=KaraokeLine.MainKaraokeLine(input.map{KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},
            "Copper translates the quiet light",KaraokeAlignment.Unspecified,700,2800)
        val lyrics=SyncedLyrics(listOf(source));val lines=normalize(lyrics).lines;val line=lines.single()
        assertEquals("Copper translates the quiet light",line.translation)
        val timeline=VisualTimeline(lines,450_000);val history=SubtitleHistory(lines)
        val fonts=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val font=TextStyle(fontFamily=fonts,fontWeight=FontWeight.Bold)
        val options=FoliaOptions(showTranslation=true,showPhonetic=false,showNextLine=false,showSubtitleBackground=false)
        val colors=FoliaColors(subtitleBackground=Color(0xff162435))
        var width by mutableIntStateOf(800);var scale by mutableFloatStateOf(1f)
        var time by mutableLongStateOf(2400);var public by mutableStateOf(false)
        var mounted by mutableStateOf(true);var offset by mutableIntStateOf(0)
        var mainRows=emptyList<TiltRowBox>();var subtitleRows=emptyList<SubtitlePaint>();var subtitleHeight=0f
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f,scale)) {
                Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
                    val typography=FoliaTypography(main=font,subtitle=font.copy(fontSize=18.sp))
                    val opts=options.copy(offsetMillis=offset)
                    if(mounted)Box(Modifier.size(width.dp,600.dp)) {
                        if(public)FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(time,false)},Modifier.fillMaxSize(),
                            typography=typography,colors=colors,options=opts,style=FoliaStyle.Tilt(TiltTuning(.75f,0f)))
                        else {
                            val us=(time-offset)*1000
                            val selection=timeline.at(us)
                            (selection.exiting.map {it}+listOfNotNull(selection.primary)).forEach { epoch ->key(epoch) {
                                TiltLine(line,{FoliaPlaybackSnapshot(time,false)},font,colors,opts,config=TiltConfig(.75,0.0),
                                    enterUs=epoch.enterUs,leaveUs=epoch.leaveUs,onLayout={mainRows=it})
                            }}
                            val content=resolveSubtitleContent(lines,selection.primary?.line,history.recent(us,selection.primary?.line),
                                listOf(SubtitleSource.Translation),visible=true,allowFallback=false)
                            ClassicSubtitles(content,lines,{FoliaPlaybackSnapshot(time,false)},typography,colors,opts,width.dp,
                                selection.primary?.enterUs?:content.line?.let{lines[it].startUs},
                                Modifier.align(Alignment.BottomCenter).padding(bottom=112.dp),
                                onLayout={height,rows->subtitleHeight=height;subtitleRows=rows})
                        }
                    }
                }
            }
        }
        fun pixels():IntArray {waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        val reference=File("../verification/tilt-fixtures/subtitles-v2/rows.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.groupBy{it[0]}
        val out=File("../build/verification/tilt-subtitles-kotlin").apply{mkdirs()}
        val report=mutableListOf("# file role index text64 font width height top")
        val errors=mutableListOf<String>()
        for((file,expected)in reference) {
            val parts=file.removeSuffix(".png").split('-')
            runOnIdle{width=parts[0].toInt();scale=parts[1].toFloat();time=parts[2].toLong();public=false}
            val internal=pixels()
            val mainHeight=mainRows.last().box.bottom
            val actual=mainRows.mapIndexed {i,row->listOf(file,"main",i,
                Base64.getEncoder().encodeToString(row.text.toByteArray()),row.fontSize,row.box.width,row.box.height,(600-mainHeight)/2+row.box.top)}+
                subtitleRows.mapIndexed {i,row->listOf(file,"subtitle",i,
                    Base64.getEncoder().encodeToString(row.text.toByteArray()),row.layout.layoutInput.style.fontSize.value*scale,
                    row.boxWidth,row.height,600-112-subtitleHeight+row.top)}
            report+=actual.map{it.joinToString("\t")}
            for(e in expected) {
                val a=actual.single{it[1]==e[1]&&it[2].toString()==e[2]}
                assertEquals(e[3],a[3],"source text $file/${e[1]}")
                // At 2400ms the subtitle has settled. Main top removes ONLY its
                // observed source row transform; entry/exit remains pixel evidence.
                if(time==2400L)for((column,gate)in listOf(4 to .011,5 to .51,6 to .03125,7 to .1)) {
                    val value=a[column].toString().toDouble();val sourceValue=e[column].toDouble()
                    if(abs(value-sourceValue)>gate)errors+="$file/${e[1]}/${e[2]} column=$column native=$value source=$sourceValue gate=$gate"
                }
            }
            runOnIdle{public=true};assertContentEquals(internal,pixels(),"actual public core/subtitles $file")
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(file))
        }
        out.resolve("rows.tsv").writeText(report.joinToString("\n")+"\n")
        out.resolve("failures.txt").writeText(errors.joinToString("\n")+"\n")
        val frozen=pixels();mainClock.advanceTimeBy(1500);assertContentEquals(frozen,pixels(),"paused subtitle/media")
        val baselineTime=time
        runOnIdle{time=1000};pixels();runOnIdle{time=baselineTime};assertContentEquals(frozen,pixels(),"reverse seek")
        runOnIdle{offset=125;time=baselineTime+125};assertContentEquals(frozen,pixels(),"offset once")
        runOnIdle{mounted=false};pixels();runOnIdle{mounted=true};assertContentEquals(frozen,pixels(),"remount")
        assertTrue(errors.isEmpty(),errors.take(30).joinToString("\n"))
    }
}
