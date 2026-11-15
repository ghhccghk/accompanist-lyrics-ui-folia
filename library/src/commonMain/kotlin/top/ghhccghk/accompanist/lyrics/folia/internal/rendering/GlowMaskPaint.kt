package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.DrawScope

/** The mask already contains each shadow's ink alpha. Keep the animated span
 * opacity outside the tint operation for translucent shadows; folding both into
 * bitmap paint alpha changes low-coverage rounding on the software canvas. */
internal class GlowMaskPaint(ink: Color) {
    private val tint = ColorFilter.tint(ink.copy(alpha = 1f))
    private val separateOpacity = ink.alpha > 0f && ink.alpha < 1f
    private val groupPaint = Paint()

    fun draw(scope: DrawScope, mask: ImageBitmap, origin: Offset, opacity: Float) = with(scope) {
        if (!separateOpacity) {
            drawImage(mask, origin, alpha = opacity, colorFilter = tint)
        } else {
            groupPaint.alpha = opacity
            val canvas = drawContext.canvas
            canvas.saveLayer(Rect(origin.x, origin.y, origin.x + mask.width, origin.y + mask.height), groupPaint)
            try {
                drawImage(mask, origin, colorFilter = tint)
            } finally {
                canvas.restore()
            }
        }
    }
}
