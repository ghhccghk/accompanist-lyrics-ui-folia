package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.*
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import java.io.File
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PublicPartitaTest {
    @Test fun publicMatchesCoreAndRetainsTransformedClickIdentity() = runDesktopComposeUiTest(width=800,height=600) {
        val input=File("../verification/classic-fixtures/scenes.tsv").readLines().map{it.split('\t')}.filter{it[0]=="wrap"}
        val source=KaraokeLine.MainKaraokeLine(input.map{KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},null,KaraokeAlignment.Unspecified,700,2800)
        val lyrics=SyncedLyrics(listOf(source));val line=normalize(lyrics).lines.single()
        val font=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val style=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold,fontSize=44.sp)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(1500,false))
        var public by mutableStateOf(false)
        var width by mutableStateOf(800)
        var tuning by mutableStateOf(PartitaTuning())
        var options by mutableStateOf(FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false))
        var selected:ISyncedLine?=null;var pressed:ISyncedLine?=null;var geo:PartitaGeometry?=null
        setContent {
            Box(Modifier.fillMaxSize().background(Color(0xFF162435)),contentAlignment=Alignment.Center) {
                if(public) FoliaLyricsView(lyrics,{snapshot},Modifier.size(width.dp,600.dp),
                    style=FoliaStyle.Partita(tuning),typography=FoliaTypography(main=style),options=options,
                    onLineClicked={selected=it},onLinePressed={pressed=it})
                else PartitaLine(line,{snapshot},style,FoliaColors(),options,modifier=Modifier.size(width.dp,600.dp),onLayout={geo=it})
            }
        }
        fun pixels():IntArray {waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
        val core=pixels()
        runOnIdle{public=true}
        assertContentEquals(core,pixels(),"Public entry and isolated Partita renderer")
        mainClock.advanceTimeBy(2000)
        assertContentEquals(core,pixels(),"Paused scene has no independent animation clock")
        runOnIdle{snapshot=FoliaPlaybackSnapshot(850,false,1)};pixels()
        runOnIdle{snapshot=FoliaPlaybackSnapshot(1500,false,2)}
        assertContentEquals(core,pixels(),"Seek reconstructs the same scene")
        runOnIdle{options=options.copy(offsetMillis=100);snapshot=FoliaPlaybackSnapshot(1600,false,3)}
        assertContentEquals(core,pixels(),"Shared media offset applies once")
        runOnIdle{width=380};pixels()
        runOnIdle{width=800}
        assertContentEquals(core,pixels(),"Resize restores geometry without history")
        val layout=buildPartitaLayout(line,600.0,FoliaAnimationIntensity.Normal)
        val row=layout.rows.first();val g=geo!!.rows.first();val w=g.words.first()
        val chunk=samplePartitaChunk(1_500_000,700_000,row,renderProfile(700_000,2_800_000),false)
        val word=sampleWord(1_500_000,700_000,row.words.first().startUs,row.words.first().endUs,
            renderProfile(700_000,2_800_000),partitaWordPoses(row,FoliaAnimationIntensity.Normal).first(),false)
        fun rotate(p:Offset,degrees:Float):Offset {
            val a=degrees*PI/180;return Offset((p.x*cos(a)-p.y*sin(a)).toFloat(),(p.x*sin(a)+p.y*cos(a)).toFloat())
        }
        val contentWidth=geo!!.rows.maxOf{it.box.left*2+it.box.width}
        val origin=Offset((800-contentWidth)/2,(600-geo!!.height)/2)
        val inRow=Offset(w.left+w.width/2+word.x,w.top+w.height/2+word.y)
        val rowCenter=Offset(g.box.width/2,g.box.height/2)
        val inLine=origin+Offset(g.box.left+g.box.width/2+chunk.x,g.box.top+g.box.height/2+chunk.y)+rotate((inRow-rowCenter)*chunk.scale,chunk.rotation)
        val frame=sampleLine(1_500_000,700_000,null,renderProfile(700_000,2_800_000),false,1f,1f)
        val point=(inLine-Offset(400f,300f))*frame.scale+Offset(400f,300f+frame.breathingY)
        onRoot().performTouchInput{click(point)}
        runOnIdle{assertSame(source,selected);selected=null}
        onRoot().performTouchInput{click(Offset(4f,4f))}
        runOnIdle{assertNull(selected)}
        onRoot().performTouchInput{longClick(point)}
        runOnIdle{assertSame(source,pressed)}
        runOnIdle{tuning=tuning.copy(avoidOverlap=true);options=options.copy(reducedMotion=true)}
        val still=pixels();mainClock.advanceTimeBy(2000)
        assertContentEquals(still,pixels(),"Reduced motion and fit remain media driven")
    }
}
