package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.folia.internal.animation.*
import java.io.File
import kotlin.test.*

class UpstreamMotionTest {
    private fun rows(file:String) = File("../verification/classic-fixtures/$file.tsv").readLines().filterNot {it.startsWith("#")||it.isBlank()}.map {it.split('\t')}
    @Test fun bodyAndPoseMatchOriginalVariantGenerators() {
        rows("motion").forEach {c ->
            val mode=Reveal.entries.first {it.name.equals(c[0],true)}
            val profile=RenderProfile(mode,c[4].toLong(),when(mode){Reveal.Normal->150_000;Reveal.Fast->80_000;Reveal.Instant->30_000})
            val frame=sampleWord(c[5].toLong(),c[3].toLong(),c[1].toLong(),c[2].toLong(),profile,Pose(12.3f,-4.2f,1.24f,2.1f,-7.3f),false)
            listOf(frame.x,frame.y,frame.scale,frame.rotation,frame.alpha,frame.mix,frame.blur).forEachIndexed {i,v->
                assertEquals(c[i+6].toDouble(),v.toDouble(),.0001,"column ${i+6}: $c")
            }
        }
    }
    @Test fun lineMotionMatchesOriginalVariantsAndDefaultTransitions() {
        rows("line-motion").forEach {c ->
            val mode=Reveal.entries.first {it.name.equals(c[0],true)}
            val elapsed=c[1].toLong(); val exit=c[2].toLong()
            val frame=sampleLine(elapsed+exit,0,elapsed,RenderProfile(mode,2_000_000,0),false,0f,1f)
            listOf(frame.alpha,frame.scale,frame.blur).forEachIndexed {i,v->
                assertEquals(c[i+3].toDouble(),v.toDouble(),.0001,"column ${i+3}: $c")
            }
        }
    }
}
