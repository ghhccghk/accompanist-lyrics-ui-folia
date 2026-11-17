package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import java.util.Base64
import kotlin.random.Random
import kotlin.test.*

class CappellaRevealTest {
    private fun rows(file: String) = File("../verification/cappella-fixtures/$file.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    private fun decode(s: String) = String(Base64.getDecoder().decode(s), Charsets.UTF_8)
    private fun inputs(): Map<String, CappellaLine> = rows("reveal-inputs").groupBy { it[1] }.mapValues { (_, rs) ->
        val l = rs.first { it[0] == "L" }
        CappellaLine(decode(l[3]), l[4].toDouble(), l[5].toDouble(), rs.filter { it[0] == "W" }.map { w ->
            CappellaWord(decode(w[3]), w[4].toDouble(), w[5].toDouble(),
                rs.filter { it[0] == "S" && it[3] == w[2] }.map { s ->
                    CappellaSyllable(decode(s[4]), s[5].toDouble(), s[6].toDouble()) }) })
    }

    @Test fun matchesOriginalGraphemesFadeAndTimestampIncludingMalformedInput() {
        val cases = inputs()
        for ((key, rs) in rows("reveal").groupBy { it[1] }) {
            val plan = CappellaRevealPlan(cases.getValue(key))
            val glyphs = rs.filter { it[0] == "G" }
            assertEquals(glyphs.map { decode(it[3]) }, plan.characters, key)
            for (r in glyphs) {
                val i = r[2].toInt()
                assertEquals(r[4].toDouble(), plan.revealTimes[i], 1e-10, "$key/$i reveal")
                assertEquals(r[5].toDouble(), plan.timeline[i].end, 1e-10, "$key/$i end")
                assertEquals(r[6].toDouble(), plan.fadeMillis[i], 1e-8, "$key/$i fade")
            }
            assertEquals(rs.single { it[0] == "R" }[2].toDouble(), plan.timestampReady, 1e-10, key)
            val queries = rs.filter { it[0] == "Q" }
            for (r in queries + queries.shuffled(Random(317)) + queries.reversed()) {
                val t = r[2].toDouble()
                assertEquals(r[3].toInt(), plan.visibleCount(t), "$key/$t text")
                assertEquals(r[4].toInt(), plan.bubbleCount(t), "$key/$t bubble")
            }
        }
    }

    @Test fun protectsClustersWhenParserSyllablesCutCombiningOrZwjSequences() {
        for ((text, parts) in listOf("e\u0301" to listOf("e", "\u0301"),
            "👩🏽‍🚀" to listOf("👩", "🏽", "\u200d", "🚀"))) {
            val word = CappellaWord(text, 1.0, 4.0, parts.mapIndexed { i, s ->
                CappellaSyllable(s, 1.0 + i * .5, 1.5 + i * .5) })
            val plan = CappellaRevealPlan(CappellaLine(text, 1.0, 4.0, listOf(word)))
            assertEquals(listOf(text), plan.characters)
            assertEquals(1.0, plan.revealTimes.single())
            assertEquals(1.0 + parts.size * .5, plan.timeline.single().end)
            assertEquals(0, plan.visibleCount(.999)); assertEquals(1, plan.visibleCount(1.0))
            val captured = plan.progress(0, 1.25)
            plan.progress(0, 10.0); plan.progress(0, .0)
            assertEquals(captured, plan.progress(0, 1.25), "seek/pause uses only media time")
        }
    }
}
