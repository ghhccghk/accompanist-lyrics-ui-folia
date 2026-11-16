package top.ghhccghk.accompanist.folia.nativeprobe

import top.ghhccghk.accompanist.folia.sample.playback.PreviewPlaybackController

/** Headless Native feasibility check. This is not the audio player or FoliaLyricsView. */
fun main() {
    var now = 0L
    val clock = PreviewPlaybackController(10_000) { now }
    fun advance(millis: Long) { now += millis * 1_000_000 }
    fun expect(label: String, position: Long, playing: Boolean, discontinuity: Long) {
        val actual = clock.refresh()
        check(actual.positionMillis == position && actual.isPlaying == playing &&
            actual.discontinuity == discontinuity) { "$label: $actual" }
        println("$label position=${actual.positionMillis} playing=${actual.isPlaying} discontinuity=${actual.discontinuity}")
    }
    expect("initial", 0, false, 0)
    clock.play()
    advance(1250)
    expect("playing", 1250, true, 0)
    clock.changeSpeed(2.0)
    advance(500)
    expect("double-speed", 2250, true, 0)
    clock.pause()
    advance(30_000)
    expect("paused", 2250, false, 0)
    clock.seek(9250)
    expect("paused-seek", 9250, false, 1)
    clock.looping = true
    clock.play()
    advance(6000)
    expect("multiple-loops", 1250, true, 3)
    clock.looping = false
    advance(5000)
    expect("end", 10_000, false, 3)
    clock.play()
    expect("replay", 0, true, 4)
    clock.pause()
    for (position in listOf(8000L, 0L, 9999L, 1L, 4800L)) {
        clock.seek(position)
        advance(1500)
        check(clock.refresh().positionMillis == position)
    }
    println("PASS: production preview clock and Compose runtime executed on Kotlin/Native; no UI/audio claimed.")
}
