param([Parameter(Mandatory=$true)][string]$OutputDirectory,[string]$Cargo='cargo')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot '../experimental-mask/rust-environment.ps1')
Invoke-MaskRustToolchain -Cargo $Cargo -Build {
    param($Cargo)
    New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
    $metadata=@()
    foreach($target in @('aarch64-linux-android','x86_64-linux-android')) {
        $path=Join-Path $OutputDirectory ($target+'.json')
        & $Cargo '+1.91.1' metadata --manifest-path (Join-Path $PSScriptRoot 'Cargo.toml') --locked --offline --filter-platform $target --format-version 1 |
            Out-File -LiteralPath $path -Encoding utf8
        if($LASTEXITCODE -ne 0){throw 'Locked offline Cargo metadata failed; build/fetch pinned dependencies first'}
        $metadata+=$path
    }
    & python (Join-Path $PSScriptRoot '../../scripts/archive-renderer-dependencies.py') --metadata @metadata --lock (Join-Path $PSScriptRoot 'Cargo.lock') --output (Join-Path $OutputDirectory 'bundle') --supplements (Join-Path $PSScriptRoot 'third-party-license-supplements')
    if($LASTEXITCODE -ne 0){throw 'Dependency archive verification failed'}
}
