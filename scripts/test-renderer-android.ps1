param([Parameter(Mandatory=$true)][string]$Sdk,[Parameter(Mandatory=$true)][string]$Serial,[string]$Cargo='cargo')
$ErrorActionPreference='Stop'
if ($Serial -notlike 'emulator-*') { throw 'Specify the task-owned emulator explicitly' }
$root=Split-Path $PSScriptRoot
. (Join-Path $root 'native/experimental-mask/rust-environment.ps1')
Invoke-MaskRustToolchain -Cargo $Cargo -Build {
    param($Cargo)
    $savedFlags=$env:RUSTFLAGS
    $savedLinker=$env:CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER
    try {
        $env:CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER=Join-Path $Sdk 'ndk/28.2.13676358/toolchains/llvm/prebuilt/windows-x86_64/bin/clang.exe'
        $env:RUSTFLAGS='-C link-arg=--target=x86_64-linux-android26 -C link-arg=-Wl,-z,max-page-size=16384'
        & $Cargo '+1.91.1' test --manifest-path (Join-Path $root 'native/renderer/Cargo.toml') --lib --release --locked --target x86_64-linux-android --no-run
        if($LASTEXITCODE -ne 0){throw 'Rust test compilation failed'}
        $binary=Get-ChildItem (Join-Path $root 'native/renderer/target/x86_64-linux-android/release/deps') -Filter 'lyrics_folia_renderer-*' |
            Where-Object {$_.Extension -eq ''} | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if(-not $binary){throw 'No test executable'}
        $adb=Join-Path $Sdk 'platform-tools/adb.exe'
        & $adb -s $Serial push $binary.FullName /data/local/tmp/folia-renderer-tests
        if($LASTEXITCODE -ne 0){throw 'Test upload failed'}
        & $adb -s $Serial shell chmod 755 /data/local/tmp/folia-renderer-tests
        & $adb -s $Serial shell /data/local/tmp/folia-renderer-tests --test-threads=1
        if($LASTEXITCODE -ne 0){throw 'Android Rust tests failed'}
    } finally {$env:RUSTFLAGS=$savedFlags;$env:CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER=$savedLinker}
}
