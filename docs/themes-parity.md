# 多主题移植与验收

2026-11-16：用户要求补齐多主题，覆盖此前仅classic首版的优先级安排。
按固定 `v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5` 登记。
未接入的样式不放入公共选择器，不通过classic换名冒充。

## 名称与支持矩阵

源码路径相对上游 `src/components/visualizer/`；中文名来自 `src/i18n/locales/zh-CN.ts`。
用户预览标签中的Lumi／流光，在本基线对应 `classic/entry.tsx` 的Luminous／流光；
固定版另有 `lumiere/entry.tsx` 绘光，不混为一个主题或静默增加绘光范围。

| 主题 | 固定mode／源码 | 上游行为 | 当前差距与后续任务 |
| --- | --- | --- | --- |
| 流光 | classic；`classic/Visualizer.tsx`、`GlowWord.tsx` | 居中词布局、字素辉光、余韵和句转场 | 已实现但未最终验收；完整画面栅格、Android复杂字幕残差继续保留在classic-parity.md |
| 云阶 | partita；`partitaLayout.ts`、`PartitaChunk.tsx`、`partitaOverlap.ts` | 依高度拆块、交替阶梯错位、块级入场、引导线、GlowWord | **移植校准中**：已接FoliaStyle.Partita与示例，完整画面尚有残差，未最终验收 |
| 倾诉 | tilt；`VisualizerTilt.tsx` | 种子决定1–4段，水平／大号斜体两种排版，字素上下位移 | 未实现；先分段、宽度重拆和字号拟合，再校准字素时间；不能仅旋转classic |
| 浮名 | fume；`VisualizerFume.tsx`、`fumeArticleLayout.ts`、`fumeCameraStep.ts`、`fumeReveal.ts` | 整曲文章布局、字素打印、步进/平滑相机、读后残留 | 未实现；文章与确定性相机模型、resize/逆向seek对照，背景不纳入 |
| 心象 | cadenza；`VisualizerCadenza.tsx`、`cadenzaPlacements.ts`、`cadenzaEnvelopes.ts` | 碎片云布局、焦点强调、状态绘制顺序、漂移包络 | 未实现；先同种子布局/包络，再文字层，不引入原DOM依赖 |
| 群唱 | cappella；`VisualizerCappella.tsx`、`cappellaMessages.ts`、`cappellaMessageLayout.ts`、`cappellaReveal.ts` | 多发送者对话、气泡、消息保留与逐字揭示 | 未实现；明确core声部与发送者映射；先文字消息，不复制未知来源头像/emoji |
| 时计 | pendolo；`VisualizerPendolo.tsx`、`pendoloGeometry.ts`、`pendoloTimeline.ts`、`PendoloActiveLyricSweep.tsx` | 圆弧歌词轨、机械跳步、当前句扫描、字幕轨 | 未实现；先文字几何/跳步/扫描；音频鼓点与节拍增强单列，不伪造输入 |
| 镜台 | diorama；`VisualizerDiorama.tsx`、`cameraPath.ts`、`DioramaSceneLayout.ts` | 文字沿三维路径排列、镜头穿行与曲间连续过渡 | 未实现；核心镜头涉及旧“不做整套三维场景”边界，先调查最小平台能力；若必须改变边界单独请求决定，不以2D假效果验收 |

除classic外，表中主题目录前缀与mode相同。实施顺序按复用依赖安排，不代表已经支持。
字体、颜色、视口、媒体时间、随机种子相同才作视觉对照；暂停冻结、seek直接重建、
完整shaping与内部字素计时、尺寸变化仍为每个主题共同验收条件。

## 云阶：内部布局内核

上游依据：`partitaLayout.ts:buildSequentialColumns/buildPartitaWordConfigs`、
`partitaOverlap.ts:getPartitaOverlapSpacing`。
目标：`internal/styles/partita/PartitaLayout.kt`。使用原版正弦种子流，先拆块再消费
错位/缩放随机数；16px根字号rem与歌词em分开，避让先执行CSS toFixed再求值。

`verification/classic-browser/generate-partita.mjs` 直接加载干净固定checkout中的原函数，
生成 `verification/partita-fixtures/layout.tsv`。6种合成输入（Latin、粘连标点、CJK、
Arabic、组合字素/emoji、单词）、2起点、3高度、3强度、语义开关，共216种布局/612行。
检查拆块、显示词、5项块姿态、每词姿态、混合单位避让和总字素数。
这些数值控制**不能代替云阶画面验收**。

本项结果：612行全部通过；UpstreamGeometryTest及带Display名称的原间奏用例通过。
校正75799d8的测试名记录：项目没有DisplayUnitsTest；该轮通配符未运行完整塑形回归，
不能由测试总数推定已执行。共享塑形提取后须显式运行UpstreamGraphemeTest。
Android示例assembleDebug、Desktop示例compileKotlin通过。首次测试源缺translation参数
已修复后重跑；未关闭检查。此步骤未启动设备或可见窗口、未进行主题视觉验收。

平台适配：lyrics-core仅行时间输入显式保留一条行计时run；保留fragments和
sourceGraphemes，不因粘连或语义分组丢弃内部计时。分词字典/Unicode版本差异
仍需在实际发生差异的输入中记录，不能笼统豁免。

## 云阶舞台与公共入口（校准中）

| 上游 | Kotlin目标 | 行为 |
| --- | --- | --- |
| `PartitaChunk.tsx` animate/transition | PartitaMotion.kt | 从默认姿态进入waiting；相同目标的y/rotate继续400ms easeOut，x/scale在active中断后使用200/20 spring；唱后相同目标继续运行 |
| `VisualizerPartita.tsx` flex/字体/float/useOverlapFit | PartitaGeometry.kt、PartitaLine.kt、FoliaLyricsView.kt | 独立nowrap阶梯块；384px固有列高、内外边距、5.5vw字体、长行密度、宽高拟合、媒体时间float |
| `PartitaChunk.tsx`引导span | PartitaGuides.kt | 左右交替、1px线、32px竖线、10px阴影、独立opacity/scale组 |
| GlowWord/variants/wordGlow | PartitaLine.kt复用既有采样/栅格/图层 | 完整shaping、字素遮罩、颜色alpha顺序、词/块/句三层；不启动独立时钟 |
| Partita传入共享字幕的字号 | ClassicSubtitles.kt内部partita分支 | 翻译clamp(16.8px,2.2vw,19.2px)，下一句clamp(14px,1.8vw,16px)，classic默认分支保持 |

原版墙钟过渡适配为可重建媒体时间，暂停冻结。core片段可能切入代理对/组合字素/ZWJ，
此时禁止以该点换块；保留源片段后交给整段塑形。有效原版输入不改变拆块，源字素时间
与上游有效重计时分别保留。这是输入适配，不是丢掉内部计时。

capture-partita.mjs挂载实际固定版Partita/PartitaChunk/GlowWord；纯文字shell排除背景/
播放器，字幕另验。使用既有明确许可字体，没有SF Pro。默认216张、紧密入场72张、
避让72张原版PNG/条件在build/verification/partita-browser。默认和紧密场景共702条
实际Motion块状态，五通道0.0001容差通过，包括waiting中断，非仅测稳态。

首轮216帧前景并集RGB MAE均值3.748739，最高14.441281（chaotic窄屏wrap2100ms）。
首轮未含引导线阴影且竖线底点差8px，已按CSS修正；旧图/误差保存于partita-kotlin-first
及partita-kotlin-first-comparison。修正后结果另记，不能用此中间残差宣称视觉达标。
compare-partita.py保留原坐标、前景并集及PNG散列，不做拟合配准。

PublicPartitaTest已通过：公开/独立renderer相同、暂停、逆向seek、偏移一次、
800→380→800恢复、变换后的点击/长按原行身份与空白不命中。示例本地歌曲和开发/演示
均在显示设置选云阶；引导线、语义、错位、避让可调；version=1设置兼容原classic默认。

首次宽/窄矩阵分别放在单个UI测试导致协程超时，保留失败日志；现按独立场景运行，
不移除时间点、暂停断言或逆向重访。避让探针最初错误扣除了首行正marginTop，已使用
正确原点，保留原期望与0.51px DOM整数度量容差，修正后normal两宽度通过。

待验收：完整画面栅格/变换残差、更多CJK/混排、字幕、连续跨句录屏和新主题的Android
设备/真实Desktop窗口。共享classic旧验证不能当本主题运行验证。缺少core语义的合唱
涟漪、音频背景增强未实现；本主题仍为中间移植，不称为全部主题或最终等价实现。

### 精度与短词对照修复

完整矩阵曾有一项真实失败：chaotic/wrap/800/avoid 第三块top原版280px，Kotlin
279.46875px，超出既有0.51px整数DOM度量容差。原版将clamp的rem/vw各操作数
toFixed(3)，最终used font=33.736px；旧实现把最终字号截为33.73px并误用于布局。
已将CSS字号/行盒精度与百分之一像素字形缓存分开，没有修改原期望或放宽容差。
同时按PartitaChunk自己的dir确定各块方向，保留blur(0px)句容器的独立合成表面。

此后30个场景矩阵、公开入口及逻辑回归通过，默认216帧前景并集RGB MAE均值
3.689387（首轮3.748739），最大14.370593。新增tight/avoid合计360帧在短词修复前
均值4.881806、最大17.301336；这些仍为残差，不能与首轮不同矩阵直接作改善比较。
已目视默认接触表和四个最差帧。短词Early在950ms仍模糊是可确认行为错误，已继续修复。

capture-word-filter.mjs从实际原版组件记录429条滤镜值：active的none与passed的
blur(0px)为同一数值目标，原来的200ms tween继续到终点；旧sampleWord错误冻结
在activeEnd再乘一次passed衰减。共享采样已按真实轨迹修复，原版数值0.0001容差通过。
没有用改动预期曲线掩盖失败。受影响的tight六组画面另行重采；其最终残差继续登记。
六组重采通过全部暂停/逆访断言：tight前景MAE均值6.338375→5.032105，
最大17.301336→13.026016；360帧总均值4.620552，最差仍为窄屏Calm避让
2800ms的15.473299。没有据此宣布完整画面等价。新结果及原始SHA记录在
build/verification/partita-kotlin-comparison/conditions.json。

显式运行UpstreamGraphemeTest、ClassicSamplerTest、CssWordTextTest、UpstreamGeometryTest，
classic固定42帧和字幕宽窄84帧散列均与变更前相同。sample:shared:jvmTest、Desktop
compileKotlin、Android assembleDebug通过。本轮使用离屏Compose和无界面Chromium，
未开启用户桌面窗口/模拟器；新主题尚无设备或真实窗口运行验收。复现见
verification/partita-fixtures/README.md，经典既有残差继续保留。

## 倾诉：分段、拟合和内部计时（尚无公共舞台）

上游VisualizerTilt.tsx的determineLineCount/buildTiltLayout使用UTF-16长度、秒种子，
决定1–4段及唯一大号斜体段；宽度比>=1.6时重拆，按原分支选择拟合并保留0.5/0.55下限。
SentenceLayout按五层标点/括号/中西文/空格/特殊符号优先级拆分，再选语义中点或种子回退。
对应TiltLayout.kt、TiltSentences.kt，不调用classic或Partita布局冒充。

findSegmentWordRange/buildCharTimings对应TiltTiming.kt，保留段内非空白字素计时和
切入词内时的原版映射；pulse为0.2–0.9s正弦后接25%尾部，不是无限呼吸。
lyrics-core原始片段仍由适配层保留。平台必要调整：原版UTF-16中点回退可能切断代理对
或组合字素，Kotlin只使用合法EGC边界，不能分开的字素不强拆。这一输入保护单列测试。

直接运行原始SentenceLayout得到378种分段；原版私有声明逐字提取到本地测试wrapper，
以明确的合成测量回调生成2430种布局、120个pulse状态、108个词范围/内部计时。
Kotlin对应数值测试全部通过；Android debug assembly通过。没有替换上游函数体，
也没有把合成宽度控制当成真实字体度量或视觉证据。该核心仍未接入主题选择器。
下一项是两种排版的真实文字绘制、分段/字素入场及同字体视口的原版浏览器对照。
