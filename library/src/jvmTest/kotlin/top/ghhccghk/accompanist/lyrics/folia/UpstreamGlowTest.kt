package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.animation.*
import java.io.File
import kotlin.test.*

class UpstreamGlowTest {
    @Test fun curvesMatchOriginalVariantsAndFramerGenerator() {
        File("../verification/classic-fixtures/glow.tsv").readLines().filterNot { it.startsWith("#") || it.isBlank() }.forEach { row ->
            val c = row.split('\t')
            val mode = Reveal.entries.first { it.name.equals(c[0], true) }
            val total = c[1].toInt(); val start = c[2].toLong(); val end = c[3].toLong()
            val duration = maxOf(end - start, when(mode) { Reveal.Normal -> 100_000; Reveal.Fast -> 120_000; Reveal.Instant -> 80_000 })
            var actual = activeGraphemeGlow(c[6].toLong(), mode, duration, total, start, c[4].toLong(), c[5].toLong())
            if (c[7].toLong() >= 0) actual = passedGraphemeGlow(actual, c[7].toLong(), mode)
            assertEquals(c[8].toDouble(), actual.toDouble(), .00002, row)
        }
    }
}
