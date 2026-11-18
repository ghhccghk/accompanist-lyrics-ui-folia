"""Read-only analysis of the original, unmodified Motion snapshot transport."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
folder = ROOT / 'verification/cappella-projection'
source = json.loads((folder / 'frame-phases-v2.json').read_text(encoding='utf-8'))
assert source['originalGeometryMatches'] and source['instrumentedGeometryMatches']
trace = source['runs'][1]['trace']
rows = []
for event in trace:
    if event['tag'] != 'after-media-before-raf':
        continue
    before = next(p for p in trace if p['media'] == event['media'] and p['tag'] == 'before-media')
    after = next(p for p in trace if p['media'] == event['media'] and p['tag'] == 'after-browser-task')
    for row in event['rows']:
        projection = row['projection']
        if projection is None or projection['snapshot'] is None:
            continue
        incoming = next((r for r in before['rows'] if r['id'] == row['id']), None)
        target = next((r for r in after['rows'] if r['id'] == row['id']), None)
        if incoming is None or target is None:
            continue
        # Independently invert the observed complete row rectangle using its
        # ORIGINAL own y/scale. Do not use the snapshot as an input or native
        # values as expectations. This fixture has no scaled ancestor.
        top, height = incoming['rowRect'][1], incoming['rowRect'][3]
        own = incoming['projection']['latest']
        y, scale = own.get('y', 0), own.get('scale', 1)
        origin = top + height / 2 - y
        predicted = origin + (top - y - origin) / scale
        snapshot = projection['snapshot']['layout']['y']['min']
        rows.append(dict(media=event['media'], id=row['id'],
                         inverseOwnTransformTop=predicted, originalSnapshotTop=snapshot,
                         error=abs(predicted-snapshot),
                         originalFinalTargetTop=target['projection']['layout']['layout']['y']['min']))
assert len(rows) == 11
assert max(r['error'] for r in rows) <= 1e-9
print(json.dumps(dict(scope='Original before-media DOM rectangle -> original snapshot; not a production repair.',
                      rows=rows, peak=max(r['error'] for r in rows)), indent=2))
