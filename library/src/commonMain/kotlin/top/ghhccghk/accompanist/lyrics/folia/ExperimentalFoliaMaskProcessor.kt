package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Experimental, synchronous CPU alpha-mask hook. The default remains Kotlin.
 * Input is borrowed read-only ARGB, with one output array per sigma, in order.
 * Implementations must not retain input arrays. Null means an explicit fallback
 * to Kotlin; hosts should expose the reason. This is not a scene/render backend.
 */
fun interface ExperimentalFoliaMaskProcessor {
    fun blur(pixels: IntArray, width: Int, height: Int, sigmas: FloatArray): List<IntArray>?
}

/** Scoped to a view/composition; changing identity rebuilds prepared glow masks. */
val LocalFoliaMaskProcessor = staticCompositionLocalOf<ExperimentalFoliaMaskProcessor?> { null }
