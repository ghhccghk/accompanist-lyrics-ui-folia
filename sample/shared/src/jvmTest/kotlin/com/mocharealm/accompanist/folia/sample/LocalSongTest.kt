package com.mocharealm.accompanist.folia.sample

import com.mocharealm.accompanist.folia.sample.playback.*
import com.mocharealm.accompanist.folia.sample.importing.*
import com.mocharealm.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import kotlinx.coroutines.*
import kotlin.test.*

class LocalSongTest {
    private val source=object:LyricsSource { override val name="synthetic.wav"; override suspend fun read()=byteArrayOf() }
    private class Player:LocalAudioPlayer {
        override val metadata=SongMetadata("Synthetic")
        override val durationMillis=10000L
        override var snapshot=FoliaPlaybackSnapshot(0,false,1)
        var closed=false
        override fun poll() {}
        override fun play(){snapshot=snapshot.copy(isPlaying=true)}
        override fun pause(){snapshot=snapshot.copy(isPlaying=false)}
        override fun seek(positionMillis:Long){snapshot=snapshot.copy(positionMillis=positionMillis,discontinuity=snapshot.discontinuity+1)}
        override fun close(){closed=true;pause()}
    }
    @Test fun failuresCancellationAndStalePreparationPreserveSession()=runBlocking {
        val session=SongSession();val old=Player()
        assertTrue(session.open(SongFiles(source),{old}))
        assertFalse(session.open(SongFiles(source),{error("decoder rejected")}))
        assertSame(old,session.current!!.player);assertFalse(old.closed);assertNotNull(session.error)
        val first=Player();val second=Player();val gate=CompletableDeferred<Unit>()
        val pending=async(start=CoroutineStart.UNDISPATCHED){session.open(SongFiles(source),{gate.await();first})}
        assertTrue(session.open(SongFiles(source),{second}));gate.complete(Unit)
        assertFalse(pending.await());assertTrue(first.closed);assertTrue(old.closed);assertSame(second,session.current!!.player)
        val cancelled=Player();val gate2=CompletableDeferred<Unit>()
        val task=async(start=CoroutineStart.UNDISPATCHED){session.open(SongFiles(source),{gate2.await();cancelled})}
        session.cancelPending();gate2.complete(Unit)
        assertFalse(task.await());assertTrue(cancelled.closed);assertSame(second,session.current!!.player)
        session.close();assertTrue(second.closed)
    }
    @Test fun nativePositionIsNotMultipliedAndDiscontinuitiesSurviveTrackChanges()=runBlocking {
        val session=SongSession();val one=Player();session.open(SongFiles(source),{one})
        val initial=session.snapshot.discontinuity
        session.control{it.seek(4200)}
        assertEquals(4200,session.snapshot.positionMillis);assertTrue(session.snapshot.discontinuity>initial)
        val seekRevision=session.snapshot.discontinuity
        val two=Player();session.open(SongFiles(source),{two})
        assertTrue(session.snapshot.discontinuity>seekRevision);assertEquals(0,session.snapshot.positionMillis)
        session.control{it.pause()};repeat(30){session.poll()};assertFalse(session.snapshot.isPlaying);assertEquals(0,session.snapshot.positionMillis)
        session.close()
    }
    @Test fun translationMatchesSourceTimestampsAndRejectsUnsupportedInput() {
        val lyrics=parseSongLyrics("[00:01.00]Amber\n[00:03.00]Echo".encodeToByteArray(),"[00:01.00]光影\n[00:03.00]世界".encodeToByteArray())
        assertEquals(listOf("光影","世界"),lyrics.lines.map{(it as SyncedLine).translation})
        assertFailsWith<IllegalArgumentException>{parseSongLyrics("not timed".encodeToByteArray())}
        assertFailsWith<IllegalArgumentException>{parseSongLyrics("[00:01.00]Amber".encodeToByteArray(),"[00:02.00]光影".encodeToByteArray())}
        assertFailsWith<Exception>{parseSongLyrics(byteArrayOf(0xC3.toByte()))}
    }
    @Test fun cancelledNativePickerRestoresOnlyItsOwnPreviouslyPlayingSession()=runBlocking {
        val session=SongSession();val first=Player();session.open(SongFiles(source),{first})
        session.beginFileChoice();assertFalse(first.snapshot.isPlaying)
        session.finishFileChoice();session.resumeFileChoice();assertTrue(first.snapshot.isPlaying)
        session.control{it.pause()};session.beginFileChoice();session.finishFileChoice();session.resumeFileChoice()
        assertFalse(first.snapshot.isPlaying)
        session.control{it.play()};session.beginFileChoice()
        val second=Player();session.open(SongFiles(source),{second});session.control{it.pause()}
        session.finishFileChoice();session.resumeFileChoice();assertFalse(second.snapshot.isPlaying)
        session.close()
    }
}
