plugins {
    kotlin("multiplatform") version "2.4.10"
    id("com.android.kotlin.multiplatform.library") version "9.3.2"
    id("org.jetbrains.compose") version "1.12.0"
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.10"
}
kotlin {
    jvmToolchain(21)
    android { namespace = "org.example.foliaconsumer"; compileSdk = 37; minSdk = 21 }
    jvm()
    sourceSets {
        commonMain.dependencies { implementation("com.mocharealm.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT") }
        jvmTest.dependencies {
            implementation(kotlin("test"))
            implementation("org.jetbrains.compose.ui:ui-test:1.12.0")
            implementation(compose.desktop.currentOs)
        }
    }
}
