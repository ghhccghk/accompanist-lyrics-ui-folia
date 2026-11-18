"""Build a local Maven repository and verify an independent coordinate consumer.
Python 3 stdlib only; Windows/Linux/macOS. Never uploads or configures remotes.
"""
from io import BytesIO
from pathlib import Path
import json
import os
import subprocess
import xml.etree.ElementTree as ET
from zipfile import ZipFile

def verify_dependency(name):
    assert not any(fragment in name for fragment in ("sample", "native", "rust")), name


root = Path(__file__).resolve().parents[1]
wrapper = [str(root / "gradlew.bat")] if os.name == "nt" else ["sh", str(root / "gradlew")]
subprocess.run([*wrapper, "--project-dir", str(root), "-PfoliaExperimentalMask=false", ":library:publishAllPublicationsToVerificationRepository"], check=True)
repository = root / "build/local-maven"
subprocess.run([*wrapper, "--project-dir", str(root / "verification/consumer"),
                f"-PfoliaRepository={repository.as_uri()}", "jvmTest", "compileAndroidMain"], check=True)
group = "top.ghhccghk.accompanist"
namespace = {"m": "http://maven.apache.org/POM/4.0.0"}
for artifact in ("lyrics-ui-folia", "lyrics-ui-folia-jvm", "lyrics-ui-folia-android"):
    directory = repository / group.replace(".", "/") / artifact / "0.1.0-SNAPSHOT"
    metadata = ET.parse(directory / "maven-metadata.xml").getroot()
    assert metadata.findtext("groupId") == group
    for snapshot in metadata.findall("versioning/snapshotVersions/snapshotVersion"):
        extension, version = snapshot.findtext("extension"), snapshot.findtext("value")
        classifier = snapshot.findtext("classifier")
        suffix = f"-{classifier}" if classifier else ""
        path = directory / f"{artifact}-{version}{suffix}.{extension}"
        if extension == "pom":
            pom = ET.parse(path).getroot()
            assert pom.findtext("m:groupId", namespaces=namespace) == group
            for dependency in pom.findall("m:dependencies/m:dependency", namespace):
                name = dependency.findtext("m:artifactId", namespaces=namespace)
                owner = dependency.findtext("m:groupId", namespaces=namespace)
                verify_dependency(name)
                if name.startswith("lyrics-ui-folia"):
                    assert owner == group
                if name == "lyrics-core":
                    assert owner == "com.mocharealm.accompanist"
        if extension == "module":
            for variant in json.loads(path.read_text(encoding="utf-8"))["variants"]:
                for dependency in variant.get("dependencies", []):
                    verify_dependency(dependency["module"])
                if target := variant.get("available-at"):
                    verify_dependency(target["module"])
        if extension not in ("jar", "aar"):
            continue
        with ZipFile(path) as archive:
            for notice in ("LICENSE", "NOTICE", "THIRD_PARTY_NOTICES.md", "upstream.md"):
                assert f"META-INF/lyrics-ui-folia/{notice}" in archive.namelist(), (path, notice)
            names = archive.namelist()
            if "classes.jar" in names:
                with ZipFile(BytesIO(archive.read("classes.jar"))) as classes:
                    names += classes.namelist()
            assert not any(name.lower().endswith((".so", ".dll", ".dylib")) for name in names), path
            assert not any(name.startswith("com/mocharealm/accompanist/lyrics/folia/") for name in names), path
            if artifact == "lyrics-ui-folia-jvm" and not classifier:
                assert "top/ghhccghk/accompanist/lyrics/folia/FoliaLyricsViewKt.class" in names
    print(f"PASS {artifact}: coordinates, dependency boundary, entry point and packaged notices")
print("PASS independent JVM render/callback and Android compilation; no remote publication")
