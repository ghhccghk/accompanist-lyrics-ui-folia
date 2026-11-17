package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.FontLoadResult

/** Read standard OpenType OS/2 sTypoAscender/Descender through public Skia API.
 * No font-file assumptions, fixed font names, reflection or internal Compose API. */
internal actual fun dioramaTypographicRatio(style: TextStyle, resolver: FontFamily.Resolver): Double? {
    val loaded = resolver.resolve(style.fontFamily, style.fontWeight ?: FontWeight.Normal,
        style.fontStyle ?: FontStyle.Normal, style.fontSynthesis ?: FontSynthesis.All).value as FontLoadResult
    return loaded.typeface?.getTableData("OS/2")?.use { data ->
        val bytes = data.bytes
        if (bytes.size < 72) return@use null
        fun signed16(at: Int): Int = (((bytes[at].toInt() and 255) shl 8) or
            (bytes[at + 1].toInt() and 255)).toShort().toInt()
        val ascent = signed16(68); val descent = signed16(70)
        if (ascent <= 0 || descent > 0 || ascent <= descent) null else ascent.toDouble() / (ascent - descent)
    }
}
