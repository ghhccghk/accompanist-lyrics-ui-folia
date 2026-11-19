plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}
abstract class BuildLyricsRenderer : DefaultTask() {
    @get:InputFiles @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val nativeInputs: ConfigurableFileCollection
    @get:Input abstract val cargo: Property<String>
    @get:Internal abstract val ndkDirectory: DirectoryProperty
    @get:Internal abstract val script: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty
    @get:OutputDirectory abstract val symbolDirectory: DirectoryProperty
    @get:Inject abstract val processes: ExecOperations
    @TaskAction fun build() {
        require(System.getProperty("os.name").startsWith("Windows")) {
            "Producer currently verified on Windows; use precompiled Maven AAR as a consumer."
        }
        processes.exec {
            val shell = File(System.getenv("SystemRoot"), "System32/WindowsPowerShell/v1.0/powershell.exe")
            commandLine(shell, "-NoProfile", "-ExecutionPolicy", "Bypass", "-File", script.get().asFile,
                "-NdkDirectory", ndkDirectory.get().asFile, "-OutputDirectory", outputDirectory.get().asFile,
                "-SymbolDirectory",symbolDirectory.get().asFile,"-Cargo", cargo.get())
        }
    }
}
val renderer = tasks.register<BuildLyricsRenderer>("buildLyricsRenderer") {
    val directory = rootProject.layout.projectDirectory.dir("native/renderer")
    nativeInputs.from(fileTree(directory) { include("src/**", "Cargo.toml", "Cargo.lock", "rust-toolchain.toml", "build-android.ps1") },
        rootProject.file("native/experimental-mask/rust-environment.ps1"))
    cargo.set(providers.gradleProperty("foliaRustCargo").orElse("cargo"))
    script.set(directory.file("build-android.ps1"))
    ndkDirectory.set(androidComponents.sdkComponents.sdkDirectory.map { it.dir("ndk/28.2.13676358") })
    outputDirectory.set(layout.buildDirectory.dir("generated/renderer/jniLibs"))
    symbolDirectory.set(layout.buildDirectory.dir("generated/renderer/symbols"))
}
val dependencyBundle = tasks.register<Exec>("archiveRendererDependencies") {
    inputs.files(rootProject.files("native/renderer/Cargo.toml", "native/renderer/Cargo.lock", "native/renderer/rust-toolchain.toml",
        "native/renderer/archive-dependencies.ps1",
        "scripts/archive-renderer-dependencies.py", "native/experimental-mask/rust-environment.ps1"))
    inputs.dir(rootProject.file("native/renderer/third-party-license-supplements"))
    val output=layout.buildDirectory.dir("generated/rendererDependencies")
    outputs.dir(output)
    val shell=File(System.getenv("SystemRoot"),"System32/WindowsPowerShell/v1.0/powershell.exe")
    commandLine(shell,"-NoProfile","-ExecutionPolicy","Bypass","-File",rootProject.file("native/renderer/archive-dependencies.ps1"),
        "-OutputDirectory",output.get().asFile,"-Cargo",providers.gradleProperty("foliaRustCargo").orElse("cargo").get())
    dependsOn(renderer)
}
kotlin {
    jvmToolchain(21)
    android {
        namespace = "top.ghhccghk.accompanist.lyrics.folia.nativeandroid"
        compileSdk = 37
        minSdk = 26
        optimization { consumerKeepRules.file("consumer-rules.pro") }
        withHostTestBuilder {}.configure {}
    }
    sourceSets {
        androidMain.dependencies { api(project(":library")); implementation(compose.foundation) }
        androidMain { kotlin.srcDir("src/main/kotlin") }
    }
}
androidComponents.onVariants { variant ->
    variant.sources.jniLibs?.addGeneratedSourceDirectory(renderer, BuildLyricsRenderer::outputDirectory)
}
val sourceArchive = tasks.register<Zip>("rendererSourceArchive") {
    archiveClassifier.set("rust-sources")
    from(rootProject.file("native/renderer")) { exclude("target/**") }
    from(rootProject.files("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md"))
    from(rootProject.file("LICENSES/MIT-three-0.185.1.txt")) { into("LICENSES") }
    from(rootProject.file("native/experimental-mask/rust-environment.ps1")) { into("build-support") }
    from(rootProject.file("scripts/archive-renderer-dependencies.py")) { into("build-support") }
    dependsOn(dependencyBundle)
    from(layout.buildDirectory.dir("generated/rendererDependencies/bundle")) { into("third-party") }
}
val symbolArchive=tasks.register<Zip>("rendererSymbolArchive") {
    archiveClassifier.set("native-symbols")
    from(renderer.map { it.symbolDirectory })
}
tasks.withType<Zip>().configureEach {
    if (name != "rendererSourceArchive") {
        from(rootProject.files("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md", "LICENSES/MIT-three-0.185.1.txt")) { into("META-INF/lyrics-ui-folia-native") }
        dependsOn(dependencyBundle)
        from(layout.buildDirectory.dir("generated/rendererDependencies/bundle")) {
            exclude("registry-sources/**")
            into("META-INF/lyrics-ui-folia-native/dependencies")
        }
    }
}
publishing {
    repositories {
        maven {
            name = "Verification"
            url = providers.gradleProperty("foliaLocalRepository")
                .map { rootProject.file(it).toURI() }
                .getOrElse(rootProject.layout.buildDirectory.dir("local-maven").get().asFile.toURI())
        }
    }
    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "kotlinMultiplatform") "lyrics-ui-folia-native" else "lyrics-ui-folia-native-android"
        if (name != "kotlinMultiplatform") { artifact(sourceArchive);artifact(symbolArchive) }
        pom {
            name.set("lyrics-ui-folia-native-android")
            description.set("Optional experimental Android Vulkan lyrics renderer; precompiled Rust runtime")
            licenses { license { name.set("AGPL-3.0-only"); url.set("https://www.gnu.org/licenses/agpl-3.0.html") } }
        }
    }
}
