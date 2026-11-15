$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$wrapper = Join-Path $projectRoot 'gradlew.bat'
& $wrapper --project-dir $projectRoot :library:publishAllPublicationsToVerificationRepository
if ($LASTEXITCODE -ne 0) { throw 'Local Maven assembly failed' }

$repositoryDirectory = Join-Path $projectRoot 'build/local-maven'
$repositoryUri = ([Uri]$repositoryDirectory).AbsoluteUri
$consumerDirectory = Join-Path $projectRoot 'verification/consumer'
& $wrapper --project-dir $consumerDirectory "-PfoliaRepository=$repositoryUri" jvmTest compileAndroidMain
if ($LASTEXITCODE -ne 0) { throw 'Independent consumer verification failed' }

Add-Type -AssemblyName System.IO.Compression.FileSystem
$groupDirectory = Join-Path $repositoryDirectory 'top/ghhccghk/accompanist'
$expectedGroup = 'top.ghhccghk.accompanist'
foreach ($artifact in @('lyrics-ui-folia', 'lyrics-ui-folia-jvm', 'lyrics-ui-folia-android')) {
    $versionDirectory = Join-Path $groupDirectory "$artifact/0.1.0-SNAPSHOT"
    [xml]$metadata = Get-Content -LiteralPath (Join-Path $versionDirectory 'maven-metadata.xml')
    if ($metadata.metadata.groupId -ne $expectedGroup) { throw "Unexpected group for $artifact" }
    foreach ($snapshot in $metadata.metadata.versioning.snapshotVersions.snapshotVersion) {
        if ($snapshot.extension -eq 'pom') {
            [xml]$pom = Get-Content -LiteralPath (Join-Path $versionDirectory "$artifact-$($snapshot.value).pom")
            if ($pom.project.groupId -ne $expectedGroup) { throw "Unexpected POM group for $artifact" }
            foreach ($dependency in $pom.project.dependencies.dependency) {
                if ($dependency.artifactId -like 'lyrics-ui-folia*' -and $dependency.groupId -ne $expectedGroup) {
                    throw "Stale Folia group in $artifact POM"
                }
                if ($dependency.artifactId -eq 'lyrics-core' -and $dependency.groupId -ne 'com.mocharealm.accompanist') {
                    throw "External lyrics-core coordinate was changed in $artifact POM"
                }
            }
        }
        if ($snapshot.extension -notin @('jar', 'aar')) { continue }
        $classifier = if ($snapshot.classifier) { "-$($snapshot.classifier)" } else { '' }
        $archivePath = Join-Path $versionDirectory "$artifact-$($snapshot.value)$classifier.$($snapshot.extension)"
        $archive = [IO.Compression.ZipFile]::OpenRead($archivePath)
        try {
            foreach ($notice in @('LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md', 'upstream.md')) {
                if (-not $archive.GetEntry("META-INF/lyrics-ui-folia/$notice")) { throw "Missing $notice in $archivePath" }
            }
            if ($archive.Entries.FullName -like 'com/mocharealm/accompanist/lyrics/folia/*') {
                throw "Old Folia package in $archivePath"
            }
            if ($artifact -eq 'lyrics-ui-folia-jvm' -and -not $snapshot.classifier -and
                -not $archive.GetEntry('top/ghhccghk/accompanist/lyrics/folia/FoliaLyricsViewKt.class')) {
                throw "New Folia entry point is missing in $archivePath"
            }
        } finally { $archive.Dispose() }
    }
}
Write-Host 'New Maven coordinates / JVM entry point / independent JVM runtime / Android compilation and packaged notices passed.'
