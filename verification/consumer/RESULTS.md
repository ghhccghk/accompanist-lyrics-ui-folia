# Local artifact consumer verification — 2026-11-16

Command: `python scripts/verify-consumer.py` (Python stdlib, no new dependencies).
Windows x64, JDK21.0.5, Gradle9.5.0, Android SDK37. No Cargo needed/invoked.

PASS: local file Maven repository, KMP/Android/JVM publications and current sources.
PASS: separate Gradle root consumes `top.ghhccghk.accompanist:lyrics-ui-folia:0.1.0-SNAPSHOT`
through repository coordinates (no project/includeBuild/local source dependency).
PASS: independent JVM Compose rendering/callback test; Android Kotlin compilation.
PASS: POM group and external lyrics-core coordinate; LICENSE/NOTICE/third-party/source
ledger in archives; JVM entry point with new package; Android nested classes inspected.
PASS: no sample/native/Rust dependencies or folia_mask_experiment binaries in library.

Artifact manifest: latest-artifacts.json (latest timestamped local snapshot versions).
Ignored full logs: .work/maven-consumer-current.txt and .work/maven-portable-verifier.txt.
Portable Python wrapper selection/URI handling added for Linux/macOS, but actual
consumer build has only run on Windows. No remote upload, signing, Central account
configuration, Linux/macOS native runtime or Android device UI verification claimed.
