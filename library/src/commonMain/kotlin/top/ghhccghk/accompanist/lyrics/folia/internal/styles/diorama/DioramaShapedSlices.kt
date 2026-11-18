// SPDX-License-Identifier: AGPL-3.0-only
// Independent complete-paragraph coverage partition for the Folia text adapter.
// Source units and internal clocks are unchanged. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.text.TextLayoutResult
import kotlin.math.max

/** One texel column belongs to one original display unit. Logical grapheme
 * selections supply visual intervals, not one bounding rectangle spanning bidi
 * runs. Padding/gaps go to their nearest visual interval. This is coverage
 * ownership, NOT isolated glyph shaping or an invented timing model.
 *
 * Integer, nonoverlapping clips preserve the exact complete-run alpha even at
 * a ligature split. Colour-only text spans are unsuitable: they can split the
 * shaping/style run and remove a piece of a cross-unit fi ligature. */
internal fun dioramaShapedColumns(layout: TextLayoutResult, units: List<DioramaUnit>,
    width: Int, padding: Int, checkpoint: () -> Unit = {}): List<List<IntRange>> {
    data class Interval(val left: Float, val right: Float, val owner: Int)
    val intervals = units.flatMapIndexed { index, unit ->
        checkpoint()
        var cursor = unit.charStart
        unit.timeline.map { glyph ->
            val start = cursor
            cursor += glyph.text.length
            val rect = layout.getPathForRange(start, cursor).getBounds()
            Interval(rect.left, rect.right, index)
        }
    }
    if (intervals.isEmpty()) return List(units.size) { emptyList() }
    val ranges = List(units.size) { mutableListOf<IntRange>() }
    var previous = -1
    var start = 0
    for (x in 0 until width) {
        if(x%256==0)checkpoint()
        val center = x + .5 - padding
        // A shaping cluster may straddle source word boundaries. Partition its
        // existing coverage at those caret advances, retaining every unit's
        // own clock, rather than reshaping it or dropping one of the clocks.
        var best = Double.POSITIVE_INFINITY
        var owner = 0
        for (interval in intervals) {
            val distance = max(0.0, max(interval.left - center, center - interval.right))
            if (distance < best) { best = distance; owner = interval.owner }
        }
        if (owner != previous) {
            if (previous >= 0) ranges[previous] += start until x
            start = x
            previous = owner
        }
    }
    if (previous >= 0) ranges[previous] += start until width
    checkpoint()
    return ranges
}
