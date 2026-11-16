package top.ghhccghk.accompanist.lyrics.folia

import java.io.File
import java.util.Base64
import kotlin.random.Random
import kotlin.test.*
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume.*

class FumeCameraTrackTest {
    private fun rows(name: String) = File("../verification/fume-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith('#') || it.isBlank() }.map { it.split('\t') }

    @Test fun eventPreparedContinuousTrajectoryMatchesAdaptedSourceWithoutQueryHistory() {
        val blocks = rows("camera-blocks").map { f -> FumeArticleBlock(f[0].toInt(),
            FumeLyric(String(Base64.getDecoder().decode(f[1]), Charsets.UTF_8), f[2].toDouble(), f[3].toDouble()), false,
            f[4].toDouble(), f[5].toDouble(), f[6].toDouble(), f[7].toDouble(), f[6].toDouble(), 32.0, f[8].toDouble(), 1) }
        val geometry = blocks.associate { block -> block.sourceIndex to FumeFocusBlock(block,
            rows("camera-rows").filter { it[0].toInt() == block.sourceIndex }.map { f ->
                FumeFocusRow(f[1].toInt(), f[2].toInt(), f[3].toDouble(), f[4].toDouble(), f[5].toDouble(), f[6].split(',').map(String::toDouble))
            }) }
        val article = FumeArticle(2500.0, 4600.0, 600.0, 2, 24.0, 1.0, FumeBounds(0.0, 0.0, 2500.0, 4600.0), blocks)
        val epochs = listOf(FumeFocusEpoch(0.0, 0), FumeFocusEpoch(1.5, 2), FumeFocusEpoch(3.0, 0), FumeFocusEpoch(4.5, 4))
        val output = mutableListOf("# scenario\trate\ttime\tstepped\tstatic\tnodes\tnativePose\toriginalPose")
        var largestError = 0.0
        for ((scenario, frames) in rows("camera-continuous").groupBy { it[0] }) {
            val config = FumeCameraTrackConfig(FumeViewport(800.0, 600.0), 6.8,
                stepped = frames.first()[3].toBoolean(), static = frames.first()[4].toBoolean(), floatOrigin = 9.25)
            val track = FumeCameraTrack(article, geometry, epochs, config, 8.0)
            val captured = mutableMapOf<Double, FumeCameraFrame>()
            for (frame in frames) {
                val time = frame[2].toDouble(); val result = track.sample(time); val p = result.state.pose
                val values = listOf(p.x, p.y, p.vx, p.vy, p.fx, p.fy, p.scale, p.vs, p.fs)
                for ((field, expected) in frame[6].split(',').map(String::toDouble).withIndex()) {
                    largestError = maxOf(largestError, kotlin.math.abs(expected - values[field]))
                    assertEquals(expected, values[field], 1e-5, "scenario=$scenario time=$time field=$field")
                }
                captured[time]?.let { assertEquals(it, result, "sampling rate cannot affect output") }
                captured[time] = result
                output += (frame.take(5) + track.preparedNodeCount.toString() + values.joinToString(",") + frame[7]).joinToString("\t")
            }
            val nodes = track.preparedNodeCount
            for ((time, expected) in captured.entries.shuffled(Random(839))) {
                assertEquals(expected, track.sample(time), "random/reverse seek $time")
                assertEquals(expected, track.sample(time), "paused media $time")
            }
            assertEquals(nodes, track.preparedNodeCount)
            assertEquals(track.sample(0.0), track.sample(-10.0))
            assertEquals(track.sample(8.0), track.sample(3600.0))
            assertFailsWith<IllegalArgumentException> { track.sample(Double.NaN) }
            println("continuous scenario=$scenario preparedNodes=$nodes independentQueries=${captured.size}")
        }
        File("../build/verification/fume-camera-continuous").apply { mkdirs() }
            .resolve("native.tsv").writeText(output.joinToString("\n") + "\n")
        println("continuous adapted-source maximum error=$largestError; raw browser residual is reported separately, NOT asserted accepted")
    }
}
