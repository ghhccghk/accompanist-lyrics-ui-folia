package top.ghhccghk.accompanist.folia.sample

import java.util.Locale

internal data class DesktopMaskPlatform(val id: String, val libraryName: String) {
    val resourcePath get() = "/native/$id/$libraryName"
}

internal fun desktopMaskPlatform(osName: String, archName: String): DesktopMaskPlatform? {
    val arch = when (archName.lowercase(Locale.ROOT)) {
        "amd64", "x86_64" -> "x86_64"
        "aarch64", "arm64" -> "aarch64"
        else -> return null
    }
    val os = osName.lowercase(Locale.ROOT)
    return when {
        os.startsWith("windows") && arch == "x86_64" -> DesktopMaskPlatform("windows-$arch", "folia_mask_experiment.dll")
        os.startsWith("linux") -> DesktopMaskPlatform("linux-$arch", "libfolia_mask_experiment.so")
        os.startsWith("mac") -> DesktopMaskPlatform("macos-$arch", "libfolia_mask_experiment.dylib")
        else -> null
    }
}
