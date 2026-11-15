package top.ghhccghk.accompanist.folia.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class SampleViewModel : ViewModel() {
    val session = SampleSession()
    override fun onCleared() { session.clock.pause(); session.songs.close() }
}

class MainActivity : ComponentActivity() {
    private var sampleVisible by mutableStateOf(false)
    private val model by lazy { ViewModelProvider(this)[SampleViewModel::class.java] }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FoliaSampleApp(visible = sampleVisible, session = model.session) }
    }
    override fun onStart() { super.onStart(); sampleVisible = true }
    override fun onStop() { sampleVisible = false; model.session.clock.pause(); model.session.songs.current?.player?.pause(); super.onStop() }
}
