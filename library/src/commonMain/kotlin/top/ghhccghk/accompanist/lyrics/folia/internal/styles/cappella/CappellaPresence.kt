// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 CappellaMessageRow.tsx. SPDX-License-Identifier: AGPL-3.0-only.
// Media-event adaptation of source presence targets; no React/Motion runtime bundled.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

internal data class CappellaPresenceFrame(val opacity:Double,val scale:Double,val y:Double)

/** Cancellation of exit preserves the current outer transform. A keyed message
 * is not a new entrance while AnimatePresence still owns the previous instance. */
internal class CappellaPresenceTrack(events:List<Pair<Double,Boolean>>,initial:Boolean,
    private val config:CappellaTransition) {
    private data class Segment(val time:Double,val from:CappellaPresenceFrame,
        val to:CappellaPresenceFrame,val curve:CappellaCurve)
    private val origin=if(initial)CappellaPresenceFrame(1.0,1.0,0.0)
        else CappellaPresenceFrame(0.0,config.enterScale,config.enterY)
    private val segments:List<Segment>
    private fun sample(s:Segment,time:Double):CappellaPresenceFrame {
        val p=cappellaTween(0.0,1.0,time-s.time,s.curve,config).value
        fun mix(a:Double,b:Double)=a+(b-a)*p
        return CappellaPresenceFrame(mix(s.from.opacity,s.to.opacity),mix(s.from.scale,s.to.scale),mix(s.from.y,s.to.y))
    }
    init {
        val result=mutableListOf<Segment>()
        for((time,present)in events) {
            val from=result.lastOrNull()?.let{sample(it,time)}?:origin
            val target=if(present)CappellaPresenceFrame(1.0,1.0,0.0)
                else CappellaPresenceFrame(0.0,config.exitScale,config.exitY)
            result+=Segment(time,from,target,if(present)CappellaCurve.ENTER else CappellaCurve.EXIT)
        }
        segments=result
    }
    fun at(time:Double,leftLimit:Boolean=false):CappellaPresenceFrame =
        segments.lastOrNull{it.time<time||(!leftLimit&&it.time==time)}?.let{sample(it,time)}?:origin
}
