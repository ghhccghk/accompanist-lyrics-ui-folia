package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class ClassicLayerDensityTest {
    @Test fun recordedDensityReadsAreStableAndPreserveCallerCoordinates()=runDesktopComposeUiTest(width=400,height=400) {
        var software by mutableStateOf(false)
        var layered by mutableStateOf(true)
        var d by mutableStateOf(1f)
        var observed:Pair<Float,Float>?=null
        setContent {
            CompositionLocalProvider(LocalDensity provides Density(d,1.2f),LocalClassicSoftwareLayers provides software) {
                val layer=rememberClassicLayer()
                Canvas(Modifier.size(160.dp)) {
                    val ink:androidx.compose.ui.graphics.drawscope.DrawScope.()->Unit={
                        observed=density to fontScale
                        drawRect(Color.Red,Offset(6*density,8*density),Size(20*density,14*density))
                    }
                    translate(31f,43f) {
                        if(layered){layer.record(this,IntSize(160,160),ink);drawClassicLayer(layer)}else ink()
                    }
                }
            }
        }
        fun pixels():List<Int> {val image=captureToImage();return IntArray(image.width*image.height).also{image.readPixels(it)}.toList()}
        for(density in listOf(1f,1.5f,2f))for(fallback in listOf(false,true)) {
            runOnIdle{d=density;software=fallback;layered=true};waitForIdle()
            assertEquals(density to 1.2f,observed,"recorded density/software=$fallback")
            val actual=pixels()
            runOnIdle{layered=false};waitForIdle()
            assertEquals(actual,pixels(),"layer/caller transform density=$density software=$fallback")
        }
    }
}
