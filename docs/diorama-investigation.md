# 镜台：文字三维能力调查

固定 Folia v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5。
源码路径以下相对 `src/components/visualizer/diorama`；不是公开主题交付。

| 层 | 原始位置与真实职责 | 当前结果／后续工作 |
| --- | --- | --- |
| 文字 | `dioramaSceneLayout.ts:145–230`：CJK字素／其他词分组、前缀宽度；`dioramaTextRaster.ts:29–185`：128px完整字体栈Canvas纹理、邻行4096边界、四层纯halo | 本轮仅共享原纹理隔离投影；内部文字分组/原词身份/完整EGC时钟/core桥接已迁，67原输入641字素/226单元时间峰0；Kotlin整行塑形切片待移植，不能把单位独立栅格照抄成逐字塑形 |
| 路径／摆位 | `cameraPath.ts:223–346`：整曲8世界单位步长、局部正交框架、offset/roll/yaw/scale/look | 已迁入内部DioramaProjection.kt；四seed（含UTF-16 emoji）、12行、四weave的原函数数值对照 |
| 运镜 | `cameraPath.ts:531–754`：13类shot、读头truck、空档settle、drift；`CameraRig.tsx:200–434`：SmoothDamp、24单位seek snap、回正和安全视锥、四元数跟随 | 尚未移植。冻结lookAt投影不是CameraRig，不能以数学通过代替播放镜头验收；媒体重建策略必须另建对照 |
| 投影 | `VisualizerDiorama.tsx:470–478`：真实R3F/Three透视相机55°；`dioramaSceneUnits.ts:103–142`：固定5.2距离fit、basis×yaw×roll | 最小Double投影／平面单应矩阵与Three数值通过；JVM Compose Matrix实际透视纹理绘制9对画面可运行；Android仅共同代码构建 |
| 深度／顺序 | `DioramaScene.tsx:624–651`：邻行正常透明mesh、depthWrite=false、默认depthTest；`DioramaActiveUnitPlanes.tsx:21–75`：glow/soul/base顺序17/18/19，全部depthTest=false/depthWrite=false，前两层Additive | 保留真实z/平面朝向后可研究文字平面专用合成；普通旋转缩放不能替代透视。当前探针没有近面穿越裁剪、遮挡、背面剔除或多平面合成，不能称深度验收 |
| 距离／雾／色彩 | `dioramaSceneUnits.ts:95`：.9–2近消融／32–40远消失；`DioramaScene.tsx:393–428`：12–30雾、阻尼色；`dioramaSceneFrame.ts:92–140,184+`：邻行／当前单元效果 | life/fit数值已迁；逐像素view-depth雾、线性色彩／additive混色、三种跟唱与envelope尚未实现。单平面均色不能充作最终雾化 |
| 切歌 | `dioramaSequencer.ts:82–148`：有界段、源索引；`dioramaTransition.ts:17–92`：3.2秒/46单位迁移；`CameraRig.tsx:280–359`：飞行弧／bank／aim | 本轮未移植，保持独立调查；不得把其他歌曲历史带成永久缓存 |
| 背景／音频 | `DioramaParticleField.tsx`、`dioramaParticle*`、`dioramaMoteField.ts`；`dioramaSceneFrame.ts:42–85`：粒子回收；`DioramaScene.tsx:430–565`：音频包络／beat跳跃 | 完整背景引擎／音频分析在既定范围外，不移植也不伪造。原相机不由音频驱动；无beat输入保持无beat效果 |

最小结论：文字需要真实三维坐标、透视投影及分层合成，但不因此自动要求引入完整
Three/OpenGL场景引擎。共同Double数学无native依赖；JVM Skia的Compose Matrix能绘制
有透视缩短的真实平面。Android绘制、深度／近面、混色／雾和连续相机仍要逐项验证。
目前没有发现必须越过完整背景边界才能开展文字移植的依据；也尚无完整镜台主题。

3719原函数／Three数值记录，峰差1.3058638615e-9（门槛1e-8）；9个同纹理真实WebGL
与JVM Canvas图，全非背景含低alpha MAE14.518306、边缘盒差最多2px。视觉已检查；
纹理采样／抗锯齿差明确保留，不能将其解释为原版等价。冻结相机不含运镜、揭示、
原React组装；无Android运行或设备性能结论。
证据与复现见 `verification/diorama-projection/README.md`。

文字内核追加：DioramaText.kt保留词身份、UTF16范围及内部每个字素时钟；跨parser边界
的EGC保留所有源区间（ZWJ片段独立分段后可能贡献多个区间）。原对象/嵌套伴唱/
源行边界/字幕保持；无词只派生整句均匀时间。对应来源、原函数矩阵与保留的自编测试
断言错误见verification/diorama-text。没有新增公开主题或绘制结论。
