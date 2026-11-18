// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.ui.graphics.ImageBitmap
import java.io.*
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream
import java.util.zip.InflaterInputStream

internal actual class DioramaRasterCache actual constructor(private val memoryLimit:Long,
    private val residentLimit:Long,private val enforce:Boolean,private val diskFactory:()->DioramaRasterDisk) {
    private val rasters=mutableListOf<DioramaRaster>()
    private val pool=linkedMapOf<DioramaRaster,ImageBitmap>()
    private var published=emptyList<DioramaRaster>()
    private var disk:DioramaRasterDisk?=null
    private var closed=false
    actual val spilled:Boolean @Synchronized get()=disk!=null
    actual val totalBytes:Long @Synchronized get()=rasters.sumOf{it.bytes}
    actual val residentBytes:Long @Synchronized get()=pool.keys.sumOf{it.bytes}
    init{require(memoryLimit>0&&residentLimit>=0)}

    @Synchronized actual fun retain(image:ImageBitmap):DioramaRaster {
        check(!closed)
        val handle=DioramaRaster(image);rasters+=handle;pool[handle]=image
        if(disk==null&&residentBytes>memoryLimit)disk=diskFactory()
        disk?.let{store->
            // During initial preparation no handle is exposed to a renderer.
            for(entry in pool.keys.toList()) {
                entry.diskId=store.write(pool.getValue(entry));entry.resident=null
            }
            pool.clear()
        }
        return handle
    }

    @Synchronized actual fun load(required:List<DioramaRaster>,prefetch:List<DioramaRaster>,
        checkpoint:()->Unit):DioramaRasterWindow {
        check(!closed);checkpoint()
        val needed=required.distinct();val bytes=needed.sumOf{it.bytes}
        check(!enforce||bytes<=residentLimit){"镜台当前文字窗口超出纹理内存预算，请降低单行长度"}
        if(disk==null)return DioramaRasterWindow(pool.toMap())
        val budget=maxOf(memoryLimit,bytes)
        // Retired displayed handles stay alive until publish on the UI thread.
        // They never get invalidated while a frame is drawing.
        val missing=needed.filter{it !in pool}.sumOf{it.bytes}
        for(key in pool.keys.toList())if(key !in needed&&
            (key !in prefetch||residentBytes+missing>budget))pool.remove(key)
        fun get(key:DioramaRaster) {
            checkpoint()
            if(key !in pool)pool[key]=disk!!.read(checkNotNull(key.diskId))
            checkpoint()
        }
        needed.forEach(::get)
        for(key in prefetch.distinct())if(key !in pool&&key.bytes<=budget-residentBytes)get(key)
        checkpoint()
        return DioramaRasterWindow(pool.toMap())
    }

    /** Only UI thread after load has returned and its coroutine is still live. */
    @Synchronized actual fun publish(window:DioramaRasterWindow) {
        check(!closed)
        published.forEach{it.resident=null}
        window.images.forEach{(key,image)->key.resident=image}
        published=window.images.keys.toList()
    }

    @Synchronized actual fun close() {
        if(closed)return
        closed=true
        rasters.forEach{it.resident=null};rasters.clear();pool.clear();published=emptyList()
        disk?.close();disk=null
    }
}

/** Session-local compressed N32 premultiplied bytes, preserving native coverage
 * without PNG/unpremultiply quantization. Not portable persistent user data. */
internal class DioramaFileRasterDisk(parent:File,private val diskLimit:Long=512L*1024*1024):DioramaRasterDisk {
    val file:File
    private val records=mutableListOf<Record>()
    private var closed=false
    private data class Record(val offset:Long,val length:Long,val width:Int,val height:Int)
    init {
        try {file=File.createTempFile("folia-diorama-",".raster",parent)}
        catch(error:IOException){throw IllegalStateException("无法创建镜台纹理缓存，请检查缓存存储空间",error)}
    }
    override fun write(image:ImageBitmap):Int {
        check(!closed)
        try {
            val offset=file.length()
            val raw=dioramaNativeRasterBytes(image)
            check(raw.size.toLong()==image.width.toLong()*image.height*4){"镜台纹理缓存像素格式不匹配"}
            val deflater=Deflater(Deflater.BEST_SPEED)
            try {
                FileOutputStream(file,true).use{fileOut->
                    // Bound disk usage while streaming, not after filling disk.
                    val bounded=object:FilterOutputStream(fileOut){
                        var written=offset
                        override fun write(b:Int){check(written<diskLimit){"镜台临时纹理缓存超出存储预算"};out.write(b);written++}
                        override fun write(b:ByteArray,off:Int,len:Int){
                            check(len<=diskLimit-written){"镜台临时纹理缓存超出存储预算"};out.write(b,off,len);written+=len
                        }
                    }
                    DeflaterOutputStream(bounded,deflater,32768).use{it.write(raw)}
                }
            }finally{deflater.end()}
            records+=Record(offset,file.length()-offset,image.width,image.height)
            return records.lastIndex
        }catch(error:IOException){throw IllegalStateException("镜台纹理缓存写入失败，请检查缓存存储空间",error)}
    }
    override fun read(id:Int):ImageBitmap {
        check(!closed)
        val r=records[id]
        try {
            RandomAccessFile(file,"r").use{input->
                input.seek(r.offset)
                val bounded=object:InputStream(){
                    var left=r.length
                    override fun read():Int {if(left==0L)return -1;val v=input.read();if(v>=0)left--;return v}
                    override fun read(b:ByteArray,off:Int,len:Int):Int {
                        if(left==0L)return -1
                        val n=input.read(b,off,minOf(len.toLong(),left).toInt());if(n>0)left-=n;return n
                    }
                }
                InflaterInputStream(bounded).use{stream->
                    val expected=r.width.toLong()*r.height*4
                    check(expected<=Int.MAX_VALUE){"镜台单张纹理缓存过大"}
                    val raw=ByteArray(expected.toInt());var read=0
                    while(read<raw.size){val n=stream.read(raw,read,raw.size-read);check(n>0){"镜台纹理缓存不完整"};read+=n}
                    check(stream.read()==-1){"镜台纹理缓存长度无效"}
                    return dioramaNativeRasterFromBytes(r.width,r.height,raw)
                }
            }
        }catch(error:IOException){throw IllegalStateException("镜台纹理缓存读取失败，请重新加载歌词",error)}
    }
    override fun close() {
        if(closed)return
        closed=true;records.clear()
        check(!file.exists()||file.delete()){ "镜台临时纹理缓存清理失败" }
    }
}
