# 示例 Rust 遮罩 A/B 开关 — 2026-11-16

用户要求先提供示例开关供真实设备测试。本次只把已有 Rust CPU alpha 遮罩内核
接入流光 classic／云阶 Partita 的辉光准备，包括云阶引导线。不改变排版、shaping、
内部字素时间、媒体时间、动画及 Compose 绘制；不是完整 native 渲染器。

## 使用

待选歌页、真实歌曲页的「设置」，以及「开发／演示 → 设置」均提供
「实验 Rust 辉光遮罩」。默认关闭，每次新建会话仍从 Kotlin 开始，不写入持久偏好。
同一 session 在真实歌曲／开发演示间保留选择。先打开，再选歌也可以。

- 打开：尝试加载真实 JNI 库，核对 ABI v1，并运行 1×1 内核启动自检。
- 有调用后显示「实际：Rust CPU 遮罩 · N 次」，累计 JNI 准备耗时包含传输及结果数组，
  不包含文字测量、位图创建／读回、GPU 或全帧；不能当作 FPS。
- 已加载但无歌词／最简画质等没有遮罩工作时，显示「尚未计算遮罩」。
- 缺库、不支持的 ABI、ABI 自检失败、非法输入或 JNI 错误明确显示原因并回退 Kotlin。
  单个工作区上限 32 MiB；超过时本会话回退并提示，可关开重试。
- 关开会使遮罩缓存失效，按当前歌词与媒体位置重新准备；不会重置播放器、seek 或倍速。
  对比时可以暂停在同一点反复切换。暂停期间媒体动画仍冻结。
- JNI 同步执行，没有后台线程、native session 或保留指针。组件移除／切换时释放
  工作区引用，direct buffers 由 JVM/ART GC 回收，不声称立即返还进程内存。

测试时请固定同一歌曲／歌词、主题、画质、视口及动态效果设置，分别比较初次准备、
切句卡顿和持续播放；本次内核仅影响遮罩准备，不能推定持续动画掉帧已修复。

## Android 构建与交付

本次生成的真机测试包：`build/outputs/folia-sample-rust-experimental-debug.apk`。
这是含真实 `.so` 的 debug APK，Android 6.0/API23 起，ABI 为 arm64-v8a 和 x86_64；
没有交付 32 位 native。普通构建仍无需 Rust，开关在缺库时报告不可用。

Windows 开发机需要 Android SDK、固定 NDK **28.2.13676358/r28c**、Rust **1.91.1**
及以下 target；工具链与 Cargo.lock 锁定，不下载／提交 native 二进制到源码仓库：

```powershell
rustup toolchain install 1.91.1 --profile minimal
rustup target add aarch64-linux-android x86_64-linux-android --toolchain 1.91.1
./gradlew.bat :sample:android-app:assembleDebug -PfoliaExperimentalMask=true
```

可用 `-PfoliaRustCargo=<cargo.exe>` 选择私有安装，`CARGO_HOME`/`RUSTUP_HOME`沿用
调用环境。Gradle 可选任务只为 debug 生成／打包 jniLibs，默认命令、库 AAR/Maven 产物
不依赖 Cargo，不包含该 native 运行时。构建脚本目前支持 Windows NDK host。
NDK 链接目标 API23，显式 16 KiB ELF 页对齐。

普通 Kotlin 构建：

```powershell
./gradlew.bat :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin
```

Desktop 同样有开关；普通分发未捆绑 DLL，必须由启动 JVM 的 `java.library.path`
提供本地编译的 `folia_mask_experiment.dll` 才可加载，不自行搜索私人目录。
本次 Desktop 实际 JNI 验证入口：

```powershell
./native/experimental-mask/build-windows-gnu.ps1
./gradlew.bat :sample:shared:jvmNativeMaskRuntimeTest '-PfoliaNativeMaskDirectory=native/experimental-mask/target/release'
```

## 已验证与限制

- Windows x64/JDK21.0.5 实际 release DLL/JNI，`-Xcheck:jni`；两主题各 12 组
  Kotlin／Rust ARGB 全图完全相同：700/950/1500/2700/2900/3100 ms，800×600／380×600，
  相同合成 `wrap` 数据、FoliaParityLatin-Bold 44sp、背景及配置。
  前进/反向 seek、暂停、切回、resize、输入错误回退、移除组件通过。
- 示例实际设置开关调用 Rust（截图为 19 次），缺库时回退及重启默认 Kotlin；
  两种设置入口均通过 UI 测试。普通 shared 测试和 GlowMask/SoftwareBlur 回归通过。
- 旧 JNI ABI 的 30 组随机尺寸／半径逐像素及越界、只读、别名、NaN 等错误检查重跑通过。
- Android native 可选 APK／普通无 native APK 均构建，Desktop 编译通过；
  native APK 内两个 ABI 的 JNI 导出及 ELF LOAD 对齐检查、16KiB zipalign、签名验证通过。
- 本机 `adb devices` 没有设备。**没有本轮 Android 真机／模拟器运行或性能验证**，
  不把 Windows JNI 对照扩展为 Android 像素、设备帧率或整主题验收。
- 已有 classic／多主题视觉残差仍见 classic-parity.md、themes-parity.md；
  本次 A/B 证明后端切换保持当前画面，不证明原版移植残差已消除。

证据：`verification/native-mask/sample-switch/`。完整自动生成 PNG／报告位于
`build/verification/native-mask-sample/`，缺库设置截图位于
`sample/shared/build/verification/native-unavailable-settings.png`。

库只增加 scoped `ExperimentalFoliaMaskProcessor`／`LocalFoliaMaskProcessor` 窄边界，
默认 null 使用原 Kotlin。native 适配、装载、统计、开关和构建只属于 sample。
Rust 内核仍为本项目独立编写的现有 Kotlin CPU 遮罩等价实现，jni-sys 归属不变。
