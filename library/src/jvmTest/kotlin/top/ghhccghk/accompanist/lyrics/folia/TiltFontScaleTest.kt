package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class TiltFontScaleTest {
    @Test fun systemAndHostScaleUseOriginalEffectiveRowGeometryAndPublicStage() =
        runDesktopComposeUiTest(width=800,height=600) {
            val input=File("../verification/classic-fixtures/scenes.tsv").readLines().map{it.split('\t')}.filter{it[0]=="wrap"}
            val source=KaraokeLine.MainKaraokeLine(input.map{KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},null,
                KaraokeAlignment.Unspecified,700,2800)
            val lyrics=SyncedLyrics(listOf(source));val line=normalize(lyrics).lines.single()
            val font=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
            val textStyle=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold)
            val options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false)
            // Controlled original initial Canvas font matches this fixed
            // Windows host's SansSerif. The natural-default original matrix,
            // its stricter failures and pixels stay separate and unaccepted.
            val reference=File("../verification/tilt-fixtures/font-scale-native-default-v1/rows.tsv").readLines()
                .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.groupBy{it[0]}
            var width by mutableIntStateOf(800);var scale by mutableFloatStateOf(1f)
            var tilt by mutableIntStateOf(0);var public by mutableStateOf(false)
            var systemScale by mutableStateOf(true);var time by mutableLongStateOf(2400)
            var offset by mutableIntStateOf(0);var mounted by mutableStateOf(true)
            var boxes=emptyList<TiltRowBox>();var selected:ISyncedLine?=null
            setContent {
                CompositionLocalProvider(LocalDensity provides Density(1f,if(systemScale)scale else 1f)) {
                    Box(Modifier.fillMaxSize().background(Color(0xff162435))) {
                        if(mounted) {
                            val hostScale=if(systemScale)1f else scale
                            if(public)FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(time,false)},Modifier.size(width.dp,600.dp),
                                style=FoliaStyle.Tilt(TiltTuning(.75f,tilt.toFloat())),
                                typography=FoliaTypography(main=textStyle,scale=hostScale),options=options.copy(offsetMillis=offset),
                                onLineClicked={selected=it})
                            else TiltLine(line,{FoliaPlaybackSnapshot(time,false)},textStyle,FoliaColors(),options.copy(offsetMillis=offset),
                                config=TiltConfig(.75,tilt.toDouble()),fontScale=hostScale,modifier=Modifier.size(width.dp,600.dp),
                                onLayout={boxes=it})
                        }
                    }
                }
            }
            fun pixels():IntArray {waitForIdle();val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
            val out=File("../build/verification/tilt-font-scale-kotlin").apply{mkdirs()}
            val report=mutableListOf("# file index text64 font width height top")
            val errors=mutableListOf<String>()
            for((file,expected)in reference) {
                val r=expected.first()
                runOnIdle{width=r[1].toInt();scale=r[2].toFloat();tilt=r[3].toInt();systemScale=true;public=false}
                val internal=pixels()
                val actual=boxes.toList()
                if(actual.map{it.text}!=expected.map{String(Base64.getDecoder().decode(it[5]),Charsets.UTF_8)})errors+="$file source/native row text differs"
                for((i,box)in actual.withIndex()) {
                    report+=listOf(file,i,Base64.getEncoder().encodeToString(box.text.toByteArray()),box.fontSize,box.box.width,box.box.height,box.box.top).joinToString("\t")
                    val e=expected.getOrNull(i)?:continue
                    fun check(name:String,a:Double,b:Double,gate:Double) {
                        if(abs(a-b)>gate)errors+="$file/$i $name native=$a source=$b gate=$gate"
                    }
                    check("font",box.fontSize,e[6].toDouble(),.011)
                    check("width",box.box.width.toDouble(),e[7].toDouble(),.51)
                    check("height",box.box.height.toDouble(),e[8].toDouble(),.03125)
                }
                runOnIdle{public=true};assertContentEquals(internal,pixels(),"public actual core $file")
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(file))
                runOnIdle{systemScale=false;public=false}
                val host=pixels()
                // Geometry identity uses the same effective scale through both
                // host and Android-style system scaling. Full pixels are saved
                // for source comparison; do not fit a pixel tolerance here.
                if(actual!=boxes)errors+="$file host/system row geometry differs"
                runOnIdle{public=true};assertContentEquals(host,pixels(),"host-scaled public core $file")
            }
            out.resolve("rows.tsv").writeText(report.joinToString("\n")+"\n")
            out.resolve("failures.txt").writeText(errors.joinToString("\n")+"\n")
            // Last public scene: timing, offset, resize and remount remain media
            // driven. Clicking a displayed middle row returns its original core.
            val frozen=pixels();mainClock.advanceTimeBy(1000);assertContentEquals(frozen,pixels(),"pause")
            runOnIdle{time=1000};pixels();runOnIdle{time=2400};assertContentEquals(frozen,pixels(),"seek")
            runOnIdle{offset=100;time=2500};assertContentEquals(frozen,pixels(),"offset once")
            runOnIdle{mounted=false};pixels();runOnIdle{mounted=true};assertContentEquals(frozen,pixels(),"remount")
            val total=boxes.last().box.bottom
            val target=boxes.minBy {abs(it.box.center.y-total/2)}.box
            onRoot().performTouchInput{click(Offset(width/2f,(600-total)/2+target.center.y))}
            runOnIdle{assertSame(source,selected,"scaled drawn-row hit maps back to original core")}
            assertTrue(errors.isEmpty(),errors.take(35).joinToString("\n"))
        }
}
