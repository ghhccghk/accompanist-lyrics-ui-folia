// Copyright (c) 2026 chthollyphile and contributors
// Folia cc6226e4 VisualizerCappella.tsx / CappellaBubbleParts.tsx, text-only configuration.
// SPDX-License-Identifier: AGPL-3.0-only. See docs/upstream.md.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import top.ghhccghk.accompanist.lyrics.folia.internal.text.*
import kotlin.math.*

/** Complete native target stage. This internal seam deliberately does not claim
 * row entry/exit, Framer layout/springs or .2s resize transition acceptance. */
@Composable
internal fun CappellaTargetStage(lines:List<CappellaLine>,title:String,time:()->Double,
    currentLine:()->Int,textStyle:TextStyle,modifier:Modifier=Modifier,
    intensity:CappellaIntensity=CappellaIntensity.NORMAL,palette:CappellaPalette=CappellaPalette(),
    fontScale:Double=1.0,reducedMotion:Boolean=false,playbackMotion:Boolean=false,
    onRows:(List<CappellaRowLayout>)->Unit={}) {
    val density=LocalDensity.current;val resolver=LocalFontFamilyResolver.current
    val measurer=rememberTextMeasurer(cacheSize=32)
    val media by rememberUpdatedState(time);val selected by rememberUpdatedState(currentLine)
    val observer by rememberUpdatedState(onRows)
    BoxWithConstraints(modifier.fillMaxSize()) {
        val width=maxWidth.value.toDouble();val height=maxHeight.value.toDouble()
        val prep=remember(lines,title,width,fontScale,intensity,measurer,textStyle,density,resolver) {
            CappellaStagePreparation(lines,title,cappellaStageSize(width,fontScale),intensity,measurer,textStyle,density,resolver)
        }
        val animation=remember(prep,height,playbackMotion) {
            if(playbackMotion)CappellaPlaybackPreparation(prep,height)else null
        }
        val moving by remember(animation,reducedMotion) { derivedStateOf { animation?.at(media(),reducedMotion) } }
        // Target mode recomposes at geometry boundaries; moving frame sizes
        // necessarily update layout during their finite media-time responses.
        val rows by remember(prep,height,animation,reducedMotion) { derivedStateOf {
            moving?.map{it.layout}?:prep.at(media(),selected(),height)
        } }
        val current by remember { derivedStateOf { selected() } }
        SideEffect { observer(rows) }
        for((index,row) in rows.withIndex()) key(row.message.id) {
            val motion=moving?.get(index)
            val m=row.message;val line=m.line?.let(lines::get)
            val active=motion?.active?:(line!=null&&m.line==current)
            val right=m.sender.side==CappellaSide.RIGHT
            val config=motion?.style?:cappellaBubbleStyle(prep.size,prep.motion,active,line==null)
            val box=motion?.size?:CappellaBubbleSize(row.bubbleWidth/row.scale,row.bubbleHeight/row.scale)
            val placement=CappellaPlacement(row.bubbleX,row.bubbleY,row.scale,row.opacity)
            val epoch=motion?.glowEpoch?:line?.start?:0.0
            val decoration=remember(palette,intensity,epoch) { CappellaDecoration(palette,intensity,epoch) }
            CappellaBubbleTarget(line?:CappellaLine(m.text,0.0,0.0,emptyList()),{media()},textStyle,
                cappellaBubbleColors(m.sender,palette.primary,palette.accent,palette.secondary,palette.background),
                Modifier.fillMaxSize(),config,right,active,reducedMotion,box,placement,decoration)
            CappellaAvatarTarget(CappellaPlacement(row.avatarX,row.avatarY,row.scale,row.opacity*(motion?.avatarOpacity?:1.0)),m.sender,
                palette,decoration,Modifier.fillMaxSize())
            if(line!=null) {
                val plan=remember(line){CappellaRevealPlan(line)}
                val ready by remember(plan,m.line,active) { derivedStateOf {
                    m.line<selected() || media()>=(if(active)plan.timestampReady else line.end)
                } }
                val stamp=if(animation!=null)motion?.timestamp else if(ready)CappellaTimestampFrame(.62,0.0)else null
                if(stamp!=null)CappellaTimestampTarget(cappellaTimestamp(line.end),textStyle,palette.secondary,
                    box,placement,right,Modifier.fillMaxSize(),stamp)
            }
        }
    }
}

@Composable
private fun CappellaAvatarTarget(placement:CappellaPlacement,sender:CappellaSender,palette:CappellaPalette,
    decoration:CappellaDecoration,modifier:Modifier) {
    val density=LocalDensity.current
    val shadows=remember(density,decoration) { cappellaShadows(40.0,40.0,density,false,false,decoration,true) }
    Canvas(modifier) { withCappellaPlacement(placement) {
        val d=density.density;val w=40*d;val outline=Path().apply{addOval(Rect(0f,0f,w,w))}
        drawCappellaShadows(shadows,outline)
        clipPath(outline) {
            drawCircle(palette.secondary,radius=20*d,center=Offset(20*d,20*d))
            // Original color-avatar mode crops a 3x3,135-degree gradient image
            // into the padding box (38px), rather than stretching it per avatar.
            val indices=listOf(0,3,6,1,4)
            val index=if(sender.side==CappellaSide.RIGHT)8 else indices[sender.avatar%indices.size]
            val inner=38*d;val offset=Offset((index%3)*inner,(index/3)*inner)
            val start=Offset(d,d)-offset
            drawRect(Brush.linearGradient(listOf(palette.primary,palette.accent),start,start+Offset(inner*3,inner*3)),
                topLeft=Offset(d,d),size=Size(inner,inner))
            drawCircle(Color.White.copy(alpha=.24f),radius=19.5f*d,center=Offset(20*d,20*d),style=Stroke(d))
        }
    } }
}

@Composable
private fun CappellaTimestampTarget(text:String,base:TextStyle,color:Color,box:CappellaBubbleSize,
    placement:CappellaPlacement,right:Boolean,modifier:Modifier,frame:CappellaTimestampFrame) {
    val density=LocalDensity.current;val resolver=LocalFontFamilyResolver.current
    val measurer=rememberTextMeasurer(cacheSize=8)
    val style=remember(base){base.copy(fontSize=11.sp,fontWeight=FontWeight.Medium,
        fontFeatureSettings="tnum",lineHeight=TextUnit.Unspecified)}
    val paragraph=remember(text,style,density){measurer.measure(text,style,softWrap=false)}
    val metrics=remember(style,resolver,density){naturalFontMetrics(style,resolver,density)}
    val advance=remember(text,style,resolver,density,paragraph){
        cappellaCanvasAdvance(text,style,resolver,density)?:paragraph.size.width/density.density.toDouble()
    }
    Canvas(modifier) { withCappellaPlacement(placement) {
        val d=density.density
        val x=if(right)-advance-8 else box.width+8
        // CSS normal line box uses the font's natural ascent/descent.
        val top=((box.height-4)*d).toFloat()-metrics.descent-paintedNaturalBaseline(paragraph,metrics)
        drawText(paragraph,color,Offset((x*d).toFloat(),top+(frame.y*d).toFloat()),alpha=frame.opacity.toFloat(),shadow=Shadow.None)
    } }
}
