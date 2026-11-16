# lyrics-ui-folia

独立 Kotlin / Compose Multiplatform 歌词组件，入口 `FoliaLyricsView`，数据依赖已发布的
`com.mocharealm.accompanist:lyrics-core:0.5.0`。许可为 **AGPL-3.0-only**；证据和来源台账见
[docs/upstream.md](docs/upstream.md)。仓库分离不代表许可证义务自动隔离。

本项目 Maven 坐标为 `top.ghhccghk.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT`，
Kotlin 包名前缀为 `top.ghhccghk.accompanist.lyrics.folia`。示例包名也使用新前缀。
外部 `lyrics-core` 的发布坐标及模型包名保持原样；这是本项目命名迁移，不涉及原工程。

## Implementation status

当前为可构建、可运行的开发版本，尚未完成首版全部验收，也没有公开发布。
两端示例默认待选歌，用户选择本地音频后真实播放；歌词和翻译可选。原创合成场景和
无音频时钟位于“开发／演示”入口。无需原工程、lyrics-ui、SPW或外部播放器。
库独立使用不需要Rust；sample默认打包实验Rust遮罩，运行时仍默认Kotlin。
无Rust/NDK时可给sample构建传入 `-PfoliaExperimentalMask=false`。
平台格式限制与实际运行证据见 [本地播放](docs/local-playback.md)。

- classic 安全文字分组、确定性词位姿、spring、计时颜色、缓存辉光、呼吸、整句退出。
- 云阶Partita已有独立阶梯块、块级入场、引导线和逐词/字素辉光，处于移植校准阶段，
  完整视觉尚未验收。设置→歌词主题→云阶，可调引导线/语义/错位/避让。
  浮名、心象、群唱、倾诉、镜台、时计尚未实现，见[主题对照](docs/themes-parity.md)。
- 原始 core 模型、仅行时间、多声部、翻译、注音、下一句和空档；不伪造逐词时间。
- 库从宿主快照支持暂停、随机seek、倍速和循环重建；点击/长按回调返回原始歌词行。
- 真实音频示例提供播放/暂停、进度拖动、歌词点击、重播和重新选歌；倍速/循环控件仍仅在开发/演示。
- 窄屏上方信息／下方歌词，宽屏左侧封面控制／右侧歌词；短窗口使用紧凑封面。
- 本地 UTF-8 标准／增强 LRC 导入、取消、错误提示、重新载入，文件上限 1 MiB。
- 真实音频页面提供翻译、注音、下一句、画质、减少动态及偏移设置；开发/演示另有字号、旋转、呼吸、间距和避让调试设置。
- Android 配置变化保留会话；后台、桌面失焦／最小化暂停，返回后显式继续。
- 开发/演示中的调试面板默认关闭，显示实际Kotlin／画质、准备单元和模型诊断。

文字实现参考固定 Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5。
用户三段视频仅作文字视觉方向参考，没有分发其视频、原歌词、封面或字体。
分组／内部字素计时、避让几何、正文与辉光曲线、呼吸和字幕已建立原版函数／DOM／
实际 Motion 对照；完整帧仍有文字覆盖、辉光边缘及平台计量残差，尚未完成视觉验收。
源码依据与逐项结果见 [核心对照](docs/classic-parity.md)，不能称为逐帧等价复刻。

## Toolchain

锁定 Gradle 9.5.0、Kotlin 2.4.10、Compose 1.12.0、Material3 1.9.0、AGP 9.3.2、
Activity Compose 1.13.0、ICU4J 78.3 和 kotlinx.coroutines 1.11.0。
**安装 JDK 21**：JVM 编译／测试显式选择 21，避免 daemon 版本改变产物字节码。
Android 构建需要 SDK platform 37；设置 `ANDROID_HOME` 或未跟踪的 `local.properties`。
构建脚本没有开发者 SDK 或原工程路径。默认Android sample另需NDK28.2.13676358、
Rust1.91.1及两个Android target；Windows sample默认构建并捆绑x64 DLL。
安装步骤与显式关闭方式见 [Rust sample](docs/native-mask-sample.md)。
Windows/Linux/macOS构建路径、真实验证范围与Maven命令见 [构建说明](docs/BUILDING.md)。

```powershell
.\gradlew.bat :sample:desktop-app:run
.\gradlew.bat :sample:android-app:assembleDebug
.\gradlew.bat :library:jvmTest :sample:shared:jvmTest
```

APK：`sample/android-app/build/outputs/apk/debug/android-app-debug.apk`。
连接用户允许安装的设备／模拟器后可运行：

```powershell
adb install -r sample/android-app/build/outputs/apk/debug/android-app-debug.apk
adb shell am start -n top.ghhccghk.accompanist.folia.sample/.MainActivity
```

已在 API30／34 x86_64 模拟器安装运行并采集固定字体画面，API30验证Kotlin软件模糊；
两端暂停和逆向seek重访逐像素一致。实机曾拒绝安装，没有绕过设备限制。
Linux/macOS、API21..29和实机表现不在已验证矩阵中；模拟器不作为性能结论。
Desktop真实音频：Space播放/暂停，左右键±5秒。开发/演示另有Ctrl+O导入、Ctrl+R重新载入；
Esc 交由当前对话框处理。已聚焦控件先处理自己的按键。

## 导入约定

真实音频使用公开AutoParser，开发/演示单文件导入使用EnhancedLrcParser。
已验证 `[mm:ss.xx]`／`[mm:ss.xxx]` 行时间及
`<mm:ss.xx>` 增强词时间，不宣称支持其他格式。文件须为 UTF-8，可带 BOM。
取消和失败保留当前歌词，后台解析有过期请求保护。解析器的末行无限结束时间保持原样，
演示时钟取末行开始后 5 秒与其他有限结束时间的较大值。

Android 使用 OpenDocument URI，不申请整盘权限。导入能力／历史目前只保留在进程会话，
退出进程后回到待选歌页，需要重新选择文件；不宣称跨进程 URI 恢复。

“补选歌词／翻译”在原播放器上应用，不重新准备音频或跳转。独立翻译按相同起始
时间戳匹配，没有匹配时明确报错。取消、失败和过期候选保留原会话。
有限前奏／间奏采用所有声部时间并集，严格大于5秒，等待文字退场后显示原版等待球；
无歌词、出错和歌曲结束均有独立状态。减少动态、暂停和seek使用相同媒体时间。
本项两端构建、Desktop真实音频、API34真实音频与等待球验证见[本地播放记录](docs/local-playback.md)；
原生系统文件选择完整端到端与实机尚待复核，Desktop仅宣称PCM WAV/AIFF/AU最小支持。

## 最小接入

宿主提供可观察的播放快照和 seek，组件不再次乘倍速，也不创建播放器：

```kotlin
@Composable
fun LyricsPane(
    lyrics: SyncedLyrics,
    snapshot: () -> FoliaPlaybackSnapshot,
    seek: (Long) -> Unit,
) {
    FoliaLyricsView(
        lyrics = lyrics,
        playback = snapshot,
        modifier = Modifier.fillMaxSize(),
        onLineClicked = { seek(it.start.toLong()) },
    )
}
```

使用 `top.ghhccghk.accompanist.lyrics.folia.*` 和 core 公开模型。
快照含毫秒位置、`isPlaying`、`discontinuity`。正 offset 表示歌词延后：渲染使用媒体时间
减 offset，点击换回媒体时间时 seek 到 `line.start + offset`。
可编译源码见 [独立消费示例](verification/consumer/README.md)。API 尚未承诺 ABI 稳定。

## 本地消费验证和交付状态

```powershell
./scripts/verify-consumer.ps1
# 从已提交 HEAD 导出干净源码，验证两端与独立消费（需 SDK 环境变量）
./scripts/verify-checkout.ps1
```

只生成 `build/local-maven` 文件仓库，再由独立 Gradle 根目录消费
`top.ghhccghk.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT`，运行 JVM 渲染测试、
编译 Android，并检查二进制／源码包声明。没有配置远程推送或公开发布目标。

| 阶段 | 当前状态 |
| --- | --- |
| 独立工程、模型、索引、时钟、两端示例 | 已实现并构建／逻辑验证 |
| classic 和交互 | 已有固定原版数值／DOM和JVM／API30／34画面对照；完整视觉、真实窗口连续录屏及实机待验收 |
| 导入、设置、生命周期 | 已实现；跨进程导入历史和部分原生边界待补 |
| Kotlin 优化 | 已按真实歌曲采样修复软件模糊热点；内存分配检查见verification/playback-stutter，整会话改善仍待复测 |
| Rust | sample CPU遮罩实验已接入；默认Kotlin，局部像素对照通过，不宣称全帧提速 |
| 本地产物／独立消费 | 干净源码导出构建及独立消费已通过；完整传递依赖审计、CI 和正式验收待完成 |

详细证据：[Android 运行检查](docs/android-smoke.md)、[性能测量](docs/performance.md)、
[剩余验收](docs/delivery.md)。
