# Classic 核心移植对照

固定上游：Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5，AGPL-3.0-only 路线。
表中的上游路径以该提交仓库根目录为准，Kotlin 路径以 library/src/commonMain/kotlin/
com/mocharealm/accompanist/lyrics/folia/ 为准。当前表是修复清单，不是最终验收标准。

| 上游行为／源码位置 | 当前实现 | 缺失或简化 | 修复与验收 |
| --- | --- | --- | --- |
| utils/lyrics/cjkSemanticLayout.ts:buildPostLyricLayoutUnits、buildDisplayWordsFromLayoutUnits；utils/lyrics/wordSegmentation.ts | SemanticLayout.kt／DisplayUnits.kt；sticky 先作用于语义单元，保留源片段与有效时间 | ICU 与 Intl 词典版本可能不同；core缺少分词覆写字段 | 上游9组混合／空白／缩写／标点夹具通过；Arabic完整词独立状态并保护词内塑形 |
| utils/lyrics/graphemeTiming.ts:buildWordGraphemeTimings；components/visualizer/GlowWord.tsx:graphemeTimings | DisplayUnit 保留原片段、有效／源字素时间；完整塑形范围遮罩 | 连字分界处的覆盖仍需更多语言截图 | 固定源分组／时间夹具通过；继续复杂脚本验证 |
| classic/Visualizer.tsx:placementOf / calculatedMargin / lineConfig；glowWordOverlap.ts:getGlowWordReach / PairGap / Overflow | ClassicGeometry.kt：同源 reach／pairGap、seed 对齐和行距 | CJK固定字体几何已测；全帧仍未验收 | 160组几何夹具；八场景 default/avoid DOM 词距一致、矩形舍入范围一致 |
| useOverlapFit.ts:useOverlapFit | ClassicLine.kt:仅避让启用、min .5 / slack .98 / .005 收敛 | 完整帧像素仍未验收 | 380px窄屏长句CSS19.744px与浏览器对照通过 |
| GlowWord.tsx:状态选择；glowWordVariants.ts:layout/body variants | ClassicSampler.kt:sampleWord；完整 body 图层模糊 | Android <31 的 RenderEffect 支持仍受平台限制；设备验证待补 | 原版 variants + Motion 调度器数值通过；spring/rest/opacity/filter/颜色混合已对齐 |
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
