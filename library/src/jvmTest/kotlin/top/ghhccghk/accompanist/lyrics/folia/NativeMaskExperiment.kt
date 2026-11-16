package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.rendering.*
import java.io.File
import java.lang.management.ManagementFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.random.Random
import kotlin.math.*

/** Explicit development probe, never loaded by FoliaLyricsView or ordinary tests. */
internal object NativeMaskExperiment {
    @JvmStatic external fun blur(source:ByteBuffer,a:ByteBuffer,b:ByteBuffer,sigmas:ByteBuffer,
        output:ByteBuffer,width:Int,height:Int,count:Int):Int

    @JvmStatic fun main(args:Array<String>) {
        require(args.size==1){"Pass an explicit locally built experimental DLL"}
        val dll=File(args.single()).canonicalFile
        require(dll.isFile){"Native experiment is unavailable: $dll"}
        System.load(dll.path)
        fun buffer(bytes:Int)=ByteBuffer.allocateDirect(bytes).order(ByteOrder.nativeOrder())
        val report=mutableListOf("EXPERIMENTAL: actual Rust JNI library ${dll.name}",
            "JVM ${System.getProperty("java.version")}; ${System.getProperty("os.name")} ${System.getProperty("os.arch")}",
            "CPUs ${Runtime.getRuntime().availableProcessors()}; maxHeap ${Runtime.getRuntime().maxMemory()}",
            "CPU kernel and bitmap preparation only; excludes shaping, GPU, draw and full-frame latency.")
        val random=Random(20261116)
        var cases=0
        for((width,height) in listOf(1 to 1,3 to 7,37 to 13,160 to 80,320 to 160,640 to 320)) {
            val count=width*height
            val pixels=IntArray(count){random.nextInt()}
            val source=buffer(count*4).apply{asIntBuffer().put(pixels)}
            val a=buffer(count*8);val b=buffer(count*8)
            val radii=buffer(5*4).apply{asFloatBuffer().put(floatArrayOf(1f,2.25f,10f,20f,4096f))}
            val output=buffer(count*5*4)
            check(blur(source,a,b,radii,output,width,height,5)==0)
            val read=output.asIntBuffer()
            val alpha=DoubleArray(count){(pixels[it] ushr 24).toDouble()};val scratch=DoubleArray(count)
            for((r,sigma) in listOf(1f,2.25f,10f,20f,4096f).withIndex()) {
                for(i in pixels.indices)alpha[i]=(pixels[i] ushr 24).toDouble()
                val expected=blurBoxCoverage(alpha,scratch,width,height,sigma)
                for(i in pixels.indices)check(((read[r*count+i] ushr 24))==expected[i].toInt()){
                    "Kernel mismatch $width/$height sigma=$sigma pixel=$i"}
                cases++
                for(i in pixels.indices)alpha[i]=(pixels[i] ushr 24).toDouble()
                contiguousBlur(alpha,scratch,width,height,sigma)
                for(i in pixels.indices)check((read[r*count+i] ushr 24)==alpha[i].toInt())
            }
            check(blur(source,a,b,radii,source,width,height,1)==2){"Aliasing must fail"}
            check(blur(ByteBuffer.allocate(count*4),a,b,radii,output,width,height,1)==1)
            check(blur(source,a,b,radii,buffer(0),width,height,1)==1)
            check(blur(source,a,b,radii,output,0,height,1)==1)
            check(blur(source,a,b,radii,output,width,height,17)==1)
            check(blur(source,a.asReadOnlyBuffer(),b,radii,output,width,height,1)==1)
            check(blur(source,a,b.asReadOnlyBuffer(),radii,output,width,height,1)==1)
            check(blur(source,a,b,radii,output.asReadOnlyBuffer(),width,height,1)==1)
            val unaligned=buffer(count*8+1).apply{position(1)}.slice().order(ByteOrder.nativeOrder())
            check(blur(source,unaligned,b,radii,output,width,height,1)==1)
            radii.putFloat(0,Float.NaN)
            check(blur(source,a,b,radii,output,width,height,1)==1)
        }
        report+="Exact alpha parity: $cases random size/radius cases; aliases, read-only/heap/short/misaligned buffers, invalid count/dimensions/NaN rejected."
        val bean=ManagementFactory.getThreadMXBean() as? com.sun.management.ThreadMXBean
        val thread=Thread.currentThread().threadId()
        for((width,height) in listOf(320 to 160,640 to 320)) {
            val n=width*height
            val pixels=IntArray(n){i->if(i%width in width/4..width*3/4 && i/width in height/3..height*2/3)-1 else 0}
            val image=bitmapFromArgb(width,height,pixels)
            val src=buffer(n*4);val sourceView=src.asIntBuffer();val a=buffer(n*8);val b=buffer(n*8)
            val sigmas=buffer(8).apply{asFloatBuffer().put(floatArrayOf(10f,20f))}
            val output=buffer(n*8);val outputView=output.asIntBuffer();val readback=IntArray(n)
            val alpha=DoubleArray(n);val scratch=DoubleArray(n)
            val kotlinOutput=IntArray(n*2)
            var checksum=0L
            fun nativeKernel(){check(blur(src,a,b,sigmas,output,width,height,2)==0);checksum+=outputView[n/2].toLong()}
            fun kotlinKernel(){for((r,sigma) in floatArrayOf(10f,20f).withIndex()){
                for(i in pixels.indices)alpha[i]=(pixels[i] ushr 24).toDouble()
                val result=blurBoxCoverage(alpha,scratch,width,height,sigma)
                for(i in pixels.indices)kotlinOutput[r*n+i]=(result[i].toInt() shl 24) or 0xffffff
                };checksum+=kotlinOutput[n/2].toLong()}
            fun contiguousKernel(){for((r,sigma) in floatArrayOf(10f,20f).withIndex()){
                for(i in pixels.indices)alpha[i]=(pixels[i] ushr 24).toDouble()
                contiguousBlur(alpha,scratch,width,height,sigma)
                for(i in pixels.indices)kotlinOutput[r*n+i]=(alpha[i].toInt() shl 24) or 0xffffff
                };checksum+=kotlinOutput[n/2].toLong()}
            fun nativePrepare(){image.readPixels(readback);sourceView.position(0);sourceView.put(readback);nativeKernel()
                repeat(2){r->outputView.position(r*n);outputView.get(readback);val mask=bitmapFromArgb(width,height,readback)
                    mask.readPixels(readback);checksum+=readback[n/2].toLong()}}
            fun kotlinPrepare(){val masks=blurredMasks(image,floatArrayOf(10f,20f));masks.forEach{
                it.readPixels(readback);checksum+=readback[n/2].toLong()}}
            fun reusedKotlinPrepare(){image.readPixels(readback)
                for(sigma in floatArrayOf(10f,20f)) {
                    for(i in pixels.indices)alpha[i]=(readback[i] ushr 24).toDouble()
                    contiguousBlur(alpha,scratch,width,height,sigma)
                    val result=IntArray(n){(alpha[it].toInt() shl 24) or 0xffffff}
                    val mask=bitmapFromArgb(width,height,result)
                    mask.readPixels(result);checksum+=result[n/2].toLong()
                }}
            sourceView.position(0);sourceView.put(pixels)
            val methods=listOf("Kotlin-kernel" to ::kotlinKernel,"Kotlin-contiguous-kernel" to ::contiguousKernel,
                "Rust-JNI-kernel" to ::nativeKernel,"Kotlin-prepare" to ::kotlinPrepare,
                "Kotlin-reused-contiguous-prepare" to ::reusedKotlinPrepare,"Rust-JNI-prepare" to ::nativePrepare)
            methods.forEach{(_,run)->repeat(12){run()}}
            val times=methods.associate{it.first to mutableListOf<Double>()}
            val allocations=methods.associate{it.first to mutableListOf<Long>()}
            repeat(40){round->
                // Alternating order; no fastest-run selection.
                val order=if(round%2==0)methods else methods.reversed()
                order.forEach{(name,run)->
                    val before=bean?.getThreadAllocatedBytes(thread)?:0L
                    val begin=System.nanoTime();run();val elapsed=(System.nanoTime()-begin)/1e6
                    times.getValue(name)+=elapsed
                    allocations.getValue(name)+=(bean?.getThreadAllocatedBytes(thread)?:0L)-before
                }
            }
            methods.forEach{(name,_)->
                val t=times.getValue(name).sorted()
                report+="$width x $height $name ms P50=${t[19]} P95=${t[37]} P99=${t[39]}; JVM bytes/op=${allocations.getValue(name).average()}"
            }
            report+="Checksum (anti-elision, not cross-path equality): $checksum; persistent JNI direct buffers=${n*28+8} bytes; excludes native bitmap/driver allocations."
        }
        val text=report.joinToString("\n")+"\n"
        File("build/verification").mkdirs()
        File("build/verification/native-mask-experiment.txt").writeText(text)
        println(text)
    }

    /** Same row-contiguous vertical recurrence as Rust, with safe JVM indexing.
     * Probe-only candidate; production is unchanged until its own validation. */
    private fun contiguousBlur(a:DoubleArray,b:DoubleArray,width:Int,height:Int,sigma:Float) {
        val window=max(1,floor(sigma*3*sqrt(2*PI)/4+.5).toInt())
        val widths=intArrayOf(window,window,window+if(window%2==0)1 else 0)
        var input=a;var output=b
        for(horizontal in listOf(true,false)) {
            widths.forEachIndexed{pass,count->
                val left=count/2+if(count%2==0 && pass==1)-1 else 0
                val right=count-left-1
                if(horizontal) {
                    for(y in 0 until height) {
                        val base=y*width;var total=0.0
                        for(x in 0..min(right,width-1))total+=input[base+x]
                        for(x in 0 until width) {
                            output[base+x]=total
                            if(x-left>=0)total-=input[base+x-left]
                            if(x+right+1<width)total+=input[base+x+right+1]
                        }
                    }
                } else {
                    for(x in 0 until width) {
                        var total=0.0
                        for(y in 0..min(right,height-1))total+=input[y*width+x]
                        output[x]=total
                    }
                    for(y in 1 until height)for(x in 0 until width) {
                        var total=output[(y-1)*width+x]
                        if(y-1-left>=0)total-=input[(y-1-left)*width+x]
                        if(y+right<height)total+=input[(y+right)*width+x]
                        output[y*width+x]=total
                    }
                }
                val swap=input;input=output;output=swap
            }
            val divisor=widths.fold(1.0){p,v->p*v}
            for(i in input.indices)input[i]=floor(input[i]/divisor+.5).coerceIn(0.0,255.0)
        }
        check(input===a)
    }
}
