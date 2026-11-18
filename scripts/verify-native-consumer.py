"""Verify the optional precompiled AAR in an isolated coordinate-only application.

Producer publication is a separate explicit step. No Rust/NDK build is permitted
in the consumer, which is copied outside the checkout with its own Gradle home.
Python standard library only. Never installs on an unspecified device.
"""
from pathlib import Path
from zipfile import ZipFile
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--sdk", type=Path, required=True)
parser.add_argument("--java-home", type=Path, required=True)
parser.add_argument("--work", type=Path, default=Path(tempfile.gettempdir()) / "folia-native-coordinate-consumer")
parser.add_argument("--serial", help="Explicit task-owned emulator only; never autodetect a device")
args = parser.parse_args()
work = args.work.resolve()
assert work != ROOT and ROOT not in work.parents, "Consumer must be outside source checkout"
work.mkdir(parents=True, exist_ok=True)
project = work / "app"
project.mkdir(exist_ok=True)
shutil.copytree(ROOT / "verification/native-consumer", project, dirs_exist_ok=True)
shutil.copytree(ROOT / "gradle/wrapper", project / "gradle/wrapper", dirs_exist_ok=True)
for name in ("gradlew", "gradlew.bat"):
    shutil.copy2(ROOT / name, project / name)
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
           "assembleRelease", "assembleReleaseAndroidTest", "verifyConsumerTaskBoundary"]
log = work / "build.log"
with log.open("w", encoding="utf-8") as output:
    result = subprocess.run(command, cwd=project, env=env, stdout=output, stderr=subprocess.STDOUT)
if result.returncode:
    print(log.read_text(encoding="utf-8", errors="replace")[-14000:])
    raise SystemExit(result.returncode)
apk = next((project / "build/outputs/apk/release").glob("*-release.apk"))
with ZipFile(apk) as archive:
    native = [n for n in archive.namelist() if n.endswith("liblyrics_folia_renderer.so")]
    assert len(native) == 2 and {n.split('/')[1] for n in native} == {"arm64-v8a", "x86_64"}
    hashes = {n: hashlib.sha256(archive.read(n)).hexdigest() for n in native}
    assert not any("folia_mask_experiment" in n for n in archive.namelist())
    assert "classes.dex" in archive.namelist()
aar = next((ROOT / "native-renderer-android/build/outputs/aar").glob("*.aar"))
with ZipFile(aar) as archive:
    assert b"RendererJni" in archive.read("proguard.txt")
    assert {n: hashlib.sha256(archive.read(n.replace("lib/", "jni/", 1))).hexdigest() for n in native} == hashes
evidence = {"build": "PASS release + R8 + test APK; coordinate-only isolated project and Gradle home",
            "rust_on_consumer_path": False, "consumer_native_build_tasks": False,
            "packaged_runtime_sha256": hashes, "runtime": "NOT RUN", "apk": str(apk), "log": str(log)}
if args.serial:
    assert args.serial.startswith("emulator-"), "Use a task-owned emulator, not the user's physical device"
    adb = str(args.sdk / "platform-tools" / ("adb.exe" if os.name == "nt" else "adb"))
    subprocess.run([adb, "-s", args.serial, "install", "-r", str(apk)], check=True)
    tests = next((project / "build/outputs/apk/androidTest/release").glob("*.apk"))
    subprocess.run([adb, "-s", args.serial, "install", "-r", "-t", str(tests)], check=True)
    probe = subprocess.run([adb, "-s", args.serial, "shell", "am", "instrument", "-w", "org.example.folianativeconsumer.test/org.example.folianativeconsumer.NativeConsumerProbe"], check=True, capture_output=True, text=True)
    print(probe.stdout)
    assert "PASS release/R8" in probe.stdout and "FAIL " not in probe.stdout, probe.stdout
    evidence["runtime"] = probe.stdout.strip()
(work / "evidence.json").write_text(json.dumps(evidence, indent=2), encoding="utf-8")
print(json.dumps(evidence, indent=2))
