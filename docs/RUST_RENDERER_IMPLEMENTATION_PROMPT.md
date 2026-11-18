# Rust 原生歌词渲染器：无人值守实施提示词

请开始实施 `F:\android app code\lyrics-ui-folia` 的可选 Rust GPU 歌词渲染器，按既定方案完成代码、示例接入和 Maven 产物。每完成一个可验证的小功能，执行必要验证并本地 Git 提交，然后继续下一项，不逐项等待我回复“继续”。

## 1. 目标、依据和本轮终点

首先阅读完整方案：

`F:\android app code\lyrics-ui-folia\docs\RUST_RENDERER_PLAN.md`

这是本次新方向的实施依据。目标是让示例中部分卡顿主题通过真实 Rust GPU 路径绘制，并能作为可选库通过 Maven 坐标独立接入。

首个交付闭环为：

**实际 lyrics-core → 云阶 Partita 的完整文字与效果 → Rust GPU → Android 原生歌词区域 → 示例选择后端 → 独立 Maven AAR 消费 → 同质量与稳定性／性能验证。**

新方向覆盖旧文档“Rust 只做 CPU 计算实验”的阶段限制；不要继续把全部精力投入 CPU 遮罩微基准。保持原示例的选歌、播放、内嵌歌词、设置与导航，只替换歌词渲染区域。

先做到 Android 首个主题的可发布候选，再按方案评估镜台等下一个明确热点。Windows/JVM 为后续阶段，不用其窗口嵌入难点阻塞 Android；也不能在 Android 未闭环前同时重写全部主题和所有平台。

本次“可发布”指发布产物完整、经独立坐标消费验证，并有构建和来源材料；不授权自动远程上传 Maven Central、推送代码或创建远程仓库。

## 2. 开始前检查，接续当前状态

方案核对时 HEAD 为 `fdcc1e5`。先确认实际 Git 根目录、HEAD、分支、status 和适用 AGENTS.md；若已有后续功能，以实际代码与最新用户指令接续，不回退或重做。

以下是已完成背景，不是需要重新创建的功能：

- 八主题均已有 sample 入口；Diorama 是后来获准开放的实验调试预览。
- 示例已有用户选音频、真实播放、外部／内嵌歌词、帧诊断和配置。
- 镜台已有纹理预算、会话磁盘缓存和 Windows sampler 修复。
- 当前等待策略：Diorama／Fume／Cappella／Pendolo 保持连续舞台；其他四主题沿用有限空档等待球。
- Rust CPU mask／colour 实验已有实现；它们不等于本任务的 native GPU renderer。
- 基础 Kotlin 库已有本地 Maven 独立消费验证。

方案及本提示词是本任务已明确提供的设计文件，核对后可纳入任务提交。以下两项是此前遗留的外部改动，先检查实际状态，保留并排除于本任务提交：

- `sample/android-app/build.gradle.kts` 的 release/debug 签名相关改动。
- 未跟踪 `gradle/gradle-daemon-jvm.properties`。

不得因为它们未提交就清理、覆盖或一并提交。修改相同文件时只暂存本任务 hunks。

## 3. 参考仓库及可复用范围

参考项目：`https://github.com/Techuouo520/Folia-rust-android`。

本次调研固定提交：`d7ff03e083d28069c973cf204b4a8f0a9d5c8594`。
其 Cargo.lock 中 Folia-rust 来源提交：`39e33e8e89980289edcb01ad1fa9dde2b676ba9f`。

本机只读参考副本：

`F:\android app code\lyrics-ui-folia-review\2026-11-18\references\Folia-rust-android`

使用前核对提交。副本不可用时，在自己的参考目录获取固定源码；不把 main 作为浮动生产依赖。原始视觉／语义基线仍为 folia-major `v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5`。

重点检查：

- `MonetLyrics.kt` 的 AndroidView／TextureView／渲染线程／Choreographer／surface 生命周期。
- `crates/folia-bridge/src/monet.rs` 的 NativeWindow／wgpu／iced_wgpu 接入与呈现。
- `crates/folia-render` 的必要 GPU 管线。
- `crates/folia-visualizer/src/styles/partita.rs`、`styles/glow.rs` 及注册表。
- Cargo manifests、lock、JNI、native 打包与第三方补丁。

必须保留以下判断：

1. 参考仓库是应用，不是现成 Maven 库。Android 已接通的主要是 Monet 页面，并混合了 Android 原生逐字歌词控件。
2. 参考 Rust 注册表没有 Fume／Cappella／Pendolo／Diorama；不能假设它提供这些主题。
3. 参考 Partita 当前 halo 是放大半透明文字叠画，不能直接替代本项目真实模糊／多层辉光。少做效果不能算同质量提速。
4. 整个 folia-bridge 包含播放器、资料库、在线平台、网络、keyring、QuickJS 等。只抽取渲染所需部分，不引入整套应用依赖。

优先复用平台宿主和必要 GPU 模块。第一条产品路径使用本项目已验证的文字准备与场景语义；既有 Rust 主题只在确认完整 shaping、效果和时间语义可满足要求时采用。不要同时长期维护两套未完成 renderer 原型。

按文件记录来源提交、原路径、目标路径与修改点；保留既有 AGPL-3.0-only 和第三方声明，不擅自改变授权版本、不搬未知授权字体或图标。

## 4. 必须保持的工程结构

原 Apache 工程 `F:\android app code\Accompanist` 仅允许只读参考，不修改源码、配置、依赖、许可证或现有用户变动。本项目独立发布，不通过 includeBuild、子模块、本机路径或 internal API 耦合原 lyrics-ui。

保留基础 `lyrics-ui-folia` 的 Kotlin 独立使用能力；它的 POM 和 metadata 不引入 native runtime／sample／播放器。

按需要增加：

- `native-renderer-android`：可选 AAR，新的 `FoliaNativeLyricsView`、surface、loader、JNI、来源映射、consumer rules 和 .so。
- `native/renderer`：与旧 experimental-mask 分离的 scene／GPU／JNI 实现。
- `sample:android-native-app`：复用 shared UI 的薄入口、独立 application ID，不复制播放器或整套示例。

Maven group 保持 `top.ghhccghk.accompanist`，Android 可选产物拟用 `lyrics-ui-folia-native-android`。外部 lyrics-core 坐标和包名保持原样。

首版 native AAR／新示例入口按 minSdk 26 规划；原基础库 minSdk 21、普通 sample minSdk 23 保持。运行时 fallback 不能解决 manifest minSdk 或最终包 ABI 不兼容，禁止 overrideLibrary 掩盖。

现有 `FoliaLyricsView` API 保持可用。若需要访问已验证的 internal 准备结果，抽取窄的、有版本的 experimental renderer bridge；不反射、不依赖 Skiko 私有指针，也不复制一整份基础库。必要时增加兼容入口，避免直接改变已编译调用方契约。

## 5. 实施顺序与每步完成条件

### P0：确认热点并确定最小依赖

读取最新性能记录，选同字体／尺寸／画质／分辨率／歌词场景建立当前 Kotlin 基线。先检查 Partita 软件 blur 是否仍是实际热点，区分准备、绘制、上传、GPU、缓存和播放器问题。

核对参考渲染代码的最小依赖图、字体资源、Cargo／NDK 需求和 surface 能力。工具链需要变化时使用项目局部可重现配置，不因此全库升级 Kotlin／Compose／Gradle 或修改系统 PATH。

输出短的当前执行清单、第一项行为和验收命令，然后直接实现。没有设备时继续构建／ABI／AAR／场景工作，保留待测性能项，不在调查报告或重复微基准上停住。

### P1：先形成可独立消费的 native 骨架

实现一个实际 native surface、JNI session／ABI 版本／错误状态／销毁，打包专用 `liblyrics_folia_renderer.so`。最小图形只用于验证宿主和发布链路，不能称主题交付。

从本地 Maven 仓库用新工程按坐标依赖 AAR，确认 .so 来自 AAR 而非源码目录／java.library.path。消费工程不需要 Rust、Cargo、NDK native 构建任务或本仓库脚本。

先完成 ARM64；有对应环境时验证 x86_64。只编译未运行的 ABI 如实标记。完成必要检查并提交，再进入真实文字。

### P2：真实 core 与文字进入 GPU

使用实际 lyrics-core 数据，保留原来源对象在 Kotlin 表中，跨 JNI 只传稳定 source ID、scene generation 和批量数据。

完整文字 shaping 后准备纹理和显示／内部计时映射。内容、字体解析、尺寸、density／fontScale 或布局配置变化时重新准备；不得逐帧解析歌词、塑形全曲、生成大遮罩或上传整屏 Bitmap。

先验证真实文字、颜色、alpha、裁剪、缩放和来源 ID。正常帧不能从 GPU 读回整屏再交 Compose 重画。CPU prepared texture 路径可以在准备时上传，不能伪装成纯 Rust shaping。

### P3：云阶完整主题与示例闭环

补齐实际 blur／多层 glow、布局／动画、字幕、有限等待球、交互、accessibility、大字号和减少动态。保留必要的文字系统／语言差异测试，不按 UTF-16 单字符拆字形。

示例增加按主题的 Kotlin／NativePreferred／NativeRequired 选择，显示请求路径、实际路径、当前状态和明确回退原因。初期默认 Kotlin；安装了 .so 不等于该主题已由 native 绘制。

NativeRequired 必须真实执行 native，失败显式暴露；不能靠静默 Kotlin fallback 通过 native 用例。NativePreferred 可在不支持／可恢复错误时回退，保持当前歌词、配置和播放位置。

验证用户选歌、内嵌／外部歌词、播放暂停、seek、点击／长按、字号、主题切换、前后台、surface 重建、退出和多实例。一个主题完成全部链路后提交，再继续下阶段。

### P4：稳定性、性能和发布候选

完成真实环境的配对测量、release＋R8 消费、ELF／16KiB／APK 对齐、资源压力和源归档。具体指标与方法见方案第11节。

建议门槛在测量前固定：确认卡顿场景的 P95 整帧时间改善至少20%、可靠测量的超帧预算次数下降至少30%、P99 不出现超过5%的退化；同时通过同质量检查。指标不可观测时补测量方法，不能以 CPU callback 数冒充呈现帧率。结果不足则保持实验，不自动替换。

首个主题闭环完成后，依据 trace 推进镜台等第二主题。镜台复用现有文字场景并逐步迁 GPU pass，不从零重写其完整相机，也不顺带引入粒子背景／音频分析。后续 Windows/JVM 同样要真实宿主和平台 JAR 独立消费后才声明支持。

## 6. 时钟、线程、生命周期和失败约束

- Rust 负责绘制，不创建第二条音频／歌词时钟。`FoliaPlaybackSnapshot.positionMillis` 已包含速度，不再乘一次；offset 仅在约定边界处理一次。
- 渲染线程的 vsync 只决定何时画，媒体状态仍由宿主提供。不得从 worker 直接无同步读取 Compose lambda 或可变播放器镜像。
- 需要脱离主线程的 frame source 时，使用不可变／原子快照和与现有播放器一致的求值规则，保留暂停、缓冲、外推上限和 discontinuity；不无依据按1x外推。
- 任意顺序 seek、切歌和重挂可重建；不通过显示帧累计、固定帧率或人工延迟追截图。
- 首个宿主可参考 TextureView；同一真实主题完成后再做一次有界 SurfaceView 对照，核对叠层／裁剪／输入和呈现成本，选定一种正式路径。
- session、surface、资源和回调有 generation；创建／resize／detach／destroy 顺序化。发布 API 要支持多实例，不能照搬参考的单 POSTER 全局假设。
- 不可见／暂停无必要变化时停用无意义帧提交；GPU 错误有界处理，切换只回退一次，不无限重建。
- 原资源必须在 GPU 不再使用后释放；CPU／GPU／staging／mip／磁盘缓存及切换双份资源都计预算。
- 点击返回来源对象，命中使用实际可见变换；空白穿透、长按取消、菜单／字幕覆盖及屏幕阅读器语义都验证。
- 原生 GPU 路径不重复运行旧 Rust CPU mask／colour；诊断区分两个独立能力。
- native 句柄／长度／stride／大小运算先校验，不保存失效 JVM 地址；Rust panic 不跨 FFI。
- Kotlin catch 或 Rust catch_unwind 无法保证恢复 abort、native 崩溃或驱动故障，不宣称所有错误都能 fallback。
- 渲染不迁入现有私有播放进程，不建立逐帧像素 IPC。

## 7. Maven 与来源交付要求

保留基础 Kotlin-only consumer 检查，新增 native consumer，至少验证：

1. 基础库不引入 native 依赖或 .so。
2. NativeRequired 消费真实 AAR 并加载／显示／交互；无 sample 类或本机路径依赖。
3. NativePreferred 的能力失败与回退行为正确，不重置播放器。
4. release／R8 后 JNI 类和入口仍可用；consumer rules 随 AAR 提供。
5. 包含正确 ABI、动态依赖、ELF LOAD 和最终 APK／AAB 对齐；实际环境验证与静态检查分开。
6. 版本、POM、Gradle metadata、sources、文档、NOTICE、对应 Rust 源码／Cargo.lock／WGSL／补丁／构建说明完整。

固定 Rust／NDK／wgpu／iced 及引用源码版本，使用 locked 构建。参考仓库声明的版本需实际验证，不把可读取 manifest 当工具链已经可用。消费者只下载预编译 Maven 产物。

建立独立受控消费环境，不使用 project、includeBuild、源码 checkout 或缓存中的本机 native 路径伪装独立消费。发布候选记录源码提交、配置、二进制 hash 和符号文件。

可以准备 Central 所需配置，但 namespace／签名／开发者／SCM 使用真实已有信息；缺少凭据时留下待办，继续本地发行验收，不编造信息、不上传。

## 8. 验证与证据纪律

原版视觉语义、本项目 Kotlin 基线、参考 Rust 实现分别保留身份，不把任何一方未经验证地当作另一方等价。对照固定字体、视口、颜色、配置、媒体时刻、素材和实际工作量。

不能通过降分辨率、删 blur／低alpha／字幕／语义、换字形、放宽断言、挑帧、配准或更新预期让结果看起来改善。确需迁移错误模型预期时，用独立来源证据和版本化记录说明，不能只用新实现生成自己的答案。

整帧、准备延迟、GPU、实际呈现、内存分别报告。当前宿主 FrameMetrics／Compose callback 不能自动覆盖独立 native surface。统计代码也有成本，正式对照保持一致。

可在本任务创建或已明确用于本任务的隔离模拟器／测试设备上，使用独立 application ID 做测试。保留用户现有播放器与数据，不覆盖、清空或中断用户正在使用的应用。缺设备时继续其他工作，并明确设备／性能未验，不能拿旧设备结果替代。

验证范围随代码影响选择；通过后不反复无意义重跑全部矩阵。失败保留最小复现和原因，再换有证据的方法。

## 9. Git 保存与持续执行

执行方式：**一个可验证功能 → 必要验证 → 检查 staged diff → 本地 commit → 下一项。**

已有合适分支继续；新分支用 `codex/`。提交只包含该功能及必要证据，不使用笼统 `git add .` 混入遗留改动。不要凭提交数判断完成，也不要把整个 renderer 塞进一个无法审核的提交。

不推送、不发布、不改远程、不 reset --hard、不 clean -fd、不擅自 amend／改写他人历史。不编造作者身份；身份缺失则保存工作、报告真实阻碍，继续不依赖提交身份的可做事项。

不提交用户歌曲、完整私有歌词、凭据、个人路径、缓存和构建产物。必要测试素材需有明确来源或为合成数据。参考依赖需要 vendor 时有意选择最小源码并保留来源，不能把整个参考应用工作树拷入本项目。

本任务的常规调查、可逆实现、构建、测试和本地提交已获授权，不逐阶段询问是否继续。一个测试或局部方案失败时，定位并记录后继续；有外部阻碍就推进其他独立里程碑，不重复同一无效尝试。

只维护一份当前执行清单，说明已完成、待验证、唯一下一项及完成条件。每个小功能简短报告实际行为、验证层级、提交哈希和下一项；不要求我回复。

不要停在设计、空白 native View、动态库加载、样例图形、局部 benchmark 或一次本地 Maven 发布成功。首个主题实际可用、可独立消费与验证记录完整才构成首轮交付。

如执行环境确实要求结束，先保存可直接恢复的具体状态，明确工作已停止和剩余目标；不宣称后台仍在运行，不自行建立自动化。上下文压缩后从清单与实际代码接续。

现在先核对项目和方案，简短说明第一项具体改动，然后直接开始 P0／P1，持续推进到云阶原生渲染与 Maven 的完整闭环。
