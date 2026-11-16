package top.ghhccghk.accompanist.folia.sample

import java.nio.file.Files
import java.nio.file.Path

internal actual fun loadSampleNativeLibrary() = DesktopNativeLibrary.load()

internal actual fun nativeMaskPlatformHelp(): String =
    "Desktop 需要匹配当前平台的 native 库：Windows folia_mask_experiment.dll、Linux libfolia_mask_experiment.so、macOS libfolia_mask_experiment.dylib。" +
        "请用 -PfoliaExperimentalMask=true 构建桌面示例以捆绑动态库；" +
        "IDE 直接运行可在 VM options 指定 -Dfolia.mask.library=<动态库绝对路径>，或在启动时配置 java.library.path。"

/** One extraction/load per classloader, shared across toggle sessions. Only this
 * sample embeds the DLL; no library publication or global PATH modification. */
private object DesktopNativeLibrary {
    private var loaded = false

    @Synchronized fun load() {
        if (loaded) return
        val explicit = System.getProperty("folia.mask.library")?.takeIf { it.isNotBlank() }
        if (explicit != null) {
            val file = Path.of(explicit)
            if (!file.isAbsolute || !Files.isRegularFile(file)) {
                throw UnsatisfiedLinkError("folia.mask.library 必须指向存在的动态库绝对路径：$explicit")
            }
            // An explicitly supplied DLL must never be silently replaced by a bundled one.
            System.load(file.toRealPath().toString())
        } else {
            val platform = desktopMaskPlatform(System.getProperty("os.name"),System.getProperty("os.arch"))
            val stream = platform?.let { javaClass.getResourceAsStream(it.resourcePath) }
            if (stream == null) {
                System.loadLibrary("folia_mask_experiment")
            } else {
                stream.use { input ->
                    val directory = Files.createTempDirectory("folia-mask-")
                    val dll = directory.resolve(checkNotNull(platform).libraryName)
                    // Register in reverse deletion order. Windows may retain a
                    // loaded DLL until process exit; never touch unrelated files.
                    directory.toFile().deleteOnExit()
                    dll.toFile().deleteOnExit()
                    Files.copy(input, dll)
                    System.load(dll.toAbsolutePath().toString())
                }
            }
        }
        loaded = true
    }
}
