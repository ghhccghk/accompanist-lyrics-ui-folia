package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import com.mocharealm.accompanist.lyrics.folia.FoliaQuality
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.MAX_EXITING_EPOCHS

/** Retained RGBA mask pixels only, not total process/native/scratch memory. */
internal object GlowBudget {
    const val primaryBytes = 16L * 1024 * 1024
    const val exitingBytes = 16L * 1024 * 1024
    const val perExitBytes = exitingBytes / MAX_EXITING_EPOCHS

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
