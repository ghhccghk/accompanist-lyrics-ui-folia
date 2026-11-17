// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 utils/lyrics/renderHints.ts: buildLineRenderHints/render end.
// SPDX-License-Identifier: AGPL-3.0-only. Preserve original seconds arithmetic.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import kotlin.math.*

internal fun pendoloRenderEnd(start:Double,end:Double,lastWordEnd:Double=end):Double {
    val duration=max(end-start,0.0)
    if(duration<.10)return max(end,start+.067)
    val fast=duration<.18
    val enter=if(fast)(duration*.45).coerceIn(.045,.06)else min(.42,max(.22,max(duration,.12)*.34))
    val exit=if(fast)(duration*.22).coerceIn(.03,.04)else min(.32,max(.18,max(duration,.12)*.18))
    val passed=max(lastWordEnd,start)+if(fast).03 else .06
    val exitStart=if(fast)maxOf(start+enter+.01,passed,end-exit)else max(passed,end-exit)
    return max(end,exitStart+exit)
}
