package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Density
import kotlin.math.floor
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.rasterizeParagraph
import com.mocharealm.accompanist.lyrics.folia.internal.text.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.*

/** Raster diagnostic, deliberately separate from full public renderer acceptance. */
@OptIn(ExperimentalTestApi::class)
class BodySamplingParityTest {
    @Test fun density1() = checkSampling(1)
    @Test fun density2() = checkSampling(2)
    private fun checkSampling(dpr: Int) = runDesktopComposeUiTest(width = 600 * dpr, height = 400 * dpr) {
        val reference = File("../verification/classic-fixtures/body-layout.tsv").readLines()
            .filterNot { it.startsWith('#') }.map { it.split('\t') }.single { it[0] == "baseline" }
        val width = reference[4].toFloat(); val height = reference[5].toFloat()
        val baseline = reference[6].toFloat()
        var phase by mutableFloatStateOf(0f)
        var transformed by mutableStateOf(false)
        var cached by mutableStateOf(false)
        var motion by mutableStateOf(false)
        var snapLayout by mutableStateOf(false)
        setContent {
          CompositionLocalProvider(LocalDensity provides Density(dpr.toFloat(), 1f)) {
            val density = LocalDensity.current
            val style = classicBodyRasterStyle(TextStyle(fontFamily = FontFamily(Font(
                File("../verification/assets/FoliaParityLatin-Bold.ttf"), FontWeight.Bold)),
                fontWeight = FontWeight.Bold, fontSize = 48.sp))
            val layout = rememberTextMeasurer().measure("Amber", style, softWrap = false)
            val raster = remember(layout, density) { rasterizeParagraph(layout, density) }
            assertEquals(width * dpr, layout.getPathForRange(0, 5).getBounds().width, .016f * dpr)
            val paintY = baseline * dpr - paintedLineBaseline(layout, 0)
            Canvas(Modifier.fillMaxSize()) {
                withTransform({
                    val layoutPhase = if (motion) 0f else phase
                    val layout = if (snapLayout) floor(128f + layoutPhase + .5f) else 128f + layoutPhase
                    val origin = (layout + if (motion) phase else 0f) * dpr
                    translate(origin, origin)
                    if (transformed) {
                        rotate(-4f, Offset(width * dpr / 2, height * dpr / 2))
                        scale(1.6f, 1.6f, Offset(width * dpr / 2, height * dpr / 2))
                    }
                }) {
                    if (cached) drawImage(raster.image, raster.origin + Offset(0f, paintY), colorFilter = ColorFilter.tint(Color.White))
                    else drawText(layout, Color.White, Offset(0f, paintY), shadow = Shadow.None)
                }
            }
          }
        }
        val output = File("../build/verification/sampling-kotlin").apply { mkdirs() }
        for (isMotion in listOf(false, true)) for (p in listOf(0f, .25f, .5f, .75f))
            for (transform in listOf(false, true)) for (cache in listOf(false, true)) for (snap in listOf(false, true)) {
            runOnIdle { phase = p; transformed = transform; cached = cache; motion = isMotion; snapLayout = snap }; waitForIdle()
            val capture = captureToImage()
            val pixels = IntArray(600 * 400 * dpr * dpr).also { capture.readPixels(it) }
            mainClock.advanceTimeBy(1000); waitForIdle()
            assertContentEquals(pixels, IntArray(pixels.size).also { captureToImage().readPixels(it) },
                "Paused raster diagnostic $dpr/$isMotion/$p/$transform/$cache/$snap")
            val prefix = "dpr$dpr-${if (isMotion) "motion" else "layout"}-${p.toString().removeSuffix(".0")}-${if (transform) "transform" else "identity"}"
            ImageIO.write(capture.toAwtImage(), "png", output.resolve("$prefix-${if (cache) "cached" else "direct"}${if (snap) "-snapped" else ""}.png"))
        }
        output.resolve("conditions-dpr$dpr.txt").writeText("600x400 CSS; density$dpr/fontScale1; 48px FoliaParityLatin-Bold; Amber; layout vs motion origin128+phase; optional CSS layout rounding only; scale1.6/rotate-4 around CSS word center; transparent; direct vs production cached raster; no glow/timing\n")
    }
}
