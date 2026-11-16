#!/bin/sh
# Sourced by build helpers. No installation or global environment mutation.
resolve_mask_cargo() {
    mask_requested=$1
    if command -v "$mask_requested" >/dev/null 2>&1; then
        MASK_CARGO=$(command -v "$mask_requested")
    elif [ "$mask_requested" = cargo ]; then
        MASK_CARGO=
        for mask_candidate in "${CARGO_HOME:-$HOME/.cargo}/bin/cargo" "$MASK_ROOT/../../.work/rust-bootstrap/cargo/bin/cargo"; do
            if [ -x "$mask_candidate" ]; then MASK_CARGO=$mask_candidate; break; fi
        done
    else
        MASK_CARGO=
    fi
    if [ -z "$MASK_CARGO" ]; then
        echo 'Cargo not found. Install Rust 1.91.1 or pass -PfoliaRustCargo=<cargo>. Kotlin-only: -PfoliaExperimentalMask=false.' >&2
        exit 1
    fi
    mask_cargo_home=$(dirname "$(dirname "$MASK_CARGO")")
    mask_adjacent_rustup="$(dirname "$mask_cargo_home")/rustup"
    if [ -d "$mask_adjacent_rustup/toolchains" ]; then
        if [ -z "${CARGO_HOME:-}" ]; then export CARGO_HOME="$mask_cargo_home"; fi
        if [ -z "${RUSTUP_HOME:-}" ]; then export RUSTUP_HOME="$mask_adjacent_rustup"; fi
    fi
}
