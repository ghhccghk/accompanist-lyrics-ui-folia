// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// VisualizerFume.tsx:340-346, pinned cc6226e4: first-print .985 -> 1,
// duration .45s, Motion easeOut. Media-time reconstruction: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

internal fun fumeStageEntryScale(time:Double,firstStart:Double,reducedMotion:Boolean=false):Double{
    if(reducedMotion)return 1.0
    val progress=((time-firstStart)/.45).coerceIn(0.0,1.0)
    if(progress<=0)return .985
    if(progress>=1)return 1.0
    return .985+.015*fumeEntryEaseOut(progress)
}
