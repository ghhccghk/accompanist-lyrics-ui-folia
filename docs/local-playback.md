# 本地音频与等待状态 — 2026-11-15

用户新增要求覆盖旧设计的默认合成/真实音频延期安排。默认待选歌；开发/演示保留原有
确定性场景和无音频时钟。音频/元数据/文件权限仅在sample，不进入library。

2026-11-18新增内嵌歌词：未选外部文件时读取音频标签，计时文本使用原core parser，
纯文本单独显示；支持字段、优先级、翻译组合和平台验证边界见[内嵌歌词](embedded-lyrics.md)。
下述早期“歌词来自外部文件”的阶段描述不再代表全部读取能力。

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
API30/实机、Desktop不同设备/编码仍需复核。补选已在位加载，不重建播放器、不跳转；
失败/取消/过期结果保留原歌词。会话与UI验证通过，两端构建通过，设备补选尚未验证。
活动态截图selected-lyrics-wide.png固定1500ms，点击跳转边界断言独立保留。
首个真实播放闭环f429141；在位补选4d461b3。日志local-lyrics-attachment*.log。
classic原版对照及残差仍见classic-parity.md；本次不降低其验收标准。

## 等待呼吸球迁移

已迁入PreparedBreathingDots阶段公式和原Canvas逐球绘制；配置仍为3球、16dp、12dp间距，
3000ms入场/3000ms下沉回升/200ms静止/200ms退出。只把原列表起止对齐、折叠高度改为
固定舞台居中；不迁入列表状态或排版引擎。减少动态保留alpha和进度，空间缩放固定1。
这不是classic主歌词呼吸或原版六个点词的替代品。

源测试连续性/边界/无效配置原样迁移；独立测试oracle保留原公式，仅重命名包/类/配置。
9类时长、正反向逐13ms及边界采样，scale/alpha/3个dotAlpha完全相等（maxError=0）。
阶段CSV与18张JVM画面位于library/build/verification/waiting；暂停重绘和逆向重访逐像素相同。
目视7000ms图确认居中3球和逐球明暗；组件验证与下列舞台区间验证分别记录。
模型提交032f689；两端构建通过，日志.work/waiting-model-build.log。
舞台与原生Android接入结果见下一节，阶段记录不代替最终验收。

## 有限前奏/间奏接入

已接入FoliaLyricsView。原库LyricsPlaybackTimeline的严格>5000ms阈值保留；
采用所有非空主唱/伴唱（含嵌套及超出行界的有效片段）的演唱区间并集；相接区间合并。
有限0→首个演唱为前奏，并集之间为空档；负时间、空文档、未知/无效行诊断、末句之后
不推断等待区间。主句余韵/退场或任何仍活动声部存在时隐藏，恢复显示仍用原空档相位，
不重新入场。舞台固定居中，不移植列表高度折叠。移除原先无界的静态“· · ·”。

与歌词共用position-offset；无独立任务/无限动画，reducedMotion只冻结空间缩放。
sample知道真实音频时长，在音频结束时显示“播放结束”，即使导入歌词仍有未来时间戳；
无歌词继续显示“未加载歌词”。库没有音频时长参数，宿主需像示例一样提供结束状态UI。
加载/解析错误不会把无歌词伪装为空档；失败保留旧会话。

验证：20,000次随机时间与全声部区间oracle一致，半开边界/5000和5001ms/
嵌套跨行/尾声/余韵/退场测试通过。公开入口17点正反向、暂停、500ms偏移、
reducedMotion及切空文档通过，PNG在library/build/verification/waiting-stage。
首轮公开测试把25s伴唱刚结束误期望为球，断言发现仍有文字退场；
保留25s“不显示”断言并增加25.301s“显示”边界，没有缩短生产退出或降低避让要求。
日志waiting-stage-build（失败）/waiting-stage-final/waiting-end-guard（通过）。

API34实际运行：normal32/reduced16/offset16次请求，两种视口，均双截图暂停及逆向
一致。8组position+500/offset500与原媒体时间逐像素相同，40s尾声为纯背景；
check-waiting-captures.py复核，证据build/verification/classic-android-api34-waiting-*。
首次被系统全屏教学遮挡的图保留并拒绝；确认界面后重采，不修改设备动画设置或验收条件。
真实MediaPlayer同时验证在位补选不换player、不改变暂停快照/seek版本，12秒WAV
时长正确，游标713/暂停716/seek6000/恢复6372/逆向1274ms；
android34-attached/paused.png实际展示1.276s有限前奏球。

两端构建、sample JVM及相关library测试通过；未声称人工听音、原生系统选文件完整
端到端、API30本项新代码、API21..29或实机验收。Desktop系统UI工具初始化仍报
failed to write kernel assets (os error 3)，未用自写Windows自动化绕过；
Java Sound真音频与Compose离屏UI证据仍分别有效。

## 原生选择流程复核与本项收尾

有限空档接入提交1a0e9cd。之后实际操作API34 MainActivity与Android DocumentsUI，
没有通过debug输入代替选择：首次页→选择音频→Downloads的原创WAV→确认，出现
推进的0:02/0:12、暂停按钮、文件名标题/未知作者/占位封面，右侧“未加载歌词”。
随后补选歌词和独立翻译两个LRC，应用时仍在12s暂停；拖动真实进度到6s，
Amber/琥珀正确显示。再重新选歌→打开系统音频选择→返回→取消：原曲、暂停6s、
歌词/翻译保留。XML、系统截图、APK/输入SHA在build/verification/local-audio/android34-picker。
纯会话测试覆盖竞争/失败原子替换，Android原生连续切歌压力尚未实际运行。

复核使用当前构建，sample/library相关测试及Android/Desktop构建再次通过
（.work/waiting-final-retest.log）。没有依赖Rust/SPW或原工程。Desktop系统选择器受
上述工具初始化阻碍，未声称本项跨两端原生UI均验收完成。原生Android无声输出的
游标测试和真实选择路径是独立证据，不等价于人工听音；API21..29/实机仍待验证。

本项有界实现完成，不继续扩展sample。下一项恢复classic整帧字形覆盖/采样残差，
优先Android小数字号与复杂字幕；固定源码/字体/视口逐项数值和视觉比较，标准不变。
