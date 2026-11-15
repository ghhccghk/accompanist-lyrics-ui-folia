// Adapted from Folia v0.7.26 (cc6226e...), chthollyphile and contributors.
// Original files and modifications: docs/upstream.md. SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.animation

import kotlin.math.*
import com.mocharealm.accompanist.lyrics.folia.FoliaAnimationIntensity

internal enum class Reveal { Normal, Fast, Instant }
internal data class RenderProfile(val mode: Reveal, val renderEndUs: Long, val lookaheadUs: Long)
/** JSAnimation rounds elapsed milliseconds before evaluating a generator. */
internal fun motionElapsedUs(delta:Long) = (floor(delta.coerceAtLeast(0)/1000.0+.5)*1000).toLong()

internal fun renderProfile(start: Long, end: Long, lastWordEnd: Long = end): RenderProfile {
    val duration = (end - start).coerceAtLeast(0)
    if (duration < 100_000) return RenderProfile(Reveal.Instant, maxOf(end, start + 67_000), 30_000)
    val fast = duration < 180_000
    val enter = if (fast) (duration * .45).toLong().coerceIn(45_000, 60_000) else (duration * .34).toLong().coerceIn(220_000, 420_000)
    val exit = if (fast) (duration * .22).toLong().coerceIn(30_000, 40_000) else (duration * .18).toLong().coerceIn(180_000, 320_000)
    val passed = maxOf(lastWordEnd, start) + if (fast) 30_000 else 60_000
    val exitStart = maxOf(passed, end - exit, if (fast) start + enter + 10_000 else Long.MIN_VALUE)
    return RenderProfile(if (fast) Reveal.Fast else Reveal.Normal, maxOf(end, exitStart + exit), if (fast) 80_000 else 150_000)
}

internal fun spring(seconds: Double): Float {
    val t = seconds.coerceAtLeast(0.0)
    return (1 - exp(-10 * t) * (cos(10 * t) + sin(10 * t))).toFloat()
}

/** Under-damped, zero-velocity response with Motion's magnitude-dependent rest thresholds. */
internal fun physicsValue(from: Float, to: Float, seconds: Double, stiffness: Double = 200.0,
    damping: Double = 20.0, restSpeedOverride: Double? = null): Float {
    val t = seconds.coerceAtLeast(0.0); val delta = (to-from).toDouble()
    val decay = damping/2; val w = sqrt(stiffness-decay*decay)
    val envelope = exp(-decay*t)
    val remaining = envelope*delta*(cos(w*t)+decay/w*sin(w*t))
    val velocity = envelope*delta*stiffness/w*sin(w*t)
    val granular = abs(delta)<5
    val restSpeed=restSpeedOverride ?: if(granular) .01 else 2.0
    val restDelta=if(granular) .005 else .5
    if(abs(velocity)<=restSpeed && abs(remaining)<=restDelta) return to
    // Motion computes a generator's duration with 50ms probes (up to 20s), then
    // a running JSAnimation clamps to its final keyframe beyond that duration.
    // This is duration resolution, not a frame clock: every query still samples
    // the analytic media-time curve and has no mutable playback/event history.
    val probes=minOf(floor(t*20).toInt(),399)
    for(i in 0..probes) {
        val at=i*.05; val e=exp(-decay*at)
        val r=e*delta*(cos(w*at)+decay/w*sin(w*at))
        val v=e*delta*stiffness/w*sin(w*at)
        if(abs(v)<=restSpeed && abs(r)<=restDelta) return to
    }
    return (to-remaining).toFloat()
}

internal data class Pose(val x: Float, val y: Float, val scale: Float, val rotation: Float,
    val passedRotation: Float, val rotationEnabled: Boolean = true,
    val waitingX: Float = x + sin(y) * 100, val waitingY: Float = y + cos(x) * 50)
internal fun wordPose(startUs: Long, index: Int, rotation: Boolean, interlude: Boolean = false,
    intensity: FoliaAnimationIntensity = FoliaAnimationIntensity.Normal): Pose {
    val seed = startUs / 1_000_000.0 + index
    fun random(offset: Int): Double { val n = sin(seed + offset) * 10000; return n - floor(n) }
    if (interlude) {
        val y=(random(2)-.5)*15
        return Pose(0f,y.toFloat(),1.5f,0f,0f,rotation,(sin(y)*100).toFloat(),(y+50).toFloat())
    }
    val spread = when(intensity) { FoliaAnimationIntensity.Calm -> 0f; FoliaAnimationIntensity.Normal -> 40f; FoliaAnimationIntensity.Chaotic -> 120f }
    val rotate = when(intensity) { FoliaAnimationIntensity.Calm -> 0f; FoliaAnimationIntensity.Normal -> 10f; FoliaAnimationIntensity.Chaotic -> 60f }
    val scale = if(intensity == FoliaAnimationIntensity.Chaotic) .8 + random(4) * .6 else 1.1 + random(4) * .2
    val x=(random(1)-.5)*spread;val y=(random(2)-.5)*spread
    // JS config coordinates remain doubles before sin/cos builds the waiting
    // pose. Premature Float rounding is amplified by the larger Chaotic spread.
    return Pose(x.toFloat(),y.toFloat(),scale.toFloat(),
        if(rotation) ((random(3)-.5)*rotate).toFloat() else 0f,
        if(rotation) ((random(8)-.5)*45).toFloat() else 0f,rotation,
        (x+sin(y)*100).toFloat(),(y+cos(x)*50).toFloat())
}

internal data class WordFrame(val x: Float, val y: Float, val scale: Float, val rotation: Float,
    val alpha: Float, val mix: Float, val blur: Float)

/** Closed-form media-time response. Passed starts at the sampled active endpoint, including short words. */
internal fun sampleWord(timeUs: Long, lineEnterUs: Long, startUs: Long, endUs: Long,
    profile: RenderProfile, pose: Pose, reducedMotion: Boolean,
    intensity: FoliaAnimationIntensity = FoliaAnimationIntensity.Normal): WordFrame {
    val begin = maxOf(lineEnterUs, startUs - profile.lookaheadUs)
    val activeEnd = activeWordEnd(startUs, endUs, profile).coerceAtLeast(begin)
    val sampleAt = minOf(timeUs, activeEnd)
    val activeElapsed=motionElapsedUs(sampleAt-begin)
    val passedElapsed=motionElapsedUs(timeUs-activeEnd)
    val elapsed = activeElapsed / 1_000_000.0
    val alpha = easeOut(activeElapsed / 100_000.0)
    val fade = easeOut(passedElapsed / 500_000.0)
    val rotationFade = (passedElapsed / 5_000_000f).coerceIn(0f, 1f)
    val waitingX = pose.waitingX
    val waitingY = pose.waitingY
    fun blend(a: Float, b: Float, factor: Float) = a + (b - a) * factor
    val activeScale = physicsValue(.5f, pose.scale * 1.4f, elapsed)
    // Active and passed keep the same x/y targets. Motion leaves these springs
    // running instead of starting the passed tween used by scale/opacity.
    val positionElapsed=motionElapsedUs(timeUs-begin)/1_000_000.0
    val x = physicsValue(waitingX, pose.x, positionElapsed)
    val y = physicsValue(waitingY, pose.y, positionElapsed)
    // Interlude's active/passed rotation targets are both zero. Like x/y,
    // Motion leaves that spring running across the status change.
    val rotation = if (!pose.rotationEnabled) 0f else physicsValue(pose.rotation + 20, pose.rotation,
        if (pose.passedRotation == 0f) positionElapsed else elapsed)
    val duration = displayDuration(startUs, endUs, profile)
    val colorIn = (activeElapsed.toFloat() / duration).coerceIn(0f, 1f)
    val colorOut = easeInOut(passedElapsed.toDouble() / when(profile.mode) { Reveal.Normal -> 800_000; Reveal.Fast -> 240_000; Reveal.Instant -> 120_000 })
    val blurDuration = when(profile.mode) { Reveal.Normal -> 200_000; Reveal.Fast -> 120_000; Reveal.Instant -> 80_000 }
    val blur = 10 * (1 - easeOut(activeElapsed.toDouble() / blurDuration)) *
        (1 - easeOut(passedElapsed.toDouble() / when(profile.mode) { Reveal.Normal -> 500_000; Reveal.Fast -> 200_000; Reveal.Instant -> 120_000 }))
    return WordFrame(if (reducedMotion) 0f else x,
        if (reducedMotion) 0f else y,
        if (reducedMotion) 1f else blend(activeScale, pose.scale, fade),
        if (reducedMotion) 0f else if (pose.passedRotation == 0f) rotation
            else blend(rotation, pose.rotation + pose.passedRotation, rotationFade),
        if (reducedMotion) 1f else blend(alpha, if(intensity == FoliaAnimationIntensity.Chaotic) .9f else .82f, fade), colorIn * (1 - colorOut),
        if (reducedMotion) 0f else blur)
}
