package top.ghhccghk.accompanist.lyrics.folia.internal.waiting

import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.timeline.VisualSelection

internal data class WaitingInterval(val startUs: Long, val endUs: Long)

/**
 * Finite vocal-free intervals. Source LyricsPlaybackTimeline uses >5000ms; merge
 * all voices here rather than comparing adjacent main rows. Never infer an outro.
 */
internal class WaitingTimeline(lines: List<NormalizedLine>) {
    val intervals: List<WaitingInterval>
    init {
        val vocals=lines.filter {it.text.isNotBlank() && it.endUs>it.startUs && it.endUs>0}
            .sortedBy {it.startUs}
        val union=mutableListOf<WaitingInterval>()
        for(line in vocals) {
            val next=WaitingInterval(line.startUs.coerceAtLeast(0),line.endUs)
            val previous=union.lastOrNull()
            if(previous!=null && next.startUs<=previous.endUs)
                union[union.lastIndex]=previous.copy(endUs=maxOf(previous.endUs,next.endUs))
            else union+=next
        }
        intervals=buildList {
            var end=0L
            for(vocal in union) {
                if(vocal.startUs-end>5_000_000L) add(WaitingInterval(end,vocal.startUs))
                end=vocal.endUs
            }
        }
    }
    fun at(timeUs: Long): WaitingInterval? {
        var low=0;var high=intervals.size
        while(low<high) {
            val mid=(low+high) ushr 1
            if(intervals[mid].startUs<=timeUs) low=mid+1 else high=mid
        }
        return intervals.getOrNull(low-1)?.takeIf {timeUs<it.endUs}
    }
    fun visibleAt(timeUs: Long, selection: VisualSelection): WaitingInterval? =
        if(selection.primary!=null || selection.exiting.isNotEmpty() || selection.secondary.isNotEmpty()) null
        else at(timeUs)
}
