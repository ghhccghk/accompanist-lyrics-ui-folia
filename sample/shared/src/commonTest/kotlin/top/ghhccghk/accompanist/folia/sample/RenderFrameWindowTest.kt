package top.ghhccghk.accompanist.folia.sample

import kotlin.test.*

class RenderFrameWindowTest {
    @Test fun actualCallbacksInsteadOfRefreshTicksAndReset() {
        val window=RenderFrameWindow()
        for(i in 0..60)window.record(1_000_000_000+i*16_666_667L,5_000_000)
        val stats=window.read(2_000_000_020)
        assertEquals(61,stats.frames);assertEquals(60.0,stats.fps,1e-5)
        assertEquals(5.0,stats.meanMillis);assertEquals(5.0,stats.p95Millis)
        repeat(20){assertEquals(stats,window.read(2_000_000_020))}
        assertEquals(RenderFrameStats(),window.read(5_000_000_000))
        window.reset();assertEquals(RenderFrameStats(),window.read(5_000_000_000))
    }
    @Test fun capacityInvalidOrderingAndSlowFramePercentiles() {
        val window=RenderFrameWindow(capacity=20)
        for(i in 0..25)window.record(i*20_000_000L,if(i==25)100_000_000 else 2_000_000,if(i==25)3 else 0)
        window.record(20_000_000,1);window.record(500_000_000,1);window.record(600_000_000,-1)
        val stats=window.read(500_000_000)
        assertEquals(20,stats.frames);assertEquals(50.0,stats.fps);assertEquals(100.0,stats.maxMillis)
        assertEquals(2.0,stats.p95Millis);assertEquals(20.0,stats.intervalP95Millis);assertEquals(3,stats.lostReports)
        assertEquals(0.0,window.read(1_600_000_000).fps)
    }
}
