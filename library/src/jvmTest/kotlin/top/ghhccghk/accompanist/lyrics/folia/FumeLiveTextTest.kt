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
class FumeLiveTextTest {
    @Test fun narrow() = capture(380)
    @Test fun wide() = capture(800)
    private fun capture(width: Int) = runDesktopComposeUiTest(width = width, height = 600) {
        val source = File("../verification/fume-fixtures/live-geometry.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
            .filter { it[1].toInt() == width }
        val lyrics = listOf(
            FumeLyric("Amber  echo",.7,2.8,listOf(FumeWord("Amber",.7,1.6),FumeWord("echo",1.8,2.8))),
            FumeLyric("AB CD",.7,2.8,listOf(FumeWord("AB ",.7,1.6,listOf(FumeWord("A",.7,.8),FumeWord("B ",1.5,1.6))),FumeWord("CD",1.8,2.8))),
            FumeLyric("An invented paragraph whose text wraps naturally. ".repeat(5)+"\nA final echo",.7,2.8))
        val font = FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold))
        var scene by mutableStateOf(0); var glow by mutableStateOf(0.0); var time by mutableStateOf(.6)
        var prepared: List<FumeLiveText> = emptyList()
        setContent {
            val measurer = rememberTextMeasurer(cacheSize=128)
            val resolver = LocalFontFamilyResolver.current
            val drawings = remember(measurer,resolver) {
                val measure = FumeTextMeasure(measurer,TextStyle(fontFamily=font,fontWeight=FontWeight.Bold),Density(1f))
                source.map { f ->
                    val block = FumeArticleBlock(f[0].toInt(),lyrics[f[0].toInt()],f[2].toBoolean(),32.0,150.0,
                        f[3].toDouble(),f[4].toDouble(),f[5].toDouble(),f[6].toDouble(),f[7].toDouble(),f[8].toInt())
                    FumeLiveText.prepare(block,measure,resolver,Density(1f))
                }
            }
            prepared = drawings
            Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                drawings[scene].draw(this,time,null,FumeFrameTheme(FumeInk(223,229,236),FumeInk(241,123,77),glow=glow,
                    wordColors=listOf("amber echo" to FumeInk(48,202,255))),hold=.3)
            }
        }
        waitForIdle(); assertEquals(3,prepared.size)
        val out = File("../build/verification/fume-live-kotlin").apply{mkdirs()}
        val times=listOf(.6,.7,.74,.8,1.0,1.5,1.55,1.6,1.8,2.0,2.8,3.2,4.5,6.3)
        val hashes = mutableMapOf<String,IntArray>()
        fun pixels() = captureToImage().let { image -> IntArray(image.width*image.height).also { image.readPixels(it) } }
        for(s in lyrics.indices)for(g in listOf(0.0,1.0))for(t in times) {
            runOnIdle { scene=s; glow=g; time=t }; waitForIdle()
            val name="$s-$width-${g.toInt()}-${(t*1000).toInt()}"
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("$name.png"))
            hashes[name]=pixels()
        }
        // No live job, wall-clock animation or event history may influence this renderer.
        for ((name,expected) in hashes.entries.shuffled(Random(917))) {
            val f=name.split('-'); runOnIdle{scene=f[0].toInt();glow=f[2].toDouble();time=f[3].toDouble()/1000};waitForIdle()
            assertContentEquals(expected,pixels(),"seek/revisit $name")
        }
        val frozen=pixels();mainClock.advanceTimeBy(2000);waitForIdle()
        assertContentEquals(frozen,pixels(),"pause frozen")
    }
}
