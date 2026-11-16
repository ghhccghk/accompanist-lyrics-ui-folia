# 示例 Rust 遮罩 A/B 开关 — 2026-11-16

用户要求先提供示例开关供真实设备测试。本次只把已有 Rust CPU alpha 遮罩内核
接入流光 classic／云阶 Partita 的辉光准备，包括云阶引导线。不改变排版、shaping、
内部字素时间、媒体时间、动画及 Compose 绘制；不是完整 native 渲染器。

## 真机 UnsatisfiedLinkError 修复复核

2026-11-16 用户反馈后，读取已连接 arm64/API37、型号2509FPN0BC的已安装 APK：
仅含 `libandroidx.graphics.path.so`，没有 `libfolia_mask_experiment.so`。
实际通过同一 sample loader 重现缺库错误；不是把已有 .so 的链接失败猜成 ABI 不支持。
用含 native 的 debug APK `adb install -r` 更新后，该设备上实际 sample loader、ABI v1、
启动自检和15组ARGB对照通过（独立有限卷积，尺寸1×1、3×7、13×5，
sigma=1/2.25/10/20/4096），随后释放工作区。没有清除目标应用数据或卸载原应用。

界面现在保留加载阶段与原始 linker 详情，可选中文本复制，明确提示普通构建缺库时
应安装本页实验 APK，或带 `-PfoliaExperimentalMask=true` 构建。
不能把所有 UnsatisfiedLinkError 都诊断成缺库；缺依赖、缺符号和 JNI 入口失败
会显示各自原始原因。库仍不依赖 Rust；sample 构建策略已按下述新要求调整。

证据见 `verification/native-mask/device-load-fix/`，包含升级前实际APK的库清单、
升级前/后设备探测输出和本次交付APK哈希。此前 `sample-switch/` 是初次无设备阶段的证据。

无需启动播放器或操作设备界面即可重跑专用 JNI 探测：

```powershell
./gradlew.bat :sample:android-app:assembleDebug :sample:android-app:assembleDebugAndroidTest -PfoliaExperimentalMask=true
adb install -r sample/android-app/build/outputs/apk/debug/android-app-debug.apk
adb install -r -t sample/android-app/build/outputs/apk/androidTest/debug/android-app-debug-androidTest.apk
adb shell am instrument -w top.ghhccghk.accompanist.folia.sample.test/top.ghhccghk.accompanist.folia.sample.NativeMaskDeviceProbe
```

`-e expectMissing true` 仅用于刻意验证普通缺库构建；不是跳过 native 检查。
这次证明真机 native 加载及CPU结果可用，尚未证明Android完整画面一致、帧率改善或整主题达标。

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
没有交付 32 位 native。当前 Android sample 默认打包 Rust，开关仍默认 Kotlin。

Windows 开发机需要 Android SDK、固定 NDK **28.2.13676358/r28c**、Rust **1.91.1**
及以下 target；工具链与 Cargo.lock 锁定，不下载／提交 native 二进制到源码仓库：

```powershell
rustup toolchain install 1.91.1 --profile minimal
rustup target add aarch64-linux-android x86_64-linux-android --toolchain 1.91.1
./gradlew.bat :sample:android-app:assembleDebug -PfoliaExperimentalMask=true
```

可用 `-PfoliaRustCargo=<cargo.exe>` 选择私有安装，`CARGO_HOME`/`RUSTUP_HOME`沿用
调用环境。Gradle 为 Android sample 各构建变体生成／打包 jniLibs；库 AAR/Maven 产物
不依赖 Cargo，不包含该 native 运行时。构建脚本按主机选择Windows PowerShell或Linux/macOS sh；后两者尚无真实构建结果。
NDK 链接目标 API23，显式 16 KiB ELF 页对齐。

显式关闭 native 的 Kotlin sample 构建：

```powershell
./gradlew.bat :sample:android-app:assembleDebug :sample:desktop-app:compileKotlin -PfoliaExperimentalMask=false
```

Desktop 按当前主机默认捆绑 Windows DLL、Linux .so 或 macOS .dylib 为 sample-shared JAR 资源，首次打开开关时解包到独立临时目录并绝对路径加载；无需修改全局 PATH。关闭开关不卸载 JVM 已加载的 DLL。
支持的Desktop资源标识为windows-x86_64、linux-x86_64/aarch64、macos-x86_64/aarch64。
源码与脚本已接入Linux/macOS；实际native构建/加载目前仅在Windows验证。显式外部覆盖支持 VM 参数
`-Dfolia.mask.library=<DLL绝对路径>`，错误的显式路径会报告失败，不悄悄换用内置库。
可用 `-PfoliaNativeMaskDirectory=<含DLL目录>` 跳过 Desktop Cargo 构建、打包当前平台已编译的动态库。
Android 仍通过系统 APK native loader 加载 .so，错误提示分平台提供。

默认 sample 构建需要 Rust；两个 sample 都可用 `-PfoliaExperimentalMask=false` 排除 native。
此选项不改变运行时开关默认 Kotlin，不影响库独立消费。

```powershell
./gradlew.bat :sample:android-app:assembleDebug :sample:desktop-app:run
./gradlew.bat :sample:shared:verifyBundledDesktopMask
```

2026-11-16 后台验证：JDK21及用户JBR25.0.4实际从JAR加载DLL，空java.library.path，
ABI/启动自检/真实卷积/三次切换释放通过；缺库UI三测试通过。
连续默认→false构建，JAR/APK均不残留本项目native文件。
Android默认debug APK两ABI、16KiB对齐和签名通过；本轮没有设备运行验证。
详情见 `verification/native-mask/default-bundling/README.md`。

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
- 初次交付没有设备；本次修复已通过上方 arm64/API37 真机 JNI 加载和15组CPU像素对照。
  Android完整UI画面对照、设备帧率及x86_64设备运行仍未验证，不据此宣布整主题验收。
- 已有 classic／多主题视觉残差仍见 classic-parity.md、themes-parity.md；
  本次 A/B 证明后端切换保持当前画面，不证明原版移植残差已消除。

证据：`verification/native-mask/sample-switch/`。完整自动生成 PNG／报告位于
`build/verification/native-mask-sample/`，缺库设置截图位于
`sample/shared/build/verification/native-unavailable-settings.png`。

库只增加 scoped `ExperimentalFoliaMaskProcessor`／`LocalFoliaMaskProcessor` 窄边界，
默认 null 使用原 Kotlin。native 适配、装载、统计、开关和构建只属于 sample。
Rust 内核仍为本项目独立编写的现有 Kotlin CPU 遮罩等价实现，jni-sys 归属不变。

本轮内存修复及实际限制见 `verification/playback-stutter/memory/README.md`。当前测试APK：`build/outputs/folia-sample-native-memory-debug.apk`；本轮未替换设备上的应用。跨平台构建与Maven说明见 [BUILDING.md](BUILDING.md)。
