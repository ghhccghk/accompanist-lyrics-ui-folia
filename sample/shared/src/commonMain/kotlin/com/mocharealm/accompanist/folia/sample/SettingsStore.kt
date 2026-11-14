package com.mocharealm.accompanist.folia.sample

import androidx.compose.runtime.Composable
import com.mocharealm.accompanist.lyrics.folia.*

interface SettingsStore {
    suspend fun read(): String?
    suspend fun write(value: String)
}

@Composable
expect fun rememberSettingsStore(): SettingsStore

data class SampleSettings(val options: FoliaOptions = FoliaOptions(),
    val tuning: ClassicTuning = ClassicTuning(), val fontScale: Float = 1f) {
    fun encode(): String = listOf("version=1", "translation=${options.showTranslation}",
        "phonetic=${options.showPhonetic}", "next=${options.showNextLine}", "motion=${options.reducedMotion}",
        "quality=${options.quality}", "offset=${options.offsetMillis}", "rotation=${tuning.enableWordRotation}",
        "spacing=${tuning.wordSpacing}", "breathing=${tuning.breathingFloatMultiplier}",
        "overlap=${tuning.avoidOverlap}", "font=$fontScale").joinToString("\n")

    companion object {
        fun decode(value: String): SampleSettings {
            val fields = value.lineSequence().mapNotNull { line ->
                val separator = line.indexOf('=')
                if (separator <= 0) null else line.substring(0, separator) to line.substring(separator + 1)
            }.toMap()
            require(fields["version"] == "1") { "未知设置版本，请重新保存显示设置" }
            fun boolean(key: String, default: Boolean) = fields[key]?.toBooleanStrictOrNull() ?: default
            fun number(key: String, default: Float, range: ClosedFloatingPointRange<Float>) =
                fields[key]?.toFloatOrNull()?.takeIf { it.isFinite() && it in range } ?: default
            return SampleSettings(FoliaOptions(boolean("translation", true), boolean("phonetic", true),
                boolean("next", true), fields["offset"]?.toIntOrNull()?.coerceIn(-2_000, 2_000) ?: 0,
                boolean("motion", false), FoliaQuality.entries.find { it.name == fields["quality"] } ?: FoliaQuality.Standard),
                ClassicTuning(boolean("rotation", true), number("breathing", 1f, 0f..2f),
                    number("spacing", .7f, 0f..2f), boolean("overlap", false)), number("font", 1f, .5f..2f))
        }
    }
}

fun SampleSession.settings() = SampleSettings(options, tuning, fontScale)
fun SampleSession.applySettings(value: SampleSettings) {
    options = value.options; tuning = value.tuning; fontScale = value.fontScale
}
