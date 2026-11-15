package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.bitmapFromArgb
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.blurredMasks
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.test.Test
import kotlin.test.assertTrue

/** Mask kernel and bitmap conversion only; no text shaping, GPU upload or frame-rate claim. */
class MaskBenchmark {
    @Test fun twoLayerPreparationProbe() {
        val width = 320; val height = 160
        val source = bitmapFromArgb(width, height, IntArray(width * height) { index ->
            val x = index % width; val y = index / width
            if (x in 80..240 && y in 60..100) -1 else 0
        })
        fun prepare(): Int {
            val masks = blurredMasks(source, intArrayOf(10, 20))
            val inner = masks[0]
            val outer = masks[1]
            val pixel = IntArray(1)
            inner.readPixels(pixel, startX = 160, startY = 80, width = 1, height = 1)
            val alpha = pixel[0] ushr 24
            outer.readPixels(pixel, startX = 160, startY = 80, width = 1, height = 1)
            return alpha + (pixel[0] ushr 24)
        }
        val coldStart = System.nanoTime()
        var checksum = prepare()
        val coldMs = (System.nanoTime() - coldStart) / 1e6
        repeat(10) { checksum += prepare() }
        val bean = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean
        val thread = Thread.currentThread().threadId()
        val allocatedBefore = bean?.takeIf { it.isThreadAllocatedMemorySupported }?.getThreadAllocatedBytes(thread)
        val samples = DoubleArray(30) {
            val start = System.nanoTime()
            checksum += prepare()
            (System.nanoTime() - start) / 1e6
        }.sorted()
        val allocated = allocatedBefore?.let { bean!!.getThreadAllocatedBytes(thread) - it }
        assertTrue(checksum > 0)
        File("build/verification").mkdirs()
        File("build/verification/mask-benchmark.txt").writeText("""
            JVM: ${System.getProperty("java.version")}; OS: ${System.getProperty("os.name")} ${System.getProperty("os.arch")}
            CPUs: ${Runtime.getRuntime().availableProcessors()}; heap: ${Runtime.getRuntime().maxMemory()}
            320x160 original alpha rectangle, radii 10/20, two output bitmaps, one thread.
            Cold probe then 10 warmups; 30 individual paired-mask preparations.
            Text shaping/rasterization, GPU and native allocation are excluded.
            Cold ms: $coldMs
            Warm ms P50: ${samples[14]}; P95: ${samples[28]}; max: ${samples[29]}
            JVM allocated bytes/pair: ${allocated?.div(30.0)}
            Checksum: $checksum
        """.trimIndent())
    }
}
