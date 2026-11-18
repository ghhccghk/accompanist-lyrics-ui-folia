pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositories {
        maven(providers.gradleProperty("foliaRepository").get()) {
            content { includeGroup("top.ghhccghk.accompanist") }
        }
        google(); mavenCentral()
    }
}
rootProject.name = "folia-independent-native-consumer"
