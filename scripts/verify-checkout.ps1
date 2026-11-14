# Verify tracked HEAD without local.properties, untracked files or existing build output.
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$revision = & git -C $projectRoot rev-parse --verify HEAD
if ($LASTEXITCODE -ne 0) { throw 'Cannot resolve committed HEAD' }
if (-not $env:ANDROID_HOME -and -not $env:ANDROID_SDK_ROOT) {
    throw 'Set ANDROID_HOME or ANDROID_SDK_ROOT to an installed SDK before clean-checkout verification'
}
$runDirectory = Join-Path $projectRoot ("build/clean-checkout-" + [Guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $runDirectory | Out-Null
$archivePath = Join-Path $runDirectory 'source.zip'
$checkout = Join-Path $runDirectory 'source'
& git -C $projectRoot archive --format=zip "--output=$archivePath" $revision
if ($LASTEXITCODE -ne 0) { throw 'Tracked source export failed' }
Expand-Archive -LiteralPath $archivePath -DestinationPath $checkout
$wrapper = Join-Path $checkout 'gradlew.bat'
& $wrapper --project-dir $checkout :library:jvmTest :sample:shared:jvmTest :sample:desktop-app:classes :sample:android-app:assembleDebug
if ($LASTEXITCODE -ne 0) { throw "Clean-checkout build/tests failed; evidence retained in $runDirectory" }
& (Join-Path $checkout 'scripts/verify-consumer.ps1')
if ($LASTEXITCODE -ne 0) { throw "Clean-checkout independent consumer failed; evidence retained in $runDirectory" }
@"
Revision: $revision
Tracked Git archive only; no local.properties, untracked daemon config, build output or reference checkout copied.
Root JVM tests, both sample builds and independent Maven consumer passed.
SDK and JDK installations and Gradle dependency cache are supplied by the host.
This is a clean source check, not an offline/reproducible-binary/isolated-cache claim.
"@ | Set-Content -LiteralPath (Join-Path $runDirectory 'result.txt') -Encoding utf8
Write-Host "Clean-checkout verification passed for $revision. Evidence: $runDirectory"
