package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.unit.*

/** Compose/AOSP radius conversion is sigma = .57735 * radius + .5.
 * CSS blur() already specifies sigma; invert once at the platform boundary. */
internal fun nativeRadiusForCssSigma(sigma: Float): Float = ((sigma-.5f)/.57735f).coerceAtLeast(0f)

private class SoftwareDrawScope(scope: DrawScope) : DrawScope by scope

internal expect fun supportsNativeBlurLayers(): Boolean
/** Internal parity harness override, never a user-visible backend setting. */
internal val LocalClassicSoftwareLayers = staticCompositionLocalOf { false }

@Composable
internal fun rememberClassicLayer(): ClassicLayer {
    val native = rememberGraphicsLayer()
    val software = LocalClassicSoftwareLayers.current || !supportsNativeBlurLayers()
    val layer = remember(native, software) { ClassicLayer(native, software) }
    DisposableEffect(layer) { onDispose { layer.release() } }
    return layer
}

/** Group opacity and blur must work together, including on pre-31 Android.
 * Software recording uses the same intact shaped paragraphs and draw commands;
 * it never attempts to draw a hardware RenderNode into a software Canvas. */
internal class ClassicLayer(private val native: GraphicsLayer, private val software: Boolean) {
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
        softwareFrame = software || scope is SoftwareDrawScope || (blurSigma > .01f && blurSigma <= .5f)
        if (!softwareFrame) {
            native.alpha = alpha
            native.compositingStrategy = if(forceOffscreen) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            native.renderEffect = if (blurSigma > .01f) BlurEffect(nativeRadiusForCssSigma(blurSigma), nativeRadiusForCssSigma(blurSigma), TileMode.Decal) else null
            with(scope) { native.record(size = size, block = block) }
        } else {
            val image = bitmap?.takeIf { it.width == size.width && it.height == size.height }
                ?: ImageBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1)).also { bitmap = it }
            canvasScope.draw(scope, scope.layoutDirection, Canvas(image), Size(image.width.toFloat(),image.height.toFloat())) {
                drawRect(Color.Transparent, blendMode = BlendMode.Clear)
                block(SoftwareDrawScope(this))
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

internal fun DrawScope.drawClassicLayer(layer: ClassicLayer) = layer.draw(this)
