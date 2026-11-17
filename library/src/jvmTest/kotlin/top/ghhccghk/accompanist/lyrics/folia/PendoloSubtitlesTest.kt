package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import java.util.Base64
import kotlin.test.*

class PendoloSubtitlesTest {
    private fun rows(name: String) = File("../verification/pendolo-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
    private fun decode(s: String) = if (s == "-") "" else String(Base64.getDecoder().decode(s), Charsets.UTF_8)
    @Test fun originalTrackSelectionSizingAndStackedHeight() {
        val expected = rows("subtitles").groupBy { it[1] }
        for ((key, input) in rows("subtitle-inputs").groupBy { it[1] }) {
            val c = input.single { it[0] == "C" }
            val source = PendoloSubtitleSource(decode(c[6]), decode(c[7]), input.filter { it[0] == "A" }.map {
                PendoloAlternateText(PendoloSubtitleRole.valueOf(it[2].uppercase()), decode(it[3]))
            })
            val actual = pendoloSubtitleTracks(source, PendoloSubtitleMode.valueOf(c[2].uppercase()), c[3] == "1", c[4] == "1", c[5].toDouble())
            val result = expected.getValue(key)
            assertEquals(result.single { it[0] == "C" }[2].toInt(), actual.size, key)
            for ((e, a) in result.filter { it[0] == "T" }.zip(actual)) {
                assertEquals(e[3].uppercase(), a.role.name, key)
                assertEquals(decode(e[4]), a.text, key)
                assertEquals(e[5].toDouble(), a.fontPx, 0.0, key)
                assertEquals(e[6].toDouble(), a.lineHeight, 0.0, key)
                assertEquals(e[7].toInt(), a.weight, key)
                assertEquals(e[8].toDouble(), a.gapEm, 0.0, key)
                assertEquals(e[9].toDouble(), a.opacity, 0.0, key)
            }
            assertEquals(result.single { it[0] == "C" }[3].toDouble(),
                pendoloSubtitleHeight(actual) { it.text.length * 1.137 + it.lineHeight * 2 }, 1e-10, "$key height")
        }
    }
}
