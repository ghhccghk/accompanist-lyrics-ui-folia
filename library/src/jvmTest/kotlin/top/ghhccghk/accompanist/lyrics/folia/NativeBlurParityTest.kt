package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.IntSize
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class NativeBlurParityTest {
    @Test fun cssSigmaControlsActualCoverageSpread()=runDesktopComposeUiTest(width=192,height=192) {
        var sigma by mutableStateOf(.25f)
        var software by mutableStateOf(false)
        setContent {
            CompositionLocalProvider(LocalClassicSoftwareLayers provides software) {
                val layer=rememberClassicLayer()
                Canvas(Modifier.fillMaxSize()) {
                    layer.blurSigma=sigma
                    layer.record(this,IntSize(192,192)) {drawRect(Color.White,Offset(80f,80f),Size(32f,32f))}
                    drawClassicLayer(layer)
                }
            }
        }
        val output=File("../build/verification/css-blur").apply{mkdirs()}
        val report=StringBuilder("software\tsigma\tmeasuredStdDev\texpectedStdDev\n")
        for(soft in listOf(false,true)) for(value in listOf(.25f,.5f,.6f,1f,10f,20f)) {
            runOnIdle{software=soft;sigma=value};waitForIdle()
            val image=captureToImage().toAwtImage()
            ImageIO.write(image,"png",output.resolve("$soft-$value.png"))
            val pixels=image.getRGB(0,0,192,192,null,0,192)
            if(value>=10) {
                val reference=ImageIO.read(File("../verification/classic-fixtures/css-blur/${value.toInt()}.png"))
                    .getRGB(0,0,192,192,null,0,192)
                assertContentEquals(reference.map{it ushr 24},pixels.map{it ushr 24},"Original CSS blur coverage")
            }
            var mass=0.0;var moment=0.0
            for(i in pixels.indices) {val a=(pixels[i] ushr 24).toDouble();mass+=a;moment+=a*((i%192)-95.5).pow(2)}
            assertTrue(mass>0)
            val measured=sqrt(moment/mass)
            // Compare actual 8-bit browser coverage, not the infinite floating
            // Gaussian moment: quantized-away tails materially reduce variance.
            val expected=File("../verification/classic-fixtures/css-blur.tsv").readLines()
                .filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}
                .single{it[0].toFloat()==value}[1].toDouble()
            report.append("$soft\t$value\t$measured\t$expected\n")
            assertEquals(expected,measured,.5,"Actual blur spread must follow CSS sigma, software=$soft sigma=$value")
        }
        output.resolve("spread.tsv").writeText(report.toString())
    }
}
