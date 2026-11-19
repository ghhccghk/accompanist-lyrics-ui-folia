plugins {
    id("com.android.application") version "9.3.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}
// Test a missing runtime while keeping exactly the same Kotlin/AAR APIs.
val coreOnly = providers.gradleProperty("foliaConsumerCoreOnly").map(String::toBoolean).orElse(false)
val foliaVersion = providers.gradleProperty("foliaConsumerVersion").orElse("0.1.0-SNAPSHOT")
val omitRenderer = providers.gradleProperty("foliaConsumerWithoutNativeRuntime").map(String::toBoolean).orElse(false)
android {
    namespace = "org.example.folianativeconsumer"
    compileSdk = 37
    defaultConfig {
        applicationId = "org.example.folianativeconsumer"
        minSdk = 26; targetSdk = 37; versionCode = 1; versionName = "1"
        testInstrumentationRunner = "org.example.folianativeconsumer.NativeLifecycleProbe"
    }
    buildFeatures { compose = true }
    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    testBuildType = "release"
    // Producer already strips debug info. Keep its exact verified binary so
    // provenance can be checked byte-for-byte after consumer packaging.
    packaging {
        jniLibs.keepDebugSymbols += "**/liblyrics_folia_renderer.so"
        if (omitRenderer.get()) jniLibs.excludes += "**/liblyrics_folia_renderer.so"
    }
}
dependencies {
    val artifact = if (coreOnly.get()) "lyrics-ui-folia" else "lyrics-ui-folia-native-android"
    implementation("top.ghhccghk.accompanist:$artifact:${foliaVersion.get()}")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.compose.foundation:foundation:1.12.0")
}
tasks.register("verifyConsumerTaskBoundary") {
    doLast {
        val modules = configurations.getByName("releaseRuntimeClasspath").incoming.resolutionResult.allComponents
            .mapNotNull { it.moduleVersion }.filter { it.group == "top.ghhccghk.accompanist" }
        check(modules.any { it.name == "lyrics-ui-folia-android" } && modules.all { it.version == foliaVersion.get() }) { modules }
        check(modules.any { it.name == "lyrics-ui-folia-native-android" } != coreOnly.get()) { modules }
        val forbidden = tasks.names.filter { it.contains("cargo",true) || it.contains("externalNativeBuild",true) || it.contains("buildLyricsRenderer",true) }
        check(forbidden.isEmpty()) { "Consumer unexpectedly builds native: $forbidden" }
    }
}
