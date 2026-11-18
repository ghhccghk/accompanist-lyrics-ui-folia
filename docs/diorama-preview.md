# 镜台 Diorama 实验调试预览

2026-11-18 用户授权开放调试入口。选择音频和可选歌词后，设置 → 歌词主题 →
镜台；开发／演示也使用同一个 `FoliaStyle.Diorama` / `FoliaLyricsView` 入口。
这不是正式视觉验收或远程发布。原来的七个主题继续为校准预览。

```kotlin
FoliaLyricsView(
    lyrics = lyrics, // 已发布 lyrics-core 0.5.0 的真实 SyncedLyrics
    playback = { playerSnapshot },
    style = FoliaStyle.Diorama(DioramaTuning(fontScale = 1f)),
    onLineClicked = { originalLine -> seek(originalLine.start + offsetMillis) },
)
```

宿主提供媒体毫秒，不额外运行时钟或重复计算倍速。正 offset 延迟歌词，回调返回
原始 core 对象，宿主将源 start 加 offset 后 seek。普通 seek、暂停与重挂按同一媒体
时刻重建，不累计 UI 帧。切歌会取消旧准备任务并换掉原来源对象。

当前能力：原版文字路径、真实透视／朝向、内部字素时间、默认 weighted shot picker、
可重建 follower、正常邻行窗口、三层活动文字、near/far 与 FrontSide 裁剪。
整曲文字和媒体节点在可取消后台任务准备；正文倍率和系统文字缩放改变真实文字
几何，画布只做一次 density 转换。128px 是源纹理 em，不是固定屏幕字号。
`DioramaTuning` 提供字号倍率、稳定路径种子及可选文字雾颜色；透明默认禁用雾，
组件不绘制整套背景。sample 保存主题和字号倍率；种子、雾色沿用已实现默认。

点击／长按逆变换到实际 BASE 纹理并检查墨迹 alpha、正面、near/far 和完全雾化。
透明 padding、未显示文字及纯辉光不拦截手势；邻行按实际绘制顺序处理。无歌词
由 sample 显示“未加载歌词”；无效／超限准备有可见错误。按用户最新选择，镜台
前奏／间奏不插入独立等待呼吸球，也不隐藏文字舞台。其他主题的等待行为保持原样。

纹理磁盘缓存默认开启，`DioramaTuning.cacheTexturesOnDisk` 和 sample 开关控制。
准备阶段 retained ARGB payload 超过 `rasterMemoryBudgetBytes`（默认32MiB）时，
按会话转存压缩的原生预乘像素；后台加载当前正文／辉光／邻行，余量预取下一窗口。
Canvas 与命中只读取已发布内存，不同步读盘。随机 seek 未命中时显示读取状态，
加载失败有可见错误；正常卸载删除该会话临时文件，进程强杀的残留清理尚未实现。
JVM 使用系统临时目录，Android 使用应用 cacheDir；临时文件上限512MiB，不缓存音频。
目标是32MiB驻留 ARGB payload，当前必需窗口可能更大；不是总PSS或瞬时峰值保证。

“纹理预算检查”默认开启，保留分配前边长8192及单张／当前窗口96MiB检查；关闭
磁盘缓存时仍检查整曲96MiB。可在设置关闭并持久保存，库侧对应
`DioramaTuning.enforceRasterBudget=false`。关闭不跳过512MiB磁盘上限或平台限制。
切换会取消并重新准备当前歌词，超限报错，不截掉歌词。预算不包括临时blur、
驱动、相机节点及窗口替换期间旧图的短暂存活，不是总PSS上限。
JVM 的额外邻行／活动采样器
分别有既有预算并在卸载时关闭；完成准备会显式使暂停帧失效重绘，避免残留回退帧。
普通位图随已卸载舞台的引用释放，由平台回收，不声称即时回收所有像素内存。

JVM 使用已校准的有界采样器和有加法层时的透明 F16 合成；Android 当前使用真实
普通纹理绘制回退，**没有** JVM fragment／F16 的等价承诺。Minimal 画质关闭
glow/soul，减少动态固定每句镜头且关闭 soul，暂停冻结全部媒体驱动变化。
Reduced 降低 glow/soul 强度，但尚无专属纹理预算档，不能据此宣称内存或帧率收益。

仍存在的差异：媒体 follower 与原 RAF 调度、纹理采样、Android 字体中线／辉光、
深度遮挡和混合 bidi。此入口切歌重新建立走廊，尚未接完整曲间 sequencer、三走廊
中断与同走廊循环飞回；普通回跳可重建。副歌／段落／鼓点没有宿主数据时保持缺省，
不从多声部猜造。背景粒子、完整三维场景、音频分析不在当前范围。

证据：`verification/diorama-public` 是公共集成与离屏运行，原固定上游 mounted
和合成对照分别保留在 `verification/diorama-mounted`、`diorama-text-surface`。
公开入口测试不能替代原版连续视觉、Android 真机、真实 Desktop 窗口或性能验收。

本地运行：`./gradlew :sample:desktop-app:run`；Windows 使用 `gradlew.bat`。
Android：`./gradlew :sample:android-app:assembleDebug`，sample 默认携带 Rust 实验库，
运行时仍按现有 Kotlin 默认／显式开关。无 Rust/NDK 可用
`-PfoliaExperimentalMask=false`；库本身不依赖 Rust。
