# Classic 核心移植对照

当前验收状态：数据／曲线／几何已完成原版对照；完整帧仍有正文和字幕栅格残差，
字幕主题阴影／光垫已获公共契约批准并接入，见最新章节。下文按日期保留调查过程，旧缺失项
若已有后续修复，以最新章节为准，不能把历史结论当作当前能力声明。

固定上游：Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5，AGPL-3.0-only 路线。
表中的上游路径以该提交仓库根目录为准，Kotlin 路径以 library/src/commonMain/kotlin/
com/mocharealm/accompanist/lyrics/folia/ 为准。当前表是修复清单，不是最终验收标准。

| 上游行为／源码位置 | 当前实现 | 缺失或简化 | 修复与验收 |
| --- | --- | --- | --- |
| utils/lyrics/cjkSemanticLayout.ts:buildPostLyricLayoutUnits、buildDisplayWordsFromLayoutUnits；utils/lyrics/wordSegmentation.ts | SemanticLayout.kt／DisplayUnits.kt；sticky 先作用于语义单元，保留源片段与有效时间 | ICU 与 Intl 词典版本可能不同；core缺少分词覆写字段 | 上游9组混合／空白／缩写／标点夹具通过；Arabic完整词独立状态并保护词内塑形 |
| utils/lyrics/graphemeTiming.ts:buildWordGraphemeTimings；components/visualizer/GlowWord.tsx:graphemeTimings | DisplayUnit 保留原片段、有效／源字素时间；完整塑形范围遮罩 | 连字分界处的覆盖仍需更多语言截图 | 固定源分组／时间夹具通过；继续复杂脚本验证 |
| classic/Visualizer.tsx:placementOf / calculatedMargin / lineConfig；glowWordOverlap.ts:getGlowWordReach / PairGap / Overflow | ClassicGeometry.kt：同源 reach／pairGap、seed 对齐和行距 | CJK固定字体几何已测；全帧仍未验收 | 160组几何夹具；八场景 default/avoid DOM 词距一致、矩形舍入范围一致 |
| useOverlapFit.ts:useOverlapFit | ClassicLine.kt:仅避让启用、min .5 / slack .98 / .005 收敛 | 完整帧像素仍未验收 | 380px窄屏长句CSS19.744px与浏览器对照通过 |
| GlowWord.tsx:状态选择；glowWordVariants.ts:layout/body variants | ClassicSampler.kt:sampleWord；完整 body 图层模糊 | Android <31现有Kotlin软件回退/API30运行；完整效果残差待收敛 | 原版 variants + Motion 调度器数值通过；spring/rest/opacity/filter/颜色混合已对齐 |
| wordGlow.ts:wordGlowVariants | GraphemeGlow.kt；ClassicLine.kt:字素范围遮罩 | 软件近似 blur 和原版 text-shadow 的像素差仍需校准 | 108个正常／fast／instant 样本通过；保护 shaping；同字体逐帧图像待最终验收 |
| glowWordTiming.ts:getGlowWordLineContainerMotion | LineMotion.kt；整句 opacity/blur 图层 | 仍需两端设备和连续浏览器响应对照 | 120个原版调度器入场／退出组合数值通过；平台图层预留出界空间 |
| classic/Visualizer.tsx:lyricContainerFloat | LineMotion.kt：原版7s y/scale关键帧、easeInOut | Android实际运行仍需验证 | 两周期真实浏览器DOM轨迹通过；媒体时间锚定是平台适配 |

## 必要平台适配与未完成能力

- 原版 Framer 由状态改变后的墙钟驱动；组件约定要求暂停冻结、seek 可重建。
  Kotlin 将相同曲线锚定媒体时间，这是时间源适配，不允许据此忽略曲线差异。
- Compose 必须保留完整塑形运行；不能逐 UTF-16 字符重新绘制正文。
  内部字素范围仍需保留计时并驱动遮罩。跨字素连字／上下文连接处允许记录栅格差异，
  不能将缺少内部计时改称 shaping 必须牺牲的功能。
- lyrics-core 没有上游的 wordSegments/autoWordSegments、isChorus、wordColors 等扩展字段。
  不伪造来源语义；基础显示词由已发布模型的 syllables 映射。额外合唱／主题不作为已支持能力。
- 当前系统字体截图不是跨端对照证据；需要同一可再分发字体文件、SHA、尺寸和时间表。

## 执行状态

第一项数据层：已实现源 syllable、显示分组及字素时间的分离；内部字素计时现已驱动完整shaping范围遮罩。
实际执行固定源函数生成 graphemes.tsv/grouping.json。Kotlin 对照普通词、非均匀 hurricane、
缩写 It’s、组合字符／ZWJ、世界。得到一致的有效显示字素时间（微秒四舍五入）。
收缩词按上游均分有效显示时间，同时保留 sourceGraphemes 原时间；跨来源的组合字符
采用覆盖区间，避免拆塑形。Arabic 原片段与内部时间在整体塑形运行中保留。
Arabic完整词现可独立变换，词内保持shaping；跨连字覆盖仍需继续画面对照。

固定源代码在忽略的 .work/folia-major 检出，不构建或修改 Accompanist。
数值夹具必须由固定源函数执行生成并注明生成入口；浏览器须使用真实上游 GlowWord，
不能以重新实现的 HTML 近似替代后宣称原版对照。尚无完成 classic 最小闭环的结论。

## 字素辉光验证（2026-11-15）

GraphemeGlow.kt 与固定源 wordGlowVariants + 实际 Framer keyframes 生成的 glow.tsv
108 个样本对照，三种 reveal、单字／多字、延迟／峰值／passed 采样误差 < 0.00002。
曾发现 24 次高精度 Bézier 求解与上游 12 次近似存在约 0.00005 差异；已按实际运行时
求解条件修复，未放宽阈值。ClassicLine 从完整塑形结果的 selection path 切分遮罩后
再模糊，不逐字符重新塑形；移除临时 0.35/0.5 光晕强度。跨连字的裁剪仍需视觉核对。

浏览器：verification/classic-browser/serve.mjs + capture.mjs；Chromium 151.0.7922.34，
禁用 WAAPI，使用 Framer 原生 JS fallback 与虚拟时钟统一，10ms 媒体步进；源码未改。
Kotlin：ClassicParityTest，同一 Noto Bold、800×600 density1、48px 字号、原创新合成文本。
输出 build/verification/classic-{upstream,kotlin}/，default/avoid 各21检查点。
1500ms 截图已目视检查：Kotlin 仍居中且词距过宽，浏览器为 seed-start；因此仅辉光
数值曲线与接入完成，排版、整体过渡、模糊像素等价未通过。Android 此项仅构建验证。

## 排版几何验证（2026-11-15）

ClassicGeometry.kt 已替代 safeScale 和固定 gap；词整体不折行，使用上游 normal seed
对齐、双侧动态边距及九个 passed 转角采样。avoidOverlap 才应用 reach 行间距、上下
padding 和 fit；默认不因高度自动缩小。直接执行上游函数生成 geometry.tsv 的160组
reach/overflow/RTL pairGap；Kotlin 误差 < 0.0001。

固定字体 DOM 对照 browser-layout.tsv：两句 default/avoid 的全部词距分别为
66.5/33.3、57.5/24.9、97.6/33.3、85.9/24.9px，与 Kotlin 一致。
位置／宽高与 DOM offset*（整数舍入）差 < 0.51px，避让 padding 和 rowGap 一致。
测量时发现 maxIntrinsicWidth 向整数取整造成边距偏差，已改为 fractional caret advance。
图像1500ms位置接近，正文基线仍约有数像素差，需继续修复字体行高和过渡；
尚未将窄屏多行／复杂脚本或整帧像素等价列为通过。

## 正文与整句转场验证（2026-11-15）

原版 variants 加实际 animateMotionValue 调度器生成 motion.tsv / line-motion.tsv，
42个词状态与120个整句入退组合（normal/fast/instant）对照误差 < 0.0001。
词 active opacity、passed 布局和未指定 ease 的 filter/normal exit 使用调度器默认
 easeOut；显式颜色 easeInOut 保留。正常入场 scale 550/30 spring 与 opacity/filter
默认曲线不同，已分离。完整正文与整句分别应用 blur，不将辉光 blur 代替正文 blur。
图层由 rememberGraphicsLayer 管理释放，采用显式留白录制保留越界内容。
一次 LayerOutsets 尝试触发空白回归，被连续帧检查检出；已替换并恢复随机 seek
逐像素重建检查，未改动原测试阈值。固定字体截图增加活动态非空检查，防止只导出空图。

CSS 允许负 leading，Compose 保留自然字高；已居中自然 paragraph 校正正文与 glow
共同基线。浏览器等待提示在隔离壳中返回空文字，避免把界面提示混入核心像素统计。
对照图 build/verification/classic-comparison/{default,avoid}-contact.png，CSV统计前景
并集而非大片背景。当前默认检查点前景 RGB 平均绝对误差约2.9..17.5/255；
尚无像素等价结论，接下来继续校准光晕、事件/RAF时间相位和更多文字/窄屏场景。
Android 此项构建通过；API31以下 RenderEffect 连续模糊尚未补齐，不能视为通过。

## 分组、RTL 与窄屏复核（2026-11-15）

发现并修复 sticky 在语义展开前后顺序错误：保留 layout unit 的 semantic/sticky 直到
最后转换，只有 sticky 且 nonsemantic 才合并。安全塑形合并保留已生成的有效字素包络，
不再以源时间覆盖；源时间仍独立保存。Arabic 完整单词可独立变换，词内片段不拆塑形。
数字／标点跳过后按第一个字母决定方向，补充平面 Adlam 也与原版一致。

六组实际浏览器场景（scenes.tsv）均有同字体 Kotlin 截图，default/avoid 共156帧每侧。
Noto Arabic 许可／提交／SHA 见 assets/README.md。380px 的避让长句从36px缩到
CSS19.744px；原版 canvas与字形缓存实际按19.74px测量/绘制，行盒仍用CSS字号。
布局计算补1/64px行高和整数offsetHeight fit，修复636.52与636.4375恰跨舍入边界
导致的字号及0.1px词距误差；阈值未放宽。RTL整词宽度改用完整selection path，
不以端点caret之差忽略bidi空白。inspect-layout.mjs仅观察原版测量，不改其返回值。
六场景全部margin误差 <0.0001，DOM整数框误差 <0.51px；截图已检查Arabic1500ms。
连字/Arabic辉光切片、fast/instant与窄屏图像仍需最终逐帧校准，未宣称整帧等价。

## 阴影合成顺序复核（2026-11-15）

GlowWord.tsx 的字素 opacity 包住 wordGlow.ts 返回的两个 text-shadow；外层词 opacity
又包住正文和全部辉光。修复先前逐阴影／逐绘制分别乘透明度的错误：准备时先合成双阴影，
运行时以字素 opacity 绘制一次；正文与所有字素再进入词图层统一应用词 opacity。
这些是合成顺序修复，没有用改变辉光曲线补偿亮度。

capture-halo.mjs 加载原版 GlowWord，隔离 identity pose、隐藏正文、强制字素 opacity，
同 Noto Bold 64px／320×320／density1。复用浏览器原始字形覆盖作为 Kotlin 输入，
从而单独检查模糊和合成。夹具及条件保存在 verification/classic-fixtures/halo。
以前景并集 alpha MAE（0..255）计：opacity .5 从1.66525降至0.85470，
opacity .1 从0.94985降至0.54151；opacity1仍为1.14125，最大误差10。
软件三次盒式模糊与 Chromium 字形阴影核仍有残差，不能据此宣布像素等价。
连续帧／随机 seek 重建及固定字体核心截图检查通过；Android仍需实际运行验证。

## 呼吸与浏览器帧相位（2026-11-15）

capture-breathing.mjs 在 React 挂载前冻结浏览器时钟，运行原版 classic 两个7秒周期，
观察外层实际 DOM transform；index.html 的RAF包装仅记录回调时间，不改变调度。
使用实际末次RAF减挂载时刻，不能把截图请求的墙钟当作已经绘制的时间。例如3500ms
请求的末次RAF为3488ms；未校正便会把浏览器帧采样差误判为曲线差。
breathing.tsv 共25检查点，Kotlin y误差 <0.0001px、scale误差 <0.000005。
没有拟合或修改既有关键帧。挂载时间变化会使捕获失败，禁止采纳中途重载后的轨迹。
原版挂载墙钟相位替换为可重建媒体相位，暂停不走时、seek直接定位是既定平台适配。

## 公开入口舞台复核（2026-11-15）

classic/Visualizer.tsx 的70vh、p-8、max-w-6xl和独立字幕层 → FoliaLyricsView.kt。
修复入口24dp缩进、按内缩宽度计算6vw和字幕Column推移主句的问题；主句与退场句各自
居中于70%高舞台，边距32dp，宽度上限1152dp，避让预算为舞台高度减64dp。
未启用避让时允许原版长行自然溢出，不因Compose父约束偷偷缩高。
字幕移为底部独立层；字幕自身的完整上游曲线／排版仍未验收。

PublicClassicParityTest 在800和380宽、600高、同Noto Bold、normal/avoid两种模式的
750/1000/1500/1900/2100ms，公开入口与已对照浏览器的内部renderer逐像素相同。
字幕开关不改变主句区域像素，且字幕自身确实可见。截图在build/verification/classic-public。
这证明公开入口使用已验证的核心；不把内部renderer尚有的浏览器栅格残差消除或隐瞒。
连续帧、随机seek、命中源行、状态与资源预算回归通过，Android/Desktop构建通过。

## 固定中文字体对照（2026-11-15）

新增OFL Noto Sans CJK SC Bold的可重建命名子集（提交／SHA／许可见assets/README.md），
两侧同文件、600×600、36px、合成文本“合成歌词光影流动”，四组非均匀词／内部字素计时。
七场景矩阵default/avoid的DOM词距误差 <0.0001，整数框误差 <0.51px；中文两侧各26帧，
包含入场、内部字素活动、余韵与退场。已目视检查1500ms避让图，位置与词组一致，
浏览器正文栅格更软；光晕仍有已记录的核差。未将平台栅格残差或整帧误差列为通过。

## 正文隔离与静态字体元数据（2026-11-15）

capture-raster.mjs 使用原始 GlowWord 正文、identity/rotate-scale 两种姿态，隔离辉光、
计时和颜色；BodyRasterTest 使用相同文件、字号、视口和词。原始静态 Noto Bold 的
STAT 仍含100..900等整族取值，Windows Skia0.150.1解析为100/2，OS/2却为700/5。
请求Bold导致额外加粗：48px Amber透明覆盖面积原浏览器2581.71px²，Kotlin3062.68px²。
关闭fontSynthesis不解决；移除STAT后同700请求为2499.21px²，native元数据恢复700/5。

保留原始字体，新建OFL改名的FoliaParityLatin-Bold.ttf，prepare-latin.py可复现并逐表验证
字形／hinting／字距／shaping未变。原始浏览器GlowWord前后逐像素相同；两侧共同使用
派生字体，未降低字重请求或改动画预期。此为验证字体的元数据兼容修复，不在组件中
偷偷更改宿主字体。仍有约0.71px基线、hinting和变换采样差异，下一项继续核对。
证据：build/verification/raster-{upstream,kotlin}；手动整帧对照脚本新增--manual，
旧RAF证据保留。590行实际挂载状态仍使用不变的严格比较。

正文基线进一步修复：capture-raster.mjs在原版正文放置无尺寸inline基线探针，
取得八组Latin/Arabic/CJK、19.74/36/48/72px实际CSS基线。自然字体ascent/descent
分别取整、半leading向下取整，对照八组误差 <0.0001px；没有硬编码某字体的偏移。
CssTextMetrics.kt → ClassicLine.kt统一移动完整shaping正文及其字素遮罩。
48px identity前景alpha MAE从35.60566降至9.23550，rotate(-4°)/scale1.6从20.04913
降至12.08475。剩余hinting／变换采样差异保持未验收。BodyRasterTest、完整JVM、
Android APK和Desktop classes通过；此提交尚未作为Android实际运行通过。

## 实际挂载 Motion 状态复核（2026-11-15）

capture-manual.mjs 运行固定版本原始 Classic／GlowWord／variants，冻结墙钟，
由 Motion 手动调度器推进媒体时间，关闭 WAAPI 以观察 JS runtime。读取实际 Motion
values，避免 DOM matrix 六位有效数字造成假误差；严格 passed 边界追加 .001ms，
仍按原运行时规则舍入 elapsed 毫秒。两句基线与八场景、default/avoid 共590行词状态。
BrowserClassicMotionTest 对矩阵、透明度、正文模糊、句变换和字素透明度比较 <0.0001；
原 CSS 整数 RGB 与线性颜色响应比较 <0.51，没有放宽原阈值。

实际挂载对照发现三项差异并修复：Motion 以50ms探测解析弹簧 duration，运行时在
该 duration 后钳制最终 keyframe（不是连续首次 rest）；passed 不重启目标不变的
x/y spring；字素时间保留原始 Double 秒数，先舍入运行时 elapsed 毫秒，再减分数字素
delay，避免 Arabic 的600ms/7字素和 fast 的900.8ms边界误差。保留 Long 微秒接口，
渲染另传精确内部时间，不丢弃 shaping 内部计时，也不使用累积播放状态。

旧 generate-motion.mjs 的 autoplay:false sample 绕过 running completion clamp；
真实挂载对照先暴露问题后，生成器改为 play 后采样并保留同目标动画，motion.tsv约20行
随原运行时行为更新。独立实际浏览器夹具与原 dispatcher 夹具现在均通过。
完整 JVM 测试、Android assembleDebug、Desktop classes 通过。截图和诊断位于
build/verification/classic-manual-upstream；数值通过不代表完整画面通过，下一步检查栅格。

## 字幕内容与空档历史（2026-11-15）

原版 VisualizerSubtitleOverlay.tsx:resolveVisualizerSubtitleOverlayContent 与
runtime.ts:getRecentCompletedLine/getSubtitleNextLines直接在浏览器生成180组夹具，
包括句前／句中／空档／末句后、译文／注音／双行／两条预览、隐藏、纯标点／符号占位、
补充平面字母和Number类别。SubtitleContentTest随机顺序seek，逐条比较活动句、最近
完成句、行顺序／文字／去重、预览索引，全部一致。字幕保留于空档，next以字幕所留句
为基准；先取原版接下来两条再过滤占位，不能继续向后补足两条。

公开入口接入这些选择规则；SubtitlePublicTest实际运行并截图，检查空档保留、占位
不显示、倒退seek不残留。两端构建及公开入口核心布局回归通过。截图位于
build/verification/subtitle-public。字幕转场、字号与多行排版尚未验收，下一项继续。
API适配：保留既有showTranslation/showPhonetic/showNextLine三个开关，映射到对应
行源；全部关闭不显示，showNextLine关闭也禁止缺失字幕时自动预览，保持既有契约。
不加入上游播放器设置／背景引擎。原版底栏hook在隔离浏览器仅替换为默认112px宿主值。

## CSS空白与内部映射（2026-11-15）

GlowWord.tsx的块级正文／whitespace-nowrap → CssWordText.kt和ClassicLine.kt。
源文本和全部字素时间保持原样；仅排版文本折叠ASCII空白并去除块首尾普通空白，
通过源UTF-16边界映射取得完整shaping结果的覆盖。被折叠空白不画空遮罩，
但仍参与上游原始字素总数／延迟，不能令其后的文字提前发光。NBSP及组合序列保留。
原Canvas词距测量仍保留空白个数（ASCII控制空白一对一转空格），与CSS块宽分开。

新增空白场景实际原版DOM发现inline block使用宽度向上取1/64 CSS px：
“soft light”Canvas宽217.48795，CSS宽217.5，offsetWidth218。初次严格几何测试失败，
修复使用宽度量化后通过，未放宽0.51px阈值。inspect-whitespace.mjs记录原测量。
八场景default/avoid的词距 <0.0001、整数框 <0.51px；固定字体、公开入口、
连续帧／随机seek回归通过；Android/Desktop构建通过。完整栅格仍未验收。

## 字幕动画与分数排版（2026-11-15）

原版VisualizerSubtitleOverlay.tsx、subtitleFontSizes.ts、subtitleTrackStyle.ts →
SubtitleMotion.kt / ClassicSubtitles.kt。真实挂载Overlay四种组合（800/380宽、
译文/三来源去重双行），每侧84帧；subtitle-motion.tsv对outer .24s easeOut、
inner opacity .3s与y spring500/25的误差<.0001。两侧同一固定OFL字体，600高。
媒体时间锚定是暂停/seek约定的适配；设置开关墙钟exit尚未完整对照。

响应式字号、.88次行、.82透明度、.25em间距、长译文换行、预览单行省略/blur1
均接入公开入口。1500/2500/3500/5500ms行顶/行高/末行基线严格误差<.02px。
平台公开font resolver取得原始ascent/descent，避免18px取整paragraph反推错1px；
次行先量化字号至1/64px再计算相对行高，修复窄屏两行累计.03125px差，阈值未改。
预览inline-block的外围strut descent由字体度量计算，非固定字体偏移常数。

build/verification/subtitle-{upstream,kotlin,comparison}保存PNG、DOM/Motion JSON、
几何诊断和前景误差CSV。已目视检查宽屏译文及窄屏双行；稳定宽屏译文MAE约7.7/255，
双行约17.3/255，3500ms预览MAE从33.7降至2.15。几何通过不代表这些像素残差已接受。
原版text-shadow、可选背景光垫、低API preview blur仍未完成，不改称平台必须差异。
完整JVM、Android APK、Desktop classes通过；最新APK实际安装于API34 probe34模拟器，
暂停拖动至0:02，译文/注音/模糊预览显示，无AndroidRuntime异常；.work/folia-subtitle.png。
这只是Android运行检查，非固定字体跨端效果验收；实机与API31以下仍未验证。

## 辉光核与阴影覆盖分离（2026-11-15）

capture-halo.mjs现在保留普通foreground source.png并另存原版字素的.01px阴影探针、
20/40px独立阴影。普通正文alpha积分1531.75px²；正半径但尚未扩散的阴影1451.43px²，
20/40px阴影分别1450.89/1449.53px²。原正文输入混入了Chromium字形覆盖差异，不能
用它推断模糊内核。探针不改变原始双阴影/opacity预期图，所有原件保持不变。

GlowMask.kt独立实现CSS/SVG三盒近似：sigma=CSS半径/2、宽度由sigma计算、
偶宽两次相反半像素中心、每轴三个累积求和后才量化；保留密度引出的分数sigma。
旧实现固定2*sigma+1并每次整数截断，会丢失覆盖且核宽不符。
以匹配的shadow-source为输入，20px单阴影与原版逐像素一致；40px前景MAE .00046/255、
最大1；原双阴影满亮MAE .00046/255、最大1（旧核/旧输入为1.14125/255，不能混同
这两个输入条件）。另确认原版整数source-over为a+b*(256-a)/256向下取整；
以前数学四舍五入的.51断言测试的是自定量化规则，现用实际原版单/双阴影严格<.01
和max1验证，并保留数学合成误差一量化单位检查，非放宽画面验收。

.5/.1透明度的数学round采样仍有.504/.419前景MAE、最大1；未将其冒称为实际
Compose saveLayer栅格等价。完整帧还混有正文/阴影平台字形覆盖与变换采样残差，
需要继续对照。软件遮罩临时内存变动使旧准备耗时/分配基线不再代表当前版本；
本项优先修复效果，未宣称性能改善。Android <31连续模糊仍是缺失功能。

## 低API连续模糊回退（2026-11-15）

ClassicLayer.kt统一词组opacity、正文/整句blur和预览blur。API31以上/JVM保留原生图层；
低API将相同完整paragraph绘制命令录制到软件Canvas，按预乘RGBA作可重建Gaussian卷积，
透明外部不带入RGB。SoftwareBlur.kt不依赖Rust、RenderScript或新增native构建。
crop仅移除透明留白并保留3sigma支持；opacity在模糊之后作用于整个词/句，
不逐UTF-16绘字。离开Composition清除持有图像引用，原生层由Compose管理释放。

SoftwareBlurTest用半透明红色脉冲/透明蓝色外部验证解析中心alpha与无颜色污染。
ClassicParityTest新增真实软件绘制路径，共42帧，沿用所有原DOM布局断言；退场后
逆向seek回1500ms及再次暂停捕获逐像素一致。SubtitleParityTest另测380px双行/预览，
原版几何阈值仍为.02px。完整JVM及Android/Desktop构建通过。

证据：build/verification/classic-software-{kotlin,comparison}；same原版手动Motion，
800x600/固定字体，7检查点/两模式的整帧前景RGB MAE .34..4.31/255；
未将该误差作为最终通过阈值。软件帧的正文与原版较接近，提示原生图层可能在词变换
后重新栅格化；需要后续隔离验证，不能据此宣布平台必然差异或直接切换全部后端。
软件Gaussian是3sigma有限核；与原生GPU滤镜在边缘仍可能有差异。

官方API30 default x86_64 r11镜像（SHA1 5e4de3946d46f88856c35efcc4d797b381456347）
新建本轮专用read-only classic30模拟器，800x600/density160/WHPX/SwiftShader。
实际SDK=30，APK安装冷启、paused seek到0:02显示主句/译文/注音/模糊预览，
无AndroidRuntime异常；两次应用区域PNG逐像素相同（排除24px系统状态栏）。
证据.work/folia-api30{,-b}.png。默认系统字体，此为低API运行验证，非固定字体Android
画面对照；API21..29/实机/全帧耗时与峰值内存未测，不能宣称流畅或高性能。

## 动态词正文的栅格层（2026-11-15）

原版GlowWord正文和字素光晕属于独立composited层，词transform作用于层覆盖。
ClassicLine.kt现在通过ParagraphRaster.kt在最终布局尺寸下栅格化完整paragraph，
颜色变化只tint原覆盖，再与字素辉光组成同一词层；字素计时/选择path/布局不变。
使用自然paragraph高度留足overhang，字号/视口变化重新准备。此为绘制行为校准，
不是以性能为由忽略上游。没有逐字重塑形或改变曲线。

同一实际挂载Motion/字体/800x600的7检查点对照：原生路径活动态前景RGB MAE
1000ms 7.04→3.94，1500ms 7.67→4.20，2100ms 7.64→4.31；avoid亦改善。
整句退出2300/4400ms残差仍约4.05/8.11，尚未验收。已检查default对照图。
静态隔离BodyRasterTest保留原direct font诊断，另存identity-raster/transform-raster，
不覆盖旧基线证据：静态transform可能触发浏览器不同raster scale，不能与动态生命周期
混同；该诊断的缓存变体误差更高，仍需单独说明/调查，非全部姿态逐像素等价。

已进一步读当前Compose 1.12.0源码和AOSP RenderEffect.cpp：原生BlurEffect参数是
radius，内部转换sigma=.57735*radius+.5；CSS blur参数直接是sigma。
目前生产代码仍直接传入，造成整句退出模糊偏小，这是下一项明确修复任务。
完整JVM与Android/Desktop构建通过，随机seek/暂停重建保持原严格像素断言。

## CSS sigma与平台radius映射（2026-11-15）

ClassicLayer.kt现在在平台边界逆变换radius=(sigma-.5)/.57735。依据是当前Compose
1.12.0 SkiaBackedRenderEffect.skiko.kt与AOSP Android14 libs/hwui/jni/RenderEffect.cpp
/ utils/Blur.h；两端均将radius转为sigma。未改原版Motion的filter采样值。
0<sigma<=.5不能由该原生API表示，使用精确软件路径，并把软件DrawScope传给所有
嵌套词/正文层，避免Android将硬件RenderNode回放到软件Canvas。大sigma软件RGBA核
改为已核对的三盒覆盖，仍正确预乘颜色；小sigma保留有限Gaussian。

capture-blur.mjs保存CSS blur原语的真实8bit透明PNG及矩，NativeBlurParityTest读取
同一参考；10/20 sigma的原生与软件alpha两侧均逐像素一致，包含36,864像素/帧。
小sigma二阶矩误差约.00..02px，未宣称像素相同。曾以无限精度Gaussian方差作为
新测试预期，20px在8bit裁掉尾部后实际stddev21.189而非22.028；现在直接读原浏览器
量化覆盖，而不是调整生产sigma补偿测试。原生和软件10/20严格PNG断言保留。

原固定classic帧：750ms MAE2.61→.835，2300ms 4.05→.975，4400ms8.11→1.25。
活动态仍约3.9..4.7；build/verification/classic-manual-comparison保存图像/CSV，
软件路径同源对照亦保留。字幕预览也使用正确单位。完整JVM、Android/Desktop构建通过；
最新APK在API34与API30实际播放一轮，无AndroidRuntime异常。未测实机或帧耗时。

## 补充平面粘连与Number类别（2026-11-15）

cjkSemanticLayout.ts:TRAILING_WORD_CHAR_REGEX/canAttachToPrevious → SemanticLayout.kt。
旧Char.isLetterOrDigit只看末个UTF-16单元并排除Nl/No，漏掉𞤀与𐄇、Ⅷ后的缩写/标点。
现与原版一致按完整Unicode L/N正则判断；不改变分组后的内部有效/源字素时间。
直接执行固定源生成五个新场景，旧实现先失败，修复后16条新增字素结果全部匹配，
包括补充平面、字母数字、其他数字、组合附标结尾与ZWJ表情不误粘连。
完整JVM和两端构建通过。此项是数据/时间分组对照，不冒称新脚本字体画面验收。

## 字幕实际字形基线（2026-11-15）

原版 VisualizerSubtitleOverlay.tsx 的分数行高／基线 → ClassicSubtitles.kt。
此前 DOM 几何误差小于 .02px，但双行首行实际 ink 比原版低 1px；SkParagraph
reported baseline=22.64，实际 blob baseline=23。Skia TextLine.cpp 的
floor(baseline + baselineShift + .5) 解释此差异。平台适配读取实际绘制基线，
逐行裁剪同一个完整 shaped paragraph 的覆盖，将它定位到原版分数基线和行距；
没有逐字重新塑形，也没有改动画或原版预期。

capture-subtitles.mjs 从默认原版 PNG 另外提取 stable frame ink 上下边界；
SubtitleParityTest 新增 800px 双行 1500ms 首行实际 y=434..448 严格断言。
原版 84 帧／侧、原几何断言继续通过，完整 JVM 测试、Android APK／Desktop classes
通过。双行 1500ms 前景 RGB MAE 17.2768→7.2436/255；单行仍7.7071。
目视宽屏双行与380px长译文对照，无漏字／行切割异常，仍有字体边缘覆盖残差。

独立 --grayscale 诊断关闭 Chromium LCD，保留默认规范截图不变。LCD只解释部分
误差，不据此宣布平台必须差异或修改验收标准。证据：
build/verification/subtitle-{comparison,grayscale-comparison}/，原版 ink 夹具
verification/classic-fixtures/subtitle-ink.tsv。API34/API30最新APK冷启与暂停拖动检查
另见 android-smoke.md；未将系统字体运行图冒充固定字体跨端验收。

## 可选字幕主题光垫与阴影（2026-11-15）

用户批准兼容扩展后接入 subtitleBackground（默认透明）和 showSubtitleBackground。
固定源 Overlay 的 padding/insets、渐变半径／停靠点、2px文字阴影均保留；40px光垫
模糊来自原 package-lock 固定的 Tailwind4.3.3 实际生成样式。初始验证页缺少这些
utility，光垫宽高为零；已由相同编译器生成 CSS，再从有效原版重新采集，未修改上游。

透明图层隐式裁剪曾切掉模糊边缘，改为显式预留160dp的整组图层；外层淡入作用于
光垫／正文整体，内层入场仅作用正文。位置统一使用原Canvas尺寸，避免录制图层
改变 DrawScope.size 导致偏移。窄屏换行扣除原版6px横向padding，预览保留父strut。
带光垫84帧/侧，默认/次行/长译文/预览及稳定几何<.02px断言通过。

另外隐藏原版正文、保留实际光垫得到四张独立参考，800/380px的Standard路径逐像素
最大通道残差2/255，联合前景MAE .449..459/255；固定PNG回归守卫记录此量化残差，
不是将其当作全帧通过标准。独立文字阴影MAE .555..602、max3/255，正文覆盖与
量化仍有差异。完整字幕图的误差统计包含光垫扩大后的前景区域，不能与旧纯文字
MAE直接比较。完整JVM及Android/Desktop构建通过；新可选效果Android运行待下一项。

证据：verification/classic-fixtures/subtitle-backdrop/（原创合成词／OFL字体），
build/verification/subtitle-backdrop-{upstream,kotlin,software-kotlin,pad-kotlin,comparison}/，
subtitle-shadow-kotlin/。显式TextStyle.shadow保留宿主原生语义；主题派生阴影独立
按CSS sigma绘制，未将平台Shadow半径差异带入默认实现。

## Android固定字体完整帧对照入口（2026-11-15）

新增debug-only ClassicParityActivity，直接使用公开FoliaLyricsView、同一scenes.tsv和
OFL字体；强制组件density/fontScale=1及物理视口，隐藏系统栏，无示例控件。主句
比较隐藏waiting文案／字幕，与浏览器隔离范围一致。字幕比较隐藏主句，仅translation。
Android资产通过AGP Variant API生成，仅debug构建引入；不修改发布边界或原工程。

capture-android.py以实际GPU frame-commit作为ready，并对自身am启动使用no-animation，
避免窗口启动动画冒充暂停漂移。没有修改设备动画设置；每个点另取第二张截图严格比对。
API34/API30各166帧全部暂停一致；各有字体／视口／系统版本与PNG SHA记录。API30使用
软件模糊，API34原生模糊；SwiftShader模拟器不是实机性能证据。

compare-android.py输出原版/Android完整帧和描述性误差，不返回视觉通过。首轮发现：
普通主句MAE约.38..6.99，但380px avoid长句最高76.42/255、800px长字幕约41.8，
这类差异不能笼统归为AA。已读Compose AndroidTextPaint/TextMotion.android源码：
Static启用hinting/关闭subpixel，Animated启用linear/subpixel。下一项实测此公开
配置是否修复累计字距／避让偏移，再决定结果，不以现有构建和截图数宣称验收。

证据：build/verification/classic-android-api{30,34}{,-comparison}/。
JVM八场景新compare-matrix.py亦输出真实manual Motion对照，正文/复杂脚本残差仍保留。

## Android连续文字计量（2026-11-15）

主句与字幕默认采用 TextMotion.Animated，保留宿主显式指定的模式。依据当前锁定
Compose Android TextMotion/AndroidTextPaint 源码：Static 使用hinting、关闭subpixel；
Animated 使用LINEAR_TEXT_FLAG和subpixel定位。Skiko两种模式均为空配置，JVM不变。
此项是平台计量适配；原版分组、曲线、字号、避让算法和参考PNG均未改动。

API30/API34各采集166个不同固定帧并额外逆序重访baseline的1500→750→1500ms，
每次暂停双截图及重访同时间SHA均严格一致。窄屏wrap/avoid的2800ms前景RGB MAE
从76.42184/76.37729降到16.95792/16.86117，1500ms从35.88008/35.16044降到
10.13100/9.89400；长字幕800px/5500ms从41.73518/41.79086降到12.29091/16.02542。
这是相同前景定义下的描述性误差，没有设置新的视觉通过阈值。

已目视原版/Static/Animated三列窄屏对照，累计字距显著改善，仍有基线/覆盖残差。
完整JVM测试、Android debug APK和Desktop classes构建通过；API21..29与实机未验证。
证据：build/verification/classic-android-api{30,34}-linear{,-comparison}/、
android-linear-wrap-detail.png。下一项定位实际绘制基线与剩余字幕/正文栅格差异。

## 正文实际blob基线（2026-11-15）

沿用字幕已核对的SkParagraph实际baseline规则，将ClassicLine的paintTop从reported
baseline改为paintedLineBaseline。完整paragraph正文与其内部字素遮罩共用同一偏移，
没有改变选择path、内部时间或字形shaping；Android本身baseline已为整数，行为不变。
BodyRasterTest同步采用同一实际绘制定位，原版CSS baseline预期保持不变。

同源原版1500ms：group/default前景MAE6.75942→4.14902，CJK/default
5.72279→3.69432，Arabic/default5.69436→5.39004；wrap/avoid2800ms
14.23862→12.39624。已目视group与CJK活动帧；没有断字或漏掉内部辉光。
并非所有帧的像素误差都下降：baseline/default1500ms约4.20→4.54，保留该残差，
不通过调整原版参考补偿。完整JVM与Android/Desktop构建通过，暂停/seek严格断言保留。
证据：classic-matrix-comparison/、classic-manual-comparison/、painted-body-baseline-detail.png。

Android临时Paint/Compose探针另确认：19.74px的Synthetic advance为92.96875，
同一字体原浏览器93.01488；传入未预先量化的19.744px仍为92.96875，因此先前
“重复量化导致19.73px”的解释不成立。已撤回无效的字号转换实验及全部临时日志。
保留实测小数计量差异，尚不能将其认定为平台必须差异或用固定比例更改字号。
下一项已定位：Android Layout 的center绘制起点取整数，调查字幕分数居中。

## 字幕分数居中（2026-11-15）

Android14 AOSP core/java/android/text/Layout.java:getLineStartPos/drawText以int extent
和位移实现ALIGN_CENTER，getLineLeft/getLineRight的center分支也分别floor/ceil。
ClassicSubtitles改为Start排版，逐行将完整paragraph的覆盖移到分数中心；Start在RTL
采用右边缘原点，避免opposite对齐同样存在的整数extent。仍绘制同一个完整塑形结果，
不拆字、不改变原行宽/换行或字幕内容，阴影和光垫沿用相同位置。

API30/API34各8个独立字幕检查点、暂停双截图通过。380px/5500ms的前景RGB MAE
19.77683/19.86642→10.58162/10.55967；800px/1500ms 14.14114/12.88052→
11.90250/10.03905。800px/5500ms API34 16.02542→10.27252，API30保持12.29091。
已目视原版/Android修复前后/JVM四列长译文，位置改善，字体边缘覆盖残差仍在。
完整JVM、Android debug及Desktop classes通过，原字幕几何/ink断言保持不变。

首次直接冷启字幕时严格暂停检查失败：API30整个背景受启动窗口合成影响，API34
字形部分像素蓝色相差1。失败PNG留在classic-android-api{30,34}-center/，未删。
脚本现在在0ms空场景确认连续两张全部像素等于背景色后再开始检查点，分离系统启动
窗口与媒体动画；每个实际检查点仍严格比较第二张截图，不改变设备动画设置。
证据：classic-android-api{30,34}-center-settled{,-comparison}/、
subtitle-fractional-center-detail.png。最终Start版本与全部主句的设备回归已完成：
API30/API34各166个不同帧和6次逆序seek重访，全部暂停双截图/重访像素严格一致；
conditions.json绑定实际APK SHA，证据位于classic-android-api{30,34}-current/。
