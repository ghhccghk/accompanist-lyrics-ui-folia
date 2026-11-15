package com.mocharealm.accompanist.folia.sample

import android.os.Bundle
import android.net.Uri
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mocharealm.accompanist.folia.sample.playback.*
import com.mocharealm.accompanist.folia.sample.importing.parseSongLyrics
import kotlinx.coroutines.*
import java.io.File

/** Debug-only explicit integration probe; input must be an original generated 12-second WAV. */
class LocalAudioVerificationActivity:ComponentActivity() {
    private val session=SampleSession()
    private val scope=MainScope()
    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        setContent{FoliaSampleApp(session=session,settingsStore=null)}
        scope.launch {
            val records=mutableListOf("Native Android MediaPlayer; no preview clock; API="+android.os.Build.VERSION.SDK_INT)
            try {
                val source=AndroidSongFile(applicationContext,Uri.fromFile(File(filesDir,"verification.wav")),"verification.wav")
                check(session.songs.open(SongFiles(source),::prepareAndroidAudio)){session.songs.error?:"prepare failed"}
                val player=session.songs.current!!.player
                records+="durationMs="+player.durationMillis;check(player.durationMillis in 11990..12010)
                delay(700);session.songs.poll();val playing=session.songs.snapshot
                check(playing.isPlaying && playing.positionMillis in 150..2000){"Native cursor did not advance: "+playing};records+="playing="+playing
                session.songs.control{it.pause()};val paused=session.songs.snapshot;delay(300);session.songs.poll()
                check(session.songs.snapshot==paused){"Paused cursor changed"};records+="pauseStable="+paused
                session.songs.control{it.seek(6000)}
                withTimeout(3000){while(player.isSeeking || player.snapshot.positionMillis !in 5950..6050 || player.snapshot.isPlaying){delay(20);session.songs.poll()}}
                check(session.songs.snapshot.discontinuity>paused.discontinuity);records+="pausedSeek="+session.songs.snapshot
                session.songs.control{it.play()};delay(400);session.songs.poll();check(session.songs.snapshot.positionMillis>6200);records+="resume="+session.songs.snapshot
                session.songs.control{it.pause();it.seek(1000)};delay(200);session.songs.control{it.play()};delay(300);session.songs.poll()
                check(session.songs.snapshot.positionMillis in 1100..2000);records+="reverseSeek="+session.songs.snapshot
                session.songs.control{it.pause()}
                if(intent.getBooleanExtra("attachLyrics",false)) {
                    val before=session.songs.snapshot
                    check(session.songs.attachLyrics(SongFiles(source)) {
                        parseSongLyrics("[00:06.00]Amber\n[00:08.00]Echo".encodeToByteArray())
                    })
                    check(player===session.songs.current!!.player)
                    check(before==session.songs.snapshot)
                    check(session.clock.snapshot.positionMillis==0L)
                    records+="lyricsAttachedSameNativePlayer="+session.songs.snapshot
                }
                records+="SUCCESS"
                File(filesDir,"local-audio-verification.txt").writeText(records.joinToString("\n")+"\n")
                Log.i("FoliaLocalAudio","SUCCESS "+records.joinToString(";"))
            } catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Throwable) {
                File(filesDir,"local-audio-verification.txt").writeText(records.joinToString("\n")+"\nFAILED "+failure.stackTraceToString())
                Log.e("FoliaLocalAudio","FAILED",failure)
            }
        }
    }
    override fun onStop(){session.songs.control{it.pause()};super.onStop()}
    override fun onDestroy(){scope.cancel();session.songs.close();super.onDestroy()}
}
