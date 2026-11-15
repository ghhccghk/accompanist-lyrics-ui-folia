package top.ghhccghk.accompanist.folia.sample.playback

import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.net.Uri
import android.provider.OpenableColumns
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import top.ghhccghk.accompanist.folia.sample.importing.*
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AndroidSongFile(val context: Context, val uri: Uri, override val name: String) : LyricsSource {
    override suspend fun read() = withContext(Dispatchers.IO) {
        requireNotNull(context.contentResolver.openInputStream(uri)) { "无法读取所选歌词" }.use { stream ->
            val output = java.io.ByteArrayOutputStream(); val buffer = ByteArray(8192)
            while (true) { val count = stream.read(buffer); if(count < 0) break
                require(output.size() + count <= MAX_IMPORT_BYTES) { "歌词文件超过 1 MiB" }; output.write(buffer,0,count) }
            output.toByteArray()
        }
    }
}
actual val localAudioFormats = "Android：系统 MediaPlayer 支持的本地音频编码"
@Composable actual fun rememberSongFilePicker(onSelected: (FileRole, LyricsSource?) -> Unit, onError: (String) -> Unit): (FileRole) -> Unit {
    val context = LocalContext.current.applicationContext
    val selected by rememberUpdatedState(onSelected); val failed by rememberUpdatedState(onError)
    var role by rememberSaveable { mutableStateOf(FileRole.Audio) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if(uri == null) selected(role,null)
        if (uri != null) try {
            val name = context.contentResolver.query(uri,arrayOf(OpenableColumns.DISPLAY_NAME),null,null,null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            } ?: uri.lastPathSegment ?: "所选文件"
            selected(role, AndroidSongFile(context,uri,name))
        } catch (failure: Exception) { failed("文件读取失败：" + failure.message) }
    }
    return { requested -> role = requested
        try { picker.launch(if(requested == FileRole.Audio) arrayOf("audio/*") else arrayOf("*/*")) }
        catch (failure: android.content.ActivityNotFoundException) { failed("文档选择器不可用：" + failure.message) } }
}
@Composable actual fun rememberAudioPreparation(): suspend (LyricsSource) -> LocalAudioPlayer = remember { { source -> prepareAndroidAudio(source) } }

suspend fun prepareAndroidAudio(source: LyricsSource): LocalAudioPlayer {
    val file = source as? AndroidSongFile ?: error("音频不是本地文档选择器提供的文件")
    val metadata = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(file.context, file.uri)
            val bytes = retriever.embeddedPicture
            SongMetadata(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)?.takeIf { it.isNotBlank() } ?: file.name.substringBeforeLast('.'),
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.takeIf { it.isNotBlank() } ?: "未知作者",
                bytes?.takeIf { it.size <= 8 * 1024 * 1024 }?.let { data ->
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }; BitmapFactory.decodeByteArray(data,0,data.size,bounds)
                    val options = BitmapFactory.Options().apply { inSampleSize = 1; while(maxOf(bounds.outWidth,bounds.outHeight)/inSampleSize > 1024) inSampleSize *= 2 }
                    BitmapFactory.decodeByteArray(data,0,data.size,options)?.asImageBitmap()
                })
        } finally { retriever.release() }
    }
    return withContext(Dispatchers.Main.immediate) { suspendCancellableCoroutine { continuation ->
        val player = MediaPlayer()
        continuation.invokeOnCancellation { player.release() }
        player.setOnPreparedListener { if (continuation.isActive) continuation.resume(AndroidAudioPlayer(player, metadata)) }
        player.setOnErrorListener { _, what, extra ->
            if (continuation.isActive) { player.release(); continuation.resumeWithException(IllegalStateException("无法解码音频（" + what + " / " + extra + "）")) }; true
        }
        try { player.setDataSource(file.context, file.uri); player.prepareAsync() }
        catch (failure: Exception) { player.release(); if(continuation.isActive) continuation.resumeWithException(failure) }
    } }
}
internal class AndroidAudioPlayer(private val player: MediaPlayer, override val metadata: SongMetadata) : LocalAudioPlayer {
    override val durationMillis = player.duration.toLong().coerceAtLeast(1)
    override var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false)); private set
    private var discontinuity = 1L
    private var closed = false
    private var seeking: Long? = null
    override val isSeeking get() = seeking != null
    private var buffering = false
    private var failure: String? = null
    init {
        player.setOnSeekCompleteListener { seeking = null; poll() }
        player.setOnCompletionListener { snapshot = FoliaPlaybackSnapshot(durationMillis,false,discontinuity) }
        player.setOnInfoListener { _, what, _ ->
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START) buffering = true
            if (what == MediaPlayer.MEDIA_INFO_BUFFERING_END) buffering = false
            poll()
            false
        }
        player.setOnErrorListener { _, what, extra -> failure = "播放失败（" + what + " / " + extra + "）"; snapshot = snapshot.copy(isPlaying=false); true }
    }
    override fun poll() { if(!closed) { failure?.let { error(it) }; snapshot = FoliaPlaybackSnapshot(seeking ?: player.currentPosition.toLong(), player.isPlaying && seeking == null && !buffering, discontinuity) } }
    override fun play() { check(!closed); failure?.let { error(it) }; if(snapshot.positionMillis >= durationMillis) seek(0); player.start(); poll() }
    override fun pause() { if(!closed && failure == null) { if(player.isPlaying) player.pause(); poll() } }
    override fun seek(positionMillis: Long) { check(!closed); val target=positionMillis.coerceIn(0,durationMillis); seeking=target; discontinuity++; snapshot=FoliaPlaybackSnapshot(target,false,discontinuity)
        if(android.os.Build.VERSION.SDK_INT>=26) player.seekTo(target,MediaPlayer.SEEK_CLOSEST) else @Suppress("DEPRECATION") player.seekTo(target.toInt()) }
    override fun close() { if(!closed) { closed=true; snapshot=snapshot.copy(isPlaying=false); player.release() } }
}
