import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Locale

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}
kotlin { jvmToolchain(21) }

// Values accepted by the pinned Skiko JVM parser. Its GraphicsApi enum also
// contains VULKAN, but this runtime cannot select a Vulkan JVM renderer.
val foliaRenderApi = providers.gradleProperty("foliaRenderApi").map { it.trim().uppercase(Locale.ROOT) }.getOrElse("AUTO")
require(foliaRenderApi in setOf("AUTO", "DIRECT3D", "OPENGL", "ANGLE", "METAL", "SOFTWARE_FAST", "SOFTWARE_COMPAT")) {
    "Unsupported foliaRenderApi=$foliaRenderApi. Use AUTO, DIRECT3D, OPENGL, ANGLE, METAL, SOFTWARE_FAST or SOFTWARE_COMPAT. " +
        "The pinned Skiko JVM runtime does not implement Vulkan selection."
}
tasks.withType<JavaExec>().configureEach {
    if (foliaRenderApi != "AUTO") systemProperty("skiko.renderApi", foliaRenderApi)
}
dependencies {
    implementation(project(":sample:shared"))
    implementation(compose.desktop.currentOs)
}
compose.desktop {
    application {
        mainClass = "top.ghhccghk.accompanist.folia.sample.MainKt"
        if (foliaRenderApi != "AUTO") jvmArgs += "-Dskiko.renderApi=$foliaRenderApi"

        nativeDistributions {
            targetFormats(TargetFormat.Exe, TargetFormat.Msi) // 同时生成EXE和MSI
            packageName = "Accompanist folia demo" // 自定义应用名称
            packageVersion = "0.0.1" // 设置版本号

            windows {
                msiPackageVersion = packageVersion
                exePackageVersion = packageVersion
            }
        }

    }
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

tasks.register<JavaExec>("verifyThemesWindow") {
    group = "verification"
    description = "Check seven public themes and reconstruction in a real ComposeWindow"
    classpath = classicVerification.runtimeClasspath
    mainClass.set("top.ghhccghk.accompanist.folia.sample.ThemesWindowVerification")
    workingDir = rootProject.projectDir
    dependsOn(classicVerification.classesTaskName)
}

tasks.register<JavaExec>("verifyDioramaWindow") {
    group = "verification"
    description = "Draw public Diorama in a dedicated real window and record the actual graphics backend"
    classpath = classicVerification.runtimeClasspath
    mainClass.set("top.ghhccghk.accompanist.folia.sample.DioramaWindowVerification")
    workingDir = rootProject.projectDir
    dependsOn(classicVerification.classesTaskName)
}

tasks.register<JavaExec>("verifyRenderDebugWindow") {
    group = "verification"
    description = "Verify actual window render callback diagnostics and hook lifecycle"
    classpath = classicVerification.runtimeClasspath
    mainClass.set("top.ghhccghk.accompanist.folia.sample.RenderDebugWindowVerification")
    workingDir = rootProject.projectDir
    dependsOn(classicVerification.classesTaskName)
}
