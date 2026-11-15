// SPDX-License-Identifier: Apache-2.0
// Adapted from Accompanist contributors, 6b1ab72319bf2143225acf7797031efa7491897c.
// Original: lyrics/ui/internal/effects/PreparedBreathingDots.kt; see docs/upstream.md.
package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.folia.internal.waiting.*

import kotlin.math.abs
import kotlin.test.*

class WaitingDotsTest {
    @Test
    fun phaseBoundariesStayContinuousForShortAndLongGaps() {
        for (duration in listOf(1, 5001, 6400, 6401, 6416, 6417, 7000, 10000, 18000)) {
            val dots = PreparedBreathingDots(0, duration, WaitingDotsDefaults())
            assertEquals(0f, dots.scale(-1f))
            assertEquals(0f, dots.alpha(duration.toFloat()))
            for (boundary in
                listOf(dots.enterEnd, dots.dipStart, dots.stillStart, dots.exitStart)) {
                assertTrue(
                    abs(dots.scale(boundary - 0.0001f) - dots.scale(boundary + 0.0001f)) < 0.003f,
                    "Scale jumps at $boundary / $duration",
                )
            }
        }
    }

    @Test
    fun lightsProgressThroughWholeMiddleWindowAndSlotClosesSmoothly() {
        val dots = PreparedBreathingDots(1000, 11000, WaitingDotsDefaults())
        assertEquals(0f, dots.elapsed(1000))
        for (index in 0..2) assertEquals(0.2f, dots.dotAlpha(index, dots.enterEnd))
        val midpoint = (dots.enterEnd + dots.exitStart) / 2f
        assertEquals(1f, dots.dotAlpha(0, midpoint))
        assertEquals(0.6f, dots.dotAlpha(1, midpoint), 0.0001f)
        assertEquals(0.2f, dots.dotAlpha(2, midpoint))
        for (index in 0..2) assertEquals(1f, dots.dotAlpha(index, dots.exitStart), 0.0001f)
        assertEquals(0.5f, dots.visibility(110f), 0.0001f)
        assertEquals(0.5f, dots.visibility(dots.duration - 110f), 0.0001f)
        assertEquals(0f, dots.visibility(dots.duration))
        assertEquals(1f, dots.scale(dots.dipStart), 0.0001f)
        assertEquals(0.6f, dots.scale((dots.dipStart + dots.stillStart) / 2f), 0.0001f)
    }

    @Test
    fun invalidIntervalsRemainHiddenAndConfigurationIsBounded() {
        val dots =
            PreparedBreathingDots(
                2000,
                1000,
                WaitingDotsDefaults(
                    number = 0,
                    enterDurationMs = 0,
                    preExitDipAndRiseDuration = 0,
                    exitDurationMs = 0,
                ),
            )
        assertEquals(1, dots.number)
        assertEquals(0f, dots.scale(0f))
        assertEquals(0f, dots.alpha(0f))
        assertEquals(0f, dots.visibility(0f))
    }
}
