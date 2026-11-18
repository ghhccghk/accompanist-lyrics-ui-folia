# 示例后端与帧回调诊断 — 2026-11-18

## 使用

真实歌曲和“开发／演示”均可在 **设置 → 调试信息（后端／FPS）** 开启。
默认关闭、仅当前页面会话保留；面板可直接关闭。不改变媒体位置或启动播放。
失焦／不可见时停止统计，恢复可见重新采样；卸载移除监听。

显示实际／请求后端、窗口像素尺寸与 density/fontScale、主题、分辨率、画质、
准备单元和库诊断、媒体位置／暂停／discontinuity；最近两秒回调频率、耗时均值／
P95／最大值、回调间隔 P95 和丢失报告。最多保留360条 primitive 数据，每500ms
汇总刷新。没有另外启动渲染动画；面板自身更新也会产生绘制并计入统计。

**这些不是 GPU 呈现 FPS／GPU 耗时，也不是设备性能验收。**

- Desktop 包装当前 ComposeWindow 的实际 Skiko `onRender`，计 CPU 场景记录耗时，
  不含随后 native submit/present。恢复原 delegate；其他宿主替换 delegate 后不覆盖它。
  实际后端来自 `window.renderApi`，不是请求字符串。
- Android API24+ 使用 `Window.OnFrameMetricsAvailableListener` 的真实窗口报告，
  `TOTAL_DURATION` 为窗口帧指标；频率按回调到达时间计算，可能受消息分发影响。
  lostReports 是 Android 报告丢失数，不是推算掉帧数。
  只报告 HWUI／软件 Canvas；公共 API 未公开实际 GLES/Vulkan，明确显示未知。
  API23显示帧指标不可用；API24类型隔离在独立类中，未经过API23设备运行。

Windows 可显式测试 OpenGL，默认 AUTO 保留 Skiko 选择：

```powershell
.\gradlew.bat :sample:desktop-app:run -PfoliaRenderApi=OPENGL
.\gradlew.bat :sample:desktop-app:run -PfoliaRenderApi=DIRECT3D
```

打包任务也可传相同参数，写入 sample 启动 JVM 参数。锁定 Skiko JVM parser 没有
VULKAN 分支，见[精确来源与 sampler 修复](../windows-rendering/README.md)。
`-PfoliaRenderApi=VULKAN` 已验证明确失败，不静默退回默认。
ANGLE／METAL／软件值按 parser 接受并交由 Skiko 检查平台；本轮没有运行这些后端。

## 可复现验证

```powershell
.\gradlew.bat :sample:shared:jvmTest --tests '*RenderFrameWindowTest' --tests '*RenderDebugUiTest' :sample:shared:compileAndroidMain
.\gradlew.bat :sample:desktop-app:verifyRenderDebugWindow -PfoliaRenderApi=DIRECT3D
.\gradlew.bat :sample:desktop-app:verifyRenderDebugWindow -PfoliaRenderApi=OPENGL
.\gradlew.bat :sample:desktop-app:verifyDioramaWindow -PfoliaRenderApi=DIRECT3D
.\gradlew.bat :sample:desktop-app:verifyDioramaWindow -PfoliaRenderApi=OPENGL
```

- 两项确定性统计测试：60Hz输入／idle／上限／乱序／慢帧分位数／丢失报告／reset。
- 两项离屏UI：演示900宽与模拟已选音频420宽，默认关闭、整行开关、直接关闭、
  不可见停用／恢复、卸载释放，不改变两种媒体时钟。使用明确的 TEST_ONLY 探针，
  **不把模拟后端数值当实际渲染或真实音频测试**。
- 独立非聚焦真实窗口分别确认实际 DIRECT3D 和 OPENGL、回调数量和耗时非零、
  关闭恢复 delegate／停止采样、重开清旧数据、保留宿主后来设置的 delegate。
  自有 Canvas 输入变化仅为探针刺激，不由统计组件产生，不能作歌曲帧率基准。
- 实际窗口输出 `build/verification/render-debug/{DIRECT3D,OPENGL}/checks.txt`。
  UI截图 `build/verification/render-debug/demo-debug-ui-test-host.png`，仅作布局证据。
  同机环境和镜台真实窗口像素验证见 windows-rendering；OpenGL就绪文字已图检。
- Android shared 编译通过；本轮没有安装／设备／FrameMetrics 运行／GPU 或总 PSS 结论。

首次UI验证失败保留在 `.work/render-debug-validation.log` 和
`.work/render-debug-ui-diagnosis.log`：旧通用开关标签点击不触发 Switch，改为专用
可访问整行 toggle；模拟播放器在候选启动就抛错，修正夹具正常准备后验证无新增播放。
未关闭断言或改变统计预期。首次图检还发现半透明 Surface 未显式指定前景而出现暗字，
已指定主题 onSurface 并重验截图。通过日志见 `.work/render-debug-readable.log`、
`.work/render-debug-final-d3d.log` 和 `.work/render-debug-final-opengl.log`。
