package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.*

internal data class ParagraphRaster(val image: ImageBitmap, val origin: Offset)

/** Like a browser composited text layer: shape/raster once at the layout size,
 * then transform coverage. Retain overhang space; never rasterize each character. */
internal fun rasterizeParagraph(layout: TextLayoutResult, density: Density): ParagraphRaster {
    val pad = layout.size.height.coerceAtLeast(1)
    val image = ImageBitmap(layout.size.width.coerceAtLeast(1)+pad*2,layout.size.height.coerceAtLeast(1)+pad*2)
    CanvasDrawScope().draw(density,layout.layoutInput.layoutDirection,Canvas(image),Size(image.width.toFloat(),image.height.toFloat())) {
        drawText(layout,Color.White,Offset(pad.toFloat(),pad.toFloat()))
    }
    return ParagraphRaster(image,Offset(-pad.toFloat(),-pad.toFloat()))
}
