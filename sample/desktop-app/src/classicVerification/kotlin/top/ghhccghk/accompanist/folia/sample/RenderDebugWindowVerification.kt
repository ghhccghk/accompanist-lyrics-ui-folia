// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.folia.sample

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import org.jetbrains.skiko.SkiaLayer
import java.awt.Container
import java.io.File

/** Dedicated synthetic window; no preferences, audio or user window changes. */
object RenderDebugWindowVerification {
    @JvmStatic fun main(args:Array<String>) {
        var failure:Throwable?=null
        var completed=false
        application(exitProcessOnExit=false) {
            Window(onCloseRequest=::exitApplication,title="Folia render diagnostics verification",
                state=rememberWindowState(width=400.dp,height=240.dp),focusable=false) {
                val host=remember(window){DesktopRenderDebug(window)}
                var frame by remember{mutableIntStateOf(0)}
                Canvas(Modifier.fillMaxSize()) { drawRect(if(frame%2==0)Color.DarkGray else Color.Blue) }
                DisposableEffect(host){onDispose{host.setEnabled(false)}}
                LaunchedEffect(Unit) {
                    val out=File("build/verification/render-debug/${System.getProperty("skiko.renderApi","AUTO")}").apply{mkdirs()}
                    out.resolve("SUCCESS").delete()
                    try {
                        withTimeout(20_000) {
                            while(!window.isShowing)withFrameNanos{}
                            fun find(parent:Container):SkiaLayer? {
                                for(child in parent.components){
                                    if(child is SkiaLayer)return child
                                    if(child is Container)find(child)?.let{return it}
                                }
                                return null
                            }
                            val owner=checkNotNull(find(window)){"No actual window SkiaLayer"}
                            val delegate=checkNotNull(owner.renderDelegate)
                            host.setEnabled(true)
                            check(owner.renderDelegate!==delegate){"Hook not installed"}
                            // Changes are verification input, never created by instrumentation.
                            repeat(32){frame++;delay(22)}
                            val measured=host.read()
                            check(measured.frames>=4&&measured.fps>0&&measured.maxMillis>0){"No real callback measurements: $measured"}
                            val backend=window.renderApi.toString()
                            val metric=host.metric
                            check(host.backend==backend)
                            host.setEnabled(false)
                            check(owner.renderDelegate===delegate){"Original delegate not restored"}
                            repeat(6){frame++;delay(22)}
                            check(host.read()==RenderFrameStats()){"Disabled recorder still collecting"}
                            host.setEnabled(true)
                            check(host.read()==RenderFrameStats()){"Re-enable retained old measurements"}
                            repeat(8){frame++;delay(22)}
                            check(host.read().frames>=2){"Re-enabled hook did not resume"}
                            val installed=owner.renderDelegate
                            // A newer host delegate must never be overwritten on cleanup.
                            owner.renderDelegate=delegate
                            host.setEnabled(false)
                            check(owner.renderDelegate===delegate&&installed!==delegate)
                            out.resolve("checks.txt").writeText("requested=${host.requestedBackend};actual=$backend\n$measured\n$metric\nOriginal delegate restored; disabled updates ignored; re-enable resets; foreign delegate preserved.\nSynthetic input, not media performance or GPU-present FPS.\n")
                            out.resolve("SUCCESS").writeText("Actual render callback instrumentation lifecycle passed.\n")
                            println("RENDER_DEBUG_WINDOW actual=$backend $measured")
                            completed=true
                        }
                    }catch(error:Throwable){failure=error;out.resolve("failure.txt").writeText(error.stackTraceToString())}
                    finally{host.setEnabled(false);exitApplication()}
                }
            }
        }
        failure?.let{throw IllegalStateException("Render diagnostics verification failed",it)}
        check(completed){"Verification interrupted"}
    }
}
