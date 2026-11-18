// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.*
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import top.ghhccghk.accompanist.lyrics.folia.*
import java.io.File
import javax.imageio.ImageIO

/** Dedicated non-focusable real window. Exercises the actual GPU pipeline,
 * never reads user media, preferences or controls another app/process. */
object DioramaWindowVerification {
    @JvmStatic fun main(args:Array<String>) {
        var failure:Throwable?=null
        var completed=false
        application(exitProcessOnExit=false) {
            val state=rememberWindowState(width=800.dp,height=600.dp)
            var millis by remember{mutableLongStateOf(1600)}
            var token by remember{mutableIntStateOf(0)}
            var drawn by remember{mutableIntStateOf(-1)}
            var recordEpoch by remember{mutableIntStateOf(0)}
            var status by remember{mutableStateOf<FoliaStatus?>(null)}
            var attached by remember{mutableStateOf(true)}
            val font=remember{FontFamily(Font(File("verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))}
            val lyrics=remember{SyncedLyrics((0..8).map{i->
                val start=700+i*2900
                KaraokeLine.MainKaraokeLine(listOf("Amber ","echoes ","beyond").mapIndexed{j,word->
                    KaraokeSyllable(word,start+j*700,start+(j+1)*700)},null,
                    KaraokeAlignment.Unspecified,start,start+2100,null)
            })}
            Window(onCloseRequest=::exitApplication,state=state,undecorated=true,focusable=false,
                title="Folia Diorama sampler verification") {
                val layer=rememberGraphicsLayer()
                val density=LocalDensity.current
                CompositionLocalProvider(LocalDensity provides Density(1f)) {
                    Box(Modifier.fillMaxSize().background(Color(0xff162435)).drawWithContent{
                        val epoch=recordEpoch
                        status // Async descendant readiness must invalidate the recorded parent too.
                        layer.record{drawRect(Color(0xff162435));this@drawWithContent.drawContent()}
                        drawLayer(layer)
                        drawn=epoch
                    }) {
                        if(attached)FoliaLyricsView(lyrics,{FoliaPlaybackSnapshot(millis,false,token.toLong())},Modifier.fillMaxSize(),
                            typography=FoliaTypography(main=TextStyle(fontFamily=font,fontWeight=FontWeight.Bold)),
                            style=FoliaStyle.Diorama(DioramaTuning(rasterMemoryBudgetBytes=1)),
                            options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false),onStatus={status=it})
                    }
                }
                LaunchedEffect(Unit) {
                    val out=File("build/verification/diorama-native-window/${System.getProperty("skiko.renderApi","AUTO")}").apply{mkdirs()}
                    out.resolve("SUCCESS").delete()
                    try {
                        withTimeout(90_000) {
                            while(!window.isShowing)withFrameNanos{}
                            val checks=StringBuilder("media_ms\twidth\theight\trender_api\n")
                            suspend fun awaitReady(width:Int) {
                                var stable=0
                                while(stable<4) {
                                    withFrameNanos{}
                                    stable=if(layer.size.width==width&&layer.size.height==600&&(status?.mainUnitCount?:0)>0&&
                                        status?.diagnostics?.none{it.startsWith("Diorama")}==true)stable+1 else 0
                                }
                            }
                            for((time,width)in listOf(1600L to 800,7600L to 800,15600L to 380,3600L to 380,1600L to 800)) {
                                status=null;millis=time;token++
                                state.size=DpSize((width/density.density).dp,(600/density.density).dp)
                                awaitReady(width)
                                recordEpoch++
                                while(drawn!=recordEpoch)withFrameNanos{}
                                // Await the native renderer, not only recorded commands.
                                delay(180)
                                awaitReady(width)
                                val image=layer.toImageBitmap()
                                val pixels=IntArray(image.width*image.height).also{image.readPixels(it)}
                                ImageIO.write(image.toAwtImage(),"png",out.resolve("$time-$width.png"))
                                check(pixels.indices.any{it>=pixels.size/4&&pixels[it]!=Color(0xff162435).toArgb()}){
                                    "No ready lyric foreground below the status-label area"
                                }
                                delay(120)
                                val repeated=layer.toImageBitmap()
                                val paused=IntArray(pixels.size).also{repeated.readPixels(it)}
                                check(pixels.contentEquals(paused)){"Paused stage drifted"}
                                ImageIO.write(image.toAwtImage(),"png",out.resolve("$time-$width.png"))
                                checks.append("$time\t$width\t600\t${window.renderApi}\n")
                                println("DIORAMA_WINDOW_CHECK $time/$width actual=${window.renderApi}")
                            }
                            attached=false;token++;repeat(4){withFrameNanos{}}
                            val empty=layer.toImageBitmap()
                            val pixels=IntArray(empty.width*empty.height).also{empty.readPixels(it)}
                            check(pixels.all{it==pixels[0]}){"Unmount left pixels"}
                            out.resolve("checks.tsv").writeText(checks.toString())
                            out.resolve("conditions.txt").writeText("Requested=${System.getProperty("skiko.renderApi","AUTO")};actual=${window.renderApi};OS=${System.getProperty("os.name")};Java=${System.getProperty("java.version")};platformDensity=$density\nReal GPU window; recorded-layer pixels, not OS screenshot/upstream/device/performance verification. Forced disk cache; nine synthetic core lines; fixed parity Latin font; 800/380x600; pauses/seeks/resize/detach.\n")
                            out.resolve("SUCCESS").writeText("Actual ${window.renderApi} drew public Diorama; pause/seek/resize/detach passed.\n")
                            completed=true
                        }
                    }catch(error:Throwable){failure=error;out.resolve("failure.txt").writeText(error.stackTraceToString()+"\nstatus=$status\n")}
                    finally{exitApplication()}
                }
            }
        }
        failure?.let{throw IllegalStateException("Diorama native window verification failed",it)}
        check(completed){"Verification interrupted"}
    }
}
