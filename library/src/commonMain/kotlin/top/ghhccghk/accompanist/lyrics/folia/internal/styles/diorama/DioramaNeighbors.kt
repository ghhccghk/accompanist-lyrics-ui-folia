// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 dioramaSceneLayout.ts / sceneConstants.ts / sceneFrame.ts.
// Normal and same-corridor outgoing text windows; source: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import kotlin.math.sqrt

/** Normal source window. A backward query never invents a transition. */
internal fun dioramaMountedTextIndices(current: Int, total: Int): IntRange {
    require(total >= 0)
    if (total == 0) return IntRange.EMPTY
    require(current in 0 until total)
    return maxOf(0, current - 2)..minOf(total - 1, current + 3)
}

/** Source sorted union: live -2/+3, departing -2/+1. One corridor for loops;
 * cross-song indices require a separate sequencer and are not fabricated. */
internal fun dioramaTransitionTextIndices(current: Int, outgoing: Int?, total: Int): List<Int> {
    val live=dioramaMountedTextIndices(current,total)
    if(outgoing==null||total==0)return live.toList()
    require(outgoing in 0 until total)
    return (live+(maxOf(0,outgoing-2)..minOf(total-1,outgoing+1))).distinct().sorted()
}

internal fun dioramaIsOutgoing(index: Int, current: Int, outgoing: Int?): Boolean =
    outgoing!=null && kotlin.math.abs(index-outgoing)<=kotlin.math.abs(index-current)

internal fun dioramaOutgoingOpacity(offset: Int): Double =
    if(offset==0).7 else if(kotlin.math.abs(offset)<=2).45 else .25

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
    primary: DioramaLinearColor, secondary: DioramaLinearColor, outgoing: Int? = null): DioramaNeighborFrame {
    val center = dioramaComposeLocal(frame, placement.offsetR, placement.offsetU, 0.0)
    val delta = center - view.eye
    val life = dioramaTextLife(sqrt(delta.dot(delta)))
    val departing=dioramaIsOutgoing(index,current,outgoing)
    return DioramaNeighborFrame(index,
        dioramaFrameFit(advancePx * .62 / fontPx, 5.2, view.fov, view.width / view.height) * placement.scale,
        (if(departing)dioramaOutgoingOpacity(index-outgoing!!)else dioramaNeighborOpacity(index-current))*life,
        if(departing||index<current)primary else secondary, center, delta.dot(view.forward))
}
