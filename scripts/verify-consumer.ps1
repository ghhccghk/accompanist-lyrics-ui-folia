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
$groupDirectory = Join-Path $repositoryDirectory 'com/mocharealm/accompanist'
foreach ($artifact in @('lyrics-ui-folia', 'lyrics-ui-folia-jvm', 'lyrics-ui-folia-android')) {
    $versionDirectory = Join-Path $groupDirectory "$artifact/0.1.0-SNAPSHOT"
    [xml]$metadata = Get-Content -LiteralPath (Join-Path $versionDirectory 'maven-metadata.xml')
    foreach ($snapshot in $metadata.metadata.versioning.snapshotVersions.snapshotVersion) {
        if ($snapshot.extension -notin @('jar', 'aar')) { continue }
        $classifier = if ($snapshot.classifier) { "-$($snapshot.classifier)" } else { '' }
        $archivePath = Join-Path $versionDirectory "$artifact-$($snapshot.value)$classifier.$($snapshot.extension)"
        $archive = [IO.Compression.ZipFile]::OpenRead($archivePath)
        try {
            foreach ($notice in @('LICENSE', 'NOTICE', 'THIRD_PARTY_NOTICES.md', 'upstream.md')) {
                if (-not $archive.GetEntry("META-INF/lyrics-ui-folia/$notice")) { throw "Missing $notice in $archivePath" }
            }
        } finally { $archive.Dispose() }
    }
}
Write-Host 'Independent JVM runtime / Android compilation and packaged notices passed.'
