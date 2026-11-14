plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}
kotlin {
    android {
        namespace = "com.mocharealm.accompanist.lyrics.folia"
        compileSdk = 37
        minSdk = 21
        withHostTestBuilder {}.configure {}
    }
    jvm()
    sourceSets {
        commonMain.dependencies {
            api(libs.lyrics.core)
            api(compose.runtime)
            api(compose.ui)
            implementation(compose.foundation)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
    }
}
