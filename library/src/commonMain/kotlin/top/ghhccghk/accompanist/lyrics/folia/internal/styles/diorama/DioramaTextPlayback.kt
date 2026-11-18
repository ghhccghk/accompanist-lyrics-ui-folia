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
import kotlin.math.sqrt

internal data class DioramaPlaybackLine(val core: DioramaCoreLine, val text: DioramaActiveText,
    val neighbor: DioramaPlainText)

/** Core objects -> once-prepared full-shaped text -> media follower -> actual
 * projective three-layer drawing. Original weighted shot picker uses actual
 * source word timings and optional explicit metadata; overrides are calibration.
 * Normal neighbour window and optional host-color fragment fog are integrated;
 * incoming text-flight anchors are optional. Outgoing corridor composition and
 * depth testing remain separate. Near/far clipping and optional fog are shared
 * by all text layers. */
internal class DioramaTextPlayback private constructor(val lines: List<DioramaPlaybackLine>,
    val camera: DioramaCameraTrack, val width: Double, val height: Double) {
    fun lyricTime(mediaSeconds: Double, offsetMillis: Int = 0): Double {
        require(mediaSeconds.isFinite())
        // Same host contract as FoliaLyricsView: positive offset delays lyrics.
        // Only the bounded internal camera input is clamped, never the source
        // timestamps or the inverse click target returned to the media host.
        return (mediaSeconds-offsetMillis/1000.0).coerceIn(0.0,camera.endTime)
    }
    fun sourceAt(mediaSeconds: Double, offsetMillis: Int = 0): DioramaCoreLine =
        lines[camera.subjectIndex(lyricTime(mediaSeconds,offsetMillis))].core
    fun mediaStart(core: DioramaCoreLine, offsetMillis: Int = 0) = core.lyric.start+offsetMillis/1000.0
    /** Capture before preparing a ready incoming song. No old scene/cache/job
     * is retained; full outgoing-corridor composition remains a separate task. */
    fun transitionAt(mediaSeconds: Double, epoch: Int, offsetMillis: Int = 0): DioramaTextTransition {
        val time=lyricTime(mediaSeconds,offsetMillis)
        return DioramaTextTransition(camera.at(time),camera.subjects[camera.subjectIndex(time)].frame.position,epoch)
    }
    fun draw(scope: DrawScope, mediaSeconds: Double, primary: DioramaLinearColor,
        accent: DioramaLinearColor, offsetMillis: Int = 0, reducedMotion: Boolean = false,
        secondary: DioramaLinearColor = primary, fog: DioramaFog? = null) {
        val time=lyricTime(mediaSeconds,offsetMillis)
        drawStage(scope,time,camera.perspectiveAt(time,reducedMotion),primary,accent,secondary,reducedMotion,fog)
    }
    /** Shared production stage; explicit view also isolates text/window parity
     * from the separately measured media follower residual in verification. */
    fun drawStage(scope: DrawScope, time: Double, view: DioramaPerspective,
        primary: DioramaLinearColor, accent: DioramaLinearColor, secondary: DioramaLinearColor = primary,
        reducedMotion: Boolean = false, fog: DioramaFog? = null) {
        val index=camera.subjectIndex(time);val s=camera.subjects[index]
        // Original neighbours are transparent, depthWrite=false and order0;
        // active layers have order17/18/19. With no opaque background geometry,
        // far-to-near centre sorting reproduces that transparent text ordering.
        dioramaMountedTextIndices(index,lines.size).filter { it!=index }.map { i ->
            val subject=camera.subjects[i];val text=lines[i].neighbor
            dioramaNeighborFrame(i,index,text.advancePx,text.fontPx,subject.frame,subject.placement,view,primary,secondary)
        }.sortedWith(compareByDescending<DioramaNeighborFrame>{it.depth}.thenBy{it.index}).forEach { neighbor ->
            if(neighbor.opacity>0) {
                val subject=camera.subjects[neighbor.index]
                lines[neighbor.index].neighbor.draw(scope,view,subject.frame,subject.placement,
                    neighbor.color.srgb(),neighbor.opacity.toFloat(),fog)
            }
        }
        val activeDelta=dioramaComposeLocal(s.frame,s.placement.offsetR,s.placement.offsetU,0.0)-view.eye
        lines[index].text.draw(scope,time,view,s.frame,s.placement,
            primary,accent,if(reducedMotion)DioramaUnitTuning(soul=0.0)else DioramaUnitTuning(),
            life=dioramaTextLife(sqrt(activeDelta.dot(activeDelta))),fog=fog)
    }
    companion object {
        fun prepare(core: SyncedLyrics, style: TextStyle, measurer: TextMeasurer,
            resolver: FontFamily.Resolver, width: Double, height: Double, shots: List<DioramaShot>? = null,
            seedName: String, endTime: Double? = null, checkpoint: () -> Unit = {},
            metadata: List<DioramaShotMetadata> = emptyList(),
            mode: DioramaShotMode = DioramaShotMode.normal,
            transition: DioramaTextTransition? = null): DioramaTextPlayback {
            require(width.isFinite() && height.isFinite() && width>0 && height>0)
            val mapped=dioramaCoreLines(normalize(core))
            require(mapped.isNotEmpty() && (shots==null || shots.size==mapped.size))
            require(metadata.isEmpty() || metadata.size==mapped.size)
            val seed=dioramaHashSeed(seedName)
            val lyrics=mapped.map{it.lyric}
            val origin=transition?.let { it.outgoingOrigin+dioramaTransitionOffset(seed,it.epoch) }
                ?:DioramaVec(0.0,0.0,0.0)
            val frames=buildDioramaPath(mapped.size,seed).map { it.copy(position=it.position+origin) }
            // Repeated chorus text shares its immutable plain raster within
            // this preparation/font configuration, never a global song cache.
            val neighborTexts=mutableMapOf<String,DioramaPlainText>()
            val lines=mapped.map {
                checkpoint()
                val active=DioramaActiveText.prepare(it.lyric,style,measurer,resolver,checkpoint)
                checkpoint()
                val neighbor=neighborTexts.getOrPut(it.lyric.text) {
                    DioramaPlainText.prepare(it.lyric.text,style,measurer,resolver)
                }
                DioramaPlaybackLine(it,active,neighbor)
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
                moveScale=mode.move,driftScale=mode.drift,checkpoint=checkpoint,
                initialState=transition?.previous?:DioramaFollowState(),flightEpoch=transition?.epoch?:0)
            return DioramaTextPlayback(lines,track,width,height)
        }
    }
}
