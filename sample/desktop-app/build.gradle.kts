plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}
dependencies {
    implementation(project(":sample:shared"))
    implementation(compose.desktop.currentOs)
}
compose.desktop {
    application { mainClass = "com.mocharealm.accompanist.folia.sample.MainKt" }
}
