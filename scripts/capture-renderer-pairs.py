"""Capture matched paused Partita frames from the independent release consumer."""
import argparse
from importlib import import_module
import io
import json
from pathlib import Path
import subprocess
import time

import numpy as np
from PIL import Image

PACKAGE = 'org.example.folianativeconsumer'
SCREEN = (800, 600)
# Subtitle fixture at 800x600, below the main lyric and its glow.
CAPTION_REGION = np.s_[440:510, 0:800]
visual = import_module('compare-renderer-captures')


def device(adb, *args):
    return subprocess.check_output([*adb, *args], timeout=20)


def capture(adb, output, case, backend):
    key, scene, sample, w, h, scale, reduced, guides = case
    device(adb, 'shell','am','force-stop',PACKAGE)
    device(adb, 'shell','am','start','-W','-n',PACKAGE+'/.ConsumerActivity','--es','backend',backend,
           '--es','scene',scene,'--el','mediaMs',str(sample),'--ei','width',str(w),'--ei','height',str(h),
           '--ef','fontScale',str(scale),'--ez','reduced',reduced,'--ez','guides',guides)
    pid = device(adb, 'shell','pidof',PACKAGE).decode().strip()
    until = time.monotonic()+25
    status = ''
    while time.monotonic()<until:
        status = device(adb, 'logcat','-d','--pid='+pid,'-s','FoliaNativeConsumer').decode(errors='replace')
        expected = ('path=Kotlin state=Kotlin' if backend=='Kotlin' else 'path=NativeGpu state=Ready:PartitaMainAndGlow')
        if scene=='waiting' and backend=='NativeRequired':
            expected = 'path=Preparing state=Preparing'
        if any(expected in line and f'media={sample} ' in line and
               (backend == 'Kotlin' or scene == 'waiting' or f'sampled={sample * 1000} ' in line)
               for line in status.splitlines()):
            break
        if 'state=Failed' in status:
            raise RuntimeError(status)
        time.sleep(.35)
    else:
        raise RuntimeError(status)
    # Wait for actual screenshot pixels to stop changing after the status update.
    previous = None
    for _ in range(16):
        time.sleep(.35)
        png = device(adb, 'exec-out','screencap','-p')
        with Image.open(io.BytesIO(png)) as image:
            if image.size != SCREEN:
                raise ValueError(f'Expected {SCREEN} screenshot, got {image.size}')
            pixels = np.asarray(image.convert('RGB'), dtype=np.int16)
        frame = pixels[40:h, :w]
        if previous is not None and np.array_equal(previous, frame) and np.any(frame != visual.BACKGROUND):
            (output/f'{key}-{backend}.png').write_bytes(png)
            (output/f'{key}-{backend}.log').write_text(status,encoding='utf-8')
            return pixels
        previous = frame
    raise RuntimeError('Visible screenshot did not stabilize')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--adb', type=Path, required=True)
    parser.add_argument('--serial', required=True, help='Task-owned 800x600 Android emulator')
    parser.add_argument('--output', type=Path, required=True)
    parser.add_argument('--case', action='append', help='Capture only the named case; repeatable')
    args = parser.parse_args()
    if not args.serial.startswith('emulator-'):
        parser.error('Use an explicitly selected task-owned emulator')
    adb = [str(args.adb.resolve(strict=True)), '-s', args.serial]
    args.output.mkdir(parents=True, exist_ok=True)

    def standard(scene, ms):
        return (f'{scene}-{ms}', scene, ms, *SCREEN, 1, 'false', 'true')

    cases = [standard('baseline', ms) for ms in (850, 1500, 3300)]
    cases += [(name, name, 1500, *SCREEN, 1, 'false', 'true')
              for name in ('wrap', 'arabic', 'cjk', 'bidi-rtl', 'bidi-ltr', 'cluster')]
    cases += [('large-reduced', 'baseline', 1700, 640, 480, 1.5, 'true', 'false'),
              ('waiting', 'waiting', 9000, *SCREEN, 1, 'false', 'true')]
    cases += [standard('emoji', ms) for ms in (850, 1500, 2280)]
    cases += [standard('subtitle', ms) for ms in (750, 850, 1000, 1500, 3199, 3300, 3500)]
    if args.case and (unknown := set(args.case) - {case[0] for case in cases}):
        parser.error(f'Unknown cases: {sorted(unknown)}')
    results = {}
    for index, case in enumerate(cases):
        name, scene, _, width, height, *_ = case
        if args.case and name not in args.case:
            continue
        frames, errors = {}, {}
        order = ('Kotlin', 'NativeRequired') if index % 2 == 0 else ('NativeRequired', 'Kotlin')
        for backend in order:
            try:
                frames[backend] = capture(adb, args.output, case, backend)
            except (OSError, RuntimeError, ValueError, subprocess.SubprocessError) as error:
                errors[backend] = str(error)
        result = {'settings': case, 'errors': errors}
        if not errors:
            area = np.s_[40:height, :width]
            result.update(visual.summarize(frames['Kotlin'][area], frames['NativeRequired'][area]))
            if scene == 'subtitle':
                kotlin, native = (frames[backend][CAPTION_REGION] for backend in ('Kotlin', 'NativeRequired'))
                result['caption_region'] = {
                    'kotlin_foreground_pixels': int(np.any(kotlin != visual.BACKGROUND, axis=2).sum()),
                    'native_foreground_pixels': int(np.any(native != visual.BACKGROUND, axis=2).sum()),
                    'peak_error': int(np.abs(kotlin - native).max()),
                }
        results[name] = result
        print(name, json.dumps(result), flush=True)
        (args.output / 'results.json').write_text(json.dumps(results, indent=2), encoding='utf-8')
    if any(result['errors'] for result in results.values()):
        raise SystemExit('One or more captures failed')


if __name__ == '__main__':
    main()
