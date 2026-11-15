// Pinned Folia utils/lyrics/textDirection.ts RTL_LETTER_RE script list.
// SPDX-License-Identifier: AGPL-3.0-only. Source ledger: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.text

/** This is the upstream script predicate, not every Unicode strong RTL class. */
internal val foliaRtlScripts = setOf("Arabic", "Hebrew", "Syriac", "Thaana", "Nko",
    "Samaritan", "Mandaic", "Adlam", "Hanifi_Rohingya", "Yezidi")
