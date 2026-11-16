package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt.*
import top.ghhccghk.accompanist.lyrics.folia.internal.animation.renderProfile
import top.ghhccghk.accompanist.lyrics.folia.internal.text.graphemeBoundaries
import java.io.File
import java.util.Base64
import kotlin.math.*
import kotlin.test.*

class TiltMotionTest {
    @Test fun parent450msExitMatchesMountedOriginalIncludingLateTail() {
        val fixtures=rows("mounted-exits")
        assertEquals(11,fixtures.size)
        fixtures.forEach {f->
            assertEquals(f[2].toDouble(),sampleTiltLineOpacity(3_000_000+f[0].toLong()*1000,
                700_000,3_000_000,false).toDouble(),.0001,"Actual AnimatePresence parent at ${f[0]}ms")
        }
    }
    private fun rows(name:String)=File("../verification/tilt-fixtures/$name.tsv").readLines()
        .filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}
    @Test fun mountedOriginalRowAndCharacterTargets() {
        val original=rows("mounted-rows")
        val chars=rows("mounted-chars").groupBy{it.take(5)}
        val scenes=File("../verification/classic-fixtures/scenes.tsv").readLines()
            .filterNot{it.startsWith("#")||it.isBlank()}.map{it.split('\t')}.groupBy{it[0]}
        original.forEach { f ->
            val input=scenes.getValue(f[1]);val text=input.joinToString(""){it[5]}
            val words=input.map{TiltWord(it[5],it[3].toDouble()/1000,it[4].toDouble()/1000)}
            val start=input.first()[1].toLong()*1000
            val leave=renderProfile(start,input.first()[2].toLong()*1000,words.last().end.times(1e6).roundToLong()).renderEndUs
            val siblings=original.filter{it.take(4)==f.take(4)}
            var offset=0
            val segments=siblings.map { s ->
                val part=String(Base64.getDecoder().decode(s[5]),Charsets.UTF_8)
                val at=text.indexOf(part,offset);offset=at+part.length
                TiltSegment(part,s[6].toBoolean(),false,at)
            }
            val index=f[4].toInt();val segment=segments[index]
            val timings=segments.map { s ->
                val selected=tiltWordRange(s.charOffset,s.text,text,words).map{words[it]}
                (selected.firstOrNull()?.start?:start/1e6) to (selected.lastOrNull()?.end?:leave/1e6)
            }
            val visible=max(start,((timings.drop(index).minOf{it.first}-.25)*1e6).roundToLong())
            val time=f[3].toLong()*1000
            val frame=sampleTiltRow(time,visible,leave,segment.isTilt,false)
            listOf(frame.alpha,frame.y,frame.scale).forEachIndexed { i,value ->
                assertEquals(f[12+i].toDouble(),value.toDouble(),.0001,"row $i $f")
            }
            // Used CSS font operands are rounded independently. Feed the
            // original unrounded stagger amplitude to isolate Motion parity;
            // actual layout/font/stagger calibration belongs to TiltParityTest.
            val initialKey=f.take(3)+listOf("700",f[4])
            val yOffset=if(segment.isTilt)abs(chars.getValue(initialKey).first()[7].toDouble())/2 else 0.0
            val charTimes=buildTiltCharTimings(segment.charOffset,segment.text,timings[index].first,timings[index].second,text,words)
            var visual=0
            val bounds=graphemeBoundaries(segment.text).sorted()
            val expected=chars.getValue(f.take(5))
            bounds.zipWithNext().forEachIndexed { ci,(a,b) ->
                val space=segment.text.substring(a,b).all{it.isWhitespace()}
                val c=sampleTiltChar(time,visible,segment.isTilt,visual,space,charTimes.getOrNull(visual),yOffset,false)
                listOf(c.alpha,c.y,c.scale).forEachIndexed { i,value ->
                    assertEquals(expected[ci][6+i].toDouble(),value.toDouble(),.001,"char $ci/$i $f")
                }
                if(!space)visual++
            }
        }
    }
}
