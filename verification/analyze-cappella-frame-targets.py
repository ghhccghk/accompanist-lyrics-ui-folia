"""Summarize passive original target/MotionValue/DOM observations, not native expectations.

Run the source probe with --targets to a fresh ignored path first. The complete
trace must reproduce v2 geometry and phases before its extra fields are accepted.
No Motion getters or animation sampling occur in this offline analysis.
"""
from pathlib import Path
import hashlib
import json
import sys

ROOT = Path(__file__).resolve().parents[1]
path = ROOT / sys.argv[1]
source = json.loads(path.read_text(encoding='utf-8'))
prior = json.loads((ROOT / 'verification/cappella-projection/frame-phases-v2.json').read_text())
assert source['originalGeometryMatches'] and source['instrumentedGeometryMatches']
assert source['motion'] == prior['motion']
assert source['runs'][0]['states'] == prior['runs'][0]['states']
trace = source['runs'][1]['trace']
stripped = [{**event, 'rows': [{k: v for k, v in row.items() if k != 'targets'}
                              for row in event['rows']]} for event in trace]
assert stripped == prior['runs'][1]['trace'], 'Observer altered original phase state; reject'

def phase(time, tag):
    return next(event for event in trace if event['media'] == time and event['tag'] == tag)

def row(event, identity):
    return next(r for r in event['rows'] if r['id'] == identity)

identity = 'line-0.94-3'
time = 1.0166666666666666
samples = [row(phase(time, tag), identity) for tag in
           ['before-media', 'after-media-before-raf', 'after-browser-task']]
assert [r['targets']['targetSize']['height'] for r in samples] == [110, 49, 49]
assert [r['frameInlineHeight'] for r in samples] == ['110px', '49px', '110px']
assert [r['targets']['frameMotion']['latest']['height'] for r in samples] == [110] * 3
assert [r['targets']['frameMotion']['values']['height']['animationResolved'] for r in samples] == [False, False, True]

columns = ['media', 'phase', 'id', 'targetWidth', 'targetHeight', 'inlineHeight',
           'computedHeight', 'motionWidth', 'motionHeight', 'heightResolved',
           'flowTop', 'flowHeight', 'position', 'motionMargin', 'computedMargin']
records = ['# ' + '\t'.join(columns)]
for event in trace:
    for r in event['rows']:
        target = r['targets']; motion = target['frameMotion'] or {}
        size = target['targetSize'] or {}; latest = motion.get('latest', {})
        group = target['groupMotion'] or {}; height = motion.get('values', {}).get('height', {})
        records.append('\t'.join(map(str, [event['media'], event['tag'], r['id'],
            size.get('width'), size.get('height'), r['frameInlineHeight'], r['frameComputedHeight'],
            latest.get('width'), latest.get('height'), height.get('animationResolved'),
            r['flowTop'], r['flowHeight'], target['position'],
            group.get('latest', {}).get('marginTop'), target['groupComputedMargin']])))

out = ROOT / 'verification/cappella-projection'
(out / 'frame-targets-v3.tsv').write_text('\n'.join(records) + '\n', encoding='utf-8')
report = {key: source[key] for key in ['revision', 'browser', 'motion', 'viewport', 'DPR', 'hashes', 'observer']}
report.update(scope='Passive original React target vs MotionValue data fields vs DOM; not a production repair.',
              fullTraceSha256=hashlib.sha256(path.read_bytes()).hexdigest(),
              priorPhaseTraceSha256=hashlib.sha256((out / 'frame-phases-v2.json').read_bytes()).hexdigest(),
              originalGeometryMatches=True, instrumentedGeometryMatches=True,
              originalPhaseTraceMatches=True, phases=len(trace), observations=len(records)-1,
              targetChangesDuringRestoration=False,
              boundary={'media': time, 'id': identity, 'heights': [
                  {'phase': tag, 'target': r['targets']['targetSize']['height'],
                   'inline': r['frameInlineHeight'], 'motion': r['targets']['frameMotion']['latest']['height'],
                   'animationResolved': r['targets']['frameMotion']['values']['height']['animationResolved']}
                  for tag, r in zip(['before-media', 'after-media-before-raf', 'after-browser-task'], samples)]})
(out / 'frame-targets-v3.json').write_text(json.dumps(report, indent=2) + '\n', encoding='utf-8')
print(json.dumps({key: report[key] for key in ['originalPhaseTraceMatches', 'phases', 'observations', 'boundary']}, indent=2))
