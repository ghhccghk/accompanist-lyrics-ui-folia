// Adapted from Folia v0.7.26 (cc6226e...), chthollyphile and contributors.
// Original files and modifications: docs/upstream.md. SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.animation

import kotlin.math.*

internal enum class Reveal { Normal, Fast, Instant }
internal data class RenderProfile(val mode: Reveal, val renderEndUs: Long, val lookaheadUs: Long)

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
    return if(abs(velocity) <= (restSpeedOverride ?: if(granular) .01 else 2.0) && abs(remaining) <= if(granular) .005 else .5) to
        else (to-remaining).toFloat()
}

internal data class Pose(val x: Float, val y: Float, val scale: Float, val rotation: Float,
    val passedRotation: Float)
internal fun wordPose(startUs: Long, index: Int, rotation: Boolean): Pose {
    val seed = startUs / 1_000_000.0 + index
    fun random(offset: Int): Float { val n = sin(seed + offset) * 10000; return (n - floor(n)).toFloat() }
    return Pose((random(1) - .5f) * 40, (random(2) - .5f) * 40, 1.1f + random(4) * .2f,
        if (rotation) (random(3) - .5f) * 10 else 0f, if (rotation) (random(8) - .5f) * 45 else 0f)
}

internal data class WordFrame(val x: Float, val y: Float, val scale: Float, val rotation: Float,
    val alpha: Float, val mix: Float, val blur: Float)

/** Closed-form media-time response. Passed starts at the sampled active endpoint, including short words. */
internal fun sampleWord(timeUs: Long, lineEnterUs: Long, startUs: Long, endUs: Long,
    profile: RenderProfile, pose: Pose, reducedMotion: Boolean): WordFrame {
    val begin = maxOf(lineEnterUs, startUs - profile.lookaheadUs)
    val activeEnd = activeWordEnd(startUs, endUs, profile).coerceAtLeast(begin)
    val sampleAt = minOf(timeUs, activeEnd)
    val elapsed = (sampleAt - begin).coerceAtLeast(0) / 1_000_000.0
    val alpha = easeOut((sampleAt - begin) / 100_000f)
    val fade = easeOut((timeUs - activeEnd) / 500_000f)
    val rotationFade = ((timeUs - activeEnd) / 5_000_000f).coerceIn(0f, 1f)
    val waitingX = pose.x + sin(pose.y) * 100
    val waitingY = pose.y + cos(pose.x) * 50
    fun blend(a: Float, b: Float, factor: Float) = a + (b - a) * factor
    val activeScale = physicsValue(.5f, pose.scale * 1.4f, elapsed)
    val x = physicsValue(waitingX, pose.x, elapsed)
    val y = physicsValue(waitingY, pose.y, elapsed)
    val rotation = if (pose.rotation == 0f && pose.passedRotation == 0f) 0f else physicsValue(pose.rotation + 20, pose.rotation, elapsed)
    val duration = displayDuration(startUs, endUs, profile)
    val colorIn = ((sampleAt - begin).toFloat() / duration).coerceIn(0f, 1f)
    val colorOut = easeInOut((timeUs - activeEnd).toFloat() / when(profile.mode) { Reveal.Normal -> 800_000; Reveal.Fast -> 240_000; Reveal.Instant -> 120_000 })
    val blurDuration = when(profile.mode) { Reveal.Normal -> 200_000; Reveal.Fast -> 120_000; Reveal.Instant -> 80_000 }
    val blur = 10 * (1 - easeOut((sampleAt - begin).toFloat() / blurDuration)) *
        (1 - easeOut((timeUs - activeEnd).toFloat() / when(profile.mode) { Reveal.Normal -> 500_000; Reveal.Fast -> 200_000; Reveal.Instant -> 120_000 }))
    return WordFrame(if (reducedMotion) 0f else blend(x, pose.x, fade),
        if (reducedMotion) 0f else blend(y, pose.y, fade),
        if (reducedMotion) 1f else blend(activeScale, pose.scale, fade),
        if (reducedMotion) 0f else blend(rotation, pose.rotation + pose.passedRotation, rotationFade),
        if (reducedMotion) 1f else blend(alpha, .82f, fade), colorIn * (1 - colorOut),
        if (reducedMotion) 0f else blur)
}
