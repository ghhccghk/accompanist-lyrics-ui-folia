// SPDX-License-Identifier: AGPL-3.0-only
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
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.util.Base64
import javax.imageio.ImageIO
import kotlin.test.*

/** Same original isolated scene/media time; visibility ablations only, not a
 * new timing/source-geometry oracle or source texture injection. */
@OptIn(ExperimentalTestApi::class)
class DioramaCjkLayerIsolationTest {
    @Test fun fixedSourceCjkLayerContributions()=runDesktopComposeUiTest(width=800,height=600) {
        val file="cjk-800-active-1100.png"
        val r=File("../verification/diorama-fixtures/active-layers.tsv").readLines()
            .filterNot{it.startsWith('#')}.map{it.split('\t')}.single{it[0]==file}
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val renderer=DioramaActiveBaseRenderer()
        var layer by mutableStateOf("all");var sampling by mutableStateOf(false)
        val frame=buildDioramaPath(12,dioramaHashSeed("DioramaProbe"))[3]
        val placement=dioramaTextPlacement(3,dioramaHashSeed("DioramaProbe"))
        var builds=0
        try {
            setContent {
                val resolver=LocalFontFamilyResolver.current
                val stage=remember {
                    builds++
                    val words=r[5].split(';').map{it.split(',').let{DioramaTimedText(decode(it[0]),it[1].toDouble(),it[2].toDouble())}}
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityChatCJK-Bold.otf"),FontWeight.Bold)))
                    DioramaActiveText.prepare(DioramaLyric(decode(r[4]),.7,words.last().end,words),style,
                        TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr),resolver)
                }
                SideEffect{renderer.prepare(stage)}
                Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                    fun color(i:Int)=r[i].split(',').map(String::toDouble).let{DioramaLinearColor(it[0],it[1],it[2])}
                    fun vec(i:Int)=DioramaVec(r[i].toDouble(),r[i+1].toDouble(),r[i+2].toDouble())
                    val states=stage.slices.map { s ->
                        val state=dioramaUnitFrame(r[3].toDouble(),s.unit.start,s.unit.end,
                            dioramaMediaEnvelope(r[3].toDouble(),s.unit.start,s.unit.end),color(7),color(8),DioramaUnitTuning(soulActive=true))
                        state.copy(baseOpacity=if(layer=="all"||layer=="base")state.baseOpacity else 0.0,
                            glowVisible=state.glowVisible&&(layer=="all"||layer=="glow"),
                            soulVisible=state.soulVisible&&(layer=="all"||layer=="soul"))
                    }
                    stage.drawFrames(this,DioramaPerspective(vec(9),vec(12),800.0,600.0),frame,placement,states,
                        baseRenderer=if(sampling)renderer else null)
                }
            }
            fun pixels():IntArray{waitForIdle();val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}}
            fun existing(folder:String):IntArray {
                val image=ImageIO.read(File("../build/verification/$folder/$file"))
                return IntArray(image.width*image.height).also{image.getRGB(0,0,image.width,image.height,it,0,image.width)}
            }
            val out=File("../build/verification/diorama-cjk-layers-kotlin").apply{mkdirs()}
            for(kind in listOf("all","base","glow","soul")) {
                runOnIdle{layer=kind;sampling=false};val ordinary=pixels()
                if(kind=="all")assertContentEquals(existing("diorama-active-layers-kotlin"),ordinary,"Diagnostic replay equals full ordinary stage")
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("ordinary-$kind.png"))
                runOnIdle{sampling=true};val sampled=pixels()
                if(kind=="all")assertContentEquals(existing("diorama-active-layers-sampler-kotlin"),sampled,"Diagnostic replay equals sampled full stage")
                if(kind in listOf("glow","soul"))assertContentEquals(ordinary,sampled,"No sampler in additive layers")
                ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("sampled-$kind.png"))
            }
            val frozen=pixels();val count=renderer.preparations;mainClock.advanceTimeBy(1000)
            assertContentEquals(frozen,pixels());assertEquals(count,renderer.preparations);assertEquals(1,builds)
        }finally{renderer.close()}
    }
}
