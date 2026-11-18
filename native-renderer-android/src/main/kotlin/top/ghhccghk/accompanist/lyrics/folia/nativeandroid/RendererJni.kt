package top.ghhccghk.accompanist.lyrics.folia.nativeandroid

import android.view.Surface

internal object RendererJni {
    @JvmStatic external fun abiVersion(): Int
    @JvmStatic external fun create(surface: Surface, width: Int, height: Int): Long
    @JvmStatic external fun frame(handle: Long, width: Int, height: Int): Int
    @JvmStatic external fun destroy(handle: Long): Int
}

/** Loading a runtime is a capability check, never evidence a theme is drawn. */
object FoliaNativeRuntime {
    const val ABI_VERSION: Int = 1
    private val availability: Result<Int> by lazy {
        runCatching {
            System.loadLibrary("lyrics_folia_renderer")
            RendererJni.abiVersion().also { check(it == ABI_VERSION) { "Renderer ABI mismatch: $it" } }
        }
    }
    fun checkAvailability(): Result<Int> = availability
}
