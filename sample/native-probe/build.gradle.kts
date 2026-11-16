import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.gradle.api.attributes.Attribute

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
}

val hostOs = System.getProperty("os.name")
val hostArm = System.getProperty("os.arch") in setOf("aarch64", "arm64")
val requestedTarget = providers.gradleProperty("foliaNativeTarget").getOrElse(
    when {
        hostOs.startsWith("Windows") -> "mingwX64"
        hostOs == "Mac OS X" -> if (hostArm) "macosArm64" else "macosX64"
        hostOs == "Linux" -> if (hostArm) "linuxArm64" else "linuxX64"
        else -> error("Unsupported Kotlin/Native probe host: $hostOs")
    }
)

kotlin {
    val probeTarget: KotlinNativeTarget = when (requestedTarget) {
        "mingwX64" -> mingwX64()
        "linuxX64" -> linuxX64()
        "linuxArm64" -> linuxArm64()
        "macosArm64" -> macosArm64()
        "macosX64" -> macosX64()
        else -> error("Unsupported foliaNativeTarget: $requestedTarget")
    }
    probeTarget.binaries.executable {
        baseName = "folia-native-probe"
        entryPoint = "top.ghhccghk.accompanist.folia.nativeprobe.main"
    }
    sourceSets.commonMain {
        // Compile the actual production sources, without copying or replacing platform APIs.
        kotlin.srcDir("../shared/src/commonMain/kotlin/top/ghhccghk/accompanist/folia/sample/playback")
        kotlin.srcDir("../../library/src/commonMain/kotlin/top/ghhccghk/accompanist/lyrics/folia")
        kotlin.include("**/Main.kt", "**/PreviewPlaybackController.kt", "**/FoliaPlaybackSnapshot.kt")
        dependencies { implementation(compose.runtime) }
    }

    // Resolves the full sample's essential UI/model dependencies for the selected Native target.
    // A missing published variant must fail here, rather than being replaced by fake APIs.
    val compilation = probeTarget.compilations.getByName("main")
    val uiDependencies = configurations.create("nativeUiCompatibility") {
        isCanBeConsumed = false
        isCanBeResolved = true
        val compileDependencies = configurations.getByName(compilation.compileDependencyConfigurationName)
        attributes {
            compileDependencies.attributes.keySet().forEach { key ->
                @Suppress("UNCHECKED_CAST")
                val typed = key as Attribute<Any>
                compileDependencies.attributes.getAttribute(typed)?.let { attribute(typed, it) }
            }
        }
    }
    project.dependencies.add(uiDependencies.name, libs.lyrics.core)
    project.dependencies.add(uiDependencies.name, compose.ui)
    project.dependencies.add(uiDependencies.name, compose.foundation)
    project.dependencies.add(uiDependencies.name, compose.material3)
    tasks.register("checkNativeUiDependencies") {
        group = "verification"
        description = "Resolve real UI and lyrics-core variants; does not compile a native GUI."
        doLast {
            uiDependencies.files.sortedBy { it.name }.forEach { println(it.name) }
        }
    }
}
