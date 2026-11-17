// Copyright (c) 2026 chthollyphile and contributors
// VisualizerCadenza.tsx target-state equations, pinned cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only. Source/adaptations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

import kotlin.math.*

internal data class CadenzaWordState(val x: Double, val y: Double, val rotation: Double, val scale: Double,
    val bodyAlpha: Double, val blur: Double, val activeMix: Double, val glowAlpha: Double)
internal data class CadenzaWordTarget(val placement: CadenzaPlacement, val status: CadenzaStatus,
    val progress: Double, val state: CadenzaWordState)
internal data class CadenzaRipple(val x:Double,val y:Double,val radius:Double,val alpha:Double,val stroke:Double=1.2)

/** Original Canvas ring is in stage space, outside the rotated/scaled DOM word. */
internal fun cadenzaChorusRipple(line:CadenzaLyric,target:CadenzaWordTarget):CadenzaRipple? {
    val p=target.placement;val s=target.state
    if(!line.chorus||p.interlude||target.status!=CadenzaStatus.ACTIVE||s.bodyAlpha<.015&&s.glowAlpha<.015)return null
    val width=p.width*s.scale;val height=p.height*s.scale
    return CadenzaRipple(s.x+p.width/2,s.y-height*.42,max(width,height*1.08)*(.55+target.progress*.45),
        .45*(1-target.progress)*s.bodyAlpha)
}

/** Pure target state only: the source's RAF smoothing is a distinct, still required layer. */
internal fun cadenzaTargets(time: Double, line: CadenzaLyric, placements: List<CadenzaPlacement>,
    width: Double, height: Double, intensity: CadenzaIntensity = CadenzaIntensity.NORMAL,
    motionAmount: Double = 1.0): List<CadenzaWordTarget> {
    return placements.map{cadenzaTarget(time,line,it,width,height,intensity,motionAmount)}.sortedBy{it.status.ordinal}
}
internal fun cadenzaTarget(time: Double, line: CadenzaLyric, p: CadenzaPlacement, width: Double, height: Double,
    intensity: CadenzaIntensity=CadenzaIntensity.NORMAL, motionAmount: Double=1.0): CadenzaWordTarget {
        val timing=cadenzaTiming(line);val instant=timing.reveal==CadenzaReveal.INSTANT
        val status=cadenzaWordStatus(time,timing,p.fragment.word)
        val word=p.fragment.word
        val pulse=if(status==CadenzaStatus.ACTIVE&&!instant)1+sin(time*10+word.start*5)*.04*motionAmount else 1.0
        val drift=if(instant)0.0 else cadenzaPassedDrift(time,word)
        val scale=when(status){CadenzaStatus.WAITING->if(instant)p.scale else max(p.scale*.5,.5)
            CadenzaStatus.ACTIVE->if(instant)p.scale else p.scale*1.3*pulse;else->p.scale}
        val rotation=when(status){CadenzaStatus.WAITING->if(instant)p.rotation else p.rotation+20
            CadenzaStatus.PASSED->if(instant)p.rotation else p.rotation+p.passedRotation*drift;else->p.rotation}
        val x=width/2+p.x+(if(status==CadenzaStatus.PASSED)p.passedDriftX*drift else 0.0)+(if(status==CadenzaStatus.WAITING)p.entryX else 0.0)
        // Audio energy is explicitly zero in this text-only/no-analysis port.
        val y=height*.42+sin(time*2.3)*3+p.y+(if(status==CadenzaStatus.PASSED)p.passedDriftY*drift else 0.0)+(if(status==CadenzaStatus.WAITING)p.entryY else 0.0)
        val alpha=when(status){CadenzaStatus.WAITING->0.0;CadenzaStatus.ACTIVE->1.0;else->if(instant)0.0 else if(intensity==CadenzaIntensity.CHAOTIC).9 else .82}
        return CadenzaWordTarget(p,status,cadenzaWordProgress(time,timing.reveal,word),CadenzaWordState(x,y,rotation,scale,alpha,
            if(status==CadenzaStatus.WAITING&&!instant)10.0 else 0.0,cadenzaBodyMix(time,timing,word),cadenzaWordGlow(time,timing,word)))
}
