# Expose the test consumer's worker handle for deterministic JNI fault injection.
-keepclassmembers class top.ghhccghk.accompanist.lyrics.folia.nativeandroid.FoliaNativeSurfaceView {
    private long handle;
}
