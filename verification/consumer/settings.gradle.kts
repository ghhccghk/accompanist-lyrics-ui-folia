pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven(requireNotNull(providers.gradleProperty("foliaRepository").orNull) {
            "Pass -PfoliaRepository=<file Maven repository URI produced by the library>"
        }) { content { includeModule("com.mocharealm.accompanist", "lyrics-ui-folia"); includeModule("com.mocharealm.accompanist", "lyrics-ui-folia-android"); includeModule("com.mocharealm.accompanist", "lyrics-ui-folia-jvm") } }
        google(); mavenCentral()
    }
}
rootProject.name = "folia-independent-consumer"
