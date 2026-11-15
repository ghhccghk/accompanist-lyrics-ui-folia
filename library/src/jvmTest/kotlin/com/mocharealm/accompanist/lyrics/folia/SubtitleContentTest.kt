package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.*
import java.io.File
import kotlin.test.*

class SubtitleContentTest {
    @Test fun originalContentAndGapHistory() {
        val lines=File("../verification/classic-fixtures/subtitle-scenes.tsv").readLines().filterNot {it.startsWith("#")||it.isBlank()}.map {row->
            val c=row.split('\t')
            normalize(SyncedLyrics(listOf(SyncedLine(c[2],c[3],c[0].toInt(),c[1].toInt())))).lines.single().copy(phonetic=c[4])
        }
        val visual=VisualTimeline(lines);val history=SubtitleHistory(lines)
        fun role(value:String)=when(value){"current"->SubtitleSource.Current;"translation"->SubtitleSource.Translation;"romanization"->SubtitleSource.Phonetic;else->SubtitleSource.Next}
        fun name(value:SubtitleSource)=when(value){SubtitleSource.Current->"current";SubtitleSource.Translation->"translation";SubtitleSource.Phonetic->"romanization";SubtitleSource.Next->"next"}
        fun quote(s:String)="\""+s.replace("\\","\\\\").replace("\"","\\\"")+"\""
        val fixture=File("../verification/classic-fixtures/subtitle-content.tsv").readLines().filterNot {it.startsWith("#")||it.isBlank()}
        // Shuffled visits exercise history reconstruction, not mutable last-seen state.
        for(row in fixture.shuffled(kotlin.random.Random(71))) {
            val c=row.split('\t');val time=c[0].toLong()*1000
            val active=visual.at(time).primary?.line
            assertEquals(c[1].toInt().takeIf {it>=0},active,"active: $row")
            val recent=history.recent(time,active)
            assertEquals(c[2].toInt().takeIf {it>=0},recent,"recent: $row")
            val content=resolveSubtitleContent(lines,active,recent,c[3].split('+').map(::role),c[4].toBoolean())
            val actual=content.rows.joinToString(",","[","]") {"[${quote(name(it.source))},${quote(it.text)}]"}
            assertEquals(c[5],actual,row)
            assertEquals(c[6],content.upcoming.joinToString(",","[","]"),row)
        }
    }
}
