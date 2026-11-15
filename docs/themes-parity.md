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
| 云阶 | partita；`partitaLayout.ts`、`PartitaChunk.tsx`、`partitaOverlap.ts` | 依高度拆块、交替阶梯错位、块级入场、引导线、GlowWord | **当前第一项**：内部布局→块动画→同字体视口双侧画面→公共入口；目前没有公开支持 |
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

本项结果：612行全部通过；已有DisplayUnitsTest与UpstreamGeometryTest回归通过。
Android示例assembleDebug、Desktop示例compileKotlin通过。首次测试源缺translation参数
已修复后重跑；未关闭检查。此步骤未启动设备或可见窗口、未进行主题视觉验收。

平台适配：lyrics-core仅行时间输入显式保留一条行计时run；保留fragments和
sourceGraphemes，不因粘连或语义分组丢弃内部计时。分词字典/Unicode版本差异
仍需在实际发生差异的输入中记录，不能笼统豁免。

下一项：块级waiting/active/passed和中断spring、引导线、正文/字素辉光与整句图层；
同源窄宽画面与暂停/seek/resize后，才增加FoliaStyle.Partita及示例入口。
