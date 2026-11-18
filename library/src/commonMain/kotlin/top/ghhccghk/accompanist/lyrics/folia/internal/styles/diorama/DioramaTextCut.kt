// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Folia cc6226e4 VisualizerDiorama.tsx:350–455 / dioramaSceneLayout.ts:26–78.
// Bounded two-corridor text adaptation; source and limitations: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

/** A shared immutable PLAIN raster and value-only placement. No original core,
 * active glow/unit rasters, camera track, job or previous playback reference. */
internal data class DioramaDepartingPlane(val index: Int, val text: DioramaPlainText,
    val frame: DioramaFrame, val placement: DioramaPlacement)

internal fun dioramaDepartureTextIndices(outgoing: Int, total: Int): List<Int> {
    require(total>0 && outgoing in 0 until total)
    return ((maxOf(0,outgoing-2)..minOf(total-1,outgoing+1))+
        (maxOf(0,total-2) until total)).distinct().sorted()
}

/** Only one preceding corridor: outgoing -2/+1 plus its last two lines (the
 * new corridor's live -2 tail). <=6 rasters, independent of old song length.
 * Retained for reverse seek rather than discarded by a wall-clock timeout. */
internal class DioramaTextDeparture(val sourceSpan: Int, val outgoingIndex: Int,
    planes: List<DioramaDepartingPlane>) {
    val planes = planes.toList()
    init {
        require(sourceSpan > 0 && outgoingIndex in 0 until sourceSpan)
        require(this.planes.size <= 6 && this.planes.map { it.index }.distinct().size == this.planes.size)
        require(this.planes.map { it.index }.sorted() == dioramaDepartureTextIndices(outgoingIndex,sourceSpan))
    }
}

/** Separate from the existing value-only transition anchor. Capturing a cut
 * never recursively retains the previous playback or a previous departure. */
internal data class DioramaTextCut(val anchor: DioramaTextTransition, val departure: DioramaTextDeparture)
