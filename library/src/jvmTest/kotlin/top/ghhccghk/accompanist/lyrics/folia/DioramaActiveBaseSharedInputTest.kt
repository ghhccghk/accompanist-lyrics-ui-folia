// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import org.jetbrains.skia.Image
import kotlin.test.*

/** Labelled source-input isolation, NOT own-core visual acceptance. Values come
 * from original active-base.json, textures from original passive layer export.
 * No original image/pose/placement is used by the actual library path. */
@OptIn(ExperimentalTestApi::class)
class DioramaActiveBaseSharedInputTest {
    @Test fun narrow()=capture(380)
    @Test fun wide()=capture(800)
    @Test fun fractional()=capture(801)
    private fun capture(width:Int)=runDesktopComposeUiTest(width=width,height=600) {
        val probes=File("../verification/diorama-fixtures/active-base.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.filter{it[2].toInt()==width}
        val inputs=File("../verification/diorama-fixtures/active-base-sampler-input-v1/source.tsv").readLines()
            .filterNot{it.startsWith('#')||it.isBlank()}.map{it.split('\t')}.groupBy{it[0]}
        val textures=mutableMapOf<String,ImageBitmap>()
        val samplers=mutableMapOf<String,SkiaDioramaPlainSampler>()
        var current by mutableIntStateOf(0)
        try {
            for(scene in listOf("latin","arabic","cjk"))for(i in 0 until when(scene){"latin"->3;"arabic"->2;else->7}) {
                val key="$scene-$i"
                val file=File("../build/verification/diorama-active-layers-browser/$scene-unit$i-base.png")
                val image=Image.makeFromEncoded(file.readBytes()).toComposeImageBitmap()
                textures[key]=image;samplers[key]=SkiaDioramaPlainSampler(image)
            }
            val path=buildDioramaPath(12,dioramaHashSeed("DioramaProbe"))
            val p=dioramaTextPlacement(3,dioramaHashSeed("DioramaProbe"))
            setContent {
                val r=probes[current]
                Canvas(Modifier.fillMaxSize().background(Color(0xff162435))) {
                    fun vec(i:Int)=DioramaVec(r[i].toDouble(),r[i+1].toDouble(),r[i+2].toDouble())
                    val view=DioramaPerspective(vec(9),vec(12),width.toDouble(),600.0)
                    val fit=dioramaFrameFit(r[6].toDouble(),5.2,view.fov,width/600.0)*p.scale
                    val x=dioramaPlaneAxis(path[3],p,1.0,0.0)*fit
                    val y=dioramaPlaneAxis(path[3],p,0.0,-1.0)*fit
                    val center=dioramaComposeLocal(path[3],p.offsetR,p.offsetU,0.0)
                    for(u in inputs.getValue(r[0])) {
                        val key="${r[1]}-${u[2]}";val image=textures.getValue(key)
                        val w=u[4].toDouble();val h=u[5].toDouble()
                        samplers.getValue(key).draw(this,view,center+x*(u[3].toDouble()-w/2)-y*(h/2),
                            x*(w/image.width),y*(h/image.height),
                            DioramaLinearColor(u[6].toDouble(),u[7].toDouble(),u[8].toDouble()),u[9].toFloat(),null,DioramaToneMapping.None)
                    }
                }
            }
            val out=File("../build/verification/diorama-active-base-shared-kotlin").apply{mkdirs()}
            for(i in probes.indices){runOnIdle{current=i};waitForIdle();ImageIO.write(captureToImage().toAwtImage(),"png",out.resolve(probes[i][0]))}
        }finally{samplers.values.forEach{it.close()};textures.values.forEach{it.asSkiaBitmap().close()}}
    }
}
