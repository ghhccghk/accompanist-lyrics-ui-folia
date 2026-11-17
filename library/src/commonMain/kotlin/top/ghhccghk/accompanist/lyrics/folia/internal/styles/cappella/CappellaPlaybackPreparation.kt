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
internal fun cappellaAnnotated(line:CappellaLine):CappellaLine {
    val raw=max(line.end-line.start,0.0)
    if(raw<.1)return line.copy(renderEnd=max(line.end,line.start+.067))
    val fast=raw<.18
    val enter=if(fast)(raw*.45).coerceIn(.045,.06)else(raw*.34).coerceIn(.22,.42)
    val exit=if(fast)(raw*.22).coerceIn(.03,.04)else(raw*.18).coerceIn(.18,.32)
    val pass=max(line.words.lastOrNull()?.end?:line.end,line.start)+if(fast).03 else .06
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
        var end:Double?=null
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
        val projections=mutableListOf<Pair<Double,Double>>()
        var exitTop=0.0
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
            plans.flatMap{it.bubbleTargetTimes+it.timestampReady}).filter{it.isFinite()&&it>=0}.distinct().sorted()
        val all=mutableListOf<Episode>();val live=linkedMapOf<String,Episode>()
        for(t in events) {
            val current=cappellaCurrentLine(stage.lines,t,after=true)
            val target=stage.at(t,current,viewportHeight)
            val ids=target.map{it.message.id}.toSet()
            for(id in live.keys.toList())if(id !in ids){live.remove(id)!!.end=t}
            for(row in target) {
                val m=row.message;val active=m.line!=null&&m.line==current
                val size=stage.rowSize(m,active,t)
                val margin=if(active)ceil(max(size.fullBubble.height,40.0)*(stage.motion.activeScale-1))else 0.0
                val e=live.getOrPut(m.id) {
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
        }
        for(e in all) {
            e.width=CappellaScalarTrack(e.initialSize.bubble.width,e.widths,CappellaCurve.RESIZE,config)
            e.height=CappellaScalarTrack(e.initialSize.bubble.height,e.heights,CappellaCurve.RESIZE,config)
            e.scale=CappellaScalarTrack(e.initialScale,e.scales,CappellaCurve.SPRING,config)
            e.opacity=CappellaScalarTrack(e.initialOpacity,e.opacities,CappellaCurve.SPRING,config)
            e.margin=CappellaScalarTrack(e.initialMargin,e.margins,CappellaCurve.SPRING,config)
        }
        episodes=all
        // layout="position": remove exiting rows from flow immediately, but
        // compensate surviving rows from their pre-commit visual positions.
        // A new structural change retargets the current projection, not its
        // old initial offset. Exiting popLayout rows freeze that same position.
        for(t in episodes.flatMap{listOfNotNull(it.enter,it.end)}.distinct().sorted()) {
            val before=flow(t,leftLimit=true).toMap()
            val after=flow(t).toMap()
            for((e,top)in before) {
                val visual=top+e.projection(t)
                if(e.end==t)e.exitTop=visual
                val next=after[e]
                if(next!=null&&next!=top)e.projections+=t to (visual-next)
            }
        }
    }
    private fun flow(time:Double,leftLimit:Boolean=false):List<Pair<Episode,Double>> {
        var top=stage.size.top
        return episodes.filter { e->(e.enter<time||(!leftLimit&&e.enter==time)||e.initial)&&
            (e.end==null||time<e.end!!||(leftLimit&&time==e.end)) }.sortedBy{order.getValue(it.message.id)}.map { e->
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
        val exiting=episodes.filter{e->e.end?.let{
            (time>it||(!leftLimit&&time==it))&&time<it+config.exitDuration
        }==true}
            .map{it to it.exitTop}
        return (live+if(reducedMotion)emptyList()else exiting)
            .sortedBy{order.getValue(it.first.message.id)}.map { (e,flowTop)->
            val m=e.message;val end=e.end
            val top=flowTop+if(reducedMotion||end!=null&&time>=end)0.0 else e.projection(time,leftLimit)
            val active=if(end!=null&&time>=end)e.active(end)else m.line!=null&&m.line==current
            val style=cappellaBubbleStyle(stage.size,stage.motion,active,m.line==null)
            val targetSize=stage.rowSize(m,active,if(end!=null)min(time,end)else time)
            val width=if(reducedMotion)targetSize.bubble.width else e.width.at(time).value
            val height=if(reducedMotion)targetSize.bubble.height else e.height.at(time).value
            val scale=if(reducedMotion)1.0 else e.scale.at(time).value
            val margin=if(reducedMotion)0.0 else e.margin.at(time).value
            val passed=m.line!=null&&m.line<current
            val opacity=if(reducedMotion)if(passed)stage.motion.passedOpacity else 1.0 else e.opacity.at(time).value
            val rowHeight=max(40.0,height)+margin
            fun entering(from:Double,to:Double,t:Double)=if(e.initial||reducedMotion)to else
                cappellaTween(from,to,t-e.enter,CappellaCurve.ENTER,config).value
            val enterTime=if(end==null)time else min(time,end)
            var outerAlpha=entering(0.0,1.0,enterTime)
            var outerScale=entering(config.enterScale,1.0,enterTime)
            var outerY=entering(config.enterY,0.0,enterTime)
            if(end!=null&&time>=end&&!reducedMotion) {
                outerAlpha=cappellaTween(outerAlpha,0.0,time-end,CappellaCurve.EXIT,config).value
                outerScale=cappellaTween(outerScale,config.exitScale,time-end,CappellaCurve.EXIT,config).value
                outerY=cappellaTween(outerY,config.exitY,time-end,CappellaCurve.EXIT,config).value
            }
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
