package top.ghhccghk.accompanist.folia.sample.playback

import android.content.*
import android.net.Uri
import android.os.*
import androidx.compose.runtime.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import java.util.UUID
import java.util.NoSuchElementException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot

/** UI-side sample adapter. poll() never calls Binder/MediaPlayer. IPC runs on a
 * private bridge Looper; one mailbox + one UI wake avoids a queued frame backlog. */
internal class RemoteAndroidAudioPlayer(
    context: Context, private val uri: Uri, override val metadata: SongMetadata,
) : LocalAudioPlayer {
    private val context = context.applicationContext
    private val id = UUID.randomUUID().toString()
    private val mirror = RemotePlaybackMirror(id)
    private val main = Handler(Looper.getMainLooper())
    private val worker = HandlerThread("FoliaPlaybackClient-${id.take(8)}").apply { start() }
    private val io = Handler(worker.looper)
    private val latest = AtomicReference<RemotePlaybackState?>(null)
    private val failure = AtomicReference<String?>(null)
    private val wakeScheduled = AtomicBoolean(false)
    private val transportClosed = AtomicBoolean(false)
    private val ready = CompletableDeferred<RemotePlaybackState>()
    private var bound = false // Main confined.
    private var closed = false // Main confined.
    private var failed = false // Main confined.
    private var remote: Messenger? = null // Bridge confined.
    private var remoteBinder: IBinder? = null
    private val death = IBinder.DeathRecipient { fail("播放进程已断开，请重新选择歌曲") }
    private var servicePid = 0 // Diagnostics used by the isolated device probe.
    override var durationMillis = 0L; private set
    override var snapshot by mutableStateOf(FoliaPlaybackSnapshot(0,false)); private set
    override var isSeeking by mutableStateOf(false); private set
    override var updateRevision by mutableLongStateOf(0); private set

    private val incoming = Messenger(Handler(worker.looper) { message ->
        if (!transportClosed.get() && message.data.getString("session") == id) {
            when (message.what) {
                PlaybackIpc.STATE -> try {
                    val state = PlaybackIpc.decode(message.data)
                    latest.set(state)
                    servicePid = message.data.getInt("pid")
                    send(PlaybackIpc.ACK,Bundle().apply { putLong("revision",state.revision) })
                    ready.complete(state)
                    wake()
                } catch (error: Exception) { fail("接收播放状态失败：${error.message}") }
                PlaybackIpc.ERROR -> fail(message.data.getString("error") ?: "播放进程报告未知错误")
            }
        }
        true
    })
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            io.post {
                if (!transportClosed.get()) try {
                    remote = Messenger(binder)
                    remoteBinder = binder
                    binder.linkToDeath(death,0)
                    send(PlaybackIpc.PREPARE,Bundle().apply { putString("uri",uri.toString()) })
                } catch (error: RemoteException) { fail("播放进程连接失败：${error.message}") }
            }
        }
        override fun onServiceDisconnected(name: ComponentName) { fail("播放进程连接中断，请重新选择歌曲") }
        override fun onBindingDied(name: ComponentName) { fail("播放服务绑定失效，请重新选择歌曲") }
        override fun onNullBinding(name: ComponentName) { fail("播放服务未提供控制入口") }
    }

    suspend fun prepare(): RemoteAndroidAudioPlayer {
        check(Looper.myLooper() == Looper.getMainLooper())
        try {
            bound = context.bindService(Intent(context,RemotePlaybackService::class.java),connection,Context.BIND_AUTO_CREATE)
            check(bound) { "无法绑定独立播放服务" }
            val initial = withTimeout(PlaybackIpc.PREPARE_TIMEOUT_MILLIS+5000) { ready.await() }
            failure.get()?.let { error(it) }
            durationMillis = initial.durationMillis
            applyMailbox()
            return this
        } catch (error: Exception) { close(); throw error }
    }
    private fun send(what: Int, extra: Bundle = Bundle()) {
        check(Looper.myLooper() == worker.looper)
        try {
            val endpoint = remote ?: throw RemoteException("播放服务尚未连接")
            endpoint.send(Message.obtain(null,what).apply {
                replyTo = incoming
                data = extra.apply { putString("session",id) }
            })
        } catch (error: RemoteException) { fail("发送播放命令失败：${error.message}") }
    }
    private fun wake() {
        if (wakeScheduled.compareAndSet(false,true)) main.post {
            wakeScheduled.set(false)
            if (!closed) applyMailbox()
        }
    }
    private fun fail(reason: String) {
        if (failure.compareAndSet(null,reason)) {
            ready.completeExceptionally(IllegalStateException(reason))
            wake()
            disposeTransport()
        }
    }
    private fun applyMailbox() {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (closed) return
        var changed = false
        latest.getAndSet(null)?.let { state ->
            if (!failed) try { changed = mirror.accept(state) }
            catch (error: IllegalArgumentException) { fail(error.message ?: "播放状态不合法") }
        }
        if (failure.get() != null && !failed) {
            mirror.freeze(SystemClock.elapsedRealtimeNanos()); failed = true; changed = true
        }
        snapshot = mirror.snapshot(SystemClock.elapsedRealtimeNanos())
        isSeeking = mirror.isSeeking
        if (changed) updateRevision++
    }
    override fun poll() {
        if (closed) return
        applyMailbox()
        failure.get()?.let { error(it) }
    }
    private fun command(command: RemotePlaybackCommand, target: Long = 0) {
        check(!closed) { "播放会话已关闭" }
        failure.get()?.let { if (command == RemotePlaybackCommand.Pause) return else error(it) }
        applyMailbox()
        failure.get()?.let { if (command == RemotePlaybackCommand.Pause) return else error(it) }
        val sequence = mirror.command(command,SystemClock.elapsedRealtimeNanos(),target)
        snapshot = mirror.snapshot(SystemClock.elapsedRealtimeNanos())
        isSeeking = mirror.isSeeking
        io.post {
            if (!transportClosed.get()) send(when(command) {
                RemotePlaybackCommand.Play -> PlaybackIpc.PLAY
                RemotePlaybackCommand.Pause -> PlaybackIpc.PAUSE
                RemotePlaybackCommand.Seek -> PlaybackIpc.SEEK
            },Bundle().apply { putLong("command",sequence); putLong("position",target) })
        }
    }
    override fun play() = command(RemotePlaybackCommand.Play)
    override fun pause() { if (!closed) command(RemotePlaybackCommand.Pause) }
    override fun seek(positionMillis: Long) = command(RemotePlaybackCommand.Seek,positionMillis.coerceIn(0,durationMillis))
    override fun close() {
        check(Looper.myLooper() == Looper.getMainLooper())
        if (closed) return
        closed = true
        ready.cancel()
        mirror.freeze(SystemClock.elapsedRealtimeNanos())
        snapshot = mirror.snapshot(SystemClock.elapsedRealtimeNanos()); isSeeking = false
        disposeTransport()
    }
    private fun disposeTransport() {
        if (!transportClosed.compareAndSet(false,true)) return
        io.post {
            if (remote != null) send(PlaybackIpc.CLOSE)
            remoteBinder?.let {
                try { it.unlinkToDeath(death,0) } catch (_: NoSuchElementException) { /* Already dead. */ }
            }
            remote = null; remoteBinder = null
            io.removeCallbacksAndMessages(null)
            worker.quitSafely()
            main.post {
                if (bound) { bound = false; context.unbindService(connection) }
            }
        }
    }
}
