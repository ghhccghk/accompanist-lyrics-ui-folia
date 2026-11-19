# Android native renderer packaging evidence

Verified local Maven snapshot `0.1.0-20261118.225914-9` on November 19, 2026
(Asia/Taipei). Gradle 9.5.0, AGP 9.3.2, Kotlin 2.4.10, Windows producer,
Rust 1.91.1, NDK 28.2.13676358, Android native API 26, locked release,
thin LTO, one codegen unit, line-table symbols. CPU mask integration disabled.

The producer ran at base commit `f73a0546b115ac29dc338903ef98004e0d64ab60` with packaging source in
Git tree `e6e26ca4acf1c668a106503b5d556351c8d51438` and the following wrapper blobs in its working tree:

- `FoliaNativeLyricsView.kt` `5ef7c830c0f7cbc6b321d325d4ce1f708f024f58`
- `NativeLyricsSurface.kt` `99ae9dacd02f2000d72395a08b3d66aded1a1e03`
- `NativeScenePacket.kt` `b503d20978c5abab6add9ca1a6657dbbfc5d7c95`
- `RendererJni.kt` `0705df456104ecdf2bbb2b782f0f085b3bb3d545`

`Cargo.lock` SHA-256 is `6b16724c504fe8955afbd81efbec2f8f97173257836b98a4a703b73f29d4509a`.

`verify-renderer-package.py` confirmed 63 locked dependency archives, matching
manifest and actual license contents in AAR and source ZIP, both stripped ELF
binaries with four `LOAD` segments at `0x4000` and only `libandroid.so`, `libc.so`,
`libdl.so`, `libm.so` in NEEDED. Each symbol library has `.debug_line` and the
same build ID as its runtime partner.

| ABI | Build ID | Runtime SHA-256 |
| --- | --- | --- |
| arm64-v8a | `9a91546e9779c52cbc9c83bc94f372b1d8ad31fe` | `f8c2b666bbdd8877daee5901499d500a43b9e9be2ededc6be6d3bd7382250785` |
| x86_64 | `280c483b185cb7592d04644763b8a91016befa64` | `ef75671c25a6fd7d652cc7e4fd29083ff224d33da043bb96f9012a74b7ccebe2` |

| Maven artifact (`-10`) | SHA-256 |
| --- | --- |
| AAR | `360750eec96e641d1071445de68ef19d67bf42c6e20b742b34ba4bf88476b71d` |
| Rust sources | `841c4f1741fdba95e23919d4a4cf071c94d19e3453cce447b8220028b22c68f2` |
| Native symbols | `99c8dbc3d4f4bead1e6da8dd5b8c3208aa233bc90747588b350514815fac5565` |
| Kotlin sources | `86e40f0a61c5385a180841997e54aa6e29f60b3f1a2c83a19b7f970c6c868b20` |
| POM | `fb961954095b8c229294446c66209c426c12cfa77ae3a70bdd6bf5a6f53ba188` |
| Gradle module metadata | `cb9fbed22574a3cb3ab992e3a1f9364c5aecd1189beb3b8cef7a62da665e292e` |

The first build had no ELF build IDs. The verified build sets a SHA-1 build ID
on the final crate link, before copying and stripping the ELF. The `-10` wrapper
snapshot passed the release/R8 consumer and the complete lifecycle probe. Its
two runtime libraries, Rust source ZIP and symbols ZIP have the same checksums
as `-9`.

`zipalign -c -P 16 -v 4` passed on the exact release APK. Its SHA-256 is
`36d0be152301b4dda43a5a808853efa927bff4c22190d1fd1f6e98c048c59f2a`.

The same APK and instrumentation were tested on the task-owned Android 35
x86_64 Google APIs page_size_16kb image (`system-images;android-35;google_apis_ps16k;x86_64`, revision 5; official archive SHA-1
`d709cf26b375abecfbbf5803afd20749ddfa2865`),
SwiftShader Vulkan, 800x600 at 160 dpi. `getconf PAGE_SIZE` reported `16384`.
The complete release/R8 lifecycle probe passed, including native Vulkan Partita
submissions. This is a 16 KiB x86_64 emulator result, not physical hardware.
ARM64 runtime execution remains open. See [lifecycle evidence](native-renderer-p3.md).

## Scene v2 and GPU retirement rerun

On November 19, 2026, the native producer published snapshot
`0.1.0-20261119.020648-14`. The isolated coordinate-only release consumer
rebuilt and ran its lifecycle probe on the API 34 / 4 KiB and API 35 / 16 KiB
x86_64 task-owned emulators. Both reported `PASS release/R8 AAR`. The exact APK
passed `zipalign -c -P 16 -v 4`. The AAR and paired symbols were checked for
all 63 locked crate sources and licenses, both ABI build IDs, and four 0x4000
ELF LOAD alignments. The x86_64 API 35 runtime loaded the new ABI 2 binary at
16384-byte pages. The arm64-v8a binary was packaged and inspected, but no
ARM64 runtime host was available.

Eight Rust tests passed on Android Vulkan. The new GPU test uploads two
independent scene textures, renders through four increasing viewport sizes,
and checks that each old viewport's scratch attachments return to baseline
resident bytes after resize. It rejects an 8192x8192 over-budget allocation
without inflating resident accounting, then drops scenes separately and checks
the remaining texture count and byte totals. Scene retirement and resize wait
up to two seconds for submitted GPU work before releasing scratch allocations.
