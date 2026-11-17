// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 pendoloGeometry.ts and types.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Source/adaptations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import kotlin.math.*

/** Text-wheel controls only. Audio lenses, gears and backgrounds are separate. */
internal data class PendoloGeometryTuning(
    val arcRadius: Double = .42,
    val arcAngleDeg: Double = 100.0,
    val wheelCenterX: Double = 0.0,
    val wheelCenterY: Double = .5,
    val activeScale: Double = 1.25,
)

internal data class PendoloWheelItem(
    val index: Int, val angleRad: Double, val angleDeg: Double,
    val x: Double, val y: Double, val active: Boolean,
    val distance: Double, val alpha: Double, val scale: Double,
)

/** Source's 17-index candidate window and strict right-semicircle visibility.
 * Block heights must include the largest main/subtitle state, exactly as the
 * source caller does. Fractional gap anchors intentionally use uniform angles. */
internal fun pendoloWheelLayout(
    lineCount: Int, target: Double, angleOffset: Double,
    width: Double, height: Double,
    tuning: PendoloGeometryTuning = PendoloGeometryTuning(),
    radiusOffset: Double = 0.0, blockHeights: List<Double> = emptyList(),
): List<PendoloWheelItem> {
    require(lineCount >= 0 && target.isFinite() && angleOffset.isFinite())
    val cx = width * tuning.wheelCenterX
    val cy = height * tuning.wheelCenterY
    val radius = min(width, height) * tuning.arcRadius + radiusOffset
    require(radius > 0 && radius.isFinite())
    val step = tuning.arcAngleDeg * PI / 180 / 8
    val integer = target == floor(target)
    val active = if (integer && target >= 0 && target < lineCount) target.toInt() else -1
    // Keep the original empty-input center reference; the resulting window is empty.
    val center = max(0, min(lineCount - 1, floor(if (target >= 0) target else 0.0).toInt()))
    val start = max(0, center - 8)
    val end = min(lineCount - 1, center + 8)
    val angles = mutableMapOf<Int, Double>()
    if (integer && target >= start && target <= end) {
        val focal = target.toInt()
        angles[focal] = 0.0
        for (i in focal + 1..end) {
            val spacing = max(step, ((blockHeights.getOrNull(i - 1) ?: 0.0) +
                (blockHeights.getOrNull(i) ?: 0.0) + 24) / (2 * radius))
            angles[i] = angles.getValue(i - 1) + spacing
        }
        for (i in focal - 1 downTo start) {
            val spacing = max(step, ((blockHeights.getOrNull(i + 1) ?: 0.0) +
                (blockHeights.getOrNull(i) ?: 0.0) + 24) / (2 * radius))
            angles[i] = angles.getValue(i + 1) - spacing
        }
    }
    return (start..end).mapNotNull { i ->
        val distance = abs(i - target)
        val angle = (angles[i] ?: ((i - target) * step)) + angleOffset
        if (abs(angle) >= PI / 2) null else PendoloWheelItem(
            i, angle, angle * 180 / PI,
            cx + radius * cos(angle), cy + radius * sin(angle), i == active,
            distance, max(.12, cos(angle * .75).pow(2.5) * (1 - distance * .18)),
            if (i == active) tuning.activeScale else max(.7, 1 - distance * .08),
        )
    }
}

/** Source caller reserves text width from the mechanical radius, before the
 * additional .06-min-dimension lyric-ring offset. Do not conflate the two. */
internal fun pendoloAvailableTextWidth(width: Double, height: Double,
    tuning: PendoloGeometryTuning = PendoloGeometryTuning()): Double =
    max(140.0, min(width * .46, width - (width * tuning.wheelCenterX + min(width, height) * tuning.arcRadius) - 48))
