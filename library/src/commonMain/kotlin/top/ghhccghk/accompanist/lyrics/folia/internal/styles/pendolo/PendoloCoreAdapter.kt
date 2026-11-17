package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import com.mocharealm.accompanist.lyrics.core.model.ISyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries

/** Identity/ref survive normalization so a transformed row can return the
 * actual published lyrics-core instance. No dependency on original lyrics-ui. */
internal data class PendoloCoreLine(val stage:PendoloStageLine,val source:ISyncedLine,val ref:SourceRef)

internal fun pendoloCoreLines(document:NormalizedDocument,chorusLineIndices:Set<Int> = emptySet()):List<PendoloCoreLine> =
    document.lines.map { line ->
        val edges=graphemeBoundaries(line.text)
        val pending=mutableListOf<PendoloTimedText>()
        val words=buildList {
            line.fragments.forEachIndexed { i,f ->
                pending+=PendoloTimedText(line.text.substring(f.begin,f.end),f.startUs/1e6,f.endUs/1e6)
                if(f.end in edges || i==line.fragments.lastIndex){
                    add(if(pending.size==1)pending.single() else PendoloTimedText(pending.joinToString(""){it.text},
                        pending.minOf{it.start},pending.maxOf{it.end},pending.toList()))
                    pending.clear()
                }
            }
        }
        val start=line.source.start.toLong()*1000
        val end=line.source.end.toLong()*1000
        // Preserve source seconds arithmetic even at floating threshold edges.
        // Integer-microsecond classification can choose a different source mode.
        val renderEnd=pendoloRenderEnd(start/1e6,end/1e6,(line.fragments.lastOrNull()?.endUs?:end)/1e6)
        val lyric=PendoloLyric(line.text,start/1e6,end/1e6,words)
        // No-word inputs retain original Pendolo even-grapheme interpolation;
        // unlike Cadenza's line-only adaptation, no instant word is fabricated.
        PendoloCoreLine(PendoloStageLine(lyric,PendoloSubtitleSource(line.translation,line.phonetic),renderEnd,
            line.ref.index in chorusLineIndices),line.source,line.ref)
    }
