// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.dp
import com.mocharealm.accompanist.lyrics.core.model.*
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import java.nio.file.Files
import java.util.concurrent.CopyOnWriteArrayList
import javax.imageio.ImageIO
import kotlin.test.*

@OptIn(ExperimentalTestApi::class)
class PublicDioramaDiskCacheTest {
    @Test fun cachedPublicStageEqualsMemoryAcrossSeeksOffsetResizeAndUnmount()=runDesktopComposeUiTest(width=800,height=600){
        val root=Files.createTempDirectory("folia-public-cache-").toFile()
        val stores=CopyOnWriteArrayList<DioramaFileRasterDisk>()
        val factory:()->DioramaRasterDisk={
            val store=DioramaFileRasterDisk(root).also{stores+=it}
            object:DioramaRasterDisk{
                override fun write(image:ImageBitmap):Int{
                    assertFalse(java.awt.EventQueue.isDispatchThread(),"no disk writes on UI thread")
                    return store.write(image)
                }
                override fun read(id:Int):ImageBitmap{
                    assertFalse(java.awt.EventQueue.isDispatchThread(),"no disk reads on UI thread")
                    return store.read(id)
                }
                override fun close()=store.close()
            }
        }
        val core=SyncedLyrics((0 until 9).map{i->SyncedLine("Synthetic mirror stanza $i",null,700+i*2000,2300+i*2000)})
        val font=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
        var time by mutableStateOf(1600L);var offset by mutableIntStateOf(0);var width by mutableIntStateOf(800)
        var mounted by mutableStateOf(true);var cache by mutableStateOf(false)
        var status:FoliaStatus?=null;var clicked:ISyncedLine?=null
        setContent{CompositionLocalProvider(LocalDioramaRasterDiskFactory provides factory){
            Box(Modifier.fillMaxSize().background(Color(0xff162435))){if(mounted)
                FoliaLyricsView(core,{FoliaPlaybackSnapshot(time,false)},Modifier.width(width.dp).height(600.dp),
                    typography=FoliaTypography(main=font),
                    style=FoliaStyle.Diorama(DioramaTuning(cacheTexturesOnDisk=cache,rasterMemoryBudgetBytes=1)),
                    options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false,offsetMillis=offset),
                    onStatus={status=it},onLineClicked={clicked=it})
            }
        }}
        fun ready(){waitForIdle();waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0&&status!!.diagnostics.none{
            it.startsWith("DioramaPrepar")||it.startsWith("DioramaTexture")}};waitForIdle()}
        fun pixels():IntArray{val im=captureToImage();return IntArray(im.width*im.height).also{im.readPixels(it)}}
        try {
            ready()
            val expected=mutableMapOf<Long,IntArray>()
            val matrix=listOf(1600L,3600L,15600L,7600L)
            for(t in matrix){runOnIdle{time=t};ready();expected[t]=pixels()}
            runOnIdle{cache=true;time=1600;status=null};ready()
            assertTrue(stores.isNotEmpty(),"forced disk path really prepared")
            for(t in matrix+matrix.reversed()){
                runOnIdle{time=t};ready()
                assertTrue(expected.getValue(t).contentEquals(pixels()),"disk exact frame at $t")
            }
            val out=File("../build/verification/diorama-disk-cache").apply{mkdirs()}
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("cached-800.png"))
            val paused=pixels();mainClock.advanceTimeBy(1600);assertTrue(paused.contentEquals(pixels()))
            runOnIdle{time=1800;offset=200};ready()
            assertTrue(expected.getValue(1600).contentEquals(pixels()),"offset applied once")
            val im=captureToImage();val rgb=IntArray(im.width*im.height).also{im.readPixels(it)}
            var found=false
            // Actual image ink point; transparent background is not clickable.
            for(y in 8 until 590 step 8){if(found)break;for(x in 8 until 790 step 8){
                if(rgb[y*800+x]==0xff162435.toInt())continue
                onRoot().performTouchInput{click(Offset(x.toFloat(),y.toFloat()))}
                if(clicked!=null){assertTrue(core.lines.any{it===clicked});found=true;break}
            }}
            assertTrue(found,"cached text remains interactive")
            runOnIdle{width=380;status=null};ready()
            ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve("cached-380.png"))
            runOnIdle{width=800;offset=0;time=1600;status=null};ready()
            assertTrue(expected.getValue(1600).contentEquals(pixels()),"resize restore")
            // Rapid out-of-window requests must not publish an obsolete window.
            for(t in listOf(15600L,3600L,11600L,7600L))runOnIdle{time=t}
            ready();assertTrue(expected.getValue(7600).contentEquals(pixels()),"latest seek wins")
            runOnIdle{mounted=false};waitForIdle()
            waitUntil(timeoutMillis=30000){stores.all{!it.file.exists()}}
            assertEquals(0,root.listFiles()!!.size)
        }finally{runOnIdle{mounted=false};waitForIdle();waitUntil(timeoutMillis=30000){stores.all{!it.file.exists()}};root.delete()}
    }

    @Test fun storageFailureIsVisibleAndDisablingCacheRecovers()=runDesktopComposeUiTest(width=380,height=600){
        val lyrics=SyncedLyrics(listOf(SyncedLine("Synthetic storage control",null,700,2600)))
        var cache by mutableStateOf(true);var status:FoliaStatus?=null
        val factory:()->DioramaRasterDisk={throw IllegalStateException("Synthetic cache unavailable")}
        setContent{CompositionLocalProvider(LocalDioramaRasterDiskFactory provides factory){
            FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(1600,false)},Modifier.fillMaxSize(),
                style=FoliaStyle.Diorama(DioramaTuning(cacheTexturesOnDisk=cache,rasterMemoryBudgetBytes=1)),
                onStatus={status=it})
        }}
        waitUntil(timeoutMillis=30000){status?.diagnostics?.any{it.contains("Synthetic cache unavailable")}==true}
        onNodeWithText("Synthetic cache unavailable").assertExists()
        runOnIdle{assertEquals(0,status!!.mainUnitCount);cache=false;status=null}
        waitUntil(timeoutMillis=30000){(status?.mainUnitCount?:0)>0&&status!!.diagnostics.none{it.startsWith("DioramaPrepar")}}
    }
}
