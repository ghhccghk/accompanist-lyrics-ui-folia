plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace="top.ghhccghk.accompanist.folia.sample.nativegpu"
    compileSdk=37
    defaultConfig {
        applicationId="top.ghhccghk.accompanist.folia.sample.nativegpu"
        minSdk=26;targetSdk=37;versionCode=1;versionName="0.1.0"
    }
    buildFeatures{compose=true}
    buildTypes{release{isMinifyEnabled=true;proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))}}
}
dependencies {
    implementation(project(":sample:shared"))
    implementation(project(":native-renderer-android"))
    implementation(libs.activity.compose)
}
