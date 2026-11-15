// Folia v0.7.26 VisualizerSubtitleOverlay.tsx; ledger in docs/upstream.md.
// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.animation

internal data class SubtitleFrame(val outerAlpha:Float,val outerY:Float,val innerAlpha:Float,val innerY:Float)
internal fun sampleSubtitle(timeUs:Long,enterUs:Long?,hasRows:Boolean,reducedMotion:Boolean):SubtitleFrame {
    if(reducedMotion) return SubtitleFrame(.6f,0f,1f,0f)
    val outer=easeOut(motionElapsedUs(timeUs)/240_000.0)
    val age=motionElapsedUs(timeUs-(enterUs?:0))
    return SubtitleFrame(.6f*outer,20*(1-outer),
        if(hasRows) cubicEase(age/300_000.0,.25,.1,.35,1.0) else 1f,
        if(hasRows) physicsValue(10f,0f,age/1e6,500.0,25.0,10.0) else 0f)
}
