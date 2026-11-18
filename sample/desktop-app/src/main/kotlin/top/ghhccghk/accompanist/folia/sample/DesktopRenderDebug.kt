package top.ghhccghk.accompanist.folia.sample

import androidx.compose.ui.awt.ComposeWindow
import org.jetbrains.skiko.SkiaLayer
import org.jetbrains.skiko.SkikoRenderDelegate
import java.awt.Container

/** Wrap only this window's existing callback, never install an animation loop.
 * onRender surrounds scene recording; native submit/present may happen later. */
class DesktopRenderDebug(private val window:ComposeWindow):SampleRenderDebugHost {
    private val frames=RenderFrameWindow()
    private var layer:SkiaLayer?=null
    private var original:SkikoRenderDelegate?=null
    private var hook:SkikoRenderDelegate?=null
    override val backend:String get()=window.renderApi.toString()
    override val requestedBackend:String get()=System.getProperty("skiko.renderApi")?:System.getenv("SKIKO_RENDER_API")?:"AUTO"
    override val metric:String get()=if(hook!=null)"Skiko onRender CPU回调耗时；不含后续GPU提交／呈现"
        else "未找到当前窗口SkiaLayer，帧数据不可用"
    private fun find(container:Container):SkiaLayer? {
        for(child in container.components) {
            if(child is SkiaLayer)return child
            if(child is Container)find(child)?.let{return it}
        }
        return null
    }
    override fun setEnabled(enabled:Boolean) {
        if(enabled) {
            if(hook!=null)return
            val owner=find(window)?:return
            val delegate=owner.renderDelegate?:return
            synchronized(frames){frames.reset()}
            val wrapped=SkikoRenderDelegate{canvas,width,height,nanoTime->
                val start=System.nanoTime()
                try{delegate.onRender(canvas,width,height,nanoTime)}
                finally{val end=System.nanoTime();synchronized(frames){frames.record(end,end-start)}}
            }
            layer=owner;original=delegate;hook=wrapped;owner.renderDelegate=wrapped
        }else {
            // Do not overwrite a newer hook installed by the host.
            if(layer?.renderDelegate===hook)layer?.renderDelegate=original
            layer=null;original=null;hook=null
            synchronized(frames){frames.reset()}
        }
    }
    override fun read():RenderFrameStats=synchronized(frames){frames.read(System.nanoTime())}
}
