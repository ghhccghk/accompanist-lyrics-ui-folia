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
import top.ghhccghk.accompanist.lyrics.folia.ExperimentalFoliaColourProcessor
import top.ghhccghk.accompanist.lyrics.folia.LocalFoliaColourProcessor

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
    val colour = LocalFoliaColourProcessor.current
    val layer = remember(native, software, colour) { ClassicLayer(native, software, colour) }
    DisposableEffect(layer) { onDispose { layer.release() } }
    return layer
}

/** Group opacity and blur must work together, including on pre-31 Android.
 * Software recording uses the same intact shaped paragraphs and draw commands;
 * it never attempts to draw a hardware RenderNode into a software Canvas. */
internal class ClassicLayer(private val native: GraphicsLayer, private val software: Boolean,
    private val colour: ExperimentalFoliaColourProcessor? = null) {
    var alpha = 1f
    var blurSigma = 0f
    var forceOffscreen = false
    private var bitmap: ImageBitmap? = null
    private var painted: BlurredPixels? = null
    private var softwareFrame = software
    private var recorded = false
    private val canvasScope = CanvasDrawScope()
    fun record(scope: DrawScope, size: IntSize, block: DrawScope.() -> Unit) {
        // Group opacity zero cannot contribute pixels, even through a blur.
        // Skip only exact zero: faint nonzero tails remain part of the effect.
        // Invalidate the old frame so seek/replay cannot draw stale content.
        // Callers keep media state and hit geometry outside this drawing block.
        if (alpha == 0f) {
            release()
            return
        }
        // Native radius APIs cannot express 0 < sigma <= .5. Propagate the
        // software scope into nested groups so none replays a RenderNode here.
        softwareFrame = software || scope is SoftwareDrawScope || (blurSigma > .01f && blurSigma <= .5f)
        if (!softwareFrame) {
            // A tiny-sigma frame may have allocated a full software tree. Once
            // native recording resumes, none of those pixels is drawn again.
            // Drop references now rather than retaining peak bitmaps to dispose.
            release()
            native.alpha = alpha
            native.compositingStrategy = if(forceOffscreen) CompositingStrategy.Offscreen else CompositingStrategy.Auto
            native.renderEffect = if (blurSigma > .01f) BlurEffect(nativeRadiusForCssSigma(blurSigma), nativeRadiusForCssSigma(blurSigma), TileMode.Decal) else null
            // DrawScope.record temporarily reuses the caller's draw context.
            // Passing that same scope as Density can make a density read in
            // the recorded block delegate back to itself (e.g. an em halo).
            // Snapshot values and use the member's independent record scope.
            native.record(Density(scope.density, scope.fontScale), scope.layoutDirection, size, block)
        } else {
            val image = bitmap?.takeIf { it.width == size.width && it.height == size.height }
                ?: ImageBitmap(size.width.coerceAtLeast(1), size.height.coerceAtLeast(1)).also { bitmap = it }
            canvasScope.draw(scope, scope.layoutDirection, Canvas(image), Size(image.width.toFloat(),image.height.toFloat())) {
                drawRect(Color.Transparent, blendMode = BlendMode.Clear)
                block(SoftwareDrawScope(this))
            }
            painted = if (blurSigma > .01f) blurPremultiplied(image, blurSigma, colour)
                else BlurredPixels(image, Offset.Zero)
        }
        recorded = true
    }
    fun draw(scope: DrawScope) = with(scope) {
        if (!recorded) return@with
        if (!softwareFrame) drawLayer(native)
        else painted?.let { drawImage(it.image, it.origin, alpha = alpha) }
    }
    fun release() { painted = null; bitmap = null; recorded = false }
}

internal fun DrawScope.drawClassicLayer(layer: ClassicLayer) = layer.draw(this)
