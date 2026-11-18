// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 dioramaSceneLayout.ts / sceneConstants.ts / sceneFrame.ts.
// Normal single-song text window only; source and exclusions: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import kotlin.math.sqrt

/** The normal source window. Cross-song outgoing windows need the sequencer and
 * flight contract and are deliberately not inferred from a backward media seek. */
internal fun dioramaMountedTextIndices(current: Int, total: Int): IntRange {
    require(total >= 0)
    if (total == 0) return IntRange.EMPTY
    require(current in 0 until total)
    return maxOf(0, current - 2)..minOf(total - 1, current + 3)
}

internal fun dioramaNeighborOpacity(offset: Int): Double = when (offset) {
    -2 -> .1
    -1 -> .3
    1 -> .34
    2 -> .16
    3 -> .06
    else -> 0.0
}

internal data class DioramaNeighborFrame(val index: Int, val fit: Double, val opacity: Double,
    val color: DioramaLinearColor, val center: DioramaVec, val depth: Double)

/** Source frame-fit is evaluated at fixed hero distance, life at Euclidean
 * distance. View depth is ONLY the transparent painter order, never the life. */
internal fun dioramaNeighborFrame(index: Int, current: Int, advancePx: Int, fontPx: Int,
    frame: DioramaFrame, placement: DioramaPlacement, view: DioramaPerspective,
    primary: DioramaLinearColor, secondary: DioramaLinearColor): DioramaNeighborFrame {
    val center = dioramaComposeLocal(frame, placement.offsetR, placement.offsetU, 0.0)
    val delta = center - view.eye
    val life = dioramaTextLife(sqrt(delta.dot(delta)))
    return DioramaNeighborFrame(index,
        dioramaFrameFit(advancePx * .62 / fontPx, 5.2, view.fov, view.width / view.height) * placement.scale,
        dioramaNeighborOpacity(index - current) * life,
        if (index < current) primary else secondary, center, delta.dot(view.forward))
}
