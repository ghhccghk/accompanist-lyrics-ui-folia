# Classic 核心移植对照

固定上游：Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5，AGPL-3.0-only 路线。
表中的上游路径以该提交仓库根目录为准，Kotlin 路径以 library/src/commonMain/kotlin/
com/mocharealm/accompanist/lyrics/folia/ 为准。当前表是修复清单，不是最终验收标准。

| 上游行为／源码位置 | 当前实现 | 缺失或简化 | 修复与验收 |
| --- | --- | --- | --- |
| utils/lyrics/cjkSemanticLayout.ts:buildPostLyricLayoutUnits、buildDisplayWordsFromLayoutUnits；utils/lyrics/wordSegmentation.ts | SemanticLayout.kt／DisplayUnits.kt；sticky 先作用于语义单元，保留源片段与有效时间 | ICU 与 Intl 词典版本可能不同；core缺少分词覆写字段 | 上游9组混合／空白／缩写／标点夹具通过；Arabic完整词独立状态并保护词内塑形 |
| utils/lyrics/graphemeTiming.ts:buildWordGraphemeTimings；components/visualizer/GlowWord.tsx:graphemeTimings | DisplayUnit 保留原片段、有效／源字素时间；完整塑形范围遮罩 | 连字分界处的覆盖仍需更多语言截图 | 固定源分组／时间夹具通过；继续复杂脚本验证 |
| classic/Visualizer.tsx:placementOf / calculatedMargin / lineConfig；glowWordOverlap.ts:getGlowWordReach / PairGap / Overflow | ClassicGeometry.kt：同源 reach／pairGap、seed 对齐和行距 | 窄屏多行及复杂脚本仍待同字体浏览器验证 | 160组几何夹具；两行 default/avoid DOM 词距一致、矩形舍入范围一致 |
| useOverlapFit.ts:useOverlapFit | ClassicLine.kt:仅避让启用、min .5 / slack .98 / .005 收敛 | 长句缩小的浏览器验收未完成 | 补窄／长／旋转场景 |
| GlowWord.tsx:状态选择；glowWordVariants.ts:layout/body variants | ClassicSampler.kt:sampleWord；完整 body 图层模糊 | Android <31 的 RenderEffect 支持仍受平台限制；设备验证待补 | 原版 variants + Motion 调度器数值通过；spring/rest/opacity/filter/颜色混合已对齐 |
| wordGlow.ts:wordGlowVariants | GraphemeGlow.kt；ClassicLine.kt:字素范围遮罩 | 软件近似 blur 和原版 text-shadow 的像素差仍需校准 | 108个正常／fast／instant 样本通过；保护 shaping；同字体逐帧图像待最终验收 |
| glowWordTiming.ts:getGlowWordLineContainerMotion | LineMotion.kt；整句 opacity/blur 图层 | 仍需两端设备和连续浏览器响应对照 | 120个原版调度器入场／退出组合数值通过；平台图层预留出界空间 |
| classic/Visualizer.tsx:lyricContainerFloat | LineMotion.kt：原版7s y/scale关键帧、easeInOut | 浏览器长时呼吸截图仍待补 | 媒体时间锚定是平台适配；不再使用正弦替代 |

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

第一项数据层：已实现源 syllable、显示分组及字素时间的分离；内部数据仍未全部用于绘制。
实际执行固定源函数生成 graphemes.tsv/grouping.json。Kotlin 对照普通词、非均匀 hurricane、
缩写 It’s、组合字符／ZWJ、世界。得到一致的有效显示字素时间（微秒四舍五入）。
收缩词按上游均分有效显示时间，同时保留 sourceGraphemes 原时间；跨来源的组合字符
采用覆盖区间，避免拆塑形。Arabic 原片段与内部时间在整体塑形运行中保留。
保护 shaping 的 Latin／上下文运行当前仍共用外层姿态，独立逻辑词变换尚待校准。

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
