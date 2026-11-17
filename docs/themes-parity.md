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
| 倾诉 | tilt；`VisualizerTilt.tsx` | 种子决定1–4段，水平／大号斜体两种排版，字素上下位移 | 已接 FoliaStyle.Tilt 和示例，明确标注校准预览；450ms 公共退场、Latin 段宽已补，Arabic shaping、CSS transform 延迟和全局字体缓存仍未验收 |
| 浮名 | fume；`VisualizerFume.tsx`、`fumeArticleLayout.ts`、`fumeCameraStep.ts`、`fumeReveal.ts` | 整曲文章布局、字素打印、步进/平滑相机、读后残留 | 内部时序、文章布局、拟合、Latin静态与实时文字已移植并有原Canvas对照；镜头未完成，文字栅格/复杂字体仍校准，未开放选择器，背景不纳入 |
| 心象 | cadenza；`VisualizerCadenza.tsx`、`cadenzaPlacements.ts`、`cadenzaEnvelopes.ts` | 碎片云布局、焦点强调、状态绘制顺序、漂移包络 | 未实现；先同种子布局/包络，再文字层，不引入原DOM依赖 |
| 群唱 | cappella；`VisualizerCappella.tsx`、`cappellaMessages.ts`、`cappellaMessageLayout.ts`、`cappellaReveal.ts` | 多发送者对话、气泡、消息保留与逐字揭示 | 未实现；明确core声部与发送者映射；先文字消息，不复制未知来源头像/emoji |
| 时计 | pendolo；`VisualizerPendolo.tsx`、`pendoloGeometry.ts`、`pendoloTimeline.ts`、`PendoloActiveLyricSweep.tsx` | 圆弧歌词轨、机械跳步、当前句扫描、字幕轨 | 未实现；先文字几何/跳步/扫描；音频鼓点与节拍增强单列，不伪造输入 |
| 镜台 | diorama；`VisualizerDiorama.tsx`、`cameraPath.ts`、`DioramaSceneLayout.ts` | 文字沿三维路径排列、镜头穿行与曲间连续过渡 | 未实现；核心镜头涉及旧“不做整套三维场景”边界，先调查最小平台能力；若必须改变边界单独请求决定，不以2D假效果验收 |

除classic外，表中主题目录前缀与mode相同。实施顺序按复用依赖安排，不代表已经支持。
字体、颜色、视口、媒体时间、随机种子相同才作视觉对照；暂停冻结、seek直接重建、
完整shaping与内部字素计时、尺寸变化仍为每个主题共同验收条件。

## 浮名：打印与读后保留内核（未开放主题）

`FumeReveal.kt` 对应原版 `fumeReveal.ts`、`fumeTextStyle.ts`、`fumeMath.ts`。
18个合成输入的30个词范围/内部时钟、1584个边界和随机媒体时间的正逆序重建、
494个退场截止/辉光/读后保留数值通过：计数精确相等，浮点容差1e-10。
没有借用classic的粘连/语义重计时；连续焦点与离散打印分别保留；句末强制完成使用
原始end而非renderEnd。源片段切入EGC时仅合并不安全边界，原片段时钟仍保留。
原版保留时长缓存的三位小数碰撞不移植，独立查询直接求值；Infinity分支按原函数保留，
后续需连同真实调用层继续验证。证据生成器/限制见verification/fume-fixtures/README.md。
FumeRevealTest与共享UpstreamGraphemeTest通过，Android示例assembleDebug和Desktop
compileKotlin通过；没有Android连接设备或Fume画面验证，文章/镜头/绘制仍未实现。

### 浮名文章布局（后续已完成内核）

上游 `fumeArticleLayout.ts` → `FumeArticle.kt`，`fumeTextMeasure.ts` → `FumeTextMeasure.kt`：
FNV-1a稳定打散、自然/兜底hero选择、跨列占位、等高轮转、字号二分、各列数与密度的
高度评分均保留。几何与媒体时间无关，只在输入/视口/字体变动时由调用者重建；
不沿用classic的一行居中布局。Compose始终测完整字符串，输入源字素/词计时不变。

原版受控测量96篇/1560块的顺序、字号、位置与文章指标在1e-8内通过；
真实OFL同字节字体、DPR1、380/800/1440×600、短篇和48句密集文章共6篇/150块，
完整几何/字号/换行严格对齐。最初整数intrinsic测量失败，改用分数line advance，
仅拟合时悬挂尾随ASCII空格，再按已核对的百分之一像素字号边界修复，没有改期望/容差。
源码原文与完整shaping仍保留；未移植原版依赖测量历史的猜测缓存。

原版实际createStaticBlockSnapshot与native几何诊断图均已保存；
`verification/fume-article/contact.png`和comparison.json给出6帧对照。
前景并集RGB MAE均值70.5127，**没有视觉通过**：Canvas middle基线、hero文字8%内缩、
静态快照栅格/合成和逐字绘制还未移植。诊断图只有布局可验，不是Fume正式renderer。
Fume相关JVM测试和共享字素回归、Android示例APK及Desktop编译通过；没有本轮Android运行。
下一项：先按原版静态/动态文字层校正绘制，继续镜头目标与确定性媒体时间重建；
达到可验收闭环后再开放主题入口，不把纯内核完成写成主题支持。

### 2026-11-17 浮名静态文字层（最新）

上段为3224bb6节点的历史诊断，后续将原版
`fumeCanvasText.ts:createStaticBlockSnapshot`适配为`FumeStaticText.kt`。
完整行塑形后生成带padding的透明快照，恢复Canvas middle基线、hero余宽8%内缩、
原版body/hero行高和CJK基线系数。绘制时保留悬挂空格，不因拟合/整数约束再次拆行。
行的UTF-16范围保留以供后续字素打印裁剪，不逐个UTF-16字符绘制。

受控算法扩充为120篇/1632块；实际同字体3场景×3宽度共9篇/159块，包含长段落和硬换行。
文章/块/字号仍在1e-8内相等；289个实际renderLine文字和换行逐项相等，
行宽最大差0.0007893px、hero文字内缩最大差0.00003052px（各自检查上限1/64px）。
159个字号的真实Canvas middle偏移全部在1e-8内精确对齐。
每幅静态图再推进2秒墙钟像素不变；媒体打印/逆向seek原有检查继续通过。

基线第一版只按整体1/64取整，精确探针失败；实际是上升部和总em分别取整，
中点可落在1/128。另一次新renderLine检查暴露尾随空格被少算和额外空行，已修复。
未提高容差或改源期望，失败日志保留在.work/fume-static-*。

相同原6帧前景并集RGB MAE从70.5127降至3.1370；新增多行3帧后9帧均值6.0209，
最高13.7049（多行800）。原位置不配准，不做色阶拟合；截图和比较JSON仍在
verification/fume-article。**仍未通过最终视觉验收**：快照栅格/合成残差、
多行采样、其他文字系统尚待校准；当前native自然字体度量作为typo度量的临时来源，
OS/2与hhea不同的字体不能据Latin结果宣称基线等价。

本轮JVM相关用例、Android APK构建与Desktop编译通过；未连接设备，无Android实际画面。
尚无实时辉光/打印印记、确定性镜头、Fume公共主题入口。下一项继续这些依赖，并校准
复杂字体基线；不扩展示例播放器、背景引擎、SPW或native性能分支。

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

以下两节保留初次内部阶段记录；最新接入与证据见文末，不以历史“未公开”描述当前入口。

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

### 倾诉内部舞台与真实原版证据（未接公开入口）

`TiltLine.kt`独立绘制普通/斜体段、字距、内部字素揭示、上下交替错位和计时pulse。
先塑形完整段落再裁取EGC遮罩，未按UTF-16逐字符塑形。`TiltMotion.kt`重建550/600ms
行tween、500ms字素揭示、40/50ms字素延迟及行退场；句450ms淡出仍只接内部探针。
公共VisualTimeline尚未延长退场保留，不能把此内部实现称为已支持倾诉。

真实未改动上游组件、pretext锁定0.0.9、Latin/Arabic许可字体、380/800×600、DPR1：
144张原版和144张离屏Compose图已保存，12场景分段文本、暂停和逆向重访严格一致。
258行Motion状态误差≤0.0001；3882字素Motion状态误差≤0.001（输入原版未取整错位幅度，
隔离动画与字体拟合）。实际CSS 60ms transform过渡保留，但computed transform尚未等价验证。
最初10ms步进漏过2.2-.25的浮点阈值，已在oracle访问真实事件及其后1us，保留旧fixture在.work。
未改曲线预期或放宽容差。首轮0.00102px错位失败最终定位为测试用已取整CSS字号反推幅度，
不是已证实的生产精度bug；改为独立原幅度输入，生产保持Double到绘制边界。

原版字体测量有实际兼容问题：倾斜fontSpec在字体族列表后附`italic`，Chromium拒绝赋值，
沿用上一次Canvas font；初始10px sans-serif。真实pretext及Canvas测出整句229.692px，
正确90px字体为2161.710px，single模式因此不重拆并溢出视口。当前按每行局部赋值顺序
兼容，不复制全局Canvas/cache跨行历史；该全局历史差异未解决，不作为有意差异豁免。

完整画面前景并集MAE均值38.6234、最大164.9593，远未达标：Arabic原版每EGC inline-block
隔离、LTR host与Kotlin完整塑形存在显著字序/宽度/连接差异。继承未指定方向本身不能消除它；
不能破坏shaping或隐藏Arabic来通过。Latin800第二段宽原版483、Kotlin480.328px；
single整句1705与1700.547px，字距/最小空白宽、CSS整数盒和基线还需独立校准。
目视contact.png确认差异，不把数值通过当作画面验收。下一步仍是上述校准、EGC片段安全
保留原syllables、公开入口450ms时间线、字幕及平台运行。源码与复现见verification/tilt-fixtures/README.md。
证据：build/verification/tilt-{browser,kotlin,kotlin-comparison}；本次未开用户窗口/模拟器。

### 2026-11-16：倾诉入口、段宽与450ms退场

`FoliaStyle.Tilt(TiltTuning())` 已连接独立 TiltLine（不是 classic 换名）；两端 sample 的
本地歌曲与开发/演示共用主题设置。参数对应原版 splitProbability、tiltStyleProbability、
default/swap/accentAll/primaryAll，设置 version=1 向后兼容，旧默认仍是流光。
**入口和界面均标注“移植校准预览”，不是最终视觉等价支持声明。**

| 上游依据 | 本次实现 | 验证及仍有差异 |
| --- | --- | --- |
| VisualizerTilt.tsx:645 父容器 exit opacity / 0.45s easeInOut | VisualTimeline 支持主题指定保留时长；Tilt 固定450ms，其他主题保持原规则；TiltMotion 采样同曲线 | 实际原版11个退场时刻；310ms/400ms 原版 alpha .2014625/.02434718，采样误差≤.0001；公开/独立舞台11帧逐像素相同，不宣称与浏览器逐像素一致 |
| VisualizerTilt.tsx:454/507 每个 EGC inline-block，无跨元素 kerning | 完整段落塑形保留，只关闭可选 kern；Skiko 用 kern 0，Android Paint 用带引号的标签；拟合测量不受影响 | 实际原版未取整 CSS 宽度，14个 Latin 段宽最大误差0.4069px；800px第二段480.328→483.156px，原版483.297px；joining/字素内部计时仍保留 |
| VisualizerTilt.tsx:200/215 无效尾置italic字体声明；pretext0.0.9 measurement.ts 的按 fontSpec 缓存 | 每行重建无效字体键的空格/token宽缓存，后续正常字体赋值不抹掉已有度量；不引入跨行/全局历史 | 修正窄屏拟合；已新增真实 fractional CSS 度量，不能拿整数offsetWidth当精确尺寸。原版跨歌曲/跨组件缓存历史、复杂分词/全局字体状态仍未等价 |
| VisualizerTilt.tsx:355 字距/行高与完整场景 | 保持现有行盒和shaping保护，不复制原逐字断开Arabic | 144张重新挂载原版与Compose画面；前景并集MAE均值32.8560，Latin12.4144、Arabic53.2976，最差167.1414，**仍未视觉验收通过** |

旧width整数断言曾在238px原版/238.516px本地失败，进一步获取原版未取整CSS宽238.453px后，
实际差0.063px；保留原fixture，增加 mounted-geometry.tsv，没有修改原版值或扩大0.51px容差。
失败日志留.work/tilt-final-width-assertions.log。先前无效字体开关与忽略旧空格缓存的候选也未提交。

验证：Tilt分段/拟合/内部计时/实际Motion/450ms曲线；公开入口与独立舞台、暂停、反向seek、
offset一次、800→380→800恢复、点击/长按原行；经典采样/字素、云阶公开入口回归；
sample JVM设置/播放器回归、Android APK及Desktop编译。没有开启可见窗口、没有安装设备包。
Android仍缺真机主题交互和字体特性效果验证；Desktop为真实Compose离屏渲染，不冒充窗口运行。

证据：verification/tilt-public/、build/verification/tilt-{browser,kotlin,kotlin-comparison} 和
tilt-exits-{browser,kotlin}；原版fixture、字体散列及所有144帧散列已保存。
复现先启动浏览器server，运行 capture-tilt.mjs、capture-tilt-geometry.mjs、capture-tilt-exits.mjs，
再运行 `:library:jvmTest --tests '*Tilt*'` 和 compare-tilt.py。源checkout保持固定且干净。

下一项仍是CSS60ms transform真实过渡、CJK/混排/字幕矩阵及Arabic源DOM与完整塑形的处理。
这些是未解决的移植差异，不改成最终验收豁免；浮名等主题不以占位加入选择器。

### 2026-11-17：浮名实时文字对照（内部，尚无主题入口）

原版fumeBlockFrame/fumeCanvasText直接采样1104时刻、8970字素样式及317落印，
颜色/alpha/半径/落印几何/读后淡出/总览恢复在1e-8内通过。Kotlin保留整行塑形，
内部字素时钟与离散打印分离；用词段覆盖mask、run裁切及原命令顺序绘制。

原版Canvas和Compose使用相同OFL字体、原块几何、媒体时刻、1:1视口，无镜头、无配准。
三场景×两宽×辉光开关×14时刻=168对；前景RGB MAE均值2.9653、最大9.8170，
关闭辉光2.4973、开启3.4332。已查看contact.png；仍未达最终视觉等价。
实际分词/前缀advance与完整行shaping选区、词段光晕边缘、复杂字体基线和多行栅格
差异继续登记，不能归为最终有意差异。源几何在此为显式输入，仅隔离文字绘制。

168次随机重访与暂停像素严格相同；紧凑延迟mask相对初版168帧完全相同。
证据见verification/fume-live/comparison.json和contact.png，原图在build/verification/fume-live-*。
复现见verification/fume-fixtures/README.md。Android未连接设备，只报告构建；
Desktop实际Compose离屏执行，不冒称可见窗口或完整主题运行。下一项是镜头焦点与媒体时间重建。

### 浮名连续镜头响应基础（仍未开放）

原版目标/飞行选择与积分已分离，8696步原轨迹保持1e-8一致。
新增FumeSpringResponse：对原版冻结系数更新矩阵求连续响应；
125个原版时刻/375轴最大绝对误差1.1674e-12，分数时长可任意分段，
无显示帧累计。标定间隔1/60秒只标识参考原版响应，不是播放时钟。
其他原刷新率仍需比较；动态系数、限速、桥接和真实文字全景尚未通过。
本项检查还覆盖焦点特征根重合与长seek不产生NaN；两端构建通过，
没有Android运行或新增主题入口。下一项为动态媒体轨迹及全景画面对照。

### 浮名可重建媒体镜头（内部校准）

FumeCameraTrack按歌词/打印/飞行事件准备不可变节点，以连续响应任意采样；
暂停重复、随机seek/逆向及窗口末端冻结已验证。原版30/60/144Hz和不规则
9124条轨迹记录单列；native与相同连续适配的原目标观测最大差4.182e-6。
**不等同于原版画面通过**：原60Hz屏幕位置峰值残差6.079px/scale0.007003，
其他刷新率残差见comparison.json。8秒2606–2791节点，全曲准备成本未测。
首次松精度候选失败后修复事件分割并提高积分精度，没有降低断言容差。
未接sample/公共入口；下一项真实整篇文字+相机截图，特别核对Canvas
动态shadowBlur不随CTM缩放而静态snapshot随镜头缩放的区别。

### 浮名整篇舞台与镜头闭环对照（仍校准、无公共入口）

已接FumeArticleDrawing：独立测量整篇文章与内部焦点几何，应用真实镜头变换、
屏幕避让范围、下一句截止、文字保留与总览恢复，非借用classic舞台。
浏览器直接调用固定版原函数，4句合成歌词（词计时/行计时/换行）、同OFL字体、
380/800×600、18时刻、两辉光档和两跟随模式，共144对完整文字场景。
另144对显式共享原版相机，仅用于区分文字栅格误差，不作为镜头已通过。

静态快照合成与实时阴影CTM补偿后，共享镜头平均前景MAE4.822642→3.363080，
完整镜头场景15.754087→14.704448；144张原图散列严格不变。
288帧随机重访和暂停冻结严格相同，已目视接触表。既有无镜头168帧
均值2.965264→2.880905，原字形/计时测试未降低标准。

**仍不达最终验收**：真实文章暴露屏幕位置14.8672px/scale0.055319峰值差，
原版切镜在显示帧采样与规范媒体事件间的差别须继续分析。静态bitmap的分数
目标尺寸重采样、复杂字体typo基线/前缀宽、多行及全曲准备成本未解决。
证据verification/fume-scene与fume-scene-source-camera；后者只隔离文字。
未连接Android；未启用用户可见窗口。下一项优先短桥接/总览边界和分数快照尺寸。
