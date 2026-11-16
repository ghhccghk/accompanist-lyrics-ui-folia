package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.staticCompositionLocalOf

/** Optional synchronous small-sigma colour convolution, independent of alpha
 * masks. All arrays are borrowed read-only and must not be retained. The odd,
 * normalized Double kernel has 3..13 coefficients in ascending offset order.
 * Return unpremultiplied ARGB of the same dimensions, or null for Kotlin fallback.
 * Hosts must expose backend failures. Default Kotlin requires no native library.
 */
fun interface ExperimentalFoliaColourProcessor {
    fun convolve(pixels: IntArray, width: Int, height: Int, kernel: DoubleArray): IntArray?
}

/** Changing the scoped identity invalidates recorded software layers. */
val LocalFoliaColourProcessor = staticCompositionLocalOf<ExperimentalFoliaColourProcessor?> { null }
