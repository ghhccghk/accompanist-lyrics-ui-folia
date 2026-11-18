// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 cameraPath.ts:374-548 note profile/weights/pick/last-two dedupe.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import kotlin.math.*

/** Optional SOURCE metadata only. Core does not supply chorus/part/block;
 * absence remains absent, never guessed from alignment or accompaniments. */
internal data class DioramaShotMetadata(val isChorus: Boolean = false,
    val songPart: String? = null, val blockIndex: Int? = null)
internal enum class DioramaShotMode(val move: Double, val drift: Double,
    val smooth: Double, val weave: Double) {
    calm(.6,.4,.56,.5), normal(1.0,.62,.42,1.0), chaotic(1.35,.85,.33,1.35)
}
private val chorusPart=Regex("chorus|hook|refrain|drop",RegexOption.IGNORE_CASE)

internal fun dioramaShotWeights(index: Int, lines: List<DioramaLyric>,
    metadata: List<DioramaShotMetadata> = emptyList(), mode: DioramaShotMode = DioramaShotMode.normal): Map<DioramaShot,Double> {
    val line=lines.getOrNull(index)
    val meta=metadata.getOrNull(index)?:DioramaShotMetadata()
    val prev=metadata.getOrNull(index-1)?:DioramaShotMetadata()
    val chorus=meta.isChorus||chorusPart.containsMatchIn(meta.songPart?:"")
    val sectionStart=line!=null&&index>0&&index-1 in lines.indices&&
        (meta.blockIndex!=prev.blockIndex||(meta.songPart?:"")!=(prev.songPart?:""))
    val words=line?.words.orEmpty()
    val durations=words.map{it.end-it.start}.filter{it>0}
    val longNote=(durations.maxOrNull()?:0.0)>=1.3
    val staccato=durations.size>=4&&durations.average()>0&&durations.average()<=.34
    // Keep source SHOT_KINDS insertion order for floating total/pick semantics.
    val w=linkedMapOf(DioramaShot.pushIn to 1.0,DioramaShot.pullBack to 1.0,DioramaShot.orbit to 1.0,
        DioramaShot.track to 1.0,DioramaShot.crane to 1.0,DioramaShot.hold to 1.0,DioramaShot.swell to .85,
        DioramaShot.spiral to .9,DioramaShot.pendulum to 1.0,DioramaShot.flyby to .9,
        DioramaShot.arc to 1.0,DioramaShot.float to .9,DioramaShot.glide to 1.0)
    fun add(s:DioramaShot,v:Double){w[s]=w.getValue(s)+v}
    fun mul(s:DioramaShot,v:Double){w[s]=w.getValue(s)*v}
    if(chorus) {
        add(DioramaShot.orbit,2.4);add(DioramaShot.pushIn,1.8);add(DioramaShot.spiral,1.2)
        add(DioramaShot.crane,.8);add(DioramaShot.flyby,.6);add(DioramaShot.arc,1.0)
        add(DioramaShot.glide,.6);add(DioramaShot.hold,-.6);add(DioramaShot.track,-.2)
    }else {
        add(DioramaShot.track,1.1);add(DioramaShot.crane,.9);add(DioramaShot.hold,.9)
        add(DioramaShot.pendulum,.8);add(DioramaShot.float,.8);add(DioramaShot.glide,.6);add(DioramaShot.arc,.4)
    }
    if(words.size>=8) {
        add(DioramaShot.track,1.6);add(DioramaShot.pullBack,1.0);add(DioramaShot.orbit,-.4);add(DioramaShot.pushIn,-.3)
    }else if(words.size<=3) {
        add(DioramaShot.pushIn,1.4);add(DioramaShot.orbit,.9);add(DioramaShot.hold,.5);add(DioramaShot.track,-.8)
    }
    if(sectionStart) {
        add(DioramaShot.pullBack,2.2);add(DioramaShot.crane,1.4);add(DioramaShot.glide,1.0);add(DioramaShot.pushIn,-.4)
    }
    if(longNote) {
        add(DioramaShot.swell,2.4);add(DioramaShot.spiral,1.0);add(DioramaShot.float,1.2)
        add(DioramaShot.pendulum,.6);add(DioramaShot.orbit,.8);add(DioramaShot.arc,.6)
        add(DioramaShot.hold,.3);add(DioramaShot.track,-.5)
    }
    if(staccato) {
        add(DioramaShot.track,1.4);add(DioramaShot.flyby,1.5);add(DioramaShot.pushIn,.8)
        add(DioramaShot.swell,-1.6);add(DioramaShot.hold,-.8);add(DioramaShot.crane,-.4)
    }
    when(mode) {
        DioramaShotMode.calm -> {
            add(DioramaShot.hold,1.4);add(DioramaShot.float,1.3);add(DioramaShot.swell,.9)
            add(DioramaShot.glide,.8);add(DioramaShot.arc,.5);add(DioramaShot.crane,.4)
            mul(DioramaShot.orbit,.4);mul(DioramaShot.spiral,.35);mul(DioramaShot.flyby,.3)
            mul(DioramaShot.pendulum,.5);mul(DioramaShot.pushIn,.7)
        }
        DioramaShotMode.chaotic -> {
            add(DioramaShot.orbit,1.3);add(DioramaShot.spiral,1.3);add(DioramaShot.flyby,1.2)
            add(DioramaShot.pendulum,1.0);add(DioramaShot.pushIn,.8);add(DioramaShot.arc,.6)
            add(DioramaShot.crane,.4);mul(DioramaShot.hold,.35);mul(DioramaShot.float,.55);mul(DioramaShot.swell,.7)
        }
        DioramaShotMode.normal -> Unit
    }
    return w.mapValues{max(.05,it.value)}
}

internal fun dioramaPickShot(index: Int, lines: List<DioramaLyric>, seed: Double,
    metadata: List<DioramaShotMetadata> = emptyList(), mode: DioramaShotMode = DioramaShotMode.normal): DioramaShot {
    require(seed.isFinite())
    fun base(i:Int):DioramaShot {
        if(i !in lines.indices)return DioramaShot.hold
        val weights=dioramaShotWeights(i,lines,metadata,mode)
        var acc=dioramaSeededUnit(seed+i*101+7).coerceIn(0.0,1.0)*weights.values.sum()
        for((shot,weight)in weights) { acc-=weight;if(acc<=0)return shot }
        return DioramaShot.hold
    }
    val pick=base(index)
    val prev1=if(index>0)base(index-1)else null
    val prev2=if(index>1)base(index-2)else null
    if(pick==prev1||pick==prev2) {
        val others=DioramaShot.entries.filter{it!=pick&&it!=prev1&&it!=prev2}
        val pool=others.ifEmpty{DioramaShot.entries.filter{it!=pick}}
        val alt=dioramaSeededUnit(seed+index*53+19).coerceIn(0.0,1.0)
        return pool[floor(alt*pool.size).toInt()%pool.size]
    }
    return pick
}
