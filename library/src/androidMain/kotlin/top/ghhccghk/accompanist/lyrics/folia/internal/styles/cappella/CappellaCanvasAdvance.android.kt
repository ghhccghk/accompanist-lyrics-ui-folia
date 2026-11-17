package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

// Android still uses its real whole-paragraph advances. Do not pretend the JVM
// Skia metric adapter has been validated on Android Paint/shaping or a device.
internal actual fun cappellaCanvasAdvance(text: String, style: TextStyle,
    resolver: FontFamily.Resolver, density: Density): Double? = null
