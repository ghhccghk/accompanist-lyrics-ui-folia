# 本地音频与等待状态 — 2026-11-15

用户新增要求覆盖旧设计的默认合成/真实音频延期安排。默认待选歌；开发/演示保留原有
确定性场景和无音频时钟。音频/元数据/文件权限仅在sample，不进入library。

## 当前目标与完成条件

1. 待选歌、三文件对话框、平台实际音频最小闭环，成功才交换会话。
2. UTF-8计时歌词和同时间戳翻译组合、补选，失败/取消保留原会话，过期候选释放。
3. 单向迁入原库呼吸球阶段模型/绘制，记录Apache与注释中feature-text-engine来源。
4. 全部声部演唱区间并集的有限前奏/间奏；原库阈值严格大于5000ms。
   与文字退场避让，暂停/seek/偏移/减少动态/歌曲尾端验证。
5. 两端运行、设备音频游标、输出资源释放与使用说明。完成本次调整即回classic核心对照。

依赖关系调整：公共选择/事务与两平台最小适配共同形成首个可运行功能，不提交假播放器
或无效的确认按钮。随后将补选/竞争恢复、呼吸模型和舞台接入分别验证提交。

## 平台选择

Android原示例使用Media3服务；本示例使用系统MediaPlayer，无后台服务/队列/外部跟随。
Desktop原示例以外部播放器跟随为主，不假定有相同本地能力。本示例使用JDK Java Sound，
首批PCM WAV/AIFF/AU；不支持的编码明确报错，不回退无声时钟。解码PCM限制128MiB，
没有引入完整播放器框架。Android使用系统支持的本地编码。真实播放暂不提供倍速/循环
控件，演示入口保留原有能力；不把不支持的控制显示成已启用。

快照直接采样真实播放器，不外推、不累计帧时间、不重复乘倍速。暂停/缓冲/异步seek
时报告冻结或实际游标；切歌与seek由sample桥接单调discontinuity。可见性丢失暂停，
回到前台明确恢复；关闭释放资源，不扩展系统音频服务或后台播放。

## 验证和待办

首个闭环已实现：默认待选歌/三文件选择、成功才替换、两平台真实位置快照与单调
discontinuity、播放/暂停/滑条/歌词点击/重新选择。文件选择器短暂失焦暂停后，只恢复
同一原会话此前的播放状态；普通后台/失焦不自动恢复。元数据缺失有占位，Android
读取内嵌封面并限制解码大小；Desktop仅展示JDK可获得的title/author，封面为占位。

两端构建和sample JVM测试通过，首轮布局作用域编译错误和空会话持续请求帧已修复，
保留失败日志local-audio-build/local-audio-tests。原布局测试显式进入demo模式，新增
默认选择/取消UI测试及候选失败、取消、竞争、资源关闭、跨歌/seek token和翻译测试。
不是用测试播放器替代生产适配：单独的verifyLocalAudio实际打开Java Sound Clip，
12秒原创440Hz低音量PCM WAV，游标610ms、暂停650ms稳定、暂停seek5999ms、
恢复6369ms、逆向1280ms，finally关闭。实际设备/系统格式不支持时该任务报错，不跳过。
原生Android debug probe也实际解码同一WAV：API34时长12000ms，游标612ms，
暂停613ms严格稳定，seek6000ms完成、恢复6330ms、逆向1254ms；截图确认无歌词
显示“未加载歌词”，不是间奏。未声称人工听音或物理输出回录验证。

证据build/verification/local-audio（JVM离屏UI、Desktop真实音频设备、Android系统
截图/游标/输入与APK及源码SHA分开记录）；日志.work/local-audio-*.log。
复现：python verification/generate-local-audio.py .work/local-audio/synthetic.wav；
:sample:shared:verifyLocalAudio --args='.work/local-audio/synthetic.wav build/verification/local-audio/desktop-native.txt'；
python verification/capture-local-audio.py --serial <设备> --adb <adb>。
原生文件选择器尚未自动操作验收；Compose对话框/取消测试和真实解码是分开的证据。
API30/实机、Desktop不同设备/编码仍需复核。补选当前会重新准备歌曲，后续改为在位
加载并保留当前位置/播放状态；呼吸球尚未迁入，不把本项称为全部新需求完成。
classic原版对照及残差仍见classic-parity.md；本次不降低其验收标准。
