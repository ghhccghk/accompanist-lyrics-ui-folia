package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import kotlin.random.Random
import kotlin.test.*

class DioramaUnitFrameTest {
    private fun rows(name: String) = File("../verification/diorama-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }

    @Test fun actualOriginalThreeMaterialAndMeshAssignments() {
        val cases = rows("unit-frame-inputs")
        val expected = rows("unit-frames")
        assertEquals(832, cases.size)
        assertEquals(cases.size, expected.size)
        var peak = 0.0
        for ((i, row) in cases.withIndex()) {
            fun numbers(at: Int) = row[at].split(',').map(String::toDouble)
            fun color(at: Int) = numbers(at).let { DioramaLinearColor(it[0], it[1], it[2]) }
            val start = row[0].toDouble(); val end = row[1].toDouble(); val time = row[2].toDouble()
            val p = numbers(4); val t = row[7].split(',')
            val frame = dioramaUnitStep(time, start, end, row[3].toDouble(), DioramaEnvelope(p[0], p[1]),
                color(5), color(6), DioramaUnitTuning(t[0].toDouble(), t[1].toDouble(), t[2].toBoolean(), t[3].toDouble()),
                row[8].takeUnless { it == "-" }?.let { color(8) }, row[9].toDouble(), row[10].toDouble(), row[11].toDouble())
            val values = listOf(frame.envelope.light, frame.envelope.soul, frame.fill.red, frame.fill.green, frame.fill.blue,
                frame.baseOpacity, frame.glowOpacity, if (frame.glowVisible) 1.0 else 0.0,
                frame.soulOpacity, frame.soulLift, frame.soulScale, if (frame.soulVisible) 1.0 else 0.0,
                dioramaGradientEnergy(time, start, end))
            for ((field, value) in values.withIndex()) {
                val source = expected[i][field].toDouble()
                peak = maxOf(peak, kotlin.math.abs(value - source))
                assertEquals(source, value, 1e-10, "source material case=$i field=$field")
            }
        }
        File("../build/verification/diorama-unit-frame").apply { mkdirs() }.resolve("numeric.txt")
            .writeText("sourceCases=${cases.size} maximumDifference=$peak\nFinite material operator, NOT full visual acceptance\n")
    }

    @Test fun mediaEnvelopesIntegrateTheOriginalExponentialWithoutFrameHistory() {
        val start = .7; val end = 2.8
        val times = listOf(-1.0, 0.0, start, .705, 1.0, 2.7, end, 3.15, 4.95, 1000.0)
        val captures = times.associateWith { dioramaMediaEnvelope(it, start, end) }
        // Source exponential with intervals partitioned at true media boundaries.
        // No target right-end sampling or Float32 per-RAF truncation in this probe.
        for (t in times.filter { it >= 0 }) {
            var light = 0.0; var soul = 0.0
            var previous = 0.0
            val events = (listOf(0.0, start, end, t).filter { it <= t }.distinct().sorted())
            for (next in events) {
                val target = if (previous >= start && previous < end) 1.0 else 0.0
                light = dioramaEnvelopeStep(light, target, 14.0, 4.5, next - previous)
                soul = dioramaEnvelopeStep(soul, target, 12.0, 2.2, next - previous)
                previous = next
            }
            assertEquals(light, captures.getValue(t).light, 1e-12)
            assertEquals(soul, captures.getValue(t).soul, 1e-12)
        }
        for (t in times.shuffled(Random(124))) assertEquals(captures[t], dioramaMediaEnvelope(t, start, end))
        assertEquals(DioramaEnvelope(0.0, 0.0), dioramaMediaEnvelope(4.0, 1.0, 1.0))
        assertEquals(DioramaEnvelope(0.0, 0.0), dioramaMediaEnvelope(4.0, 3.0, 1.0))
    }

    @Test fun keywordsDoNotLeakBeforeTheirOwnClockAndFinishedUnitsDoNotRetint() {
        val primary = DioramaLinearColor(.9, .9, .9)
        val accent = primary
        val keyword = DioramaLinearColor(.1, .8, .3)
        fun frame(time: Double, active: Boolean = false) = dioramaUnitFrame(time, 1.0, 2.0,
            dioramaMediaEnvelope(time, 1.0, 2.0), primary, accent, DioramaUnitTuning(soulActive = active), keyword)
        assertEquals(primary, frame(.9).fill)
        assertEquals(primary, frame(1.0).fill)
        // THREE.Color.lerp preserves this arithmetic, not an endpoint copy.
        assertEquals(primary.mix(keyword, 1.0), frame(2.0).fill)
        assertEquals(primary, frame(4.15).fill)
        assertEquals(primary, frame(9.0).fill)
        assertEquals(0.0, frame(1.5).soulOpacity, "disabled active ghost must not leak flight while current")
        assertEquals(0.0, frame(1.5).soulLift)
        assertTrue(frame(2.25).soulOpacity > 0)
        assertTrue(frame(2.25).soulLift > 0)
        assertTrue(frame(1.5, true).soulOpacity > 0)
        assertEquals(frame(2.25).fill, frame(2.25, true).fill, "ghost switch must not retint base")
        assertEquals(0.0, dioramaGradientEnergy(9.0, 1.0, 2.0))
    }
}
