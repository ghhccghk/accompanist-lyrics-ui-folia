# Diorama 会话纹理缓存 — 2026-11-18

独立内存驻留适配，不修改原版相机／字素／时间公式。源基线仍为
folia-major v0.7.26 / cc6226e464050a456a2913e062374f23d3844cc5。

## 可复现验证

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaRasterCacheTest' --tests '*PublicDioramaDiskCacheTest' --tests '*PublicDioramaTest' --tests '*DioramaPlainRendererTest' --tests '*DioramaPreparationCancellationTest' :sample:shared:jvmTest --tests '*SettingsTest' :library:compileAndroidMain :sample:shared:compileAndroidMain :sample:desktop-app:compileKotlin
```

- 公共入口：9行自有合成歌词、固定字体、800/380宽视口，强制溢出到临时文件；
  与全内存路径在1600/3600/15600/7600ms及倒序逐像素一致。
- 暂停、offset一次、来源对象点击、尺寸恢复、快速seek最终请求获胜、卸载清理。
  测试磁盘读写断言不在AWT线程，读盘没有混进Canvas／命中。
- 原生预乘像素：7种颜色／全部alpha byte精确roundtrip；40张图总payload327680字节，
  驻留目标24576字节，验证转存／预取复用、取消不发布、乱序seek。
- 损坏、磁盘上限、无效目录、可见失败与关闭缓存恢复；只删除本会话自有文件。
- 设置持久化和缺失／非法值fallback；Android库／shared与Desktop编译。

本地日志：`.work/diorama-raster-cache-final.log`；先前focused与组合日志保留。
首次公共测试误清空了offset等价时间的status，等待不存在的新事件而超时；
修正测试的事件等待，未改像素比较、生产时间或失败标准。

生成画面在 `build/verification/diorama-disk-cache/cached-{800,380}.png`，可由测试重建；
已图检就绪的真实三层透视文字。380案例测试根窗口仍800宽，右侧留白不是舞台布局。

## 限制

这是JVM离屏路径／逻辑与Android编译验证，不是上游正式视觉、真实窗口、
Android设备或性能结论。32MiB是ARGB驻留软目标，不包括sampler/F16/驱动/相机、
临时准备和替换时旧窗口；没有PSS或帧率收益测量。512MiB临时存储上限；正常
卸载清理，进程强杀后遗留清理待做。整曲文本几何／相机节点仍留在内存；
内部多走廊切换探针尚不支持共享此缓存，公共组件每次切歌建立独立会话。
