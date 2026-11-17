// Copyright (c) 2026 chthollyphile and contributors
// Adapted from Folia cc6226e4 fumeCameraStep.ts, excluding background parallax.
// SPDX-License-Identifier: AGPL-3.0-only. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import kotlin.math.*

internal data class FumeCameraPose(val x: Double, val y: Double, val vx: Double = 0.0, val vy: Double = 0.0,
    val fx: Double = x, val fy: Double = y, val scale: Double = 1.18, val vs: Double = 0.0, val fs: Double = scale) {
    fun view() = FumeCameraView(x,y,scale)
}
internal data class FumeRetarget(val source: Int = -1, val started: Double = 0.0, val duration: Double = .18,
    val from: FumeCameraView = FumeCameraView(0.0,0.0,1.0), val bridge: String = "none",
    val waypoint: FumeCameraView = FumeCameraView(0.0,0.0,1.0), val phase: Double = .36)
internal data class FumeCameraState(val pose: FumeCameraPose, val retarget: FumeRetarget = FumeRetarget())
internal data class FumeCameraInput(val time: Double, val floatTime: Double, val dt: Double,
    val lineIndex: Int, val viewport: FumeViewport, val overview: FumeCameraView?, val overviewStart: Double,
    val speed: Double = 1.0, val stepped: Boolean = false, val static: Boolean = false, val intensity: String = "normal")
internal data class FumeCameraFrame(val state: FumeCameraState, val overview: Boolean, val restore: Double)
internal fun fumeInitialCamera(article: FumeArticle) = FumeCameraState(FumeCameraPose(article.width*.5,article.height*.5))
private fun cameraMix(a: Double,b: Double,q: Double) = a+(b-a)*q
private fun cameraBezier(a: Double,b: Double,c: Double,q: Double): Double {
    val t=q.coerceIn(0.0,1.0);return (1-t).pow(2)*a+2*(1-t)*t*b+t*t*c
}

/** Resolved target and retarget state, before any time integration. Keeping this separate
 * lets the original finite-step oracle and a reconstructible media response share exactly
 * the same focus, entry bias, float and flight selection. [phase] deliberately precedes a
 * newly selected flight's duration extension, matching the pinned source. */
internal data class FumeCameraIntent(val retarget: FumeRetarget, val target: FumeCameraView,
    val bridgeTarget: FumeCameraView?, val phase: Double, val boost: Double,
    val overview: Boolean, val restore: Double) {
    val bridgeRate: Double get() = if(retarget.bridge=="overview")cameraMix(12.5,22.0,1-phase)
        else cameraMix(10.5,17.5,1-phase)
}

internal fun fumeCameraIntent(before: FumeCameraState, article: FumeArticle,
    geometry: Map<Int,FumeFocusBlock>, input: FumeCameraInput,
    reveals:Map<Int,FumeReveal> = emptyMap()): FumeCameraIntent {
    val time=input.time;val old=before.pose
    var retarget=before.retarget
    val focus=fumeFocusBlock(article,input.lineIndex,time,reveals)
    val overview=input.overview!=null&&time>=input.overviewStart
    var tx=article.width*.5;var ty=article.height*.5;var ts=1.18
    var entry:FumePoint?=null;var changed=false
    fun retarget(source:Int,duration:Double) {
        retarget=FumeRetarget(source,time,duration,old.view());changed=true
    }
    if(overview) {
        tx=input.overview.x;ty=input.overview.y;ts=input.overview.scale
        if(retarget.source != -2)retarget(-2,(fumeOverviewRetarget(input.viewport)/input.speed).coerceIn(.12,1.2))
    } else if(focus!=null) {
        val g=geometry.getValue(focus.sourceIndex);val reveal=reveals[focus.sourceIndex]?:FumeReveal(focus.line)
        val point=if(input.stepped)g.stepped(reveal.count(time))else g.smooth(reveal.progress(time))
        entry=g.entry();tx=point.x;ty=point.y;ts=fumeCameraScale(focus,input.viewport)
        if(retarget.source!=focus.sourceIndex)retarget(focus.sourceIndex,(fumeCameraRetarget(focus.line)/input.speed).coerceIn(.03,.3))
    } else if(retarget.source != -1)retarget(-1,(.18/input.speed).coerceIn(.05,.4))
    val elapsed=max(time-retarget.started,0.0)
    val phase=(elapsed/max(retarget.duration,.001)).coerceIn(0.0,1.0)
    val restore=if(overview&&retarget.source == -2)fumeCameraEase(phase)else 0.0
    val boost=(1-phase).pow(3)
    val entryBias=boost.pow(.58)
    entry?.let { tx=cameraMix(tx,it.x,entryBias);ty=cameraMix(ty,it.y,entryBias) }
    if(!input.static) {
        val distance=when(input.intensity){"chaotic"->24.0;"calm"->14.0;else->18.0}
        val duration=when(input.intensity){"chaotic"->5.8;"calm"->8.5;else->7.0}
        val amplitude=when(input.intensity){"chaotic"->.014;"calm"->.008;else->.011}
        val p=input.floatTime/duration*PI*2
        val attenuation=if(overview).36 else 1.0
        val floatX=sin(p*.74+.8)*distance*.34
        val floatY=(sin(p)*distance+sin(p*.5+1.1)*distance*.22)*attenuation
        val divisor=max(ts,.001)
        tx-=floatX/divisor;ty-=floatY/divisor
        ts=(ts*(1+sin(p+.9)*amplitude*attenuation)).coerceIn(.22,2.24)
    }
    if(changed) {
        val scale=maxOf(old.scale,ts,.001)
        val screen=hypot(abs(tx-retarget.from.x)*scale,abs(ty-retarget.from.y)*scale)
        retarget=retarget.copy(bridge=if(screen>=min(input.viewport.width,input.viewport.height)*.42)"direct"else"none",
            waypoint=FumeCameraView(tx,ty,ts),phase=.5)
        if(retarget.source>=0)fumeOverviewBridge(retarget.from,FumeCameraView(tx,ty,ts),input.overview,input.viewport)?.let { bridge ->
            retarget=retarget.copy(bridge="overview",waypoint=FumeCameraView(bridge.x,bridge.y,bridge.scale),phase=bridge.phase,
                duration=max(retarget.duration,(bridge.duration/input.speed).coerceIn(.16,.9)))
        }
    }
    val bridgeTarget=if(retarget.bridge!="none"&&phase<1) {
        val q=if(retarget.bridge=="overview")1-(1-phase).pow(3)else fumeCameraEase(phase)
        fun bridge(a:Double,w:Double,b:Double)=if(retarget.bridge=="overview")cameraBezier(a,w,b,q)else cameraMix(a,b,q)
        FumeCameraView(bridge(retarget.from.x,retarget.waypoint.x,tx),
            bridge(retarget.from.y,retarget.waypoint.y,ty),bridge(retarget.from.scale,retarget.waypoint.scale,ts))
    }else null
    return FumeCameraIntent(retarget,FumeCameraView(tx,ty,ts),bridgeTarget,phase,boost,overview,restore)
}

/** Exact original one-step operator, useful for source trajectory comparison. Pure in/out state.
 * NOT a playback clock or a deterministic seek implementation on its own. Do not call once per
 * display frame as the public media model; the media-track layer owns reconstruction. */
internal fun fumeCameraStep(before: FumeCameraState, article: FumeArticle,
    geometry: Map<Int,FumeFocusBlock>, input: FumeCameraInput): FumeCameraFrame {
    val intent=fumeCameraIntent(before,article,geometry,input)
    val old=before.pose;val dt=input.dt;val retarget=intent.retarget
    val tx=intent.target.x;val ty=intent.target.y;val ts=intent.target.scale
    val phase=intent.phase;val boost=intent.boost
    val distance=hypot(tx-old.x,ty-old.y)
    val pose=intent.bridgeTarget?.let { bridge ->
        val bx=bridge.x;val by=bridge.y;val bs=bridge.scale
        val rate=if(retarget.bridge=="overview")cameraMix(12.5,22.0,1-phase)else cameraMix(10.5,17.5,1-phase)
        val catch=1-exp(-dt*rate)
        FumeCameraPose(old.x+(bx-old.x)*catch,old.y+(by-old.y)*catch,old.vx*.72,old.vy*.72,bx,by,
            (old.scale+(bs-old.scale)*catch).coerceIn(.22,2.24),old.vs*.68,bs)
    } ?: run {
        val boosted=(4.8/max(retarget.duration,.05)).coerceIn(20.0,54.0)
        val catch=1-exp(-dt*cameraMix(11.2,boosted,boost))
        val fx=old.fx+(tx-old.fx)*catch;val fy=old.fy+(ty-old.fy)*catch
        val fs=old.fs+(ts-old.fs)*(1-exp(-dt*cameraMix(5.4,12.8,boost)))
        val strength=cameraMix(208.0,(15.8/max(retarget.duration.pow(2),.0064)).coerceIn(260.0,780.0),boost)
        val damping=cameraMix(24.0,(sqrt(strength)*1.36).coerceIn(24.0,40.0),boost)
        val limit=cameraMix(1320.0,(distance/max(retarget.duration*.28,.028)).coerceIn(2600.0,8800.0),boost)
        val vx=(old.vx+((fx-old.x)*strength-old.vx*damping)*dt).coerceIn(-limit,limit)
        val vy=(old.vy+((fy-old.y)*strength-old.vy*damping)*dt).coerceIn(-limit,limit)
        val vs=(old.vs+((fs-old.scale)*cameraMix(54.0,108.0,boost)-old.vs*cameraMix(13.5,21.0,boost))*dt).coerceIn(-1.6,1.6)
        FumeCameraPose(old.x+vx*dt,old.y+vy*dt,vx,vy,fx,fy,(old.scale+vs*dt).coerceIn(.22,2.24),vs,fs)
    }
    return FumeCameraFrame(FumeCameraState(pose,retarget),intent.overview,intent.restore)
}
