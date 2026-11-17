package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.random.Random
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class FumeSceneTest {
    @Test fun narrowSmooth() = capture(380, false, 0.0)
    @Test fun narrowSmoothGlow() = capture(380, false, 1.0)
    @Test fun narrowStepped() = capture(380, true, 0.0)
    @Test fun narrowSteppedGlow() = capture(380, true, 1.0)
    @Test fun wideSmooth() = capture(800, false, 0.0)
    @Test fun wideSmoothGlow() = capture(800, false, 1.0)
    @Test fun wideStepped() = capture(800, true, 0.0)
    @Test fun wideSteppedGlow() = capture(800, true, 1.0)
    @Test fun fractionalSmooth() = capture(801, false, 0.0)
    @Test fun fractionalSmoothGlow() = capture(801, false, 1.0)
    @Test fun fractionalStepped() = capture(801, true, 0.0)
    @Test fun fractionalSteppedGlow() = capture(801, true, 1.0)

    private fun capture(width: Int, stepped: Boolean, glow: Double) = runDesktopComposeUiTest(width=width,height=600) {
        val lyrics = listOf(
            FumeLyric("Amber echoes beyond", .7, 2.8, listOf(FumeWord("Amber ",.7,1.2), FumeWord("echoes ",1.2,2.0), FumeWord("beyond",2.0,2.8))),
            FumeLyric("A second phrase",3.0,5.0), FumeLyric("Quiet afterglow",5.4,7.6),
            FumeLyric("An invented paragraph whose text wraps naturally. ".repeat(5)+"\nA final echo",8.0,11.0))
        val font = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val viewport = FumeViewport(width.toDouble(),600.0)
        val times = listOf(0.0,.7,.75,1.0,1.3,2.6,3.0,3.15,4.2,5.4,5.6,6.3,7.6,8.3,9.5,10.0,10.7,11.5)
        fun active(t:Double) = lyrics.indexOfLast { it.start<=t && t<it.end }
        val epochs = (listOf(0.0)+lyrics.flatMap{listOf(it.start,it.end)}).distinct().sorted().map{FumeFocusEpoch(it,active(it))}
        val overviewStart = lyrics.last().let{it.start+(it.renderEnd-it.start)*.5}
        var time by mutableStateOf(0.0)
        var sourceCamera by mutableStateOf(false)
        val references=File("../verification/fume-fixtures/scene-camera.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
            .filter{it[0].toInt()==width&&it[1].toDouble()==glow&&it[2].toBoolean()==stepped}
            .associate{f->f[3].toDouble() to FumeCameraFrame(FumeCameraState(FumeCameraPose(f[4].toDouble(),f[5].toDouble(),
                scale=f[6].toDouble())),f[3].toDouble()>=overviewStart,f[7].toDouble())}
        var ready: FumeCameraTrack? = null
        setContent {
            val measurer=rememberTextMeasurer(cacheSize=256)
            val resolver=LocalFontFamilyResolver.current
            val prepared=remember(measurer,resolver) {
                val measure=FumeTextMeasure(measurer,TextStyle(fontFamily=font,fontWeight=FontWeight.Bold),Density(1f))
                val article=checkNotNull(buildFumeArticle(lyrics,viewport,"FumeSceneFixture",countLines=measure::countLines))
                val drawing=FumeArticleDrawing.prepare(article,lyrics,measure,resolver,Density(1f))
                val track=FumeCameraTrack(article,drawing.focusGeometry,epochs,
                    FumeCameraTrackConfig(viewport,overviewStart,stepped=stepped,floatOrigin=9.25),12.0)
                drawing to track
            }
            ready=prepared.second
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                prepared.first.draw(this,viewport,time,if(sourceCamera)references.getValue(time)else prepared.second.sample(time),
                    FumeFrameTheme(FumeInk(223,229,236),FumeInk(241,123,77),glow=glow,
                        wordColors=listOf("amber echo" to FumeInk(48,202,255))),hold=.3)
            }
        }
        waitForIdle(); val track=assertNotNull(ready)
        for(sharedCamera in listOf(false,true)) {
        runOnIdle{sourceCamera=sharedCamera};waitForIdle()
        val out=File("../build/verification/fume-scene-kotlin"+if(sharedCamera)"-source-camera"else "").apply{mkdirs()}
        val report=mutableListOf("# time\tx\ty\tscale\trestore\tnodes")
        val pixels=mutableMapOf<Double,IntArray>()
        fun readPixels()=captureToImage().let{image->IntArray(image.width*image.height).also{image.readPixels(it)}}
        for(t in times) {
            runOnIdle{time=t};waitForIdle()
            val frame=if(sharedCamera)references.getValue(t)else track.sample(t);val p=frame.state.pose
            report+=listOf(t,p.x,p.y,p.scale,frame.restore,track.preparedNodeCount).joinToString("\t")
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$width-${glow.toInt()}-$stepped-${(t*1000).toInt()}.png"))
            pixels[t]=readPixels()
        }
        out.resolve("$width-${glow.toInt()}-$stepped.tsv").writeText(report.joinToString("\n")+"\n")
        for((t,expected)in pixels.entries.shuffled(Random(913))) {
            runOnIdle{time=t};waitForIdle();assertContentEquals(expected,readPixels(),"random seek scene=$width/$stepped/$glow/$t")
        }
        val frozen=readPixels();mainClock.advanceTimeBy(2000);waitForIdle();assertContentEquals(frozen,readPixels(),"pause whole scene")
        }
    }
}
