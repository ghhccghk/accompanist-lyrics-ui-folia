package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries

internal data class CappellaCoreLine(val stage:CappellaLine,val source:ISyncedLine,val ref:SourceRef,
    val translation:String?,val phonetic:String?)

/** Published lyrics-core carries no Folia agentId. Identity is explicit by source
 * ref (including nested accompaniment), never inferred from alignment/chorus.
 * Missing/one-agent data goes through the existing original seeded fallback. */
internal fun cappellaCoreLines(document:NormalizedDocument,agents:Map<SourceRef,String> = emptyMap()):List<CappellaCoreLine> =
    document.lines.map { line ->
        val boundaries=graphemeBoundaries(line.text)
        val pending=mutableListOf<CappellaSyllable>()
        val words=buildList {
            line.fragments.forEachIndexed { i,f ->
                pending+=CappellaSyllable(line.text.substring(f.begin,f.end),f.startUs/1e6,f.endUs/1e6)
                if(f.end in boundaries || i==line.fragments.lastIndex){
                    add(CappellaWord(pending.joinToString(""){it.text},pending.minOf{it.start},pending.maxOf{it.end},
                        if(pending.size==1)emptyList()else pending.toList()))
                    pending.clear()
                }
            }
        }
        val raw=CappellaLine(line.text,line.source.start/1000.0,line.source.end/1000.0,words,
            cappellaAgentId(agents[line.ref]))
        // Expanded normalization and EGC union endpoints are not the raw parser's
        // final-fragment endpoint used by original renderHints.
        val stage=cappellaAnnotated(raw,line.fragments.lastOrNull()?.endUs?.div(1e6)?:raw.end)
        CappellaCoreLine(stage,line.source,line.ref,line.translation,line.phonetic)
    }
