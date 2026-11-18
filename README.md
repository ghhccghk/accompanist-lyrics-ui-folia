# lyrics-ui-folia

用 Kotlin / Compose Multiplatform 实现 Folia 风格歌词，让文字随音乐呈现不同的排版与动画。

支持 **Android** 和 **JVM Desktop**，基于已发布的 `lyrics-core` 数据模型，组件入口为
`FoliaLyricsView`。附带可选择本地歌曲、真实播放的两端示例。

> 当前为开发版本，尚未公开发布到 Maven Central。七个主题为校准预览，镜台为实验调试预览；
> 原版视觉对照与设备验证仍在进行，尚不承诺完整效果等价或 API / ABI 稳定。

## 📦 项目关系

- **lyrics-core**：歌词解析、数据模型与格式转换。本项目使用已发布的
  `com.mocharealm.accompanist:lyrics-core:0.5.0`，保留其原有坐标和模型包名。
- **lyrics-ui**：原有 Compose 歌词界面，是独立的 Apache-2.0 工程。
- **lyrics-ui-folia**：本仓库，独立实现 Folia 文字显示行为，不依赖原 `lyrics-ui` 或其内部 API，
  也不需要原工程、SPW 或外部播放器。

库只负责歌词显示；音频播放、文件选择与歌曲元数据读取属于示例程序。
库的 Kotlin 实现独立可用，不要求 Rust 环境或 native 运行时。

## ✨ 特性

- **多种歌词舞台**：逐词辉光、阶梯排版、倾斜分组、词云、轮盘、消息、文章运镜与文字透视。
- **真实歌词模型**：主唱、伴唱、多声部、翻译与注音，保留完整文字塑形和内部字素计时。
- **媒体时间驱动**：通过宿主播放快照处理暂停、seek 与时间跳变，点击／长按返回原始歌词行。
- **可调显示**：主题参数、字体、颜色、字幕、歌词偏移、画质和减少动态效果。
- **独立示例**：本地音频、外部／内嵌歌词、响应式布局，以及默认关闭的后端／帧率调试信息。

各主题支持范围不同。仅含行时间的歌词会按主题规则派生揭示进度，不将它宣称为真实逐词标注；
构建或逻辑测试通过也不等于原版视觉验收通过。

## 🎨 歌词主题

以下八项均可从示例的「设置 → 歌词主题」选择。

| 主题 | 名称 | 文字表现 | 当前状态 |
| --- | --- | --- | --- |
| Classic | 流光 | 居中歌词、逐词高亮与辉光、句间转场 | 校准预览 |
| Partita | 云阶 | 阶梯分组、错位入场与引导线 | 校准预览 |
| Tilt | 倾诉 | 倾斜的分段文字与空间排布 | 校准预览 |
| Cadenza | 心象 | 碎片／词云式强调与避让 | [校准预览](docs/cadenza-preview.md) |
| Pendolo | 时计 | 弧形轮盘、刻度与扫描 | [校准预览](docs/pendolo-preview.md) |
| Cappella | 群唱 | 连续消息、字素揭示与动态布局 | [校准预览](docs/cappella-preview.md) |
| Fume | 浮名 | 整曲文章、逐步揭示与镜头移动 | [校准预览](docs/fume-preview.md) |
| Diorama | 镜台 | 真实文字透视、分层与运镜 | [实验调试预览](docs/diorama-preview.md) |

完整视觉验收尚未完成：浮名仍有镜头交接残差，群唱／时计仍有密集场景差异，镜台的完整深度、
曲间编排与平台一致性尚未完成。准确状态与证据见 [当前进展](docs/NEXT_STEPS.md) 和
[主题对照](docs/themes-parity.md)。

## 🚀 安装

当前提供本地 Maven 发布，尚无可直接从 Maven Central 下载的正式版本。在本仓库执行：

```powershell
.\gradlew.bat :library:publishToMavenLocal
```

在消费工程的 `settings.gradle.kts` 中配置 `mavenLocal()`、`google()` 和 `mavenCentral()`，
然后在 Kotlin Multiplatform 模块的 `build.gradle.kts` 中添加：

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("top.ghhccghk.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT")
        }
    }
}
```

`lyrics-core` 已作为 API 依赖传递，无需引入 `lyrics-ui`。Android 库最低 API 21；
Desktop 使用 JVM，Kotlin/Native 探针不属于完整 UI 发布目标。
详细发布与独立消费方式见 [构建说明](docs/BUILDING.md) 和
[独立消费示例](verification/consumer/README.md)。

## 🎤 最小接入

宿主负责解析歌词、播放音频并提供可观察的播放快照：

```kotlin
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import top.ghhccghk.accompanist.lyrics.folia.FoliaLyricsView
import top.ghhccghk.accompanist.lyrics.folia.FoliaOptions
import top.ghhccghk.accompanist.lyrics.folia.FoliaPlaybackSnapshot
import top.ghhccghk.accompanist.lyrics.folia.FoliaStyle

@Composable
fun LyricsPane(
    lyrics: SyncedLyrics,
    playback: () -> FoliaPlaybackSnapshot,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = FoliaOptions(showTranslation = true, offsetMillis = 0)

    FoliaLyricsView(
        lyrics = lyrics,
        playback = playback,
        modifier = modifier,
        options = options,
        style = FoliaStyle.Classic(),
        onLineClicked = { line ->
            onSeek((line.start.toLong() + options.offsetMillis).coerceAtLeast(0L))
        },
    )
}
```

`FoliaPlaybackSnapshot` 包含 `positionMillis`、`isPlaying` 和 `discontinuity`。
播放器位置已经体现倍速，组件不会再次乘速；seek、循环回跳或切歌时应更新 `discontinuity`。
真实播放使用播放器时间，不同时运行演示时钟。

正 `offsetMillis` 表示歌词延后：显示时间为媒体时间减偏移，点击歌词换回媒体时间时加回偏移。
上例包含该换算；更换主题可使用 `FoliaStyle.Partita()` 等对应类型。

## ▶️ 示例程序

首次打开进入待选歌页，选择音频后真实播放。歌词与翻译可选；未选外部歌词时自动尝试读取
歌曲内嵌歌词。没有歌词仍可播放，并显示「未加载歌词」；纯文本歌词单独展示，不伪造时间。

- 播放／暂停、进度拖动、歌词点击跳转、重播、重新选歌和补选歌词／翻译。
- 窄屏顶部曲目信息、下方歌词；宽屏左侧封面与控制、右侧歌词。
- 已验证的 UTF-8 标准／增强 LRC，以及 MP3 ID3、FLAC、M4A／MP4、WAV ID3 内嵌歌词。
- 合成歌词与无音频时钟位于「开发／演示」，倍速／循环控件目前也仅在该入口提供。
- 可选画质、减少动态和渲染分辨率；调试信息显示实际后端与帧回调统计，回调 FPS 不等于 GPU 呈现 FPS。

需要 **JDK 21**；Android 构建需要 **SDK platform 37**，示例最低 API 23。工具链版本由仓库锁定。

```powershell
# Desktop
.\gradlew.bat :sample:desktop-app:run

# Android debug APK
.\gradlew.bat :sample:android-app:assembleDebug
```

APK 输出：`sample/android-app/build/outputs/apk/debug/android-app-debug.apk`。
Linux／macOS 使用 `./gradlew`；已提供构建脚本，尚无对应主机完整构建与运行验证。

示例默认打包 Rust 实验库，运行时仍默认 Kotlin。没有 Rust／NDK 时可显式关闭打包：

```powershell
.\gradlew.bat :sample:desktop-app:run -PfoliaExperimentalMask=false
.\gradlew.bat :sample:android-app:assembleDebug -PfoliaExperimentalMask=false
```

Rust 环境见 [构建说明](docs/BUILDING.md) 和 [实验说明](docs/native-mask-sample.md)。
Windows 可用 `-PfoliaRenderApi=OPENGL` 选择后端，当前 JVM Skiko 路径不支持选择 Vulkan。

**播放限制**：Android 使用私有播放进程；Desktop 最低可靠解码范围是 PCM WAV／AIFF／AU，
能够读取其他容器标签不代表能够播放该音频。新 Android 跨进程播放与内嵌歌词的完整设备
流程仍待验证，不能沿用旧单进程结果作为证明。详见 [本地播放](docs/local-playback.md)。

## 📚 文档与验证

- [当前进展与待验收项](docs/NEXT_STEPS.md) · [主题对照](docs/themes-parity.md) · [Classic 对照](docs/classic-parity.md)
- [构建与本地 Maven](docs/BUILDING.md) · [独立消费验证](verification/consumer/README.md)
- [本地播放与导入](docs/local-playback.md) · [内嵌歌词](docs/embedded-lyrics.md) · [Android 播放进程](docs/android-playback-process.md)
- [后端与帧统计](verification/render-debug/README.md) · [渲染分辨率](docs/render-resolution.md) · [性能测量](docs/performance.md)

```powershell
.\gradlew.bat :library:jvmTest :sample:shared:jvmTest
python scripts/verify-consumer.py
```

独立消费脚本在本地发布后，从独立 Gradle 工程验证 JVM 渲染／回调、Android 编译和产物声明，
不会上传。各项记录分别说明算法、画面、平台运行与性能的验证范围，不以离屏测试替代设备验收。

## 🤝 参与贡献

欢迎提交问题与改进。报告视觉问题时，请提供平台、主题、字体、视口、媒体时间点与可分发的最小
歌词；性能问题请附设备和测量条件。涉及公共 API 或主题行为的大改动，建议先讨论。
引入上游代码或资源时请保留来源、固定提交、修改说明和适用许可，不提交无权分发的歌曲、歌词、封面或字体。

## 📜 许可与致谢

本项目采用 **AGPL-3.0-only**，见 [LICENSE](LICENSE)。文字效果参考 Folia 固定版本
`v0.7.26`，提交 `cc6226e464050a456a2913e062374f23d3844cc5`。

原 `lyrics-core`／`lyrics-ui` 的 Apache-2.0 发布边界保持不变；本仓库许可不改写原项目许可，
独立仓库也不表示许可证义务自动隔离。逐项来源、版权与修改记录见
[NOTICE](NOTICE)、[第三方声明](THIRD_PARTY_NOTICES.md) 和 [来源台账](docs/upstream.md)。
