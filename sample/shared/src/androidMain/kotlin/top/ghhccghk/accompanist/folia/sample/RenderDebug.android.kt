package top.ghhccghk.accompanist.folia.sample

import android.app.Activity
import android.annotation.TargetApi
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.FrameMetrics
import android.view.Window
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

private fun Context.debugActivity():Activity?=when(this){
    is Activity->this
    is ContextWrapper->baseContext.takeIf{it!==this}?.debugActivity()
    else->null
}

@Composable internal actual fun rememberPlatformRenderDebugHost():SampleRenderDebugHost? {
    val context=LocalContext.current
    return remember(context){context.debugActivity()?.let{
        if(Build.VERSION.SDK_INT>=24)AndroidRenderDebug24(it.window) else AndroidRenderDebug23(it.window)
    }}
}

/** API24+ actual window reports, not Choreographer refresh opportunities.
 * HWUI's public API does not identify GLES versus Vulkan; don't guess it. */
private open class AndroidRenderDebug23(protected val window:Window):SampleRenderDebugHost {
    override val backend:String get()=if(window.decorView.isHardwareAccelerated)"Android HWUI（GLES/Vulkan未公开）" else "Android 软件Canvas"
    override val requestedBackend:String get()="系统选择"
    override val metric:String get()="API23无Window FrameMetrics；帧数据不可用"
    override fun setEnabled(enabled:Boolean) {}
    override fun read()=RenderFrameStats()
}

// Keep API24-only listener types out of classes instantiated on API23.
@TargetApi(24)
private class AndroidRenderDebug24(window:Window):AndroidRenderDebug23(window) {
    private val frames=RenderFrameWindow()
    private var installed=false
    override val metric:String get()="Android Window FrameMetrics TOTAL_DURATION；回调到达频率，非GPU专用计时"
    private val listener=
        Window.OnFrameMetricsAvailableListener{_,metrics,dropped->
            val total=metrics.getMetric(FrameMetrics.TOTAL_DURATION)
            if(installed&&total>=0)frames.record(System.nanoTime(),total,dropped)
        }
    override fun setEnabled(enabled:Boolean) {
        if(enabled&&!installed){frames.reset();window.addOnFrameMetricsAvailableListener(listener,Handler(Looper.getMainLooper()));installed=true}
        else if(!enabled&&installed){window.removeOnFrameMetricsAvailableListener(listener);installed=false;frames.reset()}
    }
    override fun read():RenderFrameStats=frames.read(System.nanoTime())
}
