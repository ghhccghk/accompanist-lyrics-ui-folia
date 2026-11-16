# Android 播放与渲染进程边界调查

2026-11-16。结论：**可以用私有远程 bound Service 分开音频控制和 UI，但本轮不直接改成多进程**。
当前已实测的主要CPU热点仍在歌词的图层/模糊链；把MediaPlayer移走不会减少这些计算。
本轮先交付可独立开关的Rust颜色卷积实验。不是断言多进程无收益，也没有做多进程A/B。

## 已核对的现状

- `sample/android-app/src/main/AndroidManifest.xml`只有MainActivity，没有播放Service或
  process配置。播放实现仅属于sample，库/FoliaLyricsView仍只接媒体快照。
- `sample/shared/src/androidMain/.../playback/LocalPlayback.android.kt`：元数据读图在IO，
  `prepareAndroidAudio`切回Main创建MediaPlayer、注册回调、prepareAsync；
  AndroidAudioPlayer的poll/play/pause/seek/close由UI调用，currentPosition/isPlaying在poll读取。
- `sample/shared/src/commonMain/.../LocalSongApp.kt:LaunchedEffect`：每个withFrameNanos调用
  songs.poll。**UI卡住时快照更新也延迟**；现有样本不证明音频解码器本身在UI线程执行，
  也不证明位置查询是主要热点。不能仅因控制对象在主线程就宣称解码与绘制争用同一线程。
- `verification/playback-stutter/optimized-capture`两轮图层录制含子栈约39%，
  软件模糊约20%。这些比例重叠且不是独占耗时，不能相加或当成IPC优化上限。

## 官方约束（实际获取并核对正文）

- [进程与线程](https://developer.android.com/guide/components/processes-and-threads)：组件支持
  android:process；Service默认仍在所在进程主线程，进程分离不等于工作线程分离。
- [MediaPlayer](https://developer.android.com/reference/android/media/MediaPlayer)：不是线程安全，
  创建和所有访问应在同一线程，回调线程需要Looper。不能把每次poll随机丢到Dispatchers.IO。
- [Service manifest](https://developer.android.com/guide/topics/manifest/service-element)：以冒号
  开头的process名称创建应用私有进程；仅为Service加属性还没有实现控制、同步和故障恢复。

## 可选路径及采用条件

| 路径 | 能解决的边界 | 成本/限制 |
| --- | --- | --- |
| 同进程专用HandlerThread | MediaPlayer控制/采样不被Compose主线程直接阻塞 | 共用堆/GC；须将全部player调用与释放迁到同一Looper，并向UI发布不可变快照 |
| `:playback`私有bound Service | 再隔离控制侧堆/GC和进程故障 | Binder、额外ART/进程内存、会话与死亡恢复；总CPU/内存不保证下降 |
| 独立进程绘制 | 隔离UI计算 | 不是简单搬Compose；需要跨Surface/输入/生命周期协议，仍有GPU争用，超出本轮必要范围 |

如果后续证据显示控制侧被UI/GC拖慢，先比较同进程Looper与私有bound Service。
Service不新增后台播放、通知、系统媒体服务、队列或外部跟随。
当前LocalAudioPlayer的同步控制+UI快照契约需要适配器缓存异步结果，不可偷偷把Binder同步
往返放进每帧poll中。新实现的完成条件如下，尚未实施或宣称通过：

1. Player固定单Looper创建、准备、控制、采样、释放；UI仅读镜像快照，控制使用异步命令。
2. 私有Service `exported=false`；Binder只传命令和状态，不逐帧传歌词/位图。
   建议先50ms采样+事件通知，再实测调节频率；UI使用单调时钟外推，倍速只应用一次。
3. 快照携带会话generation、命令序号、discontinuity、位置、采样单调时间、playing/buffering/
   seeking/speed。暂停、seek开始与完成、缓冲、变速、切歌都重新锚定，拒绝过期回包。
4. 文件URI/描述符按会话管理，准备成功才切换；封面有尺寸预算，不通过Binder传全幅Bitmap。
   取消/连续选择与旧会话保留沿用已有规则；远程进程死亡应显示错误并冻结歌词，
   不能谎称原音频仍在播放。未绑定时停止采样并释放player/描述符/任务。
5. 测同设备同歌固定片段的控制延迟、音频断续、UI帧分布及两个进程合计PSS/CPU，
   含暂停/反向seek/连续切歌/页面关闭/远程死亡/重连。设备功耗和内存也必须计入。

本轮仅核对源码和官方约束，没有安装/启动远程Service，没有停止用户播放。
Kotlin/Native不消除这些Android线程/IPC约束；本轮数学迁移选择复用已有Rust/JNI工具链。
