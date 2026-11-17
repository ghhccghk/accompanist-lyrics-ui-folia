// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 VisualizerCappella.tsx/CappellaMessageRow.tsx/CappellaBubbleParts.tsx.
// SPDX-License-Identifier: AGPL-3.0-only. Media-time adaptation: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import kotlin.math.*

internal data class CappellaTimestampFrame(val opacity:Double,val y:Double)
internal data class CappellaPlaybackRow(val layout:CappellaRowLayout,val active:Boolean,
    val style:CappellaBubbleStyle,val size:CappellaBubbleSize,val glowEpoch:Double,
    val timestamp:CappellaTimestampFrame?,val avatarOpacity:Double=1.0)

/** Original externally supplied latest-array-order selector, inclusive at end.
 * The after variant builds responses at a boundary without introducing epsilon
 * media times. Exact boundary selection stays independent from its right limit. */
internal fun cappellaCurrentLine(lines:List<CappellaLine>,time:Double,after:Boolean=false):Int =
    lines.indexOfLast { time>=it.start && (time<it.renderEnd||(!after&&time==it.renderEnd)) }

/** Source utils/lyrics/renderHints.ts buildLineRenderHints; keep last-word
 * semantics (not maximum word end), including micro-line render floor. */
internal fun cappellaAnnotated(line:CappellaLine,lastWordEnd:Double=line.words.lastOrNull()?.end?:line.end):CappellaLine {
    val raw=max(line.end-line.start,0.0)
    if(raw<.1)return line.copy(renderEnd=max(line.end,line.start+.067))
    val fast=raw<.18
    val enter=if(fast)(raw*.45).coerceIn(.045,.06)else(raw*.34).coerceIn(.22,.42)
    val exit=if(fast)(raw*.22).coerceIn(.03,.04)else(raw*.18).coerceIn(.18,.32)
    val pass=max(lastWordEnd,line.start)+if(fast).03 else .06
    val start=max(pass,line.end-exit).let{if(fast)max(it,line.start+enter+.01)else it}
    return line.copy(renderEnd=max(line.end,start+exit))
}

/** Event-prepared continuous stage. No display-frame replay, coroutine, ticker,
 * or accumulating spring state. Native flow follows animated frame heights;
 * structural layout changes retain visual position with the original finite
 * ease-out projection, independently of the group's physical scale response. */
internal class CappellaPlaybackPreparation(private val stage:CappellaStagePreparation,
    private val viewportHeight:Double) {
    private val config=cappellaTransition(stage.intensity)
    private val plans=stage.lines.map(::CappellaRevealPlan)
    private val order=stage.messages.mapIndexed{i,m->m.id to i}.toMap()
    private inner class Episode(val message:CappellaMessage,val enter:Double,val initial:Boolean,
        val initialSize:CappellaRowSize,val initialScale:Double,val initialOpacity:Double,
        val initialMargin:Double,val initialActive:Boolean) {
        var unmount:Double?=null
        val presence=mutableListOf(enter to true)
        val widths=mutableListOf<Pair<Double,Double>>()
        val heights=mutableListOf<Pair<Double,Double>>()
        val scales=mutableListOf<Pair<Double,Double>>()
        val opacities=mutableListOf<Pair<Double,Double>>()
        val margins=mutableListOf<Pair<Double,Double>>()
        val states=mutableListOf<Pair<Double,Boolean>>()
        val timestamps=mutableListOf<Pair<Double,Boolean>>()
        lateinit var width:CappellaScalarTrack
        lateinit var height:CappellaScalarTrack
        lateinit var scale:CappellaScalarTrack
        lateinit var opacity:CappellaScalarTrack
        lateinit var margin:CappellaScalarTrack
        lateinit var outer:CappellaPresenceTrack
        val projections=mutableListOf<Pair<Double,Double>>()
        val exitTops=mutableMapOf<Double,Double>()
        val exitHeights=mutableMapOf<Double,Double>()
        var measured:Pair<Double,Double>?=null
        fun phase(time:Double,leftLimit:Boolean=false)=presence.lastOrNull {
            it.first<time||(!leftLimit&&it.first==time)
        }
        fun present(time:Double,leftLimit:Boolean=false)=phase(time,leftLimit)?.second?:initial
        fun projection(time:Double,leftLimit:Boolean=false):Double = projections.lastOrNull{
            it.first<time||(!leftLimit&&it.first==time)
        }?.let {
            cappellaTween(it.second,0.0,time-it.first,CappellaCurve.ENTER,config).value
        }?:0.0
        fun active(time:Double)=states.lastOrNull{it.first<=time}?.second?:initialActive
        fun epoch(time:Double)=states.lastOrNull{it.first<=time&&it.second}?.first?:enter
    }
    private val episodes:List<Episode>
    init {
        require(stage.lines.all{it.start.isFinite()&&it.renderEnd.isFinite()})
        val events=(listOf(0.0)+stage.lines.flatMap{listOf(it.start,it.renderEnd)}+
            plans.flatMap{it.bubbleTargetTimes+it.revealTimes+it.timestampReady}).filter{it.isFinite()&&it>=0}.distinct().sorted()
        val all=mutableListOf<Episode>();val live=linkedMapOf<String,Episode>()
        val retiring=linkedMapOf<String,Episode>()
        // AnimatePresence clears the completed exit batch together, rather
        // than unmounting each zero-opacity child on its individual deadline.
        fun flushCompleted(until:Double) {
            val deadline=retiring.values.maxOfOrNull{it.presence.last().first+config.exitDuration}?:return
            if(deadline<=until){retiring.values.forEach{it.unmount=deadline};retiring.clear()}
        }
        for(t in events) {
            flushCompleted(t)
            val current=cappellaCurrentLine(stage.lines,t,after=true)
            val target=stage.at(t,current,viewportHeight)
            val ids=target.map{it.message.id}.toSet()
            for(id in live.keys.toList())if(id !in ids){
                val e=live.remove(id)!!;e.presence+=t to false;retiring[id]=e
            }
            for(row in target) {
                val m=row.message;val active=m.line!=null&&m.line==current
                val size=stage.rowSize(m,active,t)
                val margin=if(active)ceil(max(size.fullBubble.height,40.0)*(stage.motion.activeScale-1))else 0.0
                val e=live.getOrPut(m.id) {
                    retiring.remove(m.id)?.also{it.presence+=t to true}?:
                    Episode(m,t,t==0.0,size,if(t==0.0)row.scale else 1.0,
                        if(t==0.0)row.opacity else 1.0,if(t==0.0)margin else 0.0,active).also{all+=it}
                }
                e.widths+=t to size.bubble.width;e.heights+=t to size.bubble.height
                e.scales+=t to row.scale;e.opacities+=t to row.opacity;e.margins+=t to margin
                if(e.states.lastOrNull()?.second!=active)e.states+=t to active
                if(m.line!=null) {
                    val line=stage.lines[m.line]
                    val ready=m.line<current||t>=(if(active)plans[m.line].timestampReady else line.end)
                    if(e.timestamps.lastOrNull()?.second!=ready)e.timestamps+=t to ready
                }
            }
            // An exiting React child keeps its last props AND subscriptions.
            // If it left while active, its prefix/size/timestamp still advance
            // on the shared MotionValue until the presence batch unmounts it.
            for(e in retiring.values) {
                val active=e.active(t)
                val size=stage.rowSize(e.message,active,t)
                e.widths+=t to size.bubble.width;e.heights+=t to size.bubble.height
                e.message.line?.let { index->
                    val ready=e.timestamps.lastOrNull()?.second==true||
                        t>=(if(active)plans[index].timestampReady else stage.lines[index].end)
                    if(e.timestamps.lastOrNull()?.second!=ready)e.timestamps+=t to ready
                }
            }
        }
        flushCompleted(Double.POSITIVE_INFINITY)
        for(e in all) {
            e.width=CappellaScalarTrack(e.initialSize.bubble.width,e.widths,CappellaCurve.RESIZE,config)
            e.height=CappellaScalarTrack(e.initialSize.bubble.height,e.heights,CappellaCurve.RESIZE,config)
            e.scale=CappellaScalarTrack(e.initialScale,e.scales,CappellaCurve.SPRING,config)
            e.opacity=CappellaScalarTrack(e.initialOpacity,e.opacities,CappellaCurve.SPRING,config)
            e.margin=CappellaScalarTrack(e.initialMargin,e.margins,CappellaCurve.SPRING,config)
            e.outer=CappellaPresenceTrack(e.presence,e.initial,config)
        }
        episodes=all
        // layout="position": remove exiting rows from flow immediately, but
        // compensate surviving rows from their pre-commit visual positions.
        // A new structural change retargets the current projection, not its
        // old initial offset. Exiting popLayout rows freeze that same position.
        val parentCommits=stage.lines.flatMap{listOf(it.start,it.renderEnd)}.filter { t->
            stage.lines.indexOfLast{it.start<t&&t<=it.renderEnd}!=cappellaCurrentLine(stage.lines,t,after=true)||
                stage.lines.indexOfLast{it.start<t}!=cappellaVisibleLine(stage.lines,t)
        }.toSet()
        for(t in events) {
            val before=flow(t,leftLimit=true).toMap()
            val after=flow(t).toMap()
            for((e,top)in before) {
                val visual=top+e.projection(t)
                if(e.phase(t)==(t to false)) {
                    // Original PopChildMeasure reads offsetTop (integer) and
                    // computed height, then fixes that outer box absolutely.
                    // The bubble may keep resizing inside this frozen row.
                    e.exitTops[t]=round(top)
                    e.exitHeights[t]=max(40.0,e.height.at(t).value)+e.margin.at(t).value
                }
                val next=after[e]
                if(next!=null&&next!=top)e.projections+=t to (visual-next)
            }
            for((e,next)in after)if(e !in before) {
                val previous=e.phase(t,leftLimit=true)
                if(previous?.second==false) {
                    val visual=e.exitTops.getValue(previous.first)+e.projection(t)
                    e.projections+=t to (visual-next)
                }
            }
            for(e in episodes) {
                if(t<e.enter||e.unmount?.let{t>=it}==true)continue
                val line=e.message.line
                val localCommit=line!=null&&(
                    e.active(t)&&(t in plans[line].bubbleTargetTimes||t in plans[line].revealTimes)||
                    e.timestamps.any{it.first==t})
                if(t!=e.enter&&t !in parentCommits&&!localCommit)continue
                val phase=e.phase(t)?:continue
                val top=if(phase.second)after[e]?:continue else e.exitTops[phase.first]?:continue
                val h=if(phase.second)max(40.0,e.height.at(t).value)+e.margin.at(t).value
                    else e.exitHeights.getValue(phase.first)
                val previous=e.measured
                // Motion compares rounded target boxes, including the row's
                // animated height even for layout="position". A local reveal
                // commit can therefore retarget a still-running projection.
                val targetChanged=previous!=null&&(floor(previous.first+.5)!=floor(top+.5)||
                    floor(previous.first+previous.second+.5)!=floor(top+h+.5))
                if(targetChanged&&e.projections.lastOrNull()?.first!=t) {
                    // A reveal commit while preceding frame heights still
                    // animate must retain the measured position as well as
                    // its running projection. Restarting only the projection
                    // loses that flow displacement (source trace: row 7,
                    // overlap/chaotic at 2.0167..2.09). Once flow has settled,
                    // the snapshot is the current box, not the stale commit.
                    // This semantic reconstruction still differs from React's
                    // RAF batching; see verification/cappella-presence.
                    val priorRows=after.keys.takeWhile{it!==e}
                    val movingFlow=priorRows.any{!it.height.at(t).done||!it.margin.at(t).done}
                    val offset=e.projection(t)+if(movingFlow)previous.first-top else 0.0
                    if(offset!=0.0)e.projections+=t to offset
                }
                e.measured=top to h
            }
        }
    }
    private fun flow(time:Double,leftLimit:Boolean=false):List<Pair<Episode,Double>> {
        var top=stage.size.top
        return episodes.filter { e->e.present(time,leftLimit) }.sortedBy{order.getValue(it.message.id)}.map { e->
            val rowTop=top
            top+=max(40.0,e.height.at(time).value)+e.margin.at(time).value+12
            e to rowTop
        }
    }
    fun at(time:Double,reducedMotion:Boolean=false):List<CappellaPlaybackRow> {
        require(time.isFinite())
        val current=cappellaCurrentLine(stage.lines,time)
        // Source selectors include renderEnd. A preparation event describes
        // its right limit, while an exact-end seek still sees the prior set.
        val leftLimit=current!=cappellaCurrentLine(stage.lines,time,after=true)
        val live=flow(time,leftLimit)
        val exiting=episodes.filter{e->e.phase(time,leftLimit)?.second==false&&
            (e.unmount==null||time<e.unmount!!)}
            .map{it to it.exitTops.getValue(it.phase(time,leftLimit)!!.first)}
        return (live+if(reducedMotion)emptyList()else exiting)
            .sortedBy{order.getValue(it.first.message.id)}.map { (e,flowTop)->
            val m=e.message;val removal=e.phase(time,leftLimit)?.takeIf{!it.second}?.first
            val top=flowTop+if(reducedMotion)0.0 else e.projection(time,leftLimit)
            val active=if(removal!=null)e.active(removal)else m.line!=null&&m.line==current
            val style=cappellaBubbleStyle(stage.size,stage.motion,active,m.line==null)
            val targetSize=stage.rowSize(m,active,removal?:time)
            val width=if(reducedMotion)targetSize.bubble.width else e.width.at(time).value
            val height=if(reducedMotion)targetSize.bubble.height else e.height.at(time).value
            val scale=if(reducedMotion)1.0 else e.scale.at(time).value
            val margin=if(reducedMotion)0.0 else e.margin.at(time).value
            val passed=m.line!=null&&m.line<current
            val opacity=if(reducedMotion)if(passed)stage.motion.passedOpacity else 1.0 else e.opacity.at(time).value
            val rowHeight=removal?.let{e.exitHeights.getValue(it)}?: (max(40.0,height)+margin)
            val outer=if(reducedMotion)CappellaPresenceFrame(1.0,1.0,0.0)else e.outer.at(time,leftLimit)
            val outerAlpha=outer.opacity;val outerScale=outer.scale;val outerY=outer.y
            val right=m.sender.side==CappellaSide.RIGHT
            val anchor=stage.size.panelX+if(right)stage.size.panelWidth else 0.0
            val bottom=top+rowHeight
            val bx=if(right)anchor-(52+width)*scale else anchor+52*scale
            val by=bottom-height*scale
            val ax=if(right)anchor-40*scale else anchor
            val ay=bottom-40*scale
            val cx=stage.size.panelX+stage.size.panelWidth/2;val cy=top+rowHeight/2
            fun x(value:Double)=cx+(value-cx)*outerScale
            fun y(value:Double)=cy+(value-cy)*outerScale+outerY
            val transform=scale*outerScale
            val layout=CappellaRowLayout(m,x(bx),y(by),max(width,style.minWidth)*transform,
                max(height,style.minHeight)*transform,x(ax),y(ay),transform,opacity*outerAlpha,top,rowHeight)
            val stamp=e.timestamps.lastOrNull{it.first<=time}?.takeIf{it.second}?.let{
                CappellaTimestampFrame(if(reducedMotion).62 else cappellaTween(0.0,.62,time-it.first,CappellaCurve.TIMESTAMP,config).value,
                    if(reducedMotion)0.0 else cappellaTween(4.0,0.0,time-it.first,CappellaCurve.TIMESTAMP,config).value)
            }
            val avatarAlpha=if(e.initial||reducedMotion)1.0 else
                cappellaTween(0.0,1.0,time-e.enter,CappellaCurve.RESIZE,config).value
            CappellaPlaybackRow(layout,active,style,CappellaBubbleSize(max(width,style.minWidth),max(height,style.minHeight)),e.epoch(time),stamp,avatarAlpha)
        }
    }
}
