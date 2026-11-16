package top.ghhccghk.accompanist.folia.sample

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.io.IOException

internal object NativeColourBridge {
    @JvmStatic external fun version(): Int
    @JvmStatic external fun convolve(source: ByteBuffer, a: ByteBuffer, b: ByteBuffer, alpha: ByteBuffer,
        kernel: ByteBuffer, output: ByteBuffer, width: Int, height: Int, radius: Int): Int
}

internal actual fun loadSampleNativeColour(): NativeColourLoad {
    var phase = "加载 Rust 颜色卷积库"
    return try {
        loadSampleNativeLibrary()
        phase = "核对颜色卷积 JNI 入口"
        check(NativeColourBridge.version() == 1) { "Rust 颜色卷积 ABI 版本不兼容" }
        val processor = JniColourProcessor()
        phase = "运行颜色卷积启动自检"
        // Identity kernel: includes low alpha, coloured transparency and neutral.
        val input = intArrayOf(0x01ff0000, 0x0080ffff, 0x7f808080, -1)
        val expected = intArrayOf(0x01ff0000, 0, 0x7f808080, -1)
        val result = processor.convolve(input,4,1,doubleArrayOf(0.0,1.0,0.0))
        if (result == null || !result.contentEquals(expected)) {
            val error = processor.stats().error ?: "Rust 颜色卷积启动自检不匹配"
            processor.release(); NativeColourLoad(error = error)
        } else { processor.resetStats(); NativeColourLoad(processor) }
    } catch (error: LinkageError) {
        NativeColourLoad(error = nativeMaskLoadFailure(phase,error))
    } catch (error: SecurityException) {
        NativeColourLoad(error = "系统拒绝加载颜色卷积库：${error.message}")
    } catch (error: IllegalStateException) {
        NativeColourLoad(error = error.message ?: "Rust 颜色卷积自检失败")
    } catch (error: IOException) {
        NativeColourLoad(error = "准备颜色卷积库失败：${error.message}\n${nativeMaskPlatformHelp()}")
    }
}

internal class JniColourProcessor : SampleNativeColourProcessor {
    private class Workspace(val pixels: Int) {
        fun buffer(bytes: Int) = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder())
        val source = buffer(pixels*4)
        val a = buffer(pixels*8)
        val b = buffer(pixels*8)
        val alpha = buffer(pixels*8)
        val kernel = buffer(13*8)
        val output = buffer(pixels*4)
    }
    private var workspace: Workspace? = null
    private var statistics = NativeMaskStats()
    @Synchronized override fun stats() = statistics
    @Synchronized fun resetStats() { statistics = NativeMaskStats() }
    @Synchronized override fun release() { workspace = null }
    private fun fail(reason: String): IntArray? {
        workspace = null
        statistics = statistics.copy(error = "$reason，颜色卷积已回退 Kotlin")
        return null
    }
    @Synchronized override fun convolve(pixels: IntArray, width: Int, height: Int, kernel: DoubleArray): IntArray? {
        if (statistics.error != null) return null
        if (width <= 0 || height <= 0 || width.toLong()*height != pixels.size.toLong() ||
            kernel.size !in 3..13 || kernel.size%2 != 1 || kernel.any { !it.isFinite() || it !in 0.0..1.0 } ||
            kotlin.math.abs(kernel.sum()-1.0) > 1e-12) return fail("Rust 颜色卷积输入不合法")
        if (pixels.size.toLong()*32 + 104 > 32L*1024*1024) return fail("Rust 颜色卷积工作区超过32 MiB")
        val start = System.nanoTime()
        val old = workspace
        val work = if (old != null && old.pixels >= pixels.size) old
            else Workspace(pixels.size).also { workspace = it }
        work.source.asIntBuffer().put(pixels)
        work.kernel.asDoubleBuffer().put(kernel)
        val status = NativeColourBridge.convolve(work.source,work.a,work.b,work.alpha,work.kernel,
            work.output,width,height,kernel.size/2)
        if (status != 0) return fail("Rust 颜色卷积 JNI 返回$status")
        val result = IntArray(pixels.size)
        work.output.asIntBuffer().get(result)
        statistics = statistics.copy(calls = statistics.calls+1, millis = statistics.millis+(System.nanoTime()-start)/1e6)
        return result
    }
}
