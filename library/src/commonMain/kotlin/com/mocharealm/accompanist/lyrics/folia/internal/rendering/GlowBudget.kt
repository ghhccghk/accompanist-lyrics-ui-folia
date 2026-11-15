package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import com.mocharealm.accompanist.lyrics.folia.FoliaQuality

/** Retained RGBA mask pixels only, not total process/native/scratch memory. */
internal object GlowBudget {
    const val primaryBytes = 16L * 1024 * 1024
    const val exitingBytes = 16L * 1024 * 1024
    fun perExitBytes(maxConcurrentExits: Int): Long {
        require(maxConcurrentExits >= 0)
        // Preserve ordinary scenes' allocation while sharing a fixed total
        // across dense transitions. Lower glow quality rather than drop text.
        return exitingBytes / maxOf(4, maxConcurrentExits)
    }

    fun layers(quality: FoliaQuality, bytesPerLayer: Long, budget: Long): Int {
        require(bytesPerLayer >= 0 && budget >= 0)
        val requested = when (quality) {
            FoliaQuality.Standard -> 2
            FoliaQuality.Reduced -> 1
            FoliaQuality.Minimal -> 0
        }
        // Division avoids multiplication overflow for oversized input estimates.
        return if (bytesPerLayer == 0L) requested else minOf(requested.toLong(), budget / bytesPerLayer).toInt()
    }
}
