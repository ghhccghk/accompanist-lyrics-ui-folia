param(
    [Parameter(Mandatory=$true)][string]$NdkDirectory,
    [Parameter(Mandatory=$true)][string]$OutputDirectory,
    [string]$Cargo = 'cargo',
    [string[]]$Abis = @('arm64-v8a','x86_64'),
    [switch]$UpdateLock
)
$ErrorActionPreference = 'Stop'
# Reuse only local tool discovery, never the CPU experiment's build or runtime.
. (Join-Path $PSScriptRoot '../experimental-mask/rust-environment.ps1')
Invoke-MaskRustToolchain -Cargo $Cargo -Build {
    param([string]$Cargo)
    if (-not ((Get-Content -LiteralPath (Join-Path $NdkDirectory 'source.properties')) -match 'Pkg.Revision = 28.2.13676358')) {
        throw 'Use pinned NDK 28.2.13676358'
    }
    $bin = Join-Path $NdkDirectory 'toolchains/llvm/prebuilt/windows-x86_64/bin'
    $clang = Join-Path $bin 'clang.exe'
    if (-not (Test-Path -LiteralPath $clang)) { throw 'Windows NDK host toolchain required' }
    $saved = @{}
    $names = @('RUSTFLAGS','CARGO_ENCODED_RUSTFLAGS','CC','AR')
    foreach ($abi in $Abis) {
        $target = switch ($abi) { 'arm64-v8a' {'aarch64-linux-android'} 'x86_64' {'x86_64-linux-android'} default {throw "Unsupported ABI $abi"} }
        $names += 'CARGO_TARGET_' + $target.ToUpper().Replace('-','_') + '_LINKER'
    }
    foreach ($name in $names) { $saved[$name] = [Environment]::GetEnvironmentVariable($name,'Process') }
    try {
        $env:CARGO_ENCODED_RUSTFLAGS = $null
        if ($UpdateLock) {
            & $Cargo '+1.91.1' generate-lockfile --manifest-path (Join-Path $PSScriptRoot 'Cargo.toml')
            if ($LASTEXITCODE -ne 0) { throw 'Lockfile generation failed' }
        }
        foreach ($abi in $Abis) {
            $target = if ($abi -eq 'arm64-v8a') {'aarch64-linux-android'} else {'x86_64-linux-android'}
            $key = 'CARGO_TARGET_' + $target.ToUpper().Replace('-','_') + '_LINKER'
            [Environment]::SetEnvironmentVariable($key,$clang,'Process')
            $env:CC = Join-Path $bin ($target + '26-clang.cmd')
            $env:AR = Join-Path $bin 'llvm-ar.exe'
            $env:RUSTFLAGS = '-C link-arg=--target=' + $target + '26 -C link-arg=-Wl,-z,max-page-size=16384'
            & $Cargo '+1.91.1' build --manifest-path (Join-Path $PSScriptRoot 'Cargo.toml') --release --locked --lib --target $target
            if ($LASTEXITCODE -ne 0) { throw "Renderer build failed: $target" }
            $destination = Join-Path $OutputDirectory $abi
            New-Item -ItemType Directory -Force -Path $destination | Out-Null
            $binary = Join-Path $PSScriptRoot "target/$target/release/liblyrics_folia_renderer.so"
            Copy-Item -LiteralPath $binary -Destination $destination
            & (Join-Path $bin 'llvm-strip.exe') --strip-debug (Join-Path $destination 'liblyrics_folia_renderer.so')
            if ($LASTEXITCODE -ne 0) { throw 'Strip failed' }
        }
    } finally {
        foreach ($name in $names) { [Environment]::SetEnvironmentVariable($name,$saved[$name],'Process') }
    }
}
