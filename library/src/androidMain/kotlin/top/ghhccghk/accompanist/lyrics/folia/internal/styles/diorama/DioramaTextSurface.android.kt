// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.graphics.drawscope.DrawScope

// No calibrated Android F16 text surface yet. The caller really draws ordinary
// planes, rather than claiming an8-bit saveLayer is the validated capability.
internal actual fun DrawScope.tryDioramaTextSurface(draw:()->Unit):Boolean=false
