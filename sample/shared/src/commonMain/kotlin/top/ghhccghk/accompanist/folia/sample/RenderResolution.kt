package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt

enum class RenderResolution(val scale: Float, val label: String) {
    Full(1f,"100%"), ThreeQuarters(.75f,"75%"), Half(.5f,"50%")
}

/** Keep the host's font conversion, including Android's nonlinear font scaling. */
private class RasterDensity(private val host: Density, override val density: Float) : Density {
    override val fontScale: Float get() = host.fontScale
    override fun Dp.toSp(): TextUnit = with(host) { this@toSp.toSp() }
    override fun TextUnit.toDp(): Dp = with(host) { this@toDp.toDp() }
}

/** Sample-only quality experiment: rasterize a smaller stage, then upscale once.
 * Physical child pixels and density both shrink; the logical viewport and media
 * time stay the same. Fractional pixel rounding can change text raster/layout.
 * The full-resolution path introduces no offscreen scaling layer. */
@Composable
internal fun SampleRenderStage(resolution: RenderResolution, modifier: Modifier,
    content: @Composable () -> Unit) {
    if (resolution == RenderResolution.Full) {
        Box(modifier) { content() }
        return
    }
    val host = LocalDensity.current
    val density = remember(host,resolution) { RasterDensity(host,host.density*resolution.scale) }
    BoxWithConstraints(modifier.clipToBounds(),contentAlignment=AbsoluteAlignment.TopLeft) {
        check(constraints.hasBoundedWidth && constraints.hasBoundedHeight) { "Lyrics stage needs a bounded viewport" }
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val rasterWidth = (width*resolution.scale).roundToInt().coerceAtLeast(1)
        val rasterHeight = (height*resolution.scale).roundToInt().coerceAtLeast(1)
        CompositionLocalProvider(LocalDensity provides density) {
            Layout(content,Modifier.graphicsLayer {
                scaleX = width.toFloat()/rasterWidth
                scaleY = height.toFloat()/rasterHeight
                transformOrigin = TransformOrigin(0f,0f)
                compositingStrategy = CompositingStrategy.Offscreen
            }) { children, _ ->
                val placeables = children.map { it.measure(Constraints.fixed(rasterWidth,rasterHeight)) }
                layout(rasterWidth,rasterHeight) { placeables.forEach { it.place(0,0) } }
            }
        }
    }
}

@Composable
internal fun RenderResolutionControls(session: SampleSession) {
    Text("歌词渲染分辨率（实验）")
    FlowRow(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
        RenderResolution.entries.forEach { resolution ->
            FilterChip(selected=session.renderResolution==resolution,
                onClick={session.renderResolution=resolution},label={Text(resolution.label)})
        }
    }
    Text("仅降低歌词舞台的像素数，文字和辉光会变柔；100% 为原始分辨率。",
        style=MaterialTheme.typography.bodySmall)
}
