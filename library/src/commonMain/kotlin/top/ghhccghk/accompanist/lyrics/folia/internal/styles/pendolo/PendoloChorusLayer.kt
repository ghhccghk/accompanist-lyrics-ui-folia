// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 PendoloActiveLyricSweep.tsx, post-mask sequential CSS filters.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.unit.IntSize
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.math.ceil

internal class PendoloChorusLayer(private val first: ClassicLayer, private val second: ClassicLayer) {
    fun draw(scope: DrawScope, width: Float, height: Float, filter: PendoloChorusFilter,
        fill: Color, accent: Color, ink: DrawScope.() -> Unit) = with(scope) {
        // The second CSS drop-shadow consumes source + first shadow. Two
        // independently blurred copies of the original ink are not equivalent.
        val pad = ceil((3 * (filter.firstSigma + filter.secondSigma) + filter.padding + 2) * density).toFloat()
        val size = IntSize(ceil(width + 2 * pad).toInt().coerceAtLeast(1),
            ceil(height + 2 * pad).toInt().coerceAtLeast(1))
        val bounds = Rect(0f, 0f, size.width.toFloat(), size.height.toFloat())
        fun DrawScope.tinted(color: Color, block: DrawScope.() -> Unit) {
            drawContext.canvas.saveLayer(bounds, Paint().apply { colorFilter = ColorFilter.tint(color) })
            block()
            drawContext.canvas.restore()
        }
        first.alpha = 1f
        first.blurSigma = (filter.firstSigma * density).toFloat()
        first.record(this, size) { tinted(fill.copy(alpha = .72f)) { translate(pad, pad, block = ink) } }
        second.alpha = 1f
        second.blurSigma = (filter.secondSigma * density).toFloat()
        second.record(this, size) {
            tinted(accent.copy(alpha = .28f)) {
                drawClassicLayer(first)
                translate(pad, pad, block = ink)
            }
        }
        translate(-pad, -pad) { drawClassicLayer(second); drawClassicLayer(first) }
        ink()
    }
}

@Composable internal fun rememberPendoloChorusLayer(): PendoloChorusLayer =
    PendoloChorusLayer(rememberClassicLayer(), rememberClassicLayer())
