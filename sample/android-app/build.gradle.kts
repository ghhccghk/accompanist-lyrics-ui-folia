plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
android {
    namespace = "com.mocharealm.accompanist.folia.sample"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.mocharealm.accompanist.folia.sample"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":sample:shared"))
    implementation(libs.activity.compose)
}
