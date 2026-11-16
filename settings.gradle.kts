pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        providers.gradleProperty("lyricsCoreRepository").orNull?.let { repository ->
            maven(repository) { content { includeGroupAndSubgroups("com.mocharealm.accompanist") } }
        }
        google()
        mavenCentral()
    }
}
rootProject.name = "lyrics-ui-folia"
include(":library", ":sample:shared", ":sample:android-app", ":sample:desktop-app")
// Opt-in feasibility experiment; never adds unsupported variants to the published library.
if (providers.gradleProperty("foliaNativeProbe").orNull == "true") {
    include(":sample:native-probe")
}
