package org.example.folianativeconsumer

import android.app.Activity
import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import top.ghhccghk.accompanist.lyrics.folia.nativeandroid.FoliaNativeRuntime
import top.ghhccghk.accompanist.lyrics.folia.nativeandroid.FoliaNativeSurfaceView

class ConsumerActivity : Activity() {
    private var surface: FoliaNativeSurfaceView? = null
    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        val root = FrameLayout(this)
        val label = TextView(this).apply { text = "Loading AAR runtime"; setTextColor(-1) }
        val availability = FoliaNativeRuntime.checkAvailability()
        if (availability.isSuccess) {
            surface = FoliaNativeSurfaceView(this).also {
                it.report = { result -> label.text = "ABI=${availability.getOrThrow()} $result" }
                root.addView(it, FrameLayout.LayoutParams(-1,-1))
            }
        } else label.text = "FAILED: ${availability.exceptionOrNull()}"
        root.addView(label); setContentView(root)
    }
    override fun onDestroy() { surface?.release(); super.onDestroy() }
}
