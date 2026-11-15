package top.ghhccghk.accompanist.lyrics.folia.internal.rendering

// Verified against the pinned Windows Chromium across three fonts/inks/two surfaces.
// Other desktop backends keep their existing path pending their own visual evidence.
internal actual fun usesNativeBodyInk(): Boolean = System.getProperty("os.name").startsWith("Windows")
