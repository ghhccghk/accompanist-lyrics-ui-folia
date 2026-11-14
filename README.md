# lyrics-ui-folia

独立 Kotlin / Compose Multiplatform 歌词组件，入口 `FoliaLyricsView`，数据依赖已发布的
`com.mocharealm.accompanist:lyrics-core:0.5.0`。许可为 **AGPL-3.0-only**；证据和来源台账见
[docs/upstream.md](docs/upstream.md)。仓库分离不代表许可证义务自动隔离。

## Implementation status

当前为可构建、可运行的开发版本，尚未完成首版全部验收，也没有公开发布。
两端示例默认使用原创合成歌词和**无音频演示时钟**；无需原工程、lyrics-ui、SPW、
外部播放器、Rust 或 NDK。实际能力和证据见 [实施记录](docs/implementation.md)。

- classic 安全文字分组、确定性词位姿、spring、计时颜色、缓存辉光、呼吸、整句退出。
- 原始 core 模型、仅行时间、多声部、翻译、注音、下一句和空档；不伪造逐词时间。
- 暂停、随机 seek、倍速、循环、歌词点击与长按；回调返回原始歌词行。
- 窄屏上方信息／下方歌词，宽屏左侧封面控制／右侧歌词；短窗口使用紧凑封面。
- 本地 UTF-8 标准／增强 LRC 导入、取消、错误提示、重新载入，文件上限 1 MiB。
- 字幕、字号、画质、减少动态效果、旋转、呼吸、间距、避免重叠和偏移设置独立保存。
- Android 配置变化保留会话；后台、桌面失焦／最小化暂停，返回后显式继续。
- 默认关闭的调试面板显示实际 Kotlin／画质、准备单元和模型诊断。

文字实现参考固定 Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5。
用户三段视频仅作文字视觉方向参考，没有分发其视频、原歌词、封面或字体。
当前分组、布局规划、辉光曲线和正文模糊仍有差异，见 [视觉规格](docs/visual-spec.md)；
不能称为逐帧等价复刻。

## Toolchain

锁定 Gradle 9.5.0、Kotlin 2.4.10、Compose 1.12.0、Material3 1.9.0、AGP 9.3.2、
Activity Compose 1.13.0、ICU4J 78.3 和 kotlinx.coroutines 1.11.0。
**安装 JDK 21**：JVM 编译／测试显式选择 21，避免 daemon 版本改变产物字节码。
Android 构建需要 SDK platform 37；设置 `ANDROID_HOME` 或未跟踪的 `local.properties`。
构建脚本没有开发者 SDK 或原工程路径。

```powershell
.\gradlew.bat :sample:desktop-app:run
.\gradlew.bat :sample:android-app:assembleDebug
.\gradlew.bat :library:jvmTest :sample:shared:jvmTest
```

APK：`sample/android-app/build/outputs/apk/debug/android-app-debug.apk`。
连接用户允许安装的设备／模拟器后可运行：

```powershell
adb install -r sample/android-app/build/outputs/apk/debug/android-app-debug.apk
adb shell am start -n com.mocharealm.accompanist.folia.sample/.MainActivity
```

已在 API 34 x86_64 模拟器安装运行；实机曾拒绝安装，没有绕过设备限制。
Linux/macOS、低 API 和实机表现不在已验证矩阵中。
Desktop：Space 播放／暂停，左右键 ±5 秒，Ctrl+O 导入，Ctrl+R 重新载入；
Esc 交由当前对话框处理。已聚焦控件先处理自己的按键。

## 导入约定

使用公开的 `EnhancedLrcParser`。已验证 `[mm:ss.xx]`／`[mm:ss.xxx]` 行时间及
`<mm:ss.xx>` 增强词时间，不宣称支持其他格式。文件须为 UTF-8，可带 BOM。
取消和失败保留当前歌词，后台解析有过期请求保护。解析器的末行无限结束时间保持原样，
演示时钟取末行开始后 5 秒与其他有限结束时间的较大值。

Android 使用 OpenDocument URI，不申请整盘权限。导入能力／历史目前只保留在进程会话，
退出进程后回到内置场景，需要重新选择文件；不宣称跨进程 URI 恢复。

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

使用 `com.mocharealm.accompanist.lyrics.folia.*` 和 core 公开模型。
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
`com.mocharealm.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT`，运行 JVM 渲染测试、
编译 Android，并检查二进制／源码包声明。没有配置远程推送或公开发布目标。

| 阶段 | 当前状态 |
| --- | --- |
| 独立工程、模型、索引、时钟、两端示例 | 已实现并构建／逻辑验证 |
| classic 和交互 | 可运行；完整上游视觉、连续录屏和设备矩阵待验收 |
| 导入、设置、生命周期 | 已实现；跨进程导入历史和部分原生边界待补 |
| Kotlin 优化 | 已测量优化时间索引；整体帧、GPU、准备和内存基线待完成 |
| Rust | 未引入；现有测量不足以支持进入实验 |
| 本地产物／独立消费 | 已通过；完整传递依赖审计、CI 和正式验收待完成 |

详细证据：[Android 运行检查](docs/android-smoke.md)、[性能测量](docs/performance.md)、
[剩余验收](docs/delivery.md)。
