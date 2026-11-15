// Folia v0.7.26 VisualizerSubtitleOverlay.tsx/subtitleFontSizes.ts/subtitleTrackStyle.ts.
// SPDX-License-Identifier: AGPL-3.0-only. Source ledger: docs/upstream.md.
package com.mocharealm.accompanist.lyrics.folia.internal.styles.classic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.rememberClassicLayer
import com.mocharealm.accompanist.lyrics.folia.internal.rendering.drawClassicLayer
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.*
import androidx.compose.ui.unit.*
import com.mocharealm.accompanist.lyrics.folia.*
import com.mocharealm.accompanist.lyrics.folia.internal.animation.sampleSubtitle
import com.mocharealm.accompanist.lyrics.folia.internal.model.NormalizedLine
import com.mocharealm.accompanist.lyrics.folia.internal.text.cssFontBaseline
import com.mocharealm.accompanist.lyrics.folia.internal.text.naturalFontMetrics
import com.mocharealm.accompanist.lyrics.folia.internal.text.paintedLineBaseline
import androidx.compose.ui.graphics.drawscope.clipRect
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.*
import kotlin.math.floor
import kotlin.math.ceil

internal data class SubtitlePaint(val text:String,val layout:TextLayoutResult,val top:Float,val baseline:Float,
    val height:Float,val alpha:Float,val blur:Boolean)

@Composable
internal fun ClassicSubtitles(content:SubtitleContent,lines:List<NormalizedLine>,playback:()->FoliaPlaybackSnapshot,
    typography:FoliaTypography,colors:FoliaColors,options:FoliaOptions,viewportWidth:Dp,enterUs:Long?,modifier:Modifier=Modifier,
    onLayout:(Float,List<SubtitlePaint>)->Unit={_,_->},additionalContent:@Composable ColumnScope.()->Unit={}) {
    val density=LocalDensity.current
    val fontResolver=LocalFontFamilyResolver.current
    val currentPlayback by rememberUpdatedState(playback)
    fun frame()=sampleSubtitle((currentPlayback().positionMillis-options.offsetMillis)*1000,enterUs,content.rows.isNotEmpty(),options.reducedMotion)
    Column(modifier.fillMaxWidth().padding(horizontal=16.dp).graphicsLayer {
        val f=frame();alpha=f.outerAlpha;translationY=f.outerY*density.density
    },horizontalAlignment=Alignment.CenterHorizontally) {
        BoxWithConstraints(Modifier.widthIn(max=896.dp).fillMaxWidth().graphicsLayer {
            val f=frame();alpha=f.innerAlpha;translationY=f.innerY*density.density
        }) {
            val measurer=rememberTextMeasurer()
            val explicitSize=typography.subtitle.fontSize.takeIf {it.isSp}?.value?:18f
            val paints=mutableListOf<SubtitlePaint>()
            var total=0f
            fun prepare(value:String,fontSize:Float,weight:FontWeight,upcoming:Boolean,alpha:Float,gap:Float) {
                val style=typography.subtitle.copy(fontSize=(floor(fontSize*100+.00001f)/100).sp,fontWeight=weight,
                    color=colors.secondary,lineHeight=TextUnit.Unspecified)
                val text=AnnotatedString(value)
                // Relative CSS line-height uses Blink's nearest 1/64px used font
                // size first (15.84px -> 15.84375px), then floors the product.
                val usedFont=floor(fontSize*density.fontScale*64+.5f)/64
                val lineHeight=floor(usedFont*1.5f*64)/64*density.density
                val natural=naturalFontMetrics(style,fontResolver,density)
                val layout=measurer.measure(text,style.copy(
                    lineHeight=(lineHeight/density.density/density.fontScale).sp,textAlign=TextAlign.Center,
                    lineHeightStyle=LineHeightStyle(LineHeightStyle.Alignment.Center,LineHeightStyle.Trim.None,LineHeightStyle.Mode.Tight)),
                    maxLines=if(upcoming) 1 else Int.MAX_VALUE,overflow=if(upcoming) TextOverflow.Ellipsis else TextOverflow.Clip,
                    softWrap=!upcoming,constraints=Constraints(maxWidth=with(density){minOf(maxWidth,if(upcoming) 672.dp else 896.dp).toPx()}.toInt().coerceAtLeast(1)))
                total+=floor(gap*density.fontScale*64)/64*density.density
                val baseline=cssFontBaseline(natural.ascent/density.density,natural.descent/density.density,lineHeight/density.density)*density.density
                paints+=SubtitlePaint(value,layout,total,baseline,lineHeight*layout.lineCount,alpha,upcoming)
                total+=lineHeight*layout.lineCount
            }
            content.rows.forEachIndexed {index,row->
                val secondary=index>0
                val size=(viewportWidth.value*.026f).coerceIn(18f,20f)*typography.scale*explicitSize/18*(if(secondary) .88f else 1f)
                prepare(row.text,size,typography.subtitle.fontWeight?:if(secondary) FontWeight.Normal else FontWeight.Medium,
                    row.source==SubtitleSource.Next,if(secondary) .82f else 1f,if(secondary) size*.25f else 0f)
            }
            content.upcoming.forEachIndexed {index,line->
                val size=(viewportWidth.value*.02f).coerceIn(14f,16f)*typography.scale*explicitSize/18
                prepare(lines[line].text,size,typography.subtitle.fontWeight?:FontWeight.Normal,true,1f,if(index>0) 8f/density.fontScale else 0f)
            }
            if(paints.lastOrNull()?.blur==true) {
                // A truncated last row has no propagated inline baseline. The parent
                // 16px/1.5 strut contributes its descent below the inline-block edge.
                val strut=naturalFontMetrics(typography.subtitle.copy(fontSize=16.sp),fontResolver,density)
                val strutHeight=24*density.fontScale
                total+=(strutHeight-cssFontBaseline(strut.ascent/density.density,strut.descent/density.density,strutHeight))*density.density
            }
            val layers=paints.map {rememberClassicLayer()}
            val observer by rememberUpdatedState(onLayout)
            SideEffect {observer(total,paints)}
            Canvas(Modifier.fillMaxWidth().height(with(density){ceil(total).toDp()}).semantics {
                this[SemanticsProperties.Text]=paints.map {AnnotatedString(it.text)}
            }) {
                val origin=size.height-total
                paints.forEachIndexed {i,row->
                    val layer=layers[i];val padding=8
                    layer.alpha=row.alpha
                    layer.blurSigma=if(row.blur) density.density else 0f
                    layer.record(this,size=IntSize(row.layout.size.width+padding*2,ceil(row.height).toInt()+padding*2)) {
                        // Keep the entire shaped paragraph. Reposition only its line
                        // coverage: SkParagraph rounds blob baselines/line heights,
                        // while CSS preserves fractional line-height across wraps.
                        repeat(row.layout.lineCount) { line ->
                            val target=row.baseline+line*row.height/row.layout.lineCount
                            withTransform({translate(0f,target-paintedLineBaseline(row.layout,line))}) {
                                clipRect(-padding.toFloat(),row.layout.getLineTop(line)+padding,
                                    row.layout.size.width+padding*2f,row.layout.getLineBottom(line)+padding) {
                                    drawText(row.layout,topLeft=Offset(padding.toFloat(),padding.toFloat()))
                                }
                            }
                        }
                    }
                    withTransform({translate((size.width-row.layout.size.width)/2-padding,origin+row.top-padding)}) {drawClassicLayer(layer)}
                }
            }
        }
        additionalContent()
    }
}
