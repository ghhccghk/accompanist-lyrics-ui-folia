package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import kotlin.test.*
import kotlin.math.*
import kotlin.random.Random
import java.io.File

class GlowMaskTest {
    @Test fun pairedTraversalProbe() {
        val report=mutableListOf("JVM ${System.getProperty("java.version")}; ${System.getProperty("os.name")} ${System.getProperty("os.arch")}",
            "CPU coverage+ARGB packing only, not shaping/bitmap/GPU/frame time. Uncontrolled host load.",
            "12 warmups each, 40 alternating original/contiguous samples per size; one thread.")
        var checksum=0L
        for((width,height) in listOf(320 to 160,640 to 320)) {
            val source=DoubleArray(width*height){if(it%width in width/4..width*3/4 && it/width in height/3..height*2/3)255.0 else 0.0}
            val a=DoubleArray(source.size);val b=DoubleArray(source.size);val output=IntArray(source.size*2)
            fun run(contiguous:Boolean) {
                for((r,sigma) in floatArrayOf(10f,20f).withIndex()) {
                    source.copyInto(a)
                    val result=if(contiguous)blurBoxCoverage(a,b,width,height,sigma) else previousCoverage(a,width,height,sigma,b)
                    for(i in source.indices)output[r*source.size+i]=(result[i].toInt() shl 24) or 0xffffff
                }
                checksum+=output[source.size/2]
            }
            repeat(12){run(false);run(true)}
            val old=mutableListOf<Double>();val candidate=mutableListOf<Double>()
            repeat(40){round->
                for(flag in if(round%2==0)listOf(false,true) else listOf(true,false)) {
                    val begin=System.nanoTime();run(flag)
                    (if(flag)candidate else old)+=(System.nanoTime()-begin)/1e6
                }
            }
            for((name,samples) in listOf("original" to old,"contiguous" to candidate)) {
                val t=samples.sorted()
                report+="$width x $height $name ms P50=${t[19]} P95=${t[37]} P99=${t[39]}"
            }
        }
        report+="Checksum: $checksum"
        File("build/verification").mkdirs()
        File("build/verification/coverage-traversal-benchmark.txt").writeText(report.joinToString("\n")+"\n")
    }
    @Test fun contiguousVerticalPassMatchesPriorRecurrenceIncludingFractionalColor() {
        val random=Random(20261116)
        for((width,height) in listOf(1 to 1,1 to 19,31 to 1,3 to 7,37 to 13,128 to 96)) {
            for(sigma in listOf(1f,2.25f,3f,7.5f,20f,4096f)) for(fractional in listOf(false,true)) {
                val source=DoubleArray(width*height){if(fractional)random.nextDouble()*255 else random.nextInt(256).toDouble()}
                val actual=blurBoxCoverage(source.copyOf(),DoubleArray(source.size),width,height,sigma)
                val expected=previousCoverage(source.copyOf(),width,height,sigma)
                assertContentEquals(expected,actual,"$width/$height sigma=$sigma fractional=$fractional")
            }
        }
    }

    // Snapshot of the pre-optimization column traversal, independent of the
    // production pass. Preserve intermediate doubles and axis quantization.
    private fun previousCoverage(source:DoubleArray,width:Int,height:Int,sigma:Float,
        scratch:DoubleArray=DoubleArray(source.size)):DoubleArray {
        val window=max(1,floor(sigma*3*sqrt(2*PI)/4+.5).toInt())
        val widths=intArrayOf(window,window,window+if(window%2==0)1 else 0)
        var input=source;var output=scratch
        for(horizontal in listOf(true,false)) {
            widths.forEachIndexed { pass,count ->
                val length=if(horizontal)width else height
                val lines=if(horizontal)height else width
                val stride=if(horizontal)1 else width
                val left=count/2+if(count%2==0&&pass==1)-1 else 0
                val right=count-left-1
                repeat(lines){line->
                    val base=if(horizontal)line*width else line
                    var total=0.0
                    for(p in 0..min(right,length-1))total+=input[base+p*stride]
                    repeat(length){p->
                        output[base+p*stride]=total
                        if(p-left>=0)total-=input[base+(p-left)*stride]
                        if(p+right+1<length)total+=input[base+(p+right+1)*stride]
                    }
                }
                val swap=input;input=output;output=swap
            }
            val divisor=widths.fold(1.0){p,v->p*v}
            for(i in input.indices)input[i]=floor(input[i]/divisor+.5).coerceIn(0.0,255.0)
        }
        return input
    }
    @Test fun transparentOutsideAndIndependentResults() {
        val width=128; val height=96
        val source=bitmapFromArgb(width,height,IntArray(width*height) { i ->
            if(i%width in 40..87 && i/width in 24..71) -1 else 0
        })
        val masks=blurredMasks(source,floatArrayOf(3f,7.5f,3f))
        fun alpha(index:Int)=IntArray(width*height).also{masks[index].readPixels(it)}.map{it ushr 24}
        val a=alpha(0);val b=alpha(1)
        assertEquals(a,alpha(2))
        assertNotSame(masks[0],masks[2])
        assertEquals(255,a[48*width+64])
        assertEquals(0,a[0]);assertEquals(0,b[0])
        assertTrue(b[48*width+32]>a[48*width+32],"Wider sigma spreads coverage")
        for(y in 0 until height) for(x in 0 until width) {
            assertEquals(a[y*width+x],a[y*width+width-1-x],"centred horizontal support")
            assertEquals(b[y*width+x],b[(height-1-y)*width+x],"centred vertical support")
        }
        assertTrue(blurredMasks(source,intArrayOf()).isEmpty())
        assertFailsWith<IllegalArgumentException>{blurredMasks(source,intArrayOf(0))}
        assertFailsWith<IllegalArgumentException>{blurredMasks(source,floatArrayOf(Float.NaN))}
    }
}
