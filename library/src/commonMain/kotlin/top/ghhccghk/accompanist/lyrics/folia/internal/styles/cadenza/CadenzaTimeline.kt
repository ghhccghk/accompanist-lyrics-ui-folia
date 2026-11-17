// Copyright (c) 2026 chthollyphile and contributors
// appPlaybackHelpers.ts findLatestActiveLineIndex + visualizer/runtime.ts cc6226e4.
// SPDX-License-Identifier: AGPL-3.0-only. Source/adaptations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza

internal data class CadenzaSceneSelection(val active:Int?,val entered:Double?,val recent:Int?,val upcoming:Int?)

/** Source array order wins, including equal starts and resumed overlaps. End is
 * inclusive upstream; exact boundary and immediately-after states are distinct.
 * No classic AnimatePresence retention or frame-history cache is applied. */
internal class CadenzaTimeline(private val lines:List<CadenzaLyric>) {
    private val ends=lines.map{cadenzaTiming(it).end}
    private val times:List<Double>
    private val exact:List<CadenzaSceneSelection>
    private val after:List<CadenzaSceneSelection>
    private val before=CadenzaSceneSelection(null,null,null,lines.indices.firstOrNull{lines[it].start>Double.NEGATIVE_INFINITY})
    init {
        require(lines.all{it.start.isFinite()}&&ends.all(Double::isFinite))
        times=(lines.map{it.start}+ends).distinct().sorted()
        val starts=lines.indices.groupBy{lines[it].start};val stops=lines.indices.groupBy{ends[it]}
        val active=BooleanArray(lines.size);val heap=mutableListOf<Int>()
        fun push(value:Int){heap+=value;var i=heap.lastIndex;while(i>0){val parent=(i-1)/2;if(heap[parent]>=value)break;heap[i]=heap[parent];i=parent};heap[i]=value}
        fun top():Int? {
            while(heap.isNotEmpty()&&!active[heap[0]]){
                val last=heap.removeAt(heap.lastIndex);if(heap.isEmpty())break
                var i=0
                while(i*2+1<heap.size){var child=i*2+1;if(child+1<heap.size&&heap[child+1]>heap[child])child++
                    if(heap[child]<=last)break;heap[i]=heap[child];i=child}
                heap[i]=last
            }
            return heap.firstOrNull()
        }
        var previous:Int?=null;var entered:Double?=null;var recent:Int?=null
        fun selection(t:Double):CadenzaSceneSelection {
            val index=top();if(index!=previous){previous=index;entered=if(index==null)null else t}
            return CadenzaSceneSelection(index,entered,if(index==null)recent else null,
                if(index!=null)(index+1).takeIf{it<lines.size}else lines.indices.firstOrNull{lines[it].start>t})
        }
        val points=mutableListOf<CadenzaSceneSelection>();val intervals=mutableListOf<CadenzaSceneSelection>()
        for(t in times){
            for(i in starts[t].orEmpty())if(ends[i]>=t){active[i]=true;push(i)}
            points+=selection(t)
            for(i in stops[t].orEmpty()){
                active[i]=false
                if(recent==null||i>recent)recent=i
            }
            intervals+=selection(t)
        }
        exact=points;after=intervals
    }
    fun at(time:Double):CadenzaSceneSelection {
        require(time.isFinite())
        var low=0;var high=times.size
        while(low<high){val m=(low+high)ushr 1;if(times[m]<=time)low=m+1 else high=m}
        if(low==0)return before
        val index=low-1
        return if(times[index]==time)exact[index]else after[index]
    }
}
