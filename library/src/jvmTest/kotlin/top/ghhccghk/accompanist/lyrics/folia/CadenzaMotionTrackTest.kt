package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cadenza.*
import java.io.File
import java.util.Base64
import kotlin.math.*
import kotlin.random.Random
import kotlin.test.*

class CadenzaMotionTrackTest {
    private fun CadenzaWordState.values()=listOf(x,y,rotation,scale,bodyAlpha,blur,activeMix,glowAlpha)
    private fun placements(line:CadenzaLyric)=line.words.mapIndexed { i,w ->
        CadenzaPlacement("word-$i",CadenzaFragment(i,w,w.text,"#f17b4d",0.0,70.0,0,w.text.length,w.text.length,emptyList()),
            i*83.0-120, i*29.0-70,70.0,46.0,i*4.5-8,1.0+i*.2,23.0,18.0,-27.0,-35.0,48.0,1.0,false)
    }
    private fun line(mode:CadenzaReveal=CadenzaReveal.NORMAL)=CadenzaLyric("Amber echoes beyond",.7,3.0,
        listOf(CadenzaWord("Amber ",.7,1.4),CadenzaWord("echoes ",1.4,2.1),CadenzaWord("beyond",2.1,2.8)),
        CadenzaHints(2.3,3.0,CadenzaTransition.NORMAL,mode))

    @Test fun exponentialMomentsMatchIndependentQuadrature() {
        // Composite Simpson integration of the defining integral (not the recurrence).
        for(x in listOf(0.0,1e-12,1e-7,.001,.1,.4999,.5,1.0,5.0,25.0,100.0))for(k in 0..3){
            val n=20000;var sum=0.0
            for(j in 0..n){val u=j.toDouble()/n;sum+=(if(j==0||j==n)1 else if(j%2==0)2 else 4)*exp(-x*(1-u))*u.pow(k)}
            assertEquals(x*sum/(3*n),cadenzaExponentialMoment(x,k),1e-10,"x=$x k=$k")
        }
    }

    @Test fun exactIntervalsAreIndependentOfTheirEndAndQueryOrder() {
        var maximum=0.0
        for(mode in CadenzaReveal.entries){
            val l=line(mode);val ps=placements(l);val track=CadenzaMotionTrack(l,ps,800.0,600.0,endTime=9.0)
            val times=(List(75){.7+Random(it+43).nextDouble()*8.3}+listOf(.7,.745,.88,1.4,2.1,2.8,3.0,9.0)).shuffled(Random(71))
            val saved=times.associateWith(track::sample)
            for(t in times){
                val short=CadenzaMotionTrack(l,ps,800.0,600.0,endTime=t).sample(t)
                short.zip(saved.getValue(t)).forEach{(a,b)->
                    assertEquals(a.placement,b.placement);assertEquals(a.status,b.status)
                    a.state.values().zip(b.state.values()).forEachIndexed{i,(v,q)->
                        maximum=max(maximum,abs(v-q));assertEquals(v,q,1e-8,"mode=$mode t=$t field=$i")
                    }
                }
            }
            for(t in times.reversed()+times.shuffled(Random(312)))assertEquals(saved.getValue(t),track.sample(t))
            assertTrue(track.preparedNodeCount<160,"event nodes, not media frames")
        }
        File("../build/verification/cadenza-motion-kotlin").apply{mkdirs()}.resolve("partition.txt").writeText("maximum_partition_delta=$maximum\n")
    }

    @Test fun constantChannelsFollowOriginalExponentialResponse() {
        val l=line();val ps=placements(l);val track=CadenzaMotionTrack(l,ps,800.0,600.0)
        val initial=track.sample(.7).single{it.placement.id=="word-0"}.state
        // First word is active at mount; original initializes rotation+16, alpha0, blur10.
        assertEquals(ps[0].rotation+16*exp(-11.0/60),initial.rotation,1e-12)
        assertEquals(1-exp(-14.0/60),initial.bodyAlpha,1e-12)
        assertEquals(10*exp(-14.0/60),initial.blur,1e-12)
        for(t in listOf(.701,.75,.9,1.1)){
            val s=track.sample(t).single{it.placement.id=="word-0"}.state
            assertEquals(1-(1-initial.bodyAlpha)*exp(-14*(t-.7)),s.bodyAlpha,1e-12)
            assertEquals(initial.blur*exp(-14*(t-.7)),s.blur,1e-12)
        }
    }

    @Test fun shortReversedAndInstantWordsRemainFiniteAndSeekable() {
        for(mode in CadenzaReveal.entries){
            val l=line(mode).copy(words=listOf(CadenzaWord("zero",.7,.7),CadenzaWord("reverse",1.2,1.1),CadenzaWord("short",1.6,1.601)))
            val track=CadenzaMotionTrack(l,placements(l),380.0,600.0,endTime=3600.0)
            for(t in listOf(3600.0,.7,1.1,1.2,1.6,1.601,1.602,3.0,.7)){
                assertTrue(track.sample(t).flatMap{it.state.values()}.all(Double::isFinite))
                assertEquals(track.sample(t),track.sample(t))
            }
            for(t in listOf(Double.NaN,Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY))assertFailsWith<IllegalArgumentException>{track.sample(t)}
        }
    }

    @Test fun compareActualOriginalPlaybackTrajectories() {
        fun rows(name:String)=File("../verification/cadenza-fixtures/$name.tsv").readLines().filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}
        fun decode(s:String)=String(Base64.getDecoder().decode(s),Charsets.UTF_8)
        val scenes=rows("sceneInputs").associate{f->f[0] to annotateCadenzaLine(CadenzaLyric(decode(f[1]),f[2].toDouble(),f[3].toDouble(),
            f[4].split('|').map{v->val w=v.split('~');CadenzaWord(decode(w[0]),w[1].toDouble(),w[2].toDouble())}))}
        val original=rows("motionStates").groupBy{it[0]};val prepared=rows("motionPlacements").groupBy{it[0]}
        val report=mutableListOf("case\tobservations\tmax_position_px\tmax_rotation_deg\tmax_scale\tmax_alpha\tmax_blur_px\tmax_mix\tmax_glow")
        for(input in rows("motionInputs")){
            val key=input[0];val l=scenes.getValue(input[1]);val width=input[2].toDouble()
            val ps=prepared.getValue(key).map{r->
                val n=r[7].split(',').map(String::toDouble);val w=l.words[r[2].toInt()]
                CadenzaPlacement(decode(r[1]),CadenzaFragment(r[2].toInt(),w,decode(r[3]),"#f17b4d",0.0,n[2],r[4].toInt(),r[5].toInt(),r[6].toInt(),emptyList()),
                    n[0],n[1],n[2],n[3],n[4],n[5],n[6],n[7],n[8],n[9],n[10],n[11],false)
            }
            val track=CadenzaMotionTrack(l,ps,width,600.0,endTime=3.15);val maximum=DoubleArray(8)
            val cache=original.getValue(key).groupBy{it[1].toDouble()}.mapValues{(t,_)->track.sample(t).associateBy{it.placement.id}}
            for(r in original.getValue(key)){
                val t=r[1].toDouble();val native=cache.getValue(t).getValue(decode(r[2])).state.values();val expected=r[3].split(',').map(String::toDouble)
                native.zip(expected).forEachIndexed{i,(v,e)->
                    if(t==l.start)assertEquals(e,v,1e-10,"original immediate mount $key/$i")
                    maximum[i]=max(maximum[i],abs(v-e));assertTrue(v.isFinite())
                }
            }
            report+=listOf(key,original.getValue(key).size,max(maximum[0],maximum[1]),*maximum.drop(2).toTypedArray()).joinToString("\t")
        }
        File("../build/verification/cadenza-motion-kotlin").apply{mkdirs()}.resolve("source-residual.tsv").writeText(report.joinToString("\n")+"\n")
        // The source samples the endpoint at RAF rate; the continuous port must
        // report that difference rather than fitting a rate-specific phase offset.
    }
}
