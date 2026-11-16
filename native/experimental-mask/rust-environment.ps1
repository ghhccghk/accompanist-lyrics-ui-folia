# Resolve an existing toolchain only. Never install tools or edit global PATH.
function Invoke-MaskRustToolchain {
    param([string]$Cargo, [scriptblock]$Build)
    $savedCargoHome = $env:CARGO_HOME
    $savedRustupHome = $env:RUSTUP_HOME
    try {
        $command = Get-Command $Cargo -CommandType Application -ErrorAction SilentlyContinue
        $resolved = if ($command) { $command.Source } else { $null }
        if (-not $resolved -and $Cargo -eq 'cargo') {
            $candidates = @()
            if ($env:CARGO_HOME) { $candidates += (Join-Path $env:CARGO_HOME 'bin/cargo.exe') }
            if ($env:USERPROFILE) { $candidates += (Join-Path $env:USERPROFILE '.cargo/bin/cargo.exe') }
            $candidates += (Join-Path $PSScriptRoot '../../.work/rust-bootstrap/cargo/bin/cargo.exe')
            $resolved = $candidates | Where-Object { Test-Path -LiteralPath $_ -PathType Leaf } | Select-Object -First 1
        }
        if (-not $resolved) {
            throw 'Cargo not found. Install Rust 1.91.1 or pass -PfoliaRustCargo=<cargo.exe>. For Kotlin-only sample use -PfoliaExperimentalMask=false.'
        }
        $resolved = (Resolve-Path -LiteralPath $resolved).Path
        # Private rustup proxies require their adjacent toolchain store too.
        $cargoDirectory = Split-Path (Split-Path $resolved)
        $adjacentRustup = Join-Path (Split-Path $cargoDirectory) 'rustup'
        if (Test-Path -LiteralPath (Join-Path $adjacentRustup 'toolchains')) {
            if (-not $env:CARGO_HOME) { $env:CARGO_HOME = $cargoDirectory }
            if (-not $env:RUSTUP_HOME) { $env:RUSTUP_HOME = $adjacentRustup }
        }
        & $Build $resolved
    } finally {
        $env:CARGO_HOME = $savedCargoHome
        $env:RUSTUP_HOME = $savedRustupHome
    }
}
