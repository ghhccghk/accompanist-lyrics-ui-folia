# Mounted 相机／材质隔离 — 2026-11-18

这是后续定位证据，**没有生产修复，也没有视觉验收通过**。公开入口仍是实验调试
预览；本组是既有内部两曲切换矩阵，不能称 sample 已有完整曲间 sequencer。

## 固定来源和控制

原版 Folia `cc6226e464050a456a2913e062374f23d3844cc5`：
`CameraRig.tsx:253` 读取上一回调发布的 active width，`:289–319` 捕获 flight，
`:322–358` 位置／look SmoothDamp，`:393–400` 阅读权重，`:403–440` 目标合成／
frame-keep／朝向 follower 和视觉 bank。对应本地 `DioramaCameraTrack`、
`DioramaCameraFollow`、`DioramaCameraFlight`；本轮均未修改。

直接读取既有 `mounted-cut-v1/steps.tsv`、`scene.json` 和 `mounted-material-v1/materials.tsv`，
使用相同 core 合成歌词、字体、380/800×600、DPR1、颜色与 cut6.8s。没有新浏览器采集，
不重新解析版本冒充新原版；原采集锁及实际解析版本以 scene.json 为准，仍为 Motion
13.4.3/dom13.4.2/utils13.3.0。48张原图逐一核对既有冻结 SHA256，全部不变。

两项 JVM Compose 测试使用真实已发布 core 和相同完整塑形的文章准备：

- 48个原时刻、每个6个因子控制，另有8个从**原观测3.2s状态**重新积分的短控制。
- 原 incoming 的1906个有限回调（继承原 outgoing 最后 ref，宽度由自己的相同字体
  准备结果提供）逐字段1e-9检查，峰 `6.039613253960852e-14`。
- `finiteOriginalAnchor` 重放原 dt 和上一回调宽度，校验控制本身。
- `finiteMediaAnchor` 只换成本地 outgoing 的可重建状态。
- `finiteCurrentWidth` 只换为当前 width；含首帧 flight 捕获和行切换耦合。
- `finiteMediaAnchorCurrentWidth` 同时更换以上两项，保留交互，不相加推算贡献。
- `media` 是原生产媒体轨迹；`originalAnchorMedia` 只将 incoming 起始 refs 换成原观测。
- `localOriginalAt3.2` 只在3.201/3.499使用：从原3.2s全部 refs 开始，按原有限算子
  自适应步长（2e-5误差界）积分，不含旧曲／飞行前3.2s的累积差。不是新的生产候选。

所有 source ref／material 注入仅在**明确标记的诊断控制**中，绝不驱动生产。
数值表为 `camera-{380,800}.tsv`、`factors-{380,800}.tsv`。
projectionMaxPx 是当前行完整宽度左右端点与中心的投影最大欧氏差，**不是全字形
几何上界**；图像比较另外执行。四元数角度只是解释量，不替代逐字段 gate。

## 3499ms 的数值定位

800px 下，与各自原回调历史比较：

| 控制 | 60Hz 三点最大投影差 px | 144Hz 三点最大投影差 px |
| --- | ---: | ---: |
| 原 incoming 有限算子控制 | <1e-9 | <1e-9 |
| 仅继承本地旧曲状态 | 0.213766 | 0.017844 |
| 仅使用当前宽度 | 0.439804 | 0.456060 |
| 两者同时改变 | 0.598697 | 0.472327 |
| 当前媒体轨迹 | 3.323647 | 1.466574 |
| 原初态的媒体轨迹 | 3.096356 | 1.446251 |
| 从原3.2s状态开始的短积分 | 0.866642 | 0.296637 |

起始旧曲位置误差 max 是0.130416／0.009502 world，但单独重放它到3499ms已明显
衰减；不能拿旧整曲误差当局部根因，也不能因此假设误差互相独立或线性可加。
当前宽度与首帧 flight 耦合的影响也不足以解释全部残差。
同3.2s初态的短段仍有差异，尚未证明是必须的平台差异，更没有豁免原版验收。

## 同画面相机／材质交叉

选自**同48矩阵**的3.201/3.499s、两宽度、两调度，共8帧。四条完整本地文字舞台：
原观测 pose／原观测 envelope、本地媒体 pose／原 envelope、原 pose／本地媒体
material、本地 pose／本地 material。原 envelope 输入仍逐项与独立采集材质1e-9核对。

本地全媒体路径与既有8张冻结媒体图**逐像素一致**；重复捕获逐像素相同，逆序媒体
查询仍返回同一不可变相机状态。透明F16合成、shaping、真实字素时钟、原样本／阈值均未修改。
所有数字使用 original 加四条本地路径的**固定五图非背景并集**，保留低alpha；
没有配准、延时、选择背景像素或丢弃失败帧。不同旧表并集均值不可直接比较。

800px／3499ms 的 MAE / 最差64×64局部：

| 相机 / 材质 | 60Hz | 144Hz |
| --- | ---: | ---: |
| 原 / 原 | 1.101806 / 1.775113 | 1.208816 / 1.804572 |
| 本地 / 原 | 21.909878 / 41.252336 | 11.296539 / 22.433333 |
| 原 / 本地 | 1.189210 / 1.938807 | 1.209516 / 1.804572 |
| 本地 / 本地 | 21.990835 / 41.252336 | 11.297483 / 22.433333 |

此处主要残差确实来自相机轨迹；只换材质保留本地相机，最差局部不改善。换成
原相机后的剩余栅格误差仍在，不能称等价。完整8帧数值、原图与交叉图 hash 见
`comparison.json`。contact `build/verification/diorama-camera-isolation/contact-800-3499.png`
已图检；仅为缩小的阅读预览，不参与像素指标，原分辨率对比图保留在同目录。

初次直接 ImageBitmap/CanvasDrawScope 重绘与旧 Compose 捕获首例差12像素、峰1，
未放宽精确断言。该采集候选和失败XML／日志保留在
`.work/diorama-camera-isolation-imagebitmap-rejected`、
`.work/diorama-camera-material-cross-failure.xml` 和 `.work/diorama-camera-material-cross.log`。
换回**原来相同的 Compose Canvas/capture 路径**后8帧精确；底层取整原因没有单独隔离。

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaMountedCameraIsolationTest'
python verification/compare-diorama-camera-isolation.py
```

依赖之前冻结的本地原图与媒体图；缺失时脚本会明确失败，不自行重采基准。通过日志
`.work/diorama-camera-isolation-verified.log`。本轮无生产代码、Android新行为／设备、
真实窗口性能或原版正式视觉结论；最新调试APK仍是84f3bcb。

## 下一条可检验假设

从同一原 flight 初态、相同 incoming target／宽度出发，逐项读出飞行阶段的
look-only orientation、对齐 target、frame-keep 后 target 和最终 follower，尤其是
3.199→3.2s交接；比较原 finite delta 与自适应误差控制下的独立原算子结果。
先判定偏差产生于 flight forcing、复合顺序还是有限采样，不凭本表加入固定帧率、
固定延迟或源 pose。若发现算子错误再作最小生产修复；本轮只是排除了两条单因素捷径。
