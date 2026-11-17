// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 AnimatedBubbleFrame / getBubbleTargetCharacterCount.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

/** Prepared from original reveal events, not accumulated display frames. The
 * current target already includes the source .2s lookahead; do not apply twice.
 * This track models a continuously active message starting at [mountTime]. */
internal class CappellaBubbleResize(private val plan:CappellaRevealPlan,private val sizes:List<CappellaBubbleSize>,mountTime:Double,
    intensity:CappellaIntensity=CappellaIntensity.NORMAL) {
    private val config=cappellaTransition(intensity)
    private fun target(time:Double)=sizes[plan.bubbleCount(time).coerceIn(sizes.indices)]
    private val initial=target(mountTime)
    private val events=plan.bubbleTargetTimes.filter{it.isFinite()&&it>mountTime}.distinct().sorted()
        .map{it to target(it)}
    private val width=CappellaScalarTrack(initial.width,events.map{it.first to it.second.width},CappellaCurve.RESIZE,config)
    private val height=CappellaScalarTrack(initial.height,events.map{it.first to it.second.height},CappellaCurve.RESIZE,config)
    fun at(time:Double)=CappellaBubbleSize(width.at(time).value,height.at(time).value)
}
