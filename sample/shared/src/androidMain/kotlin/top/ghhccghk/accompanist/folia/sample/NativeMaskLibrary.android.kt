package top.ghhccghk.accompanist.folia.sample

internal actual fun loadSampleNativeLibrary() = System.loadLibrary("folia_mask_experiment")

internal actual fun nativeMaskPlatformHelp(): String =
    "如果提示找不到 libfolia_mask_experiment.so，请更新为含 Rust 的新版 APK（arm64-v8a／x86_64）；旧包或显式关闭 native 的包不包含该库。" +
        "自行构建使用 -PfoliaExperimentalMask=true。"
