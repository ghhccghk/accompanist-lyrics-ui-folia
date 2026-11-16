package top.ghhccghk.accompanist.folia.sample

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Sample-only JNI facade. Neither library publication nor Kotlin mode loads it. */
internal object NativeMaskBridge {
    @JvmStatic external fun version(): Int
    @JvmStatic external fun blur(source: ByteBuffer, a: ByteBuffer, b: ByteBuffer, sigmas: ByteBuffer,
        output: ByteBuffer, width: Int, height: Int, count: Int): Int
}

internal actual fun loadSampleNativeMask(): NativeMaskLoad {
    var phase = "加载 libfolia_mask_experiment"
    return try {
        System.loadLibrary("folia_mask_experiment")
        phase = "核对 JNI version 入口"
        check(NativeMaskBridge.version() == 1) { "Rust 遮罩 ABI 版本不兼容" }
        val processor = JniMaskProcessor()
        // Verify the actual JNI entry and kernel before the UI can claim availability.
        phase = "运行 JNI blur 启动自检"
        val result = processor.blur(intArrayOf(-1), 1, 1, floatArrayOf(1f))
        if (result?.single()?.single() != 0x02ffffff) {
            val error = processor.stats().error ?: "Rust 遮罩启动自检不匹配"
            processor.release()
            NativeMaskLoad(error = error)
        } else {
            processor.resetStats()
            NativeMaskLoad(processor)
        }
    } catch (error: LinkageError) {
        NativeMaskLoad(error = nativeMaskLoadFailure(phase, error))
    } catch (error: SecurityException) {
        NativeMaskLoad(error = "系统拒绝加载 Rust 遮罩库（${error.javaClass.simpleName}）")
    } catch (error: IllegalStateException) {
        NativeMaskLoad(error = error.message ?: "Rust 遮罩自检失败")
    }
}

/** Keep linker details: missing file, dependency, symbol and JNI entry differ. */
internal fun nativeMaskLoadFailure(phase: String, error: LinkageError): String =
    "$phase 失败（${error.javaClass.simpleName}）：${error.message ?: "无系统详情"}\n" +
        "如果提示找不到 libfolia_mask_experiment.so，请安装 Rust 实验 APK；普通构建不包含该库。" +
        "自行构建使用 -PfoliaExperimentalMask=true。"

internal class JniMaskProcessor : SampleNativeMaskProcessor {
    private class Workspace(val pixels: Int, val layers: Int) {
        fun buffer(bytes: Int): ByteBuffer = ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder())
        val source = buffer(pixels * 4)
        val a = buffer(pixels * 8)
        val b = buffer(pixels * 8)
        val sigmas = buffer(layers * 4)
        val output = buffer(pixels * layers * 4)
    }
    private var workspace: Workspace? = null
    private var statistics = NativeMaskStats()
    @Synchronized override fun stats() = statistics
    @Synchronized fun resetStats() { statistics = NativeMaskStats() }
    // No retained native pointers or tasks. Direct buffers become GC-eligible.
    @Synchronized override fun release() { workspace = null }

    @Synchronized override fun blur(pixels: IntArray, width: Int, height: Int, sigmas: FloatArray): List<IntArray>? {
        if (statistics.error != null) return null
        if (width <= 0 || height <= 0 || width.toLong() * height != pixels.size.toLong() ||
            sigmas.isEmpty() || sigmas.size > 16 || sigmas.any { !it.isFinite() || it !in 1f..4096f }) {
            statistics = statistics.copy(error = "Rust 遮罩输入不合法，已回退 Kotlin")
            return null
        }
        // Bound retained direct memory to 32 MiB per session; large masks use Kotlin.
        if (pixels.size.toLong() * (20 + 4 * sigmas.size) > 32L * 1024 * 1024) {
            workspace = null
            statistics = statistics.copy(error = "Rust 遮罩工作区超过 32 MiB，已回退 Kotlin")
            return null
        }
        val start = System.nanoTime()
        val old = workspace
        val work = if (old != null && old.pixels >= pixels.size && old.layers >= sigmas.size) old
            else Workspace(pixels.size, sigmas.size).also { workspace = it }
        work.source.asIntBuffer().put(pixels)
        work.sigmas.asFloatBuffer().put(sigmas)
        val status = NativeMaskBridge.blur(work.source, work.a, work.b, work.sigmas, work.output, width, height, sigmas.size)
        if (status != 0) {
            workspace = null
            statistics = statistics.copy(error = "Rust JNI 返回 $status，已回退 Kotlin")
            return null
        }
        val output = work.output.asIntBuffer()
        val masks = List(sigmas.size) { IntArray(pixels.size).also { output.get(it) } }
        statistics = statistics.copy(calls = statistics.calls + 1, millis = statistics.millis + (System.nanoTime() - start) / 1e6)
        return masks
    }
}
