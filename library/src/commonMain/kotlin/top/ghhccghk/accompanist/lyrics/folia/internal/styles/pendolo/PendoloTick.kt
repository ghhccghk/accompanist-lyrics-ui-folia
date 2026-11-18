// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 VisualizerPendolo.tsx / pendoloMotionProfile.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Media-time adaptation: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import top.ghhccghk.accompanist.lyrics.folia.internal.motion.*

internal enum class PendoloIntensity { CALM, NORMAL, CHAOTIC }
internal data class PendoloTickConfig(val stiffness: Double, val damping: Double, val mass: Double = .8)
internal fun pendoloTickConfig(intensity: PendoloIntensity, snappiness: Double = 2.0): PendoloTickConfig {
    require(snappiness.isFinite() && snappiness in .5..2.0)
    val spring = when (intensity) { PendoloIntensity.CALM -> .72; PendoloIntensity.NORMAL -> 1.0; PendoloIntensity.CHAOTIC -> 1.3 }
    val damping = when (intensity) { PendoloIntensity.CALM -> 1.2; PendoloIntensity.NORMAL -> 1.0; PendoloIntensity.CHAOTIC -> .84 }
    return PendoloTickConfig(180 * snappiness * spring, (18 + 4 / maxOf(.5, snappiness)) * damping)
}

internal data class PendoloTickFrame(val target: Double, val value: Double, val velocity: Double,
    val wheelRotation: Double, val textCorrection: Double)

/** Immutable analytic tick response. Source physical parameters and angle
 * correction are retained; target changes are prepared at media boundaries.
 * Pause/seek/remount do not depend on display cadence or previous queries.
 * Retargeting carries the exact physical derivative; browser frame-velocity
 * estimation is separately measured, not reproduced by a fixed-frame replay. */
internal class PendoloTickTrack(lines: List<PendoloLineTime>, intensity: PendoloIntensity = PendoloIntensity.NORMAL,
    snappiness: Double = 2.0, private val arcAngleDeg: Double = 100.0,
    private val origin: Double = 0.0) {
    private val timeline = PendoloTimeline(lines)
    private val initial = timeline.anchor(origin)
    private data class Segment(val time: Double, val spring: DampedSpring)
    private val segments: List<Segment>
    init {
        require(origin.isFinite() && arcAngleDeg.isFinite())
        val config = pendoloTickConfig(intensity, snappiness)
        var target = initial
        segments = buildList {
            for (t in (listOf(origin) + timeline.boundaries.filter { it > origin })) {
                val next = timeline.anchor(t, after = true)
                if (target == next) continue
                val state = lastOrNull()?.let { it.spring.sampleContinuous(t - it.time) } ?: SpringSample(initial, 0.0, true)
                // Original useSpring follows attachFollow's explicit limits,
                // not JSAnimation's scale-dependent dispatcher defaults.
                val spring = DampedSpring(state.value, next, state.velocity,
                    config.stiffness, config.damping, config.mass, restDelta = .001, restSpeed = .01)
                spring.continuousDuration // resolve once during preparation
                add(Segment(t, spring))
                target = next
            }
        }
    }
    fun at(time: Double, reducedMotion: Boolean = false): PendoloTickFrame {
        require(time.isFinite())
        val t = maxOf(origin, time)
        val target = timeline.anchor(t)
        var lo = 0; var hi = segments.size
        while (lo < hi) { val m = (lo + hi) / 2; if (segments[m].time <= t) lo = m + 1 else hi = m }
        val state = if (lo == 0) SpringSample(initial, 0.0, true) else segments[lo - 1].let { it.spring.sampleContinuous(t - it.time) }
        val value = if (reducedMotion) target else state.value
        val rotation = -(value - target) * arcAngleDeg / 8
        return PendoloTickFrame(target, value, if (reducedMotion) 0.0 else state.velocity, rotation, -rotation * .65)
    }
}
