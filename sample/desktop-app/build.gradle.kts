plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":sample:shared"))
    implementation(compose.desktop.currentOs)
}
compose.desktop {
    application { mainClass = "top.ghhccghk.accompanist.folia.sample.MainKt" }
}

// Isolated native-window core verification; never included in the sample distribution.
val classicVerification by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output
    runtimeClasspath += output + compileClasspath
}
configurations[classicVerification.implementationConfigurationName].extendsFrom(configurations.implementation.get())
configurations[classicVerification.runtimeOnlyConfigurationName].extendsFrom(configurations.runtimeOnly.get())
tasks.register<JavaExec>("verifyClassicWindow") {
    group = "verification"
    description = "Run the public classic renderer in a real ComposeWindow and inspect its recorded layer"
    classpath = classicVerification.runtimeClasspath
    mainClass.set("top.ghhccghk.accompanist.folia.sample.ClassicWindowVerificationKt")
    workingDir = rootProject.projectDir
    dependsOn(classicVerification.classesTaskName)
}
