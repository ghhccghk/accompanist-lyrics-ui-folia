// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 CappellaMessageRow.tsx/CappellaBubbleParts.tsx/cappellaIntensity.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Independently evaluated ODE/tweens; no Motion runtime code bundled.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import kotlin.math.*

internal data class CappellaTransition(val enterY:Double,val enterScale:Double,val enterDuration:Double,
    val exitY:Double,val exitScale:Double,val exitDuration:Double,val stiffness:Double,val damping:Double,val mass:Double)
internal fun cappellaTransition(i:CappellaIntensity)=when(i) {
    CappellaIntensity.CALM -> CappellaTransition(14.0,.992,.28,-10.0,.985,.22,280.0,30.0,.78)
    CappellaIntensity.NORMAL -> CappellaTransition(22.0,.98,.32,-18.0,.965,.24,340.0,28.0,.72)
    CappellaIntensity.CHAOTIC -> CappellaTransition(30.0,.968,.38,-26.0,.94,.28,360.0,24.0,.68)
}
internal data class CappellaScalar(val value:Double,val velocity:Double,val done:Boolean)

/** Analytic damped oscillator. Samples depend on media elapsed time, never the
 * number/order of calls. Includes the slightly overdamped original calm profile. */
internal class CappellaSpring(private val from:Double,private val to:Double,
    private val initialVelocity:Double,private val config:CappellaTransition) {
    private val a=config.damping/(2*config.mass)
    private val omega2=config.stiffness/config.mass
    private val discriminant=a*a-omega2
    private val restSpeed=if(abs(to-from)<5).01 else 2.0
    private val restDelta=if(abs(to-from)<5).005 else .5
    private fun raw(seconds:Double):Pair<Double,Double> {
        val t=seconds.coerceAtLeast(0.0);val y=from-to
        if(discriminant>0) {
            val b=sqrt(discriminant);val slow=-a+b;val fast=-a-b
            val c=(initialVelocity-fast*y)/(slow-fast);val f=y-c
            val s=c*exp(slow*t);val q=f*exp(fast*t)
            return to+s+q to slow*s+fast*q
        }
        if(discriminant<0) {
            val b=sqrt(-discriminant);val c=(initialVelocity+a*y)/b
            val cos=cos(b*t);val sin=sin(b*t);val e=exp(-a*t)
            return to+e*(y*cos+c*sin) to e*((-a*y+b*c)*cos+(-a*c-b*y)*sin)
        }
        val c=initialVelocity+a*y;val e=exp(-a*t)
        return to+e*(y+c*t) to e*(c-a*(y+c*t))
    }
    fun generator(seconds:Double):CappellaScalar {
        val(x,v)=raw(seconds)
        val done=abs(x-to)<=restDelta&&abs(v)<=restSpeed
        return CappellaScalar(if(done)to else x,v,done)
    }
    // Motion resolves duration with 50ms probes. This is a one-time resolution
    // of the analytic response, not a playback/render clock or frame replay.
    val duration:Double=(0..400).firstOrNull{generator(it*.05).done}?.times(.05)?:Double.POSITIVE_INFINITY
    fun sample(seconds:Double):CappellaScalar {
        val t=seconds.coerceAtLeast(0.0)
        if(t>=duration)return CappellaScalar(to,0.0,true)
        return generator(t).let{if(it.done)it.copy(velocity=0.0)else it}
    }
}

internal enum class CappellaCurve { ENTER,EXIT,RESIZE,TIMESTAMP,SPRING }
private fun cappellaTweenDuration(curve:CappellaCurve,config:CappellaTransition)=when(curve) {
    CappellaCurve.ENTER->config.enterDuration
    CappellaCurve.EXIT->config.exitDuration
    CappellaCurve.RESIZE->.2
    CappellaCurve.TIMESTAMP->.18
    CappellaCurve.SPRING->error("Physical response has no fixed duration")
}
/** Continuous media-time response of the actual generator. Motion's ordinary
 * RAF tick quantizes elapsed milliseconds, while sample() explicitly bypasses
 * that quantization; the upstream fixture records the latter without rounding. */
internal fun cappellaTween(from:Double,to:Double,seconds:Double,curve:CappellaCurve,
    config:CappellaTransition):CappellaScalar {
    require(curve!=CappellaCurve.SPRING)
    val t=seconds.coerceAtLeast(0.0)
    val duration=cappellaTweenDuration(curve,config)
    val q=(t/duration).coerceIn(0.0,1.0)
    val p=if(curve==CappellaCurve.EXIT)1-q else q
    var lo=0.0;var hi=1.0;var u=.5
    // Retain Motion's bounded cubic inversion, distinct from browser CSS's
    // higher-precision curve in the glyph/sweep calibration.
    for(i in 0 until 12){u=(lo+hi)/2;val x=3*(1-u)*u*u*.58+u*u*u
        if(abs(x-p)<=1e-7)break
        if(x<p)lo=u else hi=u
    }
    if(p==0.0||p==1.0)u=p
    val ease=3*(1-u)*u*u+u*u*u
    val eased=if(curve==CappellaCurve.EXIT)1-ease else ease
    // dy/dx of ease-out, cancelling u to keep the q=0 limit finite.
    val derivative=6*(1-u)/(3.48-2.22*u)
    val velocity=if(t>=duration)0.0 else (to-from)*derivative/duration
    return CappellaScalar(from+(to-from)*eased,velocity,t>=duration)
}

/** Immutable event-prepared scalar response. Unchanged targets never restart a
 * running curve. Retargets retain position and analytic spring velocity. */
internal class CappellaScalarTrack(initial:Double,events:List<Pair<Double,Double>>,curve:CappellaCurve,
    config:CappellaTransition) {
    private class Segment(val time:Double,val target:Double,val evaluate:(Double)->CappellaScalar)
    private val segments:List<Segment>
    private val initial=initial
    init {
        require(events.all{it.first.isFinite()&&it.second.isFinite()})
        require(events.zipWithNext().all{(a,b)->a.first<=b.first})
        val prepared=mutableListOf<Segment>();var target=initial
        for((time,next)in events) {
            if(next==target)continue
            val state=prepared.lastOrNull()?.let{it.evaluate(time-it.time)}?:CappellaScalar(initial,0.0,true)
            val evaluate=if(curve==CappellaCurve.SPRING) {
                val spring=CappellaSpring(state.value,next,state.velocity,config);
                {elapsed:Double->spring.sample(elapsed)}
            }else {elapsed:Double->cappellaTween(state.value,next,elapsed,curve,config)}
            prepared+=Segment(time,next,evaluate);target=next
        }
        segments=prepared
    }
    fun at(time:Double):CappellaScalar {
        var lo=0;var hi=segments.size
        while(lo<hi){val mid=(lo+hi)/2;if(segments[mid].time<=time)lo=mid+1 else hi=mid}
        return if(lo==0)CappellaScalar(initial,0.0,true)else segments[lo-1].let{it.evaluate(time-it.time)}
    }
}
