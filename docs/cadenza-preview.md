# 心象 Cadenza 校准预览

固定 Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5，AGPL-3.0-only。
本页描述实际入口；**仍未通过完整主题视觉验收**，不能将“可选择”当作“原版等价”。

## 使用

Android/Desktop 示例：显示设置 → 歌词主题 → 心象。真实歌曲与开发/演示均可使用；
可调主题字号、排版宽度、辉光与脉动。设置自动持久化，旧设置保持流光默认值。
不改变真实音频播放器、独立进程或既有 Rust 开关。

```kotlin
FoliaLyricsView(
    lyrics = lyrics, // 已发布 lyrics-core 的 SyncedLyrics
    playback = { snapshot }, // FoliaPlaybackSnapshot，播放器负责媒体位置
    style = FoliaStyle.Cadenza(CadenzaTuning()),
    options = FoliaOptions(reducedMotion = false),
    onLineClicked = { line -> playerSeek(line.start) },
)
```

`CadenzaTuning`：fontScale=1.12，widthRatio=.72，glowIntensity=1，motionAmount=1。
`wordColors` 为按源匹配顺序的 `FoliaWordColor`；`chorusLineIndices` 为明确指定的
core顶层行下标，默认空，不从伴唱猜测副歌。正文/活动色来自 `FoliaColors`。

构建/运行：`gradlew.bat :sample:desktop-app:run`；Android 用
`gradlew.bat :sample:android-app:assembleDebug`。无 Rust 工具链时给sample命令加
`-PfoliaExperimentalMask=false`，库本身不需要 Rust。环境设置见 BUILDING.md。

## 已实现的源行为

- `cadenzaPreparedState/Fragments/Placements`：真实字体测量、断行片段、完整词hero、
  种子布局、碰撞搜索与最小重叠兜底，保留内部字素时钟及完整塑形。
- `cadenzaEnvelopes/VisualizerCadenza`：normal/fast/instant、词/字素辉光、三层原阴影、
  入场/活动/余韵/退场、词云脉动和漂移；副歌圆环位于舞台空间，不误跟随文字旋转。
- 原11/14/16响应率改为事件分段的连续媒体时间积分；无固定帧率重放、不依赖上次画面。
  暂停冻结、seek直接重建，倍速由宿主位置体现，不再乘一次。
- 原最新活动句/结束点inclusive/重叠恢复选择；不用classic额外退出层。配合现有字幕、
  有限等待球、原始行点击/长按回调、可访问性及内容更换/卸载生命周期。

## 平台与数据适配

仅有整句时间的core歌词显示一个立即着色的完整短语，不生成逐词/逐字时间。
原版解析器可能合成词级时间，本库不把缺失数据伪装成逐词精度。
core伴唱作为附加字幕保留；重叠主唱按源最新活动句取舍。
减少动态取消平移/旋转/缩放/脉动/圆环，保留媒体驱动的显示、着色与淡出。

Standard保持原字素/词辉光；Reduced使用整片段辉光；Minimal关闭缓存辉光。
按预计RGBA遮罩大小在分配前决定档位，上限16MiB，回退通过FoliaStatus报告；
切换画质会释放旧准备对象。不限制/宣称整个进程、正文缓存、临时模糊或GPU内存上限。

## 验证与尚存差异

- 原函数包络30场景/2952观测、种子落点450片段、114原浏览器准备、205桥接落点；
  数值详见verification/cadenza-fixtures/README.md。
- 实际原版定格128对，修正尾空格后MAE9.971613；与原版真实RAF16轨迹/9113词状态、
  32对内部连续响应画面另列。原60Hz峰值位置差0.14666px、scale0.04071、alpha0.03784。
- 公开FoliaLyricsView与实际原版：相同整数毫秒快照、OFL字体、380/800×600、DPR1，
  32对真实播放截图，MAE约6。未改代码的两轮原浏览器捕获中，32/32数值状态完全相同、17/32 PNG相同，
  其余有小幅AA/合成栅格波动（source-repeat.json）；这不是
  修复前后改善，报告保留实际采样，不挑选较低值当验收。接触表已检查。
- 公开入口和独立Cadenza舞台像素一致；pause/反向seek/offset/resize/重叠恢复/空档/
  点击/长按/减少动态/最低画质/卸载重挂已运行。旧classic/Partita/Tilt公开路径回归通过。
- Android APK和Desktop编译通过；本轮实际Compose离屏运行。ADB当前无设备，
  **没有Android真机/模拟器验收，也未测主题设备性能**。未打开用户可见播放器窗口。

尚未消除：原RAF右端采样的刷新率相关阶跃差；CJK基线/AA、CSS与native阴影栅格；
原版同词片段id跨句复用的历史状态与native规范挂载重建之间的差别。多句切换目前有
原选句函数及native公开路径验证，仍需实际原版跨句完整画面对照。全曲准备性能也未验收。
这些是继续修复项，不以“有意差异”替代原验收要求。

复现：启动 `node verification/classic-browser/serve.mjs`，运行
`node verification/classic-browser/capture-cadenza-public.mjs`，
`gradlew.bat :library:jvmTest --tests '*Cadenza*'`，
`python verification/compare-cadenza-motion.py --public`。
对照图/逐帧散列在 verification/cadenza-public；完整原图/native图在build/verification对应目录。
