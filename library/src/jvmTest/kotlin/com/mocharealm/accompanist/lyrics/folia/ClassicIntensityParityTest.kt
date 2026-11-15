package com.mocharealm.accompanist.lyrics.folia

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.*
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.mocharealm.accompanist.lyrics.core.model.SyncedLyrics
import com.mocharealm.accompanist.lyrics.core.model.karaoke.*
import com.mocharealm.accompanist.lyrics.folia.internal.animation.*
import com.mocharealm.accompanist.lyrics.folia.internal.model.normalize
import com.mocharealm.accompanist.lyrics.folia.internal.styles.classic.*
import com.mocharealm.accompanist.lyrics.folia.internal.text.displayUnits
import com.mocharealm.accompanist.lyrics.folia.internal.timeline.VisualTimeline
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.roundToLong
import kotlin.test.*

/** Expected configs, geometry and animations are observations of the pinned original. */
@OptIn(ExperimentalTestApi::class)
class ClassicIntensityParityTest {
    private fun rows(name: String) = File("../verification/classic-fixtures/$name.tsv").readLines()
        .filterNot { it.startsWith("#") || it.isBlank() }.map { it.split('\t') }
    private fun intensity(name: String) = FoliaAnimationIntensity.entries.single { it.name.lowercase() == name }
    private fun lyrics(scene: String): SyncedLyrics {
        val syllables = if(scene == "baseline") listOf(KaraokeSyllable("Amber ",700,1300),KaraokeSyllable("echo",1300,1900))
        else rows("scenes").filter {it[0] == scene}.map {KaraokeSyllable(it[5],it[3].toInt(),it[4].toInt())}
        return SyncedLyrics(listOf(KaraokeLine.MainKaraokeLine(syllables,null,KaraokeAlignment.Unspecified,
            700,if(scene == "baseline") 1900 else 2800)))
    }
    @Test fun originalConfigsAndMountedMotion() {
        for(c in rows("intensity-geometry")) {
            val pose=wordPose(700_000,c[3].toInt(),true,intensity=intensity(c[0]))
            listOf(pose.x,pose.y,pose.scale,pose.rotation,pose.passedRotation).forEachIndexed {i,v->
                assertEquals(c[i+4].toDouble(),v.toDouble(),.0001,c.toString())
            }
        }
        for(c in rows("intensity-motion")) {
            val line=normalize(lyrics(c[1])).lines.single()
            val word=displayUnits(line)[c[4].toInt()]
            val profile=renderProfile(line.startUs,line.endUs,line.fragments.last().endUs)
            val time=c[3].toLong()*1000
            val level=intensity(c[0])
            val frame=sampleWord(time,line.startUs,word.startUs,word.endUs,profile,
                wordPose(line.startUs,c[4].toInt(),true,intensity=level),false,level)
            listOf(frame.x,frame.y,frame.scale,frame.rotation,frame.alpha).forEachIndexed {i,v->
                assertEquals(c[i+5].toDouble(),v.toDouble(),.0001,c.toString())
            }
            val body=sampleLine(time,line.startUs,profile.renderEndUs.takeIf {time>=it},profile,false,0f,1f,level)
            listOf(body.scale,body.alpha,body.blur).forEachIndexed {i,v->
                assertEquals(c[i+10].toDouble(),v.toDouble(),.0001,c.toString())
            }
        }
    }
    @Test fun originalBreathingAcrossTwoPeriods() {
        for(c in rows("intensity-breathing")) {
            val time=((c[4].toDouble()-c[5].toDouble()).coerceAtLeast(0.0)*1000).roundToLong()
            val frame=sampleLine(time,-1_000_000,null,RenderProfile(Reveal.Normal,0,0),false,1f,1f,intensity(c[0]))
            assertEquals(c[2].toDouble(),frame.breathingY.toDouble(),.0001,c.toString())
            assertEquals(c[3].toDouble(),frame.scale.toDouble(),.000005,c.toString())
        }
    }
    @Test fun calmWide() = capture("baseline",800,FoliaAnimationIntensity.Calm)
    @Test fun calmWrap() = capture("wrap",380,FoliaAnimationIntensity.Calm)
    @Test fun normalWide() = capture("baseline",800,FoliaAnimationIntensity.Normal)
    @Test fun normalWrap() = capture("wrap",380,FoliaAnimationIntensity.Normal)
    @Test fun chaoticWide() = capture("baseline",800,FoliaAnimationIntensity.Chaotic)
    @Test fun chaoticWrap() = capture("wrap",380,FoliaAnimationIntensity.Chaotic)

    private fun capture(scene:String,width:Int,level:FoliaAnimationIntensity) = runDesktopComposeUiTest(width=width,height=600) {
        val source=lyrics(scene)
        val document=normalize(source);val timeline=VisualTimeline(document.lines)
        val family=FontFamily(Font(File("../verification/assets/FoliaParityLatin-Bold.ttf"),FontWeight.Bold))
        val style=TextStyle(fontFamily=family,fontWeight=FontWeight.Bold,fontSize=(width*.06f).coerceIn(36f,72f).sp)
        var snapshot by mutableStateOf(FoliaPlaybackSnapshot(750,false))
        var avoid by mutableStateOf(false);var public by mutableStateOf(false)
        var geometry:ClassicGeometry?=null
        val options=FoliaOptions(showTranslation=false,showPhonetic=false,showNextLine=false,animationIntensity=level)
        val colors=FoliaColors(secondary=Color.Transparent) // Waiting label is excluded in the original isolation too.
        setContent {
            val tuning=ClassicTuning(breathingFloatMultiplier=0f,avoidOverlap=avoid)
            if(public) FoliaLyricsView(source,{snapshot},Modifier.fillMaxSize().background(Color(0xFF162435)),
                typography=FoliaTypography(secondary=style),colors=colors,options=options,style=FoliaStyle.Classic(tuning))
            else Box(Modifier.fillMaxSize().background(Color(0xFF162435)).padding(horizontal=32.dp),contentAlignment=Alignment.Center) {
                val selection=timeline.at(snapshot.positionMillis*1000)
                (selection.exiting+listOfNotNull(selection.primary)).forEach {epoch->key(epoch) {
                    ClassicLine(document.lines[epoch.line],{snapshot},style,colors,options,tuning,356.dp,
                        enterUs=epoch.enterUs,leaveUs=epoch.leaveUs,onLayout={geometry=it})
                }}
            }
        }
        val out=File("../build/verification/classic-intensity-kotlin").apply{mkdirs()}
        val times=listOf(750L,1000L,1500L,1900L,2100L,2300L,2500L,2800L,3000L)
        for(mode in listOf(false,true)) {
            val seen=mutableMapOf<Long,IntArray>()
            for(ms in times+times.reversed()) {
                runOnIdle{avoid=mode;snapshot=FoliaPlaybackSnapshot(ms,false,snapshot.discontinuity+1)};waitForIdle()
                val image=captureToImage();val pixels=IntArray(width*600).also{image.readPixels(it)}
                mainClock.advanceTimeBy(1000);waitForIdle()
                assertContentEquals(pixels,IntArray(pixels.size).also{captureToImage().readPixels(it)},"Paused $level/$scene/$mode/$ms")
                seen[ms]?.let {assertContentEquals(it,pixels,"Reverse seek $level/$scene/$mode/$ms")}
                seen[ms]=pixels
                ImageIO.write(image.toAwtImage(),"png",out.resolve("${level.name.lowercase()}-$scene-${if(mode) "avoid" else "default"}-${ms}ms.png"))
                if(ms==1500L) {
                    for(c in rows("intensity-geometry").filter {it[0]==level.name.lowercase() && it[1]==scene && (it[2]=="avoid")==mode}) {
                        val g=geometry!!;val i=c[3].toInt();val box=g.boxes[i]
                        assertEquals(c[9].toDouble(),g.margins[i],.0001,c.toString())
                        listOf(box.left,box.top,box.width,box.height,g.height).forEachIndexed {j,v->assertEquals(c[j+10].toDouble(),v.toDouble(),.51,c.toString())}
                    }
                    runOnIdle{public=true};waitForIdle()
                    assertContentEquals(pixels,IntArray(pixels.size).also{captureToImage().readPixels(it)},"Public/core intensity $level/$scene/$mode")
                    runOnIdle{public=false};waitForIdle()
                }
            }
        }
    }
}
