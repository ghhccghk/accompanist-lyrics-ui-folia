package top.ghhccghk.accompanist.lyrics.folia

import java.io.File
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.test.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*

class FumeSpringResponseTest {
    @Test fun matchesUntouchedOriginalWhenCoefficientsAndTargetAreHeldConstant() {
        val rows = File("../verification/fume-fixtures/spring-response.tsv").readLines()
            .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }
        assertEquals(125, rows.size)
        var maximum = 0.0
        for (row in rows) {
            val h = row[3].toDouble(); val seconds = h * row[2].toInt()
            val position = FumeSpringResponse(row[4].toDouble(), row[5].toDouble(), row[6].toDouble(), h)
            val scale = FumeSpringResponse(row[7].toDouble(), row[8].toDouble(), row[9].toDouble(), h)
            val target = row[10].split(',').map(String::toDouble)
            val initial = row[11].split(',').map(String::toDouble)
            val expected = row[12].split(',').map(String::toDouble)
            for ((model, indices, q) in listOf(Triple(position, listOf(0, 2, 4), target[0]),
                Triple(position, listOf(1, 3, 5), target[1]), Triple(scale, listOf(6, 7, 8), target[2]))) {
                val start = FumeSpringState(initial[indices[0]], initial[indices[1]], initial[indices[2]])
                val result = model.advance(start, q, seconds)
                for ((i, actual) in listOf(result.position, result.velocity, result.focus).withIndex()) {
                    maximum = maxOf(maximum, abs(expected[indices[i]] - actual))
                    assertEquals(expected[indices[i]], actual, 1e-8, "row=$row index=$i")
                }
                // Query partition/order cannot change a constant-coefficient response.
                val split = model.advance(model.advance(start, q, seconds * .137), q, seconds * .863)
                assertEquals(result.position, split.position, 1e-9)
                assertEquals(result.velocity, split.velocity, 1e-9)
                assertEquals(result.focus, split.focus, 1e-9)
                assertEquals(start, model.advance(start, q, 0.0))
                val rate = model.derivative(q, 0.0, q, q)
                assertEquals(FumeSpringState(0.0, 0.0, -0.0), rate)
            }
        }
        println("Fume frozen original response: 125 samples / 375 axes, maximum absolute error=$maximum")
    }

    @Test fun derivativeMatchesFractionalAnalyticResponse() {
        for ((k, d, r) in listOf(Triple(208.0, 24.0, 11.2), Triple(780.0, 40.0, 54.0),
            Triple(54.0, 13.5, 5.4), Triple(108.0, 21.0, 12.8))) {
            val model = FumeSpringResponse(k, d, r)
            val start = FumeSpringState(20.0, -3.0, 17.0)
            val analytic = model.advance(start, 22.0, 1e-7)
            val middle = model.advance(start, 22.0, 5e-8)
            val derivative = model.derivative(middle.position, middle.velocity, middle.focus, 22.0)
            assertEquals(derivative.position, (analytic.position - start.position) / 1e-7, .001)
            assertEquals(derivative.velocity, (analytic.velocity - start.velocity) / 1e-7, .001)
            assertEquals(derivative.focus, (analytic.focus - start.focus) / 1e-7, .001)
        }
    }

    @Test fun coincidentFocusEigenvalueAndLongSeekStayFinite() {
        val h = 1.0 / 60; val k = 108.0; val d = 21.0
        val trace = 2 - h * d - h * h * k
        val root = sqrt(trace * trace - 4 * (1 - h * d))
        val focusRate = -ln((trace + root) / 2) / h
        val model = FumeSpringResponse(k, d, focusRate)
        val start = FumeSpringState(20.0, -3.0, 17.0)
        val result = model.advance(start, 22.0, h)
        val focus = 17 + (22 - 17) * (1 - kotlin.math.exp(-focusRate * h))
        val velocity = -3 + ((focus - 20) * k + 3 * d) * h
        assertEquals(focus, result.focus, 1e-10)
        assertEquals(velocity, result.velocity, 1e-10)
        assertEquals(20 + velocity * h, result.position, 1e-10)
        assertEquals(FumeSpringState(22.0, 0.0, 22.0), model.advance(start, 22.0, 3600.0))
        assertEquals(FumeSpringState(22.0, 0.0, 22.0),
            FumeSpringResponse(208.0, 24.0, 11.2).advance(start, 22.0, 3600.0))
    }
}
