# Android 私有播放进程

2026-11-16。按用户授权，Android sample 已将 MediaPlayer 迁到私有 `:playback` bound Service。
`8cbbebb` 是改造前调查；`fa4565b` 增加确定性镜像。本页描述当前实现，设备验证仍待完成。

## 实现边界

- `sample/android-app/src/main/AndroidManifest.xml`：Service 为 `exported=false`、`process=":playback"`。
- `sample/shared/src/androidMain/.../playback/RemotePlaybackService.kt`：专用
  `FoliaPlaybackEngine` Looper 创建、准备、控制、采样、释放 MediaPlayer，回调也在该 Looper。
- `PlaybackIpc.android.kt`：Messenger 仅传 URI、命令序号和原始状态；不传歌词、封面或逐帧画面。
  检查同应用 UID、会话和回复 Binder；最多四个并存的准备/播放会话。
- `RemoteAndroidAudioPlayer.kt`：每会话专用桥接线程处理 Binder，UI 的 poll 只读单槽邮箱与镜像，
  不调用远端或 MediaPlayer。异步事件通过 updateRevision 唤醒暂停、缓冲、错误状态中的 UI。
- `sample/shared/src/commonMain/.../playback/RemotePlaybackMirror.kt`：以跨进程共用的
  elapsedRealtimeNanos 和最新播放器位置重建；最多外推 250ms，超过即冻结，收到新样本才恢复。
  无逐帧时间累加，也没有 PreviewPlaybackController 同时推进真实歌曲。
- 播放/seek/缓冲时 50ms 采样；周期包最多一个未确认，客户端桥接线程 ACK，UI 邮箱合并状态。
  准备、控制、seek完成、错误等重要事件立即发送。暂停无周期采样。
- 连续 seek 串行执行 native seek，仅保留最新目标；旧回调不能误完成新目标。
  命令、事件序号及会话过滤旧回包；seek 开始即改变 discontinuity，确认不重复增加。
- 原歌曲可与准备中的候选共存；原 SongSession generation 规则继续负责取消和替换。
  准备超时 15s，客户端等待最多 20s；取消/失败释放候选，metadata 仍在 UI 进程 IO 上读取。
- Binder 死亡/断连冻结快照、显示错误，终止会话；用户重新选歌建立新会话，不自动复活旧歌曲。
- close 发送 CLOSE、解绑、解除死亡监听并退出桥接线程；客户端死亡也释放对应播放器。
  最后解绑销毁 Service、释放播放器和采样任务。Android 可能保留空闲进程，不能以 PID 仍存在判泄漏。
- 保持页面隐藏暂停和 ViewModel 清理。未增加后台播放、前台服务、系统媒体会话或播放队列。
  FoliaLyricsView、库发布契约和 Desktop 播放方式不变；原 Apache 工程只读。

## 已验证与待验证

- Android 普通 debug APK、独立应用 ID 探针 APK 和 instrumentation APK 构建通过。
- sample JVM 全部测试通过，含单调时间、速度一次、暂停/缓冲、乱序、1000 次确定性随机 seek、
  死亡后终止、完成重播，以及 SongSession 取消/失败候选/换歌/暂停时断连恢复。
- 当前 adb 无设备；SDK emulator 目录无 emulator.exe。本轮未安装或操作用户播放器。
  **真实 Service PID、实际播放/seek、远程死亡、资源释放、文档 URI 跨进程权限仍未在设备验证。**
- 未做同歌同设备 A/B，不能声称改善卡顿、音频断续或总内存。UI 图层/软件模糊热点仍在 UI 进程；
  额外 ART、线程和 Binder 可能增加两个进程合计 PSS。

## 运行和隔离设备探针

普通构建：`./gradlew :sample:android-app:assembleDebug`，Rust 包装沿用 sample 现有默认。
独立探针构建：

```sh
./gradlew :sample:android-app:assembleDebug :sample:android-app:assembleDebugAndroidTest -PfoliaPlaybackProbe=true
```

探针 application ID 为 `top.ghhccghk.accompanist.folia.sample.playbackprobe`，安装这组 APK 后运行：

```sh
adb shell am instrument -w top.ghhccghk.accompanist.folia.sample.playbackprobe.test/top.ghhccghk.accompanist.folia.sample.PlaybackProcessDeviceProbe
```

探针不打开 Activity，用自行生成的 12 秒静音 PCM WAV 走真实 MediaPlayer：验证独立 PID、位置前进、
暂停不漂移、快速反向 seek、失败候选不影响原会话、关闭并存候选、杀死仅该隔离包的播放进程、
冻结与重新选择恢复、桥接线程退出。不代表听感或真实歌曲 UI 验收。
本轮已保留构建产物于 `build/outputs/playback-process-probe/{probe.apk,probe-test.apk}`；
普通应用产物见 `build/outputs/folia-sample-playback-process-debug.apk`，均不入 Git。

后续连接设备：先运行隔离探针，再在用户允许的时点安装普通包，选用户文档 URI 实测暂停/seek/
切歌/退后台/关闭。固定歌曲片段和画质，记录两个进程合计 PSS/CPU、控制延迟和 UI 帧分布再判断收益。

## 官方约束依据

已核对 Android 文档：

- https://developer.android.com/guide/components/processes-and-threads
- https://developer.android.com/reference/android/media/MediaPlayer
- https://developer.android.com/guide/topics/manifest/service-element

Service 默认在所在进程主线程，必须另设 Looper 才能隔开控制线程；MediaPlayer 创建和访问必须同线程。
进程隔离不等于消除 UI 计算，也不保证进程不被系统终止。
