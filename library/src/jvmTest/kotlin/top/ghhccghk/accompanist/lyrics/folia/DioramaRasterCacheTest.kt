// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.unit.*
import kotlinx.coroutines.CancellationException
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.nio.file.Files
import kotlin.test.*

class DioramaRasterCacheTest {
    private fun image(seed:Int):ImageBitmap=ImageBitmap(256,8).also{bitmap->
        CanvasDrawScope().draw(Density(1f),LayoutDirection.Ltr,Canvas(bitmap),Size(256f,8f)){
            // Cover all alpha bytes, colored fractional alpha and plus coverage.
            for(x in 0..255)drawRect(Color((seed%7)/6f,.32f,.87f,x/255f),
                androidx.compose.ui.geometry.Offset(x.toFloat(),0f),Size(1f,8f))
        }
    }
    private fun pixels(image:ImageBitmap)=IntArray(image.width*image.height).also{image.readPixels(it)}

    @Test fun nativePremultipliedCompressionRoundTripsExactlyAndDeletesOnlyOwnedFile(){
        val root=Files.createTempDirectory("folia-cache-test-").toFile()
        val foreign=File(root,"unrelated.txt").apply{writeText("keep")}
        val disk=DioramaFileRasterDisk(root)
        try{
            for(seed in 0..6){val source=image(seed);val id=disk.write(source)
                assertTrue(pixels(source).contentEquals(pixels(disk.read(id))),"seed $seed exact ARGB readback")}
            assertTrue(disk.file.length()>0)
            disk.close();assertFalse(disk.file.exists());assertEquals("keep",foreign.readText());disk.close()
            assertFailsWith<IllegalStateException>{disk.read(0)}
        }finally{disk.close();foreign.delete();root.delete()}
    }

    @Test fun spillsBeforeSongFinishesPublishesAtomicallyAndPrefetchIsReused(){
        val root=Files.createTempDirectory("folia-cache-test-").toFile()
        val disk=DioramaFileRasterDisk(root)
        var writes=0;var reads=0
        val cache=DioramaRasterCache(24576,diskFactory={object:DioramaRasterDisk{
            override fun write(image:ImageBitmap):Int {writes++;return disk.write(image)}
            override fun read(id:Int):ImageBitmap {reads++;return disk.read(id)}
            override fun close()=disk.close()
        }})
        try{
            val rasters=(0 until 40).map{cache.retain(image(it))}
            assertTrue(cache.spilled);assertEquals(40,writes);assertEquals(0,cache.residentBytes)
            assertEquals(327680,cache.totalBytes)
            assertTrue(rasters.none{it.isResident})
            cache.publish(cache.load(rasters.take(2),listOf(rasters[2])))
            assertEquals(3,reads);assertEquals(24576,cache.residentBytes)
            val displayed=rasters[0].image
            val next=cache.load(rasters.slice(1..2),listOf(rasters[3]))
            assertSame(displayed,rasters[0].image,"loading cannot invalidate the old frame")
            assertEquals(4,reads,"next active prefetched texture reused")
            cache.publish(next)
            assertFalse(rasters[0].isResident);assertTrue(rasters[3].isResident)
            val old=rasters[2].image;val cancel=CancellationException("new seek")
            var calls=0
            assertSame(cancel,assertFailsWith<CancellationException>{cache.load(listOf(rasters[29]),emptyList()){
                if(++calls==3)throw cancel
            }})
            assertSame(old,rasters[2].image,"cancelled load never publishes")
            cache.publish(cache.load(listOf(rasters[31]),emptyList()))
            cache.publish(cache.load(listOf(rasters[1]),emptyList()))
            assertTrue(pixels(image(1)).contentEquals(pixels(rasters[1].image)),"arbitrary backward seek")
            assertTrue(cache.residentBytes<=24576)
            cache.close();assertTrue(rasters.none{it.isResident});assertEquals(0,cache.residentBytes)
            assertFalse(disk.file.exists());assertEquals(0,root.listFiles()!!.size)
        }finally{cache.close();root.delete()}
    }

    @Test fun smallSongsNeverCreateDiskAndBoundsAndFailuresRemainVisible(){
        val cache=DioramaRasterCache(16384,residentLimit=4096,diskFactory={error("disk not expected")})
        val first=cache.retain(image(1));assertFalse(cache.spilled)
        assertFailsWith<IllegalStateException>{cache.load(listOf(first),emptyList())}
        cache.close()
        val root=Files.createTempDirectory("folia-cache-test-").toFile()
        val disk=DioramaFileRasterDisk(root)
        try{
            val id=disk.write(image(4));disk.file.writeBytes(byteArrayOf(1,2,3))
            assertFailsWith<IllegalStateException>{disk.read(id)}
            val tiny=DioramaFileRasterDisk(root,diskLimit=1)
            try{assertFailsWith<IllegalStateException>{tiny.write(image(4))}}finally{tiny.close()}
            assertFailsWith<IllegalStateException>{DioramaFileRasterDisk(File(root,"missing/parent"))}
        }finally{disk.close();root.delete()}
        assertFailsWith<IllegalArgumentException>{DioramaTuning(rasterMemoryBudgetBytes=0)}
    }
}
