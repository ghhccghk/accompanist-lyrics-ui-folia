plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}
val prepareLicenseResources by tasks.registering(Sync::class) {
    from(rootProject.files("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md"))
    from(rootProject.file("LICENSES")) { into("licenses") }
    into(layout.buildDirectory.dir("generated/licenseResources/META-INF/lyrics-ui-folia"))
}
kotlin {
    jvmToolchain(21)
    android {
        namespace = "com.mocharealm.accompanist.lyrics.folia"
        compileSdk = 37
        minSdk = 21
        withHostTestBuilder {}.configure {}
    }
    jvm()
    sourceSets {
        commonMain { resources.srcDir(prepareLicenseResources.map { it.destinationDir.parentFile.parentFile }) }
        commonMain.dependencies {
            api(libs.lyrics.core)
            api(compose.runtime)
            api(compose.ui)
            implementation(compose.foundation)
        }
        commonTest.dependencies { implementation(kotlin("test")) }
        jvmMain.dependencies { implementation("com.ibm.icu:icu4j:78.3") }
        jvmTest.dependencies {
            implementation("org.jetbrains.compose.ui:ui-test:1.12.0")
            implementation(compose.desktop.currentOs)
        }
        androidMain.dependencies { implementation("com.ibm.icu:icu4j:78.3") }
    }
}
// Parity tests read pinned fixtures/fonts outside their source set. Declare
// them so a newly captured source fixture cannot leave stale tests UP-TO-DATE.
tasks.withType<Test>().configureEach {
    inputs.dir(rootProject.file("verification/classic-fixtures"))
    inputs.dir(rootProject.file("verification/assets"))
}
tasks.withType<Zip>().configureEach {
    from(rootProject.file("LICENSES")) { into("META-INF/lyrics-ui-folia/licenses") }
    from(rootProject.files("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md")) { into("META-INF/lyrics-ui-folia") }
    from(rootProject.file("docs/upstream.md")) { into("META-INF/lyrics-ui-folia") }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
publishing {
    repositories { maven { name = "Verification"; url = rootProject.layout.buildDirectory.dir("local-maven").get().asFile.toURI() } }
    publications.withType<MavenPublication>().configureEach {
        artifactId = if (name == "kotlinMultiplatform") "lyrics-ui-folia" else "lyrics-ui-folia-${name.lowercase()}"
        pom {
            name.set("lyrics-ui-folia")
            description.set("Independent Kotlin/Compose classic lyrics component for Android and JVM Desktop")
            licenses { license {
                name.set("GNU Affero General Public License v3.0 only (AGPL-3.0-only)")
                url.set("https://www.gnu.org/licenses/agpl-3.0.html")
                distribution.set("repo")
            } }
        }
    }
}
