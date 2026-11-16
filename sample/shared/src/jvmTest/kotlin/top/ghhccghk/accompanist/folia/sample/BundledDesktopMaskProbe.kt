package top.ghhccghk.accompanist.folia.sample

/** Fresh JVM per invocation; real DLL from the packaged sample JAR, no UI. */
internal object BundledDesktopMaskProbe {
    @JvmStatic fun main(args: Array<String>) {
        check(System.getProperty("java.library.path").isEmpty()) { "Probe needs empty java.library.path" }
        check(System.getProperty("folia.mask.library") == null) { "Probe must not use an external DLL override" }
        val platform = checkNotNull(desktopMaskPlatform(System.getProperty("os.name"),System.getProperty("os.arch")))
        val resource = javaClass.getResource(platform.resourcePath)
        check(resource != null && resource.protocol == "jar") { "Expected DLL inside packaged JAR, got $resource" }
        val session = SampleMaskBackend()
        check(!session.requested && session.processor == null)
        repeat(3) {
            session.selectRust(true)
            check(session.loadError == null) { session.loadError!! }
            val processor = checkNotNull(session.processor)
            check(processor.stats().calls == 0L)
            val result = processor.blur(intArrayOf(-1), 1, 1, floatArrayOf(1f))
            check(result?.single()?.single() == 0x02ffffff) { "Actual JNI convolution failed" }
            check(processor.stats().calls == 1L && processor.stats().error == null)
            session.selectRust(false)
            check(session.processor == null && !session.requested)
        }
        val colour = SampleColourBackend()
        check(!colour.requested && colour.processor == null)
        repeat(3) {
            colour.selectRust(true)
            check(colour.loadError == null) { colour.loadError!! }
            val processor = checkNotNull(colour.processor)
            check(processor.stats().calls == 0L)
            check(processor.convolve(intArrayOf(0x01ff0000),1,1,doubleArrayOf(0.0,1.0,0.0))
                ?.contentEquals(intArrayOf(0x01ff0000)) == true)
            check(processor.stats().calls == 1L && processor.stats().error == null)
            colour.selectRust(false)
            check(colour.processor == null && !colour.requested)
        }
        println("PASS: packaged JAR native library + empty java.library.path + real ABI/startup/convolution + three toggle/release cycles.")
        println("PASS: separate colour ABI/startup/convolution + three toggle/release cycles; mask selection unchanged.")
        println("Runtime: ${System.getProperty("java.version")}; ${System.getProperty("java.vendor")}; ${System.getProperty("os.arch")}")
        println("No full UI or performance claim.")
    }
}
