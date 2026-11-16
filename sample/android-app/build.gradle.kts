plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
// Sample packages native by default; library publications remain Kotlin-only.
abstract class BuildExperimentalMask : DefaultTask() {
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val nativeInputs: ConfigurableFileCollection
    @get:Input abstract val cargo: Property<String>
    @get:Internal abstract val ndkDirectory: DirectoryProperty
    @get:Internal abstract val script: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @get:Inject abstract val processes: ExecOperations
    @TaskAction fun build() {
        processes.exec {
            if (System.getProperty("os.name").startsWith("Windows")) {
                val shell = File(System.getenv("SystemRoot"), "System32/WindowsPowerShell/v1.0/powershell.exe")
                commandLine(shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.get().asFile,
                    "-NdkDirectory", ndkDirectory.get().asFile, "-OutputDirectory", outputDirectory.get().asFile,
                    "-Cargo", cargo.get())
            } else {
                commandLine("sh", File(script.get().asFile.parentFile,"build-android.sh"),
                    ndkDirectory.get().asFile,outputDirectory.get().asFile,cargo.get())
            }
        }
    }
}
if (providers.gradleProperty("foliaExperimentalMask").map { it == "true" }.orElse(true).get()) {
    val mask = tasks.register<BuildExperimentalMask>("buildExperimentalMask") {
        val directory = rootProject.layout.projectDirectory.dir("native/experimental-mask")
        nativeInputs.from(fileTree(directory) { include("src/**", "Cargo.toml", "Cargo.lock", "rust-toolchain.toml", "build-android.ps1", "rust-environment.ps1", "build-android.sh", "rust-environment.sh") })
        script.set(directory.file("build-android.ps1"))
        cargo.set(providers.gradleProperty("foliaRustCargo").orElse("cargo"))
        ndkDirectory.set(androidComponents.sdkComponents.sdkDirectory.map { it.dir("ndk/28.2.13676358") })
        outputDirectory.set(layout.buildDirectory.dir("generated/experimentalMask/jniLibs"))
    }
    androidComponents.onVariants { variant ->
        variant.sources.jniLibs?.addGeneratedSourceDirectory(mask, BuildExperimentalMask::outputDirectory)
    }
}
abstract class PrepareClassicParityAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputsToCopy: ConfigurableFileCollection
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @get:Inject
    abstract val fileSystem: FileSystemOperations
    @TaskAction fun prepare() { fileSystem.sync { from(inputsToCopy); into(outputDirectory) } }
}
val parityAssets = tasks.register<PrepareClassicParityAssets>("prepareClassicParityAssets") {
    inputsToCopy.from(rootProject.fileTree("verification/assets") {
        include("FoliaParityLatin-Bold.ttf", "NotoSansArabic-Bold.ttf", "FoliaParityCJK-Bold.otf", "FoliaParitySubtitle-Bold.otf", "FoliaParityBidi-Bold.ttf", "*OFL.txt", "OFL.txt", "README.md")
    }, rootProject.fileTree("verification/classic-fixtures") { include("scenes.tsv", "subtitle-scenes.tsv", "subtitle-cjk-scenes.tsv") })
    outputDirectory.set(layout.buildDirectory.dir("generated/classicParityAssets"))
}
androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(parityAssets, PrepareClassicParityAssets::outputDirectory)
}
val playbackProbe = providers.gradleProperty("foliaPlaybackProbe").map { it == "true" }.orElse(false)
android {
    namespace = "top.ghhccghk.accompanist.folia.sample"
    compileSdk = 37
    defaultConfig {
        applicationId = "top.ghhccghk.accompanist.folia.sample"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        if (playbackProbe.get()) applicationIdSuffix = ".playbackprobe"
        testInstrumentationRunner = if (playbackProbe.get())
            "top.ghhccghk.accompanist.folia.sample.PlaybackProcessDeviceProbe"
        else "top.ghhccghk.accompanist.folia.sample.NativeMaskDeviceProbe"
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":sample:shared"))
    implementation(libs.activity.compose)
}
