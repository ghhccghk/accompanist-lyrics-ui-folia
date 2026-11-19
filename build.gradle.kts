plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
}
val localVersion = providers.gradleProperty("foliaLocalVersion").orElse("0.1.0-SNAPSHOT")
allprojects {
    group = "top.ghhccghk.accompanist"
    version = localVersion.get()
}
