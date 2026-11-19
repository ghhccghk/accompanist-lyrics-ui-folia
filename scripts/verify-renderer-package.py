"""Check a local Maven build, paired symbols and consuming APK."""
import argparse
from contextlib import nullcontext
from io import BytesIO
import hashlib
import json
from pathlib import Path
import re
import subprocess
import tempfile
import xml.etree.ElementTree as ET
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
ARTIFACT = "lyrics-ui-folia-native-android"
CORE_ARTIFACTS = ("lyrics-ui-folia", "lyrics-ui-folia-android", "lyrics-ui-folia-jvm")
VERSION = "0.1.0-SNAPSHOT"
NOTICES = "META-INF/lyrics-ui-folia-native/dependencies/"


def sha256(data):
    return hashlib.sha256(data).hexdigest()


def files_for_snapshot(directory, artifact):
    metadata = ET.parse(directory / "maven-metadata.xml")
    files = {}
    for snapshot in metadata.findall("versioning/snapshotVersions/snapshotVersion"):
        extension = snapshot.findtext("extension")
        classifier = snapshot.findtext("classifier", "")
        version = snapshot.findtext("value")
        suffix = f"-{classifier}" if classifier else ""
        files[classifier or extension] = directory / f"{artifact}-{version}{suffix}.{extension}"
    return files


def publication_files(repository, artifact, version):
    directory = repository / "top/ghhccghk/accompanist" / artifact / version
    if version.endswith("-SNAPSHOT"):
        return files_for_snapshot(directory, artifact)
    stem = f"{artifact}-{version}"
    files = {}
    for path in directory.glob(stem + "*"):
        if path.suffix in (".aar", ".jar", ".zip", ".module", ".pom"):
            classifier = path.stem[len(stem):].removeprefix("-")
            files[classifier or path.suffix[1:]] = path
    return files


def inspect_elf(readelf, binary):
    description = subprocess.check_output([readelf, "-l", "-d", "-n", "-S", "-W", binary], text=True)
    loads = [line.split() for line in description.splitlines() if line.strip().startswith("LOAD ")]
    assert loads and all(int(row[-1], 16) >= 16384 for row in loads), binary
    for row in loads:
        offset, address, alignment = (int(row[i], 16) for i in (1, 2, -1))
        assert offset % alignment == address % alignment, binary
    needed = sorted(re.findall(r"Shared library: \[([^\]]+)\]", description))
    assert set(needed) <= {"libandroid.so", "libdl.so", "libm.so", "libc.so", "liblog.so"}, needed
    build_id = re.search(r"Build ID: ([0-9a-f]+)", description).group(1)
    return {"build_id": build_id, "needed": needed, "debug_line": ".debug_line " in description,
            "load_alignments": [row[-1] for row in loads]}


def verify_core(repository, version, core_apk=None):
    verified = {}
    for artifact in CORE_ARTIFACTS:
        files = publication_files(repository, artifact, version)
        binary = "aar" if artifact.endswith("-android") else "jar"
        for required in (binary, "sources", "pom", "module"):
            assert files[required].is_file(), files[required]
        for kind, path in files.items():
            if path.suffix in (".jar", ".aar"):
                with ZipFile(path) as archive:
                    members = archive.namelist()
                    assert not any(name.lower().endswith((".so", ".dll", ".dylib")) for name in members), path
                    if path.suffix == ".aar":
                        for name in members:
                            if name.endswith(".jar"):
                                with ZipFile(BytesIO(archive.read(name))) as nested:
                                    assert not any(member.lower().endswith((".so", ".dll", ".dylib"))
                                                   for member in nested.namelist()), (path, name)
            if kind in ("pom", "module"):
                assert b"lyrics-ui-folia-native" not in path.read_bytes(), path
        verified[artifact] = {kind: sha256(path.read_bytes()) for kind, path in files.items()}
    if core_apk:
        with ZipFile(core_apk) as archive:
            assert not any("liblyrics_folia_renderer.so" in name or "folia_mask_experiment" in name
                           for name in archive.namelist()), core_apk
        verified["consumer-apk"] = sha256(core_apk.read_bytes())
    return verified


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repository", type=Path, default=ROOT / "build/local-maven")
    parser.add_argument("--version", default=VERSION)
    parser.add_argument("--readelf", type=Path, required=True)
    parser.add_argument("--zipalign", type=Path)
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--core-apk", type=Path, help="Also verify all three Kotlin publications and the core-only APK")
    args = parser.parse_args()
    if bool(args.apk) != bool(args.zipalign):
        parser.error("Specify --apk and --zipalign together")
    files = publication_files(args.repository, ARTIFACT, args.version)
    with (ZipFile(files["aar"]) as aar, ZipFile(files["rust-sources"]) as sources,
          ZipFile(files["native-symbols"]) as symbols,
          (ZipFile(args.apk) if args.apk else nullcontext()) as apk,
          tempfile.TemporaryDirectory() as temporary):
        for path in ("Cargo.lock", "Cargo.toml", "rust-toolchain.toml", "SOURCES.md", "src/android.rs",
                     "src/draw.wgsl", "src/blur.wgsl", "src/halo.wgsl", "src/plane.wgsl",
                     "src/plane_mips.wgsl", "LICENSES/MIT-three-0.185.1.txt", "build-android.ps1"):
            assert path in sources.namelist(), path
        assert aar.read("META-INF/lyrics-ui-folia-native/MIT-three-0.185.1.txt") == sources.read("LICENSES/MIT-three-0.185.1.txt")
        manifest_bytes = sources.read("third-party/DEPENDENCIES.json")
        manifest = json.loads(manifest_bytes)
        assert aar.read(NOTICES + "DEPENDENCIES.json") == manifest_bytes
        assert sha256(sources.read("Cargo.lock")) == manifest["lock_sha256"]
        crates = set()
        for package in manifest["packages"]:
            path = package["crate"]
            crates.add("third-party/" + path)
            assert sha256(sources.read("third-party/" + path)) == package["sha256"], path
            assert package["license_texts"], path
            for notice in package["license_texts"]:
                path = notice["path"]
                payload = sources.read("third-party/" + path)
                assert sha256(payload) == notice["sha256"] and aar.read(NOTICES + path) == payload, path
        assert crates == {name for name in sources.namelist() if name.endswith(".crate")}
        assert not any(name.endswith(".crate") for name in aar.namelist())
        evidence = {"version": args.version, "crates": len(crates), "lock_sha256": manifest["lock_sha256"], "so": {}}
        for abi in ("arm64-v8a", "x86_64"):
            name = f"{abi}/liblyrics_folia_renderer.so"
            runtime = aar.read("jni/" + name)
            if apk is not None:
                assert runtime == apk.read("lib/" + name), name
            stripped, unstripped = (Path(temporary) / n for n in ("runtime.so", "symbols.so"))
            stripped.write_bytes(runtime)
            unstripped.write_bytes(symbols.read(name))
            stripped_elf = inspect_elf(args.readelf, stripped)
            symbol_elf = inspect_elf(args.readelf, unstripped)
            assert not stripped_elf["debug_line"] and symbol_elf["debug_line"], name
            assert stripped_elf["build_id"] == symbol_elf["build_id"], name
            evidence["so"][abi] = {"sha256": sha256(runtime), **stripped_elf}
    if args.apk:
        check = subprocess.run([args.zipalign, "-c", "-P", "16", "-v", "4", args.apk],
                               capture_output=True, text=True)
        if check.returncode:
            raise SystemExit(check.stdout + check.stderr)
        evidence["apk_alignment"] = "PASS zipalign -c -P 16 -v 4"
        files["consumer-apk"] = args.apk
    evidence["sha256"] = {key: sha256(path.read_bytes()) for key, path in files.items()}
    if args.core_apk:
        evidence["core_sha256"] = verify_core(args.repository, args.version, args.core_apk)
    print(json.dumps(evidence, indent=2))


if __name__ == "__main__":
    main()
