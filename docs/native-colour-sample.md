# Rust 小半径颜色卷积开关

2026-11-16。已接入sample真实绘制链，默认关闭，独立于已有“实验 Rust 辉光遮罩”。
数学实验提交8362122，Android进程调查8cbbebb。不是完整native渲染器或整会话提速验收。

## 使用

Android已生成 `build/outputs/folia-sample-native-colour-debug.apk`。设置页在真实歌曲和开发演示中
都有“实验 Rust 颜色卷积”。开启前先保持渲染分辨率100%、遮罩开关不变，以免混淆变量。
播放同一固定片段，分别关闭/开启；界面显示加载、自检、实际调用次数或具体回退原因。
只“已加载”不能当作该帧已用Rust。新会话默认Kotlin，不持久化实验开关。

构建/运行：

```powershell
./gradlew.bat :sample:android-app:assembleDebug
./gradlew.bat :sample:desktop-app:run
```

sample沿用现有native打包脚本，Android含arm64-v8a/x86_64，Windows含DLL。
`-PfoliaExperimentalMask=false`可排除整个实验动态库；两个开关均会明确报告不可用并保持Kotlin。
Linux/macOS已有平台构建脚本，但本轮没有对应主机运行验证，不声明已通过。

## 实际替换范围

- FoliaLyricsView参数不变。新增可选实验边界`ExperimentalFoliaColourProcessor`及scoped local，
  默认null。Compose层只把有限小sigma卷积交给processor，shaping、计时、舞台及平台图层保持原路径。
- `ClassicLayer`记录软件树时传入processor；`SoftwareBlur`先按原规则裁剪，再生成相同Double系数。
  仅sigma<2进入批量JNI，较大半径box覆盖、原生BlurEffect及透明层剔除保持原路径。
- 原色、灰色和白色仍按真实像素分支；预乘、Double累加顺序、有限边缘、反预乘和舍入相同。
  输入/输出各复制一次，工作区同步复用，Rust不保留Java地址或创建线程/计时器。
- 切换重建录制层，不重置播放器、预览时钟或discontinuity。所有动画仍由原媒体快照驱动。
  关闭/组件卸载释放工作区引用；不宣称direct内存立即归还OS。
- 颜色工作区持有上限32MiB，超预算/非法输入/JNI错误明确回退；与遮罩工作区**分别计费**，
  同时开启两者可能多持有约64MiB，再加像素数组/位图/GPU资源。不是内存优化开关。
- 独立颜色ABI version和真实启动自检；旧库没有入口时显示LinkageError，不显示为已启用。
  默认Kotlin发布库不捆绑/加载native。

## 验证及限制

证据在`verification/playback-stutter/native-colour`及`runtime/`：

- 8362122实验使用当前优化后的Kotlin基线，216输入+108测量前后比较/轮，两平台各两轮。
  Android大图局部收益及完整含退化项的数据在README/CSV；不是单纯语言优劣结论。
- 真实sample processor在arm64/API37独立app_process中216组ARGB/宽高/原点严格一致，
  新sample JNI入口、预算超限可见回退及release通过。**未安装/停止用户播放器，未验证Android设置UI**。
  此探针显式System.load临时库，不冒充APK应用内加载器验证。
- JVM实际sample loader/ABI/自检、空java.library.path的打包JAR、三个开关/释放循环通过。
  缺库设置页显示Kotlin回退；颜色开关不改变遮罩开关和播放位置。
- 固定Latin授权字体44sp、800/380×600、流光/云阶各6时间点共24个整帧对照严格相同；
  两侧强制软件树。每主题两种宽度950ms均有真实颜色JNI调用，PNG与数值记录保留。
  暂停、反向seek、切回、resize和错误后Kotlin画面恢复通过；不是新一轮上游浏览器效果验收。
- Android/JVM编译、选择性逻辑/像素测试、APK签名v1/v2、两个ABI的颜色/遮罩导出及16KiB
  LOAD对齐通过，详情/包hash见runtime/checks.json。签名工具的AGP metadata条目警告保留在原日志，
  未删除条目来压制。Android x86_64设备、Linux/macOS和真实歌曲受控A/B仍未验证。

下一项：同设备同歌同片段、固定画质/100%分辨率，仅切换颜色卷积，记录帧分布、
真实JNI调用、CPU及总内存。继续处理同步准备/嵌套软件绘制；不以局部内核提速宣布卡顿解决。
播放独立进程的可行性、官方约束及未实施项见android-playback-process.md。
