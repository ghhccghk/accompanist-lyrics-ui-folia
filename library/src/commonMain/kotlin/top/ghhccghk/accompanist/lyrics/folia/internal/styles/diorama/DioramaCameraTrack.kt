// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Independent media reconstruction of the ported CameraRig normal follower.
// Actual original finite operator and media adaptation are tested separately.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import kotlin.math.*

internal data class DioramaCameraSubject(val line: DioramaCameraLine, val frame: DioramaFrame,
    val placement: DioramaPlacement, val shot: DioramaShot, val trackWidth: Double, val localIndex: Int)

/** Immutable media checkpoints, prepared independently of display scheduling.
 * Step-doubling controls source-operator integration error, not image/acceptance
 * thresholds. Queries start from a prepared node, NEVER the previous UI frame.
 * Source finite SmoothDamp is delta-dependent; this adaptive continuous-limit
 * adaptation is NOT asserted equivalent to every original RAF history.
 *
 * Caller owns asynchronous preparation/cancellation. No resources, job, ticker,
 * global cache or whole-song preparation are created by at(). */
internal class DioramaCameraTrack(val subjects: List<DioramaCameraSubject>, val width: Double,
    val height: Double, val endTime: Double, private val seed: Double,
    private val smoothTime: Double = .42, private val moveScale: Double = 1.0,
    private val driftScale: Double = .62, private val tolerance: Double = .00002,
    checkpoint: () -> Unit = {}) {
    private data class Node(val time: Double, val state: DioramaFollowState)
    private val nodes: List<Node>
    val preparedNodeCount get() = nodes.size
    init {
        require(subjects.isNotEmpty())
        require(width.isFinite() && height.isFinite() && width > 0 && height > 0)
        require(endTime.isFinite() && endTime >= 0 && tolerance.isFinite() && tolerance > 0)
        require(smoothTime.isFinite() && smoothTime > 0 && seed.isFinite())
        require(moveScale.isFinite() && driftScale.isFinite())
        require(subjects.all { it.trackWidth.isFinite() && it.trackWidth >= 0 &&
            it.line.line.start.isFinite() && it.line.renderEnd.isFinite() })
        val events = (listOf(0.0,endTime)+subjects.flatMap { s ->
            val line=s.line.line
            listOf(line.start,line.start-.6,line.start+.35,s.line.renderEnd,s.line.renderEnd+1,
                s.line.renderEnd+3,s.line.renderEnd+8)+
                dioramaLineTimeline(line).flatMap { listOf(it.start,it.end) }
        }).filter { it >= 0 && it <= endTime }.distinct().sorted()
        val result=ArrayList<Node>()
        var state=advance(DioramaFollowState(),0.0,0.0).state
        result+=Node(0.0,state)
        var t=0.0;var h=min(.1,smoothTime/4)
        for (end in events.drop(1)) {
            while (t < end) {
                checkpoint()
                val dt=min(h,end-t)
                check(t+dt>t) { "Diorama camera integration lost time precision" }
                // Stay on the old subject THROUGH the left-hand interval. The
                // exact boundary below commits the new subject once with dt0.
                val coarse=advance(state,dt,t+dt,leftLimit=t+dt==end).state
                val half=advance(state,dt/2,t+dt/2).state
                val fine=advance(half,dt/2,t+dt,leftLimit=t+dt==end).state
                val error=distance(coarse,fine)
                if (error>tolerance) {
                    check(dt>1e-9) { "Diorama camera failed to converge at $t: $error" }
                    h=dt/2
                    continue
                }
                result+=Node(t+dt/2,half)
                t=if(dt==end-t)end else t+dt
                state=fine
                result+=Node(t,state)
                if(error<tolerance/4)h=min(dt*2,min(.1,smoothTime/4))else h=dt
            }
            checkpoint()
            state=advance(state,0.0,end).state
            result[result.lastIndex]=Node(end,state)
        }
        nodes=result.toList()
    }

    /** Sticky source-array order, held through gaps; no alignment/chorus inferred. */
    fun subjectIndex(time: Double, leftLimit: Boolean = false): Int = subjects.indexOfLast {
        it.line.line.start < time || (!leftLimit && it.line.line.start == time)
    }.coerceAtLeast(0)

    fun target(time: Double, leftLimit: Boolean = false): DioramaCameraTarget {
        val i=subjectIndex(time,leftLimit);val s=subjects[i]
        return s.line.target(time,s.frame,s.placement,seed,s.localIndex,s.shot,s.trackWidth,width/height,
            moveScale=moveScale,driftScale=driftScale)
    }
    private fun advance(state: DioramaFollowState, dt: Double, time: Double,
        leftLimit: Boolean = false): DioramaFollowResult {
        val s=subjects[subjectIndex(time,leftLimit)]
        return dioramaFollowStep(state,target(time,leftLimit),s.frame,s.placement,s.shot,time,
            s.line.line.start,s.line.renderEnd,dt,smoothTime,width/height)
    }

    /** Media beyond the bounded prepared song is held, not integrated forever. */
    fun at(mediaTime: Double, reducedMotion: Boolean = false): DioramaFollowState {
        require(mediaTime.isFinite())
        val t=mediaTime.coerceIn(0.0,endTime)
        if (reducedMotion) {
            val s=subjects[subjectIndex(t)]
            val target=s.line.target(s.line.line.start,s.frame,s.placement,seed,s.localIndex,s.shot,
                0.0,width/height,moveScale=0.0,driftScale=0.0)
            return DioramaFollowState(position=target.eye,look=target.look,
                orientation=DioramaQuaternion.look(target.eye,target.look))
        }
        var lo=0;var hi=nodes.lastIndex
        while(lo<hi) {
            val mid=(lo+hi+1)/2
            if(nodes[mid].time<=t)lo=mid else hi=mid-1
        }
        val node=nodes[lo]
        return if(t==node.time)node.state else advance(node.state,t-node.time,t).state
    }
    fun perspectiveAt(time: Double, reducedMotion: Boolean = false): DioramaPerspective {
        val state=at(time,reducedMotion)
        return DioramaPerspective(state.position,state.look,width,height,orientation=state.orientation)
    }

    private fun distance(a: DioramaFollowState,b: DioramaFollowState): Double {
        fun vector(a: DioramaVec,b: DioramaVec)=(a-b).let { maxOf(abs(it.x),abs(it.y),abs(it.z)) }
        fun quaternion(a: DioramaQuaternion?,b: DioramaQuaternion?): Double {
            if(a==null||b==null)return if(a==b)0.0 else 1.0
            val s=if(a.dot(b)<0)-1 else 1
            return maxOf(abs(a.x-b.x*s),abs(a.y-b.y*s),abs(a.z-b.z*s),abs(a.w-b.w*s))
        }
        return maxOf(vector(a.position,b.position),vector(a.velocity,b.velocity),
            vector(a.look,b.look),vector(a.lookVelocity,b.lookVelocity),abs(a.alignment-b.alignment),
            quaternion(a.alignmentOrientation,b.alignmentOrientation),quaternion(a.orientation,b.orientation))
    }
}
