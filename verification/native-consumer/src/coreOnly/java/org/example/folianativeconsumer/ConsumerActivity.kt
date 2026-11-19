package org.example.folianativeconsumer

import android.os.Bundle
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.ComposeView
import org.example.foliaconsumer.Consumer

/** Android application that consumes only the Kotlin Maven coordinate. */
class ConsumerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val result = TextView(this)
        val lyrics = ComposeView(this).apply {
            setContent { Consumer(onStatus = { if (it.mainUnitCount > 0) result.text = "CoreLyricsReady" }) }
        }
        setContentView(FrameLayout(this).apply {
            addView(lyrics, FrameLayout.LayoutParams(-1, -1))
            addView(result, FrameLayout.LayoutParams(-1, -2))
        })
    }
}
