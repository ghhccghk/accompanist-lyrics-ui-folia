# Kotlin/Native 示例可行性试验

2026-11-16。目标是调查示例能否脱离 JVM 编译运行，不改变歌词库公共契约。
当前新增的是无窗口的生产时钟编译探针，**不是 Native 版播放器**，不参与 Maven 发布。
Android 与现有 JVM Desktop 继续作为实际 UI 示例；Rust/JNI 遮罩、jpackage 安装包与
Kotlin/Native 可执行程序是三条不同的构建路径。

## 参考与采用范围

研究 Kotlin/kmp-native-wizard，固定参考提交
`59f6c96655212346d1d7b8ba8d1b0afe57255bc6`。
其 `build.gradle.kts` 使用 Kotlin Multiplatform 的 Native target 和
`binaries.executable { entryPoint = ... }`；`src/nativeMain` 是 JSON 命令行示例，
没有 Compose 窗口或音频实现。README 声明模板采用 CC0。
本项目独立编写构建及探针，不搬入其示例源码、不引入其序列化依赖，
沿用本项目锁定的 Kotlin 2.4.10 / Compose 1.12.0。

## 可重复执行

模块仅在显式加入 `-PfoliaNativeProbe=true` 时进入构建，默认按宿主选择目标。
不需要 Cargo；首次运行 Gradle 会下载对应 Kotlin/Native 编译器、LLVM 和系统工具链。
仍需当前根工程配置所需的 JDK/Android SDK；不会自动修改系统 PATH。

Windows x64：

```powershell
./gradlew.bat -PfoliaNativeProbe=true :sample:native-probe:linkDebugExecutableMingwX64
./sample/native-probe/build/bin/mingwX64/debugExecutable/folia-native-probe.exe
```

Linux x64 / macOS Apple Silicon 的等价命令（尚需对应主机验证）：

```sh
./gradlew -PfoliaNativeProbe=true :sample:native-probe:linkDebugExecutableLinuxX64
./sample/native-probe/build/bin/linuxX64/debugExecutable/folia-native-probe.kexe

./gradlew -PfoliaNativeProbe=true :sample:native-probe:linkDebugExecutableMacosArm64
./sample/native-probe/build/bin/macosArm64/debugExecutable/folia-native-probe.kexe
```

也可用 `-PfoliaNativeTarget=mingwX64|linuxX64|linuxArm64|macosArm64|macosX64` 选择目标。
声明目标不代表当前宿主可交叉编译或运行该目标；macosX64 已被当前 Kotlin 标记为弃用。

探针直接通过源码目录筛选编译原有 `PreviewPlaybackController.kt` 和
`FoliaPlaybackSnapshot.kt`，链接实际 Compose runtime；没有复制替代时钟或平台 API 桩。
检查暂停冻结、2 倍速、暂停 seek、跨多个循环、结束、重播及反向 seek。
它验证可共享逻辑的 Native 执行能力，不能证明文字、窗口、音频或性能已达标。

完整示例的必要依赖解析检查：

```sh
./gradlew -PfoliaNativeProbe=true :sample:native-probe:checkNativeUiDependencies
```

这是一项诊断任务：依赖缺少目标变体时应失败，不纳入普通构建的 `check`。
可显式选择其他 Native target；即使解析通过，也还没有编译完整 UI 或播放器。
`verification/native-probe/dependencies.json` 保存 core/runtime/UI/Foundation 的实际 Gradle
module 元数据哈希和 Native 目标清单；Material3 的解析结果见诊断日志。
Material3 版本由现有 Compose 插件的 `compose.material3` 提供，与 shared 模块保持一致，未另行升级。

## 完整 UI 的阻碍与实施边界

| 层 | Windows/Linux Native | macOS arm64 |
| --- | --- | --- |
| Compose runtime 1.12.0 | 有发布变体 | 有发布变体 |
| lyrics-core 0.5.0 | 无发布变体 | 有发布变体 |
| Compose UI / Foundation 1.12.0 | 无发布变体 | 有发布变体 |
| 当前 sample、Folia 平台实现 | 仅 Android/JVM actual | 尚无 Native actual / app 入口 |

Windows/Linux 完整示例首先需要真正可用的 Compose Native 桌面图形后端与
已发布 lyrics-core 目标支持。模板本身不提供这些能力，不能通过只加 target 解决。
不会以原工程源码依赖、空 actual、删除 lyrics-core 或另写假播放器绕过。

macOS arm64 的必要依赖存在，是继续完整 Native UI 调查的候选；仍需要 Mac 主机、
窗口入口、Skia/文本绘制与像素处理、Unicode 分界/方向、文件选择、元数据和本地音频实现。
现有 JVM actual 使用 ICU4J、Java Sound、Swing/Java 文件 API，不能直接用于 Native。
应先在对应主机验证真实依赖与最小 FoliaLyricsView 绘制，再接入本地播放和跨平台视觉对照，
不能将有元数据变体解释成原生播放器已经可运行。

这次尝试不更换主渲染架构或已发布模型，不把 Native 探针加入歌词库 Maven 变体，
不据此宣称播放卡顿或内存占用已改善。

## 本轮验证

- Windows x64：最终配置的 `linkDebugExecutableMingwX64` 成功；实际运行 EXE 退出码 0，
  所有上述媒体时间断言通过。使用 Kotlin/Native 2.4.10，未替换成本机其他编译器。
- `checkNativeUiDependencies`：mingwX64、linuxX64 均以缺少对应变体失败，
  涉及 lyrics-core、UI、Foundation、Material3。macosArm64 的真实 klib 依赖下载/解析成功。
  这只验证必要依赖，未编译本库、sample 的 Native 平台实现或 GUI 入口。
- 默认不开 Native probe 时，现有 `:sample:shared:compileKotlinJvm` 与
  `:sample:shared:compileAndroidMain` 成功（生产代码未改，编译任务为 UP-TO-DATE）。
- 未运行 Linux/macOS Native 程序，也未重新安装 Android 包或启动桌面窗口；
  无本轮 Native 文字截图、真实音频播放或性能数据。

证据位于 `verification/native-probe/`：`windows-run.log` 是实际标准输出，
`windows-artifact.json` 记录 EXE 大小、哈希与运行退出码；构建日志和截取的逐目标
依赖诊断同时保留。命令行输出不作为 classic 视觉验收证据。
