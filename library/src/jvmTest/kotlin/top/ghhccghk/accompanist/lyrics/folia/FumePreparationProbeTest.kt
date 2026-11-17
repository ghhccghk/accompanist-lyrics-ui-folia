package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*
import java.io.File
import kotlin.test.*
import kotlin.time.TimeSource

/** Diagnostic, not a performance acceptance threshold. Same fixture/viewport
 * before and after; no real song text or audio is sampled. */
@OptIn(ExperimentalTestApi::class)
class FumePreparationProbeTest {
    @Test fun wholeSongPreparationAndRandomMediaQueries()=runDesktopComposeUiTest(width=800,height=600){
        val lines=(0 until 120).map{i->FumeLyric("Synthetic stanza $i across the page",i*3.0+.7,i*3.0+2.8)}
        val viewport=FumeViewport(800.0,600.0);val font=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        var prepared:Pair<FumeArticleDrawing,FumeCameraTrack>?=null
        var metrics=""
        setContent {
            val measurer=rememberTextMeasurer(cacheSize=256);val resolver=LocalFontFamilyResolver.current;val density=LocalDensity.current
            val result=remember {
                val started=TimeSource.Monotonic.markNow()
                val measure=FumeTextMeasure(measurer,TextStyle(fontFamily=font,fontWeight=FontWeight.Bold),density)
                val article=checkNotNull(buildFumeArticle(lines,viewport,"FumePreparationProbe",countLines=measure::countLines))
                val layoutMs=started.elapsedNow().inWholeMilliseconds
                val drawing=FumeArticleDrawing.prepare(article,lines,measure,resolver,density)
                val drawingMs=started.elapsedNow().inWholeMilliseconds-layoutMs
                val epochs=(listOf(0.0)+lines.flatMap{listOf(it.start,it.end)}).distinct().sorted().map{t->FumeFocusEpoch(t,lines.indexOfLast{it.start<=t&&t<it.end})}
                val cameraStart=TimeSource.Monotonic.markNow()
                val track=FumeCameraTrack(article,drawing.focusGeometry,epochs,FumeCameraTrackConfig(viewport,lines.last().let{it.start+(it.renderEnd-it.start)*.5}),362.0)
                metrics="lines=120 viewport=800x600 font=FoliaParityLatin-Bold.ttf density=${density.density} fontScale=${density.fontScale} layoutMs=$layoutMs drawingMs=$drawingMs cameraMs=${cameraStart.elapsedNow().inWholeMilliseconds} nodes=${track.preparedNodeCount} blocks=${article.blocks.size}"
                drawing to track
            }
            prepared=result
            Canvas(Modifier.fillMaxSize()){result.first.draw(this,viewport,150.0,result.second.sample(150.0),FumeFrameTheme(FumeInk(223,229,236),FumeInk(241,123,77),glow=0.0))}
        }
        waitForIdle();val track=assertNotNull(prepared).second
        val probes=listOf(0.0,300.13,1.0,150.0,359.0,80.71,250.99)
        val measured=TimeSource.Monotonic.markNow();val snapshots=probes.associateWith(track::sample)
        for(t in probes.reversed())assertEquals(snapshots[t],track.sample(t))
        val out=File("../build/verification/fume-preparation").apply{mkdirs()}
        out.resolve("probe.txt").writeText("$metrics\nqueries=${probes.size*2} queryMs=${measured.elapsedNow().inWholeMilliseconds}\nJVM=${System.getProperty("java.version")} os=${System.getProperty("os.name")} arch=${System.getProperty("os.arch")}\n")
        println(metrics)
    }
}
