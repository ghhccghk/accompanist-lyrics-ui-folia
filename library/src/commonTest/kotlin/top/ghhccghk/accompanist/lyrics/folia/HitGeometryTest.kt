package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.geometry.Offset
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.WordFrame
import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.hitClassicUnit
import kotlin.math.*
import kotlin.random.Random
import kotlin.test.*

class HitGeometryTest {
    @Test fun inverseMatchesForwardTransformAcrossRandomPoses() {
        val random = Random(729)
        repeat(2_000) {
            val center = Offset(400f, 250f)
            val unit = Offset(100f + random.nextFloat() * 500, 100f + random.nextFloat() * 250)
            val frame = WordFrame(random.nextFloat() * 40, random.nextFloat() * 30,
                .3f + random.nextFloat() * 2, random.nextFloat() * 90 - 45, 1f, 1f, 1f)
            val lineScale = .7f + random.nextFloat()
            val breathing = random.nextFloat() * 30 - 15
            val density = 1f + random.nextFloat() * 2
            fun forward(local: Offset): Offset {
                val theta = frame.rotation * PI / 180
                val scaled = local * frame.scale
                val rotated = Offset((scaled.x * cos(theta) - scaled.y * sin(theta)).toFloat(),
                    (scaled.x * sin(theta) + scaled.y * cos(theta)).toFloat())
                return center + (unit + Offset(frame.x * density, frame.y * density) + rotated - center) * lineScale + Offset(0f, breathing)
            }
            assertTrue(hitClassicUnit(forward(Offset(30f, 10f)), center, lineScale, breathing, unit, frame, density, 80f, 40f))
            assertFalse(hitClassicUnit(forward(Offset(45f, 10f)), center, lineScale, breathing, unit, frame, density, 80f, 40f))
            assertFalse(hitClassicUnit(forward(Offset.Zero), center, lineScale, breathing, unit, frame.copy(alpha = 0f), density, 80f, 40f))
        }
    }
}
