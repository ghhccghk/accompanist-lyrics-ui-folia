// SPDX-License-Identifier: AGPL-3.0-only
@file:OptIn(top.ghhccghk.accompanist.lyrics.folia.ExperimentalFoliaRendererBridge::class)
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import top.ghhccghk.accompanist.lyrics.folia.*
import kotlin.math.roundToLong

/** Upload only the published raster window. Frame sampling uses the same
 * camera and text planes as the ordinary Android draw path. */
internal class DioramaNativeScene(private val playback: DioramaTextPlayback, time: Double) {
    private val window = playback.rastersAt(time).toSet()
    private val images = window.map { it.image }.distinct()
    private val textureIndices = images.withIndex().associate { it.value to it.index }
    val scene = FoliaRendererScene(playback.sourceAt(time).source, images.map { FoliaRendererTexture(it) })

    fun frame(time: Double, width: Int, height: Int, density: Float, reduced: Boolean, quality: FoliaQuality,
        primary: DioramaLinearColor, accent: DioramaLinearColor, secondary: DioramaLinearColor, fog: DioramaFog?): FoliaRendererFrame? {
        // The clock may cross a subject boundary after composition but before draw.
        val required = playback.rastersAt(time)
        if (required.any { it !in window || !it.isResident }) return null
        val view = playback.camera.perspectiveAt(time, reduced)
        val commands = buildList {
            playback.visitPlanes(time, view, primary, accent, secondary, fog, reduced, quality) { plane ->
                val texture = textureIndices[plane.image] ?: error("Diorama raster missing from prepared window")
                plane.command(view, texture, density)?.let(::add)
            }
        }
        return FoliaRendererFrame((time * 1_000_000).roundToLong(), width, height,
            FoliaRendererGroup(width, height, commands, allowDirectDraw = true))
    }
}

private fun DioramaTextPlane.command(view: DioramaPerspective, texture: Int, density: Float): FoliaRendererPlane? {
    if (alpha <= 0f) return null
    val clip = dioramaClipPlane(view, origin, x, y, image.width.toDouble(), image.height.toDouble())
    if (clip.points.size < 3) return null
    val h = view.planeHomography(origin, x, y)
    val vertices = clip.points.map { p ->
        FoliaRendererPlaneVertex(
            ((h[0] * p.x + h[1] * p.y + h[2]) * density).toFloat(),
            ((h[3] * p.x + h[4] * p.y + h[5]) * density).toFloat(),
            (h[6] * p.x + h[7] * p.y + h[8]).toFloat(),
            (p.x / image.width).toFloat().coerceIn(0f, 1f),
            (p.y / image.height).toFloat().coerceIn(0f, 1f),
        )
    }
    return FoliaRendererPlane(texture, vertices, color, alpha.coerceIn(0f, 1f), additive,
        fog?.let { FoliaRendererPlaneFog(it.encodedColor, it.near.toFloat(), it.far.toFloat()) },
        mipMaterial?.let { Triple(it.red.toFloat(), it.green.toFloat(), it.blue.toFloat()) })
}
