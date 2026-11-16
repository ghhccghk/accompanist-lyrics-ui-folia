package top.ghhccghk.accompanist.folia.sample

import android.app.Activity
import android.app.Instrumentation
import android.net.Uri
import android.os.Bundle
import android.os.Process
import kotlinx.coroutines.*
import top.ghhccghk.accompanist.folia.sample.playback.*
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Explicit isolated-package probe. Never launches an Activity or opens user audio.
 * Generated silent PCM exercises a real MediaPlayer, not a preview playback clock. */
class PlaybackProcessDeviceProbe : Instrumentation() {
    override fun onCreate(arguments: Bundle?) { super.onCreate(arguments); start() }
    override fun onStart() {
        val result = Bundle()
        try {
            check(targetContext.packageName.endsWith(".playbackprobe")) { "Requires isolated probe application ID" }
            runBlocking { withTimeout(35_000) { verifyPlayback() } }
            result.putString("status", "PASS")
            result.putString("stream", "PASS: separate PID, real position advance, pause, reverse seeks, failed candidate, peer close, process death, fresh recovery, bridge release. Silent PCM; no audible/UI/frame-performance claim.\n")
            finish(Activity.RESULT_OK, result)
        } catch (error: Throwable) {
            result.putString("status", "FAIL")
            result.putString("stream", "FAIL: ${error.stackTraceToString()}\n")
            finish(Activity.RESULT_CANCELED, result)
        }
    }
    private suspend fun verifyPlayback() = withContext(Dispatchers.Main.immediate) {
        val file = File(targetContext.cacheDir, "generated-playback-probe.wav")
        val bytes = ByteBuffer.allocate(44 + 12 * 8000 * 2).order(ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(bytes.capacity()-8).put("WAVEfmt ".toByteArray())
        bytes.putInt(16).putShort(1).putShort(1).putInt(8000).putInt(16000).putShort(2).putShort(16)
        bytes.put("data".toByteArray()).putInt(bytes.capacity()-44)
        withContext(Dispatchers.IO) { file.writeBytes(bytes.array()) }
        val players = mutableListOf<LocalAudioPlayer>()
        suspend fun prepare(): LocalAudioPlayer = prepareAndroidAudio(AndroidSongFile(targetContext, Uri.fromFile(file),file.name)).also { players.add(it) }
        suspend fun until(player: LocalAudioPlayer, condition: () -> Boolean) {
            withTimeout(5000) { while (true) { player.poll(); if (condition()) break; delay(25) } }
        }
        fun pid(player: LocalAudioPlayer): Int = player.javaClass.getDeclaredField("servicePid").apply { isAccessible=true }.getInt(player)
        try {
            val first = prepare()
            val remotePid = pid(first)
            check(remotePid > 0 && remotePid != Process.myPid()) { "Player did not move to a separate process" }
            check(first.durationMillis in 11900..12100)
            first.play()
            until(first) { first.snapshot.isPlaying && first.snapshot.positionMillis >= 350 }
            first.pause()
            delay(250); first.poll()
            check(!first.snapshot.isPlaying)
            val paused = first.snapshot.positionMillis
            delay(350); first.poll()
            check(first.snapshot.positionMillis == paused) { "Paused position drifted" }
            val epoch = first.snapshot.discontinuity
            first.seek(8000); first.seek(2500); first.seek(6000); first.seek(1000)
            until(first) { !first.isSeeking && kotlin.math.abs(first.snapshot.positionMillis-1000) < 200 }
            check(first.snapshot.discontinuity > epoch)
            first.play()
            until(first) { first.snapshot.isPlaying && first.snapshot.positionMillis > 1300 }
            val missing = File(targetContext.cacheDir,"does-not-exist-probe.wav")
            var rejected = false
            try { prepareAndroidAudio(AndroidSongFile(targetContext,Uri.fromFile(missing),missing.name)).close() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { rejected = true }
            check(rejected)
            until(first) { first.snapshot.isPlaying }
            val peer = prepare()
            check(pid(peer) == remotePid)
            peer.close()
            until(first) { first.snapshot.isPlaying }
            // All clients in this process have the isolated package UID. Never
            // kill a PID obtained from an arbitrary package/system inventory.
            Process.killProcess(remotePid)
            var disconnected = false
            withTimeout(5000) {
                while (!disconnected) {
                    delay(25)
                    try { first.poll() } catch (_: IllegalStateException) { disconnected = true }
                }
            }
            check(!first.snapshot.isPlaying)
            val frozen = first.snapshot.positionMillis
            delay(300)
            try { first.poll() } catch (_: IllegalStateException) { /* terminal */ }
            check(first.snapshot.positionMillis == frozen)
            first.close()
            val recovered = prepare()
            check(pid(recovered) != remotePid)
            recovered.play()
            until(recovered) { recovered.snapshot.isPlaying && recovered.snapshot.positionMillis >= 250 }
        } finally {
            players.forEach { it.close() }
            withContext(NonCancellable + Dispatchers.IO) { file.delete() }
        }
        withTimeout(5000) {
            while (Thread.getAllStackTraces().keys.any { it.isAlive && it.name.startsWith("FoliaPlaybackClient-") }) delay(25)
        }
    }
}
