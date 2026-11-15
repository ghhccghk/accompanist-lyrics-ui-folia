package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.*
import kotlin.math.ceil

internal data class ParagraphRaster(val image: ImageBitmap, val origin: Offset)

/** Isolate native text-shadow coverage without altering the shaped paragraph.
 * Android multiplies the shadow by foreground alpha, so transparent text cannot
 * isolate it. Move the opaque foreground wholly outside this raster and return
 * only its shadow to the original position. The integer shift preserves phase. */
internal fun DrawScope.drawParagraphShadowSource(layout: TextLayoutResult) {
    val displacement=ceil(size.width)+layout.size.width+1f
    drawText(layout,Color.White,Offset(-displacement,0f),
        shadow=Shadow(Color.White,Offset(displacement,0f),.01f))
}

/** Like a browser composited text layer: shape/raster once at the layout size,
 * then transform coverage. Retain overhang space; never rasterize each character. */
internal fun rasterizeParagraph(layout: TextLayoutResult, density: Density, ink: Color = Color.White): ParagraphRaster {
    val pad = layout.size.height.coerceAtLeast(1)
    val image = ImageBitmap(layout.size.width.coerceAtLeast(1)+pad*2,layout.size.height.coerceAtLeast(1)+pad*2)
    CanvasDrawScope().draw(density,layout.layoutInput.layoutDirection,Canvas(image),Size(image.width.toFloat(),image.height.toFloat())) {
        // TextLayoutResult's platform paint is mutable. A cached paragraph may
        // have just painted a glyph-shadow mask; null does not clear that state
        // on Android. Restore the requested foreground shadow explicitly.
        drawText(layout,ink,Offset(pad.toFloat(),pad.toFloat()),
            shadow=layout.layoutInput.style.shadow ?: Shadow.None)
    }
    return ParagraphRaster(image,Offset(-pad.toFloat(),-pad.toFloat()))
}
