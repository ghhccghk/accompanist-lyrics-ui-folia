"""Compare the SAME original incoming state; no registration or fitted offsets."""
from pathlib import Path
import hashlib
import json
import math

ROOT = Path(__file__).resolve().parent.parent
source = ROOT / 'verification/fume-fixtures/handoff-v1'
native = ROOT / 'build/verification/fume-handoff/continuous.tsv'
rows = [line.split('\t') for line in native.read_text().splitlines() if not line.startswith('#')]
groups = {}
for r in rows:
    p, q = (list(map(float, r[i].split(','))) for i in (4, 5))
    screen = math.hypot(q[0] - p[0], q[1] - p[1]) * max(q[6], p[6])
    key = (r[0], r[1])
    g = groups.setdefault(key, dict(case=r[0], rate=r[1], samples=0, screenPeak=0,
        scalePeak=0, focusPeak=0, branchMismatches=[]))
    g['samples'] += 1
    g['screenPeak'] = max(g['screenPeak'], screen)
    g['scalePeak'] = max(g['scalePeak'], abs(p[6]-q[6]))
    g['focusPeak'] = max(g['focusPeak'], math.hypot(p[4]-q[4], p[5]-q[5]))
    if r[7] != r[9]:
        g['branchMismatches'].append(dict(time=float(r[3]), current=r[7], anticipated=r[9]))
sha = lambda p: hashlib.sha256(p.read_bytes()).hexdigest()
report = dict(version=1, production='d810975 unchanged continuous model',
    scope='Same original incoming nine-state, retarget, original article/targets. No earlier song error. '
          'This diagnoses the old continuous adaptation; it is NOT visual acceptance or a successful behavior repair.',
    inputs={p.name:sha(p) for p in source.iterdir()}, nativeSha256=sha(native), groups=list(groups.values()))
out = ROOT / 'verification/fume-camera-handoff/same-incoming-v1.json'
out.write_text(json.dumps(report, indent=2)+'\n', encoding='utf-8')
for g in groups.values():
    if g['rate'] == 'mounted60':
        print(g['case'], 'screen', g['screenPeak'], 'scale', g['scalePeak'], 'branch', len(g['branchMismatches']))
