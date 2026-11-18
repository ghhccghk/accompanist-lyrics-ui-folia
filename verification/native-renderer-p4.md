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

| Maven artifact | SHA-256 |
| --- | --- |
| aar | `f408f4c16d8ac45214428c14c3e45f3cb968941582282a8c23a4c47db3fb9939` |
| rust-sources | `841c4f1741fdba95e23919d4a4cf071c94d19e3453cce447b8220028b22c68f2` |
| native-symbols | `99c8dbc3d4f4bead1e6da8dd5b8c3208aa233bc90747588b350514815fac5565` |
| sources | `5b2b87d4d08eb1d8b7f839fcdca21caaa69b8234007edb1667f98cc4e130b644` |
| pom | `fb961954095b8c229294446c66209c426c12cfa77ae3a70bdd6bf5a6f53ba188` |
| module | `41372ecd2cca36930c0c11b30ebbccdae18c71a18a2d8c1e3c32a5a77e8345a7` |

The first build had no ELF build IDs. The verified build sets a SHA-1 build ID
on the final crate link, before copying and stripping the ELF. The `-10` wrapper snapshot was then
built and verified through the release/R8 consumer. Its two runtime libraries,
Rust source ZIP and symbols ZIP have the same checksums as `-9`. `zipalign -c
-P 16 -v 4` passed on the exact APK that ran the lifecycle matrix.

- `-10` Maven AAR SHA-256: `360750eec96e641d1071445de68ef19d67bf42c6e20b742b34ba4bf88476b71d`
- Release consumer APK SHA-256: `d6d395468ed8baa1b0c15caa52e006c07a9ed1be33c50280c86d9a9b06e348b8`

16KiB OS behavior and ARM64 execution require runtime validation on a suitable
system. See [the lifecycle matrix](native-renderer-p3.md) for emulator results.
