// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

// The calibrated Skia fragment backend has no validated Android equivalent
// yet. Explicitly report unavailable; ordinary Kotlin/Compose planes remain.
// Never emulate this as an enabled Android backend or add a native dependency.
internal actual fun createDioramaPlainSampler(text:DioramaPlainText):DioramaPlainSampler?=null
