package top.ghhccghk.accompanist.lyrics.folia.internal.styles.pendolo

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

/** Inverse of the actual draw chain, in logical pixels. Do not use the rotated
 * axis-aligned bounding box: its empty corners otherwise intercept input. */
internal fun pendoloLocalPoint(row:PendoloStageRow, point:Offset, rotation:Double,
    hub:Offset, chorusScale:Double):Offset {
    val w=-rotation*PI/180
    val dx=point.x-hub.x;val dy=point.y-hub.y
    val x=hub.x+dx*cos(w)-dy*sin(w)-floor(row.item.x*64)/64
    val y=hub.y+dx*sin(w)+dy*cos(w)-(floor(row.item.y*64)/64+row.blockHeight/2)
    val a=-(row.item.angleDeg*.35-rotation*.65)*PI/180
    val scale=row.item.scale*chorusScale
    return Offset(((x*cos(a)-y*sin(a))/scale).toFloat(),((x*sin(a)+y*cos(a))/scale+row.blockHeight).toFloat())
}

internal fun pendoloHitTest(rows:List<PendoloStageRow>,point:Offset,rotation:Double,hub:Offset,
    chorusScale:(Int)->Double):Int? = rows.asReversed().firstOrNull { row ->
    val hasText=row.main.rows.any{it.width>0 && it.text.isNotBlank()} || row.subtitles.any{sub->sub.text.rows.any{it.width>0 && it.text.isNotBlank()}}
    if(!hasText || pendoloRotatingOpacity(row.item.angleDeg,rotation,row.item.alpha)<=0 || row.width<=0 || row.blockHeight<=0)false
    else {
        val p=pendoloLocalPoint(row,point,rotation,hub,chorusScale(row.item.index))
        p.x>=0 && p.x<row.width && p.y>=0 && p.y<row.blockHeight
    }
}?.item?.index
