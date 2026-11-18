import java.time.Duration

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

/** Sample-only JVM resource: optional Rust, never an input of the lyric library. */
abstract class PrepareDesktopMaskResources : DefaultTask() {
    @get:Input abstract val includeNative: Property<Boolean>
    @get:Input abstract val cargo: Property<String>
    @get:Input abstract val platformId: Property<String>
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val nativeInputs: ConfigurableFileCollection
    @get:Optional @get:InputFile @get:PathSensitive(PathSensitivity.NONE)
    abstract val prebuiltLibrary: RegularFileProperty
    @get:Internal abstract val buildScript: RegularFileProperty
    @get:Internal abstract val builtLibrary: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @get:Inject abstract val processes: ExecOperations
    @get:Inject abstract val fileSystem: FileSystemOperations

    @TaskAction fun prepare() {
        if (includeNative.get() && !prebuiltLibrary.isPresent) {
            processes.exec {
                if (platformId.get().startsWith("windows-")) {
                    val shell = File(System.getenv("SystemRoot"), "System32/WindowsPowerShell/v1.0/powershell.exe")
                    commandLine(shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File",
                        buildScript.get().asFile, "-Cargo", cargo.get())
                } else {
                    commandLine("sh", File(buildScript.get().asFile.parentFile,"build-desktop.sh"),cargo.get())
                }
            }
        }
        val library = if (includeNative.get()) prebuiltLibrary.orElse(builtLibrary).get().asFile else null
        if (library != null) check(library.isFile) { "Missing experimental desktop native library: $library" }
        // Sync only this generated resource directory. Removing the opt-in flag
        // must remove the resource as well, including consecutive builds.
        fileSystem.sync {
            into(outputDirectory)
            if (library != null) from(library) { into("native/${platformId.get()}") }
        }
        outputDirectory.file("native/folia-mask-build.txt").get().asFile.apply {
            parentFile.mkdirs()
            writeText("experimentalNative=${includeNative.get()}\nplatform=${platformId.get()}\n")
        }
    }
}

val desktopOs = System.getProperty("os.name").lowercase().let {
    when { it.startsWith("windows") -> "windows"; it.startsWith("mac") -> "macos"; it.startsWith("linux") -> "linux"; else -> "unsupported" }
}
val desktopArch = System.getProperty("os.arch").lowercase().let {
    when(it) { "amd64", "x86_64" -> "x86_64"; "arm64", "aarch64" -> "aarch64"; else -> "unsupported" }
}
val desktopPlatform = "$desktopOs-$desktopArch"
val desktopNativeName = when(desktopOs) {
    "windows" -> "folia_mask_experiment.dll"
    "macos" -> "libfolia_mask_experiment.dylib"
    else -> "libfolia_mask_experiment.so"
}
val supportedDesktopNative = desktopOs in setOf("linux","macos") && desktopArch != "unsupported" || desktopPlatform == "windows-x86_64"

val desktopMaskResources = tasks.register<PrepareDesktopMaskResources>("prepareDesktopMaskResources") {
    val directory = rootProject.layout.projectDirectory.dir("native/experimental-mask")
    includeNative.set(providers.gradleProperty("foliaExperimentalMask").map { it == "true" }.orElse(supportedDesktopNative))
    platformId.set(desktopPlatform)
    cargo.set(providers.gradleProperty("foliaRustCargo").orElse("cargo"))
    nativeInputs.from(rootProject.fileTree(directory) {
        include("src/**", "Cargo.toml", "Cargo.lock", "rust-toolchain.toml", "build-windows-gnu.ps1", "rust-environment.ps1", "build-desktop.sh", "rust-environment.sh")
    })
    providers.gradleProperty("foliaNativeMaskDirectory").orNull?.let {
        prebuiltLibrary.set(rootProject.file("$it/$desktopNativeName"))
    }
    buildScript.set(directory.file("build-windows-gnu.ps1"))
    builtLibrary.set(directory.file("target/release/$desktopNativeName"))
    outputDirectory.set(layout.buildDirectory.dir("generated/desktopMaskResources"))
}

tasks.register<JavaExec>("verifyLocalAudio") {
    group = "verification"
    description = "Explicitly verify a real Java Sound device using an original generated 12-second WAV"
    val test = kotlin.targets.getByName("jvm").compilations.getByName("test")
    classpath = files(test.output.allOutputs, test.runtimeDependencyFiles)
    mainClass.set("top.ghhccghk.accompanist.folia.sample.NativeAudioProbeKt")
    dependsOn(test.compileTaskProvider)
    workingDir = rootProject.projectDir
}
// Explicit hardware integration, never part of the ordinary JVM test suite.
tasks.register<JavaExec>("verifyLocalPlaybackThemes") {
    group = "verification"
    description = "Verify real Java Sound playback and seven-theme sample UI with an owned silent WAV"
    val test = kotlin.targets.getByName("jvm").compilations.getByName("test")
    classpath = files(test.output.allOutputs, test.runtimeDependencyFiles)
    mainClass.set("top.ghhccghk.accompanist.folia.sample.LocalPlaybackThemesProbe")
    dependsOn(test.compileTaskProvider)
    workingDir = rootProject.projectDir
    timeout.set(Duration.ofMinutes(2))
}
kotlin {
    jvmToolchain(21)
    android {
        namespace = "top.ghhccghk.accompanist.folia.sample.shared"
        compileSdk = 37
        minSdk = 23
        withHostTestBuilder {}.configure {}
    }
    jvm()
    sourceSets {
        commonMain.dependencies {
            api(project(":library"))
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        androidMain.dependencies { implementation(libs.activity.compose) }
        androidMain { kotlin.srcDir("src/jvmAndAndroidMain/kotlin") }
        jvmMain {
            kotlin.srcDir("src/jvmAndAndroidMain/kotlin")
            resources.srcDir(desktopMaskResources.map { it.outputDirectory })
        }
        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:1.12.0")
            implementation(compose.desktop.currentOs)
        }
    }
}

// Explicit native runtime/UI verification. Ordinary tests don't require a DLL.
tasks.named<Test>("jvmTest") { exclude("**/NativeMaskRuntimeTest*", "**/NativeColourRuntimeTest*") }
tasks.register<Test>("jvmNativeMaskRuntimeTest") {
    dependsOn("jvmTestClasses")
    val ordinary = tasks.named<Test>("jvmTest")
    testClassesDirs = ordinary.get().testClassesDirs
    classpath = ordinary.get().classpath
    include("**/NativeMaskRuntimeTest*", "**/NativeColourRuntimeTest*")
    workingDir = rootProject.projectDir
    jvmArgs("-Xcheck:jni")
    val directory = providers.gradleProperty("foliaNativeMaskDirectory")
    inputs.property("nativeDirectory", directory.orElse("missing"))
    directory.orNull?.let { inputs.file(File(it, desktopNativeName)) }
    doFirst {
        require(directory.isPresent) { "Provide -PfoliaNativeMaskDirectory=<directory containing experimental DLL>" }
        systemProperty("java.library.path", File(directory.get()).canonicalPath)
    }
}

// Load from the actual packaged sample-shared JAR, with an empty library path.
// No visible window or dependency on IDE launch arguments/global PATH.
tasks.register<JavaExec>("verifyBundledDesktopMask") {
    dependsOn("jvmTestClasses", "jvmJar")
    val main = kotlin.targets.getByName("jvm").compilations.getByName("main")
    val ordinary = tasks.named<Test>("jvmTest")
    classpath = ordinary.get().classpath.minus(main.output.allOutputs) + files(tasks.named("jvmJar"))
    mainClass.set("top.ghhccghk.accompanist.folia.sample.BundledDesktopMaskProbe")
    workingDir = rootProject.projectDir
    jvmArgs("-Xcheck:jni", "-Djava.library.path=")
    providers.gradleProperty("foliaProbeJava").orNull?.let { setExecutable(it) }
}
