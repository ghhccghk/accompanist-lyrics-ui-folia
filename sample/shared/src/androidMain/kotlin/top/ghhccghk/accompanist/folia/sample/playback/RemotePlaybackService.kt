package top.ghhccghk.accompanist.folia.sample.playback

import android.app.Service
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.*
import android.util.Log
import java.util.NoSuchElementException

/** Private, bound-only sample service in :playback. MediaPlayer creation, every
 * access, all callbacks and release are confined to one worker Looper. */
class RemotePlaybackService : Service() {
    private lateinit var worker: HandlerThread
    private lateinit var handler: Handler
    private lateinit var messenger: Messenger
    private val sessions = mutableMapOf<String, Session>() // Worker confined.
    private var tickScheduled = false
    private var destroyed = false
    private val tick = object : Runnable {
        override fun run() {
            tickScheduled = false
            sessions.values.toList().filter { it.needsSamples }.forEach { it.publish(false) }
            scheduleTick()
        }
    }

    override fun onCreate() {
        super.onCreate()
        worker = HandlerThread("FoliaPlaybackEngine").apply { start() }
        handler = Handler(worker.looper) { message -> handle(message); true }
        messenger = Messenger(handler)
    }
    override fun onBind(intent: Intent): IBinder = messenger.binder
    override fun onDestroy() {
        handler.post {
            destroyed = true
            handler.removeCallbacks(tick)
            sessions.values.toList().forEach { it.release() }
            worker.quitSafely()
        }
        super.onDestroy()
    }
    private fun scheduleTick() {
        if (!destroyed && !tickScheduled && sessions.values.any { it.needsSamples }) {
            tickScheduled = true; handler.postDelayed(tick, PlaybackIpc.SAMPLE_MILLIS)
        }
    }
    private fun handle(message: Message) {
        check(Looper.myLooper() == worker.looper)
        if (destroyed) return
        if (message.sendingUid != applicationInfo.uid) return
        val reply = message.replyTo ?: return
        val data = message.data
        val id = data.getString("session")?.takeIf { it.length in 1..80 } ?: return
        if (message.what == PlaybackIpc.PREPARE) {
            if (sessions.containsKey(id)) return
            if (sessions.size >= 4) { sendError(reply,id,"同时准备的播放会话过多"); return }
            val uri = data.getString("uri") ?: return
            try {
                val session = Session(id,reply)
                sessions[id] = session
                session.prepare(Uri.parse(uri))
            } catch (failure: Exception) {
                sessions[id]?.release()
                sendError(reply,id,"音频准备失败：${failure.message}")
            }
            return
        }
        val session = sessions[id]?.takeIf { it.reply.binder == reply.binder } ?: return
        if (message.what == PlaybackIpc.CLOSE) { session.release(); return }
        if (message.what == PlaybackIpc.ACK) {
            if (data.getLong("revision") >= session.revision) session.awaitingAck = false
            return
        }
        val sequence = data.getLong("command")
        if (!session.prepared || sequence <= session.command) return
        session.command = sequence
        try {
            when (message.what) {
                PlaybackIpc.PLAY -> session.play()
                PlaybackIpc.PAUSE -> session.pause()
                PlaybackIpc.SEEK -> session.seek(data.getLong("position"))
                else -> return
            }
            session.publish(true)
            scheduleTick()
        } catch (failure: Exception) { session.fail("播放控制失败：${failure.message}") }
    }
    private fun sendError(reply: Messenger, id: String, error: String) {
        try { reply.send(Message.obtain(null,PlaybackIpc.ERROR).apply {
            data = Bundle().apply { putString("session",id); putString("error",error) }
        }) } catch (failure: RemoteException) { Log.i("FoliaPlayback","Client disconnected before error delivery") }
    }

    private inner class Session(val id: String, val reply: Messenger) {
        private val player = MediaPlayer()
        var prepared = false; private set
        var command = 0L
        var revision = 0L; private set
        var awaitingAck = false
        private var duration = 0L
        private var discontinuity = 1L
        private var wantsPlay = false
        private var buffering = false
        private var completed = false
        private var closed = false
        private var target: Long? = null
        private var inFlight: Long? = null
        private var deathLinked = false
        private val death = IBinder.DeathRecipient { handler.post { release() } }
        private val timeout = Runnable { if (!prepared) fail("音频准备超时") }
        val needsSamples get() = prepared && !closed && (wantsPlay || target != null || buffering)

        fun prepare(uri: Uri) {
            reply.binder.linkToDeath(death,0); deathLinked = true
            player.setOnPreparedListener {
                if (closed) return@setOnPreparedListener
                try {
                    duration = player.duration.toLong()
                    check(duration > 0) { "无法取得有限音频时长" }
                    prepared = true
                    handler.removeCallbacks(timeout)
                    publish(true)
                } catch (failure: Exception) { fail("读取音频状态失败：${failure.message}") }
            }
            player.setOnSeekCompleteListener {
                if (closed) return@setOnSeekCompleteListener
                try {
                    val finished = inFlight ?: return@setOnSeekCompleteListener
                    inFlight = null
                    val latest = target
                    if (latest != null && latest != finished) beginSeek(latest)
                    else {
                        target = null
                        if (wantsPlay) player.start()
                    }
                    publish(true); scheduleTick()
                } catch (failure: Exception) { fail("跳转失败：${failure.message}") }
            }
            player.setOnCompletionListener {
                if (target == null) {
                    completed = true; wantsPlay = false; publish(true)
                }
            }
            player.setOnInfoListener { _, what, _ ->
                if (what == MediaPlayer.MEDIA_INFO_BUFFERING_START || what == MediaPlayer.MEDIA_INFO_BUFFERING_END) {
                    buffering = what == MediaPlayer.MEDIA_INFO_BUFFERING_START
                    publish(true); scheduleTick()
                }
                false
            }
            player.setOnErrorListener { _, what, extra -> fail("音频播放失败（$what / $extra）"); true }
            handler.postDelayed(timeout, PlaybackIpc.PREPARE_TIMEOUT_MILLIS)
            player.setDataSource(this@RemotePlaybackService,uri)
            player.prepareAsync()
        }
        fun play() {
            wantsPlay = true
            if (completed) seek(0)
            if (target == null) player.start()
        }
        fun pause() {
            wantsPlay = false
            if (player.isPlaying) player.pause()
        }
        fun seek(position: Long) {
            target = position.coerceIn(0,duration)
            completed = false
            discontinuity++
            // Only one native seek in flight. Its callback cannot accidentally
            // complete a newer request; intermediate targets are coalesced.
            if (inFlight == null) beginSeek(target!!)
        }
        private fun beginSeek(position: Long) {
            inFlight = position
            if (Build.VERSION.SDK_INT >= 26) player.seekTo(position,MediaPlayer.SEEK_CLOSEST)
            else @Suppress("DEPRECATION") player.seekTo(position.toInt())
        }
        fun publish(force: Boolean) {
            if (closed || !prepared || (awaitingAck && !force)) return
            try {
                val position = target ?: if (completed) duration else player.currentPosition.toLong().coerceIn(0,duration)
                val state = RemotePlaybackState(id,++revision,command,position,duration,SystemClock.elapsedRealtimeNanos(),
                    wantsPlay && player.isPlaying && !completed,target != null,buffering,1f,discontinuity)
                reply.send(Message.obtain(null,PlaybackIpc.STATE).apply {
                    data = PlaybackIpc.encode(state).apply { putInt("pid",Process.myPid()) }
                })
                awaitingAck = true
            } catch (failure: RemoteException) { release() }
            catch (failure: Exception) { fail("采样播放状态失败：${failure.message}") }
        }
        fun fail(error: String) {
            if (closed) return
            sendError(reply,id,error)
            release()
        }
        fun release() {
            check(Looper.myLooper() == worker.looper)
            if (closed) return
            closed = true
            handler.removeCallbacks(timeout)
            sessions.remove(id)
            if (deathLinked) {
                try { reply.binder.unlinkToDeath(death,0) }
                catch (failure: NoSuchElementException) { Log.i("FoliaPlayback","Client death already unlinked") }
            }
            player.setOnPreparedListener(null); player.setOnSeekCompleteListener(null)
            player.setOnCompletionListener(null); player.setOnInfoListener(null); player.setOnErrorListener(null)
            player.release()
        }
    }
}
