# 示例内嵌歌词 — 2026-11-18

## 使用与来源选择

“选择歌曲”中音频仍必选。歌词留空时自动读取歌曲标签；外部歌词文件优先，
其解析失败会保留原会话，不偷偷改用标签。来源显示为“外部歌词”“内嵌计时歌词”
或“内嵌纯文本歌词”。多个标签按容器顺序查找，第一个实际可解析的计时文本优先；
没有计时文本时使用第一个非空文本。没有歌词仍显示“未加载歌词”。

计时文本使用已发布 lyrics-core 0.5.0 的 AutoParser，标准／增强 LRC 的原时间和
逐词结构保留，显示仍由原播放器快照与 FoliaLyricsView 驱动。纯文本提供滚动／选择，
不派生时间，不高亮、不点击跳转、不显示等待球。字幕主题设置不改写原标签。

可在“补选歌词／翻译”选择外部文件替换标签；清除外部歌词后重新应用，会回到内嵌
来源。独立翻译可与内嵌计时文本按实际起始时间戳匹配，无时间轴／无匹配则明确报错。
补选不重建／seek 原播放器。标签损坏、过大、未知编码／不支持帧有可见提示，
不阻断可解码的音频；音频解码失败仍保留原会话。取消和过期请求沿用 generation
检查，解析完成后也先检查 token，过期请求不会再申请音频设备。

## 本轮支持的标签

| 容器／标签 | 读取内容 | 验证层级 |
| --- | --- | --- |
| MP3 ID3v2.3／2.4 USLT | Latin-1、UTF-16/BOM、UTF-16BE、UTF-8；描述与正文分离，支持 unsynchronisation | 独立 Mutagen 写标的真实 MP3＋数值测试 |
| ID3v2.2 ULT | 原结构文本 | 合成二进制测试，未做独立真实 v2.2 音频 |
| ID3 TXXX／TXX | LYRICS、UNSYNCEDLYRICS、UNSYNCED LYRICS、SYNCEDLYRICS | 合成帧测试 |
| FLAC Vorbis comment | LYRICS、UNSYNCEDLYRICS、SYNCEDLYRICS（键不区分大小写） | 独立真实 FLAC 的前两种字段＋合成多字段测试 |
| M4A／MP4 `moov/udta/meta/ilst/©lyr/data` | UTF-8／UTF-16BE，跳过音频及其他字段 | FFmpeg AAC＋Mutagen `©lyr` 真实文件；扩展 atom／UTF-16 合成测试 |
| WAV RIFF 的 `ID3 `/`id3 ` 块 | 上述 ID3 文本，处理奇数字节 padding | Mutagen 写标的真实 PCM WAV，并实际打开 Java Sound 播放 |

字段名不代表字段内必然有时间轴：例如 USLT 中存 LRC 时可以同步，纯文字则不可同步。
SYLT 的二进制时间记录、压缩／加密／分组／data-length-indicator ID3 帧、Ogg、
APEv2、Lyrics3、MP4 freeform 字段等本轮**未实现**；遇到可识别的 SYLT／不支持帧和
Ogg 会提示补选外部歌词，其余未识别容器／字段视为没有可用标签。不宣称全格式支持。
非规范 GBK 冒充 Latin-1、损坏 UTF 文本不猜编码。ID3 位于 FLAC 前且不含歌词时，
尚不继续搜索后面的 FLAC 标签；顶层至 EOF 的 MP4 size=0 atom 后不再搜索。

Android 使用 OpenDocument 原 URI 独立打开流，IO 线程读取并关闭，不复制整曲或要求
整盘权限；后续仍用已存在的私有播放进程。Desktop 文件流同样在 IO 线程读取。
此项不改变音频编码支持：Desktop 仍仅承诺 JDK 的 PCM WAV／AIFF／AU，
能读 MP3／FLAC／M4A 标签**不等于** Java Sound 可以播放这些编码。

元数据实际读取累计最多 16 MiB，单 ID3 标签最多 8 MiB，解码歌词总计最多 1 MiB／
16 个字段；最多 8192 块，MP4 深度最多 8，单次跳过最多 32 GiB。音频／非歌词
容器块优先 skip，流不能 skip 时也受读取上限保护。ID3 标签整体有界读入后检查帧，
其 APIC 封面字节也计入 8 MiB 标签限制，不额外解码／复制为封面。每次读取／跳过／块处理检查取消。
无逐帧标签读取／文件 IO，不加播放器、JNI 或库依赖。

## 实现依据与边界

EmbeddedLyricsReader 为本项目依据公开格式独立编写，没有搬入外部标签库实现：

- ID3v2.3.0：<https://id3.org/id3v2.3.0>，4.11 USLT、4.12 SYLT 和文本编码／帧头。
- ID3v2.4 frames：<https://id3.org/id3v2.4.0-frames>，4.8 USLT／4.9 SYLT；
  结构见 <https://id3.org/id3v2.4.0-structure>。不把 SYLT 冒充 USLT。
- FLAC 规范 RFC 9639：<https://www.rfc-editor.org/rfc/rfc9639.html>，Vorbis comment 块。
- MP4 文本字段用实际 Mutagen 1.47.0 的 `©lyr` 写标／回读独立验证；
  字段记录见 <https://mutagen.readthedocs.io/en/latest/api/mp4.html>。

原 SelectedLyrics.kt 的 Apache 来源头保留；本次只是 sample 文本组合与新元数据
读入，没有向原 Accompanist 工程写入内容，没有改变库入口／许可或歌词 core 模型。

验证、命令、失败保留及未验证的平台见 [verification/embedded-lyrics](../verification/embedded-lyrics/README.md)。
