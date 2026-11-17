// Copyright (c) 2026 chthollyphile and contributors
// SPDX-License-Identifier: AGPL-3.0-only
// Text-stage integration of pinned Folia useFumeCanvasStage/VisualizerFume;
// media-clock reconstruction/cancellable preparation: docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.*
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext
import kotlin.math.max

internal data class FumePreparedStage(val article:FumeArticle,val drawing:FumeArticleDrawing,
    val track:FumeCameraTrack?,val viewport:FumeViewport,val overviewStart:Double)

/** Inverse of the actual article camera. Whole row text bounds exclude blank
 * article space; waiting text is intentionally faint and visible upstream. */
internal fun fumeHitTest(prepared:FumePreparedStage,camera:FumeCameraFrame,point:Offset):Int? {
    val p=camera.state.pose
    val x=(point.x-prepared.viewport.width/2)/p.scale+p.x
    val y=(point.y-prepared.viewport.height/2)/p.scale+p.y
    return prepared.article.blocks.asReversed().firstOrNull{block->
        prepared.drawing.focusGeometry.getValue(block.sourceIndex).rows.any{row->
            row.width>0&&x>=block.x+row.left&&x<block.x+row.left+row.width&&
                y>=block.y+row.top&&y<block.y+row.top+block.lineHeight
        }
    }?.sourceIndex
}

@Composable
internal fun FumeArticleStage(lines:List<FumeLyric>,time:()->Double,currentLine:()->Int,
    textStyle:TextStyle,theme:FumeFrameTheme,modifier:Modifier=Modifier,fontScale:Double=1.0,
    heroScale:Double=1.0,hold:Double=1.0,cameraSpeed:Double=1.0,stepped:Boolean=false,
    reducedMotion:Boolean=false,showStamp:Boolean=true,layoutSeed:String="Fume",floatOrigin:Double=0.0,
    stageEntryEnabled:Boolean=true,
    onClick:((Int)->Unit)?=null,onLongPress:((Int)->Unit)?=null,
    onState:(Boolean,String?)->Unit={_,_->},onPrepared:(FumePreparedStage)->Unit={}) {
    val density=LocalDensity.current;val resolver=LocalFontFamilyResolver.current;val direction=LocalLayoutDirection.current
    val rasterDensity=remember(density){Density(density.density,1f)}
    val clock by rememberUpdatedState(time);val selected by rememberUpdatedState(currentLine)
    val observer by rememberUpdatedState(onPrepared);val status by rememberUpdatedState(onState)
    val click by rememberUpdatedState(onClick);val longPress by rememberUpdatedState(onLongPress)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val viewport=FumeViewport(maxWidth.value.toDouble(),maxHeight.value.toDouble())
        var prepared by remember(lines,viewport,textStyle,density,resolver,direction,fontScale,heroScale,layoutSeed,
            cameraSpeed,stepped,reducedMotion,theme.intensity,floatOrigin){mutableStateOf<FumePreparedStage?>(null)}
        var error by remember(lines,viewport,textStyle,density,resolver,direction,fontScale,heroScale,layoutSeed,
            cameraSpeed,stepped,reducedMotion,theme.intensity,floatOrigin){mutableStateOf<String?>(null)}
        LaunchedEffect(lines,viewport,textStyle,density,resolver,direction,fontScale,heroScale,layoutSeed,
            cameraSpeed,stepped,reducedMotion,theme.intensity,floatOrigin){
            status(false,"FumePreparing")
            try {
                val result=withContext(Dispatchers.Default){
                    val context=coroutineContext
                    val checkpoint={context.ensureActive()}
                    if(lines.none{it.text.isNotBlank()})throw IllegalStateException("没有非空的浮名文章")
                    // Build-local measurer: no mutable cache is shared between
                    // cancelled/replacement jobs or with the UI draw thread.
                    val measurer=TextMeasurer(resolver,rasterDensity,direction,cacheSize=256)
                    val measure=FumeTextMeasure(measurer,textStyle,rasterDensity)
                    val article=buildFumeArticle(lines,viewport,layoutSeed,fontScale*density.fontScale,heroScale){text,hero,size,width->
                        checkpoint();measure.countLines(text,hero,size,width)
                    }?:throw IllegalStateException("没有非空的浮名文章")
                    val drawing=FumeArticleDrawing.prepare(article,lines,measure,resolver,rasterDensity,checkpoint)
                    val last=lines.lastOrNull{it.text.isNotBlank()}
                    val overviewStart=last?.let{it.start+max(it.renderEnd-it.start,0.0)*.5}?:Double.POSITIVE_INFINITY
                    val events=(listOf(0.0)+lines.flatMap{listOf(it.start,it.renderEnd)}).filter{it>=0}.distinct().sorted()
                    val epochs=events.map{t->FumeFocusEpoch(t,lines.indexOfLast{it.start<=t&&t<=it.renderEnd}.takeIf{it in drawing.focusGeometry}?:-1)}
                    val track=if(reducedMotion)null else FumeCameraTrack(article,drawing.focusGeometry,epochs,
                        FumeCameraTrackConfig(viewport,overviewStart,cameraSpeed,stepped,intensity=theme.intensity,
                            floatOrigin=floatOrigin,extendTail=true),max(0.0,lines.maxOfOrNull{it.renderEnd}?:0.0)+2,checkpoint)
                    checkpoint();FumePreparedStage(article,drawing,track,viewport,overviewStart)
                }
                prepared=result;observer(result);status(true,null)
            }catch(cancelled:CancellationException){throw cancelled}
            catch(failure:IllegalStateException){error=failure.message?:"浮名准备失败";status(false,"FumePreparationFailed: $error")}
        }
        val p=prepared
        DisposableEffect(p){onDispose{p?.drawing?.releaseStaticSnapshots()}}
        if(p==null){BasicText(error?:"正在准备浮名文章…",style=textStyle.copy(color=theme.primary.color()));return@BoxWithConstraints}
        fun frame(t:Double):FumeCameraFrame = p.track?.sample(t)?:run{
            val overview=fumeOverview(p.article,viewport)
            val intent=fumeCameraIntent(fumeInitialCamera(p.article),p.article,p.drawing.focusGeometry,
                FumeCameraInput(t,t,0.0,selected(),viewport,overview,p.overviewStart,cameraSpeed,stepped,static=true,intensity=theme.intensity))
            FumeCameraFrame(FumeCameraState(FumeCameraPose(intent.target.x,intent.target.y,scale=intent.target.scale)),intent.overview,if(intent.overview)1.0 else 0.0)
        }
        val visible by remember(p,reducedMotion,theme){derivedStateOf{
            val camera=frame(clock())
            p.article.blocks.filter{fumeBlockOnScreen(it,camera.state.pose.view(),viewport,0.0)}
        }}
        val input=if(onClick==null&&onLongPress==null)Modifier else Modifier.pointerInput(p,density,reducedMotion){
            awaitEachGesture{
                val down=awaitFirstDown(requireUnconsumed=false)
                fun hit(point:Offset)=fumeHitTest(p,frame(clock()),point/density.density)
                val i=hit(down.position)?:return@awaitEachGesture
                down.consume()
                val outcome=withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis){true to waitForUpOrCancellation()}
                if(outcome==null){
                    if(hit(down.position)==i)longPress?.invoke(i)
                    do{val event=awaitPointerEvent();event.changes.forEach{it.consume()}}while(event.changes.any{it.pressed})
                }else outcome.second?.let{up->if(hit(up.position)==i)click?.invoke(i);up.consume()}
            }
        }
        Canvas(Modifier.fillMaxSize().graphicsLayer{
            val scale=if(stageEntryEnabled)fumeStageEntryScale(clock(),p.article.firstStart,reducedMotion).toFloat()else 1f
            scaleX=scale;scaleY=scale
        }.then(input).semantics{
            contentDescription=visible.joinToString("\n"){it.line.text}
            customActions=visible.flatMap{b->buildList{
                if(onClick!=null)add(CustomAccessibilityAction(b.line.text){click?.invoke(b.sourceIndex);true})
                if(onLongPress!=null)add(CustomAccessibilityAction("长按 ${b.line.text}"){longPress?.invoke(b.sourceIndex);true})
            }}
        }){val t=clock();clipRect{p.drawing.draw(this,viewport,t,frame(t),theme,hold,showStamp&&!reducedMotion)}}
    }
}
