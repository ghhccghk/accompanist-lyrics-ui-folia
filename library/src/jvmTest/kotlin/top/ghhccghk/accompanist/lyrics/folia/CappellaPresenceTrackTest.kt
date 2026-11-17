package top.ghhccghk.accompanist.lyrics.folia

import top.ghhccghk.accompanist.lyrics.folia.internal.styles.cappella.*
import kotlin.test.*

class CappellaPresenceTrackTest {
    @Test fun interruptedExitAndRepeatedCancellationRemainContinuous() {
        for(intensity in CappellaIntensity.entries) {
            val events=listOf(0.0 to true,.15 to false,.19 to true,.21 to false,.27 to true)
            val config=cappellaTransition(intensity)
            for(i in events.indices.drop(1)) {
                val before=CappellaPresenceTrack(events.take(i),false,config).at(events[i].first)
                val after=CappellaPresenceTrack(events.take(i+1),false,config).at(events[i].first)
                assertEquals(before,after,"retarget has no transform/opacity discontinuity: $intensity/$i")
            }
            val track=CappellaPresenceTrack(events,false,config)
            assertEquals(CappellaPresenceFrame(1.0,1.0,0.0),track.at(2.0))
            val probes=(0..100).map{it/100.0};val states=probes.associateWith{track.at(it)}
            for(time in probes.reversed())assertEquals(states[time],track.at(time))
        }
    }
    @Test fun initialPresenceDoesNotFadeAndCompletedExitIsInvisible() {
        for(intensity in CappellaIntensity.entries) {
            val config=cappellaTransition(intensity)
            val track=CappellaPresenceTrack(listOf(0.0 to true,1.0 to false),true,config)
            assertEquals(CappellaPresenceFrame(1.0,1.0,0.0),track.at(.1))
            assertEquals(CappellaPresenceFrame(0.0,config.exitScale,config.exitY),track.at(2.0))
        }
    }
}
