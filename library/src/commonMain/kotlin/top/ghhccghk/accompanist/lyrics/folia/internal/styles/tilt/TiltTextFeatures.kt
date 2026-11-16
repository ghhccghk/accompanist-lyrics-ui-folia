package top.ghhccghk.accompanist.lyrics.folia.internal.styles.tilt

/** Source inline-block EGCs do not kern against each other. Keep whole-paragraph
 * shaping and required joining/ligatures, but disable optional pair kerning.
 * Compose/Skia and Android Paint accept different tag quoting here. */
internal expect val tiltKerningOff: String
