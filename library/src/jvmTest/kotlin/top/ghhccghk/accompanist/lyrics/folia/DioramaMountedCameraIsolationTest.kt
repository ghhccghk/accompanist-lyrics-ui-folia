// SPDX-License-Identifier: AGPL-3.0-only
package top.ghhccghk.accompanist.lyrics.folia

import androidx.compose.runtime.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.synced.SyncedLine
import top.ghhccghk.accompanist.lyrics.folia.internal.styles.diorama.*
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.*
import kotlin.test.*

/** Diagnostic factor isolation, not a new oracle or camera candidate. Original
 * mounted refs seed ONLY labelled controls; production never uses source poses.
 * No original fixture, acceptance threshold or display scheduling is changed. */
@OptIn(ExperimentalTestApi::class)
class DioramaMountedCameraIsolationTest {
    @Test fun narrowSameOriginalIncomingAnchors() = isolate(380)
    @Test fun wideSameOriginalIncomingAnchors() = isolate(800)
    private fun isolate(width:Int) = runDesktopComposeUiTest(width=width,height=600) {
            val rows=File("../verification/diorama-fixtures/mounted-cut-v1/steps.tsv").readLines()
                .filterNot{it.isBlank()||it.startsWith('#')}.map{it.split('\t')}
            val times=setOf(0.0,.7,1.6,2.099,2.1,3.199,3.2,3.201,3.499,3.5,4.5,4.6)
            fun core(n:Int,label:String)=SyncedLyrics((0 until n).map{
                SyncedLine("Synthetic $label line $it",null,700+it*1400,1500+it*1400)
            })
            fun List<String>.vector(i:Int)=DioramaVec(this[i].toDouble(),this[i+1].toDouble(),this[i+2].toDouble())
            fun List<String>.quaternion(i:Int)=DioramaQuaternion(this[i].toDouble(),this[i+1].toDouble(),this[i+2].toDouble(),this[i+3].toDouble())
            fun List<String>.state()=DioramaFollowState(vector(12),vector(15),vector(18),vector(21),this[24].toDouble(),quaternion(25),quaternion(29))
            val out=File("../build/verification/diorama-camera-isolation").apply{mkdirs()}
            val report=mutableListOf("# width fps time variant positionMax lookMax orientationRadians alignmentError projectionMaxPx")
            val factors=mutableListOf("# width fps previousWidth incomingWidth anchorPositionMax anchorOrientationRadians")
            val materialRows=File("../verification/diorama-fixtures/mounted-material-v1/materials.tsv").readLines()
                .filterNot{it.isBlank()||it.startsWith('#')}.map{it.split('\t')}
                .groupBy{"${it[0]}-${it[1]}-${it[2]}"}
            fun linear(hex:Int):DioramaLinearColor {
                fun channel(shift:Int)=((hex shr shift and 255)/255.0).let{if(it<=.04045)it/12.92 else ((it+.055)/1.055).pow(2.4)}
                return DioramaLinearColor(channel(16),channel(8),channel(0))
            }
            val primary=linear(0xdfe5ec);val secondary=linear(0x758daa);val accent=linear(0xf17b4d)
            val seed=dioramaHashSeed("Incoming mirror")
            var cases=0;var finitePeak=0.0
            var crossDraw by mutableStateOf<(DrawScope.()->Unit)?>(null)
            fun vectorError(a:DioramaVec,b:DioramaVec)=(a-b).let{maxOf(abs(it.x),abs(it.y),abs(it.z))}
            fun angle(a:DioramaQuaternion,b:DioramaQuaternion)=2*acos(abs(a.normalized().dot(b.normalized())).coerceIn(0.0,1.0))
            fun assertSource(expected:DioramaFollowState,actual:DioramaFollowState) {
                val errors=listOf(vectorError(expected.position,actual.position),vectorError(expected.velocity,actual.velocity),
                    vectorError(expected.look,actual.look),vectorError(expected.lookVelocity,actual.lookVelocity),abs(expected.alignment-actual.alignment))+
                    listOf(expected.orientation!! to actual.orientation!!,expected.alignmentOrientation!! to actual.alignmentOrientation!!).map{(a,b)->
                        val sign=if(a.dot(b)<0)-1 else 1
                        maxOf(abs(a.x-b.x*sign),abs(a.y-b.y*sign),abs(a.z-b.z*sign),abs(a.w-b.w*sign))
                    }
                finitePeak=max(finitePeak,errors.max())
                assertTrue(errors.all{it<=1e-9},"Actual original source callback control $errors")
            }
            var prepared:Map<Int,Pair<DioramaTextPlayback,DioramaTextPlayback>>?=null
            setContent {
                val resolver=LocalFontFamilyResolver.current
                val values=remember {
                    val style=TextStyle(fontFamily=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold)))
                    val measurer=TextMeasurer(resolver,Density(1f),LayoutDirection.Ltr)
                    listOf(width).associateWith{width->
                        val old=DioramaTextPlayback.prepare(core(8,"outgoing"),style,measurer,resolver,width.toDouble(),600.0,
                            seedName="Outgoing mirror",endTime=12.0)
                        old to DioramaTextPlayback.prepare(core(3,"incoming"),style,measurer,resolver,width.toDouble(),600.0,
                            seedName="Incoming mirror",endTime=5.0,cut=old.cutAt(6.8,1))
                    }
                }
                SideEffect{prepared=values}
                Canvas(Modifier.fillMaxSize().background(Color(0xff162435))){crossDraw?.invoke(this)}
            }
            waitForIdle()
            for(fps in listOf(60,144)) {
                val (old,playback)=prepared!!.getValue(width)
                val group=rows.filter{it[1].toInt()==width&&it[2].toInt()==fps}
                val incoming=group.filter{it[0]=="incoming"}
                val originalAnchor=group.last{it[0]=="outgoing"}.state()
                val mediaAnchor=old.camera.at(6.8)
                val subjects=playback.camera.subjects
                // Short post-flight control starts from the OBSERVED source
                // 3.2s state. It cannot inherit errors from the prior flight or
                // outgoing song. Same operator, adaptive error bound, no UI Hz.
                fun localFromOriginal(time:Double):DioramaFollowState {
                    var state=incoming.single{it[3].toDouble()==3.2}.state()
                    var t=3.2;var h=.1
                    fun step(previous:DioramaFollowState,dt:Double,at:Double):DioramaFollowState {
                        val s=subjects[playback.camera.subjectIndex(at)]
                        val target=s.line.target(at,s.frame,s.placement,seed,s.localIndex,s.shot,s.trackWidth,width/600.0)
                        return dioramaFollowStep(previous,target,s.frame,s.placement,s.shot,at,s.line.line.start,s.line.renderEnd,dt,.42,width/600.0).state
                    }
                    fun error(a:DioramaFollowState,b:DioramaFollowState):Double {
                        fun quaternion(q:DioramaQuaternion,r:DioramaQuaternion):Double {
                            val sign=if(q.dot(r)<0)-1 else 1
                            return maxOf(abs(q.x-r.x*sign),abs(q.y-r.y*sign),abs(q.z-r.z*sign),abs(q.w-r.w*sign))
                        }
                        return maxOf(vectorError(a.position,b.position),vectorError(a.velocity,b.velocity),vectorError(a.look,b.look),
                            vectorError(a.lookVelocity,b.lookVelocity),abs(a.alignment-b.alignment),
                            quaternion(a.orientation!!,b.orientation!!),quaternion(a.alignmentOrientation!!,b.alignmentOrientation!!))
                    }
                    require(time in 3.2..<3.5)
                    while(t<time) {
                        val dt=min(h,time-t)
                        val coarse=step(state,dt,t+dt)
                        val fine=step(step(state,dt/2,t+dt/2),dt/2,t+dt)
                        val e=error(coarse,fine)
                        if(e>2e-5){check(dt>1e-9);h=dt/2;continue}
                        state=fine;t=if(dt==time-t)time else t+dt
                        h=if(e<2e-5/4)min(.1,dt*2)else dt
                    }
                    return state
                }
                val previousWidth=old.camera.subjects[old.camera.subjectIndex(6.8)].trackWidth
                assertEquals(previousWidth,incoming.first()[10].toDouble(),1e-9,"Original prior outgoing width")
                val anchoredMedia=DioramaCameraTrack(subjects,width.toDouble(),600.0,5.0,seed,initialState=originalAnchor,flightEpoch=1)
                factors+=listOf(width,fps,previousWidth,subjects[0].trackWidth,vectorError(originalAnchor.position,mediaAnchor.position),
                    angle(originalAnchor.orientation!!,mediaAnchor.orientation!!)).joinToString("\t")
                val states=mutableMapOf("finiteOriginalAnchor" to originalAnchor,"finiteMediaAnchor" to mediaAnchor,
                    "finiteCurrentWidth" to originalAnchor,"finiteMediaAnchorCurrentWidth" to mediaAnchor)
                val flights=states.mapValues{(variant,state)->
                    val s=subjects[0]
                    val target=s.line.target(0.0,s.frame,s.placement,seed,0,s.shot,
                        if(variant.endsWith("CurrentWidth")||variant=="finiteCurrentWidth")s.trackWidth else previousWidth,width/600.0)
                    DioramaCameraFlight(state.position,target.eye,seed,1)
                }
                var beforeWidth=previousWidth
                for(r in incoming) {
                    val time=r[3].toDouble();val dt=r[5].toDouble()
                    val s=subjects[playback.camera.subjectIndex(time)]
                    assertEquals(r[9],s.shot.name)
                    assertEquals(r[10].toDouble(),beforeWidth,1e-9)
                    for(variant in states.keys.toList()) {
                        val widthNow=if(variant.endsWith("CurrentWidth")||variant=="finiteCurrentWidth")s.trackWidth else beforeWidth
                        val target=s.line.target(time,s.frame,s.placement,seed,s.localIndex,s.shot,widthNow,width/600.0)
                        states[variant]=dioramaFollowStep(states.getValue(variant),target,s.frame,s.placement,s.shot,time,
                            s.line.line.start,s.line.renderEnd,dt,.42,width/600.0,flight=flights.getValue(variant).at(time,.42)).state
                    }
                    beforeWidth=s.trackWidth
                    assertEquals(r[11].toDouble(),beforeWidth,1e-9)
                    val original=r.state()
                    assertSource(original,states.getValue("finiteOriginalAnchor"))
                    if(time !in times)continue
                    val observedView=DioramaPerspective(r.vector(33),original.look,width.toDouble(),600.0,orientation=r.quaternion(36))
                    val center=dioramaComposeLocal(s.frame,s.placement.offsetR,s.placement.offsetU,0.0)
                    val localAxis=dioramaPlaneAxis(s.frame,s.placement,1.0,0.0)*(s.trackWidth/2)
                    val points=listOf(center-localAxis,center,center+localAxis)
                    val variants=states+mapOf("media" to playback.camera.at(time),"originalAnchorMedia" to anchoredMedia.at(time))+
                        if(time==3.201||time==3.499)mapOf("localOriginalAt3.2" to localFromOriginal(time))else emptyMap()
                    for((variant,state)in variants) {
                        val view=when(variant){
                            "media"->playback.camera.perspectiveAt(time)
                            "originalAnchorMedia"->anchoredMedia.perspectiveAt(time)
                            "localOriginalAt3.2"->DioramaPerspective(state.position,state.look,width.toDouble(),600.0,orientation=state.orientation)
                            else->{val visual=flights.getValue(variant).at(time,.42)
                                DioramaPerspective(state.position+visual.positionOffset,state.look,width.toDouble(),600.0,
                                    orientation=state.orientation!!*DioramaQuaternion.axis(DioramaVec(0.0,0.0,1.0),visual.bank))}
                        }
                        val projection=points.maxOf{world->
                            val a=observedView.project(world);val b=view.project(world)
                            hypot(a.x-b.x,a.y-b.y)
                        }
                        report+=listOf(width,fps,time,variant,vectorError(original.position,state.position),vectorError(original.look,state.look),
                            angle(original.orientation!!,state.orientation!!),abs(original.alignment-state.alignment),projection).joinToString("\t")
                    }
                    if(time==3.499||time==3.201) {
                        val text=playback.lines[s.localIndex].text
                        val expectedMaterials=materialRows.getValue("$width-$fps-$time").sortedBy{it[3].toInt()}
                        val delta=dioramaComposeLocal(s.frame,s.placement.offsetR,s.placement.offsetU,0.0)-observedView.eye
                        val sourceMaterials=text.slices.mapIndexed{i,slice->
                            val row=expectedMaterials[i]
                            val material=dioramaUnitFrame(time,slice.unit.start,slice.unit.end,DioramaEnvelope(row[4].toDouble(),row[5].toDouble()),
                                primary,accent,life=dioramaTextLife(sqrt(delta.dot(delta))),breath=dioramaBreathAt(6.8+time))
                            val scalars=listOf(material.envelope.light,material.envelope.soul,material.fill.red,material.fill.green,
                                material.fill.blue,material.baseOpacity,material.glowOpacity,material.soulOpacity)
                            scalars.forEachIndexed{j,value->assertEquals(row[j+4].toDouble(),value,1e-9,"Original material $width/$fps/$time/$i/$j")}
                            material
                        }
                        val plain=DioramaPlainRenderer();val base=DioramaActiveBaseRenderer()
                        try {
                            playback.preparePlainRenderer(plain,time);playback.prepareActiveBaseRenderer(base,time)
                            for(mode in listOf("sourcePoseSourceMaterial","mediaPoseSourceMaterial","sourcePoseMediaMaterial","mediaPoseMediaMaterial")) {
                                val view=if(mode.startsWith("sourcePose"))observedView else playback.camera.perspectiveAt(time)
                                val material=if(mode.endsWith("SourceMaterial"))sourceMaterials else null
                                runOnIdle{crossDraw={
                                        playback.drawStage(this,time,view,primary,accent,secondary,fog=DioramaFog(Color(0xff162435)),
                                            activeFrames=material,plainRenderer=plain,activeBaseRenderer=base,transparentTextSurface=true)
                                }}
                                fun render():ImageBitmap {waitForIdle();return captureToImage()}
                                val image=render();val pixels=IntArray(width*600).also{image.readPixels(it)}
                                val again=render();val repeated=IntArray(pixels.size).also{again.readPixels(it)}
                                assertContentEquals(pixels,repeated,"Camera/material cross freezes on the same media time")
                                val name="mounted-$width-$fps-${(time*1000).roundToInt()}.png"
                                ImageIO.write(image.toAwtImage(),"png",out.resolve("$mode-$name"))
                                if(mode=="mediaPoseMediaMaterial") {
                                    val previous=ImageIO.read(File("../build/verification/diorama-text-surface-kotlin/media-$name"))
                                    val frozen=previous.getRGB(0,0,width,600,null,0,width)
                                    assertTrue(frozen.contentEquals(pixels),
                                        "Diagnostic must reproduce frozen previous media before attribution; differingPixels=${pixels.indices.count{pixels[it]!=frozen[it]}}")
                                }
                            }
                        }finally{runOnIdle{crossDraw=null};waitForIdle();plain.close();base.close()}
                    }
                    cases++
                }
                val fixed=listOf(0.0,.7,2.1,3.199,3.2,3.201,3.499,3.5,4.6).associateWith{playback.camera.at(it)}
                for(t in fixed.keys.reversed())assertEquals(fixed[t],playback.camera.at(t),"Diagnostic must not mutate production track")
            }
            assertEquals(24,cases)
            out.resolve("camera-$width.tsv").writeText(report.joinToString("\n")+"\n")
            out.resolve("factors-$width.tsv").writeText(factors.joinToString("\n")+"\n")
            out.resolve("control-$width.txt").writeText("captures=$cases originalFinitePeak=$finitePeak\nSource anchors are diagnostic inputs only; no production change, image gate or visual acceptance.\n")
        }
}
