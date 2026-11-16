// SPDX-License-Identifier: AGPL-3.0-only
// Media-time reconstruction around the pinned Fume operator. Adaptation: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import kotlin.math.*

internal data class FumeCameraEpoch(val time: Double, val lineIndex: Int)
internal data class FumeCameraTrackExperimentConfig(val viewport: FumeViewport, val overviewStart: Double,
    val speed: Double = 1.0, val stepped: Boolean = false, val static: Boolean = false,
    val intensity: String = "normal", val floatOrigin: Double = 0.0,
    val localPixelError: Double = .02)
private data class FumeCameraNode(val time: Double, val state: FumeCameraState)

/** REJECTED runtime candidate, retained only in jvmTest. Source60 motion residual and
 * preparation-node count are not acceptable; see verification/fume-camera-track.
 * Prepared canonical media trajectory, not a frame clock. It samples the original operator
 * with adaptive step-doubling, splits retarget boundaries exactly, and never integrates from
 * the last displayed frame. Queries cannot modify prepared nodes or each other's results.
 *
 * This is a calibration adaptation: the source's finite-frame Euler spring is rate-dependent.
 * The sampled trajectory is not claimed equal to every browser refresh rate. Idle float uses
 * media seconds (fixed origin) so pause/seek are reproducible. Epochs must supply resolved
 * focus-block indices; discovering fallback/overlap epochs belongs to the timeline adapter. */
internal class FumeCameraTrackExperiment(private val article: FumeArticle, private val geometry: Map<Int,FumeFocusBlock>,
    private val epochs: List<FumeCameraEpoch>, private val config: FumeCameraTrackExperimentConfig, endTime: Double) {
    private val overview = fumeOverview(article,config.viewport)
    private val nodes: List<FumeCameraNode>
    val preparedNodeCount: Int get() = nodes.size
    init {
        require(epochs.isNotEmpty() && epochs.all { it.time.isFinite() && (it.lineIndex in geometry || article.blocks.isEmpty()) })
        require(epochs.zipWithNext().all { (a,b)->a.time<b.time })
        require(endTime.isFinite() && endTime >= epochs.last().time)
        require(config.speed.isFinite() && config.speed > 0 && config.localPixelError in .00001..1.0)
        val events=(epochs.map{it.time}+fumeCameraControlTimes(geometry.values)+listOf(config.overviewStart,endTime))
            .filter{it.isFinite()&&it>=epochs.first().time&&it<=endTime}.distinct().sorted()
        val result=mutableListOf<FumeCameraNode>()
        var time=epochs.first().time
        var state=fumeInitialCamera(article)
        for(end in events) {
            if(end>time) {
                val epoch=epochAt(time)
                state=integrate(state,time,end,epoch,time>=config.overviewStart) { t,s->result+=FumeCameraNode(t,s) }
                time=end
            }
            // Install a new target at its media boundary, never at the next rendered frame.
            val source=if(time>=config.overviewStart)-2 else epochAt(time)
            if(state.retarget.source!=source)state=step(state,time,0.0,epochAt(time),time>=config.overviewStart).state
            if(result.lastOrNull()?.time==time)result[result.lastIndex]=FumeCameraNode(time,state)
            else result+=FumeCameraNode(time,state)
        }
        nodes=result.toList()
    }
    private fun epochAt(time: Double): Int {
        var low=0;var high=epochs.size
        while(low<high){val m=(low+high)ushr 1;if(epochs[m].time<=time)low=m+1 else high=m}
        return epochs[max(low-1,0)].lineIndex
    }
    private fun step(state:FumeCameraState,time:Double,dt:Double,lineIndex:Int,showOverview:Boolean) =
        fumeCameraStep(state,article,geometry,FumeCameraInput(time,time+config.floatOrigin,dt,lineIndex,config.viewport,overview,
            if(showOverview)config.overviewStart else Double.POSITIVE_INFINITY,config.speed,config.stepped,config.static,config.intensity))
    private fun error(a:FumeCameraState,b:FumeCameraState):Double {
        val scale=maxOf(a.pose.scale,b.pose.scale,.001)
        return maxOf(abs(a.pose.x-b.pose.x)*scale,abs(a.pose.y-b.pose.y)*scale,
            abs(a.pose.scale-b.pose.scale)*max(config.viewport.width,config.viewport.height))
    }
    private fun integrate(initial:FumeCameraState,from:Double,to:Double,lineIndex:Int,showOverview:Boolean,
        accepted:(Double,FumeCameraState)->Unit={_,_->}):FumeCameraState {
        var state=initial;var time=from;var h=min(.05,to-from);var attempts=0
        while(time<to) {
            check(++attempts<=500_000){"Fume camera adaptive integration did not converge"}
            h=min(h,to-time)
            check(h>0 && time+h>time){"Fume camera integration lost media-time precision"}
            val full=step(state,time+h,h,lineIndex,showOverview).state
            val middle=step(state,time+h*.5,h*.5,lineIndex,showOverview).state
            val half=step(middle,time+h,h*.5,lineIndex,showOverview).state
            val e=error(full,half)
            check(e.isFinite()){"Fume camera integration produced nonfinite error"}
            if(e<=config.localPixelError) {
                time=if(h==to-time)to else time+h
                state=half;accepted(time,state)
                h*=if(e<config.localPixelError*.125)1.8 else 1.0
            } else h*=max(.15,.8*sqrt(config.localPixelError/e))
        }
        return state
    }
    fun sample(time: Double):FumeCameraFrame {
        require(time.isFinite())
        var low=0;var high=nodes.size
        while(low<high){val m=(low+high)ushr 1;if(nodes[m].time<=time)low=m+1 else high=m}
        val node=nodes[max(low-1,0)]
        val state=if(time<=node.time)node.state else integrate(node.state,node.time,time,epochAt(node.time),node.time>=config.overviewStart)
        val show=time>=config.overviewStart
        val restore=if(show&&state.retarget.source == -2)fumeCameraEase((time-state.retarget.started)/max(state.retarget.duration,.001))else 0.0
        return FumeCameraFrame(state,show,restore)
    }
}

/** Split actual printing and focus-curve knots, so a short line/glyph cannot be missed
 * merely because both ends of a numerical trial step lie outside its time window. */
internal fun fumeCameraControlTimes(blocks: Collection<FumeFocusBlock>): List<Double> = buildList {
    for(block in blocks) {
        val reveal=FumeReveal(block.block.line);val line=reveal.line;val size=reveal.graphemes.size
        add(line.start);add(line.end)
        val offsets=((0..size).map(Int::toDouble)+block.rows.flatMap{listOf(it.start-.7,it.start+.7,it.end-.7,it.end+.7)})
            .filter{it>=0&&it<=size}
        if(reveal.ranges.isEmpty()) {
            val duration=max(line.end-line.start,.12)
            if(size>0)for(offset in offsets)add(line.start+offset/size*duration)
        } else for(range in reveal.ranges) {
            val duration=max(range.word.end-range.word.start,.08)
            val count=max(range.end-range.start,1)
            add(range.word.start);add(range.word.end);add(range.word.start+duration)
            if(range.word.syllables.isNotEmpty())addAll(range.timings.map{it.start})
            else for(i in 1..count)add(range.word.start+(i-.2)/count*duration)
            for(offset in offsets)if(offset>=range.start&&offset<=range.end)
                add(range.word.start+(offset-range.start)/count*duration)
        }
    }
}.filter{it.isFinite()}.distinct().sorted()
