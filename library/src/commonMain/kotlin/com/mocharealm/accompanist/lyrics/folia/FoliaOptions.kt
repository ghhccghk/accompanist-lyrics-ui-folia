package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class FoliaOptions(
    val showTranslation: Boolean = true,
    val showPhonetic: Boolean = true,
    val showNextLine: Boolean = true,
    val offsetMillis: Int = 0,
    val reducedMotion: Boolean = false,
    val quality: FoliaQuality = FoliaQuality.Standard,
)

enum class FoliaQuality { Standard, Reduced, Minimal }

@Immutable
data class FoliaStatus(
    val requestedQuality: FoliaQuality,
    val actualQuality: FoliaQuality,
    val mainUnitCount: Int,
    val diagnostics: List<String>,
)

sealed interface FoliaStyle {
    @Immutable
    data class Classic(val tuning: ClassicTuning = ClassicTuning()) : FoliaStyle
}

@Immutable
data class ClassicTuning(
    val enableWordRotation: Boolean = true,
    val breathingFloatMultiplier: Float = 1f,
    val wordSpacing: Float = .7f,
    val avoidOverlap: Boolean = false,
) {
    init {
        require(breathingFloatMultiplier.isFinite() && breathingFloatMultiplier in 0f..2f)
        require(wordSpacing.isFinite() && wordSpacing in 0f..2f)
    }
}

@Immutable
data class FoliaTypography(
    /** null selects the responsive clamp(36sp, 6% of width in dp, 72sp). */
    val main: TextStyle? = null,
    val secondary: TextStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    val subtitle: TextStyle = TextStyle(fontSize = 18.sp),
    val scale: Float = 1f,
) {
    init { require(scale.isFinite() && scale in .5f..2f) }
}

@Immutable
data class FoliaColors(
    val text: Color = Color(0xFFDFE5EC),
    val active: Color = Color(0xFFFFFFFF),
    val secondary: Color = Color(0xFFADB9C8),
)
