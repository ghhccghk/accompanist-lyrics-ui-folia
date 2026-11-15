package top.ghhccghk.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.model.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.partita.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.isRtlLyric
import java.io.File
import kotlin.test.*

class PartitaLayoutTest {
    @Test fun chunkingPosesAndMixedUnitAvoidanceMatchPinnedSource() {
        val records = File("../verification/partita-fixtures/layout.tsv").readLines()
            .filterNot { it.startsWith("#") || it.isEmpty() }.map { it.split('\t') }
        assertTrue(records.size > 500, "The source matrix must be present")
        records.groupBy { it.take(6) }.forEach { (input, expected) ->
            val texts = input[1].split('~')
            val start = input[2].toLong()
            var offset = 0
            val fragments = texts.mapIndexed { i, text ->
                TimedFragment(offset, offset + text.length, start + i * 375_000L, start + (i + 1) * 375_000L)
                    .also { offset += text.length }
            }
            val text = texts.joinToString("")
            val source = SyncedLine(start=(start/1000).toInt(), end=((start+texts.size*375_000L)/1000).toInt(), content=text, translation=null)
            val line = NormalizedLine(SourceRef(0), source, text, null, null, start,
                start+texts.size*375_000L, false, fragments)
            val intensity = FoliaAnimationIntensity.entries.first { it.name.lowercase() == input[4] }
            val layout = buildPartitaLayout(line, input[3].toDouble(), intensity, PartitaConfig(semantic=input[5].toBoolean()))
            assertEquals(expected.size, layout.rows.size, input.toString())
            val measured = layout.rows.map { row ->
                PartitaMeasuredRow(row.words.indices.map { .5 + it * .375 }, partitaWordPoses(row, intensity), row,
                    isRtlLyric(row.words.joinToString(" ") { it.text }))
            }
            val spacing = partitaSpacing(measured, 48.0)
            expected.forEachIndexed { index, e ->
                val row = layout.rows[index]
                val label = "$input row $index"
                assertEquals(e[7].toInt(), row.units.size, label)
                assertEquals(e[8], row.words.joinToString("~") { it.text }, label)
                listOf(row.pose.x, row.pose.y, row.pose.scale, row.pose.rotation, row.pose.passedRotation)
                    .forEachIndexed { i, value -> assertEquals(e[9+i].toDouble(), value.toDouble(), .00002, label) }
                assertEquals(e[14].toDouble(), row.marginBottom, .000001, label)
                assertEquals(e[15].toInt(), layout.graphemes, label)
                assertEquals(e[16].toInt(), layout.perspective, label)
                val expectedPoses = e[17].split(';').map { it.split(',').map(String::toDouble) }
                measured[index].poses.forEachIndexed { i, pose ->
                    listOf(pose.x,pose.y,pose.scale,pose.rotation,pose.passedRotation).forEachIndexed { j, value ->
                        assertEquals(expectedPoses[i][j], value.toDouble(), .00002, label)
                    }
                }
                e[19].split(',').map(String::toDouble).forEachIndexed { i, gap ->
                    assertEquals(gap, spacing[index].gaps[i], .00002, "$label word $i")
                }
                assertEquals(e[20].toDouble(), spacing[index].top, .00002, label)
                assertEquals(e[21].toDouble(), spacing[index].bottom, .00002, label)
                // The internal source mapping survives the sticky/semantic display conversion.
                assertEquals(row.units.sumOf { it.words.sumOf { w -> w.fragments.size } }, row.words.sumOf { it.fragments.size }, label)
            }
            assertEquals(layout, buildPartitaLayout(line, input[3].toDouble(), intensity,
                PartitaConfig(semantic=input[5].toBoolean())), "History-free resize/rebuild: $input")
        }
    }
}
