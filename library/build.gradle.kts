plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}
val prepareLicenseResources by tasks.registering(Sync::class) {
    from(rootProject.files("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md"))
    from(rootProject.file("LICENSES")) { into("licenses") }
    into(layout.buildDirectory.dir("generated/licenseResources/META-INF/lyrics-ui-folia"))
}
kotlin {
    jvmToolchain(21)
    android {
        namespace = "top.ghhccghk.accompanist.lyrics.folia"
        compileSdk = 37
        minSdk = 21
        withHostTestBuilder {}.configure {}
    }
    jvm()
    sourceSets {
        commonMain { resources.srcDir(prepareLicenseResources.map { it.destinationDir.parentFile.parentFile }) }
        commonMain.dependencies {
            api(libs.lyrics.core)
            api(compose.runtime)
            api(compose.ui)
            implementation(compose.foundation)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmMain.dependencies { implementation("com.ibm.icu:icu4j:78.3") }
        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:1.12.0")
            implementation(compose.desktop.currentOs)
        }
        androidMain.dependencies { implementation("com.ibm.icu:icu4j:78.3") }
    }
}
// Parity tests read pinned fixtures/fonts outside their source set. Declare
// them so a newly captured source fixture cannot leave stale tests UP-TO-DATE.
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("verification/classic-fixtures"))
    inputs.dir(rootProject.file("verification/partita-fixtures"))
    inputs.dir(rootProject.file("verification/tilt-fixtures"))
    inputs.dir(rootProject.file("verification/fume-fixtures"))
    inputs.dir(rootProject.file("verification/diorama-fixtures"))
    inputs.dir(rootProject.file("verification/cadenza-fixtures"))
    inputs.dir(rootProject.file("verification/cappella-fixtures"))
    inputs.dir(rootProject.file("verification/pendolo-fixtures"))
    inputs.dir(rootProject.file("verification/assets"))
}
// Development-only JNI experiment. No Rust task or runtime dependency in the
// library, ordinary tests, or publications. Sample native packaging is separate.
val compileNativeColourProbe by tasks.registering(JavaCompile::class) {
    source(rootProject.file("verification/playback-stutter/native-colour/NativeColorExperiment.java"))
    classpath = files()
    destinationDirectory.set(layout.buildDirectory.dir("nativeColourProbe/classes"))
    javaCompiler.set(javaToolchains.compilerFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    options.release.set(8)
}
tasks.register<JavaExec>("jvmNativeColourProbe") {
    dependsOn("jvmTestClasses", compileNativeColourProbe)
    classpath = tasks.named<Test>("jvmTest").get().classpath + files(compileNativeColourProbe.map { it.destinationDirectory })
    mainClass.set("top.ghhccghk.accompanist.lyrics.folia.NativeColorExperiment")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    jvmArgs("-Xcheck:jni", "-Xmx512m")
    doFirst {
        val nativeDll = providers.gradleProperty("foliaNativeMaskDll")
        require(nativeDll.isPresent) { "Provide -PfoliaNativeMaskDll=<locally built experimental library>" }
        args(nativeDll.get(), "jvm")
    }
}
tasks.register<JavaExec>("jvmNativeMaskProbe") {
    dependsOn("jvmTestClasses")
    classpath = tasks.named<Test>("jvmTest").get().classpath
    mainClass.set("top.ghhccghk.accompanist.lyrics.folia.NativeMaskExperiment")
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
    jvmArgs("-Xcheck:jni", "-Xmx512m")
    val nativeDll = providers.gradleProperty("foliaNativeMaskDll")
    doFirst {
        require(nativeDll.isPresent) { "Provide -PfoliaNativeMaskDll=<locally built experimental library>" }
        args(nativeDll.get())
    }
}
tasks.withType<Zip>().configureEach {
    from(rootProject.file("LICENSES")) { into("META-INF/lyrics-ui-folia/licenses") }
    from(rootProject.files("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md")) { into("META-INF/lyrics-ui-folia") }
    from(rootProject.file("docs/upstream.md")) { into("META-INF/lyrics-ui-folia") }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
publishing {
    repositories { maven { name = "Verification"; url = rootProject.layout.buildDirectory.dir("local-maven").get().asFile.toURI() } }
    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "kotlinMultiplatform") "lyrics-ui-folia" else "lyrics-ui-folia-${name.lowercase()}"
        pom {
            name.set("lyrics-ui-folia")
            description.set("Independent Kotlin/Compose classic lyrics component for Android and JVM Desktop")
            licenses { license {
                name.set("GNU Affero General Public License v3.0 only (AGPL-3.0-only)")
                url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                distribution.set("repo")
            } }
        }
    }
}
