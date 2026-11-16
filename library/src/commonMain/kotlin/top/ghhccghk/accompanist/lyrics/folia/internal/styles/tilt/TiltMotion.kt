// Copyright (c) 2026 chthollyphile
// VisualizerTilt.tsx segment/character variants, fixed cc6226e4. SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt

import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*

internal data class TiltRowFrame(val alpha:Float,val y:Float,val scale:Float)
internal fun tiltEase(progress:Double)=cubicEase(progress,.25,.46,.45,.94)

internal fun sampleTiltLineOpacity(time:Long,enterUs:Long,leaveUs:Long?,reduced:Boolean):Float {
    val enter=if(reduced)1f else easeOut(motionElapsedUs(time-enterUs)/300_000.0)
    val exit=if(leaveUs!=null && time>=leaveUs)easeInOut(motionElapsedUs(time-leaveUs)/450_000.0) else 0f
    return enter*(1-exit)
}

/** Reconstruct the interrupted row tween, including its own exit under the 450ms parent fade. */
internal fun sampleTiltRow(time:Long,visibleUs:Long,leaveUs:Long?,tilt:Boolean,reduced:Boolean):TiltRowFrame {
    val at=minOf(time,leaveUs?:time)
    val duration=if(tilt)600_000.0 else 550_000.0
    val q=if(reduced)if(at>=visibleUs)1f else 0f else tiltEase(motionElapsedUs(at-visibleUs)/duration)
    val exit=if(leaveUs!=null && time>=leaveUs)tiltEase(motionElapsedUs(time-leaveUs)/duration) else 0f
    val y=if(reduced)0f else (if(tilt)24 else 20)*(1-q)
    val scale=if(tilt && !reduced).92f+.08f*q else 1f
    return TiltRowFrame(q*(1-exit),if(reduced)0f else y+((if(tilt)-16 else -12)-y)*exit,
        scale+((if(tilt && !reduced).95f else 1f)-scale)*exit)
}

internal data class TiltCharFrame(val alpha:Float,val y:Float,val scale:Float)
internal fun sampleTiltChar(time:Long,visibleUs:Long,tilt:Boolean,visualIndex:Int,space:Boolean,
    timing:TiltCharTime?,yOffset:Double,reduced:Boolean):TiltCharFrame {
    val delay=if(space)0L else visualIndex*(if(tilt)50_000L else 40_000L)
    val q=if(reduced)if(time>=visibleUs)1f else 0f else
        tiltEase(motionElapsedUs(time-visibleUs-delay)/500_000.0)
    val pulse=if(reduced||space||timing==null||time<visibleUs)0f else
        (tiltPulse(time/1e6,timing.start,timing.end)*(if(tilt).18 else .15)).toFloat()
    val y=if(reduced||space||!tilt)0f else ((if(visualIndex%2==0)-1 else 1)*yOffset*(2-q)).toFloat()
    return TiltCharFrame(q,y,1+pulse)
}
