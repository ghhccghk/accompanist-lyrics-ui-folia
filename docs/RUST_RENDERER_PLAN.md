# 可选 Rust 原生歌词渲染器实施方案

日期：2026-11-18。状态：设计方案，尚未实现或承诺性能收益。

本项目核对基线：`fdcc1e5`。本方案按用户最新要求，规划用 Rust 原生歌词控件替换示例中部分卡顿主题，并将实现交付为可通过 Maven 使用的可选库。这里的“native UI”指歌词绘制区域；选歌、内嵌歌词读取、播放、设置与页面导航继续使用已有示例能力。

本方案覆盖旧 `NATIVE_DESIGN.md` 中仅允许 CPU 计算实验的阶段限制。旧遮罩／颜色卷积实验仍可保留，不把它们当作已经实现本方案。原 Apache 工程继续只读。

## 1. 建议决策

采用“现有 Kotlin 库 + 可选 Android Rust GPU 渲染器 + 同一示例 UI 中按主题选择后端”的结构。先交付 Android，Windows/JVM 作为后续平台；不为尚未验证的平台发布空壳产物。

利用 Folia-rust-android 的 Android surface、独立渲染线程、JNI 和 wgpu 接入经验，抽取必要实现并记录来源。首个产品闭环优先云阶 Partita：当前项目已有软件模糊热点记录，参考 Rust 仓库也有同名主题，可做明确对照。镜台 Diorama 是后续 GPU 文字透视／分层的候选，不能声称参考仓库已有可直接拿来的镜台。

第一阶段优先复用本项目已有歌词语义、完整文字塑形与准备结果，将绘制、辉光和合成交给 Rust/GPU。这样能针对已知绘制热点，减少同时更换字体、布局、动画算法带来的变量。准备／计时本身若经测量成为瓶颈，再按主题将相应纯计算迁入 Rust。

成功标准是同等显示质量下的真实主题帧表现和稳定性改善，以及独立 Maven 消费可用；Rust 代码量、JNI 调用成功、示例 APK 能打包均不能代替这些标准。

## 2. 最新项目状态

- 当前八个主题均可选择：七个校准预览，Diorama 为用户后来授权的实验调试入口。此前“镜台不得开放”的阶段指令已被覆盖。
- 示例新增内嵌歌词读取和渲染／帧回调诊断；镜台增加会话磁盘缓存、纹理预算和 Windows sampler 修复。保留这些功能。
- 最新等待策略：Diorama、Fume、Cappella、Pendolo 保持自身连续文字舞台；Classic、Partita、Tilt、Cadenza 保留有限空档等待球。原生后端也遵循此策略。
- Rust 现状是 sample 专用 CPU 遮罩／颜色卷积，运行时默认 Kotlin；当前 Maven 库没有捆绑这些 native 实验。
- `scripts/verify-consumer.py` 已验证基础 Kotlin 产物的独立 JVM／Android 消费，需要扩展为“基础库不含 native”和“可选 native 产物可以独立使用”两套检查，不能删除前者。
- 帧诊断目前主要是宿主窗口／CPU 回调信息，不能直接当作新 native surface 的实际呈现帧率或 GPU 耗时。

本方案不把最新 Android 构建、旧 ARM64 计算探针或 Desktop 窗口结果扩大为新 Rust renderer 的验证。

## 3. Folia-rust-android 参考评估

检查来源：`Techuouo520/Folia-rust-android`，固定提交 `d7ff03e083d28069c973cf204b4a8f0a9d5c8594`。其 Cargo.lock 锁定的 Folia-rust Git 来源为 `39e33e8e89980289edcb01ad1fa9dde2b676ba9f`。上述提交来自本次实际读取，实施时固定版本，不跟随 main 漂移。

### 可参考的真实内容

| 内容 | 已检查的代码 | 对本项目的用途 |
| --- | --- | --- |
| Compose 嵌入 | `app/src/main/java/io/github/techuouo520/folia/MonetLyrics.kt` | AndroidView、TextureView、onRelease、可见性与宿主状态更新 |
| 独立线程与帧调度 | 同文件 `MonetView` | HandlerThread、线程所属 Choreographer、surface 代次和过时任务过滤 |
| Surface → Rust GPU | `crates/folia-bridge/src/monet.rs` | NativeWindow、wgpu surface/device、窗口重配、超时／丢失处理与 iced_wgpu 呈现 |
| Rust 主题算法 | `crates/folia-visualizer/src/styles/partita.rs` 等 | 云阶、流光、倾诉、心象的参考实现；需逐项与本项目语义核对 |
| GPU 辅助管线 | `crates/folia-render/src/blur.rs`、`scene.rs`、`glyphs.rs` | 模糊／场景／字形资源组织思路；不是本项目视觉等价证明 |
| Native 构建 | `app/build.gradle.kts` | Cargo 构建、strip、jniLibs 与绑定生成的输入／产物组织 |

### 必须认识到的差异

1. 这是 Android 应用工程，未发现现成 Maven AAR 发布配置。不能把 `libfolia_bridge.so` 复制进本项目就称库化完成。
2. 当前 Android 主接入是 Monet。`monet.rs` 构建 `Scene` 时硬指定 `Style::Monet`；`MonetPosterView.kt` 叠加 `NowPlayingLyricsTickerView`。它并非“全部歌词主题已通过 Rust 在 Android 运行”。
3. 注册的 Rust 主题为 Classic、Partita、Still、Tilt、Claddagh、Cadenza、Nebbia、Lumiere、Monet；没有 Fume、Cappella、Pendolo、Diorama。额外主题不自动进入本项目范围。
4. **同名不代表同效果。** Partita 调用 `glow::draw_word`，当前 `styles/glow.rs:182–208` 的 halo 是放大 1.06 倍的半透明文字叠画。这不能直接替代本项目的实际模糊／多层辉光，更不能用减少效果工作量得到的速度声称同质量提速。逐词绘制边界也需核对 Arabic、混合 bidi、连字和内部字素时钟。
5. `folia-bridge` 同时依赖播放器、资料库、在线服务、keyring、QuickJS、网络运行时等。需要新建渲染专用桥，不能整体引入该应用桥。
6. 参考工程配置为 minSdk 26、ARM64、wgpu 27.0.1，声明 Rust 1.99.1；这些是该提交的配置，不是本项目已经验证的工具链。不要因此整体升级现有 Kotlin／Compose／Gradle。
7. 参考仓库有 cryoglyph 的暗字覆盖率修补及额外第三方来源。不能不经画面对照就把颜色修补、字体或附带 Android 歌词控件一起搬入。

推荐：复用平台接入和必要 GPU 模块，保留一份同名 Rust 主题的隔离评估结果；产品首版优先绘制本项目的已验证场景计划。只有完整对照证实既有 Rust 主题适配更合适时，才选择完整 Rust 主题路径，不同时维护两套实验产品架构。

## 4. 结构与依赖边界

建议逐步增加以下内容，不在第一天生成所有空模块：

```text
library                                  # 现有 KMP Kotlin 库；无 native 运行时依赖
  FoliaLyricsView                         # 现有入口保持可用
  renderer bridge                        # 确有需要才增加，窄、实验性、显式版本

native-renderer-android                   # Android library / AAR
  FoliaNativeLyricsView                   # Android 可选入口；内部可回退 FoliaLyricsView
  native view + renderer session
  JNI + loader + source-object mapping
  packaged liblyrics_folia_renderer.so

native/renderer/                         # 与 experimental-mask 分开
  scene                                  # 自有场景数据／版本／校验；必要纯计算
  gpu                                    # 裁剪、纹理、辉光、合成、透视；wgpu/WGSL
  jni                                    # Android/JVM 句柄、错误、线程和 ABI

sample/shared                            # 复用选歌／播放／内嵌歌词／设置 UI
sample/android-app                       # 现有示例
sample/android-native-app                # 最薄的新入口，注入可选 renderer，复用 shared

native-renderer-jvm                      # 后续 JVM 适配 JAR
native-runtime-windows-x64               # 后续平台 DLL 资源 JAR
```

依赖方向：native 适配层依赖基础 Kotlin 库；基础库不依赖 native 产物。基础坐标的 POM／Gradle metadata 不引入 Rust、播放器或 sample。

优先使用新的 `FoliaNativeLyricsView` 包装入口，避免给已有公共函数直接新增参数而破坏已编译调用方。示例 shared 接受一个窄的歌词区域渲染提供者，由 Android native 入口注入，其他入口继续 Kotlin。

如果需要复用当前 `internal` 的标准化／PreparedScene，先抽取最小实验性 renderer bridge，通过明确的 public experimental 契约提供；不能跨模块硬引用 internal、反射或复制整套库。桥只暴露必要不可变场景和状态，不泄露 Compose/Skiko 原生指针。

## 5. 第一版的渲染路径

### 5.1 Kotlin 准备，Rust GPU 绘制

数据流：`lyrics-core 原对象 → 本项目标准化／完整 shaping → PreparedScene → Rust GPU → Android surface`。

PreparedScene 在内容、字体解析结果、尺寸、density／fontScale、布局配置发生实际变化时生成；配色／媒体时间变化使用较小更新。包含稳定 source ID、纹理资源、布局／裁剪、必要动画计划与样式数据。原始 Kotlin 来源对象留在 Kotlin 表中，Rust 只返回 ID 和当前 scene generation。

文字完整 shaping 后再建立可显示片段和内部进度映射；不逐 UTF-16 字符做独立字形，也不从 Rust 字符串索引直接推算 Kotlin UTF-16 边界。需要跨边界字符串时显式传 UTF-8 字节与映射，保留组合字符／ZWJ／RTL。

CPU 像素资源只在准备或缓存变动时批量上传。正常逐帧不传整首歌词、不读回整屏 Bitmap、不调用 native 后又在 Compose Canvas 重画整帧；这些往返会抵消 GPU 路径收益。

Rust 首先处理可见文字的变换、实际 blur／glow、裁剪和合成。GPU blur 的 kernel、半径定义、预乘 alpha、边缘采样和线性／sRGB 次序需要单独对照，不能把参考项目的简化 halo 当相同算法。

旧 CPU native mask／colour 开关只影响 Kotlin 路径；新 GPU 路径不得重复执行相同 CPU 卷积。诊断中分别显示 renderer 与可选 CPU processor。

### 5.2 按需进一步迁入 Rust

首轮 profiling 若证明卡顿主要来自主题 prepare、整曲相机预计算或 shaping，而非绘制，GPU surface 本身不能解决。此时只移交实际热点的纯计算，保留相同输入／数值／时间契约。

若使用参考仓库的完整 Rust Partita，需要先补齐辉光、字形分组、字幕、等待效果、来源命中和媒体重建语义，并通过同质量检查；未通过时仍作为参考，不替换默认主题。

## 6. Android surface、线程与时钟

### GPU 能力选择

先围绕参考锁定的 wgpu 27／iced 版本做最小构建和渲染验证，成功后锁版本，不直接升级 latest。Android 优先验证 Vulkan；GLES 只有在同一主题／格式／shader 管线真实验证后才列为 native 支持路径。Android 版本符合 minSdk 不代表设备一定满足选定 GPU 功能。

初始化查询 adapter、surface format、纹理尺寸、采样器数、混色／浮点 attachment 等实际能力；按主题声明所需能力。镜台需要的 F16 合成、mip 和多层特性不支持时回退 Kotlin，不能静默改低质量再标记为同一 renderer。诊断记录实际 backend、设备、driver 和关键能力。

### Surface 选择

首个接入原型优先沿用参考的 TextureView，便于验证现有 Compose 布局、圆角、字幕和菜单叠加。TextureView 需要在应用视图合成中呈现，有额外成本，不能仅因参考采用就认定最优。

在首个真实主题闭环中做一次同条件 TextureView／SurfaceView 验证，选择一种正式宿主。SurfaceView 必须检查 z-order、裁剪／缩放、页面过渡、窗口重建、截图采集和触摸；验证不通过不能用“独立 surface 更快”掩盖界面错误。两个控件不是逐帧动态切换项。

### 生命周期和实例

每个可见歌词实例拥有 renderer session；线程独占 GPU surface、交换链和渲染资源。参考实现的线程局部单 POSTER 不直接成为发布 API：至少支持多个独立组件实例，并有 generation 校验。

状态机：`Idle → Preparing → Ready → Suspended／Recreating → Released`，可恢复错误进入明确 Kotlin 回退路径。

- attach／resize／detach／destroy 顺序化；旧 generation 任务不能写回新窗口。
- TextureView surface 的 retain/release、SurfaceTexture 返回值和 worker 销毁顺序必须匹配；不可照搬一半生命周期。
- 前后台、不可见、暂停且无必要更新时停止无意义提交。尺寸为零不配置 swapchain。
- GPU lost／timeout 等可恢复情况有界处理，不在每帧无限重建。失效 scene 只回退一次，保留诊断。
- 返回 Kotlin 前恢复当前歌曲、配置与最新媒体位置，不 seek、不重建播放器；新路径首帧成功后再撤下旧画面，过渡期间双份资源要计入峰值预算。
- 正常卸载释放自己持有的线程、surface、native 引用和资源；GPU 使用中的资源延迟到完成点释放。

### 唯一媒体时钟

新 renderer 消费宿主已求值的媒体时刻。现有 `FoliaPlaybackSnapshot.positionMillis` 已含播放速度且没有 speed 字段，不能在 Rust 再乘速度或根据 isPlaying 自行当作 1x 外推。

渲染线程使用自己的 vsync 调度绘制，但不拥有第二条播放时钟。为了不依赖 Compose 每帧重组，sample 适配层需要线程安全的 frame source：从播放器的不可变状态／原有镜像规则求出给定单调时刻的最终媒体快照，再一次性传给 native。

当前 Compose lambda 或可变 RemotePlaybackMirror 不能未经同步从新线程直接读。必要时抽取共享的不可变时钟求值函数／原子状态邮箱，保持既有暂停、缓冲、外推上限、seek discontinuity 和速度语义。普通使用方若只给旧式 UI 快照，可采用更新时发布快照的兼容模式，不能偷偷生成预测时钟。

歌词 offset 只在同一边界减一次；点击通过 Kotlin 来源映射和宿主既有 seek 回调返回。乱序 seek、切主题、恢复窗口必须能重建，不用墙钟累积或固定显示帧步数模拟历史。

## 7. 字体、交互与后端选择

发布入口保留实际 lyrics-core 数据、字号／配色／字幕／减少动态和来源回调。

对于完整 Rust 文字路径，Compose FontFamily 不一定能自动转成 Rust 的相同字体。应支持有明确来源的字体字节／配置适配；无法匹配的字体类型报 capability 不满足并回退，不能静默改用另一个字体。CPU prepared texture 路径继续复用宿主字体，但必须观察异步字体解析完成后的失效。

点击／长按命中使用实际呈现的变换与可见几何；事件携带 scene generation、source ID 和帧标识，Kotlin 校验后返回原对象。空白穿透、拖动取消长按、字幕层与菜单覆盖需回归。

Canvas／TextureView 不会自动获得每行歌词的语义。提供基于可见行的原生 accessibility 节点或 Compose 语义覆盖，正确描述行、可执行点击与当前状态；减少动态、系统大字与字体方向都是首版要求。

建议用户配置分为：

- `Kotlin`：已有路径。
- `NativePreferred`：主题／平台／字体／配置支持且 renderer ready 后使用 native，否则明确报告 Kotlin 与回退原因。
- `NativeRequired`：调试／消费验收使用，失败明确暴露，防止测试实际在 Kotlin 上通过。

前期 sample 默认 Kotlin，允许按主题记忆实验选择。只有达到同质量、稳定性与实际性能门槛的主题才进入自动选择白名单；不依据“安装了 .so”自动替换八主题。

## 8. 主题优先顺序

| 顺序 | 主题／目标 | 依据与边界 |
| --- | --- | --- |
| 1 | Partita 云阶的 GPU 绘制闭环 | 旧真实设备采样记录过软件 blur 热点；先用最新版本复测。参考有同名 Rust 实现，但简化 halo 不能充当等价替换 |
| 2 | Diorama 镜台文字绘制 | 透视、多层、纹理、mip 和合成是 GPU 候选；参考仓库没有该主题，应复用本项目现有场景语义。先稳定文字 pass，完整相机残差仍独立修复 |
| 条件推进 | Fume 浮名 | 先区分整曲准备／相机计算与绘制。若主要耗在 CPU prepare，独立 surface 不会自动提速 |
| 后续择一 | Cappella／Pendolo／其他主题 | 根据真实 trace 选择；布局 presence、密集 tick 或算法错误先按各自原因修复，不统一归因 Kotlin |

每次只推进一个主题到“数据／绘制／交互／生命周期／性能／Maven”闭环，再扩大主题范围。

## 9. Maven 产物设计

group 保持 `top.ghhccghk.accompanist`。建议坐标如下，版本号以实际发布准备为准；这些是拟定产物，不是现已上传的依赖：

| artifactId | 产物 | 内容／阶段 |
| --- | --- | --- |
| `lyrics-ui-folia` 及已有平台变体 | 原 KMP 发布 | 保持 Kotlin 独立可用；不增加 native runtime 传递依赖 |
| `lyrics-ui-folia-native-android` | AAR | 可选 Compose/native View 适配、loader、JNI facade、consumer rules、预编译 .so；第一阶段 |
| `lyrics-ui-folia-native-jvm` | JAR | JVM wrapper／loader；Windows 接入阶段再发布 |
| `lyrics-ui-folia-native-windows-x64` | 平台资源 JAR | 经验证的 DLL 和依赖资源；同上 |

Android 消费者只增加 AAR 坐标，使用预编译 native 库，不安装 Rust、cargo-ndk、不执行本仓库脚本、不访问源码 checkout。保持 renderer 与基础库版本兼容矩阵；ABI、scene 协议和实际能力版本独立检查。

首个 renderer AAR 建议 minSdk 26，与参考的已用平台范围一致。基础库 minSdk 21 和原 sample minSdk 23 保持。使用新的薄 `sample:android-native-app`（minSdk 26、独立 application ID）复用 shared UI，避免静默抬高旧示例要求或破坏既有 APK 验证任务。

**运行时回退不能解决 manifest minSdk 冲突。** minSdk 21／23 的应用在 renderer AAR 仍要求 26 时不能直接依赖它；它们继续消费 Kotlin 库。若以后要降低 native 的 minSdk，单独验证所有依赖／加载 API 后发布，不能通过 overrideLibrary 绕过。

首个可交付 Android 组合为 ARM64；x86_64 用于可用模拟器验证，通过后纳入正式 AAR。如果只发布 ARM64，则明确 ABI 支持范围和 Kotlin 回退限制，不把“编出 x86_64”写成已验证。32 位不在首批。

ABI 也存在安装／分发阶段的限制：仅有 ARM64 native 产物时，不能承诺所有 32 位设备都能先安装 APK 再运行 Kotlin 回退。需要在最终 APK／AAB 的 ABI 选择和目标设备上验证；不支持的设备使用明确的 Kotlin-only 分发，不用运行时错误处理掩盖包兼容问题。

AAR 内用标准 `jni/<abi>/liblyrics_folia_renderer.so`，处理实际引入的其他动态依赖，核查冲突、ELF 依赖和 16KiB LOAD／最终 APK 打包对齐。专用库名避免与 sample 旧实验同名。使用现有支持的 NDK／AGP 作为第一候选，针对性验证参考代码所需版本；不全库追新。

JNI 类／方法的 R8 规则随 AAR 提供；必须测 minified release 消费者，不能只测 debug。不得依赖未经公开的 Android／Skiko 符号。

发布包提供 Kotlin／Java sources、API 文档、POM 元数据、NOTICE／来源表，以及对应 Rust 源码、Cargo.lock、WGSL、补丁与构建说明的版本化源归档。未授权字体／图标／其他应用组件不进入包；按实际引入的每个依赖保留声明，不因仓库总体许可相同省略来源核对。

本地 Maven 仓库验收是每个里程碑的一部分。若目标是 Maven Central，再单独配置已验证 namespace、签名、SCM／开发者元数据和所需文档产物；本方案不编造身份／凭据，也不自动上传。AAR 可构建、本地消费通过与 Central 已发布分别报告。

## 10. 独立消费与可重现构建

新增独立 native consumer 工程，通过坐标解析，禁止 project／includeBuild／源码路径替代。设置独立 Gradle user home 和受控 PATH，确认没有 Rust 工具链仍能构建消费 APK。

必须覆盖：

1. Kotlin-only consumer：不解析、不打包、不加载 renderer .so，原入口可用。
2. NativeRequired consumer：加载实际 AAR 内 .so、校验 ABI、显示指定主题；无 sample 包依赖，缺少符号／资源时真实失败。
3. NativePreferred consumer：不支持主题／字体／设备／后端时按契约回退，播放和原对象回调正确。
4. release + R8 consumer：实际安装／运行验证 JNI 与资源，不只检查 classes.jar。
5. 4KiB／16KiB 运行环境：除静态 ELF 和 ZIP 对齐外，在可用对应环境加载运行；不可用时明确未验证。
6. 多实例、旋转／resize、surface 重建、退出、切歌、重复启停、内存压力与错误注入。

生产者的 Rust、Cargo.lock、NDK、wgpu／iced 和引用源码版本锁定。Cargo 使用 locked 构建，CI 缓存键覆盖工具链、目标 ABI、编译配置与依赖锁。消费者不参与 native 编译。

只从验证通过的同一发布任务晋升二进制；保存 hash、源码提交、构建参数、符号文件和 source archive。不能在发布时重新拉 main 或临时下载未固定二进制。

## 11. 性能和视觉验收

### 先有当前基线

复测最新版 Kotlin，同设备、歌曲／授权合成输入、字体、尺寸、画质、分辨率、速度、温度区间与实际显示刷新率。记录准备首次可见时间、长歌词／密集过渡／随机 seek／尺寸变化，区分 CPU 准备、CPU 提交、GPU、上传、磁盘缓存和播放线程问题。

旧 Partita 软件 blur 采样只能作为选点依据，不能直接作为当前 Rust 的对照值。现有 frame callback 面板继续保留，但新 native surface 另加 frame ID、提交时间、可观测完成／呈现信息、原生资源计数和真实 backend。

Perfetto／系统工具的覆盖范围要按 Android 版本与 surface 类型核对。尤其独立 SurfaceView 的呈现不能只拿宿主 HWUI FrameMetrics 或 Compose callback FPS 代替；GPU timestamp 不可用时明确标为未知。诊断采样开销要控制，正式对照中条件一致。

### 建议门槛：实施前固定，不能看结果后放宽

- 语义：暂停、seek、offset、来源回调、可见／退出顺序与关键几何不能退化；保留原上游对照，不仅对当前 Kotlin 自比。
- 字形／光栅：完整 shaping、CJK／Arabic／emoji／bidi／字幕／大字号；GPU blur 和 gamma 用固定条件的前景、最差局部与边界检查。允许定义有证据的跨后端数值容差，但不能删除低 alpha 或拿不同显示质量比较。
- 性能：在确认卡顿的主场景中，建议 P95 整帧时间至少改善 20%，可可靠观测的超帧预算次数至少下降 30%，P99 无超过 5% 的退化。若指标在该平台不可观测，不用别的 CPU 指标冒名顶替；先补测量方法。原场景本已稳定达帧预算时，不按这个比例强行替换。
- 稳定性：至少三轮交替顺序配对，加持续播放／切歌／前后台／重建测试；验证没有增加 ANR、native crash、音频中断或资源持续增长。
- 内存：先规定测试设备的 CPU/native/GPU/缓存/瞬时双 renderer 预算，再测峰值与稳定态；报告两个播放相关进程合计 PSS。不可把 JVM heap 下降称作整体内存下降，也不能只统计活跃纹理而漏 staging／mip／交换链。

以上百分比是建议产品门槛，不是性能预测。未通过的 native 路径保持实验开关或停止产品替换，不无限扩展 Rust 重写来证明选型。

## 12. 安全和失败边界

FFI 使用固定宽度字段、长度、显式版本、checked arithmetic 和有限资源预算；native 句柄包含代次，禁止 use-after-free、重复 destroy 和旧 scene ID 复用。传入 direct buffer 时明确借用／复制／完成边界，不跨异步任务保存 JVM 临时指针。

用窄 JNI 生命周期／批量场景／帧入口即可，不为渲染器引入整套 UniFFI 业务桥；后续确有大量配置需求再评估。每帧至多必要的批量调用，主线程不等待 GPU readback。

Rust panic 不跨 FFI；catch_unwind 只覆盖可展开 panic，不能承诺接住 abort、OOM、越界导致的 native 崩溃或驱动故障。同进程 native fatal crash 无法由 Kotlin fallback 救回。安全边界主要依靠输入校验、生命周期、资源限制和真实压力测试，不能以“大 catch 后回退”代替。

不把渲染搬进现有私有播放进程。渲染器属于 UI 宿主的图形生命周期，播放器只提供时间和状态；不新增 IPC 逐帧像素传输。

## 13. 可提交里程碑

| 阶段 | 可评审交付 | 通过后再做什么 |
| --- | --- | --- |
| P0 范围与当前性能 | 固定来源、最小依赖图、Partita 当前 trace、显示质量与测量协议 | 证明要替换的具体工作；若热点不在绘制，调整该主题范围 |
| P1 可发布 native 骨架 | 新 AAR、最小独立 surface、ABI／句柄／销毁、自包含 .so、坐标 consumer | 只证明平台／Maven链路；此时不能称主题完成 |
| P2 真实文字一帧 | 真实 core → PreparedScene → Rust GPU 字体纹理、alpha／裁剪／颜色、来源 ID | 证明不是原图贴图冒充效果或仅背景加速 |
| P3 Partita 完整闭环 | 原语义、真实辉光、字幕／等待、交互／语义、暂停／seek／重建；sample后端选择 | 固定矩阵与性能对照通过才允许自动选择 |
| P4 Android 稳定与发行候选 | ARM64／可用x86_64、R8、16KiB、独立消费、资源压力、源归档 | 形成可发布候选，不自动上传 |
| P5 第二主题 | 优先镜台现有文字场景 GPU pass；按测量也可选其他明确热点 | 每主题重复同质量／交互／性能／发行验证 |
| P6 Windows/JVM 可选 | 原生子窗口或其他经证实的嵌入、JAR＋平台DLL、真实窗口/独立消费 | 通过才声明Windows支持；Linux/macOS后续单独验证 |

Windows 路线需要单独验证 AWT/Compose 混合层级、窗口句柄生命周期、DPI、输入与截图；不直接将 Android TextureView 设计映射成 Desktop 已可用。禁止借用现有 Skiko swapchain 或未经公开的 GPU 指针。必要时让 Windows 保持 Kotlin，不阻塞 Android AAR。

提交建议按上述阶段内部的小功能切分，例如 `feat: add optional native renderer Android artifact`、`feat: render prepared Partita text through native GPU`、`test: verify native AAR in independent release consumer`。每次实现、必要验证、检查 staged diff 后本地保存；不凭一次 probe 成功提交全部主题替换。

## 14. 本轮交付与实施注意

本轮仅完成源码调研和此方案，没有编译参考工程、没有实现 native renderer、没有验证它在本机／设备上的性能，也没有发布 Maven。

实施时保留原有 `sample/android-app/build.gradle.kts` 签名改动和未跟踪 `gradle/gradle-daemon-jvm.properties`，不顺手提交。若另一个执行会话新增了功能，先核对实际 HEAD 再接续。本方案新增一个设计文件，不覆盖已有证据／失败记录。

## 15. 官方资料与参考定位

资料核对日期为 2026-11-18；实现应使用选定版本的 API，不将文档 latest 自动作为升级指令。

- R1：参考 Android 仓库及固定提交 README：`https://github.com/Techuouo520/Folia-rust-android/tree/d7ff03e083d28069c973cf204b4a8f0a9d5c8594`。
- R2：该提交 `crates/folia-bridge/src/monet.rs`、`MonetLyrics.kt`、`MonetPosterView.kt`、主题 registry／glow／Partita、Cargo manifests／lock 和 Gradle 配置，本次均已实际读取。
- R3：Android graphics 架构，SurfaceView 与 TextureView：`https://source.android.com/docs/core/graphics/arch-tv`。
- R4：Compose 嵌入 Android View：`https://developer.android.com/jetpack/compose/migrate/interoperability-apis/views-in-compose`。
- R5：Android library／AAR：`https://developer.android.com/studio/projects/android-library`；发布配置：`https://developer.android.com/build/publish-library/configure-pub-variants`。
- R6：16KiB page size：`https://developer.android.com/guide/practices/page-sizes`。
- R7：wgpu 官方项目／API：`https://github.com/gfx-rs/wgpu`、`https://docs.rs/wgpu/27.0.1/wgpu/`。本轮资料也查看了 latest，但不据此要求升级参考的27版本。
- R8：Perfetto FrameTimeline：`https://perfetto.dev/docs/data-sources/frametimeline`；按实际采集版本确认独立 surface 覆盖范围。
- R9：KMP 发布：`https://kotlinlang.org/docs/multiplatform/multiplatform-publish-lib-setup.html`；Central 要求：`https://central.sonatype.org/publish/requirements/`。
- R10：后续 Desktop 句柄接入参考 JAWT：`https://docs.oracle.com/en/java/javase/21/docs/specs/AWT_Native_Interface.html`。
