// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only. Shared analytic response; docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.motion

import kotlin.math.*

internal data class SpringSample(val value:Double,val velocity:Double,val done:Boolean)

/** Analytic damped oscillator. Samples depend on media elapsed time, never the
 * number/order of calls. Includes the slightly overdamped original calm profile. */
internal class DampedSpring(private val from:Double,private val to:Double,
    private val initialVelocity:Double,stiffness:Double,damping:Double,mass:Double,
    private val restDelta:Double=if(abs(to-from)<5).005 else .5,
    private val restSpeed:Double=if(abs(to-from)<5).01 else 2.0) {
    private val a=damping/(2*mass)
    private val omega2=stiffness/mass
    private val discriminant=a*a-omega2
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

    /** Media-domain follower policy: latch the FIRST physical entry into both
     * original rest limits. Unlike JSAnimation's 50ms duration resolution, an
     * original FollowAnimation tests the generator directly on each RAF.
     * Its first observed rest depends on RAF scheduling; this continuous first
     * entry is explicit, reconstructible and independent of display frames. */
    val continuousDuration:Double by lazy { firstRestTime() }
    fun sampleContinuous(seconds:Double):SpringSample {
        val t=seconds.coerceAtLeast(0.0)
        if(t>=continuousDuration)return SpringSample(to,0.0,true)
        val(x,v)=raw(t)
        return SpringSample(x,v,false)
    }

    private fun firstRestTime():Double {
        require(a>0&&omega2>0&&restDelta>0&&restSpeed>0)
        val y=from-to
        val events=mutableListOf(0.0)
        val horizon:Double
        fun exponentialRoot(c:Double,f:Double,slow:Double,fast:Double) {
            val ratio=-f/c
            if(ratio>0){val t=ln(ratio)/(slow-fast);if(t>0&&t.isFinite())events+=t}
        }
        if(discriminant<0) {
            val b=sqrt(-discriminant);val s=(initialVelocity+a*y)/b
            val vc=initialVelocity;val vs=-a*s-b*y
            horizon=max(0.0,max(ln(max(1.0,hypot(y,s)/restDelta)),
                ln(max(1.0,hypot(vc,vs)/restSpeed)))/a)
            fun zeros(c:Double,s:Double) {
                if(c==0.0&&s==0.0)return
                val phase=atan2(s,c)+PI/2
                val first=ceil(-phase/PI).toInt();val last=floor((b*horizon-phase)/PI).toInt()
                for(n in first..last){val t=(phase+n*PI)/b;if(t>0&&t<horizon)events+=t}
            }
            // Zeros and extrema make BOTH absolute constraints monotone on
            // every interval, including very short first-rest windows.
            zeros(y,s);zeros(vc,vs);zeros(-a*vc+b*vs,-a*vs-b*vc)
        } else if(discriminant>0) {
            val b=sqrt(discriminant);val slow=-a+b;val fast=-a-b
            val c=(initialVelocity-fast*y)/(slow-fast);val f=y-c
            horizon=max(0.0,max(ln(max(1.0,(abs(c)+abs(f))/restDelta)),
                ln(max(1.0,(abs(slow*c)+abs(fast*f))/restSpeed)))/-slow)
            exponentialRoot(c,f,slow,fast)
            exponentialRoot(slow*c,fast*f,slow,fast)
            exponentialRoot(slow*slow*c,fast*fast*f,slow,fast)
        } else {
            val c=initialVelocity+a*y
            fun zero(p:Double,q:Double){val t=-p/q;if(t>0&&t.isFinite())events+=t}
            zero(y,c);zero(initialVelocity,-a*c);zero(-2*a*initialVelocity-omega2*y,a*a*c)
            var h=max(1.0,1/a)
            while(exp(-a*h)*(abs(y)+abs(c)*h)>restDelta||
                exp(-a*h)*(abs(initialVelocity)+abs(a*c)*h)>restSpeed)h*=2
            horizon=h
        }
        events+=horizon
        fun constraint(t:Double,position:Boolean):Double {
            val(x,v)=raw(t)
            return if(position)abs(x-to)-restDelta else abs(v)-restSpeed
        }
        fun root(l:Double,r:Double,position:Boolean):Double {
            var low=l;var high=r;val leftInside=constraint(l,position)<=0
            repeat(60){val middle=(low+high)/2
                if((constraint(middle,position)<=0)==leftInside)low=middle else high=middle}
            return (low+high)/2
        }
        for((left,right)in events.filter{it in 0.0..horizon}.distinct().sorted().zipWithNext()) {
            var lo=left;var hi=right
            for(position in listOf(true,false)) {
                val insideLeft=constraint(left,position)<=0;val insideRight=constraint(right,position)<=0
                if(!insideLeft&&!insideRight){lo=Double.POSITIVE_INFINITY;break}
                if(insideLeft!=insideRight){val cross=root(left,right,position)
                    if(insideLeft)hi=min(hi,cross)else lo=max(lo,cross)}
            }
            if(lo<=hi)return lo
        }
        return horizon
    }
}
