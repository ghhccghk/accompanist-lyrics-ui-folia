# Windows Diorama sampler 修复与后端 — 2026-11-18

## sampler 限额修复

用户报告 `X4510 maximum ps_5_1 sampler register index (16) exceeded`。
原生产 `DioramaPlainSampler.jvm.kt` 每级F16颜色／ARGB透明度各一child，
11级需22个；该错误不同于上面的pow和未初始化警告。

当前完整链按6个LOD区间加共享端点分组，每pass最多14个child，为宿主clip／合成
留2个位置。每像素只有一个pass负责，跨级双线性／四tap各向异性、线性RGB与独立
alpha、fog／ACES和源坐标保持。重复端点复用同一纹理，不复制像素；额外shader／
draw成本尚未做性能收益测量，不能据此宣称更快。sRGB pow支路显式非负，负值仍走
原线性段；采样变量显式初始化。原版时间／字体／相机公式未改。

图集候选撤回：固定 `latin-800-1.7-0.8.png` 首例45像素差1色阶，未放宽精确比较。
失败源码保存在 `atlas-rejected.patch.gz`（gzip 解压保留精确补丁）；本地原失败图／日志保留于build/verification
及 `.work/diorama-atlas-*.log`。坐标平移浮点量化是候选原因，未单独证明所有差异根因。

```powershell
.\gradlew.bat :library:jvmTest --tests '*DioramaMipPassRenderingTest' --tests '*DioramaMipPassTest' --tests '*DioramaPlainFragmentMatrixTest' --tests '*PublicDioramaDiskCacheTest' --tests '*PublicDioramaTest'
```

11项通过，`.work/diorama-sampler-verified.log`：

- 既有24个真实原版平面条件（380/800/801宽）下，生产分组与原独立control逐像素相同，
  含暂停／乱序／resize／卸载重挂；原fixture／像素容差不变。
- 冻结0d836a5的原生产单pass作为**独立旧实现控制**，1025×513彩色／不同alpha输入，
  LOD 0、3.25、5.999、6、6.001、6.5、7.25、9.9、10逐像素精确。不是原始Three组件。
- 数值0..20级分组：全级覆盖、每个分数LOD唯一归属、插值端点齐全、child<=14。
- 公共入口内存／磁盘、seek／offset／resize／失败／来源回调回归；同时修复快速窗口
  false→true在一轮内合并后就绪计数不恢复，以及sampler未准备完却先报ready的问题。
  原“latest seek wins”失败及就绪超时保留，未替换expected或改标准。
- 早期control清理重复close掩盖像素失败，现清理幂等；精确失败断言只缩短输出，不改判定。

Android库编译通过（`.work/diorama-sampler-ready-fixed.log`），Android仍用普通绘制
路径；不是Android设备／原版完整视觉验收。

## 后端能力依据与实际窗口

锁定Skiko `0.152.0-alpha02` sources JAR SHA256
`0c8d4577b750f2513f96afef8ecc9f52fb52e45803750bb9fffc0b7348929890`。
`jvmMain/org/jetbrains/skiko/SkikoProperties.kt:parseRenderApi` 有DIRECT3D、OPENGL、
ANGLE、METAL和软件分支，**没有VULKAN**；GraphicsApi枚举项不是JVM支持证明。
Windows ARM64的OpenGL被该解析器明确拒绝；平台不适用的后端仍由Skiko拒绝／回退。

专用非focusable真实窗口任务 `:sample:desktop-app:verifyDioramaWindow`，固定Latin字体、
9行自有core歌词、强制磁盘、density1、800/380×600，5个时间／尺寸请求，暂停精确
与卸载检查。分别显式DIRECT3D／OPENGL；输出记录**实际**window.renderApi，
并非仅请求值。结果在 `build/verification/diorama-native-window/{DIRECT3D,OPENGL}`，
原失败的过期准备画面保存在DIRECT3D-stale-recording-rejected及失败日志。
采集修正等待实际尺寸和连续就绪状态；准备label必须不能冒充歌词前景。
这是已显示窗口的GraphicsLayer记录画面，不是OS截图或GPU呈现时间测量。

Windows 10，JDK21.0.5，宿主density1.5。系统枚举RTX4060 Laptop驱动32.0.15.9174、
Intel UHD32.0.101.7085及ToDesk虚拟显示；未识别实际GPU设备，不能宣称某一GPU成绩。
原上游完整视觉、长曲内存与Android运行仍未通过。
