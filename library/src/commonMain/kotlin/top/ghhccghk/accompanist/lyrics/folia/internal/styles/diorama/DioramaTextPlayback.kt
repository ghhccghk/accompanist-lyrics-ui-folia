// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Internal text-only playback integration. NOT a public/complete Diorama theme.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import top.ghhccghk.accompanist.lyrics.folia.internal.model.normalize
import kotlin.math.max

internal data class DioramaPlaybackLine(val core: DioramaCoreLine, val text: DioramaActiveText)

/** Core objects -> once-prepared full-shaped text -> media follower -> actual
 * projective three-layer drawing. Original weighted shot picker uses actual
 * source word timings and optional explicit metadata; overrides are calibration.
 * Neighbours, cross-song flight, depth/near clipping/fog remain separate work. */
internal class DioramaTextPlayback private constructor(val lines: List<DioramaPlaybackLine>,
    val camera: DioramaCameraTrack, val width: Double, val height: Double) {
    fun lyricTime(mediaSeconds: Double, offsetMillis: Int = 0): Double {
        require(mediaSeconds.isFinite())
        return (mediaSeconds+offsetMillis/1000.0).coerceIn(0.0,camera.endTime)
    }
    fun sourceAt(mediaSeconds: Double, offsetMillis: Int = 0): DioramaCoreLine =
        lines[camera.subjectIndex(lyricTime(mediaSeconds,offsetMillis))].core
    fun mediaStart(core: DioramaCoreLine, offsetMillis: Int = 0) = core.lyric.start-offsetMillis/1000.0
    fun draw(scope: DrawScope, mediaSeconds: Double, primary: DioramaLinearColor,
        accent: DioramaLinearColor, offsetMillis: Int = 0, reducedMotion: Boolean = false) {
        val time=lyricTime(mediaSeconds,offsetMillis)
        val index=camera.subjectIndex(time);val s=camera.subjects[index]
        lines[index].text.draw(scope,time,camera.perspectiveAt(time,reducedMotion),s.frame,s.placement,
            primary,accent,if(reducedMotion)DioramaUnitTuning(soul=0.0)else DioramaUnitTuning())
    }
    companion object {
        fun prepare(core: SyncedLyrics, style: TextStyle, measurer: TextMeasurer,
            resolver: FontFamily.Resolver, width: Double, height: Double, shots: List<DioramaShot>? = null,
            seedName: String, endTime: Double? = null, checkpoint: () -> Unit = {},
            metadata: List<DioramaShotMetadata> = emptyList(),
            mode: DioramaShotMode = DioramaShotMode.normal): DioramaTextPlayback {
            require(width.isFinite() && height.isFinite() && width>0 && height>0)
            val mapped=dioramaCoreLines(normalize(core))
            require(mapped.isNotEmpty() && (shots==null || shots.size==mapped.size))
            require(metadata.isEmpty() || metadata.size==mapped.size)
            val seed=dioramaHashSeed(seedName)
            val lyrics=mapped.map{it.lyric}
            val frames=buildDioramaPath(mapped.size,seed)
            val lines=mapped.map {
                checkpoint()
                DioramaPlaybackLine(it,DioramaActiveText.prepare(it.lyric,style,measurer,resolver))
            }
            val subjects=lines.mapIndexed { index,line ->
                val placement=dioramaTextPlacement(index,seed,mode.weave)
                val rawWidth=line.text.lineAdvance*.62/128
                val fit=dioramaFrameFit(rawWidth,5.2,55.0,width/height)*placement.scale
                val shot=shots?.get(index)?:dioramaPickShot(index,lyrics,seed,metadata,mode)
                DioramaCameraSubject(DioramaCameraLine(line.core.lyric),frames[index],placement,shot,rawWidth*fit,index)
            }
            val end=endTime?:max(0.0,subjects.maxOf { it.line.renderEnd }+8)
            val track=DioramaCameraTrack(subjects,width,height,end,seed,smoothTime=mode.smooth,
                moveScale=mode.move,driftScale=mode.drift,checkpoint=checkpoint)
            return DioramaTextPlayback(lines,track,width,height)
        }
    }
}
