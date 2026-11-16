// Frozen recording behavior from 9f47417; regression oracle for zero-opacity culling.
package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.*

private class LegacyTransparentDrawScope(scope: DrawScope) : DrawScope by scope

internal class LegacyTransparentLayer(private val native: GraphicsLayer, private val software: Boolean) {
    var alpha = 1f
    var blurSigma = 0f
    var forceOffscreen = false
    private var bitmap: ImageBitmap? = null
    private var painted: BlurredPixels? = null
    private var softwareFrame = software
    private val canvasScope = CanvasDrawScope()
    fun record(scope: DrawScope, size: IntSize, block: DrawScope.() -> Unit) {
        // Native radius APIs cannot express 0 < sigma <= .5. Propagate the
        // software scope into nested groups so none replays a RenderNode here.
        softwareFrame = software || scope is LegacyTransparentDrawScope || (blurSigma > .01f && blurSigma <= .5f)
        if (!softwareFrame) {
            // A tiny-sigma frame may have allocated a full software tree. Once
            // native recording resumes, none of those pixels is drawn again.
            // Drop references now rather than retaining peak bitmaps to dispose.
            release()
            native.alpha = alpha
            native.compositingStrategy = if(forceOffscreen) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            native.renderEffect = if (blurSigma > .01f) BlurEffect(nativeRadiusForCssSigma(blurSigma), nativeRadiusForCssSigma(blurSigma), TileMode.Decal) else null
            with(scope) { native.record(size = size, block = block) }
        } else {
            val image = bitmap?.takeIf { it.width == size.width && it.height == size.height }
                ?: ImageBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1)).also { bitmap = it }
            canvasScope.draw(scope, scope.layoutDirection, Canvas(image), Size(image.width.toFloat(),image.height.toFloat())) {
                drawRect(Color.Transparent, blendMode = BlendMode.Clear)
                block(LegacyTransparentDrawScope(this))
            }
            painted = if (blurSigma > .01f) blurPremultiplied(image, blurSigma)
                else BlurredPixels(image, Offset.Zero)
        }
    }
    fun draw(scope: DrawScope) = with(scope) {
        if (!softwareFrame) drawLayer(native)
        else painted?.let { drawImage(it.image, it.origin, alpha = alpha) }
    }
    fun release() { painted = null; bitmap = null }
}
