"""Fixed original/before/after union; keep both natural and controlled metrics.

The old natural-default oracle is NEVER replaced by the diagnostic with a
different initial Canvas font. Each receives its own fixed three-way union.
"""
from pathlib import Path
import base64
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'build/verification'
BEFORE = ROOT / '.work/tilt-font-scale-before'
AFTER = BASE / 'tilt-font-scale-kotlin'
OUT = ROOT / 'verification/tilt-font-scale'
OUT.mkdir(exist_ok=True)
BG = np.array([22, 36, 53])


def native_rows(directory):
    result = {}
    for line in (directory / 'rows.tsv').read_text(encoding='utf-8').splitlines():
        if line.startswith('#') or not line:
            continue
        name, index, text, font, width, height, top = line.split('\t')
        result[name, int(index)] = dict(text=base64.b64decode(text).decode(),
            font=float(font), width=float(width), height=float(height), top=float(top))
    return result


old, new = native_rows(BEFORE), native_rows(AFTER)
report = dict(conditions='No alignment, scaling, alpha cutoff or replacement of natural-default failures. '
    'Root test canvas is 800x600; crop ONLY its known unused right margin to the requested 380px viewport. '
    '12 fixed original timestamps at 2400ms per oracle; no subtitle or full-theme acceptance.',
    gates=dict(font=.011, width=.51, height=.03125), matrices={})
contacts = []
for suffix, label in [('', 'natural'), ('-native-default', 'controlled-initial-metric-font')]:
    fixture = ROOT / f'verification/tilt-fixtures/font-scale{suffix}-v1'
    source = BASE / f'tilt-font-scale{suffix}-browser'
    data = json.loads((fixture / 'conditions.json').read_text(encoding='utf-8'))
    rows = []
    for capture in data['captures']:
        name, width = capture['file'], capture['width']
        paths = [source / name, BEFORE / name, AFTER / name]
        images = [Image.open(p).convert('RGB').crop((0, 0, width, 600)) for p in paths]
        a, b, c = [np.array(im, dtype=np.int16) for im in images]
        mask = np.any(a != BG, axis=2) | np.any(b != BG, axis=2) | np.any(c != BG, axis=2)
        entry = dict(file=name, foregroundPixels=int(mask.sum()), sourceSha256=hashlib.sha256(paths[0].read_bytes()).hexdigest(),
            beforeSha256=hashlib.sha256(paths[1].read_bytes()).hexdigest(), afterSha256=hashlib.sha256(paths[2].read_bytes()).hexdigest())
        for key, candidate in [('before', b), ('after', c)]:
            error = np.abs(a - candidate)
            local = [error[y:y+64, x:x+64][mask[y:y+64, x:x+64]].mean()
                for y in range(0, 600, 64) for x in range(0, width, 64) if mask[y:y+64, x:x+64].any()]
            residuals, failures = [], []
            for index, expected in enumerate(capture['rows']):
                actual = (old if key == 'before' else new)[name, index]
                assert actual['text'] == expected['text'], (label, name, index, 'partition')
                delta = {k: abs(actual[k] - expected[k]) for k in report['gates']}
                residuals.append(delta)
                failures += [dict(row=index, field=k, expected=expected[k], actual=actual[k], gate=gate)
                    for k, gate in report['gates'].items() if delta[k] > gate]
            entry[key] = dict(mae=float(error[mask].mean()), peakChannel=int(error.max()),
                worst64px=float(max(local)), failures=failures,
                maxGeometry={k: max(r[k] for r in residuals) for k in report['gates']})
        rows.append(entry)
        if label == 'natural' and name in ['380-1.4-0.png', '380-1.4-1.png', '800-1.4-1.png', '800-2-0.png']:
            contacts.append((name, images))
    report['matrices'][label] = dict(frames=len(rows), rows=rows, initialMetricFont=data.get('initialMetricFont', 'untouched browser default'),
        sourceFixtureSha256=hashlib.sha256((fixture/'conditions.json').read_bytes()).hexdigest(),
        summary={key: dict(meanMAE=sum(r[key]['mae'] for r in rows)/len(rows),
            maxGeometry={k:max(r[key]['maxGeometry'][k] for r in rows) for k in report['gates']},
            failedFields=sum(len(r[key]['failures']) for r in rows)) for key in ['before', 'after']})

sheet = Image.new('RGB', (1200, 4*320), '#202020')
draw = ImageDraw.Draw(sheet)
for row, (name, images) in enumerate(contacts):
    for column, (label, im) in enumerate(zip(['original natural default', 'before', 'after'], images)):
        draw.text((column*400+6, row*320+5), f'{label} | {name}', fill='white')
        im.thumbnail((400, 290))
        sheet.paste(im, (column*400+(400-im.width)//2, row*320+25))
sheet.save(OUT/'contact.png')
(OUT/'comparison.json').write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
print(json.dumps({name: item['summary'] for name,item in report['matrices'].items()}, indent=2))
