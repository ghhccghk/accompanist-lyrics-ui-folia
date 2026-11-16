#!/bin/sh
set -eu
MASK_ROOT=$(CDPATH= cd -- "$(dirname "$0")" && pwd)
ndk_directory=$1
output_directory=$2
. "$MASK_ROOT/rust-environment.sh"
resolve_mask_cargo "${3:-cargo}"
grep -q '^Pkg.Revision = 28\.2\.13676358$' "$ndk_directory/source.properties" || {
    echo 'Use pinned Android NDK 28.2.13676358 (r28c)' >&2; exit 1;
}
case "$(uname -s)" in
    Linux) ndk_host=linux-x86_64 ;;
    Darwin) ndk_host=darwin-x86_64 ;;
    *) echo 'Use build-android.ps1 on Windows' >&2; exit 1 ;;
esac
ndk_clang="$ndk_directory/toolchains/llvm/prebuilt/$ndk_host/bin/clang"
[ -x "$ndk_clang" ] || { echo "NDK host compiler missing: $ndk_clang" >&2; exit 1; }
unset CARGO_ENCODED_RUSTFLAGS
for target in aarch64-linux-android x86_64-linux-android; do
    case "$target" in
        aarch64-linux-android) abi=arm64-v8a; linker_key=CARGO_TARGET_AARCH64_LINUX_ANDROID_LINKER ;;
        x86_64-linux-android) abi=x86_64; linker_key=CARGO_TARGET_X86_64_LINUX_ANDROID_LINKER ;;
    esac
    env "$linker_key=$ndk_clang" RUSTFLAGS="-C link-arg=--target=${target}23 -C link-arg=-Wl,-z,max-page-size=16384" \
        "$MASK_CARGO" +1.91.1 build --manifest-path "$MASK_ROOT/Cargo.toml" --release --locked --lib --target "$target"
    mkdir -p "$output_directory/$abi"
    cp "$MASK_ROOT/target/$target/release/libfolia_mask_experiment.so" "$output_directory/$abi/"
done
