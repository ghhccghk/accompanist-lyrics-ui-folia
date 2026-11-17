// SPDX-License-Identifier: AGPL-3.0-only
// Text stage adapted from pinned Folia useFumeCanvasStage.ts, without background/audio engine.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density

/** One prepared article, complete shaped rows and internal EGC clocks. Owns no
 * timers/jobs; all motion is provided as an immutable media-camera frame. Internal
 * until integrated text/camera scenes have completed visual calibration. */
internal class FumeArticleDrawing private constructor(private val article: FumeArticle,
    private val lyrics: List<FumeLyric>, private val drawings: Map<Int, FumeLiveText>,
    private val rasterDensity: Density) {
    val focusGeometry: Map<Int, FumeFocusBlock> = drawings.mapValues { it.value.focus }

    fun draw(scope: DrawScope, viewport: FumeViewport, time: Double, camera: FumeCameraFrame,
        theme: FumeFrameTheme, hold: Double = 1.0, showStamp: Boolean = true) = with(scope) {
        val pose = camera.state.pose
        val pixels = rasterDensity.density
        val passedDuration = fumePassedDuration(lyrics, hold)
        withTransform({
            translate((viewport.width * pixels / 2).toFloat(), (viewport.height * pixels / 2).toFloat())
            scale(pose.scale.toFloat(), pose.scale.toFloat(), Offset.Zero)
            translate((-pose.x * pixels).toFloat(), (-pose.y * pixels).toFloat())
        }) {
            for (block in article.blocks) {
                val drawing = drawings.getValue(block.sourceIndex)
                if (fumeBlockOnScreen(block, pose.view(), viewport)) drawing.draw(this, time,
                    lyrics.getOrNull(block.sourceIndex + 1)?.start, theme, hold, passedDuration, camera.restore, showStamp,
                    liveShadowScale = pose.scale)
                else drawing.releaseStaticSnapshots()
            }
        }
    }

    companion object {
        fun prepare(article: FumeArticle, lyrics: List<FumeLyric>, measure: FumeTextMeasure,
            resolver: FontFamily.Resolver, density: Density) = FumeArticleDrawing(article, lyrics,
            article.blocks.associate { it.sourceIndex to FumeLiveText.prepare(it, measure, resolver, density) }, density)
    }
}
