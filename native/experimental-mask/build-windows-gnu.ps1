param([string]$Cargo = 'cargo')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'rust-environment.ps1')
Invoke-MaskRustToolchain -Cargo $Cargo -Build {
param([string]$Cargo)

$manifest = Join-Path $PSScriptRoot 'Cargo.toml'
$cargoCommand = Get-Command $Cargo
$rustc = Join-Path (Split-Path $cargoCommand.Source) 'rustc.exe'
if (-not (Test-Path -LiteralPath $rustc)) { $rustc = (Get-Command rustc).Source }
$sysroot = & $rustc '+1.91.1-x86_64-pc-windows-gnu' --print sysroot
if ($LASTEXITCODE -ne 0) { throw 'Cannot locate Rust 1.91.1 sysroot' }
$toolBin = Join-Path $sysroot 'lib/rustlib/x86_64-pc-windows-gnu/bin'
$bootstrap = Join-Path $PSScriptRoot 'target/bootstrap'
New-Item -ItemType Directory -Force $bootstrap | Out-Null
$linker = Join-Path $bootstrap 'ld.lld.exe'
Copy-Item -LiteralPath (Join-Path $toolBin 'rust-lld.exe') -Destination $linker
$oldFlags = $env:RUSTFLAGS
$oldLinker = $env:CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER
try {
    $env:RUSTFLAGS = '-C linker-flavor=ld -C link-self-contained=yes'
    $env:CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER = $linker
    & $Cargo '+1.91.1-x86_64-pc-windows-gnu' rustc --manifest-path $manifest --release --locked --lib -- '-C' 'link-arg=--entry=DllMainCRTStartup'
    if ($LASTEXITCODE -ne 0) { throw 'Rust mask DLL build failed' }
} finally {
    $env:RUSTFLAGS = $oldFlags
    $env:CARGO_TARGET_X86_64_PC_WINDOWS_GNU_LINKER = $oldLinker
}

}
