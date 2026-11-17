# 时计 Pendolo 校准预览

Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5，AGPL-3.0-only。
已开放真实主题入口，**正式视觉验收未通过**。密集tick、旋转/halo响应和文字光栅残差继续保留。

示例：设置 → 歌词主题 → 时计。真实选歌和开发/演示共用入口，设置自动持久化。
播放器、Android独立进程、Rust开关和采样语义不变；无歌词仍显示“未加载歌词”。
普通运行 `gradlew.bat :sample:desktop-app:run`；Android构建
`gradlew.bat :sample:android-app:assembleDebug`。Rust构建条件见BUILDING.md；
库Kotlin路径不依赖Rust；无需SPW或原lyrics-ui。

```kotlin
FoliaLyricsView(
    lyrics = lyrics,
    playback = { snapshot },
    style = FoliaStyle.Pendolo(PendoloTuning()),
    onLineClicked = { line -> seek(line.start.toLong() + offsetMillis) },
)
```

`PendoloTuning`正文/字幕倍率默认为1，范围.5..2；与`FoliaTypography.scale`
相乘后按原字号取整。主题默认活动正文28、闲置22；字幕活动16/闲置12，次轨.88再取整。
`typography.main`提供正文字体/字重，`typography.subtitle`提供字幕字体/字重；
字号由主题倍率定义，不把classic响应字号硬套轮盘。颜色取自`FoliaColors`。
`chorusLineIndices`为core顶层行索引，`wordColors`保留原范围匹配，默认均为空。
宿主不提供副歌/关键词时不猜造；伴唱作为真实额外行保留，不将alignment解释为副歌或鼓点。

仅行计时输入按原Pendolo在行内均匀派生字素时间，不称为真实逐词标注。
原core对象/ref、源起止、raw syllable与显示词/EGC时钟保留，源renderHints延长
只供选择/转场。字幕随轮盘内部行排版，不再叠加classic底部字幕。
`showTranslation/showPhonetic`控制轨，`showNextLine`不额外创建底部下一句，轮盘保留原邻句。

点击/长按反解实际hub旋转、-.65反校正、行/副歌缩放和translateY；按逆绘制顺序命中，
隐藏弧段/空行不拦截。空白舞台触摸可交由宿主。回调返回原对象，offset不写回歌词，
sample在seek时加回offset。可访问性提供可见行的点击/长按动作。

Standard/Reduced保留相同行级辉光；Minimal关闭副歌缓存辉光/halo，保留正文和时钟。
减少动态取消tick/宽度/halo过渡，保留稳定目标布局及计时着色；没有独立无限动画任务。
未限制整个进程内存，未测设备主题性能；长曲首次准备耗时仍待基线。

验证证据：verification/pendolo-public-stage。45原版完整文字轮盘对图（186行、
1302字段），公开入口真实lyrics-core输入与独立wheel逐像素一致。包括窄/宽屏、
独立大字号、Latin/CJK/Arabic、密集切句、0/入场/活动/原结束/延长结束/下一句时点。
按全部非背景像素并集计（不删除低alpha像素）：MAE6.137633，峰27.915080；
位置峰6.019072px。原旋转隔离位置峰.231502px，MAE5.550928，峰10.778672。
此为新矩阵/统计口径，不和旧阈值前景均值当改善比较；逐帧全屏/局部64px最差也记录。

暂停、随机seek、offset、resize、内容更换、切主题、减少动态/Minimal、卸载重挂、
来源点击/长按及空白触摸测试通过。首次父测试容器ripple污染暂停像素比较，已在
仅测试容器取消indication，renderer断言保持精确；首次仅字体无字号切classic崩溃，
已在公共入口解析缺失字号并回归。失败日志保留.work/pendolo-public-input*.log。

Android APK与Desktop编译、Compose离屏运行已经验证；**未启动真实Desktop窗口，
未进行Android模拟器/真机主题验收**。Android独立播放进程的设备待验项见android-playback-process.md。
密集语义事件与原RAF观察时刻/速度不同，不能靠固定一帧延迟掩盖；
复杂字体和阴影光栅继续校准。这些是未解决项，不是放宽后的最终标准。
