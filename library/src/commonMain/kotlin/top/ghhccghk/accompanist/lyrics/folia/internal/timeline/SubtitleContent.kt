// Adapted from Folia v0.7.26 cc6226e4 VisualizerSubtitleOverlay.tsx / runtime.ts.
// Source ledger: docs/upstream.md. SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.timeline

import top.ghhccghk.accompanist.lyrics.folia.internal.model.NormalizedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.renderProfile

internal enum class SubtitleSource { Current, Translation, Phonetic, Next }
internal data class SubtitleRow(val source:SubtitleSource,val text:String)
internal data class SubtitleContent(val line:Int?,val rows:List<SubtitleRow>,val upcoming:List<Int>)

internal expect fun hasReadableText(text:String):Boolean

/** The host's existing booleans choose rows; source filtering/deduplication follows Folia. */
internal fun resolveSubtitleContent(lines:List<NormalizedLine>,active:Int?,recent:Int?,
    sources:List<SubtitleSource>,visible:Boolean=true,allowFallback:Boolean=true):SubtitleContent {
    if(!visible) return SubtitleContent(null,emptyList(),emptyList())
    val index=active?:recent
    val next=index?.let { ((it+1)..minOf(it+2,lines.lastIndex)).filter {n->hasReadableText(lines[n].text)} }?:emptyList()
    val rows=mutableListOf<SubtitleRow>()
    var cursor=0
    if(index!=null) for(source in sources) {
        if(source==SubtitleSource.Next) {
            next.getOrNull(cursor++)?.let {rows+=SubtitleRow(source,lines[it].text)}
            continue
        }
        val line=lines[index]
        val text=when(source) {
            SubtitleSource.Current->line.text
            SubtitleSource.Translation->line.translation
            SubtitleSource.Phonetic->line.phonetic
            SubtitleSource.Next->null
        }?.trim()
        if(text.isNullOrEmpty() || !hasReadableText(text)) continue
        if(source!=SubtitleSource.Current && rows.any {it.source!=SubtitleSource.Next && it.text==text}) continue
        rows+=SubtitleRow(source,text)
    }
    return SubtitleContent(index,rows,if(allowFallback && rows.isEmpty() && active!=null) next else emptyList())
}

internal class SubtitleHistory(private val lines:List<NormalizedLine>) {
    private val renderEnds=lines.map {renderProfile(it.startUs,it.endUs,it.fragments.lastOrNull()?.endUs?:it.endUs).renderEndUs}
    /** Original runtime uses strictly greater-than, including the exact end boundary. */
    fun recent(timeUs:Long,active:Int?):Int? = if(active!=null) null else lines.indices.lastOrNull {timeUs>renderEnds[it]}
}
