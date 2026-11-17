package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import java.io.File
import java.util.Base64
import kotlin.random.Random
import kotlin.test.*

class CappellaMessagesTest {
    private fun rows(name: String) = File("../verification/cappella-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    private fun decode(s: String) = String(Base64.getDecoder().decode(s), Charsets.UTF_8)

    @Test fun matchesOriginalSendersAndViewportAcrossIntensityAgentsAndRandomSeeks() {
        val inputs = rows("message-inputs").groupBy { it[1] }
        for ((key, expected) in rows("messages").groupBy { it[1] }) {
            val input = inputs.getValue(key)
            val lines = input.filter { it[0] == "L" }.map { l ->
                CappellaLine(decode(l[3]), l[4].toDouble(), l[5].toDouble(), emptyList(), decode(l[6]))
            }
            val config = input.single { it[0] == "C" }
            val messages = buildCappellaMessages(lines, decode(config[3]), CappellaIntensity.valueOf(config[2].uppercase()))
            val original = expected.filter { it[0] == "M" }
            assertEquals(original.size, messages.size, key)
            for ((r, m) in original.zip(messages)) {
                assertEquals(decode(r[3]), m.id, "$key id")
                assertEquals(decode(r[4]), m.text, "$key text")
                assertEquals(r[5].uppercase(), m.sender.side.name, "$key side/${m.id}")
                assertEquals(r[6].toInt(), m.sender.avatar, "$key avatar/${m.id}")
                assertEquals(r[7].toInt().takeIf { it >= 0 }, m.line, key)
            }
            val heights = expected.filter { it[0] == "H" }.associate { decode(it[2]) to listOf(it[3].toDouble(), it[4].toDouble()) }
            val queries = expected.filter { it[0] == "Q" }
            for (q in queries + queries.shuffled(Random(817)) + queries.reversed()) {
                val t = q[2].toDouble(); val visible = cappellaVisibleLine(lines, t)
                assertEquals(q[4].toInt(), visible, "$key/$t visible")
                val actual = visibleCappellaMessages(messages, visible, q[3].toDouble(), q[5].toInt()) { m, a ->
                    heights.getValue(m.id)[if (a) 1 else 0]
                }
                assertEquals(q[6].split(',').filter { it.isNotEmpty() }.map { it.toInt() }, actual.map { messages.indexOf(it) }, "$key/$t viewport")
            }
            assertEquals(messages, buildCappellaMessages(lines, decode(config[3]), CappellaIntensity.valueOf(config[2].uppercase())))
        }
    }

    @Test fun jsTimeTokensDoNotSeedWithJavaDecimalOrExponentConventions() {
        for ((n, s) in listOf(0.0 to "0", -0.0 to "0", 1.0 to "1", 100.0 to "100", .001 to "0.001",
            1e-6 to "0.000001", 1e-7 to "1e-7", 1e20 to "100000000000000000000", 1e21 to "1e+21"))
            assertEquals(s, cappellaNumber(n))
    }
}
