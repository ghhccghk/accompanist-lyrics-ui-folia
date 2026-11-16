package top.ghhccghk.accompanist.folia.sample

import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

internal actual fun loadSampleNativeLibrary() = DesktopNativeLibrary.load()

internal actual fun nativeMaskPlatformHelp(): String =
    "Desktop 需要 folia_mask_experiment.dll（Windows x64），不是 Android .so/APK。" +
        "请用 -PfoliaExperimentalMask=true 构建桌面示例以捆绑 DLL；" +
        "IDE 直接运行可在 VM options 指定 -Dfolia.mask.library=<DLL绝对路径>，或在启动时配置 java.library.path。"

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
                throw UnsatisfiedLinkError("folia.mask.library 必须指向存在的 DLL 绝对路径：$explicit")
            }
            // An explicitly supplied DLL must never be silently replaced by a bundled one.
            System.load(file.toRealPath().toString())
        } else {
            val windows = System.getProperty("os.name").lowercase(Locale.ROOT).startsWith("windows")
            val x64 = System.getProperty("os.arch").lowercase(Locale.ROOT) in setOf("amd64", "x86_64")
            val stream = if (windows && x64) javaClass.getResourceAsStream(
                "/native/windows-x86_64/folia_mask_experiment.dll") else null
            if (stream == null) {
                System.loadLibrary("folia_mask_experiment")
            } else {
                stream.use { input ->
                    val directory = Files.createTempDirectory("folia-mask-")
                    val dll = directory.resolve("folia_mask_experiment.dll")
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
