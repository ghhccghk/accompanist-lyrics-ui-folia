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
        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:1.12.0")
            implementation(compose.desktop.currentOs)
        }
    }
}
