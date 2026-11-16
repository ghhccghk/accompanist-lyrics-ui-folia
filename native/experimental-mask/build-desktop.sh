#!/bin/sh
set -eu
MASK_ROOT=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
. "$MASK_ROOT/rust-environment.sh"
resolve_mask_cargo "${1:-cargo}"
case "$(uname -s)" in Linux|Darwin) ;; *) echo 'Use build-windows-gnu.ps1 on Windows' >&2; exit 1;; esac
"$MASK_CARGO" +1.91.1 build --manifest-path "$MASK_ROOT/Cargo.toml" --release --locked --lib
