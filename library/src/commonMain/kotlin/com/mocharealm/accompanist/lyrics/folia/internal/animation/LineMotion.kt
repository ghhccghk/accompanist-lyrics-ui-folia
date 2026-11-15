// Folia glowWordTiming.ts + classic/Visualizer.tsx; pinned source ledger: docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package com.mocharealm.accompanist.lyrics.folia.internal.animation

import kotlin.math.*

internal data class LineFrame(val alpha: Float, val scale: Float, val breathingY: Float, val blur: Float)
internal fun sampleLine(time: Long, enterUs: Long, leaveUs: Long?, profile: RenderProfile,
    reducedMotion: Boolean, breathing: Float, density: Float): LineFrame {
    val elapsed = motionElapsedUs(minOf(time, leaveUs ?: time) - enterUs)
    val exitTime = motionElapsedUs(time - (leaveUs ?: time))
    val enter = when(profile.mode) {
        Reveal.Instant -> 1f
        Reveal.Fast -> easeOut(elapsed / 160_000.0)
        Reveal.Normal -> cubicEase(elapsed / 300_000.0, .25, .1, .35, 1.0)
    }
    val initialAlpha = when(profile.mode) { Reveal.Normal -> 0f; Reveal.Fast -> .35f; Reveal.Instant -> 1f }
    val initialScale = when(profile.mode) { Reveal.Normal -> .9f; Reveal.Fast -> .96f; Reveal.Instant -> 1f }
    val initialBlur = when(profile.mode) { Reveal.Normal -> 10f; Reveal.Fast -> 4f; Reveal.Instant -> 0f }
    // Framer's default scale transition is a spring (550/30), distinct from opacity/filter.
    val enteredScale = if(profile.mode == Reveal.Normal) physicsValue(.9f,1f,elapsed/1e6,550.0,30.0,10.0)
        else initialScale+(1-initialScale)*enter
    val exit = when(profile.mode) {
        Reveal.Normal -> easeOut(exitTime / 300_000.0)
        Reveal.Fast -> easeInOut(exitTime / 160_000.0)
        Reveal.Instant -> easeOut(exitTime / 120_000.0)
    }
    val exitScale = when(profile.mode) { Reveal.Normal -> 1.1f; Reveal.Fast -> 1.04f; Reveal.Instant -> 1.02f }
    val exitBlur = when(profile.mode) { Reveal.Normal -> 20f; Reveal.Fast -> 10f; Reveal.Instant -> 6f }
    fun blend(a:Float,b:Float,q:Float) = a+(b-a)*q
    val breathQ = ((time.coerceAtLeast(0) % 7_000_000) / 1_750_000f)
    val segment = floor(breathQ).toInt().coerceIn(0,3)
    val q = easeInOut(breathQ-segment)
    val ys = floatArrayOf(0f,-14f,0f,6.3f,0f)
    val scales = floatArrayOf(1f,1.01f,1f,.995f,1f)
    val breathScale = 1 + (blend(scales[segment],scales[segment+1],q)-1)*breathing
    return LineFrame(if(reducedMotion) 1-exit else blend(initialAlpha,1f,enter)*(1-exit),
        if(reducedMotion) 1f else blend(enteredScale,exitScale,exit)*breathScale,
        if(reducedMotion) 0f else blend(ys[segment],ys[segment+1],q)*breathing*density,
        if(reducedMotion) 0f else blend(initialBlur*(1-enter),exitBlur,exit)*density)
}
