"""Original silent audio + original two-line lyrics, independently encoded/tagged.

Verification tools only (not runtime dependencies): Mutagen 1.47.0, imageio-ffmpeg 0.6.0.
Install in an isolated directory and add it to PYTHONPATH, then pass an empty output directory.
"""
import hashlib
import importlib.metadata
import json
from pathlib import Path
import subprocess
import sys
import wave

import imageio_ffmpeg
from mutagen.flac import FLAC
from mutagen.id3 import ID3, USLT, TXXX
from mutagen.mp4 import MP4
from mutagen.wave import WAVE


def main():
    assert len(sys.argv) == 2, "argument: new output directory"
    directory = Path(sys.argv[1])
    assert not directory.exists(), "Keep old fixtures/evidence; choose a new directory"
    directory.mkdir(parents=True)
    text = "[00:01.00]Amber 光\n[00:03.00]Echo 回声"
    wav = directory / "timed.wav"
    with wave.open(str(wav), "wb") as stream:
        stream.setparams((1, 2, 16000, 0, "NONE", "not compressed"))
        stream.writeframes(bytes(12 * 16000 * 2))
    ffmpeg = imageio_ffmpeg.get_ffmpeg_exe()

    def encode(name, *options):
        subprocess.run([ffmpeg, "-nostdin", "-v", "error", "-i", str(wav), *options,
                        str(directory / name)], check=True, capture_output=True)

    encode("uslt-v23.mp3", "-c:a", "libmp3lame", "-b:a", "32k")
    encode("uslt-v24.mp3", "-c:a", "libmp3lame", "-b:a", "32k")
    encode("lyrics.flac", "-c:a", "flac")
    encode("unsyncedlyrics.flac", "-c:a", "flac")
    encode("timed.m4a", "-c:a", "aac", "-b:a", "32k")
    for version, encoding in [(3, 1), (4, 3)]:
        path = directory / f"uslt-v2{version}.mp3"
        tags = ID3(path)
        tags.add(USLT(encoding=encoding, lang="eng", desc="原创描述", text=text))
        tags.save(path, v2_version=version)
        assert ID3(path).getall("USLT")[0].text == text
    for filename, key in [("lyrics.flac", "LYRICS"), ("unsyncedlyrics.flac", "UNSYNCEDLYRICS")]:
        tags = FLAC(directory / filename)
        tags[key] = text
        tags.save()
        assert FLAC(directory / filename)[key][0] == text
    tags = MP4(directory / "timed.m4a")
    tags["\xa9lyr"] = [text]
    tags.save()
    assert MP4(directory / "timed.m4a")["\xa9lyr"] == [text]
    tags = WAVE(wav)
    tags.add_tags()
    tags.tags.add(USLT(encoding=1, lang="eng", desc="", text=text))
    tags.save(v2_version=3)
    assert WAVE(wav).tags.getall("USLT")[0].text == text
    record = {
        "input": "original 12-second 16kHz mono silent PCM; original two-line text above; no user resources",
        "mutagen": importlib.metadata.version("mutagen"),
        "imageio-ffmpeg": importlib.metadata.version("imageio-ffmpeg"),
        "ffmpeg": subprocess.check_output([ffmpeg, "-version"], text=True).splitlines()[0],
        "expectedStartsMs": [1000, 3000],
        "lyrics": text,
        "fixtures": {p.name: {"bytes": p.stat().st_size, "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}
                     for p in sorted(directory.iterdir())},
    }
    (directory / "manifest.json").write_text(json.dumps(record, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(record, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
