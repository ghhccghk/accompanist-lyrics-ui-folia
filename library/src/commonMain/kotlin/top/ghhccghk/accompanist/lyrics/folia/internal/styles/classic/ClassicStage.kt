// Folia classic/Visualizer.tsx: centred 70vh p-8 stage and independent line boxes.
// SPDX-License-Identifier: AGPL-3.0-only. Source ledger: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.classic

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.floor
import kotlin.math.truncate

internal data class ClassicViewport(val width: Float, val height: Float)
internal val LocalClassicViewport = staticCompositionLocalOf<ClassicViewport?> { null }

/** useOverlapFit reads integer clientHeight, then subtracts the CSS padding. */
internal fun classicHeightBudget(viewportHeight: Float, density: Float): Float {
    val stage = floor(viewportHeight / density * .7 * 64) / 64
    return ((floor(stage + .5) - 64).coerceAtLeast(0.0) * density).toFloat()
}

internal fun classicStageTop(viewportHeight: Float, density: Float): Float {
    val viewport = (viewportHeight / density).toDouble()
    val stage = floor(viewport * .7 * 64) / 64
    return (truncate((viewport - stage) / 2 * 64) / 64 * density).toFloat()
}

internal fun classicLineTopInStage(viewportHeight: Float, lineHeight: Float, density: Float): Float {
    val stage = floor(viewportHeight / density * .7 * 64) / 64
    return ((32 + truncate((stage - 64 - lineHeight / density) / 2 * 64) / 64) * density).toFloat()
}

/** Blink stores the stage and both flex centering offsets in 1/64 CSS pixels. */
internal fun classicLineCenter(viewportHeight: Float, lineHeight: Float, density: Float): Float {
    return classicStageTop(viewportHeight, density) + classicLineTopInStage(viewportHeight, lineHeight, density) + lineHeight / 2
}

/** Static word layers snap their cumulative offset in the composited stage.
 * Parent layout fractions must not be discarded before this rounding. */
internal fun classicWordPaintOffset(value: Float, parent: Float, density: Float): Float =
    floor((value + parent) / density + .5f) * density - parent

/** Integer measurement does not require integer placement or transform origins.
 * Each active/exiting line is centred independently, including different heights. */
@Composable
internal fun ClassicStage(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val viewport = with(density) { ClassicViewport(maxWidth.toPx(), maxHeight.toPx()) }
        CompositionLocalProvider(LocalClassicViewport provides viewport) {
            Layout(content, Modifier.fillMaxSize()) { children, constraints ->
                val width = (constraints.maxWidth - 64.dp.roundToPx()).coerceAtLeast(0).coerceAtMost(1152.dp.roundToPx())
                val childConstraints = Constraints(minWidth = width, maxWidth = width)
                val measured = children.map { it.measure(childConstraints) }
                layout(constraints.maxWidth, constraints.maxHeight) {
                    measured.forEach { child ->
                        val x = (constraints.maxWidth - child.width) / 2f
                        val y = (constraints.maxHeight - child.height) / 2f
                        val ix = floor(x).toInt(); val iy = floor(y).toInt()
                        child.placeWithLayer(ix, iy) { translationX = x - ix; translationY = y - iy }
                    }
                }
            }
        }
    }
}
