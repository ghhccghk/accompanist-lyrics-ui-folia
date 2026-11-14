package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import java.io.File
import java.lang.management.ManagementFactory
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertTrue

/** Diagnostic CPU/allocation probe, not a frame-rate test or a pass/fail latency gate. */
class ComputeBenchmark {
    @Test fun randomSeekProbe() {
        val lines = normalize(SyncedLyrics(List(1000) { i ->
            SyncedLine("Original probe $i", null, i * 1_000, i * 1_000 + 800)
        })).lines
        val prepareStart = System.nanoTime()
        val index = VisualTimeline(lines)
        val prepareMs = (System.nanoTime() - prepareStart) / 1e6
        val random = Random(20261114)
        val times = LongArray(10_000) { random.nextLong(0, 1_001_000_000) }
        var checksum = 0L
        repeat(3) { times.forEach { checksum += index.at(it).primary?.line ?: 0 } }
        val thread = ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean
        val id = Thread.currentThread().threadId()
        val allocatedBefore = thread?.takeIf { it.isThreadAllocatedMemorySupported }?.getThreadAllocatedBytes(id)
        val samples = DoubleArray(20) {
            val start = System.nanoTime()
            times.forEach { checksum += index.at(it).primary?.line ?: 0 }
            (System.nanoTime() - start) / 10_000.0
        }.sorted()
        val allocated = allocatedBefore?.let { thread!!.getThreadAllocatedBytes(id) - it }
        assertTrue(checksum > 0)
        File("build/verification").mkdirs()
        File("build/verification/compute-benchmark.txt").writeText("""
            Date: 2026-11-14
            OS: ${System.getProperty("os.name")} ${System.getProperty("os.version")} ${System.getProperty("os.arch")}
            JVM: ${System.getProperty("java.version")} / ${System.getProperty("java.vm.name")}
            CPUs: ${Runtime.getRuntime().availableProcessors()}; max heap: ${Runtime.getRuntime().maxMemory()}
            Surface/font/GPU: none (logic only); 1000 synthetic lines; seeded random seek
            Warmup: 30000 queries; measurement: 20 batches of 10000 queries
            Prepare ms: $prepareMs
            Batch-average ns/query P50: ${samples[9]}; P95: ${samples[18]}; P99: ${samples[19]}
            Allocated bytes/query: ${allocated?.div(200_000.0)}
            Checksum: $checksum
        """.trimIndent())
    }
}
