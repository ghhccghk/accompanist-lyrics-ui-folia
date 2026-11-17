// Copyright (c) 2026 chthollyphile and contributors
// Media-time adaptation of VisualizerCadenza.tsx's exponential RAF filters.
// SPDX-License-Identifier: AGPL-3.0-only. Source and measured residual: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import kotlin.math.*

private fun CadenzaWordState.vector()=doubleArrayOf(x,y,rotation,scale,bodyAlpha,blur,activeMix,glowAlpha)
private fun DoubleArray.state()=CadenzaWordState(this[0],this[1],this[2],this[3],this[4],this[5],this[6],this[7])
private data class CadenzaMotionNode(val time:Double,val value:DoubleArray)
private val responseRates=doubleArrayOf(11.0,11.0,11.0,11.0,14.0,14.0,14.0,16.0)

/** Exact exponential integral of a cubic plus a sinusoid on each source target interval.
 * Nodes occur at lyric/status/curve events, NEVER a frame cadence. Queries do not mutate
 * nodes or depend on the previous displayed frame. Original right-endpoint RAF sampling
 * is not identical to this continuous forcing; comparisons report that residual. */
internal class CadenzaMotionTrack(private val line:CadenzaLyric,private val placements:List<CadenzaPlacement>,
    private val width:Double,private val height:Double,private val intensity:CadenzaIntensity=CadenzaIntensity.NORMAL,
    private val motionAmount:Double=1.0,private val startTime:Double=line.start,
    private val endTime:Double=max(line.end,cadenzaTiming(line).end)) {
    private val timing=cadenzaTiming(line)
    private val nodes:List<List<CadenzaMotionNode>>
    val preparedNodeCount:Int get()=nodes.sumOf{it.size}
    init {
        require(startTime.isFinite()&&endTime.isFinite()&&endTime>=startTime)
        require(width.isFinite()&&width>0&&height.isFinite()&&height>0&&motionAmount.isFinite())
        require(placements.map{it.id}.distinct().size==placements.size){"Cadenza placements must have unique ids"}
        nodes=placements.map{p->
            val times=events(p)
            var value=initial(p)
            buildList {
                add(CadenzaMotionNode(startTime,value))
                for((a,b)in times.zipWithNext()) {
                    value=advance(p,value,a,b);add(CadenzaMotionNode(b,value))
                }
            }
        }
    }
    private fun target(p:CadenzaPlacement,t:Double)=cadenzaTarget(t,line,p,width,height,intensity,motionAmount)
    private fun initial(p:CadenzaPlacement):DoubleArray {
        val q=target(p,startTime).state.vector()
        val instant=timing.reveal==CadenzaReveal.INSTANT&&startTime>=p.fragment.word.start
        val initial=doubleArrayOf(width/2+p.x+p.entryX,height*.42+sin(startTime*2.3)*3+p.y+p.entryY,
            if(instant)q[2]else q[2]+16,if(instant)q[3]else max(p.scale*.5,.5),
            if(instant)q[4]else 0.0,if(instant)q[5]else 10.0,if(instant)q[6]else 0.0,if(instant)q[7]else 0.0)
        // The source uses one immediate 1/60 update at effect mount. This only
        // identifies that initial response, not a native frame/playback driver.
        return DoubleArray(8){i->initial[i]+(q[i]-initial[i])*(-expm1(-responseRates[i]/60))}
    }
    private fun events(p:CadenzaPlacement):List<Double> {
        val w=p.fragment.word;val fast=timing.reveal==CadenzaReveal.FAST
        val lookahead=when(timing.reveal){CadenzaReveal.FAST->.045;CadenzaReveal.INSTANT->0.0;else->.18}
        val d=max(w.end-w.start,if(fast).045 else .1)
        return (listOf(startTime,endTime,timing.end,w.start-lookahead,w.start,w.end,
            w.start+max(w.end-w.start,if(fast).045 else .01),
            w.start+d*(if(fast).14 else .18),w.start+d*(if(fast).82 else .9),w.start+d,
            w.start+.067*.3,w.start+.067,w.end+.12,w.end+.14,w.end+.8,w.end+.9,w.end+2.5,w.end+5))
            .filter{it.isFinite()&&it>=startTime&&it<=endTime}.distinct().sorted()
    }
    private fun advance(p:CadenzaPlacement,initial:DoubleArray,a:Double,b:Double):DoubleArray {
        val duration=b-a;if(duration<=0)return initial.copyOf()
        // Each interval's nonoscillatory targets are degree<=3. Interior points
        // avoid assigning a source <=/> endpoint value to an entire interval.
        val samples=List(4){j->val t=a+duration*(j*.25+.125);target(p,t).state.vector()}
        val active=target(p,a+duration*.5).status==CadenzaStatus.ACTIVE&&timing.reveal!=CadenzaReveal.INSTANT
        val result=DoubleArray(8)
        for(i in 0..7) {
            val rate=responseRates[i]
            val amplitude=when { i==1->3.0;i==3&&active->p.scale*1.3*.04*motionAmount;else->0.0 }
            val omega=if(i==1)2.3 else 10.0
            val phase=if(i==1)0.0 else p.fragment.word.start*5
            fun wave(t:Double)=amplitude*sin(omega*t+phase)
            val f=DoubleArray(4){j->samples[j][i]-wave(a+duration*(j*.25+.125))}
            val d1=f[1]-f[0];val d2=f[2]-2*f[1]+f[0];val d3=f[3]-3*f[2]+3*f[1]-f[0]
            val c=doubleArrayOf(f[0]-.5*d1+.375*d2-.3125*d3,4*d1-4*d2+23.0/6*d3,8*d2-12*d3,32.0/3*d3)
            val x=rate*duration;val decay=exp(-x)
            var integral=0.0
            for(k in 0..3)integral+=c[k]*cadenzaExponentialMoment(x,k)
            if(amplitude!=0.0) {
                fun primitive(t:Double)=rate*sin(omega*t+phase)-omega*cos(omega*t+phase)
                integral+=amplitude*rate/(rate*rate+omega*omega)*(primitive(b)-decay*primitive(a))
            }
            result[i]=decay*initial[i]+integral
            check(result[i].isFinite()){"Nonfinite Cadenza media response"}
        }
        return result
    }
    fun sample(time:Double):List<CadenzaWordTarget> {
        require(time.isFinite())
        val t=time.coerceIn(startTime,endTime)
        return placements.mapIndexed{i,p->
            val prepared=nodes[i];var low=0;var high=prepared.size
            while(low<high){val m=(low+high)ushr 1;if(prepared[m].time<=t)low=m+1 else high=m}
            val n=prepared[max(low-1,0)]
            val value=if(t==n.time)n.value else advance(p,n.value,n.time,t)
            target(p,t).copy(state=value.state())
        }.sortedBy{it.status.ordinal}
    }
}

/** x * integral_0^1 exp(-x*(1-u)) u^degree du; stable at vanishing durations. */
internal fun cadenzaExponentialMoment(x:Double,degree:Int):Double {
    require(x.isFinite()&&x>=0&&degree in 0..3)
    if(x==0.0)return 0.0
    if(x<.5) {
        var term=x/(degree+1);var sum=term
        for(n in 0..40){term*=-x/(degree+n+2);sum+=term;if(abs(term)<1e-18*abs(sum))break}
        return sum
    }
    var value=-expm1(-x)
    for(k in 1..degree)value=1-k/x*value
    return value
}
