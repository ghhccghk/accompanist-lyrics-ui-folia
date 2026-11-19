"""Verify the optional precompiled AAR in an isolated coordinate-only application.

Producer publication is a separate explicit step. No Rust/NDK build is permitted
in the consumer, which is copied outside the checkout with its own Gradle home.
Python standard library only. Never installs on an unspecified device.
"""
from importlib import import_module
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile

sys.stdout.reconfigure(encoding="utf-8", errors="replace")

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--sdk", type=Path, required=True)
parser.add_argument("--java-home", type=Path, required=True)
parser.add_argument("--work", type=Path, default=Path(tempfile.gettempdir()) / "folia-native-coordinate-consumer")
parser.add_argument("--repository", type=Path, help="Explicit local Maven repository")
parser.add_argument("--version", default="0.1.0-SNAPSHOT")
parser.add_argument("--core-only", action="store_true", help="Build only against the Kotlin coordinate")
parser.add_argument("--serial", help="Explicit task-owned emulator only; never autodetect a device")
parser.add_argument("--availability", choices=("kotlin", "present", "absent", "session-fault", "diorama-input"),
                    help="Run the startup/availability probe instead of the full lifecycle probe")
args = parser.parse_args()
if args.core_only and args.availability:
    parser.error("--availability is for the native consumer")
if args.core_only and args.version.endswith("-SNAPSHOT"):
    parser.error("Use an immutable version for the Kotlin Android APK check")
work = args.work.resolve()
assert work != ROOT and ROOT not in work.parents, "Consumer must be outside source checkout"
work.mkdir(parents=True, exist_ok=True)
project = work / ("core-app" if args.core_only else "app")
project.mkdir(exist_ok=True)
fixture = ROOT / "verification/native-consumer"
for filename in ("build.gradle.kts", "settings.gradle.kts", "session-fault-probe.pro"):
    shutil.copy2(fixture / filename, project / filename)
(project / "src/main").mkdir(parents=True, exist_ok=True)
shutil.copy2(fixture / "src/main/AndroidManifest.xml", project / "src/main/AndroidManifest.xml")
main_source, test_source = ("coreOnly", "coreOnlyTest") if args.core_only else ("main", "androidTest")
for source_set, target_set in ((main_source, "main"), (test_source, "androidTest")):
    shutil.copytree(fixture / "src" / source_set / "java", project / "src" / target_set / "java",
                    dirs_exist_ok=True)
if args.core_only:
    source = ROOT / "verification/consumer/src/commonMain/kotlin/org/example/foliaconsumer/Consumer.kt"
    destination = project / "src/main/java/org/example/foliaconsumer"
    destination.mkdir(parents=True, exist_ok=True)
    shutil.copy2(source, destination)
else:
    assets = project / "src/main/assets"
    assets.mkdir(parents=True, exist_ok=True)
    for name in ("FoliaParityLatin-Bold.ttf", "NotoSansArabic-Bold.ttf", "FoliaParityCJK-Bold.otf",
                 "FoliaParityBidi-Bold.ttf", "OFL.txt", "CJK-OFL.txt"):
        shutil.copy2(ROOT / "verification/assets" / name, assets / name)
    shutil.copy2(ROOT / "verification/classic-fixtures/scenes.tsv", assets / "scenes.tsv")
shutil.copytree(ROOT / "gradle/wrapper", project / "gradle/wrapper", dirs_exist_ok=True)
for name in ("gradlew", "gradlew.bat"):
    shutil.copy2(ROOT / name, project / name)
if args.repository:
    repository = args.repository.resolve(strict=True)
else:
    repository = work / "maven"
    shutil.copytree(ROOT / "build/local-maven/top", repository / "top", dirs_exist_ok=True)
(project / "local.properties").write_text("sdk.dir=" + args.sdk.as_posix() + "\n", encoding="utf-8")
(project / "gradle.properties").write_text(
    "android.useAndroidX=true\norg.gradle.jvmargs=-Xmx1536m -Dfile.encoding=UTF-8\n"
    "org.gradle.workers.max=2\nkotlin.compiler.execution.strategy=in-process\n", encoding="utf-8")
env = os.environ.copy()
for key in list(env):
    if any(s in key.upper() for s in ("CARGO", "RUSTUP", "NDK", "RUSTFLAGS", "JAVA_TOOL_OPTIONS")):
        env.pop(key)
env["JAVA_HOME"] = str(args.java_home)
env["ANDROID_HOME"] = str(args.sdk)
env["GRADLE_USER_HOME"] = str(work / "gradle-home")
env["CARGO_HOME"] = str(work / "no-rust")
env["RUSTUP_HOME"] = str(work / "no-rust")
if os.name == "nt":
    system = Path(os.environ["SYSTEMROOT"])
    env["PATH"] = os.pathsep.join(map(str, (args.java_home / "bin", system / "System32", system / "System32/WindowsPowerShell/v1.0", system)))
else:
    env["PATH"] = os.pathsep.join(map(str, (args.java_home / "bin", Path("/usr/bin"), Path("/bin"))))
assert shutil.which("cargo", path=env["PATH"]) is None
assert shutil.which("rustc", path=env["PATH"]) is None
wrapper = [str(project / "gradlew.bat")] if os.name == "nt" else ["sh", str(project / "gradlew")]
command = [*wrapper, "--no-daemon", "--console=plain", f"-PfoliaRepository={repository.as_uri()}",
           f"-PfoliaConsumerVersion={args.version}",
           "assembleRelease", "assembleReleaseAndroidTest", "verifyConsumerTaskBoundary"]
if args.core_only:
    command.append("-PfoliaConsumerCoreOnly=true")
omit_renderer = args.availability == "absent"
if omit_renderer:
    command.append("-PfoliaConsumerWithoutNativeRuntime=true")
if args.availability == "session-fault":
    command.append("-PfoliaConsumerSessionFaultProbe=true")
log = work / ("core-build.log" if args.core_only else "build.log")
with log.open("w", encoding="utf-8") as output:
    result = subprocess.run(command, cwd=project, env=env, stdout=output, stderr=subprocess.STDOUT)
if result.returncode:
    print(log.read_text(encoding="utf-8", errors="replace")[-14000:])
    raise SystemExit(result.returncode)
apk = next((project / "build/outputs/apk/release").glob("*-release.apk"))
with ZipFile(apk) as archive:
    native = [n for n in archive.namelist() if n.endswith("liblyrics_folia_renderer.so")]
    if args.core_only or omit_renderer:
        assert not native, "The Kotlin-only APK must not package the renderer"
    else:
        assert len(native) == 2 and {n.split('/')[1] for n in native} == {"arm64-v8a", "x86_64"}
    hashes = {n: hashlib.sha256(archive.read(n)).hexdigest() for n in native}
    assert not any("folia_mask_experiment" in n for n in archive.namelist())
    assert "classes.dex" in archive.namelist()
publication_files = import_module("verify-renderer-package").publication_files
artifact = "lyrics-ui-folia-android" if args.core_only else "lyrics-ui-folia-native-android"
files = publication_files(repository, artifact, args.version)
aar = files["aar"]
with ZipFile(aar) as archive:
    if args.core_only:
        with ZipFile(BytesIO(archive.read("classes.jar"))) as classes:
            names = archive.namelist() + classes.namelist()
        assert not any(name.lower().endswith((".so", ".dll", ".dylib")) for name in names)
    else:
        assert b"RendererJni" in archive.read("proguard.txt")
        assert {n: hashlib.sha256(archive.read(n.replace("lib/", "jni/", 1))).hexdigest() for n in native} == hashes
evidence = {"build": "PASS release + R8 + test APK; coordinate-only isolated project and Gradle home",
            "version": args.version, "artifact": artifact,
            "apk_sha256": hashlib.sha256(apk.read_bytes()).hexdigest(),
            "aar_sha256": hashlib.sha256(aar.read_bytes()).hexdigest(),
            "rust_on_consumer_path": False, "consumer_native_build_tasks": False,
            "packaged_runtime_sha256": hashes, "runtime": "NOT RUN", "apk": str(apk), "log": str(log)}
if args.serial:
    assert args.serial.startswith("emulator-"), "Use a task-owned emulator, not the user's physical device"
    adb = str(args.sdk / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb"))
    subprocess.run([adb, "-s", args.serial, "install", "-r", str(apk)], check=True)
    tests = next((project / "build/outputs/apk/androidTest/release").glob("*.apk"))
    subprocess.run([adb, "-s", args.serial, "install", "-r", "-t", str(tests)], check=True)
    command = [adb, "-s", args.serial, "shell", "am", "instrument", "-w"]
    if args.availability:
        command += ["-e", "availability", args.availability]
    command += ["org.example.folianativeconsumer.test/org.example.folianativeconsumer.NativeLifecycleProbe"]
    probe = subprocess.run(command, check=True, capture_output=True, text=True, timeout=300)
    print(probe.stdout)
    expected = f"availability={args.availability}" if args.availability else "PASS release/R8"
    assert "PASS " in probe.stdout and expected in probe.stdout and "FAIL " not in probe.stdout, probe.stdout
    evidence["runtime"] = probe.stdout.strip()
(work / ("core-evidence.json" if args.core_only else "evidence.json")).write_text(json.dumps(evidence, indent=2), encoding="utf-8")
print(json.dumps(evidence, indent=2))
