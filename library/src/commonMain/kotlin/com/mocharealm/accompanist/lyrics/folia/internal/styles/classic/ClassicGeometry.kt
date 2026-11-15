// Port of Folia glowWordOverlap.ts and classic/Visualizer.tsx, fixed cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.styles.classic

import com.mocharealm.accompanist.lyrics.folia.internal.animation.Pose
import kotlin.math.*

internal data class Reach(val halfWidth: Double, val halfHeight: Double)
internal data class Overflow(val left: Double, val right: Double, val top: Double, val bottom: Double)
internal fun wordReach(width: Double, font: Double, pose: Pose): Reach {
    fun at(scale: Double, angle: Double): Reach {
        val radians = angle * PI / 180
        return Reach(scale * (width * abs(cos(radians)) + font * abs(sin(radians))) / 2,
            scale * (font * abs(cos(radians)) + width * abs(sin(radians))) / 2)
    }
    var reach = at(pose.scale * 1.4, pose.rotation.toDouble())
    for (step in 0..8) {
        val sample = at(pose.scale.toDouble(), pose.rotation + pose.passedRotation * step / 8.0)
        reach = Reach(max(reach.halfWidth, sample.halfWidth), max(reach.halfHeight, sample.halfHeight))
    }
    return reach
}
internal fun wordOverflow(width: Double, font: Double, pose: Pose, offsetScale: Double = 1.0): Overflow {
    val reach = wordReach(width, font, pose)
    val x = pose.x * offsetScale; val y = pose.y * offsetScale
    return Overflow(max(0.0, reach.halfWidth - width / 2 - x), max(0.0, reach.halfWidth - width / 2 + x),
        max(0.0, reach.halfHeight - font * 1.22 / 2 - y), max(0.0, reach.halfHeight - font * 1.22 / 2 + y))
}
internal fun pairGap(width: Double, nextWidth: Double, font: Double, pose: Pose, nextPose: Pose,
    rtl: Boolean, offsetScale: Double = 1.0): Double =
    wordReach(width, font, pose).halfWidth - width / 2 + wordReach(nextWidth, font, nextPose).halfWidth - nextWidth / 2 +
        (if (rtl) nextPose.x - pose.x else pose.x - nextPose.x) * offsetScale

internal fun classicMargin(width: Double, nextWidth: Double, font: Double, pose: Pose, nextPose: Pose?,
    spacing: Float, avoid: Boolean, rtl: Boolean, offsetScale: Double): Double {
    val nextX = nextPose?.x ?: 0f
    val offset = (if (rtl) nextX - pose.x else pose.x - nextX) * offsetScale
    val overflow = width * (pose.scale * 1.4 - 1) / 2 + nextWidth * ((nextPose?.scale?.times(1.4) ?: 1.0) - 1) / 2
    var margin = max(font * .12 * spacing, (overflow + offset + font * .05) * spacing)
    if (avoid && nextPose != null) margin = max(margin, pairGap(width, nextWidth, font, pose, nextPose, rtl, offsetScale) + font * .05)
    // CSS serializes logical pixels to one decimal before device scaling.
    return floor(margin / offsetScale * 10 + .5) / 10 * offsetScale
}

internal data class LayoutBox(val left: Float, val top: Float, val width: Float, val height: Float)
internal data class ClassicGeometry(val boxes: List<LayoutBox>, val margins: List<Double>, val height: Float,
    val rowGap: Float, val paddingTop: Float, val paddingBottom: Float)

/** Flex wrap uses untransformed boxes + inline-end margins; transformed reach is reserved separately. */
internal fun classicGeometry(widths: List<Double>, boxes: List<Double>, font: Double, areaWidth: Double,
    poses: List<Pose>, startUs: Long, spacing: Float, avoid: Boolean, rtl: Boolean, density: Double,
    lineFont: Double = font, interlude: Boolean = false): ClassicGeometry {
    val margins = widths.indices.map { i -> if (interlude) 48.0 * density else classicMargin(widths[i], widths.getOrElse(i + 1) { 0.0 }, font,
        poses[i], poses.getOrNull(i + 1), spacing, avoid, rtl, density) }
    // Original early-return interlude config never contributes overlap reach.
    val overflow = if (avoid && !interlude) widths.indices.map { wordOverflow(widths[it], font, poses[it], density) } else emptyList()
    val top = if (avoid) ceil((overflow.maxOfOrNull { it.top } ?: 0.0) / density) * density else 0.0
    val bottom = if (avoid) ceil((overflow.maxOfOrNull { it.bottom } ?: 0.0) / density) * density else 0.0
    val gap = if (avoid) ceil(((overflow.maxOfOrNull { it.top } ?: 0.0) + (overflow.maxOfOrNull { it.bottom } ?: 0.0)) / density) * density else 0.0
    val rows = mutableListOf<MutableList<Int>>()
    var used = 0.0
    boxes.indices.forEach { i ->
        val size = boxes[i] + margins[i]
        if (rows.isEmpty() || (used > 0 && used + size > areaWidth)) { rows.add(mutableListOf()); used = 0.0 }
        rows.last().add(i); used += size
    }
    // Blink stores used line heights in 1/64 CSS px before accumulating row sizes.
    val rowHeight = floor(lineFont / density * 1.22 * 64) / 64 * density
    val contentHeight = rows.size * rowHeight + max(0, rows.size - 1) * gap
    val height = if (avoid) contentHeight + top + bottom else max(300 * density, contentHeight)
    val output = MutableList(boxes.size) { LayoutBox(0f, 0f, 0f, 0f) }
    val justify = if (interlude) 1 else floor(startUs / 1e6 % 5).toInt().coerceIn(0, 4)
    rows.forEachIndexed { rowIndex, row ->
        val total = row.sumOf { boxes[it] + margins[it] }
        val free = areaWidth - total
        val extra = when(justify) { 3 -> max(0.0, free) / row.size; 4 -> if(row.size > 1) max(0.0, free) / (row.size - 1) else 0.0; else -> 0.0 }
        var left = when(justify) { 1 -> free / 2; 2 -> free; 3 -> if(free > 0) extra / 2 else free / 2; else -> 0.0 }
        row.forEach { i ->
            output[i] = LayoutBox((if(rtl) areaWidth - left - boxes[i] else left).toFloat(),
                (top + (if(avoid) 0.0 else (height - contentHeight) / 2) + rowIndex * (rowHeight + gap)).toFloat(), boxes[i].toFloat(), rowHeight.toFloat())
            left += boxes[i] + margins[i] + extra
        }
    }
    return ClassicGeometry(output, margins, height.toFloat(), gap.toFloat(), top.toFloat(), bottom.toFloat())
}
