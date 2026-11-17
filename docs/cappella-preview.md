# 群唱文字对话校准预览

`FoliaStyle.Cappella(CappellaTuning())` 使用独立聊天舞台，sample 的真实播放与开发／演示
设置中均可选择“群唱”。尚未通过正式视觉或设备性能验收。

| 能力 | 现状与边界 |
| --- | --- |
| 真实 core | 保留原行对象和嵌套伴唱 ref、原时间、字幕；不从 alignment 猜发送者 |
| 身份 | 可选 `CappellaLineAgent(lineIndex,agentId,accompanimentIndex)`；相同 ref 最后一项生效。空／单 agent 保留原 seeded fallback |
| 无逐词标注 | 按原版整句字素均分派生打印时间，不称真实逐词标注；不安全 EGC 边界合并显示但保留内部原时钟 |
| 主题参数 | fontScale 默认1（源18px再clamp15..26）；title 默认“歌词”，sample 提供歌曲元数据标题；background 默认#162435，作为气泡混色，不绘整屏背景 |
| 配色与质量 | FoliaColors 正文／活动／次要映射源primary／accent／secondary；Minimal关闭气泡／头像shadow与扫光；Reduced目前与Standard共用行级效果 |
| 播放与交互 | 仅使用宿主快照减offset一次；实际变换后的圆角气泡点击／长按返回原来源行；零alpha／标题／空白不拦截，sample seek再加offset一次 |
| 生命周期 | 暂停冻结、前后seek重建、切歌／主题／尺寸变化、卸载重挂经离屏检查；无新增后台ticker |
| 字幕与等待 | 共享字幕层接翻译／注音／下一句开关；不再重复绘伴唱文本。沿用有限全声部等待区间，保持offset／减少动态语义 |
| 资源 | 仅颜色头像和文字消息。未搬入内置emoji、头像或封面资产；不显示不存在的设置 |
| 偏好 | sample 保存主题／字号；标题来自当前会话，agent为宿主内容输入，不将用户歌词身份或曲目标题写入偏好 |

原版依据：固定 Folia `cc6226e464050a456a2913e062374f23d3844cc5` 的
`VisualizerCappella.tsx`、`CappellaMessageRow.tsx`、`CappellaBubbleParts.tsx`、
`cappellaMessageLayout.ts`、`cappellaMessages.ts` 和字素／renderHints函数。
逐项来源见 upstream.md。

210完整原JSX帧沿0..14s连续挂载，三档强度、窄／宽屏、Latin／长段／Arabic／CJK。
真实 core 公共入口与单独挂载内部舞台逐帧像素严格一致；原版对照最大盒差3.637201px，
opacity差.013716。全非背景像素（含低alpha）MAE2.325954；另记录通道峰和64px局部最差。
这与旧内部报告>8阈值的MAE3.393810不是同一口径，不能当收益比较。

240密集／重叠压力矩阵单独保留：存在／顺序精确，盒峰22.188593px、旧口径MAE9.173245。
仍有明显堆叠投影差；语义时间重建与React/RAF局部提交、原弹簧采样不完全一致。
整段shaping与原字素DOM的复杂字体排版／栅格也有差异。共享字幕层尚未做本主题原版
字幕整屏验收。它们均为待修复项，开放预览没有改变最终验收标准。

验证：`verification/cappella-public/README.md`、`verification/cappella-presence/README.md`。
Android APK构建与Desktop离屏执行通过，未运行Android设备／Desktop真实窗口；
没有音频独立进程或设备帧率的新结论。
