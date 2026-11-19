"""Run the packaged renderer under pressure in a dedicated consumer process.

One opted-in task emulator only. Sampled media time and submit IDs are not FPS.
"""
import argparse
import json
from pathlib import Path
import re
import subprocess
import time

PACKAGE = "org.example.folianativeconsumer"


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adb", type=Path, required=True)
    parser.add_argument("--serial", required=True, help="Explicit task-owned emulator")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    if not args.serial.startswith("emulator-"):
        parser.error("Use a task-owned emulator")
    adb = [str(args.adb.resolve(strict=True)), "-s", args.serial]
    args.output.mkdir(parents=True, exist_ok=True)

    def run(*command):
        return subprocess.check_output([*adb, *command], encoding="utf-8", errors="replace", timeout=35).strip()

    def status():
        return run("logcat", "-d", "--pid=" + pid, "-s", "FoliaNativeConsumer")

    def sample(media, *, theme="Partita", scene="baseline", width=800, height=600, font=1, play_for=0):
        run("shell", "am", "start", "-W", "--activity-single-top", "-n", PACKAGE + "/.ConsumerActivity",
            "--es", "backend", "NativeRequired", "--es", "theme", theme, "--es", "scene", scene,
            "--el", "mediaMs", str(media), "--ei", "width", str(width), "--ei", "height", str(height),
            "--ef", "fontScale", str(font), "--el", "playForMs", str(play_for))
        if run("shell", "pidof", PACKAGE) != pid:
            raise RuntimeError("Pressure process changed")
        deadline = time.monotonic() + 20
        stage = "PartitaMainAndGlow" if theme == "Partita" else "DioramaText"
        while time.monotonic() < deadline:
            events = [line for line in status().splitlines() if "STATE slot=0 " in line]
            if events and " state=Failed " in events[-1]:
                raise RuntimeError(events[-1])
            if events and (hit := re.search(r" sampled=(\d+) ", events[-1])) and (
                f"scene={scene} media={media} " in events[-1] and f" state=Ready:{stage} " in events[-1]
                and int(hit[1]) == (media + play_for) * 1000
            ):
                return dict(re.findall(r"(\w+)=([^ ]+)", events[-1].split("STATE ", 1)[-1]))
            time.sleep(.045)
        raise RuntimeError(f"No submission for {theme}/{scene} at {media}+{play_for} ms: {events[-1:]}")

    report = {"pid": None, "completed": [], "checkpoints": []}

    def checkpoint(name, event):
        meminfo = run("shell", "dumpsys", "meminfo", pid)
        (args.output / f"{name}-meminfo.txt").write_text(meminfo, encoding="utf-8")
        match = re.search(r"TOTAL PSS:\s*(\d+)", meminfo)
        report["checkpoints"].append({"phase": name, "pss_kib": int(match[1]) if match else None, "renderer": event})

    run("shell", "am", "force-stop", PACKAGE)
    run("shell", "am", "start", "-W", "-n", PACKAGE + "/.ConsumerActivity", "--es", "backend", "Kotlin")
    pid = run("shell", "pidof", PACKAGE)
    report["pid"] = pid
    try:
        checkpoint("continuous", sample(700, play_for=1200))
        report["completed"].append("continuous monotonic host clock")
        for scene, samples in (("baseline", [*range(700, 1901, 16), *range(3200, 3551, 16)]),
                               ("wrap", range(700, 1901, 16))):
            for index, media in enumerate(samples):
                event = sample(media, scene=scene)
                if index % 25 == 0:
                    checkpoint(f"{scene}-{media}", event)
            checkpoint(f"{scene}-end", event)
            report["completed"].append(f"{scene}: {len(samples)} dense timestamps")
        for theme, media, width, height, font in (("Diorama", 1500, 800, 600, 1),
                ("Diorama", 3550, 640, 480, 1.5), ("Diorama", 900, 800, 600, 1),
                ("Partita", 1500, 640, 480, 1.4), ("Partita", 3300, 800, 600, 1)):
            checkpoint(f"{theme}-{media}", sample(media, theme=theme, width=width, height=height, font=font))
        report["completed"].append("theme transition, seek, font and viewport changes")
    except (RuntimeError, subprocess.SubprocessError) as exc:
        report["failure"] = str(exc)
        raise
    finally:
        (args.output / "renderer.log").write_text(status(), encoding="utf-8")
        (args.output / "pressure.json").write_text(json.dumps(report, indent=2), encoding="utf-8")
        run("shell", "am", "force-stop", PACKAGE)
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
