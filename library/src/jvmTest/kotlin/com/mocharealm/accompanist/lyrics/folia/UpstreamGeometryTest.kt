package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.folia.internal.animation.Pose
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.*
import java.io.File
import kotlin.test.*

class UpstreamGeometryTest {
    @Test fun reachAvoidanceAndRtlMatchOriginalFunctions() {
        File("../verification/classic-fixtures/geometry.tsv").readLines().filterNot {it.startsWith("#") || it.isBlank()}.forEach { row ->
            val c = row.split('\t')
            fun d(i:Int) = c[i].toDouble()
            fun f(i:Int) = c[i].toFloat()
            val pose=Pose(f(3),f(4),f(5),f(6),f(7)); val next=Pose(f(8),f(9),f(10),f(11),f(12))
            val reach=wordReach(d(0),d(2),pose); val overflow=wordOverflow(d(0),d(2),pose)
            val values=listOf(reach.halfWidth,reach.halfHeight,overflow.left,overflow.right,overflow.top,overflow.bottom,
                pairGap(d(0),d(1),d(2),pose,next,c[13].toBoolean()))
            values.forEachIndexed { i, value -> assertEquals(d(14+i),value,.0001,"column ${14+i}: $row") }
        }
    }
}
