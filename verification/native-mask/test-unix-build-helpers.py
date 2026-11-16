"""Host-selection/quoting/failure tests under sh. NOT Linux/macOS native compilation."""
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile

repo = Path(__file__).resolve().parents[2]
shell = sys.argv[1] if len(sys.argv) > 1 else "sh"
def shell_path(path):
    value = Path(path).as_posix()
    if os.name == "nt":
        return "/" + value[0].lower() + value[2:]
    return value

with tempfile.TemporaryDirectory(prefix="folia unix helpers ") as directory:
    root = Path(directory)
    native = root / "native with spaces"
    native.mkdir()
    for name in ("build-android.sh", "build-desktop.sh", "rust-environment.sh"):
        shutil.copyfile(repo / "native/experimental-mask" / name, native / name)
    tools = root / "test-tools"
    tools.mkdir()
    def script(path, text):
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text("#!/bin/sh\nset -eu\n" + text, encoding="utf-8")
        path.chmod(0o755)
    script(tools / "uname", 'printf "%s\\n" "$TEST_OS"\n')
    script(tools / "cargo", r'''printf '%s\n' "$@" >> "$TEST_LOG"
if [ "${TEST_FAIL:-0}" != 0 ]; then exit 17; fi
target=
while [ "$#" -gt 0 ]; do
    if [ "$1" = --target ]; then shift; target=$1; fi
    shift
done
if [ -n "$target" ]; then
    mkdir -p "$TEST_NATIVE_ROOT/target/$target/release"
    printf 'test sentinel only' > "$TEST_NATIVE_ROOT/target/$target/release/libfolia_mask_experiment.so"
fi
''')
    ndk = root / "NDK with spaces"
    ndk.mkdir()
    (ndk / "source.properties").write_text("Pkg.Revision = 28.2.13676358\n", encoding="utf-8")
    for host in ("linux-x86_64", "darwin-x86_64"):
        script(ndk / f"toolchains/llvm/prebuilt/{host}/bin/clang", "exit 0\n")
    for host in ("Linux", "Darwin"):
        log = root / f"{host}.txt"
        env = os.environ.copy()
        # A shell sets POSIX paths; avoid passing Windows PATH syntax to sh.
        command = 'export PATH="$1:/usr/bin:/bin"; shift; exec sh "$@"'
        env.update(TEST_OS=host, TEST_LOG=shell_path(log), TEST_NATIVE_ROOT=shell_path(native))
        for helper, args in (("build-desktop.sh", ["cargo"]), ("build-android.sh", [shell_path(ndk), shell_path(root / "out"), "cargo"])):
            call = [shell, "-c", command, "folia-test", shell_path(tools), shell_path(native / helper), *args]
            result = subprocess.run(call, env=env, capture_output=True, text=True)
            assert result.returncode == 0, (call,result.stdout,result.stderr)
        assert "+1.91.1" in log.read_text()
        assert "aarch64-linux-android" in log.read_text()
        assert "x86_64-linux-android" in log.read_text()
        for abi in ("arm64-v8a", "x86_64"):
            assert (root / "out" / abi / "libfolia_mask_experiment.so").read_text() == "test sentinel only"
        env["TEST_FAIL"] = "1"
        failed = subprocess.run([shell, "-c", command, "folia-test", shell_path(tools), shell_path(native / "build-desktop.sh"), "cargo"], env=env, capture_output=True)
        assert failed.returncode == 17
        print(f"PASS {host}: helper arguments, spaces, two ABI copies, compiler failure propagates (mock tools only)")
