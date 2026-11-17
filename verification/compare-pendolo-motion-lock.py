"""Complete old/exact-lock source differences, after the audit capture exits."""
from pathlib import Path
import hashlib
import json
import numpy as np
from PIL import Image, ImageDraw

root = Path(__file__).resolve().parents[1]
report = root / 'verification/motion-lock-audit/pendolo-dense.json'
d = json.loads(report.read_text(encoding='utf-8'))
baseline = root / '.work' / d['run']
d['fontSha256'] = [dict(file=name, sha256=hashlib.sha256((root/'verification/assets'/name).read_bytes()).hexdigest())
                      for name in ['FoliaParityLatin-Bold.ttf', 'FoliaParityBidi-Bold.ttf', 'FoliaParityChatCJK-Bold.otf']]
contact = []
for group in d['reports']:
    condition_file = ('width-response-reference.json' if group['name']=='width-response' else group['name']+'-conditions.json')
    group['conditions'] = json.loads((root/'verification/pendolo-fixtures'/condition_file).read_text(encoding='utf-8'))
    for matrix in group['images']:
        relative = Path('build/verification') / matrix['dir']
        before, after = baseline/relative, root/relative
        assert not matrix['missingAfter']
        assert len(matrix['frames']) == (144 if group['name'] == 'tick-stage' else 168)
        changed = []
        for f in matrix['frames']:
            assert hashlib.sha256((after/f['file']).read_bytes()).hexdigest() == f['exact'], 'Capture changed during comparison'
            if f['before'] == f['exact']:
                continue
            a, b = [np.asarray(Image.open(p/f['file']).convert('RGB')).astype(int) for p in (before, after)]
            delta = abs(a-b)
            f.update(changedPixels=int(np.any(delta, axis=2).sum()), channelPeak=int(delta.max()), fullMAE=float(delta.mean()))
            changed.append(f)
        old_states, new_states = [json.loads((p/'states.json').read_text(encoding='utf-8')) for p in (before, after)] if (after/'states.json').exists() else (None, None)
        matrix['statesIdentical'] = old_states == new_states
        matrix['changedFrames'] = len(changed)
        if changed:
            contact.extend((before/f['file'], after/f['file']) for f in changed[:2])
        print(matrix['dir'], len(matrix['frames']), 'changed', len(changed), 'statesIdentical', matrix['statesIdentical'])
    for f in group['fixtures']:
        assert hashlib.sha256((root/f['file']).read_bytes()).hexdigest() == f['exact']
        assert f['before'] == f['exact'], f
d['note'] = 'All old/new fixture bytes and observed state equality are reported without altering algorithms, timing, pixel masks or gates. Small raw glyph/glow edge changes are not independently attributed to Motion versions. Existing large tick/rotation/geometry/raster residuals remain unresolved. This audit is not a new production behavior.'
report.write_text(json.dumps(d, indent=2)+'\n', encoding='utf-8')
sheet = Image.new('RGB', (1000, 400*len(contact)), (30, 30, 30))
draw = ImageDraw.Draw(sheet)
for i, pair in enumerate(contact):
    for j, p in enumerate(pair):
        draw.text((j*500+3, i*400+3), ('Previous | ' if j == 0 else 'Exact lock | ')+p.name, fill='white')
        picture = Image.open(p).convert('RGB'); picture.thumbnail((490, 370))
        sheet.paste(picture, (j*500, i*400+25))
if contact:
    sheet.save(report.parent/'pendolo-dense-source-contact.png')
