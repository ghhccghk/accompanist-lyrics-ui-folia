package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.*
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import java.io.File
import kotlin.test.*

class PartitaMotionTest {
    @Test fun actualMountedOriginalChunkTransitions() {
        val sceneRows = (File("../verification/classic-fixtures/scenes.tsv").readLines()+File("../verification/partita-fixtures/scenes.tsv").readLines())
            .filterNot { it.startsWith("#") || it.isBlank() }.map { it.split('\t') }
        val rows = (File("../verification/partita-fixtures/chunk-motion.tsv").readLines()+File("../verification/partita-fixtures/chunk-motion-tight.tsv").readLines())
            .filterNot { it.startsWith("#") || it.isBlank() }.map { it.split('\t') }
        for (c in rows) {
            val input = sceneRows.filter { it[0] == c[1] }
            val source = KaraokeLine.MainKaraokeLine(input.map { KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt()) },
                null,KaraokeAlignment.Unspecified,input.first()[1].toInt(),input.first()[2].toInt())
            val line = normalize(SyncedLyrics(listOf(source))).lines.single()
            val level = FoliaAnimationIntensity.entries.single { it.name.lowercase() == c[0] }
            val layout = buildPartitaLayout(line,600.0,level)
            val profile = renderProfile(line.startUs,line.endUs,line.fragments.last().endUs)
            val frame = samplePartitaChunk(c[3].toLong()*1000,line.startUs,layout.rows[c[4].toInt()],profile,false)
            listOf(frame.x,frame.y,frame.scale,frame.rotation,frame.alpha).forEachIndexed { i, value ->
                assertEquals(c[5+i].toDouble(),value.toDouble(),.0001,"$c property $i")
            }
        }
    }
}
