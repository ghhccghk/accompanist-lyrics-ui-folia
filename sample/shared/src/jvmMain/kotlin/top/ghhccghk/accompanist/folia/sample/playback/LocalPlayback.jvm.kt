package top.ghhccghk.accompanist.folia.sample.playback

import androidx.compose.runtime.*
import top.ghhccghk.accompanist.folia.sample.importing.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import kotlinx.coroutines.*
import java.awt.EventQueue
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import javax.sound.sampled.*

internal class DesktopSongFile(val file: File) : LyricsSource {
    override val name get() = file.name
    override suspend fun read() = withContext(Dispatchers.IO) {
        file.inputStream().use { it.readNBytes(MAX_IMPORT_BYTES + 1).also { bytes ->
            require(bytes.size <= MAX_IMPORT_BYTES) { "歌词文件超过 1 MiB" }
        } }
    }
}
actual val localAudioFormats = "Desktop：JDK 支持的 PCM WAV / AIFF / AU；不支持的编码会明确报错"

@Composable actual fun rememberSongFilePicker(onSelected: (FileRole, LyricsSource?) -> Unit, onError: (String) -> Unit): (FileRole) -> Unit {
    val scope = rememberCoroutineScope()
    val selected by rememberUpdatedState(onSelected); val failed by rememberUpdatedState(onError)
    return { role -> scope.launch {
        try {
            val file = withContext(Dispatchers.IO) {
                var value: File? = null
                EventQueue.invokeAndWait {
                    val dialog = FileDialog(null as Frame?, if (role == FileRole.Audio) "选择音频（PCM WAV / AIFF / AU）" else "选择计时歌词", FileDialog.LOAD)
                    try { dialog.isVisible = true; dialog.file?.let { value = File(dialog.directory, it) } } finally { dialog.dispose() }
                }; value
            }
            selected(role,file?.let(::DesktopSongFile))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Exception) { failed("文件选择失败：" + (failure.cause?.message ?: failure.message)) }
    }; Unit }
}

@Composable actual fun rememberAudioPreparation(): suspend (LyricsSource) -> LocalAudioPlayer = remember { { source -> prepareDesktopAudio(source) } }

/** Bounded decoded clip. Position is the device sample cursor, never elapsed wall time. */
internal suspend fun prepareDesktopAudio(source: LyricsSource): LocalAudioPlayer {
    val file = (source as? DesktopSongFile)?.file ?: error("音频不是本地选择器提供的文件")
    var acquired: Clip? = null
    try {
        return withContext(Dispatchers.IO) {
            val format = AudioSystem.getAudioFileFormat(file)
            AudioSystem.getAudioInputStream(file).use { stream ->
                val frames = stream.frameLength
                require(frames > 0 && stream.format.frameSize > 0 && frames <= (128L * 1024 * 1024) / stream.format.frameSize) {
                    "Desktop 音频需有有限帧数，解码 PCM 不超过 128 MiB"
                }
                val clip = AudioSystem.getClip().also { acquired = it }
                clip.open(stream)
                val properties = format.properties()
                DesktopAudioPlayer(clip, SongMetadata((properties["title"] as? String)?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension,
                    (properties["author"] as? String)?.takeIf { it.isNotBlank() } ?: "未知作者"))
            }
        }
    } catch (failure: Exception) { acquired?.close(); throw failure }
}

internal class DesktopAudioPlayer(private val clip: Clip, override val metadata: SongMetadata) : LocalAudioPlayer {
    override val durationMillis = (clip.microsecondLength / 1000).coerceAtLeast(1)
    override var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0, false)); private set
    private var closed = false
    private var discontinuity = 1L
    override fun poll() { if (!closed) snapshot = FoliaPlaybackSnapshot((clip.microsecondPosition / 1000).coerceIn(0, durationMillis), clip.isRunning, discontinuity) }
    override fun play() { check(!closed); if (clip.framePosition >= clip.frameLength) seek(0); clip.start(); poll() }
    override fun pause() { if (!closed) { clip.stop(); poll() } }
    override fun seek(positionMillis: Long) { check(!closed); clip.microsecondPosition = positionMillis.coerceIn(0, durationMillis) * 1000; discontinuity++; poll() }
    override fun close() { if (!closed) { pause(); closed = true; clip.close() } }
}
