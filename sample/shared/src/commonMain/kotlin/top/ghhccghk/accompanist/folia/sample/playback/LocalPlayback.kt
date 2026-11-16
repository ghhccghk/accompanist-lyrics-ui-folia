package top.ghhccghk.accompanist.folia.sample.playback

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import top.ghhccghk.accompanist.folia.sample.importing.LyricsSource
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import kotlinx.coroutines.CancellationException

enum class FileRole { Audio, Lyrics, Translation }
data class SongFiles(val audio: LyricsSource, val lyrics: LyricsSource? = null, val translation: LyricsSource? = null)
data class SongMetadata(val title: String, val artist: String = "未知作者", val artwork: ImageBitmap? = null)

/** Sample only. Position comes from the native transport, without a second clock. */
interface LocalAudioPlayer {
    val metadata: SongMetadata
    val durationMillis: Long
    val snapshot: FoliaPlaybackSnapshot
    val isSeeking: Boolean get() = false
    /** Asynchronous transport events; default desktop transport remains polled. */
    val updateRevision: Long get() = 0
    fun poll()
    fun play()
    fun pause()
    fun seek(positionMillis: Long)
    fun close()
}
@Composable expect fun rememberSongFilePicker(onSelected: (FileRole, LyricsSource?) -> Unit, onError: (String) -> Unit): (FileRole) -> Unit
@Composable expect fun rememberAudioPreparation(): suspend (LyricsSource) -> LocalAudioPlayer
expect val localAudioFormats: String
data class LoadedSong(val files: SongFiles, val player: LocalAudioPlayer, val lyrics: SyncedLyrics?, val revision: Long)

/** UI-thread confined. A candidate owns its resources until a generation-checked swap. */
class SongSession {
    var current by mutableStateOf<LoadedSong?>(null); private set
    var loading by mutableStateOf(false); private set
    var error by mutableStateOf<String?>(null); private set
    private var generation = 0L
    private var closed = false
    var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false)); private set
    private var nativeRevision: Long? = null
    private var epoch = 0L
    private var playbackFault = false
    var filePickerActive by mutableStateOf(false); private set
    private var resumeAfterPicker: LocalAudioPlayer? = null
    fun beginFileChoice() {
        poll()
        resumeAfterPicker = current?.player?.takeIf { snapshot.isPlaying }
        filePickerActive=true
        control { it.pause() }
    }
    fun finishFileChoice() { filePickerActive=false }
    fun resumeFileChoice() {
        val previous=resumeAfterPicker;resumeAfterPicker=null
        if(previous != null && previous === current?.player) control {it.play()}
    }
    fun poll() {
        val player = current?.player ?: return
        if (playbackFault) return
        try {
            player.poll()
            if (nativeRevision != player.snapshot.discontinuity) { epoch++; nativeRevision = player.snapshot.discontinuity }
            snapshot = player.snapshot.copy(discontinuity = epoch)
        } catch (failure: Exception) { playbackFault=true; error = failure.message ?: failure.toString(); snapshot = snapshot.copy(isPlaying=false) }
    }
    fun control(action: (LocalAudioPlayer) -> Unit) {
        try { current?.player?.let(action); poll() }
        catch (failure: Exception) { error = failure.message ?: failure.toString() }
    }
    fun cancelPending() { ++generation; loading = false }
    fun dismissError() { error = null }
    suspend fun attachLyrics(files: SongFiles, parse: suspend (SongFiles) -> SyncedLyrics?): Boolean {
        val original=current ?: return false
        val token=++generation
        loading=true;error=null
        try {
            require(files.audio === original.files.audio) { "补选只能应用于原音频，请使用重新选歌" }
            val lyrics=parse(files)
            if(token!=generation || closed || current !== original) return false
            current=original.copy(files=files,lyrics=lyrics,revision=token)
            return true
        } catch(cancelled:CancellationException){throw cancelled}
        catch(failure:Exception){if(token==generation && !closed)error=failure.message?:failure.toString();return false}
        finally {if(token==generation)loading=false}
    }
    suspend fun open(files: SongFiles, prepare: suspend (LyricsSource) -> LocalAudioPlayer,
        parse: suspend (SongFiles) -> SyncedLyrics? = { null }): Boolean {
        check(!closed)
        val token = ++generation
        loading = true; error = null
        var candidate: LocalAudioPlayer? = null
        try {
            val lyrics = parse(files)
            candidate = prepare(files.audio)
            if (token != generation || closed) return false
            candidate.play()
            val previous = current
            current = LoadedSong(files, candidate, lyrics, token)
            candidate = null
            playbackFault=false; nativeRevision = null; poll()
            previous?.player?.close()
            return true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { if (token == generation && !closed) error = failure.message ?: failure.toString(); return false }
        finally { candidate?.close(); if (token == generation) loading = false }
    }
    fun close() { closed = true; cancelPending(); current?.player?.close(); current = null }
}
