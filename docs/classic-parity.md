# Classic 核心移植对照

固定上游：Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5，AGPL-3.0-only 路线。
表中的上游路径以该提交仓库根目录为准，Kotlin 路径以 library/src/commonMain/kotlin/
com/mocharealm/accompanist/lyrics/folia/ 为准。当前表是修复清单，不是最终验收标准。

| 上游行为／源码位置 | 当前实现 | 缺失或简化 | 修复与验收 |
| --- | --- | --- | --- |
| utils/lyrics/cjkSemanticLayout.ts:buildPostLyricLayoutUnits、buildDisplayWordsFromLayoutUnits；utils/lyrics/wordSegmentation.ts | internal/text/DisplayUnits.kt | 任意 Latin 同词片段合并；没有 sticky 标点／缩写；CJK 无语义层。上游 classic 最终会展开语义 CJK 词，非直接合并动画 | 保存逻辑词、显示／塑形分组与来源；对照收缩词、标点、中英和上下文文字 |
| utils/lyrics/graphemeTiming.ts:buildWordGraphemeTimings；components/visualizer/GlowWord.tsx:graphemeTimings | DisplayUnit 只有总起止时间 | 合并后内部计时丢失；正文和辉光使用相同整词粒度 | 保存源片段；字素按 syllable 均分、最后终点精确；完整塑形并用字素范围遮罩控制辉光 |
| classic/Visualizer.tsx:placementOf / calculatedMargin / lineConfig；glowWordOverlap.ts:getGlowWordReach / PairGap / Overflow | ClassicLine.kt:safeScale、固定 gap、居中 | 未按宽度／偏移计算边距；缺少 passed 旋转 8 步包络、seed 对齐和行距 | 移植纯几何公式并直接执行上游函数作数值 oracle；浏览器布局矩形与同字体测量对照 |
| useOverlapFit.ts:useOverlapFit | ClassicLine.kt:最多 4 次 fit | 当前默认也缩小；上游只在 avoidOverlap 开启，min .5 / slack .98，按布局大小迭代 | 对齐触发条件和可用区域，检查窄／长／旋转场景 |
| GlowWord.tsx:状态选择；glowWordVariants.ts:layout/body variants | ClassicSampler.kt:sampleWord | spring 有闭式实现；passed 默认曲线、正文 blur 和颜色曲线不完整 | 导出原版 transition 配置并采样引擎响应；对照 lookahead、短词、passed 边界和随机 seek |
| wordGlow.ts:wordGlowVariants | ClassicSampler.kt:glowIn × glowOut；ClassicLine.kt:整词遮罩 | 临时线性曲线、0.35/0.5 层强度；无按字素延迟、6倍持续与 0.9s 余韵 | 正常／fast／instant 数值关键帧 oracle；同字体辉光截图，保留 shaping |
| glowWordTiming.ts:getGlowWordLineContainerMotion | ClassicLine.kt:lineFrame | 入场固定 .22s、线性退出、无正文／整句 blur | 对齐各模式 opacity/scale/filter 与缓动，检查整句替换和空档 |
| classic/Visualizer.tsx:lyricContainerFloat | ClassicLine.kt:sin | 上游 y=[0,-14,0,6.3,0]、scale=[1,1.01,1,.995,1]、7s easeInOut；当前正弦 | 保留关键帧形状；平台使用媒体时间以满足暂停/seek，明确此时间源适配 |

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
