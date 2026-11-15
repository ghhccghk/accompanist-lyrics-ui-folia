package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.staticCompositionLocalOf

/** Only enable color-dependent foreground coverage on a verified font backend. */
internal expect fun usesNativeBodyInk(): Boolean
internal val LocalClassicNativeBodyInk = staticCompositionLocalOf { usesNativeBodyInk() }

/** Keep the original shaped paragraph and raster scale while preserving native ink coverage.
 * One most-recent color is retained; pause reuses it and a seek reconstructs directly.
 * Never mutate a bitmap already recorded in a graphics layer or retain a color history. */
internal class BodyParagraphRaster(
    private val layout: TextLayoutResult,
    private val density: Density,
    private val nativeInk: Boolean = usesNativeBodyInk(),
) {
    private var ink = Color.Unspecified
    private var raster: ParagraphRaster? = null

    internal fun rasterFor(color: Color): ParagraphRaster {
        val requested = if (nativeInk) color else Color.White
        if (raster == null || requested != ink) {
            raster = rasterizeParagraph(layout, density, requested)
            ink = requested
        }
        return checkNotNull(raster)
    }

    fun draw(scope: DrawScope, color: Color, offset: Offset) {
        val source = rasterFor(color)
        with(scope) {
            drawImage(source.image, source.origin + offset,
                colorFilter = if (nativeInk) null else ColorFilter.tint(color))
        }
    }
}
