plugins {
    id("com.android.application") version "9.3.2"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}
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
    packaging { jniLibs.keepDebugSymbols += "**/liblyrics_folia_renderer.so" }
}
dependencies {
    implementation("top.ghhccghk.accompanist:lyrics-ui-folia-native-android:0.1.0-SNAPSHOT")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("org.jetbrains.compose.foundation:foundation:1.12.0")
}
tasks.register("verifyConsumerTaskBoundary") {
    doLast {
        val forbidden = tasks.names.filter { it.contains("cargo",true) || it.contains("externalNativeBuild",true) || it.contains("buildLyricsRenderer",true) }
        check(forbidden.isEmpty()) { "Consumer unexpectedly builds native: $forbidden" }
    }
}
