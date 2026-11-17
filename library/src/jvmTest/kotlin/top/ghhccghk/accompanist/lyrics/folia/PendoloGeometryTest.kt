package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo.*
import java.io.File
import kotlin.math.*
import kotlin.random.Random
import kotlin.test.*

class PendoloGeometryTest {
    private fun rows(name: String) = File("../verification/pendolo-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }

    @Test fun unchangedSourceWheelGeometryWithMeasuredHeightAvoidance() {
        val output = rows("geometry").groupBy { it[1] }
        val residual = mutableListOf("# key item field original native absoluteError")
        for (r in rows("geometry-inputs")) {
            val key = r[0]
            val c = PendoloGeometryTuning(r[7].toDouble(), r[8].toDouble(), r[9].toDouble(), r[10].toDouble(), r[11].toDouble())
            val actual = pendoloWheelLayout(r[1].toInt(), r[2].toDouble(), r[3].toDouble(), r[4].toDouble(), r[5].toDouble(), c,
                r[6].toDouble(), if (r[12] == "-" || r[12].isEmpty()) emptyList() else r[12].split(',').map(String::toDouble))
            val original = output.getValue(key)
            assertEquals(original.single { it[0] == "C" }[2].toInt(), actual.size, key)
            assertEquals(original.single { it[0] == "C" }[3].toDouble(),
                pendoloAvailableTextWidth(r[4].toDouble(), r[5].toDouble(), c), 1e-10, "$key source caller text width")
            for ((e, a) in original.filter { it[0] == "I" }.zip(actual)) {
                assertEquals(e[2].toInt(), a.index, key)
                assertEquals(e[7] == "1", a.active, key)
                val values = listOf(a.angleRad, a.angleDeg, a.x, a.y, a.distance, a.alpha, a.scale)
                for ((field, pair) in listOf(3, 4, 5, 6, 8, 9, 10).zip(values).withIndex()) {
                    val (column, value) = pair
                    val expected = e[column].toDouble()
                    residual += listOf(key, a.index, field, expected, value, abs(expected - value)).joinToString("\t")
                    assertEquals(expected, value, 1e-9, "$key/${a.index}/$column")
                }
            }
        }
        File("../build/verification/pendolo-geometry").apply { mkdirs() }
            .resolve("residual.tsv").writeText(residual.joinToString("\n") + "\n")
    }

    @Test fun sourceTimelineHistoryMatchesAcrossRandomSeekOverlapResumeAndInclusiveEnds() {
        val data = rows("timeline")
        val queries = data.filter { it[0] == "Q" }.groupBy { it[1] }
        for ((key, qs) in queries) {
            val lines = data.filter { it[0] == "L" && it[1] == key }.map {
                PendoloLineTime(it[3].toDouble(), it[4].toDouble(), it[5].toDouble())
            }
            val timeline = PendoloTimeline(lines)
            for (q in qs + qs.reversed() + qs.shuffled(Random(507))) {
                val t = q[2].toDouble()
                assertEquals(q[3].toInt(), timeline.current(t), "$key/$t current")
                assertEquals(q[6].toDouble(), pendoloFallbackAnchor(lines, q[3].toInt(), q[4].toInt(), q[5] == "1", t), "$key/$t pure")
                assertEquals(q[6].toDouble(), timeline.anchor(t), "$key/$t reconstruction")
                assertEquals(timeline.anchor(t), PendoloTimeline(lines).anchor(t), "$key/$t remount")
            }
        }
    }

    @Test fun opacityUsesUnwrappedAngleAndExactEdgeCutoff() {
        for (r in rows("opacity")) assertEquals(r[3].toDouble(),
            pendoloRotatingOpacity(r[0].toDouble(), r[1].toDouble(), r[2].toDouble()), 1e-12, "$r")
        assertEquals(0.0, pendoloRotatingOpacity(360.0, 0.0, 1.0))
    }

    @Test fun geometryRestoresAfterResizeAndGapIsNotAnActiveRow() {
        val heights = listOf(75.0, 140.0, 36.0, 200.0, 82.0)
        fun layout(w: Double, t: Double) = pendoloWheelLayout(5, t, 0.0, w, 480.0, radiusOffset = min(w, 480.0) * .06, blockHeights = heights)
        val original = layout(800.0, 2.0)
        layout(380.0, 2.0)
        assertEquals(original, layout(800.0, 2.0))
        assertTrue(layout(380.0, 2.5).none { it.active })
    }
}
