// Copyright © 2010-2026 three.js authors
// SPDX-License-Identifier: MIT
// Adapted from Three.js 0.185.1 ShaderChunk/tonemapping_pars_fragment.glsl.js.
// Full notice: LICENSES/MIT-three-0.185.1.txt. Source/modifications: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

/** The original R3F screen uses ACES at exposure1. Older direct-Three oracle
 * hosts explicitly used NoToneMapping; keep that separate verification policy. */
internal enum class DioramaToneMapping { None, AcesFilmic }

/** Apply to LINEAR material RGB, before output sRGB, encoded fog and blending.
 * GLSL's input/output matrices are column-major. Alpha/coverage is never mapped.
 * Double precision is independent of display scheduling and allocates no raster. */
internal fun dioramaAcesFilmic(color: DioramaLinearColor): DioramaLinearColor {
    val r=color.red/.6;val g=color.green/.6;val b=color.blue/.6
    fun fit(v: Double)=(v*(v+.0245786)-.000090537)/(v*(.983729*v+.4329510)+.238081)
    val x=fit(.59719*r+.35458*g+.04823*b)
    val y=fit(.07600*r+.90834*g+.01566*b)
    val z=fit(.02840*r+.13383*g+.83777*b)
    return DioramaLinearColor(
        (1.60475*x-.53108*y-.07367*z).coerceIn(0.0,1.0),
        (-.10208*x+1.10813*y-.00605*z).coerceIn(0.0,1.0),
        (-.00327*x-.07276*y+1.07602*z).coerceIn(0.0,1.0))
}
