// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Public preview host for pinned Diorama text/camera ports. docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Density
import com.mocharealm.accompanist.lyrics.core.model.*
import kotlinx.coroutines.*
import top.ghhccghk.accompanist.lyrics.folia.*
import kotlin.coroutines.coroutineContext
import kotlin.math.pow

/** Explicit retained-raster payload bound, excluding transient blur/driver
 * overhead; not a total-memory promise. Reject BEFORE allocating an image. */
internal class DioramaRasterBudget(private val limit:Long=96L*1024*1024,private val enforce:Boolean=true) {
    var bytes:Long=0;private set
    fun reserve(width:Int,height:Int,copies:Int) {
        if(!enforce)return
        check(width in 1..8192&&height in 1..8192&&copies in 1..3){"镜台文字纹理过大，请缩短单行歌词"}
        val extra=width.toLong()*height*4*copies
        check(extra<=limit-bytes){"镜台整曲文字超出实验预览纹理预算，请使用较短歌词"}
        bytes+=extra
    }
}

private class DioramaIdentityKey(private val value:Any) {
    override fun equals(other:Any?)=other is DioramaIdentityKey&&value===other.value
    override fun hashCode()=0
}

internal fun Color.dioramaLinear():DioramaLinearColor {
    val color=convert(ColorSpaces.Srgb)
    fun channel(v:Float)=if(v<=.04045f)v/12.92 else ((v+.055)/1.055).pow(2.4)
    return DioramaLinearColor(channel(color.red),channel(color.green),channel(color.blue))
}

@Composable
internal fun DioramaPlaybackStage(core:SyncedLyrics,time:()->Double,textStyle:TextStyle,
    modifier:Modifier=Modifier,scale:Float=1f,tuning:DioramaTuning=DioramaTuning(),
    colors:FoliaColors=FoliaColors(),options:FoliaOptions=FoliaOptions(),showText:()->Boolean={true},
    onClick:((ISyncedLine)->Unit)?=null,onLongPress:((ISyncedLine)->Unit)?=null,
    onState:(Boolean,String?)->Unit={_,_->},onPrepared:(DioramaTextPlayback)->Unit={}) {
    val density=LocalDensity.current;val resolver=LocalFontFamilyResolver.current;val direction=LocalLayoutDirection.current
    val clock by rememberUpdatedState(time);val status by rememberUpdatedState(onState)
    val observer by rememberUpdatedState(onPrepared)
    val visible by rememberUpdatedState(showText)
    val click by rememberUpdatedState(onClick);val longPress by rememberUpdatedState(onLongPress)
    val reduced=options.reducedMotion
    val mode=when(options.animationIntensity){
        FoliaAnimationIntensity.Calm->DioramaShotMode.calm
        FoliaAnimationIntensity.Normal->DioramaShotMode.normal
        FoliaAnimationIntensity.Chaotic->DioramaShotMode.chaotic
    }
    BoxWithConstraints(modifier.fillMaxSize()) {
        // Geometry lives in logical pixels; actual Canvas is scaled once below.
        val width=maxWidth.value.toDouble();val height=maxHeight.value.toDouble()
        var prepared by remember(DioramaIdentityKey(core),width,height,textStyle,resolver,direction,scale,tuning.fontScale,
            tuning.layoutSeed,tuning.enforceRasterBudget,density.fontScale,mode){mutableStateOf<DioramaTextPlayback?>(null)}
        var failure by remember(DioramaIdentityKey(core),width,height,textStyle,resolver,direction,scale,tuning.fontScale,
            tuning.layoutSeed,tuning.enforceRasterBudget,density.fontScale,mode){mutableStateOf<String?>(null)}
        LaunchedEffect(DioramaIdentityKey(core),width,height,textStyle,resolver,direction,scale,tuning.fontScale,
            tuning.layoutSeed,tuning.enforceRasterBudget,density.fontScale,mode) {
            status(false,"DioramaPreparing")
            if(width<=0||height<=0)return@LaunchedEffect
            try {
                val result=withContext(Dispatchers.Default) {
                    val context=coroutineContext;val budget=DioramaRasterBudget(enforce=tuning.enforceRasterBudget)
                    DioramaTextPlayback.prepare(core,textStyle,TextMeasurer(resolver,Density(1f,1f),direction,cacheSize=128),
                        resolver,width,height,seedName=tuning.layoutSeed,checkpoint={context.ensureActive()},mode=mode,
                        textScale=(scale*tuning.fontScale*density.fontScale).toDouble(),reserveRaster=budget::reserve)
                        .also{context.ensureActive()}
                }
                prepared=result;observer(result);status(true,null)
            }catch(cancelled:CancellationException){throw cancelled}
            catch(error:IllegalArgumentException){failure=error.message?:"镜台输入无效";status(false,"DioramaPreparationFailed: $failure")}
            catch(error:IllegalStateException){failure=error.message?:"镜台准备失败";status(false,"DioramaPreparationFailed: $failure")}
        }
        val p=prepared
        val plain=remember(p){DioramaPlainRenderer()};val active=remember(p){DioramaActiveBaseRenderer()}
        DisposableEffect(plain,active){onDispose{plain.close();active.close()}}
        if(p==null){BasicText(failure?:"正在准备镜台文字…",style=textStyle.copy(color=colors.secondary));return@BoxWithConstraints}
        val fog=remember(tuning.fogColor){tuning.fogColor.takeIf{it.alpha>0}?.let(::DioramaFog)}
        val primary=remember(colors.text){colors.text.dioramaLinear()}
        val accent=remember(colors.active){colors.active.dioramaLinear()}
        val secondary=remember(colors.secondary){colors.secondary.dioramaLinear()}
        val current by remember(p){derivedStateOf{p.camera.subjectIndex(clock().coerceIn(0.0,p.camera.endTime))}}
        // No image/mip preparation in Canvas or on every media tick.
        var preparedIndex by remember(p){mutableIntStateOf(-1)}
        LaunchedEffect(p,current){
            val t=clock().coerceIn(0.0,p.camera.endTime)
            p.preparePlainRenderer(plain,t);p.prepareActiveBaseRenderer(active,t)
            // Sampler owners are intentionally not Compose state. Publish their
            // completion so a paused frame cannot retain the initial fallback.
            preparedIndex=current
        }
        val input=if(onClick==null&&onLongPress==null)Modifier else Modifier.pointerInput(p,density,reduced,fog){
            awaitEachGesture {
                val down=awaitFirstDown(requireUnconsumed=false)
                fun hit(point:Offset):Int? {if(!visible())return null;val t=clock().coerceIn(0.0,p.camera.endTime)
                    return p.hitTest(t,p.camera.perspectiveAt(t,reduced),point/density.density,reduced,fog)}
                val i=hit(down.position)?:return@awaitEachGesture
                down.consume()
                val outcome=withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis){true to waitForUpOrCancellation()}
                if(outcome==null){
                    if(hit(down.position)==i)longPress?.invoke(p.lines[i].core.source)
                    do{val event=awaitPointerEvent();event.changes.forEach{it.consume()}}while(event.changes.any{it.pressed})
                }else outcome.second?.let{up->if(hit(up.position)==i)click?.invoke(p.lines[i].core.source);up.consume()}
            }
        }
        Canvas(Modifier.fillMaxSize().then(input).semantics {
            contentDescription=if(visible())p.lines[current].core.lyric.text else ""
        }) {
            if(!visible())return@Canvas
            val t=clock().coerceIn(0.0,p.camera.endTime)
            val s=density.density
            scale(s,s,Offset.Zero){inset(0f,0f,size.width-width.toFloat(),size.height-height.toFloat()){
            clipRect(0f,0f,width.toFloat(),height.toFloat()){
                p.drawStage(this,t,p.camera.perspectiveAt(t,reduced),primary,accent,secondary,reduced,fog,
                    plainRenderer=plain.takeIf{preparedIndex==current},activeBaseRenderer=active.takeIf{preparedIndex==current},transparentTextSurface=true,
                    activeFrames=if(options.quality!=FoliaQuality.Standard)p.run {
                        val i=camera.subjectIndex(t);val subject=camera.subjects[i]
                        val delta=dioramaComposeLocal(subject.frame,subject.placement.offsetR,subject.placement.offsetU,0.0)-camera.perspectiveAt(t,reduced).eye
                        val strength=if(options.quality==FoliaQuality.Minimal)0.0 else .6
                        lines[i].text.framesAt(t,primary,accent,DioramaUnitTuning(glow=strength,soul=if(reduced)0.0 else strength),
                            life=dioramaTextLife(kotlin.math.sqrt(delta.dot(delta))),breath=breathAt(t,reducedMotion=reduced))
                    }else null)
            }}}
        }
    }
}
