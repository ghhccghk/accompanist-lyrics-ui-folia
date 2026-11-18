# 后续实施清单 — 2026-11-15

## 唯一活动清单 — 2026-11-18

| 主题 | 内部内核 | 公开校准预览 | 正式视觉验收 |
| --- | --- | --- | --- |
| Classic / Partita / Tilt / Cadenza | 已接入 | 可从 sample 选择 | 未完成，保留各自残差 |
| Pendolo | core桥接、轮盘、扫描、字幕、副歌/交互 | sample已可选校准预览 | 未完成，密集tick/旋转/栅格差待校准 |
| Cappella | core/消息/揭示/动态presence舞台 | sample已可选文字对话校准预览 | 密集布局/光栅/设备未最终验收 |
| Fume | 实际core/文章/揭示/可取消镜头准备 | sample已可选文章镜头校准预览 | 短桥接/总览/光栅及完整生命周期未最终验收 |
| Diorama | core/完整文字透视/三层、13目标、follower/原picker、裁剪/邻行/life/fog、incoming flight/同走廊loop/有界双走廊plain媒体重建 | 无 | 完整原挂载/连续残差、depth/栅格/整曲驻留/设备尚未验收 |

8e58a1c已完成插入任务（示例主题选项）：七个实际公开主题使用自适应换行选择项，
显示中英文名、选中语义与统一校准预览提示；镜台仅说明内部验证，不开放假入口。
320/900宽度逐项选择、原播放状态不变和原三主题入口/设置17项通过；Android shared/
Desktop编译通过，离屏截图已图检。不是Android运行或主题视觉验收，见
verification/theme-options。此项不改变主题能力或偏好存储键；现继续下面唯一下一项。

当前唯一下一项：对原CJK800/1100ms的BASE做原/自身texture×原/自身placement输入消融，
区分canonical分数phase/ceil pivot与采样，保留完整shaping；不调halo/相机或改前景口径。
原CJK失败层已隔离：同原完整PNG/21mesh输入/版本不变，clone-all精确，释放几何/纹理0；
native分层重放等于两条实际完整stage路径。旧失败tile(384,320)固定573union内all=base，
所有glow/soul均纯背景；局部3.979058→5.198953属于未唱正文，不能调halo掩盖。
1项JVM/暂停/无重复准备通过，见verification/diorama-cjk-layer-isolation；无生产/设备结论。
13b8518最新debug已构建，原证书v1/v2/CRC/16KB对齐/双ABI Rust和复制hash通过：
build/deliverables/android/lyrics-ui-folia-debug-13b8518-20261118-171753.apk，SHA256
c41734a49247f091756492c6408f1d84ca2ce894c5d029b2872f1dd0032f1ba4。未安装/传输，
见verification/android-debug/2026-11-18-rtl-registration-validation.json；独立本地Maven
JVM绘制/回调与Android消费编译通过，verification/consumer/13b8518-validation.json。
RTL安全独立词原始右边界注册已修：原pivot差.656036/.720062px，按原ceil/prefix公式
现为0；仍画同一完整paragraph，跨单元cluster/混合bidi保留canonical。更广泛全词候选
虽均值更好但10个Latin局部回退，完整补丁/失败保留并撤回。最终同90七图union普通
6.578335→5.735074、sample3.422528→2.344873/局部33.035714→8.970537，均无新均值/
局部回退；180完整层普通4.438836→3.899779亦0回退。完整sample对旧ordinary有12个
CJK局部残差，旧sample180未采，不伪称是RTL前后回退。353 mounted/unsafe PNG不变，
12项JVM/Android库/Desktop编译通过，见verification/diorama-rtl-registration。无正式视觉/
设备验收；700ms四帧/媒体相机/Android采样/整曲内存仍待解。
8ee9a80交付已刷新：Android debug原证书v1/v2(37.0.0/30.0.2)、ZIP CRC/16KB对齐、
双ABI Rust ELF/副本hash通过；build/deliverables/android/
lyrics-ui-folia-debug-8ee9a80-20261118-170240.apk，SHA256
54fa875bf90e98ae684295ebbee7931de05f017710344756877210bd43ffe8fc。未传输/安装，
手机验签问题未复现；见verification/android-debug/2026-11-18-active-base-validation.json。
独立本地Maven JVM绘制/回调1项与Android消费编译/归属边界通过，
见verification/consumer/8ee9a80-validation.json。两项用户签名/daemon遗留hash不变；
无设备/真实窗口/性能或公开镜台结论，不重新无故跑同矩阵。
plain owner异常后卸载已修：原生产1项回归先失败，成功A/失败B后空request曾跳过淘汰；
改用null表示未完成请求，卸载count/bytes归零、原异常透传，5项JVM/Android库/Desktop
编译通过，见verification/diorama-owner-release。无shader/视觉/设备/性能结论。
活动BASE采样已接内部可选owner：当前行<=32项/32MiB额外payload估算，帧外准备/释放，
Android明确不可用/真实旧路径回退。原最坏活动层同813union MAE19.670767→1.308733，
峰74→22；固定48同7图union有限4.273842→1.961853，但700ms四帧均值/局部回退保留；
媒体18.583225→17.276814/局部69.117781未降。自身90完整塑形均值6.580503→3.423427，
16Arabic局部回退(峰26.238095→33.035714)未解决；原输入90诊断均值.445855/峰8/
局部1.8125，非全GPU等价。16项JVM/Android库/Desktop编译通过，567旧PNG不变；
见verification/diorama-active-base-sampler。无公开镜台/设备/正式视觉结论，不用均值掩盖失败。
最坏区域分层已完成：原1494次精确callback前缀、完整图/状态/纹理版本/matrix均不变；
原active-all=base-only，glow/soul均不可见，4正文opacity=.5。活动层原808/本418像素，
同union MAE19.792492/峰74；该口径不等于完整stage。既有Windows前景策略只改善unit0，
另3均值回退，未采用；完整shaping/分数offset/差1px记录保留。2项JVM/暂停通过、
原卸载roots/RAF=0，见verification/diorama-mounted-active-input。无生产/设备/正式视觉结论。
内部邻行stage采样已接入显式owner：帧外准备，<=8项/32MiB额外图像估算预算、
淘汰/卸载释放、保留共享原raster，父alpha/FrontSide/near/far及linear tint顺序。
JVM opt-in实际stage同48帧有限均值6.739673→4.294389(最坏仍20.364780)、媒体
19.922429→18.684469(局部72.258224→69.117781)，均无均值/局部回退；24自身
plane与独立控制逐像素相同，168普通基线hash不变。11项JVM/Android库/Desktop
编译通过；见verification/diorama-plain-sampler。Android明确不可用并真实回退，
无设备/性能/完整视觉通过，Diorama仍未开放。
bb84afd的APK刷新与独立消费已完成：本地Maven JVM绘制/来源回调及Android消费编译/
归属边界通过，见verification/consumer/bb84afd-validation.json。新debug交付
build/deliverables/android/lyrics-ui-folia-debug-bb84afd-20261118-163142.apk，SHA256
36edda06c3213ed04d217c9804710dc362bb82b27061900f9ab418c962cb636b；原证书v1/v2、
ZIP CRC、16KB对齐、双ABI Rust及复制hash核对通过，未安装/传输或复现手机报错。
见verification/android-debug/2026-11-18-plain-sampler-validation.json。Android仍旧
普通绘制，七主题与用户签名/daemon遗留未改；不因重新构建宣称手机验签问题解决。
自身完整塑形24帧控制已完成：同原Latin/Arabic/CJK/长行及380/800/801、两pose，
同union均值11.742677→1.446805/最坏64局部50.719298→3.363636，24帧无均值或
局部回退，近景Latin/Arabic不再重现被拒候选的模糊。4项JVM/实际Canvas尺寸恢复、
乱序view/卸载资源/重挂/暂停精确；原图/旧局部控制/168生产hash不变，见
verification/diorama-plain-fragment。仅测试控制，非生产/设备/正式视觉通过。
此前同700ms/cache7的默认aniso4控制已完成：同520像素union原版峰14→2、
MAE2.548718→.296795，原409/本390覆盖及残差保留，非完全像素相等。
独立inverse-Jacobian/area/longest-axis四tap，无LOD拟合；SwiftShader参考revision
未证明匹配当前浏览器驱动，不泛化等价。原isotropic哈希及168生产图不变，暂停/
sampler往返精确、narrow通过；见verification/diorama-anisotropic-control。
仅局部JVM测试控制，无生产/Android/完整主题验收；不重跑同一源单帧。
有界JVM RuntimeEffect独立linear RGB/alpha/逐fragment LOD→tint→ACES→显式sRGB
→fog→blend控制已完成：同原isotropic诊断通道峰2/MAE约.326，独立真实GPU mip
scalar oracle峰2；原aniso4仍峰14。首轮上下文OETF亮度失败11802mass与通道定位
保留，修显式原OETF后20570(原isotropic20614)，无拟合/改原图。见
verification/diorama-fragment-control。仅共享输入局部JVM控制，未进入生产/Android，
非原默认/正式视觉通过；全部168生产图不变，暂停/诊断往返确定，资源测试内关闭。
实际GPU mip0/4/5已被动读取：原PNG/状态/452callback/纹理及Source版本不变；
base RGBA与既有cache7逐字节同。mip4/5 alpha本与原峰1，但mask统一染色丢RGB，
MAE70.344595/102.155689；独立linear-F16 occupancy RGB峰1，包含alpha0的48/12
非零RGB分量。原小mip共7196bytes保留，见verification/diorama-mounted-mips。
仅原始纹理通道数值控制/1项JVM通过，非完整stage/设备/性能或正式视觉验收。
显式isotropic center-LOD候选已拒绝并撤回：虽mounted48均值改善，plain24有7均值/
6局部回退、近景Latin/Arabic可见模糊，不能用均值掩盖；全部72原图不变，完整候选
补丁/失败/同union数值见verification/diorama-minification。候选14测试/Android库/
Desktop编译通过不代表视觉通过；撤回后5项及168生产图hash恢复。无该候选产品改动。
原克隆采样消融已完成：原aniso4=409像素，关mip=194、native=193；同513union
关mip对native MAE.400260，原10.972060，trilinear/aniso1则512像素。证明该场景
mip采样是覆盖扩散贡献，非全部根因或等价证明；原完整图/状态/452callback均不变。
证据verification/diorama-mounted-sampling；修改clone诊断图不替换原验收帧。
实际原mesh投影控制已完成：452callback/完整PNG/状态未变，4角本独立core几何
世界峰4.055e-9/投影1.483e-7px/深度1.617e-9，homography峰5.684e-14px。
不能解释193/409覆盖差。原texture确为trilinear/aniso4，MSAA4；见
verification/diorama-mounted-geometry，非全矩阵/设备/视觉通过。
上一项strict/mipmap假设已做有界反证：公开Bitmap shader经Compose绘制，no-mip
与生产逐像素相同，trilinear仅23分量变化且MAE13.762021→13.762836微退步。
候选完整补丁与结果保存verification/diorama-sampling-shader并已精确反向撤回；
撤回后narrow通过、原生产/控制图hash不变。未证明strict或mipmap是根因，无产品收益。
同输入控制已完成：own-texture重放与生产stage逐像素相同；源完整图/状态在分层
clone渲染后仍同原hash。源实际邻行7=409非背景像素，本=193；共享源cache7后
局部MAE13.705786→13.762021，覆盖输入不足以解释残差，见verification/diorama-mounted-input。
只保留35KB合成源测试纹理，不进入生产；数据/原失败均保留，不称图形后端等价。
c3e464c已完成真实cache452callback被动读取/整屏hash和8张原几何控制；随后
Windows邻行覆盖采用既有前景栅格策略，raw MAE3.784223→1.606260/峰130–133→20。
同原72图：plain24帧均值全部改善(13.079068→11.776553)，仍11局部回退；mounted
有限34改善/2回退/12不变，媒体24/12/12、局部峰82.267296→82.462264，全部保留。
相机/材料数值门槛未变，仍非正式视觉验收。证据verification/diorama-plain-coverage。
raw候选探索为历史，保留verification/diorama-mounted-raster，不能以新输出覆盖旧报告。
46f89e7新增Android debug交付已构建/v1/v2/ZIP/16KB/双ABI Rust核对，未安装；
见verification/android-debug/2026-11-18-plain-coverage-validation.json。Android语义未因
Windows栅格修正改变，无新设备/性能结论；旧被占用APK未覆盖，两个外部遗留未提交。
本轮交付构建/公共入口/独立消费已完成：325c2bd下公开JVM38项与新版选择器2项
通过；Android debug APK构建、apksigner v1/v2、arm64-v8a/x86_64真实ELF库核对通过。
APK在sample/android-app/build/outputs/apk/debug/android-app-debug.apk，未安装，
见verification/android-debug/2026-11-18-validation.json。两项外部签名/daemon配置
保留并排除提交。独立本地Maven JVM绘制/回调、Android消费编译及归属/依赖边界
通过，见verification/consumer/325c2bd-validation.json；无远程发布或新设备结论。
700ms采样线索已做一次有界反证：原CanvasTexture确用anisotropy4/mipmap，显式
Medium hint候选在48mounted原矩阵中144本PNG全部字节不变，无收益，已逐行撤回；
补丁/完整48对照保留verification/diorama-filtering。不宣称GPU等价或采样无影响，
不反复重试同hint；不能用此排除文字输入/投影误差。
325c2bd媒体域辉光呼吸为历史下一项，已实现：保留原.9+.1*sin(phase*1.9)，phase采用
media-offset与不可变cut/loop锚点，暂停冻结/乱序seek/resize/重挂恢复，reduced恒1。
同48原图五图union有限MAE6.954943→6.887058(28改善/0回退/20不变)，媒体
20.290248→20.258454(22改善/6回退/20不变，7帧局部回退全部保留)；局部峰仍
20.434397/83.4，不称wall/RAF或视觉等价。21项定向JVM/Android库/Desktop编译
通过；610旧direct及48有限材质PNG hash不变；见verification/diorama-breath。
用户新增交付要求：修改后刷新Android debug APK并检查，提供路径；不安装。
b85e4ce同原mounted有限材质控制为历史下一项：44实际原分组/内部时钟一致，192原材料状态
峰7.21e-12/1e-9，Float32包络零差；独立own-core重放4686区间，无原状态驱动绘制。
四图同union有限解析/有限历史MAE6.937855→6.817019，局部同20.434397，30改善/
6回退/12不变全部保留；生产96及旧direct610 PNG哈希不变。证据
verification/diorama-mounted-materials，不是媒体相机/完整文字/设备视觉验收。
c75652d已修ACES历史下一项：原Three shader960控制不透明峰147→0(门槛1)、混合/fog
峰2(门槛2)；首修HDR提前截断峰106失败保留，修运算顺序而非放宽。原48PNG和
4686 camera数值未变，610旧直接Three-policy PNG hash全不变；25定向JVM/
Android库/Desktop编译及本地独立Maven JVM绘制/Android消费与归属打包通过。
同五图union有限MAE7.251753→6.954840/局部24.617612→20.434397，媒体
20.348141→20.289881/局部87.853968→83.4；有限26/媒体27帧均值回退如实保留，
不是原版视觉通过。证据verification/diorama-tonemapping，镜台继续内部；无新设备/
真实窗口/音频/性能证据。原直接Three NoToneMapping oracle未改，未加入固定延迟。
原挂载CameraRig+文字层为历史下一项，已完成：真实原React/R3F两组件、default
shot picker、原Float32 envelope/呼吸/fog/缓存；显式host控制ready edge/媒体/wall/
manual advance及cache RAF，未改source refs/算法。4686实际区间与提取算子峰0，
本独立core+字形+有限历史峰6.04e-14/1e-9、宽度峰8.88e-16；原组件卸载root/RAF全0。
同union前景均值有限7.251753/媒体20.348141，局部24.617612/87.853968，媒体位置
残差.130588world，触边36/38/38，无空帧；仍未正式视觉/设备/真实窗口验收。
实际callback先camera再text，切句读上一活动宽；生产重建未人为插入一帧延迟。
截至175dabe实际R3F toneMapping=ACES(4)，旧直接Three为NoToneMapping；当时材质缺项已由上节补齐。
原gate/timeout/三走廊/字体及device仍缺。证据verification/diorama-mounted；仅JVM
新两项通过，生产未改；reference依赖按原锁校核并限定于verification，无库依赖。
跨曲plain文字窗口为历史下一项，已完成<=6旧plain共享快照/全局索引/原归属及
中心深度排序，不保留旧core/active raster/整曲playback/job，不递归保留旧快照。
48原完整文字帧/244材质：有限pose峰1.42e-14、材质峰2.84e-14(门槛1e-9)；
同union前景均值有限6.331865/媒体20.047587、局部23.651852/97.018091，媒体含
outgoing历史位置残差.110025world，触边41/41/40、无空帧，仍未正式视觉验收。
原组件完整挂载/三走廊打断/晚到歌词/换字体等尚未实现；三走廊明确拒绝而非吞掉。
暂停/乱序seek/offset/reduced/尺寸恢复/重挂/取消通过；Android库/Desktop编译和
公共三主题及loop/core/offset定向回归通过，80旧loop native PNG hash不变。
首轮3.2s旧尾窗口测试假设被独立原表否定，失败保存；不改原表/原图。
证据verification/diorama-cut-stage；无新设备/真实窗口/音频/性能结论。
loop回程完整文字对照已补，后续不再重复该40帧；跨曲采用同样的固定方法。
同走廊loop为历史下一项，4a69b2a已完成：原32连续有限控制峰8.74e-13、26窗口/
436材质峰1.42e-14、真实core原对象/文字/raster/世界帧复用、取消/媒体重建通过；
outgoing窗口与flight同一媒体时间，在3.2s精确结束。15定向JVM/Android库/Desktop
编译通过。实际native contact仍有强放大/裁切/重叠，该提交时无完整原loop光栅结论，
见verification/diorama-loop；显式loop edge与任意顺序seek区分，无新设备/窗口。
随后新增40帧原完整文字层对照，原版同样有回程强放大/裁切；本有限相机峰1.42e-14，
同union前景均值有限4.765380/媒体14.574012，最坏局部36.963333/80.101010，
触边24/23/22不完全相同，媒体含outgoing历史位置残差.066822world，未正式验收。
原文字函数+有限camera组合，非mounted React/RAF包络；见verification/diorama-loop-stage。
4a69b2a当前产物独立Maven消费JVM绘制/回调、Android编译/边界/归属打包通过，
证据verification/consumer/4a69b2a-validation.json；仅本地，不是设备/窗口/性能结论。
此前incoming flight为历史下一项，486b8c1已完成：480原同初态有限控制峰1.42e-14、
96真实core原Three/本有限/本媒体文字帧，另67连续观察且原8关键帧及step完全不变。
同矩阵前景均值有限3.955025/媒体12.366458，媒体位置残差.035038world；
21/21/26空帧、47/47/42触边均保留，不称原版视觉验收。12定向JVM/Android库/
Desktop编译通过；暂停/乱序seek/offset/resize/reduced/重挂通过，无新设备或窗口。
证据verification/diorama-flight；截至486b8c1完整outgoing corridor/loop/late-lyrics gate未实现。
正常有限1482原控制通过；de7baa6旧normal图表因不是当前基线仅120/288hash匹配，
失败原样记录，不将此称无变化，亦不改旧报告/期望。详该README/失败日志。
loop回归另发现实际2100ms行在media2300/offset200时因秒浮点相减误选前行；
f7c2ecc按core毫秒单位换算，无round/epsilon/帧延迟，十组正负offset精确边界及前后
0.1µs/完整文字像素/点击逆映射通过。证据verification/diorama-offset新增边界节；
旧loop失败保留。loop当前结果以verification/diorama-loop为准，不覆盖原失败记录。
群唱当前局部探针已收束：159条原target/live-value/DOM记录及完整独立重复一致；
1.016667s目标110→49后保持49，inline却110→49→110、MV一直110，证伪再次切target。
原v2所有phase值不变。媒体重建尚缺可成立的瞬时DOM写入/解析交接策略，
22.188593px未修，不加入固定帧延迟或再试freeze；证据cappella-projection/frame-targets-v3。
浮名有限限幅位移的媒体重建亦仍未成立，原同初态/frozen-clamp证据保留。
此前“群唱局部交接唯一下一项”为本轮诊断前历史，恢复时不要重复已完成探针。
最近完成：群唱共享塑形cluster按首个逻辑EGC时钟绘制，原fi/ffi/ffl/RTL lam-alef
24个独立span端点对照验证；保留原所有字素时钟和完整行单次绘制。原30帧固定union
均值11.677862→6.984958，最坏64px223.259843→93；italic100ms最坏局部→21.333333。
19项定向JVM/Android库与Desktop编译通过；240密集PNG和六表、105真实DIRECT3D
窗口hash全部不变，独立Maven消费通过。ADB一次无设备；Android29+新元数据路径
仅编译、旧API/自定义font metadata继续待验，不称正式视觉/设备/性能通过。
证据verification/cappella-cluster-ink，旧失败/original30未改；前述fi下一项为历史。
138ee5a已保存此前b40c9da的真实窗口/独立消费回归。
此前“群唱绘制后真实窗口/独立消费下一项”为历史：b40c9da后已完成一轮105项
真实DIRECT3D窗口回归及独立Maven JVM绘制/原core身份、Android消费编译与归属打包。
91项hash不变，14项群唱hash随完整文字修复变化，恢复/暂停等自确定性通过；
验证窗口focusable=false，不抢用户输入。证据verification/themes-native-window/
post-cappella-ink和verification/consumer/b40c9da-validation.json。固定contact已图检，
保留Tilt大字重叠/Fume镜头等残差；这不是原版视觉/选歌音频/Android设备/性能通过。
Fume有限限幅位移的媒体重建策略仍未成立，已有同初态/frozen-clamp-v1证据；
不重试focus-only/midpoint-only或固定帧延迟、不改旧oracle。
最近完成：群唱活动路径单次完整行+内部EGC阶梯alpha，15统一alpha/15变换覆盖
零差，30原字素count/CSS透明度一致；原同union均值12.375545→11.677862，最坏局部
223.259843未改善，italic100ms微退步已记录，不能正式验收。240几何六表字节不变，
22.188593px未修。分次vectorclip和constantgradient失败均保留；16项定向JVM、
Android库/Desktop编译通过，证据verification/cappella-active-ink，无新设备/性能结论。
已提交930d104：群唱已读/减少动态完整行绘制，十组像素覆盖零差；原五组同union均值
7.962819→7.081652，斜体局部109.412121→10.674576。密集240几何六表字节不变，
22.188593px残差保留；整屏均值6.387616→6.387741微退步如实保留。证据
verification/cappella-complete-text，JVM/public/Android库/Desktop编译通过，无设备结论。
已提交1d6850a：真实Java Sound+完整离屏sample七主题探针修复普通LRC末行无界导致
Fume不收敛。sample按真实时长/已有demo时长生成显示副本，raw core保持；七主题
暂停/seek像素恢复通过，3个定向测试类/Android shared及Desktop编译通过，证据
verification/local-playback-themes。原生文件选择/本轮Android设备/人工听音仍待验。
此前Desktop选歌“唯一下一项”为历史；当前记录只证明真音频游标和离屏UI集成，
不替代真实窗口/原版视觉/设备验收。Fume/群唱残差继续保留，Diorama不开放入口。
真实Desktop七主题窗口复核和manifest/图检已保存（059087b/verification/themes-native-window），
此前“待保存”为历史。没有选歌/真实音频/设备的新证据。

c50e5b4后七主题真实DIRECT3D窗口105检查重跑完成，旧2d472d4图/表原样保留；
只有Tilt large-font hash变化，其余104不变，恢复/暂停/resize/字号/设置/offset/切输入/
卸载/反向切主题全部通过。固定contact图检仍可见Tilt重叠/Fume镜头，不称原版通过。
当前本地Maven独立消费JVM渲染/原core回调、Android编译/依赖边界/归属打包通过，
仅build/local-maven发布，详verification/themes-native-window/post-tilt-v2和
verification/consumer/c50e5b4-validation.json。ADB一次检查无设备，无新Android运行结论。
此前“当前Tilt后窗口/独立消费下一项”为历史；镜台fog提交c50e5b4，仍内部。

镜台文字fragment fog已接全邻行及活动三层：原view-axis12/30、编码后RGB混合、
alpha/life保留，透视变换下32段有界梯度；原8bit中间颜色门槛先失败，保留全部失败，
改half-float stop后原门槛通过而非放宽。1221原控制factor/代数峰0，stop峰.000244。
54原完整文字同union均值12.221155→3.742278，局部46.276676→21.879310；56原shader
斜面/普通加色stress均值39.521592→1.824069，局部122.333333→15.982094。预设solid
ray对照峰原.519/本1.685byte，未删低alpha或配准；108旧无fog图hash不变。
10定向JVM/Android库/Desktop编译通过，固定contact已图检。证据verification/diorama-fog。
仍非原mounted CameraRig、depth/跨曲/调色/整曲驻留或设备性能验收；无公共新主题。

Diorama内部偏移已修为media-offset，点击逆映射lineStart+offset，与既有设计和
两端sample一致；原自测曾复制反号，保留源/失败日志并注明迁移依据，不改上游数据。
新增独立契约回归先失败（+200ms在1.7s误选1.9s行，正确应2.1s才进入），再通过
正/负/零偏移/原对象/点击往返/完整文字镜头像素/减少动态/暂停/不重准备验证。
8定向JVM与Android库/Desktop编译通过；仍内部主题，无新原图/设备/性能声明。
证据verification/diorama-offset（9e46baa）；补充caller审计修正唯一遗留的邻行验证器
fixtureLyric→media转换（先因旧反号失败，未弱化断言），4项补验通过，108旧native图
hash完全不变，原数值/原图未更新。日志.work/diorama-offset-neighbor-fixed-bridge.log。

Tilt真实原版正文+字幕整屏30帧对照已完成：真实原overlay/默认.6透明度、有效单行翻译
配置/同真实core/字体/视口/字号，公开对内部像素完全相同。字号/宽/高/top峰
.000001/.20275/.01565/.03125px，暂停/seek/offset/重挂通过；MAE11.119261、局部最坏
152.333333，未最终视觉通过。上游380/800px、1.4字号自身盒重叠10.421295/38.483795px，
2倍也有明显字形重叠，保留为可读性问题，不擅自移动/缩字称通过。
首轮source透明度误设1的无效整套图/输入/报告保留.work，修配置后全30帧重采，无产品收益
声明。证据verification/tilt-subtitles；没有本轮音频/设备/新窗口/性能结论。

Tilt系统字号接入fit/重分行/行高/居中/stagger已修（64d5619），sp只乘一次；1.4的CSS半舍入
和测量125.99截断也已修。同原12帧44行自然默认字体矩阵，MAE84.912471→14.683705，
字号峰30.672→.016px/行高41.406225→.03125px；仍8个.011字号门槛失败，未降低。
被动测量定位浏览器sans与Windows Skia Segoe UI默认空格差；另控相同初始Canvas字体
的独立原版矩阵字号峰0，宽.20275/高.01565px，通过原门槛，不替换自然基线。
18定向JVM及Android库/Desktop编译通过，暂停/seek/offset/重挂/原对象点击通过；
字幕/设备/新窗口未验，证据verification/tilt-font-scale。此处为行为修复非正式视觉通过。

镜台长行取消检查已接到ownership/每256列、单元paint和halo pass边界，原异常原样
传播，不返回部分对象；actual core重试和draw/seek无准备检查通过，418旧hash不变。
单次native shaping/draw/blur仍为原子操作，不保证硬取消时限，也不解决整曲驻留。
JVM/Android库/Desktop编译通过，证据同verification/diorama-raster-window。

镜台完整行临时位图已移除：同一完整shaped layout直接绘制整数owned窗口，旧90/180/54
舞台图及单元/控制图共418hash全部不变。两条32词长行advance9205/10136px，独立原414
字素/单位记录一致；全部1024px窗口覆盖峰0，去除13.52/14.86MB理论RGBA临时量。
仍保留40.22/54.66MB body+halo，单个巨型/双向分离单位与整曲驻留未设全局上限，不能
称4096安全或设备节省；未验证CPU代价。JVM/Android库/Desktop编译通过，见
verification/diorama-raster-window。生产未新增逐帧准备，无新设备/窗口/性能结论。

Cappella同一提交分阶段探针已完成：30条纯读取观察/control均与原几何相同，独立重跑
全部状态和trace精确相同。11条原incoming DOM经原own y/scale逆变换重建snapshot峰0。
1.016667s的local React提交在RAF任务返回后才恢复frame110/目标204.53125，不能把任务内
49px状态当最终值。源运输identity成立，但媒体事件precommit/live flow/target分离尚无
通过的生产修复；保留22.188593px、旧失败候选，不重试冻结/多衰减/固定延迟。
见verification/cappella-projection/frame-phases-v{1,2}.json和README。
Fume饱和位移策略仍未解；镜台full-stage塑形小退步/整曲准备/雾深度继续保留。

已完成哈希：c4ef479完整塑形coverage修复，c8ef3e9实际Pendolo follower停止阈值，
f35aa63群唱帧阶段诊断（非修复），983660f镜台移除完整行临时位图。
c8ef3e9后scripts/verify-consumer.ps1独立本地Maven消费通过：JVM运行/core身份、Android
消费编译和打包声明。首次独立项目缺SDK环境失败保留，仅读根local.properties后临时设
进程ANDROID_HOME重跑成功并恢复环境，未写个人路径/远程发布。日志.work/consumer-c8ef3e9{-sdk,}.log。

Pendolo实际follower停止阈值已修：原useSpring实际attachFollow为.001/.01（包括大跳），
旧普通dispatcher默认.005/.01或.5/2不适用。54原态/972raw/3501轨迹及独立首rest根，
峰3.55e-14/4.80e-14/1.31e-14秒；22浮点根边界布尔差明确保留并验两侧。连续首rest
一次准备锁存，不按RAF累计。新增27原完整轮盘/真实core公共帧，legacy旋转控制峰
.832573°→1.11e-14°，同前景MAE14.925368→13.753192、最坏41.930028→16.299364，
最差64px局部82.583333未改善。旧144native图hash完全相同，.325106°/1.364706px仍在；
原30/60/144源driver证明7s实际167/183/194ms采样而非语义200ms，不加固定延迟追图。
旧dispatcher fixture/1e-9/1e-8断言另保留。公共暂停/seek/offset/resize/重挂及Cappella
共享核回归、Android库/Desktop编译通过，无新真窗口/设备/性能。证据verification/pendolo-follower。

Diorama完整塑形遮罩修复：原advance矩形裁斜体756px/混合bidi重复1504px，透明span
候选也损伤fi合字327px已拒绝。现一次完整栅格、可分离视觉列ownership、原pivot缩放，
四原Canvas完整run/真实core场景重组alpha全部逐像素相等，原单位/时钟保留。固定旧
90/180/54图均值6.604446→6.642979/4.425606→4.451274/6.614592→6.618104，确有小退步，
最差64px局部改善不掩盖均值；栅格phase/crop/halo贡献尚未单独隔离，完整舞台未验收。
见verification/diorama-shaped-mask；JVM离屏/Android库编译/Desktop编译通过，无新设备/
真窗口/性能结论，镜台保持未公开。原overhang/bidi“下一项”为历史，coverage已修而
完整原逐单位视觉和整曲内存仍待验。Fume局部交接仍需有依据的饱和位移重建策略。

Diorama正常邻行窗口/life已接内部core舞台：原前2后3、符号opacity、固定5.2fit/
欧氏life、primary/secondary、透明far-to-near再活动17/18/19，活动三层也接life。
37原窗口精确一致、180材质行峰1.0812e-11（1e-9门槛）；54同源/前/后全前景union
MAE15.895084→6.616512，最坏图34.659908→12.806258、64px局部49.563957→22.985632。
窄CJK原/本仍裁词，未以均值宣布可读性通过。准备次数/原core身份/暂停/乱序seek/
offset/设置/重挂及既有逻辑resize通过，Android库/Desktop编译通过。正常window不
冒充跨曲flight/fog/depth；整曲文字内存仍待有界准备方案。证据verification/diorama-neighbors。

Fume冻结原首spring实验v1完成：6个真实源incoming/756原有限算子映射，完整9态峰0；
96个无饱和校准控制log峰3.87e-12。8个合法输入、饱和、校准时长控制仍有位置差
.684600世界单位（512/1024数值细化差.003026，不作为精确误差界）；源整段用终值限速
更新位移，且1步/2步同总时长X峰差4.805517，不能靠RK步长缩小或focus-only修好。
108对全9态合并不都在合法速度域内，报告明确区分。生产/旧fixture/92图未变，
4个定向测试类通过。尚需明确的分支局部饱和位移重建策略和原mounted矩阵改善；
见verification/fume-camera-handoff/frozen-clamp-v1.md。不是行为修复或视觉验收。

Diorama文字平面裁剪已接完整正文/活动三层：实际Three18同姿态stress，MAE
99.915892→3.080586（同source/before/after完整前景union）；10个原空白case全部精确
为空，去除221550错误像素。216旧前方PNG重跑hash完全不变。8定向JVM/Android库
编译通过；无设备/真窗口/性能结论，不冒充深度buffer/fog/完整镜台。见verification/diorama-clipping。

Diorama原weighted picker已接内部core文字（02b8d8c）：1404原始controls/13权重，选择完全一致、
权重峰0；默认缺省chorus/part/block未伪造，三个原motion preset接入。真实core默认选镜/
切句精确边界/offset/逆序seek/设置/逻辑resize/重挂通过；4定向JVM及Android库编译通过。
不是新原版连续画面或设备通过，camera/栅格残差保留。见verification/diorama-shots。

Diorama正常follower文字闭环已实现（de7baa6）：1482同原incoming状态峰2.22e-16，150 snap/
62安全视锥；96原有限步文字/自产文字finite和media同前景MAE4.675817/13.058764，
后者最坏57.104488，低alpha范围仍28px且有裁词，不冒充原RAF等价。13定向JVM/
Android库编译/Desktop编译通过；暂停、显示调度、逆序seek、offset、取消、逻辑尺寸/
设置/重挂通过。无新设备/真实窗口/性能；详见verification/diorama-follow。

Cappella二次提交探针已完成（6e23b78）：原1.016667s的两次高度61.265625/122.265625px，
snapshot top仅差2.23e-6px，排除“二次提交必然多衰减一次”的简单原因。独立重跑17条
trace/全部几何逐值一致且与固定原版基线相等；初次lookup读取错误保留。生产未改，
22.188593px残差未修复。进一步假设必须分离pre-render DOM/flow/post-RAF状态，
不能固定帧延迟；见verification/cappella-projection。该局部阻碍不阻塞文字镜头工作。

Cappella减少动态flow修复：原直接目标box却仍用普通动画height/margin排flow，压力误差40.460..66.508px；
现减少动态的flow和rowHeight都直接取当前目标，六组240探测时刻全部0（原1e-9门槛）。
真实core毫秒数据/公开像素相等、暂停/seek/重挂通过；普通240PNG和6表逐字节不变。
14项定向回归/Android库编译通过；非原版普通动画等价改进，非新设备结论。证据verification/cappella-reduced-flow。

Pendolo旧tick/chorus/CSS-width精确Motion锁补验已完成：144/168/168原帧，全部fixture/
被动状态相同；4张tick和6张chorus图有2..19像素微差，峰通道4，光晕168图不变。
旧源图/native/报告完整保存在.work/pendolo-motion-lock-v1，专用4184实例已关闭。
13个wheel案例与单独width回归通过；仍有tick盒1.364706px/旋转.325106°、chorus盒6.019072px。
见verification/motion-lock-audit/pendolo-dense.json；这是精确基线复核，没有新生产行为修复。

真实DIRECT3D窗口发现并修复Tilt画质切换后状态count永久0：复用已准备字形但向新画质epoch重发count，
不重排/不改视觉。PublicTiltTest新增回归先失败9→0、后全部3项通过；Android默认含Rust APK构建通过。
059087b后七主题真实DIRECT3D窗口105次检查已通过，含暂停/seek/resize/字号/设置/offset/换输入/
卸载重挂/反向切主题。首轮误采Fume准备提示已保留为无效证据，验证器按准备输入清除旧ready后
完整重跑34s并图检。证据verification/themes-native-window；这不是音频/原版视觉/Android设备验收。
Tilt大字字幕重叠、Fume oversized camera等仍需固定原版条件另验，不因窗口可运行而忽略。

Cappella240精确锁复核已完成（0844f4f）：999数值行与被动状态完全不变，38图有原始差异，
见verification/motion-lock-audit/cappella-pressure.json。局部snapshot候选完整采集配对均值6.370007→6.047083，
峰22.188593→32.171478px，overlap/calm1.3s明显倒退，已恢复且不作为修复合入；
9aa439a的说明曾误用采集中途结果（6.376148→6.209810/峰21.650655），该部分无效；
同提交comparison.json已是完整结果，现更正文档，保留采集/读取重叠的失败记录。
见verification/cappella-projection。下一条可检验假设为分离原同一媒体更新内的parent/local二次提交，
同一DOM前态测量两次快照；不能把逐事件冻结补偿/固定一帧延迟当修复。当前残差仍22.188593px。
设备2026-11-18检查一次：adb无设备，不重复轮询；私有播放进程实机验证仍待设备。

Fume同原incoming九维局部交接v1已完成（bcf23e0）。8组/520原步骤，
目标峰1.14e-13/单步算子峰2.28e-13；连续模型提前分支与有限步clamp位移问题已分离，
800/smooth3.116667原bridge但当前anticipated spring；同初态dx14.207132。
5.4s控制组仍有8.164..8.314px峰，旧整曲误差存在相互抵消。生产模型没有修复，
未重采92图、未改旧适配fixture；见verification/fume-camera-handoff同初态节及v1原始记录。
下一条有界相机假设为冻结原首spring目标/系数，跨clamp阈值比较完整(x,v,focus)映射，
先解释非可逆限幅位移再推导分段媒体算子，不再猜focus-only/midpoint-only/固定延迟。
此前“同九维初态交接对照”为历史下一项，实验已完成而行为修复仍未完成。
镜台有界文字/透视/材质/镜头目标探针已完成；后续完整rig/回正/切曲、weighted picker、
near/depth/fog、Android中线、overhang/混合bidi与设备仍未完成，不开放Diorama。
Fume短bridge/总览仍未修复：mounted原版与当前连续模型在3.15s、10s差最大；
FumeMountedPublicTest现正式输出完整9维pose/retarget；无需再替换忽略的诊断测试。
原诊断.work/fume-camera-diagnostic.log保留，后续用build/verification/fume-mounted-kotlin/*.tsv。
3s时两侧retarget一致(.13s direct)，3.15s位置/速度/滤波焦点均有差，不是文字布局；
总览源60Hz首采9.7而连续起点9.69，禁止用固定延迟掩盖。原连续适配fixture门槛
保持不变，尚无通过实际原帧矩阵的改进公式；并行推进不依赖它的文字层。
原React父层入场fbbf7f6与Diorama有界探针dc5e02f已完成。
上一项Fume公共闭环e532ff2只交付校准预览，不是最终效果通过（fume-preview.md）。

已完成（起点295d260）：
- c59a70f 正文/字幕独立缩放、源字号取整/行高、重排/避让/扫描/辉光尺寸贯通。
  原轮盘16对/58行，字号和实际活动行断行全部一致；406数值字段，盒峰.010182px；
  MAE6.604243/峰10.092899，仍有栅格差。窄屏大字体原先少一行（64.125px）已修复，
  同矩阵MAE16.078982→6.604243；失败图/JSON及hyphen回归失败日志保留。
  原88扫描控制、24默认配色PNG不变；pause/随机seek/重挂/字号与尺寸恢复通过。
  Android APK/Desktop编译通过，未运行设备/真实窗口。证据verification/pendolo-typography-stage。
- Pendolo公共校准闭环：FoliaStyle.Pendolo、真实播放/演示入口/偏好、原对象点击/长按、
  offset/切歌/切主题/重挂、独立字幕/Minimal辉光关闭。修复仅字体无字号切classic崩溃。
  45真实源轮盘/core公共入口对图，1302字段，完整盒峰6.019072px，旋转隔离.231502px。
  全非背景像素含低alpha光晕MAE6.137633/峰27.915080，局部64px最差/通道峰也留证。
  不与旧>8阈值均值当改善比较。完整Pendolo与四个旧公开主题回归通过；sample选择/
  偏好通过；Android APK/Desktop编译通过。未启动真窗口、未运行Android设备，不报性能。
  当前可实际选择五主题；正式视觉验收仍未通过。证据docs/pendolo-preview.md及
  verification/pendolo-public-stage；最后构建.work/pendolo-public-final.log。
  提交b0ed98e。
- Cappella core桥接：保留原对象/ref/嵌套伴唱、raw时间与字幕、完整EGC/内部时钟；
  agent按显式SourceRef映射，缺失/单agent使用原seed fallback，无alignment推断。
  32原源行/96sender/56renderHints阈值及原reveal/messages控制通过，两端构建通过。
  原last-end不被EGC合并的最大end覆盖；没有新画面/设备结论，见verification/cappella-core。

73085e6 Cappella projection：同240压力矩阵MAE11.890488→9.173245，盒峰40.109467→22.188593px；
常规210图hash不变。原局部提交snapshot实测确认漏算前行连续高度位移，已补；
已稳定布局的旧测量不产生伪位移。仍有React/RAF提交残差，不能宣称正式验收。
原失败证据及原版被动事件探针见verification/cappella-presence。

aefca39 Cappella公共闭环：真实core、显式嵌套agent、原对象点击/长按、offset、
暂停/seek/resize/切主题/切歌/卸载重挂、Minimal无辉光、sample真实播放与演示主题入口。
210完整公共帧先与内部逐像素一致，再对实际原JSX；全非背景含低alpha MAE2.325954/
盒峰3.637201px/opacity峰.013716，不能与旧>8口径直接比较。密集仍差22.188593px，
未最终视觉验收。共享五个公开主题回归、sample选主题/设置、Android APK/Desktop编译通过；
无设备/真实窗口/性能结论。证据verification/cappella-public及docs/cappella-preview.md。

f20c1a2 Fume raw core计时修复：声明start/end与raw末片段end独立保留，不再使用
归一化范围或EGC union末端替代。56原renderHints阈值/完整EGC时钟控制、
已有FumeReveal/原逐步camera与两端构建通过；没有新画面/设备结论。

Fume整曲准备：120合成行，同字体/800×600/360s/114371节点，原74.202s
触发既有超时；每block缓存EGC/时间计划后相机1.308s，布局1.992s、绘制准备.119s。
提交6dadc01。原数值门槛未改，432张旧scene/隔离PNG hash全部相同，两端构建通过。单次桌面
观测非设备统计收益；证据verification/fume-preparation。原短bridge/总览边界已被动
记录：总览阈9.69，原60Hz首次9.7；14.867px/.055318仍是未解决校准残差。

e532ff2 Fume公共闭环：FoliaStyle.Fume、整曲后台可取消准备/字号与系统fontScale、原来源回调、
逆镜头命中/长按/空白穿透、暂停/seek/offset/切主题/换输入/卸载重挂、Minimal与减少动态、
sample真实播放/演示入口及偏好。216公共帧与内部逐像素一致；对原函数管线含低alpha
MAE12.016782、镜头峰15.933273px、scale .056221。不是旧矩阵改善比较；未覆盖原React
完整挂载，正式视觉验收未过。120行字号恢复、六个旧公开主题回归、sample设置与两端
构建通过。未启动真实窗口/Android设备。证据verification/fume-public。

dc5e02f Diorama有界调查：3719真实原函数/Three.js0.185.1数值峰1.3059e-9，
9组原纹理真实WebGL与JVM Compose透视图已运行；含低alpha MAE14.518306，边缘盒差
最多2px，采样差未过最终标准。不是CameraRig/深度/文字塑形/公开主题验收；
无Android绘制运行。源码逐层依赖/最小可行路径见docs/diorama-investigation.md，
verification/diorama-projection。没有引入Kotlin native/3D背景引擎。

fbbf7f6 Fume首次打印父层入场：实际原React/canvas挂载92帧；.985→1/.45s媒体重建、
画布裁切和实际变换命中。原依赖motion-dom13.4.2/utils13.3.0重新锁定，92原图hash/
状态均不变；旧其他主题Motion证据需精确lock复核，不自动追认。入场scale峰1.285e-7，
同矩阵入场前景MAE3.699709→2.510674、全部8.553688→8.140111，镜头峰15.933273px/
.056221不变。暂停/seek/重挂/点击/120行及两端构建通过，未运行设备/真实窗口。
初次40步数学反解不符原12步近似的失败保留；证据verification/fume-mounted。

52bc6f3 镜台文字准备：原splitDioramaLyricUnits与共享时间函数67同输入，641字素/226单元，
词身份/UTF16范围完全一致、时间峰0。实际core桥接保留来源/嵌套/字幕/原时间；
EGC跨parser片段保留全部内部clock。只是内部内核，没有新公开主题/视觉结论。
两端构建/原透视数值回归通过，证据verification/diorama-text。

e3bb67c 镜台独立完整文字透视：同字体/视点24原WebGL对Kotlin自产纹理，advance/缩图字号/
画布尺寸精确一致、middle基线1e-6内；修复悬挂空格漏算11px与中文hhea/OS2错位。
全非背景MAE13.081760/墨迹盒峰2px，仍有采样差。没有共享原PNG、没有图像配准。
原失败CJK图/报告保留；JVM离屏/两端构建通过，无设备/真实窗口。Android字体表度量
暂缺，不能宣称其中文基线通过。证据verification/diorama-text-plane。

e7037f7 镜台活动材质内核：实际原updateDioramaActiveUnits/Three对象832组13字段峰0，
原Float32写入/线性色/关键词/渐变/显现/注册辉光/灵魂层阶段均核对。独立媒体指数
包络可逆/暂停，事件边界原指数1e-12；不是原RAF连续画面等价或视觉验收。
两端构建通过，下一项仍是完整塑形切片/三层实际绘制，证据verification/diorama-unit-frame。

ffd64e2 镜台活动正文/渐变绘制：ONE完整塑形段落按显示单元切片、内部EGC时钟保留，
原版真实active layout/material/WebGL（glow/soul0）90对，前景MAE6.605919/盒峰2px。
字号/原线性色/逐单元时间、暂停/seek/重挂、时间变化不重备文字通过；两端构建通过。
无设备/真实窗口；混合bidi与跨单元overhang未完成验收，证据verification/diorama-active-base。

49529b1 镜台注册辉光/灵魂层：实际原纹理/三层材质180对（媒体解析包络注入，非原RAF），
同源层序/深度偏移/局部抬升放大/共享词色；材质字段峰1.771e-8、暂停/seek/重挂通过。
原shadowColor先于blur的量化顺序已校正，原失败保留；全前景MAE4.427326、范围峰21px
未最终验收。两端构建/JVM离屏通过，无设备/真实窗口；见verification/diorama-active-layers。

c5ceac9 镜台镜头目标：13shot/read-head/hold/drift、缓存EGC/方向/renderEnd与原CameraRig
私有块2808组11字段峰1.066e-14；132张同文字三层原Three与Kotlin自产镜头图，
世界pose峰3.553e-15、全前景MAE5.773035/范围峰20px。非原完整rig、不是正式验收；
原target极端位置也裁词，不能以此降低最终标准。两端构建/JVM离屏通过，无设备/
真窗口；证据verification/diorama-camera-targets，source段落hash保留，未改原代码。

c08b8e2 Fume单因素焦点候选已拒绝：RK存储改当前B(t)但保留旧forcing/分支，92同mask
MAE8.175929→8.664704、屏幕峰15.933273→15.981226，旧适配断言失败，未改预期。
候选撤回、生产代码逐字恢复，原FumeCameraTrack/FumeMounted/PublicCappellaStage回归通过；
完整失败/哈希/具体下一步见verification/fume-camera-handoff。不是视觉修复或验收通过。

3ae1376 精确Motion lock复核：原13.4.3/dom13.4.2/utils13.3.0独立4177/server/cache顺序采集，
Pendolo公共45原图/四张输入表hash不变；Cappella常规210原图20变化（17..143pixels，
channel峰34），输入/存在/盒/alpha表不变，三处被动glyphCount差异原样保留、不归因于版本。
公共Cappella重新验证通过，MAE2.325973、盒3.637200px；旧报告与全hash保留。
不是密集240或全部旧Pendolo矩阵复核，也不是视觉验收；见verification/motion-lock-audit。

Fume第二个单因素（只去end-intent覆盖，保留midpoint）也已拒绝：92同mask
MAE8.020819→8.806774，屏幕峰18.297365px，3.15/800也退化；旧适配断言失败。
生产源码再度逐字恢复，完整9维差值/失败日志见verification/fume-camera-handoff；
不重试这两种局部改动。接续应先用相同incoming状态校准，而非再从整曲误差猜公式。
恢复后原FumeCameraTrack/Mounted及本轮Diorama Camera/Active回归全部通过，
.work/theme-final-restored-check.log；本轮执行已保存并停止，未完成最终主题验收。

用户签名配置与daemon JVM文件保持原样、不暂存。Android播放独立进程设备验收仍待。

## 历史记录 — 以下“下一项”均为当时记录，不是活动计划

### 2026-11-17 主题继续点（历史）

- a8c58e7已保存内部配色。新增真实lyrics-core→Pendolo桥接：原对象/ref、正文与字幕、
  原始/显示结束时间、跨EGC分片的嵌套时钟均保留。无词输入使用原版整句均匀字素计时，
  不借用Cadenza的instant适配，不产生假词。56原renderHints阈值/偏移控制通过；
  发现1.18-1.0浮点阈值差导致整数公式多留170.4ms，已按原秒数公式修复，失败log保留。
  真实发布模型的身份/字素/诊断测试和两端构建通过；无新UI/设备结论，见verification/pendolo-core。
  下一项：原字号/字幕缩放、变换后点击命中、公共校准入口及同条件公开入口对照；
  仍不宣布Pendolo/所有主题整体完成，密集tick与栅格差留在上项证据中。

- b0a2328已保存副歌轮盘。当前内部PendoloPalette替换固定正文/字幕/强调色，
  同时影响普通、扫描、关键词fallback、双shadow和halo，原关键词色独立保留。
  24组真实原JSX替代配色及24旋转隔离，盒峰.068329px，图MAE7.946432/峰12.017226；
  不是与默认矩阵的改善比较。palette关闭/恢复、暂停/seek/重挂通过，1202旧图不变。
  两端构建通过，无设备。证据verification/pendolo-palette-stage。
  下一项具体：原字号/字幕缩放与lyrics-core桥接、变换后点击，再公开标明校准中的入口；
  密集tick采样和整屏栅格差继续保留，未完成/镜台等主题见themes-parity，不声称全部移植。

- 61036f7副歌扫描后，现接原父层scale保留/中断和条件halo，时间事件可逆重建；
  原轮盘168对/804行，另168原旋转隔离、168低alpha光晕和48软件光晕对照。
  整屏MAE11.918332/峰45.121911，盒6.019072px；原旋转隔离盒.231502px。
  halo不丢弃低alpha像素，均值.313817/峰通道2；软件.294727/峰通道2。
  尚未最终达标：密集切句源60Hz对1.02s起点在1.033333s激活，实读WAAPI66.6667ms，
  native按语义80ms重建；未通过固定帧延迟掩盖。来源事件/其余栅格差留证据。
  修复共享ClassicLayer录制Density自引用递归；密度/原坐标native/软件测试通过，
  1034旧native PNG不变。两端构建通过，无设备；证据verification/pendolo-chorus-stage。
  初次StackOverflow和visibility隔离失败均保留；未把失败图放入验收结果。
  下一项：密集轮盘采样差和剩余整屏栅格定位，再接core/公共校准交互；仍不开放正式主题。

- 52bbf16已保存时计范围关键词。副歌扫描层现保留原三档/.58混色/遮罩后顺序双辉光，
  60组原参数精确；264真源组件对图，528宽/前缀字段精确，另132强制软件路径。
  主矩阵MAE3.124627/峰9.237356，软件3.403482/8.729687；400旧native图hash不变。
  暂停/随机seek/resize/重挂与配置恢复通过，两端构建通过，无设备/性能结论。
  证据verification/pendolo-chorus-sweep；下一项原轮盘halo/父层scale和中断激活，
  再整屏配色/辉光及公共校准入口。仍不将扫描层完成当时计整体通过。

- b2b13c2保存idle宽度后，时计范围关键词色已实现，单独保留原整句范围优先级/ASCII词/
  CJK短语/EGC映射/同色合并，非复用心象不同的逐词匹配。原10例281行完全一致；
  真原扫描组件88对图，176行宽/前缀精确，图MAE5.799414/峰9.237356仍未最终验收。
  以完整塑形段落的预备路径着色并共用原计时mask，暂停/seek/配置恢复通过；
  既有312原生图hash不变。两端构建通过，无设备运行。证据verification/pendolo-color-sweep。
  来源host缓存导致的首次无palette采集已拒绝，重启专用服务器并加guard；tick重采TSV全相同，
  仅2张精确入场源PNG仍有变化，比较报告如实记录，不藏在native修复结论里。
  下一项：显式副歌的混色/双drop-shadow/halo及scale源对照，再整屏配色与公共校准预览。

- 35ef276后已补齐时计保留idle节点的200ms最大宽过渡/改向/反向缩时，字幕约束独立。
  原144图完整对照bbox17.652888→1.364707px，原旋转隔离17.652888→0.105115px；
  未旋转宽峰0.03125px（源下一观察帧与native语义时刻差）及wheel0.325107°仍保留。
  Blink真实CSS的248宽/138时长字段、重访/暂停/seek/减少动态通过；旧168原生图hash不变。
  两端构建通过。144原图加宽度observer后4图hash改变，但tick/几何完全不变；前后native
  均用当前同一源图重比，不冒称源图全部相同。图MAE13.137682仍未最终验收。
  证据verification/pendolo-width；下一项chorus强调/关键词色与原真实组件对照，再公共校准预览。
  近期保存：0d993e1整屏字幕目标、b520191媒体tick、35ef276旋转舞台。仍无时计公共选择器。

- b520191机械内核后已接真实wheel hub旋转、-.65文字反校正和绘制期边缘alpha；
  原useSpring/完整JSX保持挂载144对/694行，native暂停/随机seek/重挂一致。
  未验收：wheel峰0.325107°、bbox17.652888px、图MAE13.235468，原旋转隔离仍17.65px。
  来源定位非活动主句transition-all的200ms max-width过渡，native目前直接目标宽；
  下一项优先补齐这一过渡（保留完整段落塑形），再chorus/关键词及公共校准预览。
  全Pendolo测试/Android APK/Desktop编译通过，无设备运行。证据verification/pendolo-tick-stage。

- 时计tick媒体内核已实现三档/.5到2锐度、原物理响应/轮盘旋转/-.65文字反校正、
  语义事件携速改向及inclusive末尾右极限。1134组原Motion响应/2304次时间查询通过，
  最大4.25e-14；随机seek/暂停/重挂/减少动态通过。旧Cappella弹簧原样抽取共用，
  不模拟显示帧累计。证据verification/pendolo-tick；不算真实挂载useSpring或画面通过。
  下一项：机械旋转接入真实轮盘绘制，比较原版挂载useSpring的RAF/改向速度与图像。

- 时计整屏目标轮盘已接真实主句/字幕布局、半圆避让、CSS层级旋转与距离alpha。
  原版未改JSX子树80对/484行，5808字段最大0.0005px，数学/预留高度完全相同；
  初始换行/固有宽度/Arabic空格tracking失败已修复且失败图保留。暂停/随机seek/resize/
  重挂一致；旧88扫描图hash不变。图MAE10.650323、峰21.807819仍不达最终画面验收。
  全Pendolo测试/Android APK/Desktop编译通过，无设备验证；见verification/pendolo-stage。
  下一项：原版机械tick弹簧/旋转/反向文字校正与媒体可重建事件，之后chorus/公共校准入口。
  仍不把目标定格轮盘当完整时计主题；保留群唱/Fume未解差异。

- b484008时计扫描层后，字幕选轨/样式内核已按原版移植：both去重/占位过滤/alternate
  fallback、Unicode L/N可读性、focal/idle字号与次轨缩小/淡化、1.5行高和每轨.25em。
  原528配置/168轨字段与受控高度求和一致，两端构建通过；尚未接真实字幕布局/画面，
  不把受控高度回调误报为排版验收。下一项：原版文字轮盘子树与native整屏实际字幕/
  圆弧目标布局对照，再机械跳步；仍不开放未完成的主题选择器。

- 3e321a0时计几何内核后，真实扫描文字层已移植：原版实际组件88对同字体/宽度/时间PNG，
  176原行宽/前缀度量完全相同，字素时钟和原DOM遮罩通过，暂停/88次随机重访/尺寸恢复/
  卸载重挂像素一致。Windows正文栅格校正MAE8.461170→5.681259，峰值9.120121，
  残差仍保留，不算文字最终验收；Arabic连续塑形/非单调prefix均保留。
  下一项：时计字幕轨真实高度、旋转层和机械跳步整屏原版对照，再core公共校准预览；
  chorus/关键词色、Android运行仍待做。证据verification/pendolo-sweep。

- 时计首项：PendoloGeometry/PendoloTimeline已实现原17索引候选、半圆/110度淡出、
  实测高度避让与半格空档；历史按语义边界重建，包含重叠后长句恢复，非最近起点猜测。
  原版252场景/781行、2476时间查询、273不取模透明度及252真实caller宽度对照通过；
  逆向/乱序seek、重建、尺寸恢复通过，Android APK/Desktop编译通过。
  仅内部数值内核，不等于主题可用或画面验收。下一项：时计完整塑形换行、内部字素
  扫描/字幕实际保留高度与原版真实文字图，再机械跳步和公共入口。

- 当前群唱修复：按真实选句/可见行及局部字素、目标尺寸、时间戳更新准备layout提交，
  比较原Motion取整后的上下边界；未采用固定帧回放或猜测延迟。240对压力图MAE
  13.2914256564→11.8904876599，存在/顺序仍完全相同；位置峰值40.109467px和
  alpha峰值0.050386未改善，尚未验收。常规矩阵全部PNG哈希不变。
  全部Cappella JVM测试、Android debug APK、Desktop编译通过，设备未连接。
  下一项：定位present行在前行缩小时的投影目标保持；同时推进时计的独立原几何/
  时间内核对照，避免将群唱未解决视觉差异当作已完成主题。

- e916c41完成群唱常规媒体舞台。新增rapid/overlap原版240对/999行暴露退场复入重复气泡，
  已修复key复用/中断连续性/批次移除/frozen popLayout盒与退场订阅；原存在/顺序全部相同，
  乱序seek/暂停/重挂载通过。压力图仍MAE13.291426、位置峰值40.109467px，尚未视觉验收；
  常规图MAE3.393810、几何3.637201px。完整失败日志/前后PNG保留，见verification/cappella-presence。
  下一项优先定位短周期layout projection/flow及旧props计时；core/public入口仍未添加，
  不把身份/顺序修复当压力效果完成。后续时计原几何/跳步源码已定位但尚未写实现。

- 群唱媒体事件舞台已接选句/active→passed/宽高/弹簧/入退场/位置补偿/头像淡入/时间戳。
  210对完整原JSX连续播放、594原行，消息存在及绘制顺序完全相同；暂停/210次乱序重访/
  重挂载/减少动态通过。图MAE3.390252，几何仍差3.637201px、alpha0.013716，不算最终验收。
  对照host按原store隔离MotionValue与React选句；原强制父级每RAF重绘会反复中断layout，
  已保留独立压力场景/失败采集，不混称改善。证据verification/cappella-playback。
  下一项：重叠/极速消息和退场完成前重现的presence复用，再core/public校准预览；
  仍无群唱公共入口，Android设备/复杂栅格/窗口动态projection待验，不擅改验收标准。

- a988543 群唱响应内核已提交。当前孤立气泡实际接入0.2s宽高转场，92对原版稳定挂载
  AnimatedBubbleFrame图/92次seek及暂停通过；宽残差0.243535px、高0.07211px、图MAE0.879876，
  不把它当动态几何精确通过，原target断言仍0.001px。当前仅连续active消息，未接公共入口。
  下一项：整曲活动状态事件、active→passed宽高、行入退场/位置/缩放和时间戳整屏对照。

- 02e86f6 群唱整屏目标舞台已提交。后续响应内核1134项原Motion dispatcher观测通过，
  包含calm轻微过阻尼、质量参数、带速度改向、原0.2s宽高及0.18s时间戳曲线。
  最大值误差2.85e-14、速度5.97e-13；不把generator对照当完整DOM转场通过。
  修正sample不舍入ms/normal退场0.24s，保留失败证据。下一项把响应接到媒体事件准备的舞台，
  用原版实际连续RAF/换句和窗口消息淘汰逐项核对；仍无群唱公共入口。

- 群唱native整屏目标舞台已接真实气泡、3x3渐变头像、时间戳、外阴影、媒体时间扫光。
  60对原完整JSX/native图及暂停/60次随机重访/重挂载检查；初始分数宽度截断造成标题换行，
  已修复并保留失败图，最新像素残差见verification/cappella-stage-visual。
  原2508几何/248Canvas探针阈值不变；CSS淡入/扫光是显式媒体锚定对照，不冒充原版时钟。
  尚缺动态row/layout spring、.2s尺寸和时间戳入场；下一项先移植/验证媒体可重建的这些响应，
  再core/public/sample。Android无设备，只构建，不宣称设备性能或群唱最终视觉验收。

- 群唱完整舞台目标几何：60原场景/228行/2508字段，独立Compose测量最大差
  5.859375e-5px；248 Canvas探针完全相同，原1e-3阈值不变。16.56px ceil跨界已通过
  JVM glyph16.16/GPOS定点边界修正，不加epsilon。Android仍真实段落测量回退、无设备。
  原失败审计保留；新矩阵Bidi/ChatCJK覆盖正文/标题/时间戳，旧复杂字体回退不再当严格同字体。
  尚无native完整舞台/动态视觉验收。下一项：把独立气泡、头像/时间戳接到该舞台，
  建立真实整屏图，再实现媒体可重建的宽高、行入退场/弹簧和扫光。


- 44e3176 群唱真实字体气泡/文字层：76前缀尺寸完全一致（修复尾空格悬挂），233原CSS
  opacity对照通过；48对原组件/native图MAE0.95330035，图已检查，暂停/随机seek像素一致。
  这是隔离target frame+媒体锚定CSS淡入，未覆盖row springs/堆叠/宽高动画/扫光/时间戳。
  下一项：原完整消息行/舞台几何、强度尺寸和状态缩放，再可重建转场及core公共入口。


- 882990f / 43073e6 群唱消息编排：24场景/774原消息/864视口决策完全一致，三档强度、UTF16种子、
  短句carry/flip/force-right和多agent轮转已实现，文本配置无emoji池。
  core0.5.0已核对无agentId，不能以alignment伪造身份；后续显式映射或原种子fallback。
  两端构建通过、无UI验收。下一项：原气泡prefix尺寸/完整塑形文字舞台及真实截图。


- 7896d89 群唱字素揭示内核：55原输入/565字素/3172时间查询通过，源模块未经改写。
  原DOM码点与metrics字素不一致的两个复杂文字案例已保留证据；native保持EGC，
  源音节切入EGC时保留原输入并合并覆盖时段。Android APK/Desktop编译通过，
  未做群唱UI/设备验收、未开放入口。下一项：发送者/消息编排与可见窗口原函数对照。


- 0c80870：心象已接FoliaStyle.Cadenza/CadenzaTuning及sample“心象”校准预览，
  四项调参/旧设置迁移、点击/长按、offset、pause/seek/resize/卸载重挂检查通过。
  公开入口与原版实际RAF同毫秒/同字体32对截图已建立，MAE约6（重复原浏览器栅格
  有小幅波动，不能称为参数修复改善）；128原定格默认图不变。库仅行计时显式静态整句，
  不伪造逐词；减少动态取消几何运动，画质预算只限制缓存辉光，不声称总进程内存。
  Android无连接设备，仅APK构建；Desktop实际Compose离屏运行。详见cadenza-preview.md。
  下一项：群唱Cappella原消息/发送者/逐字揭示内核和原函数对照，再真实气泡舞台；
  Fume镜头、Cadenza跨句源状态历史及各主题视觉余差继续保留，不冒称剩余主题完成。

- 心象原版选句内核已完成：12个空/普通/极短/重叠/乱序场景、1269条原函数观测，
  活动句/最近完成/下一句及边界正逆查询全部一致；结束点inclusive单独保留，
  长句被短句覆盖后重现的挂载时刻可直接重建，无classic额外退出层。
  两端构建通过；这不是host画面通过。下一项：接FoliaLyricsView与sample校准入口，
  同时处理原core无逐词数据、减少动态、mask预算与生命周期/交互验证。

- 心象群唱圆环与关键词配色完成内部校准：38次原Canvas调用几何误差≤1.706e-13，
  17个原配色结果完全相同；32对同字体/视口/时刻图MAE6.99146，图已核对。
  不把本矩阵与其他场景均值直接比较；原128张定格PNG全部保持相同。
  source strokeWidth的float存储已按实际绘制精度核对，未放宽几何预期。
  暂无core副歌标记，内部显式chorus默认false，不把多声部当副歌推断。
  下一项：原runtime的活动句/尾部窗口与native生命周期，再主题入口和减少动态设置。

- 心象媒体时间平滑已完成内部实现：原版实际RAF的16条轨迹/9113词状态、32对
  连续播放截图已记录；60Hz峰值差position0.14666px/rotation0.62183°/scale0.04071/
  alpha0.03784，原版30/144Hz阶跃采样差另列。原版按RAF右端点采样，native按连续
  forcing积分，未做帧率相位拟合；不是完全等价验收。32对MAE均值6.02439，图已核对。
  随机seek/暂停像素完全相同；极短/倒序/瞬时词、长seek和积分矩独立数值验证通过，
  Android APK/Desktop编译通过，设备运行未验证。证据verification/cadenza-motion。
  下一项：心象可配置正文/词色、群唱圆环、减少动态/行切换闭环，仍不开放伪完整入口。

- a48f140：Cadenza词片段/core桥接已提交。最新独立Compose测量+完整塑形文字舞台已建立；
  原版实际挂载DOM/Canvas定格128对，修复尾空格误分配后均值MAE26.42894→9.97161，
  八组几何最大差9.156e-5px；CJK栅格/基线仍有差，尚未做媒体时间平滑/完整主题入口。
  下一项：原逐帧低通到可重建媒体时间的适配与真实连续轨迹/画面，之后才讨论公开预览。

- 2e02da7：Cadenza落点已提交。最新词范围/片段/core桥接114原浏览器准备、205落点
  ≤1.137e-13；仅源段宽/换行输入下成立，独立Compose测量与完整绘制仍待做。
  下一项真实字体准备和文字舞台，随后媒体时间平滑及完整场景。

- 27f373c：Cadenza包络已提交。最新落点108场景/450片段≤3.638e-11，完整词hero、
  跨行排除、原避让/兜底/漂移已移植，两端构建通过；下一项真实测量、词范围和跨行片段。
  上述均为内部数值内核，尚无Cadenza公共入口或渲染验收。

- Cadenza包络内核完成：原版30组合/2952观测、20阴影probe，1e-10和随机重访通过；
  两端构建通过，未开主题入口。下一项原落点/避让/跨行片段规则，再真实绘制对照。

- 当前：快照按原版stage的分数目标尺寸重采样，新增801px视口，总计216对完整场景及216对共享镜头场景。
  旧144原图散列保持不变；新矩阵平均MAE15.81175/3.42741，不能与旧144均值直接当改善比较。
  暂不开放Fume；镜头限速/切镜仍有14.8672px残差。下一项并行的独立主题核心：Cadenza包络与落点，
  先原模块数值对照，后实际文字画面对照；Fume余差保留，不能视为已验收。

- 最新：整篇文章+实际镜头+文字的144对画面已建立，另144对共享原版镜头隔离栅格。
  静态快照/实时shadowBlur CTM修复后共享镜头MAE4.8226→3.3631，整体15.7541→14.7044，
  原图散列全部不变；随机重访/暂停严格一致。真实文章相机残差14.8672px/scale0.055319，
  明确未验收、未开放sample。下一项：短桥接与总览切镜边界、分数快照目标尺寸，
  然后异步可取消准备与正式预览。证据verification/fume-scene(-source-camera)。


- e938450：连续媒体镜头已实现内部校准，不按显示帧累计；9124条原版/适配记录，
  native适配误差≤4.182e-6，随机/逆向/重复采样严格一致。原60Hz仍有屏幕6.079px/
  scale0.007003残差，8秒2606–2791节点，不宣称视觉或准备性能通过。
  下一项：整篇文字+真实相机闭环截图，修复动态Canvas阴影与静态snapshot的CTM语义；
  完成场景对照之后才开放浮名预览。当前未接sample，不增加播放开销。


- 9ff84e9：原版目标/飞行选择与积分已分离，8696步原算子回归不变。新增连续弹簧响应
  log(A)/h，直接原版冻结系数125采样/375轴最大误差1.17e-12，任意分段和重根/长seek检查通过。
  h只标定原版响应，不是媒体或渲染帧率；动态系数、限速、飞行仍需轨迹与画面对照。
  Android APK与Desktop编译通过；没有设备运行、没有开放Fume入口。下一项：事件边界
  准备的连续媒体镜头，取消按显示采样插节点，量化动态/飞行误差，再做完整文字相机对照。


- 媒体镜头候选已否决并仅保留jvmTest：事件分割+自适应原算子可冻结/逆向重建，
  但对原60Hz峰值偏107.45 world px/scale0.06265，8秒5162–5202节点。不能当视觉通过，
  未进commonMain或sample。360源算子受控采样与Kotlin≤1.4e-10只证明实验复现。
  证据verification/fume-camera-track；下一步需要更高阶/解析响应并保持实际源轨迹校准，
  不用固定FPS回放做媒体模型，也不让该实验增加用户播放开销。Fume仍无公共入口。

- 原版spring/飞行单步算子已转不可变输入输出：16组30/60/144Hz和不规则步长、8696步
  pose/velocity/focus/retarget原值1e-8通过；直接/总览桥、限速、漂浮、总览恢复均覆盖。
  此算子没有用作逐显示帧媒体时钟，尚不是可seek镜头。下一步建立媒体时间轨迹重建，
  区分原版离散积分的频率依赖与平台适配，先量化轨迹/画面再接公共入口。

- 镜头目标已移植：步进/平滑打印焦点、跨行0.7字素混合、字号比例、切镜时长、总览、
  远距离飞行桥与overscan。1253项原模块采样1e-8通过，乱序相同，两端构建通过。
  已核对原版跨行目标在边界回跳（不是连续曲线），保留原行为和首次错误连续性假设日志。
  仍不宣称动态相机/完整主题完成。下一步：原版逐帧spring改为可重建媒体时间运动，
  与不同采样率原版轨迹比较，显式记录平台适配；之后才有Fume公开入口。

- FumeLiveText已接实际整行塑形裁切、逐字颜色/余韵/辉光、落印和静态淡出。原版/Compose
  168对同字体同时间1:1截图，前景MAE均值2.9653、最高9.8170；随机重访168次与暂停严格相同。
  对照图已目视；仍是渲染校准，不是完整主题通过，字体/分词/多行残差保留。
  mask改为延迟分配与紧凑边界，168张PNG与初版严格相同，没有用降质通过。
- 下一项具体工作：原版fumeCamera的打印焦点、跨行0.7字素过渡、总览和远距离飞行；
  再处理原版逐帧spring/wall-clock漂浮到可冻结、可seek媒体时间的适配，不用固定帧率假时钟。

- 新增实时帧内核：直接记录原版drawFumeLiveBlock，1104时刻/8970字素样式/317落印，
  颜色、alpha、阴影半径、落印几何、整句光、读后淡出/总览恢复1e-8通过；逆向采样相同。
  这是可独立验证的数值移植，不代表字形/画面或完整Fume主题已完成。
- 当前具体工作：把上述参数接入保留完整shaping的行绘制，采集相同字体/视口/媒体时刻
  的原Canvas和Kotlin画面；随后镜头与正式入口。原有静态复杂字体差异仍待解决。

- 目标：固定原版多主题移植。倾诉06cc619已开放校准预览；Fume时序f3ae828、
  文章布局3224bb6已提交，保持非正式支持标记。
- 当前已完成FumeStaticText：原版完整行快照、middle基线、hero8%文字内缩、悬挂空格。
  120篇受控/1632块对照通过；同字体9篇159块严格几何、289行文本/换行、159基线偏移通过。
  原6帧MAE70.5127→3.1370；扩展9帧均值6.0209，最高13.7049，尚未最终视觉通过。
  失败候选和修复证据保留；当前报告/图在verification/fume-article，来源在upstream.md。
- JVM检查和两端构建通过；未连接Android，未启动用户可见窗口。Fume仍无公开入口，
  当前静态文字层没有冒充完整主题。hhea与OS/2 typo不同的字体仍需正式解决。
- 下一项具体工作：先完善Fume复杂字体基线/静态栅格，再移植fumeBlockFrame/fumeCanvasText的
  动态打印、逐字辉光与读后颜色轨迹；随后fumeCamera目标与可暂停/seek的媒体时间重建，
  完成浏览器/Kotlin闭环再开放sample。倾诉Arabic/CSS延迟等余差继续登记。
- 保留Android release签名配置与Gradle daemon属性用户改动，不暂存；原Apache工程只读。

## 2026-11-16 多主题恢复（最新继续点）

- 当前目标：恢复原版多主题文字移植；倾诉先补已有内部舞台的真实入口、段宽和450ms退出。
  完成条件是源码依据、公开/独立路径验证和真实原版对照；不把主题可选当最终视觉等价。
- 已实现FoliaStyle.Tilt和sample校准预览、概率/配色持久化；媒体时间、pause/seek/resize与交互通过。
  上游11个父层退出采样≤.0001；14段Latin fractional CSS宽最大误差0.4069px。
  144帧MAE均值32.8560；Arabic/全局字体缓存/CSS60ms过渡仍未完成，详见themes-parity.md。
- Android APK/共享模块与Desktop编译、相关库及sample JVM测试通过；ADB无设备，未安装或打开用户窗口。
- 倾诉本轮已提交06cc619；余差保留，不当作最终通过。随后推进浮名自己的文章/相机依赖，
  不借用classic舞台。FumeReveal内核已完成原版18场景、30个范围、1584时刻及494曲线/保留值对照。
  打印计数与连续焦点分离，句末强制完成、后随空格、重叠/逆序音节和无穷值分支按原版保留；
  core输入只合并跨EGC的源片段，并保存原片段时间。没有新增主题入口或宣称画面通过。
- 浮名时序提交f3ae828；文章布局/拟合内核随后完成：受控96篇1560块、真实同字体6篇150块
  几何字号在1e-8内通过；初轮失败与修复路径保留，没有提高容差。
  原版静态snapshot与native诊断图MAE均值70.5127，不代表文字绘制通过；证据见
  verification/fume-article。正式相机/绘制和Fume公共入口均未完成。
- 下一项：Fume原版Canvas middle基线、hero8%文字内缩和静态快照/动态字素层；
  然后镜头目标/媒体时间重建，达到闭环才开放入口；倾诉余差继续登记。
- 只提交本次代码；Android release签名配置及daemon属性是已有/并行用户改动，保留不暂存。

## 2026-11-16 Android播放独立进程（最新继续点）

- 用户授权实际改造：私有bound Service、独立:playback进程、固定MediaPlayer Looper；
  sample异步镜像保持原LocalAudioPlayer操作及FoliaLyricsView入口，Desktop不迁移。
- 已完成确定性快照镜像：会话/事件/命令过滤，单调时钟有界外推，暂停/缓冲/seek冻结，
  反向/连续seek与discontinuity、完成重播、死亡后不被旧包复活。JVM真实逻辑测试通过。
- 已实现Messenger服务与客户端、取消/换歌/失联/解绑资源，状态事件唤醒暂停中的UI；
  普通APK与隔离应用ID设备探针构建通过，sample全套JVM测试通过。当前adb无设备，探针未运行。
  运行与准确验证边界见docs/android-playback-process.md。下一项：设备隔离探针、文档URI权限和
  真实歌曲控制/生命周期验证，再以两进程合计PSS/CPU和帧分布判断收益。
- 验收须证明实际服务PID不同、控制/媒体位置可用、UI不逐帧IPC、故障及关闭后释放；
  不把能构建当成设备或帧率通过。进程隔离不承诺减少UI卷积成本或总内存。

## 2026-11-16 Native数学热点实验（最新继续点）

- 目标：迁移真实CPU热点，与优化后Kotlin比较，必须包含JNI/复制并保持像素和媒体行为。
- 已提交8362122：小sigma预乘颜色卷积Rust原型，Windows/Android两ABI构建；两平台各两轮真实JNI
  324项/轮ARGB与几何相同，错误buffer拒绝通过；所有数据见verification/playback-stutter/native-colour。
  初版桌面退化已保留，最终连续行累加更快；Android大图观察到收益，小图/桌面仍有不占优项。
- 运行时颜色实验已接入单独开关，默认关闭，原遮罩开关仍只控制遮罩。两主题24整帧对照、
  暂停/反向seek/resize/切回/错误回退与真实JNI调用通过；JVM打包JAR实际加载通过。
  Android arm64实际sample processor216组像素/几何、预算回退通过，两ABI/16KiB/签名检查通过。
  未安装APK、未做Android UI/真实歌曲A/B。运行与证据见docs/native-colour-sample.md。
  下一项：固定片段、画质、100%分辨率，只切换颜色卷积，测帧分布/CPU/总内存；
  继续同步准备/嵌套软件树优化，不拿局部速度代替核心视觉或流畅验收。
- Android进程调查完成（docs/android-playback-process.md）：主线程创建/操作MediaPlayer、每帧poll；Manifest无播放Service。
  调查提交8cbbebb。
  远程Service可行但不会移走UI卷积热点，需Binder状态/故障/生命周期边界；不能仅加process属性。
  官方正文已核对；进程/线程迁移现已按用户授权实施，仍未声称实测收益。
  Kotlin/Native数学库未测，完整Native UI依赖限制不等于数学库不可行；本轮复用现有Rust工具链。

## 2026-11-16 卡顿优化（当前继续点）

- 已提交9d40a77：按实际像素复用白色/中性色重复卷积，保持全部像素/原点/尺寸。
  360专门输入/135原颜色/120嵌套层通过；78新采画面与历史hash完全相同。
  真机独立app_process两对交替，48输出SHA/几何严格相同，白色大图sigma.25
  P50约13→4ms；彩色回退的首候选已拒绝，最终数据与失败候选均保留。
  证据verification/playback-stutter/neutral-blur；没有替换正在播放的APK或整会话提速结论。
- 用户追加顺序：先将降低渲染分辨率做成sample明确可选档位（默认100%，单独记录画质代价），
  再调查有内存预算/取消与seek规则的下一句预热。二者不能冒充无损优化或GPU着色器缓存。
- sample分辨率三档已实现：真实歌曲/演示设置共用并持久化，100%与原舞台像素相同。
  三档内部物理尺寸/RTL真实指针/宿主字体转换通过；30画面暂停、反向和窗口恢复通过。
  首版RTL偏移已按原坐标断言修复。Android构建/签名通过；设备随后断开，未安装/真机试档。
  运行与画质限制见docs/render-resolution.md；下一步固定片段实测三档，再拆分有预算的异步预热。
- 最新真机复采：用户连接设备/配置歌曲后，两轮15秒取得2682/2708样本，均零丢失。
  已安装APK SHA与8a45ecb优化构建产物完全一致；未安装或操作播放器，原进程持续运行。
  635/628条保留帧记录，最长602.520/1059.384ms；图层录制含子栈39.484/39.874%、
  软件blur19.612/20.030%，最长帧均与嵌套绘制/blur/bilerp重叠。
  尚未达流畅目标；不是同片段A/B，不据此计算前后收益。证据verification/playback-stutter/optimized-capture。
- 第一项已提交ca9e07c：ClassicLayer精确零alpha不再执行绘制/子树及滤镜，并清空软件帧、禁止重放旧帧。
  frozen旧实现120个组合逐像素一致；528张重采云阶/classic/字幕PNG完全一致；暂停/反向重访通过。
  具体分配测量和限制见verification/playback-stutter/transparent-layers；不宣称整会话提速。
- 第二项已提交8a45ecb：Kotlin辉光遮罩按有限卷积支持范围缩小临时计算区域，输出尺寸/位置/全部像素保持。
  345张遮罩与全尺寸参考逐像素相同；135组软件颜色blur及原内核对照通过；
  36张新采云阶换行/Arabic/词组图与ca9e07c完全相同，原版几何/暂停/反向seek检查通过。
  JVM640×500留白场景P50 16.73→6.52ms、托管分配12.80→8.01MB；非真机播放结论。
  Rust路径保持原实现；本项不声称Rust或全渲染提速。证据verification/playback-stutter/cropped-masks。
- Android/JVM共享示例编译、默认含两ABI native的debug APK及签名检查通过；
  包位于build/outputs/folia-sample-stutter-debug.apk，SHA见上述checks.json。
  本轮真机已确认运行此包，但未建立受控的同片段旧版A/B，也未补完整UI验收。
- 下一项：优先检查可见的小sigma嵌套software路径及后代栅格尺寸/缩放，
  以同像素修复降低同步绘制开销，再做同片段A/B；不更改blur/opacity曲线或视觉验收标准。
- 未解决：非零alpha小sigma整棵树软件回退、同步准备/真实播放峰值，仍需同歌同设备A/B。

## 2026-11-16 当前真机播放重采

- 用户要求先抓当前卡顿，暂停代码优化，不安装/操作播放器。两次用户态15秒采样分别2894/2676样本，零丢样。
- 云阶软件blur含子栈21.0%–34.4%，图层录制33.2%–48.7%；不同歌曲时间段，不是优化A/B。
- 同步保留349条去重帧记录，最大966.892ms；其235个CPU样本中222个图层录制、164个软件blur。
  另有175–188ms帧与同步GlowMask准备重叠。仅时间重叠，不作独占耗时/全会话FPS结论。
- 证据：verification/playback-stutter/current-capture；原始采样留.work/stutter-*，无用户内容入库。
- 后续零alpha绘制修复验证已完成，见上方当前继续点；尚未安装/真机复测。
  再处理嵌套software与同步遮罩准备，最终同设备同歌同配置A/B。保留用户Desktop与daemon改动。

## 2026-11-16 Kotlin/Native 编译尝试（当前继续点）

目标：学习kmp-native-wizard，调查当前示例脱离JVM的可行性；完整窗口/文字/真实音频才算Native播放器。

- 新增按需sample:native-probe，直接复用生产时钟和快照；Windows x64 Kotlin/Native2.4.10
  编译及实际EXE运行通过，pause/speed/seek/loop/replay断言通过。无GUI/音频，不宣称完整示例移植。
- 完整UI必要依赖诊断：Windows/Linux缺lyrics-core与Compose UI等Native变体，真实解析失败；
  macosArm64 klib解析通过，尚无Mac编译或窗口/音频运行证据。
- 默认Android/JVM shared编译检查通过（UP-TO-DATE）；库发布目标未扩展，用户Desktop/daemon改动保留。
- 证据与命令：docs/KOTLIN_NATIVE.md、verification/native-probe；没有Native性能改善结论。
- 下一项：有Mac主机时先编译最小FoliaLyricsView和平台actual，再做文件/播放；
  Windows/Linux完整Native UI需真实图形后端和已发布模型支持，不能靠该命令行模板绕过。
  此调查不取代仍未完成的classic视觉残差与真实歌曲卡顿验证。

## 2026-11-16 默认 native 打包与内存（当前继续点）

目标：sample默认携带可用native，Kotlin仍为运行时默认；减少模糊临时内存而不减画质。

- 已提交28f59ad：Android默认两ABI、Windows sample JAR捆绑DLL和分平台加载/诊断。
  JDK21/JBR25空library.path的真实JNI通过；缺库UI回退三测试通过；false构建无残留。
  默认Android debug APK两ABI/16KiB/签名检查通过，本轮未做设备UI运行。
- 已提交a01afca，内存修复：复用卷积数组、全尺寸时免裁剪复制、恢复native时释放软件帧引用。
  JVM640×320每次少约2.34MiB；Android合成大图两轮少12.6%–13.7%托管分配。
  405组像素、原版CSS覆盖及云阶30场景暂停/seek检查通过。CPU耗时波动，不宣称稳定提速。
  证据：verification/playback-stutter/memory。
- 仍未解决：嵌套tiny-sigma软件回退、同步准备、真实播放停顿及进程内存峰值。
  不能把本轮分配下降当成整会话PSS或FPS改善。
- 已提交240c10e：Cargo缺PATH自动定位已有私有工具链；Linux/macOS sh构建与平台资源选择已接入。
  Windows默认无Cargo参数的真实构建/加载通过；Unix脚本仅编排测试，实际主机尚未验证。
- Maven三个变体、许可声明和独立消费已通过，便携Python验证脚本支持三主机。未远程发布。
- 下一项：同设备同歌固定片段/设置复测；有Linux/macOS环境后实测；再回到倾诉及classic上游视觉残差。
  用户Desktop Gradle配置与daemon属性保留，不暂存。


## 2026-11-16 真实歌曲卡顿采样（最新继续点）

当前目标：按真实设备CPU热点修复卡顿，保持主题效果；局部内核提速不能代替流畅度验收。

- 用户配合播放，15秒simpleperf取得3390样本/零丢样；调用栈确认当前主题云阶。
  软件模糊含子调用25.01%、ClassicLayer.record34.48%；嵌套软件绘制与同步准备需继续处理。
  保留的重叠帧记录中有约1.56秒长停顿，非完整会话掉帧率。
- 第一项修复：缓存Bitmap尺寸、显式读取范围、按行复制裁剪像素，避免像素循环JNI。
  同设备独立app_process合成1200×500单次CPU模糊P50约减少17%–25%；仅一轮探索性对照。
  8组输出尺寸/位置/ARGB校验值一致；JVM405组旧实现逐像素对照通过。
  云阶30场景/360画面含pause/reverse-seek及原版几何检查、blur覆盖检查通过，两端库编译通过。
- 最终native-enabled Android包已构建并检查两ABI/16KiB/签名；设备断开，未安装，未做最终包UI/帧率复核。
  证据、命令、限制：verification/playback-stutter/README.md。
- 未解决：tiny-sigma导致整棵绘制树软件回退；同步遮罩/引导线准备与大尺寸图层。
  不声称第一项修复已解决秒级停顿；不降低画质掩盖问题。
- 下一项：检查透明组的无效软件录制，建立嵌套fallback的有界/同像素修复；重连后同歌同片段/设置A/B。
  Rust开关只处理辉光遮罩，不代表全渲染加速。用户Desktop配置及daemon属性继续保留、不暂存。

## 2026-11-16 示例真机 A/B 入口（最新继续点）

当前目标：交付可切换实际 Kotlin/Rust CPU 辉光遮罩的示例，保持当前 classic/云阶像素与媒体行为。
完成条件：真实 JNI 调用／诚实回退、同帧像素／pause／seek／resize验证、含两 ABI 的可安装 APK。

- 已实现 scoped 遮罩注入、两处显示设置开关、启动自检、实际调用计数、错误和有界工作区回退。
- Windows 两主题24组成对画面零ARGB差异；Android两ABI构建及16KiB打包检查通过。
- 首次无设备；后续真机UnsatisfiedLinkError已定位为安装包缺少.so，更新含native包后
  arm64/API37加载/启动自检/15组CPU像素对照通过。设备帧率/完整UI仍未验证；
  新证据见verification/native-mask/device-load-fix，既有上游视觉残差不变。
- 已提交42ccb96（开关）及0d5f74f（缺库诊断与真机复核）。
- 本项验证、命令与证据见native-mask-sample.md；含native包已保留数据更新到连接设备，由用户继续测流畅度。
- 下一项：结合设备反馈定位实际掉帧路径，再继续倾诉分段／字序／转场及原版视觉对照。
  不无限扩展native框架，不把仅遮罩开关显示为全渲染引擎。
- 用户Desktop打包配置与gradle-daemon-jvm.properties保持原样，不暂存。


## 2026-11-16 多主题执行入口（覆盖下方旧下一项）

最新继续点：倾诉内部舞台/144帧/258行与3882字素Motion目标对照已经建立，
但Arabic完整塑形与源逐EGC DOM存在大差距，未接公开入口。themes-parity.md保留实际残差。
2026-11-16用户新增授权：可以实验Rust改善性能。先验证遮罩JNI单热点真实收益，
Kotlin仍独立默认，不能把实验DLL称为全渲染后端或以性能挤掉主题视觉验收。
下一项：完成有边界/像素/交替测量的native实验；再继续倾诉段宽、字序、CSS过渡及公共450ms时间线。

- ed80f1b保存倾诉内部舞台和实际原版对照；两端构建通过，完整视觉未达标/未公开。
- Rust真实DLL/JNI实验已完成30组像素、错误buffer及三进程交替基准；
  对同算法Kotlin没有稳定优势，不启用生产。证据见performance.md与verification/native-mask。
  下一项先验证/落地连续扫描Kotlin热点修复，然后继续倾诉；不能无限扩展native工程。

- 3f3ee55保存真实Rust遮罩JNI实验及三轮公平对照，未启用生产后端。
- Kotlin连续纵向扫描已验证72组旧递推精确值及372张当前基线画面不变；
  640×320 CPU+ARGB配对P50 5.7849→4.1757ms，两端构建通过。不是全帧/设备性能结论。
  下一项恢复倾诉视觉：段间距/完整塑形与源EGC布局、CSS错位/脉冲过渡及450ms公共时间线；
  保持未通过的Arabic残差，不以native实验代替主题交付。

当前目标：云阶Partita独立文字布局、块级动画、引导线和GlowWord效果，
在固定原版同字体/视口/媒体时间建立数值及画面对照，再接公共入口。

- [x] 内部依高度拆块、种子姿态、混合em/px避让；原函数216布局/612行全部通过，
  两端构建及原分组/几何回归通过；来源/结果见themes-parity.md。不是视觉验收。
- [x] 块入场/活动/余韵，逐词和内部字素辉光，引导线、字幕字号及句转场；视觉仍有残差。
- [x] 30场景/360帧、暂停/seek/resize重建；公共入口和示例主题选择。不是整主题最终验收。
  已保存4fb71ed（布局前置75799d8）；完整数值/残差证据见themes-parity.md。
- [ ] 云阶剩余栅格/避让画面残差、CJK/混排、字幕和平台运行；详细结果见themes-parity.md。
- [ ] 倾诉→浮名→心象→群唱→时计；镜台先调查三维核心与既定边界。

来源/差异见themes-parity.md；classic既有残差、标准和历史证据不删除。
其他主题未接公共入口，不以数值内核或构建成功宣称效果完成。
下一项：倾诉真实分段/重拆/拟合/字素计时的原版对照，然后独立文字舞台。
倾诉内核已完成378种分段、2430种合成宽度布局及原版pulse/内部计时对照；
尚未接公共入口。具体下一项为普通/斜体两种文字舞台和实际浏览器截图。
用户Desktop打包配置及gradle-daemon-jvm.properties保留、不暂存。

以现有可运行版本为起点，每项完成后独立验证并提交；未勾选项不代表已支持。
保持 Android/JVM、classic、Kotlin 和 AGPL-3.0-only 边界。

## 当前执行清单（原版效果第一优先）

用户已恢复执行。当前目标仍是固定原版classic核心对照，不扩示例、发布或Rust。
本地音频/等待球的有界调整已完成，证据与限制见local-playback.md；旧暂停记录只作历史。

本轮已保存：
- 0a8c296：三字体/颜色的原版、JVM和API34覆盖控制；Android彩色路径没有整体改善。
- bea2940：本项目包名及Maven group改为top.ghhccghk前缀；两端构建、新坐标本地
  独立消费JVM渲染/回调及Android编译通过，108张控制PNG不变。外部lyrics-core
  保留com.mocharealm坐标/包名，来源归属不改；本项未冒称重跑设备UI。
- 012a907正文覆盖修复：Windows保留完整shaping/原比例纹理，按实际正文颜色栅格化，
  只缓存最近颜色；Android及其他Desktop仍用原白色mask。128条原版实际正文颜色
  逐条一致、暂停/逆向和缓存不可变性通过；两端构建通过。详见classic-parity.md末节。
- 半透明辉光：修正每层阴影颜色alpha与字素span opacity的合成次序；27个原版
  半透明控制全部改善，alpha MAE均值1.348241→0.115102、最大22→2。
  公开入口颜色变更/重新挂载/暂停/seek、两种图层路径及两端构建通过；
  原42+108个不透明PNG严格不变。没有本项新的Android设备或Desktop OS截图验证。

未解决差异：彩色完整54帧39改善/3退步/12不变，默认42帧14改善/16微退步/12不变；
正文隔离54帧42改善/0退步/12不变。不能从隔离结果推定完整合成的误差抵消原因，
也不把这些退步改称允许差异。完整classic仍未验收通过；Android覆盖/复杂字幕仍待修。

下一项：针对冷色1200ms三字体，分离原版真实辉光与正文合成、图层采样历史；
保留相同字体/视口/媒体时间，核对字素遮罩与浏览器真实span几何，再按证据修复。
不通过拟合gamma/加粗、改预期或关闭检查掩盖残差。GUI工具不可用时不反复重试；
继续可独立的数值/离屏对照，设备/OS截图验证准确标明边界。
运行期间检测到sample/desktop-app/build.gradle.kts外部打包配置改动；保留、不暂存。
用户遗留gradle/gradle-daemon-jvm.properties同样不修改/提交。

目标：固定 cc6226e4 classic 与 Kotlin/Compose 文字行为闭环。完成条件是同源输入、
字体、视口、时间点的行为及画面验收；构建、测试/提交数量不能代替效果。

已完成：
- 45b69fc：下一句预览固有宽度/溢出/672px省略与光垫；336原版对照帧，
  API30/34各96请求，旧260帧不变；残差未豁免。按用户要求完成后暂停。
- 2beddf8：显式行高字幕paint基线，覆盖字形控制精确匹配，旧字幕PNG不变。
- a590f15行盒/滤镜修复：36个原版DOM中心/预算；Calm wrap2800ms
  MAE18.12→3.78，保住此前四个退步动画点；API34三档各72请求有效，残差和限制见classic-parity.md。
- b65c46b：Desktop Direct3D真实窗口60请求与800→380→800，内部图层三方对照。
- 84549ac：原版同终态/不同栅格历史对照，拒绝造成完整画面退步的直接段落实验，生产代码已恢复。
- 9d375a1：原版DPR1/2静态布局取整/连续Motion分离，完整场景和公开入口对照；API34实际56请求。
- 显示词/塑形运行/源字素计时分离，语义粘连、间距、旋转包络、换行和避让原版对照。
- 正文/字素辉光、入场/活动/余韵/退场、7s呼吸与实际Motion数值对照。
- 字幕布局/曲线/实际基线/主题光垫，透明默认；API30 Kotlin连续模糊回退。
- 40864ca：Android连续文字计量；3ac7a8b：正文实际blob基线；6074280：字幕分数居中。
- API30/API34最终APK各166个不同帧和6次逆向seek重访，暂停/重访严格像素一致。
- 同媒体网格246帧/侧密集原版/Kotlin序列，328条实际Motion状态严格数值对照通过。
- 11bc4ad保存密集对照；Windows正文LCD/阴影AA分离后连续最高误差5.13→4.60，
  Android两端各28次检查与原PNG完全相同。
- API30/API34真实screenrecord播放两句及两次暂停seek；双PNG严格一致，解码帧已目视。
- 原生阴影源覆盖与正文paint恢复：隔离双层辉光MAE1.11→0.31；两端各172次严格检查。
- 1e56c60：阴影源修复；API30/API34逐字advance控制实验保留原浏览器、字体表与native数据。
- 7f92c1c后继续分离实际字形覆盖：64倍控制改善advance，但小字号源覆盖MAE仅26.80→25.95。
- Compose与原生Paint两端各5字号alpha逐像素相同，保留平台输入残差；不以此豁免整帧验收。
- 密集换句：原版901ms为12个退场+1个当前句，已移除4句截断并按退场期限保留；
  同源曲线/逆向seek通过，普通246帧hash保持。视觉证据见classic-parity.md最新章节。
- db34c26：normal/fast入场中断，真实Motion的句ID/opacity/scale/blur误差<.0001；公开入口28帧、
  28次逆向重访及暂停严格相同；PNG与误差保留，未宣布整帧视觉通过。
- 433be7a：六点间奏，保留独立点词/完整点串，按原版居中/48px词距/1.5缩放/纵向偏移；
  零目标旋转spring跨状态继续运行。原版几何/Motion通过；API30/34各96请求严格暂停/重访。
- 830ab84：混排正文默认继承整句方向，修复RTL数字/标点词内顺序，排除RTL整数容器原点偏移；
  同一许可字体的原版字符框与公开32帧通过，两API各64请求严格暂停/逆向一致。
- 首Letter方向谓词按原版十个Script精确匹配，补齐罕见RTL脚本/中性前缀反例；
  逻辑对照及两端构建通过，不冒称罕见脚本文字画面已验收。

尚未解决：
1. 完整文字覆盖/变换采样残差：静态CSS布局原点取整后，连续baseline前景MAE最高3.43；
   复杂场景仍有残差，CJK avoid1500ms误差从2.19回升2.99，不能视为整帧验收通过。
2. Android小数advance仍与浏览器略异；字幕长句MAE约10.6，不能笼统归为AA而宣布通过。
3. Desktop已在Direct3D真实窗口验证800→380→800、60次暂停/逆向请求；
   捕获的是内部GraphicsLayer。外部UI观察工具仍初始化失败，OS截图/输入/连续录屏未验证；
   API21..29与实机未验证，模拟器不能作为性能证据。

证据/源码位置/调查结果统一见classic-parity.md；图片和CSV在build/verification。
2c6ebed短窗口避让已修：180/200px高时使用原版62/76预算；停止条件也按整数offsetHeight，
避免76.046875px行在76预算里又缩一轮。36个原版画面/几何、72次JVM正逆向、
公开入口/核心相同、暂停和尺寸恢复通过；失败及中间实验保留。API34短视口72请求通过。
拟合字号基线修复已通过JVM及两端构建，API34重采240请求通过（fitted-baselines-android.log）。
原版10个拟合字号基线、Latin/Arabic/CJK控制已保存；段落reported baseline不能代表实际paint，
原生TextLine与段落alpha零误差的控制证实自然行高应使用rounded raw ascent。
只换CSS度量的候选令Chaotic2800误差4.65→38.88，已拒绝并保留fit-baseline-natural-only；
联合修正恢复正常高度三档/连续/八场景全PNG完全一致，normal180短视口11.13→2.84。
API34的120张不同PNG与上版完全相同，不能声称Android覆盖改善；字幕全部回归通过。
显式行高字幕已修复实际paint与reported baseline不等的CJK14px偏移，36组native alpha精确匹配；
84帧原版对照/168次正逆向、暂停通过，旧260张字幕PNG不变。来源/失败候选/残差见classic-parity.md。
已修复：原版下一句预览inline-block固有宽度可越过380视口，恢复min-content、
672px省略边界及光垫定位；同源完整覆盖字体336帧/暂停/逆向验证，原260帧不变。
受影响4帧MAE17.46→3.71；Direct3D实际窗口36请求，API30/34各96请求均严格暂停/重访。
完整数值、字体来源及未通过的字形覆盖残差见classic-parity.md末节。
用户要求完成本项后暂停（2026-11-15）；本项保存后不开始新功能。
恢复时下一项：继续定位整帧字形覆盖/采样残差，优先Android小数字号及复杂字幕，
以同源同字体控制隔离原因；必要时补混排/连字字幕对照。不得降标准或回到示例扩展/发布。
Calm分数居中及三档API34画面已重采，但整个classic栅格尚未等价；
API34最高残差18.14、正文小数字形覆盖/字幕残差继续保留，不能据此宣布最终视觉通过。
normal/fast中断入场的Android验证已完成：两API各56请求，28画面及28逆向重访，暂停双截图严格相同；
fast1241ms仍有约15.3 RGB MAE，已加入原版/JVM/两API图，不能隐藏。debug源码/构建/设备条件见classic-parity.md。
C盘空间不足已通过项目私有API34 AVD和F盘临时目录绕开；首次全屏系统引导造成暂停失败的证据保留，
确认系统状态后从空白舞台重采全部条件通过，未削弱断言或修改用户原AVD/文件。
完整JVM82项中密集退出首次1分钟超时，隔离复跑47s通过；未调整测试/容差，完整失败日志保留。
64倍字号与被拒绝的直接段落路径均未带入生产，原PNG与验收标准保持。
示例扩展、发布和非必要优化暂缓；旧性能数据不代表新栅格路径，Rust尚未引入。

## 历史工作（保留）

1. [x] 修正主句切换、空档和切歌时的状态报告，验证不会沿用旧句的词数或降级状态。
2. [x] 增加可重复的连续媒体时间检查点，覆盖入场、逐词、退场、空档、随机 seek 和减少动态效果。
   输出合成内容图片与测量条件，不把平台字体图片当作跨机器 golden。
3. [x] 测量双层辉光遮罩内核的耗时与 JVM 分配，复用栅格与缓冲区，并检查像素一致性。
   完整文字排版、质量差异、GPU 与设备测量仍未完成。
4. [ ] 评估整个可见场景的遮罩预算与准备生命周期，避免把每行预算误称为总内存上限。
   已增加主句16MiB、所有未完成退场句合计16MiB的持有遮罩像素上限及逐级降质；
   临时准备资源、GC/GPU 开销、异步准备与生命周期测量仍待完成。
5. [ ] 重新验证 Android 最新变换命中、RTL、大字体和原生交互；区分构建、模拟器和实机结论。
   API34 已复查最新命中／空白不命中、1.5 倍系统字号与 Arabic 塑形；
   Compose 增加窄屏 RTL＋大字号验证。Android RTL locale 与其他原生边界仍待验收。
6. [ ] 增加导入历史与进程重建恢复，显式处理文件已失效与 URI 权限不可保留的情况。
7. [ ] 补齐依赖声明审计、干净检出验证和独立消费验收，更新运行与交付文档。

视觉等价仍需固定且许可明确的字体与固定 Folia 基线浏览器对照。全帧 CPU/GPU、
纹理上传和设备内存测量尚未完成；不据此宣称高帧率，也不提前引入 Rust。

已有验证与限制见 implementation.md、visual-spec.md、performance.md、android-smoke.md。
用户遗留的未跟踪 Gradle daemon 配置不纳入提交，不修改只读参考工程。

## 历史暂停记录（2026-11-15，现已恢复）

下述工作后续已在0a8c296完成验证和提交，以当前执行清单及classic-parity.md最新节为准。

因影响用户使用电脑，已停止本轮浏览器服务器和私有 API34 模拟器，不再构建/采集。
当前新增多字体/多颜色静态覆盖控制，生产 renderer 未改；源码/脚本/JVM测试仍未提交。
原版三字体×两字号×三颜色×两背景36帧已采集，浏览器白色mask再着色与直接彩色文字
存在差异，尚不能据此宣布Kotlin修复完成。原20帧参考conditions SHA已核对恢复一致。
Android新探针APK构建通过；彩色矩阵采集曾遇内存不足、ADB瞬断，日志保留在.work/
coverage-colors-*。最终采集是否完整及结果需恢复后检查，不宣称已验证；JVM新测试未运行。
恢复先检查build/verification/foreground-coverage-colors-*与日志，不重做已通过的旧调查。
下一项：完成同条件JVM/API34直接彩色、彩色缓存和白色mask着色对照，再决定最小生产修复。
用户遗留gradle/gradle-daemon-jvm.properties仍未修改/提交；本轮不提交未完成验证的改动。
