package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.core.model.*
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.text.*
import com.mocharealm.accompanist.lyrics.folia.internal.animation.*
import kotlin.random.Random
import kotlin.test.*

class ClassicSamplerTest {
    @Test fun referenceThresholdsAndRenderWindows() {
        assertEquals(Reveal.Instant, renderProfile(0, 99_999).mode)
        assertEquals(Reveal.Fast, renderProfile(0, 100_000).mode)
        assertEquals(Reveal.Fast, renderProfile(0, 179_999).mode)
        assertEquals(Reveal.Normal, renderProfile(0, 180_000).mode)
        assertEquals(67_000, renderProfile(0, 0).renderEndUs)
        assertEquals(1_240_000, renderProfile(0, 1_000_000).renderEndUs)
        assertEquals(0f, spring(0.0))
        assertEquals(1f, spring(2.0), .0001f)
    }
    @Test fun randomSeeksRebuildExactlyAndProduceFiniteValues() {
        val profile = renderProfile(0, 3_000_000)
        val pose = wordPose(0, 1, true)
        val random = Random(41)
        repeat(2_000) {
            val time = random.nextLong(-500_000, 4_000_000)
            val direct = sampleWord(time, 0, 1_000_000, 1_000_000, profile, pose, false)
            sampleWord(random.nextLong(0, 4_000_000), 0, 1_000_000, 1_000_000, profile, pose, false)
            assertEquals(direct, sampleWord(time, 0, 1_000_000, 1_000_000, profile, pose, false))
            assertTrue(listOf(direct.x, direct.y, direct.scale, direct.rotation, direct.alpha, direct.mix, direct.blur).all { it.isFinite() })
        }
    }
    @Test fun shortWordDoesNotJumpAtPassedBoundary() {
        val profile = renderProfile(0, 500_000)
        val pose = wordPose(0, 0, false)
        val before = sampleWord(20_000, 0, 0, 20_000, profile, pose, false)
        val after = sampleWord(20_001, 0, 0, 20_000, profile, pose, false)
        assertEquals(before.scale, after.scale, .0001f)
        assertEquals(0f, after.rotation)
    }
    @Test fun segmentationKeepsGraphemesLigaturesAndContextualRuns() {
        val grapheme = "👩‍🚀"
        assertEquals(setOf(0, grapheme.length), graphemeBoundaries(grapheme))
        assertEquals(setOf(0, 2), graphemeBoundaries("á"))
        fun units(parts: List<String>) = displayUnits(normalize(SyncedLyrics(listOf(
            KaraokeLine.MainKaraokeLine(parts.mapIndexed { i, text -> KaraokeSyllable(text, i * 100, (i + 1) * 100) },
                null, KaraokeAlignment.Unspecified, 0, parts.size * 100)))).lines.single())
        assertEquals(listOf("tonight"), units(listOf("to", "night")).map { it.text })
        assertEquals(listOf("á"), units(listOf("a", "́")).map { it.text })
        assertEquals(listOf("مرحبا بالعالم"), units(listOf("مرحبا ", "بالعالم")).map { it.text })
    }
}
