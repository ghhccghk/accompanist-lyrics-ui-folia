package top.ghhccghk.accompanist.folia.sample

import top.ghhccghk.accompanist.folia.sample.playback.PreviewPlaybackController
import kotlin.test.*

class PreviewPlaybackTest {
    @Test fun anchoringIsIndependentOfRefreshFrequencyAndPausesFreeze() {
        var now = 0L
        val clock = PreviewPlaybackController(10_000) { now }
        clock.play()
        repeat(100) { now += 1_000_000; clock.refresh() }
        now = 2_000_000_000
        assertEquals(2_000, clock.refresh().positionMillis)
        clock.pause()
        val paused = clock.snapshot
        now += 20_000_000_000
        assertEquals(paused, clock.refresh())
        clock.play()
        now += 1_000_000_000
        assertEquals(3_000, clock.refresh().positionMillis)
    }
    @Test fun speedChangesRebaseAndSeekPreservesPlayState() {
        var now = 0L
        val clock = PreviewPlaybackController(10_000) { now }
        clock.play()
        now = 1_000_000_000
        clock.changeSpeed(2.0)
        assertEquals(1_000, clock.snapshot.positionMillis)
        now += 500_000_000
        assertEquals(2_000, clock.refresh().positionMillis)
        assertEquals(0, clock.snapshot.discontinuity)
        clock.seek(100)
        assertTrue(clock.snapshot.isPlaying)
        assertEquals(1, clock.snapshot.discontinuity)
        clock.pause()
        clock.seek(-50)
        assertFalse(clock.snapshot.isPlaying)
        assertEquals(0, clock.snapshot.positionMillis)
    }
    @Test fun loopHandlesMultipleWrapsWithoutAccumulatingFrameError() {
        var now = 0L
        val clock = PreviewPlaybackController(1_000) { now }
        clock.looping = true
        clock.play()
        now = 3_250_000_000
        assertEquals(250, clock.refresh().positionMillis)
        assertEquals(3, clock.snapshot.discontinuity)
        clock.looping = false
        now += 750_000_000
        assertFalse(clock.refresh().isPlaying)
        assertEquals(1_000, clock.snapshot.positionMillis)
    }
    @Test fun invalidSpeedAndDurationAreExplicitErrors() {
        assertFailsWith<IllegalArgumentException> { PreviewPlaybackController(0) }
        val clock = PreviewPlaybackController(1_000) { 0 }
        assertFailsWith<IllegalArgumentException> { clock.changeSpeed(Double.NaN) }
        assertFailsWith<IllegalArgumentException> { clock.changeSpeed(0.0) }
    }
}
