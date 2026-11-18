# 内嵌歌词验证 — 2026-11-18

## 复现

先在独立目录安装**验证工具**（不进入 app 或 Gradle 依赖）：

```powershell
python -m pip install --target .work/embedded-python mutagen==1.47.0 imageio-ffmpeg==0.6.0
$env:PYTHONPATH=(Resolve-Path .work/embedded-python).Path
python verification/generate-embedded-audio.py .work/embedded-fixtures-v1
.\gradlew.bat :sample:shared:verifyEmbeddedAudio --args='.work/embedded-fixtures-v1 build/verification/embedded-lyrics/desktop-production-tags-and-audio.txt'
.\gradlew.bat :sample:shared:jvmTest --tests '*EmbeddedLyricsTest' --tests '*EmbeddedLyricsUiTest' --tests '*LocalSongTest' --tests '*SongSelectionTest' :sample:shared:compileAndroidMain :sample:desktop-app:compileKotlin
```

生成器要求新目录，拒绝覆盖旧输入；12 秒原创静音 PCM＋原创两行中英歌词，
FFmpeg 7.1 和 Mutagen 1.47.0 写标并由 Mutagen 回读。六文件 SHA／大小及版本见
fixtures-manifest.json；音频二进制在忽略的 `.work`，不提交歌曲或工具二进制。

## 结果与区分

- 9 项数据／事务测试：ID3v2.2/3/4、四编码、BOM 继承、扩展头、两种 unsync、
  TXXX、明确 SYLT 限制、FLAC 多字段、MP4 atom／UTF-16、WAV padding、截断／
  长度／坏 UTF／取消关闭、外部优先、翻译、纯文本、同播放器补选和过期拒绝。
- 3 项新增离屏 UI：420×800 计时标签经真实公共 FoliaLyricsView，点击 1000ms 行＋
  offset250 回到媒体1250ms；900×640纯文本无点击／等待球，外部替换不动原播放
  快照；损坏标签提示可见且不变成会话失败。UI 使用明确测试播放器，不冒充设备。
- 6 项原 LocalSongTest＋2 项原 SongSelectionTest 回归。共20项成功，零失败／跳过。
- 独立六文件全部经生产 DesktopSongFile → 共用标签 reader → 实际 core parser，
  输出起点1000/3000ms。真实标签 WAV 经生产 prepareDesktopAudio／SongSession
  打开实际 Java Sound Clip，游标推进、暂停seek1500稳定并释放。**其余五文件只验
  标签读取，未扩大 Desktop 解码承诺。** 实测记录 desktop-production-tags-and-audio.txt。
- Android shared 与 Desktop app 编译通过；Android debug APK 构建／验签另见本目录
  validation.json／android-debug 下的记录。没有安装／传输到用户手机，不宣称 Android
  OpenDocument／私有播放进程真机端到端、实际喇叭回录、性能或原版主题视觉通过。
- 两张离屏截图在 `sample/shared/build/verification/embedded-lyrics`，均已图检：
  embedded-timed-narrow.png、embedded-plain-wide.png。不提交可重建画面。
  本轮没有新增真实 Desktop 窗口／原生选文件对话框运行证据。

功能提交 `7ca2823`；debug交付为
`build/deliverables/android/lyrics-ui-folia-debug-7ca2823-20261118-201924.apk`，
SHA256 `90115417823bfb4dd9b47c31de5e667bdca64b7f46ef6fe6a36f4319a31114fe`。
原证书、两版工具v1/v2、ZIP CRC、16KiB对齐、两ABI Rust、五个新功能DEX符号及
副本一致性检查通过。此后的reader改动仅澄清“ID3有界整体读入，APIC也占限制”的
注释；功能／测试逻辑未改，APK仍准确对应功能提交。未安装／传输，不能据本机验签
推断手机验签／安装问题已解决。原两项外部遗留hash保持不变并排除在提交之外。

## 保留的首次失败

`.work/embedded-tests.log`：新测试最后一条 assertFailsWith 导致推断返回异常对象，
JUnit 要求 void，补显式 Unit。`.work/embedded-tests-v2.log`：误用 Desktop KMP 任务
名，修为实际 compileKotlin。`.work/embedded-tests-v3.log` 与 embedded-ui-hang.txt：
错误提示 UI 测试让测试播放器一直 playing，自动测试时钟等 idle 无法结束；jstack
确认在该用例 setContent/waitForIdle 后，仅停止本轮专用 Test Executor。测试在打开
后先检查实际 playing 再暂停来固定截图，真实连续播放独立由 Clip 探针验证；没有改
生产帧循环、弱化断言、禁用测试或隐瞒停止日志。最终 `.work/embedded-tests-v4.log`
20项通过。随后解析失败提示／单一content来源与增强LRC字词时钟断言完成后，
`.work/embedded-validation-final.log` 再次运行全部20项、六文件生产读取／Clip探针和
两端编译通过；validation.json保存源码hash和最终结果，没有沿用旧通过覆盖新改动。
