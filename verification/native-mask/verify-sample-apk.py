"""Check optional mask packaging, ELF64 exports and 16 KiB load/ZIP alignment.

Standard library only. This checks the artifact, not Android runtime behavior.
"""
import argparse
import hashlib
import json
import struct
import zipfile
from pathlib import Path


def elf64(data, machine):
    assert data[:6] == b"\x7fELF\x02\x01", "Expected little-endian ELF64"
    assert struct.unpack_from("<H", data, 18)[0] == machine, "Wrong machine"
    phoff = struct.unpack_from("<Q", data, 32)[0]
    phsize, phcount = struct.unpack_from("<HH", data, 54)
    aligns = [struct.unpack_from("<Q", data, phoff + i * phsize + 48)[0]
              for i in range(phcount) if struct.unpack_from("<I", data, phoff + i * phsize)[0] == 1]
    assert aligns and all(value >= 16384 for value in aligns), "ELF LOAD alignment below 16 KiB"
    shoff = struct.unpack_from("<Q", data, 40)[0]
    shsize, shcount = struct.unpack_from("<HH", data, 58)
    sections = [struct.unpack_from("<IIQQQQIIQQ", data, shoff + i * shsize) for i in range(shcount)]
    symbols = set()
    dependencies = []
    for section in sections:
        if section[1] not in (6, 11):  # SHT_DYNAMIC, SHT_DYNSYM
            continue
        strings = sections[section[6]]
        table = data[strings[4]: strings[4] + strings[5]]
        def name(offset):
            return table[offset:table.index(b"\0", offset)].decode("ascii")
        for offset in range(section[4], section[4] + section[5], section[9]):
            if section[1] == 11:
                index, info, visibility, defined = struct.unpack_from("<IBBH", data, offset)
                if defined and info >> 4 == 1 and visibility & 3 == 0:
                    symbols.add(name(index))
            else:
                tag, value = struct.unpack_from("<qQ", data, offset)
                if tag == 1:
                    dependencies.append(name(value))
    prefix = "Java_top_ghhccghk_accompanist_folia_sample_NativeMaskBridge_"
    assert {prefix + "blur", prefix + "version"} <= symbols, "Missing runtime JNI exports"
    assert set(dependencies) <= {"libc.so", "libdl.so", "libm.so", "liblog.so"}, "Unexpected native dependency"
    return {"load_alignment": aligns, "jni_blur_and_version_exported": True, "needed": dependencies}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("apk", type=Path)
    parser.add_argument("--native", action="store_true")
    args = parser.parse_args()
    raw = args.apk.read_bytes()
    report = {"artifact": args.apk.name, "bytes": len(raw), "sha256": hashlib.sha256(raw).hexdigest(), "libraries": {}}
    with zipfile.ZipFile(args.apk) as apk:
        masks = [entry for entry in apk.namelist() if entry.endswith("/libfolia_mask_experiment.so")]
        expected = {"lib/arm64-v8a/libfolia_mask_experiment.so": 183, "lib/x86_64/libfolia_mask_experiment.so": 62}
        assert set(masks) == (set(expected) if args.native else set()), "Unexpected mask ABI set"
        for entry in masks:
            info = apk.getinfo(entry)
            assert info.compress_type == zipfile.ZIP_STORED, "Native library must be stored uncompressed"
            filename, extra = struct.unpack_from("<HH", raw, info.header_offset + 26)
            offset = info.header_offset + 30 + filename + extra
            assert offset % 16384 == 0, "Native ZIP entry is not 16 KiB aligned"
            data = apk.read(entry)
            report["libraries"][entry] = dict(elf64(data, expected[entry]), zip_offset=offset,
                sha256=hashlib.sha256(data).hexdigest(), bytes=len(data))
    print(json.dumps(report, indent=2))


if __name__ == "__main__":
    main()
