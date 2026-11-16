param(
    [Parameter(Mandatory=$true)][string]$NdkDirectory,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [string]$Cargo = 'cargo'
)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'rust-environment.ps1')
Invoke-MaskRustToolchain -Cargo $Cargo -Build {
param([string]$Cargo)

$revision = Get-Content -LiteralPath (Join-Path $NdkDirectory 'source.properties')
if (-not ($revision -match 'Pkg.Revision = 28.2.13676358')) { throw 'Use the pinned Android NDK 28.2.13676358 (r28c)' }
$clang = Join-Path $NdkDirectory 'toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe'
if (-not (Test-Path -LiteralPath $clang)) { throw 'This helper requires the Windows NDK host toolchain' }
$savedFlags = $env:RUSTFLAGS
$savedEncoded = $env:CARGO_ENCODED_RUSTFLAGS
try {
    $env:CARGO_ENCODED_RUSTFLAGS = $null
    foreach ($abi in @(
        @{ Target='aarch64-linux-android'; Clang='aarch64-linux-android23'; Abi='arm64-v8a' },
        @{ Target='x86_64-linux-android'; Clang='x86_64-linux-android23'; Abi='x86_64' }
    )) {
        $key = 'CARGO_TARGET_' + $abi.Target.ToUpper().Replace('-','_') + '_LINKER'
        $savedLinker = [Environment]::GetEnvironmentVariable($key, 'Process')
        try {
            [Environment]::SetEnvironmentVariable($key, $clang, 'Process')
            $env:RUSTFLAGS = '-C link-arg=--target=' + $abi.Clang + ' -C link-arg=-Wl,-z,max-page-size=16384'
            & $Cargo '+1.91.1' build --manifest-path (Join-Path $PSScriptRoot 'Cargo.toml') --release --locked --lib --target $abi.Target
            if ($LASTEXITCODE -ne 0) { throw "Rust mask build failed: $($abi.Target)" }
            $destination = Join-Path $OutputDirectory $abi.Abi
            New-Item -ItemType Directory -Force -Path $destination | Out-Null
            Copy-Item -LiteralPath (Join-Path $PSScriptRoot "target/$($abi.Target)/release/libfolia_mask_experiment.so") -Destination $destination
        } finally { [Environment]::SetEnvironmentVariable($key, $savedLinker, 'Process') }
    }
} finally {
    $env:RUSTFLAGS = $savedFlags
    $env:CARGO_ENCODED_RUSTFLAGS = $savedEncoded
}

}
