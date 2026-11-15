package com.mocharealm.accompanist.lyrics.folia

import com.mocharealm.accompanist.lyrics.folia.internal.animation.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.text.displayUnits
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import java.io.File
import kotlin.math.*
import kotlin.test.*

/** Original browser's actual layout/body/glow state, not a second implementation of variants. */
class BrowserClassicMotionTest {
    @Test fun mountedOriginalClassicMatchesMediaSamples() = compare("browser-motion.tsv")
    @Test fun denseOriginalSequenceMatchesMediaSamples() = compare("browser-sequence-motion.tsv")
    private fun compare(file:String) {
        fun line(a:String,b:String,start:Int,middle:Int,end:Int)=KaraokeLine.MainKaraokeLine(
            listOf(KaraokeSyllable(a,start,middle),KaraokeSyllable(b,middle,end)),null,KaraokeAlignment.Unspecified,start,end)
        val documents=mutableMapOf("baseline" to normalize(SyncedLyrics(listOf(line("Amber ","echo",700,1300,1900),line("Silver ","light",3200,3650,4100)))))
        File("../verification/classic-fixtures/scenes.tsv").readLines().filterNot{it.startsWith("#")||it.isBlank()}
            .map{it.split('\t')}.groupBy{it[0]}.forEach{(name,rows)->
                val source=KaraokeLine.MainKaraokeLine(rows.map{KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())},null,
                    KaraokeAlignment.Unspecified,rows[0][1].toInt(),rows[0][2].toInt())
                documents[name]=normalize(SyncedLyrics(listOf(source)))
            }
        File("../verification/classic-fixtures/$file").readLines().filterNot{it.startsWith("#")||it.isBlank()}.forEach {row ->
            val c=row.split('\t');val time=c[1].toLong()*1000
            val document=documents.getValue(c[19])
            val line=document.lines.first{it.startUs==c[2].toDouble().roundToLong()*1000}
            val units=displayUnits(line);val index=c[3].toInt();val word=units[index]
            val profile=renderProfile(line.startUs,line.endUs,line.fragments.last().endUs)
            val frame=sampleWord(time,line.startUs,word.startUs,word.endUs,profile,wordPose(line.startUs,index,true),false)
            val angle=frame.rotation*PI/180
            val matrix=listOf(cos(angle)*frame.scale,sin(angle)*frame.scale,-sin(angle)*frame.scale,cos(angle)*frame.scale,
                frame.x.toDouble(),frame.y.toDouble(),frame.alpha.toDouble(),frame.blur.toDouble())
            matrix.forEachIndexed {j,v->assertEquals(c[j+4].toDouble(),v,.0001,"matrix/body column ${j+4}: $row")}
            val base=listOf(223.0,229.0,236.0)
            base.forEachIndexed {j,b ->
                val channel=sqrt(b*b+(255*255-b*b)*frame.mix)
                assertEquals(c[j+12].toDouble(),channel,.51,"color column ${j+12}: $row")
            }
            val leave=profile.renderEndUs.takeIf{time>=it}
            val body=sampleLine(time,line.startUs,leave,profile,false,0f,1f)
            listOf(body.scale,body.alpha,body.blur).forEachIndexed{j,v->assertEquals(c[j+15].toDouble(),v.toDouble(),.0001,"line column ${j+15}: $row")}
            c[18].split(',').map{it.toDouble()}.forEachIndexed {i,expected ->
                val g=word.graphemes[i]
                val actual=sampleGraphemeGlow(time,line.startUs,word.startUs,word.endUs,g.startUs,g.endUs,word.graphemes.size,profile,g.startSeconds,g.endSeconds)
                assertEquals(expected,actual.toDouble(),.0001,"grapheme $i: $row")
            }
        }
    }
}
