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
)

@Immutable
data class FoliaTypography(
    /** null selects the responsive clamp(36sp, 6% of width in dp, 72sp). */
    val main: TextStyle? = null,
    val secondary: TextStyle = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Medium),
    val subtitle: TextStyle = TextStyle(fontSize = 18.sp),
)

@Immutable
data class FoliaColors(
    val text: Color = Color(0xFFDFE5EC),
    val active: Color = Color(0xFFFFFFFF),
    val secondary: Color = Color(0xFFADB9C8),
)
