package com.mocharealm.accompanist.lyrics.folia.internal.rendering

import androidx.compose.ui.geometry.Offset
import com.mocharealm.accompanist.lyrics.folia.internal.animation.WordFrame
import kotlin.math.*

/** Inverse of the Canvas line then unit transforms; local rectangle is centered on the glyph run. */
internal fun hitClassicUnit(point: Offset, canvasCenter: Offset, lineScale: Float, breathingY: Float,
    unitCenter: Offset, frame: WordFrame, density: Float, width: Float, height: Float): Boolean {
    if (lineScale <= 0f || frame.scale <= 0f || frame.alpha <= .001f) return false
    val inLine = (point - Offset(0f, breathingY) - canvasCenter) / lineScale + canvasCenter
    val relative = inLine - unitCenter - Offset(frame.x * density, frame.y * density)
    val radians = -frame.rotation * PI / 180
    val x = (relative.x * cos(radians) - relative.y * sin(radians)) / frame.scale
    val y = (relative.x * sin(radians) + relative.y * cos(radians)) / frame.scale
    return x >= -width / 2 && x <= width / 2 && y >= -height / 2 && y <= height / 2
}
