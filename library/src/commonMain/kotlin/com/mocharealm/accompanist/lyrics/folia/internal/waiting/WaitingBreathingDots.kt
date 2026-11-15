// SPDX-License-Identifier: Apache-2.0
// Adapted from Accompanist contributors, 6b1ab72319bf2143225acf7797031efa7491897c.
// Original: lyrics/ui/composable/lyrics/KaraokeBreathingDots.kt; see docs/upstream.md.
package com.mocharealm.accompanist.lyrics.folia.internal.waiting

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal data class WaitingDotsDefaults(
    val number: Int = 3,
    val size: Dp = 16.dp,
    val margin: Dp = 12.dp,
    val enterDurationMs: Int = 3000,
    val preExitStillDuration: Int = 200,
    val preExitDipAndRiseDuration: Int = 3000,
    val exitDurationMs: Int = 200,
    val breathingDotsColor: Color = Color.White,
)

/** Same phase/radius/spacing/dot-alpha calculation; a centred fixed stage, not a collapsing list row. */
@Composable
internal fun WaitingBreathingDots(
    startTimeMs: Int,
    endTimeMs: Int,
    currentTimeProvider: () -> Int,
    modifier: Modifier = Modifier,
    reducedMotion: Boolean = false,
    defaults: WaitingDotsDefaults = WaitingDotsDefaults(),
) {
    val currentTime by rememberUpdatedState(currentTimeProvider)
    val timeline = remember(startTimeMs, endTimeMs, defaults) {
        PreparedBreathingDots(startTimeMs, endTimeMs, defaults)
    }
    Canvas(modifier.fillMaxWidth().height(defaults.size.coerceAtLeast(1.dp).coerceAtLeast(40.dp))) {
        val time = timeline.elapsed(currentTime())
        val alpha = timeline.alpha(time)
        // Reduced motion retains media-time progress but removes breathing/entry spatial motion.
        val scale = if (reducedMotion) 1f else timeline.scale(time)
        val diameterPx = defaults.size.coerceAtLeast(1.dp).toPx()
        val marginPx = defaults.margin.coerceAtLeast(0.dp).toPx()
        val totalWidth = diameterPx * timeline.number + marginPx * (timeline.number - 1)
        if (scale > 0f && alpha > 0f) {
            val origin = (size.width - totalWidth) * .5f
            val center = origin + totalWidth * .5f
            val radius = diameterPx * .5f * scale
            for (index in 0 until timeline.number) {
                val baseX = origin + diameterPx * .5f + (diameterPx + marginPx) * index
                drawCircle(
                    color = defaults.breathingDotsColor,
                    radius = radius,
                    center = Offset(center + (baseX - center) * scale, size.height * .5f),
                    alpha = alpha * timeline.dotAlpha(index, time),
                )
            }
        }
    }
}
