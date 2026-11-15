package top.ghhccghk.accompanist.folia.sample.fixtures

import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.core.model.synced.*

data class Scene(val id: String, val title: String, val durationMillis: Long,
    val lyrics: SyncedLyrics, val checkpoints: List<Long>)

private fun timed(words: List<String>, start: Int, step: Int = 900,
    translation: String? = null, phonetic: String? = null) = KaraokeLine.MainKaraokeLine(
    words.mapIndexed { index, text -> KaraokeSyllable(text, start + index * step, start + (index + 1) * step) },
    translation, KaraokeAlignment.Unspecified, start, start + words.size * step, phonetic)

/** Original synthetic text; no commercial lyrics, artwork or bundled fonts. */
val scenes: List<Scene> = listOf(
    Scene("mixed", "光与文字 · 中英合成场景", 22_000, SyncedLyrics(listOf(
        timed(listOf("让", "文字", "随着", "时间", "发光"), 2_000, translation = "Let the words follow the light", phonetic = "ràng wén zì suí zhe shí jiān fā guāng"),
        timed(listOf("We ", "write ", "a ", "little ", "light"), 8_000, translation = "写下一点光"),
        timed(listOf("暂停", "之后", "仍在", "这里"), 15_000, translation = "Still here when time stands still")), id = "mixed"),
        listOf(0, 2_000, 2_450, 6_500, 8_000, 15_000, 21_999)),
    Scene("short", "极短行 · 边界检查", 4_000, SyncedLyrics(listOf(
        timed(listOf("微"), 500, 67), timed(listOf("短"), 567, 120), timed(listOf("一", "瞬"), 687, 90),
        timed(listOf("再", "见"), 900, 600)), id = "short"), listOf(499, 500, 566, 567, 686, 687, 900)),
    Scene("long", "长句与长音", 20_000, SyncedLyrics(listOf(timed(listOf(
        "Long ", "notes ", "float ", "across ", "a ", "quiet ", "window ", "while ", "the ", "letters ", "find ", "their ", "place."), 1_000, 1_200)), id = "long"), listOf(1_000, 4_000, 10_000)),
    Scene("gap", "前奏与空档", 20_000, SyncedLyrics(listOf(SyncedLine("前奏结束", null, 5_000, 7_000),
        SyncedLine("空档之后", null, 14_000, 16_000)), id = "gap"), listOf(0, 5_000, 7_000, 10_000, 14_000, 16_000)),
    Scene("voices", "重叠声部", 12_000, SyncedLyrics(listOf(
        timed(listOf("主唱", "保留", "自己的", "时间"), 1_000, 1_500).copy(accompanimentLines = listOf(
            KaraokeLine.AccompanimentKaraokeLine(listOf(KaraokeSyllable("轻声回应", 3_000, 6_000)), "Harmony", KaraokeAlignment.Unspecified, 3_000, 6_000))),
        timed(listOf("另一", "道光"), 4_000, 2_000)), id = "voices"), listOf(1_000, 3_000, 4_000, 6_000, 7_000)),
    Scene("unicode", "Unicode 与完整塑形", 20_000, SyncedLyrics(listOf(
        SyncedLine("á · 👩‍🚀 · 家庭 👨‍👩‍👧‍👦", "组合字符与 ZWJ", 1_000, 6_000),
        SyncedLine("مرحبا بالعالم", "阿拉伯文", 7_000, 12_000),
        SyncedLine("नमस्ते दुनिया", "复杂文字", 13_000, 18_000)), id = "unicode"), listOf(1_000, 7_000, 13_000)),
    Scene("line", "仅行时间 · 不派生逐词计时", 12_000, SyncedLyrics(listOf(
        SyncedLine("只有整句的时间", "Line timing only", 1_000, 5_000),
        SyncedLine("这句也不伪造逐字时间", null, 6_000, 10_000)), id = "line"), listOf(1_000, 5_000, 6_000)),
    Scene("stress", "压力样本 · 300 单元", 32_000, SyncedLyrics(listOf(timed(List(300) { "光" }, 1_000, 100)), id = "stress"), listOf(1_000, 10_000, 20_000)),
    Scene("empty", "空歌词", 8_000, SyncedLyrics(emptyList(), id = "empty"), listOf(0)),
    Scene("invalid", "异常数据 · 诊断", 8_000, SyncedLyrics(listOf(UncheckedSyncedLine("逆序行", null, 5_000, 2_000)), id = "invalid"), listOf(0)),
)
