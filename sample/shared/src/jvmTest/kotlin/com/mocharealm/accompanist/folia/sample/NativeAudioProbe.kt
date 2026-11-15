package com.mocharealm.accompanist.folia.sample

import com.mocharealm.accompanist.folia.sample.playback.*
import kotlinx.coroutines.*
import java.io.File

/** Explicit hardware verification task, not a device-dependent unit test. */
fun main(args:Array<String>)=runBlocking {
    require(args.size==2){"Arguments: generated WAV input, evidence output"}
    val output=File(args[1]).apply{parentFile.mkdirs()}
    val player=prepareDesktopAudio(DesktopSongFile(File(args[0])))
    val records=mutableListOf("JDK Java Sound actual Clip; no preview clock; no extrapolation","durationMs="+player.durationMillis)
    try {
        check(player.durationMillis in 11990..12010)
        player.play();delay(650);player.poll();val first=player.snapshot
        check(first.isPlaying && first.positionMillis in 200..1800){"Device cursor did not advance: "+first}
        records+="playing="+first
        player.pause();val paused=player.snapshot;delay(300);player.poll()
        check(player.snapshot==paused){"Native pause changed"};records+="pauseStable="+paused
        player.seek(6000);val sought=player.snapshot
        check(kotlin.math.abs(sought.positionMillis-6000)<=2);check(!sought.isPlaying);check(sought.discontinuity>paused.discontinuity)
        records+="pausedSeek="+sought
        player.play();delay(400);player.poll();check(player.snapshot.positionMillis>6200);records+="resume="+player.snapshot
        player.pause();player.seek(1000);player.play();delay(300);player.poll();check(player.snapshot.positionMillis in 1100..2000)
        records+="reverseSeek="+player.snapshot
    } finally {player.close();records+="closed=true";output.writeText(records.joinToString("\n")+"\n")}
    println("NATIVE_AUDIO_VERIFIED "+output.absolutePath)
}
