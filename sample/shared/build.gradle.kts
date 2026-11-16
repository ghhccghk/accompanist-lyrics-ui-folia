plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
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
        jvmMain { kotlin.srcDir("src/jvmAndAndroidMain/kotlin") }
        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:1.12.0")
            implementation(compose.desktop.currentOs)
        }
    }
}

// Explicit native runtime/UI verification. Ordinary tests don't require a DLL.
tasks.named<Test>("jvmTest") { exclude("**/NativeMaskRuntimeTest*") }
tasks.register<Test>("jvmNativeMaskRuntimeTest") {
    dependsOn("jvmTestClasses")
    val ordinary = tasks.named<Test>("jvmTest")
    testClassesDirs = ordinary.get().testClassesDirs
    classpath = ordinary.get().classpath
    include("**/NativeMaskRuntimeTest*")
    workingDir = rootProject.projectDir
    jvmArgs("-Xcheck:jni")
    val directory = providers.gradleProperty("foliaNativeMaskDirectory")
    inputs.property("nativeDirectory", directory.orElse("missing"))
    directory.orNull?.let { inputs.file(File(it, "folia_mask_experiment.dll")) }
    doFirst {
        require(directory.isPresent) { "Provide -PfoliaNativeMaskDirectory=<directory containing experimental DLL>" }
        systemProperty("java.library.path", File(directory.get()).canonicalPath)
    }
}
