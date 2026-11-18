package top.ghhccghk.accompanist.lyrics.folia.nativeandroid

import android.content.Context
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Choreographer
import android.view.Surface
import android.view.TextureView
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

/** Each View owns its worker/session. No global poster or unsynchronised player reads. */
class FoliaNativeSurfaceView internal constructor(
    context: Context,
    private val waitForScene: Boolean,
) : TextureView(context), TextureView.SurfaceTextureListener {
    constructor(context: Context) : this(context, waitForScene = false)

    private val thread = HandlerThread("Folia renderer").apply { start() }
    private val worker = Handler(thread.looper)
    private var serial = 0L // UI thread
    private var released = false
    var report: (String) -> Unit = {}
    internal var onPacketSubmitted:(NativeScenePacket,LongArray?)->Unit={_,_->}
    private val mailbox=AtomicReference<NativeScenePacket?>(null)
    private val waking=AtomicBoolean(false)
    private val uploaded=mutableSetOf<Long>() // worker only
    private var active=true
    private var lastSubmittedId=-1L
    private var unopenedSurface: Surface? = null // worker only
    private var handle = 0L // worker only
    private var widthPx = 0
    private var heightPx = 0
    private var visible = false
    private var queued = false
    private var failures = 0
    private val clock by lazy { Choreographer.getInstance() }
    private val callback = Choreographer.FrameCallback { draw() }
    init { isOpaque = false; surfaceTextureListener = this }
    private fun stop() {
        if (queued) clock.removeFrameCallback(callback)
        queued = false
        unopenedSurface?.release()
        unopenedSurface = null
        if (handle > 0) RendererJni.destroy(handle)
        handle = 0
        uploaded.clear()
        lastSubmittedId=-1L
    }
    private fun hasScene() = mailbox.get()?.frames?.isNotEmpty() == true

    private fun wake() {
        val canCreate = unopenedSurface != null && (!waitForScene || hasScene())
        if (!queued && active && visible && (handle > 0 || canCreate) && widthPx > 0 && heightPx > 0) {
            queued = true; clock.postFrameCallback(callback)
        }
    }
    private fun draw() {
        queued = false
        if (!visible || !active) return
        val generation = workerGeneration
        val packet=mailbox.get()
        if(packet!=null&&packet.id==lastSubmittedId)return
        if (waitForScene && handle <= 0 && packet?.frames.isNullOrEmpty()) return
        if(packet!=null&&(packet.width!=widthPx||packet.height!=heightPx))return
        if (handle <= 0 && !createSession(generation)) return
        val result = try {
            if(packet==null)RendererJni.frame(handle,widthPx,heightPx)
            else {
                val wanted=packet.scenes.map{it.id}.toSet()
                (uploaded - wanted).forEach { id ->
                    val status = RendererJni.dropScene(handle, id)
                    check(status == 0) { "DropScene:$status" }
                    uploaded.remove(id)
                }
                packet.scenes.filter{it.id !in uploaded}.forEach{scene->
                    scene.textures.forEach{texture->
                        val status=RendererJni.upload(handle,texture.id,texture.width,texture.height,
                            texture.rgba.directBuffer(),texture.rgba.size,texture.sigma1,texture.sigma2,texture.inkAlpha)
                        check(status == 0) { "Upload:$status:${texture.width}x${texture.height}" }
                    }
                    uploaded+=scene.id
                }
                RendererJni.render(handle,packet.bytes.directBuffer(),packet.bytes.size)
            }
        } catch (error: Exception) {
            fail(generation, error.message ?: "WorkerException")
            return
        }
        if (result < 0 || (result == 1 && ++failures > 3)) {
            fail(generation, "Render:$result")
        } else if (result == 1) wake()
        else {
            failures = 0
            if(packet!=null)lastSubmittedId=packet.id
            val stats=if(packet==null)null else RendererJni.stats(handle)
            post { if (!released && serial == generation) {
                report(if(packet==null)"SurfaceReady" else "NativeFrameSubmitted")
                if(packet!=null)onPacketSubmitted(packet,stats)
            } }
        }
        // Paused/unchanged scenes submit once, never spin at vsync indefinitely.
    }
    internal fun publish(packet:NativeScenePacket) {
        if(released)return
        mailbox.set(packet)
        if(waking.compareAndSet(false,true))worker.post{waking.set(false);wake()}
    }
    fun setRenderingActive(value:Boolean) {
        if(!released)worker.post{
            if(active==value)return@post
            active=value
            if(!active&&queued){clock.removeFrameCallback(callback);queued=false}
            wake()
        }
    }
    private var workerGeneration = 0L
    override fun onSurfaceTextureAvailable(texture: SurfaceTexture, width: Int, height: Int) {
        if (released) return
        val generation = ++serial
        val surface = Surface(texture)
        val shown = isShown && windowVisibility == VISIBLE
        if (!worker.post {
            stop()
            workerGeneration = generation
            widthPx = width
            heightPx = height
            visible = shown
            failures = 0
            unopenedSurface = surface
            wake()
        }) surface.release()
    }
    private fun fail(generation: Long, reason: String) {
        stop()
        post { if (!released && serial == generation) report("Failed:$reason") }
    }

    private fun createSession(generation: Long): Boolean {
        val surface = unopenedSurface ?: return false
        unopenedSurface = null
        return try {
            FoliaNativeRuntime.checkAvailability().getOrThrow()
            handle = RendererJni.create(surface, widthPx, heightPx)
            check(handle > 0) { "Create:$handle" }
            true
        } catch (error: Throwable) {
            fail(generation, error.message ?: "CreateException")
            false
        } finally {
            surface.release()
        }
    }
    override fun onSurfaceTextureSizeChanged(texture: SurfaceTexture, width: Int, height: Int) {
        worker.post { widthPx = width; heightPx = height; wake() }
    }
    override fun onSurfaceTextureUpdated(texture: SurfaceTexture) = Unit
    override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
        ++serial
        if (!worker.post { stop(); texture.release() }) texture.release()
        return false // retained until the worker has stopped using ANativeWindow
    }
    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (!released) worker.post {
            visible = isVisible
            if (!visible && queued) { clock.removeFrameCallback(callback); queued = false }
            wake()
        }
    }
    fun release() {
        if (released) return
        released = true; ++serial;mailbox.set(null)
        worker.post { stop(); thread.quitSafely() }
    }
}
