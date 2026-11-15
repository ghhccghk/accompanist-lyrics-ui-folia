plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}
abstract class PrepareClassicParityAssets : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputsToCopy: ConfigurableFileCollection
    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty
    @get:Inject
    abstract val fileSystem: FileSystemOperations
    @TaskAction fun prepare() { fileSystem.sync { from(inputsToCopy); into(outputDirectory) } }
}
val parityAssets = tasks.register<PrepareClassicParityAssets>("prepareClassicParityAssets") {
    inputsToCopy.from(rootProject.fileTree("verification/assets") {
        include("FoliaParityLatin-Bold.ttf", "NotoSansArabic-Bold.ttf", "FoliaParityCJK-Bold.otf", "FoliaParitySubtitle-Bold.otf", "FoliaParityBidi-Bold.ttf", "*OFL.txt", "OFL.txt", "README.md")
    }, rootProject.fileTree("verification/classic-fixtures") { include("scenes.tsv", "subtitle-scenes.tsv", "subtitle-cjk-scenes.tsv") })
    outputDirectory.set(layout.buildDirectory.dir("generated/classicParityAssets"))
}
androidComponents.onVariants(androidComponents.selector().withBuildType("debug")) { variant ->
    variant.sources.assets?.addGeneratedSourceDirectory(parityAssets, PrepareClassicParityAssets::outputDirectory)
}
android {
    namespace = "com.mocharealm.accompanist.folia.sample"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.mocharealm.accompanist.folia.sample"
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures { compose = true }
}
dependencies {
    implementation(project(":sample:shared"))
    implementation(libs.activity.compose)
}
