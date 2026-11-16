package top.ghhccghk.accompanist.folia.sample

import top.ghhccghk.accompanist.folia.sample.playback.*
import kotlin.random.Random
import kotlin.test.*

class RemotePlaybackMirrorTest {
    private fun state(revision: Long = 1, command: Long = 0, position: Long = 1000, playing: Boolean = true,
        seeking: Boolean = false, buffering: Boolean = false, speed: Float = 1f, discontinuity: Long = 1) =
        RemotePlaybackState("song",revision,command,position,60_000,1_000_000_000,playing,seeking,buffering,speed,discontinuity)

    @Test fun sampledPositionAndSpeedAreAppliedOnce() {
        val mirror=RemotePlaybackMirror("song")
        assertTrue(mirror.accept(state(speed=2f)))
        assertEquals(1200,mirror.snapshot(1_100_000_000).positionMillis)
        assertEquals(1500,mirror.snapshot(4_000_000_000).positionMillis)
        assertFalse(mirror.snapshot(4_000_000_000).isPlaying)
        assertEquals(1000,mirror.snapshot(900_000_000).positionMillis)
        assertTrue(mirror.accept(state(revision=2,position=4500).copy(anchorNanos=4_000_000_000)))
        assertTrue(mirror.snapshot(4_000_000_000).isPlaying)
    }
    @Test fun pauseAndBufferingFreeze() {
        for(packet in listOf(state(playing=false),state(buffering=true),state(seeking=true))) {
            val mirror=RemotePlaybackMirror("song");mirror.accept(packet)
            assertEquals(1000,mirror.snapshot(20_000_000_000).positionMillis)
            assertFalse(mirror.snapshot(20_000_000_000).isPlaying)
        }
        val mirror=RemotePlaybackMirror("song");mirror.accept(state())
        mirror.command(RemotePlaybackCommand.Pause,1_100_000_000)
        assertEquals(1100,mirror.snapshot(10_000_000_000).positionMillis)
        assertFalse(mirror.accept(state(revision=2)))
        assertTrue(mirror.accept(state(revision=3,command=1,position=1111,playing=false)))
        assertEquals(1111,mirror.snapshot(10_000_000_000).positionMillis)
    }
    @Test fun rapidSeeksRejectOldPacketsAndDoNotDoubleCountDiscontinuity() {
        val mirror=RemotePlaybackMirror("song");mirror.accept(state())
        val initial=mirror.snapshot(1_000_000_000).discontinuity
        mirror.command(RemotePlaybackCommand.Seek,1_000_000_000,50_000)
        mirror.command(RemotePlaybackCommand.Seek,1_000_000_000,500)
        assertEquals(500,mirror.snapshot(2_000_000_000).positionMillis)
        assertEquals(initial+2,mirror.snapshot(2_000_000_000).discontinuity)
        assertFalse(mirror.accept(state(revision=3,command=1,position=50_000)))
        assertTrue(mirror.accept(state(revision=4,command=2,position=500,seeking=true,discontinuity=3)))
        assertTrue(mirror.isSeeking)
        assertTrue(mirror.accept(state(revision=5,command=2,position=500,discontinuity=3)))
        assertFalse(mirror.isSeeking)
        assertEquals(initial+2,mirror.snapshot(1_000_000_000).discontinuity)
        assertFalse(mirror.accept(state(revision=4,command=2,position=50_000)))
        assertFalse(mirror.accept(state(revision=6,command=2).copy(session="old-song")))
    }
    @Test fun randomSeekReconstructionAndClamping() {
        val random=Random(20261116)
        val mirror=RemotePlaybackMirror("song");mirror.accept(state())
        repeat(1000) { i ->
            val target=random.nextLong(-20_000,80_000)
            val sequence=mirror.command(RemotePlaybackCommand.Seek,1_000_000_000,target)
            assertEquals(target.coerceIn(0,60_000),mirror.snapshot(3_000_000_000).positionMillis)
            assertTrue(mirror.accept(state(revision=2L+i,command=sequence,position=target.coerceIn(0,60_000),
                playing=false,discontinuity=2L+i)))
            assertEquals(target.coerceIn(0,60_000),mirror.snapshot(3_000_000_000).positionMillis)
        }
    }
    @Test fun deathCannotBeRevivedByLatePackets() {
        val mirror=RemotePlaybackMirror("song");mirror.accept(state())
        mirror.freeze(1_100_000_000)
        assertEquals(1100,mirror.snapshot(10_000_000_000).positionMillis)
        assertFalse(mirror.snapshot(10_000_000_000).isPlaying)
        assertFalse(mirror.accept(state(revision=999,position=5000)))
        assertFailsWith<IllegalStateException> { mirror.command(RemotePlaybackCommand.Play,2_000_000_000) }
    }
    @Test fun completionAndReplayReanchor() {
        val mirror=RemotePlaybackMirror("song");mirror.accept(state(position=60_000,playing=false))
        val before=mirror.snapshot(1_000_000_000).discontinuity
        mirror.command(RemotePlaybackCommand.Play,1_000_000_000)
        assertEquals(0,mirror.snapshot(1_000_000_000).positionMillis)
        mirror.accept(state(revision=2,command=1,position=0,seeking=true,discontinuity=2))
        mirror.accept(state(revision=3,command=1,position=0,discontinuity=2))
        assertEquals(before+1,mirror.snapshot(1_000_000_000).discontinuity)
        assertEquals(100,mirror.snapshot(1_100_000_000).positionMillis)
    }
    @Test fun invalidTransportDataIsRejected() {
        for(packet in listOf(state(speed=Float.NaN),state(position=-1),state(command=100),state().copy(durationMillis=0))) {
            assertFailsWith<IllegalArgumentException> { RemotePlaybackMirror("song").accept(packet) }
        }
    }
}
