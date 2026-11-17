// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only. Shared analytic response; docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.motion

import kotlin.math.*

internal data class SpringSample(val value:Double,val velocity:Double,val done:Boolean)

/** Analytic damped oscillator. Samples depend on media elapsed time, never the
 * number/order of calls. Includes the slightly overdamped original calm profile. */
internal class DampedSpring(private val from:Double,private val to:Double,
    private val initialVelocity:Double,stiffness:Double,damping:Double,mass:Double) {
    private val a=damping/(2*mass)
    private val omega2=stiffness/mass
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
    fun generator(seconds:Double):SpringSample {
        val(x,v)=raw(seconds)
        val done=abs(x-to)<=restDelta&&abs(v)<=restSpeed
        return SpringSample(if(done)to else x,v,done)
    }
    // Motion resolves duration with 50ms probes. This is a one-time resolution
    // of the analytic response, not a playback/render clock or frame replay.
    val duration:Double=(0..400).firstOrNull{generator(it*.05).done}?.times(.05)?:Double.POSITIVE_INFINITY
    fun sample(seconds:Double):SpringSample {
        val t=seconds.coerceAtLeast(0.0)
        if(t>=duration)return SpringSample(to,0.0,true)
        return generator(t).let{if(it.done)it.copy(velocity=0.0)else it}
    }
}
