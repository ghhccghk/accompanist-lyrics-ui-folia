package top.ghhccghk.accompanist.folia.sample.playback

import android.content.Context
import android.media.MediaMetadataRetriever
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
import kotlinx.coroutines.*

class AndroidSongFile(val context: Context, val uri: Uri, override val name: String) : LyricsSource {
    override suspend fun embeddedLyrics() = withContext(Dispatchers.IO) {
        readEmbeddedLyrics { requireNotNull(context.contentResolver.openInputStream(uri)) { "无法读取音频标签" } }
    }
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
    var candidate: RemoteAndroidAudioPlayer? = null
    try {
        return withContext(Dispatchers.Main.immediate) {
            RemoteAndroidAudioPlayer(file.context,file.uri,metadata).also { candidate = it }.prepare()
        }
    } catch (failure: Throwable) {
        // withContext may discard a successful result when the caller is
        // cancelled during the dispatcher hop. Preserve resource ownership.
        withContext(NonCancellable + Dispatchers.Main.immediate) { candidate?.close() }
        throw failure
    }
}
