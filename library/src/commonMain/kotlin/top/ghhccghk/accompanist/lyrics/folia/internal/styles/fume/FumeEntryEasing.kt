// Copyright (c) 2024 Motion B.V.
// Original BezierEasing: Copyright (c) 2014 Gaëtan Renaudeau.
// SPDX-License-Identifier: MIT
// Adapted from locked motion-utils13.3.0 easing/cubic-bezier.mjs and ease.mjs.
// That implementation credits gre/bezier-easing; both MIT notices retained in
// LICENSES/MIT-motion-utils-13.3.0.txt and MIT-bezier-easing.txt.
// Specialised to easeOut; no lookup table/Newton iteration, same12-step budget.
package top.ghhccghk.accompanist.lyrics.folia.internal.styles.fume

import kotlin.math.abs

internal fun fumeEntryEaseOut(time:Double):Double{
    if(time==0.0||time==1.0)return time
    fun cubic(t:Double,a:Double,b:Double)=(((1-3*b+3*a)*t+(3*b-6*a))*t+3*a)*t
    var lo=0.0;var hi=1.0;var u=0.0
    for(i in 0 until 12){
        u=lo+(hi-lo)/2
        val error=cubic(u,0.0,.58)-time
        if(error>0)hi=u else lo=u
        if(abs(error)<=.0000001)break
    }
    return cubic(u,0.0,1.0)
}
